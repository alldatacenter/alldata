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

#include <WorkerTasks/MergeTreeDataMutator.h>

#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <Storages/MergeTree/MergedBlockOutputStream.h>
#include <Storages/MergeTree/MergedColumnOnlyOutputStream.h>
#include <Storages/MergeTree/MergeTreeDataWriter.h>
#include <Storages/MergeTree/StorageFromMergeTreeDataPart.h>
#include <DataStreams/ExpressionBlockInputStream.h>
#include <DataStreams/MaterializingBlockInputStream.h>
#include <DataStreams/SquashingBlockInputStream.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/MapHelpers.h>
#include <Processors/Sources/SourceFromSingleChunk.h>
#include <Processors/Executors/PipelineExecutingBlockInputStream.h>
#include <Interpreters/MutationsInterpreter.h>
#include <Interpreters/Context.h>
#include <Common/interpolate.h>
#include <Common/typeid_cast.h>
#include <Common/escapeForFileName.h>
#include <Common/Endian.h>
#include <Parsers/queryToString.h>
#include <Columns/ColumnByteMap.h>
#include <Common/FieldVisitorToString.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeDataPartType.h>
#include <WorkerTasks/ManipulationTaskParams.h>

#include <cmath>
#include <ctime>
#include <numeric>
#include <boost/algorithm/string/replace.hpp>


namespace ProfileEvents
{
    extern const Event MergedRows;
    extern const Event MergedUncompressedBytes;
    extern const Event MergesTimeMilliseconds;
    extern const Event Merge;
}

namespace CurrentMetrics
{
    extern const Metric BackgroundPoolTask;
    extern const Metric Manipulation;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int ABORTED;
    extern const int CANNOT_OPEN_FILE;
}

/// To do mutate, reserve amount of space equals to sum size of parts times specified coefficient.
static const double DISK_USAGE_COEFFICIENT_TO_RESERVE = 1.1;

MergeTreeDataMutator::MergeTreeDataMutator(MergeTreeMetaBase & data_, size_t background_pool_size_)
    : data(data_)
    , background_pool_size(background_pool_size_)
    , log(&Poco::Logger::get(data.getLogName() + " (CnchMutator)"))
{
}

UInt64 MergeTreeDataMutator::getMaxSourcePartSizeForMutation() const
{
    const auto data_settings = data.getSettings();
    size_t busy_threads_in_pool = CurrentMetrics::values[CurrentMetrics::BackgroundPoolTask].load(std::memory_order_relaxed);

    /// DataPart can be store only at one disk. Get maximum reservable free space at all disks.
    UInt64 disk_space = data.getStoragePolicy(IStorage::StorageLocation::MAIN)->getMaxUnreservedFreeSpace();

    /// Allow mutations only if there are enough threads, leave free threads for merges else
    if (busy_threads_in_pool <= 1
        || background_pool_size - busy_threads_in_pool >= data_settings->number_of_free_entries_in_pool_to_execute_mutation)
        return static_cast<UInt64>(disk_space / DISK_USAGE_COEFFICIENT_TO_RESERVE);

    return 0;
}

/** Progress callback.
  * What it should update:
  * - approximate progress
  * - amount of read rows
  * - various metrics
  * - time elapsed for current mutate.
  */

/// Auxiliary struct that for each mutate stage stores its current progress.
/// A stage is: the horizontal stage + a stage for each gathered column (if we are doing a
/// Vertical merge) or a mutation of a single part. During a single stage all rows are read.
struct MutateStageProgress
{
    explicit MutateStageProgress(Float64 weight_)
        : is_first(true) , weight(weight_)
    {
    }

    MutateStageProgress(Float64 initial_progress_, Float64 weight_)
        : initial_progress(initial_progress_), is_first(false), weight(weight_)
    {
    }

    Float64 initial_progress = 0.0;
    bool is_first;
    Float64 weight;

    UInt64 total_rows = 0;
    UInt64 rows_read = 0;
};

/// TODO: rename to manipulation
class MutateProgressCallback
{
public:
    MutateProgressCallback(
        ManipulationList::Entry & manipulation_entry_, UInt64 & watch_prev_elapsed_, MutateStageProgress & stage_)
        : manipulation_entry(manipulation_entry_)
        , watch_prev_elapsed(watch_prev_elapsed_)
        , stage(stage_)
    {
        updateWatch();
    }

    ManipulationListEntry & manipulation_entry;
    UInt64 & watch_prev_elapsed;
    MutateStageProgress & stage;

    void updateWatch()
    {
        UInt64 watch_curr_elapsed = manipulation_entry->watch.elapsed();
        ProfileEvents::increment(ProfileEvents::MergesTimeMilliseconds, (watch_curr_elapsed - watch_prev_elapsed) / 1000000);
        watch_prev_elapsed = watch_curr_elapsed;
    }

    void operator() (const Progress & value)
    {
        ProfileEvents::increment(ProfileEvents::MergedUncompressedBytes, value.read_bytes);
        if (stage.is_first)
        {
            ProfileEvents::increment(ProfileEvents::MergedRows, value.read_rows);
            ProfileEvents::increment(ProfileEvents::Merge);
        }
        updateWatch();

        manipulation_entry->bytes_read_uncompressed += value.read_bytes;
        if (stage.is_first)
            manipulation_entry->rows_read += value.read_rows;

        stage.total_rows += value.total_rows_to_read;
        stage.rows_read += value.read_rows;
        if (stage.total_rows > 0)
        {
            manipulation_entry->progress.store(
                stage.initial_progress + stage.weight * stage.rows_read / stage.total_rows,
                std::memory_order_relaxed);
        }
    }
};

