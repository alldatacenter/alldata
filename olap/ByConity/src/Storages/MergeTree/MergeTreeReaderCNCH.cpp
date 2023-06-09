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

#include <Storages/MergeTree/MergeTreeReaderCNCH.h>

#include <Columns/ColumnArray.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/MapHelpers.h>
#include <DataTypes/NestedUtils.h>
#include <Interpreters/inplaceBlockConversions.h>
#include <IO/ReadBufferFromFileBase.h>
#include <Storages/DiskCache/DiskCacheFactory.h>
#include <Storages/DiskCache/IDiskCacheStrategy.h>
#include <Storages/DiskCache/DiskCacheSegment.h>
#include <Storages/MergeTree/IMergeTreeReader.h>
#include <Storages/MergeTree/MergeTreeDataPartWide.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/MergeTree/MergeTreeReaderStreamWithSegmentCache.h>
#include <bits/types/clockid_t.h>
#include <Common/escapeForFileName.h>
#include <Common/typeid_cast.h>
#include <Core/NamesAndTypes.h>
#include <DataTypes/Serializations/ISerialization.h>
#include <IO/ReadBufferFromFileBase.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Storages/DiskCache/DiskCache_fwd.h>
#include <Poco/Logger.h>
#include <utility>

namespace ProfileEvents
{
    extern const Event CnchReadRowsFromDiskCache;
    extern const Event CnchReadRowsFromRemote;
    extern const Event CnchReadDataMicroSeconds;
    extern const Event CnchAddStreamsElapsedMilliseconds;
    extern const Event CnchAddStreamsParallelTasks;
    extern const Event CnchAddStreamsParallelElapsedMilliseconds;
    extern const Event CnchAddStreamsSequentialTasks;
    extern const Event CnchAddStreamsSequentialElapsedMilliseconds;
}

