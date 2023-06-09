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
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/Rule/Rule.h>

namespace DB
{
class FilterWindowToPartitionTopN : public Rule
{
public:
    RuleType getType() const override { return RuleType::FILTER_WINDOW_TO_PARTITION_TOPN; }
    String getName() const override { return "FILTER_WINDOW_TO_PARTITION_TOPN"; }

    PatternPtr getPattern() const override { return Patterns::filter()->withSingle(Patterns::window()->withSingle(Patterns::exchange())); }

    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}
