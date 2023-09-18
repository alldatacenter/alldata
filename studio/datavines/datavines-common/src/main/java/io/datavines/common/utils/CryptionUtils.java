/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.utils;

import io.datavines.common.exception.DataVinesException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptionUtils {

    private static String ALGORITHM = "AES";

    private static String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static String IV_PARAMETER_SPE = "0123456789ABCEDF";

    public static String encryptByAES(String input, String key) throws Exception {

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec sks = new SecretKeySpec(key.getBytes(), ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(IV_PARAMETER_SPE.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE,sks,iv);
        byte[] bytes = cipher.doFinal(input.getBytes());
        return bytesToHexString(bytes);
    }

    public static String decryptByAES(String input, String key) throws Exception {

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec sks = new SecretKeySpec(key.getBytes(), ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(IV_PARAMETER_SPE.getBytes());
        cipher.init(Cipher.DECRYPT_MODE,sks,iv);
        byte [] inputBytes = hexStringToBytes(input);
        byte[] bytes = cipher.doFinal(inputBytes);
        return new String(bytes);
    }

    private static byte[] hexStringToBytes(String hexString) {
        if (hexString.length() % 2 != 0) {
            throw new DataVinesException("string length not valid");
        }

        int length = hexString.length() / 2;
        byte[] resultBytes = new byte[length];
        for (int index = 0; index < length; index++) {
            String result = hexString.substring(index * 2, index * 2 + 2);
            resultBytes[index] = Integer.valueOf(Integer.parseInt(result, 16)).byteValue();
        }
        return resultBytes;
    }

    private static String bytesToHexString(byte[] sources) {
        if (sources == null) {
            return null;
        }

        StringBuilder stringBuffer = new StringBuilder();
        for (byte source : sources) {
            String result = Integer.toHexString(source& 0xff);
            if (result.length() < 2) {
                result = "0" + result;
            }
            stringBuffer.append(result);
        }

        return stringBuffer.toString();
    }
}
