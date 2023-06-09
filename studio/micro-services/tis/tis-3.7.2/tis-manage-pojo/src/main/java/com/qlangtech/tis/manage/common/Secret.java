/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Secret {

    private static final int KEY_LENGTH = 24;

    private static final String ENCODING = "UTF-8";

    private static final String Algorithm = "DESede";

    private static final Base64 base64 = new Base64();

    public static void main(String[] args) throws Exception {
        System.out.println(base64Decode("4R3w3rYAUM+lJZeLYKMbjA=="));
    }

    public static String base64Encode(String plaintext) throws UnsupportedEncodingException {
        if (plaintext == null) {
            return null;
        }
        byte[] plaintextBytes = plaintext.getBytes(ENCODING);
        return new String(base64.encode(plaintextBytes));
    }

    public static String base64Decode(String cipherText) throws IOException {
        return new String(base64.decode(cipherText));
    }

    public static String encrypt(String src, String cryptKey) {
        try {
            SecretKey desKey = new SecretKeySpec(build3DesKey(base64Decode(cryptKey)), Algorithm);
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.ENCRYPT_MODE, desKey);
            byte[] cryptBytes = c1.doFinal(src.getBytes(ENCODING));
            return new String(base64.encode(cryptBytes));
        } catch (Exception e) {
            throw new RuntimeException(src, e);
        }
    }

    public static String decrypt(String src, String cryptKey) {
        try {
            cryptKey = base64Decode(cryptKey);
            SecretKey desKey = new SecretKeySpec(build3DesKey(cryptKey), Algorithm);
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.DECRYPT_MODE, desKey);
            byte[] cryptBytes = base64.decode(src);
            return new String(c1.doFinal(cryptBytes));
        } catch (Exception e) {
            throw new RuntimeException(src, e);
        }
    }

    public static byte[] build3DesKey(String keyStr) throws UnsupportedEncodingException {
        byte[] key = new byte[KEY_LENGTH];
        byte[] temp = keyStr.getBytes(ENCODING);
        if (key.length > temp.length) {
        } else {
        }
        return key;
    }
}
