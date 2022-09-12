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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Utils for Network
 */
@Slf4j
public class NetworkUtils {

    /**
     * Get the real IP of the requesting client
     *
     * @param request HTTP request params from the client
     * @return real IP of the client
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        // get real ip from Nginx proxy
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.isBlank(realIp) || "unknown".equalsIgnoreCase(realIp)) {
            realIp = request.getHeader("X-Forwarded-For");
        }
        if (StringUtils.isBlank(realIp) || "unknown".equalsIgnoreCase(realIp)) {
            realIp = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(realIp) || "unknown".equalsIgnoreCase(realIp)) {
            realIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(realIp) || "unknown".equalsIgnoreCase(realIp)) {
            realIp = request.getRemoteAddr();
        }

        // In the case of multiple proxies, the first IP is the real IP of the client,
        // and multiple IPs are divided according to ','
        if (realIp != null && realIp.length() > 15) { // length of "***.***.***.***"
            if (realIp.indexOf(",") > 0) {
                realIp = realIp.substring(0, realIp.indexOf(","));
            }
        }

        return realIp;
    }

    /**
     * Get local IP
     *
     * @return local IP
     */
    public static String getLocalIp() {
        InetAddress localHost = null;
        try {
            localHost = Inet4Address.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
