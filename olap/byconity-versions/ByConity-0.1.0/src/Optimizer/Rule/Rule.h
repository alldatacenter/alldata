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
#include <Optimizer/Rule/Pattern.h>
#include <QueryPlan/CTEInfo.h>
#include <QueryPlan/PlanNode.h>

#include <utility>

namespace DB
{
class OptimizationContext;
using OptContextPtr = std::shared_ptr<OptimizationContext>;
using GroupId = UInt32;
using CTEId = UInt32;

struct RuleContext
{
    ContextMutablePtr context;
    CTEInfo & cte_info;
    OptContextPtr optimization_context = nullptr;
    GroupId group_id = -1;
};

class Rule;
using RulePtr = std::shared_ptr<Rule>;

enum class RuleType : UInt32
{
    MERGE_EXCEPT = 0,
    MERGE_INTERSECT,
    MERGE_UNION,
    IMPLEMENT_EXCEPT,
    IMPLEMENT_INTERSECT,

    COMMON_PREDICATE_REWRITE,
    SWAP_PREDICATE_REWRITE,
    SIMPLIFY_EXPRESSION_REWRITE,
    SIMPLIFY_PREDICATE_REWRITE,
    UN_WARP_CAST_IN_PREDICATE_REWRITE,
    SIMPLIFY_JOIN_FILTER_REWRITE,

    DISTINCT_TO_AGGREGATE,
    LEFT_JOIN_TO_RIGHT_JOIN,
    INLINE_PROJECTION,
    INLINE_PROJECTION_INTO_JOIN,
    INLINE_PROJECTION_ON_JOIN_INTO_JOIN,
    PULL_PROJECTION_ON_JOIN_THROUGH_JOIN,

    PUSH_PARTIAL_AGG_THROUGH_EXCHANGE,
    PUSH_PARTIAL_SORTING_THROUGH_EXCHANGE,
    PUSH_PARTIAL_LIMIT_THROUGH_EXCHANGE,

    REMOVE_REDUNDANT_FILTER,
    REMOVE_REDUNDANT_UNION,
    REMOVE_REDUNDANT_PROJECTION,
    REMOVE_REDUNDANT_CROSS_JOIN,
    REMOVE_REDUNDANT_OUTER_JOIN,
    REMOVE_REDUNDANT_JOIN,
    REMOVE_REDUNDANT_LIMIT,
    REMOVE_REDUNDANT_AGGREGATE,
    REMOVE_REDUNDANT_ENFORCE_SINGLE_ROW,
    REMOVE_READ_NOTHING,

    PUSH_AGG_THROUGH_OUTER_JOIN,

    JOIN_ENUM_ON_GRAPH,
    INNER_JOIN_COMMUTATION,
    PULL_Left_JOIN_THROUGH_INNER_JOIN,
    PULL_Left_JOIN_PROJECTION_THROUGH_INNER_JOIN,
    PULL_Left_JOIN_FILTER_THROUGH_INNER_JOIN,

    MAGIC_SET_FOR_AGGREGATION,
    MAGIC_SET_FOR_PROJECTION_AGGREGATION,

    MAGIC_SET_PUSH_THROUGH_FILTER,
    MAGIC_SET_PUSH_THROUGH_JOIN,
    MAGIC_SET_PUSH_THROUGH_PROJECTION,
    MAGIC_SET_PUSH_THROUGH_AGGREGATING,

    INLINE_CTE,

    PUSH_JOIN_THROUGH_UNION,
    PUSH_DYNAMIC_FILTER_BUILDER_THROUGH_EXCHANGE,

    PUSH_LIMIT_INTO_DISTINCT,
    PUSH_LIMIT_THROUGH_PROJECTION,
    PUSH_LIMIT_THROUGH_EXTREMES,
    PUSH_LIMIT_THROUGH_UNION,
    PUSH_LIMIT_THROUGH_OUTER_JOIN,

    PUSH_LIMIT_INTO_TABLE_SCAN,
    PUSH_FILTER_INTO_TABLE_SCAN,

    INNER_JOIN_REORDER,

    MERGE_AGGREGATINGS,
    SWAP_WINDOWS,
    MERGE_PREDICATES_USING_DOMAIN_TRANSLATOR,

    FILTER_WINDOW_TO_PARTITION_TOPN,

    // Implementation
    SET_JOIN_DISTRIBUTION,

    NUM_RULES,

    INITIAL,
    UNDEFINED,

};

class TransformResult;

/**
 * A Rule is used to rewrite a plan node, which consists of
 * 1) what kind of plan node can be accepted by this rule(see `Rule::getPattern`);
 * 2) how to rewrite a plan node(see `Rule::apply`).
 */
class Rule
{
public:
    virtual ~Rule() = default;
    virtual RuleType getType() const = 0;
    virtual String getName() const = 0;
    virtual PatternPtr getPattern() const = 0;
    virtual bool isEnabled(ContextPtr) { return true; }
    virtual const std::vector<RuleType> & blockRules() const
    {
        static std::vector<RuleType> empty;
        return empty;
    }

    IQueryPlanStep::Type getTargetType()
    {
        if (target_type == IQueryPlanStep::Type::UNDEFINED)
        {
            target_type = getPattern()->getTargetType();
        }
        return target_type;
    }

    TransformResult transform(const PlanNodePtr & node, RuleContext & context);

protected:
    // The return value should be either
    // 1. a nullopt, if the rule didn't do any changes on the query plan;
    // 2. a non-empty value with a non-null PlanNodePtr, which is the rewritten plan node of the rule application;
    virtual TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) = 0;

private:
    IQueryPlanStep::Type target_type = IQueryPlanStep::Type::UNDEFINED;
};

class TransformResult final
{
public:
    TransformResult(std::initializer_list<PlanNodePtr> plans_) : plans(plans_) { }

    TransformResult(PlanNodePtr plan_, bool erase_old_ = false, bool erase_all_ = false)
        : plans(PlanNodes{std::move(plan_)}), erase_old(erase_old_), erase_all(erase_all_)
    {
    }

    explicit TransformResult(PlanNodes plans_, bool erase_old_ = false, bool erase_all_ = false)
        : plans(std::move(plans_)), erase_old(erase_old_), erase_all(erase_all_)
    {
    }

    static TransformResult of(const std::optional<PlanNodePtr> & plan_);

    const PlanNodes & getPlans() const { return plans; }

    // erase_old indicates that the returned GroupExpr must be better than the old one, so we can remove it from Group.
    bool isEraseOld() const { return erase_old; }

    // erase_all indicates that the returned GroupExpr must be better than all other candidates in the Group.
    bool isEraseAll() const { return erase_all; }

    bool empty() const { return plans.empty(); }

private:
    PlanNodes plans;
    bool erase_old = false;
    bool erase_all = false;
};

}
