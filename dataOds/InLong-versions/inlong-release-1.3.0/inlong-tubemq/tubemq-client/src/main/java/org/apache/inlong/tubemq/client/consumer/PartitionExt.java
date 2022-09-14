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

package org.apache.inlong.tubemq.client.consumer;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlItem;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlResult;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlRuleHandler;

/**
 * Partition Extension class. Add flow control mechanism.
 *
 * This class will record consume context, and calculate the consume message size per minute based
 * on the predefined strategy.
 */
public class PartitionExt extends Partition {
    private static final long serialVersionUID = 7587342323917872253L;
    private final FlowCtrlRuleHandler groupFlowCtrlRuleHandler;
    private final FlowCtrlRuleHandler defFlowCtrlRuleHandler;
    private long nextLimitUpdateTime = 0;
    private FlowCtrlResult curFlowCtrlVal;
    private long nextStatTime = 0L;
    private long lastGetTime = 0L;
    private int recvMsgSize = 0;
    private FlowCtrlItem filterCtrlItem = new FlowCtrlItem(3, -2, -2, -2);
    private long recvMsgInMin = 0L;
    private long limitMsgInSec = 0L;
    private long lastDataRdDlt = TBaseConstants.META_VALUE_UNDEFINED;
    private int totalRcvZeroCount = 0;
    private long lastRptTIme;
    private int reqProcType;
    private int errCode;
    private boolean isEscLimit;
    private int msgSize;
    private long limitDlt;
    private long curDataDlt;
    private boolean isRequireSlow = false;
    private boolean isLastPackConsumed = false;

    public PartitionExt(final FlowCtrlRuleHandler groupFlowCtrlRuleHandler,
                        final FlowCtrlRuleHandler defFlowCtrlRuleHandler,
                        final BrokerInfo broker, final String topic, final int partitionId) {
        super(broker, topic, partitionId);
        this.groupFlowCtrlRuleHandler = groupFlowCtrlRuleHandler;
        this.defFlowCtrlRuleHandler = defFlowCtrlRuleHandler;
    }

    public long procConsumeResult(boolean isFilterConsume) {
        long dltTime = System.currentTimeMillis() - this.lastRptTIme;
        return procConsumeResult(isFilterConsume, this.reqProcType,
                this.errCode, this.msgSize, this.isEscLimit,
                this.limitDlt, this.curDataDlt, this.isRequireSlow) - dltTime;
    }

    /**
     * Process the consume result.
     *
     * @param isFilterConsume if current consume should be filtered
     * @param reqProcType     type information
     * @param errCode         error code
     * @param msgSize         message size
     * @param isReqEscLimit   if the rsplimitDlt is ignored in current consume
     * @param rsplimitDlt     max offset of the current consume
     * @param lastDataDlt     offset of the last data fetch
     * @param isRequireSlow  if the server requires slow down
     * @return message size per minute
     */
    public long procConsumeResult(boolean isFilterConsume, int reqProcType, int errCode,
                                  int msgSize, boolean isReqEscLimit, long rsplimitDlt,
                                  long lastDataDlt, boolean isRequireSlow) {
        if (lastDataDlt >= 0) {
            this.lastDataRdDlt = lastDataDlt;
        }
        this.recvMsgSize += msgSize;
        this.recvMsgInMin += msgSize;
        long currTime = System.currentTimeMillis();
        checkAndCalcDataLimit(currTime);
        if (errCode != TErrCodeConstants.NOT_FOUND
                && errCode != TErrCodeConstants.SUCCESS) {
            return rsplimitDlt;
        }
        if (msgSize == 0 && errCode != TErrCodeConstants.SUCCESS) {
            this.totalRcvZeroCount += 1;
        } else {
            this.totalRcvZeroCount = 0;
        }
        if (this.totalRcvZeroCount > 0) {
            if (this.groupFlowCtrlRuleHandler.getMinZeroCnt() != Integer.MAX_VALUE) {
                return groupFlowCtrlRuleHandler
                        .getCurFreqLimitTime(this.totalRcvZeroCount, (int) rsplimitDlt);
            } else {
                return defFlowCtrlRuleHandler
                        .getCurFreqLimitTime(this.totalRcvZeroCount, (int) rsplimitDlt);
            }
        }
        if (isReqEscLimit) {
            return 0;
        } else {
            if (this.recvMsgInMin >= this.curFlowCtrlVal.dataLtInSize
                    || this.recvMsgSize >= this.limitMsgInSec) {
                return this.curFlowCtrlVal.freqLtInMs > rsplimitDlt
                        ? this.curFlowCtrlVal.freqLtInMs : rsplimitDlt;
            }
            if (errCode == TErrCodeConstants.SUCCESS) {
                if (isFilterConsume && filterCtrlItem.getFreqLtInMs() >= 0) {
                    if (isRequireSlow) {
                        return this.filterCtrlItem.getZeroCnt();
                    } else {
                        return this.filterCtrlItem.getFreqLtInMs();
                    }
                } else if (!isFilterConsume && filterCtrlItem.getDataLtInSZ() >= 0) {
                    return this.filterCtrlItem.getDataLtInSZ();
                }
            }
            return rsplimitDlt;
        }
    }

