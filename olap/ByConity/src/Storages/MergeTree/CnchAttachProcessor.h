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

#include <set>
#include <mutex>
#include <Poco/Logger.h>
#include <Interpreters/Context.h>
#include <Common/ThreadPool.h>
#include <Catalog/Catalog.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/PartitionCommands.h>
#include <Transaction/TxnTimestamp.h>

namespace DB
{

// Define exception point, will throw exception if exception knob is set to certain value.
// For test only
enum class AttachFailurePoint
{
    BEFORE_COLLECT_PARTS = 1 << 0,
    COLLECT_PARTS_FROM_UNIT = 1 << 1,
    PARTS_ASSERT_FAIL = 1 << 2,
    LOAD_PART = 1 << 3,
    DISCOVER_PATH = 1 << 4,
    CHECK_FILTER_RESULT = 1 << 5, // Same as check bucket property failed
    ROWS_ASSERT_FAIL = 1 << 6,
    DETACH_PARTITION_FAIL = 1 << 7,
    PREPARE_WRITE_UNDO_FAIL = 1 << 8,
    MOVE_PART_FAIL = 1 << 9,
    GEN_DELETE_MARK_FAIL = 1 << 10,
    BEFORE_COMMIT_FAIL = 1 << 11,
    MID_COMMIT_FAIL = 1 << 12,
    AFTER_COMMIT_FAIL = 1 << 13
};

// Filter for attach operation, has 3 modes
// 1. filter out single part
// 2. filter out parts within some partition
// 3. use every parts
class AttachFilter
{
public:
    enum Mode
    {
        PART,
        PARTITION,
        PARTS,
    };

    explicit AttachFilter(Mode m = PARTS, const String& obj_id = ""): mode(m), object_id(obj_id) {}

    static AttachFilter createPartFilter(const String& part_name,
        MergeTreeDataFormatVersion ver);
    static AttachFilter createPartitionFilter(const String& partition);
    static AttachFilter createPartsFilter();

    // Filter out if this part should attach
    bool filter(const MergeTreePartInfo& part_info) const;

    void checkFilterResult(const std::vector<MutableMergeTreeDataPartsCNCHVector>& parts_from_sources,
        UInt64 attach_limit) const;

    String toString() const;

    Mode mode;
    String object_id;

private:
    MergeTreePartInfo part_name_info;
};

// Single source of parts collection, will calcVisibleParts for each source.
// For example, a table's detached directory is a single source. For attach
// from path, if it may contains multiple subdirectory, each subdirectory have 
// it's own parts, then each subdirectory will consider as a single source
class CollectSource
{
public:
    struct Unit
    {
        Unit(const DiskPtr& dsk, const String& pth): disk(dsk), rel_path(pth) {}

        DiskPtr disk;
        String rel_path;
    };

    // A source may have multiple collect unit, for example, a table with multinamenode
    // enabled, it may have multiple detached path, one for each namenode, then each
    // detached path is consider as a collect unit
    std::vector<Unit> units;
};

class AttachContext
{
public:
    struct TempResource
    {
        TempResource(): disk(nullptr) {}

        DiskPtr disk;
        std::map<String, String> rename_map;
    };

    AttachContext(const Context& qctx, int pool_expand_thres, int max_thds, Poco::Logger* log):
        query_ctx(qctx), expand_thread_pool_threshold(pool_expand_thres),
        max_worker_threads(max_thds), new_txn(nullptr), logger(log) {}

    void writeRenameRecord(const DiskPtr& disk, const String& from, const String& to);
    // Persist rename map to kv in form of undo-buffer
    void writeRenameMapToKV(Catalog::Catalog & catalog, const String& uuid, const TxnTimestamp& txn_id);

    void commit();
    void rollback();

    // Get worker pool, argument is job number, if job_nums is large enough
    // it may reallocate worker pool
    ThreadPool& getWorkerPool(int job_nums);

