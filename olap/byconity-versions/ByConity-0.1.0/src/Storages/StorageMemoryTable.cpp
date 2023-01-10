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
#if 0 /// NOT need for CnchKafka, maybe can delete

#include <Storages/StorageMemoryTable.h>
#include <boost/range/algorithm_ext/erase.hpp>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/InterpreterInsertQuery.h>
#include <Interpreters/InterpreterAlterQuery.h>
#include <Interpreters/castColumn.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Interpreters/addMissingDefaults.h>
#include <DataStreams/ConvertingBlockInputStream.h>
#include <DataStreams/IBlockInputStream.h>
#include <Databases/IDatabase.h>
#include <Storages/StorageFactory.h>
#include <Storages/AlterCommands.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTExpressionList.h>
#include <Common/setThreadName.h>
#include <Common/CurrentMetrics.h>
#include <Common/MemoryTracker.h>
#include <Common/FieldVisitors.h>
#include <Common/typeid_cast.h>
#include <Common/ProfileEvents.h>
#include <common/logger_useful.h>
#include <common/range.h>
#include <QueryPlan/ExpressionStep.h>
#include <Processors/Transforms/FilterTransform.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <Processors/Sources/SourceFromInputStream.h>
#include <QueryPlan/SettingQuotaAndLimitsStep.h>
#include <QueryPlan/ReadFromPreparedSource.h>
#include <QueryPlan/UnionStep.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>

#include <DataStreams/ExpressionBlockInputStream.h>
#include <Storages/Kafka/StorageHaKafka.h>
#include <boost/algorithm/string.hpp>
#include <Columns/ColumnByteMap.h>
#include <DataTypes/MapHelpers.h>

namespace ProfileEvents
{
    extern const Event StorageMemoryFlush;
    extern const Event StorageMemoryErrorOnFlush;
    extern const Event StorageMemoryPassedAllMinThresholds;
    extern const Event StorageMemoryPassedTimeMaxThreshold;
    extern const Event StorageMemoryPassedRowsMaxThreshold;
    extern const Event StorageMemoryPassedBytesMaxThreshold;

}

namespace CurrentMetrics
{
    extern const Metric StorageMemoryRows;
    extern const Metric StorageMemoryBytes;
}

namespace Poco
{
class Logger;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int INFINITE_LOOP;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int BAD_TYPE_OF_FIELD;
    extern const int ALTER_OF_COLUMN_IS_FORBIDDEN;
}


StorageMemoryTable::StorageMemoryTable(const StorageID & table_id_,
    const ColumnsDescription & columns_,
    const ConstraintsDescription & constraints_,
    const String & comment,
    ContextPtr context_,
    size_t num_shards_,
    const Thresholds & min_thresholds_,
    const Thresholds & max_thresholds_,
    const StorageID & destination_id_,
    bool allow_materialized_,
    size_t write_block_queue_size)
    : IStorage(table_id_),
    WithContext(context_->getBufferContext()),
    num_shards(num_shards_),
    min_thresholds(min_thresholds_),
    max_thresholds(max_thresholds_),
    destination_id(destination_id_),
    allow_materialized(allow_materialized_),
    buffer_contexts(num_shards_)
{
    ColumnsDescription total_columns(columns_);
    total_columns.add(ColumnDescription("_info", std::make_shared<DataTypeString>()));

    StorageInMemoryMetadata storage_metadata;
    storage_metadata.setColumns(total_columns);
    storage_metadata.setConstraints(constraints_);
    storage_metadata.setComment(comment);
    setInMemoryMetadata(storage_metadata);

    log = &Poco::Logger::get("StorageMemoryTable (" + table_id_.getNameForLogs() + ")");

    for (size_t i = 0; i < num_shards; ++i)
    {
        buffer_contexts[i].buffer.index = i;
        auto task = getContext()->getMemoryTableSchedulePool().createTask(log->name(),
                [this, buffer_index = i] { asyncWriteBlock(buffer_index); });
        task->deactivate();
        buffer_contexts[i].async_write_task = std::move(task);
        buffer_contexts[i].write_block_queue = std::make_shared<ConcurrentBoundedQueue<WriteBlockRequest>>(write_block_queue_size);
    }
    read_memory_mode = ReadMemoryTableMode::ALL;

}

StorageMemoryTable::~StorageMemoryTable()
{
    try
    {
        shutdown();
    }
    catch (...)
    {
        tryLogCurrentException(log, "~StorageMemoryTable");
    }
}

