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

#include <Interpreters/SystemLog.h>
#include <Core/NamesAndTypes.h>
#include <Core/NamesAndAliases.h>
#include <Processors/IProcessor.h>

namespace DB
{

struct ProcessorProfileLogElement
{
    time_t event_time{};
    Decimal64 event_time_microseconds{};

    UInt64 id{};
    std::vector<UInt64> parent_ids;

    UInt64 plan_step{};
    UInt64 plan_group{};

    String query_id;
    String processor_name;

    /// Milliseconds spend in IProcessor::work()
    UInt32 elapsed_us{};
    /// IProcessor::NeedData
    UInt32 input_wait_elapsed_us{};
    /// IProcessor::PortFull
    UInt32 output_wait_elapsed_us{};

    size_t input_rows{};
    size_t input_bytes{};
    size_t output_rows{};
    size_t output_bytes{};

    static std::string name() { return "ProcessorsProfileLog"; }
    static NamesAndTypesList getNamesAndTypes();
    static NamesAndAliases getNamesAndAliases() { return {}; }
    void appendToBlock(MutableColumns & columns) const;
};

class ProcessorsProfileLog : public SystemLog<ProcessorProfileLogElement>
{
public:
    ProcessorsProfileLog(
        ContextPtr context_,
        const String & database_name_,
        const String & table_name_,
        const String & storage_def_,
        size_t flush_interval_milliseconds_);
};

}
