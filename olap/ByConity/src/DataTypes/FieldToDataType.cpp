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

#include <DataTypes/FieldToDataType.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeMap.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypesDecimal.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeNothing.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypeBitMap64.h>
#include <DataTypes/getLeastSupertype.h>
#include <DataTypes/DataTypeFactory.h>
#include <Common/Exception.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int EMPTY_DATA_PASSED;
    extern const int LOGICAL_ERROR;
}


DataTypePtr FieldToDataType::operator() (const Null &) const
{
    return std::make_shared<DataTypeNullable>(std::make_shared<DataTypeNothing>());
}

DataTypePtr FieldToDataType::operator() (const NegativeInfinity &) const
{
    throw Exception("It's invalid to have -inf literals in SQL", ErrorCodes::LOGICAL_ERROR);
}

DataTypePtr FieldToDataType::operator() (const PositiveInfinity &) const
{
    throw Exception("It's invalid to have +inf literals in SQL", ErrorCodes::LOGICAL_ERROR);
}

DataTypePtr FieldToDataType::operator() (const UInt64 & x) const
{
    if (x <= std::numeric_limits<UInt8>::max()) return std::make_shared<DataTypeUInt8>();
    if (x <= std::numeric_limits<UInt16>::max()) return std::make_shared<DataTypeUInt16>();
    if (x <= std::numeric_limits<UInt32>::max()) return std::make_shared<DataTypeUInt32>();
    return std::make_shared<DataTypeUInt64>();
}

DataTypePtr FieldToDataType::operator() (const Int64 & x) const
{
    if (x <= std::numeric_limits<Int8>::max() && x >= std::numeric_limits<Int8>::min()) return std::make_shared<DataTypeInt8>();
    if (x <= std::numeric_limits<Int16>::max() && x >= std::numeric_limits<Int16>::min()) return std::make_shared<DataTypeInt16>();
    if (x <= std::numeric_limits<Int32>::max() && x >= std::numeric_limits<Int32>::min()) return std::make_shared<DataTypeInt32>();
    return std::make_shared<DataTypeInt64>();
}

DataTypePtr FieldToDataType::operator() (const Float64 &) const
{
    return std::make_shared<DataTypeFloat64>();
}

DataTypePtr FieldToDataType::operator() (const UInt128 &) const
{
    return std::make_shared<DataTypeUInt128>();
}

DataTypePtr FieldToDataType::operator() (const Int128 &) const
{
    return std::make_shared<DataTypeInt128>();
}

DataTypePtr FieldToDataType::operator() (const UInt256 &) const
{
    return std::make_shared<DataTypeUInt256>();
}

DataTypePtr FieldToDataType::operator() (const Int256 &) const
{
    return std::make_shared<DataTypeInt256>();
}

DataTypePtr FieldToDataType::operator() (const UUID &) const
{
    return std::make_shared<DataTypeUUID>();
}

DataTypePtr FieldToDataType::operator() (const String &) const
{
    return std::make_shared<DataTypeString>();
}

DataTypePtr FieldToDataType::operator() (const DecimalField<Decimal32> & x) const
{
    using Type = DataTypeDecimal<Decimal32>;
    return std::make_shared<Type>(Type::maxPrecision(), x.getScale());
}

DataTypePtr FieldToDataType::operator() (const DecimalField<Decimal64> & x) const
{
    using Type = DataTypeDecimal<Decimal64>;
    return std::make_shared<Type>(Type::maxPrecision(), x.getScale());
}

DataTypePtr FieldToDataType::operator() (const DecimalField<Decimal128> & x) const
{
    using Type = DataTypeDecimal<Decimal128>;
    return std::make_shared<Type>(Type::maxPrecision(), x.getScale());
}

DataTypePtr FieldToDataType::operator() (const DecimalField<Decimal256> & x) const
{
    using Type = DataTypeDecimal<Decimal256>;
    return std::make_shared<Type>(Type::maxPrecision(), x.getScale());
}

DataTypePtr FieldToDataType::operator() (const Array & x) const
{
    DataTypes element_types;
    element_types.reserve(x.size());

    for (const Field & elem : x)
        element_types.emplace_back(applyVisitor(FieldToDataType(), elem));

    return std::make_shared<DataTypeArray>(getLeastSupertype(element_types));
}


DataTypePtr FieldToDataType::operator() (const Tuple & tuple) const
{
    if (tuple.empty())
        throw Exception("Cannot infer type of an empty tuple", ErrorCodes::EMPTY_DATA_PASSED);

    DataTypes element_types;
    element_types.reserve(tuple.size());

    for (const auto & element : tuple)
        element_types.push_back(applyVisitor(FieldToDataType(), element));

    return std::make_shared<DataTypeTuple>(element_types);
}

DataTypePtr FieldToDataType::operator() (const Map & map) const
{
    DataTypes key_types;
    DataTypes value_types;
    key_types.reserve(map.size());
    value_types.reserve(map.size());

    for (const auto & elem : map)
    {
        const auto & tuple = elem.safeGet<const Tuple &>();
        assert(tuple.size() == 2);
        key_types.push_back(applyVisitor(FieldToDataType(), tuple[0]));
        value_types.push_back(applyVisitor(FieldToDataType(), tuple[1]));
    }

    return std::make_shared<DataTypeMap>(getLeastSupertype(key_types), getLeastSupertype(value_types));
}

DataTypePtr FieldToDataType::operator() (const ByteMap & map) const
{
    DataTypePtr keyType, valueType;

    if (map.empty())
    {
        keyType = std::make_shared<DataTypeNothing>();
        valueType = std::make_shared<DataTypeNothing>();
    }
    else
    {
        keyType = applyVisitor(FieldToDataType(), map[0].first);
        valueType = applyVisitor(FieldToDataType(), map[0].second);
    }

    return std::make_shared<DataTypeByteMap>(keyType, valueType);
}

DataTypePtr FieldToDataType::operator() (const AggregateFunctionStateData & x) const
{
    const auto & name = static_cast<const AggregateFunctionStateData &>(x).name;
    return DataTypeFactory::instance().get(name);
}

DataTypePtr FieldToDataType::operator() (const BitMap64 &) const
{
    return std::make_shared<DataTypeBitMap64>();
}

}
