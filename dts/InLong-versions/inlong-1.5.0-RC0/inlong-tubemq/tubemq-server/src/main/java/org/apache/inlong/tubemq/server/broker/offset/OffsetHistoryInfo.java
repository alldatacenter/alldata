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

package org.apache.inlong.tubemq.server.broker.offset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.server.common.TServerConstants;

/**
 * The offset snapshot of the consumer group on the broker.
 */
public class OffsetHistoryInfo {

    private final int brokerId;
    private final String groupName;
    private final Map<String, Map<Integer, OffsetCsmRecord>> histOffsetMap = new HashMap<>();

    public OffsetHistoryInfo(int brokerId, String groupName) {
        this.brokerId = brokerId;
        this.groupName = groupName;
    }

    /**
     * Add confirmed offset of topic-partitionId.
     *
     * @param topicName      topic name
     * @param partitionId    partition id
     * @param cfmOffset      the confirmed offset
     */
    public void addCfmOffsetInfo(String topicName, int partitionId, long cfmOffset) {
        final int storeId = partitionId < TBaseConstants.META_STORE_INS_BASE
                ? 0
                : partitionId / TBaseConstants.META_STORE_INS_BASE;
        Map<Integer, OffsetCsmRecord> storeOffsetMap = histOffsetMap.get(topicName);
        if (storeOffsetMap == null) {
            Map<Integer, OffsetCsmRecord> tmpMap = new HashMap<>();
            storeOffsetMap = histOffsetMap.putIfAbsent(topicName, tmpMap);
            if (storeOffsetMap == null) {
                storeOffsetMap = tmpMap;
            }
        }
        OffsetCsmRecord offsetCsmRecord = storeOffsetMap.get(storeId);
        if (offsetCsmRecord == null) {
            OffsetCsmRecord tmpRecord = new OffsetCsmRecord(storeId);
            offsetCsmRecord = storeOffsetMap.putIfAbsent(storeId, tmpRecord);
            if (offsetCsmRecord == null) {
                offsetCsmRecord = tmpRecord;
            }
        }
        offsetCsmRecord.addOffsetCfmInfo(partitionId, cfmOffset);
    }

    /**
     * Add inflight offset of topic-partitionId.
     *
     * @param topicName      topic name
     * @param partitionId    partition id
     * @param tmpOffset      the inflight offset
     */
    public void addInflightOffsetInfo(String topicName, int partitionId, long tmpOffset) {
        final int storeId = partitionId < TBaseConstants.META_STORE_INS_BASE
                ? 0
                : partitionId / TBaseConstants.META_STORE_INS_BASE;
        Map<Integer, OffsetCsmRecord> storeOffsetMap = histOffsetMap.get(topicName);
        if (storeOffsetMap == null) {
            Map<Integer, OffsetCsmRecord> tmpMap = new HashMap<>();
            storeOffsetMap = histOffsetMap.putIfAbsent(topicName, tmpMap);
            if (storeOffsetMap == null) {
                storeOffsetMap = tmpMap;
            }
        }
        OffsetCsmRecord offsetCsmRecord = storeOffsetMap.get(storeId);
        if (offsetCsmRecord == null) {
            OffsetCsmRecord tmpRecord = new OffsetCsmRecord(storeId);
            offsetCsmRecord = storeOffsetMap.putIfAbsent(storeId, tmpRecord);
            if (offsetCsmRecord == null) {
                offsetCsmRecord = tmpRecord;
            }
        }
        offsetCsmRecord.addOffsetFetchInfo(partitionId, tmpOffset);
    }

    public String getGroupName() {
        return groupName;
    }

    public Map<String, Map<Integer, OffsetCsmRecord>> getOffsetMap() {
        return histOffsetMap;
    }

