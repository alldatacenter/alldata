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

#include <Storages/IngestPartition.h>
#include <Storages/MergeTree/MergedBlockOutputStream.h>
#include <Storages/MergeTree/MergedColumnOnlyOutputStream.h>
#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <Storages/StorageMergeTree.h>
#include <Storages/PartitionCommands.h>
#include <Storages/PartLinker.h>

#include <Interpreters/InterpreterAlterQuery.h>
#include <Interpreters/executeQuery.h>
#include <Interpreters/InterserverCredentials.h>
#include <IO/WriteHelpers.h>
#include <Client/Connection.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/queryToString.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTFunction.h>

#include <Processors/Executors/PipelineExecutingBlockInputStream.h>

#include <DataStreams/IBlockOutputStream.h>
#include <DataStreams/SquashingBlockInputStream.h>
#include <DataStreams/SquashingBlockOutputStream.h>
#include <DataStreams/AddingDefaultBlockOutputStream.h>
#include <DataStreams/RemoteBlockInputStream.h>

#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/MapHelpers.h>
#include <Core/Field.h>

#include <Columns/ColumnByteMap.h>
#include <Columns/ColumnNullable.h>
#include <Common/typeid_cast.h>
#include <Common/ZooKeeper/KeeperException.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int DUPLICATE_COLUMN;
    extern const int INGEST_PARTITON_DUPLICATE_KEYS;
    extern const int ABORTED;
}

namespace ActionLocks
{
    extern const StorageActionBlockType PartsMerge;
}

// ------ Utilities for Ingest partition -------
int IngestPartition::compare(Columns & target_key_cols, Columns & src_key_cols, size_t n, size_t m)
{
    for (size_t i = 0; i < target_key_cols.size(); ++i)
    {
        auto order = target_key_cols[i]->compareAt(n, m, *(src_key_cols[i]), 1);
        if (order != 0)
            return order;
    }
    return 0;
}


std::optional<NameAndTypePair> IngestPartition::tryGetMapColumn(const StorageInMemoryMetadata & meta_data, const String & col_name)
{
    if (!meta_data.getColumns().hasPhysical(col_name) && isMapImplicitKey(col_name))
    {
        auto & columns = meta_data.getColumns();
        for (auto & nt : (columns.getOrdinary()))
        {
            if (nt.type->isMap())
            {
                if (nt.type->isMapKVStore() ? (col_name == nt.name + ".key" || col_name == nt.name + ".value")
                                            : startsWith(col_name, getMapKeyPrefix(nt.name)))
                {
                    return nt;
                }
            }
        }
    }

    return std::nullopt;
}

void IngestPartition::writeNewPart(const StorageInMemoryMetadata & meta_data, const IngestPartition::IngestSources & src_blocks, BlockOutputStreamPtr & output, Names & all_columns_with_partition_key)
{
    output->writePrefix();

    for (const auto & src_block : src_blocks)
    {
        MutableColumns target_columns;
        DataTypes target_column_types;
        Names target_column_names;

        // we pack together all columns that have same column name, ordinary column is one to one
        // while map implicit column may be one to more
        // column name -> [column name or map key name for map implicit column -> the column]
        std::unordered_map<String, std::unordered_map<String, ColumnPtr>> src_columns;
        // map column name -> key type
        std::unordered_map<String, DataTypePtr> key_types;

        for (const auto & col_name : all_columns_with_partition_key)
        {
            const auto & column_type_name = src_block->block.getByName(col_name);
            // handle map column
            const auto & map_col_opt = tryGetMapColumn(meta_data, col_name);
            if (map_col_opt)
            {
                auto & map_col = *map_col_opt;
                if (!src_columns.count(map_col.name))
                {
                    target_columns.push_back(map_col.type->createColumn());
                    target_column_types.push_back(map_col.type);
                    target_column_names.push_back(map_col.name);

                    const auto & map_col_type = typeid_cast<const DataTypeByteMap &>(*map_col.type);
                    key_types.emplace(map_col.name, map_col_type.getKeyType());
                }

                auto map_key = parseKeyNameFromImplicitColName(column_type_name.name, map_col.name);
                src_columns[map_col.name][map_key] = column_type_name.column;
            }
            else
            {
                target_columns.push_back(column_type_name.type->createColumn());
                target_column_types.push_back(column_type_name.type);
                target_column_names.push_back(column_type_name.name);

                src_columns[column_type_name.name][column_type_name.name] = column_type_name.column;
            }
        }

        auto & src_match_col = src_block->block.getByName("___match_count").column;

        for (size_t n = 0; n < src_block->block.rows(); ++n)
        {
            if (src_match_col->getUInt(n) == 0)
            {
                for (size_t i = 0; i < target_columns.size(); ++i)
                {
                    if (target_column_types[i]->isMap())
                    {
                        auto & key_type = key_types[target_column_names[i]];
                        ByteMap map;

                        for (auto & entry : src_columns[target_column_names[i]])
                        {
                            if (entry.second->isNullAt(n)) continue;
                            Field field;
                            entry.second->get(n, field);
                            map.push_back(std::make_pair(key_type->stringToVisitorField(entry.first), field));
                        }

                        target_columns[i]->insert(map);
                    }
                    else
                    {
                        target_columns[i]->insertFrom(*(src_columns[target_column_names[i]][target_column_names[i]]), n);
                    }
                }
            }
        }

        Block block;
        for (size_t i = 0; i < target_columns.size(); ++i)
        {
            block.insert(ColumnWithTypeAndName(std::move(target_columns[i]), target_column_types[i], target_column_names[i]));
        }

        if (block.rows() > 0)
            output->write(block);
    }

    output->writeSuffix();
}

