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

#include <Core/Types.h>
#include <Interpreters/Context.h>
#include <Storages/AlterCommands.h>
#include <Storages/Hive/HiveDataPart.h>
#include <Storages/IStorage.h>
#include <Storages/SelectQueryInfo.h>
// #include <Disks/IDisk.h>
#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Storages/MergeTree/CnchHiveSettings.h>
#include <common/shared_ptr_helper.h>

namespace DB
{
class StorageCloudHive : public shared_ptr_helper<StorageCloudHive>, public IStorage, public WithMutableContext
{
public:
    ~StorageCloudHive() override;

    std::string getName() const override { return "CloudHive"; }

    // void alter(
    //     const AlterCommands & params,
    //     const String & current_database_name,
    //     const String & current_table_name,
    //     const Context & context,
    //     TableStructureWriteLockHolder & table_lock_holder) override;


    Pipe read(
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned num_streams) override;

    void read(
        QueryPlan & query_plan,
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned num_streams) override;


    DataTypePtr getPartitionValueType() const;

    // BlockOutputStreamPtr write(const ASTPtr & query, const Context & query_context) override;

    void loadDataParts(HiveDataPartsCNCHVector & parts, UInt64 worker_topology_hash = 0);

    // const String & getCnchDatabase() const { return cnch_database_name; }
    // const String & getCnchTable() const { return cnch_table_name; }

    // const String & getLogName() const { return log_name; }

    HiveDataPartsCNCHVector getDataPartsVector() const { return parts; }

private:
    String cnch_database_name;
    String cnch_table_name;

    // ContextMutablePtr global_context;

public:
    const CnchHiveSettings settings;

private:
    String log_name;
    Poco::Logger * log;

    // DiskPtr disk;

    HiveDataPartsCNCHVector parts;

public:
    StorageCloudHive(
        StorageID table_id_,
        String cnch_database_name,
        String cnch_table_name,
        const ColumnsDescription & columns,
        const ConstraintsDescription & constraints_,
        ContextMutablePtr context_,
        const CnchHiveSettings & settings_);
};

} /// EOF
