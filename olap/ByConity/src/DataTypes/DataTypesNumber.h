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

#include <type_traits>
#include <Core/Field.h>
#include <DataTypes/DataTypeNumberBase.h>
#include <DataTypes/Serializations/SerializationNumber.h>


namespace DB
{

template <typename T> inline std::optional<IDataType::Range> getRangeForNumeric()
{
    return IDataType::Range{std::numeric_limits<T>::min(), std::numeric_limits<T>::max()};
}

// Float32 doesn't have a well-defined range, because of Inf, -Inf, NaN.
template <> inline std::optional<IDataType::Range> getRangeForNumeric<Float32>()
{
    return std::nullopt;
}

// Float64 doesn't have a well-defined range, because of Inf, -Inf, NaN.
template <> inline std::optional<IDataType::Range> getRangeForNumeric<Float64>()
{
    return std::nullopt;
}

template <typename T>
class DataTypeNumber final : public DataTypeNumberBase<T>
{
    bool equals(const IDataType & rhs) const override { return typeid(rhs) == typeid(*this); }

    bool canBeUsedAsVersion() const override { return true; }
    bool isSummable() const override { return true; }
    bool canBeUsedInBitOperations() const override { return true; }
    bool canBeUsedInBooleanContext() const override { return true; }
    bool canBeInsideNullable() const override { return true; }

    bool canBePromoted() const override { return true; }
    DataTypePtr promoteNumericType() const override
    {
        using PromotedType = DataTypeNumber<NearestFieldType<T>>;
        return std::make_shared<PromotedType>();
    }

    SerializationPtr doGetDefaultSerialization() const override
    {
        return std::make_shared<SerializationNumber<T>>();
    }

public:
    std::optional<IDataType::Range> getRange() const override
    {
        return getRangeForNumeric<T>();
    }
};

using DataTypeUInt8 = DataTypeNumber<UInt8>;
using DataTypeUInt16 = DataTypeNumber<UInt16>;
using DataTypeUInt32 = DataTypeNumber<UInt32>;
using DataTypeUInt64 = DataTypeNumber<UInt64>;
using DataTypeInt8 = DataTypeNumber<Int8>;
using DataTypeInt16 = DataTypeNumber<Int16>;
using DataTypeInt32 = DataTypeNumber<Int32>;
using DataTypeInt64 = DataTypeNumber<Int64>;
using DataTypeFloat32 = DataTypeNumber<Float32>;
using DataTypeFloat64 = DataTypeNumber<Float64>;

using DataTypeUInt128 = DataTypeNumber<UInt128>;
using DataTypeInt128 = DataTypeNumber<Int128>;
using DataTypeUInt256 = DataTypeNumber<UInt256>;
using DataTypeInt256 = DataTypeNumber<Int256>;

}
