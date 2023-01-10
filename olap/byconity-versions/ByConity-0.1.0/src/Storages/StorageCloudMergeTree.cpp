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

#include <Storages/StorageCloudMergeTree.h>

#include <Common/Exception.h>
#include "Core/UUID.h"
#include "Storages/IStorage.h"
#include <Interpreters/evaluateConstantExpression.h>
#include <Interpreters/ExpressionAnalyzer.h>
#include <Interpreters/TreeRewriter.h>
#include <MergeTreeCommon/CnchBucketTableCommon.h>
#include <Processors/Pipe.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <Storages/MergeTree/CloudMergeTreeBlockOutputStream.h>
#include <Storages/MergeTree/MergeTreeDataSelectExecutor.h>
#include <Storages/MergeTree/MergeTreeDataWriter.h>
#include <Storages/MutationCommands.h>
#include <WorkerTasks/CloudMergeTreeMutateTask.h>
#include <WorkerTasks/CloudMergeTreeMergeTask.h>
#include <WorkerTasks/CloudMergeTreeReclusterTask.h>
#include <WorkerTasks/CloudUniqueMergeTreeMergeTask.h>
#include <WorkerTasks/ManipulationTaskParams.h>
#include <WorkerTasks/ManipulationType.h>
#include <CloudServices/CloudMergeTreeDedupWorker.h>
#include <CloudServices/CnchPartsHelper.h>
#include <Storages/IngestColumnCnch/IngestColumnCnchHelper.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

StorageCloudMergeTree::StorageCloudMergeTree(
    const StorageID & table_id_,
    String cnch_database_name_,
    String cnch_table_name_,
    const String & relative_data_path_,
    const StorageInMemoryMetadata & metadata_,
    ContextMutablePtr context_,
    const String & date_column_name_,
    const MergeTreeMetaBase::MergingParams & merging_params_,
    std::unique_ptr<MergeTreeSettings> settings_)
    : MergeTreeCloudData( // NOLINT
        table_id_,
        relative_data_path_.empty()
            ? (settings_->cnch_table_uuid.value.empty() ? UUIDHelpers::UUIDToString(table_id_.uuid) : settings_->cnch_table_uuid.value)
            : relative_data_path_,
        metadata_,
        context_,
        date_column_name_,
        merging_params_,
        std::move(settings_))
    , cnch_database_name(std::move(cnch_database_name_))
    , cnch_table_name(std::move(cnch_table_name_))
{
    const String cnch_uuid = getSettings()->cnch_table_uuid.value;
    if (cnch_uuid.empty())
        relative_auxility_storage_path = fs::path("auxility_store") / UUIDHelpers::UUIDToString(table_id_.uuid) / "";
    else
        relative_auxility_storage_path = fs::path("auxility_store") / cnch_uuid / "";

    format_version = MERGE_TREE_CHCH_DATA_STORAGTE_VERSION;

    if (getInMemoryMetadataPtr()->hasUniqueKey() && getSettings()->cloud_enable_dedup_worker)
        dedup_worker = std::make_unique<CloudMergeTreeDedupWorker>(*this);
}

void StorageCloudMergeTree::startup()
{
    if (dedup_worker)
        dedup_worker->start();
}

void StorageCloudMergeTree::shutdown()
{
    if (dedup_worker)
        dedup_worker->stop();
}

StorageCloudMergeTree::~StorageCloudMergeTree()
{
}

void StorageCloudMergeTree::read(
    QueryPlan & query_plan,
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    size_t max_block_size,
    unsigned num_streams)
{
    if (auto plan = MergeTreeDataSelectExecutor(*this).read(
            column_names, metadata_snapshot, query_info, local_context, max_block_size, num_streams, processed_stage))
        query_plan = std::move(*plan);
}

Pipe StorageCloudMergeTree::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    const size_t max_block_size,
    const unsigned num_streams)
{
    QueryPlan plan;
    read(plan, column_names, metadata_snapshot, query_info, local_context, processed_stage, max_block_size, num_streams);
    return plan.convertToPipe(
        QueryPlanOptimizationSettings::fromContext(local_context), BuildQueryPipelineSettings::fromContext(local_context));
}

BlockOutputStreamPtr StorageCloudMergeTree::write(const ASTPtr &, const StorageMetadataPtr & metadata_snapshot, ContextPtr local_context)
{
    bool enable_staging_area = metadata_snapshot->hasUniqueKey() && bool(local_context->getSettingsRef().enable_staging_area_for_write);
    return std::make_shared<CloudMergeTreeBlockOutputStream>(*this, metadata_snapshot, std::move(local_context), enable_staging_area);
}

