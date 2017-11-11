package com.netflix.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

public abstract class AbstractPropertyRepoTest<PR extends PropertyRepo> {
    private final Function<Properties, PR> propertyRepoFunction;
    private final BiConsumer<String, Object> propertyMutator;
    protected PR propertyRepo;

    static class CountingCallback implements Runnable {
        private AtomicInteger counter = new AtomicInteger();

        @Override
        public void run() {
            counter.getAndIncrement();
        }

        public int getInvocations() {
            return counter.get();
        }

        public void reset() {
            counter.set(0);
        }
    }

    protected AbstractPropertyRepoTest(Function<Properties, PR> propertyRepoFunction,
            BiConsumer<String, Object> propertyMutator) {
        this.propertyRepoFunction = propertyRepoFunction;
        this.propertyMutator = propertyMutator;
    }

    @Before
    public void init() {
        Properties properties = new Properties();
        properties.put("PropertyRepoTest.string1", "value1");
        properties.put("PropertyRepoTest.string2", "value1,value2,value3");
        properties.put("PropertyRepoTest.string3", "value4,value5,value6");
        properties.put("PropertyRepoTest.int1", "75");
        properties.put("PropertyRepoTest.int2", "76");
        properties.put("PropertyRepoTest.bool1", "true");
        properties.put("PropertyRepoTest.bool2", "false");
        properties.put("PropertyRepoTest.bool3", "yes");
        properties.put("PropertyRepoTest.bool4", "no");

        propertyRepo = propertyRepoFunction.apply(properties);
    }

