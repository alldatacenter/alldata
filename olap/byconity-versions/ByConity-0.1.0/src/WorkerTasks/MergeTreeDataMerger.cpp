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

#include <WorkerTasks/MergeTreeDataMerger.h>

#include <Common/ProfileEvents.h>
#include <Common/filesystemHelpers.h>
#include <DataStreams/ColumnGathererStream.h>
#include <DataStreams/ExpressionBlockInputStream.h>
#include <DataStreams/MaterializingBlockInputStream.h>
#include <DataTypes/DataTypeByteMap.h>
#include <Processors/Executors/PipelineExecutingBlockInputStream.h>
#include <Processors/Merges/AggregatingSortedTransform.h>
#include <Processors/Merges/CollapsingSortedTransform.h>
#include <Processors/Merges/GraphiteRollupSortedTransform.h>
#include <Processors/Merges/MergingSortedTransform.h>
#include <Processors/Merges/ReplacingSortedTransform.h>
#include <Processors/Merges/SummingSortedTransform.h>
#include <Processors/Merges/VersionedCollapsingTransform.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <Processors/Transforms/MaterializingTransform.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/MergeTree/MergeTreeDataPartWide.h>
#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <Storages/MergeTree/MergedBlockOutputStream.h>
#include <Storages/MergeTree/MergedColumnOnlyOutputStream.h>
#include <WorkerTasks/ManipulationTaskParams.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>


namespace ProfileEvents
{
    extern const Event CloudMergeStarted;
    extern const Event CloudMergeEnded;
}

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int ABORTED;
    extern const int DIRECTORY_ALREADY_EXISTS;
}

namespace
{
    String TMP_PREFIX = "tmp_merge_";
    constexpr auto DISK_USAGE_COEFFICIENT_TO_RESERVE = 1.1;

    size_t estimateNeededDiskSpace(const MergeTreeDataPartsVector & source_parts)
    {
        size_t res = 0;
        for (const auto & part : source_parts)
            res += part->getBytesOnDisk();
        return static_cast<size_t>(res * DISK_USAGE_COEFFICIENT_TO_RESERVE);
    }

    String toDebugString(const MergeTreeDataPartsVector & parts)
    {
        WriteBufferFromOwnString out;
        for (const auto & part : parts)
        {
            out << part->name;
            out << ' ';
        }
        return std::move(out.str());
    }

    SortDescription getSortDescription(const Names & sort_columns, const Block & header)
    {
        SortDescription sort_description;
        size_t sort_columns_size = sort_columns.size();
        sort_description.reserve(sort_columns_size);

        for (size_t i = 0; i < sort_columns_size; ++i)
            sort_description.emplace_back(header.getPositionByName(sort_columns[i]), 1, 1);
        return sort_description;
    }
}

MergeTreeDataMerger::MergeTreeDataMerger(
    MergeTreeMetaBase & data_,
    const ManipulationTaskParams & params_,
    ContextPtr context_,
    ManipulationListElement * manipulation_entry_,
    CheckCancelCallback check_cancel_,
    bool build_rowid_mappings_)
    : data(data_)
    , data_settings(data.getSettings())
    , metadata_snapshot(data.getInMemoryMetadataPtr())
    , params(params_)
    , context(context_)
    , manipulation_entry(manipulation_entry_)
    , check_cancel(std::move(check_cancel_))
    , build_rowid_mappings(build_rowid_mappings_)
    , rowid_mappings(params.source_data_parts.size())
    , log(&Poco::Logger::get(data.getLogName() + " (Merger)"))
{
    if (build_rowid_mappings && data.merging_params.mode != MergeTreeMetaBase::MergingParams::Ordinary)
        throw Exception(
            ErrorCodes::LOGICAL_ERROR, "Rowid mapping is only supported for Ordinal mode, got {}", data.merging_params.getModeName());
}

MergeTreeDataMerger::~MergeTreeDataMerger()
{
    /// Make sure source streams are released before prefetcher
    merged_stream.reset();
    // if (prefetcher)
    //     prefetcher.reset();
}

