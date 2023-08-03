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

#include <FormaterTool/PartMerger.h>
#include <FormaterTool/ZipHelper.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTPartToolKit.h>
#include <Interpreters/InterpreterCreateQuery.h>
#include <Interpreters/Context.h>
#include <common/logger_useful.h>
#include <Poco/Path.h>
#include <Poco/DirectoryIterator.h>
#include <Common/StringUtils/StringUtils.h>

namespace DB
{

#define PART_MERGER_RELATIVE_TMP_PATH "parts_dir/"
#define DEFAULT_MAX_MERGE_SIZE 1024*1024*1024*10
#define DEFAULT_MAX_MERGE_ROWS 10485760
#define DEFAULT_MAX_MERGE_THREADS 1

namespace ErrorCodes
{
    extern const int INCORRECT_QUERY;
    extern const int LOGICAL_ERROR;
    extern const int DIRECTORY_DOESNT_EXIST;
    extern const int DIRECTORY_ALREADY_EXISTS;
}

PartMerger::PartMerger(const ASTPtr & query_ptr_, ContextMutablePtr context_)
    : PartToolkitBase(query_ptr_, context_)
{
    const ASTPartToolKit & pm_query = query_ptr->as<ASTPartToolKit &>();
    if (pm_query.type != PartToolType::MERGER)
        throw Exception("Wrong input query.", ErrorCodes::INCORRECT_QUERY);
    source_path = pm_query.source_path->as<ASTLiteral &>().value.safeGet<String>();
    target_path = pm_query.target_path->as<ASTLiteral &>().value.safeGet<String>();

    if (source_path.empty() || target_path.empty())
        throw Exception("Source path and target path cannot be empty.", ErrorCodes::LOGICAL_ERROR);

    if (!endsWith(source_path, "/"))
        source_path.append("/");

    if (!endsWith(target_path, "/"))
        target_path.append("/");

    if (!fs::exists(source_path))
        throw Exception("Source path " + source_path + " does not exists.", ErrorCodes::DIRECTORY_DOESNT_EXIST);

    if (fs::exists(target_path))
        throw Exception("Target location already exists. Please use a non-exists directory instead.", ErrorCodes::DIRECTORY_ALREADY_EXISTS);

    working_path = Poco::Path::current();
    getContext()->setPath(working_path);
    getContext()->setTemporaryStorage(working_path + "tmp");

    if (fs::exists(working_path + PT_RELATIVE_LOCAL_PATH))
    {
        LOG_INFO(log, "Local path {} already exists. Will remove it.", PT_RELATIVE_LOCAL_PATH);
        fs::remove_all(working_path + PT_RELATIVE_LOCAL_PATH);
    }

    // create storage data path
    fs::create_directories(working_path + PT_RELATIVE_LOCAL_PATH);
    // create target directory to get merged parts
    fs::create_directories(target_path);

    applySettings();
}

void PartMerger::execute()
{
    /// collect all parts from source directories and move to tmp data path waiting for attach.
    String data_part_tmp_path = working_path + PT_RELATIVE_LOCAL_PATH + PART_MERGER_RELATIVE_TMP_PATH;
    PartNamesWithDisks parts = collectPartsFromSource(source_path, data_part_tmp_path);

    if (parts.empty())
    {
        LOG_INFO(log, "Cannot find any part to be merged.");
        return;
    }

    LOG_INFO(log, "Start to merge {} data parts.", parts.size());
    StoragePtr table = getTable();
    auto merge_tree_storage = dynamic_cast<StorageMergeTree *>(table.get());

    if (!merge_tree_storage)
        throw Exception("Cannot get storage as MergeTree, its an logical error.", ErrorCodes::LOGICAL_ERROR);

    merge_tree_storage->attachPartsInDirectory(parts, PART_MERGER_RELATIVE_TMP_PATH, getContext());

    /// get all data parts;
    // MergeTreeData::DataParts data_parts = merge_tree_storage->getDataParts();

    auto entries = selectPartsToMerge(*merge_tree_storage, DEFAULT_MAX_MERGE_SIZE, DEFAULT_MAX_MERGE_ROWS);

    logMergeGroups(entries);

    size_t max_threads = user_settings.count("max_merge_threads") ? user_settings["max_merge_threads"].safeGet<UInt64>() : DEFAULT_MAX_MERGE_THREADS;
    size_t max_merge_size = user_settings.count("max_merge_size") ? user_settings["max_merge_size"].safeGet<UInt64>() : DEFAULT_MAX_MERGE_SIZE;
    size_t max_merge_rows = user_settings.count("max_merge_rows") ? user_settings["max_merge_rows"].safeGet<UInt64>() : DEFAULT_MAX_MERGE_SIZE;

    LOG_INFO(log, "Execute merge with parameter:  [max_threads: {}, max_merge_size: {}, max_merge_rows: {}]",
        max_threads, max_merge_size, max_merge_rows);

    /// create new merger mutator because the merge related code is private in StorageMergeTree.
    MergeTreeDataMergerMutator merger_mutator(*merge_tree_storage, getContext()->getSettingsRef().background_pool_size);
    auto table_lock_holder = table->lockForShare(RWLockImpl::NO_QUERY, merge_tree_storage->getSettings()->lock_acquire_timeout_for_background_operations);

    auto run_merge_task = [&] (const MergeEntryPtr & entry)
    {
        if (entry->parts.size()==1)
        {
            /// for those entry wich only have one part, we just move original zip file to destination.
            String part_zip_file = entry->parts[0]->name + ".zip";
            String file_full_path = data_part_tmp_path + part_zip_file;
            if (fs::exists(file_full_path))
                fs::rename(file_full_path, target_path + part_zip_file);
            else
                throw Exception("Original part file does not exists.", ErrorCodes::LOGICAL_ERROR);

            LOG_DEBUG(log, "Directly move part file {} to target path", part_zip_file);
        }
        else
        {
            auto merge_list_entry = getContext()->getMergeList().insert(table->getStorageID(), *entry, merge_tree_storage->getContext()->getSettingsRef());
            ReservationPtr reserved_space = merge_tree_storage->tryReserveSpace(MergeTreeDataMergerMutator::estimateNeededDiskSpace(entry->parts), entry->parts[0]->volume);
            auto new_part = merger_mutator.mergePartsToTemporaryPart(
                *entry,
                table->getInMemoryMetadataPtr(),
                *(merge_list_entry),
                table_lock_holder,
                time(nullptr),
                getContext(),
                reserved_space,
                false,
                {},
                merge_tree_storage->merging_params);

            merger_mutator.renameMergedTemporaryPart(new_part, entry->parts, nullptr);

            LOG_DEBUG(log, "Merged {} parts into new part {}.", toString(entry->parts.size()), new_part->name);

            ZipHelper::zipFile(working_path + PT_RELATIVE_LOCAL_PATH + new_part->name + "/", target_path + new_part->name + ".zip");
        }
    };

    ThreadPool pool(max_threads);

    for (const auto & entry : entries)
    {
        pool.scheduleOrThrow(
            [&, merge_entry=entry]()
            {
                run_merge_task(merge_entry);
            }
        );
    }

    pool.wait();

    LOG_INFO(log, "Finish to merge data parts.");
}

std::vector<MergeEntryPtr> PartMerger::selectPartsToMerge(const MergeTreeData & data, size_t max_total_size_to_merge, size_t max_total_rows_to_merge)
{
    std::vector<MergeEntryPtr> res;
    MergeTreeData::DataPartsVector data_parts = data.getDataPartsVector();
    if (data_parts.empty())
    {
        LOG_ERROR(log, "Cannot get any parts from storage.");
        return res;
    }

    MergeTreeData::DataPartsVector parts_to_merge;
    String prev_partition_id;
    size_t total_merge_size = 0;
    size_t total_merge_rows = 0;

    auto add_merge_entry = [&]()
    {
        auto current_entry = std::make_shared<FutureMergedMutatedPart>();
        current_entry->assign(std::move(parts_to_merge));
        res.emplace_back(current_entry);
    };

    // auto current_entry = std::make_shared<FutureMergedMutatedPart>();
    for (const MergeTreeData::DataPartPtr & part : data_parts)
    {
        const String & partition_id = part->info.partition_id;

        if (prev_partition_id.empty())
            prev_partition_id = partition_id;

        if (prev_partition_id != partition_id ||
            total_merge_size + part->getBytesOnDisk() > max_total_size_to_merge ||
            total_merge_rows + part->rows_count > max_total_rows_to_merge)
        {
            if (parts_to_merge.size())
            {
                add_merge_entry();
                total_merge_size = 0;
                total_merge_rows = 0;
                parts_to_merge = MergeTreeData::DataPartsVector{};
            }

            if (prev_partition_id != partition_id)
                prev_partition_id = partition_id;
        }

        parts_to_merge.emplace_back(part);
        total_merge_size += part->getBytesOnDisk();
        total_merge_rows += part->rows_count;
    }

    if (parts_to_merge.size())
        add_merge_entry();

    return res;
}

void PartMerger::logMergeGroups(const std::vector<MergeEntryPtr> & merge_groups)
{
    WriteBufferFromOwnString wb;
    for (size_t i=0; i<merge_groups.size(); i++)
    {
        wb << "[";
        for (size_t j=0; j<merge_groups[i]->parts.size(); j++)
        {
            wb << merge_groups[i]->parts[j]->name;
            if (j<merge_groups[i]->parts.size()-1)
                wb << ",";
        }
        wb << "]";
        if (i < merge_groups.size()-1)
            wb << ",";
    }
    LOG_DEBUG(log, "Merge groups: {}", wb.str());
}

}
