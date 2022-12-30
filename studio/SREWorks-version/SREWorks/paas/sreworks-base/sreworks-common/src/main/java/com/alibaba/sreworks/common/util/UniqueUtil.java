package com.alibaba.sreworks.common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jinghua.yjh
 */
public class UniqueUtil {

    private static final AtomicLong ATOMIC_LONG = new AtomicLong(0L);

    public static String uniqueId() {
        return String.format("%s_%s", ClientUtil.localClient, ATOMIC_LONG.addAndGet(1));
    }

    public static boolean isEmpty(String string) {
        return string == null || "".equals(string) || "null".equals(string.toLowerCase());
    }

}