    public long getCurDataDlt() {
        return curDataDlt;
    }

    public FlowCtrlResult getCurFlowCtrlVal() {
        return curFlowCtrlVal;
    }

    public long getRecvMsgInMin() {
        return recvMsgInMin;
    }

    public long getLimitMsgInSec() {
        return limitMsgInSec;
    }

    public long getLastRptTIme() {
        return lastRptTIme;
    }

    public int getReqProcType() {
        return reqProcType;
    }

    public int getErrCode() {
        return errCode;
    }

    public boolean isEscLimit() {
        return isEscLimit;
    }

    public int getMsgSize() {
        return msgSize;
    }

    public long getLimitDlt() {
        return limitDlt;
    }

    public boolean isLastPackConsumed() {
        return isLastPackConsumed;
    }

    public void setLastPackConsumed(boolean lastPackConsumed) {
        isLastPackConsumed = lastPackConsumed;
    }

    public boolean getAndResetLastPackConsumed() {
        boolean curVal = this.isLastPackConsumed;
        this.isLastPackConsumed = false;
        return curVal;
    }

    public void setPullTempData(int reqProcType, int errCode,
                                boolean isEscLimit, int msgSize,
                                long limitDlt, long curDataDlt,
                                boolean isRequireSlow) {
        this.lastRptTIme = System.currentTimeMillis();
        this.reqProcType = reqProcType;
        this.errCode = errCode;
        this.isEscLimit = isEscLimit;
        this.msgSize = msgSize;
        this.limitDlt = limitDlt;
        this.curDataDlt = curDataDlt;
        this.isRequireSlow = isRequireSlow;
    }

    public void clearPullTempData() {
        this.lastRptTIme = -1;
        this.reqProcType = -1;
        this.errCode = -1;
        this.isEscLimit = false;
        this.msgSize = -1;
        this.limitDlt = -1;
        this.curDataDlt = -1;

    }

    public long getNextLimitUpdateTime() {
        return nextLimitUpdateTime;
    }

    public void setNextLimitUpdateTime(long nextLimitUpdateTime) {
        this.nextLimitUpdateTime = nextLimitUpdateTime;
    }

    public long getCurAllowedMsgSize() {
        return this.curFlowCtrlVal.dataLtInSize;
    }

    public void setCurAllowedMsgSize(int curAllowedMsgSize) {
        this.curFlowCtrlVal.setDataLtInSize(curAllowedMsgSize);
    }

    public long getNextStatTime() {
        return nextStatTime;
    }

    public void setNextStatTime(long nextStatTime) {
        this.nextStatTime = nextStatTime;
    }

    public long getLastGetTime() {
        return lastGetTime;
    }

    public void setLastGetTime(long lastGetTime) {
        this.lastGetTime = lastGetTime;
    }

    public int getRecvMsgSize() {
        return recvMsgSize;
    }

    public void setRecvMsgSize(int recvMsgSize) {
        this.recvMsgSize = recvMsgSize;
    }

    public long getLastDataRdDlt() {
        return lastDataRdDlt;
    }

    public void setLastDataRdDlt(long lastDataRdDlt) {
        this.lastDataRdDlt = lastDataRdDlt;
    }

    public int getTotalRcvZeroCount() {
        return totalRcvZeroCount;
    }

    public void setTotalRcvZeroCount(int totalRcvZeroCount) {
        this.totalRcvZeroCount = totalRcvZeroCount;
    }

    /**
     * Check and update current control flow threshold.
     *
     * @param currTime current time
     */
    private void checkAndCalcDataLimit(long currTime) {
        if (currTime > nextLimitUpdateTime) {
            this.recvMsgSize = 0;
            this.recvMsgInMin = 0;
            if (this.lastDataRdDlt < 0) {
                this.curFlowCtrlVal = new FlowCtrlResult(Integer.MAX_VALUE, 20);
            } else {
                this.curFlowCtrlVal =
                        groupFlowCtrlRuleHandler.getCurDataLimit(this.lastDataRdDlt);
                this.filterCtrlItem = groupFlowCtrlRuleHandler.getFilterCtrlItem();
                if (this.curFlowCtrlVal == null) {
                    this.curFlowCtrlVal =
                            defFlowCtrlRuleHandler.getCurDataLimit(this.lastDataRdDlt);
                    if (this.curFlowCtrlVal == null) {
                        this.curFlowCtrlVal = new FlowCtrlResult(Long.MAX_VALUE, 0);
                    }
                }
                if (this.filterCtrlItem.getFreqLtInMs() < 0) {
                    this.filterCtrlItem = defFlowCtrlRuleHandler.getFilterCtrlItem();
                }
                currTime = System.currentTimeMillis();
            }
            this.limitMsgInSec = this.curFlowCtrlVal.dataLtInSize / 12;
            this.nextLimitUpdateTime = currTime + TBaseConstants.CFG_FC_MAX_LIMITING_DURATION;
            this.nextStatTime = currTime + TBaseConstants.CFG_FC_MAX_SAMPLING_PERIOD;
        } else if (currTime > nextStatTime) {
            this.recvMsgSize = 0;
            this.nextStatTime = currTime + TBaseConstants.CFG_FC_MAX_SAMPLING_PERIOD;
        }
    }
}
