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

#include <Optimizer/MergeSetOperation.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/Rule/Rewrite/MergeSetOperationRules.h>

namespace DB
{
PatternPtr MergeUnionRule::getPattern() const
{
    return Patterns::unionn();
}

TransformResult MergeUnionRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    SetOperationMerge merge_operation(node, *rule_context.context);
    auto result = merge_operation.merge();

    if (result)
        return result;
    else
        return {};
}

PatternPtr MergeExceptRule::getPattern() const
{
    return Patterns::except();
}

TransformResult MergeExceptRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    SetOperationMerge merge_operation(node, *rule_context.context);
    auto result = merge_operation.mergeFirstSource();

    if (result)
        return result;
    else
        return {};
}

PatternPtr MergeIntersectRule::getPattern() const
{
    return Patterns::intersect();
}

TransformResult MergeIntersectRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    SetOperationMerge merge_operation(node, *rule_context.context);
    auto result = merge_operation.merge();

    if (result)
        return result;
    else
        return {};
}
}
