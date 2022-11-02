package com.elasticsearch.cloud.monitor.metric.common.utils;

import com.google.common.base.Splitter;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagUtils {
    private static Splitter splitter=Splitter.on("=").limit(2).trimResults();
    /**
     * Parses an integer value as a long from the given character sequence.
     * <p/>
     * This is equivalent to {@link Long#parseLong(String)} except it's up to 100% faster on {@link
     * String} and always works in O(1) space even with {@link StringBuilder} buffers (where it's 2x
     * to 5x faster).
     *
     * @param s The character sequence containing the integer value to parse.
     * @return The value parsed.
     * @throws NumberFormatException if the value is malformed or overflows.
     */
    public static long parseLong(final CharSequence s) {
        final int n = s.length();  // Will NPE if necessary.
        if (n == 0) {
            throw new NumberFormatException("Empty string");
        }
        char c = s.charAt(0);  // Current character.
        int i = 1;  // index in `s'.
        if (c < '0' && (c == '+' || c == '-')) {  // Only 1 test in common case.
            if (n == 1) {
                throw new NumberFormatException("Just a sign, no value: " + s);
            } else if (n > 20) {  // "+9223372036854775807" or "-9223372036854775808"
                throw new NumberFormatException("Value too long: " + s);
            }
            c = s.charAt(1);
            i = 2;  // Skip over the sign.
        } else if (n > 19) {  // "9223372036854775807"
            throw new NumberFormatException("Value too long: " + s);
        }
        long v = 0;  // The result (negated to easily handle MIN_VALUE).
        do {
            if ('0' <= c && c <= '9') {
                v -= c - '0';
            } else {
                throw new NumberFormatException("Invalid character '" + c + "' in " + s);
            }
            if (i == n) {
                break;
            }
            v *= 10;
            c = s.charAt(i++);
        } while (true);
        if (v > 0) {
            throw new NumberFormatException("Overflow in " + s);
        } else if (s.charAt(0) == '-') {
            return v;  // Value is already negative, return unchanged.
        } else if (v == Long.MIN_VALUE) {
            throw new NumberFormatException("Overflow in " + s);
        } else {
            return -v;  // Positive value, need to fix the sign.
        }
    }

    /**
     * Parses a tag into a TreeMap.
     *
     * @param tags The TreeMap into which to store the tag.
     * @param tag  A String of the form "tag=value".
     * @throws IllegalArgumentException if the tag is malformed.
     * @throws IllegalArgumentException if the tag was already in tags with a different value.
     */
    public static void parse(final Map<String, String> tags, final String tag) {
        final List<String> kv = splitter.splitToList(tag);
        if (kv == null || kv.size() == 0 || StringUtils.isEmpty(kv.get(0))) {
            return;
        }
        String value = "null";
        if (kv.size() >= 2 && StringUtils.isNotEmpty(kv.get(1))) {
            value = kv.get(1);
        } else if (!tag.trim().endsWith("=")) {
            throw new IllegalArgumentException(String.format("invalid value, tag is %s", tag));
        }
        tags.put(kv.get(0), value);
    }

    /**
     * Get tag string from given TreeMap
     *
     * @param tags The TreeMap of tags, which is orderd
     * @return A String of the form "tag=value"
     */
    public static String getTag(final Map<String, String> tags) {
        return getTag(tags, ',');
    }

    public static String getTag(final Map<String, String> tags, char sep) {
        if (tags == null) {
            return null;
        }
        boolean firstKeyValue = true;
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (firstKeyValue) {
                firstKeyValue = false;
            } else {
                sb.append(sep);
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Ensures that a given string is a valid metric name or tag name/value.
     *
     * @param what A human readable description of what's being validated.
     * @param s    The string to validate.
     * @throws IllegalArgumentException if the string isn't valid.
     */
    public static void validateString(final String what, final String s) {
        if (s == null) {
            throw new IllegalArgumentException("Invalid " + what + ": null");
        } else if ("".equals(s)) {
            throw new IllegalArgumentException("Invalid " + what + ": empty string");
        }
        final int n = s.length();
        for (int i = 0; i < n; i++) {
            final char c = s.charAt(i);
            if (!(('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9') || c == '-' || c == '_'
                || c == '.' || Character.isLetter(c))) { // TODO removed || c == '/'
                throw new IllegalArgumentException("Invalid " + what + " (\"" + s + "\"): illegal character: " + c);
            }
        }
    }

    public static Map<String, String> getCommonTags(Collection<String> tagKeys, Map<String, String> tags) {
        Map<String, String> groupByTags = new HashMap<>(tagKeys.size());
        for (String key : tagKeys) {
            String tagValue = tags.get(key);
            if (tagValue != null) {
                groupByTags.put(key, tagValue);
            }
        }
        return groupByTags;
    }

    public static String getHostnameFromTags(Map<String, String> tags) {
        if(tags == null){
            return null;
        }
        String hostname = tags.get("Hostname");
        if (hostname != null) {
            return hostname;
        } else {
            return tags.get("host");
        }
    }

}
