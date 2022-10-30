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

package org.apache.inlong.tubemq.server.master.nodemanage.nodebroker;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.utils.CheckSum;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corebase.utils.Tuple4;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Broker sync data holder
 */
public class BrokerSyncData {
    private static final Logger logger =
            LoggerFactory.getLogger(BrokerSyncData.class);
    // current data push id
    private long dataPushId;
    // data need to sync
    private final AtomicLong syncDownDataConfId =
            new AtomicLong(System.currentTimeMillis());
    private int syncDownDataChkSumId;
    private ManageStatus mngStatus;
    private String syncDownBrokerConfInfo;
    private Map<String, String> syncDownTopicConfInfoMap = new HashMap<>();
    private boolean isStatusChanged = false;
    private boolean isConfChanged = false;

    // report info
    private long syncUpDataConfId = TBaseConstants.META_VALUE_UNDEFINED;
    private int syncUpDataChkSumId = TBaseConstants.META_VALUE_UNDEFINED;
    private String syncUpBrokerConfInfo;
    private List<String> syncUpTopicConfInfos = new ArrayList<>();
    Map<String, TopicInfo> syncUpTopicInfoMap = new HashMap<>();
    private long lastDataUpTime = 0;

    public BrokerSyncData() {

    }

    /**
     * Update current sync data
     *
     * @param isForceSync  if force sync data to broker
     * @param dataPushId   data push id for verify
     * @param mngStatus    new broker manage status
     * @param brokerConfInfo  broker default configure
     * @param topicConfInfoMap topic configure set
     *
     * @return whether changed, if null: input brokerConfInfo data is illegal
     *         f0 : manage status changed
     *         f1 : configure changed
     */
    public Tuple2<Boolean, Boolean> updBrokerSyncData(boolean isForceSync, long dataPushId,
                                                      ManageStatus mngStatus,
                                                      String brokerConfInfo,
                                                      Map<String, String> topicConfInfoMap) {
        isStatusChanged = false;
        isConfChanged = false;
        // check parameters
        if (TStringUtils.isBlank(brokerConfInfo)) {
            return null;
        }
        this.dataPushId = dataPushId;
        if (isForceSync || this.mngStatus != mngStatus) {
            this.mngStatus = mngStatus;
            isStatusChanged = true;
        }

        if (isForceSync || isSyncDataChanged(brokerConfInfo, topicConfInfoMap)) {
            this.syncDownBrokerConfInfo = brokerConfInfo;
            if (topicConfInfoMap == null) {
                this.syncDownTopicConfInfoMap = new HashMap<>();
            } else {
                this.syncDownTopicConfInfoMap = topicConfInfoMap;
            }
            this.syncDownDataChkSumId =
                    calculateConfigCrc32Value(syncDownBrokerConfInfo, syncDownTopicConfInfoMap);
            isConfChanged = true;
        }
        if (isStatusChanged && isConfChanged) {
            this.syncDownDataConfId.incrementAndGet();
        }
        return new Tuple2<>(isStatusChanged, isConfChanged);
    }

    /**
     * Book the report data by broker
     * @param brokerInfo      broker info
     * @param syncDataConfId   data configure id
     * @param syncDataChkSumId data check-sum id
     * @param isTakeData  if carry the data info
     * @param syncBrokerConfInfo  broker default config
     * @param syncTopicConfInfos topic config set
     *
     * @return whether the broker data synchronized
     */
    public boolean bookBrokerReportInfo(BrokerInfo brokerInfo,
                                        long syncDataConfId, int syncDataChkSumId,
                                        boolean isTakeData, String syncBrokerConfInfo,
                                        List<String> syncTopicConfInfos) {
        this.syncUpDataConfId = syncDataConfId;
        this.syncUpDataChkSumId = syncDataChkSumId;
        if (isTakeData) {
            this.syncUpBrokerConfInfo = syncBrokerConfInfo;
            if (syncTopicConfInfos == null) {
                this.syncUpTopicConfInfos = new ArrayList<>();
            } else {
                this.syncUpTopicConfInfos = syncTopicConfInfos;
            }
            Map<String, TopicInfo> tmpInfoMap = parseTopicInfoConf(brokerInfo);
            if (tmpInfoMap == null) {
                this.syncUpTopicInfoMap.clear();
            } else {
                this.syncUpTopicInfoMap = tmpInfoMap;
            }
            this.lastDataUpTime = System.currentTimeMillis();
        }
        return isConfSynchronized();
    }

