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

#include <CloudServices/CnchBGThreadCommon.h>
#include <Core/BackgroundSchedulePool.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/StorageID.h>
#include <Storages/IStorage_fwd.h>
#include <common/logger_useful.h>

namespace DB
{
namespace Catalog
{
    class Catalog;
}

class StorageCnchMergeTree;
class StorageCnchKafka;
class TxnTimestamp;

class ICnchBGThread : protected WithContext, private boost::noncopyable
{
public:
    virtual ~ICnchBGThread();

    auto getType() const { return thread_type; }
    auto getName() const { return toString(thread_type); }
    auto & getStorageID() const { return storage_id; }

    void start();
    void wakeup();
    virtual void stop();
    virtual void drop() { }

    bool error() { return failed_storage.load(std::memory_order_relaxed) >= 3; }

    /**
     *  Get storage object from Catalog, can get a storage which represents dropped table.
     *  Throw CATALOG_SERVICE_INTERNAL_ERROR exception if no record found in Catalog, and
     *  set error flag.
     */
    StoragePtr getStorageFromCatalog();

    static StorageCnchMergeTree & checkAndGetCnchTable(StoragePtr & storage);
    static StorageCnchKafka & checkAndGetCnchKafka(StoragePtr & storage);

    /// TODO: REMOVE ME
    CnchBGThreadStatus getThreadStatus()
    {
        /// return (scheduled_task->taskIsActive() && !is_stale) ? CnchBGThreadStatus::Running : CnchBGThreadStatus::Stopped;
        return CnchBGThreadStatus::Running;
    }

    virtual Strings getBestPartitionsForGC(const StoragePtr &) { return {}; }

    virtual void updatePartCache(const String &, Int64) { }

    /// metrics
    auto getStartupTime() const { return startup_time; }
    auto getLastWakeupInterval() const { return last_wakeup_interval.load(std::memory_order_relaxed); }
    auto getLastWakeupTime() const { return last_wakeup_time.load(std::memory_order_relaxed); }
    auto getNumWakeup() const { return num_wakeup.load(std::memory_order_relaxed); }

protected:
    ICnchBGThread(ContextPtr global_context_, CnchBGThreadType thread_type, const StorageID & storage_id);

    virtual void runImpl() = 0;

    bool inWakeup() const { return wakeup_called; }

    TxnTimestamp calculateMinActiveTimestamp() const;

private:
    virtual void preStart() { }
    void run();

protected:
    const CnchBGThreadType thread_type;
    const StorageID storage_id;
    std::shared_ptr<Catalog::Catalog> catalog;
    Poco::Logger * log;
    BackgroundSchedulePool::TaskHolder scheduled_task;

    /// Set to true when the BackgroundThread quit because of another same task already started on other servers. Only for MergeMutateThread.
    bool is_stale{false};

private:
    std::atomic_int failed_storage{false};

    std::mutex wakeup_mutex;
    std::condition_variable wakeup_cv;
    bool wakeup_finished{true};
    bool wakeup_called{false};

    /// metrics
    time_t startup_time;
    std::atomic<time_t> last_wakeup_interval{0};
    std::atomic<time_t> last_wakeup_time{0};
    std::atomic<uint64_t> num_wakeup{0};
};

using CnchBGThreadPtr = std::shared_ptr<ICnchBGThread>;

}
