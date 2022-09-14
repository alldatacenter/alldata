/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.inlong.sort.base.metric;

import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.util.ValidateMetricOptionUtils;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.regex.Pattern;

import static org.apache.inlong.sort.base.Constants.DELIMITER;

public class MetricOption {
    private static final String IP_OR_HOST_PORT = "^(.*):([0-9]|[1-9]\\d|[1-9]\\d{"
            + "2}|[1-9]\\d{"
            + "3}|[1-5]\\d{"
            + "4}|6[0-4]\\d{"
            + "3}|65[0-4]\\d{"
            + "2}|655[0-2]\\d|6553[0-5])$";

    private final String groupId;
    private final String streamId;
    private final String nodeId;
    private final HashSet<String> ipPortList;
    private String ipPorts;

    public MetricOption(String inLongMetric) {
        this(inLongMetric, null);
    }

    public MetricOption(String inLongMetric, @Nullable String inLongAudit) {
        ValidateMetricOptionUtils.validateInlongMetricIfSetInlongAudit(inLongMetric, inLongAudit);
        String[] inLongMetricArray = inLongMetric.split(DELIMITER);
        Preconditions.checkArgument(inLongMetricArray.length == 3,
                "Error inLong metric format: " + inLongMetric);
        this.groupId = inLongMetricArray[0];
        this.streamId = inLongMetricArray[1];
        this.nodeId = inLongMetricArray[2];
        this.ipPortList = new HashSet<>();
        this.ipPorts = null;

        if (inLongAudit != null) {
            String[] ipPortStrs = inLongAudit.split(DELIMITER);
            this.ipPorts = inLongAudit;
            for (String ipPort : ipPortStrs) {
                Preconditions.checkArgument(Pattern.matches(IP_OR_HOST_PORT, ipPort),
                        "Error inLong audit format: " + inLongAudit);
                this.ipPortList.add(ipPort);
            }
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public HashSet<String> getIpPortList() {
        return ipPortList;
    }

    public String getIpPorts() {
        return ipPorts;
    }
}
