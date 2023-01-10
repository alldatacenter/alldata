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

#include <cctype>
#include <cstdint>
#include <cstring>

namespace DB::ResourceManagement
{
namespace VWScheduleAlgoImpl
{
    /// vw_schedule_algo.
    enum Algo : uint8_t
    {
        Unknown = 0,
        Random = 1,
        LocalRoundRobin = 2,
        LocalLowCpu = 3,
        LocalLowMem = 4,
        LocalLowDisk = 5,

        GlobalRoundRobin = 102,
        GlobalLowCpu = 103,
        GlobalLowMem = 104,
        GlobalLowDisk = 105, /// for pickWorker
    };
}
using VWScheduleAlgo = VWScheduleAlgoImpl::Algo;

constexpr auto toString(VWScheduleAlgo algo)
{
    switch (algo)
    {
        case VWScheduleAlgo::Random:
            return "Random";
        case VWScheduleAlgo::LocalRoundRobin:
            return "LocalRoundRobin";
        case VWScheduleAlgo::LocalLowCpu:
            return "LocalLowCpu";
        case VWScheduleAlgo::LocalLowMem:
            return "LocalLowMem";
        case VWScheduleAlgo::LocalLowDisk:
            return "LocalLowDisk";

        case VWScheduleAlgo::GlobalRoundRobin:
            return "GlobalRoundRobin";
        case VWScheduleAlgo::GlobalLowCpu:
            return "GlobalLowCpu";
        case VWScheduleAlgo::GlobalLowMem:
            return "GlobalLowMem";
        case VWScheduleAlgo::GlobalLowDisk:
            return "GlobalLowDisk";

        default:
            return "Unknown";
    }
}

constexpr auto toVWScheduleAlgo(char * algo_str)
{
    for (size_t i = 0; algo_str[i]; ++i)
    {
        auto & c = algo_str[i];
        c = std::tolower(c);
    }

    if (strcmp(algo_str,  "random") == 0)
        return VWScheduleAlgo::Random;
    else if (strcmp(algo_str,  "localroundrobin") == 0)
        return VWScheduleAlgo::LocalRoundRobin;
    else if (strcmp(algo_str,  "locallowcpu") == 0)
        return VWScheduleAlgo::LocalLowCpu;
    else if (strcmp(algo_str,  "locallowmem") == 0)
        return VWScheduleAlgo::LocalLowMem;
    else if (strcmp(algo_str,  "locallowdisk") == 0)
        return VWScheduleAlgo::LocalLowDisk;

    else if (strcmp(algo_str,  "globalroundrobin") == 0)
        return VWScheduleAlgo::GlobalRoundRobin;
    else if (strcmp(algo_str,  "globallowcpu") == 0)
        return VWScheduleAlgo::GlobalLowCpu;
    else if (strcmp(algo_str,  "globallowmem") == 0)
        return VWScheduleAlgo::GlobalLowMem;
    else if (strcmp(algo_str,  "globallowdisk") == 0)
        return VWScheduleAlgo::GlobalLowDisk;
    else
        return VWScheduleAlgo::Unknown;
}

}
