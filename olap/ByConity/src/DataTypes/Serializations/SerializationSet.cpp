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

#include <DataTypes/Serializations/SerializationSet.h>
#include <Columns/ColumnSet.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadBufferFromString.h>
#include <IO/WriteBufferFromString.h>
#include <Interpreters/Set.h>

#include <Formats/FormatSettings.h>
#include <Formats/ProtobufReader.h>
#include <Common/typeid_cast.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int CANNOT_READ_ALL_DATA;
    extern const int CANNOT_READ_ARRAY_FROM_TEXT;
    extern const int LOGICAL_ERROR;
}

void SerializationSet::serializeBinary(const IColumn & column, size_t, WriteBuffer & ostr) const
{
    const auto & set = typeid_cast<const ColumnSet &>(column).getData();
    if (!set)
        throw Exception("Set is null when serialization", ErrorCodes::LOGICAL_ERROR);

    set->serialize(ostr);
}

void SerializationSet::deserializeBinary(IColumn & column, ReadBuffer & istr) const
{
    auto & column_set = typeid_cast<ColumnSet &>(column);

    auto set = Set::deserialize(istr);

    column_set.setData(set);
}

void SerializationSet::serializeBinaryBulk(const IColumn & column, WriteBuffer & ostr, size_t, size_t) const
{
    serializeBinary(column, 0, ostr);
}

void SerializationSet::deserializeBinaryBulk(IColumn & column, ReadBuffer & istr, size_t, double) const
{
    deserializeBinary(column, istr);
}

void SerializationSet::serializeText(const IColumn & , size_t , WriteBuffer & , const FormatSettings &) const
{
    throw Exception("Serialization type doesn't support serializeText", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::serializeTextJSON(const IColumn & , size_t , WriteBuffer & , const FormatSettings & ) const
{
    throw Exception("Serialization type doesn't support serializeTextJSON", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::deserializeTextJSON(IColumn & , ReadBuffer & , const FormatSettings &) const
{
    throw Exception("Deserialization type doesn't support deserializeTextJSON", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::serializeBinary(const Field & , WriteBuffer & ) const
{
    throw Exception("Serialization type doesn't support serializeBinary", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::deserializeBinary(Field &, ReadBuffer & ) const
{
    throw Exception("Deserialization type doesn't support deserializeBinary", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::deserializeWholeText(IColumn & , ReadBuffer & , const FormatSettings & ) const
{
    throw Exception("Deserialization type doesn't support deserializeWholeText", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::serializeTextEscaped(const IColumn & , size_t , WriteBuffer & , const FormatSettings &) const
{
    throw Exception("Serialization type doesn't support serializeTextEscaped", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::deserializeTextEscaped(IColumn & , ReadBuffer & , const FormatSettings &) const
{
    throw Exception("Deserialization type doesn't support deserializeTextEscaped", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::serializeTextQuoted(const IColumn & , size_t , WriteBuffer & , const FormatSettings &) const
{
    throw Exception("Serialization type doesn't support serializeTextQuoted", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::deserializeTextQuoted(IColumn & , ReadBuffer & , const FormatSettings &) const
{
    throw Exception("Deserialization type doesn't support deserializeTextQuoted", ErrorCodes::NOT_IMPLEMENTED);
}


void SerializationSet::serializeTextXML(const IColumn & , size_t , WriteBuffer & , const FormatSettings &) const
{
    throw Exception("Serialization type doesn't support serializeTextXML", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::serializeTextCSV(const IColumn & , size_t , WriteBuffer & , const FormatSettings &) const
{
    throw Exception("Serialization type doesn't support serializeTextCSV", ErrorCodes::NOT_IMPLEMENTED);
}

void SerializationSet::deserializeTextCSV(IColumn & , ReadBuffer & , const FormatSettings & ) const
{
    throw Exception("Deserialization type doesn't support deserializeTextCSV", ErrorCodes::NOT_IMPLEMENTED);
}
}
