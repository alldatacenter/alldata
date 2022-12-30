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

package org.apache.inlong.tubemq.server.master.web.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;

public class WebGroupResCtrlHandler extends AbstractWebHandler {

    public WebGroupResCtrlHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_group_resctrl_info",
                "adminQueryGroupResCtrlConf");
        // register modify method
        registerModifyWebMethod("admin_add_group_resctrl_info",
                "adminAddGroupResCtrlConf");
        registerModifyWebMethod("admin_batch_add_group_resctrl_info",
                "adminBatchAddGroupResCtrlConf");
        registerModifyWebMethod("admin_update_group_resctrl_info",
                "adminModGroupResCtrlConf");
        registerModifyWebMethod("admin_batch_update_group_resctrl_info",
                "adminBatchUpdGroupResCtrlConf");
        registerModifyWebMethod("admin_delete_group_resctrl_info",
                "adminDelGroupResCtrlConf");
    }

    /**
     * query group resource control info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryGroupResCtrlConf(HttpServletRequest req,
                                                    StringBuilder sBuffer,
                                                    ProcessResult result) {
        // build query entity
        GroupResCtrlEntity qryEntity = new GroupResCtrlEntity();
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
        final Set<String> inGroupSet = (Set<String>) result.getRetData();
        // get resCheckStatus info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.RESCHECKENABLE, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean resCheckEnable = (Boolean) result.getRetData();
        // get and valid qryPriorityId info
        if (!WebParameterUtils.getQryPriorityIdParameter(req,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int inQryPriorityId = (int) result.getRetData();
        // get flowCtrlEnable info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.FLOWCTRLENABLE, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean flowCtrlEnable = (Boolean) result.getRetData();
        qryEntity.updModifyInfo(qryEntity.getDataVerId(),
                resCheckEnable, TBaseConstants.META_VALUE_UNDEFINED, inQryPriorityId,
                flowCtrlEnable, TBaseConstants.META_VALUE_UNDEFINED, null);
        Map<String, GroupResCtrlEntity> groupResCtrlEntityMap =
                defMetaDataService.getGroupCtrlConf(inGroupSet, qryEntity);
        // build return result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupResCtrlEntity entity : groupResCtrlEntityMap.values()) {
            if (entity == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer = entity.toWebJsonStr(sBuffer, true, true);
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * add group resource control info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddGroupResCtrlConf(HttpServletRequest req,
                                                  StringBuilder sBuffer,
                                                  ProcessResult result) {
        return innAddOrUpdGroupResCtrlConf(req, sBuffer, result, true);
    }

    /**
     * Add group resource control info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddGroupResCtrlConf(HttpServletRequest req,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        return innBatchAddOrUpdGroupResCtrlConf(req, sBuffer, result, true);
    }

    /**
     * update group resource control info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminModGroupResCtrlConf(HttpServletRequest req,
                                                  StringBuilder sBuffer,
                                                  ProcessResult result) {
        return innAddOrUpdGroupResCtrlConf(req, sBuffer, result, false);
    }

    /**
     * update group resource control info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchUpdGroupResCtrlConf(HttpServletRequest req,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        return innBatchAddOrUpdGroupResCtrlConf(req, sBuffer, result, false);
    }

    /**
     * delete group resource control rule
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDelGroupResCtrlConf(HttpServletRequest req,
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
        Set<String> batchGroupNames = (Set<String>) result.getRetData();
        // delete group resource record
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (String groupName : batchGroupNames) {
            retInfo.add(defMetaDataService.delGroupResCtrlConf(
                    opEntity.getModifyUser(), groupName, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private StringBuilder innAddOrUpdGroupResCtrlConf(HttpServletRequest req,
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
        final Set<String> batchGroupNames = (Set<String>) result.getRetData();
        // get resCheckStatus info
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.RESCHECKENABLE,
                false, (isAddOp ? false : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean resCheckEnable = (Boolean) result.getRetData();
        // get and valid allowedBrokerClientRate info
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.ALWDBCRATE,
                false, (isAddOp ? TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int allowedBClientRate = (int) result.getRetData();
        // get def cluster setting info
        ClusterSettingEntity defClusterSetting =
                defMetaDataService.getClusterDefSetting(false);
        // get and valid qryPriorityId info
        if (!WebParameterUtils.getQryPriorityIdParameter(req,
                false, (isAddOp ? defClusterSetting.getQryPriorityId()
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int qryPriorityId = (int) result.getRetData();
        // get flowCtrlEnable info
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.FLOWCTRLENABLE,
                false, (isAddOp ? false : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean flowCtrlEnable = (Boolean) result.getRetData();
        // get and flow control rule info
        int flowRuleCnt = WebParameterUtils.getAndCheckFlowRules(req,
                (isAddOp ? TServerConstants.BLANK_FLOWCTRL_RULES : null), sBuffer, result);
        if (!result.isSuccess()) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        String flowCtrlInfo = (String) result.getRetData();
        // add group resource record
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (String groupName : batchGroupNames) {
            retInfo.add(defMetaDataService.addOrUpdGroupCtrlConf(isAddOp, opEntity, groupName,
                    resCheckEnable, allowedBClientRate, qryPriorityId,
                    flowCtrlEnable, flowRuleCnt, flowCtrlInfo, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private StringBuilder innBatchAddOrUpdGroupResCtrlConf(HttpServletRequest req,
                                                           StringBuilder sBuffer,
                                                           ProcessResult result,
                                                           boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity defOpEntity = (BaseEntity) result.getRetData();
        // get group resource control json record
        if (!getGroupResCtrlJsonSetInfo(req, isAddOp, defOpEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupResCtrlEntity> addRecordMap =
                (Map<String, GroupResCtrlEntity>) result.getRetData();
        // add or update group resource record
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (GroupResCtrlEntity newResCtrlEntity : addRecordMap.values()) {
            retInfo.add(defMetaDataService.addOrUpdGroupCtrlConf(
                    isAddOp, newResCtrlEntity, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private boolean getGroupResCtrlJsonSetInfo(HttpServletRequest req, boolean isAddOp,
                                               BaseEntity defOpEntity, StringBuilder sBuffer,
                                               ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPRESCTRLSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> ctrlJsonArray =
                (List<Map<String, String>>) result.getRetData();
        // get default qryPriorityId
        ClusterSettingEntity defClusterSetting =
                defMetaDataService.getClusterDefSetting(false);
        int defQryPriorityId = defClusterSetting.getQryPriorityId();
        // check and get topic control configure
        GroupResCtrlEntity itemEntity;
        Map<String, GroupResCtrlEntity> addRecordMap = new HashMap<>();
        for (Map<String, String> itemValueMap : ctrlJsonArray) {
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
            // get resCheckStatus info
            if (!WebParameterUtils.getBooleanParamValue(itemValueMap, WebFieldDef.RESCHECKENABLE,
                    false, (isAddOp ? false : null), sBuffer, result)) {
                return result.isSuccess();
            }
            final Boolean resCheckEnable = (Boolean) result.getRetData();
            // get and valid allowedBrokerClientRate info
            if (!WebParameterUtils.getIntParamValue(itemValueMap, WebFieldDef.ALWDBCRATE,
                    false, (isAddOp ? TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN
                            : TBaseConstants.META_VALUE_UNDEFINED),
                    TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
                return result.isSuccess();
            }
            final int allowedBClientRate = (int) result.getRetData();
            // get def cluster setting info
            // get and valid qryPriorityId info
            if (!WebParameterUtils.getQryPriorityIdParameter(itemValueMap,
                    false, (isAddOp ? defClusterSetting.getQryPriorityId()
                            : TBaseConstants.META_VALUE_UNDEFINED),
                    TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
                return result.isSuccess();
            }
            final int qryPriorityId = (int) result.getRetData();
            // get flowCtrlEnable info
            if (!WebParameterUtils.getBooleanParamValue(itemValueMap,
                    WebFieldDef.FLOWCTRLENABLE, false,
                    (isAddOp ? false : null), sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean flowCtrlEnable = (Boolean) result.getRetData();
            // get and flow control rule info
            int flowRuleCnt = WebParameterUtils.getAndCheckFlowRules(itemValueMap,
                    (isAddOp ? TServerConstants.BLANK_FLOWCTRL_RULES : null), sBuffer, result);
            if (!result.isSuccess()) {
                return result.isSuccess();
            }
            String flowCtrlInfo = (String) result.getRetData();
            itemEntity =
                    new GroupResCtrlEntity(itemOpEntity, groupName);
            itemEntity.updModifyInfo(itemEntity.getDataVerId(),
                    resCheckEnable, allowedBClientRate, qryPriorityId,
                    flowCtrlEnable, flowRuleCnt, flowCtrlInfo);
            addRecordMap.put(itemEntity.getGroupName(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.GROUPRESCTRLSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private StringBuilder buildRetInfo(List<GroupProcessResult> retInfo,
                                       StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupProcessResult entry : retInfo) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"groupName\":\"").append(entry.getGroupName()).append("\"")
                    .append(",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrMsg()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

}
