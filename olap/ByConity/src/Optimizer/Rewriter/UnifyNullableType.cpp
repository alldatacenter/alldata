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

#include <Optimizer/Rewriter/UnifyNullableType.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Analyzers/TypeAnalyzer.h>
#include <Core/Block.h>
#include <Core/ColumnsWithTypeAndName.h>
#include <Core/NamesAndTypes.h>
#include <DataTypes/DataTypeAggregateFunction.h>
#include <DataTypes/DataTypeNullable.h>
#include <Interpreters/join_common.h>
#include <Optimizer/SymbolsExtractor.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/MergeSortingStep.h>
#include <QueryPlan/PartialSortingStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/WindowStep.h>
#include <Parsers/ASTTablesInSelectQuery.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NOT_IMPLEMENTED;
}

void UnifyNullableType::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    if (!context->getSettingsRef().join_use_nulls)
        return;
    UnifyNullableVisitor visitor{context, plan.getCTEInfo()};
    Void v;
    auto result = VisitorUtil::accept(plan.getPlanNode(), visitor, v);
    plan.update(result);
}

PlanNodePtr UnifyNullableVisitor::visitProjectionNode(ProjectionNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);
    const auto & step = *node.getStep();
    auto assignments = step.getAssignments();
    NameToType set_nullable;
    const auto & input_header = child->getStep()->getOutputStream().header;
    auto type_analyzer = TypeAnalyzer::create(context, input_header.getNamesAndTypes());
    for (auto & assignment : assignments)
    {
        String name = assignment.first;
        ConstASTPtr value = assignment.second;
        DataTypePtr type = type_analyzer.getType(value);
        set_nullable[name] = type;
    }

    auto expression_step = std::make_shared<ProjectionStep>(
        child->getStep()->getOutputStream(), assignments, set_nullable, step.isFinalProject(), step.getDynamicFilters());
    return ProjectionNode::createPlanNode(context->nextNodeId(), std::move(expression_step), PlanNodes{child}, node.getStatistics());
}

PlanNodePtr UnifyNullableVisitor::visitFilterNode(FilterNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);
    const DataStream & input = child->getStep()->getOutputStream();
    const auto & step = *node.getStep();
    // re-create filter step, update it's input/output stream types.
    auto filter_step = std::make_shared<FilterStep>(input, step.getFilter(), step.removesFilterColumn());
    auto filter_node = FilterNode::createPlanNode(context->nextNodeId(), std::move(filter_step), PlanNodes{child}, node.getStatistics());
    return filter_node;
}

