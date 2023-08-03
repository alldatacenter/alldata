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

#include <Storages/StorageCloudHive.h>

// #include <Disks/DiskHdfs.h>
#include <Parsers/parseQuery.h>
#include <Storages/AlterCommands.h>
#include <Storages/Hive/HiveDataSelectExecutor.h>
#include <Storages/StorageFactory.h>
// #include <Common/getFQDNOrHostName.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Parsers/ASTLiteral.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>

// #include <Core/UUIDHelpers.h>

namespace DB
{
namespace ErrorCodes
{
    const extern int ABORTED;
    const extern int BAD_ARGUMENTS;
    const extern int NOT_IMPLEMENTED;
    const extern int ACCESS_DELETED_PART;
    extern const int CNCH_MEMORY_BUFFER_IS_NOT_AVAILABLE;
};

StorageCloudHive::StorageCloudHive(
    StorageID table_id_,
    String cnch_database_name_,
    String cnch_table_name_,
    const ColumnsDescription & columns_,
    const ConstraintsDescription & constraints_,
    ContextMutablePtr context_,
    const CnchHiveSettings & settings_)
    : IStorage(table_id_)
    , WithMutableContext(context_->getGlobalContext())
    , cnch_database_name(cnch_database_name_)
    , cnch_table_name(cnch_table_name_)
    , settings(settings_)
    , log(&Poco::Logger::get(table_id_.getNameForLogs() + " (CloudHive)"))
{
    StorageInMemoryMetadata metadata;
    metadata.setColumns(columns_);
    metadata.setConstraints(constraints_);

    setInMemoryMetadata(metadata);

    // disk = std::make_shared<DiskHdfs>("hdfs", "/");
}

StorageCloudHive::~StorageCloudHive() = default;

Pipe StorageCloudHive::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    size_t max_block_size,
    unsigned num_streams)
{
    LOG_TRACE(log, " CloudHive column_names size = {}", column_names.size());
    QueryPlan plan;
    read(plan, column_names, metadata_snapshot, query_info, local_context, processed_stage, max_block_size, num_streams);
    return plan.convertToPipe(
        QueryPlanOptimizationSettings::fromContext(local_context), BuildQueryPipelineSettings::fromContext(local_context));
}

void StorageCloudHive::read(
    QueryPlan & query_plan,
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum /*processed_stage*/,
    size_t max_block_size,
    unsigned num_streams)
{
    LOG_TRACE(log, " CloudHive read ");
    if (auto plan
        = HiveDataSelectExecutor(*this).read(column_names, metadata_snapshot, query_info, local_context, max_block_size, num_streams))
        query_plan = std::move(*plan);
}

// BlockOutputStreamPtr StorageCloudHive::write([[maybe_unused]]const ASTPtr & query, [[maybe_unused]]const Context & query_context)
// {
//     throw Exception("write is not supported now", ErrorCodes::NOT_IMPLEMENTED);
// }

void StorageCloudHive::loadDataParts(HiveDataPartsCNCHVector & data_parts, UInt64 /*worker_topology_hash*/)
{
    // auto current_topology_hash = getContext()->getWorkerTopologyHash(getStorageUUID());
    // if (worker_topology_hash && current_topology_hash && worker_topology_hash != current_topology_hash)
    // {
    //     LOG_INFO(log, "Worker_topology_hash not match. Stop loading data parts.");
    //     return;
    // }

    this->parts = data_parts;

    LOG_DEBUG(log, "Loaded data parts {} items", this->parts.size());
}

DataTypePtr StorageCloudHive::getPartitionValueType() const
{
    DataTypePtr partition_value_type;
    auto partition_types = getInMemoryMetadataPtr()->partition_key.sample_block.getDataTypes();
    if (partition_types.empty())
        partition_value_type = std::make_shared<DataTypeUInt8>();
    else
        partition_value_type = std::make_shared<DataTypeTuple>(std::move(partition_types));
    return partition_value_type;
}

void registerStorageCloudHive(StorageFactory & factory)
{
    StorageFactory::StorageFeatures features{
        .supports_settings = true,
        .supports_projections = true,
        .supports_sort_order = true,
    };

    factory.registerStorage(
        "CloudHive",
        [](const StorageFactory::Arguments & args) {
            ASTs & engine_args = args.engine_args;

            engine_args[0] = evaluateConstantExpressionOrIdentifierAsLiteral(engine_args[0], args.getLocalContext());
            engine_args[1] = evaluateConstantExpressionOrIdentifierAsLiteral(engine_args[1], args.getLocalContext());

            String remote_database = engine_args[0]->as<ASTLiteral &>().value.safeGet<String>();
            String remote_table = engine_args[1]->as<ASTLiteral &>().value.safeGet<String>();
            CnchHiveSettings storage_settings = args.getContext()->getCnchHiveSettings();
            storage_settings.loadFromQuery(*args.storage_def);

            return StorageCloudHive::create(
                args.table_id, remote_database, remote_table, args.columns, args.constraints, args.getContext(), storage_settings);
        },
        features);
}

} /// EOF
