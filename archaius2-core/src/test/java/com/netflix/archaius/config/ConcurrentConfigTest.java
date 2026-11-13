package com.netflix.archaius.config;

import com.netflix.archaius.Layers;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.Layer;
import com.netflix.archaius.api.StrInterpolator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ConcurrentConfigTest {
    @TestFactory
    Stream<DynamicTest> reentrantRefreshes() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind + " retries after a same-thread notification during snapshotting", () ->
                        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                            Parent parent = new Parent(kind);
                            ReentrantConfig child = new ReentrantConfig();
                            child.setProperty("reentrant", "old");
                            parent.add(child);
                            Thread thread = Thread.currentThread();
                            child.snapshots = 0;
                            child.afterSnapshot = () -> {
                                assertSame(thread, Thread.currentThread());
                                child.setProperty("reentrant", "new");
                            };

                            child.setProperty("trigger", "value");

                            // The nested refresh publishes first, so the outer stale snapshot must be rebuilt.
                            assertEquals(3, child.snapshots);
                            assertTrue(parent.contains(child));
                            assertEquals("new", parent.config.getRawProperty("reentrant"));
                            assertEquals("value", parent.config.getRawProperty("trigger"));
                        })));
    }

    @TestFactory
    Stream<DynamicTest> randomizedConcurrentAdditionsAndUpdates() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind + " retains children and committed updates under concurrent writes", () -> {
                    Parent parent = new Parent(kind);
                    List<DefaultSettableConfig> registered = new CopyOnWriteArrayList<>();
                    registered.add(parent.source);
                    int adders = 3;
                    int mutators = 3;
                    CountDownLatch start = new CountDownLatch(1);
                    ExecutorService executor = Executors.newFixedThreadPool(adders + mutators);
                    List<Future<?>> operations = new ArrayList<>();
                    try {
                        for (int writer = 0; writer < adders + mutators; writer++) {
                            final int id = writer;
                            operations.add(executor.submit(() -> {
                                assertTrue(start.await(5, SECONDS), "Writers did not start");
                                // Fixed seeds make failures reproducible while scheduling varies between runs.
                                Random random = new Random(772L + id);
                                int iterations = id < adders ? 8 : 60;
                                for (int iteration = 0; iteration < iterations; iteration++) {
                                    String key = "writer-" + id + "-" + iteration;
                                    if (id < adders) {
                                        DefaultSettableConfig child = new DefaultSettableConfig(key);
                                        child.setProperty(key, iteration);
                                        parent.add(child);
                                        registered.add(child);
                                    } else {
                                        DefaultSettableConfig child = registered.get(random.nextInt(registered.size()));
                                        child.setProperty(key, iteration);
                                    }
                                    // Each write has its own key, so every committed value must remain visible.
                                    assertEquals(iteration, parent.config.getRawProperty(key));
                                    parent.assertRegistered(registered);
                                    if (random.nextBoolean()) {
                                        Thread.yield();
                                    }
                                }
                                return null;
                            }));
                        }
                        start.countDown();
                        for (Future<?> operation : operations) {
                            operation.get(15, SECONDS);
                        }
                        assertEquals(1 + adders * 8, registered.size());
                        parent.assertRegistered(registered);
                        for (DefaultSettableConfig child : registered) {
                            child.forEachPropertyUninstrumented((key, value) ->
                                    assertEquals(value, parent.config.getRawProperty(key), key));
                        }
                    } finally {
                        start.countDown();
                        executor.shutdownNow();
                        assertTrue(executor.awaitTermination(5, SECONDS), "Writers did not terminate");
                    }
                }));
    }

    @TestFactory
    Stream<DynamicTest> failedAdditionRestoresChildBeforeRetry() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind + " rolls back a failed addition before retrying", () -> {
                    Parent parent = new Parent(kind);
                    FailingConfig child = new FailingConfig();
                    Decoder decoder = child.getDecoder();
                    StrInterpolator interpolator = child.getStrInterpolator();
                    parent.config.setDecoder(mock(Decoder.class));
                    parent.config.setStrInterpolator(mock(StrInterpolator.class));
                    ConfigListener observer = mock(ConfigListener.class);
                    parent.config.addListener(observer);
                    child.failNextSnapshot = true;

                    assertSame(child.failure, assertThrows(IllegalStateException.class, () -> parent.add(child)));
                    child.setProperty("failed", "value");

                    assertAll(
                            () -> assertFalse(parent.contains(child)),
                            () -> assertEquals(0, child.getListeners().size()),
                            () -> assertSame(decoder, child.getDecoder()),
                            () -> assertSame(interpolator, child.getStrInterpolator()),
                            () -> verifyNoInteractions(observer));

                    parent.add(child);
                    clearInvocations(observer);
                    child.setProperty("retried", "value");

                    assertEquals(1, child.getListeners().size());
                    verify(observer, times(1)).onConfigUpdated(parent.config);
                    assertEquals("value", parent.config.getRawProperty("retried"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> removalDetachesListenerBeforeReadding() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind + " detaches a removed child before readding it", () -> {
                    Parent parent = new Parent(kind);
                    ConfigListener observer = mock(ConfigListener.class);
                    parent.config.addListener(observer);

                    parent.remove(parent.source);
                    clearInvocations(observer);
                    parent.source.setProperty("removed", "value");

                    assertAll(
                            () -> assertEquals(0, parent.source.getListeners().size()),
                            () -> verifyNoInteractions(observer));

                    parent.add(parent.source);
                    clearInvocations(observer);
                    parent.source.setProperty("readded", "value");

                    assertEquals(1, parent.source.getListeners().size());
                    verify(observer, times(1)).onConfigUpdated(parent.config);
                    assertEquals("value", parent.config.getRawProperty("readded"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> sharedChildKeepsListenerUntilLastRemoval() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind + " keeps one listener until a shared child's last removal", () -> {
                    Parent parent = new Parent(kind);
                    FailingConfig child = new FailingConfig();
                    parent.add(child);
                    child.failNextSnapshot = true;

                    assertSame(child.failure, assertThrows(IllegalStateException.class,
                            () -> parent.addShared(child)));
                    assertEquals(1, child.getListeners().size());

                    parent.addShared(child);
                    assertEquals(1, child.getListeners().size());
                    parent.remove(child);
                    ConfigListener observer = mock(ConfigListener.class);
                    parent.config.addListener(observer);
                    child.setProperty("shared", "value");

                    assertEquals(1, child.getListeners().size());
                    verify(observer, times(1)).onConfigUpdated(parent.config);
                    assertEquals("value", parent.config.getRawProperty("shared"));

                    parent.removeShared(child);
                    clearInvocations(observer);
                    child.setProperty("detached", "value");

                    assertEquals(0, child.getListeners().size());
                    verifyNoInteractions(observer);
                    assertNull(parent.config.getRawProperty("shared"));
                }));
    }

    @Test
    void layeredRefreshesCanIterateChildrenConcurrently() throws Exception {
        Parent parent = Parent.layeredWithBlockingChildFirst();

        try (Pause refresh = parent.blocker.snapshot) {
            // Pause before the refresh reaches the application layer's child iterator.
            refresh.start(parent.blocker::fireUpdate);
            refresh.awaitPaused();

            parent.source.setProperty("new", "value");

            refresh.resumeAndAwaitCompletion();
        }

        assertEquals("value", parent.config.getRawProperty("new"));
    }

    @TestFactory
    Stream<DynamicTest> refreshDoesNotLoseConcurrentlyAddedChild() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind, () -> {
                    Parent parent = new Parent(kind);
                    DefaultSettableConfig added = new DefaultSettableConfig("added");
                    added.setProperty("added", "value");

                    try (Pause refresh = parent.pauseAfterCopyingSource()) {
                        refresh.start(parent.blocker::fireUpdate);
                        refresh.awaitPaused();

                        parent.add(added);

                        refresh.resumeAndAwaitCompletion();
                    }

                    // Publishing the older refresh must not discard the newly added child.
                    assertTrue(parent.contains(added));
                    assertEquals("value", parent.config.getRawProperty("added"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> refreshDoesNotRestoreConcurrentlyRemovedChild() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind, () -> {
                    Parent parent = new Parent(kind);

                    try (Pause refresh = parent.pauseAfterCopyingSource()) {
                        refresh.start(parent.blocker::fireUpdate);
                        refresh.awaitPaused();

                        parent.remove(parent.source);

                        refresh.resumeAndAwaitCompletion();
                    }

                    // The paused refresh still holds the removed child's properties.
                    assertFalse(parent.contains(parent.source));
                    assertNull(parent.config.getRawProperty("source"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> olderRefreshDoesNotOverwriteNewerValues() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind, () -> {
                    Parent parent = new Parent(kind);

                    try (Pause refresh = parent.pauseAfterCopyingSource()) {
                        refresh.start(parent.blocker::fireUpdate);
                        refresh.awaitPaused();

                        parent.source.setProperty("new", "value");

                        refresh.resumeAndAwaitCompletion();
                    }

                    // The older snapshot lacks this key and must not replace the newer snapshot.
                    assertEquals("value", parent.source.getRawProperty("new"));
                    assertEquals("value", parent.config.getRawProperty("new"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> additionDoesNotOverwriteConcurrentChildUpdate() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind, () -> {
                    Parent parent = new Parent(kind);
                    DefaultSettableConfig added = new DefaultSettableConfig("added");

                    try (Pause addition = parent.pauseAfterCopyingSource()) {
                        addition.start(() -> parent.add(added));
                        addition.awaitPaused();

                        parent.source.setProperty("new", "value");

                        addition.resumeAndAwaitCompletion();
                    }

                    assertTrue(parent.contains(added));
                    assertEquals("value", parent.config.getRawProperty("new"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> removalDoesNotOverwriteConcurrentChildUpdate() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind, () -> {
                    Parent parent = new Parent(kind);

                    try (Pause removal = parent.blocker.snapshot) {
                        removal.start(() -> parent.remove(parent.source));
                        removal.awaitPaused();

                        parent.blocker.setProperty("new", "value");

                        removal.resumeAndAwaitCompletion();
                    }

                    assertFalse(parent.contains(parent.source));
                    assertEquals("value", parent.config.getRawProperty("new"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> additionIncludesChildUpdateDuringListenerSetup() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind, () -> {
                    Parent parent = new Parent(kind);
                    PausingWiringConfig added = new PausingWiringConfig("added");

                    try (Pause addition = added.wiring) {
                        // Pause during setup, before the child's properties are copied.
                        addition.start(() -> parent.add(added));
                        addition.awaitPaused();

                        added.setProperty("new", "value");

                        addition.resumeAndAwaitCompletion();
                    }

                    assertTrue(parent.contains(added));
                    assertEquals("value", parent.config.getRawProperty("new"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> additionIncludesChildUpdateAfterSnapshot() {
        return Stream.of("composite", "reversed composite", "layered").map(kind ->
                DynamicTest.dynamicTest(kind, () -> {
                    Parent parent = new Parent(kind);
                    BlockingConfig added = new BlockingConfig();

                    try (Pause addition = added.snapshot) {
                        // Pause after copying the new child, before publishing it in the parent.
                        addition.start(() -> parent.add(added));
                        addition.awaitPaused();

                        added.setProperty("new", "value");

                        addition.resumeAndAwaitCompletion();
                    }

                    assertTrue(parent.contains(added));
                    assertEquals("value", parent.config.getRawProperty("new"));
                }));
    }

    /** Pauses the first time a parent wires its interpolator into this config during {@code addConfig}. */
    private static final class PausingWiringConfig extends DefaultSettableConfig {
        private final Pause wiring = new Pause();

        PausingWiringConfig(String name) {
            super(name);
        }

        @Override
        public void setStrInterpolator(StrInterpolator interpolator) {
            super.setStrInterpolator(interpolator);
            wiring.pauseIfArmed();
        }
    }

    private static final class Parent {
        private final DefaultCompositeConfig composite;
        private final DefaultLayeredConfig layered;
        private final Config config;
        private final DefaultSettableConfig source = new DefaultSettableConfig("source");
        private final BlockingConfig blocker = new BlockingConfig();

        Parent(String kind) throws Exception {
            this(kind, Layers.LIBRARY);
        }

        static Parent layeredWithBlockingChildFirst() throws Exception {
            return new Parent("layered", Layers.RUNTIME);
        }

        private Parent(String kind, Layer blockingChildLayer) throws Exception {
            source.setProperty("source", "value");
            if (kind.equals("layered")) {
                composite = null;
                layered = new DefaultLayeredConfig();
                config = layered;
                add(source);
                layered.addConfig(blockingChildLayer, blocker);
            } else {
                composite = new DefaultCompositeConfig(kind.equals("reversed composite"));
                layered = null;
                config = composite;
                // The blocker must be visited after the source's properties have been copied.
                if (kind.equals("reversed composite")) {
                    add(blocker);
                    add(source);
                } else {
                    add(source);
                    add(blocker);
                }
            }
        }

        void add(Config child) throws Exception {
            if (composite != null) {
                assertTrue(composite.addConfig(child.getName(), child));
            } else {
                layered.addConfig(Layers.APPLICATION, child);
            }
        }

        void remove(Config child) {
            if (composite != null) {
                assertSame(child, composite.removeConfig(child.getName()));
            } else {
                assertSame(child, layered.removeConfig(Layers.APPLICATION, child.getName()).get());
            }
        }

        void addShared(Config child) throws Exception {
            if (composite != null) {
                assertTrue(composite.addConfig("alias", child));
            } else {
                layered.addConfig(Layers.LIBRARY, child);
            }
        }

        void removeShared(Config child) {
            if (composite != null) {
                assertSame(child, composite.removeConfig("alias"));
            } else {
                assertSame(child, layered.removeConfig(Layers.LIBRARY, child.getName()).get());
            }
        }

        boolean contains(Config child) {
            return composite != null
                    ? composite.getConfig(child.getName()) == child
                    : layered.getConfigsAtLayer(Layers.APPLICATION).contains(child);
        }

        void assertRegistered(List<DefaultSettableConfig> registered) {
            // Capture the expected children first so concurrent additions cannot make the check race.
            List<DefaultSettableConfig> expected = new ArrayList<>(registered);
            if (composite != null) {
                Collection<String> names = composite.getConfigNames();
                for (Config child : expected) {
                    assertTrue(names.contains(child.getName()), child.getName());
                    assertSame(child, composite.getConfig(child.getName()));
                }
            } else {
                assertTrue(layered.getConfigsAtLayer(Layers.APPLICATION).containsAll(expected));
            }
        }

        /** Pauses after copying the source's properties but before publishing the parent state. */
        Pause pauseAfterCopyingSource() {
            return blocker.snapshot;
        }
    }

    private static final class FailingConfig extends DefaultSettableConfig {
        private final IllegalStateException failure = new IllegalStateException("Snapshot failed");
        private boolean failNextSnapshot;

        @Override
        public void forEachPropertyUninstrumented(BiConsumer<String, Object> consumer) {
            if (failNextSnapshot) {
                failNextSnapshot = false;
                throw failure;
            }
            super.forEachPropertyUninstrumented(consumer);
        }
    }

    private static final class ReentrantConfig extends DefaultSettableConfig {
        private Runnable afterSnapshot;
        private int snapshots;

        @Override
        public void forEachPropertyUninstrumented(BiConsumer<String, Object> consumer) {
            snapshots++;
            super.forEachPropertyUninstrumented(consumer);
            Runnable callback = afterSnapshot;
            if (callback != null) {
                // Consume the update before notifying so the nested snapshot observes a quiescent child.
                afterSnapshot = null;
                callback.run();
            }
        }
    }

    private static final class BlockingConfig extends DefaultSettableConfig {
        private final Pause snapshot = new Pause();

        void fireUpdate() {
            // Invoke the registered listeners directly so snapshot failures reach the test.
            getListeners().forEach(listener -> listener.onConfigUpdated(this));
        }

        @Override
        public void forEachPropertyUninstrumented(BiConsumer<String, Object> consumer) {
            super.forEachPropertyUninstrumented(consumer);
            snapshot.pauseIfArmed();
        }
    }

    /** Pauses only once so a retried snapshot can finish without blocking again. */
    private static final class Pause implements AutoCloseable {
        private final AtomicBoolean armed = new AtomicBoolean();
        private final CountDownLatch paused = new CountDownLatch(1);
        private final CountDownLatch resume = new CountDownLatch(1);
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private Future<?> operation;

        void start(Executable action) {
            armed.set(true);
            operation = executor.submit(() -> {
                try {
                    action.execute();
                } catch (Throwable e) {
                    throw new AssertionError(e);
                }
            });
        }

        void awaitPaused() throws InterruptedException {
            assertTrue(paused.await(5, SECONDS), "Operation did not reach the pause");
        }

        void resumeAndAwaitCompletion() throws Exception {
            resume.countDown();
            if (operation != null) {
                operation.get(5, SECONDS);
            }
        }

        @Override
        public void close() throws Exception {
            try {
                // Always release the worker, including when the concurrent action or an assertion fails.
                resumeAndAwaitCompletion();
            } finally {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, SECONDS), "Paused operation did not terminate");
            }
        }

        void pauseIfArmed() {
            if (armed.compareAndSet(true, false)) {
                paused.countDown();
                try {
                    assertTrue(resume.await(5, SECONDS), "Operation was not resumed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
        }
    }
}