void MergeTreeDataMerger::prepareColumnNamesAndTypes()
{
    all_column_names = metadata_snapshot->getColumns().getNamesOfPhysical();
    storage_columns = metadata_snapshot->getColumns().getAllPhysical();

    Names sort_key_columns_vec = metadata_snapshot->getSortingKey().expression->getRequiredColumns();
    std::set<String> key_columns(sort_key_columns_vec.cbegin(), sort_key_columns_vec.cend());
    for (const auto & index : metadata_snapshot->getSecondaryIndices())
    {
        Names index_columns_vec = index.expression->getRequiredColumns();
        std::copy(index_columns_vec.cbegin(), index_columns_vec.cend(), std::inserter(key_columns, key_columns.end()));
    }

    for (const auto & projection : metadata_snapshot->getProjections())
    {
        Names projection_columns_vec = projection.required_columns;
        std::copy(projection_columns_vec.cbegin(), projection_columns_vec.cend(), std::inserter(key_columns, key_columns.end()));
    }

    const auto & merging_params = data.merging_params;

    /// Force unique key columns and extra column for Unique mode,
    /// otherwise MergedBlockOutputStream won't have the required columns to generate unique key index file.
    if (metadata_snapshot->hasUniqueKey())
    {
        auto unique_key_expr = metadata_snapshot->getUniqueKey().expression;
        if (!unique_key_expr)
            throw Exception("Missing unique key expression for Unique mode", ErrorCodes::LOGICAL_ERROR);

        Names index_columns_vec = unique_key_expr->getRequiredColumns();
        std::copy(index_columns_vec.cbegin(), index_columns_vec.cend(), std::inserter(key_columns, key_columns.end()));

        /// also need version column when building unique key index file
        if (!merging_params.hasExplicitVersionColumn())
            key_columns.insert(merging_params.version_column);
    }

    /// Force sign column for Collapsing mode
    if (merging_params.mode == MergeTreeMetaBase::MergingParams::Collapsing)
        key_columns.emplace(merging_params.sign_column);

    /// Force version column for Replacing mode
    if (merging_params.mode == MergeTreeMetaBase::MergingParams::Replacing)
        key_columns.emplace(merging_params.version_column);

    /// Force sign column for VersionedCollapsing mode. Version is already in primary key.
    if (merging_params.mode == MergeTreeMetaBase::MergingParams::VersionedCollapsing)
        key_columns.emplace(merging_params.sign_column);

    /// Force to merge at least one column in case of empty key
    if (key_columns.empty())
        key_columns.emplace(storage_columns.front().name);

    /// TODO: also force "summing" and "aggregating" columns to make Horizontal merge only for such columns

    for (const auto & column : storage_columns)
    {
        if (key_columns.count(column.name))
        {
            merging_columns.emplace_back(column);
            merging_column_names.emplace_back(column.name);
        }
        else
        {
            gathering_columns.emplace_back(column);
            gathering_column_names.emplace_back(column.name);
        }
    }
}

void MergeTreeDataMerger::prepareNewParts()
{
    const auto & new_part_name = params.new_part_names.front();

    /// Check directory
    String new_part_tmp_path = TMP_PREFIX + toString(UInt64(context->getCurrentCnchStartTime())) + '-' + new_part_name;
    DiskPtr disk = space_reservation->getDisk();
    String new_part_tmp_rel_path = data.getRelativeDataPath(IStorage::StorageLocation::AUXILITY) + "/" + new_part_tmp_path;

    if (disk->exists(new_part_tmp_rel_path))
        throw Exception("Directory " + fullPath(disk, new_part_tmp_rel_path) + " already exists", ErrorCodes::DIRECTORY_ALREADY_EXISTS);
    disk->createDirectories(new_part_tmp_rel_path); /// TODO: could we remove it ?

    /// Create new data part object
    auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + new_part_name, disk, 0);
    auto part_info = MergeTreePartInfo::fromPartName(new_part_name, data.format_version);
    new_data_part = std::make_shared<MergeTreeDataPartWide>(data, new_part_name,
        part_info, single_disk_volume, new_part_tmp_path, nullptr,
        IStorage::StorageLocation::AUXILITY);

    /// Common fields
    /// TODO uuid
    new_data_part->setColumns(storage_columns);
    new_data_part->partition.assign(params.source_data_parts.front()->partition);
    new_data_part->is_temp = true;

    /// TODO: support TTL ?

    /// CNCH fields
    new_data_part->columns_commit_time = params.columns_commit_time;
    /// use max_mutation_commit_time
    /// because we will remove outdated columns, indices, bitmap indices, skip indices if need when merge new parts.
    TxnTimestamp mutation_commit_time = 0;
    for (const auto & part : params.source_data_parts)
        mutation_commit_time = std::max(mutation_commit_time, part->mutation_commit_time);
    new_data_part->mutation_commit_time = mutation_commit_time;

    if (params.is_bucket_table)
        new_data_part->bucket_number = params.source_data_parts.front()->bucket_number;
}


