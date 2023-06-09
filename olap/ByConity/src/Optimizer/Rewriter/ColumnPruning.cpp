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

#include <Optimizer/Rewriter/ColumnPruning.h>

#include <Interpreters/ExpressionActions.h>
#include <Optimizer/Correlation.h>
#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/SymbolsExtractor.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/ApplyStep.h>
#include <QueryPlan/AssignUniqueIdStep.h>
#include <QueryPlan/DistinctStep.h>
#include <Optimizer/Rewriter/RemoveUnusedCTE.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/LimitByStep.h>
#include <QueryPlan/MergeSortingStep.h>
#include <QueryPlan/MergingSortedStep.h>
#include <QueryPlan/PartialSortingStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/UnionStep.h>
#include <QueryPlan/WindowStep.h>
#include <QueryPlan/Dummy.h>

namespace DB
{
void ColumnPruning::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    ColumnPruningVisitor visitor{context, plan.getCTEInfo(), plan.getPlanNode()};
    NameSet require;
    for (const auto & item : plan.getPlanNode()->getStep()->getOutputStream().header)
        require.insert(item.name);
    auto result = VisitorUtil::accept(plan.getPlanNode(), visitor, require);
    plan.update(result);
}

PlanNodePtr ColumnPruningVisitor::visitLimitByNode(LimitByNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    for (const auto & item : step->getColumns())
        require.insert(item);
    auto child = VisitorUtil::accept(node.getChildren()[0], *this, require);
    auto limit_step = std::make_shared<LimitByStep>(
        child->getStep()->getOutputStream(), step->getGroupLength(), step->getGroupOffset(), step->getColumns());
    return LimitByNode::createPlanNode(context->nextNodeId(), std::move(limit_step), PlanNodes{child}, node.getStatistics());
}

PlanNodePtr ColumnPruningVisitor::visitWindowNode(WindowNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    NameSet child_require = require;

    std::vector<WindowFunctionDescription> window_functions;
    for (const auto & function : step->getFunctions())
    {
        if (!require.contains(function.column_name))
            continue;

        child_require.erase(function.column_name);

        window_functions.push_back(function);
        child_require.insert(function.argument_names.begin(), function.argument_names.end());
    }
    for (const auto & item : step->getWindow().order_by)
        child_require.insert(item.column_name);

    for (const auto & item : step->getWindow().partition_by)
        child_require.insert(item.column_name);

    for (const auto & item : step->getWindow().full_sort_description)
        child_require.insert(item.column_name);

    auto child = VisitorUtil::accept(node.getChildren()[0], *this, child_require);

    if (window_functions.empty())
        return child;

    auto window_step = std::make_shared<WindowStep>(child->getStep()->getOutputStream(), step->getWindow(), window_functions, step->needSort());

    PlanNodes children{child};
    return WindowNode::createPlanNode(context->nextNodeId(), std::move(window_step), children, node.getStatistics());
}


PlanNodePtr ColumnPruningVisitor::visitFilterNode(FilterNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();

    bool remove = !require.contains(step->getFilterColumnName());

    NameSet child_require = require;
    const auto & filter = step->getFilter();
    auto symbols = SymbolsExtractor::extract(filter);
    child_require.insert(symbols.begin(), symbols.end());

    auto child = VisitorUtil::accept(node.getChildren()[0], *this, child_require);

    auto expr_step = std::make_shared<FilterStep>(child->getStep()->getOutputStream(), step->getFilter(), remove);
    PlanNodes children{child};
    auto expr_node = FilterNode::createPlanNode(context->nextNodeId(), std::move(expr_step), children, node.getStatistics());
    return expr_node;
}

