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

import java.lang.reflect.Field;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.StringUtils;

/**
 * This provide a simple rsa encryption materials for client-side encryption.
 */
public class SimpleRSAEncryptionMaterials implements EncryptionMaterials {
    // Enable bouncy castle provider
    static {
        CryptoRuntime.enableBouncyCastle();
    }
    public static final String KEY_WRAP_ALGORITHM = "RSA/NONE/PKCS1Padding";
    private KeyPair keyPair;
    private Map<String, String> desc;
    private final LinkedHashMap<KeyPair, Map<String, String>> keyPairDescMaterials = 
                                    new LinkedHashMap<KeyPair, Map<String, String>>();

    public SimpleRSAEncryptionMaterials(KeyPair keyPair) {
        assertParameterNotNull(keyPair, "KeyPair");
        this.keyPair = keyPair;
        desc = new HashMap<String, String>();
        keyPairDescMaterials.put(keyPair, desc);
    }

    public SimpleRSAEncryptionMaterials(KeyPair keyPair, Map<String, String> desc) {
        assertParameterNotNull(keyPair, "KeyPair");
        this.keyPair = keyPair;
        this.desc = (desc == null) ? new HashMap<String, String>() : new HashMap<String, String>(desc);
        keyPairDescMaterials.put(keyPair, desc);
    }

    /**
     * Add a key pair and its descrption for decrypting data.
     * 
     * @param keyPair
     *            The RSA key pair.
     * @param description
     *            The descripton of encryption materails.
     */
    public synchronized void addKeyPairDescMaterial(KeyPair keyPair, Map<String, String> description) {
        assertParameterNotNull(keyPair, "keyPair");
        if (description != null) {
            keyPairDescMaterials.put(keyPair, new HashMap<String, String>(description));
        } else {
            keyPairDescMaterials.put(keyPair, new HashMap<String, String>());
        }
    }

