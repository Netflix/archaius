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
package com.netflix.archaius;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.converters.ArrayTypeConverterFactory;
import com.netflix.archaius.converters.EnumTypeConverterFactory;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;


public class DefaultDecoderTest {
    @SuppressWarnings("unused") // accessed via reflection
    private static Collection<Long> collectionOfLong;
    @SuppressWarnings("unused") // accessed via reflection
    private static List<Integer> listOfInteger;
    @SuppressWarnings("unused") // accessed via reflection
    private static Set<Long> setOfLong;
    @SuppressWarnings("unused") // accessed via reflection
    private static Map<String, Integer> mapOfStringToInteger;

    private static final ParameterizedType collectionOfLongType;
    private static final ParameterizedType listOfIntegerType;
    private static final ParameterizedType setOfLongType;
    private static final ParameterizedType mapofStringToIntegerType;

    static {
        try {
            collectionOfLongType = (ParameterizedType) DefaultDecoderTest.class.getDeclaredField("collectionOfLong").getGenericType();
            listOfIntegerType = (ParameterizedType) DefaultDecoderTest.class.getDeclaredField("listOfInteger").getGenericType();
            setOfLongType = (ParameterizedType) DefaultDecoderTest.class.getDeclaredField("setOfLong").getGenericType();
            mapofStringToIntegerType = (ParameterizedType) DefaultDecoderTest.class.getDeclaredField("mapOfStringToInteger").getGenericType();
        } catch (NoSuchFieldException exc) {
            throw new AssertionError("listOfString field not found", exc);
        }
    }