bool StorageMemoryTable::supportsPrewhere() const
{
    if (destination_id.empty())
        return false;
    auto dest = DatabaseCatalog::instance().tryGetTable(destination_id, getContext());
    if (dest && dest.get() != this)
        return dest->supportsPrewhere();
    return false;
}

void StorageMemoryTable::dumpStatus()
{
    std::stringstream ss;
    ss << "Stats of memory table :" << getStorageID().getNameForLogs() << std::endl;
    ss << "Read memory table mode :" << readModeToString(read_memory_mode) << std::endl;
    for (auto & buffer_context : buffer_contexts)
    {
        ss << "-----------------" << std::endl;
        ss << "buffer index-" << buffer_context.buffer.index << std::endl;
        ss << "buffer total bytes-" << buffer_context.buffer.total_bytes << std::endl;
        ss << "buffer total rows-" << buffer_context.buffer.total_rows << std::endl;
        ss << "buffer block list size-" << buffer_context.buffer.data.size() << std::endl;
        ss << "buffer write request queue size-" << buffer_context.write_block_queue->size() << std::endl;
        ss << buffer_context.async_write_task->dumpStatus();
    }

    LOG_DEBUG(log, ss.str());
}


class MemoryTableSource : public SourceWithProgress
{
public:
    MemoryTableSource(const Names & column_names_, StorageMemoryTable::Buffer & buffer_, const StorageMemoryTable & storage_, const StorageMetadataPtr & metadata_snapshot)
        : SourceWithProgress(metadata_snapshot->getSampleBlockForColumns(column_names_, storage_.getVirtuals(), storage_.getStorageID()))
        , column_names_and_types(metadata_snapshot->getColumns().getByNames(ColumnsDescription::All, column_names_, true)), buffer(buffer_), storage(storage_) {}

    String getName() const override { return "MemoryTable"; }

protected:

    void extractColumns(Chunk & result)
    {
        if (buffer.data.empty())
            return;

        Columns columns;
        columns.reserve(column_names_and_types.size());
        for (const auto & elem : column_names_and_types)
        {
            if (isMapImplicitKey(elem.getNameInStorage()))
            {
                for(const auto & columnTypeName : (*buffer.data.begin())->getColumnsWithTypeAndName())
                {
                    if (startsWith(elem.getNameInStorage(), String("__") + columnTypeName.name + "__") &&
                        columnTypeName.type->isMap()&& !columnTypeName.type->isMapKVStore())
                    {
                        String implicit_key = parseKeyNameFromImplicitColName(columnTypeName.name, elem.getNameInStorage());
                        MutableColumnPtr map_column = columnTypeName.column->assumeMutable();
                        auto result_column = typeid_cast<ColumnByteMap&>(*map_column).
                                getValueColumnByKey(StringRef(implicit_key.data(), implicit_key.size()))->cloneEmpty();
                        for (const auto & block_ptr : buffer.data)
                        {
                            MutableColumnPtr other_map_column = block_ptr->getByName(columnTypeName.name).column->assumeMutable();
                            auto other_value_column = typeid_cast<ColumnByteMap&>(*other_map_column).
                                    getValueColumnByKey(StringRef(implicit_key.data(), implicit_key.size()));
                            result_column->insertRangeFrom(*other_value_column.get(), 0, block_ptr->rows());
                        }
                        columns.emplace_back(std::move(result_column));
                        break;
                    }
                }
            }
            else
            {
                const auto & column = (*buffer.data.begin())->getByName(elem.getNameInStorage()).column;
                auto result_column = column->cloneEmpty();
                for (const auto & block_ptr : buffer.data)
                    result_column->insertRangeFrom(*block_ptr->getByName(elem.getNameInStorage()).column.get(), 0, block_ptr->rows());
                columns.emplace_back(std::move(result_column));
            }
        }
        UInt64 size = columns.at(0)->size();
        result.setColumns(std::move(columns), size);
    }

    Chunk generate() override
    {
        Chunk res;

        if (has_been_read)
            return res;
        has_been_read = true;

        try
        {
            switch(storage.getReadMemoryMode())
            {
                case ReadMemoryTableMode::ALL:
                {
                    std::unique_lock buffer_lock(buffer.mutex);
                    extractColumns(res);
                }
                    break;
                case ReadMemoryTableMode::PART:
                {
                    std::unique_lock try_buffer_lock(buffer.mutex, std::try_to_lock);
                    if (try_buffer_lock.owns_lock())
                        extractColumns(res);
                }
                    break;
                case ReadMemoryTableMode::SKIP:
                    return res;
            }
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }

        return res;
    }

private:
    NamesAndTypesList column_names_and_types;
    StorageMemoryTable::Buffer & buffer;
    const StorageMemoryTable & storage;
    bool has_been_read = false;
};

