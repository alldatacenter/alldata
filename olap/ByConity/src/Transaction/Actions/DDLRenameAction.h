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

#include <Core/UUID.h>
#include <Parsers/ASTRenameQuery.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Transaction/Actions/IAction.h>

namespace DB
{

struct RenameActionParams
{
    struct RenameTableParams
    {
        String from_database;
        String from_table;
        UUID from_table_uuid;
        String to_database;
        String to_table;
    };

    struct RenameDBParams
    {
        String from_database;
        String to_database;
        std::vector<UUID> uuids;
    };

    enum class Type
    {
        RENAME_DB,
        RENAME_TABLE
    };

    RenameTableParams table_params{};
    RenameDBParams db_params{};
    Type type {Type::RENAME_TABLE};
};


class DDLRenameAction : public IAction
{
public:
    DDLRenameAction(const ContextPtr & query_context_, const TxnTimestamp & txn_id_, RenameActionParams params_)
        : IAction(query_context_, txn_id_), params(std::move(params_))
    {}

    ~DDLRenameAction() override = default;

    void executeV1(TxnTimestamp commit_time) override;

private:
    void updateTsCache(const UUID & uuid, const TxnTimestamp & commit_time) override;

    void renameTablePrefix(TxnTimestamp commit_time);
    void renameTableSuffix(TxnTimestamp commit_time);

private:
    RenameActionParams params;

    bool is_cnch_merge_tree{false};
    // bool is_cnch_kafka{false};
};

using DDLRenameActionPtr = std::shared_ptr<DDLRenameAction>;

}
