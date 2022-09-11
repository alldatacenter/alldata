/*
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

package org.apache.inlong.tubemq.server.master.web.handler;

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.common.statusdef.StepStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerAbnHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunStatusInfo;

/**
 *
 * The class to handle the default config of broker, including:
 * - Add config
 * - Update config
 * - Delete config
 * And manage the broker status.
 *
 * Please note that one IP could only host one broker, and brokerId must be unique
 */
public class WebBrokerConfHandler extends AbstractWebHandler {

    /**
     * Constructor
     *
     * @param master tube master
     */
    public WebBrokerConfHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_broker_configure",
                "adminQueryBrokerConfInfo");
        registerQueryWebMethod("admin_query_broker_run_status",
                "adminQueryBrokerRunStatusInfo");

        // register modify method
        registerModifyWebMethod("admin_add_broker_configure",
                "adminAddBrokerConfInfo");
        registerModifyWebMethod("admin_batch_add_broker_configure",
                "adminBatchAddBrokerConfInfo");
        registerModifyWebMethod("admin_update_broker_configure",
                "adminUpdateBrokerConfInfo");
        registerModifyWebMethod("admin_batch_update_broker_configure",
                "adminBatchUpdBrokerConfInfo");
        registerModifyWebMethod("admin_delete_broker_configure",
                "adminDeleteBrokerConfEntityInfo");
        registerModifyWebMethod("admin_online_broker_configure",
                "adminOnlineBrokerConf");
        registerModifyWebMethod("admin_set_broker_read_or_write",
                "adminSetReadOrWriteBrokerConf");
        registerModifyWebMethod("admin_offline_broker_configure",
                "adminOfflineBrokerConf");
        registerModifyWebMethod("admin_reload_broker_configure",
                "adminReloadBrokerConf");
        registerModifyWebMethod("admin_release_broker_autoforbidden_status",
                "adminRelBrokerAutoForbiddenStatus");

        // Deprecated methods begin
        // register modify method
        registerModifyWebMethod("admin_bath_add_broker_configure",
                "adminBatchAddBrokerConfInfo");
        // Deprecated methods end
    }

    /**
     * Query broker config
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryBrokerConfInfo(HttpServletRequest req,
                                                  StringBuilder sBuffer,
                                                  ProcessResult result) {
        BrokerConfEntity qryEntity = new BrokerConfEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        // get brokerIp info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPBROKERIP, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> brokerIpSet = (Set<String>) result.getRetData();
        // get brokerPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERPORT,
                false, TBaseConstants.META_VALUE_UNDEFINED, 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int brokerPort = (int) result.getRetData();
        // get brokerTlsPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERTLSPORT,
                false, TBaseConstants.META_VALUE_UNDEFINED, 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int brokerTlsPort = (int) result.getRetData();
        // get brokerWebPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERWEBPORT,
                false, TBaseConstants.META_VALUE_UNDEFINED, 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int brokerWebPort = (int) result.getRetData();
        // get regionId field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.REGIONID,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                TServerConstants.BROKER_REGION_ID_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int regionId = (int) result.getRetData();
        // get groupId field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.GROUPID,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                TServerConstants.BROKER_GROUP_ID_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final int groupId = (int) result.getRetData();
        // get and valid TopicPropGroup info
        if (!WebParameterUtils.getTopicPropInfo(req,
                null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final TopicPropGroup brokerProps = (TopicPropGroup) result.getRetData();
        // get and valid TopicStatusId info
        if (!WebParameterUtils.getTopicStatusParamValue(req,
                false, TopicStatus.STATUS_TOPIC_UNDEFINED, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        TopicStatus topicStatus = (TopicStatus) result.getRetData();
        // get and valid broker manage status info
        if (!getManageStatusParamValue(req, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final ManageStatus mngStatus = (ManageStatus) result.getRetData();
        // get topic info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get isInclude info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.ISINCLUDE, false, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean isInclude = (Boolean) result.getRetData();
        // get withTopic info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.WITHTOPIC, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean withTopic = (Boolean) result.getRetData();
        // fill query entity fields
        qryEntity.updModifyInfo(qryEntity.getDataVerId(), brokerPort, brokerTlsPort,
                brokerWebPort, regionId, groupId, mngStatus, brokerProps);
        Map<Integer, BrokerConfEntity> qryResult =
                defMetaDataService.getBrokerConfInfo(brokerIds, brokerIpSet, qryEntity);
        // build query result
        int totalCnt = 0;
        boolean isConfUpdated;
        boolean isConfLoaded;
        Tuple2<Boolean, Boolean> syncTuple;
        BrokerRunStatusInfo runStatusInfo;
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (BrokerConfEntity entity : qryResult.values()) {
            Map<String, TopicDeployEntity> topicConfEntityMap =
                    defMetaDataService.getBrokerTopicConfEntitySet(entity.getBrokerId());
            if (!isValidRecord(topicNameSet, isInclude, topicStatus, topicConfEntityMap)) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            isConfUpdated = false;
            isConfLoaded = false;
            runStatusInfo = brokerRunManager.getBrokerRunStatusInfo(entity.getBrokerId());
            if (runStatusInfo != null) {
                syncTuple = runStatusInfo.getDataSyncStatus();
                isConfUpdated = syncTuple.getF0();
                isConfLoaded = syncTuple.getF1();
            }
            entity.toWebJsonStr(sBuffer, isConfUpdated, isConfLoaded, true, false);
            addTopicInfo(withTopic, sBuffer, topicConfEntityMap);
            sBuffer.append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Add broker configure
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddBrokerConfInfo(HttpServletRequest req,
                                                StringBuilder sBuffer,
                                                ProcessResult result) {
        return innAddOrUpdBrokerConfInfo(req, sBuffer, result, true);
    }

    /**
     * update broker configure
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminUpdateBrokerConfInfo(HttpServletRequest req,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        return innAddOrUpdBrokerConfInfo(req, sBuffer, result, false);
    }

    /**
     * Add broker config to brokers in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddBrokerConfInfo(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
        return innBatchAddOrUpdBrokerConfInfo(req, sBuffer, result, true);
    }

    /**
     * Update broker configure in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchUpdBrokerConfInfo(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
        return innBatchAddOrUpdBrokerConfInfo(req, sBuffer, result, false);
    }

    /**
     * Delete broker config
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteBrokerConfEntityInfo(HttpServletRequest req,
                                                         StringBuilder sBuffer,
                                                         ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get isReservedData info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.ISRESERVEDDATA, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean isReservedData = (Boolean) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        for (Integer brokerId : brokerIds) {
            defMetaDataService.delBrokerConfInfo(opEntity.getModifyUser(),
                    isReservedData, brokerId, sBuffer, result);
            retInfo.add(new BrokerProcessResult(brokerId, null, result));
        }

        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Make broker config online
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminOnlineBrokerConf(HttpServletRequest req,
                                               StringBuilder sBuffer,
                                               ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        for (Integer brokerId : brokerIds) {
            retInfo.add(defMetaDataService.changeBrokerConfStatus(opEntity,
                    brokerId, ManageStatus.STATUS_MANAGE_ONLINE, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Make broker config offline
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminOfflineBrokerConf(HttpServletRequest req,
                                                StringBuilder sBuffer,
                                                ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        for (Integer brokerId : brokerIds) {
            retInfo.add(defMetaDataService.changeBrokerConfStatus(opEntity,
                    brokerId, ManageStatus.STATUS_MANAGE_OFFLINE, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Set read/write status of a broker.
     * The same operations could be made by changing broker's config,
     * but those are extracted here to simplify the code.
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminSetReadOrWriteBrokerConf(HttpServletRequest req,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        // get and valid broker read or write status info
        if (!getAcceptReadAndWriteParamValue(req, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Tuple2<Boolean, Boolean> rdWtTpl =
                (Tuple2<Boolean, Boolean>) result.getRetData();
        // change broker status
        BrokerConfEntity curEntry;
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        for (Integer brokerId : brokerIds) {
            curEntry = defMetaDataService.getBrokerConfByBrokerId(brokerId);
            if (curEntry == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        "The broker configure not exist!");
                retInfo.add(new BrokerProcessResult(brokerId, "", result));
                continue;
            }
            if (curEntry.getManageStatus().getCode()
                    < ManageStatus.STATUS_MANAGE_ONLINE.getCode()) {
                result.setFailResult(DataOpErrCode.DERR_CONDITION_LACK.getCode(),
                        "The broker configure under draft status, please online first!");
                retInfo.add(new BrokerProcessResult(brokerId, "", result));
                continue;
            }
            ManageStatus newMngStatus = ManageStatus.getNewStatus(
                    curEntry.getManageStatus(), rdWtTpl.getF0(), rdWtTpl.getF1());
            if (curEntry.getManageStatus() == newMngStatus) {
                result.setSuccResult(null);
                retInfo.add(new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result));
                continue;
            }
            retInfo.add(defMetaDataService.changeBrokerConfStatus(opEntity,
                    brokerId, newMngStatus, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Reload broker config
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminReloadBrokerConf(HttpServletRequest req,
                                               StringBuilder sBuffer,
                                               ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        for (Integer brokerId : brokerIds) {
            retInfo.add(defMetaDataService.reloadBrokerConfInfo(opEntity, brokerId, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Release broker auto forbidden status
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminRelBrokerAutoForbiddenStatus(HttpServletRequest req,
                                                           StringBuilder sBuffer,
                                                           ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        // check and get relReason field
        if (!WebParameterUtils.getStringParamValue(req, WebFieldDef.RELREASON,
                false, "Web API call", sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        String relReason = (String) result.getRetData();
        BrokerAbnHolder abnHolder = master.getBrokerAbnHolder();
        abnHolder.relAutoForbiddenBrokerInfo(brokerIds, relReason);
        WebParameterUtils.buildSuccessResult(sBuffer);
        return sBuffer;
    }

    /**
     * Query run status of broker
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryBrokerRunStatusInfo(HttpServletRequest req,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        BrokerConfEntity qryEntity = new BrokerConfEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        // check and get brokerId field
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.COMPSBROKERID, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<Integer> brokerIds = (Set<Integer>) result.getRetData();
        // get brokerIp info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPBROKERIP, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> brokerIpSet = (Set<String>) result.getRetData();
        // get withDetail info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.WITHDETAIL, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        boolean withDetail = (Boolean) result.getRetData();
        // get onlyAbnormal info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.ONLYABNORMAL, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        boolean onlyAbnormal = (Boolean) result.getRetData();
        // get onlyAutoForbidden info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.ONLYAUTOFBD, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        boolean onlyAutoForbidden = (Boolean) result.getRetData();
        // get onlyEnableTLS info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.ONLYENABLETLS, false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        boolean onlyEnableTLS = (Boolean) result.getRetData();
        // query current broker configures
        Map<Integer, BrokerConfEntity> brokerConfEntityMap =
                defMetaDataService.getBrokerConfInfo(brokerIds, brokerIpSet, null);
        BrokerAbnHolder abnHolder = master.getBrokerAbnHolder();
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        Map<Integer, BrokerAbnHolder.BrokerAbnInfo> brokerAbnInfoMap =
                abnHolder.getBrokerAbnormalMap();
        Map<Integer, BrokerAbnHolder.BrokerFbdInfo> brokerFbdInfoMap =
                abnHolder.getAutoForbiddenBrokerMapInfo();
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (BrokerConfEntity entity : brokerConfEntityMap.values()) {
            BrokerAbnHolder.BrokerAbnInfo brokerAbnInfo =
                    brokerAbnInfoMap.get(entity.getBrokerId());
            if (onlyAbnormal && brokerAbnInfo == null) {
                continue;
            }
            BrokerAbnHolder.BrokerFbdInfo brokerForbInfo =
                    brokerFbdInfoMap.get(entity.getBrokerId());
            if (onlyAutoForbidden && brokerForbInfo == null) {
                continue;
            }
            BrokerInfo brokerInfo = brokerRunManager.getBrokerInfo(entity.getBrokerId());
            if (onlyEnableTLS && (brokerInfo == null || !brokerInfo.isEnableTLS())) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"brokerId\":").append(entity.getBrokerId())
                    .append(",\"brokerIp\":\"").append(entity.getBrokerIp())
                    .append("\",\"brokerPort\":").append(entity.getBrokerPort())
                    .append(",\"brokerWebPort\":").append(entity.getBrokerWebPort())
                    .append(",\"manageStatus\":\"").append(entity.getManageStatusStr()).append("\"");
            if (brokerInfo == null) {
                sBuffer.append(",\"brokerTLSPort\":").append(entity.getBrokerTLSPort())
                        .append(",\"enableTLS\":\"-\"");
            } else {
                sBuffer.append(",\"brokerTLSPort\":").append(entity.getBrokerTLSPort())
                        .append(",\"enableTLS\":").append(brokerInfo.isEnableTLS());
            }
            if (brokerAbnInfo == null) {
                sBuffer.append(",\"isRepAbnormal\":false");
            } else {
                sBuffer.append(",\"isRepAbnormal\":true,\"repStatus\":")
                        .append(brokerAbnInfo.getAbnStatus());
            }
            if (brokerForbInfo == null) {
                sBuffer.append(",\"isAutoForbidden\":false");
            } else {
                sBuffer.append(",\"isAutoForbidden\":true");
            }
            if (entity.getManageStatus() == ManageStatus.STATUS_MANAGE_APPLY) {
                sBuffer.append(",\"runStatus\":\"-\",\"subStatus\":\"-\"")
                        .append(",\"isConfChanged\":\"-\",\"isConfLoaded\":\"-\",\"isBrokerOnline\":\"-\"")
                        .append(",\"brokerVersion\":\"-\",\"acceptPublish\":\"-\",\"acceptSubscribe\":\"-\"");
            } else {
                Tuple2<Boolean, Boolean> pubSubTuple =
                        entity.getManageStatus().getPubSubStatus();
                BrokerRunStatusInfo runStatusInfo =
                        brokerRunManager.getBrokerRunStatusInfo(entity.getBrokerId());
                if (runStatusInfo == null) {
                    sBuffer.append(",\"runStatus\":\"unRegister\",\"subStatus\":\"-\"")
                            .append(",\"isConfChanged\":\"-\",\"isConfLoaded\":\"-\",\"isBrokerOnline\":\"-\"")
                            .append(",\"brokerVersion\":\"-\",\"acceptPublish\":\"-\",\"acceptSubscribe\":\"-\"");
                } else {
                    StepStatus stepStatus = runStatusInfo.getCurStepStatus();
                    if (runStatusInfo.isOnline()) {
                        if (stepStatus == StepStatus.STEP_STATUS_UNDEFINED) {
                            sBuffer.append(",\"runStatus\":\"running\",\"subStatus\":\"idle\"");
                        } else {
                            sBuffer.append(",\"runStatus\":\"running\"")
                                    .append(",\"subStatus\":\"processing_event\",\"stepOp\":")
                                    .append(stepStatus.getCode());
                        }
                    } else {
                        if (stepStatus == StepStatus.STEP_STATUS_UNDEFINED) {
                            sBuffer.append(",\"runStatus\":\"notRegister\",\"subStatus\":\"idle\"");
                        } else {
                            sBuffer.append(",\"runStatus\":\"notRegister\"")
                                    .append(",\"subStatus\":\"processing_event\",\"stepOp\":")
                                    .append(stepStatus.getCode());
                        }
                    }
                    Tuple2<Boolean, Boolean> syncTuple =
                            runStatusInfo.getDataSyncStatus();
                    sBuffer.append(",\"isConfChanged\":\"").append(syncTuple.getF0())
                            .append("\",\"isConfLoaded\":\"").append(syncTuple.getF1())
                            .append("\",\"isBrokerOnline\":\"").append(runStatusInfo.isOnline())
                            .append("\"").append(",\"brokerVersion\":\"-\",\"acceptPublish\":\"")
                            .append(pubSubTuple.getF0()).append("\",\"acceptSubscribe\":\"")
                            .append(pubSubTuple.getF1()).append("\"");
                    if (withDetail) {
                        sBuffer = runStatusInfo.toJsonString(sBuffer.append(","));
                    }
                }
            }
            sBuffer.append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    private StringBuilder innAddOrUpdBrokerConfInfo(HttpServletRequest req,
                                                    StringBuilder sBuffer,
                                                    ProcessResult result,
                                                    boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get cluster default setting info
        ClusterSettingEntity defClusterSetting =
                defMetaDataService.getClusterDefSetting(false);
        // get brokerPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERPORT,
                false, (isAddOp ? defClusterSetting.getBrokerPort()
                        : TBaseConstants.META_VALUE_UNDEFINED), 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int brokerPort = (int) result.getRetData();
        // get brokerTlsPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERTLSPORT,
                false, (isAddOp ? defClusterSetting.getBrokerTLSPort()
                        : TBaseConstants.META_VALUE_UNDEFINED), 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int brokerTlsPort = (int) result.getRetData();
        // get brokerWebPort field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.BROKERWEBPORT,
                false, (isAddOp ? defClusterSetting.getBrokerWebPort()
                        : TBaseConstants.META_VALUE_UNDEFINED), 1, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int brokerWebPort = (int) result.getRetData();
        // get regionId field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.REGIONID,
                false, (isAddOp ? TServerConstants.BROKER_REGION_ID_DEF
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.BROKER_REGION_ID_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int regionId = (int) result.getRetData();
        // get groupId field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.GROUPID,
                false, (isAddOp ? TServerConstants.BROKER_GROUP_ID_DEF
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.BROKER_GROUP_ID_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int groupId = (int) result.getRetData();
        // get and valid TopicPropGroup info
        if (!WebParameterUtils.getTopicPropInfo(req,
                (isAddOp ? defClusterSetting.getClsDefTopicProps() : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        TopicPropGroup brokerProps = (TopicPropGroup) result.getRetData();
        // get and valid broker manage status info
        if (!getManageStatusParamValue(req, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        ManageStatus mngStatus = (ManageStatus) result.getRetData();
        // add record and process result
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        if (isAddOp) {
            // get brokerIp and brokerId field
            if (!getBrokerIpAndIdParamValue(req, sBuffer, result)) {
                WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
                return sBuffer;
            }
            Tuple2<Integer, String> brokerIdAndIpTuple =
                    (Tuple2<Integer, String>) result.getRetData();
            // set the initial status
            if (mngStatus == ManageStatus.STATUS_MANAGE_UNDEFINED) {
                mngStatus = ManageStatus.STATUS_MANAGE_APPLY;
            }
            retInfo.add(defMetaDataService.addOrUpdBrokerConfig(isAddOp, opEntity,
                    brokerIdAndIpTuple.getF0(), brokerIdAndIpTuple.getF1(), brokerPort,
                    brokerTlsPort, brokerWebPort, regionId, groupId,
                    mngStatus, brokerProps, sBuffer, result));
        } else {
            // check and get brokerId field
            if (!WebParameterUtils.getIntParamValue(req,
                    WebFieldDef.COMPSBROKERID, true, sBuffer, result)) {
                WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
                return sBuffer;
            }
            Set<Integer> brokerIdSet = (Set<Integer>) result.getRetData();
            for (Integer brokerId : brokerIdSet) {
                retInfo.add(defMetaDataService.addOrUpdBrokerConfig(isAddOp, opEntity,
                        brokerId, "", brokerPort, brokerTlsPort, brokerWebPort,
                        regionId, groupId, mngStatus, brokerProps, sBuffer, result));
            }
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Add default config to brokers in batch
     *
     * @param req
     * @return
     */
    private StringBuilder innBatchAddOrUpdBrokerConfInfo(HttpServletRequest req,
                                                         StringBuilder sBuffer,
                                                         ProcessResult result,
                                                         boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity defOpEntity = (BaseEntity) result.getRetData();
        // check and get brokerJsonSet info
        if (!getBrokerJsonSetInfo(req, isAddOp, defOpEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<Integer, BrokerConfEntity> addedRecordMap =
                (HashMap<Integer, BrokerConfEntity>) result.getRetData();
        // add record and process result
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        for (BrokerConfEntity brokerEntity : addedRecordMap.values()) {
            retInfo.add(defMetaDataService.addOrUpdBrokerConfig(
                    isAddOp, brokerEntity, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Check if the record is valid
     *
     * @param qryTopicSet
     * @param topicStatus
     * @param isInclude
     * @param topicConfEntityMap
     * @return
     */
    private boolean isValidRecord(Set<String> qryTopicSet, Boolean isInclude,
                                  TopicStatus topicStatus,
                                  Map<String, TopicDeployEntity> topicConfEntityMap) {
        if (topicConfEntityMap == null || topicConfEntityMap.isEmpty()) {
            return ((qryTopicSet == null || qryTopicSet.isEmpty())
                    && topicStatus == TopicStatus.STATUS_TOPIC_UNDEFINED);
        }
        // first search topic if match require
        if (qryTopicSet != null && !qryTopicSet.isEmpty()) {
            boolean matched = false;
            Set<String> curTopics = topicConfEntityMap.keySet();
            if (isInclude) {
                for (String topic : qryTopicSet) {
                    if (curTopics.contains(topic)) {
                        matched = true;
                        break;
                    }
                }
            } else {
                matched = true;
                for (String topic : qryTopicSet) {
                    if (curTopics.contains(topic)) {
                        matched = false;
                        break;
                    }
                }
            }
            if (!matched) {
                return false;
            }
        }
        // second check topic status if match
        if (topicStatus != TopicStatus.STATUS_TOPIC_UNDEFINED) {
            for (TopicDeployEntity topicConfEntity : topicConfEntityMap.values()) {
                if (topicConfEntity.getDeployStatus() == topicStatus) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Private method to add topic info
     *
     * @param withTopic
     * @param sBuffer
     * @param topicConfEntityMap
     * @return
     */
    private StringBuilder addTopicInfo(Boolean withTopic, StringBuilder sBuffer,
                                       Map<String, TopicDeployEntity> topicConfEntityMap) {
        if (withTopic) {
            sBuffer.append(",\"topicSet\":[");
            int topicCount = 0;
            if (topicConfEntityMap != null) {
                for (TopicDeployEntity topicEntity : topicConfEntityMap.values()) {
                    if (topicCount++ > 0) {
                        sBuffer.append(",");
                    }
                    topicEntity.toWebJsonStr(sBuffer, true, true);
                }
            }
            sBuffer.append("]");
        }
        return sBuffer;
    }

    private boolean getBrokerJsonSetInfo(HttpServletRequest req, boolean isAddOp,
                                         BaseEntity defOpEntity, StringBuilder sBuffer,
                                         ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.BROKERJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> brokerJsonArray =
                (List<Map<String, String>>) result.getRetData();
        // check and get cluster default setting info
        ClusterSettingEntity defClusterSetting =
                defMetaDataService.getClusterDefSetting(false);
        // check and get broker configure
        BrokerConfEntity itemEntity;
        HashMap<Integer, BrokerConfEntity> addedRecordMap = new HashMap<>();
        for (Map<String, String> brokerObject : brokerJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(brokerObject,
                    isAddOp, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get brokerPort field
            if (!WebParameterUtils.getIntParamValue(brokerObject, WebFieldDef.BROKERPORT,
                    false, (isAddOp ? defClusterSetting.getBrokerPort()
                            : TBaseConstants.META_VALUE_UNDEFINED), 1, sBuffer, result)) {
                return result.isSuccess();
            }
            int brokerPort = (int) result.getRetData();
            // get brokerTlsPort field
            if (!WebParameterUtils.getIntParamValue(brokerObject, WebFieldDef.BROKERTLSPORT,
                    false, (isAddOp ? defClusterSetting.getBrokerTLSPort()
                            : TBaseConstants.META_VALUE_UNDEFINED), 1, sBuffer, result)) {
                return result.isSuccess();
            }
            int brokerTlsPort = (int) result.getRetData();
            // get brokerWebPort field
            if (!WebParameterUtils.getIntParamValue(brokerObject, WebFieldDef.BROKERWEBPORT,
                    false, (isAddOp ? defClusterSetting.getBrokerWebPort()
                            : TBaseConstants.META_VALUE_UNDEFINED), 1, sBuffer, result)) {
                return result.isSuccess();
            }
            int brokerWebPort = (int) result.getRetData();
            // get regionId field
            if (!WebParameterUtils.getIntParamValue(brokerObject, WebFieldDef.REGIONID,
                    false, (isAddOp ? TServerConstants.BROKER_REGION_ID_DEF
                            : TBaseConstants.META_VALUE_UNDEFINED),
                    TServerConstants.BROKER_REGION_ID_MIN, sBuffer, result)) {
                return result.isSuccess();
            }
            int regionId = (int) result.getRetData();
            // get groupId field
            if (!WebParameterUtils.getIntParamValue(brokerObject, WebFieldDef.GROUPID,
                    false, (isAddOp ? TServerConstants.BROKER_GROUP_ID_DEF
                            : TBaseConstants.META_VALUE_UNDEFINED),
                    TServerConstants.BROKER_GROUP_ID_MIN, sBuffer, result)) {
                return result.isSuccess();
            }
            int groupId = (int) result.getRetData();
            // get and valid TopicPropGroup info
            if (!WebParameterUtils.getTopicPropInfo(brokerObject,
                    (isAddOp ? defClusterSetting.getClsDefTopicProps() : null), sBuffer, result)) {
                return result.isSuccess();
            }
            TopicPropGroup brokerProps = (TopicPropGroup) result.getRetData();
            // get and valid broker manage status info
            if (!getManageStatusParamValue(brokerObject, sBuffer, result)) {
                return result.isSuccess();
            }
            ManageStatus mngStatus = (ManageStatus) result.getRetData();
            if (isAddOp) {
                // get brokerIp and brokerId field
                if (!getBrokerIpAndIdParamValue(brokerObject, sBuffer, result)) {
                    return result.isSuccess();
                }
                Tuple2<Integer, String> brokerIdAndIpTuple =
                        (Tuple2<Integer, String>) result.getRetData();
                // set the initial status
                if (mngStatus == ManageStatus.STATUS_MANAGE_UNDEFINED) {
                    mngStatus = ManageStatus.STATUS_MANAGE_APPLY;
                }
                // buid new record
                itemEntity = new BrokerConfEntity(itemOpEntity,
                        brokerIdAndIpTuple.getF0(), brokerIdAndIpTuple.getF1());
                itemEntity.updModifyInfo(itemOpEntity.getDataVerId(), brokerPort,
                        brokerTlsPort, brokerWebPort, regionId, groupId,
                        mngStatus, brokerProps);
                addedRecordMap.put(itemEntity.getBrokerId(), itemEntity);
            } else {
                // check and get brokerId field
                if (!WebParameterUtils.getIntParamValue(brokerObject,
                        WebFieldDef.BROKERID, true, sBuffer, result)) {
                    return result.isSuccess();
                }
                Integer brokerId = (Integer) result.getRetData();
                itemEntity = new BrokerConfEntity(itemOpEntity, brokerId, "");
                itemEntity.updModifyInfo(itemOpEntity.getDataVerId(), brokerPort, brokerTlsPort,
                        brokerWebPort, regionId, groupId, mngStatus, brokerProps);
                addedRecordMap.put(itemEntity.getBrokerId(), itemEntity);
            }
        }
        // check result
        if (addedRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record in ")
                    .append(WebFieldDef.BROKERJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addedRecordMap);
        return result.isSuccess();
    }

    private StringBuilder buildRetInfo(List<BrokerProcessResult> retInfo,
                                       StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (BrokerProcessResult entry : retInfo) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"brokerId\":").append(entry.getBrokerId())
                    .append(",\"brokerIp\":\"").append(entry.getBrokerIp())
                    .append("\",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrMsg()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    private <T> boolean getBrokerIpAndIdParamValue(T paramCntr,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        // get brokerIp
        if (!WebParameterUtils.getStringParamValue(paramCntr,
                WebFieldDef.BROKERIP, true, null, sBuffer, result)) {
            return result.isSuccess();
        }
        String brokerIp = (String) result.getRetData();
        // get brokerId
        if (!WebParameterUtils.getIntParamValue(paramCntr,
                WebFieldDef.BROKERID, false, 0, 0, sBuffer, result)) {
            return result.isSuccess();
        }
        int brokerId = (int) result.getRetData();
        // valid brokerIp and brokerId
        if (brokerId <= 0) {
            try {
                brokerId = abs(AddressUtils.ipToInt(brokerIp));
            } catch (Exception e) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                        sBuffer.append("Get ").append(WebFieldDef.BROKERID.name)
                                .append(" by ").append(WebFieldDef.BROKERIP.name)
                                .append(" error !, exception is :")
                                .append(e.toString()).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
        }
        BrokerConfEntity curEntity = defMetaDataService.getBrokerConfByBrokerIp(brokerIp);
        if (curEntity != null) {
            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                    sBuffer.append("Duplicated broker configure record, query by ")
                            .append(WebFieldDef.BROKERIP.name)
                            .append(" : ").append(brokerIp).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        curEntity = defMetaDataService.getBrokerConfByBrokerId(brokerId);
        if (curEntity != null) {
            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                    sBuffer.append("Duplicated broker configure record, query by ")
                            .append(WebFieldDef.BROKERID.name).append(" : ")
                            .append(brokerId).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(new Tuple2<>(brokerId, brokerIp));
        return result.isSuccess();
    }

    private <T> boolean getManageStatusParamValue(T paramCntr,
                                                  StringBuilder sBuffer,
                                                  ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(paramCntr,
                WebFieldDef.MANAGESTATUS, false,
                null, sBuffer, result)) {
            return result.isSuccess();
        }
        String statusInfo = (String) result.getRetData();
        ManageStatus mngStatus = ManageStatus.STATUS_MANAGE_UNDEFINED;
        if (TStringUtils.isNotBlank(statusInfo)) {
            try {
                mngStatus = ManageStatus.descOf(statusInfo);
            } catch (Throwable e) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                        sBuffer.append("Illegal ").append(WebFieldDef.MANAGESTATUS.name)
                                .append(" parameter value :").append(e.getMessage()).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
        }
        result.setSuccResult(mngStatus);
        return result.isSuccess();
    }

    private <T> boolean getAcceptReadAndWriteParamValue(T paramCntr,
                                                        StringBuilder sBuffer,
                                                        ProcessResult result) {
        if (!WebParameterUtils.getBooleanParamValue(paramCntr,
                WebFieldDef.ACCEPTPUBLISH, false, null, sBuffer, result)) {
            return result.isSuccess();
        }
        Boolean publishParam = (Boolean) result.getRetData();
        if (!WebParameterUtils.getBooleanParamValue(paramCntr,
                WebFieldDef.ACCEPTSUBSCRIBE, false, null, sBuffer, result)) {
            return result.isSuccess();
        }
        Boolean subscribeParam = (Boolean) result.getRetData();
        if (publishParam == null && subscribeParam == null) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    sBuffer.append("Fields ").append(WebFieldDef.ACCEPTPUBLISH.name)
                            .append(" or ").append(WebFieldDef.ACCEPTSUBSCRIBE.name)
                            .append(" must exist at this method!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(new Tuple2<>(publishParam, subscribeParam));
        return result.isSuccess();
    }

}
