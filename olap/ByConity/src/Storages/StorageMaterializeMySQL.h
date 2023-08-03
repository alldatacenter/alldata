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

#if !defined(ARCADIA_BUILD)
#include "config_core.h"
#endif

#if USE_MYSQL

#include <Storages/StorageProxy.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

class StorageMaterializeMySQL final : public shared_ptr_helper<StorageMaterializeMySQL>, public StorageProxy
{
    friend struct shared_ptr_helper<StorageMaterializeMySQL>;
public:
    String getName() const override { return "MaterializeMySQL"; }

    StorageMaterializeMySQL(const StoragePtr & nested_storage_, const IDatabase * database_);

    bool needRewriteQueryWithFinal(const Names & column_names) const override;

    Pipe read(
        const Names & column_names, const StorageMetadataPtr & metadata_snapshot, SelectQueryInfo & query_info,
        ContextPtr context, QueryProcessingStage::Enum processed_stage, size_t max_block_size, unsigned num_streams) override;

    BlockOutputStreamPtr write(const ASTPtr &, const StorageMetadataPtr &, ContextPtr) override { throwNotAllowed(); }

    NamesAndTypesList getVirtuals() const override;

    StoragePtr getNested() const override { return nested_storage; }

    void drop() override { nested_storage->drop(); }

private:
    [[noreturn]] void throwNotAllowed() const
    {
        throw Exception("This method is not allowed for MaterializeMySQL", ErrorCodes::NOT_IMPLEMENTED);
    }

    StoragePtr nested_storage;
    const IDatabase * database;
};

}

#endif