QueryProcessingStage::Enum StorageMemoryTable::getQueryProcessingStage(
    ContextPtr local_context,
    QueryProcessingStage::Enum to_stage,
    const StorageMetadataPtr &,
    SelectQueryInfo & query_info) const
{
    if (destination_id)
    {
        auto destination = DatabaseCatalog::instance().getTable(destination_id, local_context);

        if (destination.get() == this)
            throw Exception("Destination table is myself. Read will cause infinite loop.", ErrorCodes::INFINITE_LOOP);

        return destination->getQueryProcessingStage(local_context, to_stage, destination->getInMemoryMetadataPtr(), query_info);
    }

    return QueryProcessingStage::FetchColumns;
}

Pipe StorageMemoryTable::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    const size_t max_block_size,
    const unsigned num_streams)
{
    QueryPlan plan;
    read(plan, column_names, metadata_snapshot, query_info, local_context, processed_stage, max_block_size, num_streams);
    return plan.convertToPipe(
        QueryPlanOptimizationSettings::fromContext(local_context),
        BuildQueryPipelineSettings::fromContext(local_context));
}

void StorageMemoryTable::read(
    QueryPlan & query_plan,
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    size_t max_block_size,
    unsigned num_streams)
{
    if (destination_id)
    {
        auto destination = DatabaseCatalog::instance().getTable(destination_id, local_context);

        if (destination.get() == this)
            throw Exception("Destination table is myself. Read will cause infinite loop.", ErrorCodes::INFINITE_LOOP);

        auto destination_lock = destination->lockForShare(local_context->getCurrentQueryId(), local_context->getSettingsRef().lock_acquire_timeout);

        auto destination_metadata_snapshot = destination->getInMemoryMetadataPtr();

        const bool dst_has_same_structure = std::all_of(column_names.begin(), column_names.end(), [metadata_snapshot, destination_metadata_snapshot](const String& column_name)
        {
            const auto & dest_columns = destination_metadata_snapshot->getColumns();
            const auto & our_columns = metadata_snapshot->getColumns();
            auto dest_columm = dest_columns.tryGetColumnOrSubcolumn(ColumnsDescription::AllPhysical, column_name);
            return dest_columm && dest_columm->type->equals(*our_columns.getColumnOrSubcolumn(ColumnsDescription::AllPhysical, column_name).type);
        });

        if (dst_has_same_structure)
        {
            if (query_info.order_optimizer)
                query_info.input_order_info = query_info.order_optimizer->getInputOrder(destination_metadata_snapshot, local_context);

            /// The destination table has the same structure of the requested columns and we can simply read blocks from there.
            destination->read(
                query_plan, column_names, destination_metadata_snapshot, query_info,
                local_context, processed_stage, max_block_size, num_streams);
        }
        else
        {
            /// There is a struct mismatch and we need to convert read blocks from the destination table.
            const Block header = metadata_snapshot->getSampleBlock();
            Names columns_intersection = column_names;
            Block header_after_adding_defaults = header;
            const auto & dest_columns = destination_metadata_snapshot->getColumns();
            const auto & our_columns = metadata_snapshot->getColumns();
            for (const String & column_name : column_names)
            {
                if (!dest_columns.hasPhysical(column_name))
                {
                    LOG_WARNING(log, "Destination table {} doesn't have column {}. The default values are used.", destination_id.getNameForLogs(), backQuoteIfNeed(column_name));
                    boost::range::remove_erase(columns_intersection, column_name);
                    continue;
                }
                const auto & dst_col = dest_columns.getPhysical(column_name);
                const auto & col = our_columns.getPhysical(column_name);
                if (!dst_col.type->equals(*col.type))
                {
                    LOG_WARNING(log, "Destination table {} has different type of column {} ({} != {}). Data from destination table are converted.", destination_id.getNameForLogs(), backQuoteIfNeed(column_name), dst_col.type->getName(), col.type->getName());
                    header_after_adding_defaults.getByName(column_name) = ColumnWithTypeAndName(dst_col.type, column_name);
                }
            }

            if (columns_intersection.empty())
            {
                LOG_WARNING(log, "Destination table {} has no common columns with block in buffer. Block of data is skipped.", destination_id.getNameForLogs());
            }
            else
            {
                destination->read(
                        query_plan, columns_intersection, destination_metadata_snapshot, query_info,
                        local_context, processed_stage, max_block_size, num_streams);

                if (query_plan.isInitialized())
                {

                    auto actions = addMissingDefaults(
                            query_plan.getCurrentDataStream().header,
                            header_after_adding_defaults.getNamesAndTypesList(),
                            metadata_snapshot->getColumns(),
                            local_context);

                    auto adding_missed = std::make_unique<ExpressionStep>(
                            query_plan.getCurrentDataStream(),
                            std::move(actions));

                    adding_missed->setStepDescription("Add columns missing in destination table");
                    query_plan.addStep(std::move(adding_missed));

                    auto actions_dag = ActionsDAG::makeConvertingActions(
                            query_plan.getCurrentDataStream().header.getColumnsWithTypeAndName(),
                            header.getColumnsWithTypeAndName(),
                            ActionsDAG::MatchColumnsMode::Name);

                    auto converting = std::make_unique<ExpressionStep>(query_plan.getCurrentDataStream(), actions_dag);

                    converting->setStepDescription("Convert destination table columns to Buffer table structure");
                    query_plan.addStep(std::move(converting));
                }
            }
        }

        if (query_plan.isInitialized())
        {
            StreamLocalLimits limits;
            SizeLimits leaf_limits;

            /// Add table lock for destination table.
            auto adding_limits_and_quota = std::make_unique<SettingQuotaAndLimitsStep>(
                    query_plan.getCurrentDataStream(),
                    destination,
                    std::move(destination_lock),
                    limits,
                    leaf_limits,
                    nullptr,
                    nullptr);

            adding_limits_and_quota->setStepDescription("Lock destination table for Buffer");
            query_plan.addStep(std::move(adding_limits_and_quota));
        }
    }

    Pipe pipe_from_buffers;
    {
        Pipes pipes_from_buffers;
        pipes_from_buffers.reserve(num_shards);
        for (auto & buffer_context : buffer_contexts)
            pipes_from_buffers.emplace_back(std::make_shared<MemoryTableSource>(column_names, buffer_context.buffer, *this, metadata_snapshot));

        pipe_from_buffers = Pipe::unitePipes(std::move(pipes_from_buffers));
    }

    if (pipe_from_buffers.empty())
        return;

    QueryPlan buffers_plan;

    /** If the sources from the table were processed before some non-initial stage of query execution,
      * then sources from the buffers must also be wrapped in the processing pipeline before the same stage.
      */
    if (processed_stage > QueryProcessingStage::FetchColumns)
    {
        auto interpreter = InterpreterSelectQuery(
                query_info.query, local_context, std::move(pipe_from_buffers),
                SelectQueryOptions(processed_stage));
        interpreter.buildQueryPlan(buffers_plan);
    }
    else
    {
        if (query_info.prewhere_info)
        {
            auto actions_settings = ExpressionActionsSettings::fromContext(local_context);
            if (query_info.prewhere_info->alias_actions)
            {
                pipe_from_buffers.addSimpleTransform([&](const Block & header)
                {
                    return std::make_shared<ExpressionTransform>(
                        header,
                        std::make_shared<ExpressionActions>(query_info.prewhere_info->alias_actions, actions_settings));
                });
            }

            if (query_info.prewhere_info->row_level_filter)
            {
                pipe_from_buffers.addSimpleTransform([&](const Block & header)
                {
                    return std::make_shared<FilterTransform>(
                            header,
                            std::make_shared<ExpressionActions>(query_info.prewhere_info->row_level_filter, actions_settings),
                            query_info.prewhere_info->row_level_column_name,
                            false);
                });
            }

            pipe_from_buffers.addSimpleTransform([&](const Block & header)
            {
                return std::make_shared<FilterTransform>(
                        header,
                        std::make_shared<ExpressionActions>(query_info.prewhere_info->prewhere_actions, actions_settings),
                        query_info.prewhere_info->prewhere_column_name,
                        query_info.prewhere_info->remove_prewhere_column);
            });
        }

        auto read_from_buffers = std::make_unique<ReadFromPreparedSource>(std::move(pipe_from_buffers));
        read_from_buffers->setStepDescription("Read from buffers of Buffer table");
        buffers_plan.addStep(std::move(read_from_buffers));
    }

    if (!query_plan.isInitialized())
    {
        query_plan = std::move(buffers_plan);
        return;
    }

    auto result_header = buffers_plan.getCurrentDataStream().header;

    /// Convert structure from table to structure from buffer.
    if (!blocksHaveEqualStructure(query_plan.getCurrentDataStream().header, result_header))
    {
        auto convert_actions_dag = ActionsDAG::makeConvertingActions(
                query_plan.getCurrentDataStream().header.getColumnsWithTypeAndName(),
                result_header.getColumnsWithTypeAndName(),
                ActionsDAG::MatchColumnsMode::Name);

        auto converting = std::make_unique<ExpressionStep>(query_plan.getCurrentDataStream(), convert_actions_dag);
        query_plan.addStep(std::move(converting));
    }

    DataStreams input_streams;
    input_streams.emplace_back(query_plan.getCurrentDataStream());
    input_streams.emplace_back(buffers_plan.getCurrentDataStream());

    std::vector<std::unique_ptr<QueryPlan>> plans;
    plans.emplace_back(std::make_unique<QueryPlan>(std::move(query_plan)));
    plans.emplace_back(std::make_unique<QueryPlan>(std::move(buffers_plan)));
    query_plan = QueryPlan();

    auto union_step = std::make_unique<UnionStep>(std::move(input_streams));
    union_step->setStepDescription("Unite sources from Memory table");
    query_plan.unitePlans(std::move(union_step), std::move(plans));
}

