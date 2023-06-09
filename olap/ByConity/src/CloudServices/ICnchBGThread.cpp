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

#include <CloudServices/ICnchBGThread.h>
#include <CloudServices/CnchServerClientPool.h>

#include <Catalog/Catalog.h>
#include <Interpreters/Context.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/Kafka/StorageCnchKafka.h>

namespace DB
{
ICnchBGThread::ICnchBGThread(ContextPtr global_context_, CnchBGThreadType thread_type_, const StorageID & storage_id_)
    : WithContext(global_context_)
    , thread_type(thread_type_)
    , storage_id(storage_id_)
    , catalog(global_context_->getCnchCatalog())
    , log(&Poco::Logger::get(storage_id.getNameForLogs() + "(" + toString(thread_type) + ")"))
    , scheduled_task(global_context_->getSchedulePool().createTask(log->name(), [this] { run(); }))
    , startup_time(time(nullptr))
{
}

ICnchBGThread::~ICnchBGThread()
{
    try
    {
        stop();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void ICnchBGThread::start()
{
    LOG_TRACE(log, "Starting");
    /// FIXME
    preStart();
    scheduled_task->activateAndSchedule();
}

void ICnchBGThread::wakeup()
{
    LOG_DEBUG(log, "Waking up");

    {
        std::lock_guard lock(wakeup_mutex);
        if (!wakeup_finished) /// avoid concurrent call
            return;
        wakeup_finished = false;
    }

    while (true)
    {
        scheduled_task->activateAndSchedule();

        std::unique_lock lock(wakeup_mutex);
        wakeup_cv.wait(lock);
        if (wakeup_finished)
            break;
    }

    LOG_DEBUG(log, "Woke up");
}

void ICnchBGThread::stop()
{
    LOG_TRACE(log, "Stopping");
    scheduled_task->deactivate();
}

void ICnchBGThread::run()
{
    auto now = time(nullptr);
    if (auto wakeup_time = last_wakeup_time.exchange(now, std::memory_order_relaxed))
        last_wakeup_interval.store(now - wakeup_time, std::memory_order_relaxed);
    num_wakeup.fetch_add(1, std::memory_order_relaxed);

    wakeup_called = false;
    if (!wakeup_finished) /// avoid unnecessary locking
    {
        std::lock_guard lock(wakeup_mutex);
        wakeup_called = !wakeup_finished;
    }

    runImpl();

    if (wakeup_called)
    {
        std::lock_guard lock(wakeup_mutex);
        wakeup_called = false;
        wakeup_finished = true;
    }
    wakeup_cv.notify_one();
}


StoragePtr ICnchBGThread::getStorageFromCatalog()
{
    try
    {
        auto res = catalog->getTableByUUID(*getContext(), toString(storage_id.uuid), TxnTimestamp::maxTS(), true /* with delete */);
        failed_storage.store(0, std::memory_order_relaxed);
        return res;
    }
    catch (...)
    {
        failed_storage.fetch_add(1, std::memory_order_relaxed);
        throw;
    }
}

StorageCnchMergeTree & ICnchBGThread::checkAndGetCnchTable(StoragePtr & storage)
{
    if (auto * t = dynamic_cast<StorageCnchMergeTree *>(storage.get()))
        return *t;
    throw Exception("Table " + storage->getStorageID().getNameForLogs() + " is not StorageCnchMergeTree", ErrorCodes::LOGICAL_ERROR);
}

StorageCnchKafka & ICnchBGThread::checkAndGetCnchKafka(StoragePtr & storage)
{
    if (auto * t = dynamic_cast<StorageCnchKafka *>(storage.get()))
        return *t;
    throw Exception("Table " + storage->getStorageID().getNameForLogs() + " is not StorageCnchKafka", ErrorCodes::LOGICAL_ERROR);
}

TxnTimestamp ICnchBGThread::calculateMinActiveTimestamp() const
{
    /// TODO: P3 opt this: use query timestamp with parts set in task

    TxnTimestamp min_active_ts = getContext()->getTimestamp();

    auto server_clients = getContext()->getCnchServerClientPool().getAll();
    for (auto & c : server_clients)
    {
        try
        {
            if (auto ts = c->getMinActiveTimestamp(storage_id))
                min_active_ts = std::min(*ts, min_active_ts);
        }
        catch (...)
        {
            tryLogCurrentException(log);
        }
    }

    return min_active_ts;
}

}
