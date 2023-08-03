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
#include <Core/Field.h>


namespace DB
{

class DataTypeBitMap64 final : public IDataType
{
public:
    using FieldType = roaring::Roaring64Map;
    static constexpr bool is_parametric = false;

    const char * getFamilyName() const override
    {
        return "BitMap64";
    }

    TypeIndex getTypeId() const override { return TypeIndex::BitMap64; }

    MutableColumnPtr createColumn() const override;

    Field getDefault() const override
    {
        return Field(BitMap64());
    }

    bool equals(const IDataType & rhs) const override;

    bool isParametric() const override { return false; }
    bool haveSubtypes() const override { return false; }
    bool isComparable() const override { return true; }
    bool canBeComparedWithCollation() const override { return true; }
    bool isValueUnambiguouslyRepresentedInContiguousMemoryRegion() const override { return true; }
    bool isCategorial() const override { return true; }
    bool canBeInsideNullable() const override { return false; }
    bool canBeInsideLowCardinality() const override { return false; }

    SerializationPtr doGetDefaultSerialization() const override;
};

}