    /**
     *  whether the broker data is synchronized
     *
     * @return true: the configure is synchronized;
     *         false: not synchronized
     */
    public boolean isConfSynchronized() {
        return (this.syncDownDataConfId.get() == this.syncUpDataConfId
                && this.syncDownDataChkSumId == this.syncUpDataChkSumId);
    }

    public boolean isDownTopicConfEmpty() {
        return this.syncDownTopicConfInfoMap.isEmpty();
    }

    /**
     * Get need sync to broker's data
     *
     * @return return data container,
     */
    public Tuple4<Long, Integer, String, List<String>> getBrokerSyncData() {
        if (isConfSynchronized()) {
            return new Tuple4<>(syncDownDataConfId.get(), syncDownDataChkSumId, null, null);
        } else {
            List<String> topicInfoList = new ArrayList<>();
            for (String topicInfo : syncDownTopicConfInfoMap.values()) {
                if (topicInfo != null) {
                    topicInfoList.add(topicInfo);
                }
            }
            return new Tuple4<>(syncDownDataConfId.get(), syncDownDataChkSumId,
                    syncDownBrokerConfInfo, topicInfoList);
        }
    }

    /**
     * Get the broker publish info
     * @return need sync data
     *         f0 : manage status
     *         f1 : topic configure
     */
    public Tuple2<ManageStatus, Map<String, TopicInfo>> getBrokerPublishInfo() {
        return new Tuple2<>(mngStatus, syncUpTopicInfoMap);
    }

    public long getDataPushId() {
        return dataPushId;
    }

    public long getLastDataReportTime() {
        return lastDataUpTime;
    }

    /**
     * Judge the sync-data is change
     * @param brokerConfInfo  broker default config
     * @param topicConfInfoMap topic config set
     *
     * @return whether the sync-data is change
     */
    private boolean isSyncDataChanged(String brokerConfInfo,
                                      Map<String, String> topicConfInfoMap) {
        return !Objects.equals(syncDownBrokerConfInfo, brokerConfInfo)
                || !Objects.equals(syncDownTopicConfInfoMap, topicConfInfoMap);
    }

    /**
     * Format broker sync data to json string
     *
     * @param sBuffer  string process container
     * @return process result
     */
    public StringBuilder toJsonString(StringBuilder sBuffer) {
        sBuffer.append("{\"dataPushId\":").append(dataPushId)
                .append(",\"mngStatus\":\"").append(mngStatus.getDescription())
                .append("\",\"syncDownDataConfId\":").append(syncDownDataConfId.get())
                .append(",\"syncDownDataChkSumId\":").append(syncDownDataChkSumId)
                .append(",\"isStatusChanged\":").append(isStatusChanged)
                .append(",\"isConfChanged\":").append(isConfChanged)
                .append(",\"syncDownBrokerConfInfo\":\"").append(syncDownBrokerConfInfo)
                .append("\",\"syncDownTopicConfInfoMap\":\"").append(syncDownTopicConfInfoMap.toString())
                .append("\",\"syncUpDataConfId\":").append(syncUpDataConfId)
                .append(",\"syncUpDataChkSumId\":").append(syncUpDataChkSumId)
                .append(",\"syncUpBrokerConfInfo\":\"").append(syncUpBrokerConfInfo)
                .append("\",\"syncUpTopicConfInfos\":\"").append(syncUpTopicConfInfos.toString())
                .append("\",\"syncUpTopicInfoMap\":\"").append(syncUpTopicInfoMap.toString())
                .append("\",\"lastDataUpTime\":").append(lastDataUpTime)
                .append("}");
        return sBuffer;
    }

