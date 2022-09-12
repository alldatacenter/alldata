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

package org.apache.inlong.tubemq.corebase.policies;

import org.apache.inlong.tubemq.corebase.TBaseConstants;

public class FlowCtrlItem {

    private int type = 0;   // 0: current limit, 1: frequency limit, 2: SSD transfer 3: request frequency control
    private int startTime = TBaseConstants.META_VALUE_UNDEFINED;
    private int endTime = TBaseConstants.META_VALUE_UNDEFINED;
    private long dltInM = TBaseConstants.META_VALUE_UNDEFINED;
    private long dataLtInSZ = TBaseConstants.META_VALUE_UNDEFINED;
    private int freqLtInMs = TBaseConstants.META_VALUE_UNDEFINED;
    private int zeroCnt = TBaseConstants.META_VALUE_UNDEFINED;

    public FlowCtrlItem(int type, int startTime, int endTime,
                        long dltInM, long dataLtInSZ, int freqLtInMs) {
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dltInM = dltInM;
        this.dataLtInSZ = dataLtInSZ;
        this.freqLtInMs = freqLtInMs;
    }

    public FlowCtrlItem(int type,
                        int normFreqLtInMs, int filterFreqInMs, int minDataFilterFreqInMs) {
        this.type = type;
        this.freqLtInMs = filterFreqInMs;
        this.dataLtInSZ = normFreqLtInMs;
        this.zeroCnt = minDataFilterFreqInMs;
    }

    public FlowCtrlItem(int type, int zeroCnt, int freqLtInMs) {
        this.type = type;
        this.freqLtInMs = freqLtInMs;
        this.zeroCnt = zeroCnt;
    }

    public FlowCtrlItem(int type, int startTime, int endTime, long dltStartInM, long dltEndInM) {
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dltInM = dltStartInM;
        this.dataLtInSZ = dltEndInM;
    }

    public SSDCtrlResult getSSDStartDltProc(int currH, int currM) {
        if (this.type != 2) {
            return new SSDCtrlResult(TBaseConstants.META_VALUE_UNDEFINED,
                    TBaseConstants.META_VALUE_UNDEFINED);
        }
        int curTime = currH * 100 + currM;
        if (curTime < this.startTime || curTime > this.endTime) {
            return new SSDCtrlResult(TBaseConstants.META_VALUE_UNDEFINED,
                    TBaseConstants.META_VALUE_UNDEFINED);
        }
        return new SSDCtrlResult(dltInM, dataLtInSZ);
    }

    public int getFreLimit(int msgZeroCount) {
        if (this.type != 1) {
            return TBaseConstants.META_VALUE_UNDEFINED;
        }
        if (msgZeroCount >= this.zeroCnt) {
            return this.freqLtInMs;
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public FlowCtrlResult getDataLimit(long dltInM, int currH, int currM) {
        if (this.type != 0 || dltInM <= this.dltInM) {
            return null;
        }
        int curTime = currH * 100 + currM;
        if (curTime < this.startTime || curTime > this.endTime) {
            return null;
        }
        return new FlowCtrlResult(this.dataLtInSZ, this.freqLtInMs);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final FlowCtrlItem other = (FlowCtrlItem) obj;
        if (this.type != other.type
                || this.startTime != other.startTime
                || this.endTime != other.endTime
                || this.dataLtInSZ != other.dataLtInSZ
                || this.freqLtInMs != other.freqLtInMs
                || this.zeroCnt != other.zeroCnt) {
            return false;
        }
        return true;
    }

    public int getType() {
        return type;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public long getDltInM() {
        return dltInM;
    }

    public long getDataLtInSZ() {
        return dataLtInSZ;
    }

    public int getFreqLtInMs() {
        return freqLtInMs;
    }

    public int getZeroCnt() {
        return zeroCnt;
    }

    /**
     * Build json result string buffer
     *
     * @param sBuilder   the string buffer
     * @return  the result content
     */
    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        switch (this.type) {
            case 1:
                return sBuilder.append("{\"zeroCnt\":").append(this.zeroCnt)
                        .append(",\"freqInMs\":").append(this.freqLtInMs).append("}");
            case 2:
                return sBuilder.append("{\"start\":\"")
                        .append(String.format("%02d", this.startTime / 100))
                        .append(":").append(String.format("%02d", this.startTime % 100))
                        .append("\",\"end\":\"")
                        .append(String.format("%02d", this.endTime / 100))
                        .append(":").append(String.format("%02d", this.endTime % 100))
                        .append("\",\"dltStInM\":").append(this.dltInM / 1024 / 1024)
                        .append(",\"dltEdInM\":").append(this.dataLtInSZ / 1024 / 1024).append("}");
            case 3:
                return sBuilder.append("{\"normFreqInMs\":").append((int) this.dataLtInSZ)
                        .append(",\"filterFreqInMs\":").append(this.freqLtInMs)
                        .append(",\"minDataFilterFreqInMs\":").append(this.zeroCnt).append("}");
            case 0:
            default:
                return sBuilder.append("{\"start\":\"")
                        .append(String.format("%02d", this.startTime / 100))
                        .append(":").append(String.format("%02d", this.startTime % 100))
                        .append("\",\"end\":\"")
                        .append(String.format("%02d", this.endTime / 100))
                        .append(":").append(String.format("%02d", this.endTime % 100))
                        .append("\",\"dltInM\":").append(this.dltInM)
                        .append(",\"limitInM\":").append(this.dataLtInSZ / 1024 / 1024)
                        .append(",\"freqInMs\":").append(this.freqLtInMs).append("}");
        }
    }
}