Names IngestPartition::getOrderedKeys(const Names & names_to_order, const StorageInMemoryMetadata & meta_data)
{
    auto ordered_keys = meta_data.getColumnsRequiredForPrimaryKey();

    if (names_to_order.empty())
    {
        return ordered_keys;
    }
    else
    {
        for (auto & key : names_to_order)
        {
            bool found = false;
            for (auto & table_key : ordered_keys)
            {
                if (table_key == key)
                    found = true;
            }

            if (!found)
                throw Exception("Some given keys are not part of the table's primary key, please check!", ErrorCodes::BAD_ARGUMENTS);
        }

        // get reorderd ingest key
        Names res;
        for (size_t i = 0; i < ordered_keys.size(); ++i)
        {
            for (auto & key : names_to_order)
            {
                if (key == ordered_keys[i])
                    res.push_back(key);
            }
        }

        for (size_t i = 0; i < ordered_keys.size(); ++i)
        {
            if (i < res.size() && res[i] != ordered_keys[i])
                throw Exception("Reordered ingest key must be a prefix of the primary key.", ErrorCodes::BAD_ARGUMENTS);
        }

        return res;
    }
}

void IngestPartition::checkIngestColumns(const StorageInMemoryMetadata & meta_data, bool & has_map_implicite_key)
{
    if (!meta_data.getColumns().getMaterialized().empty())
        throw Exception("There is materialized column in table which is not allowed!", ErrorCodes::BAD_ARGUMENTS);

    for (auto & primary_key : meta_data.getColumnsRequiredForPrimaryKey())
    {
        for (auto & col_name : column_names)
        {
            if (col_name == primary_key)
                throw Exception("Column " + backQuoteIfNeed(col_name) + " is part of the table's primary key which is not allowed!", ErrorCodes::BAD_ARGUMENTS);
        }
    }

    for (auto & partition_key : meta_data.getColumnsRequiredForPartitionKey())
    {
        for (auto & col_name : column_names)
        {
            if (col_name == partition_key)
                throw Exception("Column " + backQuoteIfNeed(col_name) + " is part of the table's partition key which is not allowed!", ErrorCodes::BAD_ARGUMENTS);
        }
    }

    std::unordered_set<String> all_columns;
    for (const auto & col_name : column_names)
    {
        /// Check for duplicates
        if (!all_columns.emplace(col_name).second)
            throw Exception("Ingest duplicate column " + backQuoteIfNeed(col_name), ErrorCodes::DUPLICATE_COLUMN);

        if (isMapImplicitKeyNotKV(col_name))
        {
            has_map_implicite_key = true;
            continue;
        }

        if (meta_data.getColumns().get(col_name).type->isMap())
            throw Exception("Ingest whole map column " + backQuoteIfNeed(col_name) +
                            " is not supported, you can specify a map key.", ErrorCodes::BAD_ARGUMENTS);
    }
}

void IngestPartition::checkColumnStructure(const StorageInMemoryMetadata & target_data, const StorageInMemoryMetadata & src_data, const Names & names)
{
    for (const auto & col_name : names)
    {
        const auto & target = target_data.getColumns().getColumnOrSubcolumn(ColumnsDescription::GetFlags::AllPhysical, col_name);
        const auto & src = src_data.getColumns().getColumnOrSubcolumn(ColumnsDescription::GetFlags::AllPhysical, col_name);

        if (target.name != src.name)
            throw Exception("Column structure mismatch, found different names of column " + backQuoteIfNeed(col_name),
                            ErrorCodes::BAD_ARGUMENTS);

        if (!target.type->equals(*src.type))
            throw Exception("Column structure mismatch, found different types of column " + backQuoteIfNeed(col_name),
                            ErrorCodes::BAD_ARGUMENTS);
    }
}

void IngestPartition::checkPartitionRows(MergeTreeData::DataPartsVector & parts, const Settings & settings, const String & table_type, bool check_compact_map)
{
    size_t rows = 0;
    for (auto & part : parts)
    {
        if (check_compact_map && part->versions->enable_compact_map_data)
            throw Exception("INGEST PARTITON not support compact map type now", ErrorCodes::NOT_IMPLEMENTED);

        rows += part->rows_count;
        if (rows > settings.max_ingest_rows_size)
            throw Exception("Only up to " + settings.max_ingest_rows_size.toString() +
                            " rows for " + table_type + " partition when ingesting.", ErrorCodes::LOGICAL_ERROR);
    }
}

std::vector<Names> IngestPartition::groupColumns(const Settings & settings)
{
    std::vector<Names> res;
    auto max_cols = settings.max_ingest_columns_size;

    for (size_t i = 0; i < (column_names.size() + max_cols - 1) / max_cols; ++i)
    {
        Names group;
        for (size_t j = i * max_cols; j < column_names.size() && j < (i + 1) * max_cols; ++j)
        {
            group.push_back(column_names[j]);
        }
        res.push_back(group);
    }

    return res;
}

IngestPartition::IngestPartition(
        const StoragePtr & target_table_,
        const StoragePtr & source_table_,
        const ASTPtr & partition_,
        const Names & column_names_,
        const Names & key_names_,
        Int64 mutation_,
        const ContextPtr & context_)
        : target_table(target_table_)
        , source_table(source_table_)
        , partition(partition_)
        , column_names(column_names_)
        , key_names(key_names_)
        , mutation(mutation_)
        , context(context_)
        , log(&Poco::Logger::get("IngestPartition")) {}


