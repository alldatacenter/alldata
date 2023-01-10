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

#include <Optimizer/ImplementSetOperation.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Interpreters/ExpressionActions.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/UnionStep.h>

namespace DB
{
String SetOperationNodeTranslator::MARKER = "marker";

TranslationResult SetOperationNodeTranslator::makeSetContainmentPlanForDistinct(PlanNodeBase & node)
{
    Names markers = allocateSymbols(node.getChildren().size(), MARKER);
    // identity projection for all the fields in each of the sources plus marker columns
    PlanNodes with_markers = appendMarkers(markers, node.getChildren(), node.shared_from_this());

    // add a union over all the rewritten sources. The outputs of the union have the same name as the
    // original intersect node
    NamesAndTypes output;
    for (const auto & item : node.getStep()->getOutputStream().header)
    {
        output.emplace_back(item.name, item.type);
    }
    for (auto & marker : markers)
    {
        output.emplace_back(marker, std::make_shared<DataTypeUInt8>());
    }
    PlanNodePtr union_node = unionNodes(with_markers, output);

    // add count aggregations
    auto aggregation_outputs = allocateSymbols(markers.size(), "sum");


    Names group_by_keys;
    for (const auto & item : node.getStep()->getOutputStream().header)
        group_by_keys.emplace_back(item.name);

    AggregateDescriptions aggregates;
    for (size_t i = 0; i < markers.size(); i++)
    {
        AggregateDescription aggregate_desc;
        aggregate_desc.column_name = aggregation_outputs[i];
        aggregate_desc.argument_names = {markers[i]};
        DataTypes types{std::make_shared<DataTypeUInt8>()};
        Array params;
        AggregateFunctionProperties properties;
        aggregate_desc.function = AggregateFunctionFactory::instance().get("sum", types, params, properties);

        aggregates.push_back(aggregate_desc);
    }

    auto agg_step = std::make_shared<AggregatingStep>(union_node->getStep()->getOutputStream(), group_by_keys, aggregates, GroupingSetsParamsList{}, true);
    PlanNodes children{union_node};
    PlanNodePtr agg_node = std::make_shared<AggregatingNode>(context.nextNodeId(), std::move(agg_step), children);

    return TranslationResult{agg_node, aggregation_outputs};
}

PlanNodePtr SetOperationNodeTranslator::unionNodes(const PlanNodes & children, const NamesAndTypes cols)
{
    DataStreams input_stream;
    for (const auto & item : children)
        input_stream.emplace_back(item->getStep()->getOutputStream());
    DataStream output;
    for (const auto & col : cols)
    {
        output.header.insert(ColumnWithTypeAndName{col.type, col.name});
    }
    auto union_step = std::make_unique<UnionStep>(input_stream, output, false);

    PlanNodePtr union_node = std::make_shared<UnionNode>(context.nextNodeId(), std::move(union_step), children);
    return union_node;
}

}
