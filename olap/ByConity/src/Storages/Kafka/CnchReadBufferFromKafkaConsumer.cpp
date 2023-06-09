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

#include <Common/config.h>
#if USE_RDKAFKA

#include <Storages/Kafka/CnchReadBufferFromKafkaConsumer.h>
#include <Storages/Kafka/KafkaCommon.h>

namespace DB
{
    namespace ErrorCodes
    {
        extern const int RDKAFKA_EXCEPTION;
    }

using namespace std::chrono_literals;

CnchReadBufferFromKafkaConsumer::~CnchReadBufferFromKafkaConsumer()
{
    try
    {
        if (current)
            current = Message();

        /// NOTE: see https://github.com/edenhill/librdkafka/issues/2077
        consumer->unsubscribe();
        consumer->unassign();
    }
    catch (...)
    {
        LOG_ERROR(log, "{}(): {}", __func__, getCurrentExceptionMessage(false));
    }

    try
    {
        while (consumer->get_consumer_queue().next_event(50ms));
    }
    catch (...)
    {
        LOG_ERROR(log, "{}(): {}", __func__, getCurrentExceptionMessage(false));
    }
}

cppkafka::TopicPartitionList CnchReadBufferFromKafkaConsumer::getOffsets() const
{
    cppkafka::TopicPartitionList tpl;
    for (const auto & p : offsets)
        tpl.emplace_back(p.first.first, p.first.second, p.second);
    return tpl;
}

void CnchReadBufferFromKafkaConsumer::clearOffsets()
{
    offsets.clear();
}

void CnchReadBufferFromKafkaConsumer::commit()
{
    if (!offsets.empty())
    {
        auto tpl = getOffsets();
        LOG_TRACE(log, "Committing offsets: {}", DB::Kafka::toString(tpl));

        try
        {
            consumer->commit(tpl);
            /// Clear committed offsets to avoid redundant committing
            offsets.clear();
        }
        catch (...)
        {
            /// TODO P0:
            /// Ignore commit exception or the messages would be duplicated
            /// The offsets are kept for next committing
            LOG_ERROR(log, "{}(): {}", __func__, getCurrentExceptionMessage(false));
        }
    }

    /// Still reset current message to free resource
    if (current)
        current = Message();
}

void CnchReadBufferFromKafkaConsumer::subscribe(const Names & topics)
{
    // While we wait for an assignment after subscribtion, we'll poll zero messages anyway.
    // If we're doing a manual select then it's better to get something after a wait, then immediate nothing.
    if (consumer->get_cached_subscription().empty())
    {
        consumer->pause(); // don't accidentally read any messages
        consumer->subscribe(topics);
        consumer->poll(2s);
        consumer->resume();
    }

    reset();
}

void CnchReadBufferFromKafkaConsumer::unsubscribe()
{
    LOG_TRACE(log, "Re-joining claimed consumer after failure");
    /// Need clear recorded offsets to avoid messages loss
    offsets.clear();
    consumer->unsubscribe();
}

void CnchReadBufferFromKafkaConsumer::assign(const cppkafka::TopicPartitionList & topic_partition_list)
{
    if (consumer->get_cached_assignment().empty())
        consumer->assign(topic_partition_list);

    reset();
}

void CnchReadBufferFromKafkaConsumer::unassign()
{
    LOG_TRACE(log, "Re-joining claimed consumer after failure");
    /// Need clear recorded offsets to avoid messages loss
    offsets.clear();
    consumer->unassign();
}


bool CnchReadBufferFromKafkaConsumer::nextImpl()
{
    /// NOTE: ReadBuffer was implemented with an immutable underlying contents in mind.
    ///       If we failed to poll any message once - don't try again.
    ///       Otherwise, the |poll_timeout| expectations get flawn.
    if (stalled)
        return false;

    while (!hasExpired() && read_messages < batch_size && run->load(std::memory_order_relaxed))
    {
        auto new_message = consumer->poll(std::chrono::milliseconds(poll_timeout));
        if (!new_message)
            continue;

        if (auto err = new_message.get_error())
        {
            /// TODO: should throw exception instead
            LOG_ERROR(log, "Consumer error: {}", err.to_string());
            stalled = true;
            throw Exception(err.to_string(), ErrorCodes::RDKAFKA_EXCEPTION);
        }

        /// Get an available message, save it for committing
        current = std::move(new_message);
        read_messages += 1;

        /// The term `position` gives the offset of the next message (i.e. offset of current message + 1)
        /// Record or update this `position` for later committing,
        /// Once committed, the `postition` and the `committed position` would be equal
        offsets[{current.get_topic(), current.get_partition()}] = current.get_offset() + 1;

        const auto & payload = current.get_payload();

        /// Check empty payload to avoid CORE DUMP
        if (!payload || !payload.get_data())
        {
            empty_messages += 1;
            continue;
        }

        read_bytes += payload.get_size();

        /// Here, we got a new message

        // XXX: very fishy place with const casting.
        auto *new_position = reinterpret_cast<char *>(const_cast<unsigned char *>(payload.get_data()));
        BufferBase::set(new_position, payload.get_size(), 0);
        return true;
    }

    /// This buffer/consumer has been expired if reached here
    LOG_TRACE(log, "Stalled. Polled {} messages", read_messages);
    stalled = true;
    return false;
}


void CnchReadBufferFromKafkaConsumer::reset()
{
    create_time = time(nullptr);
    read_messages = 0;
    read_bytes = 0;
    empty_messages = 0;
    stalled = false;
}

bool CnchReadBufferFromKafkaConsumer::hasExpired()
{
    alive_time = time(nullptr);
    return ((alive_time - create_time) * 1000 > expire_timeout);
}

}

#endif
