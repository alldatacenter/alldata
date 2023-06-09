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

#include <Core/NamesAndTypes.h>
#include <Core/Types.h>
#include <DataTypes/IDataType.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Core/Block.h>
#include <Columns/IColumn.h>

namespace DB
{

class DataStream;

class IQueryPlanStep;
using QueryPlanStepPtr = std::shared_ptr<IQueryPlanStep>;

class Context;
using ContextPtr = std::shared_ptr<const Context>;

struct AggregatingTransformParams;
using AggregatingTransformParamsPtr = std::shared_ptr<AggregatingTransformParams>;

class ArrayJoinAction;
using ArrayJoinActionPtr = std::shared_ptr<ArrayJoinAction>;

class TableJoin;
using TableJoinPtr = std::shared_ptr<TableJoin>;

#define SERIALIZE_ENUM(ITEM, BUF) writeBinary(UInt8(ITEM), BUF);

#define DESERIALIZE_ENUM(TYPE, ITEM, BUF) \
    TYPE ITEM; \
    { \
        UInt8 tmp; \
        readBinary(tmp, BUF); \
        ITEM = TYPE(tmp); \
    }

template<typename Type>
void serializeEnum(const Type & item, WriteBuffer & buf)
{
    writeBinary(UInt8(item), buf);
}

template<typename Type>
void deserializeEnum(Type & item, ReadBuffer & buf)
{
    UInt8 tmp;
    readBinary(tmp, buf);
    item = Type(tmp);
}

void serializeStrings(const Strings & strings, WriteBuffer & buf);
Strings deserializeStrings(ReadBuffer & buf);

void serializeStringSet(const NameSet & stringSet, WriteBuffer & buf);
NameSet deserializeStringSet(ReadBuffer & buf);

template<typename T>
void serializeItemVector(const std::vector<T> & itemVec, WriteBuffer & buf)
{
    writeBinary(itemVec.size(), buf);
    for (const auto & item : itemVec)
        item.serialize(buf);
}

template<typename T>
std::vector<T> deserializeItemVector(ReadBuffer & buf)
{
    size_t s_size;
    readBinary(s_size, buf);

    std::vector<T> itemVec(s_size);
    for (size_t i = 0; i < s_size; ++i)
    {
        T item;
        item.deserialize(buf);
        itemVec[i] = item;
    }

    return itemVec;
}

void serializeBlock(const Block & block, WriteBuffer & buf);
void serializeBlockWithData(const Block & block, WriteBuffer & buf);
Block deserializeBlock(ReadBuffer & buf);

void serializeColumn(const ColumnPtr & column, const DataTypePtr & data_type, WriteBuffer & buf);
ColumnPtr deserializeColumn(ReadBuffer & buf);

void serializeDataStream(const DataStream & stream, WriteBuffer & buf);
void serializeDataStreamFromDataStreams(const std::vector<DataStream> & stream, WriteBuffer & buf);
DataStream deserializeDataStream(ReadBuffer & buf);

void serializeAggregatingTransformParams(const AggregatingTransformParamsPtr & params, WriteBuffer & buf);
AggregatingTransformParamsPtr deserializeAggregatingTransformParams(ReadBuffer & buf, ContextPtr context);

void serializeArrayJoinAction(const ArrayJoinActionPtr & array_join, WriteBuffer & buf);
ArrayJoinActionPtr deserializeArrayJoinAction(ReadBuffer & buf, ContextPtr context);

void serializePlanStep(const QueryPlanStepPtr & step, WriteBuffer & buf);
QueryPlanStepPtr deserializePlanStep(ReadBuffer & buf, ContextPtr context);


}
