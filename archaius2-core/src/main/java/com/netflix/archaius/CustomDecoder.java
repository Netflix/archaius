package com.netflix.archaius;

import com.netflix.archaius.api.TypeConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A configurable {@code Decoder} implementation which allows extension through custom {@code TypeConverter.Factory}
 * instances. The custom factories are searched first, and if no appropriate converter is found, the default
 * converters used by {@code DefaultDecoder} will be consulted.
 */
public class CustomDecoder extends AbstractRegistryDecoder {
    private CustomDecoder(Collection<? extends TypeConverter.Factory> typeConverterFactories) {
        super(typeConverterFactories);
    }

    /**
     * Create a new {@code CustomDecoder} with the supplied {@code TypeConverter.Factory} instances installed.
     * The default converter factories will still be registered, but will be installed AFTER any custom ones,
     * giving callers the opportunity to override the behavior of the default converters.
     *
     * @param customTypeConverterFactories the collection of converter factories to use for this decoder
     */
    public static CustomDecoder create(Collection<? extends TypeConverter.Factory> customTypeConverterFactories) {
        Objects.requireNonNull(customTypeConverterFactories, "customTypeConverterFactories == null");
        List<TypeConverter.Factory> factories = new ArrayList<>(customTypeConverterFactories);
        factories.addAll(DefaultDecoder.DEFAULT_FACTORIES);
        return new CustomDecoder(factories);
    }
}
