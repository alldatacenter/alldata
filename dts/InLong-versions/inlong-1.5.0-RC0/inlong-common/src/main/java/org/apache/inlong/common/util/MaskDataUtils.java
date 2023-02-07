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

package org.apache.inlong.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * MaskDataUtils is used to mask sensitive message in the raw data.
 */
public class MaskDataUtils {

    private static final List<String> KEYWORDS = Arrays.asList(
            "password", "pwd", "pass",
            "token", "secret_token", "secretToken",
            "secret_id", "secretId",
            "secret_key", "secretKey",
            "public_key", "publicKey");
    private static final List<String> SEPARATORS = Arrays.asList(":", "=", "\": \"", "\":\"");
    private static final List<Character> STOP_CHARACTERS = Arrays.asList('\'', '"');
    private static final List<Character> KNOWN_DELIMITERS =
            Collections.unmodifiableList(Arrays.asList('\'', '"', '<', '>'));

    /**
     * mask sensitive message in the raw data
     *
     * @param stringBuilder raw data
     */
    public static void mask(StringBuilder stringBuilder) {
        boolean maskedThisCharacter;
        int pos;
        int newPos;
        int length = stringBuilder.length();
        for (pos = 0; pos < length; pos++) {
            maskedThisCharacter = false;
            newPos = maskData(stringBuilder, '*', pos, length);
            maskedThisCharacter = newPos != pos;
            if (maskedThisCharacter) {
                length = stringBuilder.length();
                maskedThisCharacter = false;
            }
            if (!maskedThisCharacter) {
                while (pos < length
                        && !(Character.isWhitespace(stringBuilder.charAt(pos))
                                || STOP_CHARACTERS.contains(stringBuilder.charAt(pos)))) {
                    pos++;
                }
            }
        }
    }

    /**
     * replace sensitive message with six specified maskChar
     *
     * @param builder raw data
     * @param maskChar specified character for replace sensitive data
     * @param startPos the start position of sensitive data
     * @param endPos the end position of sensitive data
     * @return the new end position of replaced data
     */
    private static int mask(StringBuilder builder, char maskChar, int startPos, int endPos) {
        final String masked = "" + maskChar + maskChar + maskChar + maskChar + maskChar + maskChar;
        builder.replace(startPos, endPos, masked);
        return startPos + 6;
    }

    /**
     * mask data from specified start position of raw data
     *
     * @param builder raw data
     * @param maskChar specified character for replace sensitive data
     * @param startPos the start position of raw data
     * @param buffLength the length of raw data
     * @return the start position of first masked data
     */
    public static int maskData(StringBuilder builder, char maskChar, int startPos, int buffLength) {
        int charPos = startPos;
        if (charPos + 5 > buffLength) {
            return startPos;
        }

        Character character = builder.charAt(charPos);
        if (isKeyWorkdStart(character)) {
            int keywordStart = 0;
            int keywordLength = 0;
            String keywordUsed = null;
            for (String keyword : KEYWORDS) {
                keywordStart = StringUtils.indexOfIgnoreCase(builder, keyword, charPos);
                if (keywordStartAtRightPosition(keywordStart, charPos)) {
                    keywordLength = keyword.length();
                    keywordUsed = keyword;
                    break;
                }
            }

            if (keywordStart != startPos && keywordStart != startPos + 1) {
                return startPos;
            }

            int idxSeparator;
            for (String separator : SEPARATORS) {
                idxSeparator = StringUtils.indexOf(builder, separator, keywordStart + keywordLength);
                if (idxSeparator == keywordStart + keywordLength) {
                    charPos = maskStartPosition(keywordStart, keywordLength, separator, builder);

                    int endPos = detectEnd(builder, buffLength, charPos, keywordUsed, keywordLength, separator);

                    if (endPos > charPos) {
                        return mask(builder, maskChar, charPos, endPos);
                    }
                }
            }
        }

        return startPos;
    }

