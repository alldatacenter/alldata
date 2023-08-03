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

#include <DataTypes/Serializations/SerializationTime.h>

#include <Columns/ColumnVector.h>
#include <Common/assert_cast.h>
#include <Common/typeid_cast.h>
#include <Core/Types.h>
#include <Formats/FormatSettings.h>
#include <IO/Operators.h>
#include <IO/ReadBuffer.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/WriteHelpers.h>

namespace DB
{

SerializationTime::SerializationTime(UInt32 scale_)
    : SerializationDecimalBase<Decimal64>(DecimalUtils::max_precision<Decimal64>, scale_){}

void SerializationTime::serializeText(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & /*settings*/) const
{
    auto value = assert_cast<const ColumnType &>(column).getData()[row_num];
    writeTimeText(value, scale, ostr);
}

void SerializationTime::deserializeText(IColumn & column, ReadBuffer & istr, const FormatSettings &) const
{
    Decimal64 result = 0;
    readTimeText(result, scale, istr);
    assert_cast<ColumnType &>(column).getData().push_back(result);
}

void SerializationTime::deserializeWholeText(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    deserializeTextEscaped(column, istr, settings);
}

void SerializationTime::serializeTextEscaped(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    serializeText(column, row_num, ostr, settings);
}

static inline void readText(Decimal64 & x, UInt32 scale, ReadBuffer & istr, const FormatSettings &)
{
    readTimeText(x, scale, istr);
}

void SerializationTime::deserializeTextEscaped(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    Decimal64 x = 0;
    readText(x, scale, istr, settings);
    assert_cast<ColumnType &>(column).getData().push_back(x);
}

void SerializationTime::serializeTextQuoted(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    writeChar('\'', ostr);
    serializeText(column, row_num, ostr, settings);
    writeChar('\'', ostr);
}

void SerializationTime::deserializeTextQuoted(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    Decimal64 x = 0;
    assertChar('\'', istr);
    readText(x, scale, istr, settings);
    assertChar('\'', istr);
    assert_cast<ColumnType &>(column).getData().push_back(x);    /// It's important to do this at the end - for exception safety.
}

void SerializationTime::serializeTextJSON(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    writeChar('"', ostr);
    serializeText(column, row_num, ostr, settings);
    writeChar('"', ostr);
}

void SerializationTime::deserializeTextJSON(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    Decimal64 x = 0;
    assertChar('"', istr);
    readText(x, scale, istr, settings);
    assertChar('"', istr);
    assert_cast<ColumnType &>(column).getData().push_back(x);
}

void SerializationTime::serializeTextCSV(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    writeChar('"', ostr);
    serializeText(column, row_num, ostr, settings);
    writeChar('"', ostr);
}

void SerializationTime::deserializeTextCSV(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    Decimal64 x = 0;

    if (istr.eof())
        throwReadAfterEOF();

    char maybe_quote = *istr.position();

    if (isQuoted(maybe_quote))
        ++istr.position();

    readText(x, scale, istr, settings);

    if (isQuoted(maybe_quote))
        assertChar(maybe_quote, istr);

    assert_cast<ColumnType &>(column).getData().push_back(x);
}

}
