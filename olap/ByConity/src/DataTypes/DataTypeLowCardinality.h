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
#include <Columns/IColumnUnique.h>


namespace DB
{

class DataTypeLowCardinality : public IDataType
{
protected:
    DataTypePtr dictionary_type;

public:
    DataTypeLowCardinality(DataTypePtr dictionary_type_);

    const DataTypePtr & getDictionaryType() const { return dictionary_type; }

    String doGetName() const override
    {
        return "LowCardinality(" + dictionary_type->getName() + ")";
    }
    const char * getFamilyName() const override { return "LowCardinality"; }
    TypeIndex getTypeId() const override { return TypeIndex::LowCardinality; }
    DataTypePtr getFullLowCardinalityTypePtr() const;

    MutableColumnPtr createColumn() const override;

    Field getDefault() const override;

    bool equals(const IDataType & rhs) const override;

    bool isParametric() const override { return true; }
    bool haveSubtypes() const override { return true; }
    bool cannotBeStoredInTables() const override { return dictionary_type->cannotBeStoredInTables(); }
    bool shouldAlignRightInPrettyFormats() const override { return dictionary_type->shouldAlignRightInPrettyFormats(); }
    bool textCanContainOnlyValidUTF8() const override { return dictionary_type->textCanContainOnlyValidUTF8(); }
    bool isComparable() const override { return dictionary_type->isComparable(); }
    bool canBeComparedWithCollation() const override { return dictionary_type->canBeComparedWithCollation(); }
    bool canBeUsedAsVersion() const override { return dictionary_type->canBeUsedAsVersion(); }
    bool isSummable() const override { return dictionary_type->isSummable(); }
    bool canBeUsedInBitOperations() const override { return dictionary_type->canBeUsedInBitOperations(); }
    bool canBeUsedInBooleanContext() const override { return dictionary_type->canBeUsedInBooleanContext(); }
    bool isValueRepresentedByNumber() const override { return dictionary_type->isValueRepresentedByNumber(); }
    bool isValueRepresentedByInteger() const override { return dictionary_type->isValueRepresentedByInteger(); }
    bool isValueUnambiguouslyRepresentedInContiguousMemoryRegion() const override { return true; }
    bool haveMaximumSizeOfValue() const override { return dictionary_type->haveMaximumSizeOfValue(); }
    size_t getMaximumSizeOfValueInMemory() const override { return dictionary_type->getMaximumSizeOfValueInMemory(); }
    size_t getSizeOfValueInMemory() const override { return dictionary_type->getSizeOfValueInMemory(); }
    bool isCategorial() const override { return false; }
    bool isNullable() const override { return false; }
    bool onlyNull() const override { return false; }
    bool lowCardinality() const override { return true; }

    static MutableColumnUniquePtr createColumnUnique(const IDataType & keys_type);
    static MutableColumnUniquePtr createColumnUnique(const IDataType & keys_type, MutableColumnPtr && keys);

    /// Key can not be null because it's meaningless
    bool canBeMapKeyType() const override { return dictionary_type->canBeMapKeyType(); }
    /// Due to LowCardinality can not be inside nullable, so if dictionary_type is not nullable, ColumnByteMap can not insert Null field for missing key when handling each row. You can see more information in method ColumnByteMap::getValueColumnByKey.
    bool canBeMapValueType() const override;

protected:
    SerializationPtr doGetDefaultSerialization() const override;

    template <typename ... Params>
    using SerializeFunctionPtr = void (IDataType::*)(const IColumn &, size_t, Params ...) const;

    template <typename... Params, typename... Args>
    void serializeImpl(const IColumn & column, size_t row_num, SerializeFunctionPtr<Params...> func, Args &&... args) const;

    template <typename ... Params>
    using DeserializeFunctionPtr = void (IDataType::*)(IColumn &, Params ...) const;

    template <typename ... Params, typename... Args>
    void deserializeImpl(IColumn & column, DeserializeFunctionPtr<Params...> func, Args &&... args) const;

    template <typename Creator>
    static MutableColumnUniquePtr createColumnUniqueImpl(const IDataType & keys_type, const Creator & creator);
};

/**
 * For Low Cardinality column fall-back, this type only live in serialize process.
 * Deserialize still use the parent type, and the dict version help to work properly.
 */
class DataTypeFullLowCardinality : public DataTypeLowCardinality
{
public:
    DataTypeFullLowCardinality(DataTypePtr dictionary_type_);
    String doGetName() const override
    {
        return "FullLowCardinality(" + dictionary_type->getName() + ")";
    }

protected:
    SerializationPtr doGetDefaultSerialization() const override;
};

/// Returns dictionary type if type is DataTypeLowCardinality, type otherwise.
DataTypePtr removeLowCardinality(const DataTypePtr & type);

/// Remove LowCardinality recursively from all nested types.
DataTypePtr recursiveRemoveLowCardinality(const DataTypePtr & type);

/// Remove LowCardinality recursively from all nested columns.
ColumnPtr recursiveRemoveLowCardinality(const ColumnPtr & column);

/// Convert column of type from_type to type to_type by converting nested LowCardinality columns.
ColumnPtr recursiveTypeConversion(const ColumnPtr & column, const DataTypePtr & from_type, const DataTypePtr & to_type);

}
