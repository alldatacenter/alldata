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

package org.apache.inlong.tubemq.corebase.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;

/**
 * Utility to String operations.
 */
public class TStringUtils {

    // empty string
    public static final String EMPTY = "";

    public static boolean isEmpty(String str) {
        return StringUtils.isEmpty(str);
    }

    public static boolean isNotEmpty(String str) {
        return StringUtils.isNotEmpty(str);
    }

    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    public static boolean isNotBlank(String str) {
        return StringUtils.isNotBlank(str);
    }

    public static String trim(String str) {
        return StringUtils.trim(str);
    }

    public static int getLevenshteinDistance(String s, String t) {
        return StringUtils.getLevenshteinDistance(s, t);
    }

    public static boolean isLetter(char ch) {
        return (Character.isUpperCase(ch)
                || Character.isLowerCase(ch));
    }

    public static boolean isLetterOrDigit(char ch) {
        return isLetter(ch) || Character.isDigit(ch);
    }

    /**
     * <p/>
     * <pre>
     * TStringUtils.toCamelCase(null)  = null
     * TStringUtils.toCamelCase("")    = ""
     * TStringUtils.toCamelCase("aBc") = "aBc"
     * TStringUtils.toCamelCase("aBc def") = "aBcDef"
     * TStringUtils.toCamelCase("aBc def_ghi") = "aBcDefGhi"
     * TStringUtils.toCamelCase("aBc def_ghi 123") = "aBcDefGhi123"
     * </pre>
     * <p/>
     * </p> <p> This method preserves all separators except underscores and whitespace. </p>
     *
     * @param origStr The string to be converted
     * @return Convert the string to Camel Case
     *     if it is <code>null</code>, return<code>null</code>
     */
    public static String toCamelCase(String origStr) {
        if (isEmpty(origStr)) {
            return origStr;
        }
        origStr = origStr.trim();
        int length = origStr.length();

        char curChar;
        char preChar;
        int curWritePos = 0;
        boolean upperCaseNext = false;
        char[] tgtStr = new char[length];
        for (int index = 0; index < length;) {
            curChar = origStr.charAt(index);
            index += Character.charCount(curChar);
            // ignore white space chars
            if (Character.isWhitespace(curChar)) {
                continue;
            }
            // process char and '_' delimiter
            if (isLetter(curChar)) {
                if (upperCaseNext) {
                    upperCaseNext = false;
                    curChar = Character.toUpperCase(curChar);
                } else {
                    if (curWritePos == 0) {
                        curChar = Character.toLowerCase(curChar);
                    } else {
                        preChar = tgtStr[curWritePos - 1];
                        // judge pre-read char not Letter or digit
                        if (!isLetterOrDigit(preChar)) {
                            curChar = Character.toLowerCase(curChar);
                        } else {
                            if (Character.isUpperCase(preChar)) {
                                curChar = Character.toLowerCase(curChar);
                            }
                        }
                    }
                }
                tgtStr[curWritePos++] = curChar;
            } else {
                if (curChar == '_') {
                    upperCaseNext = true;
                } else {
                    tgtStr[curWritePos++] = curChar;
                }
            }
        }
        return new String(tgtStr, 0, curWritePos);
    }

    /**
     * Get the authorization signature based on the provided values
     * base64.encode(hmacSha1(password, username, timestamp, random number))
     *
     * @param usrName       the user name
     * @param usrPassWord   the password of username
     * @param timestamp     the time stamp
     * @param randomValue   the random value
     */
    public static String getAuthSignature(final String usrName,
            final String usrPassWord,
            long timestamp, int randomValue) {
        Base64 base64 = new Base64();
        StringBuilder sbuf = new StringBuilder(512);
        byte[] baseStr =
                base64.encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, usrPassWord)
                        .hmac(sbuf.append(usrName)
                                .append(timestamp)
                                .append(randomValue)
                                .toString()));
        sbuf.delete(0, sbuf.length());
        String signature = "";
        try {
            signature = URLEncoder.encode(new String(baseStr,
                    TBaseConstants.META_DEFAULT_CHARSET_NAME),
                    TBaseConstants.META_DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return signature;
    }

    /**
     * Build attribute information
     *
     * @param srcAttrs   the current attribute
     * @param attrKey    the attribute key
     * @param attrVal    the attribute value
     * @return           the new attribute information
     */
    public static String setAttrValToAttributes(String srcAttrs,
            String attrKey, String attrVal) {
        StringBuilder sbuf = new StringBuilder(512);
        if (isBlank(srcAttrs)) {
            return sbuf.append(attrKey).append(TokenConstants.EQ).append(attrVal).toString();
        }
        if (!srcAttrs.contains(attrKey)) {
            return sbuf.append(srcAttrs)
                    .append(TokenConstants.SEGMENT_SEP)
                    .append(attrKey).append(TokenConstants.EQ).append(attrVal).toString();
        }
        boolean notFirst = false;
        String[] strAttrs = srcAttrs.split(TokenConstants.SEGMENT_SEP);
        for (String strAttrItem : strAttrs) {
            if (isNotBlank(strAttrItem)) {
                if (notFirst) {
                    sbuf.append(TokenConstants.SEGMENT_SEP);
                }
                if (strAttrItem.contains(attrKey)) {
                    sbuf.append(attrKey).append(TokenConstants.EQ).append(attrVal);
                } else {
                    sbuf.append(strAttrItem);
                }
                notFirst = true;
            }
        }
        return sbuf.toString();
    }

    /**
     * Get attribute value by key from attribute information
     *
     * @param srcAttrs   the current attribute
     * @param attrKey    the attribute key
     * @return           the attribute value
     */
    public static String getAttrValFrmAttributes(String srcAttrs, String attrKey) {
        if (!isBlank(srcAttrs)) {
            String[] strAttrs = srcAttrs.split(TokenConstants.SEGMENT_SEP);
            for (String attrItem : strAttrs) {
                if (isNotBlank(attrItem)) {
                    String[] kv = attrItem.split(TokenConstants.EQ);
                    if (attrKey.equals(kv[0])) {
                        if (kv.length == 1) {
                            return "";
                        } else {
                            return kv[1];
                        }
                    }
                }
            }
        }
        return null;
    }

}
