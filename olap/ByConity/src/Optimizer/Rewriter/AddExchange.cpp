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

#include <Optimizer/Rewriter/AddExchange.h>

#include <Optimizer/Property/PropertyDeriver.h>
#include <Optimizer/Property/PropertyDeterminer.h>
#include <Optimizer/Property/PropertyEnforcer.h>
#include <Optimizer/Property/PropertyMatcher.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
void AddExchange::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    ExchangeVisitor visitor{};
    Property required{Partitioning{Partitioning::Handle::SINGLE}};
    ExchangeContext cxt{context, required};
    ExchangeResult result = VisitorUtil::accept(plan.getPlanNode(), visitor, cxt);

    PlanNodePtr node = result.getNodePtr();
    Property output = result.getOutputProperty();
    if (!PropertyMatcher::matchNodePartitioning(*context, required.getNodePartitioningRef(), output.getNodePartitioning()))
    {
        Utils::checkArgument(node->getChildren().size() == 1, "Output node has more than 1 child");

        PlanNodePtr child = node->getChildren()[0];
        PlanNodePtr enforced_node = PropertyEnforcer::enforceNodePartitioning(child, required, output, *cxt.getContext());
        PlanNodes enforced_children{enforced_node};
        node->replaceChildren(enforced_children);
    }
    plan.update(node);
}

ExchangeResult ExchangeVisitor::visitPlanNode(PlanNodeBase & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    PropertySet required_set = PropertyDeterminer::determineRequiredProperty(node.getStep(), cxt.getRequired())[0];
    PlanNodePtr child = ptr->getChildren()[0];
    Property preferred = required_set[0];
    ExchangeResult result = visitChild(child, cxt);
    return rebaseAndDeriveProperties(ptr, result, *cxt.getContext());
}

ExchangeResult ExchangeVisitor::visitProjectionNode(ProjectionNode & node, ExchangeContext & cxt)
{
    const auto & step = *node.getStep();
    auto assignments = step.getAssignments();
    std::unordered_map<String, String> identities = Utils::computeIdentityTranslations(assignments);
    Property translated = cxt.getRequired().translate(identities);
    ExchangeContext child_cxt{cxt.getContext(), translated};
    return visitPlanNode(node, child_cxt);
}

