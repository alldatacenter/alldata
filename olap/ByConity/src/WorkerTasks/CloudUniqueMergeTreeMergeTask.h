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

#include <memory>
#include <CloudServices/commitCnchParts.h>
#include <Core/Types.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <WorkerTasks/ManipulationTask.h>
#include <WorkerTasks/ManipulationTaskParams.h>
#include <common/logger_useful.h>

namespace DB
{
namespace CatalogService
{
    class Catalog;
}

class MergeTreeDataMerger;
class StorageCloudMergeTree;

class CloudUniqueMergeTreeMergeTask : public std::enable_shared_from_this<CloudUniqueMergeTreeMergeTask>, public ManipulationTask
{
public:
    CloudUniqueMergeTreeMergeTask(StorageCloudMergeTree & storage_, ManipulationTaskParams params_, ContextPtr context_);

    void executeImpl() override;

private:
    DeleteBitmapMetaPtrVector
    getDeleteBitmapMetas(Catalog::Catalog & catalog, const IMergeTreeDataPartsVector & parts, TxnTimestamp ts);

    void updateDeleteBitmap(Catalog::Catalog & catalog, const MergeTreeDataMerger & merger, DeleteBitmapPtr & out_bitmap);

    StorageCloudMergeTree & storage;
    String log_name;
    Poco::Logger * log;
    CnchDataWriter cnch_writer;
    String partition_id;

    DeleteBitmapMetaPtrVector prev_bitmap_metas;
    DeleteBitmapMetaPtrVector curr_bitmap_metas;
    ImmutableDeleteBitmapVector prev_bitmaps;
    ImmutableDeleteBitmapVector curr_bitmaps;
};

} // namespace DB
