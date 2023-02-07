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

package org.apache.inlong.sort.standalone.sink.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Test for PartitionerSelector
 */
public class PartitionerSelectorTest {

    private static final String V_INLONG_ID = "testId";
    private static final String V_INLONG_ID_TOPIC = "U_TOPIC_testId";
    private static final String V_LOCALIP = "127.0.0.1";
    private static final int V_PORT = 1234;

    @Test
    public void test() {
        try {
            Node node = new Node(0, V_LOCALIP, V_PORT);
            List<Node> nodes = new ArrayList<>();
            nodes.add(node);
            List<PartitionInfo> partitionInfos = new ArrayList<>();
            partitionInfos.add(new PartitionInfo(V_INLONG_ID_TOPIC, 0,
                    node, new Node[0], new Node[0]));
            partitionInfos.add(new PartitionInfo(V_INLONG_ID_TOPIC, 1,
                    node, new Node[0], new Node[0]));

            PartitionerSelector obj = new PartitionerSelector();
            HashMap<String, Object> configs = new HashMap<>();
            configs.put(ProducerConfig.CLIENT_ID_CONFIG, "clientId");
            obj.configure(configs);
            Cluster cluster = new Cluster("clusterId", nodes, partitionInfos,
                    new HashSet<String>(), new HashSet<String>());
            obj.partition(V_INLONG_ID_TOPIC, null, null, null, null, cluster);
            obj.partition(V_INLONG_ID_TOPIC, "", V_INLONG_ID.getBytes(),
                    V_INLONG_ID.getBytes(), V_INLONG_ID.getBytes(), cluster);
            obj.onNewBatch(V_INLONG_ID_TOPIC, cluster, 0);
            obj.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}