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

#include <Optimizer/PlanNodeCardinality.h>

#include <Optimizer/ExpressionDeterminism.h>
#include <QueryPlan/AggregatingStep.h>

namespace DB
{

class PlanNodeCardinality::Visitor : public PlanNodeVisitor<PlanNodeCardinality::Range, Void>
{
public:
    Range visitPlanNode(PlanNodeBase &, Void &) override { return Range{0, std::numeric_limits<size_t>::max()}; }

    static Range applyLimit(const Range & source, size_t limit)
    {
        limit = std::min(source.upperBound, limit);
        size_t lower = std::min(limit, source.lowerBound);
        return Range{lower, limit};
    }

    static Range applyOffset(const Range & source, size_t offset)
    {
        return Range{
            std::max(source.lowerBound - offset, static_cast<size_t>(0)), std::max(source.upperBound - offset, static_cast<size_t>(0))};
    }

    Range visitLimitNode(LimitNode & node, Void & context) override
    {
        auto source_range = VisitorUtil::accept(node.getChildren()[0], *this, context);
        const auto * step = dynamic_cast<const LimitStep *>(node.getStep().get());
        return applyLimit(applyOffset(source_range, step->getOffset()), step->getLimit());
    }

    Range visitProjectionNode(ProjectionNode & node, Void & context) override
    {
        // todo: arrayJoin
        return VisitorUtil::accept(node.getChildren()[0], *this, context);
    }

    Range visitUnionNode(UnionNode & node, Void & context) override
    {
        if (node.getChildren().size() == 1)
            return PlanNodeVisitor::visitUnionNode(node, context);
        else
            return Range{0, std::numeric_limits<size_t>::max()};
    }

    Range visitExchangeNode(ExchangeNode & node, Void & context) override
    {
        return VisitorUtil::accept(node.getChildren()[0], *this, context);
    }

    Range visitFilterNode(FilterNode & node, Void & context) override
    {
        auto source_range = VisitorUtil::accept(node.getChildren()[0], *this, context);
        return Range{0, source_range.upperBound};
    }

    Range visitEnforceSingleRowNode(EnforceSingleRowNode &, Void &) override { return Range{1, 1}; }

    Range visitValuesNode(ValuesNode & node, Void &) override
    {
        const auto * step = dynamic_cast<const ValuesStep *>(node.getStep().get());
        return Range{step->getRows(), step->getRows()};
    }

    Range visitAggregatingNode(AggregatingNode & node, Void & context) override
    {
        const auto * step = dynamic_cast<const AggregatingStep *>(node.getStep().get());
        if (step->getKeys().empty())
            return Range{1, 1};

        auto source_range = VisitorUtil::accept(node.getChildren()[0], *this, context);
        // hasDefaultOutput ? 1 : 0
        return Range{std::max(static_cast<size_t>(0), source_range.lowerBound), std::max(static_cast<size_t>(1), source_range.upperBound)};
    }

    Range visitWindowNode(WindowNode & node, Void & context) override { return VisitorUtil::accept(node.getChildren()[0], *this, context); }

    Range visitDistinctNode(DistinctNode & node, Void & context) override
    {
        auto source_range = VisitorUtil::accept(node.getChildren()[0], *this, context);
        auto step = dynamic_cast<const DistinctStep *>(node.getStep().get());
        auto limit_hint = step->getLimitHint();
        if (limit_hint != 0)
            return Range{std::min(static_cast<size_t>(1), source_range.lowerBound), std::min(limit_hint, source_range.upperBound)};
        return Range{std::min(static_cast<size_t>(1), source_range.lowerBound), source_range.upperBound};
    }
};

PlanNodeCardinality::Range PlanNodeCardinality::extractCardinality(PlanNodeBase & node)
{
    PlanNodeCardinality::Visitor visitor;
    Void context{};
    return VisitorUtil::accept(node, visitor, context);
}

}