class MemoryTableBlockOutputStream : public IBlockOutputStream
{
public:
    explicit MemoryTableBlockOutputStream(StorageMemoryTable & storage_, const StorageMetadataPtr & metadata_snapshot_)
    : storage(storage_), metadata_snapshot(metadata_snapshot_) {}

    Block getHeader() const override { return metadata_snapshot->getSampleBlock(); }

    void write(const Block & block) override
    {
        if (!block)
            return;

        // Check table structure.
        metadata_snapshot->check(block, true);
        size_t rows = block.rows();
        if (!rows)
            return;

        StoragePtr destination;
        if (!storage.destination_id.empty())
        {
            destination = DatabaseCatalog::instance().tryGetTable(storage.destination_id, storage.getContext());
            if (destination.get() == &storage)
                throw Exception("Destination table is myself. Write will cause infinite loop.", ErrorCodes::INFINITE_LOOP);
        }

        /// get consume index from block
        String info_str;
        extractField(block, "_info", info_str, 0);
        KafkaConsumeInfo kafka_info = KafkaConsumeInfo::parse(info_str);

        /// Insert into buffer
        Block sorted_block = block.sortColumns();
        insertIntoBuffer(const_cast<Block &>(sorted_block), storage.buffer_contexts[kafka_info.consume_index].buffer);
    }
private:
    StorageMemoryTable & storage;
    StorageMetadataPtr metadata_snapshot;

