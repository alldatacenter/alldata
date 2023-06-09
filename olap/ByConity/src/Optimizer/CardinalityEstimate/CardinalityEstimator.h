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

#include <Analyzers/TypeAnalyzer.h>
#include <Interpreters/Context.h>
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/CTEVisitHelper.h>

namespace DB
{
class CardinalityEstimator
{
public:
    static std::optional<PlanNodeStatisticsPtr> estimate(
        ConstQueryPlanStepPtr & step,
        CTEInfo & cte_info,
        std::vector<PlanNodeStatisticsPtr> children_stats,
        ContextMutablePtr context,
        bool simple_children,
        std::vector<bool> is_table_scan);


    static std::optional<PlanNodeStatisticsPtr> estimate(PlanNodeBase & node, CTEInfo & cte_info, ContextMutablePtr context, bool recursive = false);
    static void estimate(QueryPlan & plan, ContextMutablePtr context);
};

struct CardinalityContext
{
    ContextMutablePtr context;
    CTEInfo & cte_info;
    std::vector<PlanNodeStatisticsPtr> children_stats;
    bool simple_children = false;
    std::vector<bool> children_are_table_scan = {};
    bool is_table_scan = false;
};

class CardinalityVisitor : public StepVisitor<PlanNodeStatisticsPtr, CardinalityContext>
{
public:
    PlanNodeStatisticsPtr visitStep(const IQueryPlanStep &, CardinalityContext &) override;

    PlanNodeStatisticsPtr visitProjectionStep(const ProjectionStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitFilterStep(const FilterStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitJoinStep(const JoinStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitAggregatingStep(const AggregatingStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitWindowStep(const WindowStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitMergingAggregatedStep(const MergingAggregatedStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitUnionStep(const UnionStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitIntersectStep(const IntersectStep &, CardinalityContext &) override;
    PlanNodeStatisticsPtr visitExceptStep(const ExceptStep &, CardinalityContext &) override;
    PlanNodeStatisticsPtr visitExchangeStep(const ExchangeStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitRemoteExchangeSourceStep(const RemoteExchangeSourceStep &, CardinalityContext &) override;
    PlanNodeStatisticsPtr visitTableScanStep(const TableScanStep & step, CardinalityContext & card_context) override;
    PlanNodeStatisticsPtr visitReadNothingStep(const ReadNothingStep &, CardinalityContext &) override;
    PlanNodeStatisticsPtr visitValuesStep(const ValuesStep & step, CardinalityContext &) override;
    PlanNodeStatisticsPtr visitLimitStep(const LimitStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitLimitByStep(const LimitByStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitSortingStep(const SortingStep &, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitMergeSortingStep(const MergeSortingStep &, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitPartialSortingStep(const PartialSortingStep &, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitMergingSortedStep(const MergingSortedStep &, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitDistinctStep(const DistinctStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitExtremesStep(const ExtremesStep &, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitApplyStep(const ApplyStep &, CardinalityContext &) override;
    PlanNodeStatisticsPtr visitEnforceSingleRowStep(const EnforceSingleRowStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitAssignUniqueIdStep(const AssignUniqueIdStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitCTERefStep(const CTERefStep & step, CardinalityContext & context) override;
    PlanNodeStatisticsPtr visitPartitionTopNStep(const PartitionTopNStep & step, CardinalityContext & context) override;
};

class PlanCardinalityVisitor : public PlanNodeVisitor<PlanNodeStatisticsPtr, CardinalityContext>
{
public:
    PlanCardinalityVisitor(CTEInfo & cte_info) : cte_helper(cte_info) { }

    PlanNodeStatisticsPtr visitPlanNode(PlanNodeBase &, CardinalityContext &) override;
    PlanNodeStatisticsPtr visitCTERefNode(CTERefNode & node, CardinalityContext & context) override;
private:
    CTEPreorderVisitHelper cte_helper;
};

}
