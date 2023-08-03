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

#include <Interpreters/InterpreterDropWorkerGroupQuery.h>

#include <Interpreters/Context.h>
#include <Parsers/ASTDropWorkerGroupQuery.h>
#include <ResourceManagement/ResourceManagerClient.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_MANAGER_ILLEGAL_CONFIG;
}

InterpreterDropWorkerGroupQuery::InterpreterDropWorkerGroupQuery(const ASTPtr & query_ptr_, ContextPtr context_)
    : WithContext(context_), query_ptr(query_ptr_) {}

BlockIO InterpreterDropWorkerGroupQuery::execute()
{
    auto & drop_query = query_ptr->as<ASTDropWorkerGroupQuery &>();

    if (auto rm_client = getContext()->getResourceManagerClient())
        rm_client->dropWorkerGroup(drop_query.worker_group_id, drop_query.if_exists);
    else
        throw Exception("Can't apply DDL of worker group as RM is not enabled.", ErrorCodes::RESOURCE_MANAGER_ILLEGAL_CONFIG);

    return {};
}

}
