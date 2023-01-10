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

#include <common/shared_ptr_helper.h>
#include <Storages/IStorage.h>

namespace DB
{
class Context;

class StorageSystemCnchTableInfo : public shared_ptr_helper<StorageSystemCnchTableInfo>, public IStorage
{
    friend struct shared_ptr_helper<StorageSystemCnchTableInfo>;
public:
    std::string getName() const override { return "SystemCnchTableInfo"; }

    Pipe read(
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        const size_t max_block_size,
        const unsigned num_streams) override;


protected:
    StorageSystemCnchTableInfo(const StorageID & table_id_);

};

}
