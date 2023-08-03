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

#include <Optimizer/Rule/Rewrite/RemoveRedundantRules.h>

#include <DataTypes/DataTypeNullable.h>
#include <Optimizer/ExpressionInterpreter.h>
#include <Optimizer/PlanNodeCardinality.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/ReadNothingStep.h>
#include <QueryPlan/UnionStep.h>

namespace DB
{
TransformResult RemoveRedundantFilter::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;
    const auto * step = dynamic_cast<const FilterStep *>(node->getStep().get());
    auto expr = step->getFilter();

    if (const auto * literal = expr->as<ASTLiteral>())
    {
        const auto & input_columns = step->getInputStreams()[0].header;
        auto result = ExpressionInterpreter::evaluateConstantExpression(expr, input_columns.getNamesToTypes(), context);
        if (result.has_value() && result->second.isNull())
        {
            auto null_step = std::make_unique<ReadNothingStep>(step->getOutputStream().header);
            auto null_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(null_step));
            return {null_node};
        }

        UInt64 value;
        if (literal->value.tryGet(value) && value == 0)
        {
            auto null_step = std::make_unique<ReadNothingStep>(step->getOutputStream().header);
            auto null_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(null_step));
            return {null_node};
        }
        if (literal->value.tryGet(value) && value == 1)
        {
            return node->getChildren()[0];
        }
    }

    return {};
}

TransformResult RemoveRedundantUnion::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;
    const auto * step = dynamic_cast<const UnionStep *>(node->getStep().get());

    DataStreams inputs;
    PlanNodes children;
    for (auto & child : node->getChildren())
    {
        if (!dynamic_cast<const ReadNothingStep *>(child->getStep().get()))
        {
            inputs.emplace_back(child->getStep()->getOutputStream());
            children.emplace_back(child);
        }
    }

    if (children.empty())
    {
        auto null_step = std::make_unique<ReadNothingStep>(step->getOutputStream().header);
        return PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(null_step));
    }

    // local union equals to local gather (make thread single), can't rewrite to projection.
    if (children.size() == 1 && !step->isLocal())
    {
        auto input_columns = children[0]->getStep()->getOutputStream().header;
        Assignments assignments;
        NameToType name_to_type;
        const auto & output_to_inputs = step->getOutToInputs();
        for (const auto & output_to_input : output_to_inputs)
        {
            String output = output_to_input.first;
            for (const auto & input : output_to_input.second)
            {
                for (auto & input_column : input_columns)
                {
                    if (input == input_column.name)
                    {
                        Assignment column{output, std::make_shared<ASTIdentifier>(input_column.name)};
                        assignments.emplace_back(column);
                        name_to_type[output] = input_column.type;
                    }
                }
            }
        }
        auto project_step = std::make_shared<ProjectionStep>(children[0]->getStep()->getOutputStream(), assignments, name_to_type);
        return PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(project_step), children, node->getStatistics());
    }

    if (children.size() != node->getChildren().size())
    {
        auto union_step = std::make_unique<UnionStep>(inputs, step->getOutputStream(), step->isLocal());
        return PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(union_step), children, node->getStatistics());
    }

    return {};
}

TransformResult RemoveRedundantProjection::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    auto * projection_node = dynamic_cast<ProjectionNode *>(node.get());
    if (!projection_node)
        return {};
    const auto & step = *projection_node->getStep();

    if (Utils::isIdentity(step))
    {
        const DataStream & output_stream = step.getOutputStream();
        const DataStream & source_output_stream = node->getChildren()[0]->getStep()->getOutputStream();

        // remove duplicated columns
        std::unordered_set<std::string> output_symbols;
        for (const auto & column : output_stream.header)
        {
            output_symbols.emplace(column.name);
        }
        std::unordered_set<std::string> source_output_symbols;
        for (const auto & column : source_output_stream.header)
        {
            source_output_symbols.emplace(column.name);
        }
        if (output_symbols == source_output_symbols)
        {
            return node->getChildren()[0];
        }
    }

    if (node->getChildren()[0]->getStep()->getType() == IQueryPlanStep::Type::ReadNothing)
    {
        return PlanNodeBase::createPlanNode(
            context.context->nextNodeId(), std::make_shared<ReadNothingStep>(node->getStep()->getOutputStream().header));
    }
    return {};
}

