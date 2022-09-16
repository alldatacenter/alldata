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

package org.apache.inlong.tubemq.server.common.utils;

import java.util.HashSet;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;

public class ClientSyncInfo {
    private boolean updated = false;
    private long brokerConfigId = TBaseConstants.META_VALUE_UNDEFINED;
    private long topicMetaInfoId = TBaseConstants.META_VALUE_UNDEFINED;
    private long lstAssignedTime = TBaseConstants.META_VALUE_UNDEFINED;
    private boolean repPartInfo = false;
    private final Set<Partition> subPartSet = new HashSet<>();

    public ClientSyncInfo() {

    }

    /**
     * Update the client reported subscription information
     *
     * @param brokerRunManager   the broker run-manager
     * @param clientSubRepInfo   the client reported subscription information
     */
    public void updSubRepInfo(BrokerRunManager brokerRunManager,
                              ClientMaster.ClientSubRepInfo clientSubRepInfo) {
        if (clientSubRepInfo == null) {
            return;
        }
        if (clientSubRepInfo.hasBrokerConfigId()) {
            this.brokerConfigId = clientSubRepInfo.getBrokerConfigId();
            this.updated = true;
        }
        if (clientSubRepInfo.hasTopicMetaInfoId()) {
            this.topicMetaInfoId = clientSubRepInfo.getTopicMetaInfoId();
            this.updated = true;
        }
        if (clientSubRepInfo.hasLstAssignedTime()) {
            this.lstAssignedTime = clientSubRepInfo.getLstAssignedTime();
            this.updated = true;
        }
        if (clientSubRepInfo.hasReportSubInfo()) {
            this.repPartInfo = clientSubRepInfo.getReportSubInfo();
            if (this.repPartInfo) {
                for (String info : clientSubRepInfo.getPartSubInfoList()) {
                    if (TStringUtils.isBlank(info)) {
                        continue;
                    }
                    String[] strInfo = info.split(TokenConstants.SEGMENT_SEP);
                    String[] strPartInfoSet = strInfo[1].split(TokenConstants.ARRAY_SEP);
                    for (String partStr : strPartInfoSet) {
                        String[] strPartInfo = partStr.split(TokenConstants.ATTR_SEP);
                        BrokerInfo brokerInfo =
                                brokerRunManager.getBrokerInfo(Integer.parseInt(strPartInfo[0]));
                        if (brokerInfo == null) {
                            continue;
                        }
                        subPartSet.add(new Partition(brokerInfo,
                                strInfo[0], Integer.parseInt(strPartInfo[1])));
                    }
                }
            }
            this.updated = true;
        }
    }

    public boolean isUpdated() {
        return updated;
    }

    public long getBrokerConfigId() {
        return brokerConfigId;
    }

    public long getTopicMetaInfoId() {
        return topicMetaInfoId;
    }

    public long getLstAssignedTime() {
        return lstAssignedTime;
    }

    public Tuple2<Boolean, Set<Partition>> getRepSubInfo() {
        return new Tuple2<>(this.repPartInfo, this.subPartSet);
    }

    public void clear() {
        this.updated = false;
        this.brokerConfigId = TBaseConstants.META_VALUE_UNDEFINED;
        this.topicMetaInfoId = TBaseConstants.META_VALUE_UNDEFINED;
        this.lstAssignedTime = TBaseConstants.META_VALUE_UNDEFINED;
        this.repPartInfo = false;
        this.subPartSet.clear();
    }
}
