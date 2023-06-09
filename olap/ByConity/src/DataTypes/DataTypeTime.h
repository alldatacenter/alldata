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

#include <Core/Types.h>
#include <DataTypes/DataTypeDecimalBase.h>

namespace DB
{

// `scale` determines number of decimal places for sub-second part of the Time.

class DataTypeTime final : public DataTypeDecimalBase<Decimal64>
{
public:
    using Base = DataTypeDecimalBase<Decimal64>;
    static constexpr UInt8 default_scale = 3;

    static constexpr auto family_name = "Time";
    static constexpr auto type_id = TypeIndex::Time;

    explicit DataTypeTime(UInt32 scale);

    const char * getFamilyName() const override { return family_name; }
    std::string doGetName() const override;
    TypeIndex getTypeId() const override { return type_id; }

    bool equals(const IDataType & rhs) const override;

    bool canBePromoted() const override { return false; }

    bool canBeUsedAsVersion() const override { return true; }

protected:
    SerializationPtr doGetDefaultSerialization() const override;
};

}
