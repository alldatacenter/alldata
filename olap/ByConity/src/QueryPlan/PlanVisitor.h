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

#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/ApplyStep.h>
#include <QueryPlan/ArrayJoinStep.h>
#include <QueryPlan/AssignUniqueIdStep.h>
#include <QueryPlan/CTERefStep.h>
#include <QueryPlan/CreatingSetsStep.h>
#include <QueryPlan/CubeStep.h>
#include <QueryPlan/DistinctStep.h>
#include <QueryPlan/EnforceSingleRowStep.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/ExpressionStep.h>
#include <QueryPlan/ExtremesStep.h>
#include <QueryPlan/FillingStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/FinalSampleStep.h>
#include <QueryPlan/FinishSortingStep.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/LimitByStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/SortingStep.h>
#include <QueryPlan/MergeSortingStep.h>
#include <QueryPlan/MergingAggregatedStep.h>
#include <QueryPlan/MergingSortedStep.h>
#include <QueryPlan/OffsetStep.h>
#include <QueryPlan/PartialSortingStep.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/PlanSegmentSourceStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/QueryCacheStep.h>
#include <QueryPlan/ReadFromMergeTree.h>
#include <QueryPlan/ReadFromPreparedSource.h>
#include <QueryPlan/ReadNothingStep.h>
#include <QueryPlan/RemoteExchangeSourceStep.h>
#include <QueryPlan/RollupStep.h>
#include <QueryPlan/SettingQuotaAndLimitsStep.h>
#include <QueryPlan/SymbolAllocator.h>
#include <QueryPlan/TableScanStep.h>
#include <QueryPlan/TotalsHavingStep.h>
#include <QueryPlan/UnionStep.h>
#include <QueryPlan/ValuesStep.h>
#include <QueryPlan/Void.h>
#include <QueryPlan/WindowStep.h>

namespace DB
{
/// PlanNode visitor, for optimizer only.
template <typename R, typename C>
class PlanNodeVisitor
{
public:
    virtual ~PlanNodeVisitor() = default;

    virtual R visitPlanNode(PlanNodeBase &, C &) { throw Exception("Visitor does not supported this plan node.", ErrorCodes::NOT_IMPLEMENTED); }

#define VISITOR_DEF(TYPE) \
    virtual R visit##TYPE##Node(TYPE##Node & node, C & context) { return visitPlanNode(dynamic_cast<PlanNodeBase &>(node), context); }
    APPLY_STEP_TYPES(VISITOR_DEF)
#undef VISITOR_DEF
};

/// IQueryPlanStep visitor
template <typename R, typename C>
class StepVisitor
{
public:
    virtual ~StepVisitor() = default;

    virtual R visitStep(const IQueryPlanStep &, C &)
    {
        throw Exception("Visitor does not supported this step.", ErrorCodes::NOT_IMPLEMENTED);
    }

#define VISITOR_DEF(TYPE) \
    virtual R visit##TYPE##Step(const TYPE##Step & step, C & context) \
    { \
        return visitStep(dynamic_cast<const IQueryPlanStep &>(step), context); \
    }
    APPLY_STEP_TYPES(VISITOR_DEF)
#undef VISITOR_DEF
};

/// QueryPlan::Node visitor
template <typename R, typename C>
class NodeVisitor
{
public:
    virtual ~NodeVisitor() = default;

    virtual R visitNode(QueryPlan::Node *, C &) { throw Exception("Visitor does not supported this step.", ErrorCodes::NOT_IMPLEMENTED); }

#define VISITOR_DEF(TYPE) \
    virtual R visit##TYPE##Node(QueryPlan::Node * node, C & context) { return visitNode(node, context); }
    APPLY_STEP_TYPES(VISITOR_DEF)
#undef VISITOR_DEF
};

class VisitorUtil
{
public:

    template <typename R, typename C>
    static R accept(PlanNodeBase & node, PlanNodeVisitor<R, C> & visitor, C & context)
    {
#define VISITOR_DEF(TYPE) \
    if (node.getType() == IQueryPlanStep::Type::TYPE) \
    { \
        return visitor.visit##TYPE##Node(dynamic_cast<TYPE##Node &>(node), context); \
    }
        APPLY_STEP_TYPES(VISITOR_DEF)

#undef VISITOR_DEF
        return visitor.visitPlanNode(node, context);
    }

    template <typename R, typename C>
    static R accept(PlanNodePtr node, PlanNodeVisitor<R, C> & visitor, C & context)
    {
#define VISITOR_DEF(TYPE) \
    if (node->getType() == IQueryPlanStep::Type::TYPE) \
    { \
        return visitor.visit##TYPE##Node(dynamic_cast<TYPE##Node &>(*node), context); \
    }
        APPLY_STEP_TYPES(VISITOR_DEF)

#undef VISITOR_DEF
        return visitor.visitPlanNode(*node, context);
    }

    template <typename R, typename C>
    static R accept(ConstQueryPlanStepPtr step, StepVisitor<R, C> & visitor, C & context)
    {
#define VISITOR_DEF(TYPE) \
    if (step->getType() == IQueryPlanStep::Type::TYPE) \
    { \
        return visitor.visit##TYPE##Step(dynamic_cast<const TYPE##Step &>(*step), context); \
    }
        APPLY_STEP_TYPES(VISITOR_DEF)

#undef VISITOR_DEF
        return visitor.visitStep(*step, context);
    }

    template <typename R, typename C>
    static R accept(QueryPlan::Node * node, NodeVisitor<R, C> & visitor, C & context)
    {
#define VISITOR_DEF(TYPE) \
    if (node && node->step->getType() == IQueryPlanStep::Type::TYPE) \
    { \
        return visitor.visit##TYPE##Node(node, context); \
    }
        APPLY_STEP_TYPES(VISITOR_DEF)
#undef VISITOR_DEF
        return visitor.visitNode(node, context);
    }

};

}
