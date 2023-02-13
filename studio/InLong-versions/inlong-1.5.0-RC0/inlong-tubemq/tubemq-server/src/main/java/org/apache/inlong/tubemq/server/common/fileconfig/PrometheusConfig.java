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

package org.apache.inlong.tubemq.server.common.fileconfig;

public class PrometheusConfig {

    // Whether to enable metric collection through Prometheus
    private boolean promEnable = false;
    // Prometheus http port
    private int promHttpPort = 9081;
    // snapshot interval
    private long promSnapshotIntvl = 60000L;
    // metric domains
    private String promMetricDomains = "TubeMQ";
    // Cluster name
    private String promClusterName = "InLong";

    public PrometheusConfig() {

    }

    public boolean isPromEnable() {
        return promEnable;
    }

    public void setPromEnable(boolean promEnable) {
        this.promEnable = promEnable;
    }

    public int getPromHttpPort() {
        return promHttpPort;
    }

    public void setPromHttpPort(int promHttpPort) {
        this.promHttpPort = promHttpPort;
    }

    public long getPromSnapshotIntvl() {
        return promSnapshotIntvl;
    }

    public void setPromSnapshotIntvl(long promSnapshotIntvl) {
        this.promSnapshotIntvl = promSnapshotIntvl;
    }

    public String getPromMetricDomains() {
        return promMetricDomains;
    }

    public void setPromMetricDomains(String promMetricDomains) {
        this.promMetricDomains = promMetricDomains;
    }

    public String getPromClusterName() {
        return promClusterName;
    }

    public void setPromClusterName(String promClusterName) {
        this.promClusterName = promClusterName;
    }

    public String toString() {
        return new StringBuilder(512)
                .append("\"PrometheusConfig\":{\"promEnable\":").append(promEnable)
                .append(",\"promClusterName\":\"").append(promClusterName)
                .append("\",\"promMetricDomains\":\"").append(promMetricDomains)
                .append("\",\"promHttpPort\":").append(promHttpPort)
                .append(",\"promSnapshotIntvl\":").append(promSnapshotIntvl)
                .append("}").toString();
    }
}
