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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.Cipher;

import org.junit.Test;
import junit.framework.Assert;

public class RSAEncryptionUnitTest {
    private final String PLAIN_TEXT = "kdnsknshiwonrjsn23e1vdjknvlsfnsl34ihsohnqm92u32jns.msl082mjk73643dns";

    private final String PUBLIC_KEY_PEM_XC509 = 
            "-----BEGIN PUBLIC KEY-----\n"
            + "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIUc0RE+OF4qvJkFp/sBR4iiPy\n"
            + "5czlKdHoOKOjhvh93aGpipoMb05+t07XSOBDJUzKGhqqVQJZEQahKXJUU0h3mxYy\n"
            + "xRQMhhWWWdH1LH4s/GAjf4h5l+6tKxS6mnZGH4IlbJz1pvbPiZjzD6BEWtGBMAxZ\n" + "IjqPgSRjJpB6fBIrHQIDAQAB\n"
            + "-----END PUBLIC KEY-----";

    private final String PRIVATE_KEY_PEM_PKCS1 = 
            "-----BEGIN RSA PRIVATE KEY-----\n"
            + "MIICXQIBAAKBgQDIUc0RE+OF4qvJkFp/sBR4iiPy5czlKdHoOKOjhvh93aGpipoM\n"
            + "b05+t07XSOBDJUzKGhqqVQJZEQahKXJUU0h3mxYyxRQMhhWWWdH1LH4s/GAjf4h5\n"
            + "l+6tKxS6mnZGH4IlbJz1pvbPiZjzD6BEWtGBMAxZIjqPgSRjJpB6fBIrHQIDAQAB\n"
            + "AoGAG7kmdkyYWnkqaTTvMWi/DIehvgYLu1N0V30vOHx/e3vm2b3y3/GvnV3lLWpK\n"
            + "j0BkRjwioJwvPQBcOIWx6vWzu4soHNI+e1FTJgFETfWs1+HAPgR9GptbDJGdVHc4\n"
            + "i85JB+nKvbuEmm/kq0xmdQ3OeSVqZqyflmGTncCMUAK5WAECQQDpBws3eDa7w4Sr\n"
            + "ZyomMMiv0UW6GYWWXxnvSVzK2A55AQiSoejU2RPhZ6LJzuvr2Mez9l7mOvyiJvvd\n"
            + "caO6UawdAkEA3BFM2z82m3Q9eYPSegB2knCAuKTjZZmDLt7Ibd/z1KgdKr3tpzRt\n"
            + "WNlxqS0l9bsN79IfSwGwwbFFhiQSrWRLAQJAJs9gg92GqCD5IJ7u+ytW0Ul2ZndH\n"
            + "s3KlXCAIz1PKnUaZyeojYAfDcuAS0a+fxUj2gbd/uLKMTulVO11o2mgt1QJBAJBb\n"
            + "UN0pNDr5HzJMxI5/K0iYP/ffQcNt1d2zCir5E0tWE/vrpq9d9rSnvqVJFnOBBn1g\n"
            + "imJ7c2U7Ue3ST+YpugECQQDGSAxsLaSGFLfbBOKzM7spv3cvza7vYBksfR6xT6KR\n"
            + "aURh6yaGmkK8gfmrkFMtWQ0CC3fNecewUNLpScuKunCh\n" 
            + "-----END RSA PRIVATE KEY-----";

    private static String PRIVATE_KEY_PEM_PKCS8 = 
            "-----BEGIN PRIVATE KEY-----\n"
            + "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMhRzRET44Xiq8mQ\n"
            + "Wn+wFHiKI/LlzOUp0eg4o6OG+H3doamKmgxvTn63TtdI4EMlTMoaGqpVAlkRBqEp\n"
            + "clRTSHebFjLFFAyGFZZZ0fUsfiz8YCN/iHmX7q0rFLqadkYfgiVsnPWm9s+JmPMP\n"
            + "oERa0YEwDFkiOo+BJGMmkHp8EisdAgMBAAECgYAbuSZ2TJhaeSppNO8xaL8Mh6G+\n"
            + "Bgu7U3RXfS84fH97e+bZvfLf8a+dXeUtakqPQGRGPCKgnC89AFw4hbHq9bO7iygc\n"
            + "0j57UVMmAURN9azX4cA+BH0am1sMkZ1UdziLzkkH6cq9u4Sab+SrTGZ1Dc55JWpm\n"
            + "rJ+WYZOdwIxQArlYAQJBAOkHCzd4NrvDhKtnKiYwyK/RRboZhZZfGe9JXMrYDnkB\n"
            + "CJKh6NTZE+FnosnO6+vYx7P2XuY6/KIm+91xo7pRrB0CQQDcEUzbPzabdD15g9J6\n"
            + "AHaScIC4pONlmYMu3sht3/PUqB0qve2nNG1Y2XGpLSX1uw3v0h9LAbDBsUWGJBKt\n"
            + "ZEsBAkAmz2CD3YaoIPkgnu77K1bRSXZmd0ezcqVcIAjPU8qdRpnJ6iNgB8Ny4BLR\n"
            + "r5/FSPaBt3+4soxO6VU7XWjaaC3VAkEAkFtQ3Sk0OvkfMkzEjn8rSJg/999Bw23V\n"
            + "3bMKKvkTS1YT++umr132tKe+pUkWc4EGfWCKYntzZTtR7dJP5im6AQJBAMZIDGwt\n"
            + "pIYUt9sE4rMzuym/dy/Nru9gGSx9HrFPopFpRGHrJoaaQryB+auQUy1ZDQILd815\n" + "x7BQ0ulJy4q6cKE=\n"
            + "-----END PRIVATE KEY-----";

