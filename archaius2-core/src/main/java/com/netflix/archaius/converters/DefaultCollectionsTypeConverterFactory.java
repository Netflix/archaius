package com.netflix.archaius.converters;

import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.exceptions.ConverterNotFoundException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

public final class DefaultCollectionsTypeConverterFactory implements TypeConverter.Factory {
    public static final DefaultCollectionsTypeConverterFactory INSTANCE = new DefaultCollectionsTypeConverterFactory();
    private DefaultCollectionsTypeConverterFactory() {}

    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            if (parameterizedType.getRawType().equals(Map.class)) {
                return Optional.of(createMapTypeConverter(
                        registry.get(parameterizedType.getActualTypeArguments()[0]).orElseThrow(() -> new ConverterNotFoundException("No converter found")),
                        registry.get(parameterizedType.getActualTypeArguments()[1]).orElseThrow(() -> new ConverterNotFoundException("No converter found")),
                        LinkedHashMap::new));
            } else if (parameterizedType.getRawType().equals(Set.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        LinkedHashSet::new));
            } else if (parameterizedType.getRawType().equals(SortedSet.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        TreeSet::new));
            } else if (parameterizedType.getRawType().equals(List.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        ArrayList::new));
            } else if (parameterizedType.getRawType().equals(LinkedList.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        LinkedList::new));
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> TypeConverter<?> createCollectionTypeConverter(final Type type, TypeConverter.Registry registry, final Supplier<Collection<T>> collectionFactory) {
        TypeConverter elementConverter = registry.get(type).orElseThrow(() -> new ConverterNotFoundException("No converter found"));

        boolean ignoreEmpty = !String.class.equals(type);

        return value -> {
            final Collection collection = collectionFactory.get();
            if (!value.isEmpty()) {
                Arrays.asList(value.split("\\s*,\\s*")).forEach(v -> {
                    if (!v.isEmpty() || !ignoreEmpty) {
                        collection.add(elementConverter.convert(v));
                    }
                });
            }
            return collection;
        };
    }

    private TypeConverter<?> createMapTypeConverter(final TypeConverter<?> keyConverter, final TypeConverter<?> valueConverter, final Supplier<Map> mapFactory) {
        return s -> {
            Map result = mapFactory.get();
            Arrays
                    .stream(s.split("\\s*,\\s*"))
                    .filter(pair -> !pair.isEmpty())
                    .map(pair -> pair.split("\\s*=\\s*"))
                    .forEach(kv -> result.put(
                            keyConverter.convert(kv[0]),
                            valueConverter.convert(kv[1])));
            return Collections.unmodifiableMap(result);
        };
    }
}