void MergeTreeDataMerger::chooseMergeAlgorithm()
{
    sum_input_rows_upper_bound = manipulation_entry->total_rows_count;

    if (data_settings->enable_vertical_merge_algorithm == 0)
    {
        merge_alg = MergeAlgorithm::Horizontal;
        return;
    }

    bool is_supported_storage = data.merging_params.mode == MergeTreeMetaBase::MergingParams::Ordinary
        || data.merging_params.mode == MergeTreeMetaBase::MergingParams::Collapsing
        || data.merging_params.mode == MergeTreeMetaBase::MergingParams::Replacing
        || data.merging_params.mode == MergeTreeMetaBase::MergingParams::VersionedCollapsing;

    // return MergeAlgorithm::Vertical if there is a map key since we cannot know how many keys in this column.
    // If there are too many keys, it may exhaust all file handles.
    if (is_supported_storage)
    {
        for (const auto & column : gathering_columns)
        {
            if (column.type->isMap() || column.type->lowCardinality())
            {
                merge_alg = MergeAlgorithm::Vertical;
                return;
            }
        }
    }

    bool enough_ordinary_cols = gathering_columns.size() >= data_settings->vertical_merge_algorithm_min_columns_to_activate;

    bool enough_total_rows = sum_input_rows_upper_bound >= data_settings->vertical_merge_algorithm_min_rows_to_activate;

    bool no_parts_overflow = params.source_data_parts.size() <= RowSourcePart::MAX_PARTS;

    merge_alg = (is_supported_storage && enough_total_rows && enough_ordinary_cols && no_parts_overflow) ? MergeAlgorithm::Vertical
                                                                                                         : MergeAlgorithm::Horizontal;

    LOG_DEBUG(log, "Selected MergeAlgorithm: {}", toString(MergeAlgorithm::Vertical));
}

void MergeTreeDataMerger::prepareForProgress()
{
    if (merge_alg == MergeAlgorithm::Vertical)
    {
        /// calc map { column -> size }
        for (const auto & part : params.source_data_parts)
            part->accumulateColumnSizes(merged_column_to_size);

        column_sizes.emplace(merged_column_to_size, merging_column_names, gathering_column_names);
    }

    horizontal_stage_progress.emplace(column_sizes ? column_sizes->keyColumnsWeight() : 1.0);
}

void MergeTreeDataMerger::prepareVerticalMerge()
{
    tmp_disk = context->getTemporaryVolume()->getDisk();
    rows_sources_file = createTemporaryFile(tmp_disk->getPath());
    rows_sources_uncompressed_write_buf = std::make_unique<WriteBufferFromFile>(rows_sources_file->path());
    rows_sources_write_buf = std::make_unique<CompressedWriteBuffer>(*rows_sources_uncompressed_write_buf);

    /// Collect implicit columns of byte map
    NamesAndTypesList old_gathering_columns;
    std::swap(old_gathering_columns, gathering_columns);
    for (auto & column : old_gathering_columns)
    {
        if (column.type->isMap() && !column.type->isMapKVStore())
        {
            auto [curr, end] = getMapColumnRangeFromOrderedFiles(column.name, merged_column_to_size);
            for (; curr != end; ++curr)
                gathering_columns.emplace_back(
                    curr->first, dynamic_cast<const DataTypeByteMap *>(column.type.get())->getValueTypeForImplicitColumn());
        }
        else
        {
            gathering_columns.push_back(column);
        }
    }
    gathering_columns.sort(); /// It gains btter performance if gathering by sorted columns
}

