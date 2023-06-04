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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AES encryption and decryption utils.
 */
@Slf4j
public class AESUtils {

    private static final int DEFAULT_VERSION = 1;
    private static final int KEY_SIZE = 128;
    private static final String ALGORITHM = "AES";
    private static final String RNG_ALGORITHM = "SHA1PRNG";

    private static final String CONFIG_FILE = "application.properties";
    private static final String CONFIG_ITEM_ENCRYPT_KEY_PREFIX = "inlong.encrypt.key.value";
    private static final String CONFIG_ITEM_ENCRYPT_VERSION = "inlong.encrypt.version";

    public static Map<Integer, String> AES_KEY_MAP = new ConcurrentHashMap<>();
    public static Integer CURRENT_VERSION;

    /**
     * Load the application properties configuration
     */
    private static Properties getApplicationProperties() throws IOException {
        Properties properties = new Properties();
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath() + CONFIG_FILE;
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            properties.load(inputStream);
        }
        return properties;
    }

    /**
     * Get the current aes key version
     */
    public static Integer getCurrentVersion(Properties properties) throws IOException {
        if (CURRENT_VERSION != null) {
            return CURRENT_VERSION;
        }

        if (properties == null) {
            properties = getApplicationProperties();
        }
        String verStr = properties.getProperty(CONFIG_ITEM_ENCRYPT_VERSION);
        if (StringUtils.isNotEmpty(verStr)) {
            CURRENT_VERSION = Integer.valueOf(verStr);
        } else {
            CURRENT_VERSION = DEFAULT_VERSION;
        }
        log.debug("Crypto CURRENT_VERSION = {}", CURRENT_VERSION);
        return CURRENT_VERSION;
    }

    /**
     * Get aes key from config file
     */
    public static String getAesKeyByConfig(Integer version) throws Exception {
        Properties properties = getApplicationProperties();
        Integer targetVersion = (version == null ? getCurrentVersion(properties) : version);
        if (StringUtils.isNotEmpty(AES_KEY_MAP.get(targetVersion))) {
            return AES_KEY_MAP.get(targetVersion);
        }

        // get aes key under specified version
        String keyName = CONFIG_ITEM_ENCRYPT_KEY_PREFIX + targetVersion;
        String aesKey = properties.getProperty(keyName);
        if (StringUtils.isEmpty(aesKey)) {
            throw new RuntimeException(String.format("cannot find encryption key %s in application config", keyName));
        }
        AES_KEY_MAP.put(targetVersion, aesKey);
        return aesKey;
    }

    /**
     * Generate key
     */
    private static SecretKey generateKey(byte[] aesKey) throws Exception {
        SecureRandom random = SecureRandom.getInstance(RNG_ALGORITHM);
        random.setSeed(aesKey);
        KeyGenerator gen = KeyGenerator.getInstance(ALGORITHM);
        gen.init(KEY_SIZE, random);
        return gen.generateKey();
    }

    /**
     * Encrypt by key
     */
    public static byte[] encrypt(byte[] plainBytes, byte[] key) throws Exception {
        SecretKey secKey = generateKey(key);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secKey);
        return cipher.doFinal(plainBytes);
    }

    /**
     * Encrypt by key and current version
     */
    public static String encryptToString(byte[] plainBytes, Integer version) throws Exception {
        if (version == null) {
            // no encryption
            return new String(plainBytes, StandardCharsets.UTF_8);
        }
        byte[] keyBytes = getAesKeyByConfig(version).getBytes(StandardCharsets.UTF_8);
        return parseByte2HexStr(encrypt(plainBytes, keyBytes));
    }

    /**
     * Decrypt by key and specified version
     */
    public static byte[] decrypt(byte[] cipherBytes, byte[] key) throws Exception {
        SecretKey secKey = generateKey(key);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secKey);
        return cipher.doFinal(cipherBytes);
    }

    /**
     * Encrypt by property key
     */
    public static byte[] decryptAsString(String cipherText, Integer version) throws Exception {
        if (version == null) {
            // No decryption: treated as plain text
            return cipherText.getBytes(StandardCharsets.UTF_8);
        }
        byte[] keyBytes = getAesKeyByConfig(version).getBytes(StandardCharsets.UTF_8);
        return decrypt(parseHexStr2Byte(cipherText), keyBytes);
    }

    /**
     * Parse byte to String in Hex type
     */
    public static String parseByte2HexStr(byte[] buf) {
        StringBuilder strBuf = new StringBuilder();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            strBuf.append(hex.toUpperCase());
        }
        return strBuf.toString();
    }

    /**
     * Parse String to byte as Hex type
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }
}
