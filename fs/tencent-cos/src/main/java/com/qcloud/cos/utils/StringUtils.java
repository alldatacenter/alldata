/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

/**
 * Utilities for converting objects to strings.
 */
public class StringUtils {

    private static final String DEFAULT_ENCODING = "UTF-8";

    public static final String COMMA_SEPARATOR = ",";

    public static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);

    public static Integer toInteger(StringBuilder value) {
        return Integer.parseInt(value.toString());
    }

    public static String toString(StringBuilder value) {
        return value.toString();
    }

    public static Boolean toBoolean(StringBuilder value) {
        return Boolean.getBoolean(value.toString());
    }

    public static String fromInteger(Integer value) {
        return Integer.toString(value);
    }

    public static String fromLong(Long value) {
        return Long.toString(value);
    }

    public static String fromString(String value) {
        return value;
    }

    public static String fromBoolean(Boolean value) {
        return Boolean.toString(value);
    }

    public static String fromBigInteger(BigInteger value) {
        return value.toString();
    }

    public static String fromBigDecimal(BigDecimal value) {
        return value.toString();
    }


    public static BigInteger toBigInteger(String s) {
        return new BigInteger(s);
    }

    public static BigDecimal toBigDecimal(String s) {
        return new BigDecimal(s);
    }

    public static String fromFloat(Float value) {
        return Float.toString(value);
    }

    /**
     * Converts the specified date to an ISO 8601 timestamp string and returns
     * it.
     *
     * @param value
     *            The date to format as an ISO 8601 timestamp string.
     *
     * @return An ISO 8601 timestamp string created from the specified date.
     */
    public static String fromDate(Date value) {
        return DateUtils.formatISO8601Date(value);
    }

    /**
     * Returns the string representation of the specified double.
     *
     * @param d
     *            The double to represent as a string.
     *
     * @return The string representation of the specified double.
     */
    public static String fromDouble(Double d) {
        return Double.toString(d);
    }

    /**
     * Returns the string representation of the specified Byte.
     *
     * @param b
     *            The Byte to represent as a string.
     *
     * @return The string representation of the specified Byte.
     */
    public static String fromByte(Byte b) {
        return Byte.toString(b);
    }

    public static String replace( String originalString, String partToMatch, String replacement ) {
        StringBuilder buffer = new StringBuilder( originalString.length() );
        buffer.append( originalString );

        int indexOf = buffer.indexOf( partToMatch );
        while (indexOf != -1) {
            buffer = buffer.replace(indexOf, indexOf + partToMatch.length(), replacement);
            indexOf = buffer.indexOf(partToMatch, indexOf + replacement.length());
        }

        return buffer.toString();
    }

    /**
     * Joins the strings in parts with joiner between each string
     * @param joiner the string to insert between the strings in parts
     * @param parts the parts to join
     */
    public static String join(String joiner, String... parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            builder.append(parts[i]);
            if (i < parts.length - 1) {
                builder.append(joiner);
            }
        }
        return builder.toString();
    }

    /**
     * A null-safe trim method. If the input string is null, returns null;
     * otherwise returns a trimmed version of the input.
     */
    public static String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    /**
     * @return true if the given value is either null or the empty string
     */
    public static boolean isNullOrEmpty(String value) {
        if (value == null) {
            return true;
        }
        return value.isEmpty();
    }

    /**
     * Removes any surrounding quotes from the specified string and returns a
     * new string.
     *
     * @param s
     *            The string to check for surrounding quotes.
     *
     * @return A new string created from the specified string, minus any
     *         surrounding quotes.
     */
    public static String removeQuotes(String s) {
        if (s == null) return null;

        s = s.trim();
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);

        return s;
    }

    /**
     * Returns a new string created by joining each of the strings in the
     * specified list together, with a comma between them.
     *
     * @param strings
     *            The list of strings to join into a single, comma delimited
     *            string list.
     * @return A new string created by joining each of the strings in the
     *         specified list together, with a comma between strings.
     */
    public static String join(List<String> strings) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (String s : strings) {
            if (!first) result.append(", ");

            result.append(s);
            first = false;
        }

        return result.toString();
    }

    /**
     * Performs a case insensitive comparison and returns true if the data
     * begins with the given sequence.
     */
    public static boolean beginsWithIgnoreCase(final String data, final String seq) {
        return data.regionMatches(true, 0, seq, 0, seq.length());
    }
}
