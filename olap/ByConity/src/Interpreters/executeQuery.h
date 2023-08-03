/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Core/QueryProcessingStage.h>
#include <DataStreams/BlockIO.h>
#include <Processors/QueryPipeline.h>
#include <Interpreters/SegmentScheduler.h>

namespace DB
{

class ReadBuffer;
class WriteBuffer;


/// Parse and execute a query.
void executeQuery(
    ReadBuffer & istr,                  /// Where to read query from (and data for INSERT, if present).
    WriteBuffer & ostr,                 /// Where to write query output to.
    bool allow_into_outfile,            /// If true and the query contains INTO OUTFILE section, redirect output to that file.
    ContextMutablePtr context,                 /// DB, tables, data types, storage engines, functions, aggregate functions...
    std::function<void(const String &, const String &, const String &, const String &)> set_result_details, /// If a non-empty callback is passed, it will be called with the query id, the content-type, the format, and the timezone.
    const std::optional<FormatSettings> & output_format_settings = std::nullopt, /// Format settings for output format, will be calculated from the context if not set.
    bool internal = false /// If a query is a internal which cannot be inserted into processlist.
);


/// More low-level function for server-to-server interaction.
/// Prepares a query for execution but doesn't execute it.
/// Returns a pair of block streams which, when used, will result in query execution.
/// This means that the caller can to the extent control the query execution pipeline.
///
/// To execute:
/// * if present, write INSERT data into BlockIO::out
/// * then read the results from BlockIO::in.
///
/// If the query doesn't involve data insertion or returning of results, out and in respectively
/// will be equal to nullptr.
///
/// Correctly formatting the results (according to INTO OUTFILE and FORMAT sections)
/// must be done separately.
BlockIO executeQuery(
    const String & query,     /// Query text without INSERT data. The latter must be written to BlockIO::out.
    ContextMutablePtr context,       /// DB, tables, data types, storage engines, functions, aggregate functions...
    bool internal = false,    /// If true, this query is caused by another query and thus needn't be registered in the ProcessList.
    QueryProcessingStage::Enum stage = QueryProcessingStage::Complete,    /// To which stage the query must be executed.
    bool may_have_embedded_data = false /// If insert query may have embedded data
);

/// Old interface with allow_processors flag. For compatibility.
BlockIO executeQuery(
    const String & query,
    ContextMutablePtr context,
    bool internal,
    QueryProcessingStage::Enum stage,
    bool may_have_embedded_data,
    bool allow_processors /// If can use processors pipeline
);

/// Get target server for a query, can be localhost
HostWithPorts getTargetServer(ContextPtr context, ASTPtr & ast);

/// Execute the query on the target server.
void executeQueryByProxy(ContextMutablePtr context, const HostWithPorts & server, const ASTPtr & ast, BlockIO & res);

}
