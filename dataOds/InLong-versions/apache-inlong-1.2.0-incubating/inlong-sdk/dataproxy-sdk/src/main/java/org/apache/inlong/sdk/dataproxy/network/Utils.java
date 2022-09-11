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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.network;

import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static String userIp;

    static {
        userIp = getLocalIp();
    }

    public static String getLocalIp() {
        if (userIp != null) {
            return userIp;
        }
        String ip = "127.0.0.1";
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (Exception ignored) {
            logger.warn("getLocalIp ", ignored);
        }
        userIp = ip;
        return ip;
    }

    public static boolean validLocalIp(String currLocalHost) throws ProxysdkException {
        String ip = "127.0.0.1";
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (Exception ex) {
            logger.error("error while get local ip", ex);
        }
        if (!ip.equals(currLocalHost)) {
            logger.warn("ip is not equal {} {}", currLocalHost, ip);
        }
        userIp = ip;
        return true;
    }

    public static String getMD5String(String source) {
        final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        String retString = null;

        if (source == null) {
            return retString;
        }

        try {
            StringBuilder sb = new StringBuilder();
            MessageDigest md = MessageDigest.getInstance("MD5");
            try {
                md.update(source.getBytes("utf8"), 0, source.length());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            byte[] retBytes = md.digest();
            for (byte b : retBytes) {
                sb.append(hexDigits[(b >> 4) & 0x0f]);
                sb.append(hexDigits[b & 0x0f]);
            }

            retString = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("" + e);
        }

        return retString;
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((!Character.isWhitespace(str.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    public static byte[] toBytes(String ipAddr) {
        byte[] ret = new byte[4];
        try {
            String[] ipArr = ipAddr.split("\\.");
            ret[0] = (byte) (Integer.parseInt(ipArr[0]) & 0xFF);
            ret[1] = (byte) (Integer.parseInt(ipArr[1]) & 0xFF);
            ret[2] = (byte) (Integer.parseInt(ipArr[2]) & 0xFF);
            ret[3] = (byte) (Integer.parseInt(ipArr[3]) & 0xFF);
            return ret;
        } catch (Exception e) {
            throw new IllegalArgumentException(ipAddr + " is invalid IP");
        }
    }

    public static int bytesToInt(byte[] bytes) {
        int addr = bytes[3] & 0xFF;
        addr |= ((bytes[2] << 8) & 0xFF00);
        addr |= ((bytes[1] << 16) & 0xFF0000);
        addr |= ((bytes[0] << 24) & 0xFF000000);
        return addr;
    }

    public static String convertListToString(List<String> list, Character ch) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator itr = list.iterator();
        sb.append(itr.next());
        while (itr.hasNext()) {
            sb.append(ch).append(itr.next());
        }
        return sb.toString();
    }

    public static String generateSignature(String secureId, long timestamp, int randomValue, String secureKey) {
        Base64 base64 = new Base64();
        byte[] baseStr = base64.encode(HmacUtils.hmacSha1(secureKey, secureId + timestamp + randomValue));
        String result = "";
        try {
            result = URLEncoder.encode(new String(baseStr), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getAuthorizenInfo(final String secretId, final String secretKey, long timestamp, int nonce) {
        String signature = generateSignature(secretId, timestamp, nonce, secretKey);
        return "manager " + secretId + " " + timestamp + " " + nonce + " " + signature;
    }

    public static String convertSetToString(Set<String> list, Character ch) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator itr = list.iterator();
        sb.append(itr.next());
        while (itr.hasNext()) {
            sb.append(ch).append(itr.next());
        }
        return sb.toString();
    }

}