PlanNodePtr ColumnPruningVisitor::visitProjectionNode(ProjectionNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();

    if (step->isFinalProject())
    {
        require = NameSet{};
        for (const auto & item : step->getAssignments())
            require.insert(item.first);
    }

    for (const auto & dynamic_filter : step->getDynamicFilters())
        require.insert(dynamic_filter.first);

    NameSet child_require;

    Assignments assignments;
    NameToType name_to_type;
    for (const auto & assignment : step->getAssignments())
    {
        if (require.contains(assignment.first) || !ExpressionDeterminism::isDeterministic(assignment.second, context))
        {
            const auto & ast = assignment.second;
            auto symbols = SymbolsExtractor::extract(ast);
            child_require.insert(symbols.begin(), symbols.end());
            assignments.emplace_back(assignment);
            name_to_type[assignment.first] = step->getNameToType().at(assignment.first);
        }
    }

    auto child = VisitorUtil::accept(node.getChildren()[0], *this, child_require);

    // if empty project return child node.
    if (assignments.empty())
        return child;

    auto expr_step = std::make_shared<ProjectionStep>(
    child->getStep()->getOutputStream(), std::move(assignments), std::move(name_to_type), step->isFinalProject(), step->getDynamicFilters());
    PlanNodes children{child};
    auto expr_node = ProjectionNode::createPlanNode(context->nextNodeId(), std::move(expr_step), children, node.getStatistics());
    return expr_node;
}

PlanNodePtr ColumnPruningVisitor::visitApplyNode(ApplyNode & node, NameSet & require)
{
    NameSet right_require;
    for (const auto & item : node.getChildren()[1]->getStep()->getOutputStream().header)
    {
        right_require.insert(item.name);
    }
    auto right = VisitorUtil::accept(node.getChildren()[1], *this, right_require);

    const auto * step = node.getStep().get();

    Names correlation = Correlation::prune(right, step->getCorrelation());

    NameSet left_require = require;
    left_require.insert(correlation.begin(), correlation.end());

    const auto & assignment = step->getAssignment();
    auto ast = assignment.second;
    if (ast && ast->as<ASTFunction>())
    {
        const auto & fun = ast->as<ASTFunction &>();
        if (fun.name == "in" || fun.name == "notIn" || fun.name == "globalIn" || fun.name == "globalNotIn")
        {
            ASTIdentifier & in_left = fun.arguments->getChildren()[0]->as<ASTIdentifier &>();
            left_require.insert(in_left.name());
        }
    }
    else if(ast && ast->as<ASTQuantifiedComparison>())
    {
        const auto & qc = ast->as<ASTQuantifiedComparison &>();
        ASTIdentifier & qc_left = qc.children[0]->as<ASTIdentifier &>();
        left_require.insert(qc_left.name());
    }

    auto left = VisitorUtil::accept(node.getChildren()[0], *this, left_require);

    DataStreams input{left->getStep()->getOutputStream(), right->getStep()->getOutputStream()};

    auto apply_step = std::make_shared<ApplyStep>(input, correlation, step->getApplyType(), step->getSubqueryType(), step->getAssignment());
    PlanNodes children{left, right};
    auto apply_node = ApplyNode::createPlanNode(context->nextNodeId(), std::move(apply_step), children, node.getStatistics());
    return apply_node;
}

