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

#include <MergeTreeCommon/GlobalGCManager.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/Kafka/StorageCnchKafka.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Protos/RPCHelpers.h>
#include <Common/Status.h>
#include <Catalog/Catalog.h>


namespace DB
{
GlobalGCManager::GlobalGCManager(
    ContextMutablePtr global_context_,
    size_t default_max_threads,
    size_t default_max_free_threads,
    size_t default_max_queue_size)
    : WithContext(global_context_), log(&Poco::Logger::get("GlobalGCManager"))
{
    const auto & config_ref = getContext()->getConfigRef();
    this->max_threads =
        config_ref.getUInt("global_gc.threadpool_max_size", default_max_threads);
    const size_t max_free_threads =
        config_ref.getUInt("global_gc.threadpool_max_free_threads", default_max_free_threads);
    const size_t queue_size =
        config_ref.getUInt("global_gc.threadpool_max_queue_size", default_max_queue_size);

    LOG_DEBUG(log, "init thread pool with max_threads: {} max_free_threads: {} queue_size: {}",
        max_threads, max_free_threads, queue_size);
    if (max_threads > 0)
        threadpool = std::make_unique<ThreadPool>(max_threads, max_free_threads, queue_size);
}

size_t GlobalGCManager::getNumberOfDeletingTables() const
{
    std::lock_guard<std::mutex> lock(mutex);
    return deleting_uuids.size();
}

std::set<UUID> GlobalGCManager::getDeletingUUIDs() const
{
    std::lock_guard<std::mutex> lock(mutex);
    return deleting_uuids;
}

namespace GlobalGCHelpers
{
size_t calculateApproximateWorkLimit(size_t max_threads)
{
    return GlobalGCManager::MAX_BATCH_WORK_SIZE * max_threads * 2;
}

bool canReceiveMoreWork(size_t max_threads, size_t deleting_table_num, size_t num_of_new_tables)
{
    size_t approximate_work_limit = calculateApproximateWorkLimit(max_threads);
    return (deleting_table_num < approximate_work_limit) &&
        ((deleting_table_num + num_of_new_tables) < (approximate_work_limit + GlobalGCManager::MAX_BATCH_WORK_SIZE));
}

size_t amountOfWorkCanReceive(size_t max_threads, size_t deleting_table_num)
{
    size_t approximate_work_limit = calculateApproximateWorkLimit(max_threads);
    if (deleting_table_num < approximate_work_limit)
    {
        size_t batch_num = ((approximate_work_limit + GlobalGCManager::MAX_BATCH_WORK_SIZE) - deleting_table_num - 1) / GlobalGCManager::MAX_BATCH_WORK_SIZE;
        return batch_num * GlobalGCManager::MAX_BATCH_WORK_SIZE;
    }
    return 0;
}

namespace {

void cleanDisks(const Disks & disks, const String & relative_path, Poco::Logger * log)
{
    for (const DiskPtr & disk : disks)
    {
        if (disk->exists(relative_path))
        {
            disk->removeRecursive(relative_path);
            LOG_TRACE(log, "Removed relative path {} of disk type {}, root path {}!",
                relative_path, DiskType::toString(disk->getType()), disk->getPath());
        }
        else
            LOG_WARNING(log, "Relative path {} of disk type {}, root path {} doesn't exist!",
                relative_path, DiskType::toString(disk->getType()), disk->getPath());
    }
}

void dropBGStatusesInCatalogForCnchMergeTree(UUID uuid, Catalog::Catalog * catalog)
{
    catalog->dropBGJobStatus(uuid, CnchBGThreadType::Clustering);
    catalog->dropBGJobStatus(uuid, CnchBGThreadType::MergeMutate);
    catalog->dropBGJobStatus(uuid, CnchBGThreadType::PartGC);
    catalog->dropBGJobStatus(uuid, CnchBGThreadType::DedupWorker);
}

void dropBGStatusInCatalogForCnchKafka(UUID uuid, Catalog::Catalog * catalog)
{
    catalog->dropBGJobStatus(uuid, CnchBGThreadType::Consumer);
}

} /// end anonymous namespace

bool executeGlobalGC(const Protos::DataModelTable & table, const Context & context, Poco::Logger * log)
{
    auto storage_id = StorageID{table.database(), table.name(), RPCHelpers::createUUID(table.uuid())};

    if (!Status::isDeleted(table.status()))
    {
        LOG_ERROR(log, "Table {} already in trash, but status is not deleted", storage_id.getNameForLogs());
        return false;
    }

    LOG_INFO(log, "Table: {} is deleted, will execute GlobalGC for it", storage_id.getNameForLogs());

    try
    {
        auto catalog = context.getCnchCatalog();

        /// delete data directory of the table from hdfs
        auto storage = catalog->tryGetTableByUUID(context, UUIDHelpers::UUIDToString(storage_id.uuid), TxnTimestamp::maxTS(), true);
        if (!storage)
        {
            LOG_INFO(log, "Fail to get table by UUID, table probably already got deleted");
            return true;
        }

        StorageCnchMergeTree * mergetree = dynamic_cast<StorageCnchMergeTree*>(storage.get());
        if (mergetree)
        {
            LOG_DEBUG(log, "Remove data path for table {}", storage_id.getNameForLogs());
            StoragePolicyPtr remote_storage_policy = mergetree->getStoragePolicy(IStorage::StorageLocation::MAIN);
            Disks remote_disks = remote_storage_policy->getDisks();
            const String & relative_path = mergetree->getRelativeDataPath(IStorage::StorageLocation::MAIN);
            cleanDisks(remote_disks, relative_path, log);

            // StoragePolicyPtr local_storage_policy = mergetree->getLocalStoragePolicy();
            // Disks local_disks = local_storage_policy->getDisks();
            // //const String local_store_path = mergetree->getLocalStorePath();
            // cleanDisks(local_disks, relative_path, log);

            LOG_DEBUG(log, "Remove background job statues for table {}", storage_id.getNameForLogs());
            dropBGStatusesInCatalogForCnchMergeTree(storage_id.uuid, catalog.get());
        }

        if (StorageCnchKafka * kafka_storage = dynamic_cast<StorageCnchKafka *>(storage.get()))
            dropBGStatusInCatalogForCnchKafka(storage_id.uuid, catalog.get());

        /// delete metadata of data parts
        LOG_DEBUG(log, "Remove data parts meta for table {}", storage_id.getNameForLogs());
        catalog->clearDataPartsMetaForTable(storage);

        /// TODO delete bitmaps is not support;
#if 0
        auto all_delete_bitmaps = catalog->getAllDeleteBitmaps(storage, TxnTimestamp::maxTS());
        for (auto & delete_bitmap : all_delete_bitmaps)
            delete_bitmap->removeFile();
#endif

        /// delete table's metadata
        LOG_DEBUG(log, "Remove table meta for table {}", storage_id.getNameForLogs());
        catalog->clearTableMetaForGC(storage_id.database_name, storage_id.table_name, table.commit_time());
    }
    catch (...)
    {
        LOG_ERROR(log, "Failed to remove meta data for table {}", storage_id.getNameForLogs());
        tryLogCurrentException(log);
    }

    return true;
}

std::vector<UUID> getUUIDsFromTables(const std::vector<Protos::DataModelTable> & tables)
{
    std::vector<UUID> ret;
    std::transform(tables.begin(), tables.end(),
        std::back_inserter(ret),
        [] (const Protos::DataModelTable & table)
        {
            return RPCHelpers::createUUID(table.uuid());
        }
    );
    return ret;
}

std::vector<Protos::DataModelTable> removeDuplication(
    const std::set<UUID> & deleting_uuids,
    std::vector<Protos::DataModelTable> tables
)
{
    std::set<UUID> added_uuids;
    tables.erase(
        std::remove_if(tables.begin(), tables.end(),
            [& deleting_uuids, & added_uuids] (const Protos::DataModelTable & table)
            {
                UUID uuid = RPCHelpers::createUUID(table.uuid());
                auto ret = added_uuids.insert(uuid);
                return (!ret.second) ||
                        (deleting_uuids.find(uuid) != deleting_uuids.end());
            }
        ), tables.end());
    return tables;
}

}// end namespace GlobalGCHelpers

GlobalGCManager::GlobalGCTask::GlobalGCTask(
    std::vector<Protos::DataModelTable> tables_,
    GlobalGCManager & manager_
) : tables(std::move(tables_)), manager(manager_)
{}

void GlobalGCManager::GlobalGCTask::operator()()
{
    for (const auto & table : tables)
    {
        if (manager.isShutdown())
            return;
        try
        {
            manager.executor(table, *manager.getContext(), manager.log);
        }
        catch(...)
        {
            LOG_WARNING(manager.log, "got exception while remove table {}.{}",
                table.database(), table.name());
            tryLogCurrentException(manager.log);
        }
        manager.removeDeletingUUID(RPCHelpers::createUUID(table.uuid()));
    }
}

bool GlobalGCManager::scheduleImpl(std::vector<Protos::DataModelTable> && tables)
{
    const uint64_t wait_microseconds = 500000; /// 0.5 s
    std::vector<UUID> uuids = GlobalGCHelpers::getUUIDsFromTables(tables);
    bool ret = true;
    GlobalGCTask gc_task{std::move(tables), *this};

    {
        std::lock_guard<std::mutex> lock(mutex);
        deleting_uuids.insert(uuids.begin(), uuids.end());
    }

    try
    {
        threadpool->scheduleOrThrow(std::move(gc_task), 0, wait_microseconds);
    }
    catch (const std::exception & e)
    {
        LOG_ERROR(log, "Fail to schedule, got exception: {}", e.what());
        ret = false;
    }

    if (!ret)
    {
        std::lock_guard<std::mutex> lock(mutex);
        std::for_each(uuids.begin(), uuids.end(),
            [this] (const auto & uuid)
            {
                deleting_uuids.erase(uuid);
            }
        );
        return false;
    }

    return true;
}

bool GlobalGCManager::schedule(std::vector<Protos::DataModelTable> tables)
{
    if (!threadpool)
        return false;

    bool is_shutdown_cp = false;
    {
        std::lock_guard<std::mutex> lock(mutex);
        is_shutdown_cp = is_shutdown;
    }

    if (is_shutdown_cp)
    {
        LOG_WARNING(log, "Can't receive work while shutting down");
        return false;
    }

    std::set<UUID> deleting_uuids_clone;
    {
        std::lock_guard<std::mutex> lock(mutex);
        deleting_uuids_clone = deleting_uuids;
    }

    tables = GlobalGCHelpers::removeDuplication(deleting_uuids_clone, std::move(tables));
    if (!GlobalGCHelpers::canReceiveMoreWork(max_threads, deleting_uuids_clone.size(), tables.size()))
    {
        LOG_WARNING(log, "Fail to schedule because too much work to do,"
            " misconfiguration between DM and servers, num of deleting table"
            " {}, number of new add table {}", deleting_uuids_clone.size(), tables.size());
        return false;
    }

    std::vector<Protos::DataModelTable> tables_bucket;
    for (auto && table : tables)
    {
        tables_bucket.push_back(std::move(table));
        if (tables_bucket.size() >= MAX_BATCH_WORK_SIZE)
        {
            if (!scheduleImpl(std::move(tables_bucket)))
            {
                LOG_WARNING(log, "Failed to scheduleImpl, probably because full queue, queue size: {}"
                    , threadpool->active());
                return false;
            }
        }
    }

    if ((!tables_bucket.empty()) &&
        (!scheduleImpl(std::move(tables_bucket)))
    )
    {
        LOG_WARNING(log, "Failed to scheduleImpl, probably because full queue, number of active job: {}"
            , threadpool->active());
        return false;
    }
    return true;
}

bool GlobalGCManager::isShutdown() const
{
    std::lock_guard<std::mutex> lock(mutex);
    return is_shutdown;
}

void GlobalGCManager::removeDeletingUUID(UUID uuid)
{
    std::lock_guard<std::mutex> lock(mutex);
    deleting_uuids.erase(uuid);
}

void GlobalGCManager::shutdown()
{
    std::lock_guard<std::mutex> lock(mutex);
    is_shutdown = true;
}

GlobalGCManager::~GlobalGCManager()
{
    shutdown();
}

} /// end namespace
