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
import java.util.Arrays;
import java.util.HashSet;
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
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;

@Deprecated
public class WebAdminFlowRuleHandler extends AbstractWebHandler {

    private static final String blankFlowCtrlRules = "[]";
    private static final List<Integer> allowedPriorityVal = Arrays.asList(1, 2, 3);
    private static final Set<String> rsvGroupNameSet =
            new HashSet<>(Arrays.asList(TServerConstants.TOKEN_DEFAULT_FLOW_CONTROL));

    public WebAdminFlowRuleHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_group_flow_control_rule",
                "adminQueryGroupFlowCtrlRule");
        // register modify method
        registerModifyWebMethod("admin_set_group_flow_control_rule",
                "adminSetGroupFlowCtrlRule");
        registerModifyWebMethod("admin_rmv_group_flow_control_rule",
                "adminDelGroupFlowCtrlRule");
        registerModifyWebMethod("admin_upd_group_flow_control_rule",
                "adminUpdGroupFlowCtrlRule");
    }

    /**
     * query group flow control rule
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryGroupFlowCtrlRule(HttpServletRequest req,
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
        final Set<String> groupNameSet = (Set<String>) result.getRetData();
        // get and valid qryPriorityId info
        if (!WebParameterUtils.getQryPriorityIdParameter(req,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int inQryPriorityId = (int) result.getRetData();
        // get flowCtrlEnable's statusId info
        if (!WebParameterUtils.getFlowCtrlStatusParamValue(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean flowCtrlEnable = (Boolean) result.getRetData();
        qryEntity.updModifyInfo(qryEntity.getDataVerId(), null,
                TBaseConstants.META_VALUE_UNDEFINED, inQryPriorityId,
                flowCtrlEnable, TBaseConstants.META_VALUE_UNDEFINED, null);
        Map<String, GroupResCtrlEntity> groupResCtrlEntityMap =
                defMetaDataService.getGroupCtrlConf(groupNameSet, qryEntity);
        // build return result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupResCtrlEntity resCtrlEntity : groupResCtrlEntityMap.values()) {
            if (resCtrlEntity == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer = resCtrlEntity.toOldVerFlowCtrlWebJsonStr(sBuffer, true);
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * add group flow control rule
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminSetGroupFlowCtrlRule(HttpServletRequest req,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        return innAddOrUpdGroupFlowCtrlRule(req, sBuffer, result, true);
    }

    /**
     * modify group flow control rule
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminUpdGroupFlowCtrlRule(HttpServletRequest req,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        return innAddOrUpdGroupFlowCtrlRule(req, sBuffer, result, false);
    }

    /**
     * delete group flow control rule
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDelGroupFlowCtrlRule(HttpServletRequest req,
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
        // add or modify records
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            retInfoList.add(defMetaDataService.insertGroupCtrlConf(opEntity, groupName,
                    TServerConstants.QRY_PRIORITY_DEF_VALUE, Boolean.FALSE,
                    0, TServerConstants.BLANK_FLOWCTRL_RULES, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * add or modify flow control rule
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    private StringBuilder innAddOrUpdGroupFlowCtrlRule(HttpServletRequest req,
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
        // get and valid qryPriorityId info
        if (!WebParameterUtils.getQryPriorityIdParameter(req,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        int qryPriorityId = (int) result.getRetData();
        // get flowCtrlEnable's statusId info
        if (!WebParameterUtils.getFlowCtrlStatusParamValue(req,
                false, null, sBuffer, result)) {
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
        // add or modify records
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            retInfoList.add(defMetaDataService.insertGroupCtrlConf(opEntity, groupName,
                    qryPriorityId, flowCtrlEnable, flowRuleCnt, flowCtrlInfo, sBuffer, result));
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
            sBuffer.append("{\"groupName\":\"").append(entry.getGroupName()).append("\"")
                    .append(",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrMsg()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

}
