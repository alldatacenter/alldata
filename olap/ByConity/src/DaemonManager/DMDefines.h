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

namespace DB::DaemonManager
{
/// check liveness for every N iteration ~ each iteration 10 sec => total 4 minute
constexpr size_t LIVENESS_CHECK_INTERVAL = 24;
constexpr uint32_t BYTEKV_BATCH_SCAN = 100;
constexpr int32_t SLOW_EXECUTION_THRESHOLD_MS = 200;

using UUIDs = std::unordered_set<UUID>;

}
