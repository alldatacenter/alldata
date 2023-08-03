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

#include <Interpreters/InterpreterCreateWorkerGroupQuery.h>

#include <Interpreters/Context.h>
#include <Parsers/ASTCreateWorkerGroupQuery.h>
#include <Parsers/ASTSetQuery.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerClient.h>

#include <Poco/String.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int RESOURCE_MANAGER_ILLEGAL_CONFIG;
}

InterpreterCreateWorkerGroupQuery::InterpreterCreateWorkerGroupQuery(const ASTPtr & query_ptr_, ContextPtr context_)
    : WithContext(context_), query_ptr(query_ptr_) {}

BlockIO InterpreterCreateWorkerGroupQuery::execute()
{
    auto & create_query = query_ptr->as<ASTCreateWorkerGroupQuery &>();

    RM::WorkerGroupData worker_group_data;
    worker_group_data.id = create_query.worker_group_id;

    if (create_query.settings)
    {
        for (auto & change : create_query.settings->changes)
        {
            if (change.name == "type")
            {
                auto value = change.value.safeGet<std::string>();

                if (0 == Poco::icompare(value, "Physical"))
                    worker_group_data.type = WorkerGroupType::Physical;
                else if (0 == Poco::icompare(value, "Shared"))
                    worker_group_data.type = WorkerGroupType::Shared;
                else if (0 == Poco::icompare(value, "Composite"))
                    worker_group_data.type = WorkerGroupType::Composite;
                else
                    throw Exception("Unknown Worker Group type", ErrorCodes::LOGICAL_ERROR);
            }
            else if (change.name == "psm")
            {
                auto value = change.value.safeGet<std::string>();
                worker_group_data.psm = std::move(value);
            }
            else if (change.name == "shared_worker_group")
            {
                auto value = change.value.safeGet<std::string>();
                worker_group_data.linked_id = value;
            }
            else
            {
                throw Exception("Unknown Worker Group setting " + change.name, ErrorCodes::LOGICAL_ERROR);
            }
        }
    }

    if (auto rm_client = getContext()->getResourceManagerClient())
        rm_client->createWorkerGroup(create_query.worker_group_id, create_query.if_not_exists, create_query.vw_name, worker_group_data);
    else
        throw Exception("Can't apply DDL of worker group as RM is not enabled.", ErrorCodes::RESOURCE_MANAGER_ILLEGAL_CONFIG);

    return {};
}

}
