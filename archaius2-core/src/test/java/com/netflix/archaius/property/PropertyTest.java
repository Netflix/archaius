/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.property;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.Property.Subscription;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyListener;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.MapConfig;

@SuppressWarnings("deprecation")
public class PropertyTest {
    static class MyService {
        private final Property<Integer> value;
        private final Property<Integer> value2;
        
        AtomicInteger setValueCallsCounter;

        MyService(PropertyFactory config) {
            setValueCallsCounter = new AtomicInteger(0);
            value  = config.getProperty("foo").asInteger(1);
            value.addListener(new MethodInvoker<>(this, "setValue"));
            value2 = config.getProperty("foo").asInteger(2);
        }

        // Called by the config listener.
        @SuppressWarnings("unused")
        public void setValue(Integer value) {
            setValueCallsCounter.incrementAndGet();
        }
    }

    static class CustomType {

        static CustomType DEFAULT = new CustomType(1,1);
        static CustomType ONE_TWO = new CustomType(1,2);

        private final int x;
        private final int y;

        CustomType(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "CustomType [x=" + x + ", y=" + y + "]";
        }
    }

    @Test
    public void test() throws ConfigException {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        MyService service = new MyService(factory);

        Assert.assertEquals(1, (int)service.value.get());
        Assert.assertEquals(2, (int)service.value2.get());

        config.setProperty("foo", "123");

        Assert.assertEquals(123, (int)service.value.get());
        Assert.assertEquals(123, (int)service.value2.get());
        // setValue() is called once when we init to 1 and twice when we set foo to 123.
        Assert.assertEquals(1, service.setValueCallsCounter.get());
    }

    @Test
    public void testBasicTypes() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        config.setProperty("foo", "10");
        config.setProperty("shmoo", "true");
        config.setProperty("loo", CustomType.ONE_TWO);