/***
 * Return 0 / 1 to indicate whether a ingestion happend, so that we determine to generate a ingest log or not.
 */
bool IngestPartition::ingestPartition()
{
    MergeTreeData * target_data = dynamic_cast<MergeTreeData *>(target_table.get());
    if (!target_data)
        return false;
    auto target_meta = target_data->getInMemoryMetadataPtr();

    // if (target_data->getIngestionBlocker().isCancelled())
    //     throw Exception("INGEST PARTITION was cancelled", ErrorCodes::ABORTED);

    /// Order is important when execute the outer join.
    const auto & ordered_key_names = getOrderedKeys(key_names, *target_meta);
    /// Some check for columns.
    bool has_map_implicite_key = false;
    checkIngestColumns(*target_meta, has_map_implicite_key);

    /// Now not support ingesting to storage with compact map type
    if (has_map_implicite_key && target_data->getSettings()->enable_compact_map_data)
    {
        throw Exception("INGEST PARTITON not support compact map type now", ErrorCodes::NOT_IMPLEMENTED);
    }

    // step 1: read block list from source table, and add one column (matched count)
    // step 2: read block from parts in this partition, part by part, also add one column (matched count)
    // - Probe source block list (as both blocks are sorted based on keys)
    // - Update block if match, otherwise place default value
    // - write ingested columns to part
    //
    // step 3: check the source table's block list match count, find those count 0 keys, and form a new Block, and
    // write to disk as new part

    /// STEP 1:
    auto * source_data = dynamic_cast<MergeTreeData *>(source_table.get());
    if (!source_data)
    {
        throw Exception("INGEST PARTITON source table only support merge tree table", ErrorCodes::NOT_IMPLEMENTED);
    }

    auto source_meta = source_data->getInMemoryMetadataPtr();
    /// check whether the columns have equal structure between target table and source table
    checkColumnStructure(*target_meta, *source_meta, column_names);
    checkColumnStructure(*target_meta, *source_meta, ordered_key_names);
    checkColumnStructure(*target_meta, *source_meta, target_meta->getColumnsRequiredForPartitionKey());

    /// TO DO Asks to complete merges and does not allow them to start.
    auto target_merge_blocker = target_table->getActionLock(ActionLocks::PartsMerge);
    auto source_merge_blocker = source_table->getActionLock(ActionLocks::PartsMerge);

    String partition_id = target_data->getPartitionIDFromQuery(partition, context);

    auto execute_query = [&](const String & query)
    {
        ReadBufferFromString istr(query);
        String dummy_string;
        WriteBufferFromString ostr(dummy_string);
        // std::optional<CurrentThread::QueryScope> query_scope;
        auto query_context = Context::createCopy(context);
        // query_scope.emplace(query_context);
        executeQuery(istr, ostr, false, query_context, {}, std::nullopt, true);
        //query_scope.reset();
    };

    auto clear_column = [&](const String & column_name) -> MutationCommand
    {
        std::shared_ptr<ASTAlterCommand> ast_command = std::make_shared<ASTAlterCommand>();
        ast_command->type = ASTAlterCommand::DROP_COLUMN;
        ast_command->if_exists = true;
        ast_command->clear_column = true;
        ast_command->detach = false;
        ast_command->partition = partition;
        ast_command->column = std::make_shared<ASTIdentifier>(column_name);
        ast_command->children.push_back(ast_command->column);
        ast_command->children.push_back(ast_command->partition);

        MutationCommand res = *MutationCommand::parse(ast_command.get(), true);
        return res;
    };

    auto parts_to_read = source_data->getDataPartsVectorInPartition(MergeTreeDataPartState::Committed, partition_id);
    if (parts_to_read.empty())
    {
        if (context->getSettingsRef().allow_ingest_empty_partition)
        {
            /// drop the target column in target partition
            MutationCommands commands;
            for (const auto & column_name : column_names)
                commands.emplace_back(clear_column(column_name));

            if (commands.empty())
                return false;

            target_table->mutate(commands, context);

            LOG_DEBUG(log, "Drop target partition columns since no parts to be ingested inside partition ID {} from table {}", partition_id, source_table->getStorageID().getFullTableName());
            return false;
        }
        else
        {
            LOG_WARNING(log, "No parts to be ingested inside partition ID {} from table {}", partition_id, source_table->getStorageID().getFullTableName());
            return false;
        }
    }

    /// Read target table in partiton_id, and probe src blocks
    MergeTreeData::DataPartsVector parts_to_ingest = target_data->getDataPartsVectorInPartition(MergeTreeDataPartState::Committed, partition_id);
    if (parts_to_ingest.empty())
    {
        String source_db_table = source_data->getStorageID().getFullTableName();
        String target_db_table = target_data->getStorageID().getFullTableName();
        String move_to_query = "ALTER TABLE " + source_db_table + " MOVE PARTITION ID '" + partition_id + "'" + " TO TABLE " + target_db_table;
        LOG_TRACE(log, "AST move partition: {}", move_to_query);

        execute_query(move_to_query);

        return false;
    }

    /// check total rows for source table and target table
    const auto & settings = context->getSettingsRef();
    checkPartitionRows(parts_to_read, settings, "source table", false);
    checkPartitionRows(parts_to_ingest, settings, "target table", has_map_implicite_key);

    Names all_columns = column_names;
    all_columns.insert(all_columns.end(), ordered_key_names.begin(), ordered_key_names.end());

    Names all_columns_with_partition_key(all_columns.begin(), all_columns.end());
    for (auto & partition_key : target_data->getInMemoryMetadataPtr()->getColumnsRequiredForPartitionKey())
    {
        bool duplicate = false;
        for (auto & col_name : all_columns_with_partition_key)
        {
            if (col_name == partition_key)
            {
                duplicate = true;
                break;
            }
        }

        if (!duplicate)
            all_columns_with_partition_key.push_back(partition_key);
    }

    auto source_blocks = generateSourceBlocks(*source_data, parts_to_read, all_columns_with_partition_key);

    /// STEP 2:
    IngestParts ingest_parts = generateIngestParts(*target_data, parts_to_ingest);

    ingestion(*target_data, ingest_parts, source_blocks, column_names, ordered_key_names, all_columns, settings);

    /// STEP 3:
    Block sample_block = target_meta->getSampleBlock();
    Block ingested_header;
    for (const auto & name : all_columns_with_partition_key)
    {
        auto column_name = name;
        /// No need to add implicit map column
        if (isMapImplicitKeyNotKV(name))
            column_name = parseMapNameFromImplicitColName(name);
        auto column = target_meta->getColumns().getColumnOrSubcolumn(ColumnsDescription::GetFlags::AllPhysical, column_name);
        ingested_header.insertUnique(ColumnWithTypeAndName(column.type, column.name));
    }

    BlockOutputStreamPtr new_part_output = target_table->write(ASTPtr(), target_meta, context);
    new_part_output = std::make_shared<SquashingBlockOutputStream>(new_part_output,
                                                                    sample_block,
                                                                    settings.min_insert_block_size_rows,
                                                                    settings.min_insert_block_size_bytes);
    new_part_output = std::make_shared<AddingDefaultBlockOutputStream>(new_part_output,
                                                                        ingested_header,
                                                                        target_meta->getColumns(), context);
    writeNewPart(*target_meta, source_blocks, new_part_output, all_columns_with_partition_key);

    // After ingest, the columns have been updated
    // context->dropCaches(target_data->getDatabaseName(), target_data->getTableName());

    return true;
}

