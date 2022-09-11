/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb.network;

import java.net.InetSocketAddress;

import org.apache.commons.lang.math.NumberUtils;

/**
 * IPPort
 */
public class IpPort {

    public final String ip;
    public final int port;
    public final String key;
    public final InetSocketAddress addr;

    /**
     * Constructor
     */
    public IpPort() {
        this("127.0.0.1", 0);
    }

    /**
     * Constructor
     * 
     * @param ip
     * @param port
     */
    public IpPort(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.key = getIpPortKey(ip, port);
        this.addr = new InetSocketAddress(ip, port);
    }

    /**
     * Constructor
     * 
     * @param addr
     */
    public IpPort(InetSocketAddress addr) {
        this.ip = addr.getHostName();
        this.port = addr.getPort();
        this.key = getIpPortKey(ip, port);
        this.addr = addr;
    }

    /**
     * getIpPortKey
     * 
     * @param  ip
     * @param  port
     * @return
     */
    public static String getIpPortKey(String ip, int port) {
        return ip + ":" + port;
    }

    /**
     * hashCode
     * 
     * @return
     */
    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + port;
        return result;
    }

    /**
     * equals
     * 
     * @param  o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!(o instanceof IpPort)) {
            return false;
        }

        try {
            IpPort ctp = (IpPort) o;
            if (ip != null && ip.equals(ctp.ip) && port == ctp.port) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * parseIpPort
     * 
     * @param  ipPort
     * @return
     */
    public static IpPort parseIpPort(String ipPort) {
        String[] splits = ipPort.split(":");
        if (splits.length == 2) {
            String strIp = splits[0];
            String strPort = splits[1];
            int port = NumberUtils.toInt(strPort, 0);
            if (port > 0) {
                return new IpPort(strIp, port);
            }
        }
        return null;
    }

    /**
     * toString
     * 
     * @return
     */
    public String toString() {
        return key;
    }
}
