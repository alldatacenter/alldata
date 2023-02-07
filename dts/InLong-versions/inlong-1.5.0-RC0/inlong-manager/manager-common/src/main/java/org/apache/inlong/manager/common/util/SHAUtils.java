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

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.crypto.hash.SimpleHash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA encryption and decryption utils.
 */
@Slf4j
@UtilityClass
public class SHAUtils {

    public static final String ALGORITHM_NAME = "SHA-256";
    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Get SHA from the given string.
     *
     * @param source string to be encrypted
     * @return SHA string after encrypt
     */
    public static String getSHAString(String source) {
        if (source == null) {
            return null;
        }

        String retString = null;
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM_NAME);
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
     * Encrypt the given string by SHA-256 encryption algorithm.
     *
     * @param source string to be encrypted
     * @return SHA string after encrypt
     */
    public static String encrypt(String source) {
        return new SimpleHash(ALGORITHM_NAME, source, null, 1024).toHex();
    }

}
