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

package org.apache.inlong.dataproxy.source;

public class ConfStringUtils {

    /**
     * Check ip format
     *
     * @param ip ip
     * @return Boolean
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        boolean b = false;
        ip = ip.trim();
        if (ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            String[] s = ip.split("\\.");
            if (Integer.parseInt(s[0]) < 255) {
                if (Integer.parseInt(s[1]) < 255) {
                    if (Integer.parseInt(s[2]) < 255) {
                        if (Integer.parseInt(s[3]) < 255) {
                            b = true;
                        }
                    }
                }
            }
        }
        return b;
    }

    /**
     * Check ip port
     *
     * @param port Port
     * @return Boolean
     */
    public static boolean isValidPort(int port) {
        if (port < 0 || port > 65535) {
            return false;
        }
        return true;
    }
}
