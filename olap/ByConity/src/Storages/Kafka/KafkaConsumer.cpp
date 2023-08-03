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

#include <Storages/Kafka/KafkaConsumer.h>

#include <Common/Exception.h>
#include <cppkafka/topic.h>
#include <cppkafka/topic_partition.h>
#include <cppkafka/topic_partition_list.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int RDKAFKA_EXCEPTION;
}

using namespace cppkafka;

inline bool is_serious_err(cppkafka::Error error)
{
    auto && ec = error.get_error();
    return ec == RD_KAFKA_RESP_ERR__TRANSPORT
        || ec == RD_KAFKA_RESP_ERR__DESTROY
        || ec == RD_KAFKA_RESP_ERR__AUTHENTICATION
        || ec == RD_KAFKA_RESP_ERR_TOPIC_AUTHORIZATION_FAILED
        || ec == RD_KAFKA_RESP_ERR_GROUP_AUTHORIZATION_FAILED
        || ec == RD_KAFKA_RESP_ERR_CLUSTER_AUTHORIZATION_FAILED;
}

#define exception_wrapper(_s)    \
    try { _s }                   \
    catch (const cppkafka::HandleException & _e) { \
        if (is_serious_err(_e.get_error())) \
            this->is_destroyed = true; \
        throw DB::Exception(std::string(__func__) + "(): " + _e.what(), ErrorCodes::RDKAFKA_EXCEPTION);   \
    } catch (const cppkafka::Exception & _e) { \
        throw DB::Exception(std::string(__func__) + "(): " + _e.what(), ErrorCodes::RDKAFKA_EXCEPTION);   \
    }

cppkafka::Message KafkaConsumer::poll(std::chrono::milliseconds timeout)
{
    auto message = cppkafka::Consumer::poll(timeout);
    if (message && message.get_error())
    {
        if (is_serious_err(message.get_error()))
            this->is_destroyed = true;

        throw Exception("poll(): " + message.get_error().to_string(), ErrorCodes::RDKAFKA_EXCEPTION);
    }
    return message;
}

void KafkaConsumer::subscribe(const std::vector<std::string>& topics)
{
    exception_wrapper({
        cppkafka::Consumer::subscribe(topics);
        cached_subscription = topics;
    })

}

void KafkaConsumer::unsubscribe()
{
    cached_subscription.clear();

    exception_wrapper( return cppkafka::Consumer::unsubscribe(); )
}

void KafkaConsumer::assign(const TopicPartitionList& topic_partitions)
{
    exception_wrapper({
        cppkafka::Consumer::assign(topic_partitions);
        cached_assignment = topic_partitions;
    })
}

void KafkaConsumer::unassign()
{
    cached_assignment.clear();

    exception_wrapper( return cppkafka::Consumer::unassign(); )
}

void KafkaConsumer::commit(const TopicPartitionList& topic_partitions)
{
    exception_wrapper( return cppkafka::Consumer::commit(topic_partitions); )
}

TopicPartitionList KafkaConsumer::get_offsets_committed(const TopicPartitionList& topic_partitions) const
{
    exception_wrapper( return cppkafka::Consumer::get_offsets_committed(topic_partitions); )
}

std::vector<std::string> KafkaConsumer::get_subscription() const
{
    exception_wrapper( return cppkafka::Consumer::get_subscription(); )
}

TopicPartitionList KafkaConsumer::get_assignment() const
{
    exception_wrapper( return cppkafka::Consumer::get_assignment(); )
}

Topic KafkaConsumer::get_topic(const std::string& name)
{
    exception_wrapper( return cppkafka::Consumer::get_topic(name); )
}

TopicMetadata KafkaConsumer::get_metadata(const Topic& topic) const
{
    exception_wrapper( return cppkafka::Consumer::get_metadata(topic); )
}

} // end of namespace DB
