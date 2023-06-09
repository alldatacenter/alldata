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


#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/HDFS/WriteBufferFromHDFS.h>
#include <Common/Coding.h>
#include <Common/ThreadPool.h>

namespace DB::ErrorCodes
{
extern const int LOGICAL_ERROR;
}

namespace DB
{
static String dataModelName(const DataModelDeleteBitmapPtr & model)
{
    std::stringstream ss;
    ss << model->partition_id() << "_" << model->part_min_block() << "_" << model->part_max_block() << "_"
       << model->reserved() << "_" << model->type() << "_" << model->txn_id();
    return ss.str();
}

static String deleteBitmapDirRelativePath(const String & partition_id)
{
    std::stringstream ss;
    ss << "DeleteFiles/" << partition_id <<  "/";
    return ss.str();
}

static String deleteBitmapFileRelativePath(const Protos::DataModelDeleteBitmap & model)
{
    std::stringstream ss;
    ss << deleteBitmapDirRelativePath(model.partition_id()) << model.part_min_block() << "_" << model.part_max_block() << "_"
       << model.reserved() << "_" << model.type() << "_" << model.txn_id() << ".bitmap";
    return ss.str();
}

std::shared_ptr<LocalDeleteBitmap> LocalDeleteBitmap::createBaseOrDelta(
    const MergeTreePartInfo & part_info,
    const ImmutableDeleteBitmapPtr & base_bitmap,
    const DeleteBitmapPtr & delta_bitmap,
    UInt64 txn_id)
{
    if (!base_bitmap || !delta_bitmap)
        throw Exception("base_bitmap and delta_bitmap cannot be null", ErrorCodes::LOGICAL_ERROR);

    if (delta_bitmap->cardinality() <= DeleteBitmapMeta::kInlineBitmapMaxCardinality)
    {
        return std::make_shared<LocalDeleteBitmap>(part_info, DeleteBitmapMetaType::Delta, txn_id, delta_bitmap);
    }
    else
    {
        *delta_bitmap |= *base_bitmap; // change delta_bitmap to the new base bitmap
        return std::make_shared<LocalDeleteBitmap>(part_info, DeleteBitmapMetaType::Base, txn_id, delta_bitmap);
    }
}

LocalDeleteBitmap::LocalDeleteBitmap(
    const MergeTreePartInfo & info, DeleteBitmapMetaType type, UInt64 txn_id, DeleteBitmapPtr bitmap_)
    : LocalDeleteBitmap(info.partition_id, info.min_block, info.max_block, type, txn_id, std::move(bitmap_))
{
}

LocalDeleteBitmap::LocalDeleteBitmap(
    const String & partition_id, Int64 min_block, Int64 max_block, DeleteBitmapMetaType type, UInt64 txn_id, DeleteBitmapPtr bitmap_)
    : model(std::make_shared<Protos::DataModelDeleteBitmap>()), bitmap(std::move(bitmap_))
{
    model->set_partition_id(partition_id);
    model->set_part_min_block(min_block);
    model->set_part_max_block(max_block);
    model->set_type(static_cast<Protos::DataModelDeleteBitmap_Type>(type));
    model->set_txn_id(txn_id);
    model->set_cardinality(bitmap ? bitmap->cardinality() : 0);
}

UndoResource LocalDeleteBitmap::getUndoResource(const TxnTimestamp & new_txn_id) const
{
    return UndoResource(new_txn_id, UndoResourceType::DeleteBitmap, dataModelName(model), deleteBitmapFileRelativePath(*model));
}

bool LocalDeleteBitmap::canInlineStoreInCatalog() const
{
    return !bitmap || bitmap->cardinality() <= DeleteBitmapMeta::kInlineBitmapMaxCardinality;
}

DeleteBitmapMetaPtr LocalDeleteBitmap::dump(const MergeTreeMetaBase & storage) const
{
    if (bitmap)
    {
        if (model->cardinality() <= DeleteBitmapMeta::kInlineBitmapMaxCardinality)
        {
            String value;
            value.reserve(model->cardinality() * sizeof(UInt32));
            for (auto it = bitmap->begin(); it != bitmap->end(); ++it)
                PutFixed32(&value, *it);
            model->set_inlined_value(std::move(value));
        }
        else
        {
            bitmap->runOptimize();
            size_t size = bitmap->getSizeInBytes();
            PODArray<char> buf(size);
            size = bitmap->write(buf.data());
            {
                DiskPtr disk = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getAnyDisk();
                String dir_rel_path = fs::path(storage.getRelativeDataPath(IStorage::StorageLocation::MAIN)) / deleteBitmapDirRelativePath(model->partition_id());
                disk->createDirectories(dir_rel_path);

                String file_rel_path = fs::path(storage.getRelativeDataPath(IStorage::StorageLocation::MAIN)) / deleteBitmapFileRelativePath(*model);
                auto out = disk->writeFile(file_rel_path);
                auto * data_out = dynamic_cast<WriteBufferFromHDFS *>(out.get());
                if (!data_out)
                    throw Exception(ErrorCodes::LOGICAL_ERROR, "Writing part to hdfs but write buffer is not hdfs");

                data_out->write(buf.data(), size);
            }
            model->set_file_size(size);
            LOG_TRACE(storage.getLogger(), "Dumped delete bitmap {}", dataModelName(model));
        }
    }
    return std::make_shared<DeleteBitmapMeta>(storage, model);
}

DeleteBitmapMetaPtrVector dumpDeleteBitmaps(const MergeTreeMetaBase & storage, const LocalDeleteBitmaps & temp_bitmaps)
{
    Stopwatch watch;
    DeleteBitmapMetaPtrVector res;
    res.resize(temp_bitmaps.size());

    size_t non_inline_bitmaps = 0;
    for (auto & temp_bitmap : temp_bitmaps)
    {
        non_inline_bitmaps += (!temp_bitmap->canInlineStoreInCatalog());
    }

    const UInt64 max_dumping_threads = storage.getSettings()->cnch_parallel_dumping_threads;
    if (max_dumping_threads == 0 || non_inline_bitmaps <= 4)
    {
        for (size_t i = 0; i < temp_bitmaps.size(); ++i)
            res[i] = temp_bitmaps[i]->dump(storage);
    }
    else
    {
        size_t pool_size = std::min(non_inline_bitmaps, max_dumping_threads);
        ThreadPool pool(pool_size);
        for (size_t i = 0; i < temp_bitmaps.size(); ++i)
        {
            if (temp_bitmaps[i]->canInlineStoreInCatalog())
                res[i] = temp_bitmaps[i]->dump(storage);
            else
            {
                pool.scheduleOrThrowOnError([i, &res, &temp_bitmaps, &storage] {
                    /// can avoid locking because different threads are writing different elements
                    /// 1. different threads are writing different elements
                    /// 2. vector is resized in advance, hence no reallocation
                    res[i] = temp_bitmaps[i]->dump(storage);
                });
            }
        }
        pool.wait(); /// will rethrow exception if task failed
    }

    LOG_TRACE(
        storage.getLogger(),
        "Dumped {} bitmaps ({} not inline) in {} ms",
        temp_bitmaps.size(),
        non_inline_bitmaps,
        watch.elapsedMilliseconds());
    return res;
}

String DeleteBitmapMeta::getNameForLogs() const
{
    return dataModelName(model);
}

void DeleteBitmapMeta::removeFile()
{
    if (model->has_file_size())
    {
        DiskPtr disk = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getAnyDisk();
        String rel_file_path = fs::path(storage.getRelativeDataPath(IStorage::StorageLocation::MAIN)) / deleteBitmapFileRelativePath(*model);
        if (likely(disk->exists(rel_file_path)))
        {
            disk->removeFile(rel_file_path);
            LOG_TRACE(storage.getLogger(), "Removed delete bitmap file {}", rel_file_path);
        }
        else
            LOG_WARNING(storage.getLogger(), "Trying to remove file {}, but it doest exist.", rel_file_path);
    }
}

String DeleteBitmapMeta::getBlockName() const
{
    WriteBufferFromOwnString wb;
    writeString(model->partition_id(), wb);
    writeChar('_', wb);
    writeIntText(model->part_min_block(), wb);
    writeChar('_', wb);
    writeIntText(model->part_max_block(), wb);
    return wb.str();
}

bool DeleteBitmapMeta::operator<(const DeleteBitmapMeta & rhs) const
{
    return std::forward_as_tuple(model->partition_id(), model->part_min_block(), model->part_max_block(), model->commit_time())
        < std::forward_as_tuple(rhs.model->partition_id(), rhs.model->part_min_block(), rhs.model->part_max_block(), rhs.model->commit_time());
}

void deserializeDeleteBitmapInfo(const MergeTreeMetaBase & storage, const DataModelDeleteBitmapPtr & meta, DeleteBitmapPtr & to_bitmap)
{
    assert(meta != nullptr);
    assert(to_bitmap != nullptr);
    auto cardinality = meta->cardinality();
    if (cardinality == 0)
        return;

    if (meta->has_inlined_value())
    {
        const char * data = meta->inlined_value().data();
        for (UInt32 i = 0; i < cardinality; ++i)
        {
            UInt32 value = DecodeFixed32(data);
            data += sizeof(UInt32);
            to_bitmap->add(value);
        }
    }
    else
    {
        assert(meta->has_file_size());
        /// deserialize bitmap from remote storage
        Roaring bitmap;
        {
            PODArray<char> buf(meta->file_size());
            String path
                = fs::path(storage.getFullPathOnDisk(IStorage::StorageLocation::MAIN, storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getAnyDisk())) / deleteBitmapFileRelativePath(*meta);
            ReadBufferFromByteHDFS in(
                path,
                /*pread=*/false,
                storage.getContext()->getHdfsConnectionParams(),
                /*buf_size=*/meta->file_size(),
                /*existing_memory=*/buf.data(),
                /*alignment=*/0,
                /*read_all_once=*/true);
            auto is_eof = in.eof(); /// will trigger reading into buf
            if (is_eof)
                throw Exception(
                    "Unexpected EOF when reading " + path + ",  size=" + toString(meta->file_size()), ErrorCodes::LOGICAL_ERROR);
            bitmap = Roaring::read(buf.data());
            assert(bitmap.cardinality() == cardinality);
        }
        /// union bitmap to result
        *to_bitmap |= bitmap;
    }
}

}