ManipulationTaskPtr StorageCloudMergeTree::manipulate(const ManipulationTaskParams & input_params, ContextPtr task_context)
{
    ManipulationTaskPtr task;
    switch (input_params.type)
    {
        case ManipulationType::Merge:
            if (getInMemoryMetadataPtr()->hasUniqueKey())
                task = std::make_shared<CloudUniqueMergeTreeMergeTask>(*this, input_params, task_context);
            else
                task = std::make_shared<CloudMergeTreeMergeTask>(*this, input_params, task_context);
            break;
        case ManipulationType::Mutate:
            task = std::make_shared<CloudMergeTreeMutateTask>(*this, input_params, task_context);
            break;
        case ManipulationType::Clustering:
            task = std::make_shared<CloudMergeTreeReclusterTask>(*this, input_params, task_context);
            break;
        default:
            throw Exception("Unsupported manipulation task: " + String(typeToString(input_params.type)), ErrorCodes::NOT_IMPLEMENTED);
    }

    /// task->execute();

    /// LOG_DEBUG(log, "Finished manipulate task {}", input_params.task_id);
    return task;
}

void StorageCloudMergeTree::checkMutationIsPossible(const MutationCommands & commands, const Settings & /*settings*/) const
{
    for (const auto & command : commands)
    {
        if (command.type == MutationCommand::Type::FAST_DELETE)
            throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute FASTDELETE commands");
        if (command.type == MutationCommand::Type::DELETE)
            throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute DELETE commands");
        if (command.type == MutationCommand::Type::UPDATE)
            throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute UPDATE commands");
        if (command.type == MutationCommand::Type::MATERIALIZE_TTL)
            throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute MATERIALIZE_TTL commands");
    }
}

void StorageCloudMergeTree::checkAlterPartitionIsPossible(const PartitionCommands & commands, const StorageMetadataPtr &, const Settings &) const
{
    for (const auto & command : commands)
    {
        if (command.type != PartitionCommand::Type::INGEST_PARTITION)
            throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute any PARTITION commands except INGEST_PARTITON");
    }
}

Pipe StorageCloudMergeTree::alterPartition(
    const StorageMetadataPtr & metadata_snapshot,
    const PartitionCommands & commands,
    ContextPtr local_context)
{
    for (auto & command : commands)
    {
        switch (command.type)
        {
            case PartitionCommand::INGEST_PARTITION:
                ingestPartition(metadata_snapshot, command, local_context);
                break;
            default:
                throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute any PARTITION commands except INGEST_PARTITON");
        }
    }

    return {};
}

bool StorageCloudMergeTree::checkStagedParts()
{
    if (getInMemoryMetadataPtr()->hasUniqueKey())
        return true;

    auto catalog = getContext()->getCnchCatalog();
    auto ts = getContext()->getTimestamp();
    auto cnch_table = catalog->tryGetTableByUUID(*getContext(), UUIDHelpers::UUIDToString(getStorageUUID()), ts);

    /// get all the partitions of committed staged parts
    auto staged_parts = catalog->getStagedParts(cnch_table, ts);
    staged_parts = CnchPartsHelper::calcVisibleParts(staged_parts, /*collect_on_chain*/ false);
    size_t num_to_publish = 0;
    for (auto & part : staged_parts)
    {
        if (TxnTimestamp(ts).toMillisecond() - part->commit_time.toMillisecond()
            > getSettings()->staged_part_lifetime_threshold_ms_to_block_kafka_consume)
        {
            num_to_publish++;
            LOG_DEBUG(
                log,
                "The part: {}, commit time: {} ms, current time: {} ms, staged_part_lifetime_threshold_ms_to_block_kafka_consume: {} ms.",
                part->name,
                part->commit_time.toMillisecond(),
                TxnTimestamp(ts).toMillisecond(),
                getSettings()->staged_part_lifetime_threshold_ms_to_block_kafka_consume);
        }
    }
    if (num_to_publish == 0)
        return true;
    LOG_DEBUG(
        log,
        "There are {} parts need to be published which are commited before {} ms ago.",
        staged_parts.size() << getSettings()->staged_part_lifetime_threshold_ms_to_block_kafka_consume);
    return false;
}

