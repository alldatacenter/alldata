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

#include <Interpreters/IInterpreter.h>
#include <Parsers/IAST_fwd.h>
#include <Parsers/ASTSystemQuery.h>
#include <Storages/IStorage_fwd.h>
#include <Interpreters/StorageID.h>
#include <Common/ActionLock.h>
#include <Disks/IVolume.h>


namespace Poco { class Logger; }

namespace DB
{

class Context;
class AccessRightsElements;


/** Implement various SYSTEM queries.
  * Examples: SYSTEM SHUTDOWN, SYSTEM DROP MARK CACHE.
  *
  * Some commands are intended to stop/start background actions for tables and comes with two variants:
  *
  * 1. SYSTEM STOP MERGES table, SYSTEM START MERGES table
  * - start/stop actions for specific table.
  *
  * 2. SYSTEM STOP MERGES, SYSTEM START MERGES
  * - start/stop actions for all existing tables.
  * Note that the actions for tables that will be created after this query will not be affected.
  */
class InterpreterSystemQuery : public IInterpreter, WithMutableContext
{
public:
    InterpreterSystemQuery(const ASTPtr & query_ptr_, ContextMutablePtr context_);

    BlockIO execute() override;

private:
    ASTPtr query_ptr;
    Poco::Logger * log = nullptr;
    StorageID table_id = StorageID::createEmpty();      /// Will be set up if query contains table name
    VolumePtr volume_ptr;

    /// Tries to get a replicated table and restart it
    /// Returns pointer to a newly created table if the restart was successful
    StoragePtr tryRestartReplica(const StorageID & replica, ContextMutablePtr context, bool need_ddl_guard = true);

    void restartReplicas(ContextMutablePtr system_context);
    void syncReplica(ASTSystemQuery & query);

    void restoreReplica();

    void dropReplica(ASTSystemQuery & query);
    bool dropReplicaImpl(ASTSystemQuery & query, const StoragePtr & table);
    void flushDistributed(ASTSystemQuery & query);
    void restartDisk(String & name);

    AccessRightsElements getRequiredAccessForDDLOnCluster() const;
    void startStopAction(StorageActionBlockType action_type, bool start);
    void startOrStopConsume(ASTSystemQuery::Type type);

    void executeMetastoreCmd(ASTSystemQuery & query) const;

    void executeDedup(const ASTSystemQuery & query);

    void dumpCnchServerManagerStatus();

    void dropCnchPartCache(ASTSystemQuery & query);

    void dropChecksumsCache(const StorageID & table_id) const;

    BlockIO executeCnchCommand(ASTSystemQuery & query, ContextMutablePtr & system_context);
    BlockIO executeLocalCommand(ASTSystemQuery & query, ContextMutablePtr & system_context);

    void executeBGTaskInCnchServer(ContextMutablePtr & system_context, ASTSystemQuery::Type type) const;

    void executeSyncDedupWorker(ContextMutablePtr & system_context) const;

    // clear Broken Table infos
    void clearBrokenTables(ContextMutablePtr & system_context) const;

    void extendQueryLogElemImpl(QueryLogElement &, const ASTPtr &, ContextPtr) const override;

    /// fetch part from remote storage and attach to target table.
    void fetchParts(const ASTSystemQuery & query, const StorageID & table_id, ContextPtr local_context);

    void executeActionOnCNCHLog(const String & table, ASTSystemQuery::Type type);
};


}
