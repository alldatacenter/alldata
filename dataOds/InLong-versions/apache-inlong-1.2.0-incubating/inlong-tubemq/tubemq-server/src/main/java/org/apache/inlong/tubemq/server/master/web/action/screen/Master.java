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

package org.apache.inlong.tubemq.server.master.web.action.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.cluster.ProducerInfo;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corerpc.exception.StandbyException;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.TopicPSInfoManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;
import org.apache.inlong.tubemq.server.master.web.simplemvc.Action;
import org.apache.inlong.tubemq.server.master.web.simplemvc.RequestContext;

public class Master implements Action {

    private final TMaster master;

    public Master(TMaster master) {
        this.master = master;
    }

    @Override
    public void execute(RequestContext requestContext) {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            HttpServletRequest req = requestContext.getReq();
            if (this.master.isStopped()) {
                throw new Exception("Sever is stopping...");
            }
            MetaDataService defMetaDataService =
                    this.master.getMetaDataService();
            if (!defMetaDataService.isSelfMaster()) {
                throw new StandbyException("Please send your request to the master Node.");
            }
            String type = req.getParameter("type");
            if ("consumer".equals(type)) {
                getConsumerListInfo(req, sBuilder);
            } else if ("sub_info".equals(type)) {
                getConsumerSubInfo(req, sBuilder);
            } else if ("producer".equals(type)) {
                getProducerListInfo(req, sBuilder);
            } else if ("broker".equals(type)) {
                innGetBrokerInfo(req, sBuilder, true);
            } else if ("newBroker".equals(type)) {
                innGetBrokerInfo(req, sBuilder, false);
            } else if ("topic_pub".equals(type)) {
                getTopicPubInfo(req, sBuilder);
            } else if ("unbalance_group".equals(type)) {
                getUnbalanceGroupInfo(sBuilder);
            } else {
                sBuilder.append("Unsupported request type : ").append(type);
            }
            requestContext.put("sb", sBuilder.toString());
        } catch (Exception e) {
            requestContext.put("sb", "Bad request from client. " + e.getMessage());
        }
    }

    /**
     * Get consumer list info
     *
     * @param req
     * @param sBuilder
     * @return
     */
    private void getConsumerListInfo(final HttpServletRequest req, StringBuilder sBuilder) {
        ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
        String group = req.getParameter("group");
        if (group != null) {
            int index = 1;
            List<String> consumerViewInfos =
                    consumerHolder.getConsumerViewList(group);
            if (CollectionUtils.isEmpty(consumerViewInfos)) {
                List<String> groupList = consumerHolder.getAllGroupName();
                sBuilder.append("No such group.\n\nCurrent all groups(")
                        .append(groupList.size()).append("):\n");
                for (String currGroup : groupList) {
                    sBuilder.append(currGroup).append("\n");
                }
            } else {
                Collections.sort(consumerViewInfos);
                for (String consumerViewInfo : consumerViewInfos) {
                    if (consumerViewInfo == null) {
                        continue;
                    }
                    sBuilder.append(index).append(". ")
                            .append(consumerViewInfo).append("\n");
                    index++;
                }
            }
        }
    }

    /**
     * Get consumer subscription info
     *
     * @param req
     * @param sBuilder
     * @return
     */
    private void getConsumerSubInfo(final HttpServletRequest req, StringBuilder sBuilder) {
        ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
        String group = req.getParameter("group");
        if (group != null) {
            List<Tuple2<String, Boolean>> consumerList =
                    consumerHolder.getConsumerIdAndTlsInfos(group);
            if (CollectionUtils.isEmpty(consumerList)) {
                List<String> groupList = consumerHolder.getAllGroupName();
                sBuilder.append("No such group.\n\nCurrent all group(")
                        .append(groupList.size()).append("):\n");
                for (String currGroup : groupList) {
                    sBuilder.append(currGroup).append("\n");
                }
            } else {
                sBuilder.append("\n########################## Subscribe Relationship ############################\n\n");
                Map<String, Map<String, Map<String, Partition>>> currentSubInfoMap =
                        master.getCurrentSubInfoMap();
                for (int i = 0; i < consumerList.size(); i++) {
                    Tuple2<String, Boolean> consumer = consumerList.get(i);
                    sBuilder.append("*************** ").append(i + 1)
                            .append(". ").append(consumer.getF0())
                            .append("#isOverTLS=").append(consumer.getF1())
                            .append(" ***************");
                    Map<String, Map<String, Partition>> topicSubMap =
                            currentSubInfoMap.get(consumer.getF0());
                    if (topicSubMap != null) {
                        int totalSize = 0;
                        for (Map.Entry<String, Map<String, Partition>> entry : topicSubMap.entrySet()) {
                            totalSize += entry.getValue().size();
                        }
                        sBuilder.append("(").append(totalSize).append(")\n\n");
                        for (Map.Entry<String, Map<String, Partition>> entry : topicSubMap.entrySet()) {
                            Map<String, Partition> partMap = entry.getValue();
                            if (partMap != null) {
                                for (Partition part : partMap.values()) {
                                    sBuilder.append(consumer.getF0())
                                            .append("#").append(part.toString()).append("\n");
                                }
                            }
                        }
                    }
                    sBuilder.append("\n\n");
                }
            }
        }
    }

    /**
     * Get producer list info
     *
     * @param req
     * @param sBuilder
     * @return
     */
    private void getProducerListInfo(final HttpServletRequest req, StringBuilder sBuilder) {
        String producerId = req.getParameter("id");
        if (producerId != null) {
            ProducerInfo producer = master.getProducerHolder().getProducerInfo(producerId);
            if (producer != null) {
                sBuilder.append(producer.toString());
            } else {
                sBuilder.append("No such producer!");
            }
        } else {
            String topic = req.getParameter("topic");
            if (topic != null) {
                TopicPSInfoManager topicPSInfoManager =
                        master.getTopicPSInfoManager();
                Set<String> producerSet =
                        topicPSInfoManager.getTopicPubInfo(topic);
                if (producerSet != null && !producerSet.isEmpty()) {
                    int index = 1;
                    for (String producer : producerSet) {
                        sBuilder.append(index).append(". ").append(producer).append("\n");
                        index++;
                    }
                }
            }
        }
    }

    /**
     * Get broker info
     *
     * @param req
     * @param sBuilder
     * @param isOldRet
     * @return
     */
    private void innGetBrokerInfo(final HttpServletRequest req,
                                           StringBuilder sBuilder, boolean isOldRet) {
        Map<Integer, BrokerInfo> brokerInfoMap = null;
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        String brokerIds = req.getParameter("ids");
        if (TStringUtils.isBlank(brokerIds)) {
            brokerInfoMap = brokerRunManager.getBrokerInfoMap(null);
        } else {
            String[] brokerIdArr = brokerIds.split(",");
            List<Integer> idList = new ArrayList<>(brokerIdArr.length);
            for (String strId : brokerIdArr) {
                idList.add(Integer.parseInt(strId));
            }
            brokerInfoMap = brokerRunManager.getBrokerInfoMap(idList);
        }
        if (brokerInfoMap != null) {
            int index = 1;
            MetaDataService defMetaDataService = master.getMetaDataService();
            for (BrokerInfo broker : brokerInfoMap.values()) {
                sBuilder.append("\n################################## ")
                        .append(index).append(". ").append(broker.toString())
                        .append(" ##################################\n");
                List<TopicInfo> topicInfoList =
                        brokerRunManager.getPubBrokerPushedTopicInfo(broker.getBrokerId());
                Map<String, TopicDeployEntity> topicConfigMap =
                        defMetaDataService.getBrokerTopicConfEntitySet(broker.getBrokerId());
                if (topicConfigMap == null) {
                    for (TopicInfo info : topicInfoList) {
                        sBuilder = info.toStrBuilderString(sBuilder);
                        sBuilder.append("\n");

                    }
                } else {
                    for (TopicInfo info : topicInfoList) {
                        TopicDeployEntity bdbEntity = topicConfigMap.get(info.getTopic());
                        if (bdbEntity == null) {
                            sBuilder = info.toStrBuilderString(sBuilder);
                            sBuilder.append("\n");
                        } else {
                            if (isOldRet) {
                                if (bdbEntity.isValidTopicStatus()) {
                                    sBuilder = info.toStrBuilderString(sBuilder);
                                    sBuilder.append("\n");
                                }
                            } else {
                                sBuilder = info.toStrBuilderString(sBuilder);
                                sBuilder.append(TokenConstants.SEGMENT_SEP)
                                        .append(bdbEntity.getTopicStatusId()).append("\n");
                            }
                        }
                    }
                }
                index++;
            }
        }
    }

    /**
     * Get topic publish info
     *
     * @param req
     * @param sBuilder
     * @return
     */
    private void getTopicPubInfo(final HttpServletRequest req, StringBuilder sBuilder) {
        String topic = req.getParameter("topic");
        Set<String> producerIds =
                master.getTopicPSInfoManager().getTopicPubInfo(topic);
        if (producerIds != null && !producerIds.isEmpty()) {
            for (String producerId : producerIds) {
                sBuilder.append(producerId).append("\n");
            }
        } else {
            sBuilder.append("No producer has publish this topic.");
        }
    }

    /**
     * Get un-balanced group info
     *
     * @param sBuilder
     * @return
     */
    private void getUnbalanceGroupInfo(StringBuilder sBuilder) {
        ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        Map<String, Map<String, Map<String, Partition>>> currentSubInfoMap =
                master.getCurrentSubInfoMap();
        int currPartSize = 0;
        Set<String> topicSet;
        List<Partition> partList;
        List<String> consumerIdList;
        Map<String, Partition> topicSubInfoMap;
        Map<String, Map<String, Partition>> consumerSubInfoMap;
        List<String> groupList = consumerHolder.getAllServerBalanceGroups();
        for (String group : groupList) {
            if (group == null) {
                continue;
            }
            topicSet = consumerHolder.getGroupTopicSet(group);
            for (String topic : topicSet) {
                if (topic == null) {
                    continue;
                }
                currPartSize = 0;
                consumerIdList = consumerHolder.getConsumerIdList(group);
                if (CollectionUtils.isNotEmpty(consumerIdList)) {
                    for (String consumerId : consumerIdList) {
                        if (consumerId == null) {
                            continue;
                        }
                        consumerSubInfoMap = currentSubInfoMap.get(consumerId);
                        if (consumerSubInfoMap != null) {
                            topicSubInfoMap = consumerSubInfoMap.get(topic);
                            if (topicSubInfoMap != null) {
                                currPartSize += topicSubInfoMap.size();
                            }
                        }
                    }
                }
                partList = brokerRunManager.getSubBrokerAcceptSubParts(topic);
                if (currPartSize != partList.size()) {
                    sBuilder.append(group).append(":").append(topic).append("\n");
                }
            }
        }
    }
}
