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

#include <Catalog/DataModelPartWrapper.h>
#include <Protos/DataModelHelpers.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int FORMAT_VERSION_TOO_OLD;
    extern const int CORRUPTED_DATA;
}

DataModelPartWrapper::DataModelPartWrapper() = default;

UInt64 ServerDataPart::getCommitTime() const
{
    if (commit_time)
        return *commit_time;
    return part_model_wrapper->part_model->commit_time();
}

void ServerDataPart::setCommitTime(const UInt64 & new_commmit_time) const
{
    commit_time = std::make_optional<UInt64>(new_commmit_time);
}

UInt64 ServerDataPart::getColumnsCommitTime() const
{
    return part_model().has_columns_commit_time() ? part_model().columns_commit_time() : 0;
}

UInt64 ServerDataPart::getMutationCommitTime() const
{
    return part_model().has_mutation_commit_time() ? part_model().mutation_commit_time() : 0;
}

bool ServerDataPart::containsExactly(const ServerDataPart & other) const
{
    const auto & this_info = *part_model_wrapper->info;
    const auto & other_info = *other.part_model_wrapper->info;
    /// Note: For parts with same p_id, block_id, the higher the level, the greater the commit_time.
    /// We only compare level or commit_time here for fault tolerance.
    return this_info.partition_id == other_info.partition_id
        && this_info.min_block == other_info.min_block
        && this_info.max_block == other_info.max_block
        && (this_info.level > other_info.level || getCommitTime() > other.getCommitTime());
}

void ServerDataPart::setPreviousPart(const ServerDataPartPtr & part) const { prev_part = part; }
const ServerDataPartPtr & ServerDataPart::tryGetPreviousPart() const { return prev_part; }

const ServerDataPartPtr & ServerDataPart::getPreviousPart() const
{
    if (!prev_part)
        throw Exception("No previous part of " + part_model_wrapper->name, ErrorCodes::LOGICAL_ERROR);
    return prev_part;
}

ServerDataPartPtr ServerDataPart::getBasePart() const
{
    ServerDataPartPtr part = shared_from_this();
    while (part->isPartial())
    {
        if (!(part = part->tryGetPreviousPart()))
            throw Exception("Previous part of partial part " + part_model_wrapper->name + " is absent", ErrorCodes::LOGICAL_ERROR);
    }
    return part;
}

void ServerDataPart::serializePartitionAndMinMaxIndex(const MergeTreeMetaBase & storage, WriteBuffer & buf) const
{
    if (unlikely(storage.format_version < MERGE_TREE_DATA_MIN_FORMAT_VERSION_WITH_CUSTOM_PARTITIONING))
        throw Exception("MergeTree data format is too old", ErrorCodes::FORMAT_VERSION_TOO_OLD);

    part_model_wrapper->partition.store(storage, buf);
    if (!isEmpty())
    {
        if (!minmax_idx() || !minmax_idx()->initialized)
            throw Exception("Attempt to write uninitialized MinMax index", ErrorCodes::LOGICAL_ERROR);
        /// FIXME: use correct part path
        minmax_idx()->store(storage, "", buf);
    }

    /// Skip partition_id check if this is a deleted part
    if (!deleted())
    {
        String calculated_partition_id = part_model_wrapper->partition.getID(storage);
        if (calculated_partition_id != info().partition_id)
            throw Exception(
                "While loading part " + name() + ": calculated partition ID: " + calculated_partition_id
                    + " differs from partition ID in part name: " + info().partition_id,
                ErrorCodes::CORRUPTED_DATA);
    }
}

void ServerDataPart::serializeDeleteBitmapMetas([[maybe_unused]] const MergeTreeMetaBase & storage, WriteBuffer & buffer) const
{
    // assert(storage.hasUniqueKey());
    UInt64 num = std::distance(delete_bitmap_metas.begin(), delete_bitmap_metas.end());
    writeVarUInt(num, buffer);

    String tmp_buf;
    for (auto & meta : delete_bitmap_metas)
    {
        tmp_buf.resize(0);
        if (unlikely(!meta->SerializeToString(&tmp_buf)))
            throw Exception("Failed to serialize delete bitmap meta for " + name(), ErrorCodes::LOGICAL_ERROR);
        writeStringBinary(tmp_buf, buffer);
    }
}

UInt64 ServerDataPart::rowsCount() const { return part_model_wrapper->part_model->rows_count(); }
bool ServerDataPart::isEmpty() const { return !isPartial() && part_model_wrapper->part_model->rows_count() == 0; }
UInt64 ServerDataPart::size() const { return part_model_wrapper->part_model->size();}
bool ServerDataPart::isPartial() const { return part_model_wrapper->info->hint_mutation; }
bool ServerDataPart::isDropRangePart() const { return deleted() && part_model_wrapper->info->min_block == 0 && part_model_wrapper->info->level == MergeTreePartInfo::MAX_LEVEL;}
bool ServerDataPart::deleted() const { return part_model_wrapper->part_model->deleted(); }
const Protos::DataModelPart & ServerDataPart::part_model() const { return *part_model_wrapper->part_model; }
const MergeTreePartInfo & ServerDataPart::info() const { return *part_model_wrapper->info; }
const String & ServerDataPart::name() const { return part_model_wrapper->name; }
const MergeTreePartition & ServerDataPart::partition() const { return part_model_wrapper->partition; }
const std::shared_ptr<IMergeTreeDataPart::MinMaxIndex> & ServerDataPart::minmax_idx() const { return part_model_wrapper->minmax_idx; }

MutableMergeTreeDataPartCNCHPtr ServerDataPart::toCNCHDataPart(
    const MergeTreeMetaBase & storage,
    /*const std::unordered_map<UInt32, String> & id_full_paths,*/
    const std::optional<std::string> & relative_path) const
{
    auto res = createPartFromModel(storage, part_model(), /*id_full_paths,*/ relative_path);

    if (prev_part)
        res->setPreviousPart(prev_part->toCNCHDataPart(storage, /*id_full_paths,*/ relative_path));

    return res;
}

void ServerDataPart::setVirtualPartSize(const UInt64 & vp_size) const { virtual_part_size = vp_size; }

UInt64 ServerDataPart::getVirtualPartSize() const { return virtual_part_size; }

}
