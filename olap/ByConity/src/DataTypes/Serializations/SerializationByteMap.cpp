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

#include <common/map.h>

#include <DataTypes/Serializations/SerializationByteMap.h>
#include <DataTypes/Serializations/SerializationArray.h>
#include <DataTypes/Serializations/SerializationLowCardinality.h>

#include <Common/StringUtils/StringUtils.h>
#include <Columns/ColumnByteMap.h>
#include <Columns/ColumnArray.h>
#include <Core/Field.h>
#include <Formats/FormatSettings.h>
#include <Common/typeid_cast.h>
#include <Common/assert_cast.h>
#include <Common/quoteString.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/ReadBufferFromString.h>
#include <IO/Operators.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int CANNOT_READ_MAP_FROM_TEXT;
}

SerializationByteMap::SerializationByteMap(const SerializationPtr & key_, const SerializationPtr & value_/*, const SerializationPtr & nested_*/)
    : key(key_), value(value_)/*, nested(nested_)*/
{
}

/*
static const IColumn & extractNestedColumn(const IColumn & column)
{
    return assert_cast<const ColumnByteMap &>(column).getNestedColumn();
}

static IColumn & extractNestedColumn(IColumn & column)
{
    return assert_cast<ColumnByteMap &>(column).getNestedColumn();
}
*/

namespace
{
    void serializeMapSizesPositionIndependent(const IColumn & column, WriteBuffer & ostr, UInt64 offset, UInt64 limit)
    {
        const ColumnByteMap & column_map = typeid_cast<const ColumnByteMap &>(column);
        const ColumnByteMap::Offsets & offset_values = column_map.getOffsets();
        size_t size = offset_values.size();

        if (!size)
            return;

        size_t end = limit && (offset + limit < size)
            ? offset + limit
            : size;

        ColumnByteMap::Offset prev_offset = offset_values[offset - 1];
        for (size_t i = offset; i < end; ++i)
        {
            ColumnByteMap::Offset current_offset = offset_values[i];
            writeIntBinary(current_offset - prev_offset, ostr);
            prev_offset = current_offset;
        }
    }

    void deserializeMapSizesPositionIndependent(ColumnByteMap & column_map, ReadBuffer & istr, UInt64 limit)
    {
        ColumnByteMap::Offsets & offset_values = column_map.getOffsets();
        size_t initial_size = offset_values.size();
        offset_values.resize(initial_size + limit);

        size_t i = initial_size;
        ColumnArray::Offset current_offset = initial_size ? offset_values[initial_size - 1] : 0;
        while (i < initial_size + limit && !istr.eof())
        {
            ColumnArray::Offset current_size = 0;
            readIntBinary(current_size, istr);
            current_offset += current_size;
            offset_values[i] = current_offset;
            ++i;
        }

        offset_values.resize(i);
    }
}

void SerializationByteMap::serializeBinary(const Field & field, WriteBuffer & ostr) const
{
    const auto & map = get<const ByteMap &>(field);
    writeVarUInt(map.size(), ostr);
    for (const auto & elem : map)
    {
        key->serializeBinary(elem.first, ostr);
        value->serializeBinary(elem.second, ostr);
    }
}

void SerializationByteMap::deserializeBinary(Field & field, ReadBuffer & istr) const
{
    size_t size;
    readVarUInt(size, istr);
    field = ByteMap(size);
    for (auto & elem : field.get<ByteMap &>())
    {
        key->deserializeBinary(elem.first, istr);
        value->deserializeBinary(elem.second, istr);
    }
}

