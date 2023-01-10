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

#include <Storages/MergeTree/CnchAttachProcessor.h>
#include <memory>
#include <set>
#include <utility>
#include <Databases/DatabasesCommon.h>
#include <CloudServices/commitCnchParts.h>
#include <CloudServices/CnchPartsHelper.h>
#include <Interpreters/trySetVirtualWarehouse.h>
#include <Parsers/ASTLiteral.h>
#include <Storages/StorageMaterializedView.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Storages/MergeTree/MergeTreeCNCHDataDumper.h>
#include "Storages/PartitionCommands.h"

namespace DB
{

namespace ErrorCodes
{
    extern const int BUCKET_TABLE_ENGINE_MISMATCH;
    extern const int NOT_IMPLEMENTED;
}

MergeTreeDataPartsVector fromCNCHPartsVec(const MutableMergeTreeDataPartsCNCHVector & parts)
{
    MergeTreeDataPartsVector converted_parts;
    for (const MutableMergeTreeDataPartCNCHPtr& part : parts)
    {
        converted_parts.push_back(part);
    }
    return converted_parts;
}

MutableMergeTreeDataPartsCNCHVector toCNCHPartsVec(const MergeTreeDataPartsVector& parts)
{
    MutableMergeTreeDataPartsCNCHVector converted_parts;
    for (const MergeTreeDataPartPtr& part : parts)
    {
        converted_parts.push_back(std::dynamic_pointer_cast<MergeTreeDataPartCNCH>(
            std::const_pointer_cast<IMergeTreeDataPart>(part)
        ));
    }
    return converted_parts;
}

AttachFilter AttachFilter::createPartFilter(const String& part_name,
    MergeTreeDataFormatVersion ver)
{
    AttachFilter filter(Mode::PART, part_name);
    if (!MergeTreePartInfo::tryParsePartName(part_name, &filter.part_name_info, ver))
    {
        throw Exception(fmt::format("Can't parse part info from {}", part_name),
            ErrorCodes::BAD_ARGUMENTS);
    }
    return filter;
}

AttachFilter AttachFilter::createPartitionFilter(const String& partition)
{
    return AttachFilter(Mode::PARTITION, partition);
}

AttachFilter AttachFilter::createPartsFilter()
{
    return AttachFilter(Mode::PARTS);
}

bool AttachFilter::filter(const MergeTreePartInfo& part_info) const
{
    switch (mode)
    {
        case PART:
        {
            return part_name_info.containsExactly(part_info);
        }
        case PARTITION:
        {
            return part_info.partition_id == object_id;
        }
        case PARTS:
        {
            return true;
        }
    }
    __builtin_unreachable();
}

void AttachFilter::checkFilterResult(const std::multiset<String>& visible_part_names,
    const Context& query_ctx) const
{
    switch (mode)
    {
        case PART:
        {
            if (visible_part_names.empty())
            {
                throw Exception(fmt::format("Part {} not found", object_id),
                    ErrorCodes::BAD_ARGUMENTS);
            }
            else if (visible_part_names.size() > 1)
            {
                throw Exception(fmt::format("Expect only one visible part, got {}",
                    visible_part_names.size()), ErrorCodes::BAD_ARGUMENTS);
            }
            else
            {
                if (String visible_part = *visible_part_names.begin(); visible_part != object_id)
                {
                    throw Exception(fmt::format("Can't attach part {}, maybe you want to attach {}",
                        object_id, visible_part), ErrorCodes::BAD_ARGUMENTS);
                }
            }
            return;
        }
        case PARTITION:
        case PARTS:
        {
            if (UInt64 part_limit = query_ctx.getSettingsRef().cnch_part_attach_limit;
                visible_part_names.size() > part_limit)
            {
                throw Exception(fmt::format("Parts number {} exceed {}", visible_part_names.size(),
                    part_limit), ErrorCodes::BAD_ARGUMENTS);
            }
            return;
        }
    }
    __builtin_unreachable();
}

String AttachFilter::toString() const
{
    switch (mode)
    {
        case PART:
        {
            return "{Part: " + object_id + "}";
        }
        case PARTITION:
        {
            return "{Partition: " + object_id + "}";
        }
        case PARTS:
        {
            return "{Parts}";
        }
    }
    __builtin_unreachable();
}

void AttachContext::writeRenameRecord(const DiskPtr &disk, const String &from,
    const String &to)
{
    LOG_TRACE(logger, fmt::format("Write rename record, disk path {}, relative path {} -> {}",
        disk->getPath(), from, to));

    std::lock_guard<std::mutex> lock(mu);

    auto& res = resources[disk->getName()];
    res.disk = disk;
    res.rename_map[from] = to;
}

void AttachContext::writeRenameMapToKV(Catalog::Catalog& catalog, const String& uuid,
    const TxnTimestamp& txn_id)
{
    UndoResources undo_buffers;
    for (const auto & [disk_name, resource] : resources)
    {
        for (const auto & [from, to] : resource.rename_map)
        {
            undo_buffers.emplace_back(txn_id, UndoResourceType::FileSystem, from, to, disk_name);
        }
    }
    catalog.writeUndoBuffer(uuid, txn_id, undo_buffers);
}

void AttachContext::commit()
{
    if (new_txn != nullptr)
    {
        query_ctx.getCnchTransactionCoordinator().finishTransaction(new_txn);
    }
}

void AttachContext::rollback()
{
    if (new_txn != nullptr)
    {
        query_ctx.getCnchTransactionCoordinator().finishTransaction(new_txn);
    }

    size_t total_records = 0;
    for (const auto& [_, resource] : resources)
    {
        total_records += resource.rename_map.size();
    }

    ThreadPool& pool = getWorkerPool(total_records);
    for (const auto& [_, resource] : resources)
    {
        for (const auto& entry : resource.rename_map)
        {
            pool.scheduleOrThrowOnError([&disk = resource.disk, from=entry.first, to=entry.second]() {
                disk->moveDirectory(to, from);
            });
        }
    }
    pool.wait();
}

ThreadPool& AttachContext::getWorkerPool(int job_nums)
{
    bool need_create_thread_pool = worker_pool == nullptr;
    if (!need_create_thread_pool)
    {
        // Already have a thread pool
        if ((job_nums - static_cast<int>(worker_pool->getMaxThreads())) \
            > expand_thread_pool_threshold)
        {
            worker_pool->wait();
            worker_pool = nullptr;

            need_create_thread_pool = true;
        }
    }

    if (need_create_thread_pool)
    {
        worker_pool = std::make_unique<ThreadPool>(
            std::max(1, std::min(max_worker_threads, job_nums)));
    }
    return *worker_pool;
}

void CnchAttachProcessor::exec()
{
    if (is_unique_tbl)
    {
        if (target_tbl.merging_params.hasVersionColumn())
        {
            throw Exception("Attach parition to a storage with version column is not supported",
                ErrorCodes::NOT_IMPLEMENTED);
        }
        if (command.replace)
        {
            throw Exception("Replace partition or part is not supported for unique table",
                ErrorCodes::NOT_IMPLEMENTED);
        }
    }

    AttachContext attach_ctx(*query_ctx, 8, 16, logger);

    try
    {
        // Find all parts which matchs filter, these parts will retain it's origin
        // position, then calculate parts chain and return all visible parts
        std::pair<AttachFilter, PartsFromSources> collect_res = collectParts(attach_ctx);
        PartsFromSources& parts_from_sources = collect_res.second;

        // Assign new part name and rename it to target location
        MutableMergeTreeDataPartsCNCHVector parts_to_commit = prepareParts(
            parts_from_sources, attach_ctx);

        if (command.replace)
        {
            genPartsDeleteMark(parts_to_commit);
        }

        if (!parts_to_commit.empty())
        {
            // Commit transaction
            NameSet staged_parts_name;
            {
                CnchDataWriter cnch_writer(target_tbl, query_ctx, ManipulationType::Insert);
                if (is_unique_tbl)
                {
                    for (const auto& part : parts_to_commit)
                    {
                        staged_parts_name.insert(part->info.getPartName());
                    }
                    cnch_writer.commitPreparedCnchParts(DumpedData{
                        .staged_parts = std::move(parts_to_commit),
                    });
                }
                else
                {
                    cnch_writer.commitPreparedCnchParts(DumpedData{
                        .parts = std::move(parts_to_commit),
                    });
                }

                auto& txn_coordinator = query_ctx->getCnchTransactionCoordinator();
                TransactionCnchPtr txn = query_ctx->getCurrentTransaction();
                txn->setMainTableUUID(target_tbl.getStorageUUID());
                txn_coordinator.commitV2(txn);
            }

            if (is_unique_tbl)
            {
                waitingForDedup(collect_res.first.object_id, staged_parts_name);
            }

            refreshView();
        }
    }
    catch(...)
    {
        tryLogCurrentException(logger);

        attach_ctx.rollback();

        throw;
    }

    attach_ctx.commit();
}

String CnchAttachProcessor::trimPathPostSlash(const String& path)
{
    String path_copy = path;
    if (!path_copy.empty() && path_copy.back() == '/')
    {
        path_copy.pop_back();
    }
    return path_copy;
}

String CnchAttachProcessor::relativePathTo(const String& source, const String& target)
{
    std::filesystem::path source_path(trimPathPostSlash(source));
    std::filesystem::path target_path(trimPathPostSlash(target));

    if (source_path.is_absolute() ^ target_path.is_absolute())
    {
        throw Exception(fmt::format("Source {} and target {} have only one absolute path",
            source, target), ErrorCodes::BAD_ARGUMENTS);
    }
    auto source_iter = source_path.begin();
    auto target_iter = target_path.begin();
    for (auto source_end = source_path.end(), target_end = target_path.end();
        source_iter != source_end && target_iter != target_end; ++source_iter, ++target_iter)
    {
        if (*source_iter != *target_iter)
        {
            break;
        }
    }

    LOG_INFO(&Poco::Logger::get("Source"), "Path " + String(source_path));

    std::filesystem::path relative_path;
    for (auto source_end = source_path.end(); source_iter != source_end; ++source_iter)
    {
        LOG_INFO(&Poco::Logger::get("Source"), fmt::format("Rel '{}'", String(*source_iter)));
        relative_path /= "..";
    }
    for (auto target_end = target_path.end(); target_iter != target_end; ++target_iter)
    {
        relative_path /= *target_iter;
    }
    return relative_path;
}

std::pair<AttachFilter, CnchAttachProcessor::PartsFromSources> CnchAttachProcessor::collectParts(
    AttachContext& attach_ctx)
{
    AttachFilter filter;
    PartsFromSources chained_parts_from_sources;

    if (!command.from_table.empty())
    {
        String database = command.from_database.empty() ?
            query_ctx->getCurrentDatabase() : command.from_database;
        from_storage = DatabaseCatalog::instance().getTable(
            StorageID(database, command.from_table), query_ctx);
        auto * from_cnch_table = target_tbl.checkStructureAndGetCnchMergeTree(from_storage);

        if (command.attach_from_detached)
        {
            auto partition_id = from_cnch_table->getPartitionIDFromQuery(command.partition, query_ctx);

            if (is_unique_tbl && partition_id.empty())
            {
                /// NOTE: For now, we only support `ATTACH DETACHED PARTITION 'xxx' FROM target`, the other
                /// variants might work as well, but we did not tested well.
                throw Exception("Unique table try to attach from a empty partition",
                   ErrorCodes::NOT_IMPLEMENTED);
            }
            filter = AttachFilter::createPartitionFilter(partition_id);
            chained_parts_from_sources = collectPartsFromTableDetached(*from_cnch_table,
                filter, attach_ctx);
        }
        else
        {
            if (is_unique_tbl)
            {
                throw Exception("Doest not supported for unique table",
                    ErrorCodes::NOT_IMPLEMENTED);
            }

            chained_parts_from_sources = collectPartsFromActivePartition(*from_cnch_table,
                attach_ctx);
        }
    }
    else
    {
        if (is_unique_tbl)
        {
            throw Exception("Doest not supported for unique table",
                ErrorCodes::NOT_IMPLEMENTED);
        }

        // Construct filter
        filter = AttachFilter::createPartsFilter();
        if (command.part)
        {
            String part_name = typeid_cast<const ASTLiteral &>(*command.partition)
                .value.safeGet<String>();
            filter = AttachFilter::createPartFilter(part_name, target_tbl.format_version);
        }
        else if (!command.parts)
        {
            String partition_id = target_tbl.getPartitionIDFromQuery(command.partition,
                query_ctx);
            filter = AttachFilter::createPartitionFilter(partition_id);
        }

        if (command.from_zookeeper_path.empty())
        {
            chained_parts_from_sources = collectPartsFromTableDetached(target_tbl,
                filter, attach_ctx);
        }
        else
        {
            chained_parts_from_sources = collectPartsFromPath(command.from_zookeeper_path,
                filter, attach_ctx);
        }
    }

    // Check filter result
    std::multiset<String> visible_part_names;
    for (const MutableMergeTreeDataPartsCNCHVector& parts : chained_parts_from_sources)
    {
        for (const MutableMergeTreeDataPartCNCHPtr& part : parts)
        {
            visible_part_names.insert(part->name);
        }
    }
    filter.checkFilterResult(visible_part_names, *query_ctx);

    // Check part's hash def against table's
    if (target_tbl.isBucketTable() && !query_ctx->getSettingsRef().skip_table_definition_hash_check)
    {
        UInt64 table_def_hash = target_tbl.getTableHashForClusterBy();
        auto check_part_chain_hash = [this, table_def_hash](
                const IMergeTreeDataPartPtr& part) {
            for (IMergeTreeDataPartPtr current = part; current != nullptr;
                current = current->tryGetPreviousPart())
            {
                if (current->bucket_number < 0 || table_def_hash != part->table_definition_hash)
                {
                    LOG_INFO(logger, fmt::format("Part's table_definition_hash [{}] "
                        "is different from target table's table_definition_hash [{}]. "
                        "Part file path: {}, Part bucket number: {}", part->table_definition_hash,
                        table_def_hash, part->getFullPath(), part->bucket_number));
                    throw Exception("Source parts are not bucket parts or have different CLUSTER BY "
                        "definition from the target table. ", ErrorCodes::BUCKET_TABLE_ENGINE_MISMATCH);
                }
            }
        };

        for (const auto& parts : chained_parts_from_sources)
        {
            for (const auto& part : parts)
            {
                check_part_chain_hash(part);
            }
        }
    }

    return std::make_pair<AttachFilter, PartsFromSources>(std::move(filter),
        std::move(chained_parts_from_sources));
}

CnchAttachProcessor::PartsFromSources CnchAttachProcessor::collectPartsFromTableDetached(
    const StorageCnchMergeTree& tbl, const AttachFilter& filter, AttachContext& attach_ctx)
{
    LOG_DEBUG(logger, fmt::format("Collect parts from table {} with filter {}",
        tbl.getLogName(), filter.toString()));

    std::vector<CollectSource> sources(1);
    CollectSource& source = sources.back();

    // Table's detached directory in every disk form a single source
    // and should calculate visible parts together
    Disks remote_disks = tbl.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    for (const DiskPtr& disk : remote_disks)
    {
        String src_rel_path = std::filesystem::path(tbl.getRelativeDataPath(IStorage::StorageLocation::MAIN))
            / "detached" / "";
        source.units.emplace_back(disk, src_rel_path);
    }

    return collectPartsFromSources(tbl, sources, filter, attach_ctx);
}

CnchAttachProcessor::PartsFromSources CnchAttachProcessor::collectPartsFromPath(
    const String& path, const AttachFilter& filter, AttachContext& attach_ctx)
{
    LOG_DEBUG(logger, fmt::format("Collect parts from path {} with filter {}",
        path, filter.toString()));

    auto [src_path, disk] = findBestDiskForHDFSPath(path);

    std::vector<CollectSource> sources = discoverCollectSources(target_tbl, disk,
        src_path, query_ctx->getSettingsRef().cnch_part_attach_max_source_discover_level);

    return collectPartsFromSources(target_tbl, sources, filter, attach_ctx);
}

std::vector<CollectSource> CnchAttachProcessor::discoverCollectSources(
    const StorageCnchMergeTree& tbl, const DiskPtr& disk, const String& root_path,
    int max_discover_depth)
{
    std::vector<String> current_level_path{root_path};
    std::vector<String> next_level_path;

    // Walkthrough current level's dir, if we should drill down, return next level's
    // dirs. If current level has some directory which can be parsed as valid part name,
    // return empty vector to indicate we should use this level as source
    auto walkthrough = [this, &tbl, disk](const std::vector<String>& current_level)
            -> std::vector<String> {
        MergeTreePartInfo part_info;
        std::vector<String> next_level;
        for (const String& path : current_level)
        {
            for (auto iter = disk->iterateDirectory(path); iter->isValid(); iter->next())
            {
                String current_path = std::filesystem::path(path) / iter->name();
                if (disk->isDirectory(current_path))
                {
                    if (MergeTreePartInfo::tryParsePartName(iter->name(), &part_info, tbl.format_version))
                    {
                        LOG_TRACE(logger, fmt::format("Stop discover source since "
                            "{} is a valid part name", iter->name()));
                        return {};
                    }
                    else
                    {
                        next_level.push_back(current_path);
                    }
                }
            }
        }
        return next_level;
    };

    for (int i = 0; i < max_discover_depth; ++i)
    {
        next_level_path = walkthrough(current_level_path);
        if (next_level_path.empty())
        {
            break;
        }

        current_level_path.swap(next_level_path);
    }

    // Construct sources from current level's path
    std::vector<CollectSource> sources(current_level_path.size());
    for (size_t i = 0; i < current_level_path.size(); ++i)
    {
        LOG_TRACE(logger, fmt::format("Construct new source from {}",
            std::string(std::filesystem::path(disk->getPath()) / current_level_path[i])));

        sources.emplace_back();
        sources.back().units.emplace_back(disk, current_level_path[i]);
    }
    return sources;
}

CnchAttachProcessor::PartsFromSources CnchAttachProcessor::collectPartsFromSources(
    const StorageCnchMergeTree& tbl, const std::vector<CollectSource>& sources,
    const AttachFilter& filter, AttachContext& attach_ctx)
{
    // TODO(wsy) Parallel this
    size_t total_parts_num = 0;
    PartsFromSources parts_from_sources;
    for (const CollectSource& source : sources)
    {
        parts_from_sources.emplace_back();
        MutableMergeTreeDataPartsCNCHVector& founded_parts = parts_from_sources.back();

        for (const CollectSource::Unit& unit : source.units)
        {
            LOG_DEBUG(logger, fmt::format("Collect parts from disk {}, path {}",
                unit.disk->getName(),
                std::string(std::filesystem::path(unit.disk->getPath()) / unit.rel_path)));

            if (!unit.disk->exists(unit.rel_path))
            {
                LOG_DEBUG(logger, fmt::format("Path {} doesn't exist, skip",
                    std::string(std::filesystem::path(unit.disk->getPath()) / unit.rel_path)));
            }
            else
            {
                String unit_rel_path = std::filesystem::path(unit.rel_path) / "";
                collectPartsFromUnit(tbl, unit.disk, unit_rel_path,
                    query_ctx->getSettingsRef().cnch_part_attach_drill_down,
                    filter, founded_parts);
            }
        }

        total_parts_num += founded_parts.size();
    }

    if (size_t expected_parts = query_ctx->getSettingsRef().cnch_part_attach_assert_parts_count;
        expected_parts != 0 && total_parts_num != expected_parts)
    {
        throw Exception(fmt::format("Expected parts count {} but got {}", expected_parts,
            total_parts_num), ErrorCodes::BAD_ARGUMENTS);
    }

    // Parallel load parts
    auto& worker_pool = attach_ctx.getWorkerPool(total_parts_num);
    for (MutableMergeTreeDataPartsCNCHVector& parts : parts_from_sources)
    {
        for (const MutableMergeTreeDataPartCNCHPtr& part : parts)
        {
            worker_pool.scheduleOrThrowOnError([part]() {
                part->loadFromFileSystem(false);
            });
        }
    }
    worker_pool.wait();

    // Calculate visible parts
    for (MutableMergeTreeDataPartsCNCHVector& parts : parts_from_sources)
    {
        MergeTreeDataPartsVector converted_parts = fromCNCHPartsVec(parts);
        parts = toCNCHPartsVec(CnchPartsHelper::calcVisibleParts(converted_parts, false));
    }

    return parts_from_sources;
}

void CnchAttachProcessor::collectPartsFromUnit(const StorageCnchMergeTree& tbl,
    const DiskPtr& disk, String& path, int max_drill_down_level,
    const AttachFilter& filter, MutableMergeTreeDataPartsCNCHVector& founded_parts)
{
    if (max_drill_down_level < 0)
    {
        LOG_INFO(logger, fmt::format("Terminate collect since reach max drill down level at {}",
            path));
        return;
    }

    auto volume = std::make_shared<SingleDiskVolume>("single_disk_vol", disk);
    for (auto iter = disk->iterateDirectory(path);
        iter->isValid(); iter->next())
    {
        String current_entry_path = std::filesystem::path(path) / iter->name();
        if (disk->isDirectory(current_entry_path))
        {
            MergeTreePartInfo part_info;
            if (MergeTreePartInfo::tryParsePartName(iter->name(), &part_info,
                tbl.format_version))
            {
                if (filter.filter(part_info))
                {
                    // HACK here, since part's relative path to disk is related to storage's
                    // so, have a relative path here
                    founded_parts.push_back(std::make_shared<MergeTreeDataPartCNCH>(
                        tbl, iter->name(), volume,
                        relativePathTo(tbl.getRelativeDataPath(IStorage::StorageLocation::MAIN), current_entry_path)));
                }
            }
            else
            {
                LOG_TRACE(logger, fmt::format("Failed to parse part name from {}, "
                    "drill down with limit {}", std::string(std::filesystem::path(disk->getPath()) / current_entry_path),
                    max_drill_down_level - 1));

                String dir_name = iter->name() + '/';
                path += dir_name;
                collectPartsFromUnit(tbl, disk, path, max_drill_down_level - 1,
                    filter, founded_parts);
                path.resize(path.size() - dir_name.size());
            }
        }
        else
        {
            LOG_DEBUG(logger, fmt::format("When collect parts from disk {}, path {} "
                "is a file, skip", disk->getName(), std::string(std::filesystem::path(disk->getPath()) / iter->path())));
        }
    }
}

CnchAttachProcessor::PartsFromSources
CnchAttachProcessor::collectPartsFromActivePartition(StorageCnchMergeTree & tbl, [[maybe_unused]] AttachContext & attach_ctx)
{
    LOG_DEBUG(logger, fmt::format("Collect parts from table {} active parts", tbl.getLogName()));

    IMergeTreeDataPartsVector parts;
    PartitionCommand drop_command;
    drop_command.type
        = partitionCommandHasWhere(command) ? PartitionCommand::Type::DROP_PARTITION_WHERE : PartitionCommand::Type::DROP_PARTITION;
    drop_command.partition = command.partition->clone();
    tbl.dropPartitionOrPart(drop_command, query_ctx, &parts);

    // dropPartition will commit old transaction, we need to create a
    // new transaction here
    if (query_ctx->getCurrentTransaction()->getStatus() == CnchTransactionStatus::Finished)
    {
        TransactionCnchPtr txn = query_ctx->getCnchTransactionCoordinator().createTransaction();
        attach_ctx.setAdditionalTxn(txn);
        query_ctx->setCurrentTransaction(txn, false);
    }

    // Convert part
    return {toCNCHPartsVec(parts)};
}

std::pair<String, DiskPtr> CnchAttachProcessor::findBestDiskForHDFSPath(
    const String& from_path)
{
    auto prefix_match = [](const String& disk, const String& target) {
        std::filesystem::path disk_path(trimPathPostSlash(disk));
        std::filesystem::path target_path(trimPathPostSlash(target));

        if (!disk_path.is_absolute() || !target_path.is_absolute())
        {
            throw Exception("Expect only absolute path", ErrorCodes::BAD_ARGUMENTS);
        }

        auto disk_iter = disk_path.begin();
        auto target_iter = target_path.begin();
        size_t matching_prefix = 0;
        for (auto disk_end = disk_path.end(), target_end = target_path.end();
            disk_iter != disk_end && target_iter != target_end; ++disk_iter, ++target_iter)
        {
            if (*disk_iter != *target_iter)
            {
                break;
            }
            ++matching_prefix;
        }

        if (disk_iter != disk_path.end())
        {
            return std::make_pair<UInt32, String>(0, "");
        }
        else
        {
            std::filesystem::path relative_path_to_disk;
            for (auto target_end = target_path.end(); target_iter != target_end; ++target_iter)
            {
                relative_path_to_disk /= *target_iter;
            }
            return std::make_pair<UInt32, String>(matching_prefix, relative_path_to_disk / "");
        }
    };

    DiskPtr best_disk = nullptr;
    UInt32 max_match_depth = 0;
    String rel_path_on_disk;

    Disks disks = target_tbl.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    for (const DiskPtr& disk : disks)
    {
        std::pair<UInt32, String> res = prefix_match(disk->getPath(), from_path);
        if (res.first > max_match_depth)
        {
            best_disk = disk;
            max_match_depth = res.first;
            rel_path_on_disk = res.second;
        }
    }

    if (best_disk == nullptr)
    {
        best_disk = target_tbl.getStoragePolicy(IStorage::StorageLocation::MAIN)->getVolume(0)->getDefaultDisk();
        // Since currently table will assume it's data in disk_root/{table_uuid},
        // Use a relative path to hack here...
        // Currently we use default disk, maybe use disk with most prefix?
        rel_path_on_disk = relativePathTo(best_disk->getPath(),
            from_path);
        LOG_INFO(logger, fmt::format("Path {} is not contained in any name node, "
            "will use default disk, disk base path: {}, relative path: {}",
            from_path, best_disk->getPath(), rel_path_on_disk));
    }
    return std::pair<String, DiskPtr>(rel_path_on_disk, best_disk);
}

// Return flattern data parts
MutableMergeTreeDataPartsCNCHVector CnchAttachProcessor::prepareParts(
    const PartsFromSources& parts_from_sources, AttachContext& attach_ctx)
{
    // Old part and corresponding new part info
    std::vector<std::vector<std::pair<IMergeTreeDataPartPtr, MergeTreePartInfo>>> parts_and_infos_from_sources;
    // Use multiset to prevent different source have part with same name
    std::multiset<String> visible_part_names;

    size_t total_parts_count = 0;
    size_t total_rows_count = 0;

    UInt64 current_tx_id = query_ctx->getCurrentTransactionID().toUInt64();
    for (const MutableMergeTreeDataPartsCNCHVector& visible_parts : parts_from_sources)
    {
        parts_and_infos_from_sources.emplace_back();
        std::vector<std::pair<IMergeTreeDataPartPtr, MergeTreePartInfo>>& parts_and_infos =
            parts_and_infos_from_sources.back();

        for (const MutableMergeTreeDataPartCNCHPtr& part : visible_parts)
        {
            UInt64 new_block_id = query_ctx->getTimestamp();
            UInt64 new_mutation = current_tx_id;

            visible_part_names.insert(part->name);

            for (IMergeTreeDataPartPtr current_part = part; current_part != nullptr;
                current_part = current_part->tryGetPreviousPart())
            {
                auto prev_part = current_part->tryGetPreviousPart();
                if (current_part->isPartial() &&
                    (prev_part == nullptr || current_part->info.hint_mutation != prev_part->info.mutation))
                {
                    throw Exception("Previous part of partital part is absent",
                        ErrorCodes::LOGICAL_ERROR);
                }

                auto new_part_info = MergeTreePartInfo::fromPartName(current_part->info.getPartNameWithHintMutation(),
                    target_tbl.format_version);
                new_part_info.min_block = new_block_id;
                new_part_info.max_block = new_block_id;
                new_part_info.mutation = new_mutation--;

                if (current_part->isPartial())
                {
                    new_part_info.hint_mutation = new_mutation;
                }

                if (!current_part->deleted)
                {
                    total_rows_count += current_part->rows_count;
                }

                parts_and_infos.emplace_back(current_part, new_part_info);
            }
        }

        total_parts_count += parts_and_infos.size();
    }

    if (size_t expected_rows = query_ctx->getSettingsRef().cnch_part_attach_assert_rows_count;
        expected_rows != 0 && expected_rows != total_rows_count)
    {
        throw Exception(fmt::format("Expected rows count {} but got {}", expected_rows, total_rows_count),
            ErrorCodes::BAD_ARGUMENTS);
    }

    // Parallel rename, move parts from source location to target location
    MutableMergeTreeDataPartsCNCHVector prepared_parts;
    prepared_parts.resize(total_parts_count);

    // Create target directory first
    Disks disks = target_tbl.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    for (const DiskPtr& disk : disks)
    {
        disk->createDirectories(target_tbl.getRelativeDataPath(IStorage::StorageLocation::MAIN));
    }

    // Write rename record to kv first
    for (auto& parts_and_infos : parts_and_infos_from_sources)
    {
        for (std::pair<IMergeTreeDataPartPtr, MergeTreePartInfo>& part_and_info : parts_and_infos)
        {
            IMergeTreeDataPartPtr part = part_and_info.first;
            MergeTreePartInfo part_info = part_and_info.second;
            String part_name = part_info.getPartNameWithHintMutation();
            String target_path = std::filesystem::path(target_tbl.getRelativeDataPath(IStorage::StorageLocation::MAIN))
                / part_name / "";
            attach_ctx.writeRenameRecord(part->volume->getDefaultDisk(), part->getFullRelativePath(),
                target_path);
        }
    }
    attach_ctx.writeRenameMapToKV(*(query_ctx->getCnchCatalog()),
        UUIDHelpers::UUIDToString(target_tbl.getStorageUUID()),
        query_ctx->getCurrentTransaction()->getTransactionID());

    UInt64 table_def_hash = target_tbl.getTableHashForClusterBy();
    size_t offset = 0;
    auto& worker_pool = attach_ctx.getWorkerPool(total_parts_count);
    for (auto& parts_and_infos : parts_and_infos_from_sources)
    {
        for (std::pair<IMergeTreeDataPartPtr, MergeTreePartInfo>& part_and_info : parts_and_infos)
        {
            worker_pool.scheduleOrThrowOnError([&prepared_parts, table_def_hash, offset, part = part_and_info.first, part_info = part_and_info.second, this]() {
                String part_name = part_info.getPartNameWithHintMutation();
                String target_path = std::filesystem::path(target_tbl.getRelativeDataPath(IStorage::StorageLocation::MAIN))
                    / part_name / "";
                part->volume->getDisk()->moveDirectory(part->getFullRelativePath(), target_path);

                prepared_parts[offset] = std::make_shared<MergeTreeDataPartCNCH>(
                    target_tbl, part_name, part->volume, part_name);
                prepared_parts[offset]->loadFromFileSystem(false);
                prepared_parts[offset]->table_definition_hash = table_def_hash;
            });
            ++offset;
        }
    }
    worker_pool.wait();

    return prepared_parts;
}

void CnchAttachProcessor::genPartsDeleteMark(MutableMergeTreeDataPartsCNCHVector& parts_to_write)
{
    auto parts_to_drop = target_tbl.selectPartsByPartitionCommand(query_ctx, command);
    if (!parts_to_drop.empty())
    {
        if (target_tbl.isBucketTable() && !query_ctx->getSettingsRef().skip_table_definition_hash_check)
        {
            auto table_def_hash = target_tbl.getTableHashForClusterBy();
            for (const auto& part : parts_to_drop)
            {
                if (part->part_model().bucket_number() < 0 || table_def_hash != part->part_model().table_definition_hash())
                {
                    LOG_DEBUG(logger, fmt::format("Part's table_definition_hash [{}] is "
                        "different from target's table_definition_hash [{}]",
                        part->part_model().table_definition_hash(), table_def_hash));
                    throw Exception("Source parts are not bucket parts or have different CLUSTER BY definition from the target table. ",
                        ErrorCodes::BUCKET_TABLE_ENGINE_MISMATCH);
                }
            }
        }

        MergeTreeCNCHDataDumper dumper(target_tbl);
        for (auto && temp_part : target_tbl.createDropRangesFromParts(parts_to_drop, query_ctx->getCurrentTransaction()))
        {
            auto dumped_part = dumper.dumpTempPart(temp_part);
            dumped_part->is_temp = false;
            parts_to_write.push_back(std::move(dumped_part));
        }
    }
}

void CnchAttachProcessor::waitingForDedup(const String& partition_id,
    const NameSet& staged_parts_name)
{
    LOG_INFO(logger, fmt::format("Attach partition committed {} staged parts. "
        "waiting for dedup in {}", staged_parts_name.size(), partition_id));

    /// Sync the attach process to wait for the dedup to finish before returns.
    NameSet partitions_filter = {partition_id};
    auto unique_key_attach_partition_timeout = query_ctx->getSettingsRef().unique_key_attach_partition_timeout;

    Stopwatch timer;
    while (true)
    {
        auto ts = query_ctx->getTimestamp();
        auto curr_staged_parts = target_tbl.getStagedParts(ts, &partitions_filter);
        bool exists = false;
        for (const auto & part : curr_staged_parts)
            exists |= staged_parts_name.count(part->name);

        if (!exists)
            break;
        else if (timer.elapsedMilliseconds() >= 1000 * unique_key_attach_partition_timeout)
            throw Exception("Attach partition timeout for unique table", ErrorCodes::TIMEOUT_EXCEEDED);

        /// Sleep for a while, not burning cpu cycles.
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    LOG_INFO(logger, fmt::format("Attach partition dedup {} parts finished, costs {} ms",
        staged_parts_name.size(), timer.elapsedMilliseconds()));
}

void CnchAttachProcessor::refreshView()
{
    /// When target table have some dependencies pushing data to views with refresh actions.
    try
    {
        ContextMutablePtr refresh_context = Context::createCopy(query_ctx);
        auto worker_group = getWorkerGroupForTable(target_tbl, refresh_context);
        refresh_context->setCurrentWorkerGroup(worker_group);
        std::vector<StoragePtr> views = getViews(target_tbl.getStorageID(), refresh_context);
        for (auto & view : views)
        {
            if (auto mv = dynamic_cast<StorageMaterializedView*>(view.get()))
                mv->refresh(command.partition, refresh_context, true);
        }
    }
    catch(...)
    {
        tryLogCurrentException(logger);
    }
}

}
