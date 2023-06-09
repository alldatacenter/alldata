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

#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include "Catalog/DataModelPartWrapper_fwd.h"
#include <Poco/Logger.h>
#include <Core/Types.h>
#include <Core/Names.h>
#include <Core/Block.h>
#include <Storages/IStorage.h>
#include <Interpreters/Context_fwd.h>
#include <mutex>

namespace DB
{

class StorageCloudMergeTree;

namespace IngestColumnCnch
{

/// Do the ingestion job in worker
class MemoryInefficientIngest
{
public:
    struct IngestSource
    {
        IngestSource(const Block & block_) : block(block_) {}
        Block block;
        mutable std::mutex mutex;
    };

    using IngestSourcePtr = std::shared_ptr<IngestSource>;
    using IngestSources = std::vector<IngestSourcePtr>;

    MemoryInefficientIngest(
        StoragePtr target_storage,
        StoragePtr source_storage,
        String partition_id,
        Strings ingest_column_names,
        Strings ordered_key_names,
        ContextPtr context_);

    std::vector<MemoryInefficientIngest::IngestSources> readSourceParts(const Names &);
    void ingestPartition();

private:
    StoragePtr target_storage;
    StoragePtr source_storage;
    StorageCloudMergeTree * target_cloud_merge_tree;
    StorageCloudMergeTree * source_cloud_merge_tree;
    const String partition_id;
    const Strings ingest_column_names;
    const Strings ordered_key_names;
    MergeTreeDataPartsVector target_parts;
    MergeTreeDataPartsVector source_parts;
    ContextPtr context;
    Poco::Logger * log;
};


} /// end namespace IngestColumnCnch

}
