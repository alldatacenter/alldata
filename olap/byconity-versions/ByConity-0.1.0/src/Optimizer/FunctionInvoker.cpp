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

#include <Optimizer/FunctionInvoker.h>

#include <Functions/FunctionFactory.h>

namespace DB
{

FieldWithType FunctionInvoker::execute(const String & function_name, const FieldsWithType & arguments, ContextPtr context)
{
    ColumnsWithTypeAndName columns(arguments.size());

    std::transform(arguments.begin(), arguments.end(), columns.begin(), [](auto & arg) {
        auto col = arg.type->createColumnConst(1, arg.value);
        return ColumnWithTypeAndName(col, arg.type, "");
    });

    return execute(function_name, columns, context);
}

FieldWithType FunctionInvoker::execute(const String & function_name, const ColumnsWithTypeAndName & arguments, ContextPtr context)
{
    auto function_builder = FunctionFactory::instance().get(function_name, context);

    FunctionBasePtr function_base = function_builder->build(arguments);
    auto result_type = function_base->getResultType();
    auto result_column = function_base->execute(arguments, result_type, 1);

    if (!result_column || result_column->size() != 1)
        throw Exception("Invalid result.", ErrorCodes::LOGICAL_ERROR);

    return {result_type, (*result_column)[0]};
}

}