static bool needSyncPart(size_t input_rows, size_t input_bytes, const MergeTreeSettings & settings)
{
    return ((settings.min_rows_to_fsync_after_merge && input_rows >= settings.min_rows_to_fsync_after_merge)
        || (settings.min_compressed_bytes_to_fsync_after_merge && input_bytes >= settings.min_compressed_bytes_to_fsync_after_merge));
}

IMutableMergeTreeDataPartsVector MergeTreeDataMutator::mutatePartsToTemporaryParts(
    const ManipulationTaskParams & params,
    ManipulationListEntry & manipulation_entry,
    ContextPtr context,
    TableLockHolder & holder)
{
    auto metadata_snapshot = params.storage->getInMemoryMetadataPtr();
    auto * table = dynamic_cast<MergeTreeMetaBase *>(params.storage.get());

    if (!table)
        throw Exception("Can't mutate part for storage: " + params.storage->getName(), ErrorCodes::LOGICAL_ERROR);

    IMutableMergeTreeDataPartsVector new_partial_parts;
    new_partial_parts.reserve(params.source_data_parts.size());
    for (const auto & part : params.source_data_parts)
    {
        auto estimated_space_for_result = static_cast<size_t>(part->getBytesOnDisk() * DISK_USAGE_COEFFICIENT_TO_RESERVE);
        ReservationPtr reserved_space = table->reserveSpace(estimated_space_for_result, IStorage::StorageLocation::AUXILITY);  // TODO: calc estimated_space_for_result
        new_partial_parts.push_back(
            mutatePartToTemporaryPart(part, metadata_snapshot, *params.mutation_commands, manipulation_entry, params.txn_id, context, reserved_space, holder));
        new_partial_parts.back()->columns_commit_time = params.columns_commit_time;
        new_partial_parts.back()->mutation_commit_time = params.mutation_commit_time;
    }

    return new_partial_parts;
}

