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


#include <DataTypes/DataTypeBitMap64.h>
#include <DataTypes/Serializations/SerializationBitMap64.h>

#include <Columns/ColumnBitMap64.h>
#include <Columns/ColumnConst.h>

#include <Common/typeid_cast.h>
#include <Common/assert_cast.h>
#include <Formats/FormatSettings.h>

#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <IO/VarInt.h>

#ifdef __SSE2__
#include <emmintrin.h>
#endif


namespace DB
{

void SerializationBitMap64::serializeBinary(const Field & field, WriteBuffer & ostr) const
{
    const BitMap64 & s = get<const BitMap64 &>(field);
    size_t bytes_size = s.getSizeInBytes();
    writeVarUInt(bytes_size, ostr);
    PODArray<char> buffer(bytes_size);
    s.write(buffer.data());
    writeString(buffer.data(), bytes_size, ostr);
}


void SerializationBitMap64::deserializeBinary(Field & field, ReadBuffer & istr) const
{
    UInt64 size{0};
    readVarUInt(size, istr);
    PODArray<char> buffer(size);
    istr.readStrict(buffer.data(), size);

    field = Field(BitMap64::readSafe(buffer.data(), size));
}


void SerializationBitMap64::serializeBinary(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
{
    const auto & s = assert_cast<const ColumnBitMap64 &>(column).getBitMapAt(row_num);
    size_t bytes_size = s.getSizeInBytes();
    writeVarUInt(bytes_size, ostr);
    PODArray<char> buffer(bytes_size);
    s.write(buffer.data());
    writeString(buffer.data(), bytes_size, ostr);
}


void SerializationBitMap64::deserializeBinary(IColumn & column, ReadBuffer & istr) const
{
    ColumnBitMap64 & column_bitmap = assert_cast<ColumnBitMap64 &>(column);
    ColumnBitMap64::Chars & data = column_bitmap.getChars();
    ColumnBitMap64::Offsets & offsets = column_bitmap.getOffsets();

    UInt64 size;
    readVarUInt(size, istr);

    size_t old_chars_size = data.size();
    size_t offset = old_chars_size + size + 1;
    offsets.push_back(offset);

    try
    {
        data.resize(offset);
        istr.readStrict(reinterpret_cast<char*>(&data[offset - size - 1]), size);
        data.back() = 0;
    }
    catch (...)
    {
        offsets.pop_back();
        data.resize_assume_reserved(old_chars_size);
        throw;
    }
}


void SerializationBitMap64::serializeBinaryBulk(const IColumn & column, WriteBuffer & ostr, size_t offset, size_t limit) const
{
    const ColumnBitMap64 & column_bitmap = typeid_cast<const ColumnBitMap64 &>(column);
    const ColumnBitMap64::Chars & data = column_bitmap.getChars();
    const ColumnBitMap64::Offsets & offsets = column_bitmap.getOffsets();

    size_t size = column.size();
    if (!size)
        return;

    size_t end = limit && offset + limit < size
                 ? offset + limit
                 : size;

    if (offset == 0)
    {
        UInt64 str_size = offsets[0] - 1;
        writeVarUInt(str_size, ostr);
        ostr.write(reinterpret_cast<const char *>(data.data()), str_size);

        ++offset;
    }

    for (size_t i = offset; i < end; ++i)
    {
        UInt64 str_size = offsets[i] - offsets[i - 1] - 1;
        writeVarUInt(str_size, ostr);
        ostr.write(reinterpret_cast<const char *>(&data[offsets[i - 1]]), str_size);
    }
}


template <int UNROLL_TIMES>
static NO_INLINE void deserializeBinarySSE2(ColumnBitMap64::Chars & data, ColumnBitMap64::Offsets & offsets, ReadBuffer & istr, size_t limit)
{
    size_t offset = data.size();
    for (size_t i = 0; i < limit; ++i)
    {
        if (istr.eof())
            break;

        UInt64 size;
        readVarUInt(size, istr);

        offset += size + 1;
        offsets.push_back(offset);

        data.resize(offset);

        if (size)
        {
#ifdef __SSE2__
            /// An optimistic branch in which more efficient copying is possible.
            if (offset + 16 * UNROLL_TIMES <= data.capacity() && istr.position() + size + 16 * UNROLL_TIMES <= istr.buffer().end())
            {
                const __m128i * sse_src_pos = reinterpret_cast<const __m128i *>(istr.position());
                const __m128i * sse_src_end = sse_src_pos + (size + (16 * UNROLL_TIMES - 1)) / 16 / UNROLL_TIMES * UNROLL_TIMES;
                __m128i * sse_dst_pos = reinterpret_cast<__m128i *>(&data[offset - size - 1]);

                while (sse_src_pos < sse_src_end)
                {
                    for (size_t j = 0; j < UNROLL_TIMES; ++j)
                        _mm_storeu_si128(sse_dst_pos + j, _mm_loadu_si128(sse_src_pos + j));

                    sse_src_pos += UNROLL_TIMES;
                    sse_dst_pos += UNROLL_TIMES;
                }

                istr.position() += size;
            }
            else
#endif
            {
                istr.readStrict(reinterpret_cast<char*>(&data[offset - size - 1]), size);
            }
        }

        data[offset - 1] = 0;
    }
}


void SerializationBitMap64::deserializeBinaryBulk(IColumn & column, ReadBuffer & istr, size_t limit, double avg_value_size_hint) const
{
    ColumnBitMap64 & column_bitmap = typeid_cast<ColumnBitMap64 &>(column);
    ColumnBitMap64::Chars & data = column_bitmap.getChars();
    ColumnBitMap64::Offsets & offsets = column_bitmap.getOffsets();

    double avg_chars_size = 1; /// By default reserve only for empty strings.

    if (avg_value_size_hint && avg_value_size_hint > sizeof(offsets[0]))
    {
        /// Randomly selected.
        constexpr auto avg_value_size_hint_reserve_multiplier = 1.2;

        avg_chars_size = (avg_value_size_hint - sizeof(offsets[0])) * avg_value_size_hint_reserve_multiplier;
    }

    size_t size_to_reserve = data.size() + std::ceil(limit * avg_chars_size);

    /// Never reserve for too big size.
    if (size_to_reserve < 256 * 1024 * 1024)
    {
        try
        {
            data.reserve(size_to_reserve);
        }
        catch (Exception & e)
        {
            e.addMessage(
                    "(avg_value_size_hint = " + toString(avg_value_size_hint)
                    + ", avg_chars_size = " + toString(avg_chars_size)
                    + ", limit = " + toString(limit) + ")");
            throw;
        }
    }

    offsets.reserve(offsets.size() + limit);

    if (avg_chars_size >= 64)
        deserializeBinarySSE2<4>(data, offsets, istr, limit);
    else if (avg_chars_size >= 48)
        deserializeBinarySSE2<3>(data, offsets, istr, limit);
    else if (avg_chars_size >= 32)
        deserializeBinarySSE2<2>(data, offsets, istr, limit);
    else
        deserializeBinarySSE2<1>(data, offsets, istr, limit);
}


void SerializationBitMap64::serializeText(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings &) const
{
    BitMap64 bitmap = static_cast<const ColumnBitMap64 &>(column).getBitMapAt(row_num);
    String res = bitmap.toString();
    writeString(res, ostr);
}


void SerializationBitMap64::serializeTextEscaped(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings &) const
{
    BitMap64 bitmap = static_cast<const ColumnBitMap64 &>(column).getBitMapAt(row_num);
    String res = bitmap.toString();
    writeEscapedString(res, ostr);
}

// get a uint_64_t number from a char * type
inline uint64_t bitshift(char const* p, char const* q) {
    uint64_t result = 0;
    bool is_negative{false};

    // ascii code 45 is '-'
    if (static_cast<unsigned>(*p) == 45u)
    {
        is_negative = true;
        ++p;
    }

    for (; p < q; ++p)
    {
        auto diff = static_cast<unsigned>(*p - 48);  // ascii code 48 is '0'
        if (diff < 10u)
            result = (result << 1) + (result << 3) + diff;
        else
            throw Exception("Unexpected character: " + String(p, 1)
                    + " , only digit('0' - '9') is allowed", ErrorCodes::LOGICAL_ERROR);
    }

    return is_negative ? static_cast<uint64_t>(-result) : result;
}

// get a uint_64_t number from a string type
inline uint64_t bitshift2(std::string const& value) {
    char const* p = value.c_str();
    char const* q = p + value.size();
    return bitshift(p, q);
}

void SerializationBitMap64::deserializeWholeText(IColumn & column, ReadBuffer & istr, const FormatSettings &) const
{
    if (istr.eof())
        throwReadAfterEOF();

    auto peekAndMoveIfSame =  [&](char c) -> bool {
        if (*istr.position() == c)
        {
            ++istr.position();
            return true;
        }
        return false;
    };

    bool double_quoted = peekAndMoveIfSame('\"');
    bool single_quoted = peekAndMoveIfSame('\'');
    assertChar('[', istr);

    BitMap64 x;
    uint64_t source = 0;
    while (!istr.eof())
    {
        skipWhitespaceIfAny(istr);

        char * next_pos = find_first_symbols<' ', ',', ']'>(istr.position(), istr.buffer().end());

        if (next_pos > istr.position())
        {
            uint64_t temp = bitshift(istr.position(), next_pos);
            if (next_pos != istr.buffer().end() && (*next_pos == ' ' || *next_pos == ',' || *next_pos == ']'))
            {
                if (source > 0) {
                    temp = source * std::pow(10, (next_pos - istr.position())) + temp;
                    source = 0;
                }
                x.add(temp);
            } else {
                if (source > 0)
                    source = source * std::pow(10, (next_pos - istr.position())) + temp;
                else
                    source = temp;
            }
        }

        istr.position() = next_pos;

        while (istr.hasPendingData() && *istr.position() == ' ')
            ++istr.position();

        if (!istr.hasPendingData())
            continue;

        if (*istr.position() == ',')
        {
            ++istr.position();
        }

        if (*istr.position() == ']')
        {
            static_cast<ColumnBitMap64 &>(column).insert(x);
            break;
        }
    }

    assertChar(']', istr);
    if (double_quoted)
        assertChar('\"', istr);
    if (single_quoted)
        assertChar('\'', istr);
}

void SerializationBitMap64::deserializeTextEscaped(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    deserializeWholeText(column, istr, settings);
}


void SerializationBitMap64::serializeTextQuoted(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    serializeText(column, row_num, ostr, settings);
}


void SerializationBitMap64::deserializeTextQuoted(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    deserializeWholeText(column, istr, settings);
}


void SerializationBitMap64::serializeTextJSON(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    writeJSONString(static_cast<const ColumnBitMap64 &>(column).getBitMapAt(row_num).toString(), ostr, settings);
}


void SerializationBitMap64::deserializeTextJSON(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    deserializeWholeText(column, istr, settings);
}


void SerializationBitMap64::serializeTextXML(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & ) const
{
    writeXMLStringForTextElement(static_cast<const ColumnBitMap64 &>(column).getBitMapAt(row_num).toString(), ostr);
}


void SerializationBitMap64::serializeTextCSV(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    WriteBufferFromOwnString wb;
    serializeText(column, row_num, wb, settings);
    writeCSV(wb.str(), ostr);
}

void SerializationBitMap64::deserializeTextCSV(IColumn & column, ReadBuffer & istr, const FormatSettings & settings) const
{
    deserializeWholeText(column, istr, settings);
}

}
