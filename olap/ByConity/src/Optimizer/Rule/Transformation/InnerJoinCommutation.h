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
/**
 * InnerJoinCommutation is mutually exclusive with JoinEnumOnGraph rule,
 * the later also do inner join commutation works.
 *
 * Transforms:
 * <pre>
 * - Inner Join
 *     - X
 *     - Y
 * </pre>
 * Into:
 * <pre>
 * - Inner Join
 *     - Y
 *     - X
 * </pre>
 */
class InnerJoinCommutation : public Rule
{
public:
    RuleType getType() const override { return RuleType::INNER_JOIN_COMMUTATION; }
    String getName() const override { return "INNER_JOIN_COMMUTATION"; }

    PatternPtr getPattern() const override;

    const std::vector<RuleType> & blockRules() const override;

    static bool supportSwap(const JoinStep & s) { return s.getKind() == ASTTableJoin::Kind::Inner && s.supportSwap(); }
    static PlanNodePtr swap(JoinNode & node, RuleContext & rule_context);

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}
