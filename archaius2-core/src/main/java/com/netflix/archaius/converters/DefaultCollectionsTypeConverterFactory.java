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
import java.util.function.Function;
import java.util.function.Supplier;

public final class DefaultCollectionsTypeConverterFactory implements TypeConverter.Factory {
    public static final DefaultCollectionsTypeConverterFactory INSTANCE = new DefaultCollectionsTypeConverterFactory();
    private DefaultCollectionsTypeConverterFactory() {}

    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType().equals(Map.class)) {
                return Optional.of(createMapTypeConverter(
                        registry.get(parameterizedType.getActualTypeArguments()[0]).orElseThrow(() -> new ConverterNotFoundException("No converter found")),
                        registry.get(parameterizedType.getActualTypeArguments()[1]).orElseThrow(() -> new ConverterNotFoundException("No converter found")),
                        LinkedHashMap::new));
            } else if (parameterizedType.getRawType().equals(Set.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        LinkedHashSet::new,
                        Collections::emptySet,
                        Collections::unmodifiableSet));
            } else if (parameterizedType.getRawType().equals(SortedSet.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        TreeSet::new,
                        Collections::emptySortedSet,
                        Collections::unmodifiableSortedSet));
            } else if (parameterizedType.getRawType().equals(List.class) || parameterizedType.getRawType().equals(Collection.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        ArrayList::new,
                        Collections::emptyList,
                        Collections::unmodifiableList));
            } else if (parameterizedType.getRawType().equals(LinkedList.class)) {
                return Optional.of(createCollectionTypeConverter(
                        parameterizedType.getActualTypeArguments()[0],
                        registry,
                        LinkedList::new,
                        LinkedList::new,
                        Function.identity()));
            }
        }

        return Optional.empty();
    }

    private static <E, T extends Collection<E>> TypeConverter<T> createCollectionTypeConverter(final Type elementType,
                                                                                               final TypeConverter.Registry registry,
                                                                                               final Supplier<T> collectionFactory,
                                                                                               final Supplier<T> emptyCollectionFactory,
                                                                                               final Function<T, T> finisher) {
        @SuppressWarnings("unchecked")
        TypeConverter<E> elementConverter = (TypeConverter<E>) registry.get(elementType)
                .orElseThrow(() -> new ConverterNotFoundException("No converter found"));

        boolean ignoreEmpty = !String.class.equals(elementType);

        return value -> {
            if (value.isEmpty()) {
                return emptyCollectionFactory.get();
            }
            final T collection = collectionFactory.get();
            for (String item : value.split("\\s*,\\s*")) {
                if (!item.isEmpty() || !ignoreEmpty) {
                    collection.add(elementConverter.convert(item));
                }
            }
            return finisher.apply(collection);
        };
    }

    private static <K, V> TypeConverter<Map<K, V>> createMapTypeConverter(final TypeConverter<K> keyConverter,
                                                                          final TypeConverter<V> valueConverter,
                                                                          final Supplier<Map<K, V>> mapFactory) {
        return s -> {
            if (s.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<K, V> result = mapFactory.get();

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