IngestPartition::IngestSources IngestPartition::generateSourceBlocks(MergeTreeData & source_data,
                                                    const MergeTreeData::DataPartsVector & parts_to_read,
                                                    const Names & all_columns_with_partition_key)
{
    auto settings = context->getSettingsRef();
    IngestPartition::IngestSources src_blocks;
    auto match_type = std::make_shared<DataTypeUInt8>();

    for (auto & read_part : parts_to_read)
    {
        bool read_with_direct_io = settings.min_bytes_to_use_direct_io != 0 &&
                                    read_part->getBytesOnDisk() >= settings.min_bytes_to_use_direct_io;


        auto source_input = std::make_shared<MergeTreeSequentialSource>(source_data,
                                                                        source_data.getInMemoryMetadataPtr(),
                                                                        read_part,
                                                                        nullptr,
                                                                        all_columns_with_partition_key,
                                                                        read_with_direct_io, true);

        QueryPipeline source_pipeline;
        source_pipeline.init(Pipe(std::move(source_input)));
        source_pipeline.setMaxThreads(1);
        BlockInputStreamPtr pipeline_input_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(source_pipeline));
        pipeline_input_stream = std::make_shared<SquashingBlockInputStream>(pipeline_input_stream, settings.min_insert_block_size_rows * 2,
                                                        settings.min_insert_block_size_bytes * 4);

        pipeline_input_stream->readPrefix();
        while (Block block = pipeline_input_stream->read())
        {
            // append match count column into block
            auto match_col = match_type->createColumn();
            typeid_cast<ColumnUInt8 &>(*match_col).getData().resize_fill(block.rows());
            block.insert(ColumnWithTypeAndName(std::move(match_col), match_type, "___match_count"));
            src_blocks.push_back(std::make_shared<IngestSource>(block));
        }
        pipeline_input_stream->readSuffix();
    }

    return src_blocks;
}

IngestParts IngestPartition::generateIngestParts(MergeTreeData & data, const MergeTreeData::DataPartsVector & parts)
{
    IngestParts ingest_parts;

    for (const auto & part : parts)
    {
        MergeTreePartInfo new_part_info = part->info;
        new_part_info.mutation = mutation;

        size_t estimated_space_for_result = MergeTreeDataMergerMutator::estimateNeededDiskSpace({part});
        ReservationPtr reserved_space = MergeTreeData::reserveSpace(estimated_space_for_result, part->volume);

        FutureMergedMutatedPart future_part;
        future_part.name = new_part_info.getPartName();
        future_part.parts.push_back(part);
        future_part.part_info = new_part_info;
        future_part.updatePath(data, reserved_space);
        /**
         * we always right wide part after ingestion.
         */
        future_part.type = MergeTreeDataPartType::WIDE;

        IngestPartPtr ingest_part = std::make_shared<IngestPart>(future_part, std::move(reserved_space));
        ingest_parts.push_back(ingest_part);
    }

    return ingest_parts;
}

ASTPtr IngestPartition::getDefaultFilter(const String & column_name)
{
    auto name_type = target_table->getInMemoryMetadata().getColumns().getColumnOrSubcolumn(ColumnsDescription::GetFlags::AllPhysical, column_name);
    Field value = name_type.type->getDefault();
    auto literal = std::make_shared<ASTLiteral>(value);
    auto identifier = std::make_shared<ASTIdentifier>(column_name);
    if (name_type.type->isNullable())
        return makeASTFunction("isNotNull", identifier);
    else
        return makeASTFunction("notEquals", identifier, literal);
}

