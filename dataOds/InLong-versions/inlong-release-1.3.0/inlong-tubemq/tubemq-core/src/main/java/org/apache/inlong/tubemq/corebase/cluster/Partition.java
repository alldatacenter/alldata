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
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;

public class Partition implements Comparable<Partition>, Serializable {

    private static final long serialVersionUID = 7587964123917870096L;

    private final BrokerInfo broker;
    private final String topic;
    private final int partitionId;
    private String partitionKey;
    private String partitionFullStr;
    private String partitionTLSFullStr;
    private int retries = 0;
    private long delayTimeStamp = TBaseConstants.META_VALUE_UNDEFINED;

    /**
     * create a Partition with broker info , topic and partitionId
     *
     * @param broker
     * @param topic
     * @param partitionId
     */
    public Partition(final BrokerInfo broker,
                     final String topic,
                     final int partitionId) {
        super();
        this.broker = broker;
        this.topic = topic;
        this.partitionId = partitionId;
        this.builderPartitionStr();
    }

    /**
     * create a Partition with full part info String
     *
     * @param strPartInfo   the partition information in string format
     */
    public Partition(String strPartInfo) {
        this.broker =
                new BrokerInfo(strPartInfo.split(TokenConstants.SEGMENT_SEP)[0]);
        String strInfo = strPartInfo.split(TokenConstants.SEGMENT_SEP)[1];
        this.topic = strInfo.split(TokenConstants.ATTR_SEP)[0];
        this.partitionId = Integer.parseInt(strInfo.split(TokenConstants.ATTR_SEP)[1]);
        this.builderPartitionStr();
    }

    /**
     * create a Partition with full broker and part info string
     *
     * @param broker
     * @param partStr
     */
    public Partition(BrokerInfo broker,
                     String partStr) {
        this.broker = broker;
        this.topic = partStr.split(TokenConstants.ATTR_SEP)[0];
        this.partitionId = Integer.parseInt(partStr.split(TokenConstants.ATTR_SEP)[1]);
        this.builderPartitionStr();
    }

    public BrokerInfo getBroker() {
        return this.broker;
    }

    public int getBrokerId() {
        return this.broker.getBrokerId();
    }

    public String getHost() {
        return this.broker.getHost();
    }

    public int getPort() {
        return this.broker.getPort();
    }

    public String getPartitionKey() {
        return this.partitionKey;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartitionId() {
        return this.partitionId;
    }

    public void increRetries(int retries) {
        this.retries += retries;
        if (this.retries > 3) {
            this.delayTimeStamp = System.currentTimeMillis() + 3 * 1000;
            this.retries = 0;
        }
    }

    public void resetRetries() {
        this.retries = 0;
        this.delayTimeStamp = 0;
    }

    public int getRetries() {
        return retries;
    }

    public long getDelayTimeStamp() {
        return delayTimeStamp;
    }

    public void setDelayTimeStamp(long dltTime) {
        this.delayTimeStamp = System.currentTimeMillis() + dltTime;
    }

    @Override
    public int compareTo(final Partition other) {
        if (this.broker.getBrokerId() != other.broker.getBrokerId()) {
            return this.broker.getBrokerId() > other.broker.getBrokerId() ? 1 : -1;
        }
        if (!this.broker.getHost().equals(other.broker.getHost())) {
            return this.broker.getHost().compareTo(other.broker.getHost());
        }
        if (!this.topic.equals(other.topic)) {
            return this.topic.compareTo(other.topic);
        } else if (this.partitionId != other.partitionId) {
            return this.partitionId > other.partitionId ? 1 : -1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((broker == null) ? 0 : broker.hashCode());
        result = prime * result + partitionId;
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Partition other = (Partition) obj;
        if (broker == null) {
            if (other.broker != null) {
                return false;
            }
        } else if (broker.getBrokerId() != other.broker.getBrokerId()) {
            return false;
        } else if (!broker.getHost().equals(other.broker.getHost())) {
            return false;
        }
        if (partitionId != other.partitionId) {
            return false;
        }
        if (topic == null) {
            if (other.topic != null) {
                return false;
            }
        } else if (!topic.equals(other.topic)) {
            return false;
        }
        return true;
    }

    /**
     * init some string info of partition
     */
    private void builderPartitionStr() {
        StringBuilder sBuilder = new StringBuilder(256);
        this.partitionKey = sBuilder.append(this.broker.getBrokerId())
                .append(TokenConstants.ATTR_SEP).append(this.topic)
                .append(TokenConstants.ATTR_SEP).append(this.partitionId).toString();
        sBuilder.delete(0, sBuilder.length());
        this.partitionFullStr = sBuilder.append(this.broker.toString())
                .append(TokenConstants.SEGMENT_SEP).append(this.topic)
                .append(TokenConstants.ATTR_SEP).append(this.partitionId).toString();
        sBuilder.delete(0, sBuilder.length());
        this.partitionTLSFullStr = sBuilder.append(this.broker.getFullTLSInfo())
                .append(TokenConstants.SEGMENT_SEP).append(this.topic)
                .append(TokenConstants.ATTR_SEP).append(this.partitionId).toString();

    }

    @Override
    public String toString() {
        return this.partitionFullStr;
    }

    /**
     * get the partition full string info
     *
     * @param overTLS
     * @return
     */
    public String getPartitionFullStr(boolean overTLS) {
        if (overTLS) {
            return this.partitionTLSFullStr;
        } else {
            return this.partitionFullStr;
        }

    }

}
