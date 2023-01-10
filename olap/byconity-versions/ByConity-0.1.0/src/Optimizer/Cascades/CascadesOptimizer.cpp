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

#include <Optimizer/Cascades/CascadesOptimizer.h>

#include <Interpreters/DistributedStages/PlanSegmentSplitter.h>
#include <Optimizer/Cascades/Task.h>
#include <Optimizer/Rule/Implementation/SetJoinDistribution.h>
#include <Optimizer/Rule/Rewrite/InlineProjections.h>
#include <Optimizer/Rule/Transformation/InlineCTE.h>
#include <Optimizer/Rule/Transformation/InnerJoinCommutation.h>
#include <Optimizer/Rule/Transformation/JoinEnumOnGraph.h>
#include <Optimizer/Rule/Transformation/LeftJoinToRightJoin.h>
#include <Optimizer/Rule/Transformation/MagicSetForAggregation.h>
#include <Optimizer/Rule/Transformation/MagicSetPushDown.h>
#include <Optimizer/Rule/Transformation/PullOuterJoin.h>
#include <QueryPlan/CTERefStep.h>
#include <QueryPlan/GraphvizPrinter.h>
#include <QueryPlan/AnyStep.h>
#include <QueryPlan/PlanPattern.h>
#include <QueryPlan/ReadNothingStep.h>
#include <QueryPlan/ValuesStep.h>
#include <Storages/StorageDistributed.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int OPTIMIZER_TIMEOUT;
}

void CascadesOptimizer::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    CascadesContext cascades_context{
        context, plan.getCTEInfo(), WorkerSizeFinder::find(plan, *context), PlanPattern::maxJoinSize(plan, context)};

    auto start = std::chrono::high_resolution_clock::now();
    auto root = cascades_context.initMemo(plan.getPlanNode());

    auto root_id = root->getGroupId();
    auto single = Property{Partitioning{Partitioning::Handle::SINGLE}};

    auto actual_property = optimize(root_id, cascades_context, single);
    LOG_DEBUG(cascades_context.getLog(), cascades_context.getInfo());
    GraphvizPrinter::printMemo(cascades_context.getMemo(), context, GraphvizPrinter::MEMO_PATH);
    GraphvizPrinter::printMemo(cascades_context.getMemo(), root_id, context, GraphvizPrinter::MEMO_GRAPH_PATH);

    auto result = buildPlanNode(root_id, cascades_context, single);
    for (auto & item : actual_property.getCTEDescriptions())
    {
        auto cte_id = item.first;
        auto cte_def_group = cascades_context.getMemo().getCTEDefGroupByCTEId(cte_id);
        auto cte_property = CTEDescription::createCTEDefGlobalProperty(actual_property, cte_id, cte_def_group->getCTESet());
        auto cte = buildPlanNode(cte_def_group->getId(), cascades_context, cte_property);
        plan.getCTEInfo().update(cte_id, cte);
    }

    auto end = std::chrono::high_resolution_clock::now();
    auto ms_int = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    LOG_DEBUG(cascades_context.getLog(), "Cascades use {} ms", ms_int.count());

    plan.update(result);
}

Property CascadesOptimizer::optimize(GroupId root_group_id, CascadesContext & context, const Property & required_prop)
{
    auto root_context = std::make_shared<OptimizationContext>(context, required_prop);
    auto root_group = context.getMemo().getGroupById(root_group_id);
    context.getTaskStack().push(std::make_shared<OptimizeGroup>(root_group, root_context));

    auto start_time = std::chrono::system_clock::now();
    while (!context.getTaskStack().empty())
    {
        auto task = context.getTaskStack().top();
        context.getTaskStack().pop();
        task->execute();

        // Check to see if we have at least one plan, and if we have exceeded our
        // timeout limit
        auto now = std::chrono::system_clock::now();
        UInt64 elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - start_time).count();
        if (elapsed >= context.getTaskExecutionTimeout())
        {
            GraphvizPrinter::printMemo(context.getMemo(), root_group->getId(), context.getContext(), GraphvizPrinter::MEMO_GRAPH_PATH);
            throw Exception(
                "Cascades exhausted the time limit of " + std::to_string(context.getTaskExecutionTimeout()) + " ms",
                ErrorCodes::OPTIMIZER_TIMEOUT);
        }
    }

    return root_group->getBestExpression(required_prop)->getActual();
}


PlanNodePtr CascadesOptimizer::buildPlanNode(GroupId root, CascadesContext & context, const Property & required_prop) // NOLINT(misc-no-recursion)
{
    auto group = context.getMemo().getGroupById(root);
    auto winner = group->getBestExpression(required_prop);

    auto input_properties = winner->getRequireChildren();

    PlanNodes children;
    for (size_t index = 0; index < input_properties.size(); ++index)
    {
        auto child = buildPlanNode(winner->getGroupExpr()->getChildrenGroups()[index], context, input_properties[index]);
        children.emplace_back(child);
    }

    return winner->buildPlanNode(context, children);
}

GroupExprPtr CascadesContext::initMemo(const PlanNodePtr & plan_node)
{
    PlanNodes nodes;
    std::queue<PlanNodePtr> queue;
    queue.push(plan_node);

    while (!queue.empty())
    {
        auto node = queue.front();
        for (const auto & child : node->getChildren())
        {
            queue.push(child);
        }
        if (auto read_step = dynamic_cast<const CTERefStep *>(node->getStep().get()))
        {
            if (!memo.containsCTEId(read_step->getId()))
            {
                auto cte_expr = initMemo(cte_info.getCTEDef(read_step->getId()));
                memo.recordCTEDefGroupId(read_step->getId(), cte_expr->getGroupId());
                node->setStatistics(memo.getGroupById(cte_expr->getGroupId())->getStatistics());
            }
        }
        queue.pop();
    }

    GroupExprPtr root_expr;
    recordPlanNodeIntoGroup(plan_node, root_expr, RuleType::INITIAL);
    return root_expr;
}