String IngestPartition::generateFilterString()
{
    ASTs conditions;
    for (const auto & column_name : column_names)
    {
        conditions.push_back(getDefaultFilter(column_name));
    }

    if (conditions.empty())
        return "";

    ASTPtr filter = conditions[0];
    for (size_t i = 1; i < conditions.size(); ++i)
        filter = makeASTFunction("or", filter, conditions[i]);

    return "(" + queryToString(filter) + ")";
}

/***
 * we only need to fetch rows without default values. there are two cases with default values that we can skip
 * 1. rows already in target table, after ingestion, ingested column still be default values, this means
 *    - ingested column is default values
 *    - ingested column does not match this row
 *
 * 2. rows not in target table, after ingestion, we append new part to target table with these rows filled by default value
 *    - new part will be fetched to replica, no need to fetch these rows to ingest.
 *    - if new part is not fetched, ingestion on this partition is blocked, it is guaranteed by log selection
 *    - ingestion log is executed in order, also guaranteed by log selection and lock (zookeeper)
 *
 ***/
String IngestPartition::generateRemoteQuery(
    const String & source_database_name,
    const String & source_table_name,
    const String & partition_id,
    const Strings & column_lists)
{
    std::stringstream query_ss;
    query_ss << "select";
    for (size_t i = 0; i < column_lists.size(); ++i)
    {
        query_ss << " " << backQuoteIfNeed(column_lists[i]);
        if (i != column_lists.size() - 1)
            query_ss << ",";
    }

    auto filter = generateFilterString();
    if (!filter.empty())
        filter = " and " + filter;

    // as ingest column assume record in block is sorted by keys(blockJoinBlocks function), we add order by clauses
    // to guarantee this assumption.
    if (source_database_name.empty())
        query_ss << " from " << backQuoteIfNeed(source_table_name)
                << " where _partition_id = '" << partition_id << "'" << filter << " order by";
    else
        query_ss << " from " << backQuoteIfNeed(source_database_name) << "." << backQuoteIfNeed(source_table_name)
                << " where _partition_id = '" << partition_id << "'" << filter << " order by";


    size_t num_key_names = key_names.size();
    for (size_t i = 0; i < num_key_names; ++i)
    {
        query_ss << " " << backQuoteIfNeed(key_names[i]);
        if (i != num_key_names - 1)
            query_ss << ",";
    }

    return query_ss.str();
}

MergeTreeData::MutableDataPartPtr IngestPartition::ingestPart(MergeTreeData & data, const IngestPartPtr & ingest_part, const IngestPartition::IngestSources & src_blocks,
                const Names & ingest_column_names, const Names & ordered_key_names, const Names & all_columns,
                const Settings & settings)
{
    bool cached_cancel = false;
    auto check_cached_cancel = [&] {
            if (cached_cancel)
                return true;
            return cached_cancel; /**= data.getIngestionBlocker().isCancelled()**/
    };

    if (check_cached_cancel())
        throw Exception("INGEST PARTITION was cancelled", ErrorCodes::ABORTED);

    auto future_part = ingest_part->future_part;
    const auto & target_part = future_part.parts[0];
    LOG_TRACE(&Poco::Logger::get("Ingestion"), "Begin ingest part {}", target_part->name);

    bool is_wide_part = isWidePart(target_part);

    NamesAndTypesList part_columns = target_part->getNamesAndTypes();
    auto storage_names = part_columns.getNames();
    NameSet target_columns_names(storage_names.begin(), storage_names.end());
    Block ingest_header;
    for (auto & col_name : ingest_column_names)
    {
        const auto & src_column = src_blocks.at(0)->block.getByName(col_name);
        ingest_header.insert(ColumnWithTypeAndName(src_column.type, src_column.name));

        if (!target_columns_names.count(col_name))
        {
            part_columns.insert(part_columns.end(), NameAndTypePair(src_column.name, src_column.type));
        }
    }

    auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + future_part.name, ingest_part->reserved_space->getDisk(), 0);
    auto new_data_part = data.createPart(
        future_part.name, future_part.type, future_part.part_info, single_disk_volume, "tmp_ingest_" + future_part.name);

    new_data_part->uuid = future_part.uuid;
    new_data_part->is_temp = true;
    new_data_part->ttl_infos = target_part->ttl_infos;
    new_data_part->versions = target_part->versions;

    new_data_part->setColumns(part_columns);
    new_data_part->checksums_ptr = std::make_shared<MergeTreeData::DataPart::Checksums>();
    *(new_data_part->checksums_ptr) = *(target_part->getChecksums());
    /// It shouldn't be changed by mutation.
    new_data_part->index_granularity_info = target_part->index_granularity_info;
    new_data_part->partition.assign(target_part->partition);


    auto disk = new_data_part->volume->getDisk();

    if (is_wide_part)
    {
        /// Don't change granularity type while mutating subset of columns
        auto mrk_extension = target_part->index_granularity_info.is_adaptive ? getAdaptiveMrkExtension(new_data_part->getType())
                                                                            : getNonAdaptiveMrkExtension();

        auto files_to_skip = PartLinker::collectFilesToSkip(target_part, ingest_header, std::set<MergeTreeIndexPtr>{}, mrk_extension, std::set<MergeTreeProjectionPtr>{}, false);
        NameToNameVector files_to_rename;

        PartLinker part_linker(disk, new_data_part->getFullRelativePath(), target_part->getFullRelativePath(), files_to_skip, files_to_rename);
        part_linker.execute();

        ingestWidePart(data,
                    ingest_header,
                    new_data_part,
                    target_part,
                    src_blocks,
                    ingest_column_names,
                    ordered_key_names,
                    all_columns,
                    settings,
                    check_cached_cancel);
    }
    else
    {
        disk->createDirectories(new_data_part->getFullRelativePath());

        ingestCompactPart(data,
                    part_columns,
                    new_data_part,
                    target_part,
                    src_blocks,
                    ingest_column_names,
                    ordered_key_names,
                    settings,
                    check_cached_cancel);
    }

    LOG_TRACE(&Poco::Logger::get("Ingestion"), "End ingest part {}", future_part.name);

    return new_data_part;
}

