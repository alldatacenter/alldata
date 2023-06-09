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
#include <Optimizer/Rule/Rule.h>

#include <Optimizer/Rule/Transformation/MagicSetForAggregation.h>

namespace DB
{

class MagicSetPushThroughProject : public MagicSetRule
{
public:
    RuleType getType() const override { return RuleType::MAGIC_SET_PUSH_THROUGH_PROJECTION; }
    String getName() const override { return "MAGIC_SET_PUSH_THROUGH_PROJECTION"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

/**
 * NOTE: MagicSetPushThroughJoin blocks JOIN_ENUM_ON_GRAPH
 */
class MagicSetPushThroughJoin : public MagicSetRule
{
public:
    RuleType getType() const override { return RuleType::MAGIC_SET_PUSH_THROUGH_JOIN; }
    String getName() const override { return "MAGIC_SET_PUSH_THROUGH_JOIN"; }

    PatternPtr getPattern() const override;

    const std::vector<RuleType> & blockRules() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
    static std::optional<PlanNodePtr> trySwapJoin(const PlanNodePtr & node, RuleContext & context);
};

class MagicSetPushThroughFilter : public MagicSetRule
{
public:
    RuleType getType() const override { return RuleType::MAGIC_SET_PUSH_THROUGH_FILTER; }
    String getName() const override { return "MAGIC_SET_PUSH_THROUGH_FILTER"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

class MagicSetPushThroughAggregating : public MagicSetRule
{
public:
    RuleType getType() const override { return RuleType::MAGIC_SET_PUSH_THROUGH_AGGREGATING; }
    String getName() const override { return "MAGIC_SET_PUSH_THROUGH_AGGREGATING"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}
