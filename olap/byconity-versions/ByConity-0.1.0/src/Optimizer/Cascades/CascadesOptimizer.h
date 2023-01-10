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

#include <Optimizer/Cascades/GroupExpression.h>
#include <Optimizer/Cascades/Memo.h>
#include <Optimizer/Property/Property.h>
#include <Optimizer/Rewriter/Rewriter.h>
#include <Optimizer/Rule/Rule.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/CTEVisitHelper.h>

#include <stack>

namespace DB
{
class CascadesContext;

class OptimizerTask;
using OptimizerTaskPtr = std::shared_ptr<OptimizerTask>;
using TaskStack = std::stack<OptimizerTaskPtr>;
using CTEDefPropertyRequirements = std::unordered_map<CTEId, std::unordered_set<Property, PropertyHash>>;

class OptimizationContext;
using OptContextPtr = std::shared_ptr<OptimizationContext>;

class CascadesOptimizer : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;

    String name() const override { return "CascadesOptimizer"; }
    static Property optimize(GroupId root, CascadesContext & context, const Property & required_prop);
    static PlanNodePtr buildPlanNode(GroupId root, CascadesContext & context, const Property & required_prop);
};

class CascadesContext
{
public:
    explicit CascadesContext(ContextMutablePtr context_, CTEInfo & cte_info, size_t worker_size_, size_t max_join_size_);

    GroupExprPtr initMemo(const PlanNodePtr & plan_node);

    bool recordPlanNodeIntoGroup(
        const PlanNodePtr & plan_node,
        GroupExprPtr & group_expr,
        RuleType produce_rule = RuleType::UNDEFINED,
        GroupId target_group = UNDEFINED_GROUP);
    GroupExprPtr makeGroupExpression(const PlanNodePtr & plan_node, RuleType produce_rule = RuleType::UNDEFINED);

    ContextMutablePtr getContext() const { return context; }
    TaskStack & getTaskStack() { return task_stack; }
    Memo & getMemo() { return memo; }
    size_t getWorkerSize() const { return worker_size; }
    Poco::Logger * getLog() const { return log; }
    bool isSupportFilter() const { return support_filter; }
    CTEInfo & getCTEInfo() { return cte_info; }
    CTEDefPropertyRequirements & getCTEDefPropertyRequirements() { return cte_property_requirements; }

    const std::vector<RulePtr> & getTransformationRules() const { return transformation_rules; }
    const std::vector<RulePtr> & getImplementationRules() const { return implementation_rules; }

    String getInfo() const
    {
        std::stringstream ss;

        ss << "Group: " << memo.getGroups().size() << '\n';

        size_t total_logical_expr = 0;
        size_t total_physical_expr = 0;
        for (auto & group : memo.getGroups())
        {
            total_logical_expr += group->getLogicalExpressions().size();
            total_physical_expr += group->getPhysicalExpressions().size();
        }
        ss << "Logical Expr: " << total_logical_expr << '\n';
        ss << "Physical Expr: " << total_physical_expr << '\n';


        return ss.str();
    }

    UInt64 getTaskExecutionTimeout() const { return task_execution_timeout; }

private:
    ContextMutablePtr context;
    CTEInfo & cte_info;
    CTEDefPropertyRequirements cte_property_requirements;
    TaskStack task_stack;
    Memo memo;
    std::vector<RulePtr> transformation_rules;
    std::vector<RulePtr> implementation_rules;
    size_t worker_size = 1;
    size_t max_join_size;
    bool support_filter;
    UInt64 task_execution_timeout;
    Poco::Logger * log;
};

class OptimizationContext
{
public:
    OptimizationContext(
        CascadesContext & context_, const Property & required_prop_, double cost_upper_bound_ = std::numeric_limits<double>::max())
        : context(context_), required_prop(required_prop_), cost_upper_bound(cost_upper_bound_)
    {
    }

    const Property & getRequiredProp() const { return required_prop; }
    double getCostUpperBound() const { return cost_upper_bound; }
    void setCostUpperBound(double cost_upper_bound_) { cost_upper_bound = cost_upper_bound_; }
    void pushTask(const OptimizerTaskPtr & task) const { context.getTaskStack().push(task); }

    const std::vector<RulePtr> & getTransformationRules() const { return context.getTransformationRules(); }
    const std::vector<RulePtr> & getImplementationRules() const { return context.getImplementationRules(); }

    CascadesContext & getOptimizerContext() const { return context; }
    Memo & getMemo() { return context.getMemo(); }
    PlanNodeId nextNodeId() { return context.getContext()->nextNodeId(); }

private:
    CascadesContext & context;

    /**
     * Required properties
     */
    Property required_prop;

    /**
     * Cost Upper Bound (for pruning)
     */
    double cost_upper_bound;
};

class WorkerSizeFinder : public PlanNodeVisitor<std::optional<size_t>, Void>
{
public:
    static size_t find(QueryPlan & query_plan, const Context & context);
    std::optional<size_t> visitPlanNode(PlanNodeBase & node, Void & context) override;
    std::optional<size_t> visitTableScanNode(TableScanNode & node, Void & context) override;
    std::optional<size_t> visitCTERefNode(CTERefNode & node, Void & context) override;

private:
    explicit WorkerSizeFinder(CTEInfo & cte_info_) : cte_info(cte_info_) { }
    CTEInfo & cte_info;
};
}
