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

#include <Transaction/TransactionCommon.h>

#include <Catalog/Catalog.h>
#include <Core/Types.h>
#include "common/logger_useful.h"
#include <Common/Exception.h>
// #include <Transaction/CnchExplicitTransaction.h>
#include "Disks/IDisk.h"
#include <MergeTreeCommon/CnchStorageCommon.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Interpreters/Context.h>
#include <cppkafka/cppkafka.h>

namespace DB
{
namespace ErrorCodes
{
    // extern const int BAD_CAST;
    extern const int BAD_TYPE_OF_FIELD;
    extern const int LOGICAL_ERROR;
    extern const int BAD_ARGUMENTS;
}

const char * txnStatusToString(CnchTransactionStatus status)
{
    switch (status)
    {
        case CnchTransactionStatus::Running:
            return "Running";
        case CnchTransactionStatus::Finished:
            return "Finished";
        case CnchTransactionStatus::Aborted:
            return "Aborted";
        case CnchTransactionStatus::Inactive:
            return "Inactive";
        case CnchTransactionStatus::Unknown:
            return "Unknown";
    }

    throw Exception("Bad type of Transaction Status", ErrorCodes::BAD_TYPE_OF_FIELD);
}

const char * txnPriorityToString(CnchTransactionPriority priority)
{
    switch (priority)
    {
        case CnchTransactionPriority::low:
            return "Low";
        case CnchTransactionPriority::high:
            return "High";
    }

    throw Exception("Bad type of Transaction Priority", ErrorCodes::BAD_TYPE_OF_FIELD);
}

const char * txnTypeToString(CnchTransactionType type)
{
    switch (type)
    {
        case CnchTransactionType::Implicit:
            return "Implicit";
        case CnchTransactionType::Explicit:
            return "Explicit";
    }

    throw Exception("Bad type of Transaction Type", ErrorCodes::BAD_TYPE_OF_FIELD);
}

const char * txnInitiatorToString(CnchTransactionInitiator initiator)
{
    switch (initiator)
    {
        case CnchTransactionInitiator::Server:
            return "Server";

        case CnchTransactionInitiator::Worker:
            return "Worker";

        case CnchTransactionInitiator::Kafka:
            return "Kafka";

        case CnchTransactionInitiator::Merge:
            return "Merge";

        case CnchTransactionInitiator::GC:
            return "GC";

        case CnchTransactionInitiator::Txn:
            return "Txn";
    }

    throw Exception("Bad type of Transaction Initiator", ErrorCodes::BAD_TYPE_OF_FIELD);
}

void UndoResource::clean(Catalog::Catalog & , [[maybe_unused]]MergeTreeMetaBase * storage) const
{
    if (metadataOnly())
        return;
    DiskPtr disk;
    if (diskName().empty())
    {
        // For cnch, this storage policy should only contains one disk
        disk = storage->getStoragePolicy(IStorage::StorageLocation::MAIN)->getAnyDisk();
    }
    else
    {
        disk = storage->getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByName(diskName());
    }

    /// This can happen in testing environment when disk name may change time to time
    if (!disk)
    {
        throw Exception("Disk " + diskName() + " not found. This should only happens in testing or unstable environment. If this exception is on production, there's a bug", ErrorCodes::LOGICAL_ERROR);
    }

    if (type() == UndoResourceType::Part || type() == UndoResourceType::DeleteBitmap || type() == UndoResourceType::StagedPart)
    {
        const auto & resource_relative_path = placeholders(1);
        String rel_path = storage->getRelativeDataPath(IStorage::StorageLocation::MAIN) + resource_relative_path;
        if (disk->exists(rel_path))
        {
            LOG_DEBUG(log, "Will remove undo path {}", disk->getPath() + rel_path);
            disk->removeRecursive(rel_path);
        }
    }
    else if (type() == UndoResourceType::FileSystem)
    {
        const String & src_path = placeholders(0);
        const String & dst_path = placeholders(1);
        if (!disk->exists(dst_path))
        {
            LOG_TRACE(log, "Disk {} does not contain {}, nothing to move", disk->getPath(), dst_path);
        }
        else
        {
            /// move dst to src
            disk->moveDirectory(dst_path, src_path);
        }
    }
    else
    {
        LOG_DEBUG(log, "Undefined clean method.");
    }
}

UndoResourceNames integrateResources(const UndoResources & resources)
{
    UndoResourceNames result;
    for (const auto & resource : resources)
    {
        if (resource.type() == UndoResourceType::Part)
        {
            result.parts.insert(resource.placeholders(0));
        }
        else if (resource.type() == UndoResourceType::DeleteBitmap)
        {
            result.bitmaps.insert(resource.placeholders(0));
        }
        else if (resource.type() == UndoResourceType::StagedPart)
        {
            result.staged_parts.insert(resource.placeholders(0));
        }
        else if (resource.type() == UndoResourceType::FileSystem)
        {
            /// try to get part name from dst path
            String dst_path = resource.placeholders(1);
            if (dst_path.empty() && dst_path.back() == '/')
                dst_path.pop_back();
            String part_name = dst_path.substr(dst_path.find_last_of('/') + 1);
            if (MergeTreePartInfo::tryParsePartName(part_name, nullptr, MERGE_TREE_CHCH_DATA_STORAGTE_VERSION))
            {
                result.parts.insert(part_name);
            }
        }
        else
            throw Exception("Unknown undo resource type " + toString(static_cast<int>(resource.type())), ErrorCodes::LOGICAL_ERROR);
    }
    return result;
}

}
