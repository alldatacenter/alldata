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

#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Optimizer/FunctionInvoker.h>
#include <Common/FieldVisitorToString.h>
#include <Common/FieldVisitors.h>
#include <Common/tests/gtest_global_context.h>
#include <Common/tests/gtest_global_register.h>

#include <gtest/gtest.h>

#include <iostream>

using namespace DB;

void checkResult(const FieldWithType & a, const FieldWithType & b)
{
    if (!a.type->equals(*b.type) || a.value != b.value)
        GTEST_FAIL() << "check fails, a: " << applyVisitor(FieldVisitorToString(), a.value)
                     << "[" + a.type->getName() + "], b: " << applyVisitor(FieldVisitorToString(), b.value) << "[" + b.type->getName() + "]"
                     << std::endl;
}

TEST(OptimizerFunctionInvokerTest, FunctionInvoker)
{
    const auto & context = getContext().context;
    tryRegisterFunctions();

    checkResult(
        FunctionInvoker::execute(
            "plus", FieldsWithType{{std::make_shared<DataTypeUInt8>(), 1U}, {std::make_shared<DataTypeUInt8>(), 1U}}, context),
        FieldWithType{std::make_shared<DataTypeUInt16>(), 2U});

    checkResult(
        FunctionInvoker::execute("base64Encode", FieldsWithType{{std::make_shared<DataTypeString>(), "foo"}}, context),
        FieldWithType{std::make_shared<DataTypeString>(), "Zm9v"});

    checkResult(
        FunctionInvoker::execute("length", FieldsWithType{{makeNullable(std::make_shared<DataTypeString>()), Null()}}, context),
        FieldWithType{makeNullable(std::make_shared<DataTypeUInt64>()), Null()});
}