ExchangeResult ExchangeVisitor::visitFilterNode(FilterNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitJoinNode(JoinNode & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    PropertySet required_set = PropertyDeterminer::determineRequiredProperty(node.getStep(), cxt.getRequired())[0];

    PlanNodePtr left = ptr->getChildren()[0];
    PlanNodePtr right = ptr->getChildren()[1];

    Property left_property = required_set[0];
    Property right_property = required_set[1];

    ExchangeContext left_child_property{cxt.getContext(), left_property};
    ExchangeContext right_child_property{cxt.getContext(), right_property};
    ExchangeResult left_result = visitChild(left, left_child_property);
    ExchangeResult right_result = visitChild(right, right_child_property);

    if (!PropertyMatcher::matchNodePartitioning(
            *cxt.getContext(), left_property.getNodePartitioningRef(), left_result.getOutputProperty().getNodePartitioning()))
    {
        PlanNodePtr enforced_node = PropertyEnforcer::enforceNodePartitioning(
            left_result.getNodePtr(), left_property, left_result.getOutputProperty(), *cxt.getContext());
        Property enforced_prop = PropertyDeriver::deriveProperty(enforced_node->getStep(), left_result.getOutputProperty(), *cxt.getContext());
        ExchangeResult enforced_result{enforced_node, enforced_prop};
        left_result = enforced_result;
    }

    if (!PropertyMatcher::matchNodePartitioning(
            *cxt.getContext(), right_property.getNodePartitioningRef(), right_result.getOutputProperty().getNodePartitioning()))
    {
        PlanNodePtr enforced_node = PropertyEnforcer::enforceNodePartitioning(
            right_result.getNodePtr(), right_property, right_result.getOutputProperty(), *cxt.getContext());
        Property enforced_prop = PropertyDeriver::deriveProperty(enforced_node->getStep(), right_result.getOutputProperty(), *cxt.getContext());
        ExchangeResult enforced_result{enforced_node, enforced_prop};
        right_result = enforced_result;
    }

    std::vector<ExchangeResult> results;
    results.emplace_back(left_result);
    results.emplace_back(right_result);
    return rebaseAndDeriveProperties(ptr, results, *cxt.getContext());
}

ExchangeResult ExchangeVisitor::visitAggregatingNode(AggregatingNode & node, ExchangeContext & cxt)
{
    return enforceNodeAndStream(node, cxt);
}

ExchangeResult ExchangeVisitor::visitMergingAggregatedNode(MergingAggregatedNode & node, ExchangeContext & cxt)
{
    return enforceNodeAndStream(node, cxt);
}

ExchangeResult ExchangeVisitor::visitUnionNode(UnionNode & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    PropertySet required_set = PropertyDeterminer::determineRequiredProperty(node.getStep(), cxt.getRequired())[0];

    std::vector<ExchangeResult> results;
    PlanNodes children;
    PropertySet children_property;

    for (size_t index = 0; index < ptr->getChildren().size(); ++index)
    {
        PlanNodePtr child = ptr->getChildren()[index];
        Property preferred = required_set[index];
        ExchangeContext child_cxt(cxt.getContext(), preferred);
        auto result = visitChild(child, child_cxt);
        if (!PropertyMatcher::matchNodePartitioning(
                *cxt.getContext(), preferred.getNodePartitioningRef(), result.getOutputProperty().getNodePartitioning()))
        {
            PlanNodePtr enforced_node
                = PropertyEnforcer::enforceNodePartitioning(result.getNodePtr(), preferred, result.getOutputProperty(), *cxt.getContext());
            Property enforced_prop = PropertyDeriver::deriveProperty(enforced_node->getStep(), result.getOutputProperty(), *cxt.getContext());
            ExchangeResult enforced_result{enforced_node, enforced_prop};
            result = enforced_result;
        }
        children_property.emplace_back(result.getOutputProperty());
        children.push_back(result.getNodePtr());
        results.emplace_back(result);
    }


    Property output_prop = PropertyDeriver::deriveProperty(node.getStep(), children_property, *cxt.getContext());
    node.replaceChildren(children);
    return ExchangeResult{ptr, output_prop};
}

ExchangeResult ExchangeVisitor::visitExchangeNode(ExchangeNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitRemoteExchangeSourceNode(RemoteExchangeSourceNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitTableScanNode(TableScanNode & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    Property output = PropertyDeriver::deriveProperty(ptr->getStep(), *cxt.getContext());
    return ExchangeResult{ptr, output};
}

ExchangeResult ExchangeVisitor::visitReadNothingNode(ReadNothingNode & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    Property output = PropertyDeriver::deriveProperty(ptr->getStep(), *cxt.getContext());
    return ExchangeResult{ptr, output};
}

ExchangeResult ExchangeVisitor::visitValuesNode(ValuesNode & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    Property output = PropertyDeriver::deriveProperty(ptr->getStep(), *cxt.getContext());
    return ExchangeResult{ptr, output};
}

ExchangeResult ExchangeVisitor::visitLimitNode(LimitNode & node, ExchangeContext & cxt)
{
    return enforceNodeAndStream(node, cxt);
}

ExchangeResult ExchangeVisitor::visitLimitByNode(LimitByNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitSortingNode(SortingNode & node, ExchangeContext & cxt)
{
    return enforceNodeAndStream(node, cxt);
}

ExchangeResult ExchangeVisitor::visitMergeSortingNode(MergeSortingNode & node, ExchangeContext & cxt)
{
    return enforceNodeAndStream(node, cxt);
}

ExchangeResult ExchangeVisitor::visitPartialSortingNode(PartialSortingNode & node, ExchangeContext & cxt)
{
    return enforceNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitMergingSortedNode(MergingSortedNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitIntersectNode(IntersectNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitExceptNode(ExceptNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

//ExchangeResult ExchangeVisitor::visitMaterializingNode(MaterializingNode & node, ExchangeContext & cxt)
//{
//    return visitPlan(node, cxt);
//}
//
//ExchangeResult ExchangeVisitor::visitDecompressionNode(DecompressionNode & node, ExchangeContext & cxt)
//{
//    return visitPlan(node, cxt);
//}

ExchangeResult ExchangeVisitor::visitDistinctNode(DistinctNode & node, ExchangeContext & cxt)
{
    return enforceNodeAndStream(node, cxt);
}

ExchangeResult ExchangeVisitor::visitExtremesNode(ExtremesNode & node, ExchangeContext & cxt)
{
    return enforceNode(node, cxt);
}

//ExchangeResult ExchangeVisitor::visitFinalSamplingNode(FinalSamplingNode & node, ExchangeContext & cxt)
//{
//    return enforceNodeAndStream(node, cxt);
//}

ExchangeResult ExchangeVisitor::visitWindowNode(WindowNode & node, ExchangeContext & cxt)
{
    return enforceNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitApplyNode(ApplyNode & node, ExchangeContext &)
{
    throw Exception("Unsupported node " + node.getStep()->getName(), ErrorCodes::NOT_IMPLEMENTED);
}

ExchangeResult ExchangeVisitor::visitEnforceSingleRowNode(EnforceSingleRowNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitAssignUniqueIdNode(AssignUniqueIdNode & node, ExchangeContext & cxt)
{
    return visitPlanNode(node, cxt);
}

ExchangeResult ExchangeVisitor::visitCTERefNode(CTERefNode & node, ExchangeContext &)
{
    throw Exception("Unsupported node " + node.getStep()->getName(), ErrorCodes::NOT_IMPLEMENTED);
}

ExchangeResult ExchangeVisitor::visitChild(PlanNodePtr node, ExchangeContext & cxt)
{
    return VisitorUtil::accept(node, *this, cxt);
}

ExchangeResult ExchangeVisitor::rebaseAndDeriveProperties(const PlanNodePtr & node, ExchangeResult & result, Context & cxt)
{
    // replace the children of current node
    PlanNodes child;
    child.emplace_back(result.getNodePtr());
    node->replaceChildren(child);

    // derive property base on the replaced node
    return deriveProperties(node, result.getOutputProperty(), cxt);
}

ExchangeResult ExchangeVisitor::rebaseAndDeriveProperties(const PlanNodePtr & node, std::vector<ExchangeResult> & results, Context & cxt)
{
    PlanNodes children;
    PropertySet input_properties = std::vector<Property>();
    for (auto result : results)
    {
        children.emplace_back(result.getNodePtr());
        input_properties.emplace_back(result.getOutputProperty());
    }
    node->replaceChildren(children);
    return deriveProperties(node, input_properties, cxt);
}

ExchangeResult ExchangeVisitor::deriveProperties(const PlanNodePtr & node, Property & input_property, Context & cxt)
{
    PropertySet input_properties = std::vector<Property>();
    input_properties.emplace_back(input_property);
    return deriveProperties(node, input_properties, cxt);
}

ExchangeResult ExchangeVisitor::deriveProperties(const PlanNodePtr & node, PropertySet & input_properties, Context & cxt)
{
    Property property = PropertyDeriver::deriveProperty(node->getStep(), input_properties, cxt);
    return ExchangeResult{node, property};
}

ExchangeResult ExchangeVisitor::enforceNodeAndStream(PlanNodeBase & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    PropertySet required_set = PropertyDeterminer::determineRequiredProperty(node.getStep(), cxt.getRequired())[0];
    PlanNodePtr child = ptr->getChildren()[0];
    Property preferred = required_set[0];
    ExchangeResult result = visitChild(child, cxt);

    ExchangeResult enforced_node_result;
    if (!PropertyMatcher::matchNodePartitioning(
            *cxt.getContext(), preferred.getNodePartitioningRef(), result.getOutputProperty().getNodePartitioning()))
    {
        PlanNodePtr enforced_node
            = PropertyEnforcer::enforceNodePartitioning(result.getNodePtr(), preferred, result.getOutputProperty(), *cxt.getContext());
        Property enforced_node_prop = PropertyDeriver::deriveProperty(enforced_node->getStep(), result.getOutputProperty(), *cxt.getContext());
        enforced_node_result = ExchangeResult{enforced_node, enforced_node_prop};
    }

    ExchangeResult enforced_stream_result;
    if (!PropertyMatcher::matchStreamPartitioning(
            *cxt.getContext(), preferred.getStreamPartitioning(), result.getOutputProperty().getStreamPartitioning()))
    {
        if (enforced_node_result.getNodePtr() != nullptr)
        {
            PlanNodePtr enforced_stream_node = PropertyEnforcer::enforceStreamPartitioning(
                enforced_node_result.getNodePtr(), preferred, enforced_node_result.getOutputProperty(), *cxt.getContext());
            Property enforced_stream_prop
                = PropertyDeriver::deriveProperty(enforced_stream_node->getStep(), enforced_node_result.getOutputProperty(), *cxt.getContext());
            enforced_stream_result = ExchangeResult{enforced_stream_node, enforced_stream_prop};
        }
        else
        {
            PlanNodePtr enforced_stream_node = PropertyEnforcer::enforceStreamPartitioning(
                result.getNodePtr(), preferred, result.getOutputProperty(), *cxt.getContext());
            Property enforced_stream_prop = PropertyDeriver::deriveProperty(enforced_stream_node->getStep(), result.getOutputProperty(), *cxt.getContext());
            enforced_stream_result = ExchangeResult{enforced_stream_node, enforced_stream_prop};
        }
    }

    if (enforced_stream_result.getNodePtr() != nullptr)
    {
        return rebaseAndDeriveProperties(ptr, enforced_stream_result, *cxt.getContext());
    }
    if (enforced_node_result.getNodePtr() != nullptr)
    {
        return rebaseAndDeriveProperties(ptr, enforced_node_result, *cxt.getContext());
    }
    return rebaseAndDeriveProperties(ptr, result, *cxt.getContext());
}

ExchangeResult ExchangeVisitor::enforceNode(PlanNodeBase & node, ExchangeContext & cxt)
{
    PlanNodePtr ptr = node.shared_from_this();
    PropertySet required_set = PropertyDeterminer::determineRequiredProperty(node.getStep(), cxt.getRequired())[0];
    PlanNodePtr child = ptr->getChildren()[0];
    Property preferred = required_set[0];
    ExchangeResult result = visitChild(child, cxt);
    if (PropertyMatcher::matchNodePartitioning(
            *cxt.getContext(), preferred.getNodePartitioningRef(), result.getOutputProperty().getNodePartitioning()))
    {
        return rebaseAndDeriveProperties(ptr, result, *cxt.getContext());
    }
    PlanNodePtr enforced_node
        = PropertyEnforcer::enforceNodePartitioning(result.getNodePtr(), preferred, result.getOutputProperty(), *cxt.getContext());
    Property enforced_node_prop = PropertyDeriver::deriveProperty(enforced_node->getStep(), result.getOutputProperty(), *cxt.getContext());
    ExchangeResult enforced_node_result{enforced_node, enforced_node_prop};

    return rebaseAndDeriveProperties(ptr, enforced_node_result, *cxt.getContext());
}

}
