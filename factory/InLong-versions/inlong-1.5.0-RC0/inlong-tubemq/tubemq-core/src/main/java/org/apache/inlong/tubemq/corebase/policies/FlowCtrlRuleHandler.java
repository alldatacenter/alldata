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

package org.apache.inlong.tubemq.corebase.policies;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow control rule processing logic, including parsing the flow control json string,
 * obtaining the largest and smallest flow control values of each type to improve
 * the processing speed
 */
public class FlowCtrlRuleHandler {

    private final boolean isDefaultHandler;
    private final String flowCtrlName;
    private static final Logger logger =
            LoggerFactory.getLogger(FlowCtrlRuleHandler.class);
    private final TimeZone timeZone = TimeZone.getTimeZone("GMT+8:00");
    private final ReentrantLock writeLock = new ReentrantLock();
    // Flow control ID and string information obtained from the server
    private AtomicLong flowCtrlId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private AtomicInteger qryPriorityId =
            new AtomicInteger(TBaseConstants.META_VALUE_UNDEFINED);
    private String strFlowCtrlInfo;
    // The maximum interval of the flow control extracts the set of values,
    // improving the efficiency of the search return in the range
    private AtomicInteger minZeroCnt =
            new AtomicInteger(Integer.MAX_VALUE);
    private AtomicLong minDataLimitDlt =
            new AtomicLong(Long.MAX_VALUE);
    private AtomicInteger dataLimitStartTime =
            new AtomicInteger(2500);
    private AtomicInteger dataLimitEndTime =
            new AtomicInteger(TBaseConstants.META_VALUE_UNDEFINED);
    private FlowCtrlItem filterCtrlItem =
            new FlowCtrlItem(3, TBaseConstants.META_VALUE_UNDEFINED,
                    TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED);
    private long lastUpdateTime =
            System.currentTimeMillis();
    // Decoded flow control rules
    private Map<Integer, List<FlowCtrlItem>> flowCtrlRuleSet =
            new ConcurrentHashMap<>();

    public FlowCtrlRuleHandler(boolean isDefault) {
        this.isDefaultHandler = isDefault;
        if (this.isDefaultHandler) {
            flowCtrlName = "Default_FlowCtrl";
        } else {
            flowCtrlName = "Group_FlowCtrl";
        }

    }

    /**
     * Parse flow control information and update stored cached old content
     *
     * @param qryPriorityId    the query priority id
     * @param flowCtrlId       flow control information id
     * @param flowCtrlInfo     flow control information content
     * @param strBuff          the string buffer
     * @throws Exception       the exception thrown
     */
    public void updateFlowCtrlInfo(int qryPriorityId, long flowCtrlId,
            String flowCtrlInfo, StringBuilder strBuff) throws Exception {
        if (flowCtrlId == this.flowCtrlId.get()) {
            return;
        }
        long befFlowCtrlId;
        int befQryPriorityId = TBaseConstants.META_VALUE_UNDEFINED;
        Map<Integer, List<FlowCtrlItem>> flowCtrlItemsMap = null;
        if (TStringUtils.isNotBlank(flowCtrlInfo)) {
            flowCtrlItemsMap = parseFlowCtrlInfo(flowCtrlInfo);
        }
        writeLock.lock();
        try {
            befFlowCtrlId = this.flowCtrlId.getAndSet(flowCtrlId);
            this.strFlowCtrlInfo = flowCtrlInfo;
            clearStatisData();
            if (flowCtrlItemsMap == null
                    || flowCtrlItemsMap.isEmpty()) {
                this.flowCtrlRuleSet.clear();
            } else {
                flowCtrlRuleSet = flowCtrlItemsMap;
                initialStatisData();
            }
            if (qryPriorityId != TBaseConstants.META_VALUE_UNDEFINED
                    && qryPriorityId != this.qryPriorityId.get()) {
                befQryPriorityId = this.qryPriorityId.getAndSet(qryPriorityId);
            }
            this.lastUpdateTime = System.currentTimeMillis();
        } finally {
            writeLock.unlock();
        }
        strBuff.append("[Flow Ctrl] Update ").append(flowCtrlName)
                .append(", flowId from ").append(befFlowCtrlId)
                .append(" to ").append(flowCtrlId);
        if (qryPriorityId != TBaseConstants.META_VALUE_UNDEFINED
                && qryPriorityId != befQryPriorityId) {
            strBuff.append(", qryPriorityId from ").append(befQryPriorityId)
                    .append(" to ").append(qryPriorityId);
        }
        logger.info(strBuff.toString());
        strBuff.delete(0, strBuff.length());
    }

