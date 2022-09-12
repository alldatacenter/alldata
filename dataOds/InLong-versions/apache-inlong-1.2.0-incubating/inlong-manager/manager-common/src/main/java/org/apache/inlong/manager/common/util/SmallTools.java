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

package org.apache.inlong.manager.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.SimpleHash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Common tools
 */
@Slf4j
public class SmallTools {

    private static final Pattern LOWER_NUMBER_PATTERN = Pattern.compile("^(?![0-9]+$)[a-z][a-z0-9_-]{1,200}$");

    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Check if the string starts with a lowercase letter and contains only lowercase letters, digits, `-` or `_`.
     */
    public static boolean isLowerOrNum(String str) {
        if (StringUtils.isNotBlank(str)) {
            return LOWER_NUMBER_PATTERN.matcher(str).matches();
        }
        return false;
    }

    /**
     * IP validation check
     *
     * @param text need to check
     * @return true: valid, false: invalid
     */
    public static boolean ipCheck(String text) {
        if (text != null && !text.isEmpty()) {
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            return text.matches(regex);
        }
        return false;
    }

    public static boolean portCheck(int port) {
        return port > 0 && port < 65535;
    }

    /**
     * Get MD5 from the given string
     */
    public static String getMD5String(String source) {
        if (source == null) {
            return null;
        }

        String retString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source.getBytes(), 0, source.length());
            byte[] retBytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : retBytes) {
                sb.append(hexDigits[(b >> 4) & 0x0f]);
                sb.append(hexDigits[b & 0x0f]);
            }

            retString = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("" + e);
        }

        return retString;
    }

    /**
     * Encrypt user password by MD5 encryption algorithm
     *
     * @param password user password
     * @return password after encrypt
     */
    public static String passwordMd5(String password) {
        return new SimpleHash("MD5", password, null, 1024).toHex();
    }

    /**
     * Calculate expiration date
     *
     * @param validDays Expires in validDays
     * @return expiration date
     */
    public static Date getOverDueDate(Integer validDays) {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, validDays);
        return cal.getTime();
    }

    /**
     * Get valid days
     *
     * @param createDate create date
     * @param dueDate due date
     * @return valid days
     */
    public static Integer getValidDays(Date createDate, Date dueDate) {
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDate createLocalDate = createDate.toInstant().atZone(zoneId).toLocalDate();
        LocalDate dueLocalDate = dueDate.toInstant().atZone(zoneId).toLocalDate();

        return Math.toIntExact(dueLocalDate.toEpochDay() - createLocalDate.toEpochDay());
    }

}
