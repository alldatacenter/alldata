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

#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MutationCommands.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <WorkerTasks/ManipulationTask.h>


namespace DB
{

class MergeTreeDataReclusterMutator
{

public:
    MergeTreeDataReclusterMutator(MergeTreeMetaBase & data_);

    MergeTreeMutableDataPartsVector executeClusterTask(
        const ManipulationTaskParams & params,
        ManipulationListEntry & manipulation_entry,
        ContextPtr context);


private:

    MergeTreeMutableDataPartsVector executeOnSinglePart(
        const MergeTreeDataPartPtr & part,
        const ManipulationTaskParams & params,
        ManipulationListEntry & manipulation_entry,
        ContextPtr context);

    bool checkOperationIsNotCanceled(const ManipulationListEntry & manipulation_entry) const;

    MergeTreeMetaBase & data;

    Poco::Logger * log;
};

}