IMutableMergeTreeDataPartPtr MergeTreeDataMutator::mutatePartToTemporaryPart(
    const IMergeTreeDataPartPtr & source_part,
    const StorageMetadataPtr & metadata_snapshot,
    const MutationCommands & commands,
    ManipulationListEntry & manipulation_entry,
    time_t time_of_mutation,
    ContextPtr context,
    const ReservationPtr & space_reservation,
    TableLockHolder & holder)
{
    checkOperationIsNotCanceled(manipulation_entry);

    CurrentMetrics::Increment num_mutations{CurrentMetrics::Manipulation};
    // const auto & source_part = future_part.parts[0];
    auto storage_from_source_part = StorageFromMergeTreeDataPart::create(source_part);

    /// prevent other data modification tasks (recode/build bitmap index) from execution during part mutation
    auto mutate_lock = std::unique_lock<std::mutex>(source_part->mutate_mutex);

    auto context_for_reading = Context::createCopy(context);
    context_for_reading->setSetting("max_streams_to_max_threads_ratio", 1);
    context_for_reading->setSetting("max_threads", 1);
    /// Allow mutations to work when force_index_by_date or force_primary_key is on.
    context_for_reading->setSetting("force_index_by_date", Field(0));
    context_for_reading->setSetting("force_primary_key", Field(0));

    MutationCommands commands_for_part;
    for (const auto & command : commands)
    {
        if (!command.partition || source_part->info.partition_id == data.getPartitionIDFromQuery(command.partition, context_for_reading))
        {
            /// FAST_DELETE can't handle non-wide part and projections, convert to DELETE in those cases
            if (command.type == MutationCommand::FAST_DELETE && (!isWidePart(source_part) || !metadata_snapshot->getProjections().empty()))
                commands_for_part.emplace_back(MutationCommand{.type = MutationCommand::DELETE, .predicate = command.predicate, .partition = command.partition});
            else
                commands_for_part.emplace_back(command);
        }
    }

    if (source_part->isStoredOnDisk() && !isStorageTouchedByMutations(
        storage_from_source_part, metadata_snapshot, commands_for_part, Context::createCopy(context_for_reading)))
    {
        LOG_TRACE(log, "Part {} doesn't change up to mutation version {}", source_part->name, source_part->info.mutation);
        return data.cloneAndLoadDataPartOnSameDisk(source_part, "tmp_clone_", source_part->info, metadata_snapshot);
    }
    else
    {
        LOG_TRACE(log, "Mutating part {} to mutation version {}", source_part->name, source_part->info.mutation);
    }

    BlockInputStreamPtr in;
    Block updated_header;
    ImmutableDeleteBitmapPtr updated_delete_bitmap;
    std::unique_ptr<MutationsInterpreter> interpreter;

    const auto data_settings = data.getSettings();
    MutationCommands for_interpreter;
    MutationCommands for_file_renames;

    splitMutationCommands(source_part, commands_for_part, for_interpreter, for_file_renames);

    /// for modify column mutations, the new column data files should contain the same number of rows as before,
    /// thus an empty delete bitmap is used for read.
    if (for_interpreter.allOf(MutationCommand::READ_COLUMN))
    {
        storage_from_source_part->setDeleteBitmap(source_part, nullptr);
    }

    UInt64 watch_prev_elapsed = 0;
    MutateStageProgress stage_progress(1.0);

    NamesAndTypesList storage_columns = metadata_snapshot->getColumns().getAllPhysical();
    NameSet materialized_indices;
    NameSet materialized_projections;
    MutationsInterpreter::MutationKind::MutationKindEnum mutation_kind{};

    if (!for_interpreter.empty())
    {
        interpreter = std::make_unique<MutationsInterpreter>(
            storage_from_source_part, metadata_snapshot, for_interpreter, context_for_reading, true);
        materialized_indices = interpreter->grabMaterializedIndices();
        materialized_projections = interpreter->grabMaterializedProjections();
        mutation_kind = interpreter->getMutationKind();
        in = interpreter->execute();
        if (in)
            in->setProgressCallback(MutateProgressCallback(manipulation_entry, watch_prev_elapsed, stage_progress));
        updated_header = interpreter->getUpdatedHeader();
        updated_delete_bitmap = interpreter->getUpdatedDeleteBitmap();
    }

    auto new_part_info = source_part->info;
    new_part_info.level += 1;
    new_part_info.hint_mutation = new_part_info.mutation;
    new_part_info.mutation = context->getTimestamp();
    auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + source_part->name, space_reservation->getDisk(), 0);
    auto new_part_name = new_part_info.getPartName();
    auto new_data_part = data.createPart(
        new_part_name, MergeTreeDataPartType::WIDE, new_part_info, single_disk_volume, "tmp_mut_" + new_part_name,
        nullptr, IStorage::StorageLocation::AUXILITY);

    new_data_part->uuid = source_part->uuid;
    new_data_part->is_temp = true;
    new_data_part->ttl_infos = source_part->ttl_infos;
    new_data_part->versions = source_part->versions;

    /// It shouldn't be changed by mutation.
    new_data_part->index_granularity_info = source_part->index_granularity_info;
    new_data_part->setColumns(getColumnsForNewDataPart(source_part, updated_header, storage_columns, for_file_renames));
    new_data_part->partition.assign(source_part->partition);

    auto disk = new_data_part->volume->getDisk();
    String new_part_tmp_path = new_data_part->getFullRelativePath();

    SyncGuardPtr sync_guard;
    if (data.getSettings()->fsync_part_directory)
        sync_guard = disk->getDirectorySyncGuard(new_part_tmp_path);

    /// Don't change granularity type while mutating subset of columns
    auto mrk_extension = source_part->index_granularity_info.is_adaptive ? getAdaptiveMrkExtension(new_data_part->getType()) : getNonAdaptiveMrkExtension();
    bool need_sync = needSyncPart(source_part->rows_count, source_part->getBytesOnDisk(), *data_settings);

    /// TODO: check that we modify only non-key columns in this case.
    {
        /// We will modify only some of the columns. Other columns and key values can be copied as-is.
        NameSet updated_columns;
        for (const auto & name_type : updated_header.getNamesAndTypesList())
            updated_columns.emplace(name_type.name);

        auto indices_to_recalc = getIndicesToRecalculate(
            in, updated_columns, metadata_snapshot, context, materialized_indices, source_part);
        auto projections_to_recalc = getProjectionsToRecalculate(
            updated_columns, metadata_snapshot, materialized_projections, source_part);

        NameToNameVector files_to_rename = collectFilesForRenames(source_part, for_file_renames, mrk_extension);

        if (indices_to_recalc.empty() && projections_to_recalc.empty() && mutation_kind != MutationsInterpreter::MutationKind::MUTATE_OTHER
            && files_to_rename.empty())
        {
            LOG_TRACE(
                log, "Part {} doesn't change up to mutation version {} (optimized)", source_part->name, source_part->info.mutation);
            return new_data_part; /// empty partial part
        }

        String part_dir = new_data_part->getFullRelativePath();
        if (!disk->exists(part_dir))
        {
            disk->createDirectories(part_dir);
        }

        manipulation_entry->columns_written = storage_columns.size() - updated_header.columns();

        auto old_checksums = source_part->getChecksums();
        /// empty checksums, will update in mutateSomePartColumns
        new_data_part->checksums_ptr = std::make_shared<MergeTreeData::DataPart::Checksums>();

        auto compression_codec = source_part->default_codec;

        if (!compression_codec)
            throw Exception(ErrorCodes::BAD_ARGUMENTS, "Unknown codec for mutate part: {}", source_part->name);

        if (in)
        {
            mutateSomePartColumns(
                source_part,
                metadata_snapshot,
                indices_to_recalc,
                projections_to_recalc,
                updated_header,
                new_data_part,
                in,
                time_of_mutation,
                compression_codec,
                manipulation_entry,
                need_sync,
                space_reservation,
                holder,
                context);
        }

        for (const auto & [rename_from, rename_to] : files_to_rename)
        {
            if (rename_to.empty() && new_data_part->checksums_ptr->files.count(rename_from))
            {
                new_data_part->checksums_ptr->files.erase(rename_from);
            }
            else if (old_checksums->files.count(rename_from))
            {
                if (!rename_to.empty())
                {
                    new_data_part->checksums_ptr->files[rename_to] = old_checksums->files[rename_from];
                }
                new_data_part->checksums_ptr->files.erase(rename_from);
            }
        }

        if (updated_delete_bitmap)
        {
            // new_data_part->writeDeleteFile(updated_delete_bitmap, need_sync);
        }

        finalizeMutatedPart(source_part, new_data_part, compression_codec);
    }

    return new_data_part;
}

