/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <cppkafka/consumer.h>

namespace DB
{


class KafkaConsumer : public cppkafka::Consumer
{
public:
    using cppkafka::Consumer::Consumer;

    cppkafka::Message poll(std::chrono::milliseconds timeout);

    void subscribe(const std::vector<std::string>& topics);
    void unsubscribe();
    void assign(const cppkafka::TopicPartitionList& topic_partitions);
    void unassign();
    void commit(const cppkafka::TopicPartitionList& topic_partitions);
    cppkafka::TopicPartitionList get_offsets_committed(const cppkafka::TopicPartitionList& topic_partitions) const;

    std::vector<std::string> get_subscription() const;
    cppkafka::TopicPartitionList get_assignment() const;

    cppkafka::Topic get_topic(const std::string& name);
    cppkafka::TopicMetadata get_metadata(const cppkafka::Topic& topic) const;


    /***** some useful wrapper functions *****/
    const std::vector<std::string> & get_cached_subscription() const { return cached_subscription; }
    const cppkafka::TopicPartitionList & get_cached_assignment() const { return cached_assignment; }
    bool check_destroyed() const { return is_destroyed; }

private:
    std::vector<std::string> cached_subscription;
    cppkafka::TopicPartitionList cached_assignment;
    mutable bool is_destroyed {false};
};


} // end of namespace DB
