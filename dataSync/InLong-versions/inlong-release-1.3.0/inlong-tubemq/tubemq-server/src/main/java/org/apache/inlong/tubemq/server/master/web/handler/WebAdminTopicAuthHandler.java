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
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;

@Deprecated
public class WebAdminTopicAuthHandler extends AbstractWebHandler {

    public WebAdminTopicAuthHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_topic_authorize_control",
                "adminQueryTopicAuthControl");
        // register modify method
        registerModifyWebMethod("admin_set_topic_authorize_control",
                "adminEnableDisableTopicAuthControl");
        registerModifyWebMethod("admin_delete_topic_authorize_control",
                "adminDeleteTopicAuthControl");
        registerModifyWebMethod("admin_bath_add_topic_authorize_control",
                "adminBatchAddTopicAuthControl");
    }

    /**
     * Query topic authorization control
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryTopicAuthControl(HttpServletRequest req,
                                                    StringBuilder sBuffer,
                                                    ProcessResult result) {
        TopicCtrlEntity qryEntity = new TopicCtrlEntity();
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
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // query matched records
        Map<String, TopicCtrlEntity> topicCtrlMap =
                defMetaDataService.getTopicCtrlConf(topicNameSet, qryEntity);
        // build query result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (TopicCtrlEntity entity : topicCtrlMap.values()) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"topicName\":\"").append(entity.getTopicName())
                    .append("\",\"isEnable\":").append(entity.isAuthCtrlEnable())
                    .append(",\"createUser\":\"").append(entity.getCreateUser())
                    .append("\",\"createDate\":\"").append(entity.getCreateDateStr())
                    .append("\",\"authConsumeGroup\":[");
            List<GroupConsumeCtrlEntity> groupEntity =
                    defMetaDataService.getConsumeCtrlByTopic(entity.getTopicName());
            int j = 0;
            if (!groupEntity.isEmpty()) {
                for (GroupConsumeCtrlEntity itemEntity : groupEntity) {
                    if (j++ > 0) {
                        sBuffer.append(",");
                    }
                    sBuffer.append("{\"topicName\":\"").append(itemEntity.getTopicName())
                            .append("\",\"groupName\":\"")
                            .append(itemEntity.getGroupName())
                            .append("\",\"createUser\":\"")
                            .append(itemEntity.getCreateUser())
                            .append("\",\"createDate\":\"")
                            .append(itemEntity.getCreateDateStr())
                            .append("\"}");
                }
            }
            sBuffer.append("],\"groupCount\":").append(j).append(",\"authFilterCondSet\":[");
            int y = 0;
            for (GroupConsumeCtrlEntity condEntity : groupEntity) {
                if (y++ > 0) {
                    sBuffer.append(",");
                }
                int condStatusId = condEntity.isEnableFilterConsume() ? 2 : 0;
                sBuffer.append("{\"topicName\":\"").append(condEntity.getTopicName())
                        .append("\",\"groupName\":\"").append(condEntity.getGroupName())
                        .append("\",\"condStatus\":").append(condStatusId);
                if (condEntity.getFilterCondStr().length() <= 2) {
                    sBuffer.append(",\"filterConds\":\"\"");
                } else {
                    sBuffer.append(",\"filterConds\":\"")
                            .append(condEntity.getFilterCondStr())
                            .append("\"");
                }
                sBuffer.append(",\"createUser\":\"").append(condEntity.getCreateUser())
                        .append("\",\"createDate\":\"").append(condEntity.getCreateDateStr())
                        .append("\"}");
            }
            sBuffer.append("],\"filterCount\":").append(y).append("}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Enable or disable topic authorization control
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminEnableDisableTopicAuthControl(HttpServletRequest req,
                                                            StringBuilder sBuffer,
                                                            ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
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
        // get authCtrlStatus info
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.ISENABLE,
                false, false, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Boolean enableTopicAuth = (Boolean) result.getRetData();
        // add or update records
        List<TopicProcessResult> retInfo = new ArrayList<>();
        for (String topicName : topicNameSet) {
            retInfo.add(defMetaDataService.insertTopicCtrlConf(opEntity,
                    topicName, enableTopicAuth, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Add topic authorization control in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddTopicAuthControl(HttpServletRequest req,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get topicJsonSet record map
        if (!getTopicCtrlJsonSetInfo(req, opEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrMsg());
            return sBuffer;
        }
        Map<String, TopicCtrlEntity> addRecordMap =
                (Map<String, TopicCtrlEntity>) result.getRetData();
        List<TopicProcessResult> retInfo = new ArrayList<>();
        for (TopicCtrlEntity topicCtrlInfo : addRecordMap.values()) {
            retInfo.add(defMetaDataService.insertTopicCtrlConf(topicCtrlInfo, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    /**
     * Delete topic authorization control
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteTopicAuthControl(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
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
        // delete records
        List<TopicProcessResult> retInfo = new ArrayList<>();
        for (String topicName : topicNameSet) {
            retInfo.add(defMetaDataService.insertTopicCtrlConf(opEntity,
                    topicName, Boolean.FALSE, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private boolean getTopicCtrlJsonSetInfo(HttpServletRequest req, BaseEntity defOpEntity,
                                            StringBuilder sBuffer, ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.TOPICJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> deployJsonArray =
                (List<Map<String, String>>) result.getRetData();
        TopicCtrlEntity itemConf;
        Map<String, TopicCtrlEntity> addRecordMap = new HashMap<>();
        // check and get topic deployment configure
        for (Map<String, String> confMap : deployJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(confMap,
                    true, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get topicName configure info
            if (!WebParameterUtils.getStringParamValue(confMap,
                    WebFieldDef.TOPICNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String topicName = (String) result.getRetData();
            // get authCtrlStatus info
            if (!WebParameterUtils.getBooleanParamValue(confMap, WebFieldDef.ISENABLE,
                    false, false, sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean enableTopicAuth = (Boolean) result.getRetData();
            itemConf = new TopicCtrlEntity(itemOpEntity, topicName);
            itemConf.updModifyInfo(itemOpEntity.getDataVerId(),
                    TBaseConstants.META_VALUE_UNDEFINED,
                    TBaseConstants.META_VALUE_UNDEFINED, enableTopicAuth);
            addRecordMap.put(itemConf.getTopicName(), itemConf);
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

    private StringBuilder buildRetInfo(List<TopicProcessResult> retInfo,
                                       StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (TopicProcessResult entry : retInfo) {
            if (entry == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"topicName\":\"").append(entry.getTopicName()).append("\"")
                    .append(",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrMsg()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

}
