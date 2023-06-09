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

#include <Core/SettingsEnums.h>

#include <Interpreters/Context_fwd.h>

#include <cstddef>

namespace DB
{

struct Settings;

enum class CompileExpressions: uint8_t
{
    no = 0,
    yes = 1,
};

struct ExpressionActionsSettings
{
    bool can_compile_expressions = false;
    size_t min_count_to_compile_expression = 0;

    size_t max_temporary_columns = 0;
    size_t max_temporary_non_const_columns = 0;

    CompileExpressions compile_expressions = CompileExpressions::no;

    enum DialectType dialect_type = DialectType::CLICKHOUSE;

    static ExpressionActionsSettings fromSettings(const Settings & from, CompileExpressions compile_expressions = CompileExpressions::no);
    static ExpressionActionsSettings fromContext(ContextPtr from, CompileExpressions compile_expressions = CompileExpressions::no);
};

}
