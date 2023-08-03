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

#include <Storages/IStorage.h>
#include <common/shared_ptr_helper.h>
#include <Processors/Pipe.h>

namespace DB
{
/** Internal temporary storage for table function input(...)
  */

class StorageInput final : public shared_ptr_helper<StorageInput>, public IStorage
{
    friend struct shared_ptr_helper<StorageInput>;
public:
    String getName() const override { return "Input"; }

    /// A table will read from this stream.
    void setPipe(Pipe pipe_);

    Pipe read(
        const Names & column_names,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned num_streams) override;

private:
    Pipe pipe;

protected:
    StorageInput(const StorageID & table_id, const ColumnsDescription & columns_);
};
}
