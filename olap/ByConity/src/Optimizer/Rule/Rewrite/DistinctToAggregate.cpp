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

#include <Optimizer/Rule/Rewrite/DistinctToAggregate.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Optimizer/Rule/Patterns.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/DistinctStep.h>

namespace DB
{
PatternPtr DistinctToAggregate::getPattern() const
{
    return Patterns::distinct();
}

TransformResult DistinctToAggregate::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    if (!rule_context.context->getSettingsRef().enable_distinct_to_aggregate)
    {
        return {};
    }

    auto * distinct_node = dynamic_cast<DistinctNode *>(node.get());
    if (!distinct_node)
        return {};

    const auto & step = *distinct_node->getStep();

    if (step.getLimitHint() == 0)
    {
        NameSet name_set{step.getColumns().begin(), step.getColumns().end()};
        NamesAndTypes arbitrary_names;

        // check decimal type, which is not support for group by columns
        bool has_decimal_type = false;
        for (const auto & column : node->getStep()->getOutputStream().header)
        {
            TypeIndex index = column.type->getTypeId();
            if (index == TypeIndex::Decimal32 || index == TypeIndex::Decimal64 || index == TypeIndex::Decimal128)
            {
                has_decimal_type = true;
                break;
            }
            if (!name_set.contains(column.name))
                arbitrary_names.emplace_back(column.name, column.type);
        }
        if (has_decimal_type)
        {
            return {};
        }

        AggregateDescriptions descriptions;
        for (auto & name_and_type : arbitrary_names)
        {
            // for rare case, distinct columns don't contain all columns outputs.
            AggregateDescription aggregate_desc;
            aggregate_desc.column_name = name_and_type.name;
            aggregate_desc.argument_names = {name_and_type.name};
            AggregateFunctionProperties properties;
            Array parameters;
            aggregate_desc.function = AggregateFunctionFactory::instance().get("any", {name_and_type.type}, parameters, properties);
            descriptions.emplace_back(aggregate_desc);
        }
        auto group_agg_step = std::make_shared<AggregatingStep>(node->getStep()->getOutputStream(), step.getColumns(), descriptions, GroupingSetsParamsList{}, true, GroupingDescriptions{}, false, false);
        auto group_agg_node = PlanNodeBase::createPlanNode(rule_context.context->nextNodeId(), std::move(group_agg_step), node->getChildren());
        return group_agg_node;
    }

    return {};
}

}
