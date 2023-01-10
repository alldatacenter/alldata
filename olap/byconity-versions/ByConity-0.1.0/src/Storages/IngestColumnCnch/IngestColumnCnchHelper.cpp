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

#include <Storages/IngestColumnCnch/IngestColumnCnchHelper.h>
#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <Storages/MergeTree/MergedColumnOnlyOutputStream.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/ColumnsDescription.h>
#include <DataStreams/SquashingBlockInputStream.h>
#include <DataStreams/SquashingBlockOutputStream.h>
#include <DataStreams/AddingDefaultBlockOutputStream.h>
#include <DataStreams/BlocksListBlockInputStream.h>
#include <Processors/Executors/PipelineExecutingBlockInputStream.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/commitCnchParts.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/MapHelpers.h>
#include <Parsers/queryToString.h>
#include <Catalog/Catalog.h>
#include <Protos/DataModelHelpers.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INGEST_PARTITON_DUPLICATE_KEYS;
    extern const int SYSTEM_ERROR;
}

namespace
{

MergeTreeDataPartsVector getAndLoadPartsInWorker(
    Catalog::Catalog & catalog,
    StoragePtr storage,
    StorageCloudMergeTree & cloud_merge_tree,
    String partition_id,
    ContextPtr local_context)
{
    ServerDataPartsVector server_parts = catalog.getServerDataPartsInPartitions(storage, {partition_id}, local_context->getCurrentCnchStartTime(), local_context.get());

    pb::RepeatedPtrField<Protos::DataModelPart> parts_model;
    StoragePtr storage_for_cnch_merge_tree = catalog.tryGetTable(
        *local_context,
        cloud_merge_tree.getCnchDatabase(),
        cloud_merge_tree.getCnchTable(),
        local_context->getCurrentCnchStartTime());

    if (!storage_for_cnch_merge_tree)
        throw Exception("Fail to get storage from catalog", ErrorCodes::SYSTEM_ERROR);

    fillPartsModelForSend(*storage_for_cnch_merge_tree, server_parts, parts_model);

    MergeTreeMutableDataPartsVector unloaded_parts =
        createPartVectorFromModelsForSend<MergeTreeMutableDataPartPtr>(cloud_merge_tree, parts_model);

    cloud_merge_tree.loadDataParts(unloaded_parts, false);

    MergeTreeDataPartsVector res;
    std::move(unloaded_parts.begin(), unloaded_parts.end(),
        std::back_inserter(res));
    return res;
}

}

namespace IngestColumnCnch
{

std::optional<NameAndTypePair> tryGetMapColumn(const StorageInMemoryMetadata & meta_data, const String & col_name)
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

void writeNewPart(const StorageInMemoryMetadata & meta_data, const MemoryInefficientIngest::IngestSources & src_blocks, BlockOutputStreamPtr & output, Names & all_columns_with_partition_key)
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

IMergeTreeMutableDataPartPtr createEmptyTempPart(
    MergeTreeMetaBase & data,
    const MergeTreeDataPartPtr & part,
    const Names & ingest_column_names,
    ReservationPtr& reserved_space,
    const Context & context)
{
    auto new_part_info = part->info;
    new_part_info.level += 1;
    new_part_info.hint_mutation = new_part_info.mutation;
    new_part_info.mutation = context.getCurrentTransactionID().toUInt64();

    auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + part->name, reserved_space->getDisk(), 0);

    auto new_partial_part = data.createPart(part->name, MergeTreeDataPartType::WIDE,
        new_part_info, single_disk_volume, "tmp_mut_" + part->name,
        nullptr, IStorage::StorageLocation::AUXILITY);

    new_partial_part->uuid = part->uuid;
    new_partial_part->is_temp = true;
    new_partial_part->ttl_infos = part->ttl_infos;
    new_partial_part->versions = part->versions;

    new_partial_part->index_granularity_info = part->index_granularity_info;
    new_partial_part->setColumns(part->getColumns().filter(ingest_column_names));
    new_partial_part->partition.assign(part->partition);
    new_partial_part->columns_commit_time = part->columns_commit_time;
    new_partial_part->mutation_commit_time = part->mutation_commit_time;
    if (data.isBucketTable())
        new_partial_part->bucket_number = part->bucket_number;

    return new_partial_part;
}

