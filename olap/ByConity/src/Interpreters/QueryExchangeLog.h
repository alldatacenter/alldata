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
#include <Interpreters/ClientInfo.h>

namespace ProfileEvents
{
    class Counters;
}

namespace DB
{
struct QueryExchangeLogElement
{
    String initial_query_id{"-1"};
    String write_segment_id{"-1"};
    String read_segment_id{"-1"};
    String partition_id{"-1"};
    String coordinator_address{};
    time_t event_time{};

    Int32 finish_code{};
    Int8 is_modifier{};
    String message;
    String type;

    // send metric
    UInt64 send_time_ms{};
    UInt64 send_rows{};
    UInt64 send_bytes{};
    UInt64 send_uncompressed_bytes{};
    UInt64 num_send_times{};
    UInt64 ser_time_ms{};
    UInt64 send_retry{};
    UInt64 send_retry_ms{};
    UInt64 overcrowded_retry{};

    // recv metric
    UInt64 recv_time_ms{};
    UInt64 register_time_ms{};
    UInt64 recv_rows{};
    UInt64 recv_bytes{};
    UInt64 dser_time_ms{};


    std::shared_ptr<ProfileEvents::Counters> profile_counters;

    static std::string name() { return "QueryExchangeLog"; }

    static NamesAndTypesList getNamesAndTypes();
    static NamesAndAliases getNamesAndAliases();
    void appendToBlock(MutableColumns & columns) const;
};


class QueryExchangeLog : public SystemLog<QueryExchangeLogElement>
{
    using SystemLog<QueryExchangeLogElement>::SystemLog;
};

}
