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

#include <Catalog/Catalog.h>
///#include <DaemonManager/DaemonManagerClient.h>
#include <Interpreters/DatabaseCatalog.h>
#include <Interpreters/InterpreterSystemQuery.h>
#include <Parsers/parseQuery.h>
#include <Parsers/ParserSystemQuery.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/Kafka/KafkaCommon.h>
#include <Storages/Kafka/KafkaConsumer.h>
#include <Storages/Kafka/CnchKafkaOffsetManager.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}
constexpr auto RESET_CONSUME_OFFSET_BREAK_TIME = 1;

CnchKafkaOffsetManager::CnchKafkaOffsetManager(ContextPtr context, StorageID &storage_id)
    : global_context(context->getGlobalContext())
    , log(&Poco::Logger::get(storage_id.getFullTableName() + " (CnchKafkaOffsetManager)"))
{
    storage = DatabaseCatalog::instance().getTable(storage_id,  global_context);
    kafka_table = dynamic_cast<StorageCnchKafka *>(storage.get());
    if (!kafka_table)
        throw Exception("CnchKafka is expected for CnchKafkaOffsetManager, but got " + storage_id.getNameForLogs(), ErrorCodes::LOGICAL_ERROR);
}

cppkafka::TopicPartitionList CnchKafkaOffsetManager::createTopicPartitionList(uint64_t timestamp = 0)
{
    auto conf = Kafka::createConsumerConfiguration(global_context, kafka_table->getStorageID(),
                                                   kafka_table->getTopics(), kafka_table->getSettings());
    auto tool_consumer = std::make_shared<KafkaConsumer>(conf);

    /// Create TopicPartitionList from topic metadata instead of Catalog,
    /// as Catalog may have no data for some newly tables
    cppkafka::TopicPartitionList tpl;
    for (const auto & topic_name : kafka_table->getTopics())
    {
        auto topic = tool_consumer->get_topic(topic_name);
        auto topic_metadata = tool_consumer->get_metadata(topic);
        auto partition_num = topic_metadata.get_partitions().size();
        LOG_DEBUG(log, "Got {} partitions for topic {} ", partition_num, topic_name);

        for (size_t partition = 0; partition < partition_num; ++partition)
            tpl.emplace_back(topic_name, partition);  /// will use RD_KAFKA_OFFSET_INVALID as default offset
    }

    /// Try to get offsets with given timestamp
    if (timestamp > 0)
    {
        KafkaConsumer::TopicPartitionsTimestampsMap tp_ts_map;
        for (const auto & tp : tpl)
            tp_ts_map.emplace(tp, timestamp);

        LOG_DEBUG(log, "Try to get offsets with timestamp {}", timestamp);
        tpl = tool_consumer->get_offsets_for_times(tp_ts_map);
    }

    return tpl;
}

bool CnchKafkaOffsetManager::offsetValueIsSpecialPosition(int64_t value)
{
    return value == RD_KAFKA_OFFSET_BEGINNING
            || value == RD_KAFKA_OFFSET_END
            || value == RD_KAFKA_OFFSET_INVALID
            || value == RD_KAFKA_OFFSET_STORED;
}

void CnchKafkaOffsetManager::resetOffsetImpl(const cppkafka::TopicPartitionList & tpl)
{
    /// FIXME: Start / Stop consume by Daemon-Manager when it is ready
    ///auto daemon_manager = global_context.getDaemonManagerClient();
    /// Firstly, stop consumers
    ///if (kafka_table->tableIsActive())
    ///    daemon_manager->controlDaemonJob(kafka_table->getStorageID(), CnchBGThreadType::Consumer, Protos::ControlDaemonJobReq::Stop);
    std::this_thread::sleep_for(std::chrono::seconds(RESET_CONSUME_OFFSET_BREAK_TIME));
    SCOPE_EXIT({
               std::this_thread::sleep_for(std::chrono::seconds(RESET_CONSUME_OFFSET_BREAK_TIME));
               try
               {
                   ///if (kafka_table->tableIsActive())
                    ///   daemon_manager->controlDaemonJob(kafka_table->getStorageID(), CnchBGThreadType::Consumer,
                    ///                                    Protos::ControlDaemonJobReq::Start);
               }
               catch (...)
               {
                   tryLogCurrentException(log, "Failed to restart consume after reseting offsets");
               }
    });

    /// Secondly, commit offsets in transaction
    auto target_table = kafka_table->tryGetTargetTable();
    auto & txn_co = global_context->getCnchTransactionCoordinator();
    auto txn = txn_co.createTransaction(CreateTransactionOption().setPriority(CnchTransactionPriority::low));
    {
        /// Anyhow, the transaction should be finished even if exception occurs
        SCOPE_EXIT({
                       try {
                           txn_co.finishTransaction(txn);
                       }
                       catch (...) {
                           tryLogCurrentException(log, "Error occurs while finishing txn");
                       }
                   });

        txn->setKafkaTpl(kafka_table->getGroupForBytekv(), tpl);
        txn->setMainTableUUID(target_table->getStorageUUID());

        try {
            txn_co.commitV2(txn);
            /// FIXME: add `tpl` in log
            LOG_INFO(log, "Successfully to reset offsets: {}", DB::Kafka::toString(tpl));
        }
        catch (...) {
            throw Exception("Failed to commit transaction while resetting offsets", ErrorCodes::LOGICAL_ERROR);
        }
    }
}

void CnchKafkaOffsetManager::resetOffsetWithSpecificOffsets(const cppkafka::TopicPartitionList & tpl)
{
    if (tpl.empty())
        return;

    resetOffsetImpl(tpl);
}

void CnchKafkaOffsetManager::resetOffsetToSpecialPosition(int64_t offset)
{
    if (!offsetValueIsSpecialPosition(offset))
        throw Exception("Invalid offset value, special offset can only be one of {-2, -1, -1000, -1001}", ErrorCodes::BAD_ARGUMENTS);

    auto tpl = createTopicPartitionList();
    if (tpl.empty())
        throw Exception("No partition found for CnchKafka table: " + kafka_table->getStorageID().getNameForLogs(), ErrorCodes::LOGICAL_ERROR);

    /// Set offset for each TopicPartition pair
    std::for_each(tpl.begin(), tpl.end(), [&offset] (auto & tp) {tp.set_offset(offset);});

    resetOffsetImpl(tpl);
}

void CnchKafkaOffsetManager::resetOffsetWithTimestamp(UInt64 time_stamp)
{
    /// TODO how to check validity of time_stamp

    auto tpl = createTopicPartitionList(time_stamp);
    if (tpl.empty())
        throw Exception("No partition found for CnchKafka table: " + kafka_table->getStorageID().getNameForLogs(), ErrorCodes::LOGICAL_ERROR);

    resetOffsetImpl(tpl);
}

} /// namespace DB

#endif
