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

#include <Parsers/ASTQueryWithOutput.h>


namespace DB
{


/// AST, EXPLAIN or other query with meaning of explanation query instead of execution
class ASTExplainQuery : public ASTQueryWithOutput
{
public:
    enum ExplainKind
    {
        ParsedAST, /// 'EXPLAIN AST SELECT ...'
        AnalyzedSyntax, /// 'EXPLAIN SYNTAX SELECT ...'
        QueryPlan, /// 'EXPLAIN SELECT ...'
        QueryPipeline, /// 'EXPLAIN PIPELINE ...'
        MaterializedView, /// 'EXPLAIN VIEW SELECT ...'
        QueryElement, /// 'EXPLAIN ELEMENT ...'
        PlanSegment, /// 'EXPLAIN PLANSEGMENT ...'
        OptimizerPlan, /// 'EXPLAIN OPT_PLAN ...'
    };

    explicit ASTExplainQuery(ExplainKind kind_) : kind(kind_) {}

    String getID(char delim) const override { return "Explain" + (delim + toString(kind)); }
    ExplainKind getKind() const { return kind; }
    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTExplainQuery>(*this);
        res->children.clear();
        res->children.push_back(children[0]->clone());
        cloneOutputOptions(*res);
        return res;
    }

    ASTType getType() const override { return ASTType::ASTExplainQuery; }

    void setExplainedQuery(ASTPtr query_)
    {
        children.emplace_back(query_);
        query = std::move(query_);
    }

    void setSettings(ASTPtr settings_)
    {
        children.emplace_back(settings_);
        ast_settings = std::move(settings_);
    }

    const ASTPtr & getExplainedQuery() const { return query; }
    ASTPtr & getExplainedQuery() { return query; }
    const ASTPtr & getSettings() const { return ast_settings; }

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << toString(kind) << (settings.hilite ? hilite_none : "");

        if (ast_settings)
        {
            settings.ostr << ' ';
            ast_settings->formatImpl(settings, state, frame);
        }

        settings.ostr << settings.nl_or_ws;
        query->formatImpl(settings, state, frame);
    }

private:
    ExplainKind kind;

    ASTPtr query;
    ASTPtr ast_settings;

    static String toString(ExplainKind kind)
    {
        switch (kind)
        {
            case ParsedAST: return "EXPLAIN AST";
            case AnalyzedSyntax: return "EXPLAIN SYNTAX";
            case QueryPlan: return "EXPLAIN";
            case QueryPipeline: return "EXPLAIN PIPELINE";
            case MaterializedView: return "EXPLAIN VIEW";
            case QueryElement: return "EXPLAIN ELEMENT";
            case PlanSegment: return "EXPLAIN PLANSEGMENT";
            case OptimizerPlan: return "EXPLAIN OPT_PLAN";
        }

        __builtin_unreachable();
    }
};

}
