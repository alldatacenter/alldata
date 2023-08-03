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

#include "IDiskCache.h"

#include <random>
#include <Interpreters/Context.h>
#include <Common/Stopwatch.h>
#include <common/logger_useful.h>

namespace ProfileEvents
{
extern const Event DiskCacheScheduleCacheTaskMicroSeconds;
}

namespace DB
{
IDiskCache::IDiskCache(Context & context_, VolumePtr volume_, const DiskCacheSettings & settings_)
    : context(context_)
    , volume(std::move(volume_))
    , settings(settings_)
    , disk_cache_throttler(context.getDiskCacheThrottler())
{
}

void IDiskCache::asyncLoad()
{
    sync_task = context.getSchedulePool().createTask("DiskCacheMetaSync", [this] { load(); });
    sync_task->activateAndSchedule();
}

void IDiskCache::shutdown()
{
    shutdown_called = true;
    if (sync_task)
        sync_task->deactivate();
}

void IDiskCache::cacheSegmentsToLocalDisk(IDiskCacheSegmentsVector hit_segments)
{
    if (hit_segments.empty())
        return;

    Stopwatch watch;
    SCOPE_EXIT({ ProfileEvents::increment(ProfileEvents::DiskCacheScheduleCacheTaskMicroSeconds, watch.elapsedMicroseconds()); });

    // Notes: split to more tasks?
    scheduleCacheTask([this, segments = std::move(hit_segments)] {
        for (const auto & hit_segment : segments)
        {
            try
            {
                auto [disk, path] = get(hit_segment->getSegmentName());
                if (disk == nullptr)
                {
                    hit_segment->cacheToDisk(*this);
                }
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
            }
        }
    });
}

// Schedule cache task, when threadpool's current running task exceed certain ratio, start random
// drop disk cache task
bool IDiskCache::scheduleCacheTask(const std::function<void()> & task)
{
    if (shutdown_called)
        return false;

    auto & thread_pool = context.getLocalDiskCacheThreadPool();
    size_t active_task_size = thread_pool.active();
    size_t max_queue_size = thread_pool.getMaxQueueSize();
    // (Running + Pending tasks) / (Max Running + Max Pending tasks)
    size_t current_ratio = max_queue_size == 0 ? 0 : ((active_task_size * 100) / max_queue_size);

    if (current_ratio <= settings.random_drop_threshold || settings.random_drop_threshold >= 100)
    {
        return thread_pool.trySchedule(task);
    }
    else
    {
        // Drop disk cache task base on queue's full ratio
        // (current task queue full ratio/ (100 - random_drop_threshold)) * 100
        // The drop possibility when current_ratio == random_drop_threshold is 0%
        // The drop possibility when current_ratio == 100 is 100%
        size_t drop_possibility = (100 * (current_ratio - settings.random_drop_threshold)) / (100 - settings.random_drop_threshold);
        std::random_device rd;
        std::mt19937 random_generator(rd());
        std::uniform_int_distribution<size_t> dist(1, 100);
        if (dist(random_generator) <= drop_possibility)
        {
            LOG_DEBUG(log, "Drop disk cache since queue is almost full, Queue length: {}, Max: {}", active_task_size, max_queue_size);
            return false;
        }
        else
        {
            return thread_pool.trySchedule(task);
        }
    }
}

}
