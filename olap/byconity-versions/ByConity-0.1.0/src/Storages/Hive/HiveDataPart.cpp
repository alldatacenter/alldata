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

#include <Processors/Formats/Impl/ArrowBlockInputFormat.h>
#include <Processors/Formats/Impl/ArrowBufferedStreams.h>
#include <Storages/Hive/HiveDataPart.h>
#include <Storages/StorageCnchHive.h>
#include <arrow/api.h>
#include <arrow/buffer.h>
#include <arrow/status.h>
#include <parquet/arrow/reader.h>
#include <parquet/file_reader.h>
#include <parquet/statistics.h>


namespace DB
{
#define THROW_ARROW_NOT_OK(status) \
    do \
    { \
        if (const ::arrow::Status & _s = (status); !_s.ok()) \
            throw Exception(_s.ToString(), ErrorCodes::BAD_ARGUMENTS); \
    } while (false)

template <class FieldType, class StatisticsType>
Range createRangeFromParquetStatistics(std::shared_ptr<StatisticsType> stats)
{
    ///
    if (!stats->HasMinMax())
        return Range();
    return Range(FieldType(stats->min()), true, FieldType(stats->max()), true);
}

Range createRangeFromParquetStatistics(std::shared_ptr<parquet::ByteArrayStatistics> stats)
{
    if (!stats->HasMinMax())
        return Range();

    String min_val(reinterpret_cast<const char *>(stats->min().ptr), stats->min().len);
    String max_val(reinterpret_cast<const char *>(stats->max().ptr), stats->max().len);
    return Range(min_val, true, max_val, true);
}

HiveDataPart::HiveDataPart(
    const String & name_,
    const String & relative_path_,
    const DiskPtr & disk_,
    const HivePartInfo & info_,
    HDFSConnectionParams hdfs_params_,
    std::unordered_set<Int64> skip_splits_,
    NamesAndTypesList index_names_and_types_)
    : name(name_)
    , relative_path(relative_path_)
    , disk(disk_)
    , info(info_)
    , hdfs_params(hdfs_params_)
    , skip_splits(skip_splits_)
    , index_names_and_types(index_names_and_types_)
{
}

String HiveDataPart::getFullDataPartPath() const
{
    return relative_path + "/" + name;
}

String HiveDataPart::getFullTablePath() const
{
    return relative_path;
}

HiveDataPart::~HiveDataPart() = default;

void HiveDataPart::loadSplitMinMaxIndexes()
{
    if (split_minmax_idxes_loaded)
        return;
    loadSplitMinMaxIndexesImpl();
    split_minmax_idxes_loaded = true;
}

size_t HiveDataPart::getTotalRowGroups() const
{
    size_t res;
    {
        std::lock_guard lock(mutex);
        if (!reader)
            prepareReader();

        auto meta = reader->parquet_reader()->metadata();
        if (meta)
            res = meta->num_row_groups();
        else
            throw Exception("Unexpected error of getTotalRowGroups. because of meta is NULL", ErrorCodes::LOGICAL_ERROR);
    }
    return res;
}

void HiveDataPart::prepareReader() const
{
    in = std::make_unique<ReadBufferFromByteHDFS>(getFullDataPartPath(), true, hdfs_params);
    THROW_ARROW_NOT_OK(parquet::arrow::OpenFile(asArrowFile(*in), arrow::default_memory_pool(), &reader));
}

/// use arrow library, loop parquet datapart row groups
/// build requried column min、max range.
/// currently only support basic data type
void HiveDataPart::loadSplitMinMaxIndexesImpl()
{
    // if(!reader)
    //     prepareReader();

    // auto meta = reader->parquet_reader()->metadata();
    // size_t num_cols = meta->num_columns();
    // size_t num_row_groups = meta->num_row_groups();
    // const auto * schema = meta->schema();
    // for(size_t pos = 0; pos < num_cols; ++pos)
    // {
    //     String column{schema->Column(pos)->name()};
    //     boost::to_lower(column);
    //     parquet_column_positions[column] = pos;
    // }

    // split_minmax_idxes.resize(num_row_groups);
    // // LOG_TRACE(&Logger::get("HiveDataPart"), " num_row_groups: " << num_row_groups << " index_names_and_types size: " << index_names_and_types.size());

    // for(size_t i = 0; i < num_row_groups; ++i)
    // {
    //     auto row_group_meta = meta->RowGroup(i);
    //     split_minmax_idxes[i] = std::make_shared<IMergeTreeDataPart::MinMaxIndex>();
    //     split_minmax_idxes[i]->parallelogram.resize(num_cols);

    //     size_t j = 0;
    //     auto it = index_names_and_types.begin();
    //     for(; it != index_names_and_types.end(); ++j, ++it)
    //     {
    //         String column{it->name};

    //         boost::to_lower(column);
    //         auto mit = parquet_column_positions.find(column);
    //         if(mit == parquet_column_positions.end())
    //             continue;

    //         size_t pos = mit->second;
    //         auto col_chunk = row_group_meta->ColumnChunk(pos);
    //         if(!col_chunk->is_stats_set())
    //             continue;

    //         auto stats = col_chunk->statistics();
    //         if(stats->null_count() > 0)
    //             continue;

    //         if(auto bool_status = std::dynamic_pointer_cast<parquet::BoolStatistics>(stats))
    //         {
    //             split_minmax_idxes[i]->parallelogram[j] = createRangeFromParquetStatistics<UInt8>(bool_status);
    //         }
    //         else if(auto int32_stats = std::dynamic_pointer_cast<parquet::Int32Statistics>(stats))
    //         {
    //             split_minmax_idxes[i]->parallelogram[j] = createRangeFromParquetStatistics<Int32>(int32_stats);
    //         }
    //         else if(auto int64_stats = std::dynamic_pointer_cast<parquet::Int64Statistics>(stats))
    //         {
    //             split_minmax_idxes[i]->parallelogram[j] = createRangeFromParquetStatistics<Int64>(int64_stats);
    //         }
    //         else if(auto float_stats = std::dynamic_pointer_cast<parquet::FloatStatistics>(stats))
    //         {
    //             split_minmax_idxes[i]->parallelogram[j] = createRangeFromParquetStatistics<Float64>(float_stats);
    //         }
    //         else if(auto double_stats = std::dynamic_pointer_cast<parquet::FloatStatistics>(stats))
    //         {
    //             split_minmax_idxes[i]->parallelogram[j] = createRangeFromParquetStatistics<Float64>(double_stats);
    //         }
    //         else if(auto string_stats = std::dynamic_pointer_cast<parquet::ByteArrayStatistics>(stats))
    //         {
    //             split_minmax_idxes[i]->parallelogram[j] = createRangeFromParquetStatistics(string_stats);
    //         }
    //         /// Other type are not supported for minmax index, skip

    //     }
    //     split_minmax_idxes[i]->initialized = true;
    // }

    // for(auto split_minmax_idx : split_minmax_idxes)
    // {
    //     // LOG_TRACE(&Logger::get("HiveDataPart"), " idx: " << describeMinMaxIndex(split_minmax_idx));
    // }
}

// String HiveDataPart::describeMinMaxIndex(const MinMaxIndexPtr & idx) const
// {
//     if(!idx)
//         return "";

//     std::vector<String> strs;
//     strs.reserve(index_names_and_types.size());
//     size_t i = 0;
//     for(const auto & name_type : index_names_and_types)
//         strs.push_back(name_type.name + ":" + name_type.type->getName() + idx->parallelogram[i++].toString());
//     return boost::algorithm::join(strs, "|");
// }

}