void finalizeTempPart(
    const MergeTreeDataPartPtr & ingest_part,
    MergeTreeData::MutableDataPartPtr new_partial_part,
    const CompressionCodecPtr & codec)
{
    auto disk = new_partial_part->volume->getDisk();
    auto new_part_checksums_ptr = new_partial_part->getChecksums();

    if (new_partial_part->uuid != UUIDHelpers::Nil)
    {
        auto out = disk->writeFile(new_partial_part->getFullRelativePath() + IMergeTreeDataPart::UUID_FILE_NAME, {.buffer_size = 4096});
        HashingWriteBuffer out_hashing(*out);
        writeUUIDText(new_partial_part->uuid, out_hashing);
        new_part_checksums_ptr->files[IMergeTreeDataPart::UUID_FILE_NAME].file_size = out_hashing.count();
        new_part_checksums_ptr->files[IMergeTreeDataPart::UUID_FILE_NAME].file_hash = out_hashing.getHash();
    }

    {
        /// Write file with checksums.
        auto out_checksums = disk->writeFile(fs::path(new_partial_part->getFullRelativePath()) / "checksums.txt", {.buffer_size = 4096});
        new_part_checksums_ptr->versions = new_partial_part->versions;
        new_part_checksums_ptr->write(*out_checksums);
    } /// close fd

    {
        auto out = disk->writeFile(new_partial_part->getFullRelativePath() + IMergeTreeDataPart::DEFAULT_COMPRESSION_CODEC_FILE_NAME, {.buffer_size = 4096});
        DB::writeText(queryToString(codec->getFullCodecDesc()), *out);
    }

    {
        /// Write a file with a description of columns.
        auto out_columns = disk->writeFile(fs::path(new_partial_part->getFullRelativePath()) / "columns.txt", {.buffer_size = 4096});
        new_partial_part->getColumns().writeText(*out_columns);
    } /// close fd

    new_partial_part->rows_count = ingest_part->rows_count;
    new_partial_part->index_granularity = ingest_part->index_granularity;
    new_partial_part->index = ingest_part->getIndex();
    new_partial_part->minmax_idx = ingest_part->minmax_idx;
    new_partial_part->modification_time = time(nullptr);
    new_partial_part->loadProjections(false, false);
    new_partial_part->setBytesOnDisk(
        MergeTreeData::DataPart::calculateTotalSizeOnDisk(new_partial_part->volume->getDisk(), new_partial_part->getFullRelativePath()));
    new_partial_part->default_codec = codec;
}

int compare(Columns & target_key_cols, Columns & src_key_cols, size_t n, size_t m)
{
    for (size_t i = 0; i < target_key_cols.size(); ++i)
    {
        auto order = target_key_cols[i]->compareAt(n, m, *(src_key_cols[i]), 1);
        if (order != 0)
            return order;
    }
    return 0;
}

Block blockJoinBlocksCnch(Block & target_block, const MemoryInefficientIngest::IngestSources & src_blocks, const Names & ingest_column_names, const Names & ordered_key_names, const MergeTreeMetaBase & target_table)
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
            auto order = IngestColumnCnch::compare(target_key_cols, src_key_cols, n, m);
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
                if (target_table.getSettings()->ingest_default_column_value_if_not_provided)
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

    return res;
}

