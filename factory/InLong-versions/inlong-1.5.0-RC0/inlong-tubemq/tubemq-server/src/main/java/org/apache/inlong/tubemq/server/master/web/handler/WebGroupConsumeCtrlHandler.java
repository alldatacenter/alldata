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
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;

public class WebGroupConsumeCtrlHandler extends AbstractWebHandler {

    public WebGroupConsumeCtrlHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_group_csmctrl_info",
                "adminQueryGroupConsumeCtrlInfo");
        registerQueryWebMethod("admin_query_booked_csmctrl_groups",
                "adminQueryBookedConsumeCtrlGroups");

        // register modify method
        registerModifyWebMethod("admin_add_group_csmctrl_info",
                "adminAddGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_batch_add_group_csmctrl_info",
                "adminBatchAddGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_update_group_csmctrl_info",
                "adminModGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_batch_update_group_csmctrl_info",
                "adminBatchModGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_delete_group_csmctrl_info",
                "adminDelGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_batch_delete_group_csmctrl_info",
                "adminBatchDelGroupConsumeCtrlInfo");
    }

    /**
     * query booked consume-control group set
     *
     * @param req       Http Servlet Request
     * @param strBuff   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryBookedConsumeCtrlGroups(HttpServletRequest req,
            StringBuilder strBuff,
            ProcessResult result) {
        return innQueryGroupConsumeCtrlInfo(req, strBuff, result, true);
    }

    /**
     * query group consume control info
     *
     * @param req       Http Servlet Request
     * @param strBuff   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder strBuff,
            ProcessResult result) {
        return innQueryGroupConsumeCtrlInfo(req, strBuff, result, false);
    }

    /**
     * add group consume control info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, true);
    }

    /**
     * Add group consume control info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innBatchAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, true);
    }

    /**
     * modify group consume control info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminModGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, false);
    }

    /**
     * Modify group consume control info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchModGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        return innBatchAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, false);
    }

    /**
     * Delete group consume configure info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDelGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get groupName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
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
        // execute delete operation
        List<GroupProcessResult> retInfo = new ArrayList<>();
        if (topicNameSet.isEmpty()) {
            Map<String, List<GroupConsumeCtrlEntity>> groupCtrlConsumeMap =
                    defMetaDataService.getConsumeCtrlByGroupName(groupNameSet);
            for (Map.Entry<String, List<GroupConsumeCtrlEntity>> entry : groupCtrlConsumeMap.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    result.setFullInfo(true,
                            DataOpErrCode.DERR_SUCCESS.getCode(), "Ok!");
                    retInfo.add(new GroupProcessResult(entry.getKey(), "", result));
                    continue;
                }
                for (GroupConsumeCtrlEntity ctrlEntity : entry.getValue()) {
                    if (ctrlEntity != null) {
                        defMetaDataService.delConsumeCtrlConf(opEntity.getModifyUser(),
                                ctrlEntity.getGroupName(), ctrlEntity.getTopicName(), sBuffer, result);
                    }
                }
                result.setFullInfo(true,
                        DataOpErrCode.DERR_SUCCESS.getCode(), "Ok!");
                retInfo.add(new GroupProcessResult(entry.getKey(), "", result));
            }
        } else {
            for (String groupName : groupNameSet) {
                for (String topicName : topicNameSet) {
                    defMetaDataService.delConsumeCtrlConf(opEntity.getModifyUser(),
                            groupName, topicName, sBuffer, result);
                    retInfo.add(new GroupProcessResult(groupName, topicName, result));
                }
            }
        }
        buildRetInfo(retInfo, sBuffer);
        return sBuffer;
    }

    /**
     * Batch delete group consume configure info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchDelGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupCsmJsonSet data
        if (!getGroupConsumeJsonSetInfo(req, false, opEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> batchAddInfoMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // delete group consume control records
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (GroupConsumeCtrlEntity ctrlEntity : batchAddInfoMap.values()) {
            defMetaDataService.delConsumeCtrlConf(opEntity.getModifyUser(),
                    ctrlEntity.getGroupName(), ctrlEntity.getTopicName(), sBuffer, result);
            retInfo.add(new GroupProcessResult(ctrlEntity.getGroupName(),
                    ctrlEntity.getTopicName(), result));
        }
        buildRetInfo(retInfo, sBuffer);
        return sBuffer;
    }

    /**
     * query group consume control info
     *
     * @param req       Http Servlet Request
     * @param strBuff   string buffer
     * @param result    process result
     * @param onlyRetGroup  only return group name
     * @return    process result
     */
    private StringBuilder innQueryGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder strBuff,
            ProcessResult result,
            boolean onlyRetGroup) {
        // build query entity
        GroupConsumeCtrlEntity qryEntity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, strBuff, result)) {
            WebParameterUtils.buildFailResult(strBuff, result.getErrMsg());
            return strBuff;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, strBuff, result)) {
            WebParameterUtils.buildFailResult(strBuff, result.getErrMsg());
            return strBuff;
        }
        final Set<String> groupSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, strBuff, result)) {
            WebParameterUtils.buildFailResult(strBuff, result.getErrMsg());
            return strBuff;
        }
        final Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get consumeEnable info
        if (!WebParameterUtils.getEnableStatusValue(req,
                WebFieldDef.CONSUMEENABLE, false, null, strBuff, result)) {
            WebParameterUtils.buildFailResult(strBuff, result.getErrMsg());
            return strBuff;
        }
        final EnableStatus consumeEnable = (EnableStatus) result.getRetData();
        // get filterEnable info
        if (!WebParameterUtils.getEnableStatusValue(req,
                WebFieldDef.FILTERENABLE, false, null, strBuff, result)) {
            WebParameterUtils.buildFailResult(strBuff, result.getErrMsg());
            return strBuff;
        }
        EnableStatus filterEnable = (EnableStatus) result.getRetData();
        // get filterConds info
        if (!WebParameterUtils.getFilterCondSet(req, false, true, strBuff, result)) {
            WebParameterUtils.buildFailResult(strBuff, result.getErrMsg());
            return strBuff;
        }
        Set<String> filterCondSet = (Set<String>) result.getRetData();
        // query matched records
        qryEntity.updModifyInfo(qryEntity.getDataVerId(),
                consumeEnable, null, filterEnable, null);
        Map<String, List<GroupConsumeCtrlEntity>> qryResultMap =
                defMetaDataService.getGroupConsumeCtrlConf(groupSet, topicNameSet, qryEntity);
        // build return result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(strBuff);
        if (onlyRetGroup) {
            for (String groupName : qryResultMap.keySet()) {
                if (groupName == null) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    strBuff.append(",");
                }
                strBuff.append("\"").append(groupName).append("\"");
            }
        } else {
            for (List<GroupConsumeCtrlEntity> consumeCtrlEntityList : qryResultMap.values()) {
                if (consumeCtrlEntityList == null || consumeCtrlEntityList.isEmpty()) {
                    continue;
                }
                for (GroupConsumeCtrlEntity entity : consumeCtrlEntityList) {
                    if (entity == null
                            || !WebParameterUtils.isFilterSetFullIncluded(
                                    filterCondSet, entity.getFilterCondStr())) {
                        continue;
                    }
                    if (totalCnt++ > 0) {
                        strBuff.append(",");
                    }
                    entity.toWebJsonStr(strBuff, true, true);
                }
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(strBuff, totalCnt);
        return strBuff;
    }

    private StringBuilder innAddOrUpdGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opInfoEntity = (BaseEntity) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                defMetaDataService, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get groupName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        final Set<String> groupNameSet = (Set<String>) result.getRetData();
        // get consumeEnable info
        if (!WebParameterUtils.getEnableStatusValue(req,
                WebFieldDef.CONSUMEENABLE, false,
                (isAddOp ? EnableStatus.STATUS_ENABLE : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        EnableStatus consumeEnable = (EnableStatus) result.getRetData();
        // get disableCsmRsn info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.DSBCSMREASON, false,
                (isAddOp ? "" : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        String disableRsn = (String) result.getRetData();
        // get filterEnable info
        if (!WebParameterUtils.getEnableStatusValue(req,
                WebFieldDef.FILTERENABLE, false,
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
        // add group resource record
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                retInfo.add(defMetaDataService.addOrUpdConsumeCtrlInfo(isAddOp,
                        opInfoEntity, groupName, topicName, consumeEnable, disableRsn,
                        filterEnable, filterCondStr, sBuffer, result));
            }
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private StringBuilder innBatchAddOrUpdGroupConsumeCtrlInfo(HttpServletRequest req,
            StringBuilder sBuffer,
            ProcessResult result,
            boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity defOpEntity = (BaseEntity) result.getRetData();
        // check and get groupCsmJsonSet data
        if (!getGroupConsumeJsonSetInfo(req, isAddOp, defOpEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> batchAddInfoMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add group resource record
        GroupProcessResult addResult;
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (GroupConsumeCtrlEntity ctrlEntity : batchAddInfoMap.values()) {
            retInfo.add(defMetaDataService.addOrUpdConsumeCtrlInfo(
                    isAddOp, ctrlEntity, sBuffer, result));
        }
        buildRetInfo(retInfo, sBuffer);
        return sBuffer;
    }

    private StringBuilder buildRetInfo(List<GroupProcessResult> retInfo,
            StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupProcessResult result : retInfo) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"groupName\":\"").append(result.getGroupName()).append("\"")
                    .append(",\"topicName\":\"").append(result.getTopicName()).append("\"")
                    .append(",\"success\":").append(result.isSuccess())
                    .append(",\"errCode\":").append(result.getErrCode())
                    .append(",\"errInfo\":\"").append(result.getErrMsg()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    private boolean getGroupConsumeJsonSetInfo(HttpServletRequest req, boolean isAddOp,
            BaseEntity defOpEntity, StringBuilder sBuffer,
            ProcessResult result) {
        // get groupCsmJsonSet field info
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPCSMJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> filterJsonArray =
                (List<Map<String, String>>) result.getRetData();
        // parse groupCsmJsonSet field info
        GroupConsumeCtrlEntity itemConf;
        Map<String, GroupConsumeCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                defMetaDataService.getDeployedTopicSet();
        for (Map<String, String> itemsMap : filterJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemsMap,
                    isAddOp, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            if (!WebParameterUtils.getStringParamValue(itemsMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            final String groupName = (String) result.getRetData();
            if (!WebParameterUtils.getStringParamValue(itemsMap,
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
            // get consumeEnable info
            if (!WebParameterUtils.getEnableStatusValue(itemsMap,
                    WebFieldDef.CONSUMEENABLE, false,
                    (isAddOp ? EnableStatus.STATUS_ENABLE : null), sBuffer, result)) {
                return result.isSuccess();
            }
            final EnableStatus consumeEnable = (EnableStatus) result.getRetData();
            // get disableCsmRsn info
            if (!WebParameterUtils.getStringParamValue(itemsMap,
                    WebFieldDef.DSBCSMREASON, false, (isAddOp ? "" : null), sBuffer, result)) {
                return result.isSuccess();
            }
            final String disableRsn = (String) result.getRetData();
            // get filterEnable info
            if (!WebParameterUtils.getEnableStatusValue(itemsMap,
                    WebFieldDef.FILTERENABLE, false,
                    (isAddOp ? EnableStatus.STATUS_DISABLE : null), sBuffer, result)) {
                return result.isSuccess();
            }
            EnableStatus filterEnable = (EnableStatus) result.getRetData();
            // get filterConds info
            if (!WebParameterUtils.getFilterCondString(
                    itemsMap, false, isAddOp, sBuffer, result)) {
                return result.isSuccess();
            }
            String filterCondStr = (String) result.getRetData();
            // add record object
            itemConf = new GroupConsumeCtrlEntity(itemOpEntity, groupName, topicName);
            itemConf.updModifyInfo(itemOpEntity.getDataVerId(),
                    consumeEnable, disableRsn, filterEnable, filterCondStr);
            addRecordMap.put(itemConf.getRecordKey(), itemConf);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record in ")
                    .append(WebFieldDef.GROUPCSMJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }
}
