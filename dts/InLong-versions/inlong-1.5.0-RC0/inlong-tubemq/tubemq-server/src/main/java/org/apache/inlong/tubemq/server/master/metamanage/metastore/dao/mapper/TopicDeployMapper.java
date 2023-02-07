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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;

public interface TopicDeployMapper extends AbstractMapper {

    /**
     * Add a new topic deploy configure info from store
     *
     * @param entity   the record to be added
     * @param strBuff  the string buffer
     * @param result   the process result
     * @return         whether success
     */
    boolean addTopicDeployConf(TopicDeployEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Update the topic deploy configure info from store
     *
     * @param entity   the record to be updated
     * @param strBuff  the string buffer
     * @param result   the process result
     * @return         whether success
     */
    boolean updTopicDeployConf(TopicDeployEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Update topic deploy status info from store
     *
     * @param opEntity    the operator info
     * @param brokerId    the broker id
     * @param topicName   the topic name
     * @param topicStatus the new topic status
     * @param strBuff     the string buffer
     * @param result      the process result
     * @return            whether success
     */
    boolean updTopicDeployStatus(BaseEntity opEntity, int brokerId,
            String topicName, TopicStatus topicStatus,
            StringBuilder strBuff, ProcessResult result);

    /**
     * delete topic deploy configure info from store
     *
     * @param recordKey  the record key to be deleted
     * @param strBuff    the string buffer
     * @param result     the process result
     * @return           whether success
     */
    boolean delTopicDeployConf(String recordKey, StringBuilder strBuff, ProcessResult result);

    /**
     * delete topic deploy configure info from store
     *
     * @param brokerId   the broker id to be deleted
     * @param strBuff    the string buffer
     * @param result     the process result
     * @return           whether success
     */
    boolean delTopicConfByBrokerId(Integer brokerId, StringBuilder strBuff, ProcessResult result);

    boolean hasConfiguredTopics(int brokerId);

    boolean isTopicDeployed(String topicName);

    List<TopicDeployEntity> getTopicConf(TopicDeployEntity qryEntity);

    TopicDeployEntity getTopicConf(int brokerId, String topicName);

    TopicDeployEntity getTopicConfByeRecKey(String recordKey);

    /**
     * Get broker topic entity, if query entity is null, return all topic entity
     *
     * @param topicNameSet need query topic name set
     * @param brokerIdSet  need query broker id set
     * @param qryEntity   must not null
     * @return  topic deploy info by topicName's key
     */
    Map<String, List<TopicDeployEntity>> getTopicConfMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet,
            TopicDeployEntity qryEntity);

    Map<Integer, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet);

    Map<String/* topicName */, List<TopicDeployEntity>> getTopicConfMapByTopicAndBrokerIds(
            Set<String> topicSet, Set<Integer> brokerIdSet);

    Map<String, TopicDeployEntity> getConfiguredTopicInfo(int brokerId);

    Map<Integer/* brokerId */, Set<String>> getConfiguredTopicInfo(Set<Integer> brokerIdSet);

    Map<String/* topicName */, Map<Integer, String>> getTopicBrokerInfo(Set<String> topicNameSet);

    Set<Integer> getDeployedBrokerIdByTopic(Set<String> topicNameSet);

    Set<String> getDeployedTopicSet();

}
