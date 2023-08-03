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
#include <QueryPlan/JoinStep.h>

namespace DB
{
class LeftJoinToRightJoin : public Rule
{
public:
    RuleType getType() const override { return RuleType::LEFT_JOIN_TO_RIGHT_JOIN; }
    String getName() const override { return "LEFT_JOIN_TO_RIGHT_JOIN"; }

    PatternPtr getPattern() const override;

    // Left join with filter is not allowed convert to Right join with filter. (nest loop join only support left join).
    static bool supportSwap(const JoinStep & s)
    {
        return s.getKind() == ASTTableJoin::Kind::Left && s.supportSwap() && PredicateUtils::isTruePredicate(s.getFilter());
    }

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}
