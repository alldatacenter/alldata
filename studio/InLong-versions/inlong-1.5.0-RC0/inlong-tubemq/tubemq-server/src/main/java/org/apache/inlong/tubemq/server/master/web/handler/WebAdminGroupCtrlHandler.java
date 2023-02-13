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
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumeGroupInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.NodeRebInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class WebAdminGroupCtrlHandler extends AbstractWebHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(WebAdminGroupCtrlHandler.class);

    public WebAdminGroupCtrlHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_black_consumer_group_info",
                "adminQueryBlackGroupInfo");
        registerQueryWebMethod("admin_query_allowed_consumer_group_info",
                "adminQueryConsumerGroupInfo");
        registerQueryWebMethod("admin_query_group_filtercond_info",
                "adminQueryGroupFilterCondInfo");
        registerQueryWebMethod("admin_query_consume_group_setting",
                "adminQueryConsumeGroupSetting");
        // register modify method
        registerModifyWebMethod("admin_add_black_consumergroup_info",
                "adminAddBlackGroupInfo");
        registerModifyWebMethod("admin_bath_add_black_consumergroup_info",
                "adminBatchAddBlackGroupInfo");
        registerModifyWebMethod("admin_delete_black_consumergroup_info",
                "adminDeleteBlackGroupInfo");
        registerModifyWebMethod("admin_batch_delete_black_consumergroup_info",
                "adminBatchDeleteBlackGroupInfo");
        registerModifyWebMethod("admin_add_authorized_consumergroup_info",
                "adminAddConsumerGroupInfo");
        registerModifyWebMethod("admin_delete_allowed_consumer_group_info",
                "adminDeleteConsumerGroupInfo");
        registerModifyWebMethod("admin_batch_del_authorized_consumergroup_info",
                "adminBatchDelConsumerGroupInfo");
        registerModifyWebMethod("admin_bath_add_authorized_consumergroup_info",
                "adminBatchAddConsumerGroupInfo");
        registerModifyWebMethod("admin_add_group_filtercond_info",
                "adminAddGroupFilterCondInfo");
        registerModifyWebMethod("admin_bath_add_group_filtercond_info",
                "adminBatchAddGroupFilterCondInfo");
        registerModifyWebMethod("admin_mod_group_filtercond_info",
                "adminModGroupFilterCondInfo");
        registerModifyWebMethod("admin_bath_mod_group_filtercond_info",
                "adminBatchModGroupFilterCondInfo");
        registerModifyWebMethod("admin_del_group_filtercond_info",
                "adminDeleteGroupFilterCondInfo");
        registerModifyWebMethod("admin_add_consume_group_setting",
                "adminAddConsumeGroupSettingInfo");
        registerModifyWebMethod("admin_bath_add_consume_group_setting",
                "adminBatchAddConsumeGroupSetting");
        registerModifyWebMethod("admin_upd_consume_group_setting",
                "adminUpdConsumeGroupSetting");
        registerModifyWebMethod("admin_del_consume_group_setting",
                "adminDeleteConsumeGroupSetting");
        registerModifyWebMethod("admin_rebalance_group_allocate",
                "adminRebalanceGroupAllocateInfo");
        registerModifyWebMethod("admin_set_client_balance_group_consume_from_max",
                "adminSetBalanceGroupConsumeFromMax");
        registerQueryWebMethod("admin_query_client_balance_group_set",
                "adminQueryClientBalanceGroupSet");
    }

    /**
     * Query black consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryBlackGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // build query entity
        GroupConsumeCtrlEntity entity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, entity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // only query disable consume group
        entity.setConsumeEnable(EnableStatus.STATUS_DISABLE);
        Map<String, List<GroupConsumeCtrlEntity>> qryResult =
                defMetaDataService.getGroupConsumeCtrlConf(groupNameSet, topicNameSet, entity);
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (List<GroupConsumeCtrlEntity> entryList : qryResult.values()) {
            for (GroupConsumeCtrlEntity entry : entryList) {
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("{\"groupName\":\"").append(entry.getGroupName())
                        .append("\",\"topicName\":\"").append(entry.getTopicName())
                        .append("\",\"reason\":\"").append(entry.getDisableReason())
                        .append("\",\"dataVersionId\":").append(entry.getDataVerId())
                        .append(",\"createUser\":\"").append(entry.getCreateUser())
                        .append("\",\"createDate\":\"").append(entry.getCreateDateStr())
                        .append("\",\"modifyUser\":\"").append(entry.getModifyUser())
                        .append("\",\"modifyDate\":\"").append(entry.getModifyDateStr())
                        .append("\"}");
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Query allowed(authorized?) consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryConsumerGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // build query entity
        GroupConsumeCtrlEntity qryEntity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        qryEntity.setConsumeEnable(EnableStatus.STATUS_ENABLE);
        Map<String, List<GroupConsumeCtrlEntity>> qryResultMap =
                defMetaDataService.getGroupConsumeCtrlConf(groupNameSet, topicNameSet, qryEntity);
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (List<GroupConsumeCtrlEntity> entryLst : qryResultMap.values()) {
            if (entryLst == null || entryLst.isEmpty()) {
                continue;
            }
            for (GroupConsumeCtrlEntity entry : entryLst) {
                if (entry == null) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("{\"topicName\":\"").append(entry.getTopicName())
                        .append("\",\"groupName\":\"").append(entry.getGroupName())
                        .append("\",\"dataVersionId\":").append(entry.getDataVerId())
                        .append(",\"createUser\":\"").append(entry.getCreateUser())
                        .append("\",\"createDate\":\"").append(entry.getCreateDateStr())
                        .append("\",\"modifyUser\":\"").append(entry.getModifyUser())
                        .append("\",\"modifyDate\":\"").append(entry.getModifyDateStr()).append("\"}");
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Query group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // build query entity
        GroupConsumeCtrlEntity qryEntity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get condStatus field
        if (!getCondStatusParamValue(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        EnableStatus filterEnable = (EnableStatus) result.getRetData();
        // get filterConds info
        if (!WebParameterUtils.getFilterCondSet(req, false, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> filterCondSet = (Set<String>) result.getRetData();
        qryEntity.updModifyInfo(qryEntity.getDataVerId(),
                null, null, filterEnable, null);
        Map<String, List<GroupConsumeCtrlEntity>> qryResultMap =
                defMetaDataService.getGroupConsumeCtrlConf(groupNameSet, topicNameSet, qryEntity);
        // build return result
        int totalCnt = 0;
        int condStatusId = 0;
        String itemFilterStr;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (List<GroupConsumeCtrlEntity> consumeCtrlEntityList : qryResultMap.values()) {
            if (consumeCtrlEntityList == null || consumeCtrlEntityList.isEmpty()) {
                continue;
            }
            for (GroupConsumeCtrlEntity entry : consumeCtrlEntityList) {
                if (entry == null
                        || !WebParameterUtils.isFilterSetFullIncluded(
                                filterCondSet, entry.getFilterCondStr())) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                condStatusId = entry.getFilterEnable().isEnable() ? 2 : 0;
                itemFilterStr = (entry.getFilterCondStr().length() <= 2)
                        ? ""
                        : entry.getFilterCondStr();
                sBuffer.append("{\"topicName\":\"").append(entry.getTopicName())
                        .append("\",\"groupName\":\"").append(entry.getGroupName())
                        .append("\",\"condStatus\":").append(condStatusId)
                        .append(",\"filterConds\":\"").append(itemFilterStr)
                        .append("\",\"dataVersionId\":").append(entry.getDataVerId())
                        .append(",\"createUser\":\"").append(entry.getCreateUser())
                        .append("\",\"createDate\":\"").append(entry.getCreateDateStr())
                        .append("\",\"modifyUser\":\"").append(entry.getModifyUser())
                        .append("\",\"modifyDate\":\"").append(entry.getModifyDateStr()).append("\"}");
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Query consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryConsumeGroupSetting(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // build query entity
        GroupResCtrlEntity entity = new GroupResCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, entity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // get group list
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.OLDALWDBCRATE, false,
                TBaseConstants.META_VALUE_UNDEFINED, 0, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int allowedBClientRate = (int) result.getRetData();
        // query matched records
        entity.updModifyInfo(entity.getDataVerId(), null, allowedBClientRate,
                TBaseConstants.META_VALUE_UNDEFINED, null,
                TBaseConstants.META_VALUE_UNDEFINED, null);
        Map<String, GroupResCtrlEntity> groupResCtrlEntityMap =
                defMetaDataService.getGroupCtrlConf(groupNameSet, entity);
        // build return result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupResCtrlEntity entry : groupResCtrlEntityMap.values()) {
            if (entry == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"groupName\":\"").append(entry.getGroupName())
                    .append("\",\"enableBind\":1,\"allowedBClientRate\":")
                    .append(entry.getAllowedBrokerClientRate())
                    .append(",\"attributes\":\"\",\"lastBindUsedDate\":\"-\"")
                    .append(",\"dataVersionId\":").append(entry.getDataVerId())
                    .append(",\"createUser\":\"").append(entry.getCreateUser())
                    .append("\",\"createDate\":\"").append(entry.getCreateDateStr())
                    .append("\",\"modifyUser\":\"").append(entry.getModifyUser())
                    .append("\",\"modifyDate\":\"").append(entry.getModifyDateStr()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Add black consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddBlackGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                defMetaDataService, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // add black list records
        GroupConsumeCtrlEntity ctrlEntity;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                        groupName, topicName);
                if (ctrlEntity != null
                        && ctrlEntity.getConsumeEnable() == EnableStatus.STATUS_DISABLE) {
                    result.setFailResult(DataOpErrCode.DERR_SUCCESS.getCode(), "Ok!");
                    retInfoList.add(new GroupProcessResult(groupName, topicName, result));
                    continue;
                }
                retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity, groupName,
                        topicName, EnableStatus.STATUS_DISABLE, "Old API add blacklist, disable consume",
                        null, null, sBuffer, result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add black consumer group info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddBlackGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCsmJsonSetInfo(req, opEntity, EnableStatus.STATUS_DISABLE,
                "Old API batch set BlackList", sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and build result
        GroupConsumeCtrlEntity ctrlEntity;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                    entry.getGroupName(), entry.getTopicName());
            if (ctrlEntity != null
                    && ctrlEntity.getConsumeEnable() == EnableStatus.STATUS_DISABLE) {
                result.setFailResult(DataOpErrCode.DERR_SUCCESS.getCode(), "Ok!");
                retInfoList.add(new GroupProcessResult(entry.getGroupName(),
                        entry.getTopicName(), result));
                continue;
            }
            retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(entry, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Delete black consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteBlackGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // add allowed consume records
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        if (groupNameSet.isEmpty()) {
            Map<String, List<GroupConsumeCtrlEntity>> topicConsumeCtrlMap =
                    defMetaDataService.getConsumeCtrlByGroupName(topicNameSet);
            for (Map.Entry<String, List<GroupConsumeCtrlEntity>> entry : topicConsumeCtrlMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    for (GroupConsumeCtrlEntity ctrlEntity : entry.getValue()) {
                        if (ctrlEntity != null
                                && ctrlEntity.getConsumeEnable() != EnableStatus.STATUS_ENABLE) {
                            defMetaDataService.insertConsumeCtrlInfo(opEntity,
                                    ctrlEntity.getGroupName(), ctrlEntity.getTopicName(),
                                    EnableStatus.STATUS_ENABLE, "Old API delete blacklist, enable consume",
                                    null, null, sBuffer, result);
                        }
                    }
                }
                result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                retInfoList.add(new GroupProcessResult("", entry.getKey(), result));
            }
        } else {
            GroupConsumeCtrlEntity ctrlEntity;
            for (String groupName : groupNameSet) {
                for (String topicName : topicNameSet) {
                    ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                            groupName, topicName);
                    if (ctrlEntity != null
                            && ctrlEntity.getConsumeEnable() != EnableStatus.STATUS_ENABLE) {
                        retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity,
                                groupName, topicName, EnableStatus.STATUS_ENABLE,
                                "Old API delete blacklist, enable consume",
                                null, null, sBuffer, result));
                    } else {
                        result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                        retInfoList.add(new GroupProcessResult(groupName, topicName, result));
                    }
                }
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Batch delete black consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchDeleteBlackGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCsmJsonSetInfo(req, opEntity, null,
                "Old API batch Disable consume", sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and build result
        GroupConsumeCtrlEntity ctrlEntity;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                    entry.getGroupName(), entry.getTopicName());
            if (ctrlEntity != null
                    && ctrlEntity.getConsumeEnable() != EnableStatus.STATUS_ENABLE) {
                retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity,
                        entry.getGroupName(), entry.getTopicName(), EnableStatus.STATUS_ENABLE,
                        "Old API delete blacklist, enable consume",
                        null, null, sBuffer, result));
            } else {
                result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                retInfoList.add(new GroupProcessResult(
                        entry.getGroupName(), entry.getTopicName(), result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add authorized consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddConsumerGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                defMetaDataService, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        // add allowed consume records
        GroupConsumeCtrlEntity ctrlEntity;
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                        groupName, topicName);
                if (ctrlEntity != null
                        && ctrlEntity.getConsumeEnable() == EnableStatus.STATUS_ENABLE) {
                    result.setFailResult(DataOpErrCode.DERR_SUCCESS.getCode(), "Ok!");
                    retInfoList.add(new GroupProcessResult(groupName, topicName, result));
                    continue;
                }
                retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity, groupName,
                        topicName, EnableStatus.STATUS_ENABLE, "Old API add, enable consume",
                        null, null, sBuffer, result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add authorized consumer group info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddConsumerGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCsmJsonSetInfo(req, opEntity, EnableStatus.STATUS_ENABLE,
                "Old API batch set Enable Consume", sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and build result
        GroupConsumeCtrlEntity ctrlEntity;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                    entry.getGroupName(), entry.getTopicName());
            if (ctrlEntity != null
                    && ctrlEntity.getConsumeEnable() == EnableStatus.STATUS_ENABLE) {
                result.setFailResult(DataOpErrCode.DERR_SUCCESS.getCode(), "Ok!");
                retInfoList.add(new GroupProcessResult(entry.getGroupName(),
                        entry.getTopicName(), result));
                continue;
            }
            retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(entry, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Delete allowed(authorized) consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteConsumerGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        if (groupNameSet.isEmpty()) {
            Map<String, List<GroupConsumeCtrlEntity>> topicConsumeCtrlMap =
                    defMetaDataService.getConsumeCtrlByGroupName(topicNameSet);
            for (Map.Entry<String, List<GroupConsumeCtrlEntity>> entry : topicConsumeCtrlMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    for (GroupConsumeCtrlEntity ctrlEntity : entry.getValue()) {
                        if (ctrlEntity != null
                                && ctrlEntity.getConsumeEnable() != EnableStatus.STATUS_DISABLE) {
                            defMetaDataService.insertConsumeCtrlInfo(opEntity,
                                    ctrlEntity.getGroupName(), ctrlEntity.getTopicName(),
                                    EnableStatus.STATUS_DISABLE, "Old API delete, disable consume",
                                    null, null, sBuffer, result);
                        }
                    }
                }
                result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                retInfoList.add(new GroupProcessResult("", entry.getKey(), result));
            }
        } else {
            GroupConsumeCtrlEntity ctrlEntity;
            for (String groupName : groupNameSet) {
                for (String topicName : topicNameSet) {
                    ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                            groupName, topicName);
                    if (ctrlEntity != null
                            && ctrlEntity.getConsumeEnable() != EnableStatus.STATUS_DISABLE) {
                        retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity,
                                groupName, topicName, EnableStatus.STATUS_DISABLE,
                                "Old API delete, disable consume",
                                null, null, sBuffer, result));
                    } else {
                        result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                        retInfoList.add(new GroupProcessResult(groupName, topicName, result));
                    }
                }
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Delete authorized consumer group info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchDelConsumerGroupInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCsmJsonSetInfo(req, opEntity, EnableStatus.STATUS_DISABLE,
                "Old API batch delete Authorized Consume", sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and build result
        GroupConsumeCtrlEntity ctrlEntity;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                    entry.getGroupName(), entry.getTopicName());
            if (ctrlEntity != null
                    && ctrlEntity.getConsumeEnable() != EnableStatus.STATUS_DISABLE) {
                retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(entry, sBuffer, result));
            } else {
                result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                retInfoList.add(new GroupProcessResult(entry.getGroupName(),
                        entry.getTopicName(), result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrModGroupFilterCondInfo(req, sBuffer, result, true);
    }

    /**
     * Modify group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminModGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrModGroupFilterCondInfo(req, sBuffer, result, false);
    }

    /**
     * Add group filter info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innBatchAddOrUpdGroupFilterCondInfo(req, sBuffer, result, true);
    }

    /**
     * Modify group filter condition info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchModGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innBatchAddOrUpdGroupFilterCondInfo(req, sBuffer, result, false);
    }

    /**
     * Delete group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        if (groupNameSet.isEmpty()) {
            Map<String, List<GroupConsumeCtrlEntity>> topicConsumeCtrlMap =
                    defMetaDataService.getConsumeCtrlByGroupName(topicNameSet);
            for (Map.Entry<String, List<GroupConsumeCtrlEntity>> entry : topicConsumeCtrlMap.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                    retInfoList.add(new GroupProcessResult("", entry.getKey(), result));
                    continue;
                }
                for (GroupConsumeCtrlEntity ctrlEntity : entry.getValue()) {
                    if (ctrlEntity != null
                            && ctrlEntity.getFilterEnable() != EnableStatus.STATUS_DISABLE) {
                        defMetaDataService.insertConsumeCtrlInfo(opEntity,
                                ctrlEntity.getGroupName(), ctrlEntity.getTopicName(), null,
                                "Old API delete, disable filter", EnableStatus.STATUS_DISABLE,
                                TServerConstants.BLANK_FILTER_ITEM_STR, sBuffer, result);
                    }
                }
                result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                retInfoList.add(new GroupProcessResult("", entry.getKey(), result));
            }
        } else {
            GroupConsumeCtrlEntity ctrlEntity;
            for (String groupName : groupNameSet) {
                for (String topicName : topicNameSet) {
                    ctrlEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                            groupName, topicName);
                    if (ctrlEntity != null
                            && ctrlEntity.getFilterEnable() != EnableStatus.STATUS_DISABLE) {
                        retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity,
                                groupName, topicName, null,
                                "Old API delete, disable filter", EnableStatus.STATUS_DISABLE,
                                TServerConstants.BLANK_FILTER_ITEM_STR, sBuffer, result));
                    } else {
                        result.setFullInfo(true, DataOpErrCode.DERR_SUCCESS.getCode(), "Ok");
                        retInfoList.add(new GroupProcessResult(groupName, topicName, result));
                    }
                }
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Re-balance group allocation info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminRebalanceGroupAllocateInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group configure info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        String groupName = (String) result.getRetData();
        // get reJoinWait info
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.REJOINWAIT, false, 0, 0, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int reJoinWait = (int) result.getRetData();
        // get consumerId list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSCONSUMERID, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> consumerIdSet = (Set<String>) result.getRetData();
        ConsumerInfoHolder consumerInfoHolder =
                master.getConsumerHolder();
        ConsumeGroupInfo consumeGroupInfo =
                consumerInfoHolder.getConsumeGroupInfo(groupName);
        if (consumeGroupInfo == null) {
            String errInfo = sBuffer.append("The group(")
                    .append(groupName).append(") not online!").toString();
            sBuffer.delete(0, sBuffer.length());
            WebParameterUtils.buildFailResult(sBuffer, errInfo);
            return sBuffer;
        }
        Map<String, NodeRebInfo> nodeRebInfoMap = consumeGroupInfo.getBalanceMap();
        for (String consumerId : consumerIdSet) {
            if (nodeRebInfoMap.containsKey(consumerId)) {
                String errInfo = sBuffer.append("Duplicated set for consumerId(")
                        .append(consumerId).append(") in group(")
                        .append(groupName).append(")! \"}").toString();
                sBuffer.delete(0, sBuffer.length());
                WebParameterUtils.buildFailResult(sBuffer, errInfo);
                return sBuffer;
            }
        }
        logger.info(sBuffer.append("[Re-balance] Add rebalance consumer: group=")
                .append(groupName).append(", consumerIds=")
                .append(consumerIdSet.toString())
                .append(", reJoinWait=").append(reJoinWait)
                .append(", creator=").append(opEntity.getModifyUser()).toString());
        sBuffer.delete(0, sBuffer.length());
        consumerInfoHolder.addRebConsumerInfo(groupName, consumerIdSet, reJoinWait);
        WebParameterUtils.buildSuccessResult(sBuffer);
        return sBuffer;
    }

    /**
     * Add consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddConsumeGroupSettingInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrUpdConsumeGroupSettingInfo(req, sBuffer, result, true);
    }

    /**
     * Update consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminUpdConsumeGroupSetting(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrUpdConsumeGroupSettingInfo(req, sBuffer, result, false);
    }

    /**
     * Add consumer group setting in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddConsumeGroupSetting(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCtrlJsonSetInfo(req, opEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupResCtrlEntity> addRecordMap =
                (Map<String, GroupResCtrlEntity>) result.getRetData();
        // add or update and build result
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupResCtrlEntity resCtrlEntity : addRecordMap.values()) {
            retInfoList.add(defMetaDataService.insertGroupCtrlConf(
                    resCtrlEntity, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Delete consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteConsumeGroupSetting(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // add or update group control record
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            retInfoList.add(defMetaDataService.addOrUpdGroupCtrlConf(false, opEntity,
                    groupName, EnableStatus.STATUS_DISABLE, 0,
                    TBaseConstants.META_VALUE_UNDEFINED, null,
                    TBaseConstants.META_VALUE_UNDEFINED, null, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    private StringBuilder buildRetInfo(List<GroupProcessResult> retInfo,
            StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupProcessResult entry : retInfo) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"groupName\":\"").append(entry.getGroupName())
                    .append("\",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrMsg()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Inner method: add consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @param isAddOp   whether add operation
     * @return    process result
     */
    private StringBuilder innAddOrUpdConsumeGroupSettingInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // get resCheckStatus info
        if (!WebParameterUtils.getEnableStatusValue(req, WebFieldDef.RESCHECKENABLE,
                false, (isAddOp ? EnableStatus.STATUS_DISABLE : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        EnableStatus resChkEnable = (EnableStatus) result.getRetData();
        // get and valid allowedBClientRate info
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.OLDALWDBCRATE,
                false, (isAddOp ? TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int allowedBClientRate = (int) result.getRetData();
        // add or update group control record
        GroupResCtrlEntity grpResCtrl;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            grpResCtrl = defMetaDataService.getGroupCtrlConf(groupName);
            if (!isAddOp && grpResCtrl == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        DataOpErrCode.DERR_NOT_EXIST.getDescription());
                retInfoList.add(new GroupProcessResult(groupName, "", result));
                continue;
            }
            retInfoList.add(defMetaDataService.insertGroupCtrlConf(opEntity,
                    groupName, resChkEnable, allowedBClientRate, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Inner method: modify group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @param isAddOp   whether add operation
     * @return    process result
     */
    private StringBuilder innAddOrModGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                defMetaDataService, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get condStatus field
        if (!getCondStatusParamValue(req, false,
                (isAddOp ? EnableStatus.STATUS_DISABLE : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        EnableStatus filterEnable = (EnableStatus) result.getRetData();
        // get filterConds info
        if (!WebParameterUtils.getFilterCondString(req, false, isAddOp, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        String filterCondStr = (String) result.getRetData();
        // add or modify filter consume records
        GroupConsumeCtrlEntity ctrlEntity;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                ctrlEntity =
                        defMetaDataService.getConsumeCtrlByGroupAndTopic(groupName, topicName);
                if (ctrlEntity == null) {
                    if (isAddOp) {
                        retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity, groupName,
                                topicName, EnableStatus.STATUS_ENABLE, "Old API set filter conditions",
                                filterEnable, filterCondStr, sBuffer, result));
                    } else {
                        result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                                DataOpErrCode.DERR_NOT_EXIST.getDescription());
                        retInfoList.add(new GroupProcessResult(groupName, "", result));
                    }
                } else {
                    retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(opEntity, groupName,
                            topicName, null, "Old API set filter conditions",
                            filterEnable, filterCondStr, sBuffer, result));
                }
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Inner method: add group filter info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @param isAddOp   whether add operation
     * @return    process result
     */
    private StringBuilder innBatchAddOrUpdGroupFilterCondInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get filterCondJsonSet info
        if (!getFilterJsonSetInfo(req, isAddOp, opEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and build result
        GroupConsumeCtrlEntity curEntity;
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            curEntity = defMetaDataService.getConsumeCtrlByGroupAndTopic(
                    entry.getGroupName(), entry.getTopicName());
            if (curEntity == null) {
                if (isAddOp) {
                    entry.setConsumeEnable(EnableStatus.STATUS_ENABLE);
                } else {
                    result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                            DataOpErrCode.DERR_NOT_EXIST.getDescription());
                    retInfoList.add(new GroupProcessResult(entry.getGroupName(),
                            entry.getTopicName(), result));
                    continue;
                }
            }
            retInfoList.add(defMetaDataService.insertConsumeCtrlInfo(entry, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    private boolean getFilterJsonSetInfo(HttpServletRequest req, boolean isAddOp,
            BaseEntity defOpEntity, StringBuilder sBuffer,
            ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.FILTERJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> groupJsonArray =
                (List<Map<String, String>>) result.getRetData();
        GroupConsumeCtrlEntity itemEntity;
        Map<String, GroupConsumeCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                defMetaDataService.getDeployedTopicSet();
        for (Map<String, String> itemValueMap : groupJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemValueMap,
                    isAddOp, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get group configure info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            final String groupName = (String) result.getRetData();
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.TOPICNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String topicName = (String) result.getRetData();
            if (!configuredTopicSet.contains(topicName)) {
                result.setFailResult(sBuffer
                        .append(WebFieldDef.TOPICNAME.name)
                        .append(" ").append(topicName)
                        .append(" is not configure, please configure first!").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            // check and get condStatus field
            if (!getCondStatusParamValue(req, false,
                    (isAddOp ? EnableStatus.STATUS_DISABLE : null), sBuffer, result)) {
                return result.isSuccess();
            }
            EnableStatus filterEnable = (EnableStatus) result.getRetData();
            // get filterConds info
            if (!WebParameterUtils.getFilterCondString(req,
                    false, isAddOp, sBuffer, result)) {
                return result.isSuccess();
            }
            String filterCondStr = (String) result.getRetData();
            itemEntity =
                    new GroupConsumeCtrlEntity(itemOpEntity, groupName, topicName);
            itemEntity.updModifyInfo(itemOpEntity.getDataVerId(),
                    null, null, filterEnable, filterCondStr);
            addRecordMap.put(itemEntity.getRecordKey(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.FILTERJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private boolean getGroupCtrlJsonSetInfo(HttpServletRequest req, BaseEntity defOpEntity,
            StringBuilder sBuffer, ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> groupJsonArray =
                (List<Map<String, String>>) result.getRetData();
        GroupResCtrlEntity itemEntity;
        Map<String, String> itemValueMap;
        Map<String, GroupResCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                defMetaDataService.getDeployedTopicSet();
        for (int j = 0; j < groupJsonArray.size(); j++) {
            itemValueMap = groupJsonArray.get(j);
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemValueMap,
                    true, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get group configure info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String groupName = (String) result.getRetData();
            // get resCheckStatus info
            if (!WebParameterUtils.getEnableStatusValue(itemValueMap, WebFieldDef.RESCHECKENABLE,
                    false, EnableStatus.STATUS_DISABLE, sBuffer, result)) {
                return result.isSuccess();
            }
            EnableStatus resChkEnable = (EnableStatus) result.getRetData();
            // get and valid allowedBClientRate info
            if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.OLDALWDBCRATE,
                    false, TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN,
                    TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
                return result.isSuccess();
            }
            int allowedB2CRate = (int) result.getRetData();
            itemEntity =
                    new GroupResCtrlEntity(itemOpEntity, groupName);
            itemEntity.updModifyInfo(itemOpEntity.getDataVerId(), resChkEnable, allowedB2CRate,
                    TBaseConstants.META_VALUE_UNDEFINED, null,
                    TBaseConstants.META_VALUE_UNDEFINED, null);
            addRecordMap.put(itemEntity.getGroupName(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.GROUPJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private boolean getGroupCsmJsonSetInfo(HttpServletRequest req, BaseEntity defOpEntity,
            EnableStatus enableCsm, String opReason,
            StringBuilder sBuffer, ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> groupJsonArray =
                (List<Map<String, String>>) result.getRetData();
        GroupConsumeCtrlEntity itemEntity;
        Map<String, GroupConsumeCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                defMetaDataService.getDeployedTopicSet();
        for (Map<String, String> itemValueMap : groupJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemValueMap,
                    true, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get group configure info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String groupName = (String) result.getRetData();
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.TOPICNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String topicName = (String) result.getRetData();
            if (!configuredTopicSet.contains(topicName)) {
                result.setFailResult(sBuffer
                        .append(WebFieldDef.TOPICNAME.name)
                        .append(" ").append(topicName)
                        .append(" is not configure, please configure first!").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            itemEntity =
                    new GroupConsumeCtrlEntity(itemOpEntity, groupName, topicName);
            itemEntity.updModifyInfo(itemOpEntity.getDataVerId(),
                    enableCsm, opReason, null, null);
            addRecordMap.put(itemEntity.getRecordKey(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.GROUPJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private <T> boolean getCondStatusParamValue(T paramCntr, boolean required,
            EnableStatus defValue, StringBuilder sBuffer,
            ProcessResult result) {
        // check and get condStatus field
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.CONDSTATUS,
                required, TBaseConstants.META_VALUE_UNDEFINED, 0, 2, sBuffer, result)) {
            return result.isSuccess();
        }
        int paramValue = (int) result.getRetData();
        if (paramValue == TBaseConstants.META_VALUE_UNDEFINED) {
            result.setSuccResult(defValue);
        } else {
            if (paramValue == 2) {
                result.setSuccResult(EnableStatus.STATUS_ENABLE);
            } else {
                result.setSuccResult(EnableStatus.STATUS_DISABLE);
            }
        }
        return result.isSuccess();
    }

    /**
     * Query client balance group set
     *
     * @param req      request
     * @param sBuffer  string buffer
     * @param result   process result
     *
     * @return   process result
     */
    public StringBuilder adminQueryClientBalanceGroupSet(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        try {
            ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
            List<String> clientGroups = consumerHolder.getAllClientBalanceGroups();
            int j = 0;
            WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
            for (String groupName : clientGroups) {
                if (TStringUtils.isEmpty(groupName)) {
                    continue;
                }
                if (j++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("\"").append(groupName).append("\"");
            }
            WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, j);
        } catch (Exception e) {
            sBuffer.delete(0, sBuffer.length());
            WebParameterUtils.buildFailResult(sBuffer, e.getMessage());
        }
        return sBuffer;
    }

    /**
     * Set online client-balance group consume from max offset
     *
     * @param req     the request object
     * @param sBuffer  string buffer
     * @param result   the result object
     * @return  the return result
     */
    public StringBuilder adminSetBalanceGroupConsumeFromMax(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> groupNameSet = (Set<String>) result.getRetData();
        // process offset setting
        ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
        List<String> clientGroups = consumerHolder.getAllClientBalanceGroups();
        Set<String> filtedGroupSet = new TreeSet<>();
        for (String groupName : groupNameSet) {
            if (clientGroups.contains(groupName)) {
                filtedGroupSet.add(groupName);
            }
        }
        if (filtedGroupSet.isEmpty()) {
            WebParameterUtils.buildFailResult(sBuffer,
                    "all consumer groups are not client balance groups");
        } else {
            ConsumeGroupInfo groupInfo;
            for (String groupName : filtedGroupSet) {
                groupInfo = consumerHolder.getConsumeGroupInfo(groupName);
                if (groupInfo == null) {
                    continue;
                }
                groupInfo.updCsmFromMaxCtrlId();
            }
            logger.info(sBuffer.append("[Admin reset] ").append(opEntity.getModifyUser())
                    .append(" set client-balance group consume from max offset, group set = ")
                    .append(filtedGroupSet.toString()).toString());
            sBuffer.delete(0, sBuffer.length());
            WebParameterUtils.buildSuccessResult(sBuffer);
        }
        return sBuffer;
    }
}
