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

#include <Analyzers/Analysis.h>
#include <Interpreters/asof.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTTablesInSelectQuery.h>

namespace DB
{
/// join
ASTPtr joinCondition(const ASTTableJoin & join);
bool isNormalInnerJoin(const ASTTableJoin & join);
bool isCrossJoin(const ASTTableJoin & join);
bool isSemiOrAntiJoin(const ASTTableJoin & join);
bool isAsofJoin(const ASTTableJoin & join);
String getFunctionForInequality(ASOF::Inequality inequality);

/// expressions
std::vector<ASTPtr> expressionToCnf(const ASTPtr & node);
ASTPtr cnfToExpression(const std::vector<ASTPtr> & cnf);

std::vector<ASTPtr> extractExpressions(ContextPtr context, Analysis & analysis, ASTPtr root, bool include_subquery = false,
    const std::function<bool(const ASTPtr &)> & filter = [](const auto &) {return true;});

std::vector<ASTPtr> extractReferencesToScope(ContextPtr context, Analysis & analysis, ASTPtr root, ScopePtr scope, bool include_subquery = false);

}