void MergeTreeDataMerger::resetMergingColumns()
{
    merging_columns = storage_columns;
    merging_column_names = all_column_names;
    gathering_columns.clear();
    gathering_column_names.clear();
}

void MergeTreeDataMerger::chooseCompressionCodec()
{
    /// TODO: replace with new impl
    compression_codec = context->chooseCompressionCodec(
        manipulation_entry->total_size_bytes_compressed,
        static_cast<double>(manipulation_entry->total_size_bytes_compressed) / data.getTotalActiveSizeInBytes());
}

void MergeTreeDataMerger::createSources()
{
    /*
    if (context.getSettingsRef().cnch_enable_merge_prefetch)
    {
        prefetcher = std::make_unique<CnchMergePrefetcher>(context, data, params.task_id);
        for (auto & part : params.source_data_parts)
            prefetcher->submitDataPart(part, merging_columns, gathering_columns);
    }
    */

    for (const auto & part : params.source_data_parts)
    {
        /// CnchMergePrefetcher::PartFutureFiles * future_files = prefetcher ? prefetcher->tryGetFutureFiles(part->name) : nullptr;

        auto input = std::make_unique<MergeTreeSequentialSource>(
            data,
            metadata_snapshot,
            part,
            nullptr, /// delete bitmap
            merging_column_names,
            false, // read_with_direct_io: We believe source parts will be mostly in page cache
            true, /// take_column_types_from_storage
            false /// quiet
        );
        input->setProgressCallback(ManipulationProgressCallback(manipulation_entry, watch_prev_elapsed, *horizontal_stage_progress));

        Pipe pipe(std::move(input));

        if (metadata_snapshot->hasSortingKey())
        {
            pipe.addSimpleTransform([this](const Block & header) {
                return std::make_shared<ExpressionTransform>(header, metadata_snapshot->getSortingKey().expression);
            });
        }

        pipes.emplace_back(std::move(pipe));
    }
}