    /**
     * Get current data lag limit strategy
     *
     * @param lastDataDlt      current consumption lag of data
     * @return FlowCtrlResult  current flow control policy
     */
    public FlowCtrlResult getCurDataLimit(long lastDataDlt) {
        Calendar rightNow = Calendar.getInstance(timeZone);
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minu = rightNow.get(Calendar.MINUTE);
        int curTime = hour * 100 + minu;
        if (lastDataDlt < this.minDataLimitDlt.get()
                || curTime < this.dataLimitStartTime.get()
                || curTime > this.dataLimitEndTime.get()) {
            return null;
        }
        List<FlowCtrlItem> flowCtrlItemList =
                flowCtrlRuleSet.get(0);
        if (flowCtrlItemList == null
                || flowCtrlItemList.isEmpty()) {
            return null;
        }
        for (FlowCtrlItem flowCtrlItem : flowCtrlItemList) {
            if (flowCtrlItem == null) {
                continue;
            }
            FlowCtrlResult flowCtrlResult =
                    flowCtrlItem.getDataLimit(lastDataDlt, hour, minu);
            if (flowCtrlResult != null) {
                return flowCtrlResult;
            }
        }
        return null;
    }

    public int getNormFreqInMs() {
        return this.filterCtrlItem.getFreqLtInMs();
    }

    public int getMinDataFreqInMs() {
        return this.filterCtrlItem.getZeroCnt();
    }

    public FlowCtrlItem getFilterCtrlItem() {
        return this.filterCtrlItem;
    }

    /**
     * Initial data limit statistics
     */
    private void initialStatisData() {
        initialDataLimitStatisInfo();
        initialFreqLimitStatisInfo();
        initialLowFetchLimitStatisInfo();
    }

    /**
     * Initial data limit statistics
     */
    private void initialDataLimitStatisInfo() {
        List<FlowCtrlItem> flowCtrlItemList = this.flowCtrlRuleSet.get(0);
        if (flowCtrlItemList != null
                && !flowCtrlItemList.isEmpty()) {
            for (FlowCtrlItem flowCtrlItem : flowCtrlItemList) {
                if (flowCtrlItem == null) {
                    continue;
                }
                if (flowCtrlItem.getType() != 0) {
                    continue;
                }
                if (flowCtrlItem.getDltInM() < this.minDataLimitDlt.get()) {
                    this.minDataLimitDlt.set(flowCtrlItem.getDltInM());
                }

                if (flowCtrlItem.getStartTime() < this.dataLimitStartTime.get()) {
                    this.dataLimitStartTime.set(flowCtrlItem.getStartTime());
                }
                if (flowCtrlItem.getEndTime() > this.dataLimitEndTime.get()) {
                    this.dataLimitEndTime.set(flowCtrlItem.getEndTime());
                }
            }
        }
    }

    private void initialFreqLimitStatisInfo() {
        List<FlowCtrlItem> flowCtrlItemList = flowCtrlRuleSet.get(1);
        if (flowCtrlItemList != null && !flowCtrlItemList.isEmpty()) {
            for (FlowCtrlItem flowCtrlItem : flowCtrlItemList) {
                if (flowCtrlItem == null) {
                    continue;
                }
                if (flowCtrlItem.getType() != 1) {
                    continue;
                }
                if (flowCtrlItem.getZeroCnt() < this.minZeroCnt.get()) {
                    this.minZeroCnt.set(flowCtrlItem.getZeroCnt());
                }
            }
        }
    }

