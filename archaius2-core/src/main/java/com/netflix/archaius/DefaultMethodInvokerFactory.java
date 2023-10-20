package com.netflix.archaius;

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

abstract class DefaultMethodInvokerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMethodInvokerFactory.class);

    private static final DefaultMethodInvokerFactory INSTANCE;
    static {
        if (SystemUtils.isJavaVersionAtMost(JavaVersion.JAVA_1_8)) {
            INSTANCE = new LegacyJava8Factory();
        } else if (SystemUtils.isJavaVersionAtMost(JavaVersion.JAVA_16)) {
            INSTANCE = new BoundMethodHandlesFactory();
        } else { // Java 17 onwards
            INSTANCE = new LambdasFactory();
        }
        LOG.info("Choosing {} as invoker factory for default methods", INSTANCE.getClass());
    }

    static DefaultMethodInvokerFactory getFactory() {
        return INSTANCE;
    }

    /**
     * Returns a Function object that acts as a call to <code>invokerBound.method(...)</code>.
     * <p>
     * {@literal <}R> is expected to be the METHOD's return type. Unfortunately this constraint can't be enforced by the compiler.
     *
     * @param methodOwner An interface that declares (or inherits) the wanted method.
     * @param method The method to be called. This is expected to be a <code>default</code> method in METHOD_OWNER.
     * @param invokerBound An instance of the METHOD_OWNER interface.
     */
     abstract <T, R> Function<Object[], R> findOrCreateDefaultMethodInvoker(Class<T> methodOwner, Method method, T invokerBound);

    /**
     * Shared "fall back" implementation using reflection to invoke the method handle.
     */
     <T, R> Function<Object[], R> bindMethodHandle(MethodHandle methodHandle, T invokerBound) {
         // Exceptions from this call are NOT caught because we want to fail fast if the arguments to the factory are bad.
         MethodHandle boundHandle = methodHandle.bindTo(invokerBound);

         return (args) -> {
             try {
                 if (boundHandle.type().parameterCount() == 0) {

                     //noinspection unchecked
                     return (R) boundHandle.invokeWithArguments();
                 } else if (args != null) {

                     //noinspection unchecked
                     return (R) boundHandle.invokeWithArguments(args);
                 } else {
                     // This is a handle to a method WITH arguments, being called with none. This happens when
                     // invokerBound.toString() is called on an object that has a parametrized property AND the interface
                     // provides a default method for it. There's no good default to return here, so we'll just use null
                     return null;
                 }
             } catch (Throwable e) {
                 maybeWrapThenRethrow(e);
                 return null; // Unreachable, but the compiler doesn't know
             }
         };

     }


    /**
     * For Java 17 onwards, we wrap the method calls in lambdas and return that. This skips the reflection machinery and
     * provides a noticeable performance boost. This implementation is known to fail in java 9 - 11. It *should* work
     * from 12 or 13 onwards, but we have only tested it in 17 and 21.
     */
    private static class LambdasFactory extends DefaultMethodInvokerFactory {
        // We keep a running count of how many times we've seen a given Class. If we cross the threshold we stop creating
        // lambdas for that class and fall back to the reflective implementation. This is motivated by an edge case we
        // saw in clients, where they create (and leak) large numbers of proxies to the same interface. Because each lambda
        // object requires its own anon class object, which lives in metaspace, and since users sometimes size their
        // metaspaces to be much smaller than the heap, the leak becomes much more visible and causes OOMs.
        private static final Map<String, Integer> SEEN_COUNTS = new ConcurrentHashMap<>();

        // The threshold is simply a best guess. Creating 1000 lambdas for the same interface is almost certainly
        // a leak, right?
        private static final int MAX_SEEN_THRESHOLD = 1000;

        @Override
         <T, R> Function<Object[], R> findOrCreateDefaultMethodInvoker(Class<T> methodOwner, Method method, T invokerBound) {
             MethodHandles.Lookup lookup = MethodHandles.lookup();
             MethodHandle methodHandle;

             try {
                 methodHandle = lookup.findSpecial(
                         method.getDeclaringClass(),
                         method.getName(),
                         MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                         method.getDeclaringClass());
             } catch (NoSuchMethodException | IllegalAccessException e) {
                 throw new RuntimeException(e);
             }

             if (methodHandle.type().parameterCount() <= 2 &&
                 SEEN_COUNTS.getOrDefault(methodOwner.getName(), 0) <= MAX_SEEN_THRESHOLD) {

                 // For 1 or 2 argument handles (which translate to 0 and 1 argument *methods*), build a lambda object
                 // and use that to make the call. This avoids the cost of the reflection machinery and has a significant
                 // performance boost.
                 // But first, count how many times we've been here for this owner
                 SEEN_COUNTS.merge(methodOwner.getName(), 1, (oldValue, ignore) -> oldValue + 1);

                 if (methodHandle.type().parameterCount() == 1) {
                     Function<Object, Object> getter = asFunction(lookup, methodHandle);
                     //noinspection unchecked,DataFlowIssue
                     return (args) -> (R) getter.apply(invokerBound);

                 } else if (methodHandle.type().parameterCount() == 2) {
                     BiFunction<Object, Object, Object> getter = asBiFunction(lookup, methodHandle);
                     //noinspection unchecked,DataFlowIssue
                     return (args) ->
                          args == null ? null : (R) getter.apply(invokerBound, args[0]);
                 }
             }

            // Otherwise, for handles with more than 2 arguments or if we've reached the threshold, fall back to
            // reflection
            return bindMethodHandle(methodHandle, invokerBound);
         }

         /**
          * For a given no-args method M, return a (possibly cached) Function equivalent to the lambda
          * <code>instance -> instance.M()</code>,
          * where <code>instance</code> is an object of an adequate type.
          */
         @SuppressWarnings("unchecked")
         private static Function<Object, Object> asFunction(MethodHandles.Lookup lookup, MethodHandle methodHandle) {
                 try {
                 CallSite site = LambdaMetafactory.metafactory(lookup,
                         "apply",
                         MethodType.methodType(Function.class),
                         MethodType.methodType(Object.class, Object.class),
                         methodHandle,
                         methodHandle.type());
                 return (Function<Object, Object>) site.getTarget().invokeExact();
             } catch (Throwable t) {
                 maybeWrapThenRethrow(t);
                 return null; // Unreachable, but the compiler doesn't know.
             }
         }

         @SuppressWarnings("unchecked")
         private static BiFunction<Object, Object, Object> asBiFunction(MethodHandles.Lookup lookup, MethodHandle methodHandle) {
             try {
                 CallSite site = LambdaMetafactory.metafactory(lookup,
                         "apply",
                         MethodType.methodType(BiFunction.class),
                         MethodType.methodType(Object.class, Object.class, Object.class),
                         methodHandle,
                         methodHandle.type());
                 return (BiFunction<Object, Object, Object>) site.getTarget().invokeExact();
             } catch (Throwable t) {
                 maybeWrapThenRethrow(t);
                 return null; // Unreachable, but the compiler doesn't know
             }
         }
     }

    /**
     * An implementation safe to use in Java 9 - 16. It looks up a method handle which it then
     * binds directly to the requested instance.
     */
    private static class BoundMethodHandlesFactory extends DefaultMethodInvokerFactory {
         @Override
         <T, R> Function<Object[], R> findOrCreateDefaultMethodInvoker(Class<T> methodOwner, Method method, T invokerBound) {
             final MethodHandle methodHandle;

             try {
                 methodHandle = MethodHandles.lookup()
                         .findSpecial(methodOwner,
                                 method.getName(),
                                 MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                                 methodOwner);
             } catch (NoSuchMethodException | IllegalAccessException e) {
                 throw new RuntimeException("Failed to create temporary object for " + methodOwner.getName(), e);
             }

             return bindMethodHandle(methodHandle, invokerBound);
         }
     }

    /**
     * For Java 8 we use a hacky mechanism (which got disabled in Java 9) to look up a method handle which we then
     * bind directly to the requested function object.
     */
    private static class LegacyJava8Factory extends DefaultMethodInvokerFactory {

        @Override
        <T, R> Function<Object[], R> findOrCreateDefaultMethodInvoker(Class<T> methodOwner, Method method, T invokerBound) {
            final MethodHandle methodHandle;

            try {
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                        .getDeclaredConstructor(Class.class, int.class);
                constructor.setAccessible(true);
                methodHandle = constructor.newInstance(methodOwner, MethodHandles.Lookup.PRIVATE)
                        .unreflectSpecial(method, methodOwner);

            } catch (NoSuchMethodException | InstantiationException |
                     IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to create temporary object for " + methodOwner.getName(), e);
            }

            return bindMethodHandle(methodHandle, invokerBound);
        }
    }

    private static void maybeWrapThenRethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
