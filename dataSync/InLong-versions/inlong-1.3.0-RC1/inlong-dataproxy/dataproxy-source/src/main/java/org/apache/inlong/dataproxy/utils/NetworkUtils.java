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

package org.apache.inlong.dataproxy.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {
    public static String INNER_NETWORK_INTERFACE = "eth1";

    private static String localIp = null;

    static {
        localIp = getLocalIp();
    }

    /**
     * get local ip
     * @return
     */
    public static String getLocalIp() {
        if (localIp == null) {
            String ip = null;
            Enumeration<NetworkInterface> allInterface;
            try {
                allInterface = NetworkInterface.getNetworkInterfaces();
                for (; allInterface.hasMoreElements(); ) {
                    NetworkInterface oneInterface = allInterface.nextElement();
                    String interfaceName = oneInterface.getName();
                    if (oneInterface.isLoopback()
                            || !oneInterface.isUp()
                            || !interfaceName
                            .equalsIgnoreCase(INNER_NETWORK_INTERFACE)) {
                        continue;
                    }

                    Enumeration<InetAddress> allAddress = oneInterface
                            .getInetAddresses();
                    for (; allAddress.hasMoreElements(); ) {
                        InetAddress oneAddress = allAddress.nextElement();
                        ip = oneAddress.getHostAddress();
                        if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1")) {
                            continue;
                        }
                        return ip;
                    }
                }
            } catch (SocketException e1) {
                // ignore it
            }

            ip = "0.0.0.0";
            localIp = ip;
        }

        return localIp;
    }
}
