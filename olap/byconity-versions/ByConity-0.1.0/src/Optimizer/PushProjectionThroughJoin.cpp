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

#include <Optimizer/PushProjectionThroughJoin.h>

#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/Rule/Rewrite/InlineProjections.h>
#include <Optimizer/SymbolUtils.h>
#include <Optimizer/SymbolsExtractor.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/QueryPlan.h>

namespace DB
{
std::optional<PlanNodePtr> PushProjectionThroughJoin::pushProjectionThroughJoin(ProjectionNode & project, ContextMutablePtr & context)
{
    auto & step = *project.getStep();

    bool all_deterministic = true;
    auto assignments = step.getAssignments();
    const NameToType & name_to_type = step.getNameToType();
    for (auto & assigment : assignments)
    {
        if (!ExpressionDeterminism::isDeterministic(assigment.second, context))
        {
            all_deterministic = false;
        }
    }
    if (!all_deterministic)
    {
        return {};
    }

    auto * join_ptr = dynamic_cast<JoinNode *>(project.getChildren()[0].get());
    if (!join_ptr)
    {
        return {};
    }

    auto & join_step = *join_ptr->getStep();
    //    if (join_step.getKind() != ASTTableJoin::Kind::Inner && join_step.getKind() != ASTTableJoin::Kind::Cross)
    //    {
    //        return {};
    //    }
    bool can_project_left = join_step.getKind() == ASTTableJoin::Kind::Inner || join_step.getKind() == ASTTableJoin::Kind::Left
        || join_step.getKind() == ASTTableJoin::Kind::Cross;
    bool can_project_right = join_step.getKind() == ASTTableJoin::Kind::Inner || join_step.getKind() == ASTTableJoin::Kind::Right
        || join_step.getKind() == ASTTableJoin::Kind::Cross;

    PlanNodePtr join_left = join_ptr->getChildren()[0];
    PlanNodePtr join_right = join_ptr->getChildren()[1];
    const auto & join_left_header = join_left->getStep()->getOutputStream().header;
    const auto & join_right_header = join_right->getStep()->getOutputStream().header;

    std::vector<String> project_output_symbols;
    for (const auto & column : step.getOutputStream().header)
    {
        project_output_symbols.emplace_back(column.name);
    }
    std::vector<String> join_left_output_symbols;
    for (const auto & column : join_left_header)
    {
        join_left_output_symbols.emplace_back(column.name);
    }
    std::vector<String> join_right_output_symbols;
    for (const auto & column : join_right_header)
    {
        join_right_output_symbols.emplace_back(column.name);
    }

    Assignments left_assignments;
    NameToType left_name_to_type;
    Assignments right_assignments;
    NameToType right_name_to_type;
    for (auto & assignment : assignments)
    {
        auto expression = assignment.second;
        std::set<String> symbols = SymbolsExtractor::extract(expression);

        bool is_identity = false;
        if (const auto * identifier = expression->as<ASTIdentifier>())
        {
            if (assignment.first == identifier->name())
            {
                is_identity = true;
            }
        }
        // expression is satisfied with left child symbols
        if (SymbolUtils::containsAll(join_left_output_symbols, symbols))
        {
            if (!can_project_left && !is_identity)
            {
                return {};
            }
            if (!left_name_to_type.contains(assignment.first))
            {
                left_assignments.emplace_back(assignment.first, expression);
                left_name_to_type[assignment.first] = name_to_type.at(assignment.first);
            }
        }
        // expression is satisfied with right child symbols
        else if (SymbolUtils::containsAll(join_right_output_symbols, symbols))
        {
            if (!can_project_right && !is_identity)
            {
                return {};
            }
            if (!right_name_to_type.contains(assignment.first))
            {
                right_assignments.emplace_back(assignment.first, expression);
                right_name_to_type[assignment.first] = name_to_type.at(assignment.first);
            }
        }
        else
        {
            // expression is using symbols from both join sides
            return {};
        }
    }

    // add projections for symbols required by the join itself
    std::set<String> join_required_symbols = getJoinRequiredSymbols(*join_ptr);
    for (const auto & required_symbol : join_required_symbols)
    {
        for (const auto & header : join_left_header)
        {
            if (header.name == required_symbol)
            {
                if (!left_name_to_type.contains(required_symbol))
                {
                    left_assignments.emplace_back(
                        std::pair<String, ASTPtr>{required_symbol, std::make_shared<ASTIdentifier>(required_symbol)});
                    left_name_to_type[required_symbol] = header.type;
                }
            }
        }
        for (const auto & header : join_right_header)
        {
            if (header.name == required_symbol)
            {
                if (!right_name_to_type.contains(required_symbol))
                {
                    right_assignments.emplace_back(
                        std::pair<String, ASTPtr>{required_symbol, std::make_shared<ASTIdentifier>(required_symbol)});
                    right_name_to_type[required_symbol] = header.type;
                }
            }
        }
    }

    PlanNodePtr left_expression_step_inline;
    if (left_assignments.empty())
    {
        left_expression_step_inline = join_left;
    }
    else
    {
        auto left_expression_step
            = std::make_shared<ProjectionStep>(join_left->getStep()->getOutputStream(), left_assignments, left_name_to_type);
        PlanNodePtr left_expression_node
            = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(left_expression_step), PlanNodes{join_left});
        left_expression_step_inline = inlineProjections(left_expression_node, context);
    }