    /**
     * detect the end position of sensitive data
     *
     * @param builder raw data
     * @param buffLength the length of raw data
     * @param startPos the start position of sensitive data
     * @param keyword the keyword of sensitive data
     * @param keywordLength the length of keyword
     * @param separator the specified separator char
     * @return the end position of sensitive data
     */
    private static int detectEnd(StringBuilder builder, int buffLength, int startPos, String keyword,
            int keywordLength, String separator) {
        if (separator.charAt(0) == '>') {
            return detectEndXml(builder, buffLength, startPos, keyword, keywordLength);
        } else if (separator.contains("\"")) {
            return detectEndJson(builder, buffLength, startPos);
        } else {
            return detectEndNoXml(builder, buffLength, startPos);
        }
    }

    /**
     * detect end position of sensitive data in unknown format content
     *
     * @param builder raw data
     * @param buffLength the length of raw data
     * @param startPos the start position of sensitive data
     * @return the end position of sensitive data
     */
    private static int detectEndNoXml(StringBuilder builder, int buffLength, int startPos) {
        while (startPos < buffLength && !isDelimiter(builder.charAt(startPos))) {
            startPos++;
        }

        return startPos;
    }

    /**
     * detect end position of sensitive data in json
     *
     * @param builder raw data
     * @param buffLength the length of raw data
     * @param startPos the start position of sensitive data
     * @return the end position of sensitive data
     */
    private static int detectEndJson(StringBuilder builder, int buffLength, int startPos) {
        while (startPos < buffLength && !isEndOfJson(builder, startPos)) {
            startPos++;
        }

        return startPos;
    }

    /**
     * whether a character is a delimiter
     *
     * @param character
     * @return true or false
     */
    private static boolean isDelimiter(char character) {
        return Character.isWhitespace(character) || KNOWN_DELIMITERS.contains(character);
    }

    /**
     * whether data is end of json
     *
     * @param builder raw data
     * @param pos the position of raw data
     * @return true or false
     */
    private static boolean isEndOfJson(StringBuilder builder, int pos) {
        return builder.charAt(pos) == '"' && builder.charAt(pos - 1) != '\\';
    }

    /**
     * detect the end position of sensitive data in xml
     *
     * @param builder raw data
     * @param buffLength the length of raw data
     * @param startPos the start position of sensitive data
     * @param keyword the keyword of sensitive data
     * @param keywordLength the length of keyword
     * @return the end position os sensitive data
     */
    private static int detectEndXml(StringBuilder builder, int buffLength, int startPos,
            String keyword, int keywordLength) {
        if (buffLength < startPos + keywordLength + 3) {
            return -1;
        }

        int passwordEnd = StringUtils.indexOfIgnoreCase(builder, keyword, startPos);
        if (passwordEnd > 0 && builder.charAt(passwordEnd - 1) == '/' && builder.charAt(passwordEnd - 2) == '<') {
            return passwordEnd - 2;
        }

        return -1;
    }

    /**
     * whether the character is the first char of keyword
     *
     * @param character character
     * @return true or false
     */
    private static boolean isKeyWorkdStart(Character character) {
        boolean result = false;
        for (String keyword : KEYWORDS) {
            result = character.equals(keyword.charAt(0)) || result;
        }
        return result;
    }

    /**
     * whether keyword start at right position
     *
     * @param keywordStart the start position of keyword
     * @param pos the right position
     * @return true or false
     */
    private static boolean keywordStartAtRightPosition(int keywordStart, int pos) {
        return keywordStart >= 0 && (keywordStart == pos || keywordStart == pos + 1);
    }

    /**
     * the start position of sensitive data
     *
     * @param keywordStart the start position of keyword
     * @param keywordLength the length of keyword
     * @param separator the separator character of keyword and sensitive data
     * @param builder raw data
     * @return the start position of sensitive data
     */
    private static int maskStartPosition(int keywordStart, int keywordLength, String separator,
            StringBuilder builder) {
        int charPos = keywordStart + keywordLength + separator.length();
        if (Character.isWhitespace(builder.charAt(charPos))) {
            charPos++;
        }
        return charPos;
    }

}
