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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.crypto;

import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.junit.Test;
import junit.framework.Assert;

public class CryptoSchemeTest {
    @Test
    public void testAdjustIvNormal() {
        try {
            byte[] iv = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
            CryptoScheme cryptScheme = CryptoScheme.AES_CTR;
            byte[] adjustIV = cryptScheme.adjustIV(iv, 16);
            Assert.assertEquals(iv[15] + 1, adjustIV[15]);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAdjustIvUnnormal() {
        try {
            byte[] iv = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };
            CryptoScheme cryptScheme = CryptoScheme.AES_CTR;
            byte[] adjustIV = cryptScheme.adjustIV(iv, 16);
            Assert.fail("IV length should be 16.");
        } catch (UnsupportedOperationException e) {
            // Expected exception.
        }

        try {
            byte[] iv = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
            CryptoScheme cryptScheme = CryptoScheme.AES_CTR;
            cryptScheme.adjustIV(iv, 17);
            Assert.fail("start pos should allgned with 16 bytes.");
        } catch (IllegalArgumentException e) {
            // Expected exception.
        }
    }

    @Test
    public void testIncrementBlocks() {
        try {
            byte[] iv = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, 0, 0x01, 0x02, 0x03, 0x04 };
            byte[] retIV = CryptoScheme.incrementBlocks(iv, 0X1122334400000000L);
            byte[] expectedIV = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 0x11, 0x22, 0x33, 0x44, 0x01, 0x02, 0x03, 0x04 };
            Assert.assertTrue(Arrays.equals(retIV, expectedIV));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testIncrementBlocksUnnormal() {
        try {
            byte[] iv = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, 0, 0x01, 0x02, 0x03, 0x04, 0x05 };
            CryptoScheme.incrementBlocks(iv, 0X1122334400000000L);
            Assert.fail("iv array is not 16-bytes.");
        } catch (IllegalArgumentException e) {
            // Expected exception.
        }

        try {
            byte[] iv = null;
            CryptoScheme.incrementBlocks(iv, 0X1122334400000000L);
            Assert.fail("iv should not be null.");
        } catch (IllegalArgumentException e) {
            // Expected exception.
        }
    }

    @Test
    public void testGetSchemeFromCEKAlgo() {
        try {
            CryptoScheme scheme = CryptoScheme.fromCEKAlgo("AES/CTR/NoPadding");
            scheme.getContentChiperAlgorithm().equals(CryptoScheme.AES_CTR.getContentChiperAlgorithm());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            CryptoScheme scheme = CryptoScheme.fromCEKAlgo("illegal-algo");
            Assert.fail("get crypto scheme from illeagel algoritm name get should be failed.");
        } catch (UnsupportedOperationException e) {
            // Expected exception.
        }
    }

    @Test
    public void testCreateCipherUnnormal() {
        try {
            SecretKey cek = null;
            byte[] iv = generateIV();
            CryptoCipher cryptoCipher = CryptoScheme.AES_CTR.createCryptoCipher(cek, iv, Cipher.ENCRYPT_MODE, null);
        } catch (Exception e) {
            // Expected exception.
        }
    }

    private byte[] generateIV() {
        final byte[] iv = new byte[CryptoScheme.AES_CTR.getContentChiperIVLength()];
        new SecureRandom().nextBytes(iv);
        for (int i = 8; i < 12; i++) {
            iv[i] = 0;
        }
        return iv;
    }
}