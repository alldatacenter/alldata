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

#include <Common/ThreadPool.h>
#include <Common/Exception.h>
#include <Common/CGroup/CGroupManagerFactory.h>
#include <Common/getNumberOfPhysicalCPUCores.h>
#include <Common/CurrentThread.h>
#include <Interpreters/Context.h>
#include <ResourceGroup/IResourceGroup.h>

#include <cassert>
#include <type_traits>
#include <string_view>

#include <Poco/Util/Application.h>
#include <Poco/Util/LayeredConfiguration.h>

namespace DB
{
    namespace ErrorCodes
    {
        extern const int CANNOT_SCHEDULE_TASK;
        extern const int LOGICAL_ERROR;
    }
}

namespace CurrentMetrics
{
    extern const Metric GlobalThread;
    extern const Metric GlobalThreadActive;
    extern const Metric LocalThread;
    extern const Metric LocalThreadActive;
}


template <typename Thread>
ThreadPoolImpl<Thread>::ThreadPoolImpl(DB::CpuSetPtr cpu_set_)
    : ThreadPoolImpl(getNumberOfPhysicalCPUCores(), std::move(cpu_set_))
{
}


template <typename Thread>
ThreadPoolImpl<Thread>::ThreadPoolImpl(size_t max_threads_, DB::CpuSetPtr cpu_set_)
    : ThreadPoolImpl(max_threads_, max_threads_, max_threads_, true, std::move(cpu_set_))
{
}

template <typename Thread>
ThreadPoolImpl<Thread>::ThreadPoolImpl(size_t max_threads_, size_t max_free_threads_, size_t queue_size_, bool shutdown_on_exception_, DB::CpuSetPtr cpu_set_,  DB::CpuControllerPtr cpu_)
    : max_threads(max_threads_)
    , max_free_threads(max_free_threads_)
    , queue_size(queue_size_)
    , shutdown_on_exception(shutdown_on_exception_)
    , cpu_set(std::move(cpu_set_))
    , cpu(std::move(cpu_))
{
}

template <typename Thread>
void ThreadPoolImpl<Thread>::setMaxThreads(size_t value)
{
    std::lock_guard lock(mutex);
    max_threads = value;
}

template <typename Thread>
size_t ThreadPoolImpl<Thread>::getMaxThreads() const
{
    std::lock_guard lock(mutex);
    return max_threads;
}

template <typename Thread>
void ThreadPoolImpl<Thread>::setMaxFreeThreads(size_t value)
{
    std::lock_guard lock(mutex);
    max_free_threads = value;
}

template <typename Thread>
void ThreadPoolImpl<Thread>::setQueueSize(size_t value)
{
    std::lock_guard lock(mutex);
    queue_size = value;
}

template <typename Thread>
size_t ThreadPoolImpl<Thread>::getMaxQueueSize() const
{
    std::lock_guard lock(mutex);
    return queue_size;
}

template <typename Thread>
template <typename ReturnType>
ReturnType ThreadPoolImpl<Thread>::scheduleImpl(Job job, int priority, std::optional<uint64_t> wait_microseconds)
{
    auto on_error = [&](const std::string & reason)
    {
        if constexpr (std::is_same_v<ReturnType, void>)
        {
            if (first_exception)
            {
                std::exception_ptr exception;
                std::swap(exception, first_exception);
                std::rethrow_exception(exception);
            }
            throw DB::Exception(DB::ErrorCodes::CANNOT_SCHEDULE_TASK,
                "Cannot schedule a task: {} (threads={}, jobs={})", reason,
                threads.size(), scheduled_jobs);
        }
        else
            return false;
    };

    {
        std::unique_lock lock(mutex);

        auto pred = [this] { return !queue_size || scheduled_jobs < queue_size || shutdown; };

        if (wait_microseconds)  /// Check for optional. Condition is true if the optional is set and the value is zero.
        {
            if (!job_finished.wait_for(lock, std::chrono::microseconds(*wait_microseconds), pred))
                return on_error(fmt::format("no free thread (timeout={})", *wait_microseconds));
        }
        else
            job_finished.wait(lock, pred);

        if (shutdown)
            return on_error("shutdown");

        /// We must not to allocate any memory after we emplaced a job in a queue.
        /// Because if an exception would be thrown, we won't notify a thread about job occurrence.

        /// Check if there are enough threads to process job.
        if (threads.size() < std::min(max_threads, scheduled_jobs + 1))
        {
            try
            {
                threads.emplace_front();
            }
            catch (...)
            {
                /// Most likely this is a std::bad_alloc exception
                return on_error("cannot allocate thread slot");
            }

            try
            {
                threads.front() = Thread([this, it = threads.begin()] { worker(it); });

                if constexpr (std::is_same_v<ThreadFromGlobalPool, Thread>)
                {
                    if (cpu_set)
                    {
                        auto tid = threads.front().gettid();
                        cpu_set->addTask(tid);
                        LOG_DEBUG(&Poco::Logger::get("ThreadPool"), "add thread : {}", tid);
                    }
                }
            }
            catch (...)
            {
                if constexpr (std::is_same_v<ThreadFromGlobalPool, Thread>)
                {
                    auto & cgroup_manager = DB::CGroupManagerFactory::instance();
                    if (cgroup_manager.isInit())
                    {
                        DB::CpuSetPtr system_cpu_set = cgroup_manager.getSystemCpuSet();
                        system_cpu_set->addTask(threads.front().gettid());
                        LOG_DEBUG(&Poco::Logger::get("ThreadPool"), "clear thread for exception : {}", threads.front().gettid());
                    }
                }

                threads.pop_front();
                return on_error("cannot allocate thread");
            }
        }

        jobs.emplace(std::move(job), priority);
        ++scheduled_jobs;
        new_job_or_shutdown.notify_one();
    }

    return ReturnType(true);
}

