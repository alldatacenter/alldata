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

#include <utility>
#include <Optimizer/MergeSetOperation.h>

#include <Interpreters/predicateExpressionsUtils.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/UnionStep.h>


namespace DB
{

/**
 * Transforms:
 *
 * - Intersect
 *   - Intersect
 *     - Relation1
 *     - Relation2
 *   - Intersect
 *     - Relation3
 *     - Relation4
 *
 * Into
 *
 * - Intersect
 *   - Relation1
 *   - Relation2
 *   - Relation3
 *   - Relation4
 *
 */
PlanNodePtr SetOperationMerge::merge()
{
    auto & sources = node->getChildren();

    bool result_is_distinct = false;
    bool rewritten = false;

    std::unordered_map<String, std::vector<String>> output_to_inputs;

    for (size_t i = 0; i < sources.size(); ++i)
    {
        // Determine if set operations can be merged and whether the resulting set operation is quantified DISTINCT or ALL
        auto merged_quantifier = mergedQuantifierIsDistinct(node, sources[i]);
        if (merged_quantifier.has_value())
        {
            addMergedMappings(sources[i], i, output_to_inputs);
            result_is_distinct |= merged_quantifier.value();
            rewritten = true;
        }
        else
        {
            // Keep mapping as it is
            addOriginalMappings(sources[i], i, output_to_inputs);
        }
    }

    if (!rewritten)
    {
        return nullptr;
    }

    DataStreams input_stream;

    for (const auto & item : new_sources)
        input_stream.emplace_back(item->getStep()->getOutputStream());

    DataStream output;
    for (const auto & col : node->getStep()->getOutputStream().header)
    {
        output.header.insert(ColumnWithTypeAndName{col.type, col.name});
    }

    if (node->getStep()->getType() == IQueryPlanStep::Type::Union)
    {
        auto union_step = std::make_unique<UnionStep>(std::move(input_stream), std::move(output), std::move(output_to_inputs));
        PlanNodePtr union_node = std::make_shared<UnionNode>(context.nextNodeId(), std::move(union_step), std::move(new_sources));
        return union_node;
    }

    auto intersect_step = std::make_unique<IntersectStep>(std::move(input_stream), std::move(output), std::move(output_to_inputs), result_is_distinct);
    PlanNodePtr intersect_node = std::make_shared<IntersectNode>(context.nextNodeId(), std::move(intersect_step), std::move(new_sources));
    return intersect_node;
}

PlanNodePtr SetOperationMerge::mergeFirstSource()
{
    auto & sources = node->getChildren();

    auto & child = sources[0];

    // Determine if set operations can be merged and whether the resulting set operation is quantified DISTINCT or ALL
    auto merged_quantifier = mergedQuantifierIsDistinct(node, child);
    if (!merged_quantifier.has_value())
    {
        return {};
    }

    std::unordered_map<String, std::vector<String>> output_to_inputs;

    // Merge all sources of the first source.
    addMergedMappings(child, 0, output_to_inputs);

    // Keep remaining as it is
    for (size_t i = 1; i < sources.size(); i++)
    {
        auto & source = sources[i];
        addOriginalMappings(source, i, output_to_inputs);
    }

    DataStreams input_stream;

    for (const auto & item : new_sources)
        input_stream.emplace_back(item->getStep()->getOutputStream());

    DataStream output;
    for (const auto & col : node->getStep()->getOutputStream().header)
    {
        output.header.insert(ColumnWithTypeAndName{col.type, col.name});
    }

    if (node->getStep()->getType() == IQueryPlanStep::Type::Union)
    {
        auto union_step = std::make_unique<UnionStep>(std::move(input_stream), std::move(output), false);
        PlanNodePtr union_node = std::make_shared<UnionNode>(context.nextNodeId(), std::move(union_step), std::move(new_sources));
        return union_node;
    }
    if (node->getStep()->getType() == IQueryPlanStep::Type::Intersect)
    {
        auto intersect_step = std::make_unique<IntersectStep>(std::move(input_stream), std::move(output), merged_quantifier.value());
        PlanNodePtr intersect_node = std::make_shared<IntersectNode>(context.nextNodeId(), std::move(intersect_step), std::move(new_sources));
        return intersect_node;
    }
    if (node->getStep()->getType() == IQueryPlanStep::Type::Except)
    {
        auto except_step = std::make_unique<ExceptStep>(input_stream, output, merged_quantifier.value());
        PlanNodePtr except_node = std::make_shared<ExceptNode>(context.nextNodeId(), std::move(except_step), std::move(new_sources));
        return except_node;
    }
    return nullptr;
}

}
