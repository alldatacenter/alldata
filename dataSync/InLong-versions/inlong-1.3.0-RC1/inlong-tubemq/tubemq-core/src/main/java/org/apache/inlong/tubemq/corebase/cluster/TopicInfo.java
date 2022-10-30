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

import java.io.Serializable;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;

public class TopicInfo implements Serializable {

    private static final long serialVersionUID = -2394664452604382172L;
    private BrokerInfo broker;
    private String topic;
    private String simpleTopicInfo;
    private String simpleValue;
    private int partitionNum;
    private int topicStoreNum = 1;
    private boolean acceptPublish;
    private boolean acceptSubscribe;

    public TopicInfo(final BrokerInfo broker, final String topic,
                     final int partitionNum, final int topicStoreNum,
                     final boolean acceptPublish, final boolean acceptSubscribe) {
        this.broker = broker;
        this.topic = topic;
        this.partitionNum = partitionNum;
        this.topicStoreNum = topicStoreNum;
        this.acceptPublish = acceptPublish;
        this.acceptSubscribe = acceptSubscribe;
        this.builderTopicStr();
    }

    public BrokerInfo getBroker() {
        return broker;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartitionNum() {
        return partitionNum;
    }

    public boolean isAcceptPublish() {
        return acceptPublish;
    }

    public void setAcceptPublish(boolean acceptPublish) {
        this.acceptPublish = acceptPublish;
    }

    public boolean isAcceptSubscribe() {
        return acceptSubscribe;
    }

    public void setAcceptSubscribe(boolean acceptSubscribe) {
        this.acceptSubscribe = acceptSubscribe;
    }

    // return result <isChanged, isScaleOut>
    public Tuple2<Boolean, Boolean> updAndJudgeTopicInfo(TopicInfo newTopicInfo) {
        boolean isChanged = false;
        if (this.acceptPublish != newTopicInfo.acceptPublish) {
            isChanged = true;
            this.acceptPublish = newTopicInfo.acceptPublish;
        }
        if (this.acceptSubscribe != newTopicInfo.acceptSubscribe) {
            isChanged = true;
            this.acceptSubscribe = newTopicInfo.acceptSubscribe;
        }
        return new Tuple2<>(isChanged, (this.partitionNum != newTopicInfo.partitionNum
                || this.topicStoreNum != newTopicInfo.topicStoreNum));
    }

    public int getTopicStoreNum() {
        return topicStoreNum;
    }

    public String getSimpleValue() {
        return this.simpleValue;
    }

    public StringBuilder toStrBuilderString(final StringBuilder sBuilder) {
        return sBuilder.append(broker.toString()).append(TokenConstants.SEGMENT_SEP)
                .append(this.topic).append(TokenConstants.ATTR_SEP)
                .append(this.partitionNum).append(TokenConstants.ATTR_SEP)
                .append(this.acceptPublish).append(TokenConstants.ATTR_SEP)
                .append(this.acceptSubscribe).append(TokenConstants.ATTR_SEP)
                .append(this.topicStoreNum);
    }

    @Override
    public String toString() {
        return toStrBuilderString(new StringBuilder(512)).toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final TopicInfo other = (TopicInfo) obj;
        if (this.broker.getBrokerId() != other.broker.getBrokerId()) {
            return false;
        }
        if (!this.broker.getHost().equals(other.broker.getHost())) {
            return false;
        }
        if (!this.topic.equals(other.topic)) {
            return false;
        }
        if (this.partitionNum != other.partitionNum) {
            return false;
        }
        if (this.acceptPublish != other.acceptPublish) {
            return false;
        }
        if (this.acceptSubscribe != other.acceptSubscribe) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = broker != null ? broker.hashCode() : 0;
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (simpleTopicInfo != null ? simpleTopicInfo.hashCode() : 0);
        result = 31 * result + (simpleValue != null ? simpleValue.hashCode() : 0);
        result = 31 * result + partitionNum;
        result = 31 * result + topicStoreNum;
        result = 31 * result + (acceptPublish ? 1 : 0);
        result = 31 * result + (acceptSubscribe ? 1 : 0);
        return result;
    }

    @Override
    public TopicInfo clone() {
        return new TopicInfo(this.broker, this.topic, this.partitionNum,
                this.topicStoreNum, this.acceptPublish, this.acceptSubscribe);
    }

    private void builderTopicStr() {
        StringBuilder sBuilder = new StringBuilder(256);
        this.simpleTopicInfo = sBuilder.append(broker.getBrokerId()).append(TokenConstants.SEGMENT_SEP)
                .append(this.topic).append(TokenConstants.ATTR_SEP).append(this.partitionNum)
                .append(TokenConstants.ATTR_SEP).append(this.topicStoreNum).toString();
        sBuilder.delete(0, sBuilder.length());
        this.simpleValue = sBuilder.append(broker.getBrokerId()).append(TokenConstants.ATTR_SEP)
                .append(this.partitionNum).append(TokenConstants.ATTR_SEP)
                .append(this.topicStoreNum).toString();
    }
}
