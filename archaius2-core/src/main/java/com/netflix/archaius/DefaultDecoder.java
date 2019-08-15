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

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.converters.ArrayTypeConverterFactory;
import com.netflix.archaius.converters.DefaultCollectionsTypeConverterFactory;
import com.netflix.archaius.converters.DefaultTypeConverterFactory;
import com.netflix.archaius.converters.EnumTypeConverterFactory;
import com.netflix.archaius.exceptions.ParseException;

import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Singleton
public class DefaultDecoder implements Decoder, TypeConverter.Registry {
    private Map<Type, TypeConverter> cache = new ConcurrentHashMap<>();

    private final List<TypeConverter.Factory> factories = new ArrayList<>();

    public static DefaultDecoder INSTANCE = new DefaultDecoder();

    private DefaultDecoder() {
        factories.add(DefaultTypeConverterFactory.INSTANCE);
        factories.add(DefaultCollectionsTypeConverterFactory.INSTANCE);
        factories.add(ArrayTypeConverterFactory.INSTANCE);
        factories.add(EnumTypeConverterFactory.INSTANCE);
    }

    @Override
    public <T> T decode(Class<T> type, String encoded) {
        return decode((Type)type, encoded);
    }

    @Override
    public <T> T decode(Type type, String encoded) {
        try {
            if (encoded == null) {
                return null;
            }
            return (T)getOrCreateConverter(type).convert(encoded);
        } catch (Exception e) {
            throw new ParseException("Error decoding type `" + type.getTypeName() + "`", e);
        }
    }

    @Override
    public Optional<TypeConverter<?>> get(Type type) {
        return Optional.ofNullable(getOrCreateConverter(type));
    }

    private TypeConverter<?> getOrCreateConverter(Type type) {
        TypeConverter<?> converter = cache.get(type);
        if (converter == null) {
            converter = resolve(type);
            if (converter == null) {
                throw new RuntimeException("No converter found for type '" + type + "'");
            }
            cache.put(type, converter);
        }
        return converter;
    }

    /**
     * Iterate through all TypeConverter#Factory's and return the first TypeConverter that matches
     * @param type
     * @return
     */
    private TypeConverter<?> resolve(Type type) {
        return factories.stream()
                .flatMap(factory -> factory.get(type, this).map(Stream::of).orElseGet(Stream::empty))
                .findFirst()
                .orElseGet(() -> findValueOfTypeConverter((Class)type));
    }

    /**
     * @param type
     * @param <T>
     * @return Return a converter that uses reflection on either a static valueOf or ctor(String) to convert a string value to the
     *     type.  Will return null if neither is found
     */
    private static <T> TypeConverter<T> findValueOfTypeConverter(Type type) {
        if (!(type instanceof Class)) {
            return null;
        }

        Class cls = (Class)type;

        // Next look a valueOf(String) static method
        Method method;
        try {
            method = cls.getMethod("valueOf", String.class);
            return value -> {
                try {
                    return (T)method.invoke(null, value);
                } catch (Exception e) {
                    throw new ParseException("Error converting value '" + value + "' to '" + type.getTypeName() + "'", e);
                }
            };
        } catch (NoSuchMethodException e1) {
            // Next look for a T(String) constructor
            Constructor c;
            try {
                c = cls.getConstructor(String.class);
                return value -> {
                    try {
                        return (T)c.newInstance(value);
                    } catch (Exception e) {
                        throw new ParseException("Error converting value", e);
                    }
                };
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }
}
