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

#include <Interpreters/ExpressionActionsSettings.h>

#include <Core/Settings.h>
#include <Interpreters/Context.h>

namespace DB
{

ExpressionActionsSettings ExpressionActionsSettings::fromSettings(const Settings & from, CompileExpressions compile_expressions)
{
    ExpressionActionsSettings settings;
    settings.can_compile_expressions = from.compile_expressions;
    settings.min_count_to_compile_expression = from.min_count_to_compile_expression;
    settings.max_temporary_columns = from.max_temporary_columns;
    settings.max_temporary_non_const_columns = from.max_temporary_non_const_columns;
    settings.compile_expressions = compile_expressions;
    settings.dialect_type = from.dialect_type;

    return settings;
}

ExpressionActionsSettings ExpressionActionsSettings::fromContext(ContextPtr from, CompileExpressions compile_expressions)
{
    return fromSettings(from->getSettingsRef(), compile_expressions);
}

}