    void insertIntoBuffer(Block & block, StorageMemoryTable::Buffer & buffer)
    {
        time_t current_time = time(nullptr);

        if (!buffer.first_write_time)
            buffer.first_write_time = current_time;
        {
            std::lock_guard lock(buffer.mutex);
            std::shared_ptr<Block> write_block = std::make_shared<Block>(block.cloneEmpty());
            write_block->swap(block);
            buffer.data.push_back(write_block);
            buffer.updateBlockStats();
        }

        if (storage.checkThresholds(buffer, current_time))
            storage.flushBuffer(buffer, false);
    }
};


BlockOutputStreamPtr StorageMemoryTable::write(const ASTPtr & /*query*/, const StorageMetadataPtr & metadata_snapshot, ContextPtr /*context*/)
{
    return std::make_shared<MemoryTableBlockOutputStream>(*this, metadata_snapshot);
}

bool StorageMemoryTable::mayBenefitFromIndexForIn(
    const ASTPtr & left_in_operand, ContextPtr query_context, const StorageMetadataPtr & /*metadata_snapshot*/) const
{
    if (!destination_id)
        return false;

    auto destination = DatabaseCatalog::instance().getTable(destination_id, query_context);

    if (destination.get() == this)
        throw Exception("Destination table is myself. Read will cause infinite loop.", ErrorCodes::INFINITE_LOOP);

    return destination->mayBenefitFromIndexForIn(left_in_operand, query_context, destination->getInMemoryMetadataPtr());
}

void StorageMemoryTable::startup()
{
    if (getContext()->getSettingsRef().readonly)
    {
        LOG_WARNING(log, "Storage {} is run with readonly settings, it will not be able to insert data. Set apropriate system_profile to fix this.", getName());
    }

    for (auto & buffer_context : buffer_contexts)
        buffer_context.async_write_task->activateAndSchedule();
}