void IngestPartition::ingestion(MergeTreeData & data, const IngestParts & parts_to_ingest, const IngestPartition::IngestSources & src_blocks,
               const Names & ingest_column_names, const Names & ordered_key_names, const Names & all_columns,
               const Settings & settings)
{
    if (src_blocks.empty())
    {
        LOG_TRACE(&Poco::Logger::get("VirtualColumnUtils"), "Read source block is empty, skip ingestion");
        return;
    }

    LOG_TRACE(&Poco::Logger::get("Ingestion"), "Ingestion task with parts {}, source_blocks {}, ingest_column_name {}, ordered_key_names {}, all_columns {}",
                                                parts_to_ingest.size(),
                                                src_blocks.size(),
                                                ingest_column_names.size(),
                                                ordered_key_names.size(),
                                                all_columns.size());

    // if (data.getIngestionBlocker().isCancelled())
    //     throw Exception("INGEST PARTITION was cancelled", ErrorCodes::ABORTED);

    std::mutex ingest_mutex;
    IngestParts parts = parts_to_ingest;

    MergeTreeData::Transaction transaction(data);

    std::atomic<bool> has_ingest_exception = false;

    ThreadGroupStatusPtr thread_group = CurrentThread::getGroup();

    auto ingest_part_func = [&]()
    {
        setThreadName("IngestPart");
        if (thread_group)
            CurrentThread::attachToIfDetached(thread_group);

        while (1)
        {
            IngestPartPtr part = nullptr;

            {
                auto ingest_lock = std::lock_guard<std::mutex>(ingest_mutex);
                if (parts.empty() || has_ingest_exception)
                    return;

                part = parts.back();
                parts.pop_back();
            }

            if (!part)
                return;

            try
            {
                auto ingested_part = ingestPart(data, part, src_blocks, ingest_column_names, ordered_key_names, all_columns, settings);
                data.renameTempPartAndReplace(ingested_part, nullptr, &transaction);
            }
            catch(...)
            {
                tryLogCurrentException(__PRETTY_FUNCTION__);
                has_ingest_exception = true;
                return;
            }
        }
    };

    size_t num_threads = std::min(parts.size(), size_t(settings.parallel_ingest_threads));
    std::unique_ptr<ThreadPool> thread_pool = std::make_unique<ThreadPool>(num_threads);
    for (size_t i = 0; i < num_threads; i++)
        thread_pool->scheduleOrThrow(ingest_part_func);

    thread_pool->wait();

    if (has_ingest_exception)
        throw Exception("Ingestion failed", ErrorCodes::ABORTED);

    transaction.commit();
}

void IngestPartition::ingestWidePart(MergeTreeData & data,
                const Block & header,
                MergeTreeData::MutableDataPartPtr & new_data_part,
                const MergeTreeData::DataPartPtr & target_part,
                const IngestPartition::IngestSources & src_blocks,
                const Names & ingest_column_names, const Names & ordered_key_names, const Names & all_columns,
                const Settings & settings,
                std::function<bool()> check_cached_cancel)
{
    bool read_with_direct_io = settings.min_bytes_to_use_direct_io != 0 &&
                                target_part->getBytesOnDisk() >= settings.min_bytes_to_use_direct_io;

    /**
     * Do not load deletebitmap, since if we have rows that is deleted in target block, e.g
     * 1
     * 2 deleted
     * 3
     *
     * and source block has 2 too. if 2 of target block is not read, a new part will be generated,
     * so that we get duplicated key.
     */
    auto source_input = std::make_shared<MergeTreeSequentialSource>(data,
                                                                    data.getInMemoryMetadataPtr(),
                                                                    target_part,
                                                                    nullptr,
                                                                    all_columns,
                                                                    read_with_direct_io, true);

    QueryPipeline source_pipeline;
    source_pipeline.init(Pipe(std::move(source_input)));
    source_pipeline.setMaxThreads(1);
    BlockInputStreamPtr pipeline_input_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(source_pipeline));
    pipeline_input_stream = std::make_shared<SquashingBlockInputStream>(pipeline_input_stream, settings.min_insert_block_size_rows * 2,
                                                         settings.min_insert_block_size_bytes * 4);

    auto compression_codec = target_part->default_codec;
    std::vector<MergeTreeIndexPtr> indices_to_recalc;
    MergedColumnOnlyOutputStream out(
            new_data_part,
            data.getInMemoryMetadataPtr(),
            header,
            compression_codec,
            indices_to_recalc,
            nullptr,
            target_part->index_granularity,
            &target_part->index_granularity_info);

    auto match_type = std::make_shared<DataTypeUInt8>();

    pipeline_input_stream->readPrefix();
    out.writePrefix();

    Block block;
    while (!check_cached_cancel() && (block = pipeline_input_stream->read()))
    {
        /// append match count column into block
        auto match_col = match_type->createColumn();
        typeid_cast<ColumnUInt8 &>(*match_col).getData().resize_fill(block.rows());
        block.insert(ColumnWithTypeAndName(std::move(match_col), match_type, "___match_count"));

        /// assume blocks already sorted by keys, implement join semantics
        const auto & res_block = blockJoinBlocks(data, block, src_blocks, ingest_column_names, ordered_key_names, false);
        out.write(res_block);
    }

    if (check_cached_cancel())
        throw Exception("INGEST PARTITION was cancelled", ErrorCodes::ABORTED);

    pipeline_input_stream->readSuffix();

    auto changed_checksums = out.writeSuffixAndGetChecksums(new_data_part, *(new_data_part->getChecksums()), false);
    new_data_part->getChecksums()->add(std::move(changed_checksums));

    MergeTreeDataMergerMutator::finalizeMutatedPart(target_part, new_data_part, false, compression_codec);
}

