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

import org.apache.inlong.tubemq.server.common.TServerConstants;

public class ZKConfig {

    private String zkServerAddr = "localhost:2181";
    private String zkNodeRoot = "/tubemq";
    private int zkSessionTimeoutMs = 180000;
    private int zkConnectionTimeoutMs = 600000;
    private int zkSyncTimeMs = 1000;
    private long zkCommitPeriodMs = 5000L;
    private int zkCommitFailRetries = TServerConstants.CFG_ZK_COMMIT_DEFAULT_RETRIES;
    private long zkMasterCheckPeriodMs = 5000L;

    public ZKConfig() {

    }

    public String getZkServerAddr() {
        return zkServerAddr;
    }

    public void setZkServerAddr(String zkServerAddr) {
        this.zkServerAddr = zkServerAddr;
    }

    public String getZkNodeRoot() {
        return zkNodeRoot;
    }

    public void setZkNodeRoot(String zkNodeRoot) {
        this.zkNodeRoot = zkNodeRoot;
    }

    public int getZkSessionTimeoutMs() {
        return zkSessionTimeoutMs;
    }

    public void setZkSessionTimeoutMs(int zkSessionTimeoutMs) {
        this.zkSessionTimeoutMs = zkSessionTimeoutMs;
    }

    public int getZkConnectionTimeoutMs() {
        return zkConnectionTimeoutMs;
    }

    public void setZkConnectionTimeoutMs(int zkConnectionTimeoutMs) {
        this.zkConnectionTimeoutMs = zkConnectionTimeoutMs;
    }

    public int getZkSyncTimeMs() {
        return zkSyncTimeMs;
    }

    public void setZkSyncTimeMs(int zkSyncTimeMs) {
        this.zkSyncTimeMs = zkSyncTimeMs;
    }

    public int getZkCommitFailRetries() {
        return zkCommitFailRetries;
    }

    public void setZkCommitFailRetries(int zkCommitFailRetries) {
        this.zkCommitFailRetries = zkCommitFailRetries;
    }

    public long getZkCommitPeriodMs() {
        return zkCommitPeriodMs;
    }

    public void setZkCommitPeriodMs(long zkCommitPeriodMs) {
        this.zkCommitPeriodMs = zkCommitPeriodMs;
    }

    public long getZkMasterCheckPeriodMs() {
        return zkMasterCheckPeriodMs;
    }

    public void setZkMasterCheckPeriodMs(long zkMasterCheckPeriodMs) {
        this.zkMasterCheckPeriodMs = zkMasterCheckPeriodMs;
    }

    @Override
    public String toString() {
        return new StringBuilder(512)
                .append("\"ZKConfig\":{\"zkServerAddr\":\"").append(zkServerAddr)
                .append("\",\"zkNodeRoot\":\"").append(zkNodeRoot)
                .append("\",\"zkSessionTimeoutMs\":").append(zkSessionTimeoutMs)
                .append(",\"zkConnectionTimeoutMs\":").append(zkConnectionTimeoutMs)
                .append(",\"zkSyncTimeMs\":").append(zkSyncTimeMs)
                .append(",\"zkCommitPeriodMs\":").append(zkCommitPeriodMs)
                .append(",\"zkCommitFailRetries\":").append(zkCommitFailRetries)
                .append(",\"zkMasterCheckPeriodMs\":").append(zkMasterCheckPeriodMs)
                .append("}").toString();
    }
}