    @Test
    public void testUsePrivateKeyPKCS8() {
        try {
            final RSAPrivateKey privateKey = SimpleRSAEncryptionMaterials.getPrivateKeyFromPemPKCS8(PRIVATE_KEY_PEM_PKCS8);
            final RSAPublicKey publicKey = SimpleRSAEncryptionMaterials.getPublicKeyFromPemX509(PUBLIC_KEY_PEM_XC509);

            KeyPair keyPair = new KeyPair(publicKey, privateKey);

            byte[] encryptedData = encrypt(keyPair.getPublic(), PLAIN_TEXT.getBytes());
            byte[] decryptedData = decrypt(keyPair.getPrivate(), encryptedData);
            String decryptedStr = new String(decryptedData);
            Assert.assertEquals(PLAIN_TEXT, decryptedStr);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUsePrivateKeyPKCS1() {
        try {
            final RSAPrivateKey privateKey = SimpleRSAEncryptionMaterials.getPrivateKeyFromPemPKCS1(PRIVATE_KEY_PEM_PKCS1);
            final RSAPublicKey publicKey = SimpleRSAEncryptionMaterials.getPublicKeyFromPemX509(PUBLIC_KEY_PEM_XC509);

            KeyPair keyPair = new KeyPair(publicKey, privateKey);

            byte[] encryptedData = encrypt(keyPair.getPublic(), PLAIN_TEXT.getBytes());
            byte[] decryptedData = decrypt(keyPair.getPrivate(), encryptedData);
            String decryptedStr = new String(decryptedData);
            Assert.assertEquals(PLAIN_TEXT, decryptedStr);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUsePrivateKeyPKCS1AndPKCS8() {
        try {
            final RSAPrivateKey privateKeyPKCS1 = SimpleRSAEncryptionMaterials.getPrivateKeyFromPemPKCS1(PRIVATE_KEY_PEM_PKCS1);
            final RSAPublicKey publicKey = SimpleRSAEncryptionMaterials.getPublicKeyFromPemX509(PUBLIC_KEY_PEM_XC509);
            final RSAPrivateKey privateKeyPKCS8 = SimpleRSAEncryptionMaterials.getPrivateKeyFromPemPKCS8(PRIVATE_KEY_PEM_PKCS8);
            
            // encrypt by public key
            byte[] encryptedData = encrypt(publicKey, PLAIN_TEXT.getBytes());

            // dicrypt by private key pkcs1
            byte[] decryptedData = decrypt(privateKeyPKCS1, encryptedData);
            String decryptedStrPKCS1 = new String(decryptedData);
            Assert.assertEquals(PLAIN_TEXT, decryptedStrPKCS1);

            // decrypt by private key pkcs8
            decryptedData = decrypt(privateKeyPKCS8, encryptedData);
            String decryptedStrPKCS8 = new String(decryptedData);
            Assert.assertEquals(decryptedStrPKCS1, decryptedStrPKCS8);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private byte[] encrypt(PublicKey publicKey, byte[] plainData) throws Exception {
        if (publicKey == null) {
            throw new Exception("public key is null.");
        }

        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", getBouncyCastleProvider());
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] output = cipher.doFinal(plainData);
        return output;
    }

    private byte[] decrypt(PrivateKey privateKey, byte[] cipherData) throws Exception {
        if (privateKey == null) {
            throw new Exception("private key is null.");
        }
        Cipher cipher = null;
        cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", getBouncyCastleProvider());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] output = cipher.doFinal(cipherData);
        return output;
    }

    public static Provider getBouncyCastleProvider()
    {
        try {
            Class<?> clz = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            return (Provider)clz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