MergeTreeMutableDataPartPtr ingestPart(
    MergeTreeMetaBase & target_table,
    MergeTreeDataPartPtr ingest_part,
    const MemoryInefficientIngest::IngestSources & src_blocks,
    const Names & ingest_column_names,
    const Names & ordered_key_names,
    const Names & all_columns,
    const Settings & settings,
    ContextPtr context)
{
    bool read_with_direct_io = settings.min_bytes_to_use_direct_io != 0 &&
                               ingest_part->bytes_on_disk >= settings.min_bytes_to_use_direct_io;


    auto source_input = std::make_unique<MergeTreeSequentialSource>(
        target_table,
        target_table.getInMemoryMetadataPtr(),
        ingest_part, all_columns, read_with_direct_io, true);

    QueryPipeline source_pipeline;
    source_pipeline.init(Pipe(std::move(source_input)));
    source_pipeline.setMaxThreads(1);
    BlockInputStreamPtr pipeline_input_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(source_pipeline));

    pipeline_input_stream = std::make_shared<SquashingBlockInputStream>(pipeline_input_stream, settings.min_insert_block_size_rows, settings.min_insert_block_size_bytes);

    Block header;
    for (auto & col_name : ingest_column_names)
    {
        auto & src_column = src_blocks.at(0)->block.getByName(col_name);
        header.insert(ColumnWithTypeAndName(src_column.type, src_column.name));
    }

    auto match_type = std::make_shared<DataTypeUInt8>();
    BlocksList res_block_list;
    pipeline_input_stream->readPrefix();

    while (auto block = pipeline_input_stream->read())
    {
        /// append match count column into block
        auto match_col = match_type->createColumn();
        typeid_cast<ColumnUInt8 &>(*match_col).getData().resize_fill(block.rows());
        block.insert(ColumnWithTypeAndName(std::move(match_col), match_type, "___match_count"));

        /// assume blocks already sorted by keys, implement join semantics
        auto res_block = blockJoinBlocksCnch(block, src_blocks, ingest_column_names, ordered_key_names, target_table);
        res_block_list.push_back(std::move(res_block));
    }

    pipeline_input_stream->readSuffix();

    //create new part

    auto estimated_space_for_result = static_cast<size_t>(ingest_part->getBytesOnDisk());

    ReservationPtr reserved_space = target_table.reserveSpace(estimated_space_for_result,
        IStorage::StorageLocation::AUXILITY);
    auto new_partial_part = createEmptyTempPart(target_table, ingest_part, ingest_column_names, reserved_space, *context);
    auto disk = new_partial_part->volume->getDisk();
    String new_part_tmp_path = new_partial_part->getFullRelativePath();
    SyncGuardPtr sync_guard;
    if (target_table.getSettings()->fsync_part_directory)
        sync_guard = disk->getDirectorySyncGuard(new_part_tmp_path);

    auto old_checksums = ingest_part->getChecksums();
    /// empty checksums, will update in mutateSomePartColumns
    new_partial_part->checksums_ptr = std::make_shared<MergeTreeData::DataPart::Checksums>();

    auto compression_codec = ingest_part->default_codec;

    if (!compression_codec)
        throw Exception(ErrorCodes::BAD_ARGUMENTS, "Unknown codec for mutate part: {}", ingest_part->name);

    BlockInputStreamPtr res_block_in = std::make_shared<BlocksListBlockInputStream>(std::move(res_block_list));
    MergedColumnOnlyOutputStream out(
        new_partial_part,
        target_table.getInMemoryMetadataPtr(),
        res_block_in->getHeader(),
        compression_codec,
        {},
        nullptr,
        ingest_part->index_granularity,
        &ingest_part->index_granularity_info
    );

    res_block_in->readPrefix();
    out.writePrefix();
    while (Block block = res_block_in->read())
        out.write(block);
    res_block_in->readSuffix();

    auto changed_checksums = out.writeSuffixAndGetChecksums(new_partial_part, *(new_partial_part->getChecksums()));
    new_partial_part->checksums_ptr->add(std::move(changed_checksums));

    finalizeTempPart(ingest_part, new_partial_part, compression_codec);
    return new_partial_part;
}

void ingestionCnch(
    MergeTreeMetaBase & data,
    IMergeTreeDataPartsVector && parts_to_ingest,
    const MemoryInefficientIngest::IngestSources & src_blocks,
    const Names & ingest_column_names,
    const Names & ordered_key_names,
    const Names & all_columns,
    const Settings & settings,
    ContextPtr context)
{
    IMutableMergeTreeDataPartsVector new_partial_parts;
    new_partial_parts.reserve(parts_to_ingest.size());
    std::reverse(parts_to_ingest.begin(), parts_to_ingest.end());
    std::mutex parts_to_ingest_mutex;
    std::mutex new_partitial_parts_mutex;
    std::atomic<bool> has_ingest_exception = false;

    const size_t num_threads = std::min(settings.parallel_ingest_threads.value, parts_to_ingest.size());
    Exceptions exceptions(num_threads);
    int count = 0;

    auto ingest_part_func = [&]()
    {
        int idx = 0;
        {
            auto count_lock = std::lock_guard<std::mutex>(new_partitial_parts_mutex);
            idx = count;
            ++count;
        }

        LOG_TRACE(data.getLogger(), "ingestionCnch thread idx: {}", idx);

        while (1)
        {
            MergeTreeDataPartPtr part = nullptr;

            {
                auto parts_to_ingest_lock = std::lock_guard<std::mutex>(parts_to_ingest_mutex);
                if (parts_to_ingest.empty() || has_ingest_exception)
                    return;

                part = parts_to_ingest.back();
                parts_to_ingest.pop_back();
            }

            if (!part)
                return;

            try
            {
                MergeTreeMutableDataPartPtr new_partial_part = ingestPart(data, part, src_blocks, ingest_column_names, ordered_key_names, all_columns, settings, context);

                {
                    auto new_partitial_parts_lock = std::lock_guard<std::mutex>(new_partitial_parts_mutex);
                    new_partial_parts.push_back(std::move(new_partial_part));
                }
            }
            catch(...)
            {
                tryLogCurrentException(__PRETTY_FUNCTION__);
                has_ingest_exception = true;
                exceptions[idx] = std::current_exception();
                return;
            }
        }
    };

    std::unique_ptr<ThreadPool> thread_pool = std::make_unique<ThreadPool>(num_threads);
    for (size_t i = 0; i < num_threads; i++)
        thread_pool->scheduleOrThrowOnError(ingest_part_func);

    thread_pool->wait();
    if (has_ingest_exception)
        rethrowFirstException(exceptions);

    CnchDataWriter cnch_writer(data, context, ManipulationType::Insert, context->getCurrentQueryId());
    cnch_writer.dumpAndCommitCnchParts(new_partial_parts);
}

