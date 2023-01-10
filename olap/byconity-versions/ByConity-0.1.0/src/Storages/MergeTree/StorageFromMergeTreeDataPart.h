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
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeDataSelectExecutor.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <Processors/QueryPipeline.h>
#include <Core/Defines.h>

#include <common/shared_ptr_helper.h>

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

namespace DB
{

/// A Storage that allows reading from a single MergeTree data part.
class StorageFromMergeTreeDataPart final : public shared_ptr_helper<StorageFromMergeTreeDataPart>, public IStorage
{
    friend struct shared_ptr_helper<StorageFromMergeTreeDataPart>;
public:
    String getName() const override { return "FromMergeTreeDataPart"; }

    Pipe read(
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum /*processed_stage*/,
        size_t max_block_size,
        unsigned num_streams) override
    {
        /// copy `delete_bitmaps` because delete_bitmap_getter may live longer than `this`
        MergeTreeData::DeleteBitmapGetter delete_bitmap_getter = [snapshot = delete_bitmaps](auto & part) -> ImmutableDeleteBitmapPtr
        {
            if (auto it = snapshot.find(part); it != snapshot.end())
                return it->second;
            throw Exception(ErrorCodes::LOGICAL_ERROR, "Not found delete bitmap for part " + part->name);
        };
        // NOTE: It's used to read normal parts only
        QueryPlan query_plan = std::move(*MergeTreeDataSelectExecutor(parts.front()->storage)
                                              .readFromParts(
                                                  parts,
                                                  delete_bitmap_getter,
                                                  column_names,
                                                  metadata_snapshot,
                                                  metadata_snapshot,
                                                  query_info,
                                                  context,
                                                  max_block_size,
                                                  num_streams));

        return query_plan.convertToPipe(
            QueryPlanOptimizationSettings::fromContext(context), BuildQueryPipelineSettings::fromContext(context));
    }

    bool supportsPrewhere() const override { return true; }

    bool supportsIndexForIn() const override { return true; }

    bool mayBenefitFromIndexForIn(
        const ASTPtr & left_in_operand, ContextPtr query_context, const StorageMetadataPtr & metadata_snapshot) const override
    {
        return parts.front()->storage.mayBenefitFromIndexForIn(left_in_operand, query_context, metadata_snapshot);
    }

    NamesAndTypesList getVirtuals() const override
    {
        return parts.front()->storage.getVirtuals();
    }

    String getPartitionId() const
    {
        return parts.front()->info.partition_id;
    }

    String getPartitionIDFromQuery(const ASTPtr & ast, ContextPtr context) const
    {
        return parts.front()->storage.getPartitionIDFromQuery(ast, context);
    }

    void checkMutationIsPossible(const MutationCommands & commands, const Settings & settings) const override
    {
        return parts.front()->storage.checkMutationIsPossible(commands, settings);
    }

    const MergeTreeData::DataPartsVector & getParts() const
    {
        return parts;
    }

    /// use a different delete bitmap than part's one for reading
    void setDeleteBitmap(const MergeTreeData::DataPartPtr & part, DeleteBitmapPtr delete_bitmap)
    {
        delete_bitmaps[part] = std::move(delete_bitmap);
    }

protected:
    StorageFromMergeTreeDataPart(const MergeTreeData::DataPartPtr & part_)
        : IStorage(getIDFromPart(part_))
        , parts({part_})
    {
        setInMemoryMetadata(part_->storage.getInMemoryMetadata());
        delete_bitmaps.insert({part_, part_->getDeleteBitmap()});
    }

    StorageFromMergeTreeDataPart(MergeTreeData::DataPartsVector && parts_)
        : IStorage(getIDFromParts(parts_))
        , parts(std::move(parts_))
    {
        setInMemoryMetadata(parts.front()->storage.getInMemoryMetadata());
        for (auto & part : parts)
            delete_bitmaps.insert({part, part->getDeleteBitmap()});
    }

private:
    MergeTreeData::DataPartsVector parts;
    MergeTreeData::DataPartsDeleteSnapshot delete_bitmaps;

    static StorageID getIDFromPart(const MergeTreeData::DataPartPtr & part_)
    {
        auto table_id = part_->storage.getStorageID();
        return StorageID(table_id.database_name, table_id.table_name + " (part " + part_->name + ")");
    }

    static StorageID getIDFromParts(const MergeTreeData::DataPartsVector & parts_)
    {
        assert(!parts_.empty());
        auto table_id = parts_.front()->storage.getStorageID();
        return StorageID(table_id.database_name, table_id.table_name + " (parts)");
    }
};

}
