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

#include <Parsers/ParserExplainQuery.h>

#include <Parsers/ASTExplainQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/ParserSelectWithUnionQuery.h>
#include <Parsers/ParserSetQuery.h>
#include <Parsers/ParserQuery.h>

namespace DB
{

bool ParserExplainQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ASTExplainQuery::ExplainKind kind;

    ParserKeyword s_ast("AST");
    ParserKeyword s_explain("EXPLAIN");
    ParserKeyword s_syntax("SYNTAX");
    ParserKeyword s_pipeline("PIPELINE");
    ParserKeyword s_plan("PLAN");
    ParserKeyword s_view("VIEW");
    ParserKeyword s_element("ELEMENT");
    ParserKeyword s_plansegment("PLANSEGMENT");
    ParserKeyword s_opt_plan("OPT_PLAN");


    if (s_explain.ignore(pos, expected))
    {
        kind = ASTExplainQuery::QueryPlan;
        if (s_ast.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::ParsedAST;
        else if (s_syntax.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::AnalyzedSyntax;
        else if (s_pipeline.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::QueryPipeline;
        else if (s_plan.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::QueryPlan; //-V1048
        else if (s_view.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::MaterializedView;
        else if (s_element.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::QueryElement;
        else if (s_plansegment.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::PlanSegment;
        else if (s_opt_plan.ignore(pos, expected))
            kind = ASTExplainQuery::ExplainKind::OptimizerPlan;
    }
    else
        return false;

    auto explain_query = std::make_shared<ASTExplainQuery>(kind);

    {
        ASTPtr settings;
        ParserSetQuery parser_settings(true);

        auto begin = pos;
        if (parser_settings.parse(pos, settings, expected))
            explain_query->setSettings(std::move(settings));
        else
            pos = begin;
    }

    ParserCreateTableQuery create_p(dt);
    ParserSelectWithUnionQuery select_p(dt);
    ASTPtr query;
    if (kind == ASTExplainQuery::ExplainKind::ParsedAST)
    {
        ParserQuery p(end, dt);
        if (p.parse(pos, query, expected))
            explain_query->setExplainedQuery(std::move(query));
        else
            return false;
    }
    else if (select_p.parse(pos, query, expected) ||
             create_p.parse(pos, query, expected))
        explain_query->setExplainedQuery(std::move(query));
    else
        return false;

    node = std::move(explain_query);
    return true;
}

}
