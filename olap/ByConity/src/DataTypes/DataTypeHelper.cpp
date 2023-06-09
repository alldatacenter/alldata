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

#include <DataTypes/DataTypeHelper.h>
#include <DataTypes/DataTypeAggregateFunction.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeBitMap64.h>
#include <DataTypes/DataTypeCustom.h>
#include <DataTypes/DataTypeCustomGeo.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeTime.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeDateTime64.h>
#include <DataTypes/DataTypeDecimalBase.h>
#include <DataTypes/DataTypeEnum.h>
#include <DataTypes/DataTypeFactory.h>
#include <DataTypes/DataTypeFunction.h>
#include <DataTypes/DataTypeFixedString.h>
#include <DataTypes/DataTypeInterval.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypeMap.h>
#include <DataTypes/DataTypeNested.h>
#include <DataTypes/DataTypeNothing.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeNumberBase.h>
#include <DataTypes/DataTypesDecimal.h>
#include <DataTypes/DataTypeSet.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeUUID.h>

#include <QueryPlan/PlanSerDerHelper.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>

#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <Core/Field.h>


namespace DB
{

DataTypePtr createBaseDataTypeFromTypeIndex(TypeIndex index)
{
    switch (index)
    {
        case TypeIndex::Nothing:
            return std::make_shared<DataTypeNothing>();
        case TypeIndex::UInt8:
            return std::make_shared<DataTypeUInt8>();
        case TypeIndex::UInt16:
            return std::make_shared<DataTypeUInt16>();
        case TypeIndex::UInt32:
            return std::make_shared<DataTypeUInt32>();
        case TypeIndex::UInt64:
            return std::make_shared<DataTypeUInt64>();
        case TypeIndex::UInt128:
            return std::make_shared<DataTypeUInt128>();
        case TypeIndex::UInt256:
            return std::make_shared<DataTypeUInt256>();
        case TypeIndex::Int8:
            return std::make_shared<DataTypeInt8>();
        case TypeIndex::Int16:
            return std::make_shared<DataTypeInt16>();
        case TypeIndex::Int32:
            return std::make_shared<DataTypeInt32>();
        case TypeIndex::Int64:
            return std::make_shared<DataTypeInt64>();
        case TypeIndex::Int128:
            return std::make_shared<DataTypeInt128>();
        case TypeIndex::Int256:
            return std::make_shared<DataTypeInt256>();
        case TypeIndex::Float32:
            return std::make_shared<DataTypeFloat32>();
        case TypeIndex::Float64:
            return std::make_shared<DataTypeFloat64>();
        case TypeIndex::Date:
            return std::make_shared<DataTypeDate>();
        case TypeIndex::DateTime:
            return std::make_shared<DataTypeDateTime>();
        case TypeIndex::String:
            return std::make_shared<DataTypeString>();
        case TypeIndex::UUID:
            return std::make_shared<DataTypeUUID>();
        case TypeIndex::Set:
            return std::make_shared<DataTypeSet>();
        case TypeIndex::BitMap64:
            return std::make_shared<DataTypeBitMap64>();
        default:
            throw Exception("Cannot create data type in default", ErrorCodes::NOT_IMPLEMENTED);
    }
}

DataTypePtr deserializeDataTypeV1V1(ReadBuffer & buf)
{
    DESERIALIZE_ENUM(TypeIndex, index, buf)
    switch (index)
    {
        case TypeIndex::DateTime64: {
            UInt32 scale;
            String time_zone;
            readBinary(scale, buf);
            readBinary(time_zone, buf);
            return std::make_shared<DataTypeDateTime64>(scale, time_zone);
        }
        case TypeIndex::Time: {
            UInt32 scale;
            readBinary(scale, buf);
            return std::make_shared<DataTypeTime>(scale);
        }
        case TypeIndex::FixedString: {
            size_t n;
            readVarUInt(n, buf);
            return std::make_shared<DataTypeFixedString>(n);
        }
        case TypeIndex::Enum8: {
            size_t size;
            readVarUInt(size, buf);
            DataTypeEnum8::Values values;
            for (size_t i = 0; i < size; ++i)
            {
                String s;
                readStringBinary(s, buf);
                Int8 num;
                readBinary(num, buf);
                values.emplace_back(DataTypeEnum8::Value(s, num));
            }
            return std::make_shared<DataTypeEnum8>(values);
        }
        case TypeIndex::Enum16: {
            size_t size;
            readVarUInt(size, buf);
            DataTypeEnum16::Values values;
            for (size_t i = 0; i < size; ++i)
            {
                String s;
                readStringBinary(s, buf);
                Int16 num;
                readBinary(num, buf);
                values.emplace_back(DataTypeEnum16::Value(s, num));
            }
            return std::make_shared<DataTypeEnum16>(values);
        }
        case TypeIndex::Decimal32:
        case TypeIndex::Decimal64:
        case TypeIndex::Decimal128:
        case TypeIndex::Decimal256:
        {
            UInt32 precision;
            UInt32 scale;
            readVarUInt(precision, buf);
            readVarUInt(scale, buf);
            return createDecimal<DataTypeDecimal>(precision, scale);
        }
        case TypeIndex::Array: {
            auto nest_type = deserializeDataTypeV1V1(buf);
            return std::make_shared<DataTypeArray>(nest_type);
        }
        case TypeIndex::Tuple: {
            size_t size;
            readVarUInt(size, buf);
            DataTypes types;
            Strings names;
            for (size_t i = 0; i < size; ++i)
            {
                types.emplace_back(deserializeDataTypeV1V1(buf));
            }
            for (size_t i = 0; i < size; ++i)
            {
                String s;
                readStringBinary(s, buf);
                names.emplace_back(s);
            }

            if (isNumericASCII(names[0][0]))
            {
                return std::make_shared<DataTypeTuple>(types);
            }
            else
            {
                return std::make_shared<DataTypeTuple>(types, names);
            }
        }
        case TypeIndex::Interval: {
            DESERIALIZE_ENUM(IntervalKind::Kind, kind, buf)
            return std::make_shared<DataTypeInterval>(kind);
        }
        case TypeIndex::Nullable: {
            auto nest_type = deserializeDataTypeV1V1(buf);
            return std::make_shared<DataTypeNullable>(nest_type);
        }
        case TypeIndex::LowCardinality: {
            auto dict_type = deserializeDataTypeV1V1(buf);
            return std::make_shared<DataTypeLowCardinality>(dict_type);
        }
        case TypeIndex::Map: {
            auto key_type = deserializeDataTypeV1V1(buf);
            auto value_type = deserializeDataTypeV1V1(buf);
            return std::make_shared<DataTypeMap>(key_type, value_type);
        }
        case TypeIndex::Function: {
            size_t size;
            readVarUInt(size, buf);
            DataTypes args;
            for (size_t i = 0; i < size; ++i)
            {
                args.emplace_back(deserializeDataTypeV1V1(buf));
            }
            DataTypePtr result_type = deserializeDataTypeV1V1(buf);
            return std::make_shared<DataTypeFunction>(args, result_type);
        }
        case TypeIndex::AggregateFunction: {
            String name;
            readStringBinary(name, buf);

            size_t size;
            readVarUInt(size, buf);
            DataTypes type_args;
            for (size_t i = 0; i < size; ++i)
            {
                type_args.emplace_back(deserializeDataTypeV1V1(buf));
            }
            Array params;
            readVarUInt(size, buf);
            for (size_t i = 0; i < size; ++i)
            {
                Field field;
                readFieldBinary(field, buf);
                params.emplace_back(field);
            }
            auto & factory = AggregateFunctionFactory::instance();
            AggregateFunctionProperties property;
            auto func = factory.get(name, type_args, params, property);
            return std::make_shared<DataTypeAggregateFunction>(func, type_args, params);
        }
        default:
            return createBaseDataTypeFromTypeIndex(index);
    }
}

void serializeDataTypeV1(const DataTypePtr & data_type, WriteBuffer & buf)
{
    TypeIndex index = data_type->getTypeId();
    SERIALIZE_ENUM(index, buf)

    switch (index)
    {
        case TypeIndex::DateTime64: {
            auto type = std::dynamic_pointer_cast<const DataTypeDateTime64>(data_type);
            writeBinary(type->getScale(), buf);
            writeBinary(type->getTimeZone().getTimeZone(), buf);
            return;
        }
        case TypeIndex::Time: {
            auto type = std::dynamic_pointer_cast<const DataTypeTime>(data_type);
            writeBinary(type->getScale(), buf);
            return;
        }
        case TypeIndex::FixedString: {
            auto type = std::dynamic_pointer_cast<const DataTypeFixedString>(data_type);
            writeBinary(type->getN(), buf);
            return;
        }
    #define SERIALIZE_ENUM_TYPE(TYPE)\
        { \
            auto type = std::dynamic_pointer_cast<const TYPE>(data_type); \
            writeBinary(type->getValues().size(), buf); \
            for (const auto & value : type->getValues()) \
            { \
                writeBinary(value.first, buf); \
                writeBinary(value.second, buf); \
            } \
            return; \
        }
        case TypeIndex::Enum8:
            SERIALIZE_ENUM_TYPE(DataTypeEnum8)
        case TypeIndex::Enum16:
            SERIALIZE_ENUM_TYPE(DataTypeEnum16)
    #undef SERIALIZE_ENUM_TYPE
        case TypeIndex::Decimal32:
        case TypeIndex::Decimal64:
        case TypeIndex::Decimal128:
        case TypeIndex::Decimal256:
        {
            writeBinary(getDecimalScale(*data_type), buf);
            writeBinary(getDecimalPrecision(*data_type), buf);
            return;
        }
        case TypeIndex::Array: {
            auto type = std::dynamic_pointer_cast<const DataTypeArray>(data_type);
            serializeDataTypeV1(type->getNestedType(), buf);
            return;
        }
        case TypeIndex::Tuple: {
            auto type = std::dynamic_pointer_cast<const DataTypeTuple>(data_type);
            writeBinary(type->getElements().size(), buf);
            for (const auto & elem : type->getElements())
            {
                serializeDataTypeV1(elem, buf);
            }

            for (const auto & name : type->getElementNames())
            {
                writeBinary(name, buf);
            }
            return;
        }
        case TypeIndex::Interval: {
            auto type = std::dynamic_pointer_cast<const DataTypeInterval>(data_type);
            SERIALIZE_ENUM(type->getKind(), buf)
            return;
        }
        case TypeIndex::Nullable: {
            auto type = std::dynamic_pointer_cast<const DataTypeNullable>(data_type);
            serializeDataTypeV1(type->getNestedType(), buf);
            return;
        }
        case TypeIndex::LowCardinality: {
            auto type = std::dynamic_pointer_cast<const DataTypeLowCardinality>(data_type);
            serializeDataTypeV1(type->getDictionaryType(), buf);
            return;
        }
        case TypeIndex::Map: {
            auto type = std::dynamic_pointer_cast<const DataTypeMap>(data_type);
            serializeDataTypeV1(type->getKeyType(), buf);
            serializeDataTypeV1(type->getValueType(), buf);
            return;
        }
        case TypeIndex::Function: {
            auto type = std::dynamic_pointer_cast<const DataTypeFunction>(data_type);
            writeBinary(type->getArgumentTypes().size(), buf);
            for (const auto & arg : type->getArgumentTypes())
            {
                serializeDataTypeV1(arg, buf);
            }

            serializeDataTypeV1(type->getReturnType(), buf);
            return;
        }
        case TypeIndex::AggregateFunction: {
            auto type = std::dynamic_pointer_cast<const DataTypeAggregateFunction>(data_type);
            writeBinary(type->getFunctionName(), buf);
            writeBinary(type->getArgumentsDataTypes().size(), buf);
            for (const auto & arg : type->getArgumentsDataTypes())
            {
                serializeDataTypeV1(arg, buf);
            }
            writeBinary(type->getFunction()->getParameters().size(), buf);
            for (const auto & param : type->getFunction()->getParameters())
            {
                writeFieldBinary(param, buf);
            }
            return;
        }
        default:
            throw Exception("DataType " + data_type->getName() + " cannot be serialized", ErrorCodes::NOT_IMPLEMENTED);
    }
}

void serializeDataType(const DataTypePtr & data_type, WriteBuffer & buf)
{
    writeBinary(data_type->getName(), buf);
}

DataTypePtr deserializeDataType(ReadBuffer & buf)
{
    const DataTypeFactory & data_type_factory = DataTypeFactory::instance();
    String type_name;
    readBinary(type_name, buf);
    return data_type_factory.get(type_name);
}

void serializeDataTypes(const DataTypes & data_types, WriteBuffer & buf)
{
    writeBinary(data_types.size(), buf);
    for (const auto & item : data_types)
        serializeDataType(item, buf);
}

DataTypes deserializeDataTypes(ReadBuffer & buf)
{
    size_t size;
    readBinary(size, buf);
    DataTypes types;
    for (size_t index = 0; index < size; ++index)
    {
        index++;
        types.emplace_back(deserializeDataType(buf));
    }
    return types;
}

}