void IngestPartition::ingestCompactPart(
    MergeTreeData & data,
    const NamesAndTypesList & part_columns,
    MergeTreeData::MutableDataPartPtr & new_data_part,
    const MergeTreeData::DataPartPtr & target_part,
    const IngestPartition::IngestSources & src_blocks,
    const Names & ingest_column_names, const Names & ordered_key_names,
    const Settings & settings,
    std::function<bool()> check_cached_cancel
)
{
    bool read_with_direct_io = settings.min_bytes_to_use_direct_io != 0 &&
                                target_part->getBytesOnDisk() >= settings.min_bytes_to_use_direct_io;
    auto source_input = std::make_shared<MergeTreeSequentialSource>(data,
                                                                    data.getInMemoryMetadataPtr(),
                                                                    target_part,
                                                                    nullptr,
                                                                    part_columns.getNames(),
                                                                    read_with_direct_io, true);

    QueryPipeline source_pipeline;
    source_pipeline.init(Pipe(std::move(source_input)));
    source_pipeline.setMaxThreads(1);
    BlockInputStreamPtr pipeline_input_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(source_pipeline));
    pipeline_input_stream = std::make_shared<SquashingBlockInputStream>(pipeline_input_stream, settings.min_insert_block_size_rows * 2,
                                                         settings.min_insert_block_size_bytes * 4);

    auto compression_codec = target_part->default_codec;
    MergeTreeIndices indices_to_recalc;
    for (const auto & index : data.getInMemoryMetadataPtr()->getSecondaryIndices())
        indices_to_recalc.push_back(MergeTreeIndexFactory::instance().get(index));

    /**
     *
     * for compact format part, implicit map column should be compacted into map column,
     * so that we should not write them anymore.
     */
    NamesAndTypesList compact_out_columns;
    for (auto & column : part_columns)
    {
        if (isMapImplicitKeyNotKV(column.name))
            continue;

        compact_out_columns.push_back(column);
    }

    MergedBlockOutputStream out(
            new_data_part,
            data.getInMemoryMetadataPtr(),
            compact_out_columns,
            indices_to_recalc,
            compression_codec);

    auto match_type = std::make_shared<DataTypeUInt8>();

    pipeline_input_stream->readPrefix();
    out.writePrefix();

    IMergeTreeDataPart::MinMaxIndex minmax_idx;
    Block block;
    while (!check_cached_cancel() && (block = pipeline_input_stream->read()))
    {
        /// append match count column into block
        auto match_col = match_type->createColumn();
        typeid_cast<ColumnUInt8 &>(*match_col).getData().resize_fill(block.rows());
        block.insert(ColumnWithTypeAndName(std::move(match_col), match_type, "___match_count"));

        /// assume blocks already sorted by keys, implement join semantics
        const auto & res_block = blockJoinBlocks(data, block, src_blocks, ingest_column_names, ordered_key_names, true);

        minmax_idx.update(res_block, data.getMinMaxColumnsNames(data.getInMemoryMetadataPtr()->getPartitionKey()));

        out.write(res_block);
    }


    if (check_cached_cancel())
        throw Exception("INGEST PARTITION was cancelled", ErrorCodes::ABORTED);

    new_data_part->minmax_idx = std::move(minmax_idx);
    pipeline_input_stream->readSuffix();
    out.writeSuffixAndFinalizePart(new_data_part, false);
}

/**
 * Outer join the target_block with the src_blocks
 *
 * @param ingest_column_names The columns that going to be ingested in src_blocks
 * @param ordered_key_names The join keys
 */
