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

#include <Protos/DataModelHelpers.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Transaction/Actions/IAction.h>
#include <Transaction/TransactionCommon.h>
#include <cppkafka/cppkafka.h>

namespace DB
{

class InsertAction : public IAction
{
public:
    InsertAction(
        const ContextPtr & query_context_,
        const TxnTimestamp & txn_id_,
        StoragePtr table_,
        MutableMergeTreeDataPartsCNCHVector parts_ = {},
        DeleteBitmapMetaPtrVector delete_bitmaps_ = {},
        MutableMergeTreeDataPartsCNCHVector staged_parts_ = {})
        : IAction(query_context_, txn_id_)
        , table(std::move(table_))
        , parts(std::move(parts_))
        , delete_bitmaps(std::move(delete_bitmaps_))
        , staged_parts(std::move(staged_parts_))
    {}

    ~InsertAction() override = default;

    void appendPart(MutableMergeTreeDataPartCNCHPtr part);
    void appendDeleteBitmap(DeleteBitmapMetaPtr delete_bitmap);

    /// v1 APIs
    void executeV1(TxnTimestamp commit_time) override;

    /// V2 APIs
    void executeV2() override;
    void postCommit(TxnTimestamp commit_time) override;
    void abort() override;

    UInt32 collectNewParts() const override;

    void setBlockID(UInt64 * block_id_) { block_id = block_id_; }

    static UInt32 collectNewParts(MutableMergeTreeDataPartsCNCHVector const& parts_);

    UInt32 getSize() const override { return parts.size() + delete_bitmaps.size(); }

private:
    const StoragePtr table;
    MutableMergeTreeDataPartsCNCHVector parts;
    DeleteBitmapMetaPtrVector delete_bitmaps;
    MutableMergeTreeDataPartsCNCHVector staged_parts;

    bool executed{false};
    Poco::Logger * log{&Poco::Logger::get("InsertAction")};
    UInt64 * block_id = nullptr;
};

}

