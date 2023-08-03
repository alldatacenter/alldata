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

#include <DataTypes/Serializations/ISerialization.h>
#include <Columns/IColumn.h>
#include <IO/WriteHelpers.h>
#include <IO/Operators.h>
#include <Common/escapeForFileName.h>
#include <DataTypes/NestedUtils.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int MULTIPLE_STREAMS_REQUIRED;
    extern const int UNEXPECTED_DATA_AFTER_PARSED_VALUE;
    extern const int NOT_IMPLEMENTED;
}

String ISerialization::Substream::toString() const
{
    switch (type)
    {
        case ArrayElements:
            return "ArrayElements";
        case ArraySizes:
            return "ArraySizes";
        case NullableElements:
            return "NullableElements";
        case NullMap:
            return "NullMap";
        case TupleElement:
            return "TupleElement(" + tuple_element_name + ", "
                + std::to_string(escape_tuple_delimiter) + ")";
        case DictionaryKeys:
            return "DictionaryKeys";
        case DictionaryIndexes:
            return "DictionaryIndexes";
        case SparseElements:
            return "SparseElements";
        case SparseOffsets:
            return "SparseOffsets";
        case MapKeyElements:
            return "MapKeyElements";
        case MapValueElements:
            return "MapValueElements";
        case MapSizes:
            return "MapSizes";
    }

    __builtin_unreachable();
}

String ISerialization::SubstreamPath::toString() const
{
    WriteBufferFromOwnString wb;
    wb << "{";
    for (size_t i = 0; i < size(); ++i)
    {
        if (i != 0)
            wb << ", ";
        wb << at(i).toString();
    }
    wb << "}";
    return wb.str();
}

void ISerialization::enumerateStreams(const StreamCallback & callback, SubstreamPath & path) const
{
    callback(path);
}

void ISerialization::serializeBinaryBulk(const IColumn & column, WriteBuffer &, size_t, size_t) const
{
    throw Exception(ErrorCodes::MULTIPLE_STREAMS_REQUIRED, "Column {} must be serialized with multiple streams", column.getName());
}

void ISerialization::deserializeBinaryBulk(IColumn & column, ReadBuffer &, size_t, double) const
{
    throw Exception(ErrorCodes::MULTIPLE_STREAMS_REQUIRED, "Column {} must be deserialized with multiple streams", column.getName());
}

void ISerialization::serializeBinaryBulkWithMultipleStreams(
    const IColumn & column,
    size_t offset,
    size_t limit,
    SerializeBinaryBulkSettings & settings,
    SerializeBinaryBulkStatePtr & /* state */) const
{
    if (WriteBuffer * stream = settings.getter(settings.path))
        serializeBinaryBulk(column, *stream, offset, limit);
}

void ISerialization::deserializeBinaryBulkWithMultipleStreams(
    ColumnPtr & column,
    size_t limit,
    DeserializeBinaryBulkSettings & settings,
    DeserializeBinaryBulkStatePtr & /* state */,
    SubstreamsCache * cache) const
{
    auto cached_column = getFromSubstreamsCache(cache, settings.path);
    if (cached_column)
    {
        column = cached_column;
    }
    else if (ReadBuffer * stream = settings.getter(settings.path))
    {
        auto mutable_column = column->assumeMutable();
        deserializeBinaryBulk(*mutable_column, *stream, limit, settings.avg_value_size_hint);
        column = std::move(mutable_column);
        addToSubstreamsCache(cache, settings.path, column);
    }
}

