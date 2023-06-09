/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Poco/Notification.h>
#include <Poco/NotificationQueue.h>
#include <Poco/Timestamp.h>
#include <thread>
#include <atomic>
#include <mutex>
#include <condition_variable>
#include <vector>
#include <map>
#include <functional>
#include <boost/noncopyable.hpp>
#include <Common/ZooKeeper/Types.h>
#include <Common/CurrentMetrics.h>
#include <Common/CurrentThread.h>
#include <Common/ThreadPool.h>
#include <Common/CGroup/CGroupManager.h>
#include <Common/CGroup/CGroupManagerFactory.h>


namespace DB
{

class TaskNotification;
class BackgroundSchedulePoolTaskInfo;
class BackgroundSchedulePoolTaskHolder;


/** Executes functions scheduled at a specific point in time.
  * Basically all tasks are added in a queue and precessed by worker threads.
  *
  * The most important difference between this and BackgroundProcessingPool
  *  is that we have the guarantee that the same function is not executed from many workers in the same time.
  *
  * The usage scenario: instead starting a separate thread for each task,
  *  register a task in BackgroundSchedulePool and when you need to run the task,
  *  call schedule or scheduleAfter(duration) method.
  */
class BackgroundSchedulePool
{
public:
    friend class BackgroundSchedulePoolTaskInfo;

    using TaskInfo = BackgroundSchedulePoolTaskInfo;
    using TaskInfoPtr = std::shared_ptr<TaskInfo>;
    using TaskFunc = std::function<void()>;
    using TaskHolder = BackgroundSchedulePoolTaskHolder;
    using DelayedTasks = std::multimap<Poco::Timestamp, TaskInfoPtr>;

    TaskHolder createTask(const std::string & log_name, const TaskFunc & function);

    size_t getNumberOfThreads() const { return size; }

    /// thread_name_ cannot be longer then 13 bytes (2 bytes is reserved for "/D" suffix for delayExecutionThreadFunction())
    BackgroundSchedulePool(size_t size_, CurrentMetrics::Metric tasks_metric_, const char *thread_name_, CpuSetPtr cpu_set_ = nullptr);
    ~BackgroundSchedulePool();

private:
    using Threads = std::vector<ThreadFromGlobalPool>;

    void threadFunction();
    void delayExecutionThreadFunction();

    /// Schedule task for execution after specified delay from now.
    void scheduleDelayedTask(const TaskInfoPtr & task_info, size_t ms, std::lock_guard<std::mutex> & task_schedule_mutex_lock);

    /// Remove task, that was scheduled with delay, from schedule.
    void cancelDelayedTask(const TaskInfoPtr & task_info, std::lock_guard<std::mutex> & task_schedule_mutex_lock);

    /// Number for worker threads.
    const size_t size;
    std::atomic<bool> shutdown {false};
    Threads threads;
    Poco::NotificationQueue queue;

    /// Delayed notifications.

    std::condition_variable wakeup_cond;
    std::mutex delayed_tasks_mutex;
    /// Thread waiting for next delayed task.
    ThreadFromGlobalPool delayed_thread;
    /// Tasks ordered by scheduled time.
    DelayedTasks delayed_tasks;

    /// Thread group used for profiling purposes
    ThreadGroupStatusPtr thread_group;

    CurrentMetrics::Metric tasks_metric;
    std::string thread_name;
    CpuSetPtr cpu_set;

    void attachToThreadGroup();
};


class BackgroundSchedulePoolTaskInfo : public std::enable_shared_from_this<BackgroundSchedulePoolTaskInfo>, private boost::noncopyable
{
public:
    BackgroundSchedulePoolTaskInfo(BackgroundSchedulePool & pool_, const std::string & log_name_, const BackgroundSchedulePool::TaskFunc & function_);

    /// Schedule for execution as soon as possible (if not already scheduled).
    /// If the task was already scheduled with delay, the delay will be ignored.
    bool schedule();

    /// Schedule for execution after specified delay.
    /// If overwrite is set then the task will be re-scheduled (if it was already scheduled, i.e. delayed == true).
    bool scheduleAfter(size_t ms, bool overwrite = true);

    /// Further attempts to schedule become no-op. Will wait till the end of the current execution of the task.
    void deactivate();

    void activate();

    /// Atomically activate task and schedule it for execution.
    bool activateAndSchedule();

    /// get Coordination::WatchCallback needed for notifications from ZooKeeper watches.
    Coordination::WatchCallback getWatchCallback();

    String dumpStatus();

private:
    friend class TaskNotification;
    friend class BackgroundSchedulePool;

    void execute();

    void scheduleImpl(std::lock_guard<std::mutex> & schedule_mutex_lock);

    BackgroundSchedulePool & pool;
    std::string log_name;
    BackgroundSchedulePool::TaskFunc function;

    std::mutex exec_mutex;
    std::mutex schedule_mutex;

    /// Invariants:
    /// * If deactivated is true then scheduled, delayed and executing are all false.
    /// * scheduled and delayed cannot be true at the same time.
    bool deactivated = false;
    bool scheduled = false;
    bool delayed = false;
    bool executing = false;

    /// If the task is scheduled with delay, points to element of delayed_tasks.
    BackgroundSchedulePool::DelayedTasks::iterator iterator;
};

using BackgroundSchedulePoolTaskInfoPtr = std::shared_ptr<BackgroundSchedulePoolTaskInfo>;


class BackgroundSchedulePoolTaskHolder
{
public:
    BackgroundSchedulePoolTaskHolder() = default;
    explicit BackgroundSchedulePoolTaskHolder(const BackgroundSchedulePoolTaskInfoPtr & task_info_) : task_info(task_info_) {}
    BackgroundSchedulePoolTaskHolder(const BackgroundSchedulePoolTaskHolder & other) = delete;
    BackgroundSchedulePoolTaskHolder(BackgroundSchedulePoolTaskHolder && other) noexcept = default;
    BackgroundSchedulePoolTaskHolder & operator=(const BackgroundSchedulePoolTaskHolder & other) noexcept = delete;
    BackgroundSchedulePoolTaskHolder & operator=(BackgroundSchedulePoolTaskHolder && other) noexcept = default;

    ~BackgroundSchedulePoolTaskHolder()
    {
        if (task_info)
            task_info->deactivate();
    }

    operator bool() const { return task_info != nullptr; }

    BackgroundSchedulePoolTaskInfo * operator->() { return task_info.get(); }
    const BackgroundSchedulePoolTaskInfo * operator->() const { return task_info.get(); }

private:
    BackgroundSchedulePoolTaskInfoPtr task_info;
};

}
