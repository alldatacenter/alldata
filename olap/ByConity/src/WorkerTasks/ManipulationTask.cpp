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

#include <WorkerTasks/ManipulationTask.h>

#include <CloudServices/CnchPartsHelper.h>
#include <Interpreters/Context.h>
#include <Storages/IStorage.h>
#include <WorkerTasks/ManipulationList.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BAD_ARGUMENTS;
}

ManipulationTask::ManipulationTask(ManipulationTaskParams params_, ContextPtr context_) :
    WithContext(context_),
    params(std::move(params_))
{
    if (/*params.source_parts.empty() && */params.source_data_parts.empty())
        throw Exception("Expected non-empty source parts in ManipulationTaskParams", ErrorCodes::BAD_ARGUMENTS);

    if (params.new_part_names.empty())
        throw Exception("Expected non-empty new part names in ManipulationTaskParams", ErrorCodes::BAD_ARGUMENTS);
}

void ManipulationTask::setManipulationEntry()
{
    auto global_context = getContext()->getGlobalContext();
    manipulation_entry = global_context->getManipulationList().insert(params, false);

    auto * element = manipulation_entry->get();
    element->related_node = getContext()->getClientInfo().current_address.toString() + ":" + toString(params.rpc_port);
}

void ManipulationTask::execute()
{
    try
    {
        /// Mutation is visible in system.manipulations
        setManipulationEntry();

        executeImpl();
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

void executeManipulationTask(ManipulationTaskParams params, ContextPtr context)
{
    auto * log = &Poco::Logger::get(__func__);

    try
    {
        if (!params.storage)
            throw Exception("No storage in manipulate task parameters", ErrorCodes::LOGICAL_ERROR);

        auto task = params.storage->manipulate(params, context);
        task->execute();

        LOG_DEBUG(log, "Finished manipulate {}", params.task_id);
    }
    catch (...)
    {
        tryLogCurrentException(log, "Failed to execute " + params.toDebugString());
    }
}

}
