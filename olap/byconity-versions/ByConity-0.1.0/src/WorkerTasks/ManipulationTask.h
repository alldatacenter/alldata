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

#include <Core/Types.h>
#include <WorkerTasks/ManipulationList.h>
#include <WorkerTasks/ManipulationTaskParams.h>
#include <WorkerTasks/ManipulationType.h>
#include <Storages/IStorage_fwd.h>
#include <Interpreters/Context_fwd.h>

#include <atomic>
#include <optional>
#include <boost/noncopyable.hpp>

namespace DB
{

class ManipulationTask : public WithContext, private boost::noncopyable
{
public:
    ManipulationTask(ManipulationTaskParams params, ContextPtr context_);
    virtual ~ManipulationTask() = default;

    virtual void executeImpl() = 0;

    void execute();

    virtual bool isCancelled() { return getManipulationListElement()->is_cancelled.load(std::memory_order_relaxed); }
    virtual void setCancelled() { getManipulationListElement()->is_cancelled.store(true, std::memory_order_relaxed); }

    void setManipulationEntry();

    ManipulationListElement * getManipulationListElement() { return manipulation_entry->get(); }

    auto & getParams() { return params; }

protected:
    ManipulationTaskParams params;

    std::unique_ptr<ManipulationListEntry> manipulation_entry;
};

using ManipulationTaskPtr = std::shared_ptr<ManipulationTask>;

/// Async
void executeManipulationTask(ManipulationTaskParams params, ContextPtr context);

}
