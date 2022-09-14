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

package org.apache.inlong.sort.function;

import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.flink.table.functions.ScalarFunction;

/**
 * EncryptFunction class. It is a custom function, used to encrypt the value in the string.
 */
public class EncryptFunction extends ScalarFunction {

    private static final long serialVersionUID = -7185622027483662395L;

    private static final String ENCODING = "UTF-8";

    private static final String KEY_ALGORITHM_AES = "AES";

    private static final String KEY_ALGORITHM_3DES = "DESede";

    private static final String CIPHER_ALGORITHM_3DES = "DESede/ECB/PKCS5Padding";

    private static final String SIGN_ALGORITHMS = "SHA1PRNG";

    private static final Base64 base64 = new Base64();

    /**
     * eval is String encryption execution method
     *
     * @param field is the field to be encrypted
     * @return encrypted value
     */
    public String eval(String field, String key, String encrypt) {
        if (field != null) {
            String newValue = "";
            EncryptionType encryptionType = EncryptionType.getInstance(encrypt);
            switch (encryptionType) {
                case AES:
                    newValue = encrypt3DES(field, key);
                    break;
                case DESEDE:
                    newValue = encryptAES(field, key);
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Unsupported %s encryption type", encrypt));
            }
            return newValue;
        }
        return null;
    }

    /**
     * encrypt method
     *
     * @param data it is data to be encrypted
     * @param key the key of encryption
     * @param method encryption algorithm
     * @return
     */
    public static String encrypt(String data, String key, String method) {
        if (KEY_ALGORITHM_3DES.equals(method)) {
            return encrypt3DES(data, key);
        } else if (KEY_ALGORITHM_AES.equals(method)) {
            return encryptAES(data, key);
        } else {
            return null;
        }
    }

    /**
     * encrypt by 3DES
     * @param data it is data to be encrypted
     * @param key  the key of encryption
     * @return
     */
    public static String encrypt3DES(String data, String key) {
        try {
            SecretKey desKey = new SecretKeySpec(build3DesKey(key),
                    KEY_ALGORITHM_3DES);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_3DES);
            cipher.init(Cipher.ENCRYPT_MODE, desKey);
            return encryptBASE64(cipher.doFinal(data.getBytes(ENCODING)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * build 3DesKey
     * @param keyStr the key of encryption
     * @return
     */
    public static byte[] build3DesKey(String keyStr) {
        try {
            byte[] key = "000000000000000000000000".getBytes(ENCODING);
            byte[] temp = keyStr.getBytes(ENCODING);
            if (key.length > temp.length) {
                System.arraycopy(temp, 0, key, 0, temp.length);
            } else {
                System.arraycopy(temp, 0, key, 0, key.length);
            }
            return key;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * encrypt by BASE64
     *
     * @param plaintextBytes it is data to be encoded
     * @return
     */
    public static String encryptBASE64(byte[] plaintextBytes) throws Exception {
        return new String(base64.encode(plaintextBytes), ENCODING);
    }

    /**
     * encrypt by AES
     *
     * @param content it is content to be encrypted
     * @param key the key of encryption
     * @return
     */
    public static String encryptAES(String content, String key) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance(KEY_ALGORITHM_AES);
            SecureRandom random = SecureRandom.getInstance(SIGN_ALGORITHMS);
            random.setSeed(key.getBytes(ENCODING));
            kgen.init(128, random);
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, KEY_ALGORITHM_AES);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_AES);
            byte[] byteContent = content.getBytes(ENCODING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] byteRresult = cipher.doFinal(byteContent);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteRresult.length; i++) {
                String hex = Integer.toHexString(byteRresult[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                sb.append(hex.toUpperCase());
            }
            return sb.toString();
        } catch (Exception e) {
            e.toString();
        }
        return null;
    }

    /**
     * Encryption Algorithm Enum
     */
    private enum EncryptionType {

        /**
         * DESEDE
         */
        DESEDE,

        /**
         * AES
         */
        AES;

        public static EncryptionType getInstance(String encrypt) {
            return Arrays.stream(EncryptionType.values()).filter(v -> v.name().equalsIgnoreCase(encrypt))
                    .findFirst().orElse(null);
        }
    }
}