template <typename Thread>
void ThreadPoolImpl<Thread>::scheduleOrThrowOnError(Job job, int priority)
{
    scheduleImpl<void>(std::move(job), priority, std::nullopt);
}

template <typename Thread>
bool ThreadPoolImpl<Thread>::trySchedule(Job job, int priority, uint64_t wait_microseconds) noexcept
{
    return scheduleImpl<bool>(std::move(job), priority, wait_microseconds);
}

template <typename Thread>
void ThreadPoolImpl<Thread>::scheduleOrThrow(Job job, int priority, uint64_t wait_microseconds)
{
    scheduleImpl<void>(std::move(job), priority, wait_microseconds);
}

template <typename Thread>
void ThreadPoolImpl<Thread>::wait()
{
    {
        std::unique_lock lock(mutex);
        /// Signal here just in case.
        /// If threads are waiting on condition variables, but there are some jobs in the queue
        /// then it will prevent us from deadlock.
        new_job_or_shutdown.notify_all();
        job_finished.wait(lock, [this] { return scheduled_jobs == 0; });

        if (first_exception)
        {
            std::exception_ptr exception;
            std::swap(exception, first_exception);
            std::rethrow_exception(exception);
        }
    }
}

template <typename Thread>
ThreadPoolImpl<Thread>::~ThreadPoolImpl()
{
    finalize();
}

template <typename Thread>
void ThreadPoolImpl<Thread>::finalize()
{
    {
        std::unique_lock lock(mutex);
        shutdown = true;
    }

    new_job_or_shutdown.notify_all();

    std::vector<size_t> tids;
    std::stringstream ss;
    ss << "[";
    tids.reserve(threads.size());

    for (auto & thread : threads)
    {
        thread.join();
        if constexpr (std::is_same_v<ThreadFromGlobalPool, Thread>)
        {
            tids.emplace_back(thread.gettid());
            ss << thread.gettid() << ", ";
        }
    }
    ss << "]";


    if (!tids.empty())
    {
        auto & cgroup_manager = DB::CGroupManagerFactory::instance();
        if (cgroup_manager.isInit())
        {
            DB::CpuSetPtr system_cpu_set = cgroup_manager.getSystemCpuSet();
            system_cpu_set->addTasks(tids);
        }
        LOG_DEBUG(&Poco::Logger::get("ThreadPool"), "clear thread for finalize : {}", ss.str());
    }

    threads.clear();
}

template <typename Thread>
size_t ThreadPoolImpl<Thread>::active() const
{
    std::unique_lock lock(mutex);
    return scheduled_jobs;
}

template <typename Thread>
bool ThreadPoolImpl<Thread>::finished() const
{
    std::unique_lock lock(mutex);
    return shutdown;
}