    private void initialLowFetchLimitStatisInfo() {
        List<FlowCtrlItem> flowCtrlItemList = flowCtrlRuleSet.get(3);
        if (flowCtrlItemList != null && !flowCtrlItemList.isEmpty()) {
            for (FlowCtrlItem flowCtrlItem : flowCtrlItemList) {
                if (flowCtrlItem == null) {
                    continue;
                }
                if (flowCtrlItem.getType() != 3) {
                    continue;
                }
                this.filterCtrlItem = new FlowCtrlItem(3,
                        (int) flowCtrlItem.getDataLtInSZ(),
                        flowCtrlItem.getFreqLtInMs(),
                        flowCtrlItem.getZeroCnt());
            }
        }
    }

    private void clearStatisData() {
        this.minZeroCnt.set(Integer.MAX_VALUE);
        this.minDataLimitDlt.set(Long.MAX_VALUE);
        this.dataLimitStartTime.set(2500);
        this.dataLimitEndTime.set(TBaseConstants.META_VALUE_UNDEFINED);
        this.filterCtrlItem = new FlowCtrlItem(3, TBaseConstants.META_VALUE_UNDEFINED,
                TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED);
    }

    public int getMinZeroCnt() {
        return minZeroCnt.get();
    }

    /**
     * Get the current fetch frequency limit strategy
     *
     * @param msgZeroCnt   the continuous consumption count without messages
     * @param rcmVal       the default frequency limit value
     * @return             the required frequency limit value
     */
    public int getCurFreqLimitTime(int msgZeroCnt, int rcmVal) {
        if (msgZeroCnt < this.minZeroCnt.get()) {
            return rcmVal;
        }
        List<FlowCtrlItem> flowCtrlItemList =
                flowCtrlRuleSet.get(1);
        if (flowCtrlItemList == null
                || flowCtrlItemList.isEmpty()) {
            return rcmVal;
        }
        for (FlowCtrlItem flowCtrlItem : flowCtrlItemList) {
            if (flowCtrlItem == null) {
                continue;
            }
            int ruleVal = flowCtrlItem.getFreLimit(msgZeroCnt);
            if (ruleVal >= 0) {
                return ruleVal;
            }
        }
        return rcmVal;
    }

    public int getQryPriorityId() {
        return qryPriorityId.get();
    }

    /**
     * Set query priority id value
     * @param qryPriorityId   need to set value
     */
    public void setQryPriorityId(int qryPriorityId) {
        this.qryPriorityId.set(qryPriorityId);
    }

    public long getFlowCtrlId() {
        return flowCtrlId.get();
    }

