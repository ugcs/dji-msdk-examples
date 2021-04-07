package com.example.ugcssample.model.utils;

import java.util.Collections;
import java.util.HashSet;

public final class CollectionsUtils {

    private CollectionsUtils() {
    }

    public static <K, V> BidiMap<K, V> newBidiMap(K[] keys, V[] values) {
        return BidiMap.Builder.<K, V>start().setKeyValues(keys, values).build();
    }

    public static <E> HashSet<E> newHashSet(E... elements) {
        HashSet<E> set = new HashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }
}
