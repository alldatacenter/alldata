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

#if 0 /// USE_RDKAFKA

#include <mutex>
#include <thread>
#include <Core/NamesAndTypes.h>
#include <Storages/IStorage.h>
#include <DataStreams/IBlockOutputStream.h>
#include <Poco/Event.h>
#include <Storages/Thresholds.h>
#include <Core/BackgroundSchedulePool.h>
#include <Storages/Kafka/KafkaConsumeInfo.h>
#include <Storages/Kafka/ReadMemoryTableMode.h>
#include <Common/ConcurrentBoundedQueue.h>
#include <common/shared_ptr_helper.h>

namespace Poco { class Logger; }

namespace DB
{

class Context;
class StorageHaKafka;

/**
  * MemoryTable which is extended storage from StorageBufferTable is used inside StorageHaKafka engine.
  * During insertion, buffers the data in the RAM until certain thresholds are exceeded.
  * When thresholds are exceeded, flushes the data to another table.
  * When reading, it reads both from its buffers and from the subordinate table.
  *
  * The buffer is a set of num_shards blocks aligned with kafka consumer thread.
  * Thresholds are checked on insertion, and, periodically, in the background thread (to implement time thresholds).
  * Thresholds act independently for each shard. Each shard can be flushed independently of the others.
  * If a block is inserted into the table, which itself exceeds the max-thresholds,
  * it is written directly to append to block queue and wait for async write block to write to destination table.
  *
  * When you destroy a Buffer table, all remaining data is flushed to the subordinate table.
  * Utilize transaction to ensure kafka offset and merge tree data consistency.
  *
  */

class StorageMemoryTable : public shared_ptr_helper<StorageMemoryTable>, public IStorage, WithContext
{
friend class MemoryTableSource;
friend class MemoryTableBlockOutputStream;

public:

    std::string getName() const override { return "Buffer"; }

    QueryProcessingStage::Enum
    getQueryProcessingStage(ContextPtr, QueryProcessingStage::Enum, const StorageMetadataPtr &, SelectQueryInfo &) const override;

    Pipe read(
        const Names & column_names,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned num_streams) override;

    void read(
        QueryPlan & query_plan,
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned num_streams) override;

    BlockOutputStreamPtr write(const ASTPtr & query, const StorageMetadataPtr & /*metadata_snapshot*/, ContextPtr context) override;

    void startup() override;
    /// Flush all buffers into the subordinate table and stop background thread.
    void shutdown() override;
    bool optimize(
        const ASTPtr & query,
        const StorageMetadataPtr & metadata_snapshot,
        const ASTPtr & partition,
        bool final,
        bool deduplicate,
        const Names & deduplicate_by_columns,
        ContextPtr context) override;

    bool supportsParallelInsert() const override { return true; }
    bool supportsSubcolumns() const override { return true; }
    bool supportsSampling() const override { return true; }
    bool supportsPrewhere() const override;
    bool supportsFinal() const override { return true; }
    bool supportsIndexForIn() const override { return true; }

    bool mayBenefitFromIndexForIn(const ASTPtr & left_in_operand, ContextPtr query_context, const StorageMetadataPtr & metadata_snapshot) const override;

    bool check_buffer_status(size_t buffer_index);
    void alter(const AlterCommands & params, ContextPtr context, TableLockHolder & table_lock_holder) override;

    void flushAllBuffers(bool check_thresholds = true);
    void forceFlushBuffer(size_t buffer_index);

    void dumpStatus();
    ReadMemoryTableMode getReadMemoryMode() const {return read_memory_mode;}
    void setReadMemoryMode(ReadMemoryTableMode read_mode) { read_memory_mode = read_mode;}
    void checkAlterIsPossible(const AlterCommands & commands, ContextPtr context) const override;

private:

    struct Buffer
    {
        time_t first_write_time = 0;
        size_t total_rows = 0;
        size_t total_bytes = 0;
        std::list<std::shared_ptr<Block>> data;
        std::mutex mutex;
        size_t index = 0;

        void updateBlockStats()
        {
            total_rows = 0;
            total_bytes = 0;
            for (const auto & block_ptr : data)
            {
                total_rows += block_ptr->rows();
                total_bytes += block_ptr->bytes();
            }
        }

        void resetBlockStats()
        {
            first_write_time = 0;
            total_rows = 0;
            total_bytes = 0;
        }
    };


    ReadMemoryTableMode read_memory_mode;

    struct WriteBlockRequest
    {
        std::list<std::shared_ptr<Block>> blocks;
        WriteBlockRequest() = default;
        explicit WriteBlockRequest(std::list<std::shared_ptr<Block>> &blocks_) : blocks(blocks_) {}
    };

    struct BufferContext
    {
        std::shared_ptr<ConcurrentBoundedQueue<WriteBlockRequest>> write_block_queue;
        BackgroundSchedulePool::TaskHolder async_write_task;
        Buffer buffer;
    };

    const size_t num_shards;
    const Thresholds min_thresholds;
    const Thresholds max_thresholds;
    StorageID destination_id;
    bool allow_materialized;
    std::vector<BufferContext> buffer_contexts;
    void asyncWriteBlock(size_t buffer_index);
    Poco::Logger * log;

    bool checkThresholds(const Buffer & buffer, time_t current_time, size_t additional_rows = 0, size_t additional_bytes = 0) const;
    bool checkThresholdsImpl(size_t rows, size_t bytes, time_t time_passed) const;

    /// Reset the buffer. If check_thresholds is set - resets only if thresholds are exceeded.
    void flushBuffer(Buffer & buffer, bool check_thresholds);

    /// `table` argument is passed, as it is sometimes evaluated beforehand. It must match the `destination`.
    bool writeBlockToDestination(std::list<std::shared_ptr<Block>> & immutable_blocks, StoragePtr table);

public:
    /** num_shards - the level of internal parallelism (the number of independent buffers)
      * The buffer is flushed if all minimum thresholds or at least one of the maximum thresholds are exceeded.
      */
    StorageMemoryTable(const StorageID & table_id_,
            const ColumnsDescription & columns_,
            const ConstraintsDescription & constraints_,
            const String & comment,
            ContextPtr context_,
             size_t num_shards_,
            const Thresholds & min_thresholds_,
            const Thresholds & max_thresholds_,
            const StorageID & destination_id_,
            bool allow_materialized_,
            size_t write_block_queue_size);

    ~StorageMemoryTable() override;
};

}

#endif
