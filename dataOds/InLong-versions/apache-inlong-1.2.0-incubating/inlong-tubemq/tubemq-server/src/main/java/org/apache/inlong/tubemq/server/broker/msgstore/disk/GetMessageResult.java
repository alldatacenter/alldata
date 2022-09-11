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

package org.apache.inlong.tubemq.server.broker.msgstore.disk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker.TransferedMessage;
import org.apache.inlong.tubemq.server.broker.stats.TrafficInfo;

/**
 * Broker's reply to Consumer's GetMessage request.
 */
public class GetMessageResult {
    public boolean isSuccess;
    public int retCode = -1;
    public String errInfo;
    public long reqOffset;
    public int lastReadOffset = -2;
    public long lastRdDataOffset;
    public int totalMsgSize;
    public long waitTime = -1;
    public boolean isSlowFreq = false;
    public boolean isFromSsdFile = false;
    public HashMap<String, TrafficInfo> tmpCounters = new HashMap<>();
    public List<TransferedMessage> transferedMessageList = new ArrayList<>();
    public long maxOffset = TBaseConstants.META_VALUE_UNDEFINED;

    public GetMessageResult(boolean isSuccess, int retCode, final String errInfo,
                            final long reqOffset, final int lastReadOffset,
                            final long lastRdDataOffset, final int totalSize,
                            HashMap<String, TrafficInfo> tmpCounters,
                            List<TransferedMessage> transferedMessageList) {
        this(isSuccess, retCode, errInfo, reqOffset, lastReadOffset,
                lastRdDataOffset, totalSize, tmpCounters, transferedMessageList, false);
    }

    public GetMessageResult(boolean isSuccess, int retCode, final String errInfo,
                            final long reqOffset, final int lastReadOffset,
                            final long lastRdDataOffset, final int totalSize,
                            HashMap<String, TrafficInfo> tmpCounters,
                            List<TransferedMessage> transferedMessageList,
                            boolean isFromSsdFile) {
        this.isSuccess = isSuccess;
        this.errInfo = errInfo;
        this.retCode = retCode;
        this.tmpCounters = tmpCounters;
        this.reqOffset = reqOffset;
        this.lastReadOffset = lastReadOffset;
        this.lastRdDataOffset = lastRdDataOffset;
        this.totalMsgSize = totalSize;
        this.transferedMessageList = transferedMessageList;
        this.isFromSsdFile = isFromSsdFile;
    }

    public GetMessageResult(boolean isSuccess,
                            int retCode,
                            final long reqOffset,
                            final int lastReadOffset,
                            final String errInfo) {
        this.isSuccess = isSuccess;
        this.retCode = retCode;
        this.errInfo = errInfo;
        this.reqOffset = reqOffset;
        this.lastReadOffset = lastReadOffset;
    }

    public GetMessageResult(boolean isSuccess, int retCode,
                            final long reqOffset, final int lastReadOffset,
                            final long waitTime, final String errInfo) {
        this.isSuccess = isSuccess;
        this.retCode = retCode;
        this.errInfo = errInfo;
        this.reqOffset = reqOffset;
        this.lastReadOffset = lastReadOffset;
        this.waitTime = waitTime;
    }

    public boolean isSlowFreq() {
        return isSlowFreq;
    }

    public void setSlowFreq(boolean slowFreq) {
        isSlowFreq = slowFreq;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public HashMap<String, TrafficInfo> getTmpCounters() {
        return tmpCounters;
    }

    public void setTmpCounters(HashMap<String, TrafficInfo> tmpCounters) {
        this.tmpCounters = tmpCounters;
    }

    public List<TransferedMessage> getTransferedMessageList() {
        return transferedMessageList;
    }

    public void setTransferedMessageList(List<TransferedMessage> transferedMessageList) {
        this.transferedMessageList = transferedMessageList;
    }

    public boolean isFromSsdFile() {
        return isFromSsdFile;
    }

    public void setFromSsdFile(boolean isFromSsdFile) {
        this.isFromSsdFile = isFromSsdFile;
    }

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getErrInfo() {
        return errInfo;
    }

    public void setErrInfo(String errInfo) {
        this.errInfo = errInfo;
    }

    public int getLastReadOffset() {
        return lastReadOffset;
    }

    public void setLastReadOffset(int lastReadOffset) {
        this.lastReadOffset = lastReadOffset;
    }

    public long getReqOffset() {
        return reqOffset;
    }

    public void setReqOffset(long reqOffset) {
        this.reqOffset = reqOffset;
    }

    public long getMaxOffset() {
        return maxOffset;
    }

    public void setMaxOffset(long maxOffset) {
        this.maxOffset = maxOffset;
    }
}
