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

#include <Optimizer/Rule/Rule.h>

namespace DB
{
TransformResult Rule::transform(const PlanNodePtr & node, RuleContext & context)
{
    auto match_opt = getPattern()->match(node);

    if (match_opt)
        return transformImpl(node, match_opt->captures, context);
    else
        return {};
}

TransformResult TransformResult::of(const std::optional<PlanNodePtr> & plan_)
{
    if (plan_)
        return {plan_.value()};
    else
        return {};
}
}
