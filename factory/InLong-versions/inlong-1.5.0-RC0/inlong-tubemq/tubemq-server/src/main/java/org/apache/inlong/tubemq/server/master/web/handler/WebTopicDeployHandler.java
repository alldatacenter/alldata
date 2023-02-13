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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStsChgType;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunStatusInfo;

public class WebTopicDeployHandler extends AbstractWebHandler {

    /**
     * Constructor
     *
     * @param master tube master
     */
    public WebTopicDeployHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_topic_deploy_info",
                "adminNewQueryTopicCfgAndRunInfo");
        registerQueryWebMethod("admin_query_topic_deploy_configure",
                "innQueryTopicDeployConfInfo");
        registerQueryWebMethod("admin_query_broker_topic_config_info",
                "adminQueryBrokerTopicCfgAndRunInfo");
        registerQueryWebMethod("admin_query_topic_broker_config_info",
                "adminQueryTopicBrokerCfgAndRunInfo");
        registerQueryWebMethod("admin_query_deployed_topics",
                "adminQuerySimpleTopicName");
        registerQueryWebMethod("admin_query_deployed_broker_ids",
                "adminQuerySimpleBrokerId");

        // register modify method
        registerModifyWebMethod("admin_add_topic_deploy_info",
                "adminAddTopicDeployInfo");
        registerModifyWebMethod("admin_bath_add_topic_deploy_info",
                "adminBatchAddTopicDeployInfo");
        registerModifyWebMethod("admin_update_topic_deploy_info",
                "adminModifyTopicDeployInfo");
        registerModifyWebMethod("admin_batch_update_topic_deploy_info",
                "adminBatchUpdTopicDeployInfo");
        registerModifyWebMethod("admin_delete_topic_deploy_info",
                "adminDelTopicDeployInfo");
        registerModifyWebMethod("admin_redo_deleted_topic_deploy_info",
                "adminRedoDeletedTopicDeployInfo");
        registerModifyWebMethod("admin_remove_topic_deploy_info",
                "adminRmvTopicDeployInfo");

        // Deprecated methods begin
        // query
        registerQueryWebMethod("admin_query_topic_info",
                "adminOldQueryTopicCfgAndRunInfo");
        // modify
        registerModifyWebMethod("admin_add_new_topic_record",
                "adminAddTopicDeployInfo");
        registerModifyWebMethod("admin_bath_add_new_topic_record",
                "adminBatchAddTopicDeployInfo");
        registerModifyWebMethod("admin_modify_topic_info",
                "adminModifyTopicDeployInfo");
        registerModifyWebMethod("admin_delete_topic_info",
                "adminDelTopicDeployInfo");
        registerModifyWebMethod("admin_redo_deleted_topic_info",
                "adminRedoDeletedTopicDeployInfo");
        registerModifyWebMethod("admin_remove_topic_info",
                "adminRmvTopicDeployInfo");
        // Deprecated methods end
    }

    /**
     * Query topic info with new format return
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return query result
     */
    public StringBuilder adminNewQueryTopicCfgAndRunInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innQueryTopicConfAndRunInfo(req, sBuffer, result, true);
    }

    /**
     * Query topic info with old format return
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return query result
     */
    public StringBuilder adminOldQueryTopicCfgAndRunInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innQueryTopicConfAndRunInfo(req, sBuffer, result, false);
    }

    /**
     * Add new topic deployment record
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrUpdTopicDeployInfo(req, sBuffer, result, true);
    }

    /**
     * Modify topic deployment info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminModifyTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrUpdTopicDeployInfo(req, sBuffer, result, false);
    }

    /**
     * Add new topic deployment record in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innBatchAddOrUpdTopicDeployInfo(req, sBuffer, result, true);
    }

    /**
     * Add new topic deployment record in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchUpdTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innBatchAddOrUpdTopicDeployInfo(req, sBuffer, result, false);
    }

    /**
     * Delete topic info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDelTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innModifyTopicDeployStatusInfo(req,
                sBuffer, result, TopicStsChgType.STATUS_CHANGE_SOFT_DELETE);
    }

    /**
     * Remove topic info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminRmvTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innModifyTopicDeployStatusInfo(req,
                sBuffer, result, TopicStsChgType.STATUS_CHANGE_REMOVE);
    }

    /**
     * Redo delete topic info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminRedoDeletedTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innModifyTopicDeployStatusInfo(req,
                sBuffer, result, TopicStsChgType.STATUS_CHANGE_REDO_SFDEL);
    }

    /**
     * Query broker's topic configure view
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryBrokerTopicCfgAndRunInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIdSet = (Set<Integer>) result.getRetData();
        Map<Integer, List<TopicDeployEntity>> queryResult =
                defMetaDataService.getTopicDeployInfoMap(topicNameSet, brokerIdSet);
        // build query result
        int recordCount = 0;
        int topicTotalCfgCnt = 0;
        int storeTotalCfgCnt = 0;
        int partTotalCfgCnt = 0;
        int topicAccPubTotalCnt = 0;
        int topicAccSubTotalCnt = 0;
        int storeAccPubTotalCnt = 0;
        int storeAccSubTotalCnt = 0;
        int partAccPubTotalCnt = 0;
        int partAccSubTotalCnt = 0;
        int tmpPartTtlCount = 0;
        String strManageStatus;
        BrokerRunStatusInfo runStatusInfo;
        List<TopicDeployEntity> topicDeployInfo;
        Tuple2<Boolean, Boolean> publishTuple = new Tuple2<>();
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (Map.Entry<Integer, List<TopicDeployEntity>> entry : queryResult.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            BrokerConfEntity brokerConf =
                    defMetaDataService.getBrokerConfByBrokerId(entry.getKey());
            if (brokerConf == null) {
                continue;
            }
            // build return info item
            if (recordCount++ > 0) {
                sBuffer.append(",");
            }
            // reset data items
            topicTotalCfgCnt = 0;
            storeTotalCfgCnt = 0;
            partTotalCfgCnt = 0;
            topicAccPubTotalCnt = 0;
            topicAccSubTotalCnt = 0;
            storeAccPubTotalCnt = 0;
            storeAccSubTotalCnt = 0;
            partAccPubTotalCnt = 0;
            partAccSubTotalCnt = 0;
            // query detail information
            topicDeployInfo = entry.getValue();
            strManageStatus = brokerConf.getManageStatus().getDescription();
            runStatusInfo = brokerRunManager.getBrokerRunStatusInfo(entry.getKey());
            brokerRunManager.getBrokerPublishStatus(entry.getKey(), publishTuple);
            // accumulate data items
            for (TopicDeployEntity topicEntity : topicDeployInfo) {
                if (topicEntity == null) {
                    continue;
                }
                topicTotalCfgCnt++;
                storeTotalCfgCnt += topicEntity.getNumTopicStores();
                tmpPartTtlCount = topicEntity.getNumTopicStores() * topicEntity.getNumPartitions();
                partTotalCfgCnt += tmpPartTtlCount;
                if (runStatusInfo == null) {
                    continue;
                }
                if (publishTuple.getF0() && topicEntity.isAcceptPublish()) {
                    topicAccPubTotalCnt++;
                    storeAccPubTotalCnt += topicEntity.getNumTopicStores();
                    partAccPubTotalCnt += tmpPartTtlCount;
                }
                if (publishTuple.getF1() && topicEntity.isAcceptSubscribe()) {
                    topicAccSubTotalCnt++;
                    storeAccSubTotalCnt += topicEntity.getNumTopicStores();
                    partAccSubTotalCnt += tmpPartTtlCount;
                }
            }
            // build query record by broker
            sBuffer.append("{\"brokerId\":").append(brokerConf.getBrokerId())
                    .append(",\"brokerIp\":\"").append(brokerConf.getBrokerIp())
                    .append("\",\"brokerPort\":").append(brokerConf.getBrokerPort())
                    .append(",\"manageStatus\":\"").append(strManageStatus)
                    .append("\",\"acceptPublish\":").append(publishTuple.getF0())
                    .append(",\"acceptSubscribe\":").append(publishTuple.getF1());
            if (runStatusInfo == null) {
                sBuffer.append(",\"nodeRunInfo\":\"-\"");

            } else {
                sBuffer.append(",\"nodeRunInfo\":\"running\"");
            }
            sBuffer.append(",\"topicTotalCfgCnt\":").append(topicTotalCfgCnt)
                    .append(",\"storeTotalCfgCnt\":").append(storeTotalCfgCnt)
                    .append(",\"partTotalCfgCnt\":").append(partTotalCfgCnt)
                    .append(",\"topicAccPubTotalCnt\":").append(topicAccPubTotalCnt)
                    .append(",\"topicAccSubTotalCnt\":").append(topicAccSubTotalCnt)
                    .append(",\"storeAccPubTotalCnt\":").append(storeAccPubTotalCnt)
                    .append(",\"storeAccSubTotalCnt\":").append(storeAccSubTotalCnt)
                    .append(",\"partAccPubTotalCnt\":").append(partAccPubTotalCnt)
                    .append(",\"partAccSubTotalCnt\":").append(partAccSubTotalCnt)
                    .append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, recordCount);
        return sBuffer;
    }

    /**
     * Query topic's broker configure view
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryTopicBrokerCfgAndRunInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // query topic configure info
        Map<String, List<TopicDeployEntity>> topicConfMap =
                defMetaDataService.getTopicConfMapByTopicAndBrokerIds(topicNameSet, brokerIds);
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        // set statistic data items
        int recordCount = 0;
        int brokerTotalCfgCnt = 0;
        int storeTotalCfgCnt = 0;
        int partTotalCfgCnt = 0;
        int tmpPartTtlCount = 0;
        int brokerTotalRunCnt = 0;
        int storeTotalRunCnt = 0;
        int partTotalRunCnt = 0;
        int brokerAccPubTotalCnt = 0;
        int brokerAccSubTotalCnt = 0;
        int storeAccPubTotalCnt = 0;
        int storeAccSubTotalCnt = 0;
        int partAccPubTotalCnt = 0;
        int partAccSubTotalCnt = 0;
        boolean hasRunConfig = false;
        boolean topicSrvAccPubStatus = false;
        boolean topicSrvAccSubStatus = false;
        boolean enableAuthControl = false;
        // build query result
        TopicPropGroup topicProps;
        TopicCtrlEntity authEntity;
        BrokerConfEntity brokerConfEntity;
        Tuple3<Boolean, Boolean, TopicInfo> topicInfoTuple = new Tuple3<>();
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (Map.Entry<String, List<TopicDeployEntity>> entry : topicConfMap.entrySet()) {
            if (recordCount++ > 0) {
                sBuffer.append(",");
            }
            // reset the value of data items
            brokerTotalCfgCnt = 0;
            brokerTotalRunCnt = 0;
            storeTotalCfgCnt = 0;
            storeTotalRunCnt = 0;
            partTotalCfgCnt = 0;
            partTotalRunCnt = 0;
            brokerAccPubTotalCnt = 0;
            brokerAccSubTotalCnt = 0;
            storeAccPubTotalCnt = 0;
            storeAccSubTotalCnt = 0;
            partAccPubTotalCnt = 0;
            partAccSubTotalCnt = 0;
            topicSrvAccPubStatus = false;
            topicSrvAccSubStatus = false;
            enableAuthControl = false;
            for (TopicDeployEntity entity : entry.getValue()) {
                brokerConfEntity =
                        defMetaDataService.getBrokerConfByBrokerId(entity.getBrokerId());
                if (brokerConfEntity == null) {
                    continue;
                }
                // query detail information
                topicProps = entity.getTopicProps();
                brokerRunManager.getPubBrokerTopicInfo(
                        entity.getBrokerId(), entity.getTopicName(), topicInfoTuple);
                // accumulate configured data
                brokerTotalCfgCnt++;
                storeTotalCfgCnt += topicProps.getNumTopicStores();
                partTotalCfgCnt +=
                        topicProps.getNumPartitions() * topicProps.getNumTopicStores();
                if (topicInfoTuple.getF2() != null) {
                    hasRunConfig = false;
                    tmpPartTtlCount =
                            topicInfoTuple.getF2().getPartitionNum() * topicInfoTuple.getF2().getTopicStoreNum();
                    if (topicInfoTuple.getF0() && topicInfoTuple.getF2().isAcceptPublish()) {
                        hasRunConfig = true;
                        topicSrvAccPubStatus = true;
                        brokerAccPubTotalCnt++;
                        storeAccPubTotalCnt += topicInfoTuple.getF2().getTopicStoreNum();
                        partAccPubTotalCnt += tmpPartTtlCount;
                    }
                    if (topicInfoTuple.getF1() && topicInfoTuple.getF2().isAcceptSubscribe()) {
                        hasRunConfig = true;
                        topicSrvAccSubStatus = true;
                        brokerAccSubTotalCnt++;
                        storeAccSubTotalCnt += topicInfoTuple.getF2().getTopicStoreNum();
                        partAccSubTotalCnt += tmpPartTtlCount;
                    }
                    if (hasRunConfig) {
                        // accumulate running data
                        brokerTotalRunCnt++;
                        storeTotalRunCnt += topicInfoTuple.getF2().getTopicStoreNum();
                        partTotalRunCnt += tmpPartTtlCount;
                    }
                }
            }
            // query authenticate information
            authEntity = defMetaDataService.getTopicCtrlByTopicName(entry.getKey());
            if (authEntity != null) {
                enableAuthControl = authEntity.isAuthCtrlEnable();
            }
            sBuffer.append("{\"topicName\":\"").append(entry.getKey())
                    .append("\",\"brokerTotalCfgCnt\":").append(brokerTotalCfgCnt)
                    .append(",\"brokerTotalRunCnt\":").append(brokerTotalRunCnt)
                    .append(",\"storeTotalCfgCnt\":").append(storeTotalCfgCnt)
                    .append(",\"storeTotalRunCnt\":").append(storeTotalRunCnt)
                    .append(",\"partTotalCfgCnt\":").append(partTotalCfgCnt)
                    .append(",\"partTotalRunCnt\":").append(partTotalRunCnt)
                    .append(",\"brokerAccPubTotalCnt\":").append(brokerAccPubTotalCnt)
                    .append(",\"brokerAccSubTotalCnt\":").append(brokerAccSubTotalCnt)
                    .append(",\"storeAccPubTotalCnt\":").append(storeAccPubTotalCnt)
                    .append(",\"storeAccSubTotalCnt\":").append(storeAccSubTotalCnt)
                    .append(",\"partAccPubTotalCnt\":").append(partAccPubTotalCnt)
                    .append(",\"partAccSubTotalCnt\":").append(partAccSubTotalCnt)
                    .append(",\"topicSrvAccPubStatus\":").append(topicSrvAccPubStatus)
                    .append(",\"topicSrvAccSubStatus\":").append(topicSrvAccSubStatus)
                    .append(",\"enableAuthControl\":").append(enableAuthControl)
                    .append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, recordCount);
        return sBuffer;
    }

    /**
     * Query broker's topic-name set info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQuerySimpleTopicName(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        Map<Integer, Set<String>> brokerTopicConfigMap =
                defMetaDataService.getBrokerTopicConfigInfo(brokerIds);
        // build query result
        int dataCount = 0;
        int topicCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (Map.Entry<Integer, Set<String>> entry : brokerTopicConfigMap.entrySet()) {
            if (dataCount++ > 0) {
                sBuffer.append(",");
            }
            topicCnt = 0;
            sBuffer.append("{\"brokerId\":").append(entry.getKey())
                    .append(",\"topicName\":[");
            for (String topic : entry.getValue()) {
                if (topicCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("\"").append(topic).append("\"");
            }
            sBuffer.append("],\"topicCount\":").append(topicCnt).append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, dataCount);
        return sBuffer;
    }

    /**
     * Query topic's broker id set
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQuerySimpleBrokerId(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.WITHIP, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        boolean withIp = (Boolean) result.getRetData();
        Map<String, Map<Integer, String>> topicBrokerConfigMap =
                defMetaDataService.getTopicBrokerConfigInfo(topicNameSet);
        // build query result
        int dataCount = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (Map.Entry<String, Map<Integer, String>> entry : topicBrokerConfigMap.entrySet()) {
            if (dataCount++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"topicName\":\"").append(entry.getKey()).append("\",\"brokerInfo\":[");
            int topicCnt = 0;
            Map<Integer, String> brokerMap = entry.getValue();
            if (withIp) {
                for (Map.Entry<Integer, String> entry1 : brokerMap.entrySet()) {
                    if (topicCnt++ > 0) {
                        sBuffer.append(",");
                    }
                    sBuffer.append("{\"brokerId\":").append(entry1.getKey())
                            .append(",\"brokerIp\":\"").append(entry1.getValue()).append("\"}");
                }
            } else {
                for (Map.Entry<Integer, String> entry1 : brokerMap.entrySet()) {
                    if (topicCnt++ > 0) {
                        sBuffer.append(",");
                    }
                    sBuffer.append(entry1.getKey());
                }
            }
            sBuffer.append("],\"brokerCnt\":").append(topicCnt).append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, dataCount);
        return sBuffer;
    }

    /**
     * Query topic info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder innQueryTopicDeployConfInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        TopicDeployEntity qryEntity = new TopicDeployEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<Integer> brokerIdSet = (Set<Integer>) result.getRetData();
        // get brokerPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERPORT,
                false, TBaseConstants.META_VALUE_UNDEFINED, 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int brokerPort = (int) result.getRetData();
        // get and valid topicProps info
        if (!WebParameterUtils.getTopicPropInfo(req, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        TopicPropGroup topicProps = (TopicPropGroup) result.getRetData();
        // get and valid TopicStatusId info
        if (!WebParameterUtils.getTopicStatusParamValue(req,
                false, TopicStatus.STATUS_TOPIC_UNDEFINED, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        TopicStatus topicStatus = (TopicStatus) result.getRetData();
        qryEntity.updModifyInfo(qryEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED,
                brokerPort, null, topicStatus, topicProps);
        Map<String, List<TopicDeployEntity>> topicDeployInfoMap =
                defMetaDataService.getTopicDeployInfoMap(topicNameSet, brokerIdSet, qryEntity);
        // build query result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (Map.Entry<String, List<TopicDeployEntity>> entry : topicDeployInfoMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            for (TopicDeployEntity entity : entry.getValue()) {
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                entity.toWebJsonStr(sBuffer, true, true);
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Query topic info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    private StringBuilder innQueryTopicConfAndRunInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isNewVer) {
        TopicDeployEntity qryEntity = new TopicDeployEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<Integer> brokerIdSet = (Set<Integer>) result.getRetData();
        // get brokerPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERPORT,
                false, TBaseConstants.META_VALUE_UNDEFINED, 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int brokerPort = (int) result.getRetData();
        // get and valid topicProps info
        if (!WebParameterUtils.getTopicPropInfo(req, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        TopicPropGroup topicProps = (TopicPropGroup) result.getRetData();
        // get withGroupAuthInfo field
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.WITHGROUPAUTHINFO,
                false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean withGroupAuthInfo = (Boolean) result.getRetData();
        // get and valid TopicStatusId info
        if (!WebParameterUtils.getTopicStatusParamValue(req,
                false, TopicStatus.STATUS_TOPIC_UNDEFINED, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        TopicStatus topicStatus = (TopicStatus) result.getRetData();
        qryEntity.updModifyInfo(qryEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED,
                brokerPort, null, topicStatus, topicProps);
        Map<String, List<TopicDeployEntity>> topicDeployInfoMap =
                defMetaDataService.getTopicDeployInfoMap(topicNameSet, brokerIdSet, qryEntity);
        // build query result
        if (isNewVer) {
            return buildNewQueryResult(withGroupAuthInfo, sBuffer, topicDeployInfoMap);
        } else {
            return buildOldQueryResult(sBuffer, topicDeployInfoMap);
        }
    }

    private StringBuilder buildOldQueryResult(StringBuilder sBuffer,
            Map<String, List<TopicDeployEntity>> topicDeployInfoMap) {
        // build query result
        int totalCnt = 0;
        int itemCount = 0;
        int condStatusId = 1;
        int maxMsgSizeInMB = 0;
        int totalCfgNumPartCount = 0;
        int totalRunNumPartCount = 0;
        boolean enableAuthCtrl;
        boolean isSrvAcceptPublish = false;
        boolean isSrvAcceptSubscribe = false;
        String strManageStatus;
        TopicCtrlEntity ctrlEntity;
        BrokerConfEntity brokerConfEntity;
        List<GroupConsumeCtrlEntity> groupCtrlInfoLst;
        Tuple3<Boolean, Boolean, TopicInfo> topicInfoTuple = new Tuple3<>();
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        ClusterSettingEntity defSetting = defMetaDataService.getClusterDefSetting(false);
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (Map.Entry<String, List<TopicDeployEntity>> entry : topicDeployInfoMap.entrySet()) {
            totalCfgNumPartCount = 0;
            totalRunNumPartCount = 0;
            isSrvAcceptPublish = false;
            isSrvAcceptSubscribe = false;
            ctrlEntity = defMetaDataService.getTopicCtrlByTopicName(entry.getKey());
            if (ctrlEntity == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            maxMsgSizeInMB = defSetting.getMaxMsgSizeInMB();
            if (ctrlEntity.getMaxMsgSizeInMB() != TBaseConstants.META_VALUE_UNDEFINED) {
                maxMsgSizeInMB = ctrlEntity.getMaxMsgSizeInMB();
            }
            enableAuthCtrl = ctrlEntity.getAuthCtrlStatus().isEnable();
            sBuffer.append("{\"topicName\":\"").append(entry.getKey())
                    .append("\",\"maxMsgSizeInMB\":").append(maxMsgSizeInMB)
                    .append(",\"topicInfo\":[");
            itemCount = 0;
            for (TopicDeployEntity entity : entry.getValue()) {
                if (itemCount++ > 0) {
                    sBuffer.append(",");
                }
                totalCfgNumPartCount += entity.getNumPartitions() * entity.getNumTopicStores();
                entity.toWebJsonStr(sBuffer, true, false);
                sBuffer.append(",\"runInfo\":{");
                strManageStatus = "-";
                brokerConfEntity =
                        defMetaDataService.getBrokerConfByBrokerId(entity.getBrokerId());
                if (brokerConfEntity != null) {
                    strManageStatus = brokerConfEntity.getManageStatus().getDescription();
                }
                brokerRunManager.getPubBrokerTopicInfo(
                        entity.getBrokerId(), entity.getTopicName(), topicInfoTuple);
                if (topicInfoTuple.getF2() == null) {
                    sBuffer.append("\"acceptPublish\":\"-\"").append(",\"acceptSubscribe\":\"-\"")
                            .append(",\"numPartitions\":\"-\"").append(",\"brokerManageStatus\":\"-\"");
                } else {
                    if (topicInfoTuple.getF0()) {
                        sBuffer.append("\"acceptPublish\":")
                                .append(topicInfoTuple.getF2().isAcceptPublish());
                        if (topicInfoTuple.getF2().isAcceptPublish()) {
                            isSrvAcceptPublish = true;
                        }
                    } else {
                        sBuffer.append("\"acceptPublish\":false");
                    }
                    if (topicInfoTuple.getF1()) {
                        sBuffer.append(",\"acceptSubscribe\":")
                                .append(topicInfoTuple.getF2().isAcceptSubscribe());
                        if (topicInfoTuple.getF2().isAcceptSubscribe()) {
                            isSrvAcceptSubscribe = true;
                        }
                    } else {
                        sBuffer.append(",\"acceptSubscribe\":false");
                    }
                    totalRunNumPartCount +=
                            topicInfoTuple.getF2().getPartitionNum() * topicInfoTuple.getF2().getTopicStoreNum();
                    sBuffer.append(",\"numPartitions\":")
                            .append(topicInfoTuple.getF2().getPartitionNum())
                            .append(",\"numTopicStores\":")
                            .append(topicInfoTuple.getF2().getTopicStoreNum())
                            .append(",\"brokerManageStatus\":\"").append(strManageStatus).append("\"");
                }
                sBuffer.append("}}");
            }
            sBuffer.append("],\"infoCount\":").append(itemCount)
                    .append(",\"totalCfgNumPart\":").append(totalCfgNumPartCount)
                    .append(",\"isSrvAcceptPublish\":").append(isSrvAcceptPublish)
                    .append(",\"isSrvAcceptSubscribe\":").append(isSrvAcceptSubscribe)
                    .append(",\"totalRunNumPartCount\":").append(totalRunNumPartCount)
                    .append(",\"authData\":{");
            if (enableAuthCtrl) {
                sBuffer.append("\"enableAuthControl\":").append(enableAuthCtrl)
                        .append(",\"createUser\":\"").append(ctrlEntity.getModifyUser())
                        .append("\",\"createDate\":\"").append(ctrlEntity.getModifyDateStr())
                        .append("\",\"authConsumeGroup\":[");
                itemCount = 0;
                groupCtrlInfoLst = defMetaDataService.getConsumeCtrlByTopic(entry.getKey());
                for (GroupConsumeCtrlEntity groupEntity : groupCtrlInfoLst) {
                    if (itemCount++ > 0) {
                        sBuffer.append(",");
                    }
                    sBuffer.append("{\"groupName\":\"").append(groupEntity.getGroupName())
                            .append("\",\"createUser\":\"").append(groupEntity.getCreateUser())
                            .append("\",\"createDate\":\"").append(groupEntity.getCreateDateStr())
                            .append("\",\"modifyUser\":\"").append(groupEntity.getModifyUser())
                            .append("\",\"modifyDate\":\"").append(groupEntity.getModifyDateStr())
                            .append("\"}");
                }
                sBuffer.append("],\"groupCount\":").append(itemCount).append(",\"authFilterCondSet\":[");
                itemCount = 0;
                for (GroupConsumeCtrlEntity groupEntity : groupCtrlInfoLst) {
                    if (itemCount++ > 0) {
                        sBuffer.append(",");
                    }
                    condStatusId = 0;
                    if (groupEntity.getFilterEnable().isEnable()) {
                        condStatusId = 2;
                    }
                    sBuffer.append("{\"groupName\":\"").append(groupEntity.getGroupName())
                            .append("\",\"condStatus\":").append(condStatusId);
                    if (!groupEntity.getFilterEnable().isEnable()) {
                        sBuffer.append(",\"filterConds\":\"\"");
                    } else {
                        sBuffer.append(",\"filterConds\":\"")
                                .append(groupEntity.getFilterCondStr()).append("\"");
                    }
                    sBuffer.append(",\"createDate\":\"").append(groupEntity.getCreateDateStr())
                            .append("\",\"modifyUser\":\"").append(groupEntity.getModifyUser())
                            .append("\",\"modifyDate\":\"").append(groupEntity.getModifyDateStr())
                            .append("\"}");
                }
                sBuffer.append("],\"filterCount\":").append(itemCount);
            }
            sBuffer.append("}}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    private StringBuilder buildNewQueryResult(boolean withAuthInfo,
            StringBuilder sBuffer,
            Map<String, List<TopicDeployEntity>> topicDeployMap) {
        // build query result
        int totalCnt = 0;
        int itemCount = 0;
        int totalCfgNumPartCount = 0;
        int totalRunNumPartCount = 0;
        boolean isSrvAcceptPublish = false;
        boolean isSrvAcceptSubscribe = false;
        String strManageStatus;
        TopicCtrlEntity ctrlEntity;
        BrokerConfEntity brokerConfEntity;
        List<GroupConsumeCtrlEntity> groupCtrlInfoLst;
        Tuple3<Boolean, Boolean, TopicInfo> topicInfoTuple = new Tuple3<>();
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (Map.Entry<String, List<TopicDeployEntity>> entry : topicDeployMap.entrySet()) {
            totalCfgNumPartCount = 0;
            totalRunNumPartCount = 0;
            isSrvAcceptPublish = false;
            isSrvAcceptSubscribe = false;
            ctrlEntity =
                    defMetaDataService.getTopicCtrlByTopicName(entry.getKey());
            if (ctrlEntity == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            ctrlEntity.toWebJsonStr(sBuffer, true, false);
            sBuffer.append(",\"deployInfo\":[");
            itemCount = 0;
            for (TopicDeployEntity entity : entry.getValue()) {
                if (itemCount++ > 0) {
                    sBuffer.append(",");
                }
                totalCfgNumPartCount += entity.getNumPartitions() * entity.getNumTopicStores();
                entity.toWebJsonStr(sBuffer, true, false);
                sBuffer.append(",\"runInfo\":{");
                brokerConfEntity =
                        defMetaDataService.getBrokerConfByBrokerId(entity.getBrokerId());
                strManageStatus = "-";
                if (brokerConfEntity != null) {
                    strManageStatus = brokerConfEntity.getManageStatus().getDescription();
                }
                brokerRunManager.getPubBrokerTopicInfo(
                        entity.getBrokerId(), entity.getTopicName(), topicInfoTuple);
                if (topicInfoTuple.getF2() == null) {
                    sBuffer.append("\"acceptPublish\":\"-\"").append(",\"acceptSubscribe\":\"-\"")
                            .append(",\"numPartitions\":\"-\"").append(",\"brokerManageStatus\":\"-\"");
                } else {
                    if (topicInfoTuple.getF0()) {
                        sBuffer.append("\"acceptPublish\":")
                                .append(topicInfoTuple.getF2().isAcceptPublish());
                        if (topicInfoTuple.getF2().isAcceptPublish()) {
                            isSrvAcceptPublish = true;
                        }
                    } else {
                        sBuffer.append("\"acceptPublish\":false");
                    }
                    if (topicInfoTuple.getF1()) {
                        sBuffer.append(",\"acceptSubscribe\":")
                                .append(topicInfoTuple.getF2().isAcceptSubscribe());
                        if (topicInfoTuple.getF2().isAcceptSubscribe()) {
                            isSrvAcceptSubscribe = true;
                        }
                    } else {
                        sBuffer.append(",\"acceptSubscribe\":false");
                    }
                    totalRunNumPartCount +=
                            topicInfoTuple.getF2().getPartitionNum() * topicInfoTuple.getF2().getTopicStoreNum();
                    sBuffer.append(",\"numPartitions\":")
                            .append(topicInfoTuple.getF2().getPartitionNum())
                            .append(",\"numTopicStores\":")
                            .append(topicInfoTuple.getF2().getTopicStoreNum())
                            .append(",\"brokerManageStatus\":\"").append(strManageStatus).append("\"");
                }
                sBuffer.append("}}");
            }
            sBuffer.append("],\"infoCount\":").append(itemCount)
                    .append(",\"totalCfgNumPart\":").append(totalCfgNumPartCount)
                    .append(",\"isSrvAcceptPublish\":").append(isSrvAcceptPublish)
                    .append(",\"isSrvAcceptSubscribe\":").append(isSrvAcceptSubscribe)
                    .append(",\"totalRunNumPartCount\":").append(totalRunNumPartCount);
            if (withAuthInfo) {
                sBuffer.append(",\"groupAuthInfo\":[");
                groupCtrlInfoLst =
                        defMetaDataService.getConsumeCtrlByTopic(entry.getKey());
                itemCount = 0;
                for (GroupConsumeCtrlEntity groupEntity : groupCtrlInfoLst) {
                    if (itemCount++ > 0) {
                        sBuffer.append(",");
                    }
                    groupEntity.toWebJsonStr(sBuffer, true, true);
                }
                sBuffer.append("],\"groupAuthCount\":").append(itemCount);
            }
            sBuffer.append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    private StringBuilder innAddOrUpdTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get topicName info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get brokerId info
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<Integer> brokerIdSet = (Set<Integer>) result.getRetData();
        // get and valid TopicPropGroup info
        if (!WebParameterUtils.getTopicPropInfo(req, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        TopicPropGroup topicPropInfo = (TopicPropGroup) result.getRetData();
        // set valid topicStatus info
        TopicStatus topicStatus = TopicStatus.STATUS_TOPIC_UNDEFINED;
        if (isAddOp) {
            topicStatus = TopicStatus.STATUS_TOPIC_OK;
        }
        // add record
        List<TopicProcessResult> retInfo = new ArrayList<>();
        for (String topicName : topicNameSet) {
            for (Integer brokerId : brokerIdSet) {
                retInfo.add(defMetaDataService.addOrUpdTopicDeployInfo(isAddOp,
                        opEntity, brokerId, topicName, topicStatus,
                        topicPropInfo, sBuffer, result));
            }
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private StringBuilder innBatchAddOrUpdTopicDeployInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity defOpEntity = (BaseEntity) result.getRetData();
        // check and get add record map
        if (!getTopicDeployJsonSetInfo(req, isAddOp, defOpEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, TopicDeployEntity> addRecordMap =
                (Map<String, TopicDeployEntity>) result.getRetData();
        List<TopicProcessResult> retInfo = new ArrayList<>();
        for (TopicDeployEntity topicDeployInfo : addRecordMap.values()) {
            retInfo.add(defMetaDataService.addOrUpdTopicDeployInfo(isAddOp,
                    topicDeployInfo, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private boolean getTopicDeployJsonSetInfo(HttpServletRequest req, boolean isAddOp,
            BaseEntity defOpEntity, StringBuilder sBuffer,
            ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.TOPICJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> deployJsonArray =
                (List<Map<String, String>>) result.getRetData();
        TopicDeployEntity itemConf;
        Map<String, TopicDeployEntity> addRecordMap = new HashMap<>();
        // check and get topic deployment configure
        for (Map<String, String> confMap : deployJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(confMap,
                    isAddOp, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get topicName configure info
            if (!WebParameterUtils.getStringParamValue(confMap,
                    WebFieldDef.TOPICNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String topicName = (String) result.getRetData();
            // get broker configure info
            if (!getBrokerConfInfo(confMap, sBuffer, result)) {
                return result.isSuccess();
            }
            BrokerConfEntity brokerConf =
                    (BrokerConfEntity) result.getRetData();
            // get and valid TopicPropGroup info
            if (!WebParameterUtils.getTopicPropInfo(confMap,
                    (isAddOp ? brokerConf.getTopicProps() : null), sBuffer, result)) {
                return result.isSuccess();
            }
            final TopicPropGroup topicPropInfo = (TopicPropGroup) result.getRetData();
            // get topicNameId field
            int topicNameId = TBaseConstants.META_VALUE_UNDEFINED;
            TopicCtrlEntity topicCtrlEntity =
                    defMetaDataService.getTopicCtrlByTopicName(topicName);
            if (topicCtrlEntity != null) {
                topicNameId = topicCtrlEntity.getTopicId();
            }
            // set valid topicStatus info
            TopicStatus topicStatus = TopicStatus.STATUS_TOPIC_UNDEFINED;
            if (isAddOp) {
                topicStatus = TopicStatus.STATUS_TOPIC_OK;
            }
            itemConf = new TopicDeployEntity(itemOpEntity,
                    brokerConf.getBrokerId(), topicName);
            itemConf.updModifyInfo(itemOpEntity.getDataVerId(), topicNameId,
                    brokerConf.getBrokerPort(), brokerConf.getBrokerIp(),
                    topicStatus, topicPropInfo);
            addRecordMap.put(itemConf.getRecordKey(), itemConf);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record in ")
                    .append(WebFieldDef.TOPICJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private boolean getBrokerConfInfo(Map<String, String> keyValueMap,
            StringBuilder sBuffer, ProcessResult result) {
        // get brokerId
        if (!WebParameterUtils.getIntParamValue(keyValueMap,
                WebFieldDef.BROKERID, true, 0, 0, sBuffer, result)) {
            return result.isSuccess();
        }
        int brokerId = (int) result.getRetData();
        BrokerConfEntity curEntity =
                defMetaDataService.getBrokerConfByBrokerId(brokerId);
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    sBuffer.append("Not found broker configure by ")
                            .append(WebFieldDef.BROKERID.name).append(" = ").append(brokerId)
                            .append(", please create the broker's configure first!").toString());
            return result.isSuccess();
        }
        result.setSuccResult(curEntity);
        return result.isSuccess();
    }

    private StringBuilder buildRetInfo(List<TopicProcessResult> retInfo,
            StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (TopicProcessResult entry : retInfo) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"brokerId\":").append(entry.getBrokerId())
                    .append(",\"topicName\":\"").append(entry.getTopicName()).append("\"")
                    .append(",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrMsg()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Internal method to perform topic deploy status change
     *
     * @param req
     * @param chgType
     * @return
     */
    private StringBuilder innModifyTopicDeployStatusInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            TopicStsChgType chgType) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get topicName info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get brokerId info
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIdSet = (Set<Integer>) result.getRetData();
        // modify record status
        List<TopicProcessResult> retInfo = new ArrayList<>();
        for (Integer brokerId : brokerIdSet) {
            for (String topicName : topicNameSet) {
                retInfo.add(defMetaDataService.updTopicDeployStatusInfo(opEntity,
                        brokerId, topicName, chgType, sBuffer, result));
            }
        }
        return buildRetInfo(retInfo, sBuffer);
    }
}
