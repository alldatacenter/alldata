/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// #include <cstdlib>
#include "partition_router.h"

#include <climits>
#include <iostream>

namespace tubemq {

RoundRobinPartitionRouter::RoundRobinPartitionRouter() { stepped_counter_.Set(0); }

RoundRobinPartitionRouter::~RoundRobinPartitionRouter() {}

int RoundRobinPartitionRouter::GetPartition(const Message& message,
                                            const std::vector<Partition>& partitions) {
  if (partitions.empty()) return -1;
  const std::string topic = message.GetTopic();

  if (partition_router_map_.count(topic) == 0) {
    std::srand(std::time(0));
    AtomicInteger new_counter(std::rand());
    partition_router_map_[topic] = new_counter;
  }

  int round_partition_index = -1;
  size_t part_size = partitions.size();
  for (size_t i = 0; i < part_size; i++) {
    round_partition_index =
        ((partition_router_map_[topic].IncrementAndGet() & INT_MAX) % part_size);
    if (partitions[round_partition_index].GetDelayTimestamp() < Utils::CurrentTimeMillis()) {
      return round_partition_index;
    }
  }

  return (stepped_counter_.IncrementAndGet() & INT_MAX) % part_size;
}
}  // namespace tubemq