void MergeTreeDataMerger::createMergedStream()
{
    auto header = pipes.front().getHeader();
    auto sort_description = getSortDescription(metadata_snapshot->getSortingKeyColumns(), header);

    /// The order of the streams is important: when the key is matched, the elements go in the order of the source stream number.
    /// In the merged part, the lines with the same key must be in the ascending order of the identifier of original part,
    ///  that is going in insertion order.
    ProcessorPtr merged_transform;

    /// If merge is vertical we cannot calculate it
    bool blocks_are_granules_size = (merge_alg == MergeAlgorithm::Vertical && !isCompactPart(new_data_part));

    MergingSortedAlgorithm::PartIdMappingCallback row_mapping_cb;
    if (build_rowid_mappings)
    {
        row_mapping_cb = [&](size_t part_index, size_t nrows) {
            for (size_t i = 0; i < nrows; ++i)
            {
                rowid_mappings[part_index].push_back(output_rowid++);
            }
        };
    }

    UInt64 merge_block_size = data_settings->merge_max_block_size;

    const auto & merging_params = data.merging_params;
    switch (merging_params.mode)
    {
        case MergeTreeMetaBase::MergingParams::Ordinary:
            merged_transform = std::make_unique<MergingSortedTransform>(
                header,
                pipes.size(),
                sort_description,
                merge_block_size,
                0, /// limit
                rows_sources_write_buf.get(),
                true, /// queit
                blocks_are_granules_size,
                true, /// have_all_inputs
                row_mapping_cb);
            break;

        case MergeTreeMetaBase::MergingParams::Collapsing:
            merged_transform = std::make_unique<CollapsingSortedTransform>(
                header,
                pipes.size(),
                sort_description,
                merging_params.sign_column,
                false,
                merge_block_size,
                rows_sources_write_buf.get(),
                blocks_are_granules_size);
            break;

        case MergeTreeMetaBase::MergingParams::Summing:
            merged_transform = std::make_unique<SummingSortedTransform>(
                header,
                pipes.size(),
                sort_description,
                merging_params.columns_to_sum,
                metadata_snapshot->getPartitionKey().column_names,
                merge_block_size);
            break;

        case MergeTreeMetaBase::MergingParams::Aggregating:
            merged_transform = std::make_unique<AggregatingSortedTransform>(header, pipes.size(), sort_description, merge_block_size);
            break;

        case MergeTreeMetaBase::MergingParams::Replacing:
            merged_transform = std::make_unique<ReplacingSortedTransform>(
                header,
                pipes.size(),
                sort_description,
                merging_params.version_column,
                merge_block_size,
                rows_sources_write_buf.get(),
                blocks_are_granules_size);
            break;

        case MergeTreeMetaBase::MergingParams::Graphite:
            merged_transform = std::make_unique<GraphiteRollupSortedTransform>(
                header, pipes.size(), sort_description, merge_block_size, merging_params.graphite_params, time(nullptr));
            break;

        case MergeTreeMetaBase::MergingParams::VersionedCollapsing:
            merged_transform = std::make_unique<VersionedCollapsingTransform>(
                header,
                pipes.size(),
                sort_description,
                merging_params.sign_column,
                merge_block_size,
                rows_sources_write_buf.get(),
                blocks_are_granules_size);
            break;
    }

    QueryPipeline pipeline;
    pipeline.init(Pipe::unitePipes(std::move(pipes)));
    pipeline.addTransform(std::move(merged_transform));
    pipeline.setMaxThreads(1);
    merged_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(pipeline));

    // TODO:
    // if (deduplicate)
    //     merged_stream = std::make_shared<DistinctSortedBlockInputStream>(merged_stream, sort_description, SizeLimits(), 0 /*limit_hint*/, deduplicate_by_columns);

    // if (need_remove_expired_values)
    // {
    //     LOG_DEBUG(log, "Outdated rows found in source parts, TTLs processing enabled for merge");
    //     merged_stream = std::make_shared<TTLBlockInputStream>(merged_stream, data, metadata_snapshot, new_data_part, time_of_merge, force_ttl);
    // }

    if (metadata_snapshot->hasSecondaryIndices())
    {
        const auto & indices = metadata_snapshot->getSecondaryIndices();
        merged_stream = std::make_shared<ExpressionBlockInputStream>(
            merged_stream, indices.getSingleExpressionForIndices(metadata_snapshot->getColumns(), data.getContext()));
        merged_stream = std::make_shared<MaterializingBlockInputStream>(merged_stream);
    }

    const auto & index_factory = MergeTreeIndexFactory::instance();
    to = std::make_unique<MergedBlockOutputStream>(
        new_data_part,
        metadata_snapshot,
        merging_columns,
        index_factory.getMany(metadata_snapshot->getSecondaryIndices()),
        compression_codec,
        blocks_are_granules_size,
        context->getSettingsRef().optimize_map_column_serialization
    );
}

void MergeTreeDataMerger::copyMergedData()
{
    merged_stream->readPrefix();
    to->writePrefix();

    const size_t initial_reservation = space_reservation ? space_reservation->getSize() : 0;

    Block block;
    while (!check_cancel() && (block = merged_stream->read()))
    {
        rows_written += block.rows();

        to->write(block);

        manipulation_entry->rows_written = merged_stream->getProfileInfo().rows;
        manipulation_entry->bytes_written_uncompressed = merged_stream->getProfileInfo().bytes;

        /// Reservation updates is not performed yet, during the merge it may lead to higher free space requirements
        if (space_reservation && sum_input_rows_upper_bound)
        {
            /// The same progress from manipulation_entry could be used for both algorithms (it should be more accurate)
            /// But now we are using inaccurate row-based estimation in Horizontal case for backward compatibility
            Float64 progress = (merge_alg == MergeAlgorithm::Horizontal)
                ? std::min(1., 1. * rows_written / sum_input_rows_upper_bound)
                : std::min(1., manipulation_entry->progress.load(std::memory_order_relaxed));

            space_reservation->update(static_cast<size_t>((1. - progress) * initial_reservation));
        }
    }

    merged_stream->readSuffix();
    merged_stream.reset();

    if (check_cancel())
        throw Exception("Cancelled merging parts", ErrorCodes::ABORTED);

    if (build_rowid_mappings && output_rowid != rows_written)
        throw Exception(
            "Written " + toString(rows_written) + " rows, but output rowid is " + toString(output_rowid), ErrorCodes::LOGICAL_ERROR);
}

