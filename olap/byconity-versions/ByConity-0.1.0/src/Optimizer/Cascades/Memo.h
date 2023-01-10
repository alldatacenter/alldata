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

#include <Optimizer/Cascades/Group.h>
#include <Optimizer/Cascades/GroupExpression.h>

#include <vector>

namespace DB
{
class Memo
{
public:
    GroupExprPtr insertGroupExpr(GroupExprPtr group_expr, CascadesContext & context, GroupId target = UNDEFINED_GROUP);

    GroupPtr getGroupById(GroupId id) const { return groups[id]; }

    const std::vector<GroupPtr> & getGroups() const { return groups; }

    bool containsCTEId(size_t id) const { return cte_group_id_map.contains(id); }

    GroupId getCTEDefGroupId(size_t id) const { return cte_group_id_map.at(id); }

    GroupPtr getCTEDefGroupByCTEId(size_t id) const { return getGroupById(getCTEDefGroupId(id)); }

    void recordCTEDefGroupId(size_t cte_id, GroupId group_id) { cte_group_id_map[cte_id] = group_id;}

private:
    GroupId addNewGroup()
    {
        auto new_group_id = groups.size();
        groups.emplace_back(std::make_shared<Group>(new_group_id));
        return new_group_id;
    }
    /**
     * Vector of groups tracked
     */
    std::vector<GroupPtr> groups;

    /**
     * Map of cte id to group id
     */
    std::unordered_map<size_t, GroupId> cte_group_id_map;

    /**
     * Vector of tracked GroupExpressions
     * Group owns GroupExpressions, not the memo
     */
    std::unordered_set<GroupExprPtr, GroupExprPtrHash, GroupExprPtrEq> group_expressions;
};
}
