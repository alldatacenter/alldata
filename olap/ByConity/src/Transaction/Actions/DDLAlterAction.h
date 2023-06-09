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

#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/MutationCommands.h>
#include <Transaction/Actions/IAction.h>

namespace DB
{

class DDLAlterAction : public IAction
{
public:
    DDLAlterAction(const ContextPtr & query_context_, const TxnTimestamp & txn_id_, StoragePtr table_)
        : IAction(query_context_, txn_id_),
        log(&Poco::Logger::get("AlterAction")),
        table(std::move(table_))
    {
    }

    ~DDLAlterAction() override = default;

    /// TODO: versions
    void setNewSchema(String schema_);
    String getNewSchema() const { return new_schema; }

    void setMutationCommands(MutationCommands commands);

    void executeV1(TxnTimestamp commit_time) override;

private:
    void updateTsCache(const UUID & uuid, const TxnTimestamp & commit_time) override;
    void appendPart(MutableMergeTreeDataPartCNCHPtr part);
    static void updatePartData(MutableMergeTreeDataPartCNCHPtr part, TxnTimestamp commit_time);

    Poco::Logger * log;
    const StoragePtr table;

    String new_schema;
    MutationCommands mutation_commands;
};

using DDLAlterActionPtr = std::shared_ptr<DDLAlterAction>;

}
