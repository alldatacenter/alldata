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

package org.apache.inlong.tubemq.corebase.utils;

import io.netty.channel.Channel;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.apache.inlong.tubemq.corebase.exception.AddressException;

public class AddressUtils {

    private static String localIPAddress = null;

    public static synchronized String getLocalAddress() throws Exception {
        if (TStringUtils.isBlank(localIPAddress)) {
            throw new Exception("Local IP is blank, Please initial Client's Configure first!");
        }
        return localIPAddress;
    }

    @Deprecated
    public static boolean validLocalIp(String currLocalHost) {
        if (TStringUtils.isNotEmpty(localIPAddress)
                && localIPAddress.equals(currLocalHost)) {
            return true;
        }

        Enumeration<NetworkInterface> allInterface = listNetworkInterface();

        return checkValidIp(allInterface, currLocalHost);
    }

    private static boolean checkValidIp(Enumeration<NetworkInterface> allInterface,
            String currLocalHost) {
        String fstV4IP = null;
        while (allInterface.hasMoreElements()) {
            try {
                Tuple2<Boolean, String> result =
                        getValidIPV4Address(allInterface.nextElement(), currLocalHost);
                if (result.getF0()) {
                    localIPAddress = currLocalHost;
                    return true;
                }
                if (TStringUtils.isEmpty(fstV4IP)) {
                    fstV4IP = result.getF1();
                }
            } catch (Throwable e) {
                //
            }
        }
        if (fstV4IP != null) {
            localIPAddress = fstV4IP;
            return true;
        }
        throw new AddressException(new StringBuilder(256)
                .append("Illegal parameter: not found the ip(").append(currLocalHost)
                .append(") or ip v4 address in local networkInterfaces!").toString());
    }

    public static int ipToInt(String ipAddr) {
        try {
            return bytesToInt(toBytes(ipAddr));
        } catch (Exception e) {
            throw new IllegalArgumentException(ipAddr + " is invalid IP");
        }
    }

    public static String intToIp(int ipInt) {
        return new StringBuilder(128).append(((ipInt >> 24) & 0xff)).append('.')
                .append((ipInt >> 16) & 0xff).append('.').append((ipInt >> 8) & 0xff).append('.')
                .append((ipInt & 0xff)).toString();
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

    public static String getRemoteAddressIP(Channel channel) {
        String strRemoteIP = null;
        if (channel == null) {
            return strRemoteIP;
        }
        SocketAddress remoteSocketAddress = channel.remoteAddress();
        if (null != remoteSocketAddress) {
            strRemoteIP = remoteSocketAddress.toString();
            try {
                strRemoteIP = strRemoteIP.substring(1, strRemoteIP.indexOf(':'));
            } catch (Exception ee) {
                return strRemoteIP;
            }
        }
        return strRemoteIP;
    }

    public static Enumeration<NetworkInterface> listNetworkInterface() {
        try {
            return NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new AddressException(e);
        }
    }

    /**
     * Get local IP V4 address
     *
     * @return  the local IP V4 address
     */
    public static String getIPV4LocalAddress() {
        if (localIPAddress != null) {
            return localIPAddress;
        }
        String tmpAdress = null;
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            if (enumeration == null) {
                throw new AddressException("Get NetworkInterfaces is null");
            }
            while (enumeration.hasMoreElements()) {
                try {
                    Tuple2<Boolean, String> result =
                            getValidIPV4Address(enumeration.nextElement(), null);
                    if (result.getF0()) {
                        tmpAdress = result.getF1();
                        break;
                    }
                } catch (Throwable e) {
                    //
                }
            }
            if (tmpAdress == null) {
                tmpAdress = InetAddress.getLocalHost().getHostAddress();
            }
            if (tmpAdress != null) {
                localIPAddress = tmpAdress;
                return tmpAdress;
            }
        } catch (SocketException | UnknownHostException e) {
            throw new AddressException("Call getIPV4LocalAddress throw exception", e);
        }
        throw new AddressException(new StringBuilder(256)
                .append("Illegal parameter: not found the default ip")
                .append(" in local networkInterfaces!").toString());
    }

    /**
     * Get the local IP V4 address from the designated NetworkInterface
     *
     * @return  the local IP V4 address
     */
    public static String getIPV4LocalAddress(String defEthName) {
        boolean foundNetInter = false;
        try {
            Enumeration<NetworkInterface> enumeration =
                    NetworkInterface.getNetworkInterfaces();
            if (enumeration == null) {
                throw new AddressException("Get NetworkInterfaces is null");
            }
            while (enumeration.hasMoreElements()) {
                NetworkInterface oneInterface = enumeration.nextElement();
                if (oneInterface == null
                        || oneInterface.isLoopback()
                        || !oneInterface.isUp()
                        || !defEthName.equalsIgnoreCase(oneInterface.getName())) {
                    continue;
                }
                foundNetInter = true;
                try {
                    Tuple2<Boolean, String> result =
                            getValidIPV4Address(oneInterface, null);
                    if (result.getF0()) {
                        localIPAddress = result.getF1();
                        return localIPAddress;
                    }
                } catch (Throwable e) {
                    //
                }
            }
        } catch (Throwable e) {
            throw new AddressException("Call getDefNetworkAddress throw exception", e);
        }
        if (foundNetInter) {
            throw new AddressException(new StringBuilder(256)
                    .append("Illegal parameter: not found valid ip")
                    .append(" in networkInterfaces ").append(defEthName).toString());
        } else {
            throw new AddressException(new StringBuilder(256)
                    .append("Illegal parameter: ").append(defEthName)
                    .append(" does not exist or is not in a valid state!").toString());
        }
    }

    /**
     * get valid IPV4 address from networkInterface.
     *
     * @param networkInterface need check networkInterface
     * @param checkIp The IP address to be searched,
     *                if not specified, set to null
     * @return Search result, field 0 indicates whether it is successful,
     *                        field 1 carries the matched IP value;
     *                        if the checkIp is specified but not found the IP,
     *                        field 1 will return the first IPV4 address
     * @throws AddressException throw exception if found no ipv4 address
     */
    public static Tuple2<Boolean, String> getValidIPV4Address(
            NetworkInterface networkInterface, String checkIp) {
        try {
            if (networkInterface == null
                    || !networkInterface.isUp()
                    || networkInterface.isLoopback()
                    || "docker0".equals(networkInterface.getName())) {
                return new Tuple2<>(false, null);
            }
            String fstV4IP = null;
            Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress address = addrs.nextElement();
                if (address == null
                        || address.isLoopbackAddress()
                        || address instanceof Inet6Address) {
                    continue;
                }
                String localIP = address.getHostAddress();
                if (TStringUtils.isEmpty(localIP)
                        || localIP.startsWith("127.0")) {
                    continue;
                }
                if (!TStringUtils.isEmpty(checkIp)) {
                    if (TStringUtils.isEmpty(fstV4IP)) {
                        fstV4IP = localIP;
                    }
                    if (localIP.equals(checkIp)) {
                        return new Tuple2<>(true, localIP);
                    }
                    continue;
                }
                return new Tuple2<>(true, localIP);
            }
            return new Tuple2<>(false, fstV4IP);
        } catch (Throwable e) {
            throw new AddressException(new StringBuilder(256)
                    .append("Illegal parameter: ")
                    .append("unable to obtain valid IP from network card ")
                    .append(networkInterface).toString(), e);
        }
    }

}
