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

#include <Optimizer/Rewriter/Rewriter.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/SimplePlanRewriter.h>

namespace DB
{

class SetOperationMerge
{
public:
    SetOperationMerge(const PlanNodePtr & node_, Context & context_) : node(node_), context(context_) { }

    /**
     * Merge all matching source nodes. This method is assumed to be used only for associative set operations: UNION and INTERSECT.
     *
     * @return Merged plan node if applied.
     */
    PlanNodePtr merge();

    /**
     * Only merge first source node. This method is assumed to be used for EXCEPT, which is a non-associative set operation.
     * Provides a correct plan transformation also for associative set operations: UNION and INTERSECT.
     *
     * @return Merged plan node if applied.
     */
    PlanNodePtr mergeFirstSource();

private:
    /**
     * Check if node and child are mergable based on their set operation type and quantifier.
     *
     * For parent and child of type UNION, merge is always possible and the assumed quantifier is ALL, because UnionNode always represents UNION ALL.
     *
     * For parent and child of type INTERSECT, merge is always possible:
     * - if parent and child are both INTERSECT ALL, the resulting set operation is INTERSECT ALL
     * - otherwise, the resulting set operation is INTERSECT DISTINCT:
     * - if the parent is DISTINCT, the result has unique values, regardless of whether child branches were DISTINCT or ALL,
     * - if the child is DISTINCT, that branch is guaranteed to have unique values, so at most one element of the other branches will be
     * retained -- this is equivalent to just doing DISTINCT on the parent.
     *
     * For parent and child of type EXCEPT:
     * - if parent is EXCEPT DISTINCT and child is EXCEPT ALL, merge is not possible
     * - if parent and child are both EXCEPT DISTINCT, the resulting set operation is EXCEPT DISTINCT
     * - if parent and child are both EXCEPT ALL, the resulting set operation is EXCEPT ALL
     * - if parent is EXCEPT ALL and child is EXCEPT DISTINCT, the resulting set operation is EXCEPT DISTINCT
     *
     * Optional.empty() indicates that merge is not possible.
     */
    static std::optional<bool> mergedQuantifierIsDistinct(const PlanNodePtr & node, const PlanNodePtr & child)
    {
        if (node->getType() != child->getType())
        {
            return {};
        }

        if (node->getType() == IQueryPlanStep::Type::Union)
        {
            return false;
        }

        if (node->getType() == IQueryPlanStep::Type::Intersect)
        {
            if (!dynamic_cast<const IntersectStep *>(node->getStep().get())->isDistinct()
                && !dynamic_cast<const IntersectStep *>(child->getStep().get())->isDistinct())
            {
                return false;
            }
            return true;
        }

        if (dynamic_cast<const ExceptStep *>(node->getStep().get())->isDistinct()
            && !dynamic_cast<const ExceptStep *>(child->getStep().get())->isDistinct())
        {
            return {};
        }
        return dynamic_cast<const ExceptStep *>(child->getStep().get())->isDistinct();
    }

    void
    addMergedMappings(const PlanNodePtr & child, int child_index, std::unordered_map<String, std::vector<String>> & new_output_to_inputs)
    {
        auto & children = child->getChildren();
        new_sources.insert(new_sources.end(), children.begin(), children.end());

        auto step_ptr = node->getStep();
        auto & step = dynamic_cast<const SetOperationStep &>(*step_ptr);
        const std::unordered_map<String, std::vector<String>> & output_to_input = step.getOutToInputs();

        auto child_step_ptr = child->getStep();
        auto & child_step = dynamic_cast<const SetOperationStep &>(*child_step_ptr);
        const std::unordered_map<String, std::vector<String>> & child_output_to_input = child_step.getOutToInputs();

        for (auto & mapping : output_to_input)
        {
            String output = mapping.first;
            const std::vector<String> & inputs = mapping.second;
            String input = inputs[child_index];

            const std::vector<String> & child_inputs = child_output_to_input.at(input);
            if (new_output_to_inputs.contains(output))
            {
                std::vector<String> & tmp = new_output_to_inputs.at(output);
                tmp.insert(tmp.end(), child_inputs.begin(), child_inputs.end());
            }
            else
            {
                new_output_to_inputs[output] = child_inputs;
            }
        }
    }

    void
    addOriginalMappings(const PlanNodePtr & child, int child_index, std::unordered_map<String, std::vector<String>> & new_output_to_inputs)
    {
        new_sources.emplace_back(child);

        auto step_ptr = node->getStep();
        auto & step = dynamic_cast<const SetOperationStep &>(*step_ptr);
        const std::unordered_map<String, std::vector<String>> & output_to_input = step.getOutToInputs();

        for (auto & mapping : output_to_input)
        {
            String output = mapping.first;
            const std::vector<String> & inputs = mapping.second;
            String input = inputs[child_index];
            if (new_output_to_inputs.contains(output))
            {
                std::vector<String> & tmp = new_output_to_inputs.at(output);
                tmp.emplace_back(input);
            }
            else
            {
                new_output_to_inputs[output] = std::vector<String>{input};
            }
        }
    }

    PlanNodePtr node;
    Context & context;
    PlanNodes new_sources;
};

}
