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

namespace DB
{
class PullLeftJoinThroughInnerJoin : public Rule
{
public:
    RuleType getType() const override { return RuleType::PULL_Left_JOIN_THROUGH_INNER_JOIN; }
    String getName() const override { return "PULL_Left_JOIN_THROUGH_INNER_JOIN"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};


class PullLeftJoinProjectionThroughInnerJoin : public Rule
{
public:
    RuleType getType() const override { return RuleType::PULL_Left_JOIN_PROJECTION_THROUGH_INNER_JOIN; }
    String getName() const override { return "PULL_Left_JOIN_PROJECTION_THROUGH_INNER_JOIN"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

class PullLeftJoinFilterThroughInnerJoin : public Rule
{
public:
    RuleType getType() const override { return RuleType::PULL_Left_JOIN_FILTER_THROUGH_INNER_JOIN; }
    String getName() const override { return "PULL_Left_JOIN_FILTER_THROUGH_INNER_JOIN"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}
