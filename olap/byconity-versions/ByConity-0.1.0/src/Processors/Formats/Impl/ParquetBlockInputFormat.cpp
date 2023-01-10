/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include "ParquetBlockInputFormat.h"
#if USE_PARQUET

#include <Formats/FormatFactory.h>
#include <IO/ReadBufferFromMemory.h>
#include <IO/copyData.h>
#include <arrow/api.h>
#include <arrow/io/api.h>
#include <arrow/status.h>
#include <parquet/arrow/reader.h>
#include <parquet/file_reader.h>
#include "ArrowBufferedStreams.h"
#include "ArrowColumnToCHColumn.h"

#include <common/logger_useful.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int CANNOT_READ_ALL_DATA;
}

#define THROW_ARROW_NOT_OK(status)                                     \
    do                                                                 \
    {                                                                  \
        if (::arrow::Status _s = (status); !_s.ok())                   \
            throw Exception(_s.ToString(), ErrorCodes::BAD_ARGUMENTS); \
    } while (false)

ParquetBlockInputFormat::ParquetBlockInputFormat(
    ReadBuffer & in_,
    Block header_,
    const std::map<String, String> & partition_kv_,
    const std::unordered_set<Int64> & skip_row_groups_,
    const size_t row_group_index_,
    bool read_one_group_)
    : IInputFormat(std::move(header_), in_)
    , partition_kv{partition_kv_}
    , skip_row_groups{skip_row_groups_}
    , read_one_group(read_one_group_)
{
    if(read_one_group)
        row_group_current = row_group_index_;
}

// ParquetBlockInputFormat::ParquetBlockInputFormat(ReadBuffer & in_, Block header_)
//     : IInputFormat(std::move(header_), in_)
// {
// }

Chunk ParquetBlockInputFormat::generate()
{
    LOG_TRACE(&Poco::Logger::get("ParquetBlockInputFormat"), " ParquetBlockInputFormat gemerate ");
    Chunk res;

    if (!file_reader)
        prepareReader();

    LOG_TRACE(&Poco::Logger::get("ParquetBlockInputStream"), "readimpl skip_row_groups size: {}", skip_row_groups.size());
    for(; row_group_current < row_group_total && skip_row_groups.contains(row_group_current); ++row_group_current);

    if (row_group_current >= row_group_total)
        return res;

    std::shared_ptr<arrow::Table> table;
    arrow::Status read_status = file_reader->ReadRowGroup(row_group_current, column_indices, &table);
    if (!read_status.ok())
        throw ParsingException{"Error while reading Parquet data: " + read_status.ToString(),
                        ErrorCodes::CANNOT_READ_ALL_DATA};

    ++row_group_current;

    LOG_TRACE(&Poco::Logger::get("ParquetBlockInputFormat"), "CnchHiveThreadSelectBlockInputProcessor row_group_current = {} row_group_total = {}",row_group_current, row_group_total);

    arrow_column_to_ch_column->arrowTableToCHChunk(res, table);

    LOG_TRACE(&Poco::Logger::get("ParquetBlockInputFormat"), "CnchHiveThreadSelectBlockInputProcessor parquet format read size = {}", res.getNumRows());
    return res;
}

void ParquetBlockInputFormat::resetParser()
{
    IInputFormat::resetParser();

    file_reader.reset();
    column_indices.clear();
    row_group_current = 0;
}

static size_t countIndicesForType(std::shared_ptr<arrow::DataType> type)
{
    if (type->id() == arrow::Type::LIST)
        return countIndicesForType(static_cast<arrow::ListType *>(type.get())->value_type());

    if (type->id() == arrow::Type::STRUCT)
    {
        int indices = 0;
        auto * struct_type = static_cast<arrow::StructType *>(type.get());
        for (int i = 0; i != struct_type->num_fields(); ++i)
            indices += countIndicesForType(struct_type->field(i)->type());
        return indices;
    }

    if (type->id() == arrow::Type::MAP)
    {
        auto * map_type = static_cast<arrow::MapType *>(type.get());
        return countIndicesForType(map_type->key_type()) + countIndicesForType(map_type->item_type());
    }

    return 1;
}

void ParquetBlockInputFormat::prepareReader()
{
    THROW_ARROW_NOT_OK(parquet::arrow::OpenFile(asArrowFile(in), arrow::default_memory_pool(), &file_reader));
    row_group_total = file_reader->num_row_groups();
    row_group_current = 0;

    std::shared_ptr<arrow::Schema> schema;
    THROW_ARROW_NOT_OK(file_reader->GetSchema(&schema));

    arrow_column_to_ch_column = std::make_unique<ArrowColumnToCHColumn>(getPort().getHeader(), schema, "Parquet", partition_kv);

    int index = 0;
    for (int i = 0; i < schema->num_fields(); ++i)
    {
        /// STRUCT type require the number of indexes equal to the number of
        /// nested elements, so we should recursively
        /// count the number of indices we need for this type.
        int indexes_count = countIndicesForType(schema->field(i)->type());
        if (getPort().getHeader().has(schema->field(i)->name()))
        {
            for (int j = 0; j != indexes_count; ++j)
                column_indices.push_back(index + j);
        }
        index += indexes_count;
    }
}

void registerInputFormatProcessorParquet(FormatFactory &factory)
{
    factory.registerInputFormatProcessor(
            "Parquet",
            [](ReadBuffer &buf,
                const Block &sample,
                const RowInputFormatParams &,
                const FormatSettings & settings)
            {
                return std::make_shared<ParquetBlockInputFormat>(
                    buf,
                    sample,
                    settings.parquet.partition_kv,
                    settings.parquet.skip_row_groups,
                    settings.parquet.current_row_group,
                    settings.parquet.read_one_group);
            });
    factory.markFormatAsColumnOriented("Parquet");
}

}

#else

namespace DB
{
class FormatFactory;
void registerInputFormatProcessorParquet(FormatFactory &)
{
}
}

#endif
