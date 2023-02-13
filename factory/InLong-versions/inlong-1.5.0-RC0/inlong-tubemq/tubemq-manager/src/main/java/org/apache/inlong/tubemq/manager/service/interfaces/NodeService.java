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

package org.apache.inlong.tubemq.manager.service.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.node.dto.MasterDto;
import org.apache.inlong.tubemq.manager.controller.node.request.AddTopicReq;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneBrokersReq;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneTopicReq;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.service.TopicFuture;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpBrokerInfoList;

public interface NodeService {

    /**
     * query broker status
     *
     * @param masterEntry
     * @return
     */
    TubeHttpBrokerInfoList requestBrokerStatus(MasterEntry masterEntry);

    /**
     * clone brokers with topic in it
     *
     * @param req
     * @return
     *
     * @throws Exception exception
     */
    TubeMQResult cloneBrokersWithTopic(CloneBrokersReq req) throws Exception;

    /**
     * add topics to brokers
     *
     * @param masterEntry
     * @param brokerIds
     * @param addTopicReqs
     * @return
     */
    TubeMQResult addTopicsToBrokers(MasterEntry masterEntry, List<Integer> brokerIds,
            List<AddTopicReq> addTopicReqs);

    /**
     * add one topic to brokers
     *
     * @param req
     * @param masterEntry
     * @return
     *
     * @throws Exception exception
     */
    TubeMQResult addTopicToBrokers(AddTopicReq req, MasterEntry masterEntry) throws Exception;

    /**
     * config topics to brokers with max brokers limit
     *
     * @param masterEntry
     * @param topics
     * @param brokerList
     * @param maxBrokers
     * @return
     */
    boolean configBrokersForTopics(MasterEntry masterEntry,
            Set<String> topics, List<Integer> brokerList, int maxBrokers);

    void handleReloadBroker(MasterEntry masterEntry, List<Integer> needReloadList, ClusterEntry clusterEntry);

    /**
     * update broker status
     * @param clusterId
     * @param pendingTopic
     */
    void updateBrokerStatus(int clusterId, Map<String, TopicFuture> pendingTopic);

    void close() throws IOException;

    /**
     * clone topic to brokers
     *
     * @param req
     * @return
     *
     * @throws Exception exception
     */
    TubeMQResult cloneTopicToBrokers(CloneTopicReq req) throws Exception;

    /**
     * add one node to node repository
     *
     * @param masterEntry
     * @return
     */
    void addNode(MasterEntry masterEntry);

    /**
     * modify master node
     *
     * @param masterDto
     * @return
     */
    TubeMQResult modifyMasterNode(MasterDto masterDto);
}
