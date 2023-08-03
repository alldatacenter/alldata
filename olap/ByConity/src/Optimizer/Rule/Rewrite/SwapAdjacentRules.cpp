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

#include <Optimizer/PlanNodeCardinality.h>
#include <Optimizer/Rule/Rewrite/SwapAdjacenRules.h>
#include <Optimizer/Rule/Rule.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/PlanNodeIdAllocator.h>
#include <QueryPlan/WindowStep.h>

namespace DB
{
TransformResult SwapAdjacentWindows::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    if (context.context->getSettingsRef().enable_windows_reorder)
    {
        auto & step = dynamic_cast<const WindowStep &>(*node->getStep().get());
        PlanNodePtr child_ptr = node->getChildren()[0];

        if (child_ptr->getStep()->getType() != IQueryPlanStep::Type::Window)
            return {};

        auto & partition_scheme = step.getWindow().partition_by;
        std::vector<std::string> partition_keys;

        for (const auto & sort_column_description : partition_scheme)
        {
            partition_keys.emplace_back(sort_column_description.column_name);
        }

        std::vector<std::string> child_partition_keys;
        auto & child_step = dynamic_cast<const WindowStep &>(*child_ptr->getStep());
        auto & child_partition_scheme = child_step.getWindow().partition_by;

        for (const auto & sort_column_description : child_partition_scheme)
        {
            child_partition_keys.emplace_back(sort_column_description.column_name);
        }

        auto partition_len = partition_keys.size();
        size_t iterator_index = 0;
        auto child_partition_len = child_partition_keys.size();
        bool re_order = false;

        while (iterator_index < partition_len && iterator_index < child_partition_len)
        {
            if (partition_keys[iterator_index] < child_partition_keys[iterator_index] && !re_order)
            {
                re_order = true;
            }
            else if (partition_keys[iterator_index] > child_partition_keys[iterator_index] && !re_order)
            {
                return {};
            }

            iterator_index++;
        }

        if (!re_order && partition_len >= child_partition_len)
        {
            return {};
        }

        auto & sort_scheme = step.getWindow().order_by;
        std::vector<std::string> order_keys;

        for (auto & sort_column_description : sort_scheme)
        {
            order_keys.emplace_back(sort_column_description.column_name);
        }

        std::vector<std::string> window_function_arguments;

        for (auto & window_function_description : step.getWindow().window_functions)
        {
            for (auto & window_function_argument : window_function_description.argument_names)
            {
                window_function_arguments.emplace_back(window_function_argument);
            }
        }

        std::unordered_set<std::string> child_get_created_symbol_set;

        for (auto & window_function_description : child_step.getWindow().window_functions)
        {
            child_get_created_symbol_set.insert(window_function_description.column_name);
        }

        for (auto & partition_key : partition_keys)
        {
            if (child_get_created_symbol_set.count(partition_key))
            {
                return {};
            }
        }

        for (auto & order_key : order_keys)
        {
            if (child_get_created_symbol_set.count(order_key))
            {
                return {};
            }
        }

        for (auto & argument : window_function_arguments)
        {
            if (child_get_created_symbol_set.count(argument))
            {
                return {};
            }
        }

        QueryPlanStepPtr new_child_step = std::make_shared<WindowStep>(child_step.getInputStreams()[0], step.getWindow(), step.needSort());
        QueryPlanStepPtr new_step
            = std::make_shared<WindowStep>(new_child_step->getOutputStream(), child_step.getWindow(), child_step.needSort());

        auto new_child_node
            = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(new_child_step), child_ptr->getChildren());
        auto new_node = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(new_step), PlanNodes{new_child_node});

        return new_node;
    }

    return {};
}

}