    /**
     * Build brief consumption offset information in string format
     *
     * @param strBuff     string buffer
     * @param dataTime    record build time
     */
    public void buildRecordInfo(StringBuilder strBuff, long dataTime) {
        int topicCnt = 0;
        strBuff.append("{\"dt\":\"")
                .append(DateTimeConvertUtils.ms2yyyyMMddHHmmss(dataTime))
                .append("\",\"bId\":").append(brokerId)
                .append(",\"ver\":").append(TServerConstants.OFFSET_HISTORY_RECORD_SHORT_VERSION)
                .append(",\"records\":[");
        for (Map.Entry<String, Map<Integer, OffsetCsmRecord>> entry : histOffsetMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (topicCnt++ > 0) {
                strBuff.append(",");
            }
            int recordCnt = 0;
            strBuff.append("{\"topic\":\"").append(entry.getKey()).append("\",\"offsets\":[");
            Map<Integer, OffsetCsmRecord> csmOffsetRecordMap = entry.getValue();
            for (OffsetCsmRecord offsetRecord : csmOffsetRecordMap.values()) {
                if (offsetRecord == null) {
                    continue;
                }
                if (recordCnt++ > 0) {
                    strBuff.append(",");
                }
                strBuff.append("{\"storeId\":").append(offsetRecord.storeId)
                        .append(",\"iMin\":").append(offsetRecord.offsetMin)
                        .append(",\"iMax\":").append(offsetRecord.offsetMax)
                        .append(",\"dMin\":").append(offsetRecord.dataMin)
                        .append(",\"dMax\":").append(offsetRecord.dataMax)
                        .append(",\"parts\":[");
                int partCnt = 0;
                for (OffsetCsmItem csmOffsetItem : offsetRecord.partitionCsmMap.values()) {
                    if (csmOffsetItem == null) {
                        continue;
                    }
                    if (partCnt++ > 0) {
                        strBuff.append(",");
                    }
                    strBuff.append("{\"partId\":").append(csmOffsetItem.partitionId)
                            .append(",\"iCfm\":").append(csmOffsetItem.offsetCfm)
                            .append("}");
                }
                strBuff.append("]}");
            }
            strBuff.append("]}");
        }
        strBuff.append("]}");
    }

    /**
     * Parse history offset record info
     *
     * @param jsonData  string offset information
     * @param result    process result
     */
    public static boolean parseRecordInfo(String jsonData, ProcessResult result) {
        JsonObject jsonObject = null;
        try {
            jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
        } catch (Throwable e1) {
            result.setFailResult(String.format(
                    "Parse history offset value failure, reason is %s", e1.getMessage()));
            return result.isSuccess();
        }
        if (jsonObject == null) {
            result.setFailResult("Parse error, history offset value must be valid json format!");
            return result.isSuccess();
        }
        if (!jsonObject.has("ver")) {
            result.setFailResult("FIELD ver is required in history offset value!");
            return result.isSuccess();
        }
        int verValue = jsonObject.get("ver").getAsInt();
        if (verValue < TServerConstants.OFFSET_HISTORY_RECORD_SHORT_VERSION) {
            result.setFailResult("Only support v2 or next version in history offset value!");
            return result.isSuccess();
        }
        if (!jsonObject.has("records")) {
            result.setFailResult("FIELD records is required in history offset value!");
            return result.isSuccess();
        }
        List<Tuple3<String, Integer, Long>> resetOffsets = new ArrayList<>();
        JsonArray records = jsonObject.get("records").getAsJsonArray();
        for (int i = 0; i < records.size(); i++) {
            JsonObject itemInfo = records.get(i).getAsJsonObject();
            if (itemInfo == null) {
                continue;
            }
            String topicName = itemInfo.get("topic").getAsString();
            JsonArray offsets = itemInfo.get("offsets").getAsJsonArray();
            for (int j = 0; j < offsets.size(); j++) {
                JsonObject storeInfo = offsets.get(j).getAsJsonObject();
                if (storeInfo == null) {
                    continue;
                }
                JsonArray partInfos = storeInfo.get("parts").getAsJsonArray();
                for (int k = 0; k < partInfos.size(); k++) {
                    JsonObject partItem = partInfos.get(k).getAsJsonObject();
                    int partId = partItem.get("partId").getAsInt();
                    long offsetVal = partItem.get("iCfm").getAsLong();
                    resetOffsets.add(new Tuple3<>(topicName, partId, offsetVal));
                }
            }
        }
        result.setSuccResult(resetOffsets);
        return true;
    }

}