void StorageMemoryTable::shutdown()
{
    try
    {
        optimize({} /*query*/, {}, {} /*partition_id*/, false /*final*/, false /*deduplicate*/, {}, getContext());
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }

    for (auto & buffer_context : buffer_contexts)
    {
        buffer_context.async_write_task->deactivate();
        while (!buffer_context.write_block_queue->empty())
        {
            WriteBlockRequest write_request;
            if (!buffer_context.write_block_queue->pop(write_request))
                throw Exception(ErrorCodes::LOGICAL_ERROR, "Could not pop data from write_block_queue");
            writeBlockToDestination(write_request.blocks, DatabaseCatalog::instance().tryGetTable(destination_id, getContext()));
        }
    }
}


/** NOTE If you do OPTIMIZE after insertion,
  * it does not guarantee, that all data will be in destination table at the time of next SELECT just after OPTIMIZE.
  *
  * Because in case if there was already running flushBuffer method,
  *  then call to flushBuffer inside OPTIMIZE will see empty buffer and return quickly,
  *  but at the same time, the already running flushBuffer method possibly is not finished,
  *  so next SELECT will observe missing data.
  *
  * This kind of race condition make very hard to implement proper tests.
  */
bool StorageMemoryTable::optimize(
    const ASTPtr & /*query*/,
    const StorageMetadataPtr & /*metadata_snapshot*/,
    const ASTPtr & partition,
    bool final,
    bool deduplicate,
    const Names & /* deduplicate_by_columns */,
    ContextPtr /*context*/)
{
    if (partition)
        throw Exception("Partition cannot be specified when optimizing table of type Buffer", ErrorCodes::NOT_IMPLEMENTED);

    if (final)
        throw Exception("FINAL cannot be specified when optimizing table of type Buffer", ErrorCodes::NOT_IMPLEMENTED);

    if (deduplicate)
        throw Exception("DEDUPLICATE cannot be specified when optimizing table of type Buffer", ErrorCodes::NOT_IMPLEMENTED);

    flushAllBuffers(false);
    return true;
}


bool StorageMemoryTable::checkThresholds(const Buffer & buffer, time_t current_time, size_t additional_rows, size_t additional_bytes) const
{
    time_t time_passed = 0;
    if (buffer.first_write_time)
        time_passed = current_time - buffer.first_write_time;

    size_t rows = buffer.total_rows + additional_rows;
    size_t bytes = buffer.total_bytes + additional_bytes;

    LOG_TRACE(log, "checkThresholdsImpl, buffer index-{}", buffer.index);

    return checkThresholdsImpl(rows, bytes, time_passed);
}


bool StorageMemoryTable::checkThresholdsImpl(size_t rows, size_t bytes, time_t time_passed) const
{
    if (time_passed > min_thresholds.time && rows > min_thresholds.rows && bytes > min_thresholds.bytes)
    {
        ProfileEvents::increment(ProfileEvents::StorageMemoryPassedAllMinThresholds);
        return true;
    }

    if (time_passed > max_thresholds.time)
    {
        LOG_TRACE(log, "checkThresholdsImpl, time_passed-{}, max_thresholds.time-{}" ,time_passed, max_thresholds.time);
        ProfileEvents::increment(ProfileEvents::StorageMemoryPassedTimeMaxThreshold);
        return true;
    }

    if (rows > max_thresholds.rows)
    {
        LOG_TRACE(log, "checkThresholdsImpl, rows-{}, max_thresholds.rows-{}",rows, max_thresholds.rows);
        ProfileEvents::increment(ProfileEvents::StorageMemoryPassedRowsMaxThreshold);
        return true;
    }

    if (bytes > max_thresholds.bytes)
    {
        LOG_TRACE(log, "checkThresholdsImpl, bytes-{} max_thresholds.bytes-{}", bytes, max_thresholds.bytes);
        ProfileEvents::increment(ProfileEvents::StorageMemoryPassedBytesMaxThreshold);
        return true;
    }

    return false;
}


void StorageMemoryTable::flushAllBuffers(const bool check_thresholds)
{
    for (auto & buffer_context : buffer_contexts)
        flushBuffer(buffer_context.buffer, check_thresholds);
}


void StorageMemoryTable::forceFlushBuffer(size_t buffer_index)
{
    if (buffer_contexts.size() <= buffer_index)
    {
        throw Exception("Flush buffer index is larger than buffer size", ErrorCodes::LOGICAL_ERROR);
    }
    flushBuffer(buffer_contexts[buffer_index].buffer, false);
}

