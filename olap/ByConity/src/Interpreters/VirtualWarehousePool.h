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

#include <Interpreters/VirtualWarehouseHandle.h>
#include <Interpreters/WorkerGroupHandle.h>
#include <Interpreters/Context_fwd.h>
#include <Common/ConcurrentMapForCreating.h>

#include <boost/noncopyable.hpp>
#include <common/logger_useful.h>

namespace DB
{
class Context;

/**
 *  VirtualWarehousePool is a pool which will cache the used VW.
 *  So the cached handles might be outdated, the outdated handles will be replaced or removed.
 */
class VirtualWarehousePool : protected ConcurrentMapForCreating<std::string, VirtualWarehouseHandleImpl>, protected WithContext, private boost::noncopyable
{
public:
    VirtualWarehousePool(ContextPtr global_context_);

    VirtualWarehouseHandle get(const String & vw_name);

    using ConcurrentMapForCreating::erase;
    using ConcurrentMapForCreating::size;
    using ConcurrentMapForCreating::getAll;

private:
    VirtualWarehouseHandle creatorImpl(const String & vw_name);

    bool tryGetVWFromRM(const String & vw_name, VirtualWarehouseData & vw_data);
    bool tryGetVWFromCatalog(const String & vw_name, VirtualWarehouseData & vw_data);
    bool tryGetVWFromServiceDiscovery(const String & vw_name, VirtualWarehouseData & vw_data);

    void removeOrReplaceOutdatedVW();

    Poco::Logger * log {};

    std::atomic<UInt64> last_update_time_ns{0};
};

}
