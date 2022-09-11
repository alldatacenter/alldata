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

package org.apache.inlong.tubemq.server.common.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlItem;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlRuleHandler;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.TStatusConstants;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbBrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;

public class WebParameterUtils {

    private static final List<String> allowedDelUnits = Arrays.asList("s", "m", "h");
    private static final List<Integer> allowedPriorityVal = Arrays.asList(1, 2, 3);

    public static StringBuilder buildFailResult(StringBuilder sBuffer, String errMsg) {
        return sBuffer.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                .append(errMsg).append("\"}");
    }

    public static StringBuilder buildFailResultWithBlankData(int errcode, String errMsg,
                                                             StringBuilder sBuffer) {
        return sBuffer.append("{\"result\":false,\"errCode\":").append(errcode)
                .append(",\"errMsg\":\"").append(errMsg).append("\",\"data\":[]}");
    }

    public static StringBuilder buildSuccessResult(StringBuilder sBuffer) {
        return sBuffer.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\",\"data\":[]}");
    }

    public static StringBuilder buildSuccessResult(StringBuilder sBuffer, String appendInfo) {
        return sBuffer.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"")
                .append(appendInfo).append("\",\"data\":[]}");
    }

    public static StringBuilder buildSuccessWithDataRetBegin(StringBuilder sBuffer) {
        return sBuffer.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\",\"data\":[");
    }

    public static StringBuilder buildSuccessWithDataRetEnd(
            StringBuilder sBuffer, int totalCnt) {
        return sBuffer.append("],\"count\":").append(totalCnt).append("}");
    }

    /**
     * Parse the parameter value required for add update and delete
     *
     * @param paramCntr   parameter container object
     * @param isAdd       if add operation
     * @param defOpEntity default value set,
     *                   if not null, it must fill required field values
     * @param sBuffer     string buffer
     * @param result      process result of parameter value
     * @return the process result
     */
    public static <T> boolean getAUDBaseInfo(T paramCntr, boolean isAdd,
                                             BaseEntity defOpEntity,
                                             StringBuilder sBuffer,
                                             ProcessResult result) {
        // check and get data version id
        if (!WebParameterUtils.getLongParamValue(paramCntr, WebFieldDef.DATAVERSIONID,
                false, (defOpEntity == null
                        ? TBaseConstants.META_VALUE_UNDEFINED : defOpEntity.getDataVerId()),
                sBuffer, result)) {
            return result.isSuccess();
        }
        final long dataVerId = (long) result.getRetData();
        // check and get createUser or modifyUser
        String createUsr = null;
        Date createDate = new Date();
        if (isAdd) {
            // check create user field
            if (!WebParameterUtils.getStringParamValue(paramCntr, WebFieldDef.CREATEUSER,
                    defOpEntity == null,
                    (defOpEntity == null ? createUsr : defOpEntity.getCreateUser()),
                    sBuffer, result)) {
                return result.isSuccess();
            }
            createUsr = (String) result.getRetData();
            // check and get create date
            if (!WebParameterUtils.getDateParameter(paramCntr, WebFieldDef.CREATEDATE, false,
                    ((defOpEntity == null || defOpEntity.getCreateDate() == null)
                            ? createDate : defOpEntity.getCreateDate()),
                    sBuffer, result)) {
                return result.isSuccess();
            }
            createDate = (Date) result.getRetData();
        }
        // check modify user field
        if (!WebParameterUtils.getStringParamValue(paramCntr, WebFieldDef.MODIFYUSER,
                (defOpEntity == null && !isAdd),
                (defOpEntity == null ? createUsr : defOpEntity.getModifyUser()),
                sBuffer, result)) {
            return result.isSuccess();
        }
        String modifyUser = (String) result.getRetData();
        // check and get modify date
        if (!WebParameterUtils.getDateParameter(paramCntr, WebFieldDef.MODIFYDATE, false,
                ((defOpEntity == null || defOpEntity.getModifyDate() == null)
                        ? createDate : defOpEntity.getModifyDate()),
                sBuffer, result)) {
            return result.isSuccess();
        }
        Date modifyDate = (Date) result.getRetData();
        result.setSuccResult(new BaseEntity(dataVerId,
                createUsr, createDate, modifyUser, modifyDate));
        return result.isSuccess();
    }

    /**
     * Parse the parameter value required for query
     *
     * @param req        Http Servlet Request
     * @param qryEntity  query entity
     * @param sBuffer    string buffer
     * @param result     process result
     * @return process result
     */
    public static boolean getQueriedOperateInfo(HttpServletRequest req, BaseEntity qryEntity,
                                                StringBuilder sBuffer, ProcessResult result) {
        // check and get data version id
        if (!WebParameterUtils.getLongParamValue(req, WebFieldDef.DATAVERSIONID,
                false, TBaseConstants.META_VALUE_UNDEFINED, sBuffer, result)) {
            return result.isSuccess();
        }
        long dataVerId = (long) result.getRetData();
        // check createUser user field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.CREATEUSER, false, null, sBuffer, result)) {
            return result.isSuccess();
        }
        String createUser = (String) result.getRetData();
        // check modify user field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.MODIFYUSER, false, null, sBuffer, result)) {
            return result.isSuccess();
        }
        String modifyUser = (String) result.getRetData();
        // set query keys
        qryEntity.updQueryKeyInfo(dataVerId, createUser, modifyUser);
        result.setSuccResult(qryEntity);
        return result.isSuccess();
    }

    /**
     * Parse the parameter value required for query
     *
     * @param paramCntr   parameter container
     * @param required    whether required
     * @param defValue    default value
     * @param minValue    min value
     * @param sBuffer     string buffer
     * @param result      process result
     * @return process result
     */
    // get QryPriorityId parameter value
    public static <T> boolean getQryPriorityIdParameter(T paramCntr, boolean required,
                                                        int defValue, int minValue,
                                                        StringBuilder sBuffer,
                                                        ProcessResult result) {
        if (!getIntParamValue(paramCntr, WebFieldDef.QRYPRIORITYID,
                required, defValue, minValue, sBuffer, result)) {
            return result.isSuccess();
        }
        int qryPriorityId = (int) result.getRetData();
        if (qryPriorityId == defValue) {
            return result.isSuccess();
        }
        if (qryPriorityId > 303 || qryPriorityId < 101) {
            result.setFailResult(sBuffer.append("Illegal value in ")
                    .append(WebFieldDef.QRYPRIORITYID.name)
                    .append(" parameter: ").append(WebFieldDef.QRYPRIORITYID.name)
                    .append(" value must be greater than or equal")
                    .append(" to 101 and less than or equal to 303!").toString());
            sBuffer.delete(0, sBuffer.length());
            return false;
        }
        if (!allowedPriorityVal.contains(qryPriorityId % 100)) {
            result.setFailResult(sBuffer.append("Illegal value in ")
                    .append(WebFieldDef.QRYPRIORITYID.name).append(" parameter: the units of ")
                    .append(WebFieldDef.QRYPRIORITYID.name).append(" must in ")
                    .append(allowedPriorityVal).toString());
            sBuffer.delete(0, sBuffer.length());
            return false;
        }
        if (!allowedPriorityVal.contains(qryPriorityId / 100)) {
            result.setFailResult(sBuffer.append("Illegal value in ")
                    .append(WebFieldDef.QRYPRIORITYID.name)
                    .append(" parameter: the hundreds of ").append(WebFieldDef.QRYPRIORITYID.name)
                    .append(" must in ").append(allowedPriorityVal).toString());
            sBuffer.delete(0, sBuffer.length());
            return false;
        }
        result.setSuccResult(qryPriorityId);
        return result.isSuccess();
    }

    /**
     * Decode the deletePolicy parameter value from an object value
     * the value must like {method},{digital}[s|m|h]
     *
     * @param paramCntr   parameter container object
     * @param required   a boolean value represent whether the parameter is must required
     * @param defValue   a default value returned if failed to parse value from the given object
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return the process result
     */
    public static <T> boolean getDeletePolicyParameter(T paramCntr, boolean required,
                                                       String defValue, StringBuilder sBuffer,
                                                       ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(paramCntr,
                WebFieldDef.DELETEPOLICY, required, defValue, sBuffer, result)) {
            return result.isSuccess();
        }
        String delPolicy = (String) result.getRetData();
        if (TStringUtils.isBlank(delPolicy)) {
            return result.isSuccess();
        }
        // check value format
        String[] tmpStrs = delPolicy.split(",");
        if (tmpStrs.length != 2) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    sBuffer.append("Value must include one and only one comma character,")
                            .append(" the format of ").append(WebFieldDef.DELETEPOLICY.name())
                            .append(" must like {method},{digital}[m|s|h]").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (TStringUtils.isBlank(tmpStrs[0])) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    sBuffer.append("Method value must not be blank, the format of ")
                            .append(WebFieldDef.DELETEPOLICY.name())
                            .append(" must like {method},{digital}[m|s|h]").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (!"delete".equalsIgnoreCase(tmpStrs[0].trim())) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    sBuffer.append("Field ").append(WebFieldDef.DELETEPOLICY.name())
                            .append(" only support delete method now!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        String validValStr = tmpStrs[1];
        String timeUnit = validValStr.substring(validValStr.length() - 1).toLowerCase();
        if (Character.isLetter(timeUnit.charAt(0))) {
            if (!allowedDelUnits.contains(timeUnit)) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                        sBuffer.append("Field ").append(WebFieldDef.DELETEPOLICY.name())
                                .append(" only support [s|m|h] unit!").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
        }
        long validDuration = 0;
        try {
            if (timeUnit.endsWith("s")) {
                validDuration = Long.parseLong(validValStr.substring(0, validValStr.length() - 1)) * 1000;
            } else if (timeUnit.endsWith("m")) {
                validDuration = Long.parseLong(validValStr.substring(0, validValStr.length() - 1)) * 60000;
            } else if (timeUnit.endsWith("h")) {
                validDuration = Long.parseLong(validValStr.substring(0, validValStr.length() - 1)) * 3600000;
            } else {
                validDuration = Long.parseLong(validValStr) * 3600000;
            }
        } catch (Throwable e) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    sBuffer.append("The value of field ")
                            .append(WebFieldDef.DELETEPOLICY.name())
                            .append("'s valid duration must digits!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (validDuration <= 0 || validDuration > DataStoreUtils.MAX_FILE_VALID_DURATION) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    sBuffer.append("The value of field ")
                            .append(WebFieldDef.DELETEPOLICY.name())
                            .append(" must be greater than 0 and  less than or equal to")
                            .append(DataStoreUtils.MAX_FILE_VALID_DURATION)
                            .append(" seconds!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (Character.isLetter(timeUnit.charAt(0))) {
            result.setSuccResult(sBuffer.append("delete,")
                    .append(validValStr.substring(0, validValStr.length() - 1))
                    .append(timeUnit).toString());
        } else {
            result.setSuccResult(sBuffer.append("delete,")
                    .append(validValStr).append("h").toString());
        }
        sBuffer.delete(0, sBuffer.length());
        return result.isSuccess();
    }

    /**
     * get topic status parameter value
     *
     * @param req         Http Servlet Request
     * @param isRequired  whether required
     * @param defVal      a default value returned if failed to parse value from the given object
     * @param sBuffer     string buffer
     * @param result      process result of parameter value
     * @return the process result
     */
    public static boolean getTopicStatusParamValue(HttpServletRequest req,
                                                   boolean isRequired,
                                                   TopicStatus defVal,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        // get topicStatusId field
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.TOPICSTATUSID,
                isRequired, defVal.getCode(), TopicStatus.STATUS_TOPIC_UNDEFINED.getCode(),
                sBuffer, result)) {
            return result.isSuccess();
        }
        int paramValue = (int) result.getRetData();
        try {
            TopicStatus topicStatus = TopicStatus.valueOf(paramValue);
            result.setSuccResult(topicStatus);
        } catch (Throwable e) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    sBuffer.append("The value of field ")
                            .append(WebFieldDef.TOPICSTATUSID.name)
                            .append(" invalid:").append(e.getMessage()).toString());
            sBuffer.delete(0, sBuffer.length());
        }
        return result.isSuccess();
    }

    /**
     * Compare the configured ports for conflicts
     *
     * @param brokerPort     broker port
     * @param brokerTlsPort  broker tls port
     * @param brokerWebPort  broker web port
     * @param strBuff        string buffer
     * @param result     check result of parameter value
     * @return   true for valid, false for invalid
     */
    public static boolean isValidPortsSet(int brokerPort, int brokerTlsPort,
                                          int brokerWebPort, StringBuilder strBuff,
                                          ProcessResult result) {
        if (brokerPort == brokerWebPort || brokerTlsPort == brokerWebPort) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    strBuff.append(DataOpErrCode.DERR_CONFLICT_VALUE.getDescription())
                            .append(", the value of ")
                            .append(WebFieldDef.BROKERPORT.name).append(" or ")
                            .append(WebFieldDef.BROKERTLSPORT.name)
                            .append(" cannot be the same as the value of")
                            .append(WebFieldDef.BROKERWEBPORT.name).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        return true;
    }

    /**
     * Parse the parameter value for TopicPropGroup class
     *
     * @param paramCntr   parameter container object
     * @param defVal     default value
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static <T> boolean getTopicPropInfo(T paramCntr, TopicPropGroup defVal,
                                               StringBuilder sBuffer, ProcessResult result) {
        TopicPropGroup newConf = new TopicPropGroup();
        // get numTopicStores parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.NUMTOPICSTORES, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getNumTopicStores()),
                TServerConstants.TOPIC_STOREBLOCK_NUM_MIN, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setNumTopicStores((int) result.getRetData());
        // get numPartitions parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.NUMPARTITIONS, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getNumPartitions()),
                TServerConstants.TOPIC_PARTITION_NUM_MIN, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setNumPartitions((int) result.getRetData());
        // get unflushThreshold parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.UNFLUSHTHRESHOLD, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getUnflushThreshold()),
                TServerConstants.TOPIC_DSK_UNFLUSHTHRESHOLD_MIN, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setUnflushThreshold((int) result.getRetData());
        // get unflushInterval parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.UNFLUSHINTERVAL, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getUnflushInterval()),
                TServerConstants.TOPIC_DSK_UNFLUSHINTERVAL_MIN, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setUnflushInterval((int) result.getRetData());
        // get unflushDataHold parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.UNFLUSHDATAHOLD, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getUnflushDataHold()),
                TServerConstants.TOPIC_DSK_UNFLUSHDATAHOLD_MIN, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setUnflushDataHold((int) result.getRetData());
        // get memCacheMsgSizeInMB parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.MCACHESIZEINMB, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getMemCacheMsgSizeInMB()),
                TServerConstants.TOPIC_CACHESIZE_MB_MIN,
                TServerConstants.TOPIC_CACHESIZE_MB_MAX, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setMemCacheMsgSizeInMB((int) result.getRetData());
        // get memCacheFlushIntvl parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.UNFMCACHEINTERVAL, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getMemCacheFlushIntvl()),
                TServerConstants.TOPIC_CACHEINTVL_MIN, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setMemCacheFlushIntvl((int) result.getRetData());
        // get memCacheMsgCntInK parameter value
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.UNFMCACHECNTINK, false,
                (defVal == null ? TBaseConstants.META_VALUE_UNDEFINED : defVal.getMemCacheMsgCntInK()),
                TServerConstants.TOPIC_CACHECNT_INK_MIN, sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setMemCacheMsgCntInK((int) result.getRetData());
        // get deletePolicy parameter value
        if (!WebParameterUtils.getDeletePolicyParameter(paramCntr, false,
                (defVal == null ? null : defVal.getDeletePolicy()), sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setDeletePolicy((String) result.getRetData());
        // get acceptPublish parameter value
        if (!WebParameterUtils.getBooleanParamValue(paramCntr, WebFieldDef.ACCEPTPUBLISH, false,
                (defVal == null ? null : defVal.getAcceptPublish()), sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setAcceptPublish((Boolean) result.getRetData());
        // get acceptSubscribe parameter value
        if (!WebParameterUtils.getBooleanParamValue(paramCntr, WebFieldDef.ACCEPTSUBSCRIBE, false,
                (defVal == null ? null : defVal.getAcceptSubscribe()), sBuffer, result)) {
            return result.isSuccess();
        }
        newConf.setAcceptSubscribe((Boolean) result.getRetData());
        result.setSuccResult(newConf);
        return result.isSuccess();
    }

    /**
     * Parse the parameter value from an object value to a long value
     *
     * @param paramCntr   parameter container object
     * @param fieldDef   the parameter field definition
     * @param required   a boolean value represent whether the parameter is must required
     * @param defValue   a default value returned if the field not exist
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static <T> boolean getLongParamValue(T paramCntr, WebFieldDef fieldDef,
                                                boolean required, long defValue,
                                                StringBuilder sBuffer, ProcessResult result) {
        if (!getStringParamValue(paramCntr, fieldDef,
                required, null, sBuffer, result)) {
            return result.isSuccess();
        }
        String paramValue = (String) result.getRetData();
        if (paramValue == null) {
            result.setSuccResult(defValue);
            return result.isSuccess();
        }
        try {
            long paramIntVal = Long.parseLong(paramValue);
            result.setSuccResult(paramIntVal);
        } catch (Throwable e) {
            result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                    .append(" parse error: ").append(e.getMessage()).toString());
            sBuffer.delete(0, sBuffer.length());
        }
        return result.isSuccess();
    }

    /**
     * Parse the parameter value from an object value to a Boolean value
     *
     * @param paramCntr   parameter container object
     * @param required   a boolean value represent whether the parameter is must required
     * @param defValue   default value
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static <T> boolean getFlowCtrlStatusParamValue(T paramCntr, boolean required,
                                                          Boolean defValue, StringBuilder sBuffer,
                                                          ProcessResult result) {
        // check and get statusId field
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.STATUSID, required,
                TBaseConstants.META_VALUE_UNDEFINED, 0, 1, sBuffer, result)) {
            return result.isSuccess();
        }
        int paramValue = (int) result.getRetData();
        if (paramValue == TBaseConstants.META_VALUE_UNDEFINED) {
            result.setSuccResult(defValue);
        } else {
            if (paramValue == 1) {
                result.setSuccResult(Boolean.TRUE);
            } else {
                result.setSuccResult(Boolean.FALSE);
            }
        }
        return result.isSuccess();
    }

    /**
     * Parse the parameter value from an object value to a integer value
     *
     * @param paramCntr   parameter container object
     * @param fieldDef   the parameter field definition
     * @param required   a boolean value represent whether the parameter is must required
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static <T> boolean getIntParamValue(T paramCntr, WebFieldDef fieldDef,
                                               boolean required, StringBuilder sBuffer,
                                               ProcessResult result) {
        return getIntParamValue(paramCntr, fieldDef, required,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                sBuffer, result);
    }

    /**
     * Parse the parameter value from an object value to a integer value
     *
     * @param paramCntr   parameter container object
     * @param fieldDef   the parameter field definition
     * @param required   a boolean value represent whether the parameter is must required
     * @param defValue   a default value returned if the field not exist
     * @param minValue   min value required
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static <T> boolean getIntParamValue(T paramCntr, WebFieldDef fieldDef,
                                               boolean required, int defValue, int minValue,
                                               StringBuilder sBuffer, ProcessResult result) {
        return getIntParamValue(paramCntr, fieldDef, required,
                true, defValue, true, minValue,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                sBuffer, result);
    }

    /**
     * Parse the parameter value from an object value to a integer value
     *
     * @param paramCntr  parameter container object
     * @param fieldDef   the parameter field definition
     * @param required   a boolean value represent whether the parameter is must required
     * @param defValue   a default value returned if the field not exist
     * @param minValue   min value required
     * @param maxValue   max value allowed
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static <T> boolean getIntParamValue(T paramCntr, WebFieldDef fieldDef,
                                               boolean required, int defValue,
                                               int minValue, int maxValue,
                                               StringBuilder sBuffer,
                                               ProcessResult result) {
        return getIntParamValue(paramCntr, fieldDef, required, true,
                defValue, true, minValue, true, maxValue, sBuffer, result);
    }

    // get int value from parameter string value
    private static <T> boolean getIntParamValue(T paramCntr,
                                                WebFieldDef fieldDef, boolean required,
                                                boolean hasDefVal, int defValue,
                                                boolean hasMinVal, int minValue,
                                                boolean hasMaxVal, int maxValue,
                                                StringBuilder sBuffer, ProcessResult result) {
        if (!getStringParamValue(paramCntr, fieldDef,
                required, null, sBuffer, result)) {
            return result.isSuccess();
        }
        if (fieldDef.isCompFieldType()) {
            Set<Integer> tgtValueSet = new HashSet<>();
            Set<String> valItemSet = (Set<String>) result.getRetData();
            if (valItemSet.isEmpty()) {
                if (hasDefVal) {
                    tgtValueSet.add(defValue);
                }
                result.setSuccResult(tgtValueSet);
                return result.isSuccess();
            }
            for (String itemVal : valItemSet) {
                if (!checkIntValueNorms(fieldDef, itemVal,
                        hasMinVal, minValue, hasMaxVal, maxValue, sBuffer, result)) {
                    return result.isSuccess();
                }
                tgtValueSet.add((Integer) result.getRetData());
            }
            result.setSuccResult(tgtValueSet);
        } else {
            String paramValue = (String) result.getRetData();
            if (paramValue == null) {
                if (hasDefVal) {
                    result.setSuccResult(defValue);
                }
                return result.isSuccess();
            }
            checkIntValueNorms(fieldDef, paramValue,
                    hasMinVal, minValue, hasMaxVal, maxValue, sBuffer, result);
        }
        return result.isSuccess();
    }

    /**
     * Parse the parameter value from an object value to a boolean value
     *
     * @param paramCntr   parameter container object
     * @param fieldDef    the parameter field definition
     * @param required    a boolean value represent whether the parameter is must required
     * @param defValue    a default value returned if the field not exist
     * @param sBuffer     string buffer
     * @param result      process result
     * @return valid result for the parameter value
     */
    public static <T> boolean getBooleanParamValue(T paramCntr, WebFieldDef fieldDef,
                                                   boolean required, Boolean defValue,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        if (!getStringParamValue(paramCntr, fieldDef,
                required, null, sBuffer, result)) {
            return result.isSuccess();
        }
        String paramValue = (String) result.getRetData();
        if (paramValue == null) {
            result.setSuccResult(defValue);
            return result.isSuccess();
        }
        if (paramValue.equalsIgnoreCase("true")
                || paramValue.equalsIgnoreCase("false")) {
            result.setSuccResult(Boolean.parseBoolean(paramValue));
        } else {
            try {
                result.setSuccResult(!(Long.parseLong(paramValue) == 0));
            } catch (Throwable e) {
                result.setSuccResult(defValue);
            }
        }
        return result.isSuccess();
    }

    /**
     * Parse the parameter value from an object value
     *
     * @param paramCntr    parameter container object
     * @param fieldDef     the parameter field definition
     * @param required     a boolean value represent whether the parameter is must required
     * @param defValue     a default value returned if the field not exist
     * @param sBuffer      string buffer
     * @param result       process result
     * @return valid result for the parameter value
     */
    public static <T> boolean getStringParamValue(T paramCntr, WebFieldDef fieldDef,
                                                  boolean required, String defValue,
                                                  StringBuilder sBuffer, ProcessResult result) {
        String paramValue;
        // get parameter value
        if (paramCntr instanceof Map) {
            Map<String, String> keyValueMap =
                    (Map<String, String>) paramCntr;
            paramValue = keyValueMap.get(fieldDef.name);
            if (paramValue == null) {
                paramValue = keyValueMap.get(fieldDef.shortName);
            }
        } else if (paramCntr instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) paramCntr;
            paramValue = req.getParameter(fieldDef.name);
            if (paramValue == null) {
                paramValue = req.getParameter(fieldDef.shortName);
            }
        } else {
            throw new IllegalArgumentException("Unknown parameter type!");
        }
        return checkStrParamValue(paramValue,
                fieldDef, required, defValue, sBuffer, result);
    }

    /**
     * Check the parameter value
     *
     * @param paramValue  parameter value
     * @param fieldDef    the parameter field definition
     * @param required     a boolean value represent whether the parameter is must required
     * @param defValue     a default value returned if the field not exist
     * @param sBuffer     string buffer
     * @param result      process result
     * @return valid result for the parameter value
     */
    private static boolean checkStrParamValue(String paramValue, WebFieldDef fieldDef,
                                              boolean required, String defValue,
                                              StringBuilder sBuffer, ProcessResult result) {
        if (TStringUtils.isNotBlank(paramValue)) {
            // Cleanup value extra characters
            paramValue = escDoubleQuotes(paramValue.trim());
        }
        // Check if the parameter exists
        if (TStringUtils.isBlank(paramValue)) {
            if (required) {
                result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                        .append(" is missing or value is blank!").toString());
                sBuffer.delete(0, sBuffer.length());
            } else {
                procStringDefValue(fieldDef.isCompFieldType(), defValue, result);
            }
            return result.isSuccess();
        }
        // check if value is norm;
        if (fieldDef.isCompFieldType()) {
            // split original value to items
            TreeSet<String> valItemSet = new TreeSet<>();
            String[] strParamValueItems = paramValue.split(fieldDef.splitToken);
            for (String strParamValueItem : strParamValueItems) {
                if (TStringUtils.isBlank(strParamValueItem)) {
                    continue;
                }
                if (!checkStrValueNorms(fieldDef, strParamValueItem, sBuffer, result)) {
                    return result.isSuccess();
                }
                valItemSet.add((String) result.getRetData());
            }
            // check if is empty result
            if (valItemSet.isEmpty()) {
                if (required) {
                    result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                            .append(" is missing or value is blank!").toString());
                    sBuffer.delete(0, sBuffer.length());
                } else {
                    procStringDefValue(fieldDef.isCompFieldType(), defValue, result);
                }
                return result.isSuccess();
            }
            // check max item count
            if (fieldDef.itemMaxCnt != TBaseConstants.META_VALUE_UNDEFINED) {
                if (valItemSet.size() > fieldDef.itemMaxCnt) {
                    result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                            .append("'s item count over max allowed count (")
                            .append(fieldDef.itemMaxCnt).append(")!").toString());
                    sBuffer.delete(0, sBuffer.length());
                }
            }
            valItemSet.comparator();
            result.setSuccResult(valItemSet);
        } else {
            if (!checkStrValueNorms(fieldDef, paramValue, sBuffer, result)) {
                return result.isSuccess();
            }
            result.setSuccResult(paramValue);
        }
        return result.isSuccess();
    }

    /**
     * Get and valid topicName value
     *
     * @param req        Http Servlet Request
     * @param defMetaDataService  configure manager
     * @param required   a boolean value represent whether the parameter is must required
     * @param defValue   a default value returned if the field not exist
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static boolean getAndValidTopicNameInfo(HttpServletRequest req,
                                                   MetaDataService defMetaDataService,
                                                   boolean required,
                                                   String defValue,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, required, defValue, sBuffer, result)) {
            return result.isSuccess();
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        Set<String> existedTopicSet =
                defMetaDataService.getTotalConfiguredTopicNames();
        for (String topic : topicNameSet) {
            if (!existedTopicSet.contains(topic)) {
                result.setFailResult(sBuffer.append(WebFieldDef.COMPSTOPICNAME.name)
                        .append(" value ").append(topic)
                        .append(" is not configure, please configure first!").toString());
                sBuffer.delete(0, sBuffer.length());
                break;
            }
        }
        return result.isSuccess();
    }

    /**
     * check the filter conditions and get them in a String
     *
     * @param paramCntr   parameter container object
     * @param required    denote whether it translate blank condition
     * @param transBlank  whether to translate condition item
     * @param sBuffer     string buffer
     * @param result      process result of parameter value
     * @return process result
     */
    public static <T> boolean getFilterCondString(T paramCntr, boolean required,
                                                  boolean transBlank,
                                                  StringBuilder sBuffer,
                                                  ProcessResult result) {
        if (!getFilterCondSet(paramCntr, required, false, sBuffer, result)) {
            return result.isSuccess();
        }
        Set<String> filterCondSet = (Set<String>) result.getRetData();
        if (filterCondSet.isEmpty()) {
            if (transBlank) {
                sBuffer.append(TServerConstants.BLANK_FILTER_ITEM_STR);
            }
        } else {
            sBuffer.append(TokenConstants.ARRAY_SEP);
            for (String filterCond : filterCondSet) {
                sBuffer.append(filterCond).append(TokenConstants.ARRAY_SEP);
            }
        }
        result.setSuccResult(sBuffer.toString());
        sBuffer.delete(0, sBuffer.length());
        return result.isSuccess();
    }

    /**
     * check the filter conditions and get them in a set
     *
     * @param paramCntr   parameter container object
     * @param required   a boolean value represent whether the parameter is must required
     * @param transCondItem   whether to translate condition item
     * @param sBuffer     string buffer
     * @param result     process result of parameter value
     * @return process result
     */
    public static <T> boolean getFilterCondSet(T paramCntr, boolean required,
                                               boolean transCondItem,
                                               StringBuilder sBuffer,
                                               ProcessResult result) {
        if (!WebParameterUtils.getStringParamValue(paramCntr,
                WebFieldDef.FILTERCONDS, required, null, sBuffer, result)) {
            return result.isSuccess();
        }
        if (transCondItem) {
            // translate filter condition item with "''"
            TreeSet<String> newFilterCondSet = new TreeSet<>();
            Set<String> filterCondSet = (Set<String>) result.getRetData();
            if (!filterCondSet.isEmpty()) {
                for (String filterCond : filterCondSet) {
                    newFilterCondSet.add(sBuffer.append(TokenConstants.ARRAY_SEP)
                            .append(filterCond).append(TokenConstants.ARRAY_SEP).toString());
                    sBuffer.delete(0, sBuffer.length());
                }
                newFilterCondSet.comparator();
            }
            result.setSuccResult(newFilterCondSet);
        }
        return result.isSuccess();
    }

    /**
     * Judge whether the query filter item set is completely contained by the target item set
     *
     * @param qryFilterSet     the query filter item set
     * @param confFilterStr    the target item set
     * @return true all item are included, false not
     */
    public static boolean isFilterSetFullIncluded(
            Set<String> qryFilterSet, String confFilterStr) {
        if (qryFilterSet == null || qryFilterSet.isEmpty()) {
            return true;
        }
        if (confFilterStr == null
                || (confFilterStr.length() == 2
                && confFilterStr.equals(TServerConstants.BLANK_FILTER_ITEM_STR))) {
            return false;
        }
        boolean allInc = true;
        for (String filterCond : qryFilterSet) {
            if (!confFilterStr.contains(filterCond)) {
                allInc = false;
                break;
            }
        }
        return allInc;
    }

    /**
     * Parse the parameter value from an json dict
     *
     * @param req         Http Servlet Request
     * @param fieldDef    the parameter field definition
     * @param required    a boolean value represent whether the parameter is must required
     * @param defValue    a default value returned if the field not exist
     * @param result      process result
     * @return valid result for the parameter value
     */
    public static boolean getJsonDictParamValue(HttpServletRequest req,
                                                WebFieldDef fieldDef,
                                                boolean required,
                                                Map<String, Long> defValue,
                                                ProcessResult result) {
        // get parameter value
        String paramValue = req.getParameter(fieldDef.name);
        if (paramValue == null) {
            paramValue = req.getParameter(fieldDef.shortName);
        }
        if (TStringUtils.isNotBlank(paramValue)) {
            // Cleanup value extra characters
            paramValue = escDoubleQuotes(paramValue.trim());
        }
        // Check if the parameter exists
        if (TStringUtils.isBlank(paramValue)) {
            if (required) {
                result.setFailResult(new StringBuilder(512)
                        .append("Parameter ").append(fieldDef.name)
                        .append(" is missing or value is blank!").toString());
            } else {
                result.setSuccResult(defValue);
            }
            return result.isSuccess();
        }
        try {
            paramValue = URLDecoder.decode(paramValue,
                    TBaseConstants.META_DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            result.setFailResult(new StringBuilder(512)
                    .append("Parameter ").append(fieldDef.name)
                    .append(" decode error, exception is ")
                    .append(e.toString()).toString());
        }
        if (TStringUtils.isBlank(paramValue)) {
            if (required) {
                result.setFailResult(new StringBuilder(512).append("Parameter ")
                        .append(fieldDef.name).append("'s value is blank!").toString());
            } else {
                result.setSuccResult(defValue);
            }
            return result.isSuccess();
        }
        if (fieldDef.valMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (paramValue.length() > fieldDef.valMaxLen) {
                result.setFailResult(new StringBuilder(512)
                        .append("Parameter ").append(fieldDef.name)
                        .append("'s length over max allowed length (")
                        .append(fieldDef.valMaxLen).append(")!").toString());
                return result.isSuccess();
            }
        }
        // parse data
        try {
            Map<String, Long> manOffsets = new Gson().fromJson(paramValue,
                    new TypeToken<Map<String, Long>>(){}.getType());
            result.setSuccResult(manOffsets);
        } catch (Throwable e) {
            result.setFailResult(new StringBuilder(512)
                    .append("Parameter ").append(fieldDef.name)
                    .append(" value parse failure, error is ")
                    .append(e.getMessage()).append("!").toString());
        }
        return result.isSuccess();
    }

    /**
     * Parse the parameter value from an json array
     *
     * @param req         Http Servlet Request
     * @param fieldDef    the parameter field definition
     * @param required    a boolean value represent whether the parameter is must required
     * @param defValue    a default value returned if the field not exist
     * @param result      process result
     * @return valid result for the parameter value
     */
    public static boolean getJsonArrayParamValue(HttpServletRequest req,
                                                 WebFieldDef fieldDef,
                                                 boolean required,
                                                 List<Map<String, String>> defValue,
                                                 ProcessResult result) {
        // get parameter value
        String paramValue = req.getParameter(fieldDef.name);
        if (paramValue == null) {
            paramValue = req.getParameter(fieldDef.shortName);
        }
        if (TStringUtils.isNotBlank(paramValue)) {
            // Cleanup value extra characters
            paramValue = escDoubleQuotes(paramValue.trim());
        }
        // Check if the parameter exists
        if (TStringUtils.isBlank(paramValue)) {
            if (required) {
                result.setFailResult(new StringBuilder(512)
                        .append("Parameter ").append(fieldDef.name)
                        .append(" is missing or value is blank!").toString());
            } else {
                result.setSuccResult(defValue);
            }
            return result.isSuccess();
        }
        try {
            paramValue = URLDecoder.decode(paramValue,
                    TBaseConstants.META_DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            result.setFailResult(new StringBuilder(512)
                    .append("Parameter ").append(fieldDef.name)
                    .append(" decode error, exception is ")
                    .append(e.toString()).toString());
        }
        if (TStringUtils.isBlank(paramValue)) {
            if (required) {
                result.setFailResult(new StringBuilder(512).append("Parameter ")
                        .append(fieldDef.name).append("'s value is blank!").toString());
            } else {
                result.setSuccResult(defValue);
            }
            return result.isSuccess();
        }
        if (fieldDef.valMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (paramValue.length() > fieldDef.valMaxLen) {
                result.setFailResult(new StringBuilder(512)
                        .append("Parameter ").append(fieldDef.name)
                        .append("'s length over max allowed length (")
                        .append(fieldDef.valMaxLen).append(")!").toString());
                return result.isSuccess();
            }
        }
        // parse data
        try {
            List<Map<String, String>> arrayValue = new Gson().fromJson(paramValue,
                    new TypeToken<List<Map<String, String>>>(){}.getType());
            result.setSuccResult(arrayValue);
        } catch (Throwable e) {
            result.setFailResult(new StringBuilder(512)
                    .append("Parameter ").append(fieldDef.name)
                    .append(" value parse failure, error is ")
                    .append(e.getMessage()).append("!").toString());
        }
        return result.isSuccess();
    }

    /**
     * Parse the parameter value from an string value to Date value
     *
     * @param paramCntr   parameter container object
     * @param fieldDef    the parameter field definition
     * @param required    a boolean value represent whether the parameter is must required
     * @param defValue    a default value returned if failed to parse value from the given object
     * @param sBuffer     string buffer
     * @param result      process result
     * @return valid result for the parameter value
     */
    public static <T> boolean getDateParameter(T paramCntr, WebFieldDef fieldDef,
                                               boolean required, Date defValue,
                                               StringBuilder sBuffer,
                                               ProcessResult result) {
        if (!getStringParamValue(paramCntr, fieldDef,
                required, null, sBuffer, result)) {
            return result.isSuccess();
        }
        String paramValue = (String) result.getRetData();
        if (paramValue == null) {
            result.setSuccResult(defValue);
            return result.isSuccess();
        }
        Date date = DateTimeConvertUtils.yyyyMMddHHmmss2date(paramValue);
        if (date == null) {
            result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                    .append("'s value ").append(paramValue)
                    .append(" parse error, required value format is ")
                    .append(DateTimeConvertUtils.PAT_YYYYMMDDHHMMSS).toString());
            sBuffer.delete(0, sBuffer.length());
        } else {
            result.setSuccResult(date);
        }
        return result.isSuccess();
    }

    /**
     * Valid execution authorization info
     * @param req        Http Servlet Request
     * @param required   a boolean value represent whether the parameter is must required
     * @param master     current master object
     * @param sBuffer     string buffer
     * @param result     process result
     * @return valid result for the parameter value
     */
    public static boolean validReqAuthorizeInfo(HttpServletRequest req, boolean required,
                                                TMaster master, StringBuilder sBuffer,
                                                ProcessResult result) {
        if (!getStringParamValue(req, WebFieldDef.ADMINAUTHTOKEN,
                required, null, sBuffer, result)) {
            return result.isSuccess();
        }
        String paramValue = (String) result.getRetData();
        if (paramValue != null) {
            if (!paramValue.equals(master.getMasterConfig().getConfModAuthToken())) {
                result.setFailResult("illegal access, unauthorized request!");
            }
        }
        return result.isSuccess();
    }

    /**
     * process string default value
     *
     * @param isCompFieldType   the parameter if compound field type
     * @param defValue   the parameter default value
     * @param result process result
     * @return process result for default value of parameter
     */
    private static boolean procStringDefValue(boolean isCompFieldType,
                                              String defValue,
                                              ProcessResult result) {
        if (isCompFieldType) {
            TreeSet<String> valItemSet = new TreeSet<>();
            if (TStringUtils.isNotBlank(defValue)) {
                valItemSet.add(defValue);
            }
            valItemSet.comparator();
            result.setSuccResult(valItemSet);
        } else {
            result.setSuccResult(defValue);
        }
        return result.isSuccess();
    }

    /**
     * Parse the parameter string value by regex define
     *
     * @param fieldDef     the parameter field definition
     * @param paramVal     the parameter value
     * @param sBuffer      string buffer
     * @param result       process result
     * @return check result for string value of parameter
     */
    private static boolean checkStrValueNorms(WebFieldDef fieldDef, String paramVal,
                                              StringBuilder sBuffer, ProcessResult result) {
        paramVal = paramVal.trim();
        if (TStringUtils.isBlank(paramVal)) {
            result.setSuccResult(null);
            return true;
        }
        // check value's max length
        if (fieldDef.valMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (paramVal.length() > fieldDef.valMaxLen) {
                result.setFailResult(sBuffer.append("over max length for ")
                        .append(fieldDef.name).append(", only allow ")
                        .append(fieldDef.valMaxLen).append(" length").toString());
                sBuffer.delete(0, sBuffer.length());
                return false;
            }
        }
        // check value's pattern
        if (fieldDef.regexCheck) {
            if (!paramVal.matches(fieldDef.regexDef.getPattern())) {
                if ((fieldDef != WebFieldDef.TOPICNAME
                        && fieldDef != WebFieldDef.COMPSTOPICNAME)
                        || !paramVal.equals(TServerConstants.OFFSET_HISTORY_NAME)) {
                    result.setFailResult(sBuffer.append("illegal value for ")
                            .append(fieldDef.name).append(", value ")
                            .append(fieldDef.regexDef.getErrMsgTemp()).toString());
                    sBuffer.delete(0, sBuffer.length());
                    return false;
                }
            }
        }
        result.setSuccResult(paramVal);
        return true;
    }

    /**
     * Parse the parameter string value by regex define
     *
     * @param fieldDef     the parameter field definition
     * @param paramValue   the parameter value
     * @param hasMinVal    whether there is a minimum
     * @param minValue      the parameter min value
     * @param hasMaxVal    whether there is a maximum
     * @param maxValue      the parameter max value
     * @param sBuffer      string buffer
     * @param result   process result
     * @return check result for string value of parameter
     */
    private static boolean checkIntValueNorms(WebFieldDef fieldDef, String paramValue,
                                              boolean hasMinVal, int minValue,
                                              boolean hasMaxVal, int maxValue,
                                              StringBuilder sBuffer, ProcessResult result) {
        try {
            int paramIntVal = Integer.parseInt(paramValue);
            if (hasMinVal && paramIntVal < minValue) {
                result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                        .append(" value must >= ").append(minValue).toString());
                sBuffer.delete(0, sBuffer.length());
                return false;
            }
            if (hasMaxVal && paramIntVal > maxValue) {
                result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                        .append(" value must <= ").append(maxValue).toString());
                sBuffer.delete(0, sBuffer.length());
                return false;
            }
            result.setSuccResult(paramIntVal);
        } catch (Throwable e) {
            result.setFailResult(sBuffer.append("Parameter ").append(fieldDef.name)
                    .append(" parse error: ").append(e.getMessage()).toString());
            sBuffer.delete(0, sBuffer.length());
            return false;
        }
        return true;
    }

    /**
     * check the filter conditions and get them
     *
     * @param inFilterConds the filter conditions to be decoded
     * @param isTransBlank  denote whether it translate blank condition
     * @param sb            the string buffer used to construct result
     * @return the decoded filter conditions
     * @throws Exception if failed to decode the filter conditions
     */
    public static String checkAndGetFilterConds(String inFilterConds,
                                                boolean isTransBlank,
                                                StringBuilder sb) throws Exception {
        if (TStringUtils.isNotBlank(inFilterConds)) {
            inFilterConds = escDoubleQuotes(inFilterConds.trim());
        }
        if (TStringUtils.isBlank(inFilterConds)) {
            if (isTransBlank) {
                sb.append(TServerConstants.BLANK_FILTER_ITEM_STR);
            }
        } else {
            sb.append(TokenConstants.ARRAY_SEP);
            TreeSet<String> filterConds = new TreeSet<>();
            String[] strFilterConds = inFilterConds.split(TokenConstants.ARRAY_SEP);
            for (int i = 0; i < strFilterConds.length; i++) {
                if (TStringUtils.isBlank(strFilterConds[i])) {
                    continue;
                }
                String filterCond = strFilterConds[i].trim();
                if (filterCond.length() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_LENGTH) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the max length of ")
                            .append(filterCond).append(" in filterConds parameter over ")
                            .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_LENGTH)
                            .append(" characters").toString());
                }
                if (!filterCond.matches(TBaseConstants.META_TMP_FILTER_VALUE)) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the value of ")
                        .append(filterCond).append(" in filterCond parameter ")
                        .append("must only contain characters,numbers,and underscores").toString());
                }
                filterConds.add(filterCond);
            }
            int count = 0;
            for (String itemStr : filterConds) {
                if (count++ > 0) {
                    sb.append(TokenConstants.ARRAY_SEP);
                }
                sb.append(itemStr);
            }
            sb.append(TokenConstants.ARRAY_SEP);
        }
        String strNewFilterConds = sb.toString();
        sb.delete(0, sb.length());
        return strNewFilterConds;
    }

    /**
     * check the filter conditions and get them in a set
     *
     * @param inFilterConds the filter conditions to be decoded
     * @param transCondItem whether to translate condition item
     * @param checkTotalCnt whether to check condition item exceed max count
     * @param sb            the string buffer used to construct result
     * @return the decoded filter conditions
     * @throws Exception if failed to decode the filter conditions
     */
    public static Set<String> checkAndGetFilterCondSet(String inFilterConds,
                                                       boolean transCondItem,
                                                       boolean checkTotalCnt,
                                                       StringBuilder sb) throws Exception {
        Set<String> filterCondSet = new HashSet<>();
        if (TStringUtils.isBlank(inFilterConds)) {
            return filterCondSet;
        }
        inFilterConds = escDoubleQuotes(inFilterConds.trim());
        if (TStringUtils.isNotBlank(inFilterConds)) {
            String[] strFilterConds = inFilterConds.split(TokenConstants.ARRAY_SEP);
            for (int i = 0; i < strFilterConds.length; i++) {
                if (TStringUtils.isBlank(strFilterConds[i])) {
                    continue;
                }
                String filterCond = strFilterConds[i].trim();
                if (filterCond.length() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the max length of ")
                            .append(filterCond).append(" in filterConds parameter over ")
                            .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT)
                            .append(" characters").toString());
                }
                if (!filterCond.matches(TBaseConstants.META_TMP_FILTER_VALUE)) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the value of ")
                        .append(filterCond).append(" in filterCond parameter must ")
                        .append("only contain characters,numbers,and underscores").toString());
                }
                if (transCondItem) {
                    filterCondSet.add(sb.append(TokenConstants.ARRAY_SEP)
                            .append(filterCond).append(TokenConstants.ARRAY_SEP).toString());
                    sb.delete(0, sb.length());
                } else {
                    filterCondSet.add(filterCond);
                }
            }
            if (checkTotalCnt) {
                if (filterCondSet.size() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT) {
                    throw new Exception(sb.append("Illegal value: the count of filterCond's ")
                        .append("value over max allowed count(")
                        .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT).append(")!").toString());
                }
            }
        }
        return filterCondSet;
    }

    /**
     * check and get batched group names
     *
     * @param inputGroupName the group name string value
     * @param checkEmpty     whether check data emtpy
     * @param checkResToken  whether check reserved group token
     * @param resTokens      reserved group name set
     * @param sb             the string process space
     * @return the batched group names
     */
    public static Set<String> getBatchGroupNames(String inputGroupName,
                                                 boolean checkEmpty,
                                                 boolean checkResToken,
                                                 Set<String> resTokens,
                                                 StringBuilder sb) throws Exception {
        Set<String> batchOpGroupNames = new HashSet<>();
        if (TStringUtils.isNotBlank(inputGroupName)) {
            inputGroupName = escDoubleQuotes(inputGroupName.trim());
        }
        if (TStringUtils.isBlank(inputGroupName)) {
            if (checkEmpty) {
                throw new Exception("Illegal value: required groupName parameter");
            }
            return batchOpGroupNames;
        }
        String[] strGroupNames = inputGroupName.split(TokenConstants.ARRAY_SEP);
        if (strGroupNames.length > TServerConstants.CFG_BATCH_RECORD_OPERATE_MAX_COUNT) {
            throw new Exception(sb.append("Illegal value: groupName's batch count over max count ")
                .append(TServerConstants.CFG_BATCH_RECORD_OPERATE_MAX_COUNT).toString());
        }
        for (int i = 0; i < strGroupNames.length; i++) {
            if (TStringUtils.isBlank(strGroupNames[i])) {
                continue;
            }
            String groupName = strGroupNames[i].trim();
            if (checkResToken) {
                if (resTokens != null && !resTokens.isEmpty()) {
                    if (resTokens.contains(groupName)) {
                        throw new Exception(sb.append("Illegal value: in groupName parameter, '")
                            .append(groupName).append("' is a system reserved token!").toString());
                    }
                }
            }
            if (groupName.length() > TBaseConstants.META_MAX_GROUPNAME_LENGTH) {
                throw new Exception(sb.append("Illegal value: the max length of ")
                        .append(groupName).append(" in groupName parameter over ")
                        .append(TBaseConstants.META_MAX_GROUPNAME_LENGTH)
                        .append(" characters").toString());
            }
            if (!groupName.matches(TBaseConstants.META_TMP_GROUP_VALUE)) {
                throw new Exception(sb.append("Illegal value: the value of ").append(groupName)
                    .append("in groupName parameter must begin with a letter, can only contain ")
                    .append("characters,numbers,hyphen,and underscores").toString());
            }
            batchOpGroupNames.add(groupName);
        }
        if (batchOpGroupNames.isEmpty()) {
            if (checkEmpty) {
                throw new Exception("Illegal value: Null value of groupName parameter");
            }
        }
        return batchOpGroupNames;
    }

    /**
     * check and get batched broker ips
     *
     * @param inStrBrokerIps the brokerIp string value
     * @param checkEmpty     whether check data emtpy
     * @return the batched broker ids
     */
    public static Set<String> getBatchBrokerIpSet(String inStrBrokerIps,
                                                  boolean checkEmpty) throws Exception {
        Set<String> batchBrokerIps = new HashSet<>();
        if (TStringUtils.isNotBlank(inStrBrokerIps)) {
            inStrBrokerIps = escDoubleQuotes(inStrBrokerIps.trim());
        }
        if (TStringUtils.isBlank(inStrBrokerIps)) {
            if (checkEmpty) {
                throw new Exception("Illegal value: required brokerIp parameter");
            }
            return batchBrokerIps;
        }
        String[] strBrokerIps = inStrBrokerIps.split(TokenConstants.ARRAY_SEP);
        for (int i = 0; i < strBrokerIps.length; i++) {
            if (TStringUtils.isEmpty(strBrokerIps[i])) {
                continue;
            }
            String brokerIp =
                    checkParamCommonRequires("brokerIp", strBrokerIps[i], true);
            if (batchBrokerIps.contains(brokerIp)) {
                continue;
            }
            batchBrokerIps.add(brokerIp);
        }
        if (batchBrokerIps.isEmpty()) {
            if (checkEmpty) {
                throw new Exception("Illegal value: Null value of brokerIp parameter");
            }
        }
        return batchBrokerIps;
    }

    /**
     * check and get parameter value with json array
     *
     * @param paramName   the parameter name
     * @param paramValue  the object value of the parameter
     * @param paramMaxLen the maximum length of json array
     * @param required    denote whether the parameter is must required
     * @return a list of linked hash map represent the json array
     * @throws Exception
     */
    public static List<Map<String, String>> checkAndGetJsonArray(String paramName,
                                                                 String paramValue,
                                                                 int paramMaxLen,
                                                                 boolean required) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue) && !required) {
            return null;
        }
        String decTmpParamVal = null;
        try {
            decTmpParamVal = URLDecoder.decode(tmpParamValue,
                    TBaseConstants.META_DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new Exception(new StringBuilder(512).append("Decode ")
                    .append(paramName).append("error, exception is ")
                    .append(e.toString()).toString());
        }
        if (TStringUtils.isBlank(decTmpParamVal)) {
            if (required) {
                throw new Exception(new StringBuilder(512)
                        .append("Blank value of ").append(paramName)
                        .append(" parameter").toString());
            } else {
                return null;
            }
        }
        if (paramMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (decTmpParamVal.length() > paramMaxLen) {
                throw new Exception(new StringBuilder(512)
                        .append("the max length of ").append(paramName)
                        .append(" parameter is ").append(paramMaxLen)
                        .append(" characters").toString());
            }
        }
        return new Gson().fromJson(decTmpParamVal, new TypeToken<List<Map<String, String>>>(){}.getType());
    }

    /**
     * Check the broker online status
     *
     * @param curEntity the entity of bdb broker configuration
     * @return the true if broker is online, false in other cases
     */
    public static boolean checkBrokerInOnlineStatus(BdbBrokerConfEntity curEntity) {
        if (curEntity != null) {
            return (curEntity.getManageStatus() == TStatusConstants.STATUS_MANAGE_ONLINE
                    || curEntity.getManageStatus() == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE
                    || curEntity.getManageStatus() == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ);
        }
        return false;
    }

    /**
     * translate broker manage status from int to string value
     *
     * @param manageStatus the broker manage status
     * @return the broker string manage status
     */
    public static String getBrokerManageStatusStr(int manageStatus) {
        String strManageStatus = "unsupported_status";
        if (manageStatus == TStatusConstants.STATUS_MANAGE_APPLY) {
            strManageStatus = "draft";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE) {
            strManageStatus = "online";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_OFFLINE) {
            strManageStatus = "offline";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE) {
            strManageStatus = "only-read";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ) {
            strManageStatus = "only-write";
        }
        return strManageStatus;
    }

    /**
     * translate broker manage status from int to tuple2 value
     *
     * @param manageStatus the broker manage status
     * @return the broker tuple2 manage status
     */
    public static Tuple2<Boolean, Boolean> getPubSubStatusByManageStatus(int manageStatus) {
        boolean isAcceptPublish = false;
        boolean isAcceptSubscribe = false;
        if (manageStatus >= TStatusConstants.STATUS_MANAGE_ONLINE) {
            if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE) {
                isAcceptPublish = true;
                isAcceptSubscribe = true;
            } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE) {
                isAcceptPublish = false;
                isAcceptSubscribe = true;
            } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ) {
                isAcceptPublish = true;
                isAcceptSubscribe = false;
            }
        }
        return new Tuple2<>(isAcceptPublish, isAcceptSubscribe);
    }

    /**
     * check parameter is required
     *
     * @param paramName   the parameter name
     * @param paramValue  the parameter value
     * @param required    Whether the parameter is required
     * @return the parameter value without quotes
     */
    public static String checkParamCommonRequires(String paramName, String paramValue,
                                                  boolean required) throws Exception {
        String temParamValue = null;
        if (paramValue == null) {
            if (required) {
                throw new Exception(new StringBuilder(512).append("Required ")
                        .append(paramName).append(" parameter").toString());
            }
        } else {
            temParamValue = escDoubleQuotes(paramValue.trim());
            if (TStringUtils.isBlank(temParamValue)) {
                if (required) {
                    throw new Exception(new StringBuilder(512)
                            .append("Null or blank value of ").append(paramName)
                            .append(" parameter").toString());
                }
            }
        }
        return temParamValue;
    }

    /**
     * translate rule info to json format string
     *
     * @param paramCntr   the parameter name
     * @param defValue    the default value
     * @param sBuffer     string buffer
     * @param result      process result
     * @return the count of flow control rule
     */
    public static <T> int getAndCheckFlowRules(T paramCntr, String defValue,
                                               StringBuilder sBuffer,
                                               ProcessResult result) {
        // get parameter value
        String paramValue;
        // get parameter value
        if (paramCntr instanceof Map) {
            Map<String, String> keyValueMap =
                    (Map<String, String>) paramCntr;
            paramValue = keyValueMap.get(WebFieldDef.FLOWCTRLSET.name);
            if (paramValue == null) {
                paramValue = keyValueMap.get(WebFieldDef.FLOWCTRLSET.shortName);
            }
        } else if (paramCntr instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) paramCntr;
            paramValue = req.getParameter(WebFieldDef.FLOWCTRLSET.name);
            if (paramValue == null) {
                paramValue = req.getParameter(WebFieldDef.FLOWCTRLSET.shortName);
            }
        } else {
            throw new IllegalArgumentException("Unknown parameter type!");
        }
        if (TStringUtils.isBlank(paramValue)) {
            result.setSuccResult(defValue);
            return 0;
        }
        paramValue = paramValue.trim();
        return validFlowRuleValue(paramValue, sBuffer, result);
    }

    // remove double quotes in string left and right
    private static String escDoubleQuotes(String inPutStr) {
        if (TStringUtils.isBlank(inPutStr) || inPutStr.length() < 2) {
            return inPutStr;
        }
        if (inPutStr.charAt(0) == '\"'
                && inPutStr.charAt(inPutStr.length() - 1) == '\"') {
            if (inPutStr.length() == 2) {
                return "";
            } else {
                return inPutStr.substring(1, inPutStr.length() - 1).trim();
            }
        }
        return inPutStr;
    }

    // valid flow control rule informations
    private static int validFlowRuleValue(String paramValue,
                                          StringBuilder sBuffer,
                                          ProcessResult result) {
        int ruleCnt = 0;
        paramValue = paramValue.trim();
        List<Integer> ruleTypes = Arrays.asList(0, 1, 2, 3);
        FlowCtrlRuleHandler flowCtrlRuleHandler =
                new FlowCtrlRuleHandler(true);
        Map<Integer, List<FlowCtrlItem>> flowCtrlItemMap;
        try {
            flowCtrlItemMap =
                    flowCtrlRuleHandler.parseFlowCtrlInfo(paramValue);
        } catch (Throwable e) {
            result.setFailResult(sBuffer.append("Parse parameter ")
                    .append(WebFieldDef.FLOWCTRLSET.name)
                    .append(" failure: '").append(e.toString()).toString());
            sBuffer.delete(0, sBuffer.length());
            return 0;
        }
        sBuffer.append("[");
        for (Integer typeId : ruleTypes) {
            if (typeId != null) {
                int rules = 0;
                List<FlowCtrlItem> flowCtrlItems = flowCtrlItemMap.get(typeId);
                if (flowCtrlItems != null) {
                    if (ruleCnt++ > 0) {
                        sBuffer.append(",");
                    }
                    sBuffer.append("{\"type\":").append(typeId.intValue()).append(",\"rule\":[");
                    for (FlowCtrlItem flowCtrlItem : flowCtrlItems) {
                        if (flowCtrlItem != null) {
                            if (rules++ > 0) {
                                sBuffer.append(",");
                            }
                            sBuffer = flowCtrlItem.toJsonString(sBuffer);
                        }
                    }
                    sBuffer.append("]}");
                }
            }
        }
        sBuffer.append("]");
        result.setSuccResult(sBuffer.toString());
        sBuffer.delete(0, sBuffer.length());
        return ruleCnt;
    }
}
