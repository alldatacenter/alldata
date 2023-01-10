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

#include "common/types.h"
namespace DB {

struct ExchangeOptions
{
    UInt32 exhcange_timeout_ms;
    UInt64 send_threshold_in_bytes;
    UInt64 send_threshold_in_row_num;
    bool force_remote_mode = false;
    bool need_send_plan_segment_status = true;
    bool force_use_buffer = false;
};

}

