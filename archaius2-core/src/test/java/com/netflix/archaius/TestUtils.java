package com.netflix.archaius;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class TestUtils {
    @SafeVarargs
    public static <T> Set<T> set(T ... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }

    public static <T> Set<T> set(Iterator<T> values) {
        Set<T> vals = new LinkedHashSet<>();
        values.forEachRemaining(vals::add);
        return Collections.unmodifiableSet(vals);
    }

    public static <T> Set<T> set(Iterable<T> values) {
        return values instanceof Collection<?> ? new HashSet<>((Collection<T>) values) : set(values.iterator());
    }

    public static <T> int size(Iterable<T> values) {
        if (values instanceof Collection<?>) {
            return ((Collection<?>) values).size();
        }
        int count = 0;
        for (T el : values) {
            count++;
        }
        return count;
    }
}
