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

#include <ostream>

#include <DataTypes/IDataType.h>


namespace DB
{

class DataTypeString final : public IDataType
{
public:
    using FieldType = String;
    static constexpr bool is_parametric = false;
    static constexpr auto type_id = TypeIndex::String;

    const char * getFamilyName() const override
    {
        return "String";
    }

    TypeIndex getTypeId() const override { return type_id; }

    MutableColumnPtr createColumn() const override;

    Field getDefault() const override;

    bool equals(const IDataType & rhs) const override;

    bool isParametric() const override { return false; }
    bool haveSubtypes() const override { return false; }
    bool isComparable() const override { return true; }
    bool canBeComparedWithCollation() const override { return true; }
    bool isValueUnambiguouslyRepresentedInContiguousMemoryRegion() const override { return true; }
    bool isCategorial() const override { return true; }
    bool canBeInsideNullable() const override { return true; }
    bool canBeInsideLowCardinality() const override { return true; }
    bool canBeMapKeyType() const override { return true; }
    bool canBeMapValueType() const override { return true; }

    SerializationPtr doGetDefaultSerialization() const override;
    Field stringToVisitorField(const String& ins) const override;
    String stringToVisitorString(const String & ins) const override;
};

}
