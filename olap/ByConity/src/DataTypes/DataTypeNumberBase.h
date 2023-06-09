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

#pragma once

#include <DataTypes/IDataType.h>
#include <DataTypes/Serializations/SerializationNumber.h>


namespace DB
{

template <typename T>
class ColumnVector;

/** Implements part of the IDataType interface, common to all numbers and for Date and DateTime.
  */
template <typename T>
class DataTypeNumberBase : public IDataType
{
    static_assert(is_arithmetic_v<T>);

public:
    static constexpr bool is_parametric = false;
    static constexpr auto family_name = TypeName<T>;

    using FieldType = T;
    using ColumnType = ColumnVector<T>;

    const char * getFamilyName() const override { return TypeName<T>; }
    TypeIndex getTypeId() const override { return TypeId<T>; }

    Field getDefault() const override;

    MutableColumnPtr createColumn() const override;

    bool isParametric() const override { return false; }
    bool haveSubtypes() const override { return false; }

    bool shouldAlignRightInPrettyFormats() const override
    {
        /// Just a number, without customizations. Counterexample: IPv4.
        return !custom_serialization;
    }

    bool textCanContainOnlyValidUTF8() const override { return true; }
    bool isComparable() const override { return true; }
    bool isValueRepresentedByNumber() const override { return true; }
    bool isValueRepresentedByInteger() const override;
    bool isValueRepresentedByUnsignedInteger() const override;
    bool isValueUnambiguouslyRepresentedInContiguousMemoryRegion() const override { return true; }
    bool haveMaximumSizeOfValue() const override { return true; }
    size_t getSizeOfValueInMemory() const override { return sizeof(T); }
    bool isCategorial() const override { return isValueRepresentedByInteger(); }
    bool canBeInsideLowCardinality() const override { return true; }
    Field stringToVisitorField(const String & ins) const override;
    String stringToVisitorString(const String & ins) const override;

    SerializationPtr doGetDefaultSerialization() const override { return std::make_shared<SerializationNumber<T>>(); }

    bool canBeMapKeyType() const override { return true; }
    bool canBeMapValueType() const override { return true; }
};

/// Prevent implicit template instantiation of DataTypeNumberBase for common numeric types

extern template class DataTypeNumberBase<UInt8>;
extern template class DataTypeNumberBase<UInt16>;
extern template class DataTypeNumberBase<UInt32>;
extern template class DataTypeNumberBase<UInt64>;
extern template class DataTypeNumberBase<UInt128>;
extern template class DataTypeNumberBase<UInt256>;
extern template class DataTypeNumberBase<Int16>;
extern template class DataTypeNumberBase<Int8>;
extern template class DataTypeNumberBase<Int32>;
extern template class DataTypeNumberBase<Int64>;
extern template class DataTypeNumberBase<Int128>;
extern template class DataTypeNumberBase<Int256>;
extern template class DataTypeNumberBase<Float32>;
extern template class DataTypeNumberBase<Float64>;

}
