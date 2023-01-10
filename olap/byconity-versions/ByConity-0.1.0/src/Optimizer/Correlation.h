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

#include <Interpreters/Context.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <QueryPlan/PlanVisitor.h>

namespace DB
{

/**
 * Extract correlation symbol from filter node in subquery.
 */
class Correlation
{
public:
    static std::vector<String> prune(PlanNodePtr & node, const Names & origin_correlation);
    static bool containsCorrelation(PlanNodePtr & node, Names & correlation);
    static bool isCorrelated(ConstASTPtr & expression, Names & correlation);
    static bool isUnreferencedScalar(PlanNodePtr & node);
};

struct DecorrelationResult
{
    PlanNodePtr node;
    std::set<String> symbols_to_propagate{};
    std::vector<ConstASTPtr> correlation_predicates{};
    bool at_most_single_row = false;
    std::pair<Names, Names> extractCorrelations(Names & correlation);
    std::pair<Names, Names> extractJoinClause(Names & correlation);
    std::vector<ConstASTPtr> extractFilter();
};

/**
 * Split correlation part and un-correlation part in subquery.
 */
class Decorrelation
{
public:
    static std::optional<DecorrelationResult> decorrelateFilters(PlanNodePtr & node, Names & correlation, Context & context);
};

class DecorrelationVisitor : public PlanNodeVisitor<std::optional<DecorrelationResult>, Context>
{
public:
    DecorrelationVisitor(Names & correlation_) : correlation(correlation_) { }
    std::optional<DecorrelationResult> visitPlanNode(PlanNodeBase & node, Context & context) override;
    std::optional<DecorrelationResult> visitFilterNode(FilterNode & node, Context & context) override;
    std::optional<DecorrelationResult> visitProjectionNode(ProjectionNode & node, Context & context) override;

private:
    Names correlation;
};

}