    PlanNodePtr right_expression_step_inline;
    if (right_assignments.empty())
    {
        right_expression_step_inline = join_right;
    }
    else
    {
        auto right_expression_step
            = std::make_shared<ProjectionStep>(join_right->getStep()->getOutputStream(), right_assignments, right_name_to_type);
        PlanNodePtr right_expression_node
            = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(right_expression_step), PlanNodes{join_right});
        right_expression_step_inline = inlineProjections(right_expression_node, context);
    }


    const DataStream & left_data_stream = left_expression_step_inline->getStep()->getOutputStream();
    const DataStream & right_data_stream = right_expression_step_inline->getStep()->getOutputStream();
    DataStreams streams = {left_data_stream, right_data_stream};

    auto left_header = left_data_stream.header;
    auto right_header = right_data_stream.header;
    NamesAndTypes output;
    for (const auto & item : left_header)
    {
        output.emplace_back(NameAndTypePair{item.name, item.type});
    }
    for (const auto & item : right_header)
    {
        output.emplace_back(NameAndTypePair{item.name, item.type});
    }

    auto new_join_step = std::make_shared<JoinStep>(
        streams,
        DataStream{.header = step.getOutputStream().header},
        join_step.getKind(),
        join_step.getStrictness(),
        join_step.getLeftKeys(),
        join_step.getRightKeys(),
        join_step.getFilter(),
        join_step.isHasUsing(),
        join_step.getRequireRightKeys(),
        join_step.getAsofInequality(),
        join_step.getDistributionType(),
        join_step.isMagic());
    PlanNodePtr new_join_node = std::make_shared<JoinNode>(
        context->nextNodeId(), std::move(new_join_step), PlanNodes{left_expression_step_inline, right_expression_step_inline});

    return std::make_optional<PlanNodePtr>(new_join_node);
}

PlanNodePtr PushProjectionThroughJoin::inlineProjections(PlanNodePtr parent_projection, ContextMutablePtr & context)
{
    PlanNodePtr child = parent_projection->getChildren()[0];
    if (child->getStep()->getType() != IQueryPlanStep::Type::Projection)
    {
        return parent_projection;
    }
    auto result = InlineProjections::inlineProjections(parent_projection, child, context);
    if (result.has_value())
    {
        return inlineProjections(result.value(), context);
    }
    else
    {
        return parent_projection;
    }
}

std::set<String> PushProjectionThroughJoin::getJoinRequiredSymbols(JoinNode & node)
{
    // extract symbols required by the join itself
    std::set<String> join_symbols;

    const auto & step = *node.getStep();

    for (auto & key : step.getLeftKeys())
    {
        join_symbols.emplace(key);
    }
    for (auto & key : step.getRightKeys())
    {
        join_symbols.emplace(key);
    }
    auto filter = step.getFilter()->clone();
    std::set<String> filter_symbols = SymbolsExtractor::extract(filter);
    join_symbols.insert(filter_symbols.begin(), filter_symbols.end());
    return join_symbols;
}

}
