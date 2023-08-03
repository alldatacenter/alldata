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

#include <cppkafka/topic_partition.h>
#include <cppkafka/topic_partition_list.h>

#include <CloudServices/ICnchBGThread.h>
#include <Core/BackgroundSchedulePool.h>
#include <Storages/Kafka/KafkaConsumer.h>
#include <Storages/Kafka/CnchKafkaConsumerScheduler.h>
#include <Storages/Kafka/KafkaTaskCommand.h>
#include <Storages/IStorage.h>

namespace DB
{
class StorageCnchKafka;
class CnchWorkerClient;
using StorageCnchKafkaPtr = std::shared_ptr<StorageCnchKafka>;

class CnchKafkaConsumeManager : public ICnchBGThread
{
public:
    using ConsumerDependencies = std::set<StorageID>;

    CnchKafkaConsumeManager(ContextPtr context_, const StorageID & storage_id);

    ~CnchKafkaConsumeManager() override;

    void preStart() override;
    void stop() override;

    void runImpl() override;
    void iterate(StorageCnchKafka & kafka_table);

    void stopConsumers();
    void restartConsumers();

    bool checkDependencies(const StorageID & storage_id);
    ConsumerDependencies getDependenciesFromCatalog(const StorageID & storage_id);
    ConsumerDependencies getDependencies() const;

    bool checkWorkerClient(const String & consumer_table_name, size_t index) const;

    struct ConsumerInfo
    {
        ConsumerInfo() = default;
        ConsumerInfo(const ConsumerInfo & info):
            index(info.index),
            partitions(info.partitions),
            worker_client(info.worker_client),
            is_running(info.is_running),
            table_suffix(info.table_suffix) {}

        mutable std::mutex mutex;
        size_t index;
        cppkafka::TopicPartitionList partitions;
        CnchWorkerClientPtr worker_client;
        bool is_running{false};
        String table_suffix;
    };
    String getLastException() const;
    std::vector<KafkaConsumerRunningInfo> getConsumerInfos() const;
    void getOffsetsFromCatalog(cppkafka::TopicPartitionList & offsets, const StorageID & buffer_table, const String & consumer_group);

private:
    void updatePartitionCountOfTopics(StorageCnchKafka & kafka_table, bool & partitions_changed);

    CnchWorkerClientPtr selectWorker(size_t index, const String & table_suffix);
    void assignPartitionsToConsumers(StorageCnchKafka & kafka_table);

    bool checkTargetTable(const StorageCnchMergeTree *);
    void checkConsumerStatus(ConsumerInfo & info);

    void dispatchConsumerToWorker(StorageCnchKafka & kafka_table, ConsumerInfo & info, std::exception_ptr & exception);
    void stopConsumerOnWorker(ConsumerInfo & info);

    void initConsumerScheduler();

    ContextPtr createQueryContext();

/// private member variables
    std::shared_ptr<KafkaConsumer> tool_consumer = nullptr;
    ConsumerDependencies dependencies;
    std::map<String, size_t> num_partitions_of_topics;

    size_t max_needed_consumers{1};
    std::vector<ConsumerInfo> consumer_infos;
    KafkaConsumerSchedulerPtr consumer_scheduler;

    std::atomic<bool> cloud_table_has_unique_key{false};

    UInt64 exception_occur_times{0};
    mutable std::mutex last_exception_mutex;
    String last_exception;

    mutable std::mutex state_mutex;
    mutable std::mutex consumer_info_mutex;

    /// For logging exception to CnchKafkaLog
    void logExceptionToCnchKafkaLog(String msg, bool deduplicate = false);
    [[maybe_unused]]std::atomic<std::size_t> last_exception_msg_hash;
    [[maybe_unused]]std::atomic<time_t> last_exception_time;

}; // class CnchKafkaConsumeManager

} // namespace DB
#endif
