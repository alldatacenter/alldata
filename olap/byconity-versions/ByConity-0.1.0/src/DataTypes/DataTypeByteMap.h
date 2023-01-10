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

#pragma once

#include <DataTypes/IDataType.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>
#include <Common/StringUtils/StringUtils.h>


namespace DB
{

/**
 * Map data type in ByteDance.
 * Map can be in different mode:
 * - flatten mode: map is flatten as implicit key columns, __map__key
 * - compacted mode: map is flatten as implicit key columns and all key columns files
 *   are compacted in one file to avoid too many small files in the system
 * - KV mode: map is implemented as two array of keys and values, but it is slightly
 *   different from community format, i.e. it is Tuple(offset, key, value)
 * */
class DataTypeByteMap final : public IDataType
{
private:

    DataTypePtr key_type;
    DataTypePtr value_type;
    DataTypePtr implicit_column_value_type;
    DataTypePtr key_store_type;
    DataTypePtr value_store_type;

    /// 'nested' is an Array(Tuple(key_type, value_type))
    DataTypePtr nested;

public:
    static constexpr bool is_parametric = true;

    DataTypeByteMap(const DataTypePtr & keyType_, const DataTypePtr & valueType_);
    std::string doGetName() const override;
    const char * getFamilyName() const override { return "Map"; }
    TypeIndex getTypeId() const override { return TypeIndex::ByteMap; }
    bool canBeInsideNullable() const override { return false; }

    /*
    DataTypePtr tryGetSubcolumnType(const String & subcolumn_name) const override;
    ColumnPtr getSubcolumn(const String & subcolumn_name, const IColumn & column) const override;
    SerializationPtr getSubcolumnSerialization(const String & subcolumn_name,
            const BaseSerializationGetter & base_serialization_getter) const override;
    */

#if 0
    void serializeProtobuf(const IColumn & column, size_t row_num, ProtobufWriter & protobuf, size_t & value_index) const override;
    void deserializeProtobuf(IColumn & column, ProtobufReader & protobuf, bool allow_add_row, bool & row_added) const override;

    void deserializeProtobuf(IColumn & column, ProtobufReader & protobuf, String field_name, bool allow_add_row, bool & row_added) const;

#endif

    MutableColumnPtr createColumn() const override;

    Field getDefault() const override;

    bool equals(const IDataType & rhs) const override;
    bool isParametric() const override { return true; }
    bool isComparable() const override { return false; }
    bool haveSubtypes() const override { return true; }

    bool textCanContainOnlyValidUTF8() const override { return key_type->textCanContainOnlyValidUTF8() && value_type->textCanContainOnlyValidUTF8(); }
    bool isMap() const override { return true; }

    bool isValueUnambiguouslyRepresentedInContiguousMemoryRegion() const override
    {
        return key_type->isValueUnambiguouslyRepresentedInFixedSizeContiguousMemoryRegion() &&
               value_type->isValueUnambiguouslyRepresentedInFixedSizeContiguousMemoryRegion();
    }

    const DataTypePtr & getKeyType() const { return key_type; }
    const DataTypePtr & getValueType() const { return value_type; }
    const DataTypePtr & getValueTypeForImplicitColumn() const { return implicit_column_value_type; }
    const DataTypePtr & getMapStoreType(const String & name) const;
    const DataTypePtr & getNestedType() const { return nested; }

    SerializationPtr doGetDefaultSerialization() const override;
};

ColumnPtr mapOffsetsToSizes(const IColumn & column);

/// Get range of implicit column files from ordered files (e.g. std::map) with a hacking solution
template <class V>
std::pair<typename std::map<String, V>::const_iterator, typename std::map<String, V>::const_iterator>
getMapColumnRangeFromOrderedFiles(const String & map_column, const std::map<String, V> & m)
{
    String map_prefix = "__" + map_column + "__";
    auto beg = m.lower_bound(map_prefix);
    map_prefix.back() += 1; /// The modified prefix must greater than all implicit column files in map
    auto end = m.upper_bound(map_prefix);
    return {beg, end};
}

}
