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

#include <ResourceGroup/InternalResourceGroup.h>
#include <ResourceGroup/IResourceGroup.h>

#include <chrono>

#include <Core/Settings.h>
#include <Interpreters/Context.h>
#include <Interpreters/ProcessList.h>
#include <Common/CGroup/CGroupManagerFactory.h>
#include <Common/Exception.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int RESOURCE_NOT_ENOUGH;
    extern const int WAIT_FOR_RESOURCE_TIMEOUT;
    extern const int RESOURCE_GROUP_INTERNAL_ERROR;
}

bool InternalResourceGroup::canRunMore() const
{
    return (max_concurrent_queries == 0 || static_cast<Int32>(running_queries.size()) + descendent_running_queries < max_concurrent_queries)
        && (soft_max_memory_usage == 0 || cached_memory_usage_bytes < soft_max_memory_usage);
}

bool InternalResourceGroup::canQueueMore() const
{
    return static_cast<Int32>(queued_queries.size()) + descendent_queued_queries < max_queued;
}

void InternalResourceGroup::initCpu()
{
    if (cpu_shares == 0)
        return;

    CGroupManager & cgroup_manager = CGroupManagerFactory::instance();
    cpu = cgroup_manager.createCpu(name, cpu_shares);
    if (!cpu)
        return;
    thread_pool = std::make_shared<FreeThreadPool>(10000, 500, 10000, true, nullptr, cpu);
}

}
