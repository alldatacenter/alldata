/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The utilities for strings.
 */
public class StringUtils {

    private static final int STATE_NORMAL = 0;
    private static final int STATE_KEY = 2;
    private static final int STATE_VALUE = 4;
    private static final int STATE_ESCAPING = 8;
    private static final int STATE_QUOTING = 16;

    /**
     * Splits the kv text.
     *
     * <p>Both escaping and quoting is supported. When the escape character is
     * not '\0', then the next character to the escape character will be
     * escaped. When the quote character is not '\0', then all characters
     * between consecutive quote characters will be escaped.</p>
     *
     * @param text The text to be split.
     * @param entryDelimiter The delimiter of entries.
     * @param kvDelimiter The delimiter between key and value.
     * @param escapeChar The escaping character. Only valid if not '\0'.
     * @param quoteChar The quoting character.
     * @return The fields split from the text.
     */
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public static Map<String, String> splitKv(
            @Nonnull String text,
            @Nonnull Character entryDelimiter,
            @Nonnull Character kvDelimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar
    ) {
        Map<String, String> fields = new HashMap<>();

        StringBuilder stringBuilder = new StringBuilder();

        String key = "";
        String value;

        int state = STATE_KEY;

        /*
         * The state when entering escaping and quoting. When we exit escaping
         * or quoting, we should restore this state.
         */
        int kvState = STATE_KEY;

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);