void StorageMemoryTable::flushBuffer(Buffer & buffer, bool check_thresholds)
{
    std::list<std::shared_ptr<Block>> blocks_to_write;
    time_t current_time = time(nullptr);

    size_t rows = 0;
    size_t bytes = 0;
    time_t time_passed = 0;

    rows = buffer.total_rows;
    bytes = buffer.total_bytes;
    if (buffer.first_write_time)
        time_passed = current_time - buffer.first_write_time;

    if (check_thresholds)
    {
        if (!checkThresholdsImpl(rows, bytes, time_passed))
            return;
    }
    else
    {
        if (rows == 0)
            return;
    }

    {
        std::unique_lock lock(buffer.mutex);
        buffer.data.swap(blocks_to_write);
        buffer.resetBlockStats();
    }

    CurrentMetrics::sub(CurrentMetrics::StorageMemoryRows, rows);
    CurrentMetrics::sub(CurrentMetrics::StorageMemoryBytes, bytes);

    ProfileEvents::increment(ProfileEvents::StorageMemoryFlush);

    LOG_TRACE(log, "Flushing buffer with  {} rows, {} bytes, age {} seconds.", rows, bytes, time_passed);

    if (destination_id.empty())
        return;

    /** For simplicity, buffer is locked during write.
        * We could unlock buffer temporary, but it would lead to too many difficulties:
        * - data, that is written, will not be visible for SELECTs;
        * - new data could be appended to buffer, and in case of exception, we must merge it with old data, that has not been written;
        * - this could lead to infinite memory growth.
        */
    try
    {
        if (!buffer_contexts[buffer.index].write_block_queue->push(WriteBlockRequest(blocks_to_write)))
            throw Exception(ErrorCodes::LOGICAL_ERROR, "Could not push data to write_block_queue");
        buffer_contexts[buffer.index].async_write_task->schedule();
    }
    catch (...)
    {
        ProfileEvents::increment(ProfileEvents::StorageMemoryErrorOnFlush);

        /// Return the block to its place in the buffer.
        CurrentMetrics::add(CurrentMetrics::StorageMemoryRows, rows);
        CurrentMetrics::add(CurrentMetrics::StorageMemoryBytes, bytes);

        {
            std::unique_lock lock(buffer.mutex);
            buffer.data.swap(blocks_to_write);
            if (!buffer.first_write_time)
                buffer.first_write_time = current_time;
            buffer.updateBlockStats();
        }

        /// After a while, the next write attempt will happen.
        throw;
    }
}

bool StorageMemoryTable::writeBlockToDestination(std::list<std::shared_ptr<Block>> & immutable_blocks, StoragePtr table)
{
    if (destination_id.empty() || immutable_blocks.empty())
        return true;

    if (!table)
    {
        LOG_ERROR(log, "Destination table {} doesn't exist. Block of data is discarded.", destination_id.getNameForLogs());
        return true;
    }

    auto destination_metadata_snapshot = table->getInMemoryMetadataPtr();
    try
    {
        /// Parse offset information from block
        std::vector<KafkaConsumeInfo> consume_infos;
        std::vector<std::shared_ptr<Block>> start_end_blocks{immutable_blocks.front()};
        if (immutable_blocks.size() != 1)
            start_end_blocks.push_back(immutable_blocks.back());

        for (auto & block_ptr: start_end_blocks)
        {
            String info_str;
            extractField(*block_ptr, "_info", info_str, 0);
            consume_infos.emplace_back(KafkaConsumeInfo::parse(info_str));
            LOG_TRACE(log, "Commit kafka consume info-{}", info_str);
        }

        auto insert = std::make_shared<ASTInsertQuery>();
        insert->table_id = destination_id;

        /** We will insert columns that are the intersection set of columns of the buffer table and the subordinate table.
          * This will support some of the cases (but not all) when the table structure does not match.
          */
        std::shared_ptr<Block> front_raw_block = *immutable_blocks.begin();
        Block structure_of_destination_table = allow_materialized ? destination_metadata_snapshot->getSampleBlock()
                                                                  : destination_metadata_snapshot->getSampleBlockNonMaterialized();
        Block block_to_write;
        for (size_t i : collections::range(0, structure_of_destination_table.columns()))
        {
            auto dst_col = structure_of_destination_table.getByPosition(i);
            if (front_raw_block->has(dst_col.name))
            {
                auto column_type_name = front_raw_block->getByName(dst_col.name);
                MutableColumnPtr col_to = column_type_name.column->cloneEmpty();
                for (const auto& block_ptr : immutable_blocks)
                {
                    const IColumn & col_from = *block_ptr->getByName(dst_col.name).column.get();
                    col_to->insertRangeFrom(col_from, 0, block_ptr->rows());
                }
                column_type_name.column = std::move(col_to);
                if (!column_type_name.type->equals(*dst_col.type))
                {
                    LOG_WARNING(log, "Destination table {} have different type of column ({} != {}) .Block of data is converted.", destination_id.getNameForLogs(),
                                                          backQuoteIfNeed(column_type_name.name), dst_col.type->getName(), column_type_name.type->getName());
                    column_type_name.column = castColumn(column_type_name, dst_col.type);
                    column_type_name.type = dst_col.type;
                }

                block_to_write.insert(column_type_name);
            }
        }

        if (block_to_write.columns() == 0)
        {
            LOG_ERROR(log, "Destination table {} have no common columns with block in buffer. Block of data is discarded.",
                           destination_id.getNameForLogs());
            throw Exception("Destination table have no common columns with block in buffer. Block of data is discarded.", ErrorCodes::LOGICAL_ERROR);
        }

        if (block_to_write.columns() != front_raw_block->columns())
            LOG_WARNING(log, "Not all columns from block in buffer exist in destination table {}. Some columns are discarded.",
                             destination_id.getNameForLogs());

        auto list_of_columns = std::make_shared<ASTExpressionList>();
        insert->columns = list_of_columns;
        list_of_columns->children.reserve(block_to_write.columns());
        for (const auto & column : block_to_write)
            list_of_columns->children.push_back(std::make_shared<ASTIdentifier>(column.name));

        auto insert_context = Context::createCopy(getContext());
        insert_context->makeQueryContext();

        InterpreterInsertQuery interpreter{insert, insert_context, allow_materialized};
        auto block_io = interpreter.execute();
        block_io.out->writePrefix();
        block_io.out->write(block_to_write);
        block_io.out->writeSuffix();
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
        return false;
    }
    return true;
}

