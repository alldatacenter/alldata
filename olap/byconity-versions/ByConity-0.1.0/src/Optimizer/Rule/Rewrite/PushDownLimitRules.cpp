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

#include <Optimizer/Rule/Rewrite/PushDownLimitRules.h>

#include <Optimizer/PlanNodeCardinality.h>
#include <Optimizer/Rule/Patterns.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/LimitStep.h>

namespace DB
{

static bool isLimitNeeded(const LimitStep & limit, const PlanNodePtr & node)
{
    auto range = PlanNodeCardinality::extractCardinality(*node);
    return range.upperBound > limit.getLimit() + limit.getOffset();
}

PatternPtr PushLimitIntoDistinct::getPattern() const
{
    return Patterns::limit()->withSingle(Patterns::distinct());
}

TransformResult PushLimitIntoDistinct::transformImpl(PlanNodePtr node, const Captures &, RuleContext &)
{
    auto limit_step = dynamic_cast<const LimitStep *>(node->getStep().get());
    auto distinct = node->getChildren()[0];
    auto distinct_step = dynamic_cast<const DistinctStep *>(distinct->getStep().get());

    if (!isLimitNeeded(*limit_step, distinct))
        return {};

    auto new_distinct = PlanNodeBase::createPlanNode(
        distinct->getId(),
        std::make_shared<DistinctStep>(
            distinct_step->getInputStreams()[0],
            distinct_step->getSetSizeLimits(),
            limit_step->getLimit() + limit_step->getOffset(),
            distinct_step->getColumns(),
            distinct_step->preDistinct()),
        distinct->getChildren());
    node->replaceChildren({new_distinct});
    return TransformResult{node};
}

PatternPtr PushLimitThroughProjection::getPattern() const
{
    return Patterns::limit()->withSingle(Patterns::project());
}

TransformResult PushLimitThroughProjection::transformImpl(PlanNodePtr node, const Captures &, RuleContext &)
{
    auto projection = node->getChildren()[0];
    auto source = projection->getChildren()[0];

    const auto * limit_step = dynamic_cast<const LimitStep *>(node->getStep().get());
    auto new_limit = PlanNodeBase::createPlanNode(
        node->getId(),
        std::make_shared<LimitStep>(
            source->getStep()->getOutputStream(),
            limit_step->getLimit(),
            limit_step->getOffset(),
            limit_step->isAlwaysReadTillEnd(),
            limit_step->isPartial()),
        {source});

    projection->replaceChildren({new_limit});
    projection->setStatistics(node->getStatistics());
    return TransformResult{projection, true};
}

PatternPtr PushLimitThroughExtremesStep::getPattern() const
{
    return Patterns::limit()->withSingle(Patterns::extremes());
}

TransformResult PushLimitThroughExtremesStep::transformImpl(PlanNodePtr node, const Captures &, RuleContext &)
{
    auto extremes = node->getChildren()[0];
    auto source = extremes->getChildren()[0];

    node->replaceChildren({source});
    extremes->replaceChildren({node->shared_from_this()});
    return TransformResult{extremes, true};
}

PatternPtr PushLimitThroughUnion::getPattern() const
{
    return Patterns::limit()->withSingle(Patterns::unionn());
}

TransformResult PushLimitThroughUnion::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    const auto * limit_step = dynamic_cast<const LimitStep *>(node->getStep().get());
    auto unionn = node->getChildren()[0];

    bool should_apply = false;
    PlanNodes children;
    for (auto & child : unionn->getChildren())
    {
        if (isLimitNeeded(*limit_step, child))
        {
            children.emplace_back(PlanNodeBase::createPlanNode(
                context.context->nextNodeId(),
                std::make_shared<LimitStep>(
                    child->getStep()->getOutputStream(),
                    limit_step->getLimit() + limit_step->getOffset(),
                    0,
                    limit_step->isAlwaysReadTillEnd(),
                    true),
                {child}));
            should_apply = true;
        }
    }

    if (!should_apply)
        return {};

    unionn->replaceChildren(children);
    node->replaceChildren({unionn});
    return node;
}

PatternPtr PushLimitThroughOuterJoin::getPattern() const
{
    return Patterns::limit()->withSingle(Patterns::join()->matchingStep<JoinStep>([](const auto & join_step) {
        return join_step.getKind() == ASTTableJoin::Kind::Left || join_step.getKind() == ASTTableJoin::Kind::Right;
    }));
}

TransformResult PushLimitThroughOuterJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    auto join = node->getChildren()[0];
    const auto * join_step = dynamic_cast<const JoinStep *>(join->getStep().get());
    const auto * limit_step = dynamic_cast<const LimitStep *>(node->getStep().get());

    auto left = join->getChildren()[0];
    auto right = join->getChildren()[1];

    if (join_step->getKind() == ASTTableJoin::Kind::Left && isLimitNeeded(*limit_step, left))
    {
        left = PlanNodeBase::createPlanNode(
            context.context->nextNodeId(),
            std::make_shared<LimitStep>(
                left->getStep()->getOutputStream(),
                limit_step->getLimit() + limit_step->getOffset(),
                0,
                limit_step->isAlwaysReadTillEnd(),
                true),
            {left});
        join->replaceChildren({left, right});
        return node;
    }
    else if (join_step->getKind() == ASTTableJoin::Kind::Right && isLimitNeeded(*limit_step, right))
    {
        right = PlanNodeBase::createPlanNode(
            context.context->nextNodeId(),
            std::make_shared<LimitStep>(
                right->getStep()->getOutputStream(),
                limit_step->getLimit() + limit_step->getOffset(),
                0,
                limit_step->isAlwaysReadTillEnd(),
                true),
            {right});
        join->replaceChildren({left, right});
        return node;
    }

    return {};
}

}
