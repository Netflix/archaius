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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyListener;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

public class PropertyTest {
    static class MyService {
        private Property<Integer> value;
        private Property<Integer> value2;
        AtomicInteger setValueCallsCounter;

        MyService(PropertyFactory config) {
            setValueCallsCounter = new AtomicInteger(0);
            value  = config.getProperty("foo").asInteger(1).addListener(new MethodInvoker<Integer>(this, "setValue"));
            value2 = config.getProperty("foo").asInteger(2);
        }

        // Called by the config listener.
        public void setValue(Integer value) {
            setValueCallsCounter.incrementAndGet();
        }
    }

    static class CustomType {

        static CustomType DEFAULT = new CustomType(1,1);
        static CustomType ONE_TWO = new CustomType(1,2);

        private int x;
        private int y;

        CustomType(int x, int y) {
            this.x = x;
            this.y = y;
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
        Assert.assertEquals(2, service.setValueCallsCounter.get());
    }

    @Test
    public void testAllTypes() {
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
        Assert.assertEquals(10.0, doubleProp.get().doubleValue(), 0.0001);
        Assert.assertEquals(10.0f, floatProp.get().floatValue(), 0.0001f);
        Assert.assertEquals(10, intProp.get().intValue());
        Assert.assertEquals(10L, longProp.get().longValue());
        Assert.assertEquals((short) 10, shortProp.get().shortValue());
        Assert.assertEquals("10", stringProp.get());
        Assert.assertEquals(CustomType.ONE_TWO, customTypeProp.get());
    }

    @Test
    public void testUpdateDynamicChild() throws ConfigException {
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
        final AtomicInteger current = new AtomicInteger();
        prop.addListener(new PropertyListener<Integer>() {
            @Override
            public void onChange(Integer value) {
                current.set(value);
            }

            @Override
            public void onParseError(Throwable error) {
            }
        });

        Assert.assertEquals(123, current.intValue());
        config.setProperty("foo", 1);
        Assert.assertEquals(1, current.intValue());
        config.setProperty("foo", 2);
        Assert.assertEquals(2, current.intValue());
        config.clearProperty("foo");
        Assert.assertEquals(123, current.intValue());
        config.setProperty("foo", "${goo}");
        Assert.assertEquals(456, current.intValue());
    }
}
