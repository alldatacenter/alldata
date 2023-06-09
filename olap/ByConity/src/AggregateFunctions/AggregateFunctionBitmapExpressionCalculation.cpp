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

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/AggregateFunctionBitmapExpressionCalculation.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>

#pragma  GCC diagnostic ignored  "-Wunused"
#pragma GCC diagnostic ignored "-Wunused-parameter"

namespace DB
{

struct Settings;

namespace
{

template <template <typename, typename> class AggregateFunctionTemplate, typename... TArgs>
static IAggregateFunction * createWithSpecificType(const IDataType & argument_type, TArgs &&... args)
{
    WhichDataType which(argument_type);
    if (which.idx == TypeIndex::UInt8)  return new AggregateFunctionTemplate<uint8_t, uint8_t>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt16) return new AggregateFunctionTemplate<UInt16, UInt16>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt32) return new AggregateFunctionTemplate<UInt32, UInt32>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::UInt64) return new AggregateFunctionTemplate<UInt64, UInt64>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Int8)   return new AggregateFunctionTemplate<Int8, Int8>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Int16)  return new AggregateFunctionTemplate<Int16, Int16>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Int32)  return new AggregateFunctionTemplate<Int32, Int32>(std::forward<TArgs>(args)...);
    if (which.idx == TypeIndex::Int64)  return new AggregateFunctionTemplate<Int64, Int64>(std::forward<TArgs>(args)...);
    return nullptr;
}

template<template <typename, typename> class Function>
AggregateFunctionPtr createAggregateFunctionBitMapCount(const String & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    if (argument_types.size() != 2 )
        throw Exception("AggregateFunction " + name + " need two arguments", ErrorCodes::NOT_IMPLEMENTED);

    String expression;
    if (parameters.size() > 0)
        parameters[0].tryGet<String>(expression);

    UInt64 is_bitmap_execute = 0;
    if (parameters.size() > 1)
        parameters[1].tryGet<UInt64>(is_bitmap_execute);

    DataTypePtr data_type = argument_types[0];
    if (!WhichDataType(data_type).isInt())
        throw Exception("AggregateFunction " + name + " need signed numeric type (Int16 or bigger) for its first argument", ErrorCodes::NOT_IMPLEMENTED);
    if (WhichDataType(data_type).isInt8())
        throw Exception("Int8 type is not recommended! Please use Int16 or bigger size number", ErrorCodes::BAD_TYPE_OF_FIELD);

    if (!isBitmap64(argument_types[1]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    AggregateFunctionPtr res(createWithSpecificType<Function>(*data_type, argument_types, expression, is_bitmap_execute));

    // res.reset(createWithNumericType<Function>(*data_type, argument_types, expression, is_bitmap_execute));
    if (!res)
        throw Exception("Failed to create aggregate function  " + name, ErrorCodes::LOGICAL_ERROR);

    return res;
}

template<template <typename, typename> class Function>
AggregateFunctionPtr createAggregateFunctionBitMapMultiCount(const String & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    if (argument_types.size() != 2 )
        throw Exception("AggregateFunction " + name + " need two arguments", ErrorCodes::NOT_IMPLEMENTED);

    std::vector<String> expressions;
    if (parameters.size() > 0)
    {
        for (size_t i = 0; i < parameters.size(); i++)
        {
            String expression;
            parameters[i].tryGet<String>(expression);
            expressions.push_back(expression);
        }
    }

    DataTypePtr data_type = argument_types[0];
    if (!WhichDataType(data_type).isInt())
        throw Exception("AggregateFunction " + name + " need signed numeric type (Int16 or bigger) for its first argument", ErrorCodes::NOT_IMPLEMENTED);
    if (WhichDataType(data_type).isInt8())
        throw Exception("Int8 type is not recommended! Please use Int16 or bigger size number", ErrorCodes::BAD_TYPE_OF_FIELD);

    if (!isBitmap64(argument_types[1]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    AggregateFunctionPtr res;

    res.reset(createWithSpecificType<Function>(*data_type, argument_types, expressions));

    return res;
}

AggregateFunctionPtr createAggregateFunctionBitMapMultiCountWithDate(const String & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    if (argument_types.size() != 3 )
        throw Exception("AggregateFunction " + name + " need three arguments", ErrorCodes::NOT_IMPLEMENTED);

    std::vector<String> expressions;
    if (parameters.size() > 0)
    {
        for (size_t i = 0; i < parameters.size(); i++)
        {
            String expression;
            parameters[i].tryGet<String>(expression);
            expressions.push_back(expression);
        }
    }

    DataTypePtr date_type = argument_types[0];
    if (!WhichDataType(date_type).isInt())
        throw Exception("AggregateFunction " + name + " need signed numeric type (Int16 or bigger) for its first argument", ErrorCodes::NOT_IMPLEMENTED);

    DataTypePtr data_type = argument_types[1];
    if (!WhichDataType(data_type).isInt())
        throw Exception("AggregateFunction " + name + " need signed numeric type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    if (!isBitmap64(argument_types[2]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its third argument", ErrorCodes::NOT_IMPLEMENTED);

    return std::make_shared<AggregateFunctionBitMapMultiCountWithDate>(argument_types, expressions);
}

template<template <typename, typename> class Function>
AggregateFunctionPtr createAggregateFunctionBitMapExtract(const String & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    if (argument_types.size() != 2 )
        throw Exception("AggregateFunction " + name + " need two arguments", ErrorCodes::NOT_IMPLEMENTED);

    String expression;
    if (parameters.size() > 0)
        parameters[0].tryGet<String>(expression);

    UInt64 is_bitmap_execute = 0;
    if (parameters.size() > 1)
        parameters[1].tryGet<UInt64>(is_bitmap_execute);

    DataTypePtr data_type = argument_types[0];
    if (!WhichDataType(data_type).isInt())
        throw Exception("AggregateFunction " + name + " need signed numeric type (Int16 or bigger) for its first argument", ErrorCodes::NOT_IMPLEMENTED);
    if (WhichDataType(data_type).isInt8())
        throw Exception("Int8 type is not recommended! Please use Int16 or bigger size number", ErrorCodes::BAD_TYPE_OF_FIELD);

    if (!isBitmap64(argument_types[1]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    AggregateFunctionPtr res;

    res.reset(createWithSpecificType<Function>(*data_type, argument_types, expression, is_bitmap_execute));

    return res;
}

}

void registerAggregateFunctionsBitmapExpressionCalculation(AggregateFunctionFactory & factory)
{
    factory.registerFunction("BitMapCount", createAggregateFunctionBitMapCount<AggregateFunctionBitMapCount>, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapMultiCount", createAggregateFunctionBitMapMultiCount<AggregateFunctionBitMapMultiCount>, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapMultiCountWithDate", createAggregateFunctionBitMapMultiCountWithDate, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapExtract", createAggregateFunctionBitMapExtract<AggregateFunctionBitMapExtract>, AggregateFunctionFactory::CaseInsensitive);
}

}
