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

#include <Core/Field.h>
#include <DataTypes/IDataType.h>
#include <Interpreters/Context.h>

namespace DB
{
struct FieldWithType
{
    DataTypePtr type;
    Field value;
    bool operator==(const FieldWithType & other) const
    {
        return type->getTypeId() == other.type->getTypeId() && value == other.value;
    }
};

using FieldsWithType = std::vector<FieldWithType>;

/**
 * FunctionInvoker is a util to execute a function by the function's name and arguments.
 */
class FunctionInvoker
{
public:
    static FieldWithType execute(const String & function_name, const FieldsWithType & arguments, ContextPtr context);
    static FieldWithType execute(const String & function_name, const ColumnsWithTypeAndName & arguments, ContextPtr context);
};
}
