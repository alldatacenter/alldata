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

#include <Interpreters/RuntimeFilter/MultipleAliasVisitor.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Common/typeid_cast.h>

namespace DB
{

namespace ErrorCodes
{
extern const int MULTIPLE_EXPRESSIONS_FOR_ALIAS;
}


bool MultipleAliasMatcher::needChildVisit(ASTPtr & node, const ASTPtr &)
{
    /// Don't descent into table functions and subqueries and special case for ArrayJoin.
    if (node->as<ASTTableExpression>() || node->as<ASTArrayJoin>())
        return false;
    return true;
}

void MultipleAliasMatcher::visit(ASTPtr & ast, Data & data)
{
    if (auto * s = ast->as<ASTSubquery>())
        visit(*s, ast, data);
    else if (auto * aj = ast->as<ASTArrayJoin>())
        visit(*aj, ast, data);
    else if (auto * select = ast->as<ASTSelectQuery>())
        visit(*select, ast, data);
    else
        visitOther(ast, data);
}

/// The top-level aliases in the ARRAY JOIN section have a special meaning, we will not add them
/// (skip the expression list itself and its children).
void MultipleAliasMatcher::visit(const ASTArrayJoin &, const ASTPtr & ast, Data & data)
{
    visitOther(ast, data);

    std::vector<ASTPtr> grand_children;
    for (auto & child1 : ast->children)
        for (auto & child2 : child1->children)
            for (auto & child3 : child2->children)
                grand_children.push_back(child3);

    /// create own visitor to run bottom to top
    for (auto & child : grand_children)
        Visitor(data).visit(child);
}

/// set unique aliases for all subqueries. this is needed, because:
/// 1) content of subqueries could change after recursive analysis, and auto-generated column names could become incorrect
/// 2) result of different scalar subqueries can be cached inside expressions compilation cache and must have different names
void MultipleAliasMatcher::visit(ASTSubquery & subquery, const ASTPtr & ast, Data & data)
{
    MultipleAliases & multiple_aliases = data.multiple_aliases;

    // FIXME: using instancle level index to track subquery alias is problemic as it will be random
    // and cause mismatch if subquery is run on different onde(e.g. header name in perfect shard)
    // Sample query:
    //   SELECT distinct (hash_uid IN (
    //   SELECT hash_uid  FROM misc_online_cohort_res_all  WHERE (app_id = 6) AND ((cohort_id = 156) AND (version = 421)) ))  FROM misc_online_all
    //   WHERE (app_id = 6) AND (event_date >= '2019-08-08') AND (event_date <= '2019-08-14') GROUP BY hash_uid SETTINGS distributed_perfect_shard = 1 )
    //
    //static std::atomic_uint64_t subquery_index = 0;
    size_t subquery_index = 0;

    if (subquery.alias.empty())
    {
        String alias;
        do
        {
            alias = "_subquery" + std::to_string(++subquery_index);
        }
        while (multiple_aliases.count(alias));

        subquery.setAlias(alias);
        subquery.prefer_alias_to_column_name = true;
        multiple_aliases[alias].emplace_back(ast->getColumnName());
    }
    else
        visitOther(ast, data);
}

void MultipleAliasMatcher::visitOther(const ASTPtr & ast, Data & data)
{
    MultipleAliases & multiple_aliases = data.multiple_aliases;
    String alias = ast->tryGetAlias();
    if (!alias.empty())
        multiple_aliases[alias].emplace_back(ast->getColumnName());
}

static bool constantSelect(const ASTPtr & select_list)
{
    if (!select_list)
        return false;

    if (auto list = select_list->as<ASTExpressionList>())
    {
        for (auto child : list->children)
        {
            if (!child->as<ASTLiteral>())
                return false;
        }
        return true;
    }

    return false;
}

/**
 * For a filter, if it appears in result header (only when the select list is constant so that the engine should select a where column as hreader),
 * we think it is better to use alias instead of column name.
 * This behavior is consistent with columns appeared in select list.
 * Also, in some special case, header in coordinator and worker is not same due to different alias behavior,
**/
void MultipleAliasMatcher::visitFilter(ASTPtr & filter)
{
    if (auto function = filter->as<ASTFunction>())
    {
        if (function->name != "and")
        {
            if (!function->alias.empty())
                function->prefer_alias_to_column_name = true;

            if (function->arguments)
            {
                for (auto & child : function->arguments->children)
                    visitFilter(child);
            }
        }
        else
        {
            for (auto & child : function->children)
                visitFilter(child);
        }
    }
}

void MultipleAliasMatcher::visit(ASTSelectQuery & select, const ASTPtr & ast, Data & data)
{
    if (constantSelect(select.select()))
    {
        if (select.where())
            visitFilter(select.refWhere());
        if (select.prewhere())
            visitFilter(select.refPrewhere());
    }

    visitOther(ast, data);
}

}