namespace DB
{

namespace
{
    using OffsetColumns = std::map<std::string, ColumnPtr>;
}

namespace ErrorCodes
{
    extern const int MEMORY_LIMIT_EXCEEDED;
}

MergeTreeReaderCNCH::MergeTreeReaderCNCH(
    const DataPartCNCHPtr & data_part_,
    const NamesAndTypesList & columns_,
    const StorageMetadataPtr & metadata_snapshot_,
    UncompressedCache * uncompressed_cache_,
    MarkCache * mark_cache_,
    const MarkRanges & mark_ranges_,
    const MergeTreeReaderSettings & settings_,
    const ValueSizeMap & avg_value_size_hints_,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback_,
    clockid_t clock_type_)
    : IMergeTreeReader(
        data_part_, columns_, metadata_snapshot_, uncompressed_cache_,
        mark_cache_, mark_ranges_, settings_, avg_value_size_hints_)
    , log(&Poco::Logger::get("MergeTreeReaderCNCH(" + data_part_->get_name() + ")"))

{
    if (data_part->enableDiskCache())
    {
        auto [cache, cache_strategy] = DiskCacheFactory::instance().getDefault();

        segment_cache_strategy = std::move(cache_strategy);
        segment_cache = std::move(cache);
    }

    initializeStreams(profile_callback_, clock_type_);
}

size_t MergeTreeReaderCNCH::readRows(size_t from_mark, bool continue_reading, size_t max_rows_to_read, Columns & res_columns)
{
    if (!continue_reading)
        next_row_number_to_read = data_part->index_granularity.getMarkStartingRow(from_mark);

    size_t read_rows = 0;
    try
    {
        size_t num_columns = columns.size();
        checkNumberOfColumns(num_columns);

        std::unordered_map<String, size_t> res_col_to_idx;
        auto column_it = columns.begin();
        for (size_t i = 0; i < num_columns; ++i, ++column_it)
        {
            const auto & [name, type] = getColumnFromPart(*column_it);
            res_col_to_idx[name] = i;
        }

        auto sort_columns = columns;
        if (!dup_implicit_keys.empty())
            sort_columns.sort([](const auto & lhs, const auto & rhs) { return (!lhs.type->isMap()) && rhs.type->isMap(); });

        /// Pointers to offset columns that are common to the nested data structure columns.
        /// If append is true, then the value will be equal to nullptr and will be used only to
        /// check that the offsets column has been already read.
        OffsetColumns offset_columns;
        std::unordered_map<String, ISerialization::SubstreamsCache> caches;

        int row_number_column_pos = -1;
        auto name_and_type = sort_columns.begin();
        for (size_t i = 0; i < num_columns; ++i, ++name_and_type)
        {
            auto column_from_part = getColumnFromPart(*name_and_type);
            const auto & [name, type] = column_from_part;
            size_t pos = res_col_to_idx[name];

            /// The column is already present in the block so we will append the values to the end.
            bool append = res_columns[pos] != nullptr;
            if (!append)
                res_columns[pos] = type->createColumn();

            /// row number column will be populated at last after `read_rows` is set
            if (name == "_part_row_number")
            {
                row_number_column_pos = pos;
                continue;
            }

            auto & column = res_columns[pos];
            try
            {
                size_t column_size_before_reading = column->size();
                auto & cache = caches[column_from_part.getNameInStorage()];
                if (type->isMap() && !type->isMapKVStore())
                    readMapDataNotKV(
                        column_from_part, column, from_mark, continue_reading, max_rows_to_read, caches, res_col_to_idx, res_columns);
                else
                    readData(column_from_part, column, from_mark, continue_reading, max_rows_to_read, cache);

                /// For elements of Nested, column_size_before_reading may be greater than column size
                ///  if offsets are not empty and were already read, but elements are empty.
                if (!column->empty())
                    read_rows = std::max(read_rows, column->size() - column_size_before_reading);
            }
            catch (Exception & e)
            {
                /// Better diagnostics.
                e.addMessage("(while reading column " + name + ")");
                throw;
            }

            if (column->empty())
                res_columns[pos] = nullptr;
        }

        /// Populate _part_row_number column if requested
        if (row_number_column_pos >= 0)
        {
            /// update `read_rows` if no physical columns are read (only _part_row_number is requested)
            if (columns.size() == 1)
            {
                read_rows = std::min(max_rows_to_read, data_part->rows_count - next_row_number_to_read);
            }

            if (read_rows)
            {
                auto mutable_column = res_columns[row_number_column_pos]->assumeMutable();
                ColumnUInt64 & column = assert_cast<ColumnUInt64 &>(*mutable_column);
                for (size_t i = 0, row_number = next_row_number_to_read; i < read_rows; ++i)
                    column.insertValue(row_number++);
                res_columns[row_number_column_pos] = std::move(mutable_column);
            }
            else
            {
                res_columns[row_number_column_pos] = nullptr;
            }
        }

        /// NOTE: positions for all streams must be kept in sync.
        /// In particular, even if for some streams there are no rows to be read,
        /// you must ensure that no seeks are skipped and at this point they all point to to_mark.
    }
    catch (Exception & e)
    {
        if (e.code() != ErrorCodes::MEMORY_LIMIT_EXCEEDED)
            storage.reportBrokenPart(data_part->name);

        /// Better diagnostics.
        e.addMessage("(while reading from part " + data_part->getFullPath() + " "
                     "from mark " + toString(from_mark) + " "
                     "with max_rows_to_read = " + toString(max_rows_to_read) + ")");
        throw;
    }
    catch (...)
    {
        storage.reportBrokenPart(data_part->name);

        throw;
    }

    next_row_number_to_read += read_rows;
    return read_rows;
}

void MergeTreeReaderCNCH::initializeStreams(const ReadBufferFromFileBase::ProfileCallback& profile_callback, clockid_t clock_type)
{
    Stopwatch watch;
    SCOPE_EXIT({ ProfileEvents::increment(ProfileEvents::CnchAddStreamsElapsedMilliseconds, watch.elapsedMilliseconds()); });

    try
    {
        FileStreamBuilders stream_builders;

        for (const NameAndTypePair& column : columns)
        {
            initializeStreamForColumnIfNoBurden(column, profile_callback, clock_type, &stream_builders);
        }

        executeFileStreamBuilders(stream_builders);
    }
    catch (...)
    {
        storage.reportBrokenPart(data_part->name);
        throw;
    }
}

void MergeTreeReaderCNCH::initializeStreamForColumnIfNoBurden(
    const NameAndTypePair & column,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback,
    clockid_t clock_type,
    FileStreamBuilders * stream_builders)
{
    auto column_from_part = getColumnFromPart(column);
    if (column_from_part.type->isMap() && !column_from_part.type->isMapKVStore())
    {
        // Scan the directory to get all implicit columns(stream) for the map type
        const DataTypeByteMap & type_map = typeid_cast<const DataTypeByteMap &>(*column_from_part.type);

        for (auto & file : data_part->getChecksums()->files)
        {
            // Try to get keys, and form the stream, its bin file name looks like "NAME__xxxxx.bin"
            const String & file_name = file.first;
            if (isMapImplicitDataFileNameNotBaseOfSpecialMapName(file_name, column.name))
            {
                auto key_name = parseKeyNameFromImplicitFileName(file_name, column.name);
                auto impl_key_name = getImplicitColNameForMapKey(column.name, key_name);
                // Special handing if implicit key is referenced too
                if (columns.contains(impl_key_name))
                {
                    dup_implicit_keys.insert(impl_key_name);
                }

                addStreamsIfNoBurden(
                    {impl_key_name, type_map.getValueTypeForImplicitColumn()},
                    [map_col_name = column.name, this](const String& stream_name, const ISerialization::SubstreamPath& substream_path) -> String {
                        String data_file_name = stream_name;
                        if (data_part->versions->enable_compact_map_data)
                        {
                            data_file_name = ISerialization::getFileNameForStream(map_col_name, substream_path);
                        }
                        return data_file_name;
                    },
                    profile_callback, clock_type, stream_builders
                );

                map_column_keys.insert({column.name, key_name});
            }
        }

    }
    else if (isMapImplicitKeyNotKV(column.name)) // check if it's an implicit key and not KV
    {
        String map_col_name = parseMapNameFromImplicitColName(column.name);
        addStreamsIfNoBurden(
            {column.name, column.type},
            [map_col_name, this](const String& stream_name,
                    const ISerialization::SubstreamPath& substream_path) {
                String data_file_name = stream_name;
                if (data_part->versions->enable_compact_map_data)
                {
                    data_file_name = ISerialization::getFileNameForStream(
                        map_col_name, substream_path);
                }
                return data_file_name;
            },
            profile_callback, clock_type, stream_builders
        );
    }
    else if (column.name != "_part_row_number")
    {
        addStreamsIfNoBurden(column_from_part,
            [](const String & stream_name, [[maybe_unused]] const ISerialization::SubstreamPath& substream_path) {
                return stream_name;
            },
            profile_callback, clock_type, stream_builders
        );
    }
}

void MergeTreeReaderCNCH::executeFileStreamBuilders(FileStreamBuilders& stream_builders)
{
    if (stream_builders.size() <= 1)
    {
        ProfileEvents::increment(ProfileEvents::CnchAddStreamsSequentialTasks,
            stream_builders.size());
        Stopwatch watch_seq;
        SCOPE_EXIT({
            ProfileEvents::increment(
                ProfileEvents::CnchAddStreamsSequentialElapsedMilliseconds,
                watch_seq.elapsedMilliseconds());
        });

        for (const auto & [stream_name, builder] : stream_builders)
            streams.emplace(std::move(stream_name), builder());
    }
    else
    {
        ProfileEvents::increment(ProfileEvents::CnchAddStreamsParallelTasks, stream_builders.size());
        Stopwatch watch_pl;
        SCOPE_EXIT({
            ProfileEvents::increment(
                ProfileEvents::CnchAddStreamsParallelElapsedMilliseconds,
                watch_pl.elapsedMilliseconds());
        });

        ThreadPool pool(std::min(stream_builders.size(), 16UL));
        auto thread_group = CurrentThread::getGroup();
        for (const auto & [stream_name, builder] : stream_builders)
        {
            // placeholder
            auto it = streams.emplace(std::move(stream_name), nullptr).first;
            pool.scheduleOrThrowOnError(
                [it, thread_group, builder = std::move(builder)] {
                    setThreadName("AddStreamsThr");
                    if (thread_group)
                        CurrentThread::attachToIfDetached(thread_group);

                    it->second = builder();
                }
            );
        }
        pool.wait();
    }
}

void MergeTreeReaderCNCH::addStreamsIfNoBurden(
    const NameAndTypePair & name_and_type,
    const std::function<String(const String &, const ISerialization::SubstreamPath &)> & file_name_getter,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback,
    clockid_t clock_type, FileStreamBuilders * stream_builders)
{
    ISerialization::StreamCallback callback = [&](const ISerialization::SubstreamPath& substream_path) {
        String stream_name = ISerialization::getFileNameForStream(name_and_type, substream_path);

        if (streams.count(stream_name))
            return;

        String file_name = file_name_getter(stream_name, substream_path);
        bool data_file_exists = data_part->getChecksums()->files.count(file_name + DATA_FILE_EXTENSION);

        if (!data_file_exists)
            return;

        IMergeTreeDataPartPtr source_data_part = data_part->getMvccDataPart(stream_name + DATA_FILE_EXTENSION);
        String mark_file_name = source_data_part->index_granularity_info.getMarksFilePath(stream_name);

        /// data file
        String data_path = source_data_part->getFullRelativePath() + "data";
        off_t data_file_offset = source_data_part->getFileOffsetOrZero(stream_name + DATA_FILE_EXTENSION);
        size_t data_file_size = source_data_part->getFileSizeOrZero(stream_name + DATA_FILE_EXTENSION);

        /// mark file
        String mark_path = source_data_part->getFullRelativePath() + "data";
        off_t mark_file_offset = source_data_part->getFileOffsetOrZero(mark_file_name);
        size_t mark_file_size = source_data_part->getFileSizeOrZero(mark_file_name);

        if (segment_cache_strategy)
        {
            // Cache segment if necessary
            IDiskCacheSegmentsVector segments
                = segment_cache_strategy->getCacheSegments(segment_cache_strategy->transferRangesToSegments<DiskCacheSegment>(
                    all_mark_ranges,
                    source_data_part,
                    DiskCacheSegment::FileOffsetAndSize{mark_file_offset, mark_file_size},
                    source_data_part->getMarksCount(),
                    stream_name,
                    DATA_FILE_EXTENSION,
                    DiskCacheSegment::FileOffsetAndSize{data_file_offset, data_file_size}));
            segment_cache->cacheSegmentsToLocalDisk(segments);
        }

        std::function<MergeTreeReaderStreamUniquePtr()> stream_builder = [=, this]() {
            return std::make_unique<MergeTreeReaderStreamWithSegmentCache>(
                source_data_part->storage.getStorageID(),
                source_data_part->get_name(),
                stream_name,
                source_data_part->volume->getDisk(),
                source_data_part->getMarksCount(),
                data_path, data_file_offset, data_file_size,
                mark_path, mark_file_offset, mark_file_size,
                all_mark_ranges, settings, mark_cache, uncompressed_cache,
                segment_cache.get(),
                segment_cache_strategy == nullptr ? 1 : segment_cache_strategy->getSegmentSize(),
                &(source_data_part->index_granularity_info),
                profile_callback, clock_type
            );
            // TODO: here we can use the pointer to source_data_part's index_granularity_info, because *source_data_part will not be destoryed
        };

        // Check if mark is present
        auto mark_cache_key = mark_cache->hash(fullPath(source_data_part->volume->getDisk(), mark_path) + ":" + stream_name);

        if (mark_cache->get(mark_cache_key))
        {
            ProfileEvents::increment(ProfileEvents::CnchAddStreamsSequentialTasks);
            Stopwatch watch_seq;
            SCOPE_EXIT({
                ProfileEvents::increment(ProfileEvents::CnchAddStreamsSequentialElapsedMilliseconds,
                watch_seq.elapsedMilliseconds());
            });

            /// able to get marks from mark cache
            streams.emplace(std::move(stream_name), stream_builder());
        }
        else
        {
            /// prepare for loading marks parallel
            stream_builders->emplace(std::move(stream_name), std::move(stream_builder));
        }
    };

    auto serialization = data_part->getSerializationForColumn(name_and_type);
    serialization->enumerateStreams(callback);
    serializations.emplace(name_and_type.name, std::move(serialization));
}

}
