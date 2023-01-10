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
#include <DataTypes/Serializations/SerializationArray.h>


namespace DB
{


class DataTypeArray final : public IDataType
{
private:
    /// The type of array elements.
    DataTypePtr nested;

public:
    static constexpr bool is_parametric = true;

    DataTypeArray(const DataTypePtr & nested_);

    TypeIndex getTypeId() const override { return TypeIndex::Array; }

    std::string doGetName() const override
    {
        return "Array(" + nested->getName() + ")";
    }

    const char * getFamilyName() const override
    {
        return "Array";
    }

    /// Map support array key
    bool canBeInsideNullable() const override
    {
        return true;
    }

    MutableColumnPtr createColumn() const override;

    Field getDefault() const override;

    bool equals(const IDataType & rhs) const override;

    bool isParametric() const override { return true; }
    bool haveSubtypes() const override { return true; }
    bool cannotBeStoredInTables() const override { return nested->cannotBeStoredInTables(); }
    bool textCanContainOnlyValidUTF8() const override { return nested->textCanContainOnlyValidUTF8(); }
    bool isComparable() const override { return nested->isComparable(); }
    bool canBeComparedWithCollation() const override { return nested->canBeComparedWithCollation(); }

    bool isValueUnambiguouslyRepresentedInContiguousMemoryRegion() const override
    {
        return nested->isValueUnambiguouslyRepresentedInFixedSizeContiguousMemoryRegion();
    }

    DataTypePtr tryGetSubcolumnType(const String & subcolumn_name) const override;
    ColumnPtr getSubcolumn(const String & subcolumn_name, const IColumn & column) const override;
    SerializationPtr getSubcolumnSerialization(
        const String & subcolumn_name, const BaseSerializationGetter & base_serialization_getter) const override;

    SerializationPtr doGetDefaultSerialization() const override;

    const DataTypePtr & getNestedType() const { return nested; }

    /// 1 for plain array, 2 for array of arrays and so on.
    size_t getNumberOfDimensions() const;

    bool canBeMapValueType() const override { return true; }

private:
    ColumnPtr getSubcolumnImpl(const String & subcolumn_name, const IColumn & column, size_t level) const;
    DataTypePtr tryGetSubcolumnTypeImpl(const String & subcolumn_name, size_t level) const;
    SerializationPtr getSubcolumnSerializationImpl(
        const String & subcolumn_name, const BaseSerializationGetter & base_serialization_getter, size_t level) const;
};

}