PlanNodePtr ColumnPruningVisitor::visitTableScanNode(TableScanNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();

    /// You need to read at least one column to find the number of rows.
    /// We will find a column with minimum <compressed_size, type_size, uncompressed_size>.
    /// Because it is the column that is cheapest to read.
    struct ColumnSizeTuple
    {
        size_t compressed_size;
        size_t type_size;
        size_t uncompressed_size;
        String name;
        //        bool operator<(const ColumnSizeTuple & that) const
        //        {
        //            return std::tie(compressed_size, type_size, uncompressed_size)
        //                < std::tie(that.compressed_size, that.type_size, that.uncompressed_size);
        //        }
    };

    NameSet required;
    for (const auto & item : step->getColumnAlias())
        if (require.contains(item.second))
            required.insert(item.second);

    if (required.empty())
    {
        auto source_columns = step->getOutputStream().header.getNamesAndTypesList();

        /// You need to read at least one column to find the number of rows.
        /// We will find a column with minimum <compressed_size, type_size, uncompressed_size>.
        /// Because it is the column that is cheapest to read.
        struct ColumnSizeTuple
        {
            size_t compressed_size;
            size_t type_size;
            size_t uncompressed_size;
            String name;

            bool operator<(const ColumnSizeTuple & that) const
            {
                return std::tie(compressed_size, type_size, uncompressed_size)
                    < std::tie(that.compressed_size, that.type_size, that.uncompressed_size);
            }
        };

        std::vector<ColumnSizeTuple> columns;
        auto storage = step->getStorage();
        if (storage)
        {
            auto column_sizes = storage->getColumnSizes();
            for (auto & source_column : source_columns)
            {
                auto c = column_sizes.find(source_column.name);
                if (c == column_sizes.end())
                    continue;
                size_t type_size = source_column.type->haveMaximumSizeOfValue() ? source_column.type->getMaximumSizeOfValueInMemory() : 100;
                columns.emplace_back(
                    ColumnSizeTuple{c->second.data_compressed, type_size, c->second.data_uncompressed, source_column.name});
            }
        }

        if (!columns.empty())
            required.insert(std::min_element(columns.begin(), columns.end())->name);
        else if (!source_columns.empty())
        {
            if (storage)
            {
                // DO NOT choose Virtuals column, when try get smallest column.
                for (const auto & column : storage->getVirtuals())
                {
                    source_columns.remove(column);
                }
            }
            /// If we have no information about columns sizes, choose a column of minimum size of its data type.
            required.insert(ExpressionActions::getSmallestColumn(source_columns));
        }
    }

    NamesWithAliases column_names;
    for (const auto & item : step->getColumnAlias())
        if (required.contains(item.second))
            column_names.emplace_back(item);

    auto read_step = std::make_shared<TableScanStep>(
        context, step->getStorageID(), std::move(column_names), step->getQueryInfo(), step->getProcessedStage(), step->getMaxBlockSize());
    auto read_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(read_step), {}, node.getStatistics());
    return read_node;
}

PlanNodePtr ColumnPruningVisitor::visitAggregatingNode(AggregatingNode & node, NameSet & require_)
{
    const auto * step = node.getStep().get();

    NameSet child_require{step->getKeys().begin(), step->getKeys().end()};

    AggregateDescriptions aggs;
    NameSet names;
    for (const auto & agg : step->getAggregates())
    {
        if (((agg.argument_names.size() == 1 && require_.contains(agg.argument_names[0])) || require_.contains(agg.column_name))
            && !names.contains(agg.column_name))
        {
            aggs.push_back(agg);
            child_require.insert(agg.argument_names.begin(), agg.argument_names.end());
            names.insert(agg.column_name);
        }
    }

    auto child = VisitorUtil::accept(node.getChildren()[0], *this, child_require);
    if (aggs.empty() && step->getKeys().empty())
    {
        auto [symbol, node_] = createDummyPlanNode(context);
        (void) symbol;
        // require_.insert(symbol);
        return node_;
    }

    auto agg_step = std::make_shared<AggregatingStep>(
        child->getStep()->getOutputStream(), step->getKeys(), std::move(aggs), step->getGroupingSetsParams(), step->isFinal(), step->getGroupings()
        , false, step->shouldProduceResultsInOrderOfBucketNumber()
        //        step->getHaving(),
        //        step->getInteresteventsInfoList()
    );

    PlanNodes children{child};
    auto agg_node = AggregatingNode::createPlanNode(context->nextNodeId(), std::move(agg_step), children, node.getStatistics());
    return agg_node;
}

PlanNodePtr ColumnPruningVisitor::visitSortingNode(SortingNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    for (const auto & item : step->getSortDescription())
    {
        require.insert(item.column_name);
    }
    auto child = VisitorUtil::accept(node.getChildren()[0], *this, require);
    auto sort_step = std::make_shared<SortingStep>(child->getStep()->getOutputStream(), step->getSortDescription(), step->getLimit(), step->isPartial());
    return SortingNode::createPlanNode(context->nextNodeId(), std::move(sort_step), PlanNodes{child}, node.getStatistics());
}

