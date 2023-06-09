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
class PushDynamicFilterBuilderThroughExchange : public Rule
{
public:
    RuleType getType() const override { return RuleType::PUSH_DYNAMIC_FILTER_BUILDER_THROUGH_EXCHANGE; }
    String getName() const override { return "PUSH_DYNAMIC_FILTER_BUILDER_THROUGH_EXCHANGE"; }

    PatternPtr getPattern() const override;

    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};
}
