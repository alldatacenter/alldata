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
#include <Poco/Net/IPAddress.h>
#include <Parsers/IAST.h>
#include <Interpreters/Context.h>

namespace DB
{

using QueryMetricLogState = QueryLogElementType;
struct QueryStatusInfo;
struct BlockStreamProfileInfo;

struct QueryMetricLogType
{
    enum QueryType
    {
        UNKNOWN,
        CREATE,
        DROP,
        RENAME,
        SELECT,
        INSERT,
        DELETE,
        ALTER,
        OTHER,
    };

    QueryType type;

    explicit QueryMetricLogType(QueryType type_ = UNKNOWN)
        : type(type_) {}

    explicit QueryMetricLogType(UInt8 type_)
        : type(QueryType(type_)) {}

    explicit QueryMetricLogType(const ASTPtr & ast);

    String toString() const
    {
        switch (type)
        {
            case UNKNOWN:
                return "UNKNOWN";
            case CREATE:
                return "CREATE";
            case DROP:
                return "DROP";
            case RENAME:
                return "RENAME";
            case SELECT:
                return "SELECT";
            case INSERT:
                return "INSERT";
            case DELETE:
                return "DELETE";
            case ALTER:
                return "ALTER";
            case OTHER:
                return "OTHER";
        }
        return "";
    }
};

void extractDatabaseAndTableNames(const Context & context, const ASTPtr & ast, String & database, String & table);

void insertCnchQueryMetric(
    ContextMutablePtr context,
    const String & query,
    QueryMetricLogState state,
    time_t current_time,
    const ASTPtr & ast = nullptr,
    const QueryStatusInfo * info = nullptr,
    const BlockStreamProfileInfo * stream_in_info = nullptr,
    const QueryPipeline * query_pipeline = nullptr,
    bool empty_stream = false,
    UInt8 complex_query = 0,
    UInt32 init_time = 0,
    UInt32 runtime_latency = 0,
    const String & exception = {},
    const String & stack_trace = {});

}
