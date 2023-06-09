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
#include <Optimizer/CardinalityEstimate/CardinalityEstimator.h>
#include <Optimizer/CardinalityEstimate/FilterEstimator.h>
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Optimizer/CardinalityEstimate/SymbolStatistics.h>
#include <Optimizer/DynamicFilters.h>
#include <Optimizer/Rewriter/Rewriter.h>
#include <QueryPlan/SimplePlanVisitor.h>
#include <QueryPlan/SimplePlanRewriter.h>
#include <QueryPlan/PlanVisitor.h>

namespace DB
{
/**
 * AddDynamicFilters generates, analyze, merge and remove inefficient or unused dynamic filters.
 */
class AddDynamicFilters : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "AddDynamicFilters"; }

private:
    class DynamicFilterExtractor;
    class DynamicFilterPredicatesRewriter;
    class RemoveUnusedDynamicFilterRewriter;
    class AddExchange;
};

struct DynamicFilterExtractorResult
{
    std::unordered_map<DynamicFilterId, SymbolStatisticsPtr> dynamic_filter_statistics;
    std::unordered_map<DynamicFilterId, size_t> dynamic_filter_cost;
    std::unordered_map<DynamicFilterId, DynamicFilterId> merged_dynamic_filters;
};

struct DynamicFilterWithScanRows
{
    std::unordered_map<std::string, DynamicFilterId> inherited_dynamic_filters;
    size_t scan_rows;
};

class AddDynamicFilters::DynamicFilterExtractor : public PlanNodeVisitor<DynamicFilterWithScanRows, ContextMutablePtr>
{
public:
    static DynamicFilterExtractorResult extract(QueryPlan & plan, ContextMutablePtr & context);

protected:
    explicit DynamicFilterExtractor(CTEInfo & cte_info_) : cte_info(cte_info_), cte_helper(cte_info_) { }

    DynamicFilterWithScanRows visitPlanNode(PlanNodeBase & node, ContextMutablePtr & dynamic_filters) override;
    DynamicFilterWithScanRows visitProjectionNode(ProjectionNode & node, ContextMutablePtr & dynamic_filters) override;
    DynamicFilterWithScanRows visitJoinNode(JoinNode & node, ContextMutablePtr & dynamic_filters) override;
    DynamicFilterWithScanRows visitExchangeNode(ExchangeNode & node, ContextMutablePtr & context) override;
    DynamicFilterWithScanRows visitTableScanNode(TableScanNode & node, ContextMutablePtr & context) override;
    DynamicFilterWithScanRows visitCTERefNode(CTERefNode & node, ContextMutablePtr & context) override;

private:
    CTEInfo & cte_info;
    SimpleCTEVisitHelper<DynamicFilterWithScanRows> cte_helper;
    std::unordered_map<DynamicFilterId, SymbolStatisticsPtr> dynamic_filter_statistics;
    std::unordered_map<DynamicFilterId, size_t> dynamic_filter_cost;
    std::unordered_map<DynamicFilterId, DynamicFilterId> merged_dynamic_filters;
};

struct AllowedDynamicFilters
{
    std::unordered_set<DynamicFilterId> allowed_dynamic_filters;
    std::unordered_set<DynamicFilterId> effective_dynamic_filters;
    ContextMutablePtr & context;
};

struct PlanWithDynamicFilterPredicates
{
    PlanNodePtr plan;
    std::unordered_map<DynamicFilterId, DynamicFilterTypes> effective_dynamic_filters;
};

struct PlanWithScanRows
{
    PlanNodePtr plan;
    size_t scan_rows;
};

class AddDynamicFilters::DynamicFilterPredicatesRewriter : public PlanNodeVisitor<PlanWithScanRows, AllowedDynamicFilters>
{
public:
    DynamicFilterPredicatesRewriter(
        CTEInfo & cte_info,
        const std::unordered_map<DynamicFilterId, SymbolStatisticsPtr> & dynamicFilterStatistics,
        const std::unordered_map<DynamicFilterId, size_t> & dynamic_filter_cost,
        const std::unordered_map<DynamicFilterId, DynamicFilterId> & merged_dynamic_filters,
        UInt64 minimum_filter_rows,
        double maximal_filter_factor,
        bool enable_dynamic_filter_for_join);

    PlanWithDynamicFilterPredicates rewrite(const PlanNodePtr & plan, ContextMutablePtr & context);

protected:
    PlanWithScanRows visitPlanNode(PlanNodeBase & plan, AllowedDynamicFilters & context) override;
    PlanWithScanRows visitFilterNode(FilterNode & node, AllowedDynamicFilters & context) override;
    PlanWithScanRows visitAggregatingNode(AggregatingNode & node, AllowedDynamicFilters &) override;
    PlanWithScanRows visitExchangeNode(ExchangeNode & node, AllowedDynamicFilters &) override;
    PlanWithScanRows visitJoinNode(JoinNode & node, AllowedDynamicFilters & context) override;
    PlanWithScanRows visitTableScanNode(TableScanNode & node, AllowedDynamicFilters & context) override;
    PlanWithScanRows visitCTERefNode(CTERefNode & node, AllowedDynamicFilters & context) override;

private:
    SimpleCTEVisitHelper<PlanWithScanRows> cte_helper;
    const std::unordered_map<DynamicFilterId, SymbolStatisticsPtr> & dynamic_filter_statistics;
    const std::unordered_map<DynamicFilterId, size_t> & dynamic_filter_cost;
    const std::unordered_map<DynamicFilterId, DynamicFilterId> & merged_dynamic_filters;
    const UInt64 min_filter_rows;
    const double max_filter_factor;
    const bool enable_dynamic_filter_for_join;

    std::unordered_map<DynamicFilterId, DynamicFilterTypes> effective_dynamic_filters{};
};

class AddDynamicFilters::RemoveUnusedDynamicFilterRewriter : public PlanNodeVisitor<PlanNodePtr, Void>
{
public:
    explicit RemoveUnusedDynamicFilterRewriter(
        ContextMutablePtr & context_,
        CTEInfo & cte_info,
        const std::unordered_map<DynamicFilterId, DynamicFilterTypes> & effective_dynamic_filters);
    PlanNodePtr rewrite(PlanNodePtr & node);

protected:
    PlanNodePtr visitPlanNode(PlanNodeBase & plan, Void &) override;
    PlanNodePtr visitProjectionNode(ProjectionNode & node, Void &) override;
    PlanNodePtr visitCTERefNode(CTERefNode & node, Void & context) override;

    ContextMutablePtr & context;
    CTEPreorderVisitHelper cte_helper;
    const std::unordered_map<DynamicFilterId, DynamicFilterTypes> & effective_dynamic_filters;
};

class AddDynamicFilters::AddExchange : public SimplePlanRewriter<bool>
{
public:
    static PlanNodePtr rewrite(const PlanNodePtr & node, ContextMutablePtr context, CTEInfo & cte_info);

protected:
    explicit AddExchange(ContextMutablePtr context_, CTEInfo & cte_info_) : SimplePlanRewriter(context_, cte_info_) {}
    PlanNodePtr visitPlanNode(PlanNodeBase & node, bool &) override;
    PlanNodePtr visitExchangeNode(ExchangeNode & node, bool &) override;
    PlanNodePtr visitFilterNode(FilterNode & node, bool & context) override;
};

}
