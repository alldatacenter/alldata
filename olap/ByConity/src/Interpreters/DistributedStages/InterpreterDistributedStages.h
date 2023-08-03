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

#include <Interpreters/Context.h>
#include <Interpreters/IInterpreter.h>
#include <Interpreters/SelectQueryOptions.h>
#include <Interpreters/DistributedStages/PlanSegment.h>

namespace DB
{

class SegmentScheduler;
using SegmentSchedulerPtr = std::shared_ptr<SegmentScheduler>;

struct DistributedStagesSettings
{
    bool enable_distributed_stages = false;
    bool fallback_to_simple_query = false;

    DistributedStagesSettings(
        bool enable_distributed_stages_,
        bool fallback_to_simple_query_
        )
        : enable_distributed_stages(enable_distributed_stages_)
        , fallback_to_simple_query(fallback_to_simple_query_)
        {}
};
class InterpreterDistributedStages : public IInterpreter
{
public:
    InterpreterDistributedStages(const ASTPtr & query_ptr_, ContextMutablePtr context_);

    void createPlanSegments();

    BlockIO execute() override;

    BlockIO executePlanSegment();

    ASTPtr getQuery() { return query_ptr; }

    void initSettings();

    PlanSegmentTree * getPlanSegmentTree() const { return plan_segment_tree.get(); }

    static bool isDistributedStages(const ASTPtr & query, ContextPtr context_);
    static DistributedStagesSettings extractDistributedStagesSettingsImpl(const ASTPtr & query, ContextPtr context_);
    static DistributedStagesSettings extractDistributedStagesSettings(const ASTPtr & query, ContextPtr context_);

private:

    ASTPtr query_ptr;
    ContextMutablePtr context;
    Poco::Logger * log;

    PlanSegmentTreePtr plan_segment_tree = nullptr;
};

}
