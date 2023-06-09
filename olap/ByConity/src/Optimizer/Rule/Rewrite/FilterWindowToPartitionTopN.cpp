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

#include <Core/Names.h>
#include <Optimizer/ExpressionInterpreter.h>
#include <Optimizer/PlanNodeCardinality.h>
#include <Optimizer/Rule/Rewrite/FilterWindowToPartitionTopN.h>
#include <Optimizer/Rule/Rule.h>
#include <Parsers/ASTFunction.h>
#include <Processors/Transforms/PartitionTopNTransform.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/PartitionTopNStep.h>
#include <QueryPlan/PlanNode.h>
#include <common/types.h>

namespace DB
{
TransformResult FilterWindowToPartitionTopN::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    if (!context.context->getSettingsRef().enable_filter_window_to_partition_topn)
        return {};

    auto * filter_node = dynamic_cast<FilterNode *>(node.get());
    const auto & step = *filter_node->getStep();
    const auto & predicate = step.getFilter();

    auto * window_node = dynamic_cast<WindowNode *>(node->getChildren()[0].get());
    const auto & window_step = *window_node->getStep();

    auto * exchange_node = dynamic_cast<ExchangeNode *>(window_node->getChildren()[0].get());
    const auto & exchange_step = *exchange_node->getStep();

    if (dynamic_cast<PartitionTopNNode *>(exchange_node->getChildren()[0].get()))
    {
        return {};
    }


    if (const auto * func = predicate->as<ASTFunction>())
    {
        if (func->name == "less" || func->name == "lessOrEquals")
        {
            auto symbol = func->arguments->getChildren()[0];
            auto field_with_type
                = ExpressionInterpreter::evaluateConstantExpression(func->arguments->getChildren()[1], step.getInputStreams()[0].header.getNamesToTypes(), context.context);

            if (symbol->as<ASTIdentifier>() && field_with_type.has_value())
            {
                UInt64 limit;
                if (field_with_type->second.tryGet(limit))
                {
                    const auto & window_desc = window_step.getWindow();
                    if (window_desc.window_functions.size() == 1)
                    {
                        auto window_func = window_desc.window_functions[0];

                        static std::map<String, PartitionTopNModel> funcs{
                            {"row_number", RowNumber}, {"rank", RANKER}, {"dense_rank", DENSE_RANK}};
                        auto func_name = window_func.function_node->name;
                        if (window_func.column_name == symbol->getColumnName() && funcs.contains(func_name) && !window_desc.order_by.empty()
                            && !window_desc.partition_by.empty())
                        {
                            Names partition;
                            for (auto part : window_desc.partition_by)
                            {
                                partition.emplace_back(part.column_name);
                            }

                            Names order_by;
                            for (auto part : window_desc.order_by)
                            {
                                order_by.emplace_back(part.column_name);
                                if (part.direction == 1)
                                {
                                    return {};
                                }
                            }

                            auto before_exchange_sort = std::make_unique<PartitionTopNStep>(
                                exchange_node->getChildren()[0]->getCurrentDataStream(), partition, order_by, limit, funcs[func_name]);
                            auto before_exchange_sort_node = PlanNodeBase::createPlanNode(
                                context.context->nextNodeId(), std::move(before_exchange_sort), {exchange_node->getChildren()[0]});

                            auto new_exchange_step = exchange_step.copy(context.context);
                            auto new_exchange_node = PlanNodeBase::createPlanNode(
                                context.context->nextNodeId(), std::move(new_exchange_step), {before_exchange_sort_node});

                            QueryPlanStepPtr new_window = window_step.copy(context.context);
                            auto new_window_node
                                = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(new_window), {new_exchange_node});

                            QueryPlanStepPtr new_filter = step.copy(context.context);
                            auto new_filter_node
                                = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(new_filter), {new_window_node});

                            return new_filter_node;
                        }
                    }
                }
            }
        }
    }


    return {};
}

}
