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

#include <WorkerTasks/CloudMergeTreeMutateTask.h>

#include <CloudServices/commitCnchParts.h>
#include <Interpreters/Context.h>
#include <Storages/StorageCloudMergeTree.h>
#include <WorkerTasks/MergeTreeDataMutator.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int ABORTED;
}

CloudMergeTreeMutateTask::CloudMergeTreeMutateTask(
    StorageCloudMergeTree & storage_,
    ManipulationTaskParams params_,
    ContextPtr context_)
    : ManipulationTask(std::move(params_), std::move(context_))
    , storage(storage_)
{
}

void CloudMergeTreeMutateTask::executeImpl()
{
    auto lock_holder = storage.lockForShare(RWLockImpl::NO_QUERY, storage.getSettings()->lock_acquire_timeout_for_background_operations);

    MergeTreeDataMutator mutate_executor(storage, getContext()->getSettingsRef().background_pool_size);
    auto data_parts = mutate_executor.mutatePartsToTemporaryParts(params, *manipulation_entry, getContext(), lock_holder);

    if (isCancelled())
        throw Exception("Merge task " + params.task_id + " is cancelled", ErrorCodes::ABORTED);

    CnchDataWriter cnch_writer(storage, getContext(), ManipulationType::Mutate, params.task_id);
    cnch_writer.dumpAndCommitCnchParts(data_parts);
}

}
