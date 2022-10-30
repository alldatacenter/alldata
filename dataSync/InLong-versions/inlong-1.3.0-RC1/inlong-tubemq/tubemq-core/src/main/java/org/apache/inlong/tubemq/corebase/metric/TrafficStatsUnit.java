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

package org.apache.inlong.tubemq.corebase.metric;

import org.apache.inlong.tubemq.corebase.metric.impl.LongStatsCounter;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

/**
 * TrafficStatsUnit, Metric Statistics item Unit
 *
 * Currently includes the total number of messages and bytes
 * according to the statistics dimension, which can be expanded later as needed
 */
public class TrafficStatsUnit {
    // the traffic name
    private String trafficName;
    // the message count
    public LongStatsCounter msgCnt;
    // the message size
    public LongStatsCounter msgSize;

    /**
     * Accumulate the count of messages and message bytes.
     *
     * @param msgCntName    the specified count statistics item name
     * @param msgSizeName   the specified size statistics item name
     * @param prefix        the prefix of statistics items
     */
    public TrafficStatsUnit(String msgCntName, String msgSizeName, String prefix) {
        this.trafficName = prefix;
        this.msgCnt = new LongStatsCounter(msgCntName, prefix);
        this.msgSize = new LongStatsCounter(msgSizeName, prefix);
    }

    /**
     * Accumulate the count of messages and message bytes.
     *
     * @param msgCount  the specified message count
     * @param msgSize   the specified message size
     */
    public void addMsgCntAndSize(long msgCount, long msgSize) {
        this.msgCnt.addValue(msgCount);
        this.msgSize.addValue(msgSize);
    }

    /**
     * Get traffic information in json format.
     *
     * @param strBuff      the string information container
     * @param resetValue   whether reset value
     */
    public void getValue(StringBuilder strBuff, boolean resetValue) {
        if (!TStringUtils.isEmpty(this.trafficName)) {
            strBuff.append("\"").append(this.trafficName).append("\":");
        }
        if (resetValue) {
            strBuff.append("{\"")
                    .append(msgCnt.getShortName()).append("\":")
                    .append(msgCnt.getAndResetValue()).append(",\"")
                    .append(msgSize.getShortName()).append("\":")
                    .append(msgSize.getAndResetValue()).append("}");
        } else {
            strBuff.append("{\"")
                    .append(msgCnt.getShortName()).append("\":")
                    .append(msgCnt.getValue()).append(",\"")
                    .append(msgSize.getShortName()).append("\":")
                    .append(msgSize.getValue()).append("}");
        }
    }
}
