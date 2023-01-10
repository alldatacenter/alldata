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

#include <CloudServices/DedupWorkerStatus.h>
#include <Core/BackgroundSchedulePool.h>
#include <Core/Types.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <common/logger_useful.h>

namespace DB
{
class Context;
class StorageCloudMergeTree;

class CloudMergeTreeDedupWorker
{
public:
    CloudMergeTreeDedupWorker(StorageCloudMergeTree & storage_);
    ~CloudMergeTreeDedupWorker();

    void start()
    {
        is_stopped = false;
        task->activateAndSchedule();
    }

    void stop()
    {
        is_stopped = true;
        task->deactivate();
        if (!server_host_ports.empty())
            heartbeat_task->deactivate();
    }

    bool isActive() { return !is_stopped; }

    void setServerHostWithPorts(HostWithPorts host_ports);

    HostWithPorts getServerHostWithPorts();

    DedupWorkerStatus getDedupWorkerStatus();

private:
    /**
     *  Due to staged parts, engine can perform dedup tasks asynchronously, but brings unvisable time until staged parts are published.
     *  Therefore, the staged part has a maximum lifttime. When the lifetime of staged parts exceed the threshold engine will block kafka ingestion action.
     *  There are two cases that may cause the situation:
     *  1. The speed of dedup task can not catch up that of kafka ingestion.
     *  2. The interval of dedup tasks is irrational.
     *
     *  For the second case, since one dedup task of more staged parts has a higher performance, it's ideal that make the interval as long as possible. Here is the strategy of dedup task interval scheduler:
     *
     *  |<---------------- staged part max lifetime --------------->|
     *
     *  |<--reserve-->|<---safe---->|<-----------idle-------------->|
     *  t1            t2            t3                              t4
     *
     *  For each valid dedup task, it will recalculate the interval time. t4 is the begining time that launches dedup task.
     *
     *  Before doing dedup task, it will get the minimum timestamp of staged parts(short for mts). The location of mts is divided into four cases:
     *  1. idle area. mts is in t3~t4 which means that the speed of dedup task is much more than that of kafka engine. In this case, task interval increases at a rate of 1.5 times.
     *  2. safe area. mts is in t2~t3 which is the ideal status. In this case, task interval increases at a rate of 1.1 times.
     *  3. reserve ares. mts is in t1~t2 which means that it has a risk to block kafka ingestion. In this case, task interval decreases at a rate of at most 0.2 times.
     *  4. block area. mts is in the left of t1 which means that it has already blocked kafka ingestion. In this case, task interval decreases at a rate of 0.5 times.
     *
     **/
    struct TaskIntervalScheduler
    {
        UInt64 staged_part_max_life_time_ms;
        const double reserve_area_ratio = 0.2;
        const double safe_area_ratio = 0.2;
        UInt64 sleep_time_ms;
        bool has_excep_or_timeout = false;

        TaskIntervalScheduler(UInt64 staged_part_max_life_time_)
            : staged_part_max_life_time_ms(staged_part_max_life_time_), sleep_time_ms(2000)
        {
        }

        UInt64 getScheduleTime() { return has_excep_or_timeout ? 100 : sleep_time_ms; }

        void calNextScheduleTime(TxnTimestamp min_staged_part_timestamp, TxnTimestamp current_timestamp)
        {
            UInt64 mts = min_staged_part_timestamp.toMillisecond();
            UInt64 t1 = current_timestamp.toMillisecond() - staged_part_max_life_time_ms;
            UInt64 t2 = t1 + staged_part_max_life_time_ms * reserve_area_ratio;
            UInt64 t3 = t2 + staged_part_max_life_time_ms * safe_area_ratio;
            double ratio = 1.0;
            if (mts < t1) /// block area
                ratio = 0.5;
            else if (mts < t2) /// reserve area
                ratio = std::max((t2 - mts) / staged_part_max_life_time_ms * reserve_area_ratio, 0.8);
            else if (mts < t3) /// safe area
                ratio = 1.1;
            else /// idle area
                ratio = 1.5;
            LOG_DEBUG(
                &Poco::Logger::get("TaskIntervalScheduler"),
                "min staged part timestamp: {} ms, current timestamp: {} ms, final ratio is: {}, current sleep time: {} ms.",
                mts,
                current_timestamp.toMillisecond(),
                ratio,
                sleep_time_ms);
            sleep_time_ms = std::min(staged_part_max_life_time_ms * (1 - reserve_area_ratio), sleep_time_ms * ratio);
            sleep_time_ms = std::max(100.0, sleep_time_ms * 1.0);
        }
    };

    void heartbeat();
    void detachSelf();

    void run();
    void iterate();

    StorageCloudMergeTree & storage;
    ContextMutablePtr context;
    String log_name;
    Poco::Logger * log;
    BackgroundSchedulePool::TaskHolder task;
    TaskIntervalScheduler interval_scheduler;
    std::atomic<bool> is_stopped{false};

    mutable std::mutex server_mutex;
    HostWithPorts server_host_ports;
    time_t last_heartbeat_time{0};
    BackgroundSchedulePool::TaskHolder heartbeat_task;

    mutable std::mutex status_mutex;
    DedupWorkerStatus status;
};
}
