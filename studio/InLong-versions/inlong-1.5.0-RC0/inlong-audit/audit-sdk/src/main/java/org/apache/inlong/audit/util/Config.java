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

package org.apache.inlong.audit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private String localIP = "";
    private String dockerId = "";
    private static final int CGROUP_FILE_LENGTH = 50;
    private static final int DOCKERID_LENGTH = 10;

    public void init() {
        initIP();
        initDockerId();
    }

    public String getLocalIP() {
        return localIP;
    }

    public String getDockerId() {
        return dockerId;
    }

    private void initIP() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                String name = intf.getName();
                if (!name.contains("docker") && !name.contains("lo")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            String ipaddress = inetAddress.getHostAddress();
                            if (!ipaddress.contains("::") && !ipaddress.contains("0:0:")
                                    && !ipaddress.contains("fe80")) {
                                localIP = ipaddress;
                            }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            localIP = "127.0.0.1";
            return;
        }
    }

    private void initDockerId() {
        File file = new File("/proc/self/cgroup");
        if (!file.exists()) {
            return;
        }
        try (BufferedReader in = new BufferedReader(new FileReader("/proc/self/cgroup"))) {
            String dockerID = in.readLine();
            if (dockerID == null || dockerID.length() < CGROUP_FILE_LENGTH) {
                in.close();
                return;
            }
            dockerId = dockerID.substring(dockerID.length() - DOCKERID_LENGTH);
            in.close();
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }
}
