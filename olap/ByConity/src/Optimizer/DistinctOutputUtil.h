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

#include <QueryPlan/PlanVisitor.h>

namespace DB
{
class DistinctOutputQueryUtil
{
public:
    static bool isDistinct(PlanNodeBase &);
};

class IsDistinctPlanVisitor : public PlanNodeVisitor<bool, Void>
{
public:
    bool visitPlanNode(PlanNodeBase & node, Void & c) override;
    bool visitValuesNode(ValuesNode & node, Void & context) override;
    bool visitLimitNode(LimitNode & node, Void & context) override;
    bool visitIntersectNode(IntersectNode & node, Void & context) override;
    bool visitEnforceSingleRowNode(EnforceSingleRowNode & node, Void & context) override;
    bool visitAggregatingNode(AggregatingNode & node, Void & context) override;
    bool visitAssignUniqueIdNode(AssignUniqueIdNode & node, Void & context) override;
    bool visitFilterNode(FilterNode & node, Void & context) override;
    bool visitDistinctNode(DistinctNode & node, Void & context) override;
    bool visitExceptNode(ExceptNode & node, Void & context) override;
    bool visitMergingSortedNode(MergingSortedNode & node, Void & context) override;
};
}
