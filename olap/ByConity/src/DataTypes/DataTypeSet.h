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

#include <DataTypes/IDataTypeDummy.h>
#include <DataTypes/Serializations/SerializationSet.h>
#include <Columns/ColumnSet.h>

namespace DB
{

/** The data type corresponding to the set of values in the IN section.
  * Used only as an intermediate when evaluating expressions.
  */
class DataTypeSet final : public IDataTypeDummy
{
public:
    static constexpr bool is_parametric = true;
    const char * getFamilyName() const override { return "Set"; }
    TypeIndex getTypeId() const override { return TypeIndex::Set; }
    bool equals(const IDataType & rhs) const override { return typeid(rhs) == typeid(*this); }
    bool isParametric() const override { return true; }

    // Used for expressions analysis.
    MutableColumnPtr createColumn() const override { return ColumnSet::create(0, nullptr); }

    // Used only for debugging, making it DUMPABLE
    Field getDefault() const override { return Tuple(); }

    SerializationPtr doGetDefaultSerialization() const override { return std::make_shared<SerializationSet>(); }
};

}

