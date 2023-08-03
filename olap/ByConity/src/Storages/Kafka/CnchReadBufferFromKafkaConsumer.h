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
#include <Common/config.h>
#if USE_RDKAFKA

#include <Core/Names.h>
#include <Core/Types.h>
#include <IO/DelimitedReadBuffer.h>
#include <Storages/Kafka/KafkaConsumer.h>

#include <common/logger_useful.h>
#include <cppkafka/cppkafka.h>

namespace DB
{

using BufferPtr = std::shared_ptr<DelimitedReadBuffer>;
using ConsumerPtr = std::shared_ptr<KafkaConsumer>;

struct PairHash
{
    template <class T1, class T2>
    std::size_t operator() (const std::pair<T1, T2> & p) const
    {
        static std::hash<T1> t1;
        static std::hash<T2> t2;
        return t1(p.first) ^ t2(p.second);
    }
};

class CnchReadBufferFromKafkaConsumer : public ReadBuffer
{
    using Message = cppkafka::Message;

public:
    CnchReadBufferFromKafkaConsumer(
        ConsumerPtr consumer_,
        const String & logger_name,
        size_t max_batch_size,
        size_t poll_timeout_,
        size_t expire_timeout_,
        std::atomic_bool *run_)
        : ReadBuffer(nullptr, 0)
        , consumer(consumer_)
        , log(&Poco::Logger::get(logger_name))
        , batch_size(max_batch_size)
        , poll_timeout(poll_timeout_)
        , expire_timeout(expire_timeout_)
        , run(run_)
        , create_time(time(nullptr))
    {
    }

    ~CnchReadBufferFromKafkaConsumer() override;

    void commit(); // Commit all processed messages.
    void subscribe(const Names & topics); // Subscribe internal consumer to topics.
    void unsubscribe(); // Unsubscribe internal consumer in case of failure.
    void assign(const cppkafka::TopicPartitionList & topic_partition_list);
    void unassign();

    auto pollTimeout() const { return poll_timeout; }

    ConsumerPtr & getConsumer() { return consumer; }

    void reset();

    cppkafka::TopicPartitionList getOffsets() const;
    void clearOffsets();

    size_t getReadMessages() const { return read_messages; }
    size_t getReadBytes() const { return read_bytes; }
    size_t getEmptyMessages() const { return empty_messages; }
    size_t getCreateTime() const { return create_time; }
    size_t getAliveTime() const { return alive_time; }

    // Return values for the message that's being read.
    const Message & currentMessage() const { return current; }
    String currentTopic() const { return current.get_topic(); }
    String currentKey() const { return current.get_key(); }
    auto currentOffset() const { return current.get_offset(); }
    auto currentPartition() const {return current.get_partition();}
    String currentContent() const {return current.get_payload();}

private:
    ConsumerPtr consumer;
    Poco::Logger * log;
    size_t batch_size;
    size_t poll_timeout;
    size_t expire_timeout;
    std::atomic_bool * run;

    size_t create_time;
    size_t alive_time {0};
    bool stalled = false;

    Message current;

    size_t read_messages {0};
    size_t empty_messages {0};
    size_t read_bytes {0};

    std::unordered_map<
        std::pair<std::string, std::uint64_t>,
        std::int64_t,
        PairHash> offsets;

    bool nextImpl() override;
    bool hasExpired();
};

}

#endif
