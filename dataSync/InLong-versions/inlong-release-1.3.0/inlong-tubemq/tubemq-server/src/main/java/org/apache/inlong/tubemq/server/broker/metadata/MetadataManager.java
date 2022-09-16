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

package org.apache.inlong.tubemq.server.broker.metadata;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlRuleHandler;

/**
 * Metadata's management interface.
 */
public interface MetadataManager {
    void close(long waitTimeMs);

    void updateBrokerTopicConfigMap(long newBrokerMetaConfId,
                                    int newConfCheckSumId,
                                    String newBrokerDefMetaConfInfo,
                                    List<String> newTopicMetaConfInfoLst,
                                    boolean isForce,
                                    StringBuilder sb);

    boolean updateBrokerRemoveTopicMap(boolean isTakeRemoveTopics,
                                       List<String> rmvTopicMetaConfInfoLst,
                                       StringBuilder sb);

    void addPropertyChangeListener(String propertyName,
                                   PropertyChangeListener listener);

    List<String> getTopics();

    TopicMetadata getTopicMetadata(String topic);

    BrokerDefMetadata getBrokerDefMetadata();

    FlowCtrlRuleHandler getFlowCtrlRuleHandler();

    int getNumPartitions(String topic);

    int getNumTopicStores(String topic);

    long getBrokerMetadataConfId();

    int getBrokerConfCheckSumId();

    String getBrokerDefMetaConfInfo();

    List<String> getTopicMetaConfInfoLst();

    List<String> getHardRemovedTopics();

    Map<String, TopicMetadata> getRemovedTopicConfigMap();

    Integer getClosedTopicStatusId(String topic);

    boolean isClosedTopic(String topic);

    boolean isBrokerMetadataChanged();

    long getLastRptBrokerMetaConfId();

    void setLastRptBrokerMetaConfId(long rptBrokerMetaConfId);

    String getDefDeletePolicy();

    String getTopicDeletePolicy(String topic);

    Map<String, TopicMetadata> getTopicConfigMap();
}