    @Test
    public void testJavaNumbers() {
        DefaultDecoder decoder = DefaultDecoder.INSTANCE;
        
        boolean flag = decoder.decode(boolean.class, "true");
        Assert.assertTrue(flag);
        int int_value = decoder.decode(int.class, "123");
        Assert.assertEquals(123, int_value);

        Assert.assertEquals(Byte.valueOf(Byte.MAX_VALUE), decoder.decode(Byte.class, String.valueOf(Byte.MAX_VALUE)));
        Assert.assertEquals(Short.valueOf(Short.MAX_VALUE), decoder.decode(Short.class, String.valueOf(Short.MAX_VALUE)));
        Assert.assertEquals(Long.valueOf(Long.MAX_VALUE), decoder.decode(Long.class, String.valueOf(Long.MAX_VALUE)));
        Assert.assertEquals(Integer.valueOf(Integer.MAX_VALUE), decoder.decode(Integer.class, String.valueOf(Integer.MAX_VALUE)));
        Assert.assertEquals(Float.valueOf(Float.MAX_VALUE), decoder.decode(Float.class, String.valueOf(Float.MAX_VALUE)));
        Assert.assertEquals(Double.valueOf(Double.MAX_VALUE), decoder.decode(Double.class, String.valueOf(Double.MAX_VALUE)));
        Assert.assertEquals(BigInteger.valueOf(Long.MAX_VALUE), decoder.decode(BigInteger.class, String.valueOf(Long.MAX_VALUE)));
        Assert.assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), decoder.decode(BigDecimal.class, String.valueOf(Double.MAX_VALUE)));
        Assert.assertEquals(Integer.MAX_VALUE, decoder.decode(AtomicInteger.class, String.valueOf(Integer.MAX_VALUE)).get());
        Assert.assertEquals(Long.MAX_VALUE, decoder.decode(AtomicLong.class, String.valueOf(Long.MAX_VALUE)).get());
    }
    
    @Test
    public void testJavaDateTime() {
        DefaultDecoder decoder = DefaultDecoder.INSTANCE;
        
        Assert.assertEquals(Duration.parse("PT20M30S"), decoder.decode(Duration.class, "PT20M30S"));
        Assert.assertEquals(Period.of(1, 2, 25), decoder.decode(Period.class, "P1Y2M3W4D"));
        Assert.assertEquals(OffsetDateTime.parse("2016-08-03T10:15:30+07:00"), decoder.decode(OffsetDateTime.class, "2016-08-03T10:15:30+07:00"));
        Assert.assertEquals(OffsetTime.parse("10:15:30+18:00"), decoder.decode(OffsetTime.class, "10:15:30+18:00"));
        Assert.assertEquals(ZonedDateTime.parse("2016-08-03T10:15:30+01:00[Europe/Paris]"), decoder.decode(ZonedDateTime.class, "2016-08-03T10:15:30+01:00[Europe/Paris]"));
        Assert.assertEquals(LocalDateTime.parse("2016-08-03T10:15:30"), decoder.decode(LocalDateTime.class, "2016-08-03T10:15:30"));
        Assert.assertEquals(LocalDate.parse("2016-08-03"), decoder.decode(LocalDate.class, "2016-08-03"));
        Assert.assertEquals(LocalTime.parse("10:15:30"), decoder.decode(LocalTime.class, "10:15:30"));
        Assert.assertEquals(Instant.from(OffsetDateTime.parse("2016-08-03T10:15:30+07:00")), decoder.decode(Instant.class, "2016-08-03T10:15:30+07:00"));
        Date newDate = new Date();
        Assert.assertEquals(newDate, decoder.decode(Date.class, String.valueOf(newDate.getTime())));
    }
    
    @Test
    public void testJavaMiscellaneous() throws DecoderException {
        DefaultDecoder decoder = DefaultDecoder.INSTANCE;
        Assert.assertEquals(Currency.getInstance("USD"), decoder.decode(Currency.class, "USD"));
        Assert.assertEquals(BitSet.valueOf(Hex.decodeHex("DEADBEEF00DEADBEEF")), decoder.decode(BitSet.class, "DEADBEEF00DEADBEEF"));
        Assert.assertEquals("testString", decoder.decode(String.class, "testString"));
        Assert.assertEquals(URI.create("https://netflix.com"), decoder.decode(URI.class, "https://netflix.com"));
        Assert.assertEquals(Locale.ENGLISH, decoder.decode(Locale.class, "en"));
    }

    @Test
    public void testCollections() {
        Decoder decoder = DefaultDecoder.INSTANCE;
        Assert.assertEquals(Collections.emptyList(), decoder.decode(listOfIntegerType, ""));
        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), decoder.decode(listOfIntegerType, "1,2,3,4,5,6"));
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), decoder.decode(collectionOfLongType, "1,2,3,4,5,6"));
        Assert.assertEquals(Collections.singleton(2L), decoder.decode(setOfLongType, "2,2,2,2"));
        Assert.assertEquals(Collections.emptyMap(), decoder.decode(mapofStringToIntegerType, ""));
        Assert.assertEquals(Collections.singletonMap("key", 12345), decoder.decode(mapofStringToIntegerType, "key=12345"));
    }

    @Test
    public void testArrays() {
        DefaultDecoder decoder = DefaultDecoder.INSTANCE;
        Assert.assertArrayEquals(new String[] { "foo", "bar", "baz" }, decoder.decode(String[].class, "foo,bar,baz"));
        Assert.assertArrayEquals(new Integer[] {1, 2, 3, 4, 5}, decoder.decode(Integer[].class, "1,2,3,4,5"));
        Assert.assertArrayEquals(new int[] {1, 2, 3, 4, 5}, decoder.decode(int[].class, "1,2,3,4,5"));
        Assert.assertArrayEquals(new Integer[0], decoder.decode(Integer[].class, ""));
        Assert.assertArrayEquals(new int[0], decoder.decode(int[].class, ""));
        Assert.assertArrayEquals(new Long[] {1L, 2L, 3L, 4L, 5L}, decoder.decode(Long[].class, "1,2,3,4,5"));
        Assert.assertArrayEquals(new long[] {1L, 2L, 3L, 4L, 5L}, decoder.decode(long[].class, "1,2,3,4,5"));
        Assert.assertArrayEquals(new Long[0], decoder.decode(Long[].class, ""));
        Assert.assertArrayEquals(new long[0], decoder.decode(long[].class, ""));
    }

    enum TestEnumType { FOO, BAR, BAZ }
    @Test
    public void testEnum() {
        Decoder decoder = DefaultDecoder.INSTANCE;
        Assert.assertEquals(TestEnumType.FOO, decoder.decode((Type) TestEnumType.class, "FOO"));
    }

    @Test
    public void testArrayConverterIgnoresParameterizedType() {
        Optional<TypeConverter<?>> maybeConverter = ArrayTypeConverterFactory.INSTANCE.get(listOfIntegerType, DefaultDecoder.INSTANCE);
        Assert.assertFalse(maybeConverter.isPresent());
    }

    @Test
    public void testEnumConverterIgnoresParameterizedType() {
        Optional<TypeConverter<?>> maybeConverter = EnumTypeConverterFactory.INSTANCE.get(listOfIntegerType, DefaultDecoder.INSTANCE);
        Assert.assertFalse(maybeConverter.isPresent());
    }

    @Test
    public void testTypeConverterRegistry() {
        Assert.assertTrue(DefaultDecoder.INSTANCE.get(Instant.class).isPresent());

        class Foo {}
        Assert.assertFalse(DefaultDecoder.INSTANCE.get(Foo.class).isPresent());
    }
}
