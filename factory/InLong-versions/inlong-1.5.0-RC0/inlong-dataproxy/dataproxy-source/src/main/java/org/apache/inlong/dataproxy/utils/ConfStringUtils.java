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

public class ConfStringUtils {

    /**
     * isValidIp
     * @param ip
     * @return
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        if (ip.equals("localhost")) {
            ip = "127.0.0.1";
        }
        boolean b = false;
        ip = ip.trim();
        if (ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            String[] s = ip.split("\\.");
            int number0 = Integer.parseInt(s[0]);
            int number1 = Integer.parseInt(s[1]);
            int number2 = Integer.parseInt(s[2]);
            int number3 = Integer.parseInt(s[3]);
            if (number0 >= 0 && number0 <= 255) {
                if (number1 >= 0 && number1 <= 255) {
                    if (number2 >= 0 && number2 <= 255) {
                        if (number3 >= 0 && number3 <= 255) {
                            b = true;
                        }
                    }
                }
            }
        }
        return b;
    }

    /**
     * isValidPort
     * @param port
     * @return
     */
    public static boolean isValidPort(int port) {
        if (port < 0 || port > 65535) {
            return false;
        }
        return true;
    }
}
