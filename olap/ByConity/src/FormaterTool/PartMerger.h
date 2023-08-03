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
#include <FormaterTool/PartToolkitBase.h>
#include <Poco/Logger.h>

namespace DB
{

using MergeEntryPtr = std::shared_ptr<FutureMergedMutatedPart>;

class PartMerger : public PartToolkitBase
{

public:
    PartMerger(const ASTPtr & query_ptr_, ContextMutablePtr context_);
    void execute() override;

private:

    std::vector<MergeEntryPtr> selectPartsToMerge(const MergeTreeData & data, size_t max_total_size_to_merge, size_t max_total_rows_to_merge);

    void logMergeGroups(const std::vector<MergeEntryPtr> & merge_groups);

    Poco::Logger * log = &Poco::Logger::get("PartMerger");
    String source_path;
    String target_path;
    String working_path;
};

}
