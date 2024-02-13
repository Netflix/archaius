package com.netflix.archaius;

import com.netflix.archaius.api.TypeConverter;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;

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
        Assert.assertEquals("FOO", decoder.decode((Type) CharSequence.class, "foo"));
        Assert.assertEquals("FOO", decoder.decode((Type) String.class, "foo"));
        // default is overridden
        Assert.assertEquals(Long.valueOf(6), decoder.decode((Type) Long.class, "3"));
        // default converter is used
        Assert.assertEquals(Integer.valueOf(3), decoder.decode((Type) Integer.class, "3"));
    }
}