void MergeTreeDataMutator::splitMutationCommands(
    MergeTreeData::DataPartPtr part,
    const MutationCommands & commands,
    MutationCommands & for_interpreter,
    MutationCommands & for_file_renames)
{
    ColumnsDescription part_columns(part->getColumns());

    for (const auto & command : commands)
    {
        if (command.type == MutationCommand::Type::MATERIALIZE_INDEX
            || command.type == MutationCommand::Type::MATERIALIZE_PROJECTION
            || command.type == MutationCommand::Type::MATERIALIZE_TTL
            || command.type == MutationCommand::Type::DELETE
            || command.type == MutationCommand::Type::FAST_DELETE
            || command.type == MutationCommand::Type::UPDATE)
        {
            for_interpreter.push_back(command);
        }
        else if (command.type == MutationCommand::Type::DROP_INDEX || command.type == MutationCommand::Type::DROP_PROJECTION)
        {
            for_file_renames.push_back(command);
        }
        else if (part_columns.has(command.column_name))
        {
            if (command.type == MutationCommand::Type::READ_COLUMN)
            {
                for_interpreter.push_back(command);
            }
            else if (command.type == MutationCommand::Type::RENAME_COLUMN)
            {
                for_interpreter.push_back(
                {
                    .type = MutationCommand::Type::READ_COLUMN,
                    .column_name = command.rename_to,
                });
                part_columns.rename(command.column_name, command.rename_to);
            }
            else
            {
                for_file_renames.push_back(command);
            }
        }
    }
}

NameSet MergeTreeDataMutator::collectFilesForClearMapKey(MergeTreeData::DataPartPtr source_part, const MutationCommands & commands)
{
    NameSet clear_map_key_set;
    for (const auto & command : commands)
    {
        if (command.type != MutationCommand::Type::CLEAR_MAP_KEY)
            continue;

        auto files = source_part->getChecksums()->files;

        /// Collect all compacted file names of the implicit key name.
        /// If it's the compact map column and all implicit keys of it have removed, remove these compacted files.
        NameSet file_set;

        /// Remove all files of the implicit key name
        for (const auto & map_key_ast : command.map_keys->children)
        {
            const auto & key = map_key_ast->as<ASTLiteral &>().value.get<std::string>();
            /// TODO: support Int
            String implicit_key_name = getImplicitColNameForMapKey(command.column_name, escapeForFileName("'" + key + "'"));
            for (const auto & [file, _] : source_part->getChecksums()->files)
            {
                if (startsWith(file, implicit_key_name))
                {
                    if (source_part->versions->enable_compact_map_data)
                    {
                        file_set.emplace(getMapFileNameFromImplicitFileName(file));
                    }
                    /// TODO: mark delete.
                    files.erase(file);
                    clear_map_key_set.emplace(file);
                }
            }
        }

        if (source_part->versions->enable_compact_map_data)
        {
            /// Check if all implicit keys of the map column have removed
            bool has_other_implicit_column = false;
            auto map_key_prefix = genMapKeyFilePrefix(command.column_name);

            for (const auto & [file, _]: files)
            {
                if (startsWith(file, map_key_prefix))
                {
                    has_other_implicit_column = true;
                    break;
                }
            }

            if (!has_other_implicit_column)
            {
                for (const String & file: file_set)
                {
                    clear_map_key_set.emplace(file);
                }
            }
        }
    }
    return clear_map_key_set;
}


NameToNameVector MergeTreeDataMutator::collectFilesForRenames(
    MergeTreeData::DataPartPtr source_part,
    const MutationCommands & commands_for_removes,
    const String & mrk_extension)
{
    /// Collect counts for shared streams of different columns. As an example, Nested columns have shared stream with array sizes.
    std::map<String, size_t> stream_counts;
    for (const NameAndTypePair & column : source_part->getColumns())
    {
        auto serialization = source_part->getSerializationForColumn(column);
        serialization->enumerateStreams(
            [&](const ISerialization::SubstreamPath & substream_path)
            {
                ++stream_counts[ISerialization::getFileNameForStream(column, substream_path)];
            },
            {});
    }

    NameToNameVector rename_vector;
    /// Remove old data
    auto source_checksums = source_part->getChecksums();
    for (const auto & command : commands_for_removes)
    {
        if (command.type == MutationCommand::Type::DROP_INDEX)
        {
            if (source_checksums->has(INDEX_FILE_PREFIX + command.column_name + ".idx"))
            {
                rename_vector.emplace_back(INDEX_FILE_PREFIX + command.column_name + ".idx", "");
                rename_vector.emplace_back(INDEX_FILE_PREFIX + command.column_name + mrk_extension, "");
            }
        }
        else if (command.type == MutationCommand::Type::DROP_PROJECTION)
        {
            if (source_checksums->has(command.column_name + ".proj"))
                rename_vector.emplace_back(command.column_name + ".proj", "");
        }
        else if (command.type == MutationCommand::Type::DROP_COLUMN)
        {
            ISerialization::StreamCallback callback = [&](const ISerialization::SubstreamPath & substream_path)
            {
                String stream_name = ISerialization::getFileNameForStream({command.column_name, command.data_type}, substream_path);
                /// Delete files if they are no longer shared with another column.
                if (--stream_counts[stream_name] == 0)
                {
                    rename_vector.emplace_back(stream_name + ".bin", "");
                    rename_vector.emplace_back(stream_name + mrk_extension, "");
                }
            };

            auto column = source_part->getColumns().tryGetByName(command.column_name);
            if (column)
            {
                if (column->type->isMap() && !column->type->isMapKVStore())
                {
                    Strings files = source_part->getChecksums()->collectFilesForMapColumnNotKV(command.column_name);
                    for (auto & file : files)
                        rename_vector.emplace_back(file, "");
                }
                else
                {
                    auto serialization = source_part->getSerializationForColumn(*column);
                    serialization->enumerateStreams(callback);
                }
            }
        }
        else if (command.type == MutationCommand::Type::RENAME_COLUMN)
        {
            String escaped_name_from = escapeForFileName(command.column_name);
            String escaped_name_to = escapeForFileName(command.rename_to);

            ISerialization::StreamCallback callback = [&](const ISerialization::SubstreamPath & substream_path)
            {
                String stream_from = ISerialization::getFileNameForStream({command.column_name, command.data_type}, substream_path);

                String stream_to = boost::replace_first_copy(stream_from, escaped_name_from, escaped_name_to);

                if (stream_from != stream_to)
                {
                    rename_vector.emplace_back(stream_from + ".bin", stream_to + ".bin");
                    rename_vector.emplace_back(stream_from + mrk_extension, stream_to + mrk_extension);
                }
            };

            auto column = source_part->getColumns().tryGetByName(command.column_name);
            if (column)
            {
                auto serialization = source_part->getSerializationForColumn(*column);
                serialization->enumerateStreams(callback);
            }
        }
    }

    auto clear_map_key_set = collectFilesForClearMapKey(source_part, commands_for_removes);
    for (const auto & name : clear_map_key_set)
        rename_vector.emplace_back(name, "");

    return rename_vector;
}

