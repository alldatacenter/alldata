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

package org.apache.inlong.tubemq.client.producer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.cluster.Partition;

public class RoundRobinPartitionRouter implements PartitionRouter {

    private final AtomicInteger steppedCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<String/* topic */, AtomicInteger> partitionRouterMap =
            new ConcurrentHashMap<>();

    @Override
    public Partition getPartition(final Message message, final List<Partition> partitions) throws TubeClientException {
        if (partitions == null || partitions.isEmpty()) {
            throw new TubeClientException(new StringBuilder(512)
                    .append("No available partition for topic: ")
                    .append(message.getTopic()).toString());
        }
        AtomicInteger currRouterCount = partitionRouterMap.get(message.getTopic());
        if (null == currRouterCount) {
            AtomicInteger newCounter = new AtomicInteger(ThreadLocalRandom.current().nextInt());
            currRouterCount = partitionRouterMap.putIfAbsent(message.getTopic(), newCounter);
            if (null == currRouterCount) {
                currRouterCount = newCounter;
            }
        }
        Partition roundPartition = null;
        int partSize = partitions.size();
        for (int i = 0; i < partSize; i++) {
            roundPartition =
                    partitions.get((currRouterCount.incrementAndGet() & Integer.MAX_VALUE) % partSize);
            if (roundPartition != null && roundPartition.getDelayTimeStamp() < System.currentTimeMillis()) {
                return roundPartition;
            }
        }
        return partitions.get((steppedCounter.incrementAndGet() & Integer.MAX_VALUE) % partSize);
    }

}
