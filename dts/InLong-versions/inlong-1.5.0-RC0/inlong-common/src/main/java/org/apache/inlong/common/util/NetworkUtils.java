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

package org.apache.inlong.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtils.class);
    private static String localIp;

    static {
        localIp = getLocalIp();
    }

    /**
     * get local ip
     */
    public static String getLocalIp() {
        if (StringUtils.isNotBlank(localIp)) {
            return localIp;
        }
        Enumeration<NetworkInterface> allInterface;
        String ip = "127.0.0.1";
        try {
            allInterface = NetworkInterface.getNetworkInterfaces();
            while (allInterface.hasMoreElements()) {
                NetworkInterface oneInterface = allInterface.nextElement();
                if (oneInterface.isLoopback() || !oneInterface.isUp() || oneInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> allAddress = oneInterface.getInetAddresses();
                while (allAddress.hasMoreElements()) {
                    InetAddress oneAddress = allAddress.nextElement();
                    localIp = oneAddress.getHostAddress();
                    if (StringUtils.isEmpty(localIp) || localIp.equals("127.0.0.1") || localIp.contains(":")) {
                        continue;
                    }
                    return localIp;
                }
            }
        } catch (SocketException e) {
            LOGGER.error("error while get local ip", e);
        }
        localIp = ip;
        return localIp;
    }
}