NamesAndTypesList MergeTreeDataMutator::getColumnsForNewDataPart(
    MergeTreeData::DataPartPtr source_part,
    const Block & updated_header,
    NamesAndTypesList storage_columns,
    const MutationCommands & commands_for_removes)
{
    /// In compact parts we read all columns, because they all stored in a single file
    if (!isWidePart(source_part) || !isCnchPart(source_part))
        return updated_header.getNamesAndTypesList();

    NameSet removed_columns;
    NameToNameMap renamed_columns_to_from;
    /// All commands are validated in AlterCommand so we don't care about order
    for (const auto & command : commands_for_removes)
    {
        if (command.type == MutationCommand::DROP_COLUMN)
            removed_columns.insert(command.column_name);
        if (command.type == MutationCommand::RENAME_COLUMN)
            renamed_columns_to_from.emplace(command.rename_to, command.column_name);
    }

    Names source_column_names = source_part->getColumns().getNames();
    NameSet source_columns_name_set(source_column_names.begin(), source_column_names.end());

    for (auto it = storage_columns.begin(); it != storage_columns.end();)
    {
        if (updated_header.has(it->name))
        {
            auto updated_type = updated_header.getByName(it->name).type;
            if (updated_type != it->type)
                it->type = updated_type;
            ++it;
        }
        else
        {
            if (!source_columns_name_set.count(it->name))
            {
                /// Source part doesn't have column but some other column was renamed to it's name.
                auto renamed_it = renamed_columns_to_from.find(it->name);
                if (renamed_it != renamed_columns_to_from.end() && source_columns_name_set.count(renamed_it->second))
                    ++it;
                else
                    it = storage_columns.erase(it);
            }
            else
            {
                bool was_renamed = false;
                bool was_removed = removed_columns.count(it->name);

                /// Check that this column was renamed to some other name
                for (const auto & [rename_to, rename_from] : renamed_columns_to_from)
                {
                    if (rename_from == it->name)
                    {
                        was_renamed = true;
                        break;
                    }
                }

                /// If we want to rename this column to some other name, than it
                /// should it's previous version should be dropped or removed
                if (renamed_columns_to_from.count(it->name) && !was_renamed && !was_removed)
                    throw Exception(
                        ErrorCodes::LOGICAL_ERROR,
                        "Incorrect mutation commands, trying to rename column {} to {}, but part {} already has column {}",
                        renamed_columns_to_from[it->name], it->name, source_part->name, it->name);

                /// remove redundant columns for partial part
                it = storage_columns.erase(it);
            }
        }
    }

    return storage_columns;
}

MergeTreeIndices MergeTreeDataMutator::getIndicesForNewDataPart(
    const IndicesDescription & all_indices,
    const MutationCommands & commands_for_removes)
{
    NameSet removed_indices;
    for (const auto & command : commands_for_removes)
        if (command.type == MutationCommand::DROP_INDEX)
            removed_indices.insert(command.column_name);

    MergeTreeIndices new_indices;
    for (const auto & index : all_indices)
        if (!removed_indices.count(index.name))
            new_indices.push_back(MergeTreeIndexFactory::instance().get(index));

    return new_indices;
}

MergeTreeProjections MergeTreeDataMutator::getProjectionsForNewDataPart(
    const ProjectionsDescription & all_projections,
    const MutationCommands & commands_for_removes)
{
    NameSet removed_projections;
    for (const auto & command : commands_for_removes)
        if (command.type == MutationCommand::DROP_PROJECTION)
            removed_projections.insert(command.column_name);

    MergeTreeProjections new_projections;
    for (const auto & projection : all_projections)
        if (!removed_projections.count(projection.name))
            new_projections.push_back(MergeTreeProjectionFactory::instance().get(projection));

    return new_projections;
}