    @Test
    public void testSimpleString() {
        Assert.assertEquals("value1", propertyRepo.getProperty("PropertyRepoTest.string1", "default").get());
        Assert.assertEquals("value1,value2,value3",
                propertyRepo.getProperty("PropertyRepoTest.string2", "default").get());
        Assert.assertEquals("default", propertyRepo.getProperty("PropertyRepoTest.stringNotThere", "default").get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.stringNotThere", (String) null).get());
    }

    @Test
    public void testSimpleStringCallback() {
        Supplier<String> string1 = propertyRepo.getProperty("PropertyRepoTest.string1", "default");
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(string1, callback);
        int startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.string1", "value1*");
        Assert.assertEquals("value1*", string1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());

        callback.reset();
        Supplier<String> stringNotThere = propertyRepo.getProperty("PropertyRepoTest.callbackStringProp", "default");
        propertyRepo.onChange(stringNotThere, callback);
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.callbackStringProp", "some value");
        Assert.assertEquals("some value", stringNotThere.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
    }

    @Test
    public void testSimpleStringSet() {
        Set<String> stringSet = propertyRepo.getProperty("PropertyRepoTest.string2", Collections.emptySet()).get();
        Assert.assertEquals(3, stringSet.size());
        Assert.assertTrue(stringSet.contains("value1") && stringSet.contains("value2") && stringSet.contains("value3"));
        Assert.assertEquals(Collections.emptySet(),
                propertyRepo.getProperty("PropertyRepoTest.stringNotThere", Collections.emptySet()).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.stringNotThere", (Set<String>) null).get());
    }

    @Test
    public void testSimpleStringSetCallback() {
        Supplier<Set<String>> string2Supplier = propertyRepo.getProperty("PropertyRepoTest.string2",
                Collections.emptySet());
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(string2Supplier, callback);
        int startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.string2", "valueOne,valueTwo,valueThree");
        Set<String> string2 = string2Supplier.get();
        Assert.assertEquals(3, string2.size());
        Assert.assertTrue(
                string2.contains("valueOne") && string2.contains("valueTwo") && string2.contains("valueThree"));
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());

        callback = new CountingCallback();
        Supplier<Set<String>> stringNotThereSupplier = propertyRepo
                .getProperty("PropertyRepoTest.callbackStringSetProp", Collections.emptySet());
        propertyRepo.onChange(stringNotThereSupplier, callback);
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.callbackStringSetProp", "a,b,c");
        Set<String> stringNotThere = stringNotThereSupplier.get();
        Assert.assertEquals(3, stringNotThere.size());
        Assert.assertTrue(stringNotThere.contains("a") && stringNotThere.contains("b") && stringNotThere.contains("c"));
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
    }

    @Test
    public void testSimpleInt() {
        Assert.assertEquals(75, (int) propertyRepo.getProperty("PropertyRepoTest.int1", -1).get());
        Assert.assertEquals(-1, (int) propertyRepo.getProperty("PropertyRepoTest.intNotThere", -1).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.intNotThere", (Integer) null).get());
    }

    @Test
    public void testSimpleIntCallback() {
        Supplier<Integer> prop1Supplier = propertyRepo.getProperty("PropertyRepoTest.int1", -1);
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(prop1Supplier, callback);
        int startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.int1", -100);
        Assert.assertEquals(-100, (int) prop1Supplier.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());

        callback.reset();
        Supplier<Integer> prop2NotThere = propertyRepo.getProperty("PropertyRepoTest.callbackIntProp", -1);
        propertyRepo.onChange(prop2NotThere, callback);
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.callbackIntProp", -100);
        Assert.assertEquals(-100, (int) prop2NotThere.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
    }

    @Test
    public void testSimpleLong() {
        Assert.assertEquals(75L, (long) propertyRepo.getProperty("PropertyRepoTest.int1", -1L).get());
        Assert.assertEquals(-1L, (long) propertyRepo.getProperty("PropertyRepoTest.intNotThere", -1L).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.intNotThere", (Long) null).get());
    }

    @Test
    public void testSimpleLongCallback() {
        Supplier<Long> prop1Supplier = propertyRepo.getProperty("PropertyRepoTest.int1", -1L);
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(prop1Supplier, callback);
        int startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.int1", -100L);
        Assert.assertEquals(-100L, (long) prop1Supplier.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());

        callback.reset();
        Supplier<Long> prop2NotThere = propertyRepo.getProperty("PropertyRepoTest.callbackLongProp", -1L);
        propertyRepo.onChange(prop2NotThere, callback);
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.callbackLongProp", -100L);
        Assert.assertEquals(-100L, (long) prop2NotThere.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
    }

    @Test
    public void testSimpleBoolean() {
        Assert.assertEquals(true, (boolean) propertyRepo.getProperty("PropertyRepoTest.bool1", false).get());
        Assert.assertEquals(false, (boolean) propertyRepo.getProperty("PropertyRepoTest.bool2", true).get());
        Assert.assertEquals(true, (boolean) propertyRepo.getProperty("PropertyRepoTest.bool3", false).get());
        Assert.assertEquals(false, (boolean) propertyRepo.getProperty("PropertyRepoTest.bool4", true).get());
        Assert.assertFalse(propertyRepo.getProperty("PropertyRepoTest.boolNotThere", false).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.boolNotThere", (Boolean) null).get());
    }

    @Test
    public void testSimpleBooleanCallback() {
        Supplier<Boolean> prop1Supplier = propertyRepo.getProperty("PropertyRepoTest.bool1", (Boolean) null);
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(prop1Supplier, callback);
        int startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.bool1", false);
        Assert.assertFalse(prop1Supplier.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());

        callback.reset();
        Supplier<Boolean> prop2NotThere = propertyRepo.getProperty("PropertyRepoTest.callbackBoolProp", (Boolean) null);
        propertyRepo.onChange(prop2NotThere, callback);
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.callbackBoolProp", true);
        Assert.assertTrue(prop2NotThere.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
    }

    @Test
    public void testChainedString() {
        Assert.assertEquals("value1", propertyRepo
                .getProperty("PropertyRepoTest.string1", "PropertyRepoTest.stringNotThere", "default").get());
        Assert.assertEquals("value1",
                propertyRepo.getProperty("PropertyRepoTest.string1", "PropertyRepoTest.string2", "default").get());
        Assert.assertEquals("value1,value2,value3",
                propertyRepo.getProperty("PropertyRepoTest.string2", "PropertyRepoTest.string1", "default").get());
        Assert.assertEquals("value1", propertyRepo
                .getProperty("PropertyRepoTest.stringNotThere", "PropertyRepoTest.string1", "default").get());
        Assert.assertEquals("default", propertyRepo
                .getProperty("PropertyRepoTest.stringNotThere1", "PropertyRepoTest.stringNotThere2", "default").get());
        Assert.assertNull(propertyRepo
                .getProperty("PropertyRepoTest.stringNotThere1", "PropertyRepoTest.stringNotThere2", (String) null)
                .get());
    }

    @Test
    public void testChainedStringCallback() {
        Supplier<String> string1 = propertyRepo.getProperty("PropertyRepoTest.string1", "PropertyRepoTest.string2",
                "default");
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(string1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.string2", "value2*");
        Assert.assertEquals("value1", string1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.string1", "value1*");
        Assert.assertEquals("value1*", string1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<String> string2 = propertyRepo.getProperty("PropertyRepoTest.stringCallback1",
                "PropertyRepoTest.stringCallback2", "default");
        propertyRepo.onChange(string2, callback);
        Assert.assertEquals("default", string2.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.stringCallback2", "value2*");
        Assert.assertEquals("value2*", string2.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.stringCallback1", "value1*");
        Assert.assertEquals("value1*", string2.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testChainedInteger() {
        Assert.assertEquals(75,
                (int) propertyRepo.getProperty("PropertyRepoTest.int1", "PropertyRepoTest.intNotThere", -1).get());
        Assert.assertEquals(76,
                (int) propertyRepo.getProperty("PropertyRepoTest.int2", "PropertyRepoTest.int1", -1).get());
        Assert.assertEquals(75,
                (int) propertyRepo.getProperty("PropertyRepoTest.int1", "PropertyRepoTest.int2", -1).get());
        Assert.assertEquals(75,
                (int) propertyRepo.getProperty("PropertyRepoTest.intNotThere", "PropertyRepoTest.int1", -1).get());
        Assert.assertEquals(-1, (int) propertyRepo
                .getProperty("PropertyRepoTest.intNotThere1", "PropertyRepoTest.intNotThere2", -1).get());
        Assert.assertNull(propertyRepo
                .getProperty("PropertyRepoTest.intNotThere1", "PropertyRepoTest.intNotThere2", (Integer) null).get());
    }

    @Test
    public void testChainedIntegerCallback() {
        Supplier<Integer> property1 = propertyRepo.getProperty("PropertyRepoTest.int1", "PropertyRepoTest.int2", -1);
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.int2", 176);
        Assert.assertEquals(75, (int) property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.int1", 175);
        Assert.assertEquals(175, (int) property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<Integer> property2 = propertyRepo.getProperty("PropertyRepoTest.intCallback1",
                "PropertyRepoTest.intCallback2", -1);
        propertyRepo.onChange(property2, callback);
        Assert.assertEquals(-1, (int) property2.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.intCallback2", 276);
        Assert.assertEquals(276, (int) property2.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.intCallback1", 275);
        Assert.assertEquals(275, (int) property2.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testChainedLong() {
        Assert.assertEquals(75,
                (long) propertyRepo.getProperty("PropertyRepoTest.int1", "PropertyRepoTest.longNotThere", -1L).get());
        Assert.assertEquals(76,
                (long) propertyRepo.getProperty("PropertyRepoTest.int2", "PropertyRepoTest.int1", -1L).get());
        Assert.assertEquals(75,
                (long) propertyRepo.getProperty("PropertyRepoTest.int1", "PropertyRepoTest.int2", -1L).get());
        Assert.assertEquals(75,
                (long) propertyRepo.getProperty("PropertyRepoTest.longNotThere", "PropertyRepoTest.int1", -1L).get());
        Assert.assertEquals(-1L, (long) propertyRepo
                .getProperty("PropertyRepoTest.longNotThere1", "PropertyRepoTest.longNotThere2", -1L).get());
        Assert.assertNull(propertyRepo
                .getProperty("PropertyRepoTest.longNotThere1", "PropertyRepoTest.longNotThere2", (Long) null).get());
    }

    @Test
    public void testChainedLongCallback() {
        Supplier<Long> property1 = propertyRepo.getProperty("PropertyRepoTest.int1", "PropertyRepoTest.int2", -1L);
        Assert.assertEquals(75L, (long) property1.get());

        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.int2", 176L);
        Assert.assertEquals(75L, (long) property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.int1", 175L);
        Assert.assertEquals(175L, (long) property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<Long> property2 = propertyRepo.getProperty("PropertyRepoTest.longCallback1",
                "PropertyRepoTest.longCallback2", -1L);
        propertyRepo.onChange(property2, callback);
        Assert.assertEquals(-1L, (long) property2.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.longCallback2", 276L);
        Assert.assertEquals(276L, (long) property2.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.longCallback1", 275L);
        Assert.assertEquals(275L, (long) property2.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testChainedBoolean() {
        Assert.assertEquals(true, (boolean) propertyRepo
                .getProperty("PropertyRepoTest.bool1", "PropertyRepoTest.boolNotThere", false).get());
        Assert.assertEquals(false,
                (boolean) propertyRepo.getProperty("PropertyRepoTest.bool2", "PropertyRepoTest.bool1", false).get());
        Assert.assertEquals(true,
                (boolean) propertyRepo.getProperty("PropertyRepoTest.bool1", "PropertyRepoTest.bool2", false).get());
        Assert.assertEquals(true, (boolean) propertyRepo
                .getProperty("PropertyRepoTest.boolNotThere", "PropertyRepoTest.bool1", false).get());
        Assert.assertEquals(false, (boolean) propertyRepo
                .getProperty("PropertyRepoTest.boolNotThere1", "PropertyRepoTest.boolNotThere2", false).get());
        Assert.assertNull(propertyRepo
                .getProperty("PropertyRepoTest.boolNotThere1", "PropertyRepoTest.boolNotThere2", (Boolean) null).get());
    }

    @Test
    public void testChainedBooleanCallback() {
        Supplier<Boolean> property1 = propertyRepo.getProperty("PropertyRepoTest.bool1", "PropertyRepoTest.bool2",
                false);
        Assert.assertEquals(true, (boolean) property1.get());

        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.bool2", true);
        Assert.assertEquals(true, (boolean) property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.bool1", false);
        Assert.assertEquals(false, (boolean) property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<Boolean> property2 = propertyRepo.getProperty("PropertyRepoTest.boolCallback1",
                "PropertyRepoTest.boolCallback2", false);
        propertyRepo.onChange(property2, callback);
        Assert.assertEquals(false, (boolean) property2.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.boolCallback2", true);
        Assert.assertEquals(true, (boolean) property2.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.boolCallback1", false);
        Assert.assertEquals(false, (boolean) property2.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testChainedStringSet() {
        Set<String> ev1 = new HashSet<>(Arrays.asList("value1", "value2", "value3"));
        Set<String> ev2 = new HashSet<>(Arrays.asList("value4", "value5", "value6"));
        Set<String> ev3 = new HashSet<>(Arrays.asList("value7", "value8", "value9"));
        Assert.assertEquals(ev1,
                propertyRepo.getProperty("PropertyRepoTest.string2", "PropertyRepoTest.notThere", ev3).get());
        Assert.assertEquals(ev2,
                propertyRepo.getProperty("PropertyRepoTest.string3", "PropertyRepoTest.string2", ev3).get());
        Assert.assertEquals(ev1,
                propertyRepo.getProperty("PropertyRepoTest.string2", "PropertyRepoTest.string3", ev3).get());
        Assert.assertEquals(ev1,
                propertyRepo.getProperty("PropertyRepoTest.notThere", "PropertyRepoTest.string2", ev3).get());
        Assert.assertEquals(ev3,
                propertyRepo.getProperty("PropertyRepoTest.notThere1", "PropertyRepoTest.notThere2", ev3).get());
        Assert.assertNull(propertyRepo
                .getProperty("PropertyRepoTest.notThere1", "PropertyRepoTest.notThere2", (Set<String>) null).get());
    }

    @Test
    public void testChainedStringSetCallback() {
        Set<String> ev1 = new HashSet<>(Arrays.asList("value1", "value2", "value3"));
        Set<String> ev1a = new HashSet<>(Arrays.asList("value1a", "value2a", "value3a"));
        Set<String> ev2 = new HashSet<>(Arrays.asList("value4", "value5", "value6"));
        Set<String> ev3 = new HashSet<>(Arrays.asList("value7", "value8", "value9"));
        Supplier<Set<String>> property1 = propertyRepo.getProperty("PropertyRepoTest.string2",
                "PropertyRepoTest.string3", ev3);
        Assert.assertEquals(ev1, property1.get());

        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        propertyRepo.getProperty("PropertyRepoTest.string2", Collections.emptySet()).get();
        propertyRepo.getProperty("PropertyRepoTest.string3", Collections.emptySet()).get();
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.string3", "value4a,value5a,value6a");
        Assert.assertEquals(ev1, property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.string2", "value1a,value2a,value3a");
        Assert.assertEquals(ev1a, property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<Set<String>> property2 = propertyRepo.getProperty("PropertyRepoTest.stringSetCallback1",
                "PropertyRepoTest.stringSetCallback2", ev3);
        propertyRepo.onChange(property2, callback);
        Assert.assertEquals(ev3, property2.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.stringSetCallback2", "value4,value5,value6");
        Assert.assertEquals(ev2, property2.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.stringSetCallback1", "value1,value2,value3");
        Assert.assertEquals(ev1, property2.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testSupplierDefaultedString() {
        String ev1 = "value1";
        String ev2 = "value2";

        Assert.assertEquals(ev1, propertyRepo.getProperty("PropertyRepoTest.string1", () -> null, "default").get());
        Assert.assertEquals(ev1, propertyRepo.getProperty("PropertyRepoTest.string1", () -> ev2, "default").get());
        Assert.assertEquals(ev1,
                propertyRepo.getProperty("PropertyRepoTest.stringNotThere", () -> ev1, "default").get());
        Assert.assertEquals("default",
                propertyRepo.getProperty("PropertyRepoTest.stringNotThere1", () -> null, "default").get());
        Assert.assertNull(
                propertyRepo.getProperty("PropertyRepoTest.stringNotThere1", () -> null, (String) null).get());
    }

    @Test
    public void testSupplierDefaultedStringCallback() {
        Supplier<String> stringSupplier = propertyRepo.getProperty("PropertyRepoTest.string2", "default2");
        Supplier<String> string1 = propertyRepo.getProperty("PropertyRepoTest.string1", stringSupplier, "default1");
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(string1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.string2", "value2*");
        Assert.assertEquals("value1", string1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.string1", "value1*");
        Assert.assertEquals("value1*", string1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<String> stringSupplierWCallback = propertyRepo.getProperty("PropertyRepoTest.stringSupplierCallback2",
                "default2");
        Supplier<String> string2 = propertyRepo.getProperty("PropertyRepoTest.stringSupplierCallback1",
                stringSupplierWCallback, "default1");
        propertyRepo.onChange(string2, callback);
        Assert.assertEquals("default2", string2.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.stringSupplierCallback2", "value2*");
        Assert.assertEquals("value2*", string2.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.stringSupplierCallback1", "value1*");
        Assert.assertEquals("value1*", string2.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testSupplierDefaultedInteger() {
        int ev1 = 75;
        int ev2 = 76;

        Assert.assertEquals(ev1, (int) propertyRepo.getProperty("PropertyRepoTest.int1", () -> null, -1).get());
        Assert.assertEquals(ev2, (int) propertyRepo.getProperty("PropertyRepoTest.int2", () -> ev1, -1).get());
        Assert.assertEquals(ev1, (int) propertyRepo.getProperty("PropertyRepoTest.int1", () -> ev2, -1).get());
        Assert.assertEquals(ev1, (int) propertyRepo.getProperty("PropertyRepoTest.intNotThere", () -> ev1, -1).get());
        Assert.assertEquals(-1, (int) propertyRepo.getProperty("PropertyRepoTest.intNotThere1", () -> null, -1).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.intNotThere1", () -> null, (Integer) null).get());
    }

    @Test
    public void testSupplierDefaultedIntegerCallback() {
        Supplier<Integer> property2 = propertyRepo.getProperty("PropertyRepoTest.int2", (Integer) null);
        Supplier<Integer> property1 = propertyRepo.getProperty("PropertyRepoTest.int1", property2, -1);
        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.int2", 176);
        Assert.assertEquals(75, (int) property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.int1", 175);
        Assert.assertEquals(175, (int) property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<Integer> propertySD2 = propertyRepo.getProperty("PropertyRepoTest.intSDCallback2", (Integer) null);
        Supplier<Integer> propertySD1 = propertyRepo.getProperty("PropertyRepoTest.intSDCallback1", propertySD2, -1);
        propertyRepo.onChange(propertySD1, callback);
        Assert.assertEquals(-1, (int) propertySD1.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.intSDCallback2", 276);
        Assert.assertEquals(276, (int) propertySD1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.intSDCallback1", 275);
        Assert.assertEquals(275, (int) propertySD1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testSupplierDefaultedLong() {
        long ev1 = 75L;
        long ev2 = 76L;
        Assert.assertEquals(ev1, (long) propertyRepo.getProperty("PropertyRepoTest.int1", () -> null, -1L).get());
        Assert.assertEquals(ev2, (long) propertyRepo.getProperty("PropertyRepoTest.int2", () -> ev1, -1L).get());
        Assert.assertEquals(ev1, (long) propertyRepo.getProperty("PropertyRepoTest.int1", () -> ev2, -1L).get());
        Assert.assertEquals(ev1, (long) propertyRepo.getProperty("PropertyRepoTest.intNotThere", () -> ev1, -1L).get());
        Assert.assertEquals(-1,
                (long) propertyRepo.getProperty("PropertyRepoTest.intNotThere1", () -> null, -1L).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.intNotThere1", () -> null, (Integer) null).get());
    }

    @Test
    public void testSupplierDefaultedLongCallback() {
        Supplier<Long> property2 = propertyRepo.getProperty("PropertyRepoTest.int2", (Long) null);
        Supplier<Long> property1 = propertyRepo.getProperty("PropertyRepoTest.int1", property2, -1L);
        Assert.assertEquals(75L, (long) property1.get());

        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.int2", 176L);
        Assert.assertEquals(75L, (long) property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.int1", 175L);
        Assert.assertEquals(175L, (long) property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<Long> property2CB = propertyRepo.getProperty("PropertyRepoTest.longSDCallback2", (Long) null);
        Supplier<Long> property1CB = propertyRepo.getProperty("PropertyRepoTest.longSDCallback1", property2CB, -1L);
        propertyRepo.onChange(property1CB, callback);
        Assert.assertEquals(-1L, (long) property1CB.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.longSDCallback2", 276L);
        Assert.assertEquals(276L, (long) property1CB.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.longSDCallback1", 275L);
        Assert.assertEquals(275L, (long) property1CB.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testSupplierDefaultedBoolean() {
        boolean ev1 = true;
        boolean ev2 = false;
        Assert.assertEquals(ev1, (boolean) propertyRepo.getProperty("PropertyRepoTest.bool1", () -> null, false).get());
        Assert.assertEquals(ev2, (boolean) propertyRepo.getProperty("PropertyRepoTest.bool2", () -> ev1, false).get());
        Assert.assertEquals(ev1, (boolean) propertyRepo.getProperty("PropertyRepoTest.bool1", () -> ev2, false).get());
        Assert.assertEquals(ev1,
                (boolean) propertyRepo.getProperty("PropertyRepoTest.notThere", () -> ev1, false).get());
        Assert.assertEquals(false,
                (boolean) propertyRepo.getProperty("PropertyRepoTest.notThere", () -> null, false).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.notThere", () -> null, (Boolean) null).get());
    }

    @Test
    public void testSupplierDefaultedBooleanCallback() {
        Supplier<Boolean> property2 = propertyRepo.getProperty("PropertyRepoTest.bool2", (Boolean) null);
        Supplier<Boolean> property1 = propertyRepo.getProperty("PropertyRepoTest.bool1", property2, false);
        Assert.assertEquals(true, (boolean) property1.get());

        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.bool2", true);
        Assert.assertEquals(true, (boolean) property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.bool1", false);
        Assert.assertEquals(false, (boolean) property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();

        Supplier<Boolean> property2CB = propertyRepo.getProperty("PropertyRepoTest.boolDBCallback2", (Boolean) null);
        Supplier<Boolean> property1CB = propertyRepo.getProperty("PropertyRepoTest.boolDBCallback1", property2CB,
                false);
        propertyRepo.onChange(property1CB, callback);
        Assert.assertEquals(false, (boolean) property1CB.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.boolDBCallback2", true);
        Assert.assertEquals(true, (boolean) property1CB.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.boolDBCallback1", false);
        Assert.assertEquals(false, (boolean) property1CB.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

    @Test
    public void testSupplierDefaultedStringSet() {
        Set<String> ev1 = new HashSet<>(Arrays.asList("value1", "value2", "value3"));
        Set<String> ev2 = new HashSet<>(Arrays.asList("value4", "value5", "value6"));
        Set<String> ev3 = new HashSet<>(Arrays.asList("value7", "value8", "value9"));
        Assert.assertEquals(ev1, propertyRepo.getProperty("PropertyRepoTest.string2", () -> null, ev3).get());
        Assert.assertEquals(ev2, propertyRepo.getProperty("PropertyRepoTest.string3", () -> ev1, ev3).get());
        Assert.assertEquals(ev1, propertyRepo.getProperty("PropertyRepoTest.string2", () -> ev2, ev3).get());
        Assert.assertEquals(ev1, propertyRepo.getProperty("PropertyRepoTest.notThere", () -> ev1, ev3).get());
        Assert.assertEquals(ev3, propertyRepo.getProperty("PropertyRepoTest.notThere1", () -> null, ev3).get());
        Assert.assertNull(propertyRepo.getProperty("PropertyRepoTest.notThere1", () -> null, (Set<String>) null).get());
    }

    @Test
    public void testSupplierDefaultedStringSetCallback() {
        Set<String> ev1 = new HashSet<>(Arrays.asList("value1", "value2", "value3"));
        Set<String> ev1a = new HashSet<>(Arrays.asList("value1a", "value2a", "value3a"));
        Set<String> ev2 = new HashSet<>(Arrays.asList("value4", "value5", "value6"));
        Set<String> ev3 = new HashSet<>(Arrays.asList("value7", "value8", "value9"));

        Supplier<Set<String>> property2 = propertyRepo.getProperty("PropertyRepoTest.string3", (Set<String>) null);
        Supplier<Set<String>> property1 = propertyRepo.getProperty("PropertyRepoTest.string2", property2, ev3);
        Assert.assertEquals(ev1, property1.get());

        CountingCallback callback = new CountingCallback();
        propertyRepo.onChange(property1, callback);
        propertyRepo.getProperty("PropertyRepoTest.string2", Collections.emptySet()).get();
        propertyRepo.getProperty("PropertyRepoTest.string3", Collections.emptySet()).get();
        int startingInvocations;
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.string3", "value4a,value5a,value6a");
        Assert.assertEquals(ev1, property1.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.string2", "value1a,value2a,value3a");
        Assert.assertEquals(ev1a, property1.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());

        callback.reset();
        Supplier<Set<String>> property2CB = propertyRepo.getProperty("PropertyRepoTest.stringSetSDCallback2",
                (Set<String>) null);
        Supplier<Set<String>> property1CB = propertyRepo.getProperty("PropertyRepoTest.stringSetSDCallback1",
                property2CB, ev3);
        propertyRepo.onChange(property1CB, callback);
        Assert.assertEquals(ev3, property1CB.get());
        startingInvocations = callback.getInvocations();
        propertyMutator.accept("PropertyRepoTest.stringSetSDCallback2", "value4,value5,value6");
        Assert.assertEquals(ev2, property1CB.get());
        Assert.assertEquals(startingInvocations + 1, callback.getInvocations());
        propertyMutator.accept("PropertyRepoTest.stringSetSDCallback1", "value1,value2,value3");
        Assert.assertEquals(ev1, property1CB.get());
        Assert.assertEquals(startingInvocations + 2, callback.getInvocations());
    }

}
