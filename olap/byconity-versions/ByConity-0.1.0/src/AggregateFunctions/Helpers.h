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
#include <AggregateFunctions/IAggregateFunction.h>

#define FOR_BASIC_NUMERIC_TYPES(M) \
    M(UInt8) \
    M(UInt16) \
    M(UInt32) \
    M(UInt64) \
    M(Int8) \
    M(Int16) \
    M(Int32) \
    M(Int64) \
    M(Float32) \
    M(Float64)

#define FOR_NUMERIC_TYPES(M) \
    M(UInt8) \
    M(UInt16) \
    M(UInt32) \
    M(UInt64) \
    M(UInt128) \
    M(UInt256) \
    M(Int8) \
    M(Int16) \
    M(Int32) \
    M(Int64) \
    M(Int128) \
    M(Int256) \
    M(Float32) \
    M(Float64)

// added for bytedance functions
#define FOR_INTEGER_TYPES_DATA(M) \
    M(UInt8) \
    M(UInt16) \
    M(UInt32) \
    M(UInt64) \
    M(Int8) \
    M(Int16) \
    M(Int32) \
    M(Int64)

namespace DB
{
struct Settings;

/** Create an aggregate function with a numeric type in the template parameter, depending on the type of the argument.
  */
template <template <typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithNumericType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<TYPE>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<Int8>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<Int16>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename> class AggregateFunctionTemplate, template <typename> class Data, typename... TArgs>
static IAggregateFunction * createWithNumericType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<Data<TYPE>>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<Data<Int8>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<Data<Int16>>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, bool> class AggregateFunctionTemplate, bool bool_param, typename... TArgs>
static IAggregateFunction * createWithNumericType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<TYPE, bool_param>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<Int8, bool_param>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<Int16, bool_param>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, typename Data, typename... TArgs>
static IAggregateFunction * createWithNumericType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<TYPE, Data>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<Int8, Data>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<Int16, Data>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, template <typename> class Data, typename... TArgs>
static IAggregateFunction * createWithNumericType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<TYPE, Data<TYPE>>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<Int8, Data<Int8>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<Int16, Data<Int16>>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, template <typename> class Data, typename... TArgs>
static IAggregateFunction * createWithUnsignedIntegerType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
    if (which.idx == TypeIndex::UInt8) return new AggregateFunctionTemplate<UInt8, Data<UInt8>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt16) return new AggregateFunctionTemplate<UInt16, Data<UInt16>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt32) return new AggregateFunctionTemplate<UInt32, Data<UInt32>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt64) return new AggregateFunctionTemplate<UInt64, Data<UInt64>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt128) return new AggregateFunctionTemplate<UInt128, Data<UInt128>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt256) return new AggregateFunctionTemplate<UInt256, Data<UInt256>>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, template <typename> class Data, typename... TArgs>
static IAggregateFunction * createWithBasicNumberOrDateOrDateTime(const IDataType & argument_type, TArgs &&... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) \
        return new AggregateFunctionTemplate<TYPE, Data<TYPE>>(std::forward<TArgs>(args)...);
    FOR_BASIC_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH

    if (which.idx == TypeIndex::Date)
        return new AggregateFunctionTemplate<UInt16, Data<UInt16>>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::DateTime)
        return new AggregateFunctionTemplate<UInt32, Data<UInt32>>(std::forward<TArgs>(args)...);

    return nullptr;
}

template <template <typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithNumericBasedType(const IDataType & argument_type, TArgs && ... args)
{
    IAggregateFunction * f = createWithNumericType<AggregateFunctionTemplate>(argument_type, std::forward<TArgs>(args)...);
    if (f)
        return f;

    /// expects that DataTypeDate based on UInt16, DataTypeDateTime based on UInt32
    WhichDataType which(argument_type);
    if (which.idx == TypeIndex::Date) return new AggregateFunctionTemplate<UInt16>(std::forward<TArgs>(args)...);
    // if (which.idx == TypeIndex::Date32) return new AggregateFunctionTemplate<UInt32>(std::forward<TArgs>(args)...); // TODO: date32
    if (which.idx == TypeIndex::DateTime) return new AggregateFunctionTemplate<UInt32>(std::forward<TArgs>(args)...);
    // DateTime64 is decimal, so not here
    if (which.idx == TypeIndex::UUID) return new AggregateFunctionTemplate<UUID>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithDecimalType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
    if (which.idx == TypeIndex::Decimal32) return new AggregateFunctionTemplate<Decimal32>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Decimal64) return new AggregateFunctionTemplate<Decimal64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Decimal128) return new AggregateFunctionTemplate<Decimal128>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Decimal256) return new AggregateFunctionTemplate<Decimal256>(std::forward<TArgs>(args)...);
    if constexpr (AggregateFunctionTemplate<DateTime64>::DateTime64Supported)
        if (which.idx == TypeIndex::DateTime64) return new AggregateFunctionTemplate<DateTime64>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, typename Data, typename... TArgs>
static IAggregateFunction * createWithDecimalType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
    if (which.idx == TypeIndex::Decimal32) return new AggregateFunctionTemplate<Decimal32, Data>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Decimal64) return new AggregateFunctionTemplate<Decimal64, Data>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Decimal128) return new AggregateFunctionTemplate<Decimal128, Data>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Decimal256) return new AggregateFunctionTemplate<Decimal256, Data>(std::forward<TArgs>(args)...);
    if constexpr (AggregateFunctionTemplate<DateTime64, Data>::DateTime64Supported)
        if (which.idx == TypeIndex::DateTime64) return new AggregateFunctionTemplate<DateTime64, Data>(std::forward<TArgs>(args)...);
    return nullptr;
}

