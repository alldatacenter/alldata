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

#include <QueryPlan/GraphvizPrinter.h>

#include <AggregateFunctions/AggregateFunctionNull.h>
#include <DataTypes/FieldToDataType.h>
#include <IO/WriteBufferFromString.h>
#include <Interpreters/AggregateDescription.h>
#include <Interpreters/convertFieldToType.h>
#include <Parsers/formatAST.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <Processors/printPipeline.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/ApplyStep.h>
#include <QueryPlan/DistinctStep.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/LimitByStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/SortingStep.h>
#include <QueryPlan/MergeSortingStep.h>
#include <QueryPlan/MergingAggregatedStep.h>
#include <QueryPlan/MergingSortedStep.h>
#include <QueryPlan/PartialSortingStep.h>
#include <QueryPlan/PartitionTopNStep.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/UnionStep.h>
#include <QueryPlan/WindowStep.h>
#include <Storages/StorageDistributed.h>
#include <boost/algorithm/string/replace.hpp>

#include <filesystem>
#include <iostream>
#include <memory>
#include <numeric>
#include <typeinfo>

namespace DB
{
const String GraphvizPrinter::MEMO_PATH = "3998_CascadesOptimizer-Memo"; // NOLINT(cert-err58-cpp)
const String GraphvizPrinter::MEMO_GRAPH_PATH = "3999_CascadesOptimizer-Memo-Graph"; // NOLINT(cert-err58-cpp)
const String GraphvizPrinter::PIPELINE_PATH = "5000_pipeline";

static std::unordered_map<IQueryPlanStep::Type, std::string> NODE_COLORS = {
    // NOLINT(cert-err58-cpp)
    {IQueryPlanStep::Type::Projection, "bisque"},
    {IQueryPlanStep::Type::Filter, "yellow"},
    {IQueryPlanStep::Type::Join, "orange"},
    {IQueryPlanStep::Type::Aggregating, "chartreuse3"},
    {IQueryPlanStep::Type::MergingAggregated, "chartreuse3"},
    {IQueryPlanStep::Type::Window, "darkolivegreen4"},
    {IQueryPlanStep::Type::PartitionTopN, "darkolivegreen4"},
    {IQueryPlanStep::Type::Union, "turquoise4"},
    {IQueryPlanStep::Type::Intersect, "turquoise4"},
    {IQueryPlanStep::Type::Except, "turquoise4"},
    {IQueryPlanStep::Type::Exchange, "gold"},
    {IQueryPlanStep::Type::RemoteExchangeSource, "gold"},
    {IQueryPlanStep::Type::TableScan, "deepskyblue"},
    {IQueryPlanStep::Type::ReadNothing, "deepskyblue"},
    {IQueryPlanStep::Type::Values, "deepskyblue"},
    {IQueryPlanStep::Type::Limit, "gray83"},
    {IQueryPlanStep::Type::LimitBy, "gray83"},
    {IQueryPlanStep::Type::Sorting, "aliceblue"},
    {IQueryPlanStep::Type::MergeSorting, "aliceblue"},
    {IQueryPlanStep::Type::PartialSorting, "aliceblue"},
    {IQueryPlanStep::Type::MergingSorted, "aliceblue"},
    //    {IQueryPlanStep::Type::Materializing, "darkolivegreen4"},
    //    {IQueryPlanStep::Type::Decompression, "darkolivegreen4"},
    {IQueryPlanStep::Type::Distinct, "darkolivegreen4"},
    {IQueryPlanStep::Type::Extremes, "goldenrod4"},
    {IQueryPlanStep::Type::FinalSample, "goldenrod4"},
    {IQueryPlanStep::Type::Apply, "orange"},
    {IQueryPlanStep::Type::EnforceSingleRow, "bisque"},
    {IQueryPlanStep::Type::AssignUniqueId, "bisque"},
    {IQueryPlanStep::Type::CTERef, "orange"},
};

struct PrinterContext
{
    bool is_magic = false;
};

template <class V, class Func>
static std::string join(const V & v, Func && to_string, const String & sep = ", ", const String & prefix = {}, const String & suffix = {})
{
    std::stringstream out;
    out << prefix;
    if (!v.empty())
    {
        auto it = v.begin();
        out << to_string(*it);
        for (++it; it != v.end(); ++it)
            out << sep << to_string(*it);
    }
    out << suffix;
    return out.str();
}

Void PlanNodePrinter::visitPlanNode(PlanNodeBase & node, PrinterContext &)
{
    throw Exception("PlanNode " + node.getStep()->getName() + " does not have a graphviz printer", ErrorCodes::NOT_IMPLEMENTED);
}

Void PlanNodePrinter::visitProjectionNode(ProjectionNode & node, PrinterContext & context)
{
    String label{"ProjectionNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printProjectionStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitFilterNode(FilterNode & node, PrinterContext & context)
{
    auto const & step = *node.getStep();
    String label{"FilterNode"};
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printFilterStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitJoinNode(JoinNode & node, PrinterContext & context)
{
    String label{"JoinNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    if (step.isMagic())
    {
        PrinterContext magic{.is_magic = true};
        printNode(node, label, StepPrinter::printJoinStep(step), color, magic);
        VisitorUtil::accept(*node.getChildren()[0], *this, context); // left node is not magic
        VisitorUtil::accept(*node.getChildren()[1], *this, magic);
    }
    else
    {
        printNode(node, label, StepPrinter::printJoinStep(step), color, context);
        VisitorUtil::accept(*node.getChildren()[0], *this, context);
        VisitorUtil::accept(*node.getChildren()[1], *this, context);
    }
    return Void{};
}

Void PlanNodePrinter::visitAggregatingNode(AggregatingNode & node, PrinterContext & context)
{
    String label{"AggregatingNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printAggregatingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitMergingAggregatedNode(MergingAggregatedNode & node, PrinterContext & context)
{
    String label{"MergingAggregatedNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printMergingAggregatedStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitUnionNode(UnionNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"UnionNode"};
    printNode(node, label, StepPrinter::printUnionStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitIntersectNode(IntersectNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"IntersectNode"};
    printNode(node, label, StepPrinter::printIntersectStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitExceptNode(ExceptNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"ExceptNode"};
    printNode(node, label, StepPrinter::printExceptStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitExchangeNode(ExchangeNode & node, PrinterContext & context)
{
    String label{"ExchangeNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printExchangeStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitRemoteExchangeSourceNode(RemoteExchangeSourceNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"RemoteExchangeSourceNode"};
    printNode(node, label, StepPrinter::printRemoteExchangeSourceStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitTableScanNode(TableScanNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"TableScanNode"};
    printNode(node, label, StepPrinter::printTableScanStep(step), color, context);
    return Void{};
}

Void PlanNodePrinter::visitReadNothingNode(ReadNothingNode & node, PrinterContext & context)
{
    auto stepPtr = node.getStep();
    String label{node.getStep()->getName()};
    String details{"ReadNothingNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, details, color, context);
    return Void{};
}

Void PlanNodePrinter::visitValuesNode(ValuesNode & node, PrinterContext & context)
{
    String label{"ValuesNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printValuesStep(step), color, context);
    return Void{};
}

Void PlanNodePrinter::visitLimitNode(LimitNode & node, PrinterContext & context)
{
    String label{"LimitNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printLimitStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitLimitByNode(LimitByNode & node, PrinterContext & context)
{
    String label{"LimitByNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printLimitByStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitSortingNode(SortingNode & node, PrinterContext & context)
{
    String label{"SortingNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printSortingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitMergeSortingNode(MergeSortingNode & node, PrinterContext & context)
{
    String label{"MergeSortingNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printMergeSortingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitPartialSortingNode(PartialSortingNode & node, PrinterContext & context)
{
    String label{"PartialSortingNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printPartialSortingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitMergingSortedNode(MergingSortedNode & node, PrinterContext & context)
{
    String label{"MergingSortedNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printMergingSortedStep(step), color, context);
    return visitChildren(node, context);
}

//Void NodePrinter::visitMaterializingNode(MaterializingNode & node, PrinterContext & context)
//{
//    auto & stepPtr = node.getStep();
//    String label{"MaterializingNode"};
//    String details{"MaterializingNode"};
//    String color{NODE_COLORS[stepPtr->getType()]};
//    printNode(node, label, details, color, context);
//    return visitChildren(node, context);
//}
//
//Void NodePrinter::visitDecompressionNode(DecompressionNode & node, PrinterContext & context)
//{
//    auto & stepPtr = node.getStep();
//    String label{"DecompressionNode"};
//    String details{"DecompressionNode"};
//    String color{NODE_COLORS[stepPtr->getType()]};
//    printNode(node, label, details, color, context);
//    return visitChildren(node, context);
//}

Void PlanNodePrinter::visitDistinctNode(DistinctNode & node, PrinterContext & context)
{
    String label{"DistinctNode"};
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    printNode(node, label, StepPrinter::printDistinctStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitExtremesNode(ExtremesNode & node, PrinterContext & context)
{
    auto stepPtr = node.getStep();
    String label{"ExtremesNode"};
    String details{"ExtremesNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, details, color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitFinalSampleNode(FinalSampleNode & node, PrinterContext & context)
{
    auto stepPtr = node.getStep();
    String label{"FinalSamplingNode"};
    String details{"FinalSamplingNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, details, color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitApplyNode(ApplyNode & node, PrinterContext & context)
{
    auto step_ptr = node.getStep();
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"ApplyNode"};
    printNode(node, label, StepPrinter::printApplyStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitEnforceSingleRowNode(EnforceSingleRowNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"EnforceSingleRowNode"};
    printNode(node, label, StepPrinter::printEnforceSingleRowStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitAssignUniqueIdNode(AssignUniqueIdNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"AssignUniqueIdStep"};
    printNode(node, label, StepPrinter::printAssignUniqueIdStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitWindowNode(WindowNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"WindowNode"};
    printNode(node, label, StepPrinter::printWindowStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanNodePrinter::visitPartitionTopNNode(PartitionTopNNode & node, PrinterContext & context)
{
    auto step = *node.getStep();
    String color{NODE_COLORS[step.getType()]};
    String label{"PartitionTopNNode"};
    printNode(node, label, StepPrinter::printPartitionTopNStep(step), color, context);
    return visitChildren(node, context);
}

void PlanNodePrinter::printNode(
    const PlanNodeBase & node, const String & label, const String & details, const String & color, PrinterContext & context)
{
    auto escapeSpecialCharacters = [](String content) {
        boost::replace_all(content, "<", "\\<");
        boost::replace_all(content, ">", "\\>");
        boost::replace_all(content, "{", "\\{");
        boost::replace_all(content, "}", "\\}");
        boost::replace_all(content, "\"", "\\\">");
        return content;
    };

    out << "plannode_" << node.getId() << R"([label="{)" << escapeSpecialCharacters(label) << "|" << escapeSpecialCharacters(details);

    if (with_id)
        out << "|" << node.getId();

    if (node.getStatistics().isDerived())
    {
        out << "|";
        out << "Stats \\n";
        auto statistics = node.getStatistics();
        if (statistics)
            out << statistics.value()->toString();
        else
            out << "None";
    }

    String style = context.is_magic ? "rounded, filled, dashed" : "rounded, filled";

    out << R"(}", style=")" << style << R"(", shape=record, fillcolor=)" << color << "]"
        << ";" << std::endl;
}

Void PlanNodePrinter::visitChildren(PlanNodeBase & node, PrinterContext & context)
{
    auto children = node.getChildren();
    for (auto & iter : children)
    {
        VisitorUtil::accept(*iter, *this, context);
    }
    return Void{};
}

Void PlanNodePrinter::visitCTERefNode(CTERefNode & node, PrinterContext & context)
{
    const auto & step = *node.getStep();
    String label{"CTERefNode"};
    String color{NODE_COLORS.at(step.getType())};
    printNode(node, label, StepPrinter::printCTERefStep(step), color, context);

    if (cte_helper && !cte_helper->hasVisited(step.getId()))
    {
        printCTEDefNode(step.getId());
        cte_helper.value().accept(step.getId(), *this, context);
    }

    return Void{};
}

void PlanNodePrinter::printCTEDefNode(CTEId cte_id)
{
    out << "cte_" << cte_id << R"([label="{CTEDef|CTEId: )" << cte_id << R"(}", style="rounded, filled", shape=record];)" << std::endl;
}

Void PlanNodeEdgePrinter::visitPlanNode(PlanNodeBase & node, Void & context)
{
    auto children = node.getChildren();
    for (auto & iter : children)
    {
        printEdge(*iter, node);
        VisitorUtil::accept(*iter, *this, context);
    }
    return Void{};
}

void PlanNodeEdgePrinter::printEdge(PlanNodeBase & from, PlanNodeBase & to)
{
    out << "plannode_" << from.getId() << " -> "
        << "plannode_" << to.getId() << ";" << std::endl;
}

Void PlanSegmentNodePrinter::visitNode(QueryPlan::Node * node, PrinterContext &)
{
    throw Exception("PlanNode " + node->step->getName() + " does not have a graphviz printer", ErrorCodes::NOT_IMPLEMENTED);
}

Void PlanSegmentNodePrinter::visitProjectionNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    String label{"ProjectionNode"};
    const auto & step = dynamic_cast<const ProjectionStep &>(*step_ptr);
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printProjectionStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitFilterNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    const auto & step = dynamic_cast<const FilterStep &>(*step_ptr);
    String label{"FilterNode"};
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printFilterStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitJoinNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    String label{"JoinNode"};
    const auto & step = dynamic_cast<const JoinStep &>(*step_ptr);
    String color{NODE_COLORS[step_ptr->getType()]};

    if (step.isMagic())
    {
        PrinterContext magic{.is_magic = true};
        printNode(node, label, StepPrinter::printJoinStep(step), color, magic);
        VisitorUtil::accept(node->children[0], *this, context); // left node is not magic
        VisitorUtil::accept(node->children[1], *this, magic);
    }
    else
    {
        printNode(node, label, StepPrinter::printJoinStep(step), color, context);
        VisitorUtil::accept(node->children[0], *this, context);
        VisitorUtil::accept(node->children[1], *this, context);
    }
    return Void{};
}

Void PlanSegmentNodePrinter::visitAggregatingNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    String label{"AggregatingNode"};
    const auto & step = dynamic_cast<const AggregatingStep &>(*step_ptr);
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printAggregatingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitMergingAggregatedNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    String label{"MergingAggregatedNode"};
    auto & step = dynamic_cast<const MergingAggregatedStep &>(*step_ptr);
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printMergingAggregatedStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitUnionNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    auto & step = dynamic_cast<const UnionStep &>(*stepPtr);
    String label{"UnionNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printUnionStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitIntersectNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    auto & step = dynamic_cast<const IntersectStep &>(*stepPtr);
    String label{"IntersectNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printIntersectStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitExceptNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    auto & step = dynamic_cast<const ExceptStep &>(*stepPtr);
    String label{"ExceptNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printExceptStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitExchangeNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"ExchangeNode"};
    auto & step = dynamic_cast<const ExchangeStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printExchangeStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitRemoteExchangeSourceNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    auto & step = dynamic_cast<const RemoteExchangeSourceStep &>(*step_ptr);
    String label{"RemoteExchangeSourceNode"};
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printRemoteExchangeSourceStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitTableScanNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    auto & step = dynamic_cast<const TableScanStep &>(*step_ptr);
    String label{"TableScanNode"};
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printTableScanStep(step), color, context);
    return Void{};
}

Void PlanSegmentNodePrinter::visitReadNothingNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"ReadNothingNode"};
    String details{"ReadNothingNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, details, color, context);
    return Void{};
}

Void PlanSegmentNodePrinter::visitValuesNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"ValuesNode"};
    auto & step = dynamic_cast<const ValuesStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printValuesStep(step), color, context);
    return Void{};
}

Void PlanSegmentNodePrinter::visitLimitNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"LimitNode"};
    auto & step = dynamic_cast<const LimitStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printLimitStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitLimitByNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"LimitByNode"};
    auto & step = dynamic_cast<const LimitByStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printLimitByStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitMergeSortingNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"MergeSortingNode"};
    auto & step = dynamic_cast<const MergeSortingStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printMergeSortingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitSortingNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"SortingNode"};
    auto & step = dynamic_cast<const SortingStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printSortingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitPartialSortingNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    String label{"PartialSortingNode"};
    auto & step = dynamic_cast<const PartialSortingStep &>(*step_ptr);
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printPartialSortingStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitMergingSortedNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"MergingSortedNode"};
    auto & step = dynamic_cast<const MergingSortedStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printMergingSortedStep(step), color, context);
    return visitChildren(node, context);
}

//Void NodePrinter::visitMaterializingNode(MaterializingNode & node, PrinterContext & context)
//{
//    auto & stepPtr = node.getStep();
//    String label{"MaterializingNode"};
//    String details{"MaterializingNode"};
//    String color{NODE_COLORS[stepPtr->getType()]};
//    printNode(node, label, details, color, context);
//    return visitChildren(node, context);
//}
//
//Void NodePrinter::visitDecompressionNode(DecompressionNode & node, PrinterContext & context)
//{
//    auto & stepPtr = node.getStep();
//    String label{"DecompressionNode"};
//    String details{"DecompressionNode"};
//    String color{NODE_COLORS[stepPtr->getType()]};
//    printNode(node, label, details, color, context);
//    return visitChildren(node, context);
//}

Void PlanSegmentNodePrinter::visitDistinctNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"DistinctNode"};
    auto & step = dynamic_cast<const DistinctStep &>(*stepPtr);
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, StepPrinter::printDistinctStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitExtremesNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"ExtremesNode"};
    String details{"ExtremesNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, details, color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitFinalSampleNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & stepPtr = node->step;
    String label{"FinalSamplingNode"};
    String details{"FinalSamplingNode"};
    String color{NODE_COLORS[stepPtr->getType()]};
    printNode(node, label, details, color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitApplyNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    auto & step = dynamic_cast<const ApplyStep &>(*step_ptr);
    String label{"ApplyNode"};
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printApplyStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitEnforceSingleRowNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    auto & step = dynamic_cast<const EnforceSingleRowStep &>(*step_ptr);
    String label{"EnforceSingleRowNode"};
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printEnforceSingleRowStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitAssignUniqueIdNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    auto & step = dynamic_cast<const AssignUniqueIdStep &>(*step_ptr);
    String label{"AssignUniqueIdStep"};
    String color{NODE_COLORS[step_ptr->getType()]};
    printNode(node, label, StepPrinter::printAssignUniqueIdStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitWindowNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    auto & step = dynamic_cast<const WindowStep &>(*step_ptr);
    String label{"WindowNode"};
    String color{NODE_COLORS.at(step_ptr->getType())};
    printNode(node, label, StepPrinter::printWindowStep(step), color, context);
    return visitChildren(node, context);
}

Void PlanSegmentNodePrinter::visitPartitionTopNNode(QueryPlan::Node * node, PrinterContext & context)
{
    auto & step_ptr = node->step;
    auto & step = dynamic_cast<const PartitionTopNStep &>(*step_ptr);
    String label{"PartitionTopNNode"};
    String color{NODE_COLORS.at(step_ptr->getType())};
    printNode(node, label, StepPrinter::printPartitionTopNStep(step), color, context);
    return visitChildren(node, context);
}

void PlanSegmentNodePrinter::printNode(
    QueryPlan::Node * node, const String & label, const String & details, const String & color, PrinterContext & context)
{
    auto escapeSpecialCharacters = [](String content) {
        boost::replace_all(content, "<", "\\<");
        boost::replace_all(content, ">", "\\>");
        boost::replace_all(content, "{", "\\{");
        boost::replace_all(content, "}", "\\}");
        boost::replace_all(content, "\"", "\\\">");
        return content;
    };

    out << "plannode_" << node->id << R"([label="{)" << escapeSpecialCharacters(label) << "|" << escapeSpecialCharacters(details);

    if (with_id)
        out << "|" << node->id;

    //    if (node.getStatistics().isDerived())
    //    {
    //        out << "|";
    //        out << "Stats \\n";
    //        auto statistics = node.getStatistics();
    //        if (statistics)
    //            out << statistics.value()->toString();
    //        else
    //            out << "None";
    //    }

    String style = context.is_magic ? "rounded, filled, dashed" : "rounded, filled";

    out << R"(}", style=")" << style << R"(", shape=record, fillcolor=)" << color << "]"
        << ";" << std::endl;
}

Void PlanSegmentNodePrinter::visitChildren(QueryPlan::Node * node, PrinterContext & context)
{
    for (auto & iter : node->children)
    {
        VisitorUtil::accept(iter, *this, context);
    }
    return Void{};
}

Void PlanSegmentEdgePrinter::visitNode(QueryPlan::Node * node, std::unordered_map<size_t, PlanSegmentPtr &> & context)
{
    std::vector<QueryPlan::Node *> & children = node->children;
    for (auto & iter : children)
    {
        printEdge(iter, node);
        VisitorUtil::accept(iter, *this, context);
    }
    return Void{};
}

Void PlanSegmentEdgePrinter::visitRemoteExchangeSourceNode(QueryPlan::Node * node, std::unordered_map<size_t, PlanSegmentPtr &> & context)
{
    auto * step = dynamic_cast<RemoteExchangeSourceStep *>(node->step.get());
    for (const auto & input : step->getInput())
    {
        const size_t segment_id = input->getPlanSegmentId();
        auto & plan_segment_ptr = context.at(segment_id);
        printEdge(plan_segment_ptr->getQueryPlan().getRoot(), node);
    }
    return Void{};
}

void PlanSegmentEdgePrinter::printEdge(QueryPlan::Node * from, QueryPlan::Node * to)
{
    out << "plannode_" << from->id << " -> "
        << "plannode_" << to->id << ";" << std::endl;
}

void cleanDotFiles(const ContextMutablePtr & context)
{
    std::filesystem::path graphviz_path(context->getSettingsRef().graphviz_path.toString());

    try
    {
        if (!std::filesystem::exists(graphviz_path))
        {
            std::filesystem::create_directory(graphviz_path);
            return;
        }

        auto query_id = context->getInitialQueryId();

        for (auto & dir_entry : std::filesystem::directory_iterator(graphviz_path))
        {
            if (dir_entry.is_regular_file() && dir_entry.path().extension() == ".dot")
            {
                if (dir_entry.path().filename().string().find(query_id) != std::string::npos)
                {
                    continue;
                }
                std::filesystem::remove_all(dir_entry.path());
            }
        }
    }
    catch (...)
    {
    }
}


String StepPrinter::printProjectionStep(const ProjectionStep & step)
{
    std::stringstream details;
    bool has_new_symbol = false;

    details << "New Assignments : \\n";
    {
        NameSet input_symbols;

        for (auto & column : step.getInputStreams()[0].header)
            input_symbols.insert(column.name);

        for (const auto & project : step.getAssignments())
        {
            if (input_symbols.find(project.first) == input_symbols.end())
            {
                has_new_symbol = true;
                String sql = serializeAST(*project.second);
                String type;
                if (auto literal = project.second->as<ASTLiteral>())
                {
                    type = applyVisitor(FieldToDataType(), literal->value)->getName();
                }
                details << project.first << ": " << sql << type << "\\n";
            }
        }
    }

    details << "|";
    details << "Full Assignments : \\n";
    for (const auto & project : step.getAssignments())
    {
        String sql = serializeAST(*project.second);
        String type;
        if (auto literal = project.second->as<ASTLiteral>())
        {
            type = applyVisitor(FieldToDataType(), literal->value)->getName();
        }
        details << project.first << ": " << sql << type << "\\n";
    }

    if (has_new_symbol)
    {
        details << "|";
        details << "Output \\n";
        for (auto & column : step.getOutputStream().header)
        {
            details << column.name << ":";
            details << column.type->getName() << "\\n";
        }
    }

    if (step.isFinalProject())
        details << "|"
                << "final";

    if (!step.getDynamicFilters().empty())
    {
        details << "|"
                << "Dynamic Filters \\n";

        auto f = [](DynamicFilterType type) {
            switch (type)
            {
                case DynamicFilterType::Range:
                    return "Range";
                case DynamicFilterType::BloomFilter:
                    return "BloomFilter";
            }
            return "Unknown";
        };

        for (const auto & item : step.getDynamicFilters())
        {
            details << std::to_string(item.second.id) << " : " << item.first << " ";
            for (auto type : item.second.types)
            {
                details << f(type) << " ";
            }
            details << "\\n";
        }
    }
    return details.str();
}
String StepPrinter::printFilterStep(const FilterStep & step)
{
    std::stringstream details;
    details << printFilter(step.getFilter());
    details << "|";
    details << "Output \\n";
    for (const auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printJoinStep(const JoinStep & step)
{
    const Names & left = step.getLeftKeys();
    const Names & right = step.getRightKeys();
    ASTTableJoin::Kind kind = step.getKind();
    std::stringstream details;

    auto f = [](ASTTableJoin::Kind v) {
        switch (v)
        {
            case ASTTableJoin::Kind::Inner:
                return "INNER";
            case ASTTableJoin::Kind::Left:
                return "LEFT";
            case ASTTableJoin::Kind::Right:
                return "RIGHT";
            case ASTTableJoin::Kind::Full:
                return "FULL";
            case ASTTableJoin::Kind::Cross:
                return "CROSS";
            case ASTTableJoin::Kind::Comma:
                return "COMMA";
        }
    };

    auto strictnessf = [](ASTTableJoin::Strictness v) {
        switch (v)
        {
            case ASTTableJoin::Strictness::Unspecified:
                return "Unspecified";
            case ASTTableJoin::Strictness::RightAny:
                return "RightAny";
            case ASTTableJoin::Strictness::Any:
                return "Any";
            case ASTTableJoin::Strictness::Asof:
                return "Asof";
            case ASTTableJoin::Strictness::All:
                return "All";
            case ASTTableJoin::Strictness::Semi:
                return "Semi";
            case ASTTableJoin::Strictness::Anti:
                return "Anti";
        }
    };

    auto inequality = [](ASOF::Inequality v) {
        switch (v)
        {
            case ASOF::Inequality::None:
                return "None";
            case ASOF::Inequality::Less:
                return "Less";
            case ASOF::Inequality::Greater:
                return "Greater";
            case ASOF::Inequality::LessOrEquals:
                return "LessOrEquals";
            case ASOF::Inequality::GreaterOrEquals:
                return "GreaterOrEquals";
        }
    };

    if (step.isMagic())
    {
        details << "MagicSet"
                << "|";
    }

    details << "JoinKind:" << f(kind);
    details << "|";
    details << "JoinStrictness : " << strictnessf(step.getStrictness());
    details << "|";
    details << "JoinKeys\\n";
    for (int i = 0; i < static_cast<int>(left.size()); ++i)
    {
        details << left.at(i) << "=" << right.at(i) << "\\n";
    }
    details << "|";
    if (!PredicateUtils::isTruePredicate(step.getFilter()))
    {
        details << "JoinFilter\\n";
        details << step.getFilter()->getColumnName();
        details << "|";
    }
    details << inequality(step.getAsofInequality());
    details << "|";
    details << "Output: \\n";
    for (const auto & item : step.getOutputStream().header)
    {
        details << item.name << ":";
        details << item.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printAggregatingStep(const AggregatingStep & step)
{
    std::stringstream details;
    details << "GroupBy:\\n";
    auto keys = step.getKeys();
    for (auto & key : keys)
    {
        details << key << "\\n";
    }
    details << "|";
    details << "Functions:\\n";
    const AggregateDescriptions & descs = step.getAggregates();
    for (const auto & desc : descs)
    {
        String func_name = desc.function->getName();
        auto type_name = String(typeid(desc.function.get()).name());
        if (type_name.find("AggregateFunctionNull"))
        {
            func_name = String("AggNull(").append(std::move(func_name)).append(")");
        }
        details << desc.column_name << ":=" << func_name;
        details << "( ";
        details << "Argument:";
        for (const auto & argument : desc.argument_names)
        {
            details << argument << " ";
        }
        details << "Types:";
        for (const auto & type : desc.function->getArgumentTypes())
        {
            details << type->getName() << " ";
        }
        details << ")";
        details << "\\n";
        if (!desc.mask_column.empty())
        {
            details << " mask: " << desc.mask_column;
        }
        details << "\\n";
    }

    if (step.isGroupingSet())
    {
        details << "|";
        details << "Grouping Set\\n";
        for (const auto & set : step.getGroupingSetsParams())
        {
            details << "( ";
            for (const auto & name : set.used_key_names)
            {
                details << name << ", ";
            }
            details << ") ";
        }
    }

    if (!step.getGroupings().empty())
    {
        details << "|";
        details << "Grouping\\n";
        for (const auto & set : step.getGroupings())
        {
            details << set.output_name << ':';
            for (auto arg : set.argument_names)
            {
                details << arg<< ',';
            }
            details << "; ";
        }
    }

    details << "|";
    details << "Output\\n";
    for (const auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    if (step.isFinal())
        details << "|"
                << "final";
    //    if (step.isTotals())
    //        details << "|"
    //                << "totals";
    return details.str();
}

String StepPrinter::printMergingAggregatedStep(const MergingAggregatedStep & step)
{
    std::stringstream details;
    details << "GroupBy:\\n";
    auto keys = step.getKeys();
    for (auto & key : keys)
    {
        details << key << "\\n";
    }
    details << "|";
    details << "Functions:\\n";
    const AggregateDescriptions & descs = step.getParams()->params.aggregates;
    for (auto & desc : descs)
    {
        String func_name = desc.function->getName();
        auto type_name = String(typeid(desc.function.get()).name());
        if (type_name.find("AggregateFunctionNull"))
        {
            func_name = String("AggNull(").append(std::move(func_name)).append(")");
        }
        details << desc.column_name << ":=" << func_name;
        details << "( ";
        details << "Argument:";
        for (const auto & argument : desc.argument_names)
        {
            details << argument << " ";
        }
        details << "Types:";
        for (const auto & type : desc.function->getArgumentTypes())
        {
            details << type->getName() << " ";
        }
        details << ")";
        details << "\\n";
        if (!desc.mask_column.empty())
        {
            details << " mask: " << desc.mask_column;
        }
        details << "\\n";
    }

    if (!step.getGroupings().empty())
    {
        details << "|";
        details << "Grouping\\n";
        for (const auto & set : step.getGroupings())
        {
            details << set.output_name << ':';
            for (auto arg : set.argument_names)
            {
                details << arg<< ',';
            }
            details << "; ";
        }
    }

    if (step.getParams()->final)
        details << "|"
                << "final";
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printUnionStep(const UnionStep & step)
{
    std::stringstream details;
    if (step.isLocal())
    {
        details << "local union"
                << "|";
    }
    details << "OutputToInputs"
            << "|";

    for (auto & output_to_input : step.getOutToInputs())
    {
        details << output_to_input.first << ":";
        for (auto & output : output_to_input.second)
        {
            details << output << ",";
        }
        details << "\\n";
    }
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printIntersectStep(const IntersectStep & step)
{
    std::stringstream details;
    details << "IntersectNode";
    details << "|";
    details << "Distinct: " << step.isDistinct();
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printExceptStep(const ExceptStep & step)
{
    std::stringstream details;
    details << "ExceptNode";
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printExchangeStep(const ExchangeStep & step)
{
    std::stringstream details;
    auto f = [](ExchangeMode mode) {
        switch (mode)
        {
            case ExchangeMode::UNKNOWN:
                return "UNKNOWN";
            case ExchangeMode::LOCAL_NO_NEED_REPARTITION:
                return "LOCAL_NO_NEED_REPARTITION";
            case ExchangeMode::LOCAL_MAY_NEED_REPARTITION:
                return "LOCAL_MAY_NEED_REPARTITION";
            case ExchangeMode::BROADCAST:
                return "BROADCAST";
            case ExchangeMode::REPARTITION:
                return "REPARTITION";
            case ExchangeMode::GATHER:
                return "GATHER";
        }
    };
    details << "ExchangeNode " << f(step.getExchangeMode());
    if (step.needKeepOrder())
    {
        details << "|";
        details << "Keep Order\\n";
    }
    details << "|";
    details << "Shuffle Keys \\n";
    for (auto & column : step.getSchema().getPartitioningColumns())
    {
        details << column << " ";
    }
    details << "|";
    details << "Output \\n";
    for (const auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}
String StepPrinter::printRemoteExchangeSourceStep(const RemoteExchangeSourceStep & step)
{
    std::stringstream details;
    details << "Input Segments:[ ";
    auto inputs = step.getInput();
    for (const auto & input : inputs)
    {
        const size_t segment_id = input->getPlanSegmentId();
        details << segment_id << " ";
    }
    details << "]";
    return details.str();
}

String StepPrinter::printTableScanStep(const TableScanStep & step)
{
    //    auto distributed_table = dynamic_cast<StorageDistributed *>(step->getStorage().get());
    String database = step.getDatabase();
    String table = step.getTable();
    std::stringstream details;
    details << database << "." << table << "|";

    //    if (step.getStorage()->isBucketTable())
    //    {
    //        auto & storage = dynamic_cast<MergeTreeMetaBase &>(*(step.getStorage()));
    //        details << "cluster by ";
    //        for (const auto & item : storage.cluster_by_columns)
    //        {
    //            details << item << " ";
    //        }
    //        details << "into " << storage.cluster_by_total_bucket_number << '|';
    //    }
    //
    auto & query_info = step.getQueryInfo();
    auto query = query_info.query->as<ASTSelectQuery>();
    if (query->getWhere())
    {
        details << "Filter : \\n";
        details << printFilter(query->refWhere());
        details << "|";
    }

    if (query->getPrewhere())
    {
        details << "Prewhere : \\n";
        details << printFilter(query->refPrewhere());
        details << "|";
    }

    if (query->getLimitLength())
    {
        details << "Limit : \\n";
        Field converted = convertFieldToType(query->refLimitLength()->as<ASTLiteral>()->value, DataTypeUInt64());
        details << converted.safeGet<UInt64>();
        details << "|";
    }
    //
    //    details << "Block Size : \\n" << step.getMaxBlockSize() << "|";
    //
    //    details << "Alias: \\n";
    //    for (const auto & assigment : step.getColumnAlias())
    //    {
    //        details << assigment.second << ": " << assigment.first << "\\n";
    //    }
    //    details << "|";
    details << "Output \\n";
    for (const auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printValuesStep(const ValuesStep & step)
{
    std::stringstream details;
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    details << "|";
    details << "Rows :" << step.getRows();
    return details.str();
}

String StepPrinter::printLimitStep(const LimitStep & step)
{
    std::stringstream details;
    auto limit = step.getLimit();
    auto offset = step.getOffset();
    details << "Limit:" << limit << "|";
    details << "Offset:" << offset;
    details << "|";
    details << "Output\\n";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    if (step.isPartial())
        details << "|"
                << " Partial";
    return details.str();
}
String StepPrinter::printLimitByStep(const LimitByStep & step)
{
    std::stringstream details;
    details << "Limit value : " << step.getGroupLength();
    details << "|";
    details << "Limit columns : ";
    for (auto & column : step.getColumns())
        details << column << ", ";
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printMergeSortingStep(const MergeSortingStep & step)
{
    std::stringstream details;
    details << "Order By:\\n";
    auto & descs = step.getSortDescription();
    for (auto & desc : descs)
    {
        details << desc.column_name << " " << desc.direction << " " << desc.nulls_direction << "\\n";
    }
    details << "|";
    details << "Limit: " << step.getLimit();
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printSortingStep(const SortingStep & step)
{
    std::stringstream details;
    details << "Order By:\\n";
    auto & descs = step.getSortDescription();
    for (auto & desc : descs)
    {
        details << desc.column_name << " " << desc.direction << " " << desc.nulls_direction << "\\n";
    }
    details << "|";
    details << "Limit: " << step.getLimit();
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printPartialSortingStep(const PartialSortingStep & step)
{
    std::stringstream details;
    details << "Order By:\\n";
    auto & descs = step.getSortDescription();
    for (auto & desc : descs)
    {
        details << desc.column_name << " " << desc.direction << " " << desc.nulls_direction << "\\n";
    }
    details << "|";
    details << "Limit: " << step.getLimit();
    /*
    details << "|";
    details << "Output |";
    for (auto & column : step_ptr->getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
     */
    return details.str();
}

String StepPrinter::printMergingSortedStep(const MergingSortedStep & step)
{
    std::stringstream details;
    details << "Order By:\\n";
    auto & descs = step.getSortDescription();
    for (auto & desc : descs)
    {
        details << desc.column_name << "\\n";
    }

    details << "|";
    details << "Limit: " << step.getLimit();

    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printDistinctStep(const DistinctStep & step)
{
    std::stringstream details;
    details << "Columns:\\n";
    for (auto & name : step.getColumns())
    {
        details << name << "\\n";
    }
    details << "|";
    details << "limit:\\n";
    details << step.getLimitHint();
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printApplyStep(const ApplyStep & step)
{
    auto f = [](ApplyStep::ApplyType v) {
        switch (v)
        {
            case ApplyStep::ApplyType::CROSS:
                return "CROSS";
            case ApplyStep::ApplyType::LEFT:
                return "LEFT";
            case ApplyStep::ApplyType::SEMI:
                return "SEMI";
            case ApplyStep::ApplyType::ANTI:
                return "ANTI";
        }
    };

    std::stringstream details;
    details << "ApplyType : " << f(step.getApplyType());
    details << "|";
    details << "Correlation \\n";
    for (auto & name : step.getCorrelation())
    {
        details << name << " ";
    }

    auto subqueryType = [](ApplyStep::SubqueryType v) {
        switch (v)
        {
            case ApplyStep::SubqueryType::SCALAR:
                return "SCALAR";
            case ApplyStep::SubqueryType::IN:
                return "IN";
            case ApplyStep::SubqueryType::EXISTS:
                return "EXISTS";
            case ApplyStep::SubqueryType::QUANTIFIED_COMPARISON:
                return "QUANTIFIED_COMPARISON";
        }
    };

    details << "|";
    details << "SubqueryType " << subqueryType(step.getSubqueryType());
    if (step.getAssignment().second)
    {
        details << "|";
        details << "Assignment \\n";
        details << step.getAssignment().first << " = " << serializeAST(*step.getAssignment().second);
    }
    details << "|";
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}
String StepPrinter::printEnforceSingleRowStep(const EnforceSingleRowStep & step)
{
    std::stringstream details;

    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}
String StepPrinter::printAssignUniqueIdStep(const AssignUniqueIdStep & step)
{
    std::stringstream details;
    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printCTERefStep(const CTERefStep & step)
{
    std::stringstream details;
    details << "CTEId: " << step.getId() << "|";
    details << "Columns\\n";
    for (const auto & item : step.getOutputColumns())
    {
        details << item.first << ":";
        details << item.second << "\\n";
    }
    details << "Output\\n";
    for (const auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }

    return details.str();
}

String StepPrinter::printPartitionTopNStep(const PartitionTopNStep & step)
{
    std::stringstream details;
    details << "Partition";
    for (auto & desc : step.getPartition())
    {
        details << desc << ", ";
    }
    details << "|";

    details << "Order by";
    for (auto & desc : step.getOrderBy())
    {
        details << desc << ", ";
    }
    details << "|";

    details << step.getModel();
    details << "|";

    details << "Limit: " << step.getLimit();
    details << "|";

    details << "Output |";
    for (auto & column : step.getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
    return details.str();
}

String StepPrinter::printWindowStep(const WindowStep & step)
{
    std::stringstream details;

    const auto & window = step.getWindow();

    details << "Partition Key\\n";
    for (const auto & pk : window.partition_by)
        details << pk.column_name << "\\n";
    details << "|";
    details << "Sort Key\\n";
    for (const auto & sk : window.order_by)
        details << sk.column_name << " " << (sk.direction == 1 ? "ASC" : "DESC") << "\\n";
    details << "|";
    details << "Frame Type\\n";
    details << window.frame.toString();

    const auto & functions = step.getFunctions();
    details << "|";
    details << "Window Functions\\n";

    for (const auto & func : functions)
    {
        details << func.column_name << ": ";
        details << func.aggregate_function->getName() << "(";
        for (const auto & arg : func.argument_names)
            details << arg << ",";
        details << ")\\n";
    }
    /*
    details << "|";
    details << "Output |";
    for (auto & column : step_ptr->getOutputStream().header)
    {
        details << column.name << ":";
        details << column.type->getName() << "\\n";
    }
     */
    return details.str();
}

String StepPrinter::printFilter(const ConstASTPtr & filter)
{
    auto conjuncts = PredicateUtils::extractConjuncts(filter);
    if (conjuncts.empty())
        return "";

    WriteBufferFromOwnString buf;
    IAST::FormatSettings settings(buf, true);
    settings.hilite = false;
    settings.always_quote_identifiers = true;
    settings.identifier_quoting_style = IdentifierQuotingStyle::Backticks;
    conjuncts[0]->format(settings);
    for (size_t i = 1; i < conjuncts.size(); i++)
    {
        buf << "\\nAND ";
        conjuncts[i]->format(settings);
    }

    return buf.str();
}

Void PlanNodeEdgePrinter::visitCTERefNode(CTERefNode & node, Void & c)
{
    const auto & step = dynamic_cast<const CTERefStep &>(*node.getStep().get());
    if (cte_helper)
    {
        if (!cte_helper->hasVisited(step.getId()))
        {
            auto & cte_plan = *cte_helper.value().getCTEInfo().getCTEDef(step.getId());
            out << "plannode_" << cte_plan.getId() << " -> "
                << "cte_" << step.getId() << std::endl;
        }
        out << "cte_" << step.getId() << " -> "
            << "plannode_" << node.getId() << "[style=dashed];" << std::endl;
        cte_helper->accept(step.getId(), *this, c);
    }
    return Void{};
}

void cleanDotFiles(const ContextPtr & context)
{
    std::filesystem::path graphviz_path(context->getSettingsRef().graphviz_path.toString());

    try
    {
        if (!std::filesystem::exists(graphviz_path))
        {
            std::filesystem::create_directory(graphviz_path);
            return;
        }

        auto query_id = context->getInitialQueryId();

        for (auto & dir_entry : std::filesystem::directory_iterator(graphviz_path))
        {
            if (dir_entry.is_regular_file() && dir_entry.path().extension() == ".dot")
            {
                if (dir_entry.path().filename().string().find(query_id) != std::string::npos)
                {
                    continue;
                }
                std::filesystem::remove_all(dir_entry.path());
            }
        }
    }
    catch (...)
    {
    }
}

void GraphvizPrinter::printAST(const ASTPtr & astPtr, ContextMutablePtr & context, const String & visitor)
{
    if (context->getSettingsRef().print_graphviz)
    {
        cleanDotFiles(context);

        std::stringstream path;
        path << context->getSettingsRef().graphviz_path.toString();
        path << visitor << "-" << context->getInitialQueryId() << ".dot";
        std::ofstream out(path.str());
        out << GraphvizPrinter::printAST(astPtr);
        out.close();
    }
}

void GraphvizPrinter::printLogicalPlan(PlanNodeBase & root, ContextMutablePtr & context, const String & name)
{
    if (context->getSettingsRef().print_graphviz)
    {
        cleanDotFiles(context);

        std::stringstream path;
        path << context->getSettingsRef().graphviz_path.toString();
        path << name << "-" << context->getInitialQueryId() << ".dot";

        std::ofstream out(path.str());
        out << GraphvizPrinter::printLogicalPlan(root);
        out.close();
    }
}

void GraphvizPrinter::printLogicalPlan(QueryPlan & plan, ContextMutablePtr & context, const String & name)
{
    if (context->getSettingsRef().print_graphviz)
    {
        cleanDotFiles(context);

        std::stringstream path;
        path << context->getSettingsRef().graphviz_path.toString();
        path << name << "-" << context->getInitialQueryId() << ".dot";

        std::ofstream out(path.str());
        out << GraphvizPrinter::printLogicalPlan(*plan.getPlanNode(), &plan.getCTEInfo());
        out.close();
    }
}

void GraphvizPrinter::printPipeline(
    const Processors & processors, const ExecutingGraphPtr & graph, const ContextPtr & context, size_t segment_id, const String & host)
{
    if (context->getSettingsRef().print_graphviz)
    {
        cleanDotFiles(context);
        {
            std::stringstream path;
            path << context->getSettingsRef().graphviz_path.toString();
            path << PIPELINE_PATH << "-grouped"
                 << "-" << context->getInitialQueryId() << "_" << segment_id << "_" << host << ".dot";
            std::ofstream out(path.str());
            out << printGroupedPipeline(processors, graph);
            out.close();
        }
        {
            std::stringstream path;
            path << context->getSettingsRef().graphviz_path.toString();
            path << PIPELINE_PATH << "-" << context->getInitialQueryId() << "_" << segment_id << "_" << host << ".dot";
            std::ofstream out(path.str());
            out << printPipeline(processors, graph);
            out.close();
        }
    }
}

String GraphvizPrinter::printPipeline(const Processors & processors, const ExecutingGraphPtr & graph)
{
    if (graph)
    {
        for (const auto & node : graph->nodes)
        {
            {
                WriteBufferFromOwnString buffer;
                buffer << "|" << node->num_executed_jobs << " jobs";

#ifndef NDEBUG
                if (node->processor)
                {
                    size_t input_rows = 0;
                    size_t output_rows = 0;
                    for (const auto & output : node->processor->getOutputs())
                        output_rows += output.getRows();
                    for (const auto & input : node->processor->getInputs())
                        input_rows += input.getRows();

                    buffer << "\\n"
                           << "output: " << output_rows << " rows";
                    buffer << "\\n"
                           << "input: " << input_rows << " rows";
                }

                buffer << "\\n"
                       << "execution time: " << node->execution_time_ns / 1e9 << " sec.";
                buffer << "\\n"
                       << "preparation time: " << node->preparation_time_ns / 1e9 << " sec.";
#endif

                buffer << "|";
                node->processor->setDescription(buffer.str());
            }
        }
    }

    std::vector<IProcessor::Status> statuses;
    std::vector<IProcessor *> proc_list;
    statuses.reserve(graph->nodes.size());
    proc_list.reserve(graph->nodes.size());

    for (const auto & node : graph->nodes)
    {
        proc_list.emplace_back(node->processor);
        statuses.emplace_back(node->last_processor_status);
    }

    WriteBufferFromOwnString out;
    DB::printPipeline(processors, statuses, out, "BT");
    out.finalize();

    return out.str();
}

String GraphvizPrinter::printGroupedPipeline(const Processors & processors, const ExecutingGraphPtr & graph)
{
    using ProcessorId = size_t;
    std::vector<ProcessorId> roots;
    std::unordered_map<ProcessorId, std::vector<ProcessorId>> dag;

    std::unordered_map<const IProcessor *, ProcessorId> processor_to_id_map;
    for (size_t i = 0; i < processors.size(); ++i)
        processor_to_id_map[processors[i].get()] = i;

    /// build processors DAG, and find root
    for (const auto & from : processors)
    {
        auto from_id = processor_to_id_map.at(from.get());
        for (const auto & port : from->getOutputs())
        {
            if (!port.isConnected())
                continue;

            const IProcessor & out = port.getInputPort().getProcessor();
            const auto out_id = processor_to_id_map.at(&out);
            dag[from_id].emplace_back(out_id);
        }

        bool is_root = true;
        for (const auto & port : from->getInputs())
        {
            if (!port.isConnected())
                continue;
            is_root = false;
            break;
        }
        if (is_root)
            roots.emplace_back(from_id);
    }

    // collect additional information
    struct ExecutionStats
    {
        size_t num_executed_jobs = 0;
        size_t execution_time_ns = 0;
        size_t preparation_time_ns = 0;
    };
    std::unordered_map<ProcessorId, ExecutionStats> processor_execution_stats;
    if (graph)
    {
        for (const auto & node : graph->nodes)
        {
            processor_execution_stats.emplace(
                processor_to_id_map.at(node->processor),
                ExecutionStats{
                    .num_executed_jobs = node->num_executed_jobs,
                    .execution_time_ns = node->execution_time_ns,
                    .preparation_time_ns = node->preparation_time_ns});
        }
    }

    struct GroupedStats
    {
        size_t parallel_size = 0;
        size_t sum_executed_jobs = 0;
        size_t sum_execution_time = 0;
        size_t sum_preparation_time = 0;

        size_t sum_input_ports = 0;
        size_t sum_input_rows = 0;
        size_t sum_output_ports = 0;
        size_t sum_output_rows = 0;

        // for input exchange
        size_t local_exchange_input_rows = 0;
        size_t remote_exchange_input_rows = 0;

        void merge(const IProcessor & processor, const ExecutionStats & stats)
        {
            parallel_size += 1;
            sum_executed_jobs += stats.num_executed_jobs;
            sum_execution_time += stats.execution_time_ns;
            sum_preparation_time += stats.preparation_time_ns;

            sum_input_ports += processor.getInputs().size();
            sum_output_ports += processor.getOutputs().size();
#ifndef NDEBUG
            for (const auto & input : processor.getInputs())
                sum_input_rows += input.getRows();
            for (const auto & output : processor.getOutputs())
                sum_output_rows += output.getRows();
#endif
        }
    };

    struct ProcessorGroup;
    using ProcessorGroupPtr = std::shared_ptr<ProcessorGroup>;
    struct ProcessorGroup
    {
        explicit ProcessorGroup(size_t id_, String name_) : id(id_), name(name_) { }

        size_t id;
        String name;
        std::unordered_set<ProcessorId> processor_ids;
        GroupedStats stats;
        bool visited = false;

        std::unordered_map<String, ProcessorGroupPtr> children;

        void add(ProcessorId processor_id, const IProcessor & processor, const ExecutionStats & execution_stats)
        {
            if (processor_ids.count(processor_id))
                return;
            processor_ids.emplace(processor_id);
            stats.merge(processor, execution_stats);
        }
    };

    /// Build processor group tree
    std::vector<ProcessorGroupPtr> groups;
    std::unordered_map<ProcessorId, ProcessorGroupPtr> processor_to_group;
    auto root = std::make_shared<ProcessorGroup>(0, "Root");
    root->visited = true;
    groups.emplace_back(root);

    /// BFS
    for (const auto & id : roots)
    {
        auto & group_children = root->children;
        auto & root_processor = *processors[id];
        if (!group_children.contains(root_processor.getName()))
        {
            auto child = std::make_shared<ProcessorGroup>(groups.size(), root_processor.getName());
            group_children.emplace(root_processor.getName(), child);
            groups.emplace_back(child);
        }
        group_children[root_processor.getName()]->add(id, root_processor, processor_execution_stats[id]);
        processor_to_group.emplace(id, group_children[root_processor.getName()]);
    }

    for (size_t i = 1; i < groups.size(); i++)
    {
        auto group = groups[i];
        if (group->visited)
            continue;
        group->visited = true;

        /// Build child group, then add it to children.
        auto & group_children = group->children;
        for (const auto & processor_id : group->processor_ids)
            for (auto & child_processor_id : dag[processor_id])
            {
                const auto & child_processor = *processors[child_processor_id];
                if (!group_children.contains(child_processor.getName()))
                {
                    if (processor_to_group.count(child_processor_id))
                        group_children.emplace(child_processor.getName(), processor_to_group[child_processor_id]);
                    else
                    {
                        auto child = std::make_shared<ProcessorGroup>(groups.size(), child_processor.getName());
                        group_children.emplace(child_processor.getName(), child);
                        groups.emplace_back(child);
                    }
                }
                group_children[child_processor.getName()]->add(
                    child_processor_id, child_processor, processor_execution_stats[processor_id]);
                processor_to_group.emplace(child_processor_id, group_children[child_processor.getName()]);
            }
    }


    std::stringstream out;
    out << "digraph\n{\n";
    out << "  rankdir=\"BT\";\n";
    out << "  { graph [compound=true];\n node [shape=record];\n";

    /// Print Nodes
    for (size_t i = 1; i < groups.size(); i++)
    {
        auto & group = groups[i];
        out << "    group" << group->id;
        out << "[label=\"{" << group->name;
        if (group->id != 0)
        {
            out << "|"
                << "Parallel: x" << group->stats.parallel_size;
            out << "|"
                << "Executed Jobs: " << group->stats.sum_executed_jobs;
#ifndef NDEBUG
            out << "\\n"
                << "Execution Times: " << group->stats.sum_execution_time / 1e9 << " sec.";
            out << "\\n"
                << "Preparation Times: " << group->stats.sum_preparation_time / 1e9 << " sec.";
            if (group->stats.sum_output_ports)
                out << "|"
                    << "Outputs rows: " << group->stats.sum_output_rows;
            if (group->stats.sum_input_ports)
                out << "\\n"
                    << "Inputs rows: " << group->stats.sum_input_rows;
#endif
        }
        out << "}\"];\n";
    }

    out << "  }\n";

    /// Print Edges
    for (size_t i = 1; i < groups.size(); i++)
    {
        auto & group = groups[i];
        for (const auto & child : group->children)
            out << "  group" << group->id << " -> group" << child.second->id << ";\n";
    }

    out << "}\n";

    return out.str();
}

void GraphvizPrinter::printMemo(const Memo & memo, const ContextMutablePtr & context, const String & name)
{
    printMemo(memo, UNDEFINED_GROUP, context, name);
}

void GraphvizPrinter::printMemo(const Memo & memo, GroupId root_id, const ContextMutablePtr & context, const String & name)
{
    if (context->getSettingsRef().print_graphviz)
    {
        cleanDotFiles(context);

        std::stringstream path;
        path << context->getSettingsRef().graphviz_path.toString();
        path << name << "-" << context->getInitialQueryId() << ".dot";

        std::ofstream out(path.str());
        out << GraphvizPrinter::printMemo(memo, root_id);
        out.close();
    }
}

void GraphvizPrinter::printPlanSegment(const PlanSegmentTreePtr & segment, const ContextMutablePtr & context)
{
    if (context->getSettingsRef().print_graphviz)
    {
        cleanDotFiles(context);

        std::stringstream path;
        path << context->getSettingsRef().graphviz_path.toString();
        path << "4000-PlanSegment"
             << "-" << context->getInitialQueryId() << ".dot";
        std::ofstream out(path.str());
        out << GraphvizPrinter::printPlanSegment(segment);
        out.close();
    }
}

void GraphvizPrinter::printBlock(const String & stream, const Block & header, const Block & data)
{
    WriteBufferFromOwnString string;
    //    FormatSettings settings;
    //    CSVRowOutputStream csvRowOutputStream{string, header, true, settings};

    std::cout << "====================" << stream << "========================" << std::endl;
    for (auto & column : header)
    {
        std::cout << column.name << "|";
    }

    std::cout << "\n";

    for (size_t i = 0; i < data.rows(); ++i)
    {
        //        csvRowOutputStream.write(data, i);
    }
    std::cout << string.str();
    std::cout << "\n" << std::flush;
}

void appendAST(
    std::stringstream & out,
    ASTPtr & ast,
    const ASTPtr & parent,
    std::unordered_map<ASTPtr, UInt16> & asts,
    std::vector<std::pair<UInt16, UInt16>> & edges)
{
    String label = [&]() -> String {
        if (auto select_query = std::dynamic_pointer_cast<ASTSelectQuery>(parent))
        {
            if (ast == select_query->with())
                return "WITH";
            if (ast == select_query->select())
                return "SELECT";
            if (ast == select_query->tables())
                return "FROM";
            if (ast == select_query->prewhere())
                return "PREWHERE";
            if (ast == select_query->where())
                return "WHERE";
            if (ast == select_query->groupBy())
                return "GROUP BY";
            if (ast == select_query->having())
                return "HAVING";
            if (ast == select_query->window())
                return "WINDOW";
            if (ast == select_query->orderBy())
                return "ORDER BY";
            if (ast == select_query->limitBy())
                return "LIMIT BY";
            if (ast == select_query->limitOffset())
                return "LIMIT OFFSET";
            if (ast == select_query->limitLength())
                return "LIMIT LENGTH";
            if (ast == select_query->settings())
                return "SETTINGS";
        }

        if (auto func = std::dynamic_pointer_cast<ASTFunction>(parent))
        {
            if (ast == func->arguments)
                return "Function Args";
            if (ast == func->window_definition)
                return "Window Spec";
        }

        return ast->getID();
    }();

    bool print_sql = [&]() -> bool {
        if (ast->as<ASTExpressionList>())
            return false;
        if (ast->as<ASTTablesInSelectQuery>())
            return false;
        if (ast->as<ASTTablesInSelectQueryElement>())
            return false;
        if (ast->as<ASTTableExpression>())
            return false;
        return true;
    }();

    std::stringstream details;
    String sql = serializeAST(*ast);

    // handle escape characters that are special for graphviz
    boost::replace_all(sql, "<", "\\<");
    boost::replace_all(sql, ">", "\\>");
    boost::replace_all(sql, "\"", "\\\">");

#define MAX_PRINT_CHARACTERS 100
    if (sql.size() > MAX_PRINT_CHARACTERS)
    {
        sql.resize(MAX_PRINT_CHARACTERS);
        sql += "...";
    }
#undef MAX_PRINT_CHARACTERS

    details << "SQL:" << sql;

    String color{"bisque"};
    out << "ast_" << asts.at(ast) << R"([label="{)" << label;

    if (print_sql)
        out << "|" << details.str();

    out << R"(}", style="rounded, filled", shape=record, fillcolor=)" << color << "]"
        << ";" << std::endl;

    ASTs children = [&]() -> ASTs {
        if (auto * select_with_union = ast->as<ASTSelectWithUnionQuery>())
            return select_with_union->list_of_selects->children;
        if (auto * table_elem = ast->as<ASTTablesInSelectQueryElement>())
        {
            ASTs result;
            if (auto table_expr = std::dynamic_pointer_cast<ASTTableExpression>(table_elem->table_expression))
            {
                if (table_expr->database_and_table_name)
                    result.push_back(table_expr->database_and_table_name);
                if (auto table_subquery = std::dynamic_pointer_cast<ASTSubquery>(table_expr->subquery))
                    result.push_back(table_subquery->children[0]);
                if (table_expr->table_function)
                    result.push_back(table_expr->table_function);
            }
            if (table_elem->table_join)
            {
                result.push_back(table_elem->table_join);
            }
            if (table_elem->array_join)
            {
                result.push_back(table_elem->array_join);
            }
            return result;
        }
        return ast->getChildren();
    }();

    for (auto & child : children)
    {
        edges.emplace_back(asts.at(ast), asts.at(child));
        appendAST(out, child, ast, asts, edges);
    }
}

void appendASTEdge(std::stringstream & out, std::vector<std::pair<UInt16, UInt16>> & edges)
{
    for (auto & edge : edges)
    {
        out << "ast_" << edge.first << " -> "
            << "ast_" << edge.second << ";" << std::endl;
    }
}

String GraphvizPrinter::printAST(ASTPtr ptr)
{
    std::unordered_map<ASTPtr, UInt16> asts;
    std::shared_ptr<std::atomic<UInt16>> max_node_id = std::make_unique<std::atomic<UInt16>>(0);
    std::vector<std::pair<UInt16, UInt16>> edges;

    addID(ptr, asts, max_node_id);

    std::stringstream out;
    out << "digraph ast {\n";
    out << "subgraph {\n";
    appendAST(out, ptr, nullptr, asts, edges);
    out << "}\n";
    appendASTEdge(out, edges);
    out << "}\n";
    return out.str();
}

void GraphvizPrinter::addID(ASTPtr & ast, std::unordered_map<ASTPtr, UInt16> & asts, std::shared_ptr<std::atomic<UInt16>> & max_node_id)
{
    asts.emplace(ast, (*max_node_id)++);
    ASTs & children = ast->getChildren();
    for (auto & child : children)
    {
        addID(child, asts, max_node_id);
    }
}

String GraphvizPrinter::printLogicalPlan(PlanNodeBase & node, CTEInfo * cte_info)
{
    std::stringstream out;
    out << "digraph logical_plan {\n rankdir=\"BT\" \n";
    out << "subgraph {\n";
    PrinterContext printer_context{};
    PlanNodePrinter node_printer{out, true, cte_info};
    VisitorUtil::accept(node, node_printer, printer_context);
    out << "}\n";
    PlanNodeEdgePrinter edge_printer{out, cte_info};
    Void context{};
    VisitorUtil::accept(node, edge_printer, context);
    out << "}\n";
    return out.str();
}

String GraphvizPrinter::printPlanSegment(const PlanSegmentTreePtr & segmentNode)
{
    std::stringstream out;
    out << "digraph plan_segment {\n rankdir=\"BT\" \n";
    std::unordered_map<size_t, PlanSegmentPtr &> segments = segmentNode->getPlanSegmentsMap();
    std::unordered_set<PlanSegmentTree::Node *> visited_segments;
    appendPlanSegmentNodes(out, segmentNode->getRoot(), segments, visited_segments);
    out << "}\n";
    return out.str();
}

void GraphvizPrinter::appendPlanSegmentNodes(
    std::stringstream & out,
    PlanSegmentTree::Node * segmentNode,
    std::unordered_map<size_t, PlanSegmentPtr &> & segments,
    std::unordered_set<PlanSegmentTree::Node *> & visited)
{
    if (!visited.emplace(segmentNode).second)
        return;

    PlanSegmentPtr & plan_segment = segmentNode->plan_segment;

    appendPlanSegmentNode(out, plan_segment);

    QueryPlan::Node * plan = plan_segment->getQueryPlan().getRoot();
    PlanSegmentEdgePrinter edge_printer{out};
    VisitorUtil::accept(plan, edge_printer, segments);

    std::vector<PlanSegmentTree::Node *> & children = segmentNode->children;
    for (auto & child : children)
    {
        appendPlanSegmentNodes(out, child, segments, visited);
    }
}

void GraphvizPrinter::appendPlanSegmentNode(std::stringstream & out, const PlanSegmentPtr & segment_ptr)
{
    out << "subgraph ";
    out << "cluster_" << segment_ptr->getPlanSegmentId();
    out << "{\n";
    ExchangeMode mode = segment_ptr->getPlanSegmentOutput()->getExchangeMode();
    auto f = [](ExchangeMode mode_) {
        switch (mode_)
        {
            case ExchangeMode::UNKNOWN:
                return "UNKNOWN";
            case ExchangeMode::LOCAL_NO_NEED_REPARTITION:
                return "LOCAL_NO_NEED_REPARTITION";
            case ExchangeMode::LOCAL_MAY_NEED_REPARTITION:
                return "LOCAL_MAY_NEED_REPARTITION";
            case ExchangeMode::BROADCAST:
                return "BROADCAST";
            case ExchangeMode::REPARTITION:
                return "REPARTITION";
            case ExchangeMode::GATHER:
                return "GATHER";
        }
    };
    size_t segment_id = segment_ptr->getPlanSegmentId();
    out << "label = \"";
    out << "segment=[ " << segment_id << " ]\n";
    if (segment_id != 0)
    {
        out << "exchange=[ " << f(mode) << " ]\n";
        out << "shufflekeys=[ ";
        for (auto & key : segment_ptr->getPlanSegmentOutput()->getShufflekeys())
        {
            out << key << " ";
        }
        out << " ]\n";
    }
    out << "parallel_size " << segment_ptr->getParallelSize() << "\n";
    out << "cluster_name " << (segment_ptr->getClusterName().empty() ? "server" : segment_ptr->getClusterName()) << "\\n";
    out << "exchange_parallel_size " << segment_ptr->getExchangeParallelSize() << "\n";
    //    out << "exchange_output_parallel_size " << segment_ptr->getExchangeOutputParallelSize() << "\n";
    out << "\"";
    QueryPlan::Node * node = segment_ptr->getQueryPlan().getRoot();
    PrinterContext context{};
    PlanSegmentNodePrinter node_printer{out, true};
    VisitorUtil::accept(node, node_printer, context);
    out << "}\n";
}

static String printGroupEdges(const Memo & memo, GroupId root)
{
    std::stringstream out;

    std::unordered_map<GroupId, std::unordered_set<GroupId>> edge_winner;

    std::function<void(GroupId, const Property &)> findGroupWinner = [&](GroupId group_id, const Property & required_prop) {
        auto group = memo.getGroupById(group_id);
        auto winner = group->getBestExpression(required_prop);

        auto required_properties = winner->getRequireChildren();
        for (size_t index = 0; index < required_properties.size(); ++index)
        {
            auto & children_id = winner->getGroupExpr()->getChildrenGroups()[index];
            edge_winner[children_id].emplace(group_id);
            findGroupWinner(children_id, required_properties[index]);
        }
    };
    try
    {
        findGroupWinner(root, Property{Partitioning{Partitioning::Handle::SINGLE}});
    }
    catch (...)
    {
    }

    std::unordered_map<GroupId, std::unordered_set<GroupId>> edge_exists;
    for (auto & group : memo.getGroups())
    {
        GroupId father_id = group->getId();
        for (auto & expr : group->getLogicalExpressions())
        {
            for (GroupId children_id : expr->getChildrenGroups())
            {
                if (!edge_exists[children_id].contains(father_id))
                {
                    out << "group_" << children_id << "-> group_" << father_id;
                    if (edge_winner[children_id].contains(father_id))
                    {
                        out << " [penwidth = 4.0, color = red]";
                    }
                    out << ";\n";
                    edge_exists[children_id].emplace(father_id);
                }
            }
        }
    }

    for (auto & group : memo.getGroups())
    {
        for (auto & expr : group->getLogicalExpressions())
        {
            if (expr->getStep()->getType() == IQueryPlanStep::Type::CTERef)
            {
                auto cte_step = dynamic_cast<const CTERefStep *>(expr->getStep().get());
                auto cte_group = memo.getCTEDefGroupByCTEId(cte_step->getId());
                out << "group_" << cte_group->getId() << "-> group_" << group->getId();
                out << " [style=dashed];\n";
            }
        }
    }

    return out.str();
}

String GraphvizPrinter::printMemo(const Memo & memo, GroupId root)
{
    std::stringstream out;
    out << "digraph logical_plan {\n  rankdir=\"BT\" \n";
    out << "node[style=\"filled\", shape=record]\n";
    out << "subgraph {\n";

    for (auto & group : memo.getGroups())
    {
        out << printGroup(*group);
    }

    out << "}\n";

    if (root != UNDEFINED_GROUP)
    {
        out << printGroupEdges(memo, root);
    }
    else if (!memo.getGroups().empty())
    {
        GroupId pre_id = memo.getGroups()[0]->getId();
        for (size_t index = 1; index < memo.getGroups().size(); index++)
        {
            auto now_id = memo.getGroups()[index]->getId();
            out << "group_" << pre_id << "-> group_" << now_id << " [style=invis];\n";
            pre_id = now_id;
        }
    }

    out << "}\n";
    return out.str();
}

String GraphvizPrinter::printGroup(const Group & group)
{
    std::stringstream out;
    auto head_step = group.getLogicalExpressions()[0]->getStep().get();

    auto fold = [](std::string a, GroupId b) { return std::move(a) + ", " + std::to_string(b); };
    auto fold_string = [](String a, const String & b) { return std::move(a) + ", " + b; };

    auto expr_to_str = [&](const GroupExprPtr & expr) {
        String result = expr->getStep()->getName();

        if (expr->getChildrenGroups().empty())
            result += String(" []");
        else
            result += " ["
                + std::accumulate(
                          std::next(expr->getChildrenGroups().begin()),
                          expr->getChildrenGroups().end(),
                          std::to_string(expr->getChildrenGroups()[0]),
                          fold)
                + "]";

        if (expr->getStep()->getType() == IQueryPlanStep::Type::Join)
        {
            auto join_step = dynamic_cast<const JoinStep *>(expr->getStep().get());
            if (join_step->getDistributionType() == DistributionType::REPARTITION)
            {
                result += " repartition";
            }
            if (join_step->getDistributionType() == DistributionType::BROADCAST)
            {
                result += " broadcast";
            }
        }
        if (expr->getStep()->getType() == IQueryPlanStep::Type::CTERef)
        {
            auto cte_step = dynamic_cast<const CTERefStep *>(expr->getStep().get());
            result += " id: " + std::to_string(cte_step->getId());
        }
        result += "<BR/>";
        return result;
    };

    out << "group_" << group.getId()
        << "[label=<"
           "<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">";

    // type
    out << "<TR><TD COLSPAN=\"3\">" << head_step->getName() << " [" << group.getId() << "]</TD></TR>";

    if (head_step->getType() == IQueryPlanStep::Type::ReadFromStorage)
    {
        //        out << "<TR><TD COLSPAN=\"3\">" << dynamic_cast<const ReadFromStorageStep *>(head_step)->getTable() << "</TD></TR>";
    }

    if (head_step->getType() == IQueryPlanStep::Type::Filter)
    {
        out << "<TR><TD COLSPAN=\"3\">" << dynamic_cast<const FilterStep *>(head_step)->getFilterColumnName() << "</TD></TR>";
    }

    if (group.isMagic())
    {
        out << "<TR><TD COLSPAN=\"3\">Magic</TD></TR>";
    }

    if (group.isStatsDerived())
    {
        out << "<TR><TD COLSPAN=\"3\">";
        if (group.getStatistics())
        {
            auto stats = group.getStatistics().value()->toString();
            boost::replace_all(stats, "\\n", "<BR/>");
            out << stats;
        }
        else
        {
            out << "None";
        }
        out << "</TD></TR>";
    }

    // expression
    out << "<TR><TD>Logical</TD>";
    out << "<TD COLSPAN=\"2\">";
    for (auto & expr : group.getLogicalExpressions())
    {
        if (expr->isDeleted())
            out << "Deleted ";
        out << expr_to_str(expr);
    }
    out << "</TD>";
    out << "</TR>";

    out << "<TR><TD>Winner</TD>";

    // winners
    out << R"(<TD><TABLE CELLBORDER="1" BORDER="0" CELLSPACING="0">)";

    auto partition_str = [&](const Partitioning & partitioning) {
        if (partitioning.getPartitioningHandle() == Partitioning::Handle::SINGLE)
            return String("SINGLE");
        else if (partitioning.getPartitioningHandle() == Partitioning::Handle::FIXED_BROADCAST)
            return String("BROADCAST");
        else if (partitioning.getPartitioningHandle() == Partitioning::Handle::ARBITRARY)
            return String("ARBITRARY");
        else if (partitioning.getPartitioningHandle() == Partitioning::Handle::BUCKET_TABLE)
            return String("BUCKET_TABLE");
        else if (partitioning.getPartitioningHandle() == Partitioning::Handle::FIXED_ARBITRARY)
            return String("FIXED_ARBITRARY");
        else if (partitioning.getPartitioningHandle() == Partitioning::Handle::FIXED_HASH)
        {
            if (partitioning.getPartitioningColumns().empty())
            {
                return String("[]");
            }
            else
            {
                auto result = String("[")
                    + std::accumulate(
                                  std::next(partitioning.getPartitioningColumns().begin()),
                                  partitioning.getPartitioningColumns().end(),
                                  partitioning.getPartitioningColumns()[0],
                                  fold_string)
                    + "]";
                if (partitioning.isEnforceRoundRobin())
                {
                    result += " RoundR";
                }
                return result;
            }
        }
        else
            return String("UNKNOWN");
    };
    auto property_str = [&](const Property & property) {
        std::stringstream ss;
        ss << partition_str(property.getNodePartitioning());
        if (property.isPreferred())
            ss << "?";
        ss << " ";
        ss << join(
            property.getCTEDescriptions(),
            [&](const auto & cte) {
                std::stringstream string_stream;
                string_stream << "CTE(" << cte.first << ")=" << partition_str(cte.second.getNodePartitioning());
                return string_stream.str();
            },
            ",",
            " ");
        return ss.str();
    };

    for (auto & pair : group.getLowestCostExpressions())
    {
        auto & winner = pair.second;
        out << "<TR>";

        // property
        out << "<TD>";
        out << "cost: " << winner->getCost() << "<BR/>";
        out << "require: " << property_str(pair.first);
        out << "</TD>";
        // property end

        // winner
        out << "<TD>";

        if (winner->getRemoteExchange())
        {
            if (auto exchange_step = dynamic_cast<const ExchangeStep *>(winner->getRemoteExchange()->getStep().get()))
            {
                out << "enforce: ";
                out << partition_str(exchange_step->getSchema());
                out << "<BR/>";
            }
        }
        out << "actual: ";
        out << property_str(winner->getActual());
        out << "<BR/>";

        out << expr_to_str(winner->getGroupExpr());
        out << "\n";

        out << join(
            winner->getRequireChildren(), [&](const auto & item) { return property_str(item); }, ", ", "child: ")
            << "\n";
        out << "</TD>";
        // winner end

        out << "</TR>";
    }
    out << "</TABLE></TD>";
    // winner end

    out << "</TR>";

    out << "</TABLE>>, fillcolor=" << NODE_COLORS[head_step->getType()] << "]"
        << ";" << std::endl;
    return out.str();
}


}
