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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Currency;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import javax.inject.Singleton;
import javax.xml.bind.DatatypeConverter;

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.exceptions.ParseException;

/**
 * @author Spencer Gibb
 */
@Singleton
public class DefaultDecoder implements Decoder {
    private Map<Class<?>, Function<String, ?>> decoderRegistry;

    public static DefaultDecoder INSTANCE = new DefaultDecoder();
    
    {
        decoderRegistry = new IdentityHashMap<>(75);
        decoderRegistry.put(String.class, v->v);
        decoderRegistry.put(boolean.class, v->{
            if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("on")) {
                return Boolean.TRUE;
            }
            else if (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("no") || v.equalsIgnoreCase("off")) {
                return Boolean.FALSE;
            }
            throw new ParseException("Error parsing value '" + v, new Exception("Expected one of [true, yes, on, false, no, off]"));

        });
        decoderRegistry.put(Boolean.class, decoderRegistry.get(boolean.class));
        decoderRegistry.put(Integer.class, Integer::valueOf);
        decoderRegistry.put(int.class, Integer::valueOf);
        decoderRegistry.put(long.class, Long::valueOf);
        decoderRegistry.put(Long.class, Long::valueOf);
        decoderRegistry.put(short.class, Short::valueOf);
        decoderRegistry.put(Short.class, Short::valueOf);
        decoderRegistry.put(byte.class, Byte::valueOf);
        decoderRegistry.put(Byte.class, Byte::valueOf);
        decoderRegistry.put(double.class, Double::valueOf);
        decoderRegistry.put(Double.class, Double::valueOf);
        decoderRegistry.put(float.class, Float::valueOf);
        decoderRegistry.put(Float.class, Float::valueOf);
        decoderRegistry.put(BigInteger.class, BigInteger::new);
        decoderRegistry.put(BigDecimal.class, BigDecimal::new);
        decoderRegistry.put(AtomicInteger.class, s->new AtomicInteger(Integer.parseInt(s)));
        decoderRegistry.put(AtomicLong.class, s->new AtomicLong(Long.parseLong(s)));
        decoderRegistry.put(Duration.class, Duration::parse);
        decoderRegistry.put(Period.class, Period::parse);
        decoderRegistry.put(LocalDateTime.class, LocalDateTime::parse);
        decoderRegistry.put(LocalDate.class, LocalDate::parse);
        decoderRegistry.put(LocalTime.class, LocalTime::parse);
        decoderRegistry.put(OffsetDateTime.class, OffsetDateTime::parse);
        decoderRegistry.put(OffsetTime.class, OffsetTime::parse);
        decoderRegistry.put(ZonedDateTime.class, ZonedDateTime::parse);
        decoderRegistry.put(Instant.class, v->Instant.from(OffsetDateTime.parse(v)));
        decoderRegistry.put(Date.class, v->new Date(Long.parseLong(v)));
        decoderRegistry.put(Currency.class, Currency::getInstance);
        decoderRegistry.put(BitSet.class, v->BitSet.valueOf(DatatypeConverter.parseHexBinary(v)));
    }
    
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(Class<T> type, String encoded) {
        if (encoded == null) {
            return null;
        }
        if (decoderRegistry.containsKey(type)) {
            return (T)decoderRegistry.get(type).apply(encoded);
        }
        
        if (type.isArray()) {
            String[] elements = encoded.split(",");
            T[] ar = (T[]) Array.newInstance(type.getComponentType(), elements.length);
            for (int i = 0; i < elements.length; i++) {
                ar[i] = (T) decode(type.getComponentType(), elements[i]);
            }
            return (T) ar;
        }

        // Next look a valueOf(String) static method
        try {
            Method method;
            try {
                method = type.getMethod("valueOf", String.class);
                return (T) method.invoke(null, encoded);
            } catch (NoSuchMethodException e1) {
                // Next look for a T(String) constructor
                Constructor<T> c;
                try {
                    c = type.getConstructor(String.class);
                    return c.newInstance(encoded);
                }
                catch (NoSuchMethodException e) {
                    throw new RuntimeException(type.getCanonicalName() + " has no String constructor or valueOf static method");
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to instantiate value of type " + type.getCanonicalName(), e);
        }
    }
}
