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

#include <WorkerTasks/ManipulationTaskParams.h>

#include <sstream>
#include <Catalog/DataModelPartWrapper.h>
#include <CloudServices/CnchPartsHelper.h>
#include <Parsers/formatAST.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeData.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOG_ERROR;
}

String ManipulationTaskParams::toDebugString() const
{
    std::ostringstream oss;

    oss << "ManipulationTask {" << task_id << "}, type " << typeToString(type) << ", ";
    oss << storage->getStorageID().getNameForLogs() << ": ";
    if (!source_data_parts.empty())
    {
        oss << " source_parts: { " << source_data_parts.front()->name;
        for (size_t i = 1; i < source_data_parts.size(); ++i)
            oss << ", " << source_data_parts[i]->name;
        oss << " }";
    }
    if (!new_part_names.empty())
    {
        oss << " new_part_names: { " << new_part_names.front();
        for (size_t i = 1; i < new_part_names.size(); ++i)
            oss << ", " << new_part_names[i];
        oss << " }";
    }
    if (mutation_commands)
    {
        oss << " mutation_commands: " << serializeAST(*mutation_commands->ast());
    }

    if (columns_commit_time)
        oss << " columns_commit_time: " << columns_commit_time;

    if (mutation_commit_time)
        oss << " mutation_commit_time: " << mutation_commit_time;

    return oss.str();
}

template <class Vec>
void ManipulationTaskParams::assignSourcePartsImpl(const Vec & parts)
{
    if (unlikely(type == Type::Empty))
        throw Exception("Expected non-empty manipulate type", ErrorCodes::LOGICAL_ERROR);

    if (parts.empty())
        return;

    auto left = parts.begin();
    auto right = parts.begin();

    while (left != parts.end())
    {
        if (type == ManipulationType::Merge)
        {
            while (right != parts.end() && (*left)->get_partition().value == (*right)->get_partition().value)
                ++right;
        }
        else
        {
            ++right;
        }

        MergeTreePartInfo part_info;
        part_info.partition_id = (*left)->get_info().partition_id;
        part_info.min_block = (*left)->get_info().min_block;
        part_info.max_block = (*std::prev(right))->get_info().max_block;
        part_info.level = (*left)->get_info().level + 1;

        // TODO: Double check any issue: previously the mutation is set to max part's mutation, now set mutation to current txn id.
        // part_info.mutation = (*std::prev(right))->info.mutation;
        part_info.mutation = txn_id;

        for (auto it = left; it != right; ++it)
        {
            part_info.level = std::max(part_info.level, (*it)->get_info().level + 1);
        }

        new_part_names.push_back(part_info.getPartName());

        left = right;
    }
}

void ManipulationTaskParams::assignSourceParts(ServerDataPartsVector parts)
{
    assignSourcePartsImpl(parts);
    source_parts = std::move(parts);
}

void ManipulationTaskParams::assignSourceParts(MergeTreeDataPartsVector parts)
{
    assignSourcePartsImpl(parts);
    source_data_parts = std::move(parts);
}

void ManipulationTaskParams::assignParts(MergeTreeMutableDataPartsVector parts)
{
    for (auto & part: parts)
        all_parts.emplace_back(std::move(part));
    source_data_parts = CnchPartsHelper::calcVisibleParts(all_parts, false);
    assignSourcePartsImpl(source_data_parts);
}

}