Block IngestPartition::blockJoinBlocks(MergeTreeData & data,
                                       Block & target_block,
                                       const IngestPartition::IngestSources & src_blocks,
                                       const Names & ingest_column_names,
                                       const Names & ordered_key_names,
                                       bool compact)
{
    Columns target_key_cols;
    for (auto & key : ordered_key_names)
        target_key_cols.push_back(target_block.getByName(key).column);

    auto & target_match_col = const_cast<ColumnUInt8::Container &>(typeid_cast<const ColumnUInt8 &>(
            *(target_block.getByName("___match_count").column)).getData());

    std::unordered_map<String, std::vector<Field>> column_values;
    for (auto & col_name : ingest_column_names)
        column_values.emplace(std::make_pair(col_name, std::vector<Field>(target_block.rows(), Field())));

    for (const auto & src_block : src_blocks)
    {
        auto lock = std::lock_guard<std::mutex>(src_block->mutex);
        //LOG_TRACE(&Logger::get("blockJoinBlocks"), "Begin join block " + std::to_string(src_block->id) + " to target part: " + target_part->name);

        Columns src_key_cols;
        for (auto & key : ordered_key_names)
            src_key_cols.push_back(src_block->block.getByName(key).column);

        auto & src_match_col = const_cast<ColumnUInt8::Container &>(typeid_cast<const ColumnUInt8 &>(
                *(src_block->block.getByName("___match_count").column)).getData());

        for (size_t n = 0, m = 0; n < target_block.rows() && m < src_block->block.rows();)
        {
            auto order = compare(target_key_cols, src_key_cols, n, m);
            if (order < 0)
                ++n;
            else if (order > 0)
                ++m;
            else
            {
                if (target_match_col[n] == 0 && src_match_col[m] == 0)
                {
                    for (auto & col_name : ingest_column_names)
                        src_block->block.getByName(col_name).column->get(m, column_values[col_name][n]);

                    target_match_col[n] = 1;
                    src_match_col[m] = 1;

                    ++n;
                    ++m;
                }
                else
                {
                    throw Exception("Found duplicate primary keys", ErrorCodes::INGEST_PARTITON_DUPLICATE_KEYS);
                }
            }
        }

        //LOG_TRACE(&Logger::get("blockJoinBlocks"), "end join block " + std::to_string(src_block->id) + " to target part: " + target_part->name);
    }

    Block res;
    /// append columns that going to be ingested
    for (auto & col_name : ingest_column_names)
    {
        auto & old_column = target_block.getByName(col_name);

        auto target_column = old_column.type->createColumn();
        auto & values = column_values[col_name];

        for (size_t n = 0; n < target_block.rows(); ++n)
        {
            if (values[n].getType() == Field::Types::Null)
            {
                if (data.getSettings()->ingest_default_column_value_if_not_provided)
                {
                    target_column->insertDefault();
                    continue;
                }
                else
                {
                    old_column.column->get(n, values[n]);
                }
            }

            target_column->insert(values[n]);
        }

        auto col_type = old_column.type;

        res.insert(ColumnWithTypeAndName(std::move(target_column), col_type, col_name));
    }

    if (compact)
    {
        std::unordered_map<String, Names> implicit_names_mapping;
        for (auto & col_name : ingest_column_names)
        {
            if (isMapImplicitKeyNotKV(col_name))
            {
                auto map_col_name = parseMapNameFromImplicitColName(col_name);
                implicit_names_mapping[map_col_name].push_back(col_name);
            }
        }

        NameSet ingested_column(ingest_column_names.begin(), ingest_column_names.end());
        for (size_t i = 0; i < target_block.columns(); ++i)
        {
            auto column_with_type_name = target_block.getByPosition(i);
            if (ingested_column.count(column_with_type_name.name))
                continue;

            /***
             * In compact format part, we should compact all implicit map column into one ColumnMap
             * if and only if target part already has this ColumnMap.
             * e.g.
             * map1{'k1'}, map1{'k2'}, implicit column k1 and k2 should compact to map1
             */
            auto it = implicit_names_mapping.find(column_with_type_name.name);
            auto * map_column = const_cast<ColumnByteMap *>(
                                    typeid_cast<const ColumnByteMap *>(column_with_type_name.column.get()));
            if (it != implicit_names_mapping.end() && map_column)
            {
                std::unordered_map<String, ColumnPtr> implicit_columns;
                auto const & map_key_type = dynamic_cast<const DataTypeByteMap*>(column_with_type_name.type.get())->getKeyType();
                for (auto & col_name : it->second)
                {
                    /// Attention: key_name has been quoted, we need to remove the quote.
                    auto key_name = parseKeyNameFromImplicitColName(col_name, it->first);
                    ColumnWithTypeAndName * col_type_name = res.findByName(col_name);
                    ColumnPtr implicit_col = col_type_name->column;

                    key_name = map_key_type->stringToVisitorString(key_name);
                    implicit_columns[key_name] = std::move(implicit_col);
                    res.erase(col_name);
                }

                map_column->insertImplicitMapColumns(implicit_columns);
            }

            res.insert(ColumnWithTypeAndName(std::move(column_with_type_name.column), column_with_type_name.type, column_with_type_name.name));
        }
    }

    return res;
}

zkutil::EphemeralNodeHolderPtr IngestPartition::getIngestTaskLock(const zkutil::ZooKeeperPtr & zk_ptr, const String & zookeeper_path, const String & replica_name, const String & partition_id)
{
    String partitionHash = toString(std::hash<String>{}(partition_id));

    /**
     * Hold a zookeeper lock for local log, local log means the log generated in node receiving ingestion task.
     * We think only the local log needs to race condition since they can generate new part and GET_PART.
     * To avoid re-generating same rows, same partitions should be ingested in order.
     **/
    try
    {
        return zkutil::EphemeralNodeHolder::create(zookeeper_path + "/ingesting_" + partitionHash, *zk_ptr, replica_name);
    }
    catch(Coordination::Exception & e)
    {
        if (e.code == Coordination::Error::ZNODEEXISTS)
        {
            e.addMessage("only one ongoing ingesting task of same partition id is accepted for local log, please wait for the current task to complete " + partition_id);
        }
        throw;
    }
}

String IngestPartition::parseIngestPartition(const String & part_name, const MergeTreeDataFormatVersion & format_version)
{
    String partition_id;
    MergeTreePartInfo part_info;
    if (!MergeTreePartInfo::tryParsePartName(part_name, &part_info, format_version))
        partition_id = part_name;
    else
        partition_id = part_info.partition_id;

    return partition_id;
}

}
