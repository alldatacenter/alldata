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

package org.apache.inlong.sort.base.metric;

import org.apache.flink.util.Preconditions;
import org.apache.flink.util.StringUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.apache.inlong.sort.base.Constants.DELIMITER;
import static org.apache.inlong.sort.base.Constants.GROUP_ID;
import static org.apache.inlong.sort.base.Constants.STREAM_ID;

public class MetricOption implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String IP_OR_HOST_PORT = "^(.*):([0-9]|[1-9]\\d|[1-9]\\d{"
            + "2}|[1-9]\\d{"
            + "3}|[1-5]\\d{"
            + "4}|6[0-4]\\d{"
            + "3}|65[0-4]\\d{"
            + "2}|655[0-2]\\d|6553[0-5])$";

    private Map<String, String> labels;
    private final HashSet<String> ipPortList;
    private String ipPorts;
    private RegisteredMetric registeredMetric;
    private long initRecords;
    private long initBytes;
    private long initDirtyRecords;
    private long initDirtyBytes;
    private long readPhase;

    private MetricOption(
            String inlongLabels,
            @Nullable String inlongAudit,
            RegisteredMetric registeredMetric,
            long initRecords,
            long initBytes,
            Long initDirtyRecords,
            Long initDirtyBytes,
            Long readPhase) {
        Preconditions.checkArgument(!StringUtils.isNullOrWhitespaceOnly(inlongLabels),
                "Inlong labels must be set for register metric.");

        this.initRecords = initRecords;
        this.initBytes = initBytes;
        this.initDirtyRecords = initDirtyRecords;
        this.initDirtyBytes = initDirtyBytes;
        this.readPhase = readPhase;
        this.labels = new LinkedHashMap<>();
        String[] inLongLabelArray = inlongLabels.split(DELIMITER);
        Preconditions.checkArgument(Stream.of(inLongLabelArray).allMatch(label -> label.contains("=")),
                "InLong metric label format must be xxx=xxx");
        Stream.of(inLongLabelArray).forEach(label -> {
            String key = label.substring(0, label.indexOf('='));
            String value = label.substring(label.indexOf('=') + 1);
            labels.put(key, value);
        });

        this.ipPortList = new HashSet<>();
        this.ipPorts = inlongAudit;
        if (ipPorts != null) {
            Preconditions.checkArgument(labels.containsKey(GROUP_ID) && labels.containsKey(STREAM_ID),
                    "groupId and streamId must be set when enable inlong audit collect.");
            String[] ipPortStrs = inlongAudit.split(DELIMITER);
            for (String ipPort : ipPortStrs) {
                Preconditions.checkArgument(Pattern.matches(IP_OR_HOST_PORT, ipPort),
                        "Error inLong audit format: " + inlongAudit);
                this.ipPortList.add(ipPort);
            }
        }

        if (registeredMetric != null) {
            this.registeredMetric = registeredMetric;
        }
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public HashSet<String> getIpPortList() {
        return ipPortList;
    }

    public Optional<String> getIpPorts() {
        return Optional.ofNullable(ipPorts);
    }

    public RegisteredMetric getRegisteredMetric() {
        return registeredMetric;
    }

    public long getInitRecords() {
        return initRecords;
    }

    public long getInitBytes() {
        return initBytes;
    }

    public void setInitRecords(long initRecords) {
        this.initRecords = initRecords;
    }

    public void setInitBytes(long initBytes) {
        this.initBytes = initBytes;
    }

    public long getInitDirtyRecords() {
        return initDirtyRecords;
    }

    public void setInitDirtyRecords(long initDirtyRecords) {
        this.initDirtyRecords = initDirtyRecords;
    }

    public long getInitDirtyBytes() {
        return initDirtyBytes;
    }

    public void setInitDirtyBytes(long initDirtyBytes) {
        this.initDirtyBytes = initDirtyBytes;
    }

    public long getReadPhase() {
        return readPhase;
    }

    public void setReadPhase(long readPhase) {
        this.readPhase = readPhase;
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum RegisteredMetric {
        ALL,
        NORMAL,
        DIRTY
    }

    public static class Builder {

        private String inlongLabels;
        private String inlongAudit;
        private RegisteredMetric registeredMetric = RegisteredMetric.ALL;
        private long initRecords = 0L;
        private long initBytes = 0L;
        private Long initDirtyRecords = 0L;
        private Long initDirtyBytes = 0L;
        private long initReadPhase = 0L;

        private Builder() {
        }

        public MetricOption.Builder withInlongLabels(String inlongLabels) {
            this.inlongLabels = inlongLabels;
            return this;
        }

        public MetricOption.Builder withInlongAudit(String inlongAudit) {
            this.inlongAudit = inlongAudit;
            return this;
        }

        public MetricOption.Builder withRegisterMetric(RegisteredMetric registeredMetric) {
            this.registeredMetric = registeredMetric;
            return this;
        }

        public MetricOption.Builder withInitRecords(long initRecords) {
            this.initRecords = initRecords;
            return this;
        }

        public MetricOption.Builder withInitBytes(long initBytes) {
            this.initBytes = initBytes;
            return this;
        }

        public MetricOption.Builder withInitDirtyRecords(Long initDirtyRecords) {
            this.initDirtyRecords = initDirtyRecords;
            return this;
        }

        public MetricOption.Builder withInitDirtyBytes(Long initDirtyBytes) {
            this.initDirtyBytes = initDirtyBytes;
            return this;
        }

        public MetricOption.Builder withInitReadPhase(Long initReadPhase) {
            this.initReadPhase = initReadPhase;
            return this;
        }

        public MetricOption build() {
            if (inlongLabels == null && inlongAudit == null) {
                return null;
            }
            return new MetricOption(inlongLabels, inlongAudit, registeredMetric, initRecords, initBytes,
                    initDirtyRecords, initDirtyBytes, initReadPhase);
        }
    }
}