std::set<MergeTreeIndexPtr> MergeTreeDataMutator::getIndicesToRecalculate(
    BlockInputStreamPtr & input_stream,
    const NameSet & updated_columns,
    const StorageMetadataPtr & metadata_snapshot,
    ContextPtr context,
    const NameSet & materialized_indices,
    const MergeTreeData::DataPartPtr & source_part)
{
    /// Checks if columns used in skipping indexes modified.
    const auto & index_factory = MergeTreeIndexFactory::instance();
    std::set<MergeTreeIndexPtr> indices_to_recalc;
    ASTPtr indices_recalc_expr_list = std::make_shared<ASTExpressionList>();
    const auto & indices = metadata_snapshot->getSecondaryIndices();

    for (size_t i = 0; i < indices.size(); ++i)
    {
        const auto & index = indices[i];

        // If we ask to materialize and it already exists
        if (!source_part->getChecksums()->has(INDEX_FILE_PREFIX + index.name + ".idx") && materialized_indices.count(index.name))
        {
            if (indices_to_recalc.insert(index_factory.get(index)).second)
            {
                ASTPtr expr_list = index.expression_list_ast->clone();
                for (const auto & expr : expr_list->children)
                    indices_recalc_expr_list->children.push_back(expr->clone());
            }
        }
        // If some dependent columns gets mutated
        else
        {
            bool mutate = false;
            const auto & index_cols = index.expression->getRequiredColumns();
            for (const auto & col : index_cols)
            {
                if (updated_columns.count(col))
                {
                    mutate = true;
                    break;
                }
            }
            if (mutate && indices_to_recalc.insert(index_factory.get(index)).second)
            {
                ASTPtr expr_list = index.expression_list_ast->clone();
                for (const auto & expr : expr_list->children)
                    indices_recalc_expr_list->children.push_back(expr->clone());
            }
        }
    }

    if (!indices_to_recalc.empty() && input_stream)
    {
        auto indices_recalc_syntax = TreeRewriter(context).analyze(indices_recalc_expr_list, input_stream->getHeader().getNamesAndTypesList());
        auto indices_recalc_expr = ExpressionAnalyzer(
                indices_recalc_expr_list,
                indices_recalc_syntax, context).getActions(false);

        /// We can update only one column, but some skip idx expression may depend on several
        /// columns (c1 + c2 * c3). It works because this stream was created with help of
        /// MutationsInterpreter which knows about skip indices and stream 'in' already has
        /// all required columns.
        /// TODO move this logic to single place.
        input_stream = std::make_shared<MaterializingBlockInputStream>(
            std::make_shared<ExpressionBlockInputStream>(input_stream, indices_recalc_expr));
    }
    return indices_to_recalc;
}

std::set<MergeTreeProjectionPtr> MergeTreeDataMutator::getProjectionsToRecalculate(
    const NameSet & updated_columns,
    const StorageMetadataPtr & metadata_snapshot,
    const NameSet & materialized_projections,
    const MergeTreeData::DataPartPtr & source_part)
{
    /// Checks if columns used in projections modified.
    const auto & projection_factory = MergeTreeProjectionFactory::instance();
    std::set<MergeTreeProjectionPtr> projections_to_recalc;
    for (const auto & projection : metadata_snapshot->getProjections())
    {
        // If we ask to materialize and it doesn't exist
        if (!source_part->getChecksums()->has(projection.name + ".proj") && materialized_projections.count(projection.name))
        {
            projections_to_recalc.insert(projection_factory.get(projection));
        }
        else
        {
            // If some dependent columns gets mutated
            bool mutate = false;
            const auto & projection_cols = projection.required_columns;
            for (const auto & col : projection_cols)
            {
                if (updated_columns.count(col))
                {
                    mutate = true;
                    break;
                }
            }
            if (mutate)
                projections_to_recalc.insert(projection_factory.get(projection));
        }
    }
    return projections_to_recalc;
}