CloudMergeTreeDedupWorker * StorageCloudMergeTree::getDedupWorker()
{
    if (!dedup_worker)
        throw Exception("DedupWorker is not created", ErrorCodes::LOGICAL_ERROR);
    return dedup_worker.get();
}

MutationCommands StorageCloudMergeTree::getFirstAlterMutationCommandsForPart(const DataPartPtr & /*part*/) const
{
    return {};
}

StoragePolicyPtr StorageCloudMergeTree::getStoragePolicy(StorageLocation location) const
{
    String policy_name = (location == StorageLocation::MAIN ?
        getSettings()->storage_policy :
        getContext()->getCnchAuxilityPolicyName());
    return getContext()->getStoragePolicy(policy_name);
}

const String& StorageCloudMergeTree::getRelativeDataPath(StorageLocation location) const
{
    return location == StorageLocation::MAIN ?
        MergeTreeMetaBase::getRelativeDataPath(location) :
        relative_auxility_storage_path;
}

ASTs StorageCloudMergeTree::convertBucketNumbersToAstLiterals(const ASTPtr where_expression, ContextPtr local_context) const
{
    ASTs result;
    ASTPtr where_expression_copy = where_expression->clone();
    const Settings & settings = local_context->getSettingsRef();
    auto metadata_snapshot = getInMemoryMetadataPtr();
    if (settings.optimize_skip_unused_shards && where_expression && isBucketTable() && metadata_snapshot->getColumnsForClusterByKey().size() == 1 && !required_bucket_numbers.empty())
    {

        Block sample_block = metadata_snapshot->getSampleBlock();
        NamesAndTypesList source_columns = sample_block.getNamesAndTypesList();

        auto syntax_result = TreeRewriter(local_context).analyze(where_expression_copy, source_columns);
        ExpressionActionsPtr const_actions = ExpressionAnalyzer{where_expression_copy, syntax_result, local_context}.getConstActions();

        // get required source columns
        Names required_source_columns = syntax_result->requiredSourceColumns();
        NameSet required_source_columns_set = NameSet(required_source_columns.begin(), required_source_columns.end());

        // Delete all columns that are not required
        for (const auto & delete_column : sample_block.getNamesAndTypesList())
        {
            if (!required_source_columns_set.contains(delete_column.name))
            {
                sample_block.erase(delete_column.name);
            }
        }

        const_actions->execute(sample_block);

        //replace constant values as literals in AST using visitor
        if (sample_block)
        {
            InDepthNodeVisitor<ReplacingConstantExpressionsMatcher, true> visitor(sample_block);
            visitor.visit(where_expression_copy);
        }

        // Increment limit so that when limit reaches 0, it means that the limit has been exceeded
        size_t limit = settings.optimize_skip_unused_shards_limit + 1;

        const auto & blocks = evaluateExpressionOverConstantCondition(where_expression_copy, metadata_snapshot->getClusterByKey().expression, limit);

        if (!limit)
        {
            LOG_INFO(
                log,
                "Number of values for cluster_by key exceeds optimize_skip_unused_shards_limit = "
                    + std::to_string(settings.optimize_skip_unused_shards_limit)
                    + ", try to increase it, but note that this may increase query processing time.");
        }
        LOG_TRACE(
                log,
                "StorageCloudMergeTree::convertBucketNumbersToAstLiterals blocks.size() = "
                    + std::to_string(blocks->size()));
        if (blocks)
        {
            for (const auto & block : *blocks)
            {
                // Get bucket number of this single value from the IN set
                Block block_copy = block;
                prepareBucketColumn(block_copy, metadata_snapshot->getColumnsForClusterByKey(), metadata_snapshot->getSplitNumberFromClusterByKey(), metadata_snapshot->getWithRangeFromClusterByKey(), metadata_snapshot->getBucketNumberFromClusterByKey(), local_context);
                auto bucket_number = block_copy.getByPosition(block_copy.columns() - 1).column->getInt(0); // this block only contains one row

                // Create ASTLiteral using the bucket column in the block if it can be found in required_bucket_numbers
                if (settings.optimize_skip_unused_shards_rewrite_in && required_bucket_numbers.contains(bucket_number))
                {
                    const ColumnWithTypeAndName & col = block_copy.getByName(metadata_snapshot->getColumnsForClusterByKey()[0]);
                    Field field;
                    col.column->get(0, field);
                    auto ast = std::make_shared<ASTLiteral>(field);
                    result.push_back(ast);
                }
            }
        }
    }
    return result;
}


}
