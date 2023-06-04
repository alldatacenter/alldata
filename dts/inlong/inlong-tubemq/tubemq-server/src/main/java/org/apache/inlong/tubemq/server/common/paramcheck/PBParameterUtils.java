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
     * @param strBuff   a string buffer used to construct the result
     * @param result    the process result
     * @return          success or failure
     */
    public static boolean checkProducerTopicList(List<String> reqTopicLst,
            StringBuilder strBuff, ProcessResult result) {
        if (reqTopicLst == null) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary topic field info!");
            return result.isSuccess();
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
                    result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                            strBuff.append("System Topic ")
                                    .append(TServerConstants.OFFSET_HISTORY_NAME)
                                    .append(" does not allow client produce data!").toString());
                    strBuff.delete(0, strBuff.length());
                    return result.isSuccess();
                }
                transTopicList.add(topic);
            }
        }
        if (transTopicList.size() > TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    strBuff.append("Booked topic's count over max value, required max count is ")
                            .append(TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(transTopicList);
        return result.isSuccess();
    }

    /**
     * Check request topic list of consumer
     *
     * @param depTopicSet  the deployed topic set
     * @param reqTopicLst the topic list to be checked.
     * @param strBuff   a string buffer used to construct the result
     * @param result    process result
     * @return the check result
     */
    public static boolean checkConsumerTopicList(Set<String> depTopicSet,
            List<String> reqTopicLst, StringBuilder strBuff, ProcessResult result) {
        if ((reqTopicLst == null) || (reqTopicLst.isEmpty())) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary subscribed topicList data!");
            return result.isSuccess();
        }
        // remove spaces
        Set<String> transTopicSet = new HashSet<>();
        for (String topicItem : reqTopicLst) {
            if (TStringUtils.isBlank(topicItem)) {
                continue;
            }
            transTopicSet.add(topicItem.trim());
        }
        if (transTopicSet.isEmpty()) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Request subscribed topicList data must not Blank!");
            return result.isSuccess();
        }
        // check if exceed max topic count booked
        if (transTopicSet.size() > TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    strBuff.append("Subscribed topicList size over max value, required max count is ")
                            .append(TBaseConstants.META_MAX_BOOKED_TOPIC_COUNT).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Check if the topics all in deployment
        Set<String> invalidTopicSet = new HashSet<>();
        for (String reqTopic : transTopicSet) {
            if (!depTopicSet.contains(reqTopic)) {
                invalidTopicSet.add(reqTopic);
            }
        }
        if (!invalidTopicSet.isEmpty()) {
            result.setFailResult(TErrCodeConstants.TOPIC_NOT_DEPLOYED,
                    strBuff.append("Requested topic [").append(invalidTopicSet)
                            .append("] not deployed!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(transTopicSet);
        return result.isSuccess();
    }

    /**
     * Check the validity of the bootstrap Offset information specified by the consumer.
     *
     * @param csmType        the topic list to be checked.
     * @param reqTopicSet    the subscribed topic set
     * @param requiredParts  the specified partitionKey-bootstrap offset map
     * @param strBuff      the string buffer used to construct the result
     * @return the check result
     */
    public static boolean checkConsumerOffsetSetInfo(ConsumeType csmType, Set<String> reqTopicSet,
            String requiredParts, StringBuilder strBuff, ProcessResult result) {
        Map<String, Long> requiredPartMap = new HashMap<>();
        if (csmType != ConsumeType.CONSUME_BAND) {
            result.setSuccResult(requiredPartMap);
            return result.isSuccess();
        }
        if (TStringUtils.isBlank(requiredParts)) {
            result.setSuccResult(requiredPartMap);
            return result.isSuccess();
        }
        String[] partOffsetItems = requiredParts.trim().split(TokenConstants.ARRAY_SEP);
        for (String partOffset : partOffsetItems) {
            String[] partKeyVal = partOffset.split(TokenConstants.EQ);
            if (partKeyVal.length == 1) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        strBuff.append("[Parameter error] unformatted Partition-Offset value : ")
                                .append(partOffset).append(" must be aa:bbb:ccc=val1,ddd:eee:ff=val2").toString());
                return result.isSuccess();
            }
            String[] partKeyItems = partKeyVal[0].trim().split(TokenConstants.ATTR_SEP);
            if (partKeyItems.length != 3) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        strBuff.append("[Parameter error] unformatted Partition-Offset value : ")
                                .append(partOffset).append(" must be aa:bbb:ccc=val1,ddd:eee:ff=val2").toString());
                return result.isSuccess();
            }
            if (!reqTopicSet.contains(partKeyItems[1].trim())) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        strBuff.append("[Parameter error] wrong offset reset for unsubscribed topic: reset item is ")
                                .append(partOffset).append(", request topicList are ")
                                .append(reqTopicSet).toString());
                return result.isSuccess();
            }
            try {
                requiredPartMap.put(partKeyVal[0].trim(), Long.parseLong(partKeyVal[1].trim()));
            } catch (Throwable ex) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        strBuff.append("[Parameter error] required long type value of ")
                                .append(partOffset).append("' Offset!").toString());
                return result.isSuccess();
            }
        }
        result.setSuccResult(requiredPartMap);
        return result.isSuccess();
    }

    /**
     * Check the validity of consumer parameters
     *  which specify partition boostrap Offset and use server-side balancing.
     *
     * @param inConsumerInfo      the consumer information
     * @param masterConfig        the master configure
     * @param defMetaDataService  the cluster meta information
     * @param brokerRunManager    the broker running information
     * @param strBuff           the string buffer used to construct the result
     * @param result  the process result
     * @return the check result
     */
    public static boolean checkConsumerInputInfo(ConsumerInfo inConsumerInfo,
            MasterConfig masterConfig,
            MetaDataService defMetaDataService,
            BrokerRunManager brokerRunManager,
            StringBuilder strBuff,
            ProcessResult result) throws Exception {
        if (!inConsumerInfo.isRequireBound()) {
            result.setSuccResult(inConsumerInfo);
            return result.isSuccess();
        }
        if (TStringUtils.isBlank(inConsumerInfo.getSessionKey())) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "[Parameter error] blank value of sessionKey!");
            return result.isSuccess();
        }
        inConsumerInfo.setSessionKey(inConsumerInfo.getSessionKey().trim());
        if (inConsumerInfo.getSourceCount() <= 0) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "[Parameter error] totalSourceCount must over zero!");
            return result.isSuccess();
        }
        GroupResCtrlEntity offsetResetGroupEntity =
                defMetaDataService.getGroupCtrlConf(inConsumerInfo.getGroupName());
        if (masterConfig.isStartOffsetResetCheck()) {
            if (offsetResetGroupEntity == null) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        strBuff.append("[unauthorized subscribe] ConsumeGroup must be ")
                                .append("authorized by administrator before using bound subscribe")
                                .append(", please contact to administrator!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
        }
        int allowRate = (offsetResetGroupEntity != null
                && offsetResetGroupEntity.getAllowedBrokerClientRate() > 0)
                        ? offsetResetGroupEntity.getAllowedBrokerClientRate()
                        : masterConfig.getMaxGroupBrokerConsumeRate();
        int maxBrokerCount =
                brokerRunManager.getSubTopicMaxBrokerCount(inConsumerInfo.getTopicSet());
        int curBClientRate = (int) Math.floor(maxBrokerCount / inConsumerInfo.getSourceCount());
        if (curBClientRate > allowRate) {
            int minClientCnt = maxBrokerCount / allowRate;
            if (maxBrokerCount % allowRate != 0) {
                minClientCnt += 1;
            }
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    strBuff.append("[Parameter error] System requires at least ")
                            .append(minClientCnt).append(" clients to consume data together, ")
                            .append("please add client resources!").toString());
            return result.isSuccess();
        }
        result.setSuccResult(inConsumerInfo);
        return result.isSuccess();
    }

    /**
     * Check the id of broker
     *
     * @param brokerId  the id of broker to be checked
     * @param strBuff the string buffer used to construct check result
     * @param result  the process result
     * @return the check result
     */
    public static boolean checkBrokerId(String brokerId,
            StringBuilder strBuff, ProcessResult result) {
        if (TStringUtils.isBlank(brokerId)) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary brokerId data");
            return result.isSuccess();
        }
        String tmpValue = brokerId.trim();
        try {
            result.setSuccResult(Integer.parseInt(tmpValue));
        } catch (Throwable e) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    strBuff.append("Parse brokerId to int failure ").append(e.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    /**
     * Check the clientID.
     *
     * @param clientId  the client id to be checked
     * @param strBuff the string used to construct the result
     * @param result  process result
     * @return the check result
     */
    public static boolean checkClientId(String clientId,
            StringBuilder strBuff, ProcessResult result) {
        return PBParameterUtils.getStringParameter(
                WebFieldDef.CLIENTID, clientId, strBuff, result);
    }

    /**
     * Check the hostname.
     *
     * @param hostName  the hostname to be checked.
     * @param strBuff the string used to construct the result
     * @param result  the process result
     * @return the check result
     */
    public static boolean checkHostName(String hostName,
            StringBuilder strBuff, ProcessResult result) {
        return PBParameterUtils.getStringParameter(
                WebFieldDef.HOSTNAME, hostName, strBuff, result);
    }

    /**
     * Check the group name
     *
     * @param groupName the group name to be checked
     * @param strBuff the string used to construct the result
     * @param result   the process result
     * @return the check result
     */
    public static boolean checkGroupName(String groupName,
            StringBuilder strBuff, ProcessResult result) {
        return PBParameterUtils.getStringParameter(
                WebFieldDef.GROUPNAME, groupName, strBuff, result);
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
            String paramValue, StringBuilder strBuffer, ProcessResult result) {
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
            MetadataManager metadataManager, StringBuilder strBuffer, ProcessResult result) {
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
                ? partitionId
                : partitionId % TBaseConstants.META_STORE_INS_BASE;
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
