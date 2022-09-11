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

package org.apache.inlong.tubemq.server.master.balance;

import java.util.List;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;

public interface LoadBalancer {

    Map<String, Map<String, List<Partition>>> balanceCluster(
            Map<String, Map<String, Map<String, Partition>>> clusterState,
            ConsumerInfoHolder consumerHolder,
            BrokerRunManager brokerRunManager,
            List<String> groups,
            MetaDataService defMetaDataService,
            StringBuilder sBuilder);

    Map<String, Map<String, Map<String, Partition>>> resetBalanceCluster(
            Map<String, Map<String, Map<String, Partition>>> clusterState,
            ConsumerInfoHolder consumerHolder,
            BrokerRunManager brokerRunManager,
            List<String> groups,
            MetaDataService defMetaDataService,
            final StringBuilder sBuilder);

    Map<String, Map<String, List<Partition>>> bukAssign(ConsumerInfoHolder consumerHolder,
                                                        BrokerRunManager brokerRunManager,
                                                        List<String> groups,
                                                        MetaDataService defMetaDataService,
                                                        StringBuilder sBuilder);

    Map<String, Map<String, Map<String, Partition>>> resetBukAssign(ConsumerInfoHolder consumerHolder,
                                                                    BrokerRunManager brokerRunManager,
                                                                    List<String> groups,
                                                                    MetaDataService defMetaDataService,
                                                                    StringBuilder sBuilder);

    Map<String, List<Partition>> roundRobinAssignment(List<Partition> partitions,
                                                      List<String> consumers);

    ConsumerInfo randomAssignment(List<ConsumerInfo> servers);
}