    // For attach from other table's active partition, we may start a new transaction
    void setAdditionalTxn(const TransactionCnchPtr& txn)
    {
        new_txn = txn;
    }

private:
    const Context& query_ctx;
    const int expand_thread_pool_threshold;
    const int max_worker_threads;

    TransactionCnchPtr new_txn;

    std::unique_ptr<ThreadPool> worker_pool;

    std::mutex mu;
    /// Temporary resource created during ATTACH, including temp dictionary, file movement records... 
    std::map<String, TempResource> resources;

    Poco::Logger* logger;
};

// Attach will follow such process
// 1. Find all candidate parts which match filter from source(path/table etc)
// 2. Load these parts and calculate visibility
// 3. Generate new block id and mutation id for visible parts
// 4. Move these parts to final position
// 5. Commit transaction
class CnchAttachProcessor
{
public:
    CnchAttachProcessor(StorageCnchMergeTree& tbl, const PartitionCommand& cmd,
        const ContextMutablePtr& ctx):
            failure_injection_knob(ctx->getSettingsRef().attach_failure_injection_knob),
            target_tbl(tbl), from_storage(nullptr),
            is_unique_tbl(tbl.getInMemoryMetadataPtr()->hasUniqueKey()),
            command(cmd), query_ctx(ctx),
            logger(&Poco::Logger::get("CnchAttachProcessor")) {}

    void exec();

private:
    using PartsFromSources = std::vector<MutableMergeTreeDataPartsCNCHVector>;

    static String trimPathPostSlash(const String& path);
    static String relativePathTo(const String& source, const String& target);

    // Collect parts from
    // 1. Other table
    // 2. This table's detached
    // 3. Some path
    // Find matching parts from these sources, construct parts chain and return
    std::pair<AttachFilter, PartsFromSources> collectParts(AttachContext& attach_ctx);
    // Search parts from tbl's detached which match attach filter
    PartsFromSources collectPartsFromTableDetached(const StorageCnchMergeTree& tbl,
        const AttachFilter& filter, AttachContext& attach_ctx);
    // Search parts in path which match attach filter
    PartsFromSources collectPartsFromPath(const String& path, const AttachFilter& filter,
        AttachContext& attach_ctx);
    std::vector<CollectSource> discoverCollectSources(const StorageCnchMergeTree& tbl,
        const DiskPtr& disk, const String& rel_path, int& drill_down_level);
    PartsFromSources collectPartsFromSources(const StorageCnchMergeTree& tbl,
        const std::vector<CollectSource>& sources, const AttachFilter& filter,
        int max_drill_down_level, AttachContext& attach_ctx);
    void collectPartsFromUnit(const StorageCnchMergeTree& tbl,
        const DiskPtr& disk, String& path, int max_drill_down_level,
        const AttachFilter& filter, MutableMergeTreeDataPartsCNCHVector& founded_parts);
    PartsFromSources collectPartsFromActivePartition(StorageCnchMergeTree& tbl,
        AttachContext& attach_ctx);
    std::pair<String, DiskPtr> findBestDiskForHDFSPath(const String& from_path);

    // Rename parts to attach to destination with new part name
    MutableMergeTreeDataPartsCNCHVector prepareParts(const PartsFromSources& parts_from_sources,
        AttachContext& attach_ctx);

    void genPartsDeleteMark(MutableMergeTreeDataPartsCNCHVector& parts_to_write);
    void waitingForDedup(const String& partition_id, const NameSet& staged_parts_name);
    void refreshView();

    void verifyPartsNum(size_t total_parts_num) const;
    inline void injectFailure(AttachFailurePoint point) const;

    UInt64 failure_injection_knob;

    StorageCnchMergeTree& target_tbl;
    StoragePtr from_storage; /// If attach.from_table is not empty
    const bool is_unique_tbl;
    const PartitionCommand& command;
    ContextMutablePtr query_ctx;

    Poco::Logger* logger;
};

}