template <typename Thread>
void ThreadPoolImpl<Thread>::worker(typename std::list<Thread>::iterator thread_it)
{
    DENY_ALLOCATIONS_IN_SCOPE;
    CurrentMetrics::Increment metric_all_threads(
        std::is_same_v<Thread, std::thread> ? CurrentMetrics::GlobalThread : CurrentMetrics::LocalThread);

    /// Add cpu.shares
    if (cpu)
    {
        auto tid = DB::SystemUtils::gettid();
        cpu->addTask(tid);
        LOG_DEBUG(&Poco::Logger::get("ThreadPool"), "add thread : {}", tid);
    }

    while (true)
    {
        Job job;
        bool need_shutdown = false;

        {
            std::unique_lock lock(mutex);
            new_job_or_shutdown.wait(lock, [this] { return shutdown || !jobs.empty(); });
            need_shutdown = shutdown;

            if (!jobs.empty())
            {
                /// std::priority_queue does not provide interface for getting non-const reference to an element
                /// to prevent us from modifying its priority. We have to use const_cast to force move semantics on JobWithPriority::job.
                job = std::move(const_cast<Job &>(jobs.top().job));
                jobs.pop();
            }
            else
            {
                /// shutdown is true, simply finish the thread.
                return;
            }
        }

        if (!need_shutdown)
        {
            try
            {
                ALLOW_ALLOCATIONS_IN_SCOPE;
                CurrentMetrics::Increment metric_active_threads(
                    std::is_same_v<Thread, std::thread> ? CurrentMetrics::GlobalThreadActive : CurrentMetrics::LocalThreadActive);

                job();
                /// job should be reset before decrementing scheduled_jobs to
                /// ensure that the Job destroyed before wait() returns.
                job = {};
            }
            catch (...)
            {
                /// job should be reset before decrementing scheduled_jobs to
                /// ensure that the Job destroyed before wait() returns.
                job = {};

                {
                    std::unique_lock lock(mutex);
                    if (!first_exception)
                        first_exception = std::current_exception(); // NOLINT
                    if (shutdown_on_exception)
                        shutdown = true;
                    --scheduled_jobs;
                }

                job_finished.notify_all();
                new_job_or_shutdown.notify_all();
                return;
            }
        }

        {
            std::unique_lock lock(mutex);
            --scheduled_jobs;

            if (threads.size() > scheduled_jobs + max_free_threads)
            {
                if constexpr (std::is_same_v<ThreadFromGlobalPool, Thread>)
                {
                    auto & cgroup_manager = DB::CGroupManagerFactory::instance();
                    if (cgroup_manager.isInit())
                    {
                        DB::CpuSetPtr system_cpu_set = cgroup_manager.getSystemCpuSet();
                        system_cpu_set->addTask(thread_it->gettid());
                        LOG_DEBUG(&Poco::Logger::get("ThreadPool"), "clear thread for max_threads : {}", thread_it->gettid());
                    }
                }

                thread_it->detach();
                threads.erase(thread_it);
                job_finished.notify_all();
                return;
            }
        }

        job_finished.notify_all();
    }
}

FreeThreadPool & ThreadFromGlobalPool::getThreadPool()
{
    if (std::string_view(DB::CurrentThread::getQueryId()).empty())
        return GlobalThreadPool::instance();

    auto query_context = DB::CurrentThread::get().getQueryContext();
    if (!query_context)
        return GlobalThreadPool::instance();

    if (auto * resource_group = query_context->tryGetResourceGroup();
        resource_group == nullptr
        || resource_group->getType() != DB::ResourceGroupType::Internal
        || resource_group->getThreadPool() == nullptr)
        return GlobalThreadPool::instance();
    else
        return *resource_group->getThreadPool();
}


template class ThreadPoolImpl<std::thread>;
template class ThreadPoolImpl<ThreadFromGlobalPool>;

std::unique_ptr<GlobalThreadPool> GlobalThreadPool::the_instance;

void GlobalThreadPool::initialize(size_t max_threads)
{
    if (the_instance)
    {
        throw DB::Exception(DB::ErrorCodes::LOGICAL_ERROR,
            "The global thread pool is initialized twice");
    }

    the_instance.reset(new GlobalThreadPool(max_threads,
        1000 /*max_free_threads*/, 10000 /*max_queue_size*/,
        false /*shutdown_on_exception*/));
}

GlobalThreadPool & GlobalThreadPool::instance()
{
    if (!the_instance)
    {
        // Allow implicit initialization. This is needed for old code that is
        // impractical to redo now, especially Arcadia users and unit tests.
        initialize();
    }

    return *the_instance;
}
