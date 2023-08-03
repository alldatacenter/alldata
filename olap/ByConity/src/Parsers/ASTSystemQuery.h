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

#include <Parsers/ASTQueryWithOnCluster.h>
#include <Parsers/IAST.h>

#if !defined(ARCADIA_BUILD)
#    include "config_core.h"
#endif


namespace DB
{

enum class MetastoreOperation
{
    UNKNOWN,
    START_AUTO_SYNC,
    STOP_AUTO_SYNC,
    SYNC,
    DROP_ALL_KEY,
    DROP_BY_KEY
};

const char * metaOptToString(MetastoreOperation opt);

struct MetastoreOptions
{
    MetastoreOperation operation = MetastoreOperation::UNKNOWN;
    String drop_key {};
};

class ASTSystemQuery : public IAST, public ASTQueryWithOnCluster
{
public:

    enum class Type
    {
        UNKNOWN,
        SHUTDOWN,
        KILL,
        SUSPEND,
        DROP_DNS_CACHE,
        DROP_MARK_CACHE,
        DROP_UNCOMPRESSED_CACHE,
        DROP_MMAP_CACHE,
        DROP_CHECKSUMS_CACHE,
        DROP_CNCH_PART_CACHE,
#if USE_EMBEDDED_COMPILER
        DROP_COMPILED_EXPRESSION_CACHE,
#endif
        STOP_LISTEN_QUERIES,
        START_LISTEN_QUERIES,
        RESTART_REPLICAS,
        RESTART_REPLICA,
        RESTORE_REPLICA,
        DROP_REPLICA,
        SYNC_REPLICA,
        START_RESOURCE_GROUP,
        STOP_RESOURCE_GROUP,
        RELOAD_DICTIONARY,
        RELOAD_DICTIONARIES,
        RELOAD_MODEL,
        RELOAD_MODELS,
        RELOAD_EMBEDDED_DICTIONARIES,
        RELOAD_CONFIG,
        RELOAD_FORMAT_SCHEMA,
        RELOAD_SYMBOLS,
        RESTART_DISK,
        STOP_MERGES,
        START_MERGES,
        REMOVE_MERGES,
        STOP_GC,
        START_GC,
        FORCE_GC,
        STOP_TTL_MERGES,
        START_TTL_MERGES,
        STOP_FETCHES,
        START_FETCHES,
        STOP_MOVES,
        START_MOVES,
        STOP_REPLICATED_SENDS,
        START_REPLICATED_SENDS,
        STOP_REPLICATION_QUEUES,
        START_REPLICATION_QUEUES,
        FLUSH_LOGS,
        FLUSH_CNCH_LOG,
        STOP_CNCH_LOG,
        RESUME_CNCH_LOG,
        FLUSH_DISTRIBUTED,
        STOP_DISTRIBUTED_SENDS,
        START_DISTRIBUTED_SENDS,
        START_CONSUME,
        STOP_CONSUME,
        RESTART_CONSUME,
        FETCH_PARTS,
        METASTORE,
        CLEAR_BROKEN_TABLES,
        DEDUP, // dedup db.table [partition partition_expr] for repair
        SYNC_DEDUP_WORKER,
        START_DEDUP_WORKER,
        STOP_DEDUP_WORKER,
        DUMP_SERVER_STATUS,
        END
    };

    static const char * typeToString(Type type);

    Type type = Type::UNKNOWN;

    String target_model;
    String database;
    String table;
    String replica;
    String replica_zk_path;
    bool is_drop_whole_replica{};
    String storage_policy;
    String volume;
    String disk;
    UInt64 seconds{};

    // For execute/reload mutation
    String mutation_id;

    MetastoreOptions meta_ops;

    ASTPtr predicate;
    ASTPtr values_changes;

    ASTPtr target_path;

    // For DEDUP
    ASTPtr partition; // The value or ID of the partition is stored here.

    String getID(char) const override { return "SYSTEM query"; }

    ASTType getType() const override { return ASTType::ASTSystemQuery; }

    ASTPtr clone() const override;

    ASTPtr getRewrittenASTWithoutOnCluster(const std::string & new_database) const override
    {
        return removeOnCluster<ASTSystemQuery>(clone(), new_database);
    }

protected:

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};


}
