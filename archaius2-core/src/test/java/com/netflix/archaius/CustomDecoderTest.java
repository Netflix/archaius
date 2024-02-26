package com.netflix.archaius;

import com.netflix.archaius.api.TypeConverter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomDecoderTest {

    @Test
    public void testCustomTypeConverters() {
        TypeConverter<String> stringConverter = String::toUpperCase;
        TypeConverter<Long> longConverter = value -> Long.parseLong(value) * 2;
        TypeConverter.Factory factory = (type, registry) -> {
            if ((type instanceof Class<?> && ((Class<?>) type).isAssignableFrom(String.class))) {
                return Optional.of(stringConverter);
            } else if (type.equals(Long.class)) { // override default converter
                return Optional.of(longConverter);
            }
            return Optional.empty();
        };
        CustomDecoder decoder = CustomDecoder.create(Collections.singletonList(factory));
        assertEquals("FOO", decoder.decode((Type) CharSequence.class, "foo"));
        assertEquals("FOO", decoder.decode((Type) String.class, "foo"));
        // default is overridden
        assertEquals(Long.valueOf(6), decoder.decode((Type) Long.class, "3"));
        // default converter is used
        assertEquals(Integer.valueOf(3), decoder.decode((Type) Integer.class, "3"));
    }
}
