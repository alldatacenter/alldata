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

#include <CloudServices/CnchWorkerClient.h>

namespace DB
{

namespace DedupWorkerHeartbeatResultDef
{
    enum Values : uint32_t
    {
        Invalid,
        Kill,
        Success
    };
}

using DedupWorkerHeartbeatResult = DedupWorkerHeartbeatResultDef::Values;

struct DedupWorkerStatus
{
    bool is_active;
    time_t create_time = 0;
    UInt64 total_schedule_cnt = 0;
    UInt64 total_dedup_cnt = 0;
    UInt64 last_schedule_wait_ms = 0;
    UInt64 last_task_total_cost_ms = 0;
    UInt64 last_task_dedup_cost_ms = 0;
    UInt64 last_task_publish_cost_ms = 0;
    UInt64 last_task_staged_part_cnt = 0;
    UInt64 last_task_visible_part_cnt = 0;
    UInt64 last_task_staged_part_total_rows = 0;
    UInt64 last_task_visible_part_total_rows = 0;

    String worker_rpc_address;
    String worker_tcp_address;
    String last_exception;
    time_t last_exception_time = 0;
};

}