PlanNodePtr ColumnPruningVisitor::visitMergeSortingNode(MergeSortingNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    for (const auto & item : step->getSortDescription())
    {
        require.insert(item.column_name);
    }
    auto child = VisitorUtil::accept(node.getChildren()[0], *this, require);
    auto sort_step = std::make_shared<MergeSortingStep>(child->getStep()->getOutputStream(), step->getSortDescription(), step->getLimit());
    return MergeSortingNode::createPlanNode(context->nextNodeId(), std::move(sort_step), PlanNodes{child}, node.getStatistics());
}

PlanNodePtr ColumnPruningVisitor::visitMergingSortedNode(MergingSortedNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    for (const auto & item : step->getSortDescription())
    {
        require.insert(item.column_name);
    }
    auto child = VisitorUtil::accept(node.getChildren()[0], *this, require);
    auto sort_step = std::make_shared<MergingSortedStep>(child->getStep()->getOutputStream(), step->getSortDescription(), step->getMaxBlockSize(), step->getLimit());
    return MergingSortedNode::createPlanNode(context->nextNodeId(), std::move(sort_step), PlanNodes{child}, node.getStatistics());
}

PlanNodePtr ColumnPruningVisitor::visitPartialSortingNode(PartialSortingNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    for (const auto & item : step->getSortDescription())
    {
        require.insert(item.column_name);
    }
    auto child = VisitorUtil::accept(node.getChildren()[0], *this, require);
    auto sort_step
        = std::make_shared<PartialSortingStep>(child->getStep()->getOutputStream(), step->getSortDescription(), step->getLimit());
    return PartialSortingNode::createPlanNode(context->nextNodeId(), std::move(sort_step), PlanNodes{child}, node.getStatistics());
}

PlanNodePtr ColumnPruningVisitor::visitJoinNode(JoinNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();

    auto filter = step->getFilter()->clone();
    std::set<String> symbols = SymbolsExtractor::extract(filter);

    NameSet left_require = require;
    left_require.insert(step->getLeftKeys().begin(), step->getLeftKeys().end());
    left_require.insert(symbols.begin(), symbols.end());
    auto left = VisitorUtil::accept(node.getChildren()[0], *this, left_require);

    NameSet right_require = require;
    right_require.insert(step->getRightKeys().begin(), step->getRightKeys().end());
    right_require.insert(symbols.begin(), symbols.end());

    auto right = VisitorUtil::accept(node.getChildren()[1], *this, right_require);

    DataStreams inputs{left->getStep()->getOutputStream(), right->getStep()->getOutputStream()};

    ColumnsWithTypeAndName output_header;
    const auto & left_header = left->getStep()->getOutputStream().header;
    const auto & right_header = right->getStep()->getOutputStream().header;

    // remove un-referenced output symbols
    // todo keep order
    for (const auto & header : left_header)
    {
        if (require.contains(header.name))
        {
            output_header.emplace_back(header);
        }
    }
    for (const auto & header : right_header)
    {
        if (require.contains(header.name))
        {
            output_header.emplace_back(header);
        }
    }

    /// must have one output column
    if (output_header.empty())
    {
        if (left_header.columns() != 0)
        {
            output_header.emplace_back(left_header.getByPosition(0));
            left_require.insert(left_header.getByPosition(0).name);
            left = VisitorUtil::accept(node.getChildren()[0], *this, left_require);
        }
        else if (right_header.columns() != 0)
        {
            output_header.emplace_back(right_header.getByPosition(0));
            right_require.insert(right_header.getByPosition(0).name);
            right = VisitorUtil::accept(node.getChildren()[1], *this, right_require);
        }
        else
        {
            throw Exception("Join no input symbols", ErrorCodes::LOGICAL_ERROR);
        }
    }

    // column pruning can't change the output type of join.
    for (auto & output : output_header)
    {
        for (const auto & origin_output : step->getOutputStream().header)
            if (output.name == origin_output.name)
            {
                if (origin_output.type->isNullable())
                {
                    output.type = makeNullable(output.type);
                }
            }
    }

    auto join_step = std::make_shared<JoinStep>(
        inputs,
        DataStream{output_header},
        step->getKind(),
        step->getStrictness(),
        step->getLeftKeys(),
        step->getRightKeys(),
        step->getFilter(),
        step->isHasUsing(),
        step->getRequireRightKeys(),
        step->getAsofInequality(),
        step->getDistributionType(),
        step->isMagic());

    PlanNodes children{left, right};
    auto join_node = JoinNode::createPlanNode(context->nextNodeId(), std::move(join_step), children, node.getStatistics());
    return join_node;
}

