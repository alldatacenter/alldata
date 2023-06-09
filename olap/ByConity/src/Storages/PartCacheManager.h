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

#include <Catalog/CatalogUtils.h>
#include <Core/Types.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/CnchPartitionInfo.h>
#include <Common/RWLock.h>
#include <Common/HostWithPorts.h>
#include <Common/CurrentThread.h>
#include <Interpreters/Context_fwd.h>
#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Protos/DataModelHelpers.h>
#include <Storages/CnchPartitionInfo.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Core/BackgroundSchedulePool.h>

namespace DB
{

class CnchDataPartCache;
using CnchDataPartCachePtr = std::shared_ptr<CnchDataPartCache>;

struct TableMetaEntry
{
    RWLockImpl::LockHolder readLock() const
    {
        return meta_mutex->getLock(RWLockImpl::Read, CurrentThread::getQueryId().toString());
    }

    RWLockImpl::LockHolder writeLock() const
    {
        return meta_mutex->getLock(RWLockImpl::Write, CurrentThread::getQueryId().toString());
    }

    TableMetaEntry(const String & database_, const String & table_, const RWLock & lock = nullptr)
        : database(database_), table(table_)
    {
        if (!lock)
            meta_mutex = RWLockImpl::create();
        else
            meta_mutex = lock;
    }

    String database;
    String table;
    /// track the timestamp when last data ingestion or removal happens to this table; initialized with current time
    UInt64 last_update_time {0};
    /// track the metrics change. Because metrics update time is not the same with data update time, so we track them separately.
    UInt64 metrics_last_update_time {0};
    bool is_clustered {true};
    String preallocate_vw;
    mutable RWLock meta_mutex;
    std::atomic_bool partition_metrics_loaded= false;
    std::atomic_bool loading_metrics = false;
    std::atomic_bool load_parts_by_partition = false;
    std::mutex fetch_mutex;
    std::condition_variable fetch_cv;
    /// used to decide if the part/partition cache are still valid when enable write ha. If the fetched
    /// NHUT from metastore differs with cached one, we should update cache with metastore.
    std::atomic_uint64_t cached_non_host_update_ts {0};
    bool need_invalid_cache {false};
    Catalog::PartitionMap partitions;
};

using TableMetaEntryPtr = std::shared_ptr<TableMetaEntry>;

class PartCacheManager: WithMutableContext
{
public:
    using DataPartPtr = std::shared_ptr<const MergeTreeDataPartCNCH>;
    using DataPartsVector = std::vector<DataPartPtr>;

    PartCacheManager(ContextMutablePtr context_);
    ~PartCacheManager();

    void mayUpdateTableMeta(const IStorage & storage);

    void updateTableNameInMetaEntry(const String & table_uuid, const String & database_name, const String & table_name);

    std::vector<TableMetaEntryPtr> getAllActiveTables();

    UInt64 getTableLastUpdateTime(const UUID & uuid);

    bool getTableClusterStatus(const UUID & uuid);

    void setTableClusterStatus(const UUID & uuid, bool clustered);

    void setTablePreallocateVW(const UUID & uuid, String vw);

    String getTablePreallocateVW(const UUID & uuid);

    bool getTablePartitionMetrics(const IStorage & i_storage, std::unordered_map<String, PartitionFullPtr> & partitions, bool require_partition_info = true);

    bool getTablePartitions(const IStorage & storage, Catalog::PartitionMap & partitions);

    bool getPartitionList(const IStorage & storage, std::vector<std::shared_ptr<MergeTreePartition>> & partition_list);

    void invalidPartCache(const UUID & uuid);

    void invalidCacheWithNewTopology(const HostWithPortsVec & servers);

    void invalidPartCacheWithoutLock(const UUID & uuid, std::unique_lock<std::mutex> & lock);

    void invalidPartCache(const UUID & uuid, const DataPartsVector & parts);

    void insertDataPartsIntoCache(const IStorage & table, const pb::RepeatedPtrField<Protos::DataModelPart> & parts_model, const bool is_merged_parts, const bool should_update_metrics);

    /// Get count and weight in Part cache
    std::pair<UInt64, UInt64> dumpPartCache();

    std::unordered_map<String, std::pair<size_t, size_t>> getTableCacheInfo();

    using LoadPartsFunc = std::function<DataModelPartPtrVector(const Strings&, const Strings&)>;

    ServerDataPartsVector getOrSetServerDataPartsInPartitions(
        const IStorage & table, const Strings & partitions, LoadPartsFunc && load_func, const UInt64 & ts);

    void mayUpdateTableMeta(const StoragePtr & table);

    bool trySetCachedNHUTForUpdate(const UUID & uuid, const UInt64 & pts);

    bool checkIfCacheValidWithNHUT(const UUID & uuid, const UInt64 & nhut);

    void reset();

    void shutDown();

    bool couldLeverageCache(const StoragePtr & storage);
private:
    mutable std::mutex cache_mutex;
    CnchDataPartCachePtr part_cache_ptr;
    std::unordered_map<UUID, TableMetaEntryPtr> active_tables;

    /// A cache for the NHUT which has been written to bytekv. Do not need to update NHUT each time when non-host server commit parts
    /// bacause tso has 3 seconds interval. We just cache the latest updated NHUT and only write to metastore if current ts is
    /// different from it.
    std::unordered_map<UUID, UInt64> cached_nhut_for_update {};
    std::mutex cached_nhut_mutex;

    /// We manage the table meta locks here to make sure each table has only one meta lock no matter how many different table meta entry it has.
    /// The lock is cleaned by a background task if it is no longer be used by any table meta entry.
    std::unordered_map<UUID, RWLock> meta_lock_container;

    BackgroundSchedulePool::TaskHolder metrics_updater;   // Used to correct the metrics periodically.
    BackgroundSchedulePool::TaskHolder metrics_initializer;  // Used to collect metrics if it is not ready.
    BackgroundSchedulePool::TaskHolder active_table_loader; // Used to load table when server start up, only execute once;
    BackgroundSchedulePool::TaskHolder meta_lock_cleaner; // remove unused meta lock periodically;

    void updateTablePartitionsMetrics(bool skip_if_already_loaded);
    void reloadPartitionMetrics(const UUID & uuid, const TableMetaEntryPtr & table_meta);
    void cleanMetaLock();
    // load tables belongs to current server according to the topology. The task is performed asynchronously.
    void loadActiveTables();
    TableMetaEntryPtr getTableMeta(const UUID & uuid);

    // we supply two implementation for getting parts. Normally, we just use getPartsInternal. If the table parts number is huge we can
    // fetch parts sequentially for each partition by using getPartsByPartition.
    ServerDataPartsVector getServerPartsInternal(const MergeTreeMetaBase & storage, const TableMetaEntryPtr & meta_ptr,
        const Strings & partitions, const Strings & all_existing_partitions, LoadPartsFunc & load_func, const UInt64 & ts);
    ServerDataPartsVector getServerPartsByPartition(const MergeTreeMetaBase & storage, const TableMetaEntryPtr & meta_ptr,
        const Strings & partitions, const Strings & all_existing_partitions, LoadPartsFunc & load_func, const UInt64 & ts);
    //DataModelPartWrapperVector getPartsModelByPartition(const MergeTreeMetaBase & storage, const TableMetaEntryPtr & meta_ptr,
    //    const Strings & partitions, const Strings & all_existing_partitions, LoadPartsFunc & load_func, const UInt64 & ts);

    Strings getPartitionIDList(const IStorage & storage);

    static void checkTimeLimit(Stopwatch & watch);
};

using PartCacheManagerPtr = std::shared_ptr<PartCacheManager>;

}