            if (ch == kvDelimiter) {
                switch (state) {
                    case STATE_KEY:
                        key = stringBuilder.toString();
                        stringBuilder.setLength(0);
                        state = STATE_VALUE;
                        break;
                    case STATE_VALUE:
                        throw new IllegalArgumentException("Unexpected token " + ch + " at position " + i + ".");
                    case STATE_ESCAPING:
                        stringBuilder.append(ch);
                        state = kvState;
                        break;
                    case STATE_QUOTING:
                        stringBuilder.append(ch);
                        break;
                }
            } else if (ch == entryDelimiter) {
                switch (state) {
                    case STATE_KEY:
                        throw new IllegalArgumentException("Unexpected token " + ch + " at position " + i + ".");
                    case STATE_VALUE:
                        value = stringBuilder.toString();
                        fields.put(key, value);

                        stringBuilder.setLength(0);
                        state = STATE_KEY;
                        break;
                    case STATE_ESCAPING:
                        stringBuilder.append(ch);
                        state = kvState;
                        break;
                    case STATE_QUOTING:
                        stringBuilder.append(ch);
                        break;
                }
            } else if (escapeChar != null && ch == escapeChar) {
                switch (state) {
                    case STATE_KEY:
                    case STATE_VALUE:
                        kvState = state;
                        state = STATE_ESCAPING;
                        break;
                    case STATE_ESCAPING:
                        stringBuilder.append(ch);
                        state = kvState;
                        break;
                    case STATE_QUOTING:
                        stringBuilder.append(ch);
                        break;
                }
            } else if (quoteChar != null && ch == quoteChar) {
                switch (state) {
                    case STATE_KEY:
                    case STATE_VALUE:
                        kvState = state;
                        state = STATE_QUOTING;
                        break;
                    case STATE_ESCAPING:
                        stringBuilder.append(ch);
                        state = kvState;
                        break;
                    case STATE_QUOTING:
                        state = kvState;
                        break;
                }
            } else {
                stringBuilder.append(ch);
            }
        }

        switch (state) {
            case STATE_KEY:
                throw new IllegalArgumentException("Dangling key.");
            case STATE_VALUE:
                value = stringBuilder.toString();
                fields.put(key, value);
                return fields;
            case STATE_ESCAPING:
                throw new IllegalArgumentException("Not closed escaping.");
            case STATE_QUOTING:
                throw new IllegalArgumentException("Not closed quoting.");
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Concat the given fields' keys and values.
     *
     * <p>Special characters in the text will be escaped or quoted if
     * corresponding character is given. Otherwise, an exception will be
     * thrown.</p>
     *
     * @param fieldKeys The keys to be concat.
     * @param fieldValues The values to be concat.
     * @param entryDelimiter The delimiter of entries.
     * @param kvDelimiter The delimiter between key and value.
     * @param escapeChar The escape character.
     * @param quoteChar The quote character.
     * @return The concated text of given fields.
     */
    public static String concatKv(
            @Nonnull String[] fieldKeys,
            @Nonnull String[] fieldValues,
            @Nonnull Character entryDelimiter,
            @Nonnull Character kvDelimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar
    ) {
        if (fieldKeys.length != fieldValues.length) {
            throw new IllegalArgumentException("The keys' number " + fieldKeys.length
                   + " doesn't match values' number " + fieldValues.length);
        }

        Collection<Character> delimiters =
                Arrays.asList(entryDelimiter, kvDelimiter);

        StringBuilder stringBuilder = new StringBuilder();

        for (int index = 0; index < fieldKeys.length; ++index) {

            encodeText(
                    stringBuilder,
                    fieldKeys[index],
                    delimiters,
                    escapeChar,
                    quoteChar
            );

            stringBuilder.append(kvDelimiter);

            encodeText(
                    stringBuilder,
                    fieldValues[index],
                    delimiters,
                    escapeChar,
                    quoteChar
            );

            if (index < fieldKeys.length - 1) {
                stringBuilder.append(entryDelimiter);
            }
        }

        return stringBuilder.toString();
    }

    private static void encodeText(
            StringBuilder stringBuilder,
            String text,
            Collection<Character> delimiters,
            Character escapeChar,
            Character quoteChar
    ) {
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);

            if (delimiters.contains(ch)) {
                if (escapeChar != null) {
                    stringBuilder.append(escapeChar);
                    stringBuilder.append(ch);
                } else if (quoteChar != null) {
                    stringBuilder.append(quoteChar);
                    stringBuilder.append(ch);
                    stringBuilder.append(quoteChar);
                } else {
                    throw new IllegalArgumentException("There is a delimiter in the text, "
                           + "but neither escape nor quote character is specified.");
                }
            } else if (escapeChar != null && ch == escapeChar) {
                stringBuilder.append(escapeChar);
                stringBuilder.append(ch);
            } else if (quoteChar != null && ch == quoteChar) {
                if (escapeChar != null) {
                    stringBuilder.append(escapeChar);
                    stringBuilder.append(ch);
                } else {
                    throw new IllegalArgumentException("There is a quote character in the text, "
                           + "but escape character is not specified.");
                }
            } else {
                stringBuilder.append(ch);
            }
        }
    }

    /**
     * Splits the csv text.
     *
     * <p>Both escaping and quoting is supported. When the escape character is
     * not '\0', then the next character to the escape character will be
     * escaped. When the quote character is not '\0', then all characters
     * between consecutive quote characters will be escaped.</p>
     *
     * @param text The text to be split.
     * @param delimiter The delimiter of fields.
     * @param escapeChar The escaping character. Only valid if not '\0'.
     * @param quoteChar The quoting character.
     * @return The fields split from the text.
     */
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public static String[] splitCsv(
            @Nonnull String text,
            @Nonnull Character delimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar
    ) {
        List<String> fields = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();
        int state = STATE_NORMAL;

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);

            if (ch == delimiter) {
                switch (state) {
                    case STATE_NORMAL:
                        String field = stringBuilder.toString();
                        fields.add(field);
                        stringBuilder.setLength(0);
                        break;
                    case STATE_ESCAPING:
                        stringBuilder.append(ch);
                        state = STATE_NORMAL;
                        break;
                    case STATE_QUOTING:
                        stringBuilder.append(ch);
                        break;
                }
            } else if (escapeChar != null && ch == escapeChar) {
                switch (state) {
                    case STATE_NORMAL:
                        state = STATE_ESCAPING;
                        break;
                    case STATE_ESCAPING:
                        stringBuilder.append(ch);
                        state = STATE_NORMAL;
                        break;
                    case STATE_QUOTING:
                        stringBuilder.append(ch);
                        break;
                }
            } else if (quoteChar != null && ch == quoteChar) {
                switch (state) {
                    case STATE_NORMAL:
                        state = STATE_QUOTING;
                        break;
                    case STATE_ESCAPING:
                        stringBuilder.append(ch);
                        state = STATE_NORMAL;
                        break;
                    case STATE_QUOTING:
                        state = STATE_NORMAL;
                        break;
                }
            } else {
                stringBuilder.append(ch);
            }
        }

        switch (state) {
            case STATE_NORMAL:
                String field = stringBuilder.toString();
                fields.add(field);
                return fields.toArray(new String[0]);
            case STATE_ESCAPING:
                throw new IllegalArgumentException("Not closed escaping.");
            case STATE_QUOTING:
                throw new IllegalArgumentException("Not closed quoting.");
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Concat the given fields.
     *
     * <p>Special characters in the text will be escaped or quoted if
     * corresponding character is given. Otherwise, an exception will be
     * thrown.</p>
     *
     * @param fields The fields to be concat.
     * @param delimiter The delimiter of fields.
     * @param escapeChar The escape character.
     * @param quoteChar The quote character.
     * @return The concated text of given fields.
     */
    public static String concatCsv(
            @Nonnull String[] fields,
            @Nonnull Character delimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar
    ) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int index = 0; index < fields.length; ++index) {

            String field = fields[index];

            for (int i = 0; i < field.length(); ++i) {
                char ch = field.charAt(i);

                if (ch == delimiter
                            || (escapeChar != null && ch == escapeChar)
                            || (quoteChar != null && ch == quoteChar)) {

                    if (escapeChar != null) {
                        stringBuilder.append(escapeChar);
                        stringBuilder.append(ch);
                    } else if (quoteChar != null && ch != quoteChar) {
                        stringBuilder.append(quoteChar);
                        stringBuilder.append(ch);
                        stringBuilder.append(quoteChar);
                    } else {
                        throw new IllegalArgumentException("There exist special characters in the text, "
                               + "but neither escape character nor quote character is configured.");
                    }
                } else {
                    stringBuilder.append(ch);
                }
            }

            if (index < fields.length - 1) {
                stringBuilder.append(delimiter);
            }
        }

        return stringBuilder.toString();
    }

}