    public void clear() {
        writeLock.lock();
        try {
            this.strFlowCtrlInfo = "";
            this.flowCtrlRuleSet.clear();
            this.flowCtrlId.set(TBaseConstants.META_VALUE_UNDEFINED);
            this.qryPriorityId.set(TBaseConstants.META_VALUE_UNDEFINED);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Parse FlowCtrlInfo value
     *
     * @param flowCtrlInfo    flowCtrlInfo json value
     * @return                parse result
     * @throws Exception      Exception thrown
     */
    public Map<Integer, List<FlowCtrlItem>> parseFlowCtrlInfo(final String flowCtrlInfo)
            throws Exception {
        Map<Integer, List<FlowCtrlItem>> flowCtrlMap = new ConcurrentHashMap<>();
        if (TStringUtils.isBlank(flowCtrlInfo)) {
            throw new Exception("Parsing error, flowCtrlInfo value is blank!");
        }
        JsonArray objArray = null;
        try {
            objArray = JsonParser.parseString(flowCtrlInfo).getAsJsonArray();
        } catch (Throwable e1) {
            throw new Exception("Parse flowCtrlInfo value failure", e1);
        }
        if (objArray == null) {
            throw new Exception("Parsing error, flowCtrlInfo value must be valid json format!");
        }
        if (objArray.size() == 0) {
            return flowCtrlMap;
        }
        try {
            int recordNo;
            List<FlowCtrlItem> flowCtrlItemList;
            for (int i = 0; i < objArray.size(); i++) {
                JsonElement jsonItem = objArray.get(i);
                if (jsonItem == null) {
                    continue;
                }
                recordNo = i + 1;
                JsonObject jsonObject = jsonItem.getAsJsonObject();
                if (!jsonObject.has("type")) {
                    throw new Exception(new StringBuilder(512)
                            .append("FIELD type is required in record(")
                            .append(recordNo).append(") of flowCtrlInfo value!").toString());
                }
                int typeVal = jsonObject.get("type").getAsInt();
                if (typeVal < 0 || typeVal > 3) {
                    throw new Exception(new StringBuilder(512)
                            .append("the value of FIELD type must in [0,1,3] in record(")
                            .append(recordNo).append(") of flowCtrlInfo value!").toString());
                }
                switch (typeVal) {
                    case 1:
                        flowCtrlItemList = parseFreqLimit(recordNo, typeVal, jsonObject);
                        break;

                    case 2: /* Deprecated */
                        flowCtrlItemList = null;
                        break;

                    case 3:
                        flowCtrlItemList = parseLowFetchLimit(recordNo, typeVal, jsonObject);
                        break;

                    case 0:
                    default:
                        typeVal = 0;
                        flowCtrlItemList = parseDataLimit(recordNo, typeVal, jsonObject);
                        break;
                }
                if (flowCtrlItemList != null && !flowCtrlItemList.isEmpty()) {
                    flowCtrlMap.put(typeVal, flowCtrlItemList);
                }
            }
        } catch (Throwable e2) {
            throw new Exception(new StringBuilder(512)
                    .append("Parse flowCtrlInfo value failure, ")
                    .append(e2.getMessage()).toString());
        }
        return flowCtrlMap;
    }

    /**
     * lizard forgives
     *
     *  Parse data consumption limit rule info
     *
     * @param recordNo    record no
     * @param typeVal     type value
     * @param jsonObject  record json value
     * @return             parsed result
     * @throws Exception   Exception thrown
     */
    private List<FlowCtrlItem> parseDataLimit(int recordNo, int typeVal,
            JsonObject jsonObject) throws Exception {
        if (jsonObject == null || jsonObject.get("type").getAsInt() != 0) {
            throw new Exception(new StringBuilder(512)
                    .append("parse data_limit rule failure in record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        if (!jsonObject.has("rule")) {
            throw new Exception(new StringBuilder(512)
                    .append("FIELD rule is required in data_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        JsonArray ruleArray = jsonObject.get("rule").getAsJsonArray();
        if (ruleArray == null) {
            throw new Exception(new StringBuilder(512)
                    .append("emtpy rule define in data_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        // parse rule item
        int itemNo;
        ArrayList<FlowCtrlItem> flowCtrlItems = new ArrayList<>();
        for (int index = 0; index < ruleArray.size(); index++) {
            itemNo = index + 1;
            JsonObject ruleObject = ruleArray.get(index).getAsJsonObject();
            int startTime = validAndGetTimeValue(ruleObject, "start", itemNo, recordNo);
            int endTime = validAndGetTimeValue(ruleObject, "end", itemNo, recordNo);
            if (startTime >= endTime) {
                throw new Exception(new StringBuilder(512)
                        .append("the value of FIELD start must lower than the value FIELD end ")
                        .append("in data_limit item(").append(itemNo)
                        .append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            if (!ruleObject.has("dltInM")) {
                throw new Exception(new StringBuilder(512)
                        .append("FIELD dltInM is required in data_limit item(")
                        .append(itemNo).append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            long dltVal = ruleObject.get("dltInM").getAsLong();
            if (dltVal <= 20) {
                throw new Exception(new StringBuilder(512)
                        .append("the value of FIELD dltInM must be greater than 20 ")
                        .append("in data_limit item(").append(itemNo)
                        .append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            if (!ruleObject.has("limitInM")) {
                throw new Exception(new StringBuilder(512)
                        .append("FIELD limitInM is required in data_limit item(")
                        .append(itemNo).append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            long dataLimitInM = ruleObject.get("limitInM").getAsLong();
            if (dataLimitInM < 0) {
                throw new Exception(new StringBuilder(512)
                        .append("the value of FIELD limitInM must be greater than or equal to 0 ")
                        .append("in data_limit item(").append(itemNo)
                        .append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            dataLimitInM = dataLimitInM * 1024 * 1024;
            if (!ruleObject.has("freqInMs")) {
                throw new Exception(new StringBuilder(512)
                        .append("FIELD freqInMs is required in data_limit item(")
                        .append(itemNo).append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            int freqInMs = ruleObject.get("freqInMs").getAsInt();
            if (freqInMs < 200) {
                throw new Exception(new StringBuilder(512)
                        .append("the value of FIELD freqInMs must be greater than or equal to 200 ")
                        .append("in data_limit item(").append(itemNo)
                        .append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            flowCtrlItems.add(new FlowCtrlItem(typeVal,
                    startTime, endTime, dltVal, dataLimitInM, freqInMs));
        }
        if (flowCtrlItems.isEmpty()) {
            throw new Exception(new StringBuilder(512)
                    .append("not found valid rule define in data_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        Collections.sort(flowCtrlItems, new Comparator<FlowCtrlItem>() {

            @Override
            public int compare(final FlowCtrlItem o1, final FlowCtrlItem o2) {
                if (o1.getStartTime() > o2.getStartTime()) {
                    return 1;
                } else if (o1.getStartTime() < o2.getStartTime()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        return flowCtrlItems;
    }

    /**
     *  Parse frequent limit rule info
     *
     * @param recordNo    record no
     * @param typeVal     type value
     * @param jsonObject  record json value
     * @return            parsed result
     * @throws Exception  Exception thrown
     */
    private List<FlowCtrlItem> parseFreqLimit(int recordNo, int typeVal,
            JsonObject jsonObject) throws Exception {
        if (jsonObject == null || jsonObject.get("type").getAsInt() != 1) {
            throw new Exception(new StringBuilder(512)
                    .append("parse freq_limit rule failure in record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        if (!jsonObject.has("rule")) {
            throw new Exception(new StringBuilder(512)
                    .append("FIELD rule is required in freq_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        JsonArray ruleArray = jsonObject.get("rule").getAsJsonArray();
        if (ruleArray == null) {
            throw new Exception(new StringBuilder(512)
                    .append("emtpy rule define in freq_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        int itemNo;
        ArrayList<FlowCtrlItem> flowCtrlItems = new ArrayList<>();
        for (int index = 0; index < ruleArray.size(); index++) {
            itemNo = index + 1;
            JsonObject ruleObject = ruleArray.get(index).getAsJsonObject();
            if (!ruleObject.has("zeroCnt")) {
                throw new Exception(new StringBuilder(512)
                        .append("FIELD zeroCnt is required in freq_limit item(")
                        .append(itemNo).append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            int zeroCnt = ruleObject.get("zeroCnt").getAsInt();
            if (zeroCnt < 1) {
                throw new Exception(new StringBuilder(512)
                        .append("the value of FIELD zeroCnt must be greater than or equal to 1 ")
                        .append("in freq_limit item(").append(itemNo)
                        .append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            if (!ruleObject.has("freqInMs")) {
                throw new Exception(new StringBuilder(512)
                        .append("FIELD freqInMs is required in freq_limit item(")
                        .append(itemNo).append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            int freqInMs = ruleObject.get("freqInMs").getAsInt();
            if (freqInMs < 0) {
                throw new Exception(new StringBuilder(512)
                        .append("the value of FIELD freqInMs must be greater than or equal to 0 ")
                        .append("in freq_limit item(").append(itemNo)
                        .append(").record(").append(recordNo)
                        .append(") of flowCtrlInfo value!").toString());
            }
            flowCtrlItems.add(new FlowCtrlItem(typeVal, zeroCnt, freqInMs));
        }
        if (flowCtrlItems.isEmpty()) {
            throw new Exception(new StringBuilder(512)
                    .append("not found valid rule define in freq_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        // sort rule set by the value of FIELD zeroCnt
        Collections.sort(flowCtrlItems, new Comparator<FlowCtrlItem>() {

            @Override
            public int compare(final FlowCtrlItem o1, final FlowCtrlItem o2) {
                if (o1.getZeroCnt() > o2.getZeroCnt()) {
                    return -1;
                } else if (o1.getZeroCnt() < o2.getZeroCnt()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return flowCtrlItems;
    }

    /**
     *  Parse low frequent fetch count
     *
     * @param recordNo    record no
     * @param typeVal     type value
     * @param jsonObject  record json value
     * @return            parsed result
     * @throws Exception   Exception thrown
     */
    private List<FlowCtrlItem> parseLowFetchLimit(int recordNo, int typeVal,
            JsonObject jsonObject) throws Exception {
        if (jsonObject == null || jsonObject.get("type").getAsInt() != 3) {
            throw new Exception(new StringBuilder(512)
                    .append("parse low_fetch_limit rule failure in record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        if (!jsonObject.has("rule")) {
            throw new Exception(new StringBuilder(512)
                    .append("FIELD rule is required in low_fetch_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        JsonArray ruleArray = jsonObject.get("rule").getAsJsonArray();
        if (ruleArray == null) {
            throw new Exception(new StringBuilder(512)
                    .append("emtpy rule define in low_fetch_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        if (ruleArray.size() > 1) {
            throw new Exception(new StringBuilder(512)
                    .append("only allow set one rule in low_fetch_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        int itemNo;
        ArrayList<FlowCtrlItem> flowCtrlItems = new ArrayList<>();
        // parse low_fetch_limit rule record
        for (int index = 0; index < ruleArray.size(); index++) {
            itemNo = index + 1;
            JsonObject ruleObject = ruleArray.get(index).getAsJsonObject();
            int normfreqInMs = 0;
            int filterFreqInMs = 0;
            int minDataFilterFreqInMs = 0;
            if (ruleObject.has("filterFreqInMs")
                    || ruleObject.has("minDataFilterFreqInMs")) {
                if (!ruleObject.has("filterFreqInMs")) {
                    throw new Exception(new StringBuilder(512)
                            .append("FIELD filterFreqInMs is required ")
                            .append("in low_fetch_limit item(").append(itemNo)
                            .append(").record(").append(recordNo)
                            .append(") of flowCtrlInfo value!").toString());
                }
                filterFreqInMs = ruleObject.get("filterFreqInMs").getAsInt();
                if (filterFreqInMs < 0 || filterFreqInMs > 300000) {
                    throw new Exception(new StringBuilder(512)
                            .append("the value of FIELD filterFreqInMs must in [0, 300000] ")
                            .append("in low_fetch_limit item(").append(itemNo)
                            .append(").record(").append(recordNo)
                            .append(") of flowCtrlInfo value!").toString());
                }
                if (!ruleObject.has("minDataFilterFreqInMs")) {
                    throw new Exception(new StringBuilder(512)
                            .append("FIELD minDataFilterFreqInMs is required ")
                            .append("in low_fetch_limit item(").append(itemNo)
                            .append(").record(").append(recordNo)
                            .append(") of flowCtrlInfo value!").toString());
                }
                minDataFilterFreqInMs = ruleObject.get("minDataFilterFreqInMs").getAsInt();
                if (minDataFilterFreqInMs < 0 || minDataFilterFreqInMs > 300000) {
                    throw new Exception(new StringBuilder(512)
                            .append("the value of FIELD minDataFilterFreqInMs must in [0, 300000] ")
                            .append("in low_fetch_limit item(").append(itemNo)
                            .append(").record(").append(recordNo)
                            .append(") of flowCtrlInfo value!").toString());
                }
                if (minDataFilterFreqInMs < filterFreqInMs) {
                    throw new Exception(new StringBuilder(512)
                            .append("the value of FIELD minDataFilterFreqInMs must be greater ")
                            .append("than or equal to the value of FIELD filterFreqInMs")
                            .append("in low_fetch_limit item(").append(itemNo)
                            .append(").record(").append(recordNo)
                            .append(") of flowCtrlInfo value!").toString());
                }
            }
            if (ruleObject.has("normFreqInMs")) {
                normfreqInMs = ruleObject.get("normFreqInMs").getAsInt();
                if (normfreqInMs < 0 || normfreqInMs > 300000) {
                    throw new Exception(new StringBuilder(512)
                            .append("the value of FIELD normFreqInMs must in [0, 300000] ")
                            .append("in low_fetch_limit item(").append(itemNo)
                            .append(").record(").append(recordNo)
                            .append(") of flowCtrlInfo value!").toString());
                }
            }
            flowCtrlItems.add(new FlowCtrlItem(typeVal,
                    normfreqInMs, filterFreqInMs, minDataFilterFreqInMs));
        }
        if (flowCtrlItems.isEmpty()) {
            throw new Exception(new StringBuilder(512)
                    .append("not found valid rule define in low_fetch_limit record(")
                    .append(recordNo).append(") of flowCtrlInfo value!").toString());
        }
        // sort rule set by the value of filterFreqInMs
        Collections.sort(flowCtrlItems, new Comparator<FlowCtrlItem>() {

            @Override
            public int compare(final FlowCtrlItem o1, final FlowCtrlItem o2) {
                if (o1.getFreqLtInMs() > o2.getFreqLtInMs()) {
                    return -1;
                } else if (o1.getFreqLtInMs() < o2.getFreqLtInMs()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return flowCtrlItems;
    }

    @Override
    public String toString() {
        return this.strFlowCtrlInfo;
    }

    /**
     *  Parse time information
     *
     * @param ruleObject   rule value object
     * @param fieldName     field name
     * @param itemNo        rule no
     * @param recordNo      record no
     * @return              parse result
     * @throws Exception    Exception thrown
     */
    private int validAndGetTimeValue(JsonObject ruleObject, String fieldName,
            int itemNo, int recordNo) throws Exception {
        if (!ruleObject.has(fieldName)) {
            throw new Exception(new StringBuilder(512)
                    .append("FIELD ").append(fieldName).append(" is required ")
                    .append("in data_limit item(").append(itemNo)
                    .append(").record(").append(recordNo)
                    .append(") of flowCtrlInfo value!").toString());
        }
        String strTimeVal = ruleObject.get(fieldName).getAsString();
        if (TStringUtils.isBlank(strTimeVal)) {
            throw new Exception(new StringBuilder(512)
                    .append("the value of FIELD ").append(fieldName)
                    .append(" is null or blank in data_limit item(").append(itemNo)
                    .append(").record(").append(recordNo)
                    .append(") of flowCtrlInfo value!").toString());
        }
        int timeHour = 0;
        int timeMin = 0;
        String[] startItems = strTimeVal.split(TokenConstants.ATTR_SEP);
        if ((startItems.length != 2)
                || TStringUtils.isBlank(startItems[0])
                || TStringUtils.isBlank(startItems[1])) {
            throw new Exception(new StringBuilder(512)
                    .append("illegal format, the value of FIELD ").append(fieldName)
                    .append(" must be 'aa:bb' and 'aa','bb' must be int value ")
                    .append("in data_limit item(").append(itemNo)
                    .append(").record(").append(recordNo)
                    .append(") of flowCtrlInfo value!").toString());
        }
        try {
            timeHour = Integer.parseInt(startItems[0]);
        } catch (Throwable e2) {
            throw new Exception(new StringBuilder(512)
                    .append("illegal format, the value of FIELD ").append(fieldName)
                    .append(" must be 'aa:bb' and 'aa' must be int value ")
                    .append("in data_limit item(").append(itemNo)
                    .append(").record(").append(recordNo)
                    .append(") of flowCtrlInfo value!").toString());
        }
        try {
            timeMin = Integer.parseInt(startItems[1]);
        } catch (Throwable e2) {
            throw new Exception(new StringBuilder(512)
                    .append("illegal format, the value of FIELD ").append(fieldName)
                    .append(" must be 'aa:bb' and 'bb' must be int value ")
                    .append("in data_limit item(").append(itemNo)
                    .append(").record(").append(recordNo)
                    .append(") of flowCtrlInfo value!").toString());
        }
        if (timeHour < 0 || timeHour > 24) {
            throw new Exception(new StringBuilder(512)
                    .append("illegal value, the value of FIELD ").append(fieldName)
                    .append("-hour value must in [0,23] in data_limit item(")
                    .append(itemNo).append(").record(").append(recordNo)
                    .append(") of flowCtrlInfo value!").toString());
        }
        if (timeMin < 0 || timeMin > 59) {
            throw new Exception(new StringBuilder(512)
                    .append("illegal value, the value of FIELD ").append(fieldName)
                    .append("-minute value must in [0,59] in data_limit item(")
                    .append(itemNo).append(").record(").append(recordNo)
                    .append(") of flowCtrlInfo value!").toString());
        }
        return timeHour * 100 + timeMin;
    }
}
