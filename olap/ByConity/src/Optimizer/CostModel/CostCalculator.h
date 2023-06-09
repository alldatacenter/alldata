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
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Optimizer/CostModel/CostModel.h>
#include <Optimizer/CostModel/PlanNodeCost.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/QueryPlan.h>

namespace DB
{
using PlanCostMap = std::unordered_map<PlanNodeId, double>;

class CostCalculator
{
public:
    static PlanCostMap calculate(QueryPlan & plan, const Context & context);

    static PlanNodeCost calculate(
        ConstQueryPlanStepPtr & step,
        const PlanNodeStatisticsPtr & stats,
        const std::vector<PlanNodeStatisticsPtr> & children_stats,
        const Context & context,
        size_t worker_size);
};

struct CostContext
{
    CostModel cost_model;
    PlanNodeStatisticsPtr stats;
    const std::vector<PlanNodeStatisticsPtr> & children_stats;
    size_t worker_size;
};

class CostVisitor : public StepVisitor<PlanNodeCost, CostContext>
{
public:
    PlanNodeCost visitStep(const IQueryPlanStep &, CostContext &) override;

    PlanNodeCost visitProjectionStep(const ProjectionStep & step, CostContext & context) override;
    PlanNodeCost visitFilterStep(const FilterStep & step, CostContext & context) override;
    PlanNodeCost visitJoinStep(const JoinStep & step, CostContext & cost_context) override;
    PlanNodeCost visitAggregatingStep(const AggregatingStep & step, CostContext & context) override;
    PlanNodeCost visitWindowStep(const WindowStep & step, CostContext & context) override;
    PlanNodeCost visitMergingAggregatedStep(const MergingAggregatedStep & step, CostContext & context) override;
    PlanNodeCost visitUnionStep(const UnionStep & step, CostContext & context) override;
    PlanNodeCost visitIntersectStep(const IntersectStep & step, CostContext & context) override;
    PlanNodeCost visitExceptStep(const ExceptStep & step, CostContext & context) override;
    PlanNodeCost visitExchangeStep(const ExchangeStep & step, CostContext & cost_context) override;
    PlanNodeCost visitRemoteExchangeSourceStep(const RemoteExchangeSourceStep & step, CostContext & context) override;
    PlanNodeCost visitTableScanStep(const TableScanStep & step, CostContext & context) override;
    PlanNodeCost visitReadNothingStep(const ReadNothingStep & step, CostContext & context) override;
    PlanNodeCost visitValuesStep(const ValuesStep & step, CostContext & context) override;
    PlanNodeCost visitLimitStep(const LimitStep & step, CostContext & context) override;
    PlanNodeCost visitLimitByStep(const LimitByStep & step, CostContext & context) override;
    PlanNodeCost visitSortingStep(const SortingStep & step, CostContext & context) override;
    PlanNodeCost visitMergeSortingStep(const MergeSortingStep & step, CostContext & context) override;
    PlanNodeCost visitPartialSortingStep(const PartialSortingStep & step, CostContext & context) override;
    PlanNodeCost visitMergingSortedStep(const MergingSortedStep & step, CostContext & context) override;
    PlanNodeCost visitDistinctStep(const DistinctStep & step, CostContext & context) override;
    PlanNodeCost visitExtremesStep(const ExtremesStep & step, CostContext & context) override;
    PlanNodeCost visitApplyStep(const ApplyStep & step, CostContext & context) override;
    PlanNodeCost visitEnforceSingleRowStep(const EnforceSingleRowStep & step, CostContext & context) override;
    PlanNodeCost visitAssignUniqueIdStep(const AssignUniqueIdStep & step, CostContext & context) override;
    PlanNodeCost visitCTERefStep(const CTERefStep & step, CostContext & context) override;
};

struct CostWithCTEReferenceCounts
{
    double cost;
    std::unordered_map<CTEId, UInt64> cte_reference_counts;
};

class PlanCostVisitor : public PlanNodeVisitor<CostWithCTEReferenceCounts, PlanCostMap>
{
public:
    PlanCostVisitor(
        CostModel cost_model_, size_t worker_size_, CTEInfo & cte_info_, const std::unordered_map<CTEId, UInt64> & cte_ref_counts_)
        : cost_model(std::move(cost_model_)), worker_size(worker_size_), cte_info(cte_info_), cte_ref_counts(cte_ref_counts_)
    {
    }

    CostWithCTEReferenceCounts visitPlanNode(PlanNodeBase &, PlanCostMap & map) override;
    CostWithCTEReferenceCounts visitCTERefNode(CTERefNode & node, PlanCostMap & map) override;

private:
    CostModel cost_model;
    size_t worker_size;
    CTEInfo & cte_info;
    const std::unordered_map<CTEId, UInt64> & cte_ref_counts;
};


}
