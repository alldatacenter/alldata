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

package org.apache.inlong.tubemq.client.producer;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

@PowerMockIgnore("javax.management.*")
public class RoundRobinPartitionRouterTest {

    @Test(expected = TubeClientException.class)
    public void testGetPartitionInvalidInput() throws TubeClientException {
        RoundRobinPartitionRouter router = new RoundRobinPartitionRouter();
        Message message = new Message("test", new byte[]{1, 2, 3});
        List<Partition> partitions = new ArrayList<>();
        router.getPartition(message, partitions);
    }

    @Test
    public void testGetPartition() throws TubeClientException {
        RoundRobinPartitionRouter router = new RoundRobinPartitionRouter();
        Message message = new Message("test", new byte[]{1, 2, 3});
        List<Partition> partitions = new ArrayList<>();
        Partition partition = new Partition(new BrokerInfo("0:127.0.0.1:18080"), "test", 0);
        partitions.add(partition);
        Partition actualPartition = router.getPartition(message, partitions);
        assertEquals(0, actualPartition.getPartitionId());

        partition.setDelayTimeStamp(System.currentTimeMillis() + 10000000);
        actualPartition = router.getPartition(message, partitions);
        assertEquals(0, actualPartition.getPartitionId());
    }
}
