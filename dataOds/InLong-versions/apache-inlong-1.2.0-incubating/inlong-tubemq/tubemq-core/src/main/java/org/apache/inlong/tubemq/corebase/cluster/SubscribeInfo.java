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

package org.apache.inlong.tubemq.corebase.cluster;

import org.apache.inlong.tubemq.corebase.TokenConstants;

public class SubscribeInfo {

    private String consumerId;
    private String group;
    private Partition partition;
    private boolean overTLS = false;
    private String fullInfo;

    public SubscribeInfo(String strSubInfo) {
        String strConsumerInfo = strSubInfo.split(TokenConstants.SEGMENT_SEP)[0];
        String strBrokerInfo = strSubInfo.split(TokenConstants.SEGMENT_SEP)[1];
        String strPartInfo = strSubInfo.split(TokenConstants.SEGMENT_SEP)[2];
        this.consumerId = strConsumerInfo.split(TokenConstants.GROUP_SEP)[0];
        this.group = strConsumerInfo.split(TokenConstants.GROUP_SEP)[1];
        this.partition = new Partition(new BrokerInfo(strBrokerInfo), strPartInfo);
        this.builderSubscribeStr();
    }

    public SubscribeInfo(String consumerId, String group, Partition partition) {
        this.consumerId = consumerId;
        this.group = group;
        this.partition = partition;
        this.builderSubscribeStr();
    }

    public SubscribeInfo(String consumerId, String group, boolean overTLS, Partition partition) {
        this.consumerId = consumerId;
        this.group = group;
        this.partition = partition;
        this.overTLS = overTLS;
        this.builderSubscribeStr();
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
        if (this.group != null && this.partition != null) {
            this.builderSubscribeStr();
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
        if (this.group != null && this.partition != null) {
            this.builderSubscribeStr();
        }
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
        if (this.group != null && this.partition != null) {
            this.builderSubscribeStr();
        }
    }

    public int getBrokerId() {
        return partition.getBrokerId();
    }

    public String getHost() {
        return partition.getHost();
    }

    public int getPort() {
        return partition.getPort();
    }

    public String getTopic() {
        return partition.getTopic();
    }

    public int getPartitionId() {
        return partition.getPartitionId();
    }

    private void builderSubscribeStr() {
        StringBuilder sBuilder = new StringBuilder(256);
        this.fullInfo = sBuilder.append(consumerId).append(TokenConstants.GROUP_SEP)
                .append(group).append(TokenConstants.SEGMENT_SEP)
                .append(partition.getPartitionFullStr(overTLS)).toString();
    }

    @Override
    public String toString() {
        return this.fullInfo;
    }

}
