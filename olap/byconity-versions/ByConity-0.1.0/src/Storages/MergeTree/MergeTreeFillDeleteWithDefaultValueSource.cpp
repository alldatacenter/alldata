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

#include <Storages/MergeTree/MergeTreeFillDeleteWithDefaultValueSource.h>
#include <Storages/MergeTree/MergeTreeBlockReadUtils.h>
#include <Interpreters/Context.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int MEMORY_LIMIT_EXCEEDED;
}

MergeTreeFillDeleteWithDefaultValueSource::MergeTreeFillDeleteWithDefaultValueSource(
    const MergeTreeMetaBase & storage_,
    const StorageMetadataPtr & metadata_snapshot_,
    MergeTreeMetaBase::DataPartPtr data_part_,
    ImmutableDeleteBitmapPtr delete_bitmap_,
    Names columns_to_read_)
    /// virtual columns are not allowed in "columns_to_read_"
    : SourceWithProgress(metadata_snapshot_->getSampleBlockForColumns(columns_to_read_, /*virtuals*/{}, storage_.getStorageID()))
    , storage(storage_)
    , metadata_snapshot(metadata_snapshot_)
    , data_part(std::move(data_part_))
    , delete_bitmap(std::move(delete_bitmap_))
    , columns_to_read(std::move(columns_to_read_))
    , continue_reading(false)
    , mark_cache(storage.getContext()->getMarkCache())
{
    {
        size_t num_deleted = delete_bitmap ? delete_bitmap->cardinality() : 0;
        /// Print column name but don't pollute logs in case of many columns.
        if (columns_to_read.size() == 1)
            LOG_DEBUG(log, "Reading {} marks from part {}, total {} rows starting from the beginning of the part with {} deleted, column {}",
                data_part->getMarksCount(), data_part->name, data_part->rows_count, num_deleted, columns_to_read.front());
        else
            LOG_DEBUG(log, "Reading {} marks from part {}, total {} rows starting from the beginning of the part with {} deleted",
                data_part->getMarksCount(), data_part->name, data_part->rows_count, num_deleted);
    }

    addTotalRowsApprox(data_part->rows_count);

    /// Add columns because we don't want to read empty blocks
    injectRequiredColumns(storage, metadata_snapshot, data_part, columns_to_read);
    NamesAndTypesList columns_for_reader = metadata_snapshot->getColumns().getByNames(ColumnsDescription::AllPhysical, columns_to_read, false);

    MergeTreeReaderSettings reader_settings =
    {
        .min_bytes_to_use_direct_io = std::numeric_limits<size_t>::max(), // disable direct io
        .max_read_buffer_size = DBMS_DEFAULT_BUFFER_SIZE,
        .save_marks_in_cache = false
    };

    reader = data_part->getReader(columns_for_reader, metadata_snapshot,
        MarkRanges{MarkRange(0, data_part->getMarksCount())},
        /* uncompressed_cache = */ nullptr, mark_cache.get(), reader_settings);
}

