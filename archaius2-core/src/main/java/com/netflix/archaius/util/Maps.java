package com.netflix.archaius.util;

import java.util.HashMap;
import java.util.LinkedHashMap;

public final class Maps {
    private Maps() {}

    /**
     * Calculate initial capacity from expected size and default load factor (0.75).
     */
    private static int calculateCapacity(int numMappings) {
        return (int) Math.ceil(numMappings / 0.75d);
    }

    /**
     * Creates a new, empty HashMap suitable for the expected number of mappings.
     * The returned map is large enough so that the expected number of mappings can be
     * added without resizing the map.
     *
     * This is essentially a backport of HashMap.newHashMap which was added in JDK19.
     */
    public static <K, V> HashMap<K, V> newHashMap(int numMappings) {
        return new HashMap<>(calculateCapacity(numMappings));
    }

    /**
     * Creates a new, empty LinkedHashMap suitable for the expected number of mappings.
     * The returned map is large enough so that the expected number of mappings can be
     * added without resizing the map.
     *
     * This is essentially a backport of LinkedHashMap.newLinkedHashMap which was added in JDK19.
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int numMappings) {
        return new LinkedHashMap<>(calculateCapacity(numMappings));
    }
}
