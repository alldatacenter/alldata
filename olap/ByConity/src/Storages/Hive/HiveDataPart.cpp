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

#include <Storages/Hive/HiveDataPart.h>
#include <boost/algorithm/string/case_conv.hpp>
#include <arrow/io/memory.h>
#include <arrow/io/api.h>
#include <arrow/api.h>
#include <arrow/status.h>
#include <arrow/adapters/orc/adapter.h>
#include <parquet/arrow/reader.h>
#include <parquet/file_reader.h>
#include <parquet/statistics.h>
#include <orc/Statistics.hh>

#include <fmt/core.h>
#include <Core/Types.h>
#include <Common/Exception.h>
#include <Common/typeid_cast.h>
#include <Formats/FormatFactory.h>
#include <Processors/Formats/Impl/ArrowBufferedStreams.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/KeyCondition.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

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
    const String & hdfs_uri_,
    const String & relative_path_,
    const String & format_name_,
    const DiskPtr & disk_,
    const HivePartInfo & info_,
    std::unordered_set<Int64> skip_splits_,
    NamesAndTypesList index_names_and_types_)
    : name(name_)
    , hdfs_uri(hdfs_uri_)
    , relative_path(relative_path_)
    , format_name(format_name_)
    , disk(disk_)
    , info(info_)
    , skip_splits(skip_splits_)
    , index_names_and_types(index_names_and_types_)
{
}

String HiveDataPart::getFullDataPartPath() const
{
    return relative_path + "/" + name;
}

HivePartInfo HiveDataPart::getInfo() const
{
    return info;
}

String HiveDataPart::getRelativePath() const
{
    return relative_path;
}

String HiveDataPart::getFullTablePath() const
{
    return relative_path;
}

String HiveDataPart::getHDFSUri() const
{
    return hdfs_uri;
}

String HiveDataPart::getFormatName() const
{
    return format_name;
}

String HiveDataPart::getName() const
{
    return name;
}

size_t HiveDataPart::getTotalBlockNumber() const
{
    size_t res = 0;
    LOG_TRACE(&Poco::Logger::get("HiveDataPart"), " HiveDataPart format_name = {}", format_name);

    if (toFileFormat(format_name) == FileFormat::ORC)
    {
        auto data_part = std::dynamic_pointer_cast<const HiveORCFile>(shared_from_this());
        res = data_part->getTotalStripes();
    }
    else if (toFileFormat(format_name) == FileFormat::PARQUET)
    {
        auto data_part = std::dynamic_pointer_cast<const HiveParquetFile>(shared_from_this());
        res = data_part->getTotalRowGroups();
    }
    else
        throw Exception("Unexpected Format in CnchHive ,currently only support Parquet/orc", ErrorCodes::LOGICAL_ERROR);

    LOG_TRACE(&Poco::Logger::get("HiveDataPart"), " HiveDataPart res = {}", res);

    return res;
}

void HiveDataPart::loadSplitMinMaxIndexes()
{
    if (split_minmax_idxes_loaded)
        return;
    loadSplitMinMaxIndexesImpl();
    split_minmax_idxes_loaded = true;
}

arrow::Status HiveParquetFile::tryGetTotalRowGroups(size_t & res) const
{
    if (!disk)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Hive disk is not set");

    std::unique_ptr<parquet::arrow::FileReader> reader = nullptr;
    std::unique_ptr<ReadBuffer> in = disk->readFile(getFullDataPartPath());
    arrow::Status read_status = parquet::arrow::OpenFile(asArrowFile(*in), arrow::default_memory_pool(), &reader);
    if (read_status.ok())
    {
        auto parquet_meta = reader->parquet_reader()->metadata();
        if (parquet_meta)
            res = parquet_meta->num_row_groups();
        else
            res = 0;
    }
    else
        res = 0;

    return read_status;
}

size_t HiveParquetFile::getTotalRowGroups() const
{
    if (total_row_groups != 0)
        return total_row_groups;

    size_t res = 0;
    arrow::Status read_status = tryGetTotalRowGroups(res);
    if (!read_status.ok())
        throw Exception("Unexpected error of getTotalRowGroups. because of meta is NULL", ErrorCodes::LOGICAL_ERROR);

    total_row_groups = res;

    LOG_TRACE(&Poco::Logger::get("HiveDataPart"), " num_row_groups: {} total_row_groups: {}", res, total_row_groups);

    return res;
}