MemoryInefficientIngest::MemoryInefficientIngest(
        StoragePtr target_storage_,
        StoragePtr source_storage_,
        String partition_id_,
        Strings ingest_column_names_,
        Strings ordered_key_names_,
        ContextPtr context_)
        : target_storage{std::move(target_storage_)}
        , source_storage{std::move(source_storage_)}
        , target_cloud_merge_tree{dynamic_cast<StorageCloudMergeTree *>(target_storage.get())}
        , source_cloud_merge_tree{dynamic_cast<StorageCloudMergeTree *>(source_storage.get())}
        , partition_id{std::move(partition_id_)}
        , ingest_column_names{std::move(ingest_column_names_)}
        , ordered_key_names{std::move(ordered_key_names_)}
        , target_parts{}
        , source_parts{}
        , context{std::move(context_)}
        , log(target_cloud_merge_tree->getLogger())
{
    if (!target_cloud_merge_tree)
        throw Exception("target table in worker is not CloudMeregTree", ErrorCodes::LOGICAL_ERROR);
    if (!source_cloud_merge_tree)
        throw Exception("source table in worker is not CloudMeregTree", ErrorCodes::LOGICAL_ERROR);

    std::shared_ptr<Catalog::Catalog> catalog = context->getCnchCatalog();
    source_parts = getAndLoadPartsInWorker(
        *catalog, source_storage, *source_cloud_merge_tree, partition_id, context);

    target_parts = getAndLoadPartsInWorker(
        *catalog, target_storage, *target_cloud_merge_tree, partition_id, context);
}

std::vector<MemoryInefficientIngest::IngestSources> MemoryInefficientIngest::readSourceParts(const Names & all_columns_with_partition_key)
{
    //auto name_node = std::make_pair(context.getNNHost(), context.getNNPort());
    auto match_type = std::make_shared<DataTypeUInt8>();

    IMergeTreeDataPartsVector visible_source_parts = CnchPartsHelper::calcVisibleParts(source_parts, false, CnchPartsHelper::EnableLogging);

    const auto & settings = context->getSettingsRef();
    const size_t num_threads = std::min(settings.parallel_ingest_threads.value, visible_source_parts.size());
    std::vector<MemoryInefficientIngest::IngestSources> res(num_threads);
    Exceptions exceptions(num_threads);
    std::mutex read_mutex;
    int count = 0;
    std::atomic<bool> has_read_exception = false;
    auto read_part_func = [&]()
    {
        int idx = 0;
        {
            auto count_lock = std::lock_guard<std::mutex>(read_mutex);
            idx = count;
            ++count;
        }
        LOG_TRACE(log, "readSourceParts thread idx: {}", idx);

        while (1)
        {
            MergeTreeDataPartPtr part = nullptr;

            {
                auto read_lock = std::lock_guard<std::mutex>(read_mutex);
                if (visible_source_parts.empty() || has_read_exception)
                    return;

                part = visible_source_parts.back();
                visible_source_parts.pop_back();
            }

            if (!part)
                return;

            try
            {
                bool read_with_direct_io = settings.min_bytes_to_use_direct_io != 0 &&
                               part->bytes_on_disk >= settings.min_bytes_to_use_direct_io;

                auto source_input = std::make_unique<MergeTreeSequentialSource>(
                    *source_cloud_merge_tree,
                    source_storage->getInMemoryMetadataPtr(),
                    part, all_columns_with_partition_key, read_with_direct_io, true);

                QueryPipeline source_pipeline;
                source_pipeline.init(Pipe(std::move(source_input)));
                source_pipeline.setMaxThreads(1);
                BlockInputStreamPtr pipeline_input_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(source_pipeline));
                pipeline_input_stream = std::make_shared<SquashingBlockInputStream>(pipeline_input_stream, settings.min_insert_block_size_rows * 2, settings.min_insert_block_size_bytes * 4);

                pipeline_input_stream->readPrefix();

                while (Block block = pipeline_input_stream->read())
                {
                    // append match count column into block
                    auto match_col = match_type->createColumn();
                    typeid_cast<ColumnUInt8 &>(*match_col).getData().resize_fill(block.rows());
                    block.insert(ColumnWithTypeAndName(std::move(match_col), match_type, "___match_count"));
                    res[idx].push_back(std::make_shared<IngestSource>(block));
                }
                pipeline_input_stream->readSuffix();
            }
            catch(...)
            {
                tryLogCurrentException(__PRETTY_FUNCTION__);
                has_read_exception = true;
                exceptions[idx] = std::current_exception();
                return;
            }
        }
    };

    std::unique_ptr<ThreadPool> thread_pool = std::make_unique<ThreadPool>(num_threads);
    for (size_t i = 0; i < num_threads; i++)
        thread_pool->scheduleOrThrowOnError(read_part_func);

    thread_pool->wait();
    if (has_read_exception)
        rethrowFirstException(exceptions);

    return res;
}