/** For template with two arguments.
  */
template <typename FirstType, template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoNumericTypesSecond(const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(second_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<FirstType, TYPE>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<FirstType, Int8>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<FirstType, Int16>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoNumericTypes(const IDataType & first_type, const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(first_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) \
        return createWithTwoNumericTypesSecond<TYPE, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8)
        return createWithTwoNumericTypesSecond<Int8, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16)
        return createWithTwoNumericTypesSecond<Int16, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    return nullptr;
}

template <typename FirstType, template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoBasicNumericTypesSecond(const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(second_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<FirstType, TYPE>(std::forward<TArgs>(args)...);
    FOR_BASIC_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoBasicNumericTypes(const IDataType & first_type, const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(first_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) \
        return createWithTwoBasicNumericTypesSecond<TYPE, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    FOR_BASIC_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    return nullptr;
}

template <typename FirstType, template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoNumericOrDateTypesSecond(const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(second_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<FirstType, TYPE>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<FirstType, Int8>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<FirstType, Int16>(std::forward<TArgs>(args)...);

    /// expects that DataTypeDate based on UInt16, DataTypeDateTime based on UInt32
    if (which.idx == TypeIndex::Date) return new AggregateFunctionTemplate<FirstType, UInt16>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::DateTime) return new AggregateFunctionTemplate<FirstType, UInt32>(std::forward<TArgs>(args)...);

    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoNumericOrDateTypes(const IDataType & first_type, const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(first_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) \
        return createWithTwoNumericOrDateTypesSecond<TYPE, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8)
        return createWithTwoNumericOrDateTypesSecond<Int8, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16)
        return createWithTwoNumericOrDateTypesSecond<Int16, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);

    /// expects that DataTypeDate based on UInt16, DataTypeDateTime based on UInt32
    if (which.idx == TypeIndex::Date)
        return createWithTwoNumericOrDateTypesSecond<UInt16, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::DateTime)
        return createWithTwoNumericOrDateTypesSecond<UInt32, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithStringType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
    if (which.idx == TypeIndex::String) return new AggregateFunctionTemplate<String>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::FixedString) return new AggregateFunctionTemplate<String>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, typename KnownType, typename... TArgs>
static IAggregateFunction * createWithIntegerTypeLastKnown(const IDataType & argument_type, TArgs &&... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<TYPE, KnownType>(std::forward<TArgs>(args)...);
    FOR_INTEGER_TYPES_DATA(DISPATCH)
#undef DISPATCH
    return nullptr;
}

template <typename FirstType, template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoTypesSecond(const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(second_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) return new AggregateFunctionTemplate<FirstType, TYPE>(std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8) return new AggregateFunctionTemplate<FirstType, Int8>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16) return new AggregateFunctionTemplate<FirstType, Int16>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::String) return new AggregateFunctionTemplate<FirstType, String>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTwoTypes(const IDataType & first_type, const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(first_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) \
        return createWithTwoTypesSecond<TYPE, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    FOR_NUMERIC_TYPES(DISPATCH)
#undef DISPATCH
    if (which.idx == TypeIndex::Enum8)
        return createWithTwoTypesSecond<Int8, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Enum16)
        return createWithTwoTypesSecond<UInt16, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename> class AggregateFunctionTemplate, typename ... TArgs>
static IAggregateFunction * createWithIntegerType(const IDataType & argument_type, TArgs && ... args)
{
    WhichDataType which(argument_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) \
        return new AggregateFunctionTemplate<TYPE>(std::forward<TArgs>(args)...);
    FOR_INTEGER_TYPES_DATA(DISPATCH)
#undef DISPATCH
    return nullptr;
}

template <template <typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithSingleTypeLastKnown(const IDataType & attr_type, TArgs && ... args)
{
    WhichDataType which(attr_type);
    if (which.idx == TypeIndex::UInt8) return new AggregateFunctionTemplate<UInt8>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt32) return new AggregateFunctionTemplate<UInt32>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Int64) return new AggregateFunctionTemplate<Int64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt64) return new AggregateFunctionTemplate<UInt64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Float64) return new AggregateFunctionTemplate<Float64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::String) return new AggregateFunctionTemplate<String>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <typename FirstType, template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTypesSecondType(const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(second_type);
    if (which.idx == TypeIndex::UInt8) return new AggregateFunctionTemplate<FirstType, UInt8>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt32) return new AggregateFunctionTemplate<FirstType, UInt32>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Int64) return new AggregateFunctionTemplate<FirstType, Int64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt64) return new AggregateFunctionTemplate<FirstType, UInt64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Float64) return new AggregateFunctionTemplate<FirstType, Float64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::String) return new AggregateFunctionTemplate<FirstType, String>(std::forward<TArgs>(args)...);
    return nullptr;
}

template <template <typename, typename > class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithTypesAndIntegerType(const IDataType & first_type, const IDataType & second_type, TArgs && ... args)
{
    WhichDataType which(first_type);
#define DISPATCH(TYPE) \
    if (which.idx == TypeIndex::TYPE) \
        return createWithTypesSecondType<TYPE, AggregateFunctionTemplate>(second_type, std::forward<TArgs>(args)...);
    FOR_INTEGER_TYPES_DATA(DISPATCH)
#undef DISPATCH
    return nullptr;
}
}
