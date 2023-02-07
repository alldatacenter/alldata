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

package org.apache.inlong.tubemq.server.master.web.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.broker.stats.BrokerStatsType;
import org.apache.inlong.tubemq.server.common.TubeServerVersion;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.common.webbase.WebCallStatsHolder;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.TopicPSInfoManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumeGroupInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumeType;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.NodeRebInfo;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.apache.inlong.tubemq.server.master.stats.MasterStatsType;

public class WebOtherInfoHandler extends AbstractWebHandler {

    /**
     * Constructor
     *
     * @param master tube master
     */
    public WebOtherInfoHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_get_methods",
                "adminQueryAllMethods");
        registerQueryWebMethod("admin_query_sub_info",
                "getSubscribeInfo");
        registerQueryWebMethod("admin_query_consume_group_detail",
                "getConsumeGroupDetailInfo");
        // query master's version
        registerQueryWebMethod("admin_query_server_version",
                "adminQueryMasterVersion");
        // register query method
        registerQueryWebMethod("admin_get_metrics_info",
                "adminGetMetricsInfo");
        // Enable metrics statistics
        registerModifyWebMethod("admin_enable_stats",
                "adminEnableMetricsStats");
        // Disable metrics statistics
        registerModifyWebMethod("admin_disable_stats",
                "adminDisableMetricsStats");
        // Disable unnecessary statistics
        registerModifyWebMethod("admin_disable_all_stats",
                "adminDisableAllStats");
    }

    /**
     * Get all API methods supported by this version.
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryAllMethods(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        int totalCnt = getRegisteredMethods(sBuffer);
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Get subscription info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder getSubscribeInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> inGroupNameSet = (Set<String>) result.getRetData();
        if (inGroupNameSet.isEmpty()) {
            if (!WebParameterUtils.getStringParamValue(req,
                    WebFieldDef.COMPSCONSUMEGROUP, false, null, sBuffer, result)) {
                WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
                return sBuffer;
            }
            inGroupNameSet = (Set<String>) result.getRetData();
        }
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        TopicPSInfoManager topicPSInfoManager = master.getTopicPSInfoManager();
        Set<String> queryGroupSet =
                topicPSInfoManager.getGroupSetWithSubTopic(inGroupNameSet, topicNameSet);
        int totalCnt = 0;
        int topicCnt = 0;
        ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (String group : queryGroupSet) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"consumeGroup\":\"").append(group).append("\",\"topicSet\":[");
            topicCnt = 0;
            Set<String> topicSet = consumerHolder.getGroupTopicSet(group);
            int consumerCnt = consumerHolder.getConsumerCnt(group);
            for (String tmpTopic : topicSet) {
                if (topicCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("\"").append(tmpTopic).append("\"");
            }
            sBuffer.append("],\"consumerNum\":").append(consumerCnt).append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Get Master version info
     *
     * @param req       Http Servlet Request
     * @param strBuff   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryMasterVersion(HttpServletRequest req,
            StringBuilder strBuff,
            ProcessResult result) {
        WebParameterUtils.buildSuccessWithDataRetBegin(strBuff);
        strBuff.append("{\"version\":\"")
                .append(TubeServerVersion.SERVER_VERSION).append("\"}");
        WebParameterUtils.buildSuccessWithDataRetEnd(strBuff, 1);
        return strBuff;
    }

    /**
     * Get consume group detail info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    // #lizard forgives
    public StringBuilder getConsumeGroupDetailInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // get group name
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.GROUPNAME, true, null, sBuffer, result)) {
            if (!WebParameterUtils.getStringParamValue(req,
                    WebFieldDef.CONSUMEGROUP, true, null, sBuffer, result)) {
                WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
                return sBuffer;
            }
        }
        String strConsumeGroup = (String) result.getRetData();
        try {
            ConsumeType consumeType = ConsumeType.CONSUME_NORMAL;
            boolean isNotAllocate = false;
            boolean isSelectBig = true;
            String sessionKey = "";
            int reqSourceCount = -1;
            int curSourceCount = -1;
            long rebalanceCheckTime = -1;
            int defBClientRate = -2;
            int confBClientRate = -2;
            int curBClientRate = -2;
            int minRequireClientCnt = -2;
            int balanceStatus = -2;
            Set<String> topicSet = new HashSet<>();
            List<ConsumerInfo> consumerList = new ArrayList<>();
            Map<String, NodeRebInfo> nodeRebInfoMap = new ConcurrentHashMap<>();
            Map<String, TreeSet<String>> existedTopicConditions = new HashMap<>();
            ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
            ConsumeGroupInfo consumeGroupInfo = consumerHolder.getConsumeGroupInfo(strConsumeGroup);
            if (consumeGroupInfo != null) {
                if (consumeGroupInfo.getTopicSet() != null) {
                    topicSet = consumeGroupInfo.getTopicSet();
                }
                if (consumeGroupInfo.getConsumerInfoList() != null) {
                    consumerList = consumeGroupInfo.getConsumerInfoList();
                }
                if (consumeGroupInfo.getTopicConditions() != null) {
                    existedTopicConditions = consumeGroupInfo.getTopicConditions();
                }
                nodeRebInfoMap = consumeGroupInfo.getBalanceMap();
                consumeType = consumeGroupInfo.getConsumeType();
                balanceStatus = consumeGroupInfo.getBalanceChkStatus();
                defBClientRate = consumerHolder.getDefResourceRate();
                confBClientRate = consumeGroupInfo.getConfResourceRate();
                curBClientRate = consumeGroupInfo.getCurResourceRate();
                minRequireClientCnt = consumeGroupInfo.getMinReqClientCnt();
                if (consumeType == ConsumeType.CONSUME_BAND) {
                    isNotAllocate = consumeGroupInfo.isNotAllocate();
                    isSelectBig = consumeGroupInfo.isSelectedBig();
                    sessionKey = consumeGroupInfo.getSessionKey();
                    reqSourceCount = consumeGroupInfo.getSourceCount();
                    curSourceCount = consumeGroupInfo.getGroupCnt();
                    rebalanceCheckTime = consumeGroupInfo.getCurCheckCycle();
                }
            }
            sBuffer.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"")
                    .append(",\"count\":").append(consumerList.size()).append(",\"topicSet\":[");
            int itemCnt = 0;
            for (String topicItem : topicSet) {
                if (itemCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("\"").append(topicItem).append("\"");
            }
            sBuffer.append("],\"consumeGroup\":\"").append(strConsumeGroup).append("\",\"re-rebalance\":{");
            itemCnt = 0;
            for (Map.Entry<String, NodeRebInfo> entry : nodeRebInfoMap.entrySet()) {
                if (itemCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("\"").append(entry.getKey()).append("\":");
                sBuffer = entry.getValue().toJsonString(sBuffer);
            }
            sBuffer.append("},\"isBandConsume\":\"").append(consumeType.getName()).append("\"");
            // Append band consume info
            if (consumeType == ConsumeType.CONSUME_BAND) {
                sBuffer.append(",\"isNotAllocate\":").append(isNotAllocate)
                        .append(",\"sessionKey\":\"").append(sessionKey)
                        .append("\",\"isSelectBig\":").append(isSelectBig)
                        .append(",\"reqSourceCount\":").append(reqSourceCount)
                        .append(",\"curSourceCount\":").append(curSourceCount)
                        .append(",\"rebalanceCheckTime\":").append(rebalanceCheckTime);
            } else if (consumeType == ConsumeType.CONSUME_CLIENT_REB) {
                Tuple2<Long, List<String>> metaInfoTuple = consumeGroupInfo.getTopicMetaInfo();
                sBuffer.append(",\"topicMetaId\":").append(metaInfoTuple.getF0())
                        .append(",\"metaDetails\":[");
                for (String itemInfo : metaInfoTuple.getF1()) {
                    if (itemCnt++ > 0) {
                        sBuffer.append(",");
                    }
                    sBuffer.append("\"").append(itemInfo).append("\"");
                }
                sBuffer.append("]");
            }
            sBuffer.append(",\"rebInfo\":{");
            if (balanceStatus == -2) {
                sBuffer.append("\"isRebalanced\":false");
            } else if (balanceStatus == 0) {
                sBuffer.append("\"isRebalanced\":true,\"checkPasted\":false")
                        .append(",\"defBClientRate\":").append(defBClientRate)
                        .append(",\"confBClientRate\":").append(confBClientRate)
                        .append(",\"curBClientRate\":").append(curBClientRate)
                        .append(",\"minRequireClientCnt\":").append(minRequireClientCnt);
            } else {
                sBuffer.append("\"isRebalanced\":true,\"checkPasted\":true")
                        .append(",\"defBClientRate\":").append(defBClientRate)
                        .append(",\"confBClientRate\":").append(confBClientRate)
                        .append(",\"curBClientRate\":").append(curBClientRate);
            }
            sBuffer.append("},\"filterConds\":{");
            if (existedTopicConditions != null) {
                int keyCount = 0;
                for (Map.Entry<String, TreeSet<String>> entry : existedTopicConditions.entrySet()) {
                    if (keyCount++ > 0) {
                        sBuffer.append(",");
                    }
                    sBuffer.append("\"").append(entry.getKey()).append("\":[");
                    if (entry.getValue() != null) {
                        int itemCount = 0;
                        for (String filterCond : entry.getValue()) {
                            if (itemCount++ > 0) {
                                sBuffer.append(",");
                            }
                            sBuffer.append("\"").append(filterCond).append("\"");
                        }
                    }
                    sBuffer.append("]");
                }
            }
            sBuffer.append("}");
            // Append consumer info of the group
            getConsumerInfoList(consumerList, consumeType, sBuffer);
            sBuffer.append("}");
        } catch (Exception e) {
            sBuffer.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\",\"count\":0,\"data\":[]}");
        }
        return sBuffer;
    }

    /**
     * Query Master's version
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return          metric information
     */
    public StringBuilder adminQueryBrokerVersion(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        sBuffer.append("{\"version\":\"")
                .append(TubeServerVersion.SERVER_VERSION).append("\"}");
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, 1);
        return sBuffer;
    }

    /**
     * Get master's metric information
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return          metric information
     */
    public StringBuilder adminGetMetricsInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get whether to reset the metric items
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.NEEDREFRESH, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final boolean needRefresh = (Boolean) result.getRetData();
        // query current metric values;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        sBuffer.append("{\"probeTime\":\"")
                .append(DateTimeConvertUtils.ms2yyyyMMddHHmmss(System.currentTimeMillis()))
                .append("\",\"nodeName\":\"").append(master.getMasterConfig().getHostName())
                .append("\",\"nodeRole\":\"Master\",\"metrics\":{\"serviceStatus\":");
        if (needRefresh) {
            MasterSrvStatsHolder.snapShort(sBuffer);
            sBuffer.append(",\"webAPI\":");
            WebCallStatsHolder.snapShort(sBuffer);
        } else {
            MasterSrvStatsHolder.getValue(sBuffer);
            sBuffer.append(",\"webAPI\":");
            WebCallStatsHolder.getValue(sBuffer);
        }
        sBuffer.append("},\"count\":2}");
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, 1);
        return sBuffer;
    }

    /**
     * Enable Master's statistics functions.
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return          metric information
     */
    public StringBuilder adminEnableMetricsStats(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.STATSTYPE, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        String statsType = (String) result.getRetData();
        return innEnableOrDisableMetricsStats(true, statsType, req, sBuffer, result);
    }

    /**
     * Disable Master's statistics functions.
     *
     * @param req      request
     * @param sBuffer  process result
     */
    public StringBuilder adminDisableMetricsStats(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.STATSTYPE, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        String statsType = (String) result.getRetData();
        innEnableOrDisableMetricsStats(false, statsType, req, sBuffer, result);
        return sBuffer;
    }

    /**
     * Disable Master's all statistics functions.
     *
     * @param req      request
     * @param sBuffer  process result
     */
    public StringBuilder adminDisableAllStats(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        innEnableOrDisableMetricsStats(false,
                BrokerStatsType.ALL.getName(), req, sBuffer, result);
        return sBuffer;
    }

    /**
     * Disable or Enable Master's statistics functions
     *
     * @param enable     whether enable or disable
     * @param statsType  the statistics type to be operated on
     * @param req        HttpServletRequest
     * @param sBuffer    query result
     * @param result     process result
     * @return           return information
     */
    private StringBuilder innEnableOrDisableMetricsStats(boolean enable,
            String statsType,
            HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // get input metric type
        MasterStatsType inMetricType = null;
        for (MasterStatsType metricType : MasterStatsType.values()) {
            if (metricType.getName().equalsIgnoreCase(statsType)) {
                inMetricType = metricType;
                break;
            }
        }
        if (inMetricType == null) {
            sBuffer.append("{\"result\":false,\"errCode\":400,\"errMsg\":")
                    .append("\"Unmatched stat type, allowed stat type are : [");
            int count = 0;
            for (MasterStatsType metricType : MasterStatsType.values()) {
                if (count++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append(metricType.getName());
            }
            sBuffer.append("]\"}");
            return sBuffer;
        }
        // Operate separately according to the specified statistic type
        if (inMetricType == MasterStatsType.WEBAPI
                || inMetricType == MasterStatsType.ALL) {
            WebCallStatsHolder.setStatsStatus(enable);
        }
        // builder return result
        WebParameterUtils.buildSuccessResult(sBuffer);
        return sBuffer;
    }

    /**
     * Private method to append consumer info of the give list to a string builder
     *
     * @param consumerList  consumer list
     * @param consumeType   consume type
     * @param strBuffer     string buffer
     */
    private void getConsumerInfoList(final List<ConsumerInfo> consumerList,
            ConsumeType consumeType, final StringBuilder strBuffer) {
        strBuffer.append(",\"data\":[");
        if (!consumerList.isEmpty()) {
            Collections.sort(consumerList);
            Map<String, Map<String, Map<String, Partition>>> currentSubInfoMap =
                    master.getCurrentSubInfoMap();
            for (int i = 0; i < consumerList.size(); i++) {
                ConsumerInfo consumer = consumerList.get(i);
                if (consumer == null) {
                    continue;
                }
                if (i > 0) {
                    strBuffer.append(",");
                }
                strBuffer.append("{\"consumerId\":\"").append(consumer.getConsumerId())
                        .append("\",\"receivedFrom\":\"").append(consumer.getAddrRcvFrom())
                        .append("\",\"isOverTLS\":").append(consumer.isOverTLS());
                if (consumeType == ConsumeType.CONSUME_BAND) {
                    Map<String, Long> requiredPartition = consumer.getRequiredPartition();
                    if (requiredPartition == null || requiredPartition.isEmpty()) {
                        strBuffer.append(",\"initReSetPartCount\":0,\"initReSetPartInfo\":[]");
                    } else {
                        strBuffer.append(",\"initReSetPartCount\":").append(requiredPartition.size())
                                .append(",\"initReSetPartInfo\":[");
                        int totalPart = 0;
                        for (Map.Entry<String, Long> entry : requiredPartition.entrySet()) {
                            if (totalPart++ > 0) {
                                strBuffer.append(",");
                            }
                            strBuffer.append("{\"partitionKey\":\"").append(entry.getKey())
                                    .append("\",\"Offset\":").append(entry.getValue()).append("}");
                        }
                        strBuffer.append("]");
                    }
                } else if (consumeType == ConsumeType.CONSUME_CLIENT_REB) {
                    strBuffer.append(",\"sourceCount\":").append(consumer.getSourceCount())
                            .append(",\"nodeId\":").append(consumer.getNodeId());
                }
                Map<String, Map<String, Partition>> topicSubMap =
                        currentSubInfoMap.get(consumer.getConsumerId());
                if (topicSubMap == null || topicSubMap.isEmpty()) {
                    strBuffer.append(",\"parCount\":0,\"parInfo\":[]}");
                } else {
                    int totalSize = 0;
                    for (Map.Entry<String, Map<String, Partition>> entry : topicSubMap.entrySet()) {
                        totalSize += entry.getValue().size();
                    }
                    strBuffer.append(",\"parCount\":").append(totalSize).append(",\"parInfo\":[");
                    int totalPart = 0;
                    for (Map.Entry<String, Map<String, Partition>> entry : topicSubMap.entrySet()) {
                        Map<String, Partition> partMap = entry.getValue();
                        if (partMap != null) {
                            for (Partition part : partMap.values()) {
                                if (totalPart++ > 0) {
                                    strBuffer.append(",");
                                }
                                strBuffer.append("{\"partId\":").append(part.getPartitionId())
                                        .append(",\"brokerAddr\":\"").append(part.getBroker().toString())
                                        .append("\",\"topicName\":\"").append(part.getTopic()).append("\"}");
                            }
                        }
                    }
                    strBuffer.append("]}");
                }
            }
        }
        strBuffer.append("]");
    }
}