// 1. get projection pipeline and a sink to write parts
// 2. build an executor that can write block to the input stream (actually we can write through it to generate as many parts as possible)
// 3. finalize the pipeline so that all parts are merged into one part
void MergeTreeDataMutator::writeWithProjections(
    MergeTreeData::MutableDataPartPtr new_data_part,
    const StorageMetadataPtr & metadata_snapshot,
    const MergeTreeProjections & projections_to_build,
    BlockInputStreamPtr mutating_stream,
    IMergedBlockOutputStream & out,
    time_t /*time_of_mutation*/,
    ManipulationListEntry & manipulation_entry,
    const ReservationPtr & /*space_reservation*/,
    TableLockHolder & /*holder*/,
    ContextPtr context,
    IMergeTreeDataPart::MinMaxIndex * minmax_idx)
{
    size_t block_num = 0;
    Block block;
    std::map<String, MergeTreeData::MutableDataPartsVector> projection_parts;
    std::vector<SquashingTransform> projection_squashes;
    for (size_t i = 0, size = projections_to_build.size(); i < size; ++i)
    {
        projection_squashes.emplace_back(65536, 65536 * 256);
    }

    while (checkOperationIsNotCanceled(manipulation_entry) && (block = mutating_stream->read()))
    {
        if (minmax_idx)
            minmax_idx->update(block, data.getMinMaxColumnsNames(metadata_snapshot->getPartitionKey()));

        out.write(block);

        for (size_t i = 0, size = projections_to_build.size(); i < size; ++i)
        {
            const auto & projection = projections_to_build[i]->projection;
            auto in = InterpreterSelectQuery(
                          projection.query_ast,
                          context,
                          Pipe(std::make_shared<SourceFromSingleChunk>(block, Chunk(block.getColumns(), block.rows()))),
                          SelectQueryOptions{
                              projection.type == ProjectionDescription::Type::Normal ? QueryProcessingStage::FetchColumns : QueryProcessingStage::WithMergeableState})
                          .execute()
                          .getInputStream();
            in = std::make_shared<SquashingBlockInputStream>(in, block.rows(), std::numeric_limits<UInt64>::max());
            in->readPrefix();
            auto & projection_squash = projection_squashes[i];
            auto projection_block = projection_squash.add(in->read());
            if (in->read())
                throw Exception("Projection cannot increase the number of rows in a block", ErrorCodes::LOGICAL_ERROR);
            in->readSuffix();
            if (projection_block)
            {
                projection_parts[projection.name].emplace_back(
                    MergeTreeDataWriter::writeTempProjectionPart(data, log, projection_block, projection, new_data_part.get(), ++block_num, IStorage::StorageLocation::MAIN));
            }
        }

        manipulation_entry->rows_written += block.rows();
        manipulation_entry->bytes_written_uncompressed += block.bytes();
    }

    // Write the last block
    for (size_t i = 0, size = projections_to_build.size(); i < size; ++i)
    {
        const auto & projection = projections_to_build[i]->projection;
        auto & projection_squash = projection_squashes[i];
        auto projection_block = projection_squash.add({});
        if (projection_block)
        {
            projection_parts[projection.name].emplace_back(
                MergeTreeDataWriter::writeTempProjectionPart(data, log, projection_block, projection, new_data_part.get(), ++block_num, IStorage::StorageLocation::MAIN));
        }
    }

    const auto & projections = metadata_snapshot->projections;

    for (auto && [name, parts] : projection_parts)
    {
        LOG_DEBUG(log, "Selected {} projection_parts from {} to {}", parts.size(), parts.front()->name, parts.back()->name);

        const auto & projection = projections.get(name);

        std::map<size_t, MergeTreeData::MutableDataPartsVector> level_parts;
        size_t current_level = 0;
        size_t next_level = 1;
        level_parts[current_level] = std::move(parts);
        size_t max_parts_to_merge_in_one_level = 10;

        while (true)
        {
            auto & current_level_parts = level_parts[current_level];
            auto & next_level_parts = level_parts[next_level];

            MergeTreeData::MutableDataPartsVector selected_parts;
            while (selected_parts.size() < max_parts_to_merge_in_one_level && !current_level_parts.empty())
            {
                selected_parts.push_back(std::move(current_level_parts.back()));
                current_level_parts.pop_back();
            }

            if (selected_parts.empty())
            {
                if (next_level_parts.empty())
                {
                    LOG_WARNING(log, "There is no projection parts merged");
                    break;
                }
                current_level = next_level;
                ++next_level;
            }
            else if (selected_parts.size() == 1)
            {
                if (next_level_parts.empty())
                {
                    LOG_DEBUG(log, "Merged a projection part in level {}", current_level);
                    selected_parts[0]->renameTo(projection.name + ".proj", true);
                    selected_parts[0]->name = projection.name;
                    selected_parts[0]->is_temp = false;
                    new_data_part->addProjectionPart(name, std::move(selected_parts[0]));
                    break;
                }
                else
                {
                    LOG_DEBUG(log, "Forwarded part {} in level {} to next level", selected_parts[0]->name, current_level);
                    next_level_parts.push_back(std::move(selected_parts[0]));
                }
            }
            else if (selected_parts.size() > 1)
            {
                // Generate a unique part name
                // ++block_num;
                // FutureMergedMutatedPart projection_future_part;
                // MergeTreeData::DataPartsVector const_selected_parts(
                //     std::make_move_iterator(selected_parts.begin()), std::make_move_iterator(selected_parts.end()));
                // projection_future_part.assign(std::move(const_selected_parts));
                // projection_future_part.name = fmt::format("{}_{}", projection.name, ++block_num);
                // projection_future_part.part_info = {"all", 0, 0, 0};

                // MergeTreeMetaBase::MergingParams projection_merging_params;
                // projection_merging_params.mode = MergeTreeMetaBase::MergingParams::Ordinary;
                // if (projection.type == ProjectionDescription::Type::Aggregate)
                //     projection_merging_params.mode = MergeTreeMetaBase::MergingParams::Aggregating;

                // LOG_DEBUG(log, "Merged {} parts in level {} to {}", selected_parts.size(), current_level, projection_future_part.name);
                // TODO
                // next_level_parts.push_back(mergePartsToTemporaryPart(
                //     projection_future_part,
                //     projection.metadata,
                //     manipulation_entry,
                //     holder,
                //     time_of_mutation,
                //     context,
                //     space_reservation,
                //     false, // TODO Do we need deduplicate for projections
                //     {},
                //     projection_merging_params,
                //     new_data_part.get(),
                //     "tmp_merge_"));

                // next_level_parts.back()->is_temp = true;
            }
        }
    }
}

void MergeTreeDataMutator::mutateAllPartColumns(
    MergeTreeData::MutableDataPartPtr new_data_part,
    const StorageMetadataPtr & metadata_snapshot,
    const MergeTreeIndices & skip_indices,
    const MergeTreeProjections & projections_to_build,
    const MutationCommands & commands_for_removes,
    BlockInputStreamPtr mutating_stream,
    time_t time_of_mutation,
    const CompressionCodecPtr & compression_codec,
    ManipulationListEntry & manipulation_entry,
    bool need_sync,
    const ReservationPtr & space_reservation,
    TableLockHolder & holder,
    ContextPtr context)
{
    if (mutating_stream == nullptr)
        throw Exception("Cannot mutate part columns with uninitialized mutations stream. It's a bug", ErrorCodes::LOGICAL_ERROR);

    if (metadata_snapshot->hasPrimaryKey() || metadata_snapshot->hasSecondaryIndices())
        mutating_stream = std::make_shared<MaterializingBlockInputStream>(
            std::make_shared<ExpressionBlockInputStream>(mutating_stream, data.getPrimaryKeyAndSkipIndicesExpression(metadata_snapshot)));

    IMergeTreeDataPart::MinMaxIndex minmax_idx;

    MergedBlockOutputStream out{
        new_data_part,
        metadata_snapshot,
        new_data_part->getColumns(),
        skip_indices,
        compression_codec};

    mutating_stream->readPrefix();
    out.writePrefix();

    writeWithProjections(
        new_data_part,
        metadata_snapshot,
        projections_to_build,
        mutating_stream,
        out,
        time_of_mutation,
        manipulation_entry,
        space_reservation,
        holder,
        context,
        &minmax_idx);

    new_data_part->minmax_idx = std::move(minmax_idx);
    mutating_stream->readSuffix();

    /// handle clear map key commands for in-memory part
    auto part_in_memory = asInMemoryPart(new_data_part);
    if (part_in_memory)
    {
        for (const auto & command : commands_for_removes)
        {
            if (command.type != MutationCommand::Type::CLEAR_MAP_KEY)
                continue;

            auto & block = part_in_memory->block;
            auto * map_column = dynamic_cast<ColumnByteMap *>(block.getByName(command.column_name).column->assumeMutable().get());

            if (map_column)
            {
                NameSet clear_map_keys;
                for (const auto & map_key_ast : command.map_keys->children)
                {
                    const auto & key = map_key_ast->as<ASTLiteral &>().value.get<std::string>();
                    clear_map_keys.emplace(key);
                }
                map_column->removeKeys(clear_map_keys);
            }
        }
    }

    out.writeSuffixAndFinalizePart(new_data_part, need_sync);
}