void StorageMemoryTable::asyncWriteBlock(size_t buffer_index)
{
    try
    {
        while (!buffer_contexts[buffer_index].write_block_queue->empty())
        {
            WriteBlockRequest write_request;
            if (!buffer_contexts[buffer_index].write_block_queue->pop(write_request))
                throw Exception(ErrorCodes::LOGICAL_ERROR, "Could not pop data from write_block_queue");

            LOG_TRACE(log, "asyncWriteBlock thread process block write request block size-{}", write_request.blocks.size());
            writeBlockToDestination(write_request.blocks, DatabaseCatalog::instance().tryGetTable(destination_id, getContext()));
        }
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

bool StorageMemoryTable::check_buffer_status(size_t buffer_index)
{
    if (buffer_contexts.size() <= buffer_index)
    {
        throw Exception("Flush buffer index is larger than buffer size", ErrorCodes::LOGICAL_ERROR);
    }
    return buffer_contexts[buffer_index].buffer.data.empty();
}

void StorageMemoryTable::checkAlterIsPossible(const AlterCommands & commands, ContextPtr local_context) const
{
    auto name_deps = getDependentViewsByColumn(local_context);
    for (const auto & command : commands)
    {
        if (command.type != AlterCommand::Type::ADD_COLUMN && command.type != AlterCommand::Type::MODIFY_COLUMN
            && command.type != AlterCommand::Type::DROP_COLUMN && command.type != AlterCommand::Type::COMMENT_COLUMN)
            throw Exception(
                "Alter of type '" + alterTypeToString(command.type) + "' is not supported by storage " + getName(),
                ErrorCodes::NOT_IMPLEMENTED);
        if (command.type == AlterCommand::Type::DROP_COLUMN && !command.clear)
        {
            const auto & deps_mv = name_deps[command.column_name];
            if (!deps_mv.empty())
            {
                throw Exception(
                    "Trying to ALTER DROP column " + backQuoteIfNeed(command.column_name) + " which is referenced by materialized view "
                        + toString(deps_mv),
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }
        }
    }
}


void StorageMemoryTable::alter(const AlterCommands & params, ContextPtr local_context, TableLockHolder &)
{
    auto table_id = getStorageID();
    checkAlterIsPossible(params, local_context);
    auto metadata_snapshot = getInMemoryMetadataPtr();

    /// Flush all buffers to storages, so that no non-empty blocks of the old
    /// structure remain. Structure of empty blocks will be updated during first
    /// insert.
    optimize({} /*query*/, metadata_snapshot, {} /*partition_id*/, false /*final*/, false /*deduplicate*/, {}, local_context);

    StorageInMemoryMetadata new_metadata = *metadata_snapshot;
    params.apply(new_metadata, local_context);
    DatabaseCatalog::instance().getDatabase(table_id.database_name)->alterTable(local_context, table_id, new_metadata);
    setInMemoryMetadata(new_metadata);
}

}
#endif
