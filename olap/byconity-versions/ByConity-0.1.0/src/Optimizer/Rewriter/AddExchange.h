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

#include <Interpreters/Context.h>
#include <Optimizer/Property/Property.h>
#include <Optimizer/Rewriter/Rewriter.h>
#include <QueryPlan/PlanVisitor.h>

#include <utility>

namespace DB
{
/**
 * Reference paper : Incorporating Partitioning and Parallel Plans into the SCOPE Optimizer.
 */
class AddExchange : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "AddExchange"; }
};

class ExchangeResult
{
public:
    explicit ExchangeResult(PlanNodePtr node_ = {}, Property property = Property{})
        : node(std::move(node_)), output_property(std::move(property))
    {
    }
    PlanNodePtr getNodePtr() { return node; }
    Property & getOutputProperty() { return output_property; }

private:
    PlanNodePtr node;
    Property output_property;
};

class ExchangeContext
{
public:
    ExchangeContext(ContextMutablePtr context_, Property & required_) : context(context_), required(required_) { }
    ContextMutablePtr getContext() { return context; }
    Property & getRequired() { return required; }

private:
    ContextMutablePtr context;
    Property & required;
};

class ExchangeVisitor : public PlanNodeVisitor<ExchangeResult, ExchangeContext>
{
public:
    ExchangeResult visitPlanNode(PlanNodeBase &, ExchangeContext &) override;

    ExchangeResult visitProjectionNode(ProjectionNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitFilterNode(FilterNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitJoinNode(JoinNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitAggregatingNode(AggregatingNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitMergingAggregatedNode(MergingAggregatedNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitUnionNode(UnionNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitExchangeNode(ExchangeNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitRemoteExchangeSourceNode(RemoteExchangeSourceNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitTableScanNode(TableScanNode & node, ExchangeContext &) override;
    ExchangeResult visitReadNothingNode(ReadNothingNode & node, ExchangeContext &) override;
    ExchangeResult visitValuesNode(ValuesNode & node, ExchangeContext &) override;
    ExchangeResult visitLimitNode(LimitNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitLimitByNode(LimitByNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitSortingNode(SortingNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitMergeSortingNode(MergeSortingNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitPartialSortingNode(PartialSortingNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitMergingSortedNode(MergingSortedNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitIntersectNode(IntersectNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitExceptNode(ExceptNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitDistinctNode(DistinctNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitExtremesNode(ExtremesNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitWindowNode(WindowNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitApplyNode(ApplyNode & node, ExchangeContext &) override;
    ExchangeResult visitEnforceSingleRowNode(EnforceSingleRowNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitAssignUniqueIdNode(AssignUniqueIdNode & node, ExchangeContext & cxt) override;
    ExchangeResult visitCTERefNode(CTERefNode & node, ExchangeContext &) override;


private:
    /**
     * Execute the AddExchange rewrite rule for child node.
     *
     * @param node child node
     * @param cxt the preferred property for child node and context
     * @return child node with it's out property (which already have satisfied the requirement)
     */
    ExchangeResult visitChild(PlanNodePtr node, ExchangeContext & cxt);

    /**
     * Replace child first, then derive the output property.
     *
     * @param node current node.
     * @param result child with it's output property.
     * @return node with it's output property.
     */
    static ExchangeResult rebaseAndDeriveProperties(const PlanNodePtr & node, ExchangeResult & result, Context & cxt);

    /**
     * Replace children first, then derive the output property.
     *
     * @param node current node.
     * @param results children with it's output property.
     * @return node with it's output property.
     */
    static ExchangeResult rebaseAndDeriveProperties(const PlanNodePtr & node, std::vector<ExchangeResult> & results, Context & cxt);

    /**
     * Derive the actual property of node.
     *
     * @param node current node, which has single child. e.g filter, projection, aggregation.
     * @param inputProperty the actual property of child node.
     * @return node with it's actual property.
     */
    static ExchangeResult deriveProperties(const PlanNodePtr & node, Property & inputProperty, Context & cxt);

    /**
     * Derive the actual property of node.
     *
     * @param node current node, which has multiple children. e.g Join, Union.
     * @param inputProperties the actual property of child node.
     * @return node with it's actual property.
     */
    static ExchangeResult deriveProperties(const PlanNodePtr & node, PropertySet & inputProperties, Context & cxt);

    ExchangeResult enforceNodeAndStream(PlanNodeBase & node, ExchangeContext & cxt);
    ExchangeResult enforceNode(PlanNodeBase & node, ExchangeContext & cxt);
};

}