void HiveParquetFile::loadSplitMinMaxIndexesImpl()
{
    if (!disk)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Hive disk is not set");

    std::unique_ptr<parquet::arrow::FileReader> reader = nullptr;
    std::unique_ptr<ReadBuffer> in = disk->readFile(getFullDataPartPath());
    THROW_ARROW_NOT_OK(parquet::arrow::OpenFile(asArrowFile(*in), arrow::default_memory_pool(), &reader));

    auto meta = reader->parquet_reader()->metadata();
    size_t num_cols = meta->num_columns();
    size_t num_row_groups = meta->num_row_groups();
    const auto * schema = meta->schema();
    for (size_t pos = 0; pos < num_cols; ++pos)
    {
        String column{schema->Column(static_cast<int>(pos))->name()};
        boost::to_lower(column);
        parquet_column_positions[column] = pos;
    }

    split_minmax_idxes.resize(num_row_groups);
    for (size_t i = 0; i < num_row_groups; ++i)
    {
        auto row_group_meta = meta->RowGroup(static_cast<int>(i));
        split_minmax_idxes[i] = std::make_shared<IMergeTreeDataPart::MinMaxIndex>();
        split_minmax_idxes[i]->hyperrectangle.resize(num_cols, Range());

        size_t j = 0;
        auto it = index_names_and_types.begin();
        for (; it != index_names_and_types.end(); ++j, ++it)
        {
            String column{it->name};
            boost::to_lower(column);
            auto mit = parquet_column_positions.find(column);
            if (mit == parquet_column_positions.end())
                continue;

            size_t pos = mit->second;
            auto col_chunk = row_group_meta->ColumnChunk(static_cast<int>(pos));
            if (!col_chunk->is_stats_set())
                continue;

            auto stats = col_chunk->statistics();
            if (stats->HasNullCount() && stats->null_count() > 0)
                continue;

            if (auto bool_stats = std::dynamic_pointer_cast<parquet::BoolStatistics>(stats))
            {
                split_minmax_idxes[i]->hyperrectangle[j] = createRangeFromParquetStatistics<UInt8>(bool_stats);
            }
            else if (auto int32_stats = std::dynamic_pointer_cast<parquet::Int32Statistics>(stats))
            {
                split_minmax_idxes[i]->hyperrectangle[j] = createRangeFromParquetStatistics<Int32>(int32_stats);
            }
            else if (auto int64_stats = std::dynamic_pointer_cast<parquet::Int64Statistics>(stats))
            {
                split_minmax_idxes[i]->hyperrectangle[j] = createRangeFromParquetStatistics<Int64>(int64_stats);
            }
            else if (auto float_stats = std::dynamic_pointer_cast<parquet::FloatStatistics>(stats))
            {
                split_minmax_idxes[i]->hyperrectangle[j] = createRangeFromParquetStatistics<Float64>(float_stats);
            }
            else if (auto double_stats = std::dynamic_pointer_cast<parquet::FloatStatistics>(stats))
            {
                split_minmax_idxes[i]->hyperrectangle[j] = createRangeFromParquetStatistics<Float64>(double_stats);
            }
            else if (auto string_stats = std::dynamic_pointer_cast<parquet::ByteArrayStatistics>(stats))
            {
                split_minmax_idxes[i]->hyperrectangle[j] = createRangeFromParquetStatistics(string_stats);
            }
            /// Other types are not supported for minmax index, skip
        }
        split_minmax_idxes[i]->initialized = true;
    }
}

arrow::Status HiveORCFile::tryGetTotalStripes(size_t & res) const
{
    if (!disk)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Hive disk is not set");

    std::unique_ptr<arrow::adapters::orc::ORCFileReader> reader = nullptr;
    std::unique_ptr<ReadBuffer> in = disk->readFile(getFullDataPartPath());
    arrow::Status read_status = arrow::adapters::orc::ORCFileReader::Open(asArrowFile(*in), arrow::default_memory_pool(), &reader);
    if (read_status.ok())
    {
        res = reader->NumberOfStripes();
    }
    else
        res = 0;

    return read_status;
}


size_t HiveORCFile::getTotalStripes() const
{
    if (total_stripes != 0)
        return total_stripes;

    size_t res = 0;
    arrow::Status read_status = tryGetTotalStripes(res);
    if (!read_status.ok())
        throw Exception("Unexpected error of getTotalStripes. because of meta is NULL", ErrorCodes::LOGICAL_ERROR);

    total_stripes = res;

    LOG_TRACE(&Poco::Logger::get("HiveDataPart"), "res = {} total_stripes = {}", res, total_stripes);

    return res;
}

void HiveORCFile::loadFileMinMaxIndexImpl()
{
    throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Unsupported currently");
}

void HiveORCFile::loadSplitMinMaxIndexesImpl()
{
    throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Unsupported currently");
}

}
