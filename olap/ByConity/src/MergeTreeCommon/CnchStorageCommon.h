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

#include <Interpreters/WorkerGroupHandle.h>
#include <Transaction/Actions/DDLAlterAction.h>
#include <Transaction/ICnchTransaction.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <DataStreams/NullBlockOutputStream.h>
#include <DataStreams/copyData.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <Interpreters/ProcessList.h>

namespace DB
{

enum class WorkerGroupUsageType
{
    NORMAL,
    WRITE,    /// write group for INSERT SELECT and INSERT INFILE
    BUFFER,
};

inline static String typeToString(WorkerGroupUsageType type)
{
    switch (type)
    {
        case WorkerGroupUsageType::NORMAL:
            return "NORMAL";
        case WorkerGroupUsageType::WRITE:
            return "WRITE";
        case WorkerGroupUsageType::BUFFER:
            return "BUFFER";
    }
}

class VirtualWarehouseHandleImpl;
class CnchTopologyMaster;
class ProcessListEntry;

enum class CNCHStorageMediumType
{
    HDFS,
    S3
};

String toStr(CNCHStorageMediumType tp);
CNCHStorageMediumType fromStr(const String& type_str);

class CnchStorageCommonHelper
{
public:
    CnchStorageCommonHelper(const StorageID & table_id_, const String & remote_database_, const String & remote_table_);

    bool forwardQueryToServerIfNeeded(ContextPtr query_context, const UUID & storage_uuid) const;

    static bool healthCheckForWorkerGroup(ContextPtr context, WorkerGroupHandle & worker_group);

    static void sendQueryPerShard(
        ContextPtr context,
        const String & query,
        const WorkerGroupHandleImpl::ShardInfo & shard_info,
        bool need_extended_profile_info = false);

    String getCloudTableName(ContextPtr context) const;

    // For each condition, we construct a block containing
    // arguments of condition and then run a function to execute this block.
    // The result of this condition will update mask for removing parts.
    static void filterCondition(
        const ASTPtr & expression,
        const ColumnsWithTypeAndName & columns,
        const std::map<String, size_t> & nameToIdx,
        ContextPtr context,
        std::vector<int> & mask,
        const SelectQueryInfo & query_info);

    // Get all conditions.
    // The method assumes that the expression are linearized,
    // which only concated by 'and'.
    // This is reasonable since we concat all possible conditions by 'and'
    // when move these conditions from where to implicit_where.
    static ASTs getConditions(const ASTPtr & ast);

    String getCreateQueryForCloudTable(
        const String & query,
        const String & local_table_name,
        const ContextPtr & context = nullptr,
        bool enable_staging_area = false,
        const std::optional<StorageID> & cnch_storage_id = std::nullopt) const;

    String getCreateQueryForCloudTable(
        const String & query,
        const String & local_table_name,
        const String & local_database_name,
        const ContextPtr & context = nullptr,
        bool enable_staging_area = false,
        const std::optional<StorageID> & cnch_storage_id = std::nullopt) const;

    static void rewritePlanSegmentQueryImpl(ASTPtr & query, const std::string & database, const std::string & table);

    /// select query has database, table and table function names as AST pointers
    /// Creates a copy of query, changes database, table and table function names.
    static ASTPtr rewriteSelectQuery(const ASTPtr & query, const std::string & database, const std::string & table);

    StorageID table_id;
    String remote_database;
    String remote_table;
    size_t index_for_subquery = 0;
};

}
