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

#include <DataTypes/Serializations/SerializationDecimalBase.h>

#include <Columns/ColumnVector.h>
#include <Core/Types.h>
#include <Formats/ProtobufReader.h>
#include <Formats/ProtobufWriter.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Common/Endian.h>
#include <Common/assert_cast.h>
#include <Common/typeid_cast.h>


namespace DB
{

template <typename T>
void SerializationDecimalBase<T>::serializeBinary(const Field & field, WriteBuffer & ostr) const
{
    FieldType x = get<DecimalField<T>>(field);
    writeBinary(x, ostr);
}

template <typename T>
void SerializationDecimalBase<T>::serializeBinary(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
{
    const FieldType & x = assert_cast<const ColumnType &>(column).getElement(row_num);
    writeBinary(x, ostr);
}

template <typename T>
void SerializationDecimalBase<T>::serializeBinaryBulk(const IColumn & column, WriteBuffer & ostr, size_t offset, size_t limit) const
{
    const typename ColumnType::Container & x = typeid_cast<const ColumnType &>(column).getData();

    size_t size = x.size();

    if (limit == 0 || offset + limit > size)
        limit = size - offset;

    ostr.write(reinterpret_cast<const char *>(&x[offset]), sizeof(FieldType) * limit);
}

template <typename T>
void SerializationDecimalBase<T>::deserializeBinary(Field & field, ReadBuffer & istr) const
{
    typename FieldType::NativeType x;
    readBinary(x, istr);
    field = DecimalField(T(x), this->scale);
}

template <typename T>
void SerializationDecimalBase<T>::deserializeBinary(IColumn & column, ReadBuffer & istr) const
{
    typename FieldType::NativeType x;
    readBinary(x, istr);
    assert_cast<ColumnType &>(column).getData().push_back(FieldType(x));
}

template <typename T>
void SerializationDecimalBase<T>::deserializeBinaryBulk(IColumn & column, ReadBuffer & istr, size_t limit, double) const
{
    typename ColumnType::Container & x = typeid_cast<ColumnType &>(column).getData();
    size_t initial_size = x.size();
    x.resize(initial_size + limit);
    size_t size = istr.readBig(reinterpret_cast<char*>(&x[initial_size]), sizeof(FieldType) * limit);
    x.resize(initial_size + size / sizeof(FieldType));
}

template <class T, bool condition>
struct MemoryCompareWrapper;

template <class T>
struct MemoryCompareWrapper<T, true>
{
    using FieldType = T;
    using ColumnType = ColumnDecimal<T>;

    bool supportMemComparableEncoding() const { return true; }
    void serializeMemComparable(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
    {
        const FieldType & value = assert_cast<const ColumnType &>(column).getElement(row_num);
        /// NOTE: Here T is a template looks like: Decimal<Int32>, we first get the real type
        using real_type = typename NativeType<T>::Type;
        using UnsignedType = typename std::make_unsigned<real_type>::type;
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
        /// NOTE: Here T is a template looks like: Decimal<Int32>, we first get the real type
        using real_type = typename NativeType<T>::Type;
        using UnsignedType = typename std::make_unsigned<real_type>::type;
        UnsignedType unsigned_value;
        /// read a big endian value and convert to host endian
        readBinary(unsigned_value, istr);
        unsigned_value = Endian::big(unsigned_value);
        /// flip sign bit for signed type
        if constexpr (std::is_signed_v<T>)
            unsigned_value ^= (1ull << (sizeof(T) * 8 - 1));
        assert_cast<ColumnType &>(column).getData().push_back(FieldType(unsigned_value));
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
bool SerializationDecimalBase<T>::supportMemComparableEncoding() const
{
    MemoryCompareWrapper<T, IsNumberMemComparable<T>> wrapper;
    return wrapper.supportMemComparableEncoding();
}

template <typename T>
void SerializationDecimalBase<T>::serializeMemComparable(const IColumn & column, size_t row_num, WriteBuffer & ostr) const
{
    MemoryCompareWrapper<T, IsNumberMemComparable<T>> wrapper;
    return wrapper.serializeMemComparable(column, row_num, ostr);
}

template <typename T>
void SerializationDecimalBase<T>::deserializeMemComparable(IColumn & column, ReadBuffer & istr) const
{
    MemoryCompareWrapper<T, IsNumberMemComparable<T>> wrapper;
    return wrapper.deserializeMemComparable(column, istr);
}

template class SerializationDecimalBase<Decimal32>;
template class SerializationDecimalBase<Decimal64>;
template class SerializationDecimalBase<Decimal128>;
template class SerializationDecimalBase<Decimal256>;
template class SerializationDecimalBase<DateTime64>;

}
