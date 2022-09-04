package com.elasticsearch.cloud.monitor.metric.common.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.gson.Gson;

/**
 * @author xiaoping
 * @date 2019/12/29
 */
public class Utils {
    public static MapJoiner joiner= Joiner.on(",").withKeyValueSeparator("=");
    public static MapSplitter splitter= Splitter.on(",").withKeyValueSeparator("=");

    public static Joiner valueJoiner= Joiner.on(",");
    public static Splitter valueSplitter= Splitter.on(",");
    public static Splitter valuesSplitter= Splitter.on(" ").omitEmptyStrings().trimResults();

    public static Gson gson=new Gson();

    /**
     * Optimized version of {@code String#split} that doesn't use regexps. This function works in
     * O(5n) where n is the length of the string to split.
     *
     * @param s The string to split.
     * @param c The separator to use to split the string.
     * @return A non-null, non-empty array.
     */
    public static String[] splitString(final String s, final char c) {
        final char[] chars = s.toCharArray();
        int numSubstrings = 1;
        for (final char x : chars) {
            if (x == c) {
                numSubstrings++;
            }
        }
        final String[] result = new String[numSubstrings];
        final int len = chars.length;
        int start = 0;  // starting index in chars of the current substring.
        int pos = 0;    // current index in chars.
        int i = 0;      // number of the current substring.
        for (; pos < len; pos++) {
            if (chars[pos] == c) {
                result[i++] = new String(chars, start, pos - start);
                start = pos + 1;
            }
        }
        result[i] = new String(chars, start, pos - start);
        return result;
    }
}
