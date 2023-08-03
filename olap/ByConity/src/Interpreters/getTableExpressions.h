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

#include <Core/NamesAndTypes.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>

namespace DB
{

struct ASTTableExpression;
class ASTSelectQuery;

using ASTTableExprConstPtrs = std::vector<const ASTTableExpression *>;

NameSet removeDuplicateColumns(NamesAndTypesList & columns);

ASTTableExprConstPtrs getTableExpressions(const ASTSelectQuery & select_query);

const ASTTableExpression * getTableExpression(const ASTSelectQuery & select, size_t table_number);
inline ASTTableExpression * getTableExpressionNonConst(ASTSelectQuery & select, size_t table_number)
{
    return const_cast<ASTTableExpression *>(getTableExpression(select, table_number));
}

ASTPtr extractTableExpression(const ASTSelectQuery & select, size_t table_number);

TablesWithColumns getDatabaseAndTablesWithColumns(
    const ASTTableExprConstPtrs & table_expressions, ContextPtr context, bool include_alias_cols, bool include_materialized_cols);

}
