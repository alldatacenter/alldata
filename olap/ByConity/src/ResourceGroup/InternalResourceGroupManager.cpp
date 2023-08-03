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

#include <ResourceGroup/IResourceGroupManager.h>
#include <ResourceGroup/InternalResourceGroupManager.h>
#include <ResourceGroup/InternalResourceGroup.h>
#include <Interpreters/Context.h>
#include <Interpreters/Context_fwd.h>
#include <Parsers/ASTAlterQuery.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTDropQuery.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTRenameQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int RESOURCE_GROUP_ILLEGAL_CONFIG;
    extern const int RESOURCE_GROUP_MISMATCH;
}

void InternalResourceGroupManager::initialize(const Poco::Util::AbstractConfiguration &config)
{
    LOG_DEBUG(&Poco::Logger::get("ResourceGroupManager"), "Load resource group manager");
    if (!root_groups.empty())
    {
        LOG_WARNING(&Poco::Logger::get("ResourceGroupManager"), "need to restart to reload config.");
        return;
    }
    Poco::Util::AbstractConfiguration::Keys config_keys;
    String prefix = "resource_groups";
    config.keys(prefix, config_keys);
    String prefixWithKey;
    /// load resource groups
    for (const String & key : config_keys)
    {
        prefixWithKey = prefix + "." + key;
        if (key.find("resource_group") == 0)
        {
            if (!config.has(prefixWithKey + ".name"))
                throw Exception("Resource group has no name", ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);

            String name = config.getString(prefixWithKey + ".name");
            LOG_DEBUG(&Poco::Logger::get("ResourceGroupManager"), "Found resource group {}", name);
            if (groups.find(name) != groups.end())
                throw Exception("Resource group name duplicated: " + name, ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);

            auto pr = groups.emplace(std::piecewise_construct,
                                     std::forward_as_tuple(name),
                                     std::forward_as_tuple(std::make_shared<InternalResourceGroup>()));
            auto group = pr.first->second;
            group->setName(name);
            group->setSoftMaxMemoryUsage(config.getInt64(prefixWithKey + ".soft_max_memory_usage", 0));
            group->setMinQueryMemoryUsage(config.getInt64(prefixWithKey + ".min_query_memory_usage", 536870912)); /// default 512MB
            group->setMaxConcurrentQueries(
                    config.getInt(prefixWithKey + ".max_concurrent_queries", 0)); /// default 0
            group->setMaxQueued(config.getInt(prefixWithKey + ".max_queued", 0)); /// default 0
            group->setMaxQueuedWaitingMs(config.getInt(prefixWithKey + ".max_queued_waiting_ms", 5000)); /// default 5s
            group->setPriority(config.getInt(prefixWithKey + ".priority", 0));
            if (config.has(prefixWithKey + ".parent_resource_group"))
            {
                String parent_name = config.getString(prefixWithKey + ".parent_resource_group");
                group->setParentName(parent_name);
            }
            else
            {
                /// root groups
                group->setRoot();
                root_groups[name] = group.get();
            }
        }
    }
    /// Set parents
    for (auto & p : groups)
    {
        if (!p.second->getParentName().empty())
        {
            auto parent_it = groups.find(p.second->getParentName());
            if (parent_it == groups.end())
                throw Exception("Resource group's parent group not found: " + p.second->getName() + " -> " +  p.second->getParentName(), ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);
            p.second->setParent(parent_it->second.get());
        }
    }
    /// load cases
    for (const String & key : config_keys)
    {
        prefixWithKey = prefix + "." + key;
        if (key.find("case") == 0)
        {
            ResourceSelectCase select_case;
            LOG_DEBUG(&Poco::Logger::get("ResourceGroupManager"), "Found resource group case {}", key);
            if (!config.has(prefixWithKey + ".resource_group"))
                throw Exception("Select case " + key + " does not config resource group", ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);
            select_case.name = key;
            String resourceGroup = config.getString(prefixWithKey + ".resource_group");
            auto group_it= groups.find(resourceGroup);
            if (group_it == groups.end())
                throw Exception("Select case's group not found: " + key + " -> " + resourceGroup, ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);
            else if (!group_it->second->isLeaf())
                throw Exception("Select case's group is not leaf group: " + key + " -> " + resourceGroup, ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);
            select_case.group = group_it->second.get();

            if (config.has(prefixWithKey + ".user"))
                select_case.user = std::make_shared<std::regex>(config.getString(prefixWithKey + ".user"));
            if (config.has(prefixWithKey + ".query_id"))
                select_case.query_id = std::make_shared<std::regex>(config.getString(prefixWithKey + ".query_id"));
            if (config.has(prefixWithKey + ".query_type"))
            {
                String queryType = config.getString(prefixWithKey + ".query_type");
                select_case.query_type = ResourceSelectCase::translateQueryType(queryType);
                if (select_case.query_type == nullptr)
                    throw Exception("Select case's query type is illegal: " + key + " -> " + queryType, ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);
            }
            select_cases[select_case.name] = std::move(select_case);
        }
    }
    LOG_DEBUG(&Poco::Logger::get("ResourceGroupManager"), "Found {} resource groups, {} select cases.",
              groups.size(), select_cases.size());

    bool old_val = false;
    if (!root_groups.empty()
        && started.compare_exchange_strong(old_val, true, std::memory_order_seq_cst, std::memory_order_relaxed))
    {
        resource_task = std::make_unique<ResourceTask>(this);
        timer.scheduleAtFixedRate(resource_task.get(), 1, 1);
    }
}

IResourceGroup * InternalResourceGroupManager::selectGroup(const Context & query_context, const IAST * ast)
{
    const ClientInfo & client_info = query_context.getClientInfo();
    for (const auto & [_, select_case] : select_cases)
    {
        if ((select_case.user == nullptr || std::regex_match(client_info.initial_user, *(select_case.user)))
            && (select_case.query_id == nullptr || std::regex_match(client_info.initial_query_id, *(select_case.query_id)))
            && (select_case.query_type == nullptr || *select_case.query_type == ResourceSelectCase::getQueryType(ast)))
            return select_case.group;
    }

    switch (query_context.getSettingsRef().resource_group_unmatched_behavior)
    {
        case 0:
            return nullptr;
        case 1:
            throw Exception("Match no existing resource group", ErrorCodes::RESOURCE_GROUP_MISMATCH);
        case 2:
            if (!root_groups.empty())
                return root_groups.begin()->second;
            else
                throw Exception("Match no existing resource group", ErrorCodes::RESOURCE_GROUP_MISMATCH);
        default:
            throw Exception("Invalid resource_group_unmatched_behavior value", ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);
    }
}

}
