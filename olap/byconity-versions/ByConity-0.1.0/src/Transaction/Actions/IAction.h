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
// #include <Databases/DatabaseCnch.h>
#include <Storages/IStorage_fwd.h>
#include <Transaction/TxnTimestamp.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Interpreters/Context_fwd.h>
#include <Common/TypePromotion.h>

namespace DB
{
class Context;

class IAction : public TypePromotion<IAction>, public WithContext
{
public:
    IAction(const ContextPtr & query_context_, const TxnTimestamp & txn_id_);
    virtual ~IAction() = default;

    /// V1 is the old API which performs data write and txn commit in one api calls.
    /// V2 is the new API which separate data write and txn commit with 2 api calls.
    /// Currently, still keep both of them as ddl is still executed with old api because the db/table metadata still does not support intermediate state.
    virtual void executeV1(TxnTimestamp commit_time) = 0; // write data and commit.
    virtual void executeV2() {} // write data only, do not commit here.
    virtual void abort() {}
    virtual void postCommit(TxnTimestamp /*commit_time*/) {}

    virtual UInt32 collectNewParts() const { return 0; }
    virtual UInt32 getSize() const { return 0; }
protected:
    const Context & global_context;
    TxnTimestamp txn_id;

private:
    virtual void updateTsCache(const UUID &, const TxnTimestamp &) {}
};

using ActionPtr = std::shared_ptr<IAction>;

}
