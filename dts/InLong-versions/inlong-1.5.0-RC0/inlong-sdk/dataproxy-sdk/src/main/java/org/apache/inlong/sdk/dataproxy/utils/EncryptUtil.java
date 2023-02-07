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

package org.apache.inlong.sdk.dataproxy.utils;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class EncryptUtil {

    public static final String AES = "AES";
    public static final int AES_KEY_SIZE = 128;
    public static final int MAX_ENCRYPT_BLOCK = 117;
    public static final int MAX_DECRYPT_BLOCK = 128;
    private static final Logger logger = LoggerFactory.getLogger(EncryptUtil.class);

    /**
     * load key
     *
     * @param path path
     *
     * @throws Exception exception
     */
    public static String loadPublicKeyByFileText(String path) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                sb.append(readLine);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            throw new Exception("key error");
        } catch (NullPointerException e) {
            throw new Exception("npe error");
        }
    }

    public static byte[] loadPublicKeyByFileBinary(String path) throws Exception {
        try {
            File file = new File(path);
            int len = 0;
            if (file.exists()) {
                len = (int) file.length();
            } else {
                // error
            }

            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
            byte[] content = new byte[len];
            while (dis.read(content) != -1) {

            }

            dis.close();
            return content;
        } catch (IOException e) {
            throw new Exception("key error");
        } catch (NullPointerException e) {
            throw new Exception("npe error");
        }
    }

    /**
     * get key from public
     *
     * @param publicKeyStr get key string
     */
    public static RSAPublicKey loadPublicKeyByText(String publicKeyStr) {
        try {
            byte[] buffer = Base64.decodeBase64(publicKeyStr);
            // byte[] buffer = publicKeyStr.getBytes();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            logger.error("no such algorithm", e);
        } catch (InvalidKeySpecException e) {
            logger.error("invalid key spec", e);
        } catch (NullPointerException e) {
            logger.error("public key is null", e);
        }
        return null;
    }

    public static RSAPublicKey loadPublicKeyByBinary(byte[] publicKeyByte) {
        try {
            // byte[] buffer = Base64.decodeBase64(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyByte);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            logger.error("no such algorithm", e);
        } catch (InvalidKeySpecException e) {
            logger.error("invalid key spec", e);
        } catch (NullPointerException e) {
            logger.error("public key is null", e);
        }

        return null;
    }

    /**
     * get key from file
     *
     * @param path key path
     * @return whether success
     *
     * @throws Exception
     */
    public static String loadPrivateKeyByFileText(String path) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                sb.append(readLine);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            throw new Exception("key error");
        } catch (NullPointerException e) {
            throw new Exception("npe error");
        }
    }

    public static byte[] loadPrivateKeyByFileBinary(String path) throws Exception {
        try {
            File file = new File(path);
            int len = 0;
            if (file.exists()) {
                len = (int) file.length();
            } else {
                // error
            }

            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
            byte[] content = new byte[len];
            while (dis.read(content) != -1) {

            }

            dis.close();
            return content;
        } catch (IOException e) {
            throw new Exception("key error");
        } catch (NullPointerException e) {
            throw new Exception("npe error");
        }
    }

    /**
     * load private key by text
     *
     * @param privateKeyStr private key
     *
     * @throws Exception exception
     */
    public static RSAPrivateKey loadPrivateKeyByText(String privateKeyStr)
            throws Exception {
        try {
            // byte[] buffer = Base64.decodeBase64(privateKeyStr);
            byte[] buffer = privateKeyStr.getBytes();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("no such algorithm");
        } catch (InvalidKeySpecException e) {
            throw new Exception("key error");
        } catch (NullPointerException e) {
            throw new Exception("npe error");
        }
    }

    public static RSAPrivateKey loadPrivateKeyByBinary(byte[] privateKeyByte)
            throws Exception {
        try {
            // byte[] buffer = Base64.decodeBase64(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyByte);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("no such algorithm");
        } catch (InvalidKeySpecException e) {
            throw new Exception("key error");
        } catch (NullPointerException e) {
            throw new Exception("npe error");
        }
    }

    /**
     * key encrypt
     *
     * @param publicKey public key
     * @param data data
     * @return
     *
     * @throws Exception exception
     */
    public static byte[] rsaEncrypt(RSAPublicKey publicKey, byte[] data)
            throws Exception {

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;

        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;

    }

    /**
     * key decrypt
     *
     * @param privateKey key
     * @param encryptedData data
     * @return
     *
     * @throws Exception exception
     */
    public static byte[] decryptByPrivateKey(RSAPrivateKey privateKey, byte[] encryptedData)
            throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;

        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * rsa decrypt
     *
     * @param privateKey private key
     * @param cipherData data
     * @return message
     *
     * @throws Exception exception
     */
    public static byte[] rsaDecrypt(RSAPrivateKey privateKey, byte[] cipherData)
            throws Exception {
        if (privateKey == null) {
            throw new Exception("private key is null");
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
            // cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] output = cipher.doFinal(cipherData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("no such algorithm");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            throw new Exception("invalid key");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("illegal size");
        } catch (BadPaddingException e) {
            throw new Exception("bad padding");
        }
    }

    /**
     * rsa decrypt
     *
     * @param publicKey public key
     * @param cipherData cipher data
     * @return
     *
     * @throws Exception exception
     */
    public static byte[] rsaDecrypt(RSAPublicKey publicKey, byte[] cipherData)
            throws Exception {
        if (publicKey == null) {
            throw new Exception("public key is null");
        }
        Cipher cipher = null;
        try {

            cipher = Cipher.getInstance("RSA");
            // cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] output = cipher.doFinal(cipherData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("no such algorithm");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            throw new Exception("invalid key");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("illegal block");
        } catch (BadPaddingException e) {
            throw new Exception("bad padding");
        }
    }

    /**
     * generate AES key
     *
     * @return base64 key
     */
    public static byte[] generateAesKey() {

        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance(AES);
        } catch (NoSuchAlgorithmException e) {
            logger.error("generate Aes key error {}", e);
        }

        kg.init(AES_KEY_SIZE);

        SecretKey secretKey = kg.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * AES encrypt
     *
     * @param plainText
     * @param aesKey
     * @return
     */
    public static byte[] aesEncrypt(byte[] plainText, byte[] aesKey) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return cipher.doFinal(plainText);
        } catch (Exception e) {
            logger.error("aesEncrypt error {}", e);
            return null;
        }
    }

    /**
     * AES decrypt
     *
     * @param cipherText
     * @param aesKey
     * @return des decrypt
     */
    public static byte[] aesDecrypt(byte[] cipherText, byte[] aesKey) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            logger.error("aesDecrypt error {}", e);
            return null;
        }
    }
}
