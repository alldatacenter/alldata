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

package org.apache.inlong.tubemq.server.master.nodemanage.nodebroker;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartResponseM2B;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterResponseM2B;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;

/*
 * Broker operation management class
 */
public interface BrokerRunManager {

    void updBrokerStaticInfo(Map<Integer, BrokerConfEntity> brokerConfMap);

    void updBrokerStaticInfo(BrokerConfEntity entity);

    Tuple2<Long, Map<Integer, String>> getBrokerStaticInfo(boolean isOverTLS);

    void delBrokerStaticInfo(int brokerId);

    boolean brokerRegister2M(String clientId, BrokerInfo brokerInfo,
                             long reportConfigId, int reportCheckSumId,
                             boolean isTackData, String repBrokerConfInfo,
                             List<String> repTopicConfInfo, boolean isOnline,
                             boolean isOverTLS, StringBuilder sBuffer,
                             ProcessResult result);

    boolean brokerHeartBeat2M(int brokerId, long reportConfigId, int reportCheckSumId,
                              boolean isTackData, String repBrokerConfInfo,
                              List<String> repTopicConfInfo,
                              boolean isTackRmvInfo, List<String> removedTopics,
                              int rptReadStatus, int rptWriteStatus, boolean isOnline,
                              StringBuilder sBuffer, ProcessResult result);

    boolean brokerClose2M(int brokerId, StringBuilder sBuffer, ProcessResult result);

    boolean releaseBrokerRunInfo(int brokerId, String blockId, boolean isTimeout);

    BrokerRunStatusInfo getBrokerRunStatusInfo(int brokerId);

    Tuple2<Boolean, Boolean> getBrokerPublishStatus(int brokerId);

    Tuple3<ManageStatus, String, Map<String, String>> getBrokerMetaConfigInfo(int brokerId);

    void setRegisterDownConfInfo(int brokerId, StringBuilder sBuffer,
                                 RegisterResponseM2B.Builder builder);

    void setHeatBeatDownConfInfo(int brokerId, StringBuilder sBuffer,
                                 HeartResponseM2B.Builder builder);

    BrokerInfo getBrokerInfo(int brokerId);

    Map<Integer, BrokerInfo> getBrokerInfoMap(List<Integer> brokerIds);

    boolean updBrokerCsmConfInfo(int brokerId,
                                 ManageStatus mngStatus,
                                 Map<String, TopicInfo> topicInfoMap);

    void updBrokerPrdConfInfo(int brokerId,
                              ManageStatus mngStatus,
                              Map<String, TopicInfo> topicInfoMap);

    BrokerAbnHolder getBrokerAbnHolder();

    Map<String, String> getPubBrokerAcceptPubPartInfo(Set<String> topicSet);

    int getSubTopicMaxBrokerCount(Set<String> topicSet);

    Map<String, Partition> getSubBrokerAcceptSubParts(Set<String> topicSet);

    List<Partition> getSubBrokerAcceptSubParts(String topic);

    TopicInfo getPubBrokerTopicInfo(int brokerId, String topic);

    List<TopicInfo> getPubBrokerPushedTopicInfo(int brokerId);

}