void SerializationByteMap::serializeBinary(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
{
    const ColumnByteMap & column_map = static_cast<const ColumnByteMap &>(column);
    const ColumnByteMap::Offsets & offsets = column_map.getOffsets();

    size_t offset = row_num == 0 ? 0 : offsets[row_num -1];
    size_t next_offset = offsets[row_num];

    const IColumn & key_column = column_map.getKey();
    const IColumn & value_column = column_map.getValue();

    size_t size = next_offset - offset;

    writeVarUInt(size, ostr);

    for (size_t i = offset; i < next_offset; ++i)
    {
        key->serializeBinary(key_column, i, ostr);
        value->serializeBinary(value_column, i, ostr);
    }
}

void SerializationByteMap::deserializeBinary(IColumn & column, ReadBuffer & istr) const
{
    size_t size(0);
    readVarUInt(size, istr);

    ColumnByteMap & column_map = static_cast<ColumnByteMap &>(column);
    ColumnByteMap::Offsets & offsets = column_map.getOffsets();
    IColumn& key_column = column_map.getKey();
    IColumn& value_column = column_map.getValue();

    size_t  ksize = 0, vsize = 0;
    try
    {
        for (; ksize < size; ++ksize)
        {
            key->deserializeBinary(key_column, istr);
            value->deserializeBinary(value_column, istr);
            vsize++;
        }
    }
    catch(...)
    {
        if (ksize)
            key_column.popBack(ksize);
        if (vsize)
            value_column.popBack(vsize);
        throw;
    }

    offsets.push_back((offsets.empty() ? 0 : offsets.back()) + ksize);
}

template <typename KeyWriter, typename ValueWriter>
void SerializationByteMap::serializeTextImpl(
    const IColumn & column, size_t row_num, WriteBuffer & ostr, KeyWriter && key_writer, ValueWriter && value_writer) const
{
    const auto & column_map = assert_cast<const ColumnByteMap &>(column);

    const auto & offsets = column_map.getOffsets();

    size_t offset = offsets[row_num - 1];
    size_t next_offset = offsets[row_num];

    writeChar('{', ostr);
    for (size_t i = offset; i < next_offset; ++i)
    {
        if (i != offset)
            writeChar(',', ostr);

        key_writer(ostr, key, column_map.getKey(), i);
        writeChar(':', ostr);
        value_writer(ostr, value, column_map.getValue(), i);
    }
    writeChar('}', ostr);
}

template <typename KeyReader, typename ValueReader>
void SerializationByteMap::deserializeTextImpl(
    IColumn & column, ReadBuffer & istr, KeyReader && key_reader, ValueReader && value_reader) const
{
    auto & column_map = assert_cast<ColumnByteMap &>(column);

    auto & offsets = column_map.getOffsets();

    auto & key_column = column_map.getKey();
    auto & value_column = column_map.getValue();

    size_t size = 0;
    assertChar('{', istr);

    try
    {
        bool first = true;
        while (!istr.eof() && *istr.position() != '}')
        {
            if (!first)
            {
                if (*istr.position() == ',')
                    ++istr.position();
                else
                    throw Exception("Cannot read Map from text", ErrorCodes::CANNOT_READ_MAP_FROM_TEXT);
            }

            first = false;

            skipWhitespaceIfAny(istr);

            if (*istr.position() == '}')
                break;

            key_reader(istr, key, key_column);
            skipWhitespaceIfAny(istr);
            assertChar(':', istr);

            ++size;
            skipWhitespaceIfAny(istr);
            value_reader(istr, value, value_column);

            skipWhitespaceIfAny(istr);
        }

        offsets.push_back(offsets.back() + size);
        assertChar('}', istr);
    }
    catch (...)
    {
        throw;
    }
}

void SerializationByteMap::serializeText(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    auto writer = [&settings](WriteBuffer & buf, const SerializationPtr & subcolumn_serialization, const IColumn & subcolumn, size_t pos) {
        subcolumn_serialization->serializeTextQuoted(subcolumn, pos, buf, settings);
    };

    serializeTextImpl(column, row_num, ostr, writer, writer);
}

void SerializationByteMap::deserializeText(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    auto reader = [&settings](ReadBuffer & buf, const SerializationPtr & subcolumn_serialization, IColumn & subcolumn) {
        subcolumn_serialization->deserializeTextQuoted(subcolumn, buf, settings);
    };

    deserializeTextImpl(column, istr, reader, reader);
}

void SerializationByteMap::serializeTextJSON(
    const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    auto writer = [&settings](WriteBuffer & buf, const SerializationPtr & subcolumn_serialization, const IColumn & subcolumn, size_t pos) {
        subcolumn_serialization->serializeTextJSON(subcolumn, pos, buf, settings);
    };

    serializeTextImpl(column, row_num, ostr, writer, writer);
}

void SerializationByteMap::deserializeTextJSON(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    auto reader = [&settings](ReadBuffer & buf, const SerializationPtr & subcolumn_serialization, IColumn & subcolumn) {
        subcolumn_serialization->deserializeTextJSON(subcolumn, buf, settings);
    };

    deserializeTextImpl(column, istr, reader, reader);
}

void SerializationByteMap::serializeTextXML(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    const ColumnByteMap & column_map = static_cast<const ColumnByteMap &>(column);
    const ColumnByteMap::Offsets & offsets = column_map.getOffsets();

    size_t offset = row_num == 0 ? 0 : offsets[row_num -1];
    size_t next_offset = offsets[row_num];

    const IColumn & key_column = column_map.getKey();
    const IColumn & value_column = column_map.getValue();

    writeCString("<map>", ostr);
    for (size_t i = offset; i < next_offset; ++i)
    {
        writeCString("<key>", ostr);
        key->serializeTextXML(key_column, i, ostr, settings);
        writeCString("</key>", ostr);
        writeCString("<value>", ostr);
        value->serializeTextXML(value_column, i, ostr, settings);
        writeCString("</value>", ostr);
    }
    writeCString("</map>", ostr);
}

void SerializationByteMap::serializeTextCSV(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    WriteBufferFromOwnString wb;
    serializeText(column, row_num, wb, settings);
    writeCSV(wb.str(), ostr);
}

void SerializationByteMap::deserializeTextCSV(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    String s;
    readCSV(s, istr, settings.csv);
    ReadBufferFromString rb(s);
    deserializeText(column, rb, settings);
}


void SerializationByteMap::enumerateStreams(const StreamCallback & callback, SubstreamPath & path) const
{
    path.push_back(Substream::MapSizes);
    callback(path);

    path.back() = Substream::MapKeyElements;
    key->enumerateStreams(callback, path);

    path.back() = Substream::MapValueElements;
    value->enumerateStreams(callback, path);

    path.pop_back();
}

struct SerializeBinaryBulkStateByteMap : public ISerialization::SerializeBinaryBulkState
{
    /// Record key state and value state separately.
    ISerialization::SerializeBinaryBulkStatePtr key_state, value_state;
};

struct DeserializeBinaryBulkStateByteMap : public ISerialization::DeserializeBinaryBulkState
{
    /// Record key state and value state separately.
    ISerialization::DeserializeBinaryBulkStatePtr key_state, value_state;
};

static SerializeBinaryBulkStateByteMap * checkAndGetByteMapSerializeState(ISerialization::SerializeBinaryBulkStatePtr & state)
{
    if (!state)
        throw Exception("Got empty state for DataTypeByteMap.", ErrorCodes::LOGICAL_ERROR);

    auto * map_state = typeid_cast<SerializeBinaryBulkStateByteMap *>(state.get());
    if (!map_state)
    {
        auto & state_ref = *state;
        throw Exception("Invalid SerializeBinaryBulkState for DataTypeByteMap. Expected: "
                        + demangle(typeid(SerializeBinaryBulkStateByteMap).name()) + ", got "
                        + demangle(typeid(state_ref).name()), ErrorCodes::LOGICAL_ERROR);
    }

    return map_state;
}

static DeserializeBinaryBulkStateByteMap * checkAndGetByteMapDeserializeState(ISerialization::DeserializeBinaryBulkStatePtr & state)
{
    if (!state)
        throw Exception("Got empty state for DataTypeByteMap.", ErrorCodes::LOGICAL_ERROR);

    auto * map_state = typeid_cast<DeserializeBinaryBulkStateByteMap *>(state.get());
    if (!map_state)
    {
        auto & state_ref = *state;
        throw Exception("Invalid DeserializeBinaryBulkState for DataTypeByteMap. Expected: "
                        + demangle(typeid(DeserializeBinaryBulkStateByteMap).name()) + ", got "
                        + demangle(typeid(state_ref).name()), ErrorCodes::LOGICAL_ERROR);
    }

    return map_state;
}

void SerializationByteMap::serializeBinaryBulkStatePrefix(
    SerializeBinaryBulkSettings & settings,
    SerializeBinaryBulkStatePtr & state) const
{
    auto map_state = std::make_shared<SerializeBinaryBulkStateByteMap>();
    settings.path.push_back(Substream::MapKeyElements);
    key->serializeBinaryBulkStatePrefix(settings, map_state->key_state);

    settings.path.back() = Substream::MapValueElements;
    value->serializeBinaryBulkStatePrefix(settings, map_state->value_state);

    settings.path.pop_back();
    state = std::move(map_state);
}

void SerializationByteMap::serializeBinaryBulkStateSuffix(
    SerializeBinaryBulkSettings & settings,
    SerializeBinaryBulkStatePtr & state) const
{
    auto * map_state = checkAndGetByteMapSerializeState(state);
    settings.path.push_back(Substream::MapKeyElements);
    key->serializeBinaryBulkStateSuffix(settings, map_state->key_state);

    settings.path.back() = Substream::MapValueElements;
    value->serializeBinaryBulkStateSuffix(settings, map_state->value_state);

    settings.path.pop_back();
}

void SerializationByteMap::deserializeBinaryBulkStatePrefix(
    DeserializeBinaryBulkSettings & settings,
    DeserializeBinaryBulkStatePtr & state) const
{
    auto map_state = std::make_shared<DeserializeBinaryBulkStateByteMap>();
    settings.path.push_back(Substream::MapKeyElements);
    key->deserializeBinaryBulkStatePrefix(settings, map_state->key_state);

    settings.path.back() = Substream::MapValueElements;
    value->deserializeBinaryBulkStatePrefix(settings, map_state->value_state);

    settings.path.pop_back();
    state = std::move(map_state);
}

void SerializationByteMap::serializeBinaryBulkWithMultipleStreams(
    const IColumn & column,
    size_t offset,
    size_t limit,
    SerializeBinaryBulkSettings & settings,
    SerializeBinaryBulkStatePtr & state) const
{
    const ColumnByteMap & column_map = typeid_cast<const ColumnByteMap &>(column);
    auto * map_state = checkAndGetByteMapSerializeState(state);
    // Current design is as below:
    // - While one data block is written into part(new part), it only explore its up-to-date keys information
    //   in this block. i.e. collect all uniq keys to generate their files(.bin, .mrk ....)
    //
    // - Global keys are unknown for now, and the implementation should not depends this information, so from
    // per part's point of view, it only know subset of keys(and runtime processing should keep this limitation
    // in head.)
    //
    // - While parts are merges, it should be able to handle different keys in parts.
    //

    /// First serialize map sizes
    settings.path.push_back(Substream::MapSizes);
    if (auto stream = settings.getter(settings.path))
    {
        if (settings.position_independent_encoding)
            serializeMapSizesPositionIndependent(column_map, *stream, offset, limit);
        else
            SerializationNumber<ColumnByteMap::Offset>().serializeBinaryBulk(column_map.getOffsetsColumn(), *stream, offset, limit);
    }

    /// Then serialize map key
    settings.path.back() = Substream::MapKeyElements;
    auto & offset_values = column_map.getOffsets();

    if (offset > offset_values.size())
        return;

    size_t end = std::min(offset + limit, offset_values.size());
    size_t key_offset = offset ? offset_values[offset - 1] : 0;
    size_t key_limit = limit ? offset_values[end - 1] - key_offset : 0;

    if (limit == 0 || key_limit)
        key->serializeBinaryBulkWithMultipleStreams(column_map.getKey(), key_offset, key_limit, settings, map_state->key_state);

    /// Then serialize map value
    settings.path.back() = Substream::MapValueElements;
    if (limit == 0 || key_limit)
        value->serializeBinaryBulkWithMultipleStreams(column_map.getValue(), key_offset, key_limit, settings, map_state->value_state);
    settings.path.pop_back();
}

void SerializationByteMap::deserializeBinaryBulkWithMultipleStreams(
    ColumnPtr & column,
    size_t limit,
    DeserializeBinaryBulkSettings & settings,
    DeserializeBinaryBulkStatePtr & state,
    SubstreamsCache * cache) const
{
    auto mutable_column = column->assumeMutable();
    ColumnByteMap & column_map = typeid_cast<ColumnByteMap &>(*mutable_column);
    auto * map_state = checkAndGetByteMapDeserializeState(state);

    settings.path.push_back(Substream::MapSizes);
    if (auto stream = settings.getter(settings.path))
    {
        if (settings.position_independent_encoding)
            deserializeMapSizesPositionIndependent(column_map, *stream, limit);
        else
            SerializationNumber<ColumnByteMap::Offset>().deserializeBinaryBulk(column_map.getOffsetsColumn(), *stream, limit, 0);
    }

    settings.path.back() = Substream::MapKeyElements;
    auto & offset_values = column_map.getOffsets();
    ColumnPtr & key_column = column_map.getKeyPtr();

    size_t last_offset = (offset_values.empty() ? 0 : offset_values.back());
    if (last_offset < key_column->size())
        throw Exception("Nested column is longer than last offset", ErrorCodes::LOGICAL_ERROR);
    size_t key_limit = last_offset - key_column->size();
    key->deserializeBinaryBulkWithMultipleStreams(key_column, key_limit, settings, map_state->key_state, cache);

    settings.path.back() = Substream::MapValueElements;
    value->deserializeBinaryBulkWithMultipleStreams(column_map.getValuePtr(), key_limit, settings, map_state->value_state, cache);
    settings.path.pop_back();
}
}
