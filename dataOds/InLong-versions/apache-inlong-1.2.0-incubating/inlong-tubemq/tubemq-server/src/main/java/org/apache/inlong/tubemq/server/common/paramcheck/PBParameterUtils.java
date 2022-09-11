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

package org.apache.inlong.tubemq.server.common.paramcheck;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.metadata.MetadataManager;
import org.apache.inlong.tubemq.server.broker.metadata.TopicMetadata;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumeType;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PBParameterUtils {

    private static final Logger logger = LoggerFactory.getLogger(PBParameterUtils.class);

    /**
     * Check request topic list of producer
     *
     * @param reqTopicLst the topic list to be checked.
     * @param strBuffer   a string buffer used to construct the result
     * @return the check result
     */
    public static ParamCheckResult checkProducerTopicList(final List<String> reqTopicLst,
                                                          final StringBuilder strBuffer) {
        ParamCheckResult retResult = new ParamCheckResult();
        if (reqTopicLst == null) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary topic field info!");
            return retResult;
        }
        Set<String> transTopicList = new HashSet<>();
        if (!reqTopicLst.isEmpty()) {
            for (String topic : reqTopicLst) {
                if (TStringUtils.isBlank(topic)) {
                    continue;
                }
                topic = topic.trim();
                // filter system topic OFFSET_HISTORY_NAME
                if (topic.equals(TServerConstants.OFFSET_HISTORY_NAME)) {
                    retResult.setCheckResult(false,
                            TErrCodeConstants.BAD_REQUEST,
                            strBuffer.append("System Topic ")
                                    .append(TServerConstants.OFFSET_HISTORY_NAME)
                                    .append(" does not allow client produce data!").toString());
                    strBuffer.delete(0, strBuffer.length());
                    return retResult;
                }
                transTopicList.add(topic);
            }
        }
        if (transTopicList.size() > TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    strBuffer.append("Booked topic's count over max value, required max count is ")
                            .append(TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT).toString());
            strBuffer.delete(0, strBuffer.length());
            return retResult;
        }
        retResult.setCheckData(transTopicList);
        return retResult;
    }

    /**
     * Check request topic list of consumer
     *
     * @param reqTopicLst the topic list to be checked.
     * @param strBuffer   a string buffer used to construct the result
     * @return the check result
     */
    public static ParamCheckResult checkConsumerTopicList(final List<String> reqTopicLst,
                                                          final StringBuilder strBuffer) {
        ParamCheckResult retResult = new ParamCheckResult();
        if ((reqTopicLst == null)
                || (reqTopicLst.isEmpty())) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary subscribed topicList data!");
            return retResult;
        }
        Set<String> transTopicSet = new HashSet<>();
        for (String topicItem : reqTopicLst) {
            if (TStringUtils.isBlank(topicItem)) {
                continue;
            }
            transTopicSet.add(topicItem.trim());
        }
        if (transTopicSet.isEmpty()) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    "Request subscribed topicList data must not Blank!");
            return retResult;
        }
        if (transTopicSet.size() > TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    strBuffer.append("Subscribed topicList size over max value, required max count is ")
                            .append(TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT).toString());
            strBuffer.delete(0, strBuffer.length());
            return retResult;
        }
        retResult.setCheckData(transTopicSet);
        return retResult;
    }

    /**
     * Check the validity of the bootstrap Offset information specified by the consumer.
     *
     * @param csmType        the topic list to be checked.
     * @param reqTopicSet    the subscribed topic set
     * @param requiredParts  the specified partitionKey-bootstrap offset map
     * @param strBuffer      the string buffer used to construct the result
     * @return the check result
     */
    public static ParamCheckResult checkConsumerOffsetSetInfo(ConsumeType csmType,
                                                              final Set<String> reqTopicSet,
                                                              final String requiredParts,
                                                              final StringBuilder strBuffer) {
        Map<String, Long> requiredPartMap = new HashMap<>();
        ParamCheckResult retResult = new ParamCheckResult();
        if (csmType != ConsumeType.CONSUME_BAND) {
            retResult.setCheckData(requiredPartMap);
            return retResult;
        }
        if (TStringUtils.isBlank(requiredParts)) {
            retResult.setCheckData(requiredPartMap);
            return retResult;
        }
        String[] partOffsetItems = requiredParts.trim().split(TokenConstants.ARRAY_SEP);
        for (String partOffset : partOffsetItems) {
            String[] partKeyVal = partOffset.split(TokenConstants.EQ);
            if (partKeyVal.length == 1) {
                retResult.setCheckResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        strBuffer.append("[Parameter error] unformatted Partition-Offset value : ")
                                .append(partOffset).append(" must be aa:bbb:ccc=val1,ddd:eee:ff=val2").toString());
                return retResult;
            }
            String[] partKeyItems = partKeyVal[0].trim().split(TokenConstants.ATTR_SEP);
            if (partKeyItems.length != 3) {
                retResult.setCheckResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        strBuffer.append("[Parameter error] unformatted Partition-Offset value : ")
                                .append(partOffset).append(" must be aa:bbb:ccc=val1,ddd:eee:ff=val2").toString());
                return retResult;
            }
            if (!reqTopicSet.contains(partKeyItems[1].trim())) {
                retResult.setCheckResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        strBuffer.append("[Parameter error] wrong offset reset for unsubscribed topic: reset item is ")
                                .append(partOffset).append(", request topicList are ")
                                .append(reqTopicSet.toString()).toString());
                return retResult;
            }
            try {
                requiredPartMap.put(partKeyVal[0].trim(), Long.parseLong(partKeyVal[1].trim()));
            } catch (Throwable ex) {
                retResult.setCheckResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        strBuffer.append("[Parameter error] required long type value of ")
                                .append(partOffset).append("' Offset!").toString());
                return retResult;
            }
        }
        retResult.setCheckData(requiredPartMap);
        return retResult;
    }

    /**
     * Check the validity of consumer parameters
     *  which specify partition boostrap Offset and use server-side balancing.
     *
     * @param inConsumerInfo      the consumer information
     * @param masterConfig        the master configure
     * @param defMetaDataService  the cluster meta information
     * @param brokerRunManager    the broker running information
     * @param strBuffer           the string buffer used to construct the result
     * @return the check result
     */
    public static ParamCheckResult checkConsumerInputInfo(ConsumerInfo inConsumerInfo,
                                                          MasterConfig masterConfig,
                                                          MetaDataService defMetaDataService,
                                                          BrokerRunManager brokerRunManager,
                                                          StringBuilder strBuffer) throws Exception {
        ParamCheckResult retResult = new ParamCheckResult();
        if (!inConsumerInfo.isRequireBound()) {
            retResult.setCheckData(inConsumerInfo);
            return retResult;
        }
        if (TStringUtils.isBlank(inConsumerInfo.getSessionKey())) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    "[Parameter error] blank value of sessionKey!");
            return retResult;
        }
        inConsumerInfo.setSessionKey(inConsumerInfo.getSessionKey().trim());
        if (inConsumerInfo.getSourceCount() <= 0) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    "[Parameter error] totalSourceCount must over zero!");
            return retResult;
        }
        GroupResCtrlEntity offsetResetGroupEntity =
                defMetaDataService.getGroupCtrlConf(inConsumerInfo.getGroupName());
        if (masterConfig.isStartOffsetResetCheck()) {
            if (offsetResetGroupEntity == null) {
                retResult.setCheckResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        strBuffer.append("[unauthorized subscribe] ConsumeGroup must be ")
                                .append("authorized by administrator before using bound subscribe")
                                .append(", please contact to administrator!").toString());
                strBuffer.delete(0, strBuffer.length());
                return retResult;
            }
        }
        int allowRate = (offsetResetGroupEntity != null
                && offsetResetGroupEntity.getAllowedBrokerClientRate() > 0)
                ? offsetResetGroupEntity.getAllowedBrokerClientRate() : masterConfig.getMaxGroupBrokerConsumeRate();
        int maxBrokerCount =
                brokerRunManager.getSubTopicMaxBrokerCount(inConsumerInfo.getTopicSet());
        int curBClientRate = (int) Math.floor(maxBrokerCount / inConsumerInfo.getSourceCount());
        if (curBClientRate > allowRate) {
            int minClientCnt = maxBrokerCount / allowRate;
            if (maxBrokerCount % allowRate != 0) {
                minClientCnt += 1;
            }
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    strBuffer.append("[Parameter error] System requires at least ")
                            .append(minClientCnt).append(" clients to consume data together, ")
                            .append("please add client resources!").toString());
            return retResult;
        }
        retResult.setCheckData(inConsumerInfo);
        return retResult;
    }

    /**
     * Check the id of broker
     *
     * @param brokerId  the id of broker to be checked
     * @param strBuffer the string buffer used to construct check result
     * @return the check result
     */
    public static ParamCheckResult checkBrokerId(final String brokerId,
                                                 final StringBuilder strBuffer) {
        ParamCheckResult retResult = new ParamCheckResult();
        if (TStringUtils.isBlank(brokerId)) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary brokerId data");
            return retResult;
        }
        String tmpValue = brokerId.trim();
        try {
            retResult.setCheckData(Integer.parseInt(tmpValue));
        } catch (Throwable e) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    strBuffer.append("Parse brokerId to int failure ").append(e.getMessage()).toString());
            strBuffer.delete(0, strBuffer.length());
            return retResult;
        }
        return retResult;
    }

    /**
     * Check the clientID.
     *
     * @param clientId  the client id to be checked
     * @param strBuffer the string used to construct the result
     * @return the check result
     */
    public static ParamCheckResult checkClientId(final String clientId, final StringBuilder strBuffer) {
        return validStringParameter("clientId",
                clientId, TBaseConstants.META_MAX_CLIENT_ID_LENGTH, strBuffer);
    }

    /**
     * Check the hostname.
     *
     * @param hostName  the hostname to be checked.
     * @param strBuffer the string used to construct the result
     * @return the check result
     */
    public static ParamCheckResult checkHostName(final String hostName, final StringBuilder strBuffer) {
        return validStringParameter("hostName",
                hostName, TBaseConstants.META_MAX_CLIENT_HOSTNAME_LENGTH, strBuffer);
    }

    /**
     * Check the group name
     *
     * @param groupName the group name to be checked
     * @param strBuffer the string used to construct the result
     * @return the check result
     */
    public static ParamCheckResult checkGroupName(final String groupName, final StringBuilder strBuffer) {
        return validStringParameter("groupName",
                groupName, TBaseConstants.META_MAX_GROUPNAME_LENGTH, strBuffer);
    }

    private static ParamCheckResult validStringParameter(final String paramName,
                                                         final String paramValue,
                                                         int paramMaxLen,
                                                         final StringBuilder strBuffer) {
        ParamCheckResult retResult = new ParamCheckResult();
        if (TStringUtils.isBlank(paramValue)) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    strBuffer.append("Request miss necessary ")
                            .append(paramName).append(" data!").toString());
            strBuffer.delete(0, strBuffer.length());
            return retResult;
        }
        String tmpValue = paramValue.trim();
        if (tmpValue.length() > paramMaxLen) {
            retResult.setCheckResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    strBuffer.append(paramName)
                            .append("'s length over max value, required max length is ")
                            .append(paramMaxLen).toString());
            strBuffer.delete(0, strBuffer.length());
            return retResult;
        }
        retResult.setCheckData(tmpValue);
        return retResult;
    }

    /**
     * Check the string parameter
     *
     * @param fieldDef  the field to be checked
     * @param paramValue the field value to be checked
     * @param strBuffer the string pool construct the result
     * @param result    the checked result
     * @return result success or failure
     */
    public static boolean getStringParameter(WebFieldDef fieldDef,
                                             String paramValue,
                                             StringBuilder strBuffer,
                                             ProcessResult result) {
        if (TStringUtils.isBlank(paramValue)) {
            result.setFailResult(strBuffer.append("Request miss necessary ")
                    .append(fieldDef.name).append(" data!").toString());
            strBuffer.delete(0, strBuffer.length());
            return result.isSuccess();
        }
        String tmpValue = paramValue.trim();
        if (tmpValue.length() > fieldDef.valMaxLen) {
            result.setFailResult(strBuffer.append(fieldDef.name)
                    .append("'s length over max value, allowed max length is ")
                    .append(fieldDef.valMaxLen).toString());
            strBuffer.delete(0, strBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(tmpValue);
        return result.isSuccess();
    }

    /**
     * Check the topic name.
     *
     * @param topicName      the topic name to check
     * @param metadataManager the metadata manager which contains topic information
     * @param strBuffer      the string buffer used to construct the check result
     * @param result         the checked result
     * @return the check result
     */
    public static boolean getTopicNameParameter(String topicName,
                                                MetadataManager metadataManager,
                                                StringBuilder strBuffer,
                                                ProcessResult result) {
        if (!getStringParameter(WebFieldDef.TOPICNAME,
                topicName, strBuffer, result)) {
            return result.isSuccess();
        }
        String tmpValue = (String) result.getRetData();
        if (metadataManager.getTopicMetadata(tmpValue) == null) {
            result.setFailResult(TErrCodeConstants.FORBIDDEN,
                    strBuffer.append(WebFieldDef.TOPICNAME.name)
                            .append(" ").append(tmpValue)
                            .append(" not existed, please check your configure").toString());
            strBuffer.delete(0, strBuffer.length());
        }
        return result.isSuccess();
    }

    /**
     * Check the existing topic name info
     *
     * @param isProduce      whether to call on the production side
     * @param topicName      the topic name to be checked.
     * @param partitionId    the partition ID where the topic locates
     * @param metadataManager the metadata manager which contains topic information
     * @param strBuffer      the string buffer used to construct the check result
     * @param result         the checked result
     * @return the check result
     */
    public static boolean getTopicNamePartIdInfo(boolean isProduce,
                                                 String topicName, int partitionId,
                                                 MetadataManager metadataManager,
                                                 StringBuilder strBuffer,
                                                 ProcessResult result) {
        // Check and get topic name
        if (!getStringParameter(WebFieldDef.TOPICNAME,
                topicName, strBuffer, result)) {
            return result.isSuccess();
        }
        String tgtTopicName = (String) result.getRetData();
        // Check if it is an allowed calling relationship
        if (isProduce) {
            if (tgtTopicName.equals(TServerConstants.OFFSET_HISTORY_NAME)) {
                result.setFailResult(TErrCodeConstants.FORBIDDEN,
                        strBuffer.append(WebFieldDef.TOPICNAME.name)
                                .append(" ").append(tgtTopicName)
                                .append(" does not allow producers to send data!").toString());
                strBuffer.delete(0, strBuffer.length());
                return result.isSuccess();
            }
        }
        TopicMetadata topicMetadata = metadataManager.getTopicMetadata(tgtTopicName);
        if (topicMetadata == null) {
            result.setFailResult(TErrCodeConstants.FORBIDDEN,
                    strBuffer.append(WebFieldDef.TOPICNAME.name)
                            .append(" ").append(tgtTopicName)
                            .append(" not existed, please check your configure").toString());
            strBuffer.delete(0, strBuffer.length());
            return result.isSuccess();
        }
        if (metadataManager.isClosedTopic(tgtTopicName)) {
            result.setFailResult(TErrCodeConstants.FORBIDDEN,
                    strBuffer.append(WebFieldDef.TOPICNAME.name)
                            .append(" ").append(tgtTopicName)
                            .append(" has been closed").toString());
            strBuffer.delete(0, strBuffer.length());
            return result.isSuccess();
        }
        int realPartition = partitionId < TBaseConstants.META_STORE_INS_BASE
                ? partitionId : partitionId % TBaseConstants.META_STORE_INS_BASE;
        if ((realPartition < 0) || (realPartition >= topicMetadata.getNumPartitions())) {
            result.setFailResult(TErrCodeConstants.FORBIDDEN,
                    strBuffer.append(WebFieldDef.PARTITIONID.name)
                            .append(" ").append(tgtTopicName).append("-").append(partitionId)
                            .append(" not existed, please check your configure").toString());
            strBuffer.delete(0, strBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(topicMetadata);
        return result.isSuccess();
    }
}
