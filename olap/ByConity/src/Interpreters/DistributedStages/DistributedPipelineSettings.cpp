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

#include <Interpreters/DistributedStages/DistributedPipelineSettings.h>

#include <Interpreters/DistributedStages/PlanSegment.h>

namespace DB
{
DistributedPipelineSettings DistributedPipelineSettings::fromPlanSegment(PlanSegment * plan_segment)
{
    DistributedPipelineSettings settings;
    settings.is_distributed = true;
    settings.query_id = plan_segment->getQueryId();
    settings.plan_segment_id = plan_segment->getPlanSegmentId();
    settings.parallel_size = plan_segment->getParallelSize();
    settings.coordinator_address = plan_segment->getCoordinatorAddress();
    settings.current_address = plan_segment->getCurrentAddress();
    return settings;
}
}
