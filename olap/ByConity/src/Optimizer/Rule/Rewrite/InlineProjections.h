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
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/Assignment.h>
#include <Optimizer/Rule/Rule.h>

namespace DB
{

/**
 * Inlines expressions from a child project node into a parent project node
 * as long as they are all identity or not used, or they are simple constants,
 * or they are referenced only once (to avoid introducing duplicate computation)
 * and the references don't appear within a TRY block (to avoid changing semantics).
 */
class InlineProjections : public Rule
{
public:
    RuleType getType() const override { return RuleType::INLINE_PROJECTION; }
    String getName() const override { return "INLINE_PROJECTION"; }

    PatternPtr getPattern() const override;
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
    static std::optional<PlanNodePtr> inlineProjections(PlanNodePtr & parent, PlanNodePtr & child, ContextMutablePtr & context);

private:
    static std::set<String> extractInliningTargets(ProjectionNode * parent, ProjectionNode * child, ContextMutablePtr & context);
    static ASTPtr inlineReferences(const ConstASTPtr & expression, Assignments & assignments);

};

class InlineProjectionIntoJoin : public Rule
{
public:
    RuleType getType() const override { return RuleType::INLINE_PROJECTION_INTO_JOIN; }
    String getName() const override { return "INLINE_PROJECTION_INTO_JOIN"; }

    PatternPtr getPattern() const override;

    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

class InlineProjectionOnJoinIntoJoin : public Rule
{
public:
    RuleType getType() const override { return RuleType::INLINE_PROJECTION_ON_JOIN_INTO_JOIN; }
    String getName() const override { return "INLINE_PROJECTION_ON_JOIN_INTO_JOIN"; }

    PatternPtr getPattern() const override;

    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}
