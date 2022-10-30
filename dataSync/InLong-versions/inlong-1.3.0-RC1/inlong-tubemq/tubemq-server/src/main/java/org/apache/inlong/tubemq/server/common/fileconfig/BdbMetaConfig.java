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

package org.apache.inlong.tubemq.server.common.fileconfig;

import com.sleepycat.je.Durability;

public class BdbMetaConfig {
    private String repGroupName = "tubemqMasterGroup";
    private String repNodeName;
    private int repNodePort = 9001;
    private String metaDataPath = "var/meta_data";
    private String repHelperHost = "127.0.0.1:9001";
    private int metaLocalSyncPolicy = 1;
    private int metaReplicaSyncPolicy = 3;
    private int repReplicaAckPolicy = 1;
    private long repStatusCheckTimeoutMs = 10000;

    public BdbMetaConfig() {

    }

    public String getRepGroupName() {
        return repGroupName;
    }

    public void setRepGroupName(String repGroupName) {
        this.repGroupName = repGroupName;
    }

    public String getRepNodeName() {
        return repNodeName;
    }

    public void setRepNodeName(String repNodeName) {
        this.repNodeName = repNodeName;
    }

    public int getRepNodePort() {
        return repNodePort;
    }

    public void setRepNodePort(int repNodePort) {
        this.repNodePort = repNodePort;
    }

    public String getMetaDataPath() {
        return metaDataPath;
    }

    public void setMetaDataPath(String metaDataPath) {
        this.metaDataPath = metaDataPath;
    }

    public String getRepHelperHost() {
        return repHelperHost;
    }

    public void setRepHelperHost(String repHelperHost) {
        this.repHelperHost = repHelperHost;
    }

    public Durability.SyncPolicy getMetaLocalSyncPolicy() {
        switch (metaLocalSyncPolicy) {
            case 1:
                return Durability.SyncPolicy.SYNC;
            case 2:
                return Durability.SyncPolicy.NO_SYNC;
            case 3:
                return Durability.SyncPolicy.WRITE_NO_SYNC;
            default:
                return Durability.SyncPolicy.SYNC;
        }
    }

    public void setMetaLocalSyncPolicy(int metaLocalSyncPolicy) {
        this.metaLocalSyncPolicy = metaLocalSyncPolicy;
    }

    public Durability.SyncPolicy getMetaReplicaSyncPolicy() {
        switch (metaReplicaSyncPolicy) {
            case 1:
                return Durability.SyncPolicy.SYNC;
            case 2:
                return Durability.SyncPolicy.NO_SYNC;
            case 3:
                return Durability.SyncPolicy.WRITE_NO_SYNC;
            default:
                return Durability.SyncPolicy.SYNC;
        }
    }

    public void setMetaReplicaSyncPolicy(int metaReplicaSyncPolicy) {
        this.metaReplicaSyncPolicy = metaReplicaSyncPolicy;
    }

    public Durability.ReplicaAckPolicy getRepReplicaAckPolicy() {
        switch (repReplicaAckPolicy) {
            case 1:
                return Durability.ReplicaAckPolicy.SIMPLE_MAJORITY;
            case 2:
                return Durability.ReplicaAckPolicy.ALL;
            case 3:
                return Durability.ReplicaAckPolicy.NONE;
            default:
                return Durability.ReplicaAckPolicy.SIMPLE_MAJORITY;
        }
    }

    public void setRepReplicaAckPolicy(int repReplicaAckPolicy) {
        this.repReplicaAckPolicy = repReplicaAckPolicy;
    }

    public long getRepStatusCheckTimeoutMs() {
        return repStatusCheckTimeoutMs;
    }

    public void setRepStatusCheckTimeoutMs(long repStatusCheckTimeoutMs) {
        this.repStatusCheckTimeoutMs = repStatusCheckTimeoutMs;
    }

    @Override
    public String toString() {
        return new StringBuilder(512)
                .append("\"BdbMetaConfig\":{\"repGroupName\":").append(repGroupName)
                .append("\",\"repNodeName\":\"").append(repNodeName)
                .append("\",\"repNodePort\":").append(repNodePort)
                .append("\",\"metaDataPath\":\"").append(metaDataPath)
                .append("\",\"repHelperHost\":\"").append(repHelperHost)
                .append("\",\"metaLocalSyncPolicy\":").append(metaLocalSyncPolicy)
                .append(",\"metaReplicaSyncPolicy\":").append(metaReplicaSyncPolicy)
                .append(",\"repReplicaAckPolicy\":").append(repReplicaAckPolicy)
                .append(",\"repStatusCheckTimeoutMs\":").append(repStatusCheckTimeoutMs)
                .append("}").toString();
    }
}
