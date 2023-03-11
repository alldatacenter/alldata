package com.hw.lineage.common.util;

import java.util.Base64;

/**
 * @description: Base64Utils
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class Base64Utils {
    private static final Base64.Decoder decoder = Base64.getDecoder();
    private static final Base64.Encoder encoder = Base64.getEncoder();

    private Base64Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static String encode(String text) {
        return encoder.encodeToString(text.getBytes());
    }

    public static String decode(String text) {
        return new String(decoder.decode(text));
    }
}
