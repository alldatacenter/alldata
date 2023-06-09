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

#include <Common/HostWithPorts.h>
#include <MergeTreeCommon/CnchServerTopology.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Protos/RPCHelpers.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
// #include <Transaction/ICnchTransaction.h>
#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Transaction/LockRequest.h>
#include <Transaction/TxnTimestamp.h>
#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Catalog/DataModelPartWrapper.h>
#include <Protos/data_models.pb.h>
#include <google/protobuf/repeated_field.h>
#include <memory>

namespace DB
{
using DataPartInfoPtr = std::shared_ptr<MergeTreePartInfo>;
using DataModelPartPtr = std::shared_ptr<Protos::DataModelPart>;
using DataModelPartPtrVector = std::vector<DataModelPartPtr>;

namespace pb = google::protobuf;

MutableMergeTreeDataPartCNCHPtr createPartFromModelCommon(
    const MergeTreeMetaBase & storage, const Protos::DataModelPart & part_model, std::optional<std::string> relative_path = std::nullopt);

MutableMergeTreeDataPartCNCHPtr createPartFromModel(
    const MergeTreeMetaBase & storage,
    const Protos::DataModelPart & part_model,
    std::optional<std::string> relative_path = std::nullopt);

DataPartInfoPtr createPartInfoFromModel(const Protos::DataModelPartInfo & part_info_model);

/// TODO: All operation on vector can run in parallel

template <class T>
inline std::vector<T> createPartVectorFromModels(
    const MergeTreeMetaBase & storage,
    const pb::RepeatedPtrField<Protos::DataModelPart> & parts_model,
    const pb::RepeatedPtrField<std::string> * paths = nullptr)
{
    std::vector<T> res;
    res.reserve(parts_model.size());
    for (int i = 0; i < parts_model.size(); ++i)
        res.emplace_back(createPartFromModel(storage, parts_model[i], (paths ? std::optional(paths->Get(i)) : std::nullopt)));
    return res;
}

void fillPartModel(const IStorage & storage, const IMergeTreeDataPart & part, Protos::DataModelPart & part_model, bool ignore_column_commit_time = false);

void fillPartInfoModel(const IMergeTreeDataPart & part, Protos::DataModelPartInfo & part_info_model);

template <class T>
inline void fillPartsModel(const IStorage & storage, const std::vector<T> & parts, pb::RepeatedPtrField<Protos::DataModelPart> & parts_model)
{
    std::for_each(parts.begin(), parts.end(), [&](const T & part)
    {
        fillPartModel(storage, *part, *parts_model.Add());
    });
}

template <class T>
inline void fillPartsInfoModel(const std::vector<T> & parts, pb::RepeatedPtrField<Protos::DataModelPartInfo> & part_infos_model)
{
    std::for_each(parts.begin(), parts.end(), [&](const T & part)
    {
        fillPartInfoModel(*part, *part_infos_model.Add());
    });
}

void fillPartsModelForSend(const IStorage & storage, const ServerDataPartsVector & parts, pb::RepeatedPtrField<Protos::DataModelPart> & parts_model);

template <class T>
inline void fillPartsModelForSend(const IStorage & storage, const std::vector<T> & parts, pb::RepeatedPtrField<Protos::DataModelPart> & parts_model)
{
    std::set<UInt64> sent_columns_commit_time;
    std::for_each(parts.begin(), parts.end(), [&](const T & part)
    {
        auto & part_model = *parts_model.Add();
        fillPartModel(storage, *part, part_model);
        if (part_model.has_columns_commit_time() && sent_columns_commit_time.count(part_model.columns_commit_time()) == 0)
        {
            part_model.set_columns(part->columns_ptr->toString());
            sent_columns_commit_time.insert(part_model.columns_commit_time());
        }
    });
}

template <class T>
inline void fillBasePartAndDeleteBitmapModels(
    const IStorage & storage,
    const std::vector<T> & parts,
    pb::RepeatedPtrField<Protos::DataModelPart> & parts_model,
    pb::RepeatedPtrField<Protos::DataModelDeleteBitmap> & bitmaps_model)
{
    fillPartsModelForSend(storage, parts, parts_model);
    for (auto & part : parts)
    {
        for (auto & bitmap_meta : part->delete_bitmap_metas)
        {
            bitmaps_model.Add()->CopyFrom(*bitmap_meta);
        }
    }
}

inline void fillTopologyVersions(const std::list<CnchServerTopology> & topologies, pb::RepeatedPtrField<Protos::DataModelTopology> & topology_versions)
{
    std::for_each(topologies.begin(), topologies.end(), [&](const auto & topology)
    {
        auto & topology_version = *topology_versions.Add();
        topology_version.set_expiration(topology.getExpiration());
        for (const auto & host_with_port : topology.getServerList())
        {
            auto & server = *topology_version.add_servers();
            server.set_hostname(host_with_port.id);
            server.set_host(host_with_port.getHost());
            server.set_rpc_port(host_with_port.rpc_port);
            server.set_tcp_port(host_with_port.tcp_port);
            server.set_http_port(host_with_port.http_port);
        }
    });
}

inline std::list<CnchServerTopology> createTopologyVersionsFromModel(const pb::RepeatedPtrField<Protos::DataModelTopology> & topology_versions)
{
    std::list<CnchServerTopology> res;
    std::for_each(topology_versions.begin(), topology_versions.end(), [&](const auto & model)
    {
        UInt64 expiration = model.expiration();
        HostWithPortsVec servers;
        for (const auto & server : model.servers())
        {
            HostWithPorts host_with_port{server.host()};
            host_with_port.rpc_port = server.rpc_port();
            host_with_port.tcp_port = server.tcp_port();
            host_with_port.http_port = server.http_port();
            host_with_port.id = server.hostname();
            servers.push_back(host_with_port);
        }
        res.push_back(CnchServerTopology(expiration, std::move(servers)));
    });
    return res;
}

template <class T>
inline std::vector<T> createPartVectorFromModelsForSend(
    const MergeTreeMetaBase & storage,
    const pb::RepeatedPtrField<Protos::DataModelPart> & parts_model,
    const pb::RepeatedPtrField<std::string> * paths = nullptr)
{
    std::vector<T> res;
    res.reserve(parts_model.size());
    std::map<UInt64, NamesAndTypesListPtr> columns_versions;
    for (int i = 0; i < parts_model.size(); ++i)
    {
        const auto & part_model = parts_model[i];
        auto part = createPartFromModelCommon(storage, part_model, (paths ? std::optional(paths->Get(i)) : std::nullopt));
        part->columns_commit_time = part_model.columns_commit_time();
        if (part_model.has_columns())
        {
            part->setColumns(NamesAndTypesList::parse(part_model.columns()));
            if (part_model.has_columns_commit_time())
            {
                columns_versions[part_model.columns_commit_time()] = part->getColumnsPtr();
            }
        }
        else
        {
            part->setColumnsPtr(columns_versions[part_model.columns_commit_time()]);
        }
        res.emplace_back(std::move(part));
    }
    return res;
}

template <class T>
inline std::vector<T> createBasePartAndDeleteBitmapFromModelsForSend(
    const MergeTreeMetaBase & storage,
    const pb::RepeatedPtrField<Protos::DataModelPart> & parts_model,
    const pb::RepeatedPtrField<Protos::DataModelDeleteBitmap> & bitmaps_model,
    const pb::RepeatedPtrField<std::string> * paths = nullptr)
{
    std::vector<T> res = createPartVectorFromModelsForSend<T>(storage, parts_model, paths);

    auto bitmap_it = bitmaps_model.begin();
    auto same_block = [](const Protos::DataModelDeleteBitmap & bitmap, const T & part)
    {
        return bitmap.partition_id() == part->info.partition_id
               && bitmap.part_min_block() == part->info.min_block
               && bitmap.part_max_block() == part->info.max_block;
    };
    /// fill in bitmap metas for each part
    for (auto & part : res)
    {
        /// partial parts don't have bitmap.
        if (bitmap_it == bitmaps_model.end() || !same_block(*bitmap_it, part))
            continue;

        auto list_it = part->delete_bitmap_metas.before_begin();
        do
        {
            list_it = part->delete_bitmap_metas.insert_after(list_it, std::make_shared<Protos::DataModelDeleteBitmap>(*bitmap_it));
            bitmap_it++;
        }
        while (bitmap_it != bitmaps_model.end() && same_block(*bitmap_it, part));
    }
    return res;
}

inline DataModelPartPtr createPtrFromModel(Protos::DataModelPart part_model)
{
    return std::make_shared<Protos::DataModelPart>(std::move(part_model));
}

std::shared_ptr<MergeTreePartition> createPartitionFromMetaModel(const MergeTreeMetaBase & storage, const Protos::PartitionMeta & meta);

std::shared_ptr<MergeTreePartition> createParitionFromMetaString(const MergeTreeMetaBase & storage, const String & parition_minmax_info);

inline DeleteBitmapMetaPtr createFromModel(const MergeTreeMetaBase & storage, const Protos::DataModelDeleteBitmap & model)
{
    auto model_ptr = std::make_shared<Protos::DataModelDeleteBitmap>(model);
    return std::make_shared<DeleteBitmapMeta>(storage, model_ptr);
}

void fillLockInfoModel(const LockInfo & lock_info, Protos::DataModelLockInfo & model);
LockInfoPtr createLockInfoFromModel(const Protos::DataModelLockInfo & model);

DataModelPartWrapperPtr createPartWrapperFromModel(const MergeTreeMetaBase & storage, const Protos::DataModelPart & part_model);

DataModelPartWrapperPtr createPartWrapperFromModelBasic(const Protos::DataModelPart & part_model);

ServerDataPartsVector createServerPartsFromModels(const MergeTreeMetaBase & storage, const pb::RepeatedPtrField<Protos::DataModelPart> & parts_model);

ServerDataPartsVector createServerPartsFromDataParts(const MergeTreeMetaBase & storage, const MergeTreeDataPartsCNCHVector & parts);

IMergeTreeDataPartsVector createPartVectorFromServerParts(
    const MergeTreeMetaBase & storage,
    const ServerDataPartsVector & parts,
    const std::optional<std::string> & relative_path = std::nullopt);

void fillCnchHivePartsModel(const HiveDataPartsCNCHVector & parts, pb::RepeatedPtrField<Protos::CnchHivePartModel> & parts_model);
HiveDataPartsCNCHVector createCnchHiveDataParts(const ContextPtr & context, const pb::RepeatedPtrField<Protos::CnchHivePartModel> & parts_model);

}
