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

#include <memory>
#include <DataStreams/BlockIO.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/Context_fwd.h>
#include <Protos/plan_segment_manager.pb.h>

namespace DB
{
class Context;

BlockIO lazyExecutePlanSegmentLocally(PlanSegmentPtr plan_segment, ContextMutablePtr context);

void executePlanSegmentInternal(PlanSegmentPtr plan_segment, ContextMutablePtr context, bool async);

void executePlanSegmentRemotely(const PlanSegment & plan_segment, ContextPtr context, bool async);

void executePlanSegmentLocally(const PlanSegment & plan_segment, ContextPtr initial_query_context);

/**
 * Extract execute PlanSegmentTree as a common logic.
 * Used by InterpreterSelectQueryUseOptimizer and InterpreterDistributedStages
 */
BlockIO executePlanSegmentTree(PlanSegmentTreePtr & plan_segment_tree, ContextMutablePtr context);

}