    /**
     * parse broker report configure to topicInfo
     *
     * @param brokerInfo broker info
     */
    private Map<String, TopicInfo> parseTopicInfoConf(final BrokerInfo brokerInfo) {
        // check broker configure
        if (TStringUtils.isBlank(this.syncUpBrokerConfInfo)) {
            return null;
        }
        // get broker status and topic default configure
        String[] brokerConfInfoAttrs =
                this.syncUpBrokerConfInfo.split(TokenConstants.ATTR_SEP);
        int numPartitions = Integer.parseInt(brokerConfInfoAttrs[0]);
        boolean cfgAcceptPublish = Boolean.parseBoolean(brokerConfInfoAttrs[1]);
        boolean cfgAcceptSubscribe = Boolean.parseBoolean(brokerConfInfoAttrs[2]);
        int numTopicStores = 1;
        if (brokerConfInfoAttrs.length > 7) {
            if (!TStringUtils.isBlank(brokerConfInfoAttrs[7])) {
                numTopicStores = Integer.parseInt(brokerConfInfoAttrs[7]);
            }
        }
        Map<String, TopicInfo> topicInfoMap = new HashMap<>();
        // make up topicInfo info according to broker default configure
        for (String strTopicConfInfo : this.syncUpTopicConfInfos) {
            if (TStringUtils.isBlank(strTopicConfInfo)) {
                continue;
            }
            String[] topicConfAttrs =
                    strTopicConfInfo.split(TokenConstants.ATTR_SEP);
            final String tmpTopic = topicConfAttrs[0];
            int tmpPartNum = numPartitions;
            if (!TStringUtils.isBlank(topicConfAttrs[1])) {
                tmpPartNum = Integer.parseInt(topicConfAttrs[1]);
            }
            boolean tmpAcceptPublish = cfgAcceptPublish;
            if (!TStringUtils.isBlank(topicConfAttrs[2])) {
                tmpAcceptPublish = Boolean.parseBoolean(topicConfAttrs[2]);
            }
            int tmpNumTopicStores = numTopicStores;
            if (!TStringUtils.isBlank(topicConfAttrs[8])) {
                tmpNumTopicStores = Integer.parseInt(topicConfAttrs[8]);
                tmpNumTopicStores = tmpNumTopicStores > 0 ? tmpNumTopicStores : numTopicStores;
            }
            boolean tmpAcceptSubscribe = cfgAcceptSubscribe;
            if (!TStringUtils.isBlank(topicConfAttrs[3])) {
                tmpAcceptSubscribe = Boolean.parseBoolean(topicConfAttrs[3]);
            }
            topicInfoMap.put(tmpTopic, new TopicInfo(brokerInfo, tmpTopic,
                    tmpPartNum, tmpNumTopicStores, tmpAcceptPublish, tmpAcceptSubscribe));
        }
        return topicInfoMap;
    }

    /**
     * Calculate the sync-data's crc32 value
     *
     * @param brokerConfInfo  broker default config
     * @param topicConfInfoMap topic config set
     *
     * @return the crc32 value
     */
    private int calculateConfigCrc32Value(String brokerConfInfo,
                                          Map<String, String> topicConfInfoMap) {
        int result = -1;
        int capacity = 0;
        List<String> topicConfInfoLst =
                new ArrayList<>(topicConfInfoMap.values());
        Collections.sort(topicConfInfoLst);
        capacity += brokerConfInfo.length();
        for (String itemStr : topicConfInfoLst) {
            capacity += itemStr.length();
        }
        capacity *= 2;
        for (int i = 1; i < 3; i++) {
            result = inCalcBufferResult(capacity, brokerConfInfo, topicConfInfoLst);
            if (result >= 0) {
                return result;
            }
            capacity *= i + 1;
        }
        logger.error("Calculate the CRC32 value of Broker Configure error!");
        return 0;
    }

    private int inCalcBufferResult(int capacity, String brokerConfInfo,
                                   List<String> topicConfInfoLst) {
        final ByteBuffer buffer = ByteBuffer.allocate(capacity);
        buffer.put(StringUtils.getBytesUtf8(brokerConfInfo));
        for (String itemStr : topicConfInfoLst) {
            byte[] itemData = StringUtils.getBytesUtf8(itemStr);
            if (itemData.length > buffer.remaining()) {
                return -1;
            }
            buffer.put(itemData);
        }
        return CheckSum.crc32(buffer.array());
    }

}
