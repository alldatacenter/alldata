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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.Test;
import com.aliyun.oss.ClientException;
import junit.framework.Assert;

public class RenewableCipherInputStreamTest {
    @Test
    public void testMarkAndRest() {
        String content = "hdfyqlmrwbdskfnsdkfbs9hmgdr61axlodfsklfjkslvmdklu0whdfyqlmrwbdskfnsdkfbskfh"
                + "isfdfsklfjkslvmdklu0wrwurjjdnksh098j62kfgjsdfbsj4427gc1sfbjsfsj123y214y2hujnhdfyq";

        InputStream isOrig = new ByteArrayInputStream(content.getBytes());
        SecretKey cek = generateCEK();
        byte[] iv = generateIV();
        CryptoCipher cryptoCipher = CryptoScheme.AES_CTR.createCryptoCipher(cek, iv, Cipher.ENCRYPT_MODE, null);

        CipherInputStream isCurr = new RenewableCipherInputStream(isOrig, cryptoCipher, 2048);
        try {
            isCurr.mark(100);
            byte[] buffer = new byte[50];
            int len = isCurr.read(buffer, 0, 50);

            isCurr.reset();
            byte[] buffer2 = new byte[50];
            int len2 = isCurr.read(buffer2, 0, 50);
            Assert.assertEquals(len, len2);
            Assert.assertTrue(Arrays.equals(buffer, buffer2));

            isCurr.reset();
            byte[] buffer3 = new byte[50];
            int len3 = isCurr.read(buffer3);
            Assert.assertEquals(50, len3);
            Assert.assertTrue(Arrays.equals(buffer, buffer3));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMarkUnnromal() {
        String content = "hdfyqlmrwbdskfnsdkfbs9hmgdr61axlodfsklfjkslvmdklu0whdfyqlmrwbdskfnsdkfbskfh"
                + "isfdfsklfjkslvmdklu0wrwurjjdnksh098j62kfgjsdfbsj4427gc1sfbjsfsj123y214y2hujnhdfyq";

        InputStream isOrig = new ByteArrayInputStream(content.getBytes());
        SecretKey cek = generateCEK();
        byte[] iv = generateIV();
        CryptoCipher cryptoCipher = CryptoScheme.AES_CTR.createCryptoCipher(cek, iv, Cipher.ENCRYPT_MODE, null);

        CipherInputStream isCurr = new RenewableCipherInputStream(isOrig, cryptoCipher, 2048);

        try {
            isCurr.mark(50);
            byte[] buffer = new byte[40];
            isCurr.read();
            isCurr.mark(50);
            Assert.fail("The RenewableCipherInputStream marking only supported for first call to read or skip.");
        } catch (UnsupportedOperationException e) {
            // Expected exception.
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            isCurr.reset();
            isCurr.skip(10);
            isCurr.mark(50);
            Assert.fail("The RenewableCipherInputStream marking only supported for first call to read or skip.");
        } catch (UnsupportedOperationException e) {
            // Expected exception.
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testConstruction() {
        String content = "hdfyqlmrwbdskfnsdkfbs9hmgdr61axlodfsklfjkslvmdklu0whdfyqlmrwbdskfnsdkfbskfh"
                + "isfdfsklfjkslvmdklu0wrwurjjdnksh098j62kfgjsdfbsj4427gc1sfbjsfsj123y214y2hujnhdfyq";

        InputStream isOrig = new ByteArrayInputStream(content.getBytes());
        SecretKey cek = generateCEK();
        byte[] iv = generateIV();
        CryptoCipher cryptoCipher = CryptoScheme.AES_CTR.createCryptoCipher(cek, iv, Cipher.ENCRYPT_MODE, null);
        try {
            CipherInputStream isCurr = new RenewableCipherInputStream(isOrig, cryptoCipher, 2049);
            Assert.fail("buff size if should multiple of 512.");
        } catch (IllegalArgumentException e) {
            // Expected excpetion.
        }

        try {
            CipherInputStream isCurr = new RenewableCipherInputStream(isOrig, cryptoCipher, -1);
            Assert.fail("buff size if should multiple of 512.");
        } catch (IllegalArgumentException e) {
            // Expected excpetion.
        }

        try {
            CipherInputStream isCurr = new RenewableCipherInputStream(isOrig, cryptoCipher);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            CipherInputStream isCurr = new RenewableCipherInputStream(isOrig, cryptoCipher, 2048);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCipherInputStream() {
        String content = "hdfyqlmrwbdskfnsdkfbs9hmgdr61axlodfsklfjkslvmdklu0whdfyqlmrwbdskfnsdkfbskfh"
                + "isfdfsklfjkslvmdklu0wrwurjjdnksh098j62kfgjsdfbsj4427gc1sfbjsfsj123y214y2hujnhdfyq";

        InputStream isOrig = new ByteArrayInputStream(content.getBytes());
        SecretKey cek = generateCEK();
        byte[] iv = generateIV();
        CryptoCipher cryptoCipher = CryptoScheme.AES_CTR.createCryptoCipher(cek, iv, Cipher.ENCRYPT_MODE, null);

        CipherInputStream isCurr = new CipherInputStream(isOrig, cryptoCipher, 2048);
        Assert.assertEquals(isCurr.markSupported(), false);
        try {
            isCurr.mark(10);
            isCurr.reset();
            Assert.fail("{@link CipherInputStream} instance not support mark and reset.");
        } catch (Exception e) {
            // Expected excpetion.
        }
    }

    private SecretKey generateCEK() {
        KeyGenerator generator;
        final String keygenAlgo = CryptoScheme.AES_CTR.getKeyGeneratorAlgorithm();
        final int keyLength = CryptoScheme.AES_CTR.getKeyLengthInBits();
        try {
            generator = KeyGenerator.getInstance(keygenAlgo);
            generator.init(keyLength, new SecureRandom());
            SecretKey secretKey = generator.generateKey();
            for (int retry = 0; retry < 9; retry++) {
                secretKey = generator.generateKey();
                if (secretKey.getEncoded()[0] != 0)
                    return secretKey;
            }
            throw new ClientException("Failed to generate secret key");
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException("No such algorithm:" + keygenAlgo + ", " + e.getMessage(), e);
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