Chunk MergeTreeFillDeleteWithDefaultValueSource::generate()
try
{
    const auto & header = getPort().getHeader();

    if (!isCancelled() && current_row < data_part->rows_count)
    {
        size_t rows_to_read = data_part->index_granularity.getMarkRows(current_mark);
        if (rows_to_read == 0)
            throw Exception(ErrorCodes::LOGICAL_ERROR, "part {} reports 0 row for mark {}", data_part->name, current_mark);

        Columns res_columns;
        res_columns.reserve(header.columns());

        /// return a chunk of defaults when the whole mark is deleted
        if (delete_bitmap && delete_bitmap->containsRange(currentMarkStart(), currentMarkEnd()))
        {
            current_row += rows_to_read;
            current_mark++;
            continue_reading = false;

            for (auto & col : header)
                res_columns.push_back(col.type->createColumnConstWithDefaultValue(rows_to_read)->convertToFullColumnIfConst());
        }
        else
        {

            const auto & sample = reader->getColumns();
            Columns columns(sample.size());
            size_t rows_read = reader->readRows(current_mark, continue_reading, rows_to_read, columns);
            continue_reading = true;

            if (rows_read != rows_to_read)
                throw Exception(ErrorCodes::LOGICAL_ERROR, "Expect {} rows read from mark {} in part {}, got {}",
                    rows_to_read, current_mark, data_part->name, rows_read);

            /// prepare positions of deleted rows in this mark
            size_t num_deleted = 0;
            PODArray<UInt8> is_deleted(rows_read, 0);
            if (delete_bitmap)
            {
                size_t start_row = currentMarkStart();
                size_t end_row = currentMarkEnd();

                auto iter = delete_bitmap->begin();
                iter.equalorlarger(start_row);
                for (auto end = delete_bitmap->end(); iter != end && *iter < end_row; iter++)
                {
                    is_deleted[*iter - start_row] = 1;
                    num_deleted++;
                }
            }

            current_row += rows_read;
            current_mark += (rows_to_read == rows_read);

            bool should_evaluate_missing_defaults = false;
            reader->fillMissingColumns(columns, should_evaluate_missing_defaults, rows_read);

            if (should_evaluate_missing_defaults)
            {
                reader->evaluateMissingDefaults({}, columns);
            }

            reader->performRequiredConversions(columns);

            /// Reorder columns and fill result block.
            std::unordered_map<String, size_t> sample_index_by_name;
            auto sample_it = sample.begin();
            for (size_t i = 0; i < sample.size(); ++i)
            {
                sample_index_by_name[sample_it->name] = i;
                ++sample_it;
            }

            for (size_t ps = 0; ps < header.columns(); ++ps)
            {
                auto name = header.getByPosition(ps).name;
                if (auto it = sample_index_by_name.find(name); it != sample_index_by_name.end())
                    res_columns.emplace_back(std::move(columns[it->second]));
                else
                    throw Exception(ErrorCodes::LOGICAL_ERROR, "Column {} not found", name);
            }

            /// replace deleted rows with default values
            if (num_deleted)
            {
                res_columns = replaceDeletesWithDefaultValues(std::move(res_columns), is_deleted);
            }
        }
        return Chunk(std::move(res_columns), rows_to_read);
    }
    else
    {
        finish();
    }

    return {};
}
catch (...)
{
    /// Suspicion of the broken part. A part is added to the queue for verification.
    if (getCurrentExceptionCode() != ErrorCodes::MEMORY_LIMIT_EXCEEDED)
        storage.reportBrokenPart(data_part->name);
    throw;
}

Columns MergeTreeFillDeleteWithDefaultValueSource::replaceDeletesWithDefaultValues(Columns columns, const PODArray<UInt8> & is_deleted)
{
    if (is_deleted.empty())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "is_deleted must be non-empty");

    Columns res;
    res.reserve(columns.size());

    const auto & header = getPort().getHeader();

    for (size_t ps = 0; ps < columns.size(); ++ps)
    {
        auto new_column = header.getByPosition(ps).type->createColumn();
        auto & src_column = *columns[ps];

        size_t start = 0;
        for (size_t i = 1; i <= is_deleted.size(); ++i)
        {
            if (i == is_deleted.size() || is_deleted[i] != is_deleted[i - 1])
            {
                if (is_deleted[start])
                    new_column->insertManyDefaults(i - start);
                else
                    new_column->insertRangeFrom(src_column, start, i - start);

                start = i;
            }
        }

        res.emplace_back(std::move(new_column));
    }
    return res;
}

void MergeTreeFillDeleteWithDefaultValueSource::finish()
{
    /** Close the files (before destroying the object).
     * When many sources are created, but simultaneously reading only a few of them,
     * buffers don't waste memory.
     */
    reader.reset();
    data_part.reset();
}

MergeTreeFillDeleteWithDefaultValueSource::~MergeTreeFillDeleteWithDefaultValueSource() = default;

}