        Property<BigDecimal> bigDecimalProp = factory.getProperty("foo").asType(BigDecimal.class,
                                                                                BigDecimal.ONE);
        Property<BigInteger> bigIntegerProp = factory.getProperty("foo").asType(BigInteger.class,
                                                                                BigInteger.ONE);
        Property<Boolean> booleanProp = factory.getProperty("shmoo").asType(Boolean.class, false);
        Property<Byte> byteProp = factory.getProperty("foo").asType(Byte.class, (byte) 0x1);
        Property<Double> doubleProp = factory.getProperty("foo").asType(Double.class, 1.0);
        Property<Float> floatProp = factory.getProperty("foo").asType(Float.class, 1.0f);
        Property<Integer> intProp = factory.getProperty("foo").asType(Integer.class, 1);
        Property<Long> longProp = factory.getProperty("foo").asType(Long.class, 1L);
        Property<Short> shortProp = factory.getProperty("foo").asType(Short.class, (short) 1);
        Property<String> stringProp = factory.getProperty("foo").asType(String.class, "1");
        Property<CustomType> customTypeProp = factory.getProperty("loo").asType(CustomType.class,
                                                                                CustomType.DEFAULT);
        Assert.assertEquals(BigDecimal.TEN, bigDecimalProp.get());
        Assert.assertEquals(BigInteger.TEN, bigIntegerProp.get());
        Assert.assertEquals(true, booleanProp.get());
        Assert.assertEquals(10, byteProp.get().byteValue());
        Assert.assertEquals(10.0, doubleProp.get(), 0.0001);
        Assert.assertEquals(10.0f, floatProp.get(), 0.0001f);
        Assert.assertEquals(10, intProp.get().intValue());
        Assert.assertEquals(10L, longProp.get().longValue());
        Assert.assertEquals((short) 10, shortProp.get().shortValue());
        Assert.assertEquals("10", stringProp.get());
        Assert.assertEquals(CustomType.ONE_TWO, customTypeProp.get());
    }

    @Test
    public void testCollectionTypes() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        config.setProperty("foo", "10,13,13,20");
        config.setProperty("shmoo", "1=PT15M,0=PT0S");

        // Test array decoding
        Property<Byte[]> byteArray = factory.get("foo", Byte[].class);
        Assert.assertEquals(new Byte[] {10, 13, 13, 20}, byteArray.get());

        // Tests list creation and parsing, decoding of list elements, proper handling if user gives us a primitive type
        Property<List<Integer>> intList = factory.getList("foo", int.class);
        Assert.assertEquals(Arrays.asList(10, 13, 13, 20), intList.get());

        // Tests set creation, parsing non-int elements
        Property<Set<Double>> doubleSet = factory.getSet("foo", Double.class);
        Assert.assertEquals(new HashSet<>(Arrays.asList(10.0, 13.0, 20.0)), doubleSet.get());

        // Test map creation and parsing, keys and values of less-common types
        Property<Map<Short, Duration>> mapProp = factory.getMap("shmoo", Short.class, Duration.class);
        Map<Short, Duration> expectedMap = new HashMap<>();
        expectedMap.put((short) 1, Duration.ofMinutes(15));
        expectedMap.put((short) 0, Duration.ZERO);
        Assert.assertEquals(expectedMap, mapProp.get());

        // Test proper handling of unset properties
        Property<Map<CustomType, CustomType>> emptyProperty = factory.getMap("fubar", CustomType.class, CustomType.class);
        Assert.assertNull(emptyProperty.get());

        config.setProperty("fubar", "");
        Property<List<String>> emptyListProperty = factory.getList("fubar", String.class);
        Assert.assertEquals(Collections.emptyList(), emptyListProperty.get());

        Property<Set<String>> emptySetProperty = factory.getSet("fubar", String.class);
        Assert.assertEquals(Collections.emptySet(), emptySetProperty.get());

        Property<Map<String, String>> emptyMapProperty = factory.getMap("fubar", String.class, String.class);
        Assert.assertEquals(Collections.emptyMap(), emptyMapProperty.get());
    }

    @Test
    public void testCollectionTypesImmutability() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        config.setProperty("foo", "10,13,13,20");
        config.setProperty("bar", "");
        config.setProperty("baz", "a=1,b=2");

        List<Integer> list = factory.getList("foo", Integer.class).get();
        Assert.assertThrows(UnsupportedOperationException.class, () -> list.add(100));

        Set<Integer> set = factory.getSet("foo", Integer.class).get();
        Assert.assertThrows(UnsupportedOperationException.class, () -> set.add(100));

        Map<String, Integer> map = factory.getMap("baz", String.class, Integer.class).get();
        Assert.assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));

        List<Integer> emptyList = factory.getList("bar", Integer.class).get();
        Assert.assertThrows(UnsupportedOperationException.class, () -> emptyList.add(100));

        Set<Integer> emptySet = factory.getSet("bar", Integer.class).get();
        Assert.assertThrows(UnsupportedOperationException.class, () -> emptySet.add(100));

        Map<String, Integer> emptyMap = factory.getMap("bar", String.class, Integer.class).get();
        Assert.assertThrows(UnsupportedOperationException.class, () -> emptyMap.put("c", 3));
    }

    @Test
    public void testUpdateDynamicChild() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> intProp1 = factory.getProperty("foo").asInteger(1);
        Property<Integer> intProp2 = factory.getProperty("foo").asInteger(2);
        Property<String>  strProp  = factory.getProperty("foo").asString("3");

        Assert.assertEquals(1, (int)intProp1.get());
        Assert.assertEquals(2, (int)intProp2.get());

        config.setProperty("foo", "123");

        Assert.assertEquals("123", strProp.get());
        Assert.assertEquals((Integer)123, intProp1.get());
        Assert.assertEquals((Integer)123, intProp2.get());
    }

    @Test
    public void testDefaultNull() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory.getProperty("foo").asInteger(null);
        Assert.assertNull(prop.get());
    }

    @Test
    public void testDefault() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory.getProperty("foo").asInteger(123);
        Assert.assertEquals(123, prop.get().intValue());
    }

    @Test
    public void testUpdateValue() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("goo", "456");

        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory.getProperty("foo").asInteger(123);
        config.setProperty("foo", 1);

        Assert.assertEquals(1, prop.get().intValue());

        config.clearProperty("foo");
        Assert.assertEquals(123, prop.get().intValue());

        config.setProperty("foo", "${goo}");
        Assert.assertEquals(456, prop.get().intValue());

    }

    @Test
    public void testUpdateCallback() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("goo", "456");

        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory.getProperty("foo").asInteger(123);
        final AtomicReference<Integer> current = new AtomicReference<>();
        prop.addListener(new PropertyListener<Integer>() {
            @Override
            public void onChange(Integer value) {
                current.set(value);
            }

            @Override
            public void onParseError(Throwable error) {
            }
        });
        current.set(prop.get());

        Assert.assertEquals(123, current.get().intValue());
        config.setProperty("foo", 1);
        Assert.assertEquals(1, current.get().intValue());
        config.setProperty("foo", 2);
        Assert.assertEquals(2, current.get().intValue());
        config.clearProperty("foo");
        Assert.assertEquals(123, current.get().intValue());
        config.setProperty("foo", "${goo}");
        Assert.assertEquals(456, current.get().intValue());
    }

    @Test
    public void unregisterOldCallback() {
        SettableConfig config = new DefaultSettableConfig();

        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        //noinspection unchecked
        PropertyListener<Integer> listener = Mockito.mock(PropertyListener.class);
        
        Property<Integer> prop = factory.getProperty("foo").asInteger(1);
        prop.addListener(listener);

        Mockito.verify(listener, Mockito.never()).accept(Mockito.anyInt());
        config.setProperty("foo", "2");
        Mockito.verify(listener, Mockito.times(1)).accept(Mockito.anyInt());

        prop.removeListener(listener);
        config.setProperty("foo", "3");
        
        Mockito.verify(listener, Mockito.times(1)).accept(Mockito.anyInt());
    }
    
    @Test
    public void subscribePropertyChange() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<Integer> prop = factory.get("foo", String.class)
                .map(Integer::parseInt)
                .orElse(2)
                ;
        
        AtomicInteger value = new AtomicInteger();
        prop.subscribe(value::set);
        
        Assert.assertEquals(2, prop.get().intValue());
        Assert.assertEquals(0, value.get());
        config.setProperty("foo", "1");
        Assert.assertEquals(1, prop.get().intValue());
        Assert.assertEquals(1, value.get());
    }
    
    @Test
    public void unsubscribeOnChange() {
        SettableConfig config = new DefaultSettableConfig();

        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        //noinspection unchecked
        Consumer<Integer> consumer = Mockito.mock(Consumer.class);
        
        Property<Integer> prop = factory.getProperty("foo").asInteger(1);
        Subscription sub = prop.onChange(consumer);

        Mockito.verify(consumer, Mockito.never()).accept(Mockito.anyInt());
        config.setProperty("foo", "2");
        Mockito.verify(consumer, Mockito.times(1)).accept(Mockito.anyInt());

        sub.unsubscribe();
        
        config.setProperty("foo", "3");
        
        Mockito.verify(consumer, Mockito.times(1)).accept(Mockito.anyInt());        
    }
    
    @Test
    public void chainedPropertyNoneSet() {
        MapConfig config = MapConfig.builder().build();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory
                .get("first", Integer.class)
                .orElseGet("second");
        
        Assert.assertNull(prop.get());
    }
    
    @Test
    public void chainedPropertyDefault() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory
                .get("first", Integer.class)
                .orElseGet("second")
                .orElse(3);
        
        Assert.assertEquals(3, prop.get().intValue());
    }
    
    @Test
    public void chainedPropertySecondSet() {
        MapConfig config = MapConfig.builder().put("second", 2).build();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory
                .get("first", Integer.class)
                .orElseGet("second")
                .orElse(3);
        
        Assert.assertEquals(2, prop.get().intValue());
    }
    
    @Test
    public void chainedPropertyFirstSet() {
        MapConfig config = MapConfig.builder().put("first", 1).build();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        Property<Integer> prop = factory
                .get("first", Integer.class)
                .orElseGet("second")
                .orElse(3);
        
        Assert.assertEquals(1, prop.get().intValue());
    }
    
    @Test
    public void chainedPropertyNotification() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("first", 1);
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        //noinspection unchecked
        Consumer<Integer> consumer = Mockito.mock(Consumer.class);
        
        Property<Integer> prop = factory
                .get("first", Integer.class)
                .orElseGet("second")
                .orElse(3);
        
        prop.onChange(consumer);
        
        // Should not be called on register
        Mockito.verify(consumer, Mockito.never()).accept(Mockito.any());
        
        // First changed
        config.setProperty("first", 11);
        Mockito.verify(consumer, Mockito.times(1)).accept(11);

        // Unrelated change ignored
        config.setProperty("foo", 11);
        Mockito.verify(consumer, Mockito.times(1)).accept(11);

        // Second changed has no effect because first is set
        config.setProperty("second", 2);
        Mockito.verify(consumer, Mockito.times(1)).accept(11);

        // First cleared, second becomes value
        config.clearProperty("first");
        Mockito.verify(consumer, Mockito.times(1)).accept(2);
        
        // First cleared, default becomes value
        config.clearProperty("second");
        Mockito.verify(consumer, Mockito.times(1)).accept(3);
    }
    
    @Test
    public void testCache() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("foo", "1");
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        // This can't be a lambda because then mockito can't subclass it to spy on it :-P
        //noinspection Convert2Lambda,Anonymous2MethodRef
        Function<String, Integer> mapper = Mockito.spy(new Function<String, Integer>() {
            @Override
            public Integer apply(String t) {
                return Integer.parseInt(t);
            }
        });
        
        Property<Integer> prop = factory.get("foo", String.class)
                .map(mapper);
        
        Mockito.verify(mapper, Mockito.never()).apply(Mockito.anyString());
        
        Assert.assertEquals(1, prop.get().intValue());
        Mockito.verify(mapper, Mockito.times(1)).apply("1");
        
        Assert.assertEquals(1, prop.get().intValue());
        Mockito.verify(mapper, Mockito.times(1)).apply("1");

        config.setProperty("foo", "2");
        
        Assert.assertEquals(2, prop.get().intValue());
        Mockito.verify(mapper, Mockito.times(1)).apply("1");
        Mockito.verify(mapper, Mockito.times(1)).apply("2");
        
        config.setProperty("bar", "3");
        Assert.assertEquals(2, prop.get().intValue());
        Mockito.verify(mapper, Mockito.times(1)).apply("1");
        Mockito.verify(mapper, Mockito.times(2)).apply("2");
    }
    
    @Test(expected=IllegalStateException.class)
    public void mapDiscardsType() {
        MapConfig config = MapConfig.builder().build();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);

        //noinspection unused
        Property<Integer> prop = factory
                .get("first", String.class)
                .orElseGet("second")
                .map(Integer::parseInt)
                .orElseGet("third")
                ;
    }
    
    @Test
    public void customMappingWithDefault() {
        Config config = MapConfig.builder().build();
        PropertyFactory factory = DefaultPropertyFactory.from(config);

        Integer value = factory.getProperty("a").asType(Integer::parseInt, "1").get();
        Assert.assertEquals(1, value.intValue());
    }
    
    @Test
    public void customMapping() {
        Config config = MapConfig.builder()
                .put("a", "2")
                .build();
        PropertyFactory factory = DefaultPropertyFactory.from(config);

        Integer value = factory.getProperty("a").asType(Integer::parseInt, "1").get();
        Assert.assertEquals(2, value.intValue());
    }

    @Test
    public void customMappingWithError() {
        Config config = MapConfig.builder()
                .put("a", "###bad_integer_value###")
                .build();
        PropertyFactory factory = DefaultPropertyFactory.from(config);

        Integer value = factory.getProperty("a").asType(Integer::parseInt, "1").get();
        Assert.assertEquals(1, value.intValue());
    }
}
