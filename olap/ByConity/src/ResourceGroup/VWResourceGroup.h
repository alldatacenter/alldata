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

#include <ResourceGroup/IResourceGroup.h>

namespace Poco { class Logger; }

namespace DB
{

class QueryStatus;
class Context;
class CnchTopologyMaster;
using CnchTopologyMasterPtr = std::shared_ptr<CnchTopologyMaster>;

/** Resource group which has root groups for each Virtual Warehouse
  * Child classes (further filters) are currently not yet supported
  */
class VWResourceGroup : public IResourceGroup, protected WithContext
{
public:
    VWResourceGroup(ContextPtr context_);

    ResourceGroupType getType() const override { return DB::ResourceGroupType::VirtualWarehouse; }
    bool canRunMore() const override;
    bool canQueueMore() const override;

    void setSyncExpiry(Int64 expiry) { sync_expiry = expiry; }
    Int64 getSyncExpiry() const { return sync_expiry; }

private:
    Int32 getNumServers() const;

    UInt64 sync_expiry;
    mutable CnchTopologyMasterPtr topology_master;
    mutable std::atomic<bool> logged = false;
    mutable std::atomic<bool> running_limit_debug_logged = false;
    mutable std::atomic<bool> queued_limit_debug_logged = false;
    Poco::Logger * log;
};

}
