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

/**
 * Pull projection for join graph rewrite.
 *
 * Transforms:
 * <pre>
 * - Inner Join / Left Join X
 *     - Projection
 *         - Join Y
 *     - Z
 * </pre>
 * Into:
 * <pre>
 * - Projection
 *     - Inner Join / Left Join X
 *         - Join Y
 *         - Z
 * </pre>
 */
class PullProjectionOnJoinThroughJoin : public Rule
{
public:
    RuleType getType() const override { return RuleType::PULL_PROJECTION_ON_JOIN_THROUGH_JOIN; }
    String getName() const override { return "PULL_PROJECTION_ON_JOIN_THROUGH_JOIN"; }

    PatternPtr getPattern() const override;

    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}

