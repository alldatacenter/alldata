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

#include <Optimizer/PlanNodeSearcher.h>

#include <Optimizer/Utils.h>
#include <QueryPlan/PlanNode.h>

namespace DB
{
std::optional<PlanNodePtr> PlanNodeSearcher::findFirstRecursive(const PlanNodePtr & curr) // NOLINT(misc-no-recursion)
{
    if (!predicate || predicate(*curr))
        return curr;

    if (!recurse_only_when || recurse_only_when(*curr))
        for (auto & child : curr->getChildren())
            if (auto found = findFirstRecursive(child))
                return found;
    return {};
}

std::optional<PlanNodePtr> PlanNodeSearcher::findSingle()
{
    auto all = findAll();
    if (all.empty())
        return {};
    Utils::checkArgument(all.size() == 1);
    return all.at(0);
}

PlanNodePtr PlanNodeSearcher::findOnlyElement()
{
    auto all = findAll();
    Utils::checkArgument(all.size() == 1);
    return all.at(0);
}

PlanNodePtr PlanNodeSearcher::findOnlyElementOr(const PlanNodePtr & default_)
{
    auto all = findAll();
    if (all.empty())
        return default_;
    Utils::checkArgument(all.size() == 1);
    return all.at(0);
}

void PlanNodeSearcher::findAllRecursive(const PlanNodePtr & curr, std::vector<PlanNodePtr> & result) // NOLINT(misc-no-recursion)
{
    if (!predicate || predicate(*curr))
    {
        result.emplace_back(curr);
    }
    if (!recurse_only_when || recurse_only_when(*curr))
        for (auto & child : curr->getChildren())
            findAllRecursive(child, result);
}

void PlanNodeSearcher::collectRecursive( // NOLINT(misc-no-recursion)
    const PlanNodePtr & curr, const std::function<void(PlanNodeBase &)> & consumer)
{
    if (!predicate || predicate(*curr))
        consumer(*curr);

    if (!recurse_only_when || recurse_only_when(*curr))
        for (const auto & child : curr->getChildren())
            collectRecursive(child, consumer);
}

}
