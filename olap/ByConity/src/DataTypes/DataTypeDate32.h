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

#include <DataTypes/DataTypeNumberBase.h>
#include <common/DateLUT.h>

namespace DB
{
class DataTypeDate32 final : public DataTypeNumberBase<Int32>
{
public:
    static constexpr auto family_name = "Date32";

    TypeIndex getTypeId() const override { return TypeIndex::Date32; }
    const char * getFamilyName() const override { return family_name; }

    Field getDefault() const override
    {
        return -static_cast<Int64>(DateLUT::instance().getDayNumOffsetEpoch());
    }

    bool canBeUsedAsVersion() const override { return true; }
    bool canBeInsideNullable() const override { return true; }

    bool equals(const IDataType & rhs) const override;

protected:
    SerializationPtr doGetDefaultSerialization() const override;
};
}
