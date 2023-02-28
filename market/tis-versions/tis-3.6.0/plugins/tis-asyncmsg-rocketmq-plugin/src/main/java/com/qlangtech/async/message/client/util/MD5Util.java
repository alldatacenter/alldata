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
package com.qlangtech.async.message.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
 * Created by IntelliJ IDEA.
 * User: peltason
 * Date: 12-5-9
 * Time: 上午11:08
 * To change this template use File | Settings | File Templates.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MD5Util {

    private static final Logger logger = LoggerFactory.getLogger(MD5Util.class);

    private static final int LO_BYTE = 0x0f;

    private static final int MOVE_BIT = 4;

    private static final int HI_BYTE = 0xf0;

    private static final String[] HEX_DIGITS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    private static String byteArrayToHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            buf.append(byteToHexString(b[i]));
        }
        return buf.toString();
    }

    private static String byteToHexString(byte b) {
        return HEX_DIGITS[(b & HI_BYTE) >> MOVE_BIT] + HEX_DIGITS[b & LO_BYTE];
    }

    /**
     * md5
     *
     * @param origin
     * @return
     */
    public static String MD5(String origin) {
        if (origin == null) {
            // $NON-NLS-1$
            throw new IllegalArgumentException(("MULTI_000523"));
        }
        String resultString = null;
        resultString = new String(origin);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            // logger.error("Error in MD5Util.md5:" + e);
            e.printStackTrace();
        }
        return resultString;
    }

    /**
     * hmac 加密
     * @param data
     * @param secret
     * @return
     * @throws IOException
     */
    public static String hmac(String data, String secret) throws IOException {
        byte[] bytes = null;
        try {
            SecretKey secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacMD5");
            Mac mac = Mac.getInstance(secretKey.getAlgorithm());
            mac.init(secretKey);
            bytes = mac.doFinal(data.getBytes("UTF-8"));
        } catch (GeneralSecurityException e) {
            logger.error("Error in MD5Util.md5:" + e);
        }
        return byte2hex(bytes);
    }

    /**
     *  把二进制转化为大写的十六进制
     *
     * @param bytes
     * @return
     */
    private static String byte2hex(byte[] bytes) {
        StringBuilder sign = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sign.append("0");
            }
            sign.append(hex.toUpperCase());
        }
        return sign.toString();
    }

    public static boolean stringIsEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static void main(String[] args) {
        try {
            System.out.println(MD5("的是非得失法"));
            System.out.println(hmac("secret", "admin"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
