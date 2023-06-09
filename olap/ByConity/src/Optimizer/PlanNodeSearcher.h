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

#include <QueryPlan/QueryPlan.h>

namespace DB
{

class PlanNodeSearcher
{
public:
    static PlanNodeSearcher searchFrom(const PlanNodePtr & node) { return PlanNodeSearcher{node}; }

    static PlanNodeSearcher searchFrom(QueryPlan & plan)
    {
        PlanNodes nodes;
        nodes.emplace_back(plan.getPlanNode());
        for (auto & cte : plan.getCTEInfo().getCTEs())
            nodes.emplace_back(cte.second);
        return PlanNodeSearcher{nodes};
    }

    PlanNodeSearcher & where(std::function<bool(PlanNodeBase &)> && predicate_)
    {
        predicate = predicate_;
        return *this;
    }

    PlanNodeSearcher & recurseOnlyWhen(std::function<bool(PlanNodeBase &)> && recurse_only_when_)
    {
        recurse_only_when = recurse_only_when_;
        return *this;
    }

    std::optional<PlanNodePtr> findFirst()
    {
        for (const auto & node : nodes)
            return findFirstRecursive(node);
        return {};
    }

    std::optional<PlanNodePtr> findSingle();

    /**
     * Return a list of matching nodes ordered as in pre-order traversal of the plan tree.
     */
    std::vector<PlanNodePtr> findAll()
    {
        std::vector<PlanNodePtr> result;
        for (const auto & node : nodes)
            findAllRecursive(node, result);
        return result;
    }

    PlanNodePtr findOnlyElement();

    PlanNodePtr findOnlyElementOr(const PlanNodePtr & default_);

    void findAllRecursive(const PlanNodePtr & curr, std::vector<PlanNodePtr> & nodes);

    bool matches() { return findFirst().has_value(); }

    size_t count() { return findAll().size(); }

    void collect(const std::function<void(PlanNodeBase &)> & consumer)
    {
        for (const auto & node : nodes)
            collectRecursive(node, consumer);
    }

private:
    explicit PlanNodeSearcher(const PlanNodePtr & node) : nodes({node}) { }
    explicit PlanNodeSearcher(PlanNodes nodes_) : nodes(std::move(nodes_)) { }

    std::optional<PlanNodePtr> findFirstRecursive(const PlanNodePtr & curr);
    void collectRecursive(const PlanNodePtr & curr, const std::function<void(PlanNodeBase &)> & consumer);

    PlanNodes nodes;
    std::function<bool(PlanNodeBase &)> predicate; // always true if not set
    std::function<bool(PlanNodeBase &)> recurse_only_when; // always true if not set
};

}
