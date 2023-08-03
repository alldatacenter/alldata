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

#pragma once

#if USE_RDKAFKA

#include <Core/Types.h>
#include <cppkafka/cppkafka.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <IO/Operators.h>
#include <IO/WriteBufferFromString.h>
#include <IO/ReadBufferFromString.h>
#include <Core/Block.h>

#include <sstream>

namespace DB
{

struct KafkaConsumeInfo
{
    cppkafka::TopicPartitionList offsets;

    size_t consume_index;

    KafkaConsumeInfo(cppkafka::TopicPartitionList & offsets_, size_t consume_index_) : offsets(offsets_), consume_index(consume_index_){}

    String toString()
    {
        WriteBufferFromOwnString buf;
        buf << consume_index << "\n" << offsets.size() << "\n";
        for (auto & tp : offsets)
        {
            buf << tp.get_topic() << "\n" << tp.get_partition() << " " << tp.get_offset() << '\n';
        }
        return buf.str();
    }

    static KafkaConsumeInfo parse(const String &str)
    {
        cppkafka::TopicPartitionList offsets;
        size_t consume_index;
        ReadBufferFromString read_buffer(str);
        UInt64 partition_count = 0;
        String tpl_str;
        read_buffer >> consume_index >> "\n" >> partition_count >> "\n";
        offsets.reserve(partition_count);
        for (UInt64 i = 0; i < partition_count; ++i)
        {
            String topic;
            int partition;
            int64_t offset;
            read_buffer >> topic >> "\n" >> partition >> " " >> offset >> "\n";
            offsets.emplace_back(std::move(topic), partition, offset);
        }

        return KafkaConsumeInfo(offsets, consume_index);
    }
};

template <typename Type>
void extractField(const Block & block, const String & column_name, Type & element, size_t row_index)
{
    if (block.rows() <= row_index || !block.has(column_name))
        return;
    Field info;
    block.getByName(column_name).column->get(row_index, info);
    element = DB::get<const Type &>(info);
}

template<typename DataType, typename Type>
void convertBlock(Block & block, const String & column_name, Type element)
{
    if (block.has(column_name))
        block.erase(column_name);

    ColumnWithTypeAndName col;
    col.name = column_name;
    col.type = std::make_shared<DataType>();
    auto column = col.type->createColumn();
    column->insert(element);
    for (size_t i = 1; i < block.rows(); i++)
        column->insertDefault();
    col.column = std::move(column);
    block.insert(col);
}



}

#endif