    /**
     * Find the specifed key pair for decrypting by the specifed descrption.
     * 
     * @param desc
     *            The encryption description.  
     * @return the lastest specifed key pair that matchs the descrption, otherwise return null.
     */
    private KeyPair findKeyPairByDescription(Map<String, String> desc) {
        if (desc == null) {
            return null;
        }
        for (Map.Entry<KeyPair, Map<String, String>> entry : keyPairDescMaterials.entrySet()) {
            if (desc.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the lastest key-value in the LinedHashMap.
     */
    private <K, V> Entry<K, V> getTailByReflection(LinkedHashMap<K, V> map)
            throws NoSuchFieldException, IllegalAccessException {
        Field tail = map.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        return (Entry<K, V>) tail.get(map);
    }

    /**
     * Encrypt the content encryption key(cek) and iv, and put the result into
     * {@link ContentCryptoMaterialRW}.
     * 
     * @param contentMaterialRW
     *            The materials that contans all content crypto info, 
     *            it must be constructed on outside and filled with the iv cek parameters.
     *            Then it will be builed with the encrypted cek ,encrypted iv, key wrap 
     *            algorithm and encryption materials description by this method.
     */
    @Override
    public void encryptCEK(ContentCryptoMaterialRW contentMaterialRW) {
        assertParameterNotNull(contentMaterialRW, "ContentCryptoMaterialRW");
        assertParameterNotNull(contentMaterialRW.getCEK(), "ContentCryptoMaterialRW#getCEK()");
        assertParameterNotNull(contentMaterialRW.getIV(), "ContentCryptoMaterialRW#getIV()");
        try {
            Key key = keyPair.getPublic();
            Cipher cipher = Cipher.getInstance(KEY_WRAP_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new SecureRandom());
            byte[] encryptedCEK = cipher.doFinal(contentMaterialRW.getCEK().getEncoded());
            byte[] encryptedIV = cipher.doFinal(contentMaterialRW.getIV());

            contentMaterialRW.setEncryptedCEK(encryptedCEK);
            contentMaterialRW.setEncryptedIV(encryptedIV);
            contentMaterialRW.setKeyWrapAlgorithm(KEY_WRAP_ALGORITHM);
            contentMaterialRW.setMaterialsDescription(desc);
        } catch (Exception e) {
            throw new ClientException("Unable to encrypt content encryption key or iv." + e.getMessage(), e);
        }
    }

    /**
     * Decrypt the encrypted content encryption key(cek) and encrypted iv and put
     * the result into {@link ContentCryptoMaterialRW}.
     * 
     * @param contentMaterialRW 
     *                 The materials that contans all content crypto info,
     *                 it must be constructed on outside and filled with
     *                 the encrypted cek ,encrypted iv, key wrap algorithm,
     *                 encryption materials description and cek generator
     *                 algothrim. Then it will be builded with the cek and iv.
     */
    @Override
    public void decryptCEK(ContentCryptoMaterialRW contentMaterialRW) {
        assertParameterNotNull(contentMaterialRW, "ContentCryptoMaterialRW");
        assertParameterNotNull(contentMaterialRW.getEncryptedCEK(), "ContentCryptoMaterialRW#getEncryptedCEK");
        assertParameterNotNull(contentMaterialRW.getEncryptedIV(), "ContentCryptoMaterialRW#getEncryptedIV");
        assertParameterNotNull(contentMaterialRW.getKeyWrapAlgorithm(), "ContentCryptoMaterialRW#getKeyWrapAlgorithm");

        if (!contentMaterialRW.getKeyWrapAlgorithm().toLowerCase().equals(KEY_WRAP_ALGORITHM.toLowerCase())) {
            throw new ClientException(
                    "Unrecognize your object key wrap algorithm: " + contentMaterialRW.getKeyWrapAlgorithm());
        }

        try {
            KeyPair keyPair = findKeyPairByDescription(contentMaterialRW.getMaterialsDescription());
            if (keyPair == null) {
                Entry<KeyPair, Map<String, String>> entry = getTailByReflection(keyPairDescMaterials);
                keyPair = entry.getKey();
            }

            Key key = keyPair.getPrivate();
            Cipher cipher = Cipher.getInstance(KEY_WRAP_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cekBytes = cipher.doFinal(contentMaterialRW.getEncryptedCEK());
            byte[] iv = cipher.doFinal(contentMaterialRW.getEncryptedIV());
            SecretKey cek = new SecretKeySpec(cekBytes, "");

            contentMaterialRW.setCEK(cek);
            contentMaterialRW.setIV(iv);
        } catch (Exception e) {
            throw new ClientException("Unable to decrypt the secured content key and iv. " + e.getMessage(), e);
        }
    }

    /**
     * Gets a rsa private key from PKCS1 pem string.
     *
     * @param privateKeyStr
     *            The private key string in PKCS1 foramt.
     *
     * @return a new rsa private key
     */
    public static RSAPrivateKey getPrivateKeyFromPemPKCS1(final String privateKeyStr) {
        try {
            String adjustStr = StringUtils.replace(privateKeyStr, "-----BEGIN PRIVATE KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----BEGIN RSA PRIVATE KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----END PRIVATE KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----END RSA PRIVATE KEY-----", "");
            adjustStr = adjustStr.replace("\n", "");

            CryptoRuntime.enableBouncyCastle();

            byte[] buffer = BinaryUtil.fromBase64String(adjustStr);
            RSAPrivateKeySpec keySpec = CryptoRuntime.convertPemPKCS1ToPrivateKey(buffer);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            throw new ClientException("get private key from PKCS1 pem String error." + e.getMessage(), e);
        }
    }

    /**
     * Gets a rsa private key from PKCS8 pem string.
     *
     * @param privateKeyStr
     *            The private key string in PKCS8 format .
     *
     * @return a new rsa private key
     */
    public static RSAPrivateKey getPrivateKeyFromPemPKCS8(final String privateKeyStr) {
        try {
            String adjustStr = StringUtils.replace(privateKeyStr, "-----BEGIN PRIVATE KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----BEGIN RSA PRIVATE KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----END PRIVATE KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----END RSA PRIVATE KEY-----", "");
            adjustStr = adjustStr.replace("\n", "");

            byte[] buffer = BinaryUtil.fromBase64String(adjustStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new ClientException("Get private key from PKCS8 pem String error: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a rsa public key from PKCS8 pem string.
     *
     * @param publicKeyStr
     *            The public key string in PKCS8 format.
     *
     * @return a new rsa public key
     */
    public static RSAPublicKey getPublicKeyFromPemX509(final String publicKeyStr) {
        try {
            String adjustStr = StringUtils.replace(publicKeyStr, "-----BEGIN PUBLIC KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----BEGIN RSA PUBLIC KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----END PUBLIC KEY-----", "");
            adjustStr = StringUtils.replace(adjustStr, "-----END RSA PUBLIC KEY-----", "");
            adjustStr = adjustStr.replace("\n", "");

            byte[] buffer = BinaryUtil.fromBase64String(adjustStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new ClientException("Get public key from X509 pem String error." + e.getMessage(), e);
        }
    }

    private void assertParameterNotNull(Object parameterValue, String errorMessage) {
        if (parameterValue == null)
            throw new IllegalArgumentException(errorMessage);
    }
}
