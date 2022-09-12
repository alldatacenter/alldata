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

package org.apache.inlong.sdk.dataproxy.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.network.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProxyUtils.class);
    private static final int TIME_LENGTH = 13;
    private static final Set<String> invalidAttr = new HashSet<>();

    static {
        Collections.addAll(invalidAttr, "groupId", "streamId", "dt", "msgUUID", "cp",
            "cnt", "mt", "m", "sid", "t", "NodeIP", "messageId", "_file_status_check", "_secretId",
            "_signature", "_timeStamp", "_nonce", "_userName", "_clientIP", "_encyVersion", "_encyDesKey");
    }

    public static boolean isAttrKeysValid(Map<String, String> attrsMap) {
        if (attrsMap == null || attrsMap.size() == 0) {
            return false;
        }
        for (String key : attrsMap.keySet()) {
            if (invalidAttr.contains(key)) {
                logger.error("the attributes is invalid ,please check ! {}", key);
                return false;
            }
        }
        return true;
    }

    public static boolean isDtValid(long dt) {
        if (String.valueOf(dt).length() != TIME_LENGTH) {
            logger.error("dt {} is error", dt);
            return false;
        }
        return true;
    }

    /**
     * check body valid
     *
     * @param body
     * @return
     */
    public static boolean isBodyValid(byte[] body) {
        if (body == null || body.length == 0) {
            logger.error("body is error {}", body);
            return false;
        }
        return true;
    }

    /**
     * check body valid
     *
     * @param bodyList
     * @return
     */
    public static boolean isBodyValid(List<byte[]> bodyList) {
        if (bodyList == null || bodyList.size() == 0) {
            logger.error("body  is error");
            return false;
        }
        return true;
    }

    public static long covertZeroDt(long dt) {
        if (dt == 0) {
            return System.currentTimeMillis();
        }
        return dt;
    }

    public static StringBuilder convertAttrToStr(Map<String, String> extraAttrMap) {
        StringBuilder attrs = new StringBuilder();
        for (Map.Entry<String, String> entry : extraAttrMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            attrs.append(key).append("=");
            attrs.append(value).append("&");
        }
        attrs.deleteCharAt(attrs.length() - 1);
        return attrs;
    }

    /**
     * valid client config
     *
     * @param clientConfig
     */
    public static void validClientConfig(ProxyClientConfig clientConfig) {
        if (clientConfig.isNeedAuthentication()) {
            if (Utils.isBlank(clientConfig.getUserName())) {
                throw new IllegalArgumentException("Authentication require userName not Blank!");
            }
            if (Utils.isBlank(clientConfig.getSecretKey())) {
                throw new IllegalArgumentException("Authentication require secretKey not Blank!");
            }
        }
        if (!clientConfig.isLocalVisit()) {
            //if(!clientConfig.isNeedDataEncry()) {
            //    throw new IllegalArgumentException("OutNetwork visit isNeedDataEncry must be true!");
            //}
            if (!clientConfig.isNeedAuthentication()) {
                throw new IllegalArgumentException("OutNetwork visit isNeedAuthentication must be true!");
            }
            if (Utils.isBlank(clientConfig.getUserName())) {
                throw new IllegalArgumentException("Authentication require userName not Blank!");
            }
            if (Utils.isBlank(clientConfig.getSecretKey())) {
                throw new IllegalArgumentException("Authentication require secretKey not Blank!");
            }
            if (!clientConfig.isNeedVerServer()) {
                throw new IllegalArgumentException("OutNetwork visit need https, please set https parameters!");
            }
            if (Utils.isBlank(clientConfig.getTlsServerCertFilePathAndName())) {
                throw new IllegalArgumentException("OutNetwork visit need https, "
                        + "TlsServerCertFilePathAndName is Blank!");
            }
            if (Utils.isBlank(clientConfig.getTlsServerKey())) {
                throw new IllegalArgumentException("OutNetwork visit need https, tlsServerKey is Blank!");
            }
        }
    }
}