bool CascadesContext::recordPlanNodeIntoGroup(
    const PlanNodePtr & plan_node, GroupExprPtr & group_expr, RuleType produce_rule, GroupId target_group)
{
    auto new_group_expr = makeGroupExpression(plan_node, produce_rule);
    group_expr = memo.insertGroupExpr(new_group_expr, *this, target_group);
    // if memo exists the same expr, it will return the old expr
    // so it is not equal, and return false
    return group_expr == new_group_expr;
}

GroupExprPtr CascadesContext::makeGroupExpression(const PlanNodePtr & node, RuleType produce_rule)
{
    std::vector<GroupId> child_groups;
    for (auto & child : node->getChildren())
    {
        if (child->getStep()->getType() == IQueryPlanStep::Type::Any)
        {
            // Special case for LEAF
            const auto * const leaf = dynamic_cast<const AnyStep *>(child->getStep().get());
            auto child_group = leaf->getGroupId();
            child_groups.push_back(child_group);
        }
        else
        {
            // Create a GroupExpression for the child
            auto group_expr = makeGroupExpression(child, produce_rule);

            // Insert into the memo (this allows for duplicate detection)
            auto memo_expr = memo.insertGroupExpr(group_expr, *this);
            if (memo_expr == nullptr)
            {
                // Delete if need to (see InsertExpression spec)
                child_groups.push_back(group_expr->getGroupId());
            }
            else
            {
                child_groups.push_back(memo_expr->getGroupId());
            }
        }
    }
    return std::make_shared<GroupExpression>(node->getStep(), std::move(child_groups), produce_rule);
}

CascadesContext::CascadesContext(ContextMutablePtr context_, CTEInfo & cte_info_, size_t worker_size_, size_t max_join_size_)
    : context(context_)
    , cte_info(cte_info_)
    , worker_size(worker_size_)
    , max_join_size(max_join_size_)
    , support_filter(max_join_size_ <= context->getSettingsRef().max_graph_reorder_size)
    , task_execution_timeout(context->getSettingsRef().cascades_optimizer_timeout)
    , log(&Poco::Logger::get("CascadesOptimizer"))
{
    implementation_rules.emplace_back(std::make_shared<SetJoinDistribution>());

    if (max_join_size <= 10)
    {
        transformation_rules.emplace_back(std::make_shared<JoinEnumOnGraph>(support_filter));
    }
    else
    {
        transformation_rules.emplace_back(std::make_shared<InnerJoinCommutation>());
    }

    // left join inner join reorder q78, 80
    transformation_rules.emplace_back(std::make_shared<PullLeftJoinThroughInnerJoin>());
    transformation_rules.emplace_back(std::make_shared<PullLeftJoinProjectionThroughInnerJoin>());
    transformation_rules.emplace_back(std::make_shared<PullLeftJoinFilterThroughInnerJoin>());

    transformation_rules.emplace_back(std::make_shared<LeftJoinToRightJoin>());
    transformation_rules.emplace_back(std::make_shared<MagicSetForAggregation>());
    transformation_rules.emplace_back(std::make_shared<MagicSetForProjectionAggregation>());
    transformation_rules.emplace_back(std::make_shared<MagicSetPushThroughProject>());
    transformation_rules.emplace_back(std::make_shared<MagicSetPushThroughJoin>());
    transformation_rules.emplace_back(std::make_shared<MagicSetPushThroughFilter>());
    transformation_rules.emplace_back(std::make_shared<MagicSetPushThroughAggregating>());

    transformation_rules.emplace_back(std::make_shared<InlineProjections>());

    transformation_rules.emplace_back(std::make_shared<InlineCTE>());
}

size_t WorkerSizeFinder::find(QueryPlan & query_plan, const Context & context)
{
    if (context.getSettingsRef().enable_memory_catalog)
        return context.getSettingsRef().memory_catalog_worker_size;

    WorkerSizeFinder visitor {query_plan.getCTEInfo()};
    Void c{};

    // default schedule to worker cluster
    std::optional<size_t> result = VisitorUtil::accept(query_plan.getPlanNode(), visitor, c);
    if (result.has_value())
        return result.value();
    return 1;
}

std::optional<size_t> WorkerSizeFinder::visitPlanNode(PlanNodeBase & node, Void & context)
{
    for (const auto & child : node.getChildren())
    {
        auto result = VisitorUtil::accept(child, *this, context);
        if (result.has_value())
            return result;
    }
    return std::nullopt;
}

std::optional<size_t> WorkerSizeFinder::visitTableScanNode(TableScanNode & node, Void &)
{
    const auto * source_step = node.getStep().get();
    auto *distributed_table = dynamic_cast<StorageDistributed *>(source_step->getStorage().get());
    if (distributed_table)
        return std::make_optional<size_t>(distributed_table->getShardCount());
    return std::nullopt;
}

std::optional<size_t> WorkerSizeFinder::visitCTERefNode(CTERefNode & node, Void & context)
{
    const auto * step = dynamic_cast<const CTERefStep *>(node.getStep().get());
    return VisitorUtil::accept(cte_info.getCTEDef(step->getId()), *this, context);
}
}
