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

package org.apache.inlong.tubemq.server.broker.metadata;

import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

/**
 * Default metadata for broker, it mainly contains topic default config(partitions count, delete policy...).
 * These metadata will be overwrite if explicitly be set.
 */
public class BrokerDefMetadata {
    // topic's store file count.
    private int numTopicStores = 1;
    // topic's partition count.
    private int numPartitions = 1;
    // data will be flushed to disk when unflushed message count exceed this.
    private int unflushThreshold = 1000;
    // data will be flushed to disk when unflushed data size reaches the threshold, 0=disabled.
    private int unflushDataHold = 0;
    // data will be flushed to disk when elapse unflushInterval milliseconds since last flush operation.
    private int unflushInterval = 10000;
    // enable produce data to topic.
    private boolean acceptPublish = true;
    // enable consume data from topic.
    private boolean acceptSubscribe = true;
    // path to store topic's data in disk.
    private String dataPath;
    @Deprecated
    private String deleteWhen = "0 0 6,18 * * ?";
    // expire policy.
    private String deletePolicy = "delete,168h";
    // the max cache size for topic.
    private int memCacheMsgSize = 1024 * 1024;
    // the max cache message count for topic.
    private int memCacheMsgCnt = 5 * 1024;
    // the max interval(milliseconds) that topic's memory cache will flush to disk.
    private int memCacheFlushInterval = 20000;

    public BrokerDefMetadata() {

    }

    /**
     * Initial broker meta-data object by configure info
     *
     * @param brokerDefMetaConfInfo      the broker configure information.
    */
    public BrokerDefMetadata(String brokerDefMetaConfInfo) {
        if (TStringUtils.isBlank(brokerDefMetaConfInfo)) {
            return;
        }
        String[] brokerDefaultConfInfoArr = brokerDefMetaConfInfo.split(TokenConstants.ATTR_SEP);
        this.numPartitions = Integer.parseInt(brokerDefaultConfInfoArr[0]);
        this.acceptPublish = Boolean.parseBoolean(brokerDefaultConfInfoArr[1]);
        this.acceptSubscribe = Boolean.parseBoolean(brokerDefaultConfInfoArr[2]);
        this.unflushThreshold = Integer.parseInt(brokerDefaultConfInfoArr[3]);
        this.unflushInterval = Integer.parseInt(brokerDefaultConfInfoArr[4]);
        this.deleteWhen = brokerDefaultConfInfoArr[5];
        this.deletePolicy = brokerDefaultConfInfoArr[6];
        this.numTopicStores = Integer.parseInt(brokerDefaultConfInfoArr[7]);
        this.unflushDataHold = Integer.parseInt(brokerDefaultConfInfoArr[8]);
        this.memCacheMsgSize = Integer.parseInt(brokerDefaultConfInfoArr[9]) * 1024 * 512;
        this.memCacheMsgCnt = Integer.parseInt(brokerDefaultConfInfoArr[10]) * 512;
        this.memCacheFlushInterval = Integer.parseInt(brokerDefaultConfInfoArr[11]);
    }

    public int getNumTopicStores() {
        return numTopicStores;
    }

    public void setNumTopicStores(int numTopicStores) {
        this.numTopicStores = numTopicStores;
    }

    public int getNumPartitions() {
        return numPartitions;
    }

    public void setNumPartitions(int numPartitions) {
        this.numPartitions = numPartitions;
    }

    public int getUnflushThreshold() {
        return unflushThreshold;
    }

    public void setUnflushThreshold(int unflushThreshold) {
        this.unflushThreshold = unflushThreshold;
    }

    public int getUnflushDataHold() {
        return unflushDataHold;
    }

    public void setUnflushDataHold(int unflushDataHold) {
        this.unflushDataHold = unflushDataHold;
    }

    public int getUnflushInterval() {
        return unflushInterval;
    }

    public void setUnflushInterval(int unflushInterval) {
        this.unflushInterval = unflushInterval;
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

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getDeleteWhen() {
        return deleteWhen;
    }

    public void setDeleteWhen(String deleteWhen) {
        this.deleteWhen = deleteWhen;
    }

    public String getDeletePolicy() {
        return deletePolicy;
    }

    public void setDeletePolicy(String deletePolicy) {
        this.deletePolicy = deletePolicy;
    }

    public int getMemCacheMsgSize() {
        return memCacheMsgSize;
    }

    public void setMemCacheMsgSize(int memCacheMsgSize) {
        this.memCacheMsgSize = memCacheMsgSize;
    }

    public int getMemCacheMsgCnt() {
        return memCacheMsgCnt;
    }

    public void setMemCacheMsgCnt(int memCacheMsgCnt) {
        this.memCacheMsgCnt = memCacheMsgCnt;
    }

    public int getMemCacheFlushInterval() {
        return memCacheFlushInterval;
    }

    public void setMemCacheFlushInterval(int memCacheFlushInterval) {
        this.memCacheFlushInterval = memCacheFlushInterval;
    }

}