PlanNodePtr ColumnPruningVisitor::visitDistinctNode(DistinctNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    NameSet child_require = require;
    const auto & columns = step->getColumns();
    child_require.insert(columns.begin(), columns.end());
    auto child = VisitorUtil::accept(node.getChildren()[0], *this, child_require);

    auto distinct_step = std::make_shared<DistinctStep>(
        child->getStep()->getOutputStream(), step->getSetSizeLimits(), step->getLimitHint(), columns, step->preDistinct());

    PlanNodes children{child};
    auto distinct_node = DistinctNode::createPlanNode(context->nextNodeId(), std::move(distinct_step), children, node.getStatistics());
    return distinct_node;
}

PlanNodePtr ColumnPruningVisitor::visitUnionNode(UnionNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();

    std::vector<String> require_columns;

    DataStream output_stream;
    std::unordered_map<String, std::vector<String>> output_to_inputs;
    for (const auto & item : step->getOutputStream().header)
    {
        if (require.contains(item.name))
        {
            require_columns.emplace_back(item.name);
            output_stream.header.insert(item);
            output_to_inputs[item.name] = step->getOutToInputs().at(item.name);
        }
    }

    PlanNodes children;
    DataStreams children_streams;
    for (size_t i = 0; i < node.getChildren().size(); i++)
    {
        const auto & child = node.getChildren()[i];

        NameSet child_require;
        for (const auto & item : require_columns)
            child_require.insert(output_to_inputs.at(item)[i]);

        /// count(*) requires nothing but we need gave some rows.
        if (child_require.empty())
            child_require.emplace(child->getStep()->getOutputStream().header.getByPosition(0).name);

        auto new_child = VisitorUtil::accept(child, *this, child_require);
        children_streams.emplace_back(new_child->getStep()->getOutputStream());
        children.emplace_back(new_child);
    }

    auto union_step = std::make_shared<UnionStep>(std::move(children_streams), std::move(output_stream), std::move(output_to_inputs),  step->getMaxThreads(), step->isLocal());
    auto union_node = UnionNode::createPlanNode(context->nextNodeId(), std::move(union_step), children, node.getStatistics());
    return union_node;
}
PlanNodePtr ColumnPruningVisitor::visitExceptNode(ExceptNode & node, NameSet &)
{
    const auto * step = node.getStep().get();

    std::vector<size_t> require_index;

    size_t index = 0;
    DataStream output_stream;
    for (const auto & item : step->getOutputStream().header)
    {
        require_index.emplace_back(index);
        output_stream.header.insert(item);
        index++;
    }

    /// count(*) requires nothing but we need gave some rows.
    if (require_index.empty())
        require_index.emplace_back(0);

    PlanNodes children;
    DataStreams children_streams;
    for (const auto & child : node.getChildren())
    {
        NameSet child_require;
        for (const auto & item : require_index)
            child_require.insert(child->getStep()->getOutputStream().header.getByPosition(item).name);

        auto new_child = VisitorUtil::accept(child, *this, child_require);
        children_streams.emplace_back(new_child->getStep()->getOutputStream());
        children.emplace_back(new_child);
    }

    auto except_step = std::make_shared<ExceptStep>(std::move(children_streams), std::move(output_stream), step->isDistinct());
    auto except_node = ExceptNode::createPlanNode(context->nextNodeId(), std::move(except_step), children, node.getStatistics());
    return except_node;
}