PlanNodePtr UnifyNullableVisitor::visitJoinNode(JoinNode & node, Void & v)
{
    PlanNodes children;
    DataStreams inputs;
    for (const auto & item : node.getChildren())
    {
        PlanNodePtr child = VisitorUtil::accept(*item, *this, v);
        children.emplace_back(child);
        inputs.push_back(child->getStep()->getOutputStream());
    }

    const auto & join_step = *node.getStep();

    const DataStreams & input_stream = inputs;
    const DataStream & output_stream = join_step.getOutputStream();

    auto output_set_null = output_stream.header.getNamesAndTypes();
    std::unordered_map<String, DataTypePtr> left_name_to_type;
    std::unordered_map<String, DataTypePtr> right_name_to_type;
    for (const auto & left : input_stream[0].header)
    {
        left_name_to_type[left.name] = left.type;
    }
    for (const auto & right : input_stream[1].header)
    {
        right_name_to_type[right.name] = right.type;
    }

    bool has_outer_join_semantic = isAny(join_step.getStrictness()) || isAll(join_step.getStrictness())
        || join_step.getStrictness() == ASTTableJoin::Strictness::RightAny || isAsof(join_step.getStrictness());
    bool make_nullable_for_left = has_outer_join_semantic && isRightOrFull(join_step.getKind());
    bool make_nullable_for_right = has_outer_join_semantic && isLeftOrFull(join_step.getKind());

    auto update_symbol_type = [&output_set_null](const std::unordered_map<String, DataTypePtr> & type_provider, bool make_nullable) {
        std::transform(
            output_set_null.begin(),
            output_set_null.end(),
            output_set_null.begin(),
            [&type_provider, &make_nullable](const NameAndTypePair & symbol) -> NameAndTypePair {
                if (!type_provider.contains(symbol.name))
                    return symbol;

                const auto & type = type_provider.at(symbol.name);
                if (make_nullable && JoinCommon::canBecomeNullable(type))
                    return {symbol.name, JoinCommon::convertTypeToNullable(type_provider.at(symbol.name))};
                else
                    return {symbol.name, type_provider.at(symbol.name)};
            });
    };

    update_symbol_type(left_name_to_type, make_nullable_for_left);
    update_symbol_type(right_name_to_type, make_nullable_for_right);

    DataStream output_stream_set_null = DataStream{.header = output_set_null};
    auto join_step_set_null = std::make_shared<JoinStep>(
        inputs,
        output_stream_set_null,
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
    return JoinNode::createPlanNode(context->nextNodeId(), std::move(join_step_set_null), children, node.getStatistics());
}

PlanNodePtr UnifyNullableVisitor::visitAggregatingNode(AggregatingNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);

    const auto & step = *node.getStep();

    const AggregateDescriptions & descs = step.getAggregates();
    AggregateDescriptions descs_set_nullable;

    auto input_columns = child->getStep()->getOutputStream().header;
    for (const auto & desc : descs)
    {
        AggregateDescription desc_with_null;
        AggregateFunctionPtr fun = desc.function;
        Names argument_names = desc.argument_names;
        DataTypes types;
        for (auto & argument_name : argument_names)
        {
            for (auto & column : input_columns)
            {
                if (argument_name == column.name)
                {
                    types.emplace_back(recursiveRemoveLowCardinality(column.type));
                    break;
                }
            }
        }
        String fun_name = fun->getName();
        AggregateFunctionProperties properties;
        AggregateFunctionPtr fun_with_null = AggregateFunctionFactory::instance().get(fun_name, types, desc.parameters, properties);
        desc_with_null.function = fun_with_null;
        desc_with_null.parameters = desc.parameters;
        desc_with_null.column_name = desc.column_name;
        desc_with_null.argument_names = argument_names;
        desc_with_null.parameters = desc.parameters;
        desc_with_null.arguments = desc.arguments;
        desc_with_null.mask_column = desc.mask_column;

        descs_set_nullable.emplace_back(desc_with_null);
    }

    auto agg_step_set_null = std::make_shared<AggregatingStep>(
        child->getStep()->getOutputStream(),
        step.getKeys(),
        descs_set_nullable,
        step.getGroupingSetsParams(),
        step.isFinal(),
        step.getGroupings(),
        false,
        step.shouldProduceResultsInOrderOfBucketNumber()
        );
    auto agg_node_set_null
        = AggregatingNode::createPlanNode(context->nextNodeId(), std::move(agg_step_set_null), PlanNodes{child}, node.getStatistics());

    return agg_node_set_null;
}

PlanNodePtr UnifyNullableVisitor::visitWindowNode(WindowNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);

    // TODO is window function needs adjust according to NullableType ?
    const DataStream & input = child->getStep()->getOutputStream();

    const auto & step = *node.getStep();
    auto window_step = std::make_unique<WindowStep>(input, step.getWindow(), step.getFunctions(), step.needSort());
    auto window_node = WindowNode::createPlanNode(context->nextNodeId(), std::move(window_step), PlanNodes{child}, node.getStatistics());

    return window_node;
}

PlanNodePtr UnifyNullableVisitor::visitUnionNode(UnionNode & node, Void & v)
{
    const auto & step = *node.getStep();
    PlanNodes new_children;
    DataStreams new_inputs;
    NamesAndTypes new_output = step.getOutputStream().header.getNamesAndTypes();

    auto update_output_data_type = [&step](NamesAndTypes & output_symbols, const NamesAndTypes & new_input_symbols, int child_id) {
        for (auto & output : output_symbols)
        {
            const auto & input_name = step.getOutToInputs().at(output.name)[child_id];
            DataTypePtr input_type = nullptr;

            for (const auto & item : new_input_symbols)
            {
                if (item.name == input_name)
                {
                    input_type = item.type;
                    break;
                }
            }

            if (input_type->isNullable() && !output.type->isNullable())
            {
                output.type = makeNullable(output.type);
            }
        }
    };

    for (size_t i = 0; i < node.getChildren().size(); ++i)
    {
        PlanNodePtr rewritten_child = VisitorUtil::accept(*node.getChildren()[i], *this, v);
        new_children.emplace_back(rewritten_child);
        new_inputs.push_back(rewritten_child->getStep()->getOutputStream());
        update_output_data_type(new_output, rewritten_child->getStep()->getOutputStream().header.getNamesAndTypes(), i);
    }

    Block new_output_header;
    for (const auto & item : new_output)
    {
        new_output_header.insert(ColumnWithTypeAndName{item.type, item.name});
    }

    auto rewritten_step = std::make_unique<UnionStep>(
        new_inputs, DataStream{new_output_header}, step.getOutToInputs(), step.getMaxThreads(), step.isLocal());
    auto rewritten_node = UnionNode::createPlanNode(context->nextNodeId(), std::move(rewritten_step), new_children, node.getStatistics());
    return rewritten_node;
}

