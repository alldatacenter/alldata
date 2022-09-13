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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IpUtils
 */
public class IpUtils {

    public static final Logger LOG = LoggerFactory.getLogger(IpUtils.class);

    private static String localIPAddress = null;
    private static byte[] localIPAddressBytes = null;

    /**
     * getLocalAddress
     * 
     * @return
     *
     * @throws Exception
     */
    public static String getLocalAddress() {
        if (localIPAddress != null) {
            return localIPAddress;
        }

        try {
            /* loop to Ethernet cards, find a valid ip */
            final Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            InetAddress ipv6Address = null;
            while (enumeration.hasMoreElements()) {
                final NetworkInterface networkInterface = enumeration.nextElement();
                final Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                while (en.hasMoreElements()) {
                    final InetAddress address = en.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (address instanceof Inet6Address) {
                            /* return ipv6 if no ipv4 */
                            ipv6Address = address;
                            return normalizeHostAddress(ipv6Address);
                        } else {
                            /* use ipv4 first */
                            byte[] ipBytes = address.getAddress();
                            if (ipBytes.length > 0) {
                                int firstByte = ipBytes[0];
                                if ((firstByte > 0 && firstByte <= 30) || firstByte == 100) {
                                    return normalizeHostAddress(address);
                                }
                            }
                        }
                    }

                }

            }

            final InetAddress localHost = InetAddress.getLocalHost();
            localIPAddress = normalizeHostAddress(localHost);
            return localIPAddress;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return "127.0.0.1";
        }
    }

    /**
     * getLocalAddressBytes
     * 
     * @return
     *
     * @throws Exception
     */
    public static byte[] getLocalAddressBytes() throws Exception {
        if (localIPAddressBytes != null) {
            return localIPAddressBytes;
        }

        /* loop to Ethernet cards, find a valid ip */
        final Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        InetAddress ipv6Address = null;
        while (enumeration.hasMoreElements()) {
            final NetworkInterface networkInterface = enumeration.nextElement();
            final Enumeration<InetAddress> en = networkInterface.getInetAddresses();
            while (en.hasMoreElements()) {
                final InetAddress address = en.nextElement();
                if (!address.isLoopbackAddress()) {
                    if (address instanceof Inet6Address) {
                        ipv6Address = address;
                        /* return ipv6 if no ipv4 */
                        return ipv6Address.getAddress();
                    } else {
                        /* use ipv4 first */
                        byte[] ipBytes = address.getAddress();
                        if (ipBytes.length > 0) {
                            int firstByte = ipBytes[0];
                            if ((firstByte > 0 && firstByte <= 30) || firstByte == 100) {
                                return ipBytes;
                            }
                        }
                    }
                }
            }
        }

        final InetAddress localHost = InetAddress.getLocalHost();
        localIPAddressBytes = localHost.getAddress();
        return localIPAddressBytes;
    }

    /**
     * normalizeHostAddress
     * 
     * @param  localHost
     * @return
     */
    public static String normalizeHostAddress(final InetAddress localHost) {
        if (localHost instanceof Inet6Address) {
            return "[" + localHost.getHostAddress() + "]";
        } else {
            return localHost.getHostAddress();
        }
    }

    /**
     * toBytes
     * 
     * @param  ipAddr
     * @return
     */
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

    /**
     * bytesToInt
     * 
     * @param  bytes
     * @return
     */
    public static int bytesToInt(byte[] bytes) {
        int addr = bytes[3] & 0xFF;
        addr |= ((bytes[2] << 8) & 0xFF00);
        addr |= ((bytes[1] << 16) & 0xFF0000);
        addr |= ((bytes[0] << 24) & 0xFF000000);
        return addr;
    }

    /**
     * ipToInt
     * 
     * @param  ipAddr
     * @return
     */
    public static int ipToInt(String ipAddr) {
        try {
            return bytesToInt(toBytes(ipAddr));
        } catch (Exception e) {
            throw new IllegalArgumentException(ipAddr + " is invalid IP");
        }
    }

    /**
     * intToIp
     * 
     * @param  ipInt
     * @return
     */
    public static String intToIp(int ipInt) {
        return new StringBuilder().append(((ipInt >> 24) & 0xff)).append('.').append((ipInt >> 16) & 0xff).append('.')
                .append((ipInt >> 8) & 0xff).append('.').append((ipInt & 0xff)).toString();
    }
}