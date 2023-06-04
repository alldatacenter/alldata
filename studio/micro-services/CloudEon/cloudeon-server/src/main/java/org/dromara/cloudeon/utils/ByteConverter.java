package org.dromara.cloudeon.utils;

public class ByteConverter {
    private static final long KB = 1024L;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long TB = GB * 1024;

    public static long convertKBToMB(long kb) {
        return (long) Math.ceil((double) kb / MB);
    }

    public static long convertKBToGB(long kb) {
        return (long) Math.ceil((double) kb / GB);
    }

    public static long convertKBToTB(long kb) {
        return (long) Math.ceil((double) kb / TB);
    }
}