static String getNameForSubstreamPath(
    String stream_name,
    const ISerialization::SubstreamPath & path,
    bool escape_tuple_delimiter)
{
    using Substream = ISerialization::Substream;

    size_t array_level = 0;
    size_t null_level = 0;
    for (const auto & elem : path)
    {
        if (elem.type == Substream::NullMap)
            stream_name += ".null" + (null_level > 0 ? toString(null_level): "");
        else if (elem.type == Substream::ArraySizes)
            stream_name += ".size" + toString(array_level);
        else if (elem.type == Substream::ArrayElements)
            ++array_level;
        else if (elem.type == Substream::NullableElements)
            ++null_level;
        else if (elem.type == Substream::DictionaryKeys)
            stream_name += ".dict";
        else if (elem.type == Substream::SparseOffsets)
            stream_name += ".sparse.idx";
        else if (elem.type == Substream::TupleElement)
        {
            /// For compatibility reasons, we use %2E (escaped dot) instead of dot.
            /// Because nested data may be represented not by Array of Tuple,
            ///  but by separate Array columns with names in a form of a.b,
            ///  and name is encoded as a whole.
            stream_name += (escape_tuple_delimiter && elem.escape_tuple_delimiter ?
                escapeForFileName(".") : ".") + escapeForFileName(elem.tuple_element_name);
        }
        else if (elem.type == Substream::MapKeyElements)
        {
            ++array_level;
            stream_name += "%2Ekey";
        }
        else if (elem.type == Substream::MapValueElements)
        {
            ++array_level;
            stream_name += "%2Evalue";
        }
        else if (elem.type == Substream::MapSizes)
            stream_name += ".size" + toString(array_level);
    }

    return stream_name;
}

String ISerialization::getFileNameForStream(const NameAndTypePair & column, const SubstreamPath & path)
{
    return getFileNameForStream(column.getNameInStorage(), path);
}

String ISerialization::getFileNameForStream(const String & name_in_storage, const SubstreamPath & path)
{
    String stream_name;
    auto nested_storage_name = Nested::extractTableName(name_in_storage);
    if (name_in_storage != nested_storage_name && (path.size() == 1 && path[0].type == ISerialization::Substream::ArraySizes))
        stream_name = escapeForFileName(nested_storage_name);
    else
        stream_name = escapeForFileName(name_in_storage);

    return getNameForSubstreamPath(std::move(stream_name), path, true);
}

String ISerialization::getSubcolumnNameForStream(const SubstreamPath & path)
{
    auto subcolumn_name = getNameForSubstreamPath("", path, false);
    if (!subcolumn_name.empty())
        subcolumn_name = subcolumn_name.substr(1); // It starts with a dot.

    return subcolumn_name;
}

void ISerialization::addToSubstreamsCache(SubstreamsCache * cache, const SubstreamPath & path, ColumnPtr column)
{
    if (cache && !path.empty())
        cache->emplace(getSubcolumnNameForStream(path), column);
}

ColumnPtr ISerialization::getFromSubstreamsCache(SubstreamsCache * cache, const SubstreamPath & path)
{
    if (!cache || path.empty())
        return nullptr;

    auto it = cache->find(getSubcolumnNameForStream(path));
    if (it == cache->end())
        return nullptr;

    return it->second;
}

bool ISerialization::isSpecialCompressionAllowed(const SubstreamPath & path)
{
    for (const auto & elem : path)
    {
        if (elem.type == Substream::NullMap
            || elem.type == Substream::ArraySizes
            || elem.type == Substream::DictionaryIndexes
            || elem.type == Substream::SparseOffsets)
            return false;
    }
    return true;
}

void ISerialization::serializeMemComparable(const IColumn &, size_t, WriteBuffer &) const
{
    throw Exception("Serialization type doesn't support mem-comparable encoding", ErrorCodes::NOT_IMPLEMENTED);
}

void ISerialization::deserializeMemComparable(IColumn &, ReadBuffer &) const
{
    throw Exception("Serialization type doesn't support mem-comparable encoding", ErrorCodes::NOT_IMPLEMENTED);
}

void ISerialization::throwUnexpectedDataAfterParsedValue(IColumn & column, ReadBuffer & istr, const FormatSettings & settings, const String & type_name) const
{
    WriteBufferFromOwnString ostr;
    serializeText(column, column.size() - 1, ostr, settings);
    throw Exception(
        ErrorCodes::UNEXPECTED_DATA_AFTER_PARSED_VALUE,
        "Unexpected data '{}' after parsed {} value '{}'",
        std::string(istr.position(), std::min(size_t(10), istr.available())),
        type_name,
        ostr.str());
}

}
