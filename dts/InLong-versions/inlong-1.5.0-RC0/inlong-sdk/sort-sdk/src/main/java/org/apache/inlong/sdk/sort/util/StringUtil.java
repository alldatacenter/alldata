/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.sort.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StringUtil {

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
            @Nullable Character quoteChar) {
        Map<String, String> fields = new HashMap<>();

        StringBuilder stringBuilder = new StringBuilder();

        String key = "";
        String value;

        int state = STATE_KEY;

        /*
         * The state when entering escaping and quoting. When we exit escaping or quoting, we should restore this state.
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
     * formatDate
     *
     * @param dateStr String
     * @param inputFormat String
     * @param format String
     * @return String
     */
    public static String formatDate(String dateStr, String inputFormat, String format) {
        String resultStr = dateStr;

        try {
            Date date = (new SimpleDateFormat(inputFormat)).parse(dateStr);
            resultStr = formatDate(date, format);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return resultStr;
    }

    /**
     * formatDate
     *
     * @param dateStr String
     * @param format String
     * @return String
     */
    public static String formatDate(String dateStr, String format) {
        String inputFormat = "yyyy-MM-dd HH:mm:ss";
        if (dateStr == null) {
            return "";
        } else {
            if (dateStr.matches("\\d{1,4}\\-\\d{1,2}\\-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}")) {
                inputFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd HH:mm:ss";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd HH:mm";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd HH";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd";
            } else if (dateStr.matches("\\d{1,4}/\\d{1,2}/\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}")) {
                inputFormat = "yyyy/MM/dd HH:mm:ss.SSS";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd HH:mm:ss";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd HH:mm";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd HH";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd";
            }

            String resultStr = formatDate(dateStr, inputFormat, format);
            return resultStr;
        }
    }

    /**
     * formatDate
     *
     * @param date Data
     * @param format String
     * @return String
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    /**
     * use default time foramt
     *
     * @param date {@link Date}
     * @return String
     */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * Parse date time form string format to unix format.
     * Only support <b>yyyyMMdd</b>, <b>yyyyMMddHH</b> and <b>yyyyMMddHHmm</b> precision
     * whose length is 8, 10 and 12 respectively.
     *
     * @param value Date time in string format.
     * @return Unix date time.
     */
    public static long parseDateTime(String value) {
        try {
            if (value.length() < 8) {
                return -1;
            } else if (value.length() <= 9) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
                Date date = simpleDateFormat.parse(value.substring(0, 8));
                return new Timestamp(date.getTime()).getTime();
            } else if (value.length() <= 11) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHH");
                Date date = simpleDateFormat.parse(value.substring(0, 10));
                return new Timestamp(date.getTime()).getTime();
            } else {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
                Date date = simpleDateFormat.parse(value.substring(0, 12));
                return new Timestamp(date.getTime()).getTime();
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unexpected time format : " + value);
        }
    }

}