PlanNodePtr UnifyNullableVisitor::visitExchangeNode(ExchangeNode & node, Void & v)
{
    const auto & step = *node.getStep();

    PlanNodes children;
    DataStreams inputs;
    for (auto & item : node.getChildren())
    {
        PlanNodePtr child = VisitorUtil::accept(*item, *this, v);
        children.emplace_back(child);
        inputs.emplace_back(child->getStep()->getOutputStream());
    }

    // update it's input/output stream types.
    auto exchange_step_set_null = std::make_unique<ExchangeStep>(inputs, step.getExchangeMode(), step.getSchema(), step.needKeepOrder());
    auto exchange_node_set_null
        = ExchangeNode::createPlanNode(context->nextNodeId(), std::move(exchange_step_set_null), children, node.getStatistics());
    return exchange_node_set_null;
}

PlanNodePtr UnifyNullableVisitor::visitPartialSortingNode(PartialSortingNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);
    const DataStream & input = child->getStep()->getOutputStream();
    const auto & step = *node.getStep();

    // update it's input/output stream types.
    auto sort_step = std::make_unique<PartialSortingStep>(input, step.getSortDescription(), step.getLimit());
    auto sort_node = PartialSortingNode::createPlanNode(context->nextNodeId(), std::move(sort_step), PlanNodes{child}, node.getStatistics());
    return sort_node;
}

PlanNodePtr UnifyNullableVisitor::visitMergeSortingNode(MergeSortingNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);
    const DataStream & input = child->getStep()->getOutputStream();
    const auto & step = *node.getStep();

    // update it's input/output stream types.
    auto sort_step = std::make_unique<MergeSortingStep>(input, step.getSortDescription(), step.getLimit());
    auto sort_node = MergeSortingNode::createPlanNode(context->nextNodeId(), std::move(sort_step), PlanNodes{child}, node.getStatistics());
    return sort_node;
}

PlanNodePtr UnifyNullableVisitor::visitLimitNode(LimitNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);
    const DataStream & input = child->getStep()->getOutputStream();
    const auto & step = *node.getStep();

    // update it's input/output stream types.
    auto limit_step = std::make_unique<LimitStep>(input, step.getLimit(), step.getOffset());
    auto limit_node = LimitNode::createPlanNode(context->nextNodeId(), std::move(limit_step), PlanNodes{child}, node.getStatistics());
    return limit_node;
}

PlanNodePtr UnifyNullableVisitor::visitEnforceSingleRowNode(EnforceSingleRowNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);
    const DataStream & input = child->getStep()->getOutputStream();

    // update it's input/output stream types.
    auto single_step = std::make_unique<EnforceSingleRowStep>(input);
    auto single_node = EnforceSingleRowNode::createPlanNode(context->nextNodeId(), std::move(single_step), PlanNodes{child}, node.getStatistics());
    return single_node;
}

PlanNodePtr UnifyNullableVisitor::visitAssignUniqueIdNode(AssignUniqueIdNode & node, Void & v)
{
    PlanNodePtr child = VisitorUtil::accept(node.getChildren()[0], *this, v);
    const DataStream & input = child->getStep()->getOutputStream();
    const auto & step = *node.getStep();

    // update it's input/output stream types.
    auto unique_step = std::make_unique<AssignUniqueIdStep>(input, step.getUniqueId());
    auto unique_node = AssignUniqueIdNode::createPlanNode(context->nextNodeId(), std::move(unique_step), PlanNodes{child}, node.getStatistics());
    return unique_node;
}

}
