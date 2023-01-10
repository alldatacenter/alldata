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

#include <Common/Exception.h>
#include <common/logger_useful.h>
// #include <Parsers/ASTCreateMaskingPolicyQuery.h>
#include <Interpreters/Context.h>
#include <ResourceGroup/IResourceGroupManager.h>
#include <Parsers/ASTAlterQuery.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTDropQuery.h>
#include <Parsers/ASTRenameQuery.h>
// #include <Parsers/ASTDeleteQuery.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/ASTSystemQuery.h>
#include <Storages/AlterCommands.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_GROUP_ILLEGAL_CONFIG;
    extern const int RESOURCE_GROUP_MISMATCH;
}

std::shared_ptr<ResourceSelectCase::QueryType> ResourceSelectCase::translateQueryType(const DB::String &queryType)
{
    if (queryType == "DDL")
        return std::make_shared<ResourceSelectCase::QueryType>(ResourceSelectCase::QueryType::DDL);
    else if (queryType == "DATA")
        return std::make_shared<ResourceSelectCase::QueryType>(ResourceSelectCase::QueryType::DATA);
    else if (queryType == "SELECT")
        return std::make_shared<ResourceSelectCase::QueryType>(ResourceSelectCase::QueryType::SELECT);
    else if (queryType == "OTHER")
        return std::make_shared<ResourceSelectCase::QueryType>(ResourceSelectCase::QueryType::OTHER);
    return nullptr;
}

ResourceSelectCase::QueryType ResourceSelectCase::getQueryType(const DB::IAST *ast)
{
    if (ast->as<ASTCreateQuery>()
        || ast->as<ASTDropQuery>()
        || ast->as<ASTRenameQuery>()
        // || ast->as<ASTCreateMaskingPolicyQuery>()
    )
        return ResourceSelectCase::QueryType::DDL;

    else if (ast->as<ASTSelectQuery>() || ast->as<ASTSelectWithUnionQuery>())
        return ResourceSelectCase::QueryType::SELECT;

    else if (ast->as<ASTInsertQuery>()
        // || ast->as<ASTDeleteQuery>()
    )
        return ResourceSelectCase::QueryType::DATA;

    else if (const auto * ast_system = ast->as<ASTSystemQuery>(); ast_system
        && ast_system->type == ASTSystemQuery::Type::DEDUP
    )
        return ResourceSelectCase::QueryType::DATA;

    else if (const auto * alter_selects = ast->as<ASTAlterQuery>())
    {
        if (alter_selects->command_list)
        {
            for (auto child : alter_selects->command_list->children)
            {
                auto * command_ast = child->as<ASTAlterCommand>();

                if (auto command = AlterCommand::parse(command_ast); command)
                {
                    if (command->type == AlterCommand::Type::ADD_COLUMN
                    || command->type == AlterCommand::Type::DROP_COLUMN
                    || command->type == AlterCommand::Type::MODIFY_COLUMN
                    || command->type == AlterCommand::Type::COMMENT_COLUMN
                    || command->type == AlterCommand::Type::MODIFY_ORDER_BY
                    || command->type == AlterCommand::Type::MODIFY_TTL
                    || command->type == AlterCommand::Type::ADD_INDEX
                    || command->type == AlterCommand::Type::DROP_INDEX
                    //   || command->type == ASTAlterCommand::Type::CHANGE_ENGINE
                    )
                        return ResourceSelectCase::QueryType::DDL;
                }
            }
        }
        return ResourceSelectCase::QueryType::DATA;
    }
    return ResourceSelectCase::QueryType::OTHER;
}

void IResourceGroupManager::enable()
{
    disabled.store(false, std::memory_order_relaxed);
    LOG_DEBUG(&Poco::Logger::get("ResourceGroupManager"), "enabled");
}

void IResourceGroupManager::disable()
{
    disabled.store(true, std::memory_order_relaxed);
    LOG_DEBUG(&Poco::Logger::get("ResourceGroupManager"), "disabled");
}

IResourceGroupManager::Container IResourceGroupManager::getGroups() const
{
    auto lock = getReadLock();
    return groups;
}

ResourceGroupInfoVec IResourceGroupManager::getInfoVec() const
{
    auto lock = getReadLock();
    ResourceGroupInfoVec infos;
    infos.reserve(groups.size());
    for (const auto & pr : groups)
    {
        infos.emplace_back(pr.second->getInfo());
    }
    return infos;
}

ResourceGroupInfoMap IResourceGroupManager::getInfoMap() const
{
    auto lock = getReadLock();
    ResourceGroupInfoMap info_map;
    info_map.reserve(groups.size());
    for (const auto & pr : groups)
    {
        info_map[pr.first] = pr.second->getInfo();
    }
    return info_map;
}

bool IResourceGroupManager::getInfo(const String & group, ResourceGroupInfo & info) const
{
    auto lock = getReadLock();
    const auto it = groups.find(group);
    if (it == groups.end())
        return false;
    else
    {
        info = it->second->getInfo();
        return true;
    }
}

IResourceGroupManager::~IResourceGroupManager() = default;

}
