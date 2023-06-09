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

#include <Columns/ColumnConst.h>
#include <Columns/ColumnVector.h>
#include <Core/Field.h>
#include <DataTypes/Serializations/SerializationNumber.h>
#include <Formats/FormatSettings.h>
#include <Formats/ProtobufReader.h>
#include <Formats/ProtobufWriter.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Common/Endian.h>
#include <Common/NaNUtils.h>
#include <Common/assert_cast.h>
#include <Common/typeid_cast.h>

namespace DB
{

template <typename T>
void SerializationNumber<T>::serializeText(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings &) const
{
    writeText(assert_cast<const ColumnVector<T> &>(column).getData()[row_num], ostr);
}

template <typename T>
void SerializationNumber<T>::deserializeText(IColumn & column, ReadBuffer & istr, const FormatSettings &) const
{
    T x;

    if constexpr (is_integer_v<T> && is_arithmetic_v<T>)
        readIntTextUnsafe(x, istr);
    else
        readText(x, istr);

    assert_cast<ColumnVector<T> &>(column).getData().push_back(x);
}

template <typename T>
void SerializationNumber<T>::serializeTextJSON(const IColumn & column, size_t row_num, WriteBuffer & ostr, const FormatSettings & settings) const
{
    auto x = assert_cast<const ColumnVector<T> &>(column).getData()[row_num];
    writeJSONNumber(x, ostr, settings);
}

template <typename T>
void SerializationNumber<T>::deserializeTextJSON(IColumn & column, ReadBuffer & istr, const FormatSettings &) const
{
    bool has_quote = false;
    if (!istr.eof() && *istr.position() == '"')        /// We understand the number both in quotes and without.
    {
        has_quote = true;
        ++istr.position();
    }

    FieldType x;

    /// null
    if (!has_quote && !istr.eof() && *istr.position() == 'n')
    {
        ++istr.position();
        assertString("ull", istr);

        x = NaNOrZero<T>();
    }
    else
    {
        static constexpr bool is_uint8 = std::is_same_v<T, UInt8>;
        static constexpr bool is_int8 = std::is_same_v<T, Int8>;

        if (is_uint8 || is_int8)
        {
            // extra conditions to parse true/false strings into 1/0
            if (istr.eof())
                throwReadAfterEOF();
            if (*istr.position() == 't' || *istr.position() == 'f')
            {
                bool tmp = false;
                readBoolTextWord(tmp, istr);
                x = tmp;
            }
            else
                readText(x, istr);
        }
        else
        {
            readText(x, istr);
        }

        if (has_quote)
            assertChar('"', istr);
    }

    assert_cast<ColumnVector<T> &>(column).getData().push_back(x);
}

template <typename T>
void SerializationNumber<T>::deserializeTextCSV(IColumn & column, ReadBuffer & istr, const FormatSettings &) const
{
    FieldType x;
    readCSV(x, istr);
    assert_cast<ColumnVector<T> &>(column).getData().push_back(x);
}

template <typename T>
void SerializationNumber<T>::serializeBinary(const Field & field, WriteBuffer & ostr) const
{
    /// ColumnVector<T>::ValueType is a narrower type. For example, UInt8, when the Field type is UInt64
    typename ColumnVector<T>::ValueType x = get<FieldType>(field);
    writeBinary(x, ostr);
}

template <typename T>
void SerializationNumber<T>::deserializeBinary(Field & field, ReadBuffer & istr) const
{
    typename ColumnVector<T>::ValueType x;
    readBinary(x, istr);
    field = NearestFieldType<FieldType>(x);
}

template <typename T>
void SerializationNumber<T>::serializeBinary(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
{
    writeBinary(assert_cast<const ColumnVector<T> &>(column).getData()[row_num], ostr);
}

template <typename T>
void SerializationNumber<T>::deserializeBinary(IColumn & column, ReadBuffer & istr) const
{
    typename ColumnVector<T>::ValueType x;
    readBinary(x, istr);
    assert_cast<ColumnVector<T> &>(column).getData().push_back(x);
}

template <typename T>
void SerializationNumber<T>::serializeBinaryBulk(const IColumn & column, WriteBuffer & ostr, size_t offset, size_t limit) const
{
    const typename ColumnVector<T>::Container & x = typeid_cast<const ColumnVector<T> &>(column).getData();

    size_t size = x.size();

    if (limit == 0 || offset + limit > size)
        limit = size - offset;

    if (limit)
        ostr.write(reinterpret_cast<const char *>(&x[offset]), sizeof(typename ColumnVector<T>::ValueType) * limit);
}

template <typename T>
void SerializationNumber<T>::deserializeBinaryBulk(IColumn & column, ReadBuffer & istr, size_t limit, double /*avg_value_size_hint*/) const
{
    typename ColumnVector<T>::Container & x = typeid_cast<ColumnVector<T> &>(column).getData();
    size_t initial_size = x.size();
    x.resize(initial_size + limit);
    size_t size = istr.readBig(reinterpret_cast<char*>(&x[initial_size]), sizeof(typename ColumnVector<T>::ValueType) * limit);
    x.resize(initial_size + size / sizeof(typename ColumnVector<T>::ValueType));
}


template <class T, bool condition>
struct MemoryCompareWrapper;

template <class T>
struct MemoryCompareWrapper<T, true>
{
    using FieldType = T;

    bool supportMemComparableEncoding() const { return true; }
    void serializeMemComparable(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
    {
        const auto & value = assert_cast<const ColumnVector<T> &>(column).getData()[row_num];
        using UnsignedType = typename std::make_unsigned<T>::type;
        auto unsigned_value = static_cast<UnsignedType>(value);
        /// flip sign bit for signed type
        if constexpr (std::is_signed_v<T>)
            unsigned_value ^= (1ull << (sizeof(T) * 8 - 1));
        /// write in big-endian order
        unsigned_value = Endian::big(unsigned_value);
        writeBinary(unsigned_value, ostr);
    }

    void deserializeMemComparable(IColumn & column, ReadBuffer & istr) const
    {
        using UnsignedType = typename std::make_unsigned<T>::type;
        UnsignedType unsigned_value;
        /// read a big endian value and convert to host endian
        readBinary(unsigned_value, istr);
        unsigned_value = Endian::big(unsigned_value);
        /// flip sign bit for signed type
        if constexpr (std::is_signed_v<T>)
            unsigned_value ^= (1ull << (sizeof(T) * 8 - 1));
        assert_cast<ColumnVector<T> &>(column).getData().push_back(FieldType(unsigned_value));
    }
};

template <class T>
struct MemoryCompareWrapper<T, false>
{
    bool supportMemComparableEncoding() const { return false; }
    void serializeMemComparable(const IColumn & /*column*/, size_t /*row_num*/, WriteBuffer & /*ostr*/) const
    {
        throw Exception("serializeMemComparable is not supported.", ErrorCodes::LOGICAL_ERROR);
    }
    void deserializeMemComparable(IColumn & /*column*/, ReadBuffer & /*istr*/) const
    {
        throw Exception("DerializeMemComparable is not supported.", ErrorCodes::LOGICAL_ERROR);
    }
};

template <typename T>
bool SerializationNumber<T>::supportMemComparableEncoding() const
{
    MemoryCompareWrapper<T, IsNumberMemComparable<T>> wrapper;
    return wrapper.supportMemComparableEncoding();
}

template <typename T>
void SerializationNumber<T>::serializeMemComparable(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
{
    MemoryCompareWrapper<T, IsNumberMemComparable<T>> wrapper;
    return wrapper.serializeMemComparable(column, row_num, ostr);
}

template <typename T>
void SerializationNumber<T>::deserializeMemComparable(IColumn & column, ReadBuffer & istr) const
{
    MemoryCompareWrapper<T, IsNumberMemComparable<T>> wrapper;
    return wrapper.deserializeMemComparable(column, istr);
}

template class SerializationNumber<UInt8>;
template class SerializationNumber<UInt16>;
template class SerializationNumber<UInt32>;
template class SerializationNumber<UInt64>;
template class SerializationNumber<UInt128>;
template class SerializationNumber<UInt256>;
template class SerializationNumber<Int8>;
template class SerializationNumber<Int16>;
template class SerializationNumber<Int32>;
template class SerializationNumber<Int64>;
template class SerializationNumber<Int128>;
template class SerializationNumber<Int256>;
template class SerializationNumber<Float32>;
template class SerializationNumber<Float64>;

}
