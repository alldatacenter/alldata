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
#include <Parsers/ASTCreateQuery.h>
#include <Transaction/Actions/IAction.h>

namespace DB
{
struct CreateActionParams
{
    String database;
    String table;
    UUID uuid;
    String statement;
    bool attach = false;
    bool is_dictionary = false;
};

class DDLCreateAction : public IAction
{

public:
    DDLCreateAction(const ContextPtr & query_context_, const TxnTimestamp & txn_id_, CreateActionParams params_)
        : IAction(query_context_, txn_id_), params(std::move(params_))
    {}

    ~DDLCreateAction() override = default;

    void executeV1(TxnTimestamp commit_time) override;
    void abort() override;

private:
    void updateTsCache(const UUID & uuid, const TxnTimestamp & commit_time) override;

private:
    CreateActionParams params;
};

using DDLCreateActionPtr = std::shared_ptr<DDLCreateAction>;

}