void MergeTreeDataMerger::gatherColumn(const String & column_name)
{
    /// Prepare progress
    Float64 progress_before = manipulation_entry->progress.load(std::memory_order_relaxed);
    Float64 column_weight = column_sizes->columnWeight(column_name);
    MergeStageProgress column_progress(progress_before, column_weight);
    LOG_TRACE(log, "Gather column {} weight {} in progress {}", column_name, column_weight, progress_before);

    /// Prepare input streams
    const auto & parts = params.source_data_parts;
    BlockInputStreams column_part_streams(parts.size());

    for (size_t part_num = 0; part_num < parts.size(); ++part_num)
    {
        /// CnchMergePrefetcher::PartFutureFiles * future_files = prefetcher ? prefetcher->tryGetFutureFiles(parts[part_num]->name) : nullptr;

        auto column_part_source = std::make_shared<MergeTreeSequentialSource>(
            data,
            metadata_snapshot,
            parts[part_num],
            nullptr, // delete bitmap
            Names{column_name},
            false, /// read_with_direct_io: We believe source parts will be mostly in page cache
            true // take_column_types_from_storage
        );
        column_part_source->setProgressCallback(ManipulationProgressCallback(manipulation_entry, watch_prev_elapsed, column_progress));

        QueryPipeline column_part_pipeline;
        column_part_pipeline.init(Pipe(std::move(column_part_source)));
        column_part_pipeline.setMaxThreads(1);

        column_part_streams[part_num] = std::make_shared<PipelineExecutingBlockInputStream>(std::move(column_part_pipeline));
    }

    rows_sources_read_buf->seek(0, 0);

    ColumnGathererStream column_gathered_stream(
        column_name,
        column_part_streams,
        *rows_sources_read_buf,
        context->getSettingsRef().enable_low_cardinality_merge_new_algo,
        context->getSettingsRef().low_cardinality_distinct_threshold,
        DEFAULT_BLOCK_SIZE /// block_preferred_size_
    );

    /// Prepare output stream
    MergedColumnOnlyOutputStream column_to(
        new_data_part,
        metadata_snapshot,
        *writer_settings,
        column_gathered_stream.getHeader(),
        compression_codec,
        std::vector<MergeTreeIndexPtr>{},
        written_offset_columns.get(),
        to->getIndexGranularity(),
        true /// is_merge
    );
    column_to.writePrefix();

    /// Do gathering
    size_t column_elems_written = 0;

    Block block;
    while (!check_cancel() && (block = column_gathered_stream.read()))
    {
        /// TODO: support lc

        column_elems_written += block.rows();
        column_to.write(block);
    }

    if (check_cancel())
        throw Exception("Cancelled merging parts", ErrorCodes::ABORTED);

    if (rows_written != column_elems_written)
    {
        throw Exception(
            "Written " + toString(column_elems_written) + " elements of column " + column_name + ", but " + toString(rows_written)
                + " rows of PK columns",
            ErrorCodes::LOGICAL_ERROR);
    }

    column_gathered_stream.readSuffix();
    auto changed_checksums = column_to.writeSuffixAndGetChecksums(new_data_part, additional_column_checksums);
    additional_column_checksums.add(std::move(changed_checksums));

    /// Update profiles and progress
    manipulation_entry->columns_written.fetch_add(1, std::memory_order_relaxed);
    manipulation_entry->bytes_written_uncompressed.fetch_add(column_gathered_stream.getProfileInfo().bytes, std::memory_order_relaxed);
    manipulation_entry->progress.store(progress_before + column_weight, std::memory_order_relaxed);
}

