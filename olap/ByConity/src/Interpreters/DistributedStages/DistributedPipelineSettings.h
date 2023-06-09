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

#include <Interpreters/DistributedStages/AddressInfo.h>

namespace DB
{
class PlanSegment;

struct DistributedPipelineSettings
{
    bool is_distributed = false;
    String query_id{};
    size_t plan_segment_id = 0;
    size_t parallel_size = 1;
    AddressInfo coordinator_address{};
    AddressInfo current_address{};

    static DistributedPipelineSettings fromPlanSegment(PlanSegment * plan_segment);
};
}
