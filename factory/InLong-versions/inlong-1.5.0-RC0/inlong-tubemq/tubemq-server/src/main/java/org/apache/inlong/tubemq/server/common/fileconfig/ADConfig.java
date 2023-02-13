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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

public class ADConfig {

    // whether to enable data report by audit sdk
    private boolean auditEnable = false;
    // audit proxy server addresses
    private HashSet<String> auditProxyAddrSet =
            new HashSet<>(Arrays.asList("127.0.0.1:10081"));
    // file path for audit cache data
    private String auditCacheFilePath = "/data/inlong/audit";
    // max cache records for audit cache
    private int auditCacheMaxRows = 2000000;
    // audit id for production
    private int auditIdProduce = 9;
    // audit id for consumption
    private int auditIdConsume = 10;

    public ADConfig() {

    }

    public boolean isAuditEnable() {
        return auditEnable;
    }

    public void setAuditEnable(boolean auditEnable) {
        this.auditEnable = auditEnable;
    }

    public HashSet<String> getAuditProxyAddrSet() {
        return auditProxyAddrSet;
    }

    public void setAuditProxyAddrSet(List<String> auditProxyAddrs) {
        if (auditProxyAddrs == null || auditProxyAddrs.isEmpty()) {
            return;
        }
        this.auditProxyAddrSet.clear();
        for (String addrItem : auditProxyAddrs) {
            if (TStringUtils.isEmpty(addrItem)) {
                continue;
            }
            this.auditProxyAddrSet.add(addrItem);
        }
    }

    public String getAuditCacheFilePath() {
        return auditCacheFilePath;
    }

    public void setAuditCacheFilePath(String auditCacheFilePath) {
        this.auditCacheFilePath = auditCacheFilePath;
    }

    public int getAuditCacheMaxRows() {
        return auditCacheMaxRows;
    }

    public void setAuditCacheMaxRows(int auditCacheMaxRows) {
        this.auditCacheMaxRows = auditCacheMaxRows;
    }

    public int getAuditIdProduce() {
        return auditIdProduce;
    }

    public void setAuditIdProduce(int auditIdProduce) {
        this.auditIdProduce = auditIdProduce;
    }

    public int getAuditIdConsume() {
        return auditIdConsume;
    }

    public void setAuditIdConsume(int auditIdConsume) {
        this.auditIdConsume = auditIdConsume;
    }

    public String toString() {
        return new StringBuilder(512)
                .append("\"ADConfig\":{\"auditEnable\":").append(auditEnable)
                .append(",\"auditProxyAddr\":\"").append(auditProxyAddrSet)
                .append("\",\"auditCacheFilePath\":\"").append(auditCacheFilePath)
                .append("\",\"auditCacheMaxRows\":").append(auditCacheMaxRows)
                .append(",\"auditIdProduce\":").append(auditIdProduce)
                .append(",\"auditIdConsume\":").append(auditIdConsume)
                .append("}").toString();
    }
}
