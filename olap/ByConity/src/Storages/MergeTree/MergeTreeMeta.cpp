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

#include <Storages/MergeTree/MergeTreeMeta.h>
#include <Storages/MergeTree/MetastoreRocksDBImpl.h>
#include <Storages/MergeTree/MergeTreeMetaDataCommon.h>

namespace DB
{

MergeTreeMeta::MergeTreeMeta(const String _path, const String metastore_name_)
        : path(_path + "catalog.db")
        , metastore_name(metastore_name_)
        , log(&Poco::Logger::get(metastore_name + "(MetaStore)"))
{
    openMetastore();
}

MergeTreeMeta::~MergeTreeMeta()
{
    /// Make sure metastore is closed when MergeTreeMeta is destroyed.
    closeMetastore();
}


std::pair<MergeTreeMeta::MutableDataPartsVector, PartNamesWithDisks> MergeTreeMeta::loadFromMetastore(const MergeTreeMetaBase & storage)
{
    std::vector<MutableDataPartPtr> loaded_parts;
    PartNamesWithDisks unloaded_parts;

    String part_meta_prefix = getPartMetaPrefix(storage.getStorageID().uuid);

    IMetaStore::IteratorPtr iterPtr = metastore->scan(part_meta_prefix);
    while(iterPtr->hasNext() && startsWith(iterPtr->key(), part_meta_prefix))
    {
        String part_name = iterPtr->key().substr(part_meta_prefix.size());
        Protos::DataPartModel part_data;
        part_data.ParseFromString(iterPtr->value());

        try
        {
            MutableDataPartPtr part = buildPartFromMeta(storage, part_name, part_data);
            loaded_parts.push_back(part);
        }
        catch (Exception & e)
        {
            LOG_ERROR(log, "Failed to build part from metadata with key '{}'. Will remove it from metastore. (Error : {})", iterPtr->key(), e.what());
            /// add unloaded parts for later reloading from file system.
            DiskPtr disk_ptr = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByID(part_data.disk_id());
            if (disk_ptr)
                unloaded_parts.emplace_back(part_name, disk_ptr);

            metastore->drop(iterPtr->key());
        }
        iterPtr->next();
    }

    return {loaded_parts, unloaded_parts};
}

void MergeTreeMeta::addPart(const MergeTreeMetaBase & storage, const DataPartPtr & part)
{
    if (part->getProjectionParts().empty())
        metastore->put(getPartMetaKey(storage.getStorageID().uuid, part->name), getSerializedPartMeta(part));
    else
    {
        /// commit part with its projection parts.
        MetastoreRocksDBImpl::MultiWrite multi_write(metastore);
        /// add current part
        multi_write.addPut(getPartMetaKey(storage.getStorageID().uuid, part->name), getSerializedPartMeta(part));
        /// add projections
        for (auto & [projection_name, p_part] : part->getProjectionParts())
            multi_write.addPut(getProjectionKey(storage.getStorageID().uuid, part->name, projection_name), getSerializedPartMeta(p_part));

        multi_write.commit();
    }
}

void MergeTreeMeta::dropPart(const MergeTreeMetaBase & storage, const DataPartPtr & part)
{
    if (part->getProjectionParts().empty())
        metastore->drop(getPartMetaKey(storage.getStorageID().uuid, part->name));
    else
    {
        /// remove part as well as its projections
        MetastoreRocksDBImpl::MultiWrite multi_write(metastore);
        /// delete current part
        multi_write.addDelete(getPartMetaKey(storage.getStorageID().uuid, part->name));
        /// delete corresponding projections
        for (auto & [projection_name, p_part] : part->getProjectionParts())
            multi_write.addDelete(getProjectionKey(storage.getStorageID().uuid, part->name, projection_name));

        multi_write.commit();
    }
}

void MergeTreeMeta::addWAL(const MergeTreeMetaBase & storage, const String & wal_file, const DiskPtr & disk)
{
    LOG_DEBUG(log, "Add new wal file '{}' into metastore.", wal_file);
    metastore->put(getWALMetaKey(storage.getStorageID().uuid, wal_file), std::to_string(disk->getID()));
}

void MergeTreeMeta::removeWAL(const MergeTreeMetaBase & storage, const String & wal_file)
{
    LOG_DEBUG(log, "Remove outdated wal file '{}' from metastore.", wal_file);
    metastore->drop(getWALMetaKey(storage.getStorageID().uuid, wal_file));
}

PartNamesWithDisks MergeTreeMeta::getWriteAheadLogs(const MergeTreeMetaBase & storage)
{
    PartNamesWithDisks wals;

    String wal_prefix = getWALMetaPrefix(storage.getStorageID().uuid);

    IMetaStore::IteratorPtr iterPtr = metastore->scan(wal_prefix);

    while(iterPtr->hasNext() && startsWith(iterPtr->key(), wal_prefix))
    {
        String wal_file_name = iterPtr->key().substr(wal_prefix.size());
        DiskPtr disk_ptr = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByID(std::stoull(iterPtr->value()));
        if (disk_ptr)
            wals.emplace_back(wal_file_name, disk_ptr);
        else
        {
            LOG_ERROR(log, "Cannot get disk by id '{}' for WAL file '{}', remove it from metastore.", iterPtr->value(), wal_file_name);
            metastore->drop(iterPtr->key());
        }
        iterPtr->next();
    }

    return wals;
}

void MergeTreeMeta::loadProjections(const MergeTreeMetaBase & storage)
{
    String projection_prefix = getProjectionPrefix(storage.getStorageID().uuid);

    IMetaStore::IteratorPtr iterPtr = metastore->scan(projection_prefix);

    while(iterPtr->hasNext() && startsWith(iterPtr->key(), projection_prefix))
    {
        Protos::DataPartModel part_data;
        part_data.ParseFromString(iterPtr->value());

        String parent_name = part_data.parent_part();
        DataPartPtr parent_part = const_cast<MergeTreeMetaBase &>(storage).getPartIfExistsWithoutLock(parent_name, {MergeTreeDataPartState::Committed});
        if (!parent_part)
        {
            iterPtr->next();
            continue;
        }

        String projection_name = iterPtr->key().substr(projection_prefix.size() + parent_name.size() + 1); ///seed getProjectionKey() for more details.

        try
        {
            MutableDataPartPtr part = buildProjectionFromMeta(storage, projection_name, part_data, parent_part.get());
            const_cast<IMergeTreeDataPart *>(parent_part.get())->addProjectionPart(projection_name, std::move(part));
        }
        catch (Exception & e)
        {
            LOG_ERROR(log, "Failed to build projection part from metadata with key '{}'. Will remove it from metastore. (Error : {})", iterPtr->key(), e.what());
            metastore->drop(iterPtr->key());
        }

        iterPtr->next();
    }
}

void MergeTreeMeta::dropMetaData(const MergeTreeMetaBase & /*storage*/, const String & key)
{
    if (key.empty())
    {
        LOG_DEBUG(log, "Clean all metadata from metastore {}.", metastore_name);
        metastore->clean();
    }
    else
    {
        LOG_DEBUG(log, "Removing key {} from metastore {}.", key, metastore_name);
        metastore->drop(key);
    }
}

bool MergeTreeMeta::checkMetastoreStatus(const MergeTreeMetaBase & storage)
{
    String status;
    metastore->get(getMetaStoreStatusKey(storage.getStorageID().uuid), status);
    if (status == META_DATA_READY_FLAG)
        return true;
    else
        return false;
}

void MergeTreeMeta::setMetastoreStatus(const MergeTreeMetaBase & storage)
{
    metastore->put(getMetaStoreStatusKey(storage.getStorageID().uuid), META_DATA_READY_FLAG);
}

IMetaStore::IteratorPtr MergeTreeMeta::getMetaInfo(const String & prefix)
{
    return metastore->scan(prefix);
}

void MergeTreeMeta::openMetastore()
{
    LOG_TRACE(log, "Initializing storage metastore.", path);
    metastore = std::make_shared<MetastoreRocksDBImpl>(path);
}

void MergeTreeMeta::closeMetastore()
{
    std::lock_guard lock(meta_mutex);
    if (!closed)
    {
        LOG_TRACE(log, "Closing storage metastore.", path);
        metastore->close();
        closed = true;
    }
}

void MergeTreeMeta::cleanMetastore()
{
    metastore->clean();
}

/** ----------------------- COMPATIBLE CODE BEGIN-------------------------- */
/*   compatible with old metastore. remove this later   */
#define METASTORE_REDAY_KEY "IS_READY"
#define METASTORE_REDAY_VALUE "TRUE"
#define METASTORE_MIN_BLOCK_PREFIX "!BLOCK_"
bool MergeTreeMeta::checkMetaReady()
{
    String flag;
    metastore->get(METASTORE_REDAY_KEY, flag);
    if (flag == METASTORE_REDAY_VALUE)
        return true;

    return false;
}

std::pair<MergeTreeMeta::MutableDataPartsVector, PartNamesWithDisks> MergeTreeMeta::loadPartFromMetastore(const MergeTreeMetaBase & storage)
{
    /// part_name and disk_name
    std::vector<MutableDataPartPtr> loaded_parts;
    PartNamesWithDisks unloaded_parts;

    IMetaStore::IteratorPtr iterPtr = metastore->scan("");
    while(iterPtr->hasNext())
    {
        const String & key = iterPtr->key();
        const String & value = iterPtr->value();
        if (key == METASTORE_REDAY_KEY || startsWith(key, METASTORE_MIN_BLOCK_PREFIX))
        {
            iterPtr->next();
            continue;
        }

        MutableDataPartPtr part;

        /// If failed to load parts from metainfo, we will load them from filesystem later.
        try
        {
            part = createPartFromRaw(storage, key, value);
            loaded_parts.push_back(part);
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
            auto pos = key.rfind('_');
            String part_name = key.substr(0, pos);
            String disk_name = unescapeForDiskName(key.substr(pos + 1));
            DiskPtr disk_ptr = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByName(disk_name);
            if (disk_ptr)
                unloaded_parts.emplace_back(part_name, disk_ptr);
            LOG_WARNING(log, "Broken meta info for part {}", part_name);
            iterPtr->next();
            continue;
        }

        iterPtr->next();
    }

    return {loaded_parts, unloaded_parts};
}

/*  -----------------------  COMPATIBLE CODE END -------------------------- */
}
