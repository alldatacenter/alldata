package com.qcloud.cos.utils;

import java.util.Collection;

public class CollectionUtils {

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }
}