void MemoryInefficientIngest::ingestPartition()
{
    Names all_columns{ingest_column_names};
    all_columns.insert(all_columns.end(), ordered_key_names.begin(), ordered_key_names.end());

    Names all_columns_with_partition_key(all_columns.begin(), all_columns.end());
    for (auto & partition_key : source_storage->getInMemoryMetadataPtr()->getColumnsRequiredForPartitionKey())
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

    Stopwatch watch;
    std::vector<MemoryInefficientIngest::IngestSources> src_blocks_vec = readSourceParts(all_columns_with_partition_key);
    LOG_DEBUG(log, "read source parts tooks {} s", watch.elapsedSeconds());

    MemoryInefficientIngest::IngestSources src_blocks;
    std::for_each(src_blocks_vec.begin(), src_blocks_vec.end(),
        [&src_blocks] (MemoryInefficientIngest::IngestSources & e)
        {
            std::move(e.begin(), e.end(), std::back_inserter(src_blocks));
        });

    const auto & settings = context->getSettingsRef();
    /// STEP 2: Perform outer join, complete the ingestion
    if (!target_parts.empty())
    {
        IMergeTreeDataPartsVector visible_target_parts = CnchPartsHelper::calcVisibleParts(target_parts, false, CnchPartsHelper::EnableLogging);

        LOG_DEBUG(log, "number of visible_target_parts size {}", visible_target_parts.size());

        watch.restart();
        ingestionCnch(*target_cloud_merge_tree, std::move(visible_target_parts), src_blocks, ingest_column_names, ordered_key_names, all_columns, settings, context);
        LOG_DEBUG(log, "join tooks {} s", watch.elapsedSeconds());
    }

    /// STEP 3: make new parts for data in src_block but not matched
    const StorageMetadataPtr target_meta_data_ptr = target_storage->getInMemoryMetadataPtr();
    BlockOutputStreamPtr new_part_output = target_storage->write(ASTPtr(), target_meta_data_ptr, context);
    Block sample_block = target_meta_data_ptr->getSampleBlock();
    new_part_output = std::make_shared<SquashingBlockOutputStream>(
        new_part_output, sample_block, settings.min_insert_block_size_rows, settings.min_insert_block_size_bytes);

    Block ingested_header;
    for (const auto & name : all_columns_with_partition_key)
    {
        auto column_name = name;
        /// No need to add implicit map column
        if (isMapImplicitKeyNotKV(name))
            column_name = parseMapNameFromImplicitColName(name);
        auto column = target_meta_data_ptr->getColumns().getColumnOrSubcolumn(ColumnsDescription::GetFlags::AllPhysical, column_name);
        ingested_header.insertUnique(ColumnWithTypeAndName(column.type, column.name));
    }

    new_part_output = std::make_shared<AddingDefaultBlockOutputStream>(
        new_part_output,
        ingested_header,
        target_meta_data_ptr->getColumns(),
        context);

    writeNewPart(*target_meta_data_ptr, src_blocks, new_part_output, all_columns_with_partition_key);
}

} /// end namespace IngestColumnCnch

}