PlanNodePtr ColumnPruningVisitor::visitIntersectNode(IntersectNode & node, NameSet &)
{
    const auto * step = node.getStep().get();

    std::vector<size_t> require_index;

    size_t index = 0;
    DataStream output_stream;
    for (const auto & item : step->getOutputStream().header)
    {
        require_index.emplace_back(index);
        output_stream.header.insert(item);
        index++;
    }

    /// count(*) requires nothing but we need gave some rows.
    if (require_index.empty())
        require_index.emplace_back(0);

    PlanNodes children;
    DataStreams children_streams;
    for (const auto & child : node.getChildren())
    {
        NameSet child_require;
        for (const auto & item : require_index)
            child_require.insert(child->getStep()->getOutputStream().header.getByPosition(item).name);

        auto new_child = VisitorUtil::accept(child, *this, child_require);
        children_streams.emplace_back(new_child->getStep()->getOutputStream());
        children.emplace_back(new_child);
    }

    auto intersect_step = std::make_shared<IntersectStep>(std::move(children_streams), std::move(output_stream), step->isDistinct());
    auto intersect_node = IntersectNode::createPlanNode(context->nextNodeId(), std::move(intersect_step), children, node.getStatistics());
    return intersect_node;
}

PlanNodePtr ColumnPruningVisitor::visitAssignUniqueIdNode(AssignUniqueIdNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();
    if (!require.contains(step->getUniqueId()))
    {
        return VisitorUtil::accept(node.getChildren()[0], *this, require);
    }

    return visitPlanNode(node, require);
}

PlanNodePtr ColumnPruningVisitor::visitExchangeNode(ExchangeNode & node, NameSet & require)
{
    const auto * step = node.getStep().get();

    if (require.empty())
    {
        require.insert(step->getOutputStream().header.getByPosition(0).name);
    }

    PlanNodes children;
    DataStreams input_streams;
    for (auto & item : node.getChildren())
    {
        auto child = VisitorUtil::accept(item, *this, require);
        children.emplace_back(child);
        input_streams.emplace_back(child->getStep()->getOutputStream());
    }

    auto exchange_step = std::make_shared<ExchangeStep>(std::move(input_streams), step->getExchangeMode(), step->getSchema(), step->needKeepOrder());
    return ExchangeNode::createPlanNode(context->nextNodeId(), std::move(exchange_step), children, node.getStatistics());
}

PlanNodePtr ColumnPruningVisitor::visitCTERefNode(CTERefNode & node, NameSet & require)
{
    const auto * with_step = node.getStep().get();
    NameSet required;
    for (const auto & item : with_step->getOutputColumns())
        if (require.contains(item.first))
            required.emplace(item.first);

    if (required.empty())
        required.emplace(ExpressionActions::getSmallestColumn(with_step->getOutputStream().header.getNamesAndTypesList()));

    auto & cte_require = cte_require_columns[with_step->getId()];
    for (const auto & item : required)
        cte_require.emplace(with_step->getOutputColumns().at(item));
    post_order_cte_helper.acceptAndUpdate(with_step->getId(), *this, cte_require);

    NamesAndTypes result_columns;
    std::unordered_map<String, String> output_columns;
    for (const auto & item : with_step->getOutputStream().header.getNamesAndTypes())
        if (required.contains(item.name))
            result_columns.emplace_back(item);
    for (const auto & item : with_step->getOutputColumns())
        if (required.contains(item.first))
            output_columns.emplace(item);

    auto exchange_step
        = std::make_shared<CTERefStep>(DataStream{std::move(result_columns)}, with_step->getId(), std::move(output_columns), with_step->getFilter());
    return CTERefNode::createPlanNode(context->nextNodeId(), std::move(exchange_step), {}, node.getStatistics());
}
}
