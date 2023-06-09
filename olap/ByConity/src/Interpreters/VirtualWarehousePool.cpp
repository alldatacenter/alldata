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

#include <Interpreters/VirtualWarehousePool.h>

#include <Catalog/Catalog.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <ServiceDiscovery/IServiceDiscovery.h>
#include <Common/thread_local_rng.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int VIRTUAL_WAREHOUSE_NOT_FOUND;
    extern const int NO_SUCH_SERVICE;
    extern const int BRPC_CONNECT_ERROR;
    extern const int BRPC_HOST_DOWN;
}

VirtualWarehousePool::VirtualWarehousePool(ContextPtr global_context_)
    : WithContext(global_context_), log(&Poco::Logger::get("VirtualWarehousePool"))
{
}

/// This is helper function to create handle inside pool instead of a lambda function
VirtualWarehouseHandle VirtualWarehousePool::creatorImpl(const String & vw_name)
{
    VirtualWarehouseData vw_data;

    bool ok = tryGetVWFromRM(vw_name, vw_data);
    if (ok)
    {
        /// Couldn't use std::make_shared because of making ctor of VirtualWarehouseHandleImpl private
        return VirtualWarehouseHandle(new VirtualWarehouseHandleImpl(VirtualWarehouseHandleSource::RM, vw_data, getContext()));
    }
    else if (tryGetVWFromServiceDiscovery(vw_name, vw_data))
    {
        return VirtualWarehouseHandle(new VirtualWarehouseHandleImpl(VirtualWarehouseHandleSource::PSM, vw_data, getContext()));
    }
    else
    {
        throw Exception("Virtual warehouse " + vw_name + " not found", ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND);
    }
}

VirtualWarehouseHandle VirtualWarehousePool::get(const String & vw_name)
{
    UInt64 current_ns = clock_gettime_ns(CLOCK_MONOTONIC_COARSE);
    constexpr UInt64 update_interval_ns = 10ULL * 1000 * 1000 * 1000; // 10s TODO: make it configurable
    UInt64 the_last_update_time_ns = last_update_time_ns.load();
    if (current_ns >= the_last_update_time_ns + update_interval_ns
        && last_update_time_ns.compare_exchange_strong(the_last_update_time_ns, current_ns))
    {
        removeOrReplaceOutdatedVW();
    }

    auto [vw_handle, _] = getOrCreate(vw_name, [&] { return creatorImpl(vw_name); });
    return vw_handle;
}

bool VirtualWarehousePool::tryGetVWFromRM(const String & vw_name, VirtualWarehouseData & vw_data)
{
    auto rm_client = getContext()->getResourceManagerClient();
    if (!rm_client)
    {
        LOG_TRACE(log, "ResourceManagerClient is not initialized");
        return false;
    }

    try
    {
        rm_client->getVirtualWarehouse(vw_name, vw_data);
        LOG_TRACE(log, "Get VW {} from ResourceManager", vw_name);
        return true;
    }
    catch (const Exception & e)
    {
        if (e.code() == ErrorCodes::BRPC_HOST_DOWN || e.code() == ErrorCodes::BRPC_CONNECT_ERROR || e.code() == ErrorCodes::NO_SUCH_SERVICE
            || e.code() == ErrorCodes::RESOURCE_MANAGER_NO_LEADER_ELECTED)
        {
            return false;
        }

        throw;
    }
}

bool VirtualWarehousePool::tryGetVWFromServiceDiscovery(const String & vw_name, VirtualWarehouseData & vw_data)
{
    auto sd = getContext()->getServiceDiscoveryClient();
    auto psm = getContext()->getVirtualWarehousePSM();
    auto host_ports_list = sd->lookup(psm, ComponentType::WORKER, vw_name);
    if (host_ports_list.empty())
        return false;

    vw_data.uuid = UUID(UInt128(0)); /// XXX: how about random uuid ?
    vw_data.name = vw_name;
    vw_data.settings.type = RM::VirtualWarehouseType::Default;

    LOG_TRACE(log, "Get VW {} from ServiceDiscovery", vw_name );

    return true;
}

/// TODO: add it to background thread
void VirtualWarehousePool::removeOrReplaceOutdatedVW()
{
    auto copies = this->getAll();

    if (auto rm_client = getContext()->getResourceManagerClient(); rm_client)
    {
        for (auto & [_, vw] : copies)
        {
            try
            {
                VirtualWarehouseData vw_data;
                rm_client->getVirtualWarehouse(vw->getName(), vw_data);
                /// Replace with new vw
                /// 1. The changed uuid means that the vw has been deleted/re-created or renamed.
                /// 2. We think the vw data from RM is more accurate than that from PSM.
                if (vw->getUUID() != vw_data.uuid || vw->getSource() == VirtualWarehouseHandleSource::PSM)
                {
                    LOG_DEBUG(log, "Replacing outdated VW {}", vw->getName());
                    this->set(
                        vw_data.name,
                        VirtualWarehouseHandle(new VirtualWarehouseHandleImpl(VirtualWarehouseHandleSource::RM, vw_data, getContext())));
                }

                /// TODO: Should we update handle if got the newer vw_data ?
            }
            catch (const Exception & e)
            {
                if (e.code() == ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND) /// the vw has been deleted
                {
                    LOG_DEBUG(log, "Removing deleted VW {}", vw->getName());
                    this->erase(vw->getName());
                }
                else
                    LOG_DEBUG(log, "removeOrReplaceOutdatedVW: {}", e.displayText());
            }
        }
    }
    else
    {
        for (auto & [_, vw] : copies)
        {
            if (vw->getSource() == VirtualWarehouseHandleSource::PSM && vw->empty())
            {
                LOG_DEBUG(log, "Removing empty VW (from PSM) {}", vw->getName());
                this->erase(vw->getName());
            }
        }
    }
}

}