void MergeTreeDataMerger::gatherColumns()
{
    const auto & parts = params.source_data_parts;

    /// Set horizontal stage progress
    manipulation_entry->columns_written.store(merging_column_names.size(), std::memory_order_relaxed);
    manipulation_entry->progress.store(column_sizes->keyColumnsWeight(), std::memory_order_relaxed);

    /// Prepare to gather
    rows_sources_write_buf->next();
    rows_sources_uncompressed_write_buf->next();
    /// Ensure data has written to disk.
    rows_sources_uncompressed_write_buf->finalize();

    size_t sum_input_rows_exact = manipulation_entry->rows_read;
    size_t rows_sources_count = rows_sources_write_buf->count();
    /// In special case, when there is only one source part, and no rows were skipped, we may have
    /// skipped writing rows_sources file. Otherwise rows_sources_count must be equal to the total
    /// number of input rows.
    if ((rows_sources_count > 0 || parts.size() > 1) && sum_input_rows_exact != rows_sources_count)
        throw Exception(
            "Number of rows in source parts (" + toString(sum_input_rows_exact)
                + ") differs from number of bytes written to rows_sources file (" + toString(rows_sources_count) + "). It is a bug.",
            ErrorCodes::LOGICAL_ERROR);

    rows_sources_read_buf = std::make_unique<CompressedReadBufferFromFile>(tmp_disk->readFile(fileName(rows_sources_file->path())));
    written_offset_columns = std::make_unique<IMergedBlockOutputStream::WrittenOffsetColumns>();

    writer_settings = std::make_unique<MergeTreeWriterSettings>(
        new_data_part->storage.getContext()->getSettings(),
        new_data_part->storage.getSettings(),
        /*can_use_adaptive_granularity = */ new_data_part->index_granularity_info.is_adaptive,
        /* rewrite_primary_key = */ false,
        /*blocks_are_granules_size = */ false,
        context->getSettingsRef().optimize_map_column_serialization);

    for (auto & [column_name, column_type] : gathering_columns)
    {
        if (column_type->isMap() && !column_type->isMapKVStore())
            continue;
        gatherColumn(column_name);
        normal_columns_gathered += 1;
    }
}

void MergeTreeDataMerger::finalizePart()
{
    for (const auto & part : params.source_data_parts)
        new_data_part->minmax_idx.merge(part->minmax_idx);

    if (merge_alg != MergeAlgorithm::Vertical)
        to->writeSuffixAndFinalizePart(new_data_part, false, nullptr, &additional_column_checksums);
    else
        to->writeSuffixAndFinalizePart(new_data_part, false, &storage_columns, &additional_column_checksums);
}

MergeTreeMutableDataPartPtr MergeTreeDataMerger::mergePartsToTemporaryPart()
{
    const auto & parts = params.source_data_parts;
    space_reservation = data.reserveSpace(estimateNeededDiskSpace(parts), IStorage::StorageLocation::AUXILITY);

    /// TODO: do we need to support (1) TTL merge ? (2) deduplicate

    prepareColumnNamesAndTypes();

    prepareNewParts();

    LOG_DEBUG(log, "Merging {} parts: {} into {}", parts.size(), toDebugString(parts), new_data_part->relative_path);

    chooseMergeAlgorithm();

    chooseCompressionCodec();

    prepareForProgress();

    if (merge_alg == MergeAlgorithm::Vertical)
        prepareVerticalMerge();
    else
        resetMergingColumns();

    createSources();
    createMergedStream();
    copyMergedData();

    if (merge_alg == MergeAlgorithm::Vertical)
        gatherColumns();

    /// Print overall profiling info. NOTE: it may duplicates previous messages
    {
        double elapsed_seconds = manipulation_entry->watch.elapsedSeconds();
        LOG_DEBUG(
            log,
            "Merge sorted {} rows, containing {} columns ({} merged, {} gathered) in {} sec., {} rows/sec., {}/sec.",
            manipulation_entry->rows_read,
            all_column_names.size(),
            merging_column_names.size(),
            gathering_columns.size(),
            elapsed_seconds,
            manipulation_entry->rows_read / elapsed_seconds,
            ReadableSize(manipulation_entry->bytes_read_uncompressed / elapsed_seconds));
    }

    /// TODO: support project
    finalizePart();

    return new_data_part;
}

}
