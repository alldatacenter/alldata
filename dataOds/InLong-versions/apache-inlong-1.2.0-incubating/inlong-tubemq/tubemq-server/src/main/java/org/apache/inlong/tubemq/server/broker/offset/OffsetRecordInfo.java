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

package org.apache.inlong.tubemq.server.broker.offset;

import java.util.HashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;

/**
 * The offset snapshot of the consumer group on the broker.
 */
public class OffsetRecordInfo {
    private final int brokerId;
    private final String groupName;
    private final Map<String, Map<Integer, RecordItem>> histOffsetMap = new HashMap<>();

    public OffsetRecordInfo(int brokerId, String groupName) {
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
        Map<Integer, RecordItem> partOffsetMap = histOffsetMap.get(topicName);
        if (partOffsetMap == null) {
            Map<Integer, RecordItem> tmpMap = new HashMap<>();
            partOffsetMap = histOffsetMap.putIfAbsent(topicName, tmpMap);
            if (partOffsetMap == null) {
                partOffsetMap = tmpMap;
            }
        }
        partOffsetMap.put(partitionId, new RecordItem(partitionId, cfmOffset));
    }

    public Map<String, Map<Integer, RecordItem>> getOffsetMap() {
        return histOffsetMap;
    }

    /**
     * Build consumption offset information in string format
     *
     * @param strBuff     string buffer
     * @param dataTime    record build time
     */
    public void buildRecordInfo(StringBuilder strBuff, long dataTime) {
        int topicCnt = 0;
        strBuff.append("{\"groupName\":\"").append(groupName).append("\",\"recordTime\":\"")
                .append(DateTimeConvertUtils.ms2yyyyMMddHHmmss(dataTime))
                .append("\",\"brokerId\":").append(brokerId)
                .append(",\"records\":[");
        for (Map.Entry<String, Map<Integer, RecordItem>> entry : histOffsetMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (topicCnt++ > 0) {
                strBuff.append(",");
            }
            int recordCnt = 0;
            strBuff.append("{\"topicName\":\"").append(entry.getKey())
                    .append("\",\"offsetInfo\":[");
            Map<Integer, RecordItem> recordItemMap = entry.getValue();
            for (RecordItem recordItem : recordItemMap.values()) {
                if (recordCnt++ > 0) {
                    strBuff.append(",");
                }
                strBuff.append("{\"partitionKey\":\"").append(brokerId)
                        .append(TokenConstants.ATTR_SEP).append(entry.getKey())
                        .append(TokenConstants.ATTR_SEP).append(recordItem.partitionId)
                        .append("\",\"offsetCfm\":").append(recordItem.offsetCfm)
                        .append(",\"offsetMin\":").append(recordItem.offsetMin)
                        .append(",\"offsetMax\":").append(recordItem.offsetMax)
                        .append(",\"offsetLag\":").append(recordItem.offsetLag)
                        .append(",\"dataMin\":").append(recordItem.dataMin)
                        .append(",\"dataMax\":").append(recordItem.dataMax).append("}");
            }
            strBuff.append("]}");
        }
        strBuff.append("]}");
    }
}
