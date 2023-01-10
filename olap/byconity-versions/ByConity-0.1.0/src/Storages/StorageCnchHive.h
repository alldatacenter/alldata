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

#include <MergeTreeCommon/CnchStorageCommon.h>
#include <Storages/ColumnsDescription.h>
#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Storages/Hive/HiveMetastore.h>
#include <Storages/MergeTree/CnchHiveSettings.h>
#include <Storages/StorageFactory.h>
#include <hivemetastore/ThriftHiveMetastore.h>
#include <common/shared_ptr_helper.h>

namespace DB
{
class StorageCnchHive final : public shared_ptr_helper<StorageCnchHive>,
                              public IStorage,
                              public WithMutableContext,
                              public CnchStorageCommonHelper
{
public:
    ~StorageCnchHive() override;

    std::string getName() const override { return "CnchHive"; }

    void shutdown() override;

    bool isRemote() const override { return true; }

    bool isBucketTable() const override;

    const ColumnsDescription & getColumns() const { return getInMemoryMetadata().getColumns(); }

    ASTPtr getPartitionKeyList() const { return extractKeyExpressionList(partition_by_ast); }
    static ASTPtr extractKeyExpressionList(const ASTPtr & node);

    QueryProcessingStage::Enum
    getQueryProcessingStage(ContextPtr, QueryProcessingStage::Enum, const StorageMetadataPtr &, SelectQueryInfo &) const override;

    String getFullTablePath();

    Pipe read(
        const Names & /*column_names*/,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & /*query_info*/,
        ContextPtr /*local_context*/,
        QueryProcessingStage::Enum /*processed_stage*/,
        size_t /*max_block_size*/,
        unsigned /*num_streams*/) override;

    void read(
        QueryPlan & query_plan,
        const Names & /*column_names*/,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & /*query_info*/,
        ContextPtr /*local_context*/,
        QueryProcessingStage::Enum /*processed_stage*/,
        size_t /*max_block_size*/,
        unsigned /*num_streams*/) override;

    StorageCnchHive(
        const StorageID & table_id_,
        const String & remote_psm_,
        const String & remote_database_name_,
        const String & remote_table_name_,
        ASTPtr partition_by_ast_,
        ASTPtr cluster_by_ast_,
        ASTPtr order_by_ast_,
        bool is_create_,
        const ColumnsDescription & columns,
        const ConstraintsDescription & constraints_,
        ContextMutablePtr context_,
        const CnchHiveSettings & storage_settings_);

private:
    void setProperties();

    void checkSortByKey();
    void checkClusterByKey();
    void checkStorageFormat();
    void checkPartitionByKey();

    std::set<Int64> getSelectedBucketNumbers(const SelectQueryInfo & query_info, ContextPtr & context);

    HiveDataPartsCNCHVector prepareReadContext(
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & query_info,
        ContextPtr local_context,
        unsigned num_streams);

    HiveDataPartsCNCHVector selectPartsToRead(
        const Names & /*column_names_to_return*/, ContextPtr context, const SelectQueryInfo & query_info, unsigned num_streams);

    HiveDataPartsCNCHVector getDataPartsInPartitions(
        std::shared_ptr<HiveMetastoreClient> & hms_client,
        HivePartitionVector & partitions,
        ContextPtr context,
        const SelectQueryInfo & query_info,
        unsigned num_streams,
        const std::set<Int64> & required_bucket_numbers);

    HiveDataPartsCNCHVector collectHiveFilesFromPartition(
        std::shared_ptr<HiveMetastoreClient> & hms_client,
        HivePartitionPtr & partition,
        ContextPtr context,
        const SelectQueryInfo & query_info,
        const std::set<Int64> & required_bucket_numbers);

    void collectResource(ContextPtr context, const HiveDataPartsCNCHVector & parts, const String & local_table_name);

    HivePartitionVector selectPartitionsByPredicate(
        ContextPtr local_context, const SelectQueryInfo & query_info, std::shared_ptr<HiveMetastoreClient> & hms_client);

    String remote_psm;

    ASTPtr partition_by_ast;
    NamesAndTypesList partition_name_and_types;
    ExpressionActionsPtr partition_key_expr;
    ASTPtr partition_key_expr_list;

    ASTPtr cluster_by_ast;
    Names cluster_by_columns;
    ExpressionActionsPtr cluster_by_key_expr;
    Int64 cluster_by_total_bucket_number = 0;
    ASTPtr cluster_by_expr_list;

    ASTPtr order_by_ast;
    Names sorting_key_columns;
    ASTPtr sorting_key_expr_list;
    ExpressionActionsPtr sorting_key_expr;
    NamesAndTypesList sorting_name_and_types;

    ExpressionActionsPtr minmax_idx_expr;
    Names minmax_idx_columns;
    DataTypes minmax_idx_column_types;

    bool is_create;
    // ContextMutablePtr global_context;
    Poco::Logger * log;

    std::shared_ptr<Table> table = nullptr;
    mutable std::mutex mutex;

public:
    const CnchHiveSettings settings;
};

}
