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

#include <Storages/MergeTree/IMetastore.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Common/SimpleIncrement.h>

namespace DB
{

class MergeTreeMeta
{

public:

    using MetaStorePtr = std::shared_ptr<IMetaStore>;
    using MutableDataPartPtr = std::shared_ptr<IMergeTreeDataPart>;
    using DataPartPtr = std::shared_ptr<const IMergeTreeDataPart>;
    using MutableDataPartsVector = std::vector<MutableDataPartPtr>;

    MergeTreeMeta(const String _path, const String metastore_name_);

    ~MergeTreeMeta();

    /// Directly load metadata from metastore and restore all data parts.
    std::pair<MutableDataPartsVector, PartNamesWithDisks> loadFromMetastore(const MergeTreeMetaBase & storage);

    /// insert new part into metastore
    void addPart(const MergeTreeMetaBase & storage, const DataPartPtr & part);

    /// remove part from metastore
    void dropPart(const MergeTreeMetaBase & storage, const DataPartPtr & part);

    /// add new wal file into metastore
    void addWAL(const MergeTreeMetaBase & storage, const String & wal_file, const DiskPtr & disk);

    /// remove wal file from metastore
    void removeWAL(const MergeTreeMetaBase & storage, const String & wal_file);

    /// get wal file information from metastore.
    PartNamesWithDisks getWriteAheadLogs(const MergeTreeMetaBase & storage);

    /// load projections.
    void loadProjections(const MergeTreeMetaBase & storage);

    // for metadata management use. if key is not provided, clear all metadata from metastore.
    void dropMetaData(const MergeTreeMetaBase & storage, const String & key = "");

    /// check if can load from metastore
    bool checkMetastoreStatus(const MergeTreeMetaBase & storage);

    /// set status of metastore
    void setMetastoreStatus(const MergeTreeMetaBase & storage);

    /// raw interfaces to intereact with metastore;
    IMetaStore::IteratorPtr getMetaInfo(const String & prefix = "");

    /// open metastore.
    void openMetastore();

    /// Close metastore
    void closeMetastore();

    /// Clean all metadata in metastore
    void cleanMetastore();

    /** ----------------------- COMPATIBLE CODE BEGIN-------------------------- */
    /*  compatible with old metastore. remove this later  */
    bool checkMetaReady();
    std::pair<MutableDataPartsVector, PartNamesWithDisks> loadPartFromMetastore(const MergeTreeMetaBase & storage);
    /*  -----------------------  COMPATIBLE CODE END -------------------------- */


private:

    String path;
    String metastore_name;

    Poco::Logger * log;
    MetaStorePtr metastore;
    std::atomic_bool closed {false};
    std::mutex meta_mutex;

    /// add projections to metastore when committing data part.
 //   void addProjection(const MergeTreeMetaBase & storage, const String & name, const DataPartPtr & proj_part);

    /// remove projections from metastore when dropping data part.
 //   void dropProjection(const MergeTreeMetaBase & storage, const String & name, const DataPartPtr & proj_part);
};

}
