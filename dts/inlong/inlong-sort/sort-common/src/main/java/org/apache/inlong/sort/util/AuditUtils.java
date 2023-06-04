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

package org.apache.inlong.sort.util;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuditUtils {

    public static final String DELIMITER = "&";

    private static final String IP_OR_HOST_PORT = "^(.*):([0-9]|[1-9]\\d|[1-9]\\d{"
            + "2}|[1-9]\\d{"
            + "3}|[1-5]\\d{"
            + "4}|6[0-4]\\d{"
            + "3}|65[0-4]\\d{"
            + "2}|655[0-2]\\d|6553[0-5])$";

    public static HashSet<String> extractAuditIpPorts(String inlongAudit) {
        HashSet<String> ipPortList = new HashSet<>();
        String[] ipPorts = inlongAudit.split(DELIMITER);
        for (String ipPort : ipPorts) {
            Preconditions.checkArgument(Pattern.matches(IP_OR_HOST_PORT, ipPort),
                    "Error inLong audit format: " + inlongAudit);
            ipPortList.add(ipPort);
        }
        return ipPortList;
    }

    public static List<Integer> extractAuditKeys(String auditKeys) {
        return Arrays.stream(auditKeys.split(DELIMITER)).map(Integer::valueOf)
                .collect(Collectors.toList());
    }

}