void MergeTreeDataMutator::mutateSomePartColumns(
    const MergeTreeDataPartPtr & source_part,
    const StorageMetadataPtr & metadata_snapshot,
    const std::set<MergeTreeIndexPtr> & indices_to_recalc,
    const std::set<MergeTreeProjectionPtr> & projections_to_recalc,
    const Block & mutation_header,
    MergeTreeData::MutableDataPartPtr new_data_part,
    BlockInputStreamPtr mutating_stream,
    time_t time_of_mutation,
    const CompressionCodecPtr & compression_codec,
    ManipulationListEntry & manipulation_entry,
    bool need_sync,
    const ReservationPtr & space_reservation,
    TableLockHolder & holder,
    ContextPtr context)
{
    if (mutating_stream == nullptr)
        throw Exception("Cannot mutate part columns with uninitialized mutations stream. It's a bug", ErrorCodes::LOGICAL_ERROR);

    IMergedBlockOutputStream::WrittenOffsetColumns unused_written_offsets;
    MergedColumnOnlyOutputStream out(
        new_data_part,
        metadata_snapshot,
        mutation_header,
        compression_codec,
        std::vector<MergeTreeIndexPtr>(indices_to_recalc.begin(), indices_to_recalc.end()),
        nullptr,
        source_part->index_granularity,
        &source_part->index_granularity_info
    );

    mutating_stream->readPrefix();
    out.writePrefix();

    std::vector<MergeTreeProjectionPtr> projections_to_build(projections_to_recalc.begin(), projections_to_recalc.end());
    writeWithProjections(
        new_data_part,
        metadata_snapshot,
        projections_to_build,
        mutating_stream,
        out,
        time_of_mutation,
        manipulation_entry,
        space_reservation,
        holder,
        context);

    mutating_stream->readSuffix();

    auto changed_checksums = out.writeSuffixAndGetChecksums(new_data_part, *(new_data_part->getChecksums()), need_sync);

    new_data_part->checksums_ptr->add(std::move(changed_checksums));
}

void MergeTreeDataMutator::finalizeMutatedPart(
    const MergeTreeDataPartPtr & source_part,
    MergeTreeData::MutableDataPartPtr new_data_part,
    const CompressionCodecPtr & codec)
{
    auto disk = new_data_part->volume->getDisk();
    auto new_part_checksums_ptr = new_data_part->getChecksums();
    if (new_data_part->uuid != UUIDHelpers::Nil)
    {
        auto out = disk->writeFile(new_data_part->getFullRelativePath() + IMergeTreeDataPart::UUID_FILE_NAME, {.buffer_size = 4096});
        HashingWriteBuffer out_hashing(*out);
        writeUUIDText(new_data_part->uuid, out_hashing);
        new_part_checksums_ptr->files[IMergeTreeDataPart::UUID_FILE_NAME].file_size = out_hashing.count();
        new_part_checksums_ptr->files[IMergeTreeDataPart::UUID_FILE_NAME].file_hash = out_hashing.getHash();
    }

    {
        /// Write file with checksums.
        auto out_checksums = disk->writeFile(fs::path(new_data_part->getFullRelativePath()) / "checksums.txt", {.buffer_size = 4096});
        new_part_checksums_ptr->versions = new_data_part->versions;
        new_part_checksums_ptr->write(*out_checksums);
    } /// close fd

    {
        auto out = disk->writeFile(new_data_part->getFullRelativePath() + IMergeTreeDataPart::DEFAULT_COMPRESSION_CODEC_FILE_NAME, {.buffer_size = 4096});
        DB::writeText(queryToString(codec->getFullCodecDesc()), *out);
    }

    {
        /// Write a file with a description of columns.
        auto out_columns = disk->writeFile(fs::path(new_data_part->getFullRelativePath()) / "columns.txt", {.buffer_size = 4096});
        new_data_part->getColumns().writeText(*out_columns);
    } /// close fd

    new_data_part->rows_count = source_part->rows_count;
    new_data_part->index_granularity = source_part->index_granularity;
    new_data_part->index = source_part->getIndex();
    new_data_part->minmax_idx = source_part->minmax_idx;
    new_data_part->modification_time = time(nullptr);
    new_data_part->loadProjections(false, false);
    new_data_part->setBytesOnDisk(
        MergeTreeData::DataPart::calculateTotalSizeOnDisk(new_data_part->volume->getDisk(), new_data_part->getFullRelativePath()));
    new_data_part->default_codec = codec;
    // TODO:
    // new_data_part->mutation_commit_time = manipulation_entry.
    // new_data_part->calculateColumnsSizesOnDisk();
    // new_data_part->storage.lockSharedData(*new_data_part);
}

bool MergeTreeDataMutator::checkOperationIsNotCanceled(const ManipulationListEntry & manipulation_entry) const
{
    if (mutate_blocker.isCancelled() || manipulation_entry->is_cancelled)
        throw Exception("Cancelled mutating parts", ErrorCodes::ABORTED);

    return true;
}

}
