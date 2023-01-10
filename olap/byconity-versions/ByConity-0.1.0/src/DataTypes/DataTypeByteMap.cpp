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

#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeFactory.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/Serializations/SerializationByteMap.h>
#include <Parsers/IAST.h>
#include <Columns/ColumnByteMap.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadBufferFromString.h>
#include <Formats/ProtobufReader.h>
#include <Common/FieldVisitors.h>
#include <Common/escapeForFileName.h>
#include <Core/NamesAndTypes.h>
#include <common/logger_useful.h>
#include <map>

namespace DB
{

std::string DataTypeByteMap::doGetName() const
{
    return "Map(" + key_type->getName() + ", " + value_type->getName() +  ")";
}


/// If type is not nullable, wrap it to be nullable
static DataTypePtr makeNullableForMapValue(const DataTypePtr & type)
{
    /// When type is low cardinality, its dictionary type must be nullable, see more detail in DataTypeLowCardinality::canBeMapValueType()
    return type->lowCardinality() ? type: makeNullable(type);
}

DataTypeByteMap::DataTypeByteMap(const DataTypePtr & keyType_, const DataTypePtr & valueType_) : key_type(keyType_), value_type(valueType_)
{
    key_store_type = std::make_shared<DataTypeArray>(key_type);
    value_store_type = std::make_shared<DataTypeArray>(value_type);
    implicit_column_value_type = makeNullableForMapValue(value_type);
    nested = std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(DataTypes{key_type, value_type}, Names{"key", "value"}));
}

SerializationPtr DataTypeByteMap::doGetDefaultSerialization() const
{
    return std::make_shared<SerializationByteMap>(
            key_type->getDefaultSerialization(),
            value_type->getDefaultSerialization());
}

MutableColumnPtr DataTypeByteMap::createColumn() const
{
    return ColumnByteMap::create(key_type->createColumn(), value_type->createColumn());
}

Field DataTypeByteMap::getDefault() const
{
    return ByteMap();
}

bool DataTypeByteMap::equals(const IDataType & rhs) const
{
    return typeid(rhs) == typeid(*this) &&
        key_type->equals(*static_cast<const DataTypeByteMap &>(rhs).key_type) &&
        value_type->equals(*static_cast<const DataTypeByteMap &>(rhs).value_type);
}

const DataTypePtr & DataTypeByteMap::getMapStoreType(const String & name) const
{
    if (endsWith(name, ".key"))
        return key_store_type;
    else if (endsWith(name, ".value"))
        return value_store_type;
    else
        throw Exception(name + " is not a valid KV column", ErrorCodes::LOGICAL_ERROR);
}

static DataTypePtr create(const ASTPtr& arguments)
{
    if (!arguments || arguments->children.size() != 2)
        throw Exception("Map data type faimily must have two argument-type for key-value pair", ErrorCodes::BAD_ARGUMENTS);

    DataTypePtr key_type = DataTypeFactory::instance().get(arguments->children[0]);
    DataTypePtr value_type = DataTypeFactory::instance().get(arguments->children[1]);
    if (!key_type->canBeMapKeyType())
        throw Exception(ErrorCodes::BAD_ARGUMENTS, "Map Key Type {} is not compatible", key_type->getName());
    if (!value_type->canBeMapValueType())
        throw Exception(ErrorCodes::BAD_ARGUMENTS, "Map Value Type {} is not compatible", value_type->getName());

    return std::make_shared<DataTypeByteMap>(key_type, value_type);
}

void registerDataTypeByteMap(DataTypeFactory& factory)
{
    factory.registerDataType("Map", create);
}

ColumnPtr mapOffsetsToSizes(const IColumn & column)
{
    const auto & column_offsets = assert_cast<const ColumnByteMap::ColumnOffsets &>(column);
    MutableColumnPtr column_sizes = column_offsets.cloneEmpty();

    if (column_offsets.empty())
        return column_sizes;

    const auto & offsets_data = column_offsets.getData();
    auto & sizes_data = assert_cast<ColumnByteMap::ColumnOffsets &>(*column_sizes).getData();

    sizes_data.resize(offsets_data.size());

    IColumn::Offset prev_offset = 0;
    for (size_t i = 0, size = offsets_data.size(); i < size; ++i)
    {
        auto current_offset = offsets_data[i];
        sizes_data[i] = current_offset - prev_offset;
        prev_offset =  current_offset;
    }

    return column_sizes;
}

}
