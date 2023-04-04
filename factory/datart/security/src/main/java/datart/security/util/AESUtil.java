/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.security.util;

import com.alibaba.fastjson.JSON;
import datart.core.base.exception.Exceptions;
import datart.core.common.Application;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Slf4j
public class AESUtil {

    public static String encrypt(String src, String securityKey) {
        try {
            SecretKeySpec secretKeySpec = keyGen(securityKey);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return Base64.encodeBase64String(cipher.doFinal(src.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Exceptions.e(e);
        }
        return null;
    }

    public static String decrypt(String src, String securityKey) throws Exception {
        SecretKeySpec secretKeySpec = keyGen(securityKey);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return new String(cipher.doFinal(Base64.decodeBase64(src)));
    }

    public static String encrypt(String src) {
        return encrypt(src, Application.getTokenSecret());
    }

    public static String decrypt(String src) throws Exception {
        return decrypt(src, Application.getTokenSecret());
    }

    public static String encrypt(Object obj, String securityKey) {
        return encrypt(JSON.toJSONString(obj), securityKey);
    }

    public static <T> T decrypt(String src, String securityKey, Class<T> clz) {
        try {
            String json = decrypt(src, securityKey);
            return JSON.parseObject(json, clz);
        } catch (Exception e) {
            Exceptions.e(e);
        }
        return null;
    }


    private static SecretKeySpec keyGen(String securityKey) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(securityKey.getBytes(StandardCharsets.UTF_8));
        kgen.init(128, secureRandom);
        SecretKey secretKey = kgen.generateKey();
        byte[] encoded = secretKey.getEncoded();
        return new SecretKeySpec(encoded, "AES");
    }

}
