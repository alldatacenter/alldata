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

package org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.utils.ClientSyncInfo;

public class ConsumerInfo implements Comparable<ConsumerInfo>, Serializable {

    private static final long serialVersionUID = 3095734962491009712L;

    private final String consumerId;
    private final String consumerViewInfo;
    private final ConsumeType consumeType;
    private final String group;
    private final Set<String> topicSet;
    private final Map<String, TreeSet<String>> topicConditions;
    private boolean overTLS = false;
    private long startTime = TBaseConstants.META_VALUE_UNDEFINED;
    private int sourceCount = TBaseConstants.META_VALUE_UNDEFINED;
    private int nodeId = TBaseConstants.META_VALUE_UNDEFINED;
    // for band consume type node
    private String sessionKey = "";
    //select the bigger offset when offset conflict
    private boolean selectedBig = true;
    private Map<String, Long> requiredPartition;
    // for client balance node
    private long csmFromMaxOffsetCtrlId = TBaseConstants.META_VALUE_UNDEFINED;
    private long lstAssignedTime = TBaseConstants.META_VALUE_UNDEFINED;
    private long usedTopicMetaInfoId = TBaseConstants.META_VALUE_UNDEFINED;

    /**
     * Initial Consumer node information
     *
     * @param consumerId          the consumer id
     * @param overTLS             whether to communicate via TLS
     * @param group               the group name of the consumer
     * @param topicSet            the topic set subscribed
     * @param topicConditions     the topic filter condition set
     * @param consumeType         the consume type
     * @param sessionKey          the session key
     * @param startTime           the start time
     * @param sourceCount         the minimum consumer count of consume group
     * @param selectedBig         whether to choose a larger value if there is a conflict
     * @param requiredPartition   the required partitions
     */
    public ConsumerInfo(String consumerId, boolean overTLS, String group,
                        Set<String> topicSet, Map<String, TreeSet<String>> topicConditions,
                        ConsumeType consumeType, String sessionKey, long startTime,
                        int sourceCount, boolean selectedBig,
                        Map<String, Long> requiredPartition) {
        this.group = group;
        this.consumeType = consumeType;
        this.consumerId = consumerId;
        this.overTLS = overTLS;
        this.topicSet = topicSet;
        if (topicConditions == null) {
            this.topicConditions = new HashMap<>();
        } else {
            this.topicConditions = topicConditions;
        }
        this.sessionKey = sessionKey;
        this.selectedBig = selectedBig;
        this.startTime = startTime;
        this.sourceCount = sourceCount;
        this.requiredPartition = requiredPartition;
        this.consumerViewInfo = toString();
    }

    /**
     * Initial Consumer node information
     *
     * @param consumerId          the consumer id
     * @param overTLS             whether to communicate via TLS
     * @param group               the group name of the consumer
     * @param consumeType         the consume type
     * @param sourceCount         the minimum consumer count of consume group
     * @param nodeId              the node id
     * @param topicSet            the topic set subscribed
     * @param topicConditions     the topic filter condition set
     * @param curCsmCtrlId        the node's consume control id
     * @param syncInfo            the consumer report information
     */
    public ConsumerInfo(String consumerId, boolean overTLS, String group,
                        ConsumeType consumeType, int sourceCount, int nodeId,
                        Set<String> topicSet, Map<String, TreeSet<String>> topicConditions,
                        long curCsmCtrlId, ClientSyncInfo syncInfo) {
        this.group = group;
        this.consumeType = consumeType;
        this.consumerId = consumerId;
        this.overTLS = overTLS;
        this.topicSet = topicSet;
        if (topicConditions == null) {
            this.topicConditions = new HashMap<>();
        } else {
            this.topicConditions = topicConditions;
        }
        this.sourceCount = sourceCount;
        this.nodeId = nodeId;
        this.startTime = System.currentTimeMillis();
        if (curCsmCtrlId != TBaseConstants.META_VALUE_UNDEFINED) {
            this.csmFromMaxOffsetCtrlId = curCsmCtrlId;
        }
        updClientReportInfo(curCsmCtrlId,
                syncInfo.getLstAssignedTime(), syncInfo.getTopicMetaInfoId());
        this.consumerViewInfo = toString();
    }

    public void updCurConsumerInfo(ConsumerInfo inCsmInfo) {
        if (inCsmInfo.getConsumeType() != ConsumeType.CONSUME_CLIENT_REB) {
            return;
        }
        this.overTLS = inCsmInfo.overTLS;
        this.nodeId = inCsmInfo.getNodeId();
        updClientReportInfo(inCsmInfo.getCsmFromMaxOffsetCtrlId(),
                inCsmInfo.getLstAssignedTime(), inCsmInfo.getUsedTopicMetaInfoId());
    }

    public void updClientReportInfo(long lstCsmCtrlId,
                                    long lastAssignedTime,
                                    long usedTopicMetaInfoId) {
        if (lstCsmCtrlId >= 0 && lstCsmCtrlId != this.csmFromMaxOffsetCtrlId) {
            this.csmFromMaxOffsetCtrlId = lstCsmCtrlId;
        }
        if (lastAssignedTime != TBaseConstants.META_VALUE_UNDEFINED
                && this.lstAssignedTime != lastAssignedTime) {
            this.lstAssignedTime = lastAssignedTime;
        }
        if (usedTopicMetaInfoId != TBaseConstants.META_VALUE_UNDEFINED
                && this.usedTopicMetaInfoId != usedTopicMetaInfoId) {
            this.usedTopicMetaInfoId = usedTopicMetaInfoId;
        }
    }

    @Override
    public String toString() {
        StringBuilder sBuilder =
                new StringBuilder(512).append(consumerId)
                        .append("@").append(group).append(":");
        int count = 0;
        for (String topicItem : topicSet) {
            if (count++ > 0) {
                sBuilder.append(",");
            }
            sBuilder.append(topicItem);
        }
        sBuilder.append("@overTLS=").append(overTLS);
        return sBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ConsumerInfo)) {
            return false;
        }
        final ConsumerInfo info = (ConsumerInfo) obj;
        return (this.consumerId.equals(info.getConsumerId()));
    }

    @Override
    public int compareTo(ConsumerInfo o) {
        if (!this.consumerId.equals(o.consumerId)) {
            return this.consumerId.compareTo(o.consumerId);
        }
        if (!this.group.equals(o.group)) {
            return this.group.compareTo(o.group);
        }
        return 0;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public String getGroupName() {
        return group;
    }

    public Set<String> getTopicSet() {
        return topicSet;
    }

    public Map<String, TreeSet<String>> getTopicConditions() {
        return topicConditions;
    }

    public boolean isOverTLS() {
        return overTLS;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public int getSourceCount() {
        return this.sourceCount;
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public long getCsmFromMaxOffsetCtrlId() {
        return csmFromMaxOffsetCtrlId;
    }

    public boolean isRequireBound() {
        return this.consumeType == ConsumeType.CONSUME_BAND;
    }

    public String getSessionKey() {
        return this.sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Map<String, Long> getRequiredPartition() {
        return this.requiredPartition;
    }

    public ConsumeType getConsumeType() {
        return consumeType;
    }

    public boolean isSelectedBig() {
        return selectedBig;
    }

    public String getConsumerViewInfo() {
        return consumerViewInfo;
    }

    public long getLstAssignedTime() {
        return lstAssignedTime;
    }

    public long getUsedTopicMetaInfoId() {
        return usedTopicMetaInfoId;
    }

    public Tuple2<String, Boolean> getConsumerIdAndTlsInfoTuple() {
        return new Tuple2<>(consumerId, overTLS);
    }
}