TransformResult RemoveRedundantEnforceSingleRow::transformImpl(PlanNodePtr node, const Captures &, RuleContext &)
{
    if (PlanNodeCardinality::isScalar(*node->getChildren()[0]))
    {
        return node->getChildren()[0];
    }
    return {};
}

PatternPtr RemoveRedundantCrossJoin::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([&](const JoinStep & s) { return s.getKind() == ASTTableJoin::Kind::Cross; })
        ->with({Patterns::any(), Patterns::any()});
}

TransformResult RemoveRedundantCrossJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext &)
{
    // normal case
    if (node->getChildren()[0]->getStep()->getOutputStream().header.columns() == 0
        && PlanNodeCardinality::isScalar(*node->getChildren()[0]))
    {
        return node->getChildren()[1];
    }
    if (node->getChildren()[1]->getStep()->getOutputStream().header.columns() == 0
        && PlanNodeCardinality::isScalar(*node->getChildren()[1]))
    {
        return node->getChildren()[0];
    }

    return {};
}

PatternPtr RemoveReadNothing::getPattern() const
{
    return Patterns::any()->withSingle(Patterns::readNothing());
}

TransformResult RemoveReadNothing::transformImpl(PlanNodePtr, const Captures &, RuleContext &)
{
    return {};
}

PatternPtr RemoveRedundantJoin::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>(
            [&](const JoinStep & s) { return s.getKind() == ASTTableJoin::Kind::Inner || s.getKind() == ASTTableJoin::Kind::Cross; })
        ->withAny(Patterns::readNothing());
}

TransformResult RemoveRedundantJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    if (node->getChildren()[0]->getStep()->getType() == IQueryPlanStep::Type::ReadNothing
        || node->getChildren()[1]->getStep()->getType() == IQueryPlanStep::Type::ReadNothing)
    {
        return PlanNodeBase::createPlanNode(
            context.context->nextNodeId(), std::make_shared<ReadNothingStep>(node->getStep()->getOutputStream().header));
    }
    return {};
}

PatternPtr RemoveRedundantOuterJoin::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>(
            [&](const JoinStep & s) { return s.getKind() == ASTTableJoin::Kind::Left || s.getKind() == ASTTableJoin::Kind::Right; });
}

TransformResult RemoveRedundantOuterJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext &)
{
    auto * join_node = dynamic_cast<JoinNode *>(node.get());

    auto all_type_nothing = [](Block block, Names keys) {
        for (const auto & key : keys)
        {
            auto type = block.getByName(key).type;
            if (type->getTypeId() == TypeIndex::Nothing)
            {
                continue;
            }
            if (type->isNullable())
            {
                if (const auto * nullable_type= dynamic_cast<const DataTypeNullable *>(type.get()))
                {
                    if (nullable_type->getNestedType()->getTypeId() == TypeIndex::Nothing)
                    {
                        continue;
                    }
                }
            }
            return false;
        }
        return true;
    };

    if (join_node)
    {
        auto step = join_node->getStep();
        if (step->getKind() == ASTTableJoin::Kind::Left)
        {
            if (node->getChildren()[1]->getStep()->getType() == IQueryPlanStep::Type::ReadNothing
                || all_type_nothing(node->getChildren()[1]->getStep()->getOutputStream().header, step->getRightKeys()))
            {
                // todo add project to add joined columns
                return node->getChildren()[0];
            }
        }
        if (step->getKind() == ASTTableJoin::Kind::Right)
        {
            if (node->getChildren()[0]->getStep()->getType() == IQueryPlanStep::Type::ReadNothing
                || all_type_nothing(node->getChildren()[0]->getStep()->getOutputStream().header, step->getLeftKeys()))
            {
                // todo add project to add joined columns
                return node->getChildren()[1];
            }
        }
    }
    return {};
}

PatternPtr RemoveRedundantLimit::getPattern() const
{
    return Patterns::limit();
}

TransformResult RemoveRedundantLimit::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    auto * limit_node = dynamic_cast<LimitNode *>(node.get());
    if (limit_node->getStep()->getLimit() == 0)
    {
        auto null_step = std::make_unique<ReadNothingStep>(limit_node->getStep()->getOutputStream().header);
        auto null_node = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(null_step));
        return {null_node};
    }

    return {};
}

PatternPtr RemoveRedundantAggregate::getPattern() const
{
    return Patterns::aggregating();
}

TransformResult RemoveRedundantAggregate::transformImpl(PlanNodePtr, const Captures &, RuleContext &)
{
    return {};
}

}
