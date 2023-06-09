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

#include <Core/Types.h>

namespace DB
{

enum class ExchangeMode : UInt8
{
    UNKNOWN = 0,
    LOCAL_NO_NEED_REPARTITION, // for global join, if we want to increase the parallel size, just split it
    LOCAL_MAY_NEED_REPARTITION, // for local join, if we want to increase the parallel size, we need repartition
    REPARTITION,
    BROADCAST,
    GATHER
};

String exchangeModeToString(const ExchangeMode & exchange_mode);

}
