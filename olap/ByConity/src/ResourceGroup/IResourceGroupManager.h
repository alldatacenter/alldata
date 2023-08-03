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

#include <Common/RWLock.h>
#include <Interpreters/Context.h>
#include <Interpreters/Context_fwd.h>
#include <ResourceGroup/IResourceGroup.h>
#include <Parsers/IAST.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <Poco/Util/Timer.h>

#include <atomic>
#include <regex>
#include <unordered_set>
#include <unordered_map>
#include <vector>

namespace DB
{
using ResourceGroupPtr = std::shared_ptr<IResourceGroup>;
using ResourceGroupInfoMap = std::unordered_map<String, ResourceGroupInfo>;
using ResourceGroupInfoVec = std::vector<ResourceGroupInfo>;

struct ResourceSelectCase
{
    enum QueryType
    {
        DDL,
        DATA,
        SELECT,
        OTHER
    };

    static std::shared_ptr<QueryType> translateQueryType(const String & queryType);
    static QueryType getQueryType(const IAST *ast);
    using Element = std::shared_ptr<std::regex>;
    String name;
    Element user;
    Element query_id;
    std::shared_ptr<QueryType> query_type;
    IResourceGroup * group;
};

class IResourceGroupManager
{
protected:
    using Container = std::unordered_map<String, ResourceGroupPtr>;

    class ResourceTask : public Poco::Util::TimerTask
    {
    public:
        ResourceTask(IResourceGroupManager * manager_) : manager(manager_) {}

        virtual void run() override
        {
            for (auto [_, root] : manager->root_groups)
            {
                root->processQueuedQueues();
            }
        }
    private:
        IResourceGroupManager * manager;
    };

    RWLockImpl::LockHolder getReadLock() const
    {
        return mutex->getLock(RWLockImpl::Read, RWLockImpl::NO_QUERY);
    }
    RWLockImpl::LockHolder getWriteLock() const
    {
        return mutex->getLock(RWLockImpl::Write, RWLockImpl::NO_QUERY);
    }

public:
    virtual ~IResourceGroupManager();
    virtual void initialize(const Poco::Util::AbstractConfiguration & config) = 0;
    virtual IResourceGroup * selectGroup(const Context & query_context, const IAST * ast) = 0;
    virtual void shutdown() = 0;

    void enable();
    void disable();
    bool isInUse() const
    {
        return !disabled.load(std::memory_order_relaxed) && started.load(std::memory_order_relaxed);
    }

    IResourceGroupManager::Container getGroups() const;
    ResourceGroupInfoVec getInfoVec() const;
    ResourceGroupInfoMap getInfoMap() const;
    bool getInfo(const String & group, ResourceGroupInfo & ret_info) const;

protected:
    mutable RWLock mutex = RWLockImpl::create();
    mutable std::unordered_map<String, IResourceGroup*> root_groups;
    mutable Container groups;
    mutable std::unordered_map<String, ResourceSelectCase> select_cases;
    std::atomic<bool> started{false};
    std::atomic<bool> disabled{false};
    Poco::Util::Timer timer;
};

using ResourceGroupManagerPtr = std::shared_ptr<IResourceGroupManager>;

}
