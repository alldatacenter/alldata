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
#include <CloudServices/ICnchBGThread.h>

#include <Core/UUID.h>
#include <Interpreters/Context_fwd.h>
#include <Common/ConcurrentMapForCreating.h>

#include <mutex>
#include <unordered_map>

namespace DB
{
using UUIDToBGThreads = std::unordered_map<UUID, CnchBGThreadPtr>;
namespace ResourceManagement
{
    class ResourceReporterTask;
}
using ResourceReporterTask = ResourceManagement::ResourceReporterTask;

class CnchBGThreadsMap : protected ConcurrentMapForCreating<UUID, ICnchBGThread>, private WithContext, private boost::noncopyable
{
public:
    CnchBGThreadsMap(ContextPtr global_context_, CnchBGThreadType t);

    using ConcurrentMapForCreating::getAll;
    using ConcurrentMapForCreating::size;

    CnchBGThreadPtr getThread(const StorageID & storage_id) const;
    CnchBGThreadPtr tryGetThread(const StorageID & storage_id) const { return tryGet(storage_id.uuid); }

    void controlThread(const StorageID & storage_id, CnchBGThreadAction action);
    /**
     *  Create a background thread on server, and active it.
     */
    CnchBGThreadPtr startThread(const StorageID & storage_id);
    /**
     *  Stop the background thread but not remove it.
     *  For the case that table is not dropped, but need to disable its function temporarily.
     */
    void stopThread(const StorageID & storage_id) const;
    /**
     *  Remove the background thread from server. Won't throw exception if thread not found.
     */
    void tryRemoveThread(const StorageID & storage_id);

    /**
     *  Remove the background thread from server and drop related data if necessary
     */
    void tryDropThread(const StorageID & storage_id);

    void wakeupThread(const StorageID & storage_id);

    std::map<StorageID, CnchBGThreadStatus> getStatusMap() const;

    void stopAll();

    void cleanup();

private:
    CnchBGThreadPtr getOrCreateThread(const StorageID & storage_id);

    CnchBGThreadPtr createThread(const StorageID & storage_id);

private:
    CnchBGThreadType type;
};

class CnchBGThreadsMapArray : protected WithContext, private boost::noncopyable
{
public:
    CnchBGThreadsMapArray(ContextPtr global_context_);
    ~CnchBGThreadsMapArray();

    void destroy();

    inline CnchBGThreadsMap * at(size_t type)
    {
        auto res = threads_array[size_t(type)].get();
        if (unlikely(!res))
            throw Exception(ErrorCodes::LOGICAL_ERROR, "CnchBGThread for type {} is not initialized", toString(CnchBGThreadType(type)));
        return res;
    }

    void cleanThread();

    void startResourceReport();
    void stopResourceReport();

private:
    std::array<std::unique_ptr<CnchBGThreadsMap>, CnchBGThread::NumType> threads_array;

    std::unique_ptr<ResourceReporterTask> resource_reporter_task;

    /// std::atomic_bool shutdown{false};
    /// ThreadFromGlobalPool cleaner;
    BackgroundSchedulePool::TaskHolder cleaner;
};


}
