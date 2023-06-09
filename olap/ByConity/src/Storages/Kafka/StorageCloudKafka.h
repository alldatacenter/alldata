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

#include <Core/BackgroundSchedulePool.h>
#include <Interpreters/KafkaLog.h>
#include <Storages/Kafka/KafkaTaskCommand.h>
#include <Storages/Kafka/IStorageCnchKafka.h>

#include <common/shared_ptr_helper.h>
#include <cppkafka/cppkafka.h>

namespace DB
{
void dropConsumerTables(ContextMutablePtr context, const String & db_name, const String & tb_name);


/***
 * @name StorageCloudKafka
 * @desc Class for Cnch kafka task in worker side, which is the actual execution unit for kafka consumption
 * **/
class StorageCloudKafka : public shared_ptr_helper<StorageCloudKafka>, public IStorageCnchKafka
{
    friend struct shared_ptr_helper<StorageCloudKafka>;
    friend class CnchKafkaBlockInputStream;

public:
    ~StorageCloudKafka() override;
    String getName() const override { return "StorageCloudKafka"; }

    void startup() override;
    void shutdown() override;

    void setCnchStorageID(const StorageID & storage_id) { cnch_storage_id = storage_id; }

    void startConsume(size_t consumer_index, const cppkafka::TopicPartitionList & tpl);
    void stopConsume();

    bool getStreamStatus() const { return stream_run; }
    void getConsumersStatus(CnchConsumerStatus & status) const;
    cppkafka::TopicPartitionList getCurrentConsumptionOffsets();
    cppkafka::TopicPartitionList getConsumerAssignment() const;

private:
    struct ConsumerContext
    {
        std::unique_ptr<std::timed_mutex> mutex;
        std::atomic<bool> initialized{false};
        BackgroundSchedulePool::TaskHolder task;
        BufferPtr buffer;

        /***
         * @assignment:     partitions and offsets for each iteration's consumption, initialized from byteKV
         * @latest_offsets: the latest offsets after polling msgs from kafka
         * if writing data successfully, the `assignment` will be assigned to `latest_offsets`
         * else (write failed) the consumer will be assigned with the old assignment and re-poll again
         * */
        cppkafka::TopicPartitionList assignment;
        cppkafka::TopicPartitionList latest_offsets;

        bool error_event;

        void reset();
    };
    ConsumerContext consumer_context;
    size_t assigned_consumer_index;

    const SettingsChanges settings_adjustments;

    /// store server client info as global_context won't have it
    HostWithPorts server_client_address;

    Poco::Logger * log;
    String last_exception;
    UInt64 rdkafka_exception_times{0};

    StorageID cnch_storage_id{StorageID::createEmpty()};
    bool cloud_table_has_unique_key{false};

    BackgroundSchedulePool::TaskHolder check_staged_area_task;
    size_t check_staged_area_reschedule_ms;
    std::atomic<bool> wait_for_staged_parts_to_publish{false};

    mutable std::mutex table_status_mutex;
    std::atomic<bool> stream_run{false};
    std::atomic<bool> is_stopping_consume{false};
    std::condition_variable stop_cv;

    void tryLoadFormatSchemaFileFromHDFS();

    BufferPtr tryClaimBuffer(long wait_ms);
    void pushBuffer();
    void subscribeBuffer(BufferPtr & buffer);
    void unsubscribeBuffer(BufferPtr & buffer);

    cppkafka::Configuration createConsumerConfiguration();
    BufferPtr createBuffer();
    void createStreamThread(const cppkafka::TopicPartitionList &);
    void stopStreamThread();

    void streamThread();
    bool streamToViews();
    void streamCopyData(IBlockInputStream & from, IBlockOutputStream & to, ContextMutablePtr consume_context);

    bool checkDependencies(const String & database_name, const String & table_name, bool check_staged_area);
    Names filterVirtualNames(const Names & names) const;

    KafkaLogElement createKafkaLog(KafkaLogElement::Type type, size_t consumer_index);

    void checkStagedArea();

    SettingsChanges createSettingsAdjustments();

protected:
    StorageCloudKafka(
        const StorageID & table_id_,
        ContextMutablePtr context_,
        const ColumnsDescription & columns_,
        const ConstraintsDescription & constraints_,
        const ASTPtr setting_changes_,
        const KafkaSettings & settings_,
        const String & server_client_host_,
        UInt16 server_client_rpc_port_
    );
};

/// API called by ConsumeManager to launch consumer task, aka CloudKafka
void executeKafkaConsumeTask(const KafkaTaskCommand & command, ContextMutablePtr context);

}/// namespace DB

#endif
