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

#include <DataTypes/DataTypeEnum.h>
#include <Storages/StorageCnchMergeTree.h>

#include <Catalog/Catalog.h>
#include <CloudServices/CnchCreateQueryHelper.h>
#include <CloudServices/CnchMergeMutateThread.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/CnchServerResource.h>
#include <CloudServices/CnchWorkerClient.h>
#include <Core/Protocol.h>
#include <Core/Settings.h>
#include <DaemonManager/DaemonManagerClient.h>
#include <DataStreams/RemoteBlockInputStream.h>
#include <DataTypes/DataTypeTuple.h>
#include <Databases/DatabaseOnDisk.h>
#include <IO/ConnectionTimeoutsContext.h>
#include <Interpreters/ClusterProxy/SelectStreamFactory.h>
#include <Interpreters/ClusterProxy/executeQuery.h>
#include <Interpreters/Context.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/SelectQueryOptions.h>
#include <Interpreters/TranslateQualifiedNamesVisitor.h>
#include <Interpreters/VirtualWarehousePool.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Interpreters/inplaceBlockConversions.h>
#include <Interpreters/trySetVirtualWarehouse.h>
#include <MergeTreeCommon/CnchBucketTableCommon.h>
#include <MergeTreeCommon/MergeTreeDataDeduper.h>
#include <Parsers/ASTCheckQuery.h>
#include <Parsers/ASTOptimizeQuery.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/queryToString.h>
#include <Storages/AlterCommands.h>
#include <Storages/MergeTree/CloudMergeTreeBlockOutputStream.h>
#include <Storages/MergeTree/CnchAttachProcessor.h>
#include <Storages/MergeTree/PartitionPruner.h>
#include <Storages/PartitionCommands.h>
#include <Storages/StorageMaterializedView.h>
#include <Storages/VirtualColumnUtils.h>
#include <Transaction/CnchLock.h>
#include <Transaction/getCommitted.h>

#include <Catalog/DataModelPartWrapper_fwd.h>
#include <CloudServices/commitCnchParts.h>
#include <Core/NamesAndTypes.h>
#include <Core/QueryProcessingStage.h>
#include <Interpreters/TreeRewriter.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTPartition.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Processors/Sources/NullSource.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/ReadFromPreparedSource.h>
#include <Storages/MergeTree/MergeTreePartition.h>
#include <Storages/SelectQueryInfo.h>
#include <Transaction/Actions/DDLAlterAction.h>
#include <brpc/controller.h>
#include <Common/Exception.h>
#include <Common/parseAddress.h>
#include <common/logger_useful.h>


namespace ProfileEvents
{
extern const Event CatalogTime;
extern const Event TotalPartitions;
extern const Event PrunedPartitions;
extern const Event SelectedParts;
}

namespace DB
{
namespace ErrorCodes
{
    extern const int ILLEGAL_COLUMN;
    extern const int LOGICAL_ERROR;
    extern const int INDEX_NOT_USED;
    extern const int VIRTUAL_WAREHOUSE_NOT_FOUND;
    extern const int SUPPORT_IS_DISABLED;
    extern const int NO_SUCH_DATA_PART;
    extern const int BUCKET_TABLE_ENGINE_MISMATCH;
    extern const int INCOMPATIBLE_COLUMNS;
    extern const int CNCH_LOCK_ACQUIRE_FAILED;
    extern const int ALTER_OF_COLUMN_IS_FORBIDDEN;
    extern const int READONLY_SETTING;
}

/// Get basic select query to read from prepared pipe: remove prewhere, sampling, offset, final
static ASTPtr getBasicSelectQuery(const ASTPtr & original_query)
{
    auto query = original_query->clone();
    auto & select = query->as<ASTSelectQuery &>();
    auto & tables_in_select_query = select.refTables()->as<ASTTablesInSelectQuery &>();
    if (tables_in_select_query.children.empty())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Tables list is empty, it's a bug");
    auto & tables_element = tables_in_select_query.children[0]->as<ASTTablesInSelectQueryElement &>();
    if (!tables_element.table_expression)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "There is no table expression, it's a bug");
    tables_element.table_expression->as<ASTTableExpression &>().final = false;
    tables_element.table_expression->as<ASTTableExpression &>().sample_size = nullptr;
    tables_element.table_expression->as<ASTTableExpression &>().sample_offset = nullptr;

    /// TODO @canh: can we just throw prewhere away?
    if (select.prewhere() && select.where())
        select.setExpression(ASTSelectQuery::Expression::WHERE, makeASTFunction("and", select.where(), select.prewhere()));
    else if (select.prewhere())
        select.setExpression(ASTSelectQuery::Expression::WHERE, select.prewhere()->clone());
    select.setExpression(ASTSelectQuery::Expression::PREWHERE, nullptr);
    return query;
}

StorageCnchMergeTree::~StorageCnchMergeTree()
{
    try
    {
        shutdown();
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

StorageCnchMergeTree::StorageCnchMergeTree(
    const StorageID & table_id_,
    const String & relative_data_path_,
    const StorageInMemoryMetadata & metadata_,
    bool attach_,
    ContextMutablePtr context_,
    const String & date_column_name_,
    const MergeTreeMetaBase::MergingParams & merging_params_,
    std::unique_ptr<MergeTreeSettings> settings_)
    : MergeTreeMetaBase(
        table_id_,
        relative_data_path_.empty() ? UUIDHelpers::UUIDToString(table_id_.uuid) : relative_data_path_,
        metadata_,
        context_,
        date_column_name_,
        merging_params_,
        std::move(settings_),
        false,
        attach_,
        [](const String &) {})
    , CnchStorageCommonHelper(table_id_, getDatabaseName(), getTableName())
{
    relative_auxility_storage_path = fs::path("auxility_store") / UUIDHelpers::UUIDToString(table_id_.uuid) / "";
    format_version = MERGE_TREE_CHCH_DATA_STORAGTE_VERSION;
}

QueryProcessingStage::Enum StorageCnchMergeTree::getQueryProcessingStage(
    ContextPtr local_context, QueryProcessingStage::Enum, const StorageMetadataPtr &, SelectQueryInfo &) const
{
    const auto & settings = local_context->getSettingsRef();
    if (auto worker_group = local_context->tryGetCurrentWorkerGroup())
    {
        size_t num_workers = worker_group->getShardsInfo().size();
        size_t result_size = (num_workers * settings.max_parallel_replicas);
        return result_size == 1 ? QueryProcessingStage::Complete : QueryProcessingStage::WithMergeableState;
    }
    else
    {
        return QueryProcessingStage::WithMergeableState;
    }
}

void StorageCnchMergeTree::startup()
{
}

void StorageCnchMergeTree::shutdown()
{
}

Pipe StorageCnchMergeTree::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    size_t max_block_size,
    unsigned num_streams)
{
    QueryPlan plan;
    read(plan, column_names, metadata_snapshot, query_info, local_context, processed_stage, max_block_size, num_streams);
    return plan.convertToPipe(
        QueryPlanOptimizationSettings::fromContext(local_context), BuildQueryPipelineSettings::fromContext(local_context));
}

void StorageCnchMergeTree::read(
    QueryPlan & query_plan,
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    const size_t /*max_block_size*/,
    const unsigned /*num_streams*/)
{
    auto prepare_result = prepareReadContext(column_names, metadata_snapshot, query_info, local_context);
    Block header = InterpreterSelectQuery(query_info.query, local_context, SelectQueryOptions(processed_stage)).getSampleBlock();

    auto worker_group = local_context->getCurrentWorkerGroup();
    /// Return directly (with correct header) if no shard read from
    if (!worker_group || worker_group->getShardsInfo().empty())
    {
        Pipe pipe(std::make_shared<NullSource>(header));
        auto read_from_pipe = std::make_unique<ReadFromPreparedSource>(std::move(pipe));
        read_from_pipe->setStepDescription("Read from NullSource (CnchMergeTree)");
        query_plan.addStep(std::move(read_from_pipe));
        return;
    }

    /// If no parts to read from - execute locally, must make sure that all stages are executed
    /// because CnchMergeTree is a high order storage
    if (prepare_result.parts.empty())
    {
        /// Stage 1: read from source table, just assume we read everything
        const auto & source_columns = query_info.syntax_analyzer_result->required_source_columns;
        auto fetch_column_header = Block(NamesAndTypes{source_columns.begin(), source_columns.end()});
        Pipe pipe(std::make_shared<NullSource>(std::move(fetch_column_header)));
        /// Stage 2: (partial) aggregation and projection if any
        auto query = getBasicSelectQuery(query_info.query);
        InterpreterSelectQuery(query, local_context, std::move(pipe), SelectQueryOptions(processed_stage)).buildQueryPlan(query_plan);
        return;
    }

    LOG_TRACE(log, "Original query before rewrite: {}", queryToString(query_info.query));
    auto modified_query_ast = rewriteSelectQuery(query_info.query, getDatabaseName(), prepare_result.local_table_name);

    const Scalars & scalars = local_context->hasQueryContext() ? local_context->getQueryContext()->getScalars() : Scalars{};

    ClusterProxy::SelectStreamFactory select_stream_factory = ClusterProxy::SelectStreamFactory(
        header,
        processed_stage,
        StorageID::createEmpty(), /// Don't check whether table exists in cnch-worker
        scalars,
        false,
        local_context->getExternalTables());

    ClusterProxy::executeQuery(query_plan, select_stream_factory, log, modified_query_ast, local_context, worker_group);

    if (!query_plan.isInitialized())
        throw Exception("Pipeline is not initialized", ErrorCodes::LOGICAL_ERROR);
}

PrepareContextResult StorageCnchMergeTree::prepareReadContext(
    const Names & column_names, const StorageMetadataPtr & metadata_snapshot, SelectQueryInfo & query_info, ContextPtr & local_context)
{
    auto txn = local_context->getCurrentTransaction();
    if (local_context->getServerType() == ServerType::cnch_server && txn && txn->isReadOnly())
        local_context->getCnchTransactionCoordinator().touchActiveTimestampByTable(getStorageID(), txn);

    metadata_snapshot->check(column_names, getVirtuals(), getStorageID());

    auto worker_group = local_context->getCurrentWorkerGroup();
    healthCheckForWorkerGroup(local_context, worker_group);

    auto parts = selectPartsToRead(column_names, local_context, query_info);
    LOG_INFO(log, "Number of parts to read: {}", parts.size());

    if (metadata_snapshot->hasUniqueKey() && !parts.empty())
    {
        // Previous, we need to sort parts since the partial part and base part are not guarantee to be together,
        // the getDeleteBitmapMetaForParts function needs to handle the partial->base chain.
        // Now, we can remove sort logic since we don't expand the partial->base part chain after chain construction,
        // only partial part is useful in getDeleteBitmapMetaForParts for unique table.
        getDeleteBitmapMetaForParts(parts, local_context, local_context->getCurrentTransactionID());
    }

    String local_table_name = getCloudTableName(local_context);
    auto bucket_numbers = getRequiredBucketNumbers(query_info, local_context);
    collectResource(local_context, parts, local_table_name, bucket_numbers);

    return {std::move(local_table_name), std::move(parts)};
}

Strings StorageCnchMergeTree::selectPartitionsByPredicate(
    const SelectQueryInfo & query_info,
    std::vector<std::shared_ptr<MergeTreePartition>> & partition_list,
    const Names & column_names_to_return,
    ContextPtr local_context) const
{
    /// Coarse grained partition prunner: filter out the partition which will definately not sastify the query predicate. The benefit
    /// is 2-folded: (1) we can prune data parts and (2) we can reduce numbers of calls to catalog to get parts 's metadata.
    /// Note that this step still leaves false-positive parts. For example, the partition key is `toMonth(date)` and the query
    /// condition is `date > '2022-02-22' and date < '2022-03-22'` then this step won't eliminate any partition.

    /// The partition pruning rules come from 3 types:
    /// (1) TTL
    /// (2) Columns in predicate that exactly match the partition key
    /// (3) `_partition_id` or `_partition_value` if they're in predicate

    /// (1) Prune partition by partition level TTL
    TTLTableDescription table_ttl = getInMemoryMetadata().getTableTTLs();
    if (table_ttl.definition_ast)
    {
        TxnTimestamp start_ts = local_context->getCurrentTransactionID();
        time_t query_time = start_ts.toSecond();
        size_t prev_sz = partition_list.size();
        std::erase_if(partition_list, [&](const auto & partition) {
            time_t metadata_ttl = getTTLForPartition(*partition);
            return metadata_ttl && metadata_ttl < query_time;
        });
        if (partition_list.size() < prev_sz)
            LOG_DEBUG(log, "TTL rules dropped {} expired partitions", prev_sz - partition_list.size());
    }

    const auto partition_key = MergeTreePartition::adjustPartitionKey(getInMemoryMetadataPtr(), local_context);
    const auto & partition_key_expr = partition_key.expression;
    const auto & partition_key_sample = partition_key.sample_block;
    if (local_context->getSettingsRef().enable_partition_prune && partition_key_sample.columns() > 0)
    {
        /// (2) Prune partitions if there's a column in predicate that exactly match the partition key
        Names partition_key_columns;
        for (const auto & name : partition_key_sample)
        {
            partition_key_columns.emplace_back(name.name);
        }

        KeyCondition partition_condition(query_info, local_context, partition_key_columns, partition_key_expr);
        DataTypes result;
        result.reserve(partition_key_sample.getDataTypes().size());
        for (const auto & data_type : partition_key_sample.getDataTypes())
        {
            result.push_back(DataTypeFactory::instance().get(data_type->getName(), data_type->getFlags()));
        }
        size_t prev_sz = partition_list.size();
        std::erase_if(partition_list, [&](const auto & partition) {
            const auto & partition_value = partition->value;
            std::vector<FieldRef> index_value(partition_value.begin(), partition_value.end());
            auto res = partition_condition.mayBeTrueInRange(partition_key_columns.size(), index_value.data(), index_value.data(), result);
            LOG_TRACE(
                log,
                "Key condition {} is {} in [ ({}) - ({}) )",
                partition_condition.toString(),
                res,
                fmt::join(index_value, " "),
                fmt::join(index_value, " "));
            return !res;
        });
        if (partition_list.size() < prev_sz)
            LOG_DEBUG(log, "Query predicates on physical columns droped {} partitions", prev_sz - partition_list.size());

        /// (3) Prune partitions if there's `_partition_id` or `_partition_value` in query predicate
        bool has_partition_column = std::any_of(column_names_to_return.begin(), column_names_to_return.end(), [](const auto & name) {
            return name == "_partition_id" || name == "_partition_value";
        });

        if (has_partition_column && !partition_list.empty())
        {
            Block partition_block = getBlockWithVirtualPartitionColumns(partition_list);
            ASTPtr expression_ast;

            /// Generate valid expressions for filtering
            VirtualColumnUtils::prepareFilterBlockWithQuery(query_info.query, local_context, partition_block, expression_ast);

            /// Generate list of partition id that fit the query predicate
            NameSet partition_ids;
            if (expression_ast)
            {
                VirtualColumnUtils::filterBlockWithQuery(query_info.query, partition_block, local_context, expression_ast);
                partition_ids = VirtualColumnUtils::extractSingleValueFromBlock<String>(partition_block, "_partition_id");
                /// Prunning
                prev_sz = partition_list.size();
                std::erase_if(partition_list, [this, &partition_ids](const auto & partition) {
                    return partition_ids.find(partition->getID(*this)) == partition_ids.end();
                });
                if (partition_list.size() < prev_sz)
                    LOG_DEBUG(
                        log,
                        "Query predicates on `_partition_id` and `_partition_value` droped {} partitions",
                        prev_sz - partition_list.size());
            }
        }
    }
    Strings res_partitions;
    for (const auto & partition : partition_list)
        res_partitions.emplace_back(partition->getID(*this));

    return res_partitions;
}

static Block getBlockWithPartColumn(ServerDataPartsVector & parts)
{
    auto column = ColumnString::create();

    for (const auto & part : parts)
        column->insert(part->part_model_wrapper->name);

    return Block{ColumnWithTypeAndName(std::move(column), std::make_shared<DataTypeString>(), "_part")};
}

time_t StorageCnchMergeTree::getTTLForPartition(const MergeTreePartition & partition) const
{
    auto metadata_snapshot = getInMemoryMetadataPtr();
    TTLTableDescription table_ttl = metadata_snapshot->getTableTTLs();
    if (!table_ttl.definition_ast)
        return 0;

    /// Construct a block consists of partition keys then compute ttl values according to this block
    const auto & partition_key_sample = metadata_snapshot->getPartitionKey().sample_block;
    /// We can only compute ttl at this point if partition key expression includes all columns in TTL expression
    const auto & required_columns = table_ttl.rows_ttl.expression->getRequiredColumns();
    if (std::any_of(
            required_columns.begin(),
            required_columns.end(),
            [&partition_key_sample](const auto & name) { return !partition_key_sample.has(name); }))
        return 0;

    MutableColumns columns = partition_key_sample.cloneEmptyColumns();
    const auto & partition_key = partition.value;
    /// This can happen when ALTER query is implemented improperly; finish ALTER query should bypass this check.
    if (columns.size() != partition_key.size())
        throw Exception(
            ErrorCodes::LOGICAL_ERROR,
            "Partition key columns definition missmatch between inmemory and metastore, this is a bug, expect block ({}), got values "
            "({})\n",
            partition_key_sample.dumpNames(),
            fmt::join(partition_key, ", "));
    for (size_t i = 0; i < partition_key.size(); ++i)
        columns[i]->insert(partition_key[i]);

    auto names_and_types_list = partition_key_sample.getNamesAndTypesList();
    Block block;
    auto nt_iter = names_and_types_list.begin();
    auto column_iter = columns.begin();
    while (nt_iter != names_and_types_list.end() && column_iter != columns.end())
    {
        block.insert({std::move(*column_iter), nt_iter->type, nt_iter->name});
        nt_iter++;
        column_iter++;
    }


    table_ttl.rows_ttl.expression->execute(block);

    const auto & current = block.getByName(table_ttl.rows_ttl.result_column);

    const IColumn * column = current.column.get();

    if (column->size() > 1)
        throw Exception("Cannot get TTL value from table ttl ast since there are multiple ttl value", ErrorCodes::LOGICAL_ERROR);

    if (const ColumnUInt16 * column_date = typeid_cast<const ColumnUInt16 *>(column))
    {
        const auto & date_lut = DateLUT::instance();
        return date_lut.fromDayNum(DayNum(column_date->getElement(0)));
    }
    else if (const ColumnUInt32 * column_date_time = typeid_cast<const ColumnUInt32 *>(column))
    {
        return column_date_time->getElement(0);
    }
    else
        throw Exception("Unexpected type of result ttl column", ErrorCodes::LOGICAL_ERROR);
}

void StorageCnchMergeTree::filterPartsByPartition(
    ServerDataPartsVector & parts, ContextPtr local_context, const SelectQueryInfo & query_info, const Names & column_names_to_return) const
{
    /// Fine grained parts pruning by:
    /// (1) partition min-max
    /// (2) min-max index
    /// (3) part name (if _part is in the query) and uuid (todo)
    /// (4) primary key (TODO)
    /// (5) block id for deduped part (TODO)

    const Settings & settings = local_context->getSettingsRef();
    std::optional<PartitionPruner> partition_pruner;
    std::optional<KeyCondition> minmax_idx_condition;
    DataTypes minmax_columns_types;
    auto metadata_snapshot = getInMemoryMetadataPtr();

    if (metadata_snapshot->hasPartitionKey())
    {
        const auto & partition_key = metadata_snapshot->getPartitionKey();
        auto minmax_columns_names = getMinMaxColumnsNames(partition_key);
        minmax_columns_types = getMinMaxColumnsTypes(partition_key);

        minmax_idx_condition.emplace(
            query_info,
            local_context,
            minmax_columns_names,
            getMinMaxExpr(partition_key, ExpressionActionsSettings::fromContext(local_context)));
        partition_pruner.emplace(metadata_snapshot, query_info, local_context, false /* strict */);

        if (settings.force_index_by_date && (minmax_idx_condition->alwaysUnknownOrTrue() && partition_pruner->isUseless()))
        {
            String msg = "Neither MinMax index by columns (";
            bool first = true;
            for (const String & col : minmax_columns_names)
            {
                if (first)
                    first = false;
                else
                    msg += ", ";
                msg += col;
            }
            msg += ") nor partition expr is used and setting 'force_index_by_date' is set";

            throw Exception(msg, ErrorCodes::INDEX_NOT_USED);
        }
    }

    /// If `_part` virtual column is requested, we try to use it as an index.
    Block virtual_columns_block = getBlockWithPartColumn(parts);
    bool part_column_queried
        = std::any_of(column_names_to_return.begin(), column_names_to_return.end(), [](const auto & name) { return name == "_part"; });
    if (part_column_queried)
        VirtualColumnUtils::filterBlockWithQuery(query_info.query, virtual_columns_block, local_context);
    auto part_values = VirtualColumnUtils::extractSingleValueFromBlock<String>(virtual_columns_block, "_part");

    size_t prev_sz = parts.size();
    size_t empty = 0, partition_minmax = 0, minmax_idx = 0, part_value = 0;
    std::erase_if(parts, [&](const auto & part) {
        auto base_part = part->getBasePart();
        if (base_part->isEmpty())
        {
            ++empty;
            return true;
        }
        else if (partition_pruner && partition_pruner->canBePruned(*base_part))
        {
            ++partition_minmax;
            return true;
        }
        else if (
            minmax_idx_condition
            && !minmax_idx_condition->checkInHyperrectangle(base_part->minmax_idx()->hyperrectangle, minmax_columns_types).can_be_true)
        {
            ++minmax_idx;
            return true;
        }
        else if (part_values.find(part->name()) == part_values.end())
        {
            ++part_value;
            return true;
        }

        return false;
    });

    if (parts.size() < prev_sz)
        LOG_DEBUG(
            log,
            "Parts pruning rules dropped {} parts, include {} empty parts, {} parts by partition minmax, {} parts by minmax index, {} "
            "parts by part value",
            prev_sz - parts.size(),
            empty,
            partition_minmax,
            minmax_idx,
            part_value);
}

/// Add related tables for active timestamps
static void touchActiveTimestampForInsertSelectQuery(const ASTInsertQuery & insert_query, ContextPtr local_context)
{
    if (!insert_query.select)
        return;

    auto txn = local_context->getCurrentTransaction();
    if (!txn)
        return;

    auto & txn_coordinator = local_context->getCnchTransactionCoordinator();
    auto current_database = local_context->getCurrentDatabase();

    ASTs related_tables;
    bool has_table_func = false;
    if (auto * select_query = insert_query.select->as<ASTSelectQuery>())
        select_query->collectAllTables(related_tables, has_table_func);
    else if (auto * select_with_union = insert_query.select->as<ASTSelectWithUnionQuery>())
        select_with_union->collectAllTables(related_tables, has_table_func);

    for (auto & db_and_table_ast : related_tables)
    {
        DatabaseAndTableWithAlias db_and_table(db_and_table_ast, current_database);
        if (db_and_table.database == "system" || db_and_table.database == "default")
            continue;

        if (auto table = DatabaseCatalog::instance().tryGetTable(StorageID{db_and_table.database, db_and_table.table}, local_context))
            txn_coordinator.touchActiveTimestampByTable(table->getStorageID(), txn);
    }
}

static String replaceMaterializedViewQuery(StorageMaterializedView * mv, const String & table_suffix)
{
    auto query = mv->getCreateTableSql();

    ParserCreateQuery parser;
    ASTPtr ast = parseQuery(parser, query.data(), query.data() + query.size(), "", 0, DBMS_DEFAULT_MAX_PARSER_DEPTH);

    auto & create_query = ast->as<ASTCreateQuery &>();
    create_query.table += "_" + table_suffix;
    create_query.to_table_id.table_name += "_" + table_suffix;

    auto & inner_query = create_query.select->list_of_selects->children.at(0);
    if (!inner_query)
        throw Exception("Select query is necessary for mv table", ErrorCodes::LOGICAL_ERROR);

    auto & select_query = inner_query->as<ASTSelectQuery &>();
    select_query.replaceDatabaseAndTable(
        mv->getInMemoryMetadataPtr()->select.select_table_id.database_name,
        mv->getInMemoryMetadataPtr()->select.select_table_id.table_name + "_" + table_suffix);

    /// Remark: this `getObjectDefinitionFromCreateQuery` may cause issue, refer to `getTableDefinitionFromCreateQuery` in cnch-dev branch if issue happens
    return getObjectDefinitionFromCreateQuery(ast, false);
}

String StorageCnchMergeTree::extractTableSuffix(const String & gen_table_name)
{
    return gen_table_name.substr(gen_table_name.find_last_of('_') + 1);
}

Names StorageCnchMergeTree::genViewDependencyCreateQueries(
    const StorageID & storage_id, ContextPtr local_context, const String & table_suffix)
{
    Names create_view_sqls;
    std::set<StorageID> view_dependencies;
    auto storage = DatabaseCatalog::instance().getTable(storage_id, local_context);
    auto start_time = local_context->getTimestamp();

    auto catalog_client = local_context->getCnchCatalog();
    if (!catalog_client)
        throw Exception("Get catalog client failed", ErrorCodes::LOGICAL_ERROR);

    auto all_views_from_catalog = catalog_client->getAllViewsOn(*local_context, storage, start_time);
    if (all_views_from_catalog.empty())
        return create_view_sqls;

    for (auto & view : all_views_from_catalog)
        view_dependencies.emplace(view->getStorageID());

    for (const auto & dependence : view_dependencies)
    {
        auto table = DatabaseCatalog::instance().getTable(dependence, local_context);
        if (!table)
        {
            LOG_WARNING(log, "Table {} not found", dependence.getNameForLogs());
            continue;
        }

        if (auto * mv = dynamic_cast<StorageMaterializedView *>(table.get()))
        {
            auto target_table = mv->tryGetTargetTable();
            if (!target_table)
            {
                LOG_WARNING(log, "Target table for {} not exist", mv->getTargetTableName());
                continue;
            }

            /// target table should be CnchMergeTree
            auto * cnch_merge = dynamic_cast<StorageCnchMergeTree *>(target_table.get());
            if (!cnch_merge)
            {
                LOG_WARNING(log, "Table type not matched for {}, CnchMergeTree is expected", target_table->getTableName());
                continue;
            }
            auto create_target_query = target_table->getCreateTableSql();
            bool enable_staging_area = cnch_merge->getInMemoryMetadataPtr()->hasUniqueKey()
                && bool(local_context->getSettingsRef().enable_staging_area_for_write);
            auto create_local_target_query = getCreateQueryForCloudTable(
                create_target_query,
                cnch_merge->getTableName() + "_" + table_suffix,
                local_context,
                enable_staging_area,
                cnch_merge->getStorageID());
            create_view_sqls.emplace_back(create_local_target_query);
            create_view_sqls.emplace_back(replaceMaterializedViewQuery(mv, table_suffix));
        }

        /// TODO: Check cascade view dependency
    }

    return create_view_sqls;
}

BlockOutputStreamPtr
StorageCnchMergeTree::write(const ASTPtr & query, const StorageMetadataPtr & metadata_snapshot, ContextPtr local_context)
{
    bool enable_staging_area = metadata_snapshot->hasUniqueKey() && bool(local_context->getSettingsRef().enable_staging_area_for_write);
    if (enable_staging_area)
        LOG_DEBUG(log, "enable staging area for write");

    auto modified_query_ast = query->clone();
    auto & insert_query = modified_query_ast->as<ASTInsertQuery &>();

    if (insert_query.table_id.database_name.empty())
        insert_query.table_id.database_name = local_context->getCurrentDatabase();

    if (insert_query.select)
        touchActiveTimestampForInsertSelectQuery(insert_query, local_context);

    if (insert_query.select || insert_query.in_file)
    {
        if (insert_query.select && local_context->getSettingsRef().restore_table_expression_in_distributed)
        {
            RestoreTableExpressionsVisitor::Data data;
            data.database = local_context->getCurrentDatabase();
            RestoreTableExpressionsVisitor(data).visit(insert_query.select);
        }

        auto generated_tb_name = getCloudTableName(local_context);
        auto local_table_name = generated_tb_name + "_write";
        insert_query.table_id.table_name = local_table_name;

        auto create_local_tb_query = getCreateQueryForCloudTable(getCreateTableSql(), local_table_name, local_context, enable_staging_area);

        String query_statement = queryToString(insert_query);

        WorkerGroupHandle worker_group = local_context->getCurrentWorkerGroup();

        /// TODO: currently use only one write worker to do insert, use multiple write workers when distributed write is support
        const Settings & settings = local_context->getSettingsRef();
        int max_retry = 2, retry = 0;
        auto num_of_workers = worker_group->getShardsInfo().size();
        if (!num_of_workers)
            throw Exception("No heathy worker available", ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND);

        std::size_t index = std::hash<String>{}(local_context->getCurrentQueryId() + std::to_string(retry)) % num_of_workers;
        const auto * write_shard_ptr = &(worker_group->getShardsInfo().at(index));

        // TODO: healthy check by rpc
        if (settings.query_worker_fault_tolerance)
        {
            ConnectionTimeouts connection_timeouts = DB::ConnectionTimeouts::getTCPTimeoutsWithoutFailover(local_context->getSettingsRef());

            // Perform health check for selected write_shard and retry for 2 more times if there are enough write workers.
            while (true)
            {
                LOG_TRACE(log, "Health check for worker: {}", write_shard_ptr->worker_id);

                try
                {
                    // The checking task checks whether the current connection is connected or can connect.
                    auto entry = write_shard_ptr->pool->get(connection_timeouts, &settings, true);
                    Connection * conn = &(*entry);
                    conn->tryConnect(connection_timeouts);
                    break;
                }
                catch (const NetException &)
                {
                    // Don't throw network exception, instead remove the unhealthy worker unless no more available workers or reach retry limit.
                    if (++retry > max_retry)
                        throw Exception(
                            "Cannot find healthy worker after " + std::to_string(max_retry) + " times retries.",
                            ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND);

                    index = (index + 1) % num_of_workers;
                    write_shard_ptr = &(worker_group->getShardsInfo().at(index));
                }
            }
        }

        LOG_DEBUG(log, "Will send create query: {} to target worker: {}", create_local_tb_query, write_shard_ptr->worker_id);
        auto worker_client = worker_group->getWorkerClients().at(index);

        worker_client->sendCreateQueries(local_context, {create_local_tb_query});

        auto table_suffix = extractTableSuffix(generated_tb_name);
        Names dependency_create_queries = genViewDependencyCreateQueries(getStorageID(), local_context, table_suffix + "_write");
        for (const auto & dependency_create_query : dependency_create_queries)
        {
            LOG_DEBUG(log, "Will send create query {}", dependency_create_query);
        }
        worker_client->sendCreateQueries(local_context, dependency_create_queries);

        /// Ensure worker session local_context resource could be released
        if (auto session_resource = local_context->tryGetCnchServerResource())
        {
            std::vector<size_t> index_values{index};
            session_resource->setWorkerGroup(std::make_shared<WorkerGroupHandleImpl>(*worker_group, index_values));
        }

        LOG_DEBUG(log, "Prepare execute insert query: {}", query_statement);
        /// TODO: send insert query by rpc.
        sendQueryPerShard(local_context, query_statement, *write_shard_ptr, true);

        return nullptr;
    }
    else
    {
        return std::make_shared<CloudMergeTreeBlockOutputStream>(*this, metadata_snapshot, local_context, enable_staging_area);
    }
}

HostWithPortsVec StorageCnchMergeTree::getWriteWorkers(const ASTPtr & /**/, ContextPtr local_context)
{
    String vw_name = local_context->getSettingsRef().virtual_warehouse_write;
    if (vw_name.empty())
        vw_name = getSettings()->cnch_vw_write;

    if (vw_name.empty())
        throw Exception("Expected a nonempty vw name. Please specify it in query or table settings", ErrorCodes::BAD_ARGUMENTS);

    // No fixed workers for insertion, pick one randomly from worker pool
    auto vw_handle = local_context->getVirtualWarehousePool().get(vw_name);
    HostWithPortsVec res;
    for (const auto & [_, wg] : vw_handle->getAll())
    {
        auto wg_hosts = wg->getHostWithPortsVec();
        res.insert(res.end(), wg_hosts.begin(), wg_hosts.end());
    }
    return res;
}

bool StorageCnchMergeTree::optimize(
    const ASTPtr & query, const StorageMetadataPtr &, const ASTPtr & partition, bool final, bool, const Names &, ContextPtr query_context)
{
    auto & optimize_query = query->as<ASTOptimizeQuery &>();
    auto enable_try = optimize_query.enable_try;
    if (optimize_query.final)
        throw Exception(ErrorCodes::NOT_IMPLEMENTED, "FINAL is disabled because it is dangerous");

    auto bg_thread = query_context->tryGetCnchBGThread(CnchBGThreadType::MergeMutate, getStorageID());
    auto timeout_ms = query_context->getSettingsRef().max_execution_time.totalMilliseconds();

    if (!bg_thread)
    {
        auto daemon_manage_client = getContext()->getDaemonManagerClient();
        auto enable_sync = query_context->getSettingsRef().mutations_sync;
        auto partition_id = partition ? getPartitionIDFromQuery(partition, query_context) : "all";
        daemon_manage_client->forwardOptimizeQuery(getStorageID(), partition_id, enable_try, enable_sync, timeout_ms);
        return true;
    }

    Strings partition_ids;

    if (!partition)
        partition_ids = query_context->getCnchCatalog()->getPartitionIDs(shared_from_this(), query_context.get());
    else
        partition_ids.push_back(getPartitionIDFromQuery(partition, query_context));

    auto istorage = shared_from_this();
    auto * merge_mutate_thread = dynamic_cast<CnchMergeMutateThread *>(bg_thread.get());
    std::vector<String> task_ids;
    for (const auto & partition_id : partition_ids)
    {
        auto task_id = merge_mutate_thread->triggerPartMerge(istorage, partition_id, final, enable_try, false);
        if (!task_id.empty())
            task_ids.push_back(task_id);
    }

    if (query_context->getSettingsRef().mutations_sync != 0)
        merge_mutate_thread->waitTasksFinish(task_ids, timeout_ms);

    return true;
}

CheckResults StorageCnchMergeTree::checkDataCommon(const ASTPtr & query, ContextPtr local_context, ServerDataPartsVector & parts) const
{
    String local_table_name = getCloudTableName(local_context);

    auto create_table_query = getCreateQueryForCloudTable(getCreateTableSql(), local_table_name, local_context, true);

    if (local_context->getCnchCatalog())
    {
        if (const auto & check_query = query->as<ASTCheckQuery &>(); check_query.partition)
        {
            String partition_id = getPartitionIDFromQuery(check_query.partition, local_context);
            parts = local_context->getCnchCatalog()->getServerDataPartsInPartitions(
                shared_from_this(), {partition_id}, local_context->getCurrentTransactionID(), nullptr);
        }
        else
        {
            parts = getAllParts(local_context);
        }
    }

    if (parts.empty())
        return {};

    MutableMergeTreeDataPartsCNCHVector cnch_parts;
    cnch_parts.reserve(parts.size());
    std::transform(
        parts.begin(), parts.end(), std::back_inserter(cnch_parts), [this](const auto & part) { return part->toCNCHDataPart(*this); });

    CheckResults results(cnch_parts.size());
    ThreadPool pool(std::min(16UL, cnch_parts.size()));
    for (size_t i = 0; i < cnch_parts.size(); ++i)
    {
        pool.scheduleOrThrow([i, &cnch_parts, &results] {
            String message;
            bool is_passed = false;
            try
            {
                cnch_parts[i]->loadFromFileSystem(false);
                is_passed = true;
                message.clear();
            }
            catch (const Exception & e)
            {
                is_passed = false;
                message = e.message();
            }
            results[i].fs_path = cnch_parts[i]->getFullPath();
            results[i].success = is_passed;
            results[i].failure_message = std::move(message);
        });
    }
    pool.wait();

    return results;
}

CheckResults StorageCnchMergeTree::checkData(const ASTPtr & query, ContextPtr local_context)
{
    ServerDataPartsVector parts;
    return checkDataCommon(query, local_context, parts);
}

ServerDataPartsVector StorageCnchMergeTree::getAllParts(ContextPtr local_context) const
{
    // TEST_START(testlog);
    ServerDataPartsVector all_parts;
    if (local_context->getCnchCatalog())
    {
        TransactionCnchPtr cur_txn = local_context->getCurrentTransaction();
        if (cur_txn->isSecondary())
        {
            /// Get all parts in the partition list
            LOG_DEBUG(log, "Current transaction is secondary transaction, result may include uncommited data");
            all_parts = local_context->getCnchCatalog()->getAllServerDataParts(shared_from_this(), {0}, local_context.get());
            /// Fillter by commited parts and parts written by same explicit transaction
            filterPartsInExplicitTransaction(all_parts, local_context);
        }
        else
        {
            all_parts = local_context->getCnchCatalog()->getAllServerDataParts(
                shared_from_this(), cur_txn->getTransactionID(), local_context.get());
        }
        return CnchPartsHelper::calcVisibleParts(all_parts, false, CnchPartsHelper::getLoggingOption(*local_context));
    }
    // TEST_END(testlog, "Get all parts from Catalog Service");
    LOG_INFO(log, "Number of parts get from catalog: {}", all_parts.size());
    return {};
}

ServerDataPartsVector StorageCnchMergeTree::getAllPartsInPartitions(
    const Names & column_names_to_return, ContextPtr local_context, const SelectQueryInfo & query_info) const
{
    ServerDataPartsVector all_parts;

    // TEST_START(testlog);

    if (local_context->getCnchCatalog())
    {
        TransactionCnchPtr cur_txn = local_context->getCurrentTransaction();
        Stopwatch watch;
        auto partition_list = local_context->getCnchCatalog()->getPartitionList(shared_from_this(), local_context.get());
        // TEST_LOG(testlog, "get partition list.");
        Strings pruned_partitions = selectPartitionsByPredicate(query_info, partition_list, column_names_to_return, local_context);
        // TEST_LOG(testlog, "select partitions by predicate.");
        if (cur_txn->isSecondary())
        {
            /// Get all parts in the partition list
            LOG_DEBUG(log, "Current transaction is secondary transaction, result may include uncommited data");
            all_parts = local_context->getCnchCatalog()->getServerDataPartsInPartitions(
                shared_from_this(), pruned_partitions, {0}, local_context.get());
            /// Fillter by commited parts and parts written by same explicit transaction
            filterPartsInExplicitTransaction(all_parts, local_context);
        }
        else
        {
            all_parts = local_context->getCnchCatalog()->getServerDataPartsInPartitions(
                shared_from_this(), pruned_partitions, local_context->getCurrentTransactionID(), local_context.get());
        }
        // TEST_LOG(testlog, "get dataparts in partitions.");
        LOG_DEBUG(log, "Total number of parts get from bytekv: {}", all_parts.size());
        all_parts = CnchPartsHelper::calcVisibleParts(all_parts, false, CnchPartsHelper::getLoggingOption(*local_context));

        ProfileEvents::increment(ProfileEvents::CatalogTime, watch.elapsedMilliseconds());
        ProfileEvents::increment(ProfileEvents::TotalPartitions, partition_list.size());
        ProfileEvents::increment(ProfileEvents::PrunedPartitions, pruned_partitions.size());
        ProfileEvents::increment(ProfileEvents::SelectedParts, all_parts.size());
    }

    // TEST_END(testlog, "Get pruned parts from Catalog Service");
    LOG_INFO(log, "Number of parts get from catalog: {}", all_parts.size());
    return all_parts;
}


ServerDataPartsVector StorageCnchMergeTree::selectPartsToRead(
    const Names & column_names_to_return, ContextPtr local_context, const SelectQueryInfo & query_info) const
{
    auto parts = getAllPartsInPartitions(column_names_to_return, local_context, query_info);
    filterPartsByPartition(parts, local_context, query_info, column_names_to_return);
    return parts;
}

MergeTreeDataPartsCNCHVector StorageCnchMergeTree::getUniqueTableMeta(TxnTimestamp ts, const Strings & input_partitions)
{
    auto catalog = getContext()->getCnchCatalog();
    auto storage = shared_from_this();

    Strings partitions;
    if (!input_partitions.empty())
        partitions = input_partitions;
    else
        partitions = catalog->getPartitionIDs(storage, nullptr);

    auto cnch_parts = catalog->getServerDataPartsInPartitions(storage, partitions, ts, nullptr);
    auto parts = CnchPartsHelper::calcVisibleParts(cnch_parts, /*collect_on_chain=*/false);

    MergeTreeDataPartsCNCHVector res;
    res.reserve(parts.size());
    for (auto & part : parts)
        res.emplace_back(dynamic_pointer_cast<const MergeTreeDataPartCNCH>(part->getBasePart()->toCNCHDataPart(*this)));

    getDeleteBitmapMetaForParts(res, getContext(), ts);
    return res;
}

MergeTreeDataPartsCNCHVector
StorageCnchMergeTree::getStagedParts(const TxnTimestamp & ts, const NameSet * partitions, bool skip_delete_bitmap)
{
    auto catalog = getContext()->getCnchCatalog();
    MergeTreeDataPartsCNCHVector staged_parts = catalog->getStagedParts(shared_from_this(), ts, partitions);
    auto res = CnchPartsHelper::calcVisibleParts(staged_parts, /*collect_on_chain*/ false);

    if (!skip_delete_bitmap)
        getDeleteBitmapMetaForStagedParts(res, getContext(), ts);
    return res;
}

void StorageCnchMergeTree::getDeleteBitmapMetaForParts(
    const MergeTreeDataPartsCNCHVector & parts, ContextPtr local_context, TxnTimestamp start_time)
{
    auto catalog = local_context->getCnchCatalog();
    if (!catalog)
        return;

    std::set<String> request_partitions;
    for (const auto & part : parts)
    {
        const auto & partition_id = part->info.partition_id;
        request_partitions.insert(partition_id);
    }

    /// NOTE: Get all the bitmap meta needed only once from kv instead of getting many times for every partition to save time.
    Stopwatch watch;
    auto all_bitmaps
        = catalog->getDeleteBitmapsInPartitions(shared_from_this(), {request_partitions.begin(), request_partitions.end()}, start_time);
    ProfileEvents::increment(ProfileEvents::CatalogTime, watch.elapsedMilliseconds());
    LOG_DEBUG(
        log,
        "Get delete bitmap meta for total {} parts, take {} ms and read {} number of bitmap metas",
        parts.size(),
        watch.elapsedMilliseconds(),
        all_bitmaps.size());

    DeleteBitmapMetaPtrVector bitmaps;
    CnchPartsHelper::calcVisibleDeleteBitmaps(all_bitmaps, bitmaps);

    /// Both the parts and bitmaps are sorted in (partitioin_id, min_block, max_block, commit_time) order
    auto bitmap_it = bitmaps.begin();
    for (auto & part : parts)
    {
        /// search for the first bitmap
        while (bitmap_it != bitmaps.end() && !(*bitmap_it)->sameBlock(part->info))
            bitmap_it++;

        if (bitmap_it == bitmaps.end())
            throw Exception("Delete bitmap metadata of " + part->name + " is not found", ErrorCodes::LOGICAL_ERROR);

        /// add all visible bitmaps (from new to old) part
        part->setDeleteBitmapMeta(*bitmap_it);
        bitmap_it++;
    }
}

void StorageCnchMergeTree::getDeleteBitmapMetaForStagedParts(
    const MergeTreeDataPartsCNCHVector & parts, ContextPtr local_context, TxnTimestamp start_time)
{
    auto catalog = local_context->getCnchCatalog();
    if (!catalog)
        return;

    std::set<String> request_partitions;
    for (const auto & part : parts)
    {
        const auto & partition_id = part->info.partition_id;
        request_partitions.insert(partition_id);
    }

    /// NOTE: Get all the bitmap meta needed only once from kv instead of getting many times for every partition to save time.
    Stopwatch watch;
    auto all_bitmaps
        = catalog->getDeleteBitmapsInPartitions(shared_from_this(), {request_partitions.begin(), request_partitions.end()}, start_time);
    ProfileEvents::increment(ProfileEvents::CatalogTime, watch.elapsedMilliseconds());
    LOG_DEBUG(
        log,
        "Get delete bitmap meta for total {} parts, take {} ms and read {} number of bitmap metas",
        parts.size(),
        watch.elapsedMilliseconds(),
        all_bitmaps.size());

    DeleteBitmapMetaPtrVector bitmaps;
    CnchPartsHelper::calcVisibleDeleteBitmaps(all_bitmaps, bitmaps);

    /// Both the parts and bitmaps are sorted in (partitioin_id, min_block, max_block, commit_time) order
    auto bitmap_it = bitmaps.begin();
    for (auto & part : parts)
    {
        while (bitmap_it != bitmaps.end() && (*(*bitmap_it)) <= part->info)
        {
            if (!(*bitmap_it)->sameBlock(part->info))
                bitmap_it++;
            else
            {
                /// add all visible bitmaps (from new to old) part
                part->setDeleteBitmapMeta(*bitmap_it);
                bitmap_it++;
            }
        }
    }
}

void StorageCnchMergeTree::getDeleteBitmapMetaForParts(
    const ServerDataPartsVector & parts, ContextPtr local_context, TxnTimestamp start_time) const
{
    auto catalog = local_context->getCnchCatalog();
    if (!catalog)
        return;

    std::set<String> request_partitions;
    for (const auto & part : parts)
    {
        const auto & partition_id = part->part_model_wrapper->info->partition_id;
        request_partitions.insert(partition_id);
    }

    /// NOTE: Get all the bitmap meta needed only once from kv instead of getting many times for every partition to save time.
    Stopwatch watch;
    auto all_bitmaps
        = catalog->getDeleteBitmapsInPartitions(shared_from_this(), {request_partitions.begin(), request_partitions.end()}, start_time);
    ProfileEvents::increment(ProfileEvents::CatalogTime, watch.elapsedMilliseconds());
    LOG_DEBUG(
        log,
        "Get delete bitmap meta for total {} parts, take {} ms and read {} number of bitmap metas",
        parts.size(),
        watch.elapsedMilliseconds(),
        all_bitmaps.size());

    DeleteBitmapMetaPtrVector bitmaps;
    CnchPartsHelper::calcVisibleDeleteBitmaps(all_bitmaps, bitmaps);

    /// Both the parts and bitmaps are sorted in (partitioin_id, min_block, max_block, commit_time) order
    auto bitmap_it = bitmaps.begin();
    for (auto & part : parts)
    {
        /// search for the first bitmap
        while (bitmap_it != bitmaps.end() && !(*bitmap_it)->sameBlock(part->info()))
            bitmap_it++;

        if (bitmap_it == bitmaps.end())
            throw Exception("Delete bitmap metadata of " + part->name() + " is not found", ErrorCodes::LOGICAL_ERROR);

        /// add all visible bitmaps (from new to old) part
        bool found_base = false;
        auto list_it = part->delete_bitmap_metas.before_begin();
        for (auto bitmap_meta = *bitmap_it; bitmap_meta; bitmap_meta = bitmap_meta->tryGetPrevious())
        {
            list_it = part->delete_bitmap_metas.insert_after(list_it, bitmap_meta->getModel());
            if (bitmap_meta->getType() == DeleteBitmapMetaType::Base)
            {
                found_base = true;
                break;
            }
        }
        if (!found_base)
            throw Exception("Base delete bitmap of " + part->name() + " is not found", ErrorCodes::LOGICAL_ERROR);

        bitmap_it++;
    }
}

void StorageCnchMergeTree::executeDedupForRepair(const ASTPtr & partition, ContextPtr local_context)
{
    if (!getInMemoryMetadataPtr()->hasUniqueKey())
        throw Exception("SYSTEM DEDUP can only be executed on table with UNIQUE KEY", ErrorCodes::BAD_ARGUMENTS);

    if (partition && !getSettings()->partition_level_unique_keys)
        throw Exception("SYSTEM DEDUP PARTITION can only be used on table with partition_level_unique_keys=1", ErrorCodes::BAD_ARGUMENTS);

    auto txn = getContext()->getCurrentTransaction();
    if (!txn)
        throw Exception("Transaction is not set", ErrorCodes::LOGICAL_ERROR);
    txn->setMainTableUUID(getStorageUUID());

    auto catalog = getContext()->getCnchCatalog();

    CnchDedupHelper::DedupScope scope = CnchDedupHelper::DedupScope::Table();
    if (partition)
    {
        NameOrderedSet partitions;
        partitions.insert(getPartitionIDFromQuery(partition, local_context));
        scope = CnchDedupHelper::DedupScope::Partitions(partitions);
    }

    CnchLockHolder cnch_lock(
        *getContext(),
        CnchDedupHelper::getLocksToAcquire(
            scope, txn->getTransactionID(), *this, getSettings()->dedup_acquire_lock_timeout.value.totalMilliseconds()));
    if (!cnch_lock.tryLock())
        throw Exception("Failed to acquire lock for txn " + txn->getTransactionID().toString(), ErrorCodes::CNCH_LOCK_ACQUIRE_FAILED);

    TxnTimestamp ts = getContext()->getTimestamp();
    MergeTreeDataPartsCNCHVector visible_parts = CnchDedupHelper::getVisiblePartsToDedup(scope, *this, ts);
    MergeTreeDataDeduper deduper(*this, local_context);
    LocalDeleteBitmaps bitmaps_to_dump
        = deduper.repairParts(txn->getTransactionID(), CnchPartsHelper::toIMergeTreeDataPartsVector(visible_parts));

    CnchDataWriter cnch_writer(*this, local_context, ManipulationType::Insert);
    if (!bitmaps_to_dump.empty())
        cnch_writer.publishStagedParts(/*staged_parts*/ {}, bitmaps_to_dump);

    txn->commitV2();
}

void StorageCnchMergeTree::waitForStagedPartsToPublish(ContextPtr local_context)
{
    UInt64 wait_timeout_seconds = local_context->getSettingsRef().receive_timeout.value.totalSeconds();
    Stopwatch timer;
    size_t staged_parts_cnt = 0;
    do
    {
        staged_parts_cnt
            = getStagedParts(local_context->getTimestamp(), /* partitions = */ nullptr, /* skip_delete_bitmap = */ true).size();
        if (!staged_parts_cnt)
            return;
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    } while (timer.elapsedSeconds() < wait_timeout_seconds);
    LOG_WARNING(
        log,
        "There are still " + toString(staged_parts_cnt) + " staged parts to be published after " + toString(wait_timeout_seconds) + "s.");
}

void StorageCnchMergeTree::collectResource(
    ContextPtr local_context,
    ServerDataPartsVector & parts,
    const String & local_table_name,
    const std::set<Int64> & required_bucket_numbers)
{
    auto cnch_resource = local_context->getCnchServerResource();
    auto create_table_query = getCreateQueryForCloudTable(getCreateTableSql(), local_table_name, local_context);

    cnch_resource->addCreateQuery(local_context, shared_from_this(), create_table_query, local_table_name);

    // if (local_context.getSettingsRef().enable_virtual_part)
    //     setVirtualPartSize(local_context, parts, worker_group->getReadWorkers().size());

    cnch_resource->addDataParts(getStorageUUID(), parts, required_bucket_numbers);
}

UInt64 StorageCnchMergeTree::getTimeTravelRetention()
{
    return getSettings()->time_travel_retention_days;
}

void StorageCnchMergeTree::addCheckpoint(const Protos::Checkpoint & /*checkpoint*/)
{
    /// FIXME: add after it was supported
    // getContext()->getCnchCatalog()->addCheckpoint(shared_from_this(), checkpoint);
}

void StorageCnchMergeTree::removeCheckpoint(const Protos::Checkpoint & checkpoint)
{
    Protos::Checkpoint new_checkpoint(checkpoint);
    new_checkpoint.set_status(Protos::Checkpoint::Removing);
    /// FIXME: add after it was supported
    // getContext()->getCnchCatalog()->markCheckpoint(shared_from_this(), new_checkpoint);
}

void StorageCnchMergeTree::sendPreloadTasks(ContextPtr local_context, ServerDataPartsVector parts, bool sync)
{
    auto worker_group = getWorkerGroupForTable(*this, local_context);
    local_context->setCurrentWorkerGroup(worker_group);

    TxnTimestamp txn_id = local_context->getCurrentTransactionID();
    String create_table_query = genCreateTableQueryForWorker(txn_id.toString());

    /// reuse server resource for part allocation
    /// no worker session context is created
    auto server_resource = std::make_shared<CnchServerResource>(txn_id);
    server_resource->skipCleanWorker();
    /// all bucket numbers are required
    std::set<Int64> bucket_numbers;
    if (isBucketTable())
    {
        std::transform(parts.begin(), parts.end(), std::inserter(bucket_numbers, bucket_numbers.end()), [](const auto & part) {
            return part->part_model().bucket_number();
        });
    }
    server_resource->addCreateQuery(local_context, shared_from_this(), create_table_query, "");
    server_resource->addDataParts(getStorageUUID(), parts, bucket_numbers);
    /// TODO: async rpc?
    server_resource->sendResource(local_context, [&](CnchWorkerClientPtr client, auto & resources, ExceptionHandler &handler) {
        std::vector<brpc::CallId> ids;
        for (const auto & resource : resources)
        {
            auto data_parts = std::move(resource.server_parts);
            CnchPartsHelper::flattenPartsVector(data_parts);
            brpc::CallId id = client->preloadDataParts(local_context, txn_id, *this, create_table_query, data_parts, sync, handler);
            ids.emplace_back(id);
        }
        return ids;
    });
}

void StorageCnchMergeTree::filterPartsInExplicitTransaction(ServerDataPartsVector & data_parts, ContextPtr local_context) const
{
    Int64 primary_txn_id = local_context->getCurrentTransaction()->getPrimaryTransactionID().toUInt64();
    TxnTimestamp start_time = local_context->getCurrentTransaction()->getStartTime();

    std::map<TxnTimestamp, bool> success_secondary_txns;
    auto check_success_txn = [&success_secondary_txns, this](const TxnTimestamp & txn_id) -> bool {
        if (auto it = success_secondary_txns.find(txn_id); it != success_secondary_txns.end())
            return it->second;
        auto record = getContext()->getCnchCatalog()->getTransactionRecord(txn_id);
        success_secondary_txns.emplace(txn_id, record.status() == CnchTransactionStatus::Finished);
        return record.status() == CnchTransactionStatus::Finished;
    };
    std::erase_if(data_parts, [&](const auto & part) {
        return !(
            part->info().mutation == primary_txn_id && part->part_model_wrapper->part_model->has_secondary_txn_id()
            && check_success_txn(part->part_model_wrapper->part_model->secondary_txn_id()));
    });
    getCommittedServerDataParts(data_parts, start_time, &(*local_context->getCnchCatalog()));
}

namespace
{

    /// Conversion that is allowed for serializable key (primary key, sorting key).
    /// Key should be serialized in the same way after conversion.
    /// NOTE: The list is not complete.
    bool isSafeForKeyConversion(const IDataType * from, const IDataType * to)
    {
        if (from->getName() == to->getName())
            return true;

        /// Enums are serialized in partition key as numbers - so conversion from Enum to number is Ok.
        /// But only for types of identical width because they are serialized as binary in minmax index.
        /// But not from number to Enum because Enum does not necessarily represents all numbers.

        if (const auto * from_enum8 = typeid_cast<const DataTypeEnum8 *>(from))
        {
            if (const auto * to_enum8 = typeid_cast<const DataTypeEnum8 *>(to))
                return to_enum8->contains(*from_enum8);
            if (typeid_cast<const DataTypeInt8 *>(to))
                return true; // NOLINT
            return false;
        }

        if (const auto * from_enum16 = typeid_cast<const DataTypeEnum16 *>(from))
        {
            if (const auto * to_enum16 = typeid_cast<const DataTypeEnum16 *>(to))
                return to_enum16->contains(*from_enum16);
            if (typeid_cast<const DataTypeInt16 *>(to))
                return true; // NOLINT
            return false;
        }

        if (const auto * from_lc = typeid_cast<const DataTypeLowCardinality *>(from))
            return from_lc->getDictionaryType()->equals(*to);

        if (const auto * to_lc = typeid_cast<const DataTypeLowCardinality *>(to))
            return to_lc->getDictionaryType()->equals(*from);

        return false;
    }
    /// Special check for alters of VersionedCollapsingMergeTree version column
    void checkVersionColumnTypesConversion(const IDataType * old_type, const IDataType * new_type, const String column_name)
    {
        /// Check new type can be used as version
        if (!new_type->canBeUsedAsVersion())
            throw Exception(
                "Cannot alter version column " + backQuoteIfNeed(column_name) + " to type " + new_type->getName()
                    + " because version column must be of an integer type or of type Date or DateTime",
                ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);

        auto which_new_type = WhichDataType(new_type);
        auto which_old_type = WhichDataType(old_type);

        /// Check alter to different sign or float -> int and so on
        if ((which_old_type.isInt() && !which_new_type.isInt()) || (which_old_type.isUInt() && !which_new_type.isUInt())
            || (which_old_type.isDate() && !which_new_type.isDate()) || (which_old_type.isDateTime() && !which_new_type.isDateTime())
            || (which_old_type.isFloat() && !which_new_type.isFloat()))
        {
            throw Exception(
                "Cannot alter version column " + backQuoteIfNeed(column_name) + " from type " + old_type->getName() + " to type "
                    + new_type->getName() + " because new type will change sort order of version column."
                    + " The only possible conversion is expansion of the number of bytes of the current type.",
                ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
        }

        /// Check alter to smaller size: UInt64 -> UInt32 and so on
        if (new_type->getSizeOfValueInMemory() < old_type->getSizeOfValueInMemory())
        {
            throw Exception(
                "Cannot alter version column " + backQuoteIfNeed(column_name) + " from type " + old_type->getName() + " to type "
                    + new_type->getName() + " because new type is smaller than current in the number of bytes."
                    + " The only possible conversion is expansion of the number of bytes of the current type.",
                ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
        }
    }
}

void StorageCnchMergeTree::checkAlterInCnchServer(const AlterCommands & commands, ContextPtr local_context) const
{
    StorageInMemoryMetadata new_metadata = getInMemoryMetadata();
    StorageInMemoryMetadata old_metadata = getInMemoryMetadata();

    const auto & settings = local_context->getSettingsRef();

    if (!settings.allow_non_metadata_alters)
    {
        auto mutation_commands = commands.getMutationCommands(new_metadata, settings.materialize_ttl_after_modify, getContext());

        if (!mutation_commands.empty())
            throw Exception(
                ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN,
                "The following alter commands: '{}' will modify data on disk, but setting `allow_non_metadata_alters` is disabled",
                queryToString(mutation_commands.ast()));
    }
    commands.apply(new_metadata, getContext());

    /// Set of columns that shouldn't be altered.
    NameSet columns_alter_type_forbidden;

    /// Primary key columns can be ALTERed only if they are used in the key as-is
    /// (and not as a part of some expression) and if the ALTER only affects column metadata.
    NameSet columns_alter_type_metadata_only;

    /// Columns to check that the type change is safe for partition key.
    NameSet columns_alter_type_check_safe_for_partition;

    if (old_metadata.hasPartitionKey())
    {
        /// Forbid altering columns inside partition key expressions because it can change partition ID format.
        auto partition_key_expr = old_metadata.getPartitionKey().expression;
        for (const auto & action : partition_key_expr->getActions())
        {
            for (const auto * child : action.node->children)
                columns_alter_type_forbidden.insert(child->result_name);
        }

        /// But allow to alter columns without expressions under certain condition.
        for (const String & col : partition_key_expr->getRequiredColumns())
            columns_alter_type_check_safe_for_partition.insert(col);
    }

    if (old_metadata.hasUniqueKey())
    {
        for (const String & col : old_metadata.getColumnsRequiredForUniqueKey())
            columns_alter_type_forbidden.insert(col);

        if (!merging_params.version_column.empty())
            columns_alter_type_forbidden.insert(merging_params.version_column);
    }

    for (const auto & index : old_metadata.getSecondaryIndices())
    {
        for (const String & col : index.expression->getRequiredColumns())
            columns_alter_type_forbidden.insert(col);
    }

    if (old_metadata.hasSortingKey())
    {
        auto old_sorting_key_expr = old_metadata.getSortingKey().expression;
        for (const auto & action : old_sorting_key_expr->getActions())
        {
            for (const auto * child : action.node->children)
                columns_alter_type_forbidden.insert(child->result_name);
        }
        for (const String & col : old_sorting_key_expr->getRequiredColumns())
            columns_alter_type_metadata_only.insert(col);

        /// We don't process sample_by_ast separately because it must be among the primary key columns
        /// and we don't process primary_key_expr separately because it is a prefix of sorting_key_expr.
    }
    if (!merging_params.sign_column.empty())
        columns_alter_type_forbidden.insert(merging_params.sign_column);

    /// All of the above.
    NameSet columns_in_keys;
    columns_in_keys.insert(columns_alter_type_forbidden.begin(), columns_alter_type_forbidden.end());
    columns_in_keys.insert(columns_alter_type_metadata_only.begin(), columns_alter_type_metadata_only.end());
    columns_in_keys.insert(columns_alter_type_check_safe_for_partition.begin(), columns_alter_type_check_safe_for_partition.end());

    NameSet dropped_columns;

    std::map<String, const IDataType *> old_types;
    for (const auto & column : old_metadata.getColumns().getAllPhysical())
        old_types.emplace(column.name, column.type.get());

    NameSet columns_already_in_alter;
    auto all_mutations = getContext()->getCnchCatalog()->getAllMutations(getStorageID());

    for (auto & mutation : all_mutations)
    {
        auto entry = CnchMergeTreeMutationEntry::parse(mutation);

        for (auto command : entry.commands)
        {
            if (!command.column_name.empty())
                columns_already_in_alter.emplace(command.column_name);
        }
    }

    NamesAndTypesList columns_to_check_conversion;
    auto name_deps = getDependentViewsByColumn(local_context);

    for (const AlterCommand & command : commands)
    {
        /// Just validate partition expression
        if (command.partition)
        {
            getPartitionIDFromQuery(command.partition, getContext());
        }

        if (command.column_name == merging_params.version_column)
        {
            /// Some type changes for version column is allowed despite it's a part of sorting key
            if (command.type == AlterCommand::MODIFY_COLUMN)
            {
                const IDataType * new_type = command.data_type.get();
                const IDataType * old_type = old_types[command.column_name];

                if (new_type)
                    checkVersionColumnTypesConversion(old_type, new_type, command.column_name);

                /// No other checks required
                continue;
            }
            else if (command.type == AlterCommand::DROP_COLUMN)
            {
                throw Exception(
                    "Trying to ALTER DROP version " + backQuoteIfNeed(command.column_name) + " column",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }
            else if (command.type == AlterCommand::RENAME_COLUMN)
            {
                throw Exception(
                    "Trying to ALTER RENAME version " + backQuoteIfNeed(command.column_name) + " column",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }
        }

        if (command.type == AlterCommand::MODIFY_ORDER_BY && !is_custom_partitioned)
        {
            throw Exception(
                "ALTER MODIFY ORDER BY is not supported for default-partitioned tables created with the old syntax",
                ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::MODIFY_TTL && !is_custom_partitioned)
        {
            throw Exception(
                "ALTER MODIFY TTL is not supported for default-partitioned tables created with the old syntax", ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::MODIFY_SAMPLE_BY)
        {
            if (!is_custom_partitioned)
                throw Exception(
                    "ALTER MODIFY SAMPLE BY is not supported for default-partitioned tables created with the old syntax",
                    ErrorCodes::BAD_ARGUMENTS);

            checkSampleExpression(new_metadata, getSettings()->compatibility_allow_sampling_expression_not_in_primary_key);
        }
        if (command.type == AlterCommand::ADD_INDEX && !is_custom_partitioned)
        {
            throw Exception("ALTER ADD INDEX is not supported for tables with the old syntax", ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::ADD_PROJECTION && !is_custom_partitioned)
        {
            throw Exception("ALTER ADD PROJECTION is not supported for tables with the old syntax", ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::RENAME_COLUMN)
        {
            if (columns_in_keys.count(command.column_name))
            {
                throw Exception(
                    "Trying to ALTER RENAME key " + backQuoteIfNeed(command.column_name) + " column which is a part of key expression",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }
        }
        else if (command.type == AlterCommand::DROP_COLUMN)
        {
            if (command.clear)
                throw Exception(ErrorCodes::NOT_IMPLEMENTED, "CLEAR COLUMN is not supported by storage {}", getName());

            if (columns_in_keys.count(command.column_name))
            {
                throw Exception(
                    "Trying to ALTER DROP key " + backQuoteIfNeed(command.column_name) + " column which is a part of key expression",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }

            if (!command.clear)
            {
                const auto & deps_mv = name_deps[command.column_name];
                if (!deps_mv.empty())
                {
                    throw Exception(
                        "Trying to ALTER DROP column " + backQuoteIfNeed(command.column_name) + " which is referenced by materialized view "
                            + toString(deps_mv),
                        ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
                }
            }


            if (command.partition_predicate)
                throw Exception(ErrorCodes::NOT_IMPLEMENTED, "CLEAR COLUMN IN PARTITION WHERE is not supported by storage {}", getName());

            dropped_columns.emplace(command.column_name);
        }
        else if (command.isRequireMutationStage(getInMemoryMetadata()))
        {
            /// This alter will override data on disk. Let's check that it doesn't
            /// modify immutable column.
            if (columns_alter_type_forbidden.count(command.column_name))
                throw Exception(
                    "ALTER of key column " + backQuoteIfNeed(command.column_name) + " is forbidden",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);

            if (command.type == AlterCommand::MODIFY_COLUMN)
            {
                if (columns_alter_type_check_safe_for_partition.count(command.column_name))
                {
                    auto it = old_types.find(command.column_name);

                    assert(it != old_types.end());
                    if (!isSafeForKeyConversion(it->second, command.data_type.get()))
                        throw Exception(
                            "ALTER of partition key column " + backQuoteIfNeed(command.column_name) + " from type " + it->second->getName()
                                + " to type " + command.data_type->getName()
                                + " is not safe because it can change the representation of partition key",
                            ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
                }

                if (columns_alter_type_metadata_only.count(command.column_name))
                {
                    auto it = old_types.find(command.column_name);
                    assert(it != old_types.end());
                    if (!isSafeForKeyConversion(it->second, command.data_type.get()))
                        throw Exception(
                            "ALTER of key column " + backQuoteIfNeed(command.column_name) + " from type " + it->second->getName()
                                + " to type " + command.data_type->getName()
                                + " is not safe because it can change the representation of primary key",
                            ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
                }

                if (old_metadata.getColumns().has(command.column_name))
                {
                    columns_to_check_conversion.push_back(new_metadata.getColumns().getPhysical(command.column_name));
                }
            }
        }
    }

    checkProperties(new_metadata, old_metadata);
    checkTTLExpressions(new_metadata, old_metadata);

    if (!columns_to_check_conversion.empty())
    {
        auto old_header = old_metadata.getSampleBlock();
        performRequiredConversions(old_header, columns_to_check_conversion, getContext());
    }

    for (const auto & part : getDataPartsVector())
    {
        bool at_least_one_column_rest = false;
        for (const auto & column : part->getColumns())
        {
            if (!dropped_columns.count(column.name))
            {
                at_least_one_column_rest = true;
                break;
            }
        }
        if (!at_least_one_column_rest)
        {
            std::string postfix;
            if (dropped_columns.size() > 1)
                postfix = "s";
            throw Exception(
                ErrorCodes::BAD_ARGUMENTS,
                "Cannot drop or clear column{} '{}', because all columns in part '{}' will be removed from disk. Empty parts are not "
                "allowed",
                postfix,
                boost::algorithm::join(dropped_columns, ", "),
                part->name);
        }
    }
}

void StorageCnchMergeTree::checkAlterIsPossible(const AlterCommands & commands, ContextPtr local_context) const
{
    checkAlterInCnchServer(commands, local_context);
    checkAlterSettings(commands);
}

void StorageCnchMergeTree::checkAlterPartitionIsPossible(
    const PartitionCommands & commands, const StorageMetadataPtr & /*metadata_snapshot*/, const Settings & settings) const
{
    for (const auto & command : commands)
    {
        if (command.type == PartitionCommand::DROP_DETACHED_PARTITION && !settings.allow_drop_detached)
            throw DB::Exception(
                "Cannot execute query: DROP DETACHED PART is disabled "
                "(see allow_drop_detached setting)",
                ErrorCodes::SUPPORT_IS_DISABLED);

        if (!partitionCommandHasWhere(command))
        {
            if (command.part)
            {
                auto part_name = command.partition->as<ASTLiteral &>().value.safeGet<String>();
                /// We are able to parse it
                MergeTreePartInfo::fromPartName(part_name, format_version);
            }
            else if (!command.parts)
            {
                /// We are able to parse it
                getPartitionIDFromQuery(command.partition, getContext());
            }
        }
    }
}

Pipe StorageCnchMergeTree::alterPartition(
    const StorageMetadataPtr & metadata_snapshot, const PartitionCommands & commands, ContextPtr query_context)
{
    if (unlikely(!query_context->getCurrentTransaction()))
        throw Exception("Transaction is not set", ErrorCodes::LOGICAL_ERROR);

    if (forwardQueryToServerIfNeeded(query_context, getStorageUUID()))
        return {};

    auto current_query_context = Context::createCopy(query_context);

    for (auto & command : commands)
    {
        TransactionCnchPtr new_txn;

        SCOPE_EXIT({
            if (new_txn)
                current_query_context->getCnchTransactionCoordinator().finishTransaction(new_txn);
        });

        /// If previous transaction has been committed, need to set a new transaction
        /// For the first txn, it is handled by finishCurrentTransaction log in executeQuery to keep the same lifecycle as the query.
        if (current_query_context->getCurrentTransaction()->getStatus() == CnchTransactionStatus::Finished)
        {
            new_txn = current_query_context->getCnchTransactionCoordinator().createTransaction();
            current_query_context->setCurrentTransaction(new_txn, false);
        }

        switch (command.type)
        {
            case PartitionCommand::ATTACH_PARTITION:
            case PartitionCommand::ATTACH_DETACHED_PARTITION:
            case PartitionCommand::REPLACE_PARTITION:
            case PartitionCommand::REPLACE_PARTITION_WHERE: {
                CnchAttachProcessor processor(*this, command, current_query_context);
                processor.exec();
                break;
            }

            case PartitionCommand::DROP_PARTITION:
            case PartitionCommand::DROP_PARTITION_WHERE:
                dropPartitionOrPart(command, current_query_context);
                break;

            case PartitionCommand::INGEST_PARTITION:
                ingestPartition(command, current_query_context);
                break;

            default:
                IStorage::alterPartition(metadata_snapshot, commands, current_query_context);
        }
    }
    return {};
}

void StorageCnchMergeTree::alter(const AlterCommands & commands, ContextPtr local_context, TableLockHolder & /*table_lock_holder*/)
{
    auto table_id = getStorageID();
    StorageInMemoryMetadata new_metadata = getInMemoryMetadata();
    StorageInMemoryMetadata old_metadata = getInMemoryMetadata();

    TransactionCnchPtr txn = local_context->getCurrentTransaction();
    auto action = txn->createAction<DDLAlterAction>(shared_from_this());
    auto & alter_act = action->as<DDLAlterAction &>();
    alter_act.setMutationCommands(commands.getMutationCommands(old_metadata, false, local_context));

    commands.apply(new_metadata, local_context);
    checkColumnsValidity(new_metadata.columns);

    {
        String create_table_query = getCreateTableSql();
        ParserCreateQuery p_create_query;
        ASTPtr ast = parseQuery(
            p_create_query,
            create_table_query,
            local_context->getSettingsRef().max_query_size,
            local_context->getSettingsRef().max_parser_depth);

        applyMetadataChangesToCreateQuery(ast, new_metadata);
        alter_act.setNewSchema(queryToString(ast));

        LOG_DEBUG(log, "new schema for alter query: {}", alter_act.getNewSchema());
        txn->appendAction(action);
    }

    //setProperties(new_metadata, false);
    //updateHDFSRootPaths(new_metadata.root_paths_ast);
    //setTTLExpressions(new_metadata.ttl_for_table_ast);
    //setCreateTableSql(alter_act->getNewSchema());

    txn->commitV1();
    LOG_TRACE(log, "Updated shared metadata in Catalog.");
}

void StorageCnchMergeTree::checkAlterSettings(const AlterCommands & commands) const
{
    static std::set<String> supported_settings = {
        "cnch_vw_default",
        "cnch_vw_read",
        "cnch_vw_write",
        "cnch_vw_task",

        /// Setting for memory buffer
        "cnch_enable_memory_buffer",
        "cnch_memory_buffer_size",
        "min_time_memory_buffer_to_flush",
        "max_time_memory_buffer_to_flush",
        "min_bytes_memory_buffer_to_flush",
        "max_bytes_memory_buffer_to_flush",
        "min_rows_memory_buffer_to_flush",
        "max_rows_memory_buffer_to_flush",
        "max_block_size_in_memory_buffer",
        "max_bytes_to_write_wal",
        "enable_flush_buffer_with_multi_threads",
        "max_flush_threads_num",

        "insertion_label_ttl",
        "enable_local_disk_cache",
        "enable_preload_parts",

        "enable_addition_bg_task",
        "max_addition_bg_task_num",
        "max_addition_mutation_task_num",
        "max_partition_for_multi_select",

        "cnch_merge_parts_cache_timeout",
        "cnch_merge_parts_cache_min_count",
        "cnch_merge_enable_batch_select",
        "cnch_merge_max_total_rows_to_merge",
        "cnch_merge_only_realtime_partition",
        "cnch_merge_pick_worker_algo",
    };

    /// Check whether the value is legal for Setting.
    /// For example, we have a setting item, `SettingBool setting_test`
    /// If you submit a Alter query: "Alter table test modify setting setting_test='abc'"
    /// Then, it will throw an Exception here, because we can't convert string 'abc' to a Bool.
    auto settings_copy = *getSettings();

    for (auto & command : commands)
    {
        if (command.type != AlterCommand::MODIFY_SETTING)
            continue;

        for (auto & change : command.settings_changes)
        {
            if (!supported_settings.count(change.name))
                throw Exception("Setting " + change.name + " cannot be modified", ErrorCodes::SUPPORT_IS_DISABLED);

            if (getInMemoryMetadataPtr()->hasUniqueKey() && change.name == "cnch_enable_memory_buffer" && change.value.get<Int64>() == 1)
                throw Exception("Table with UNIQUE KEY doesn't support memory buffer", ErrorCodes::SUPPORT_IS_DISABLED);

            settings_copy.set(change.name, change.value);
        }
    }
}

void StorageCnchMergeTree::truncate(
    const ASTPtr & /*query*/, const StorageMetadataPtr & /* metadata_snapshot */, ContextPtr local_context, TableExclusiveLockHolder &)
{
    PartitionCommand command;
    command.type = PartitionCommand::DROP_PARTITION_WHERE;
    command.partition = std::make_shared<ASTLiteral>(Field(UInt8(1)));
    command.part = false;
    dropPartitionOrPart(command, local_context);
}

void StorageCnchMergeTree::dropPartitionOrPart(const PartitionCommand & command,
    ContextPtr local_context, IMergeTreeDataPartsVector* dropped_parts, size_t max_threads)
{
    auto svr_parts = selectPartsByPartitionCommand(local_context, command);
    if (svr_parts.empty())
    {
        if (command.part)
            throw Exception(ErrorCodes::NO_SUCH_DATA_PART, "No part found");
        else
            return;
    }

    auto parts = createPartVectorFromServerParts(*this, svr_parts);
    dropPartsImpl(svr_parts, parts, command.detach, local_context, max_threads);

    if (dropped_parts != nullptr)
    {
        *dropped_parts = std::move(parts);
    }
}

void StorageCnchMergeTree::dropPartsImpl(ServerDataPartsVector& svr_parts_to_drop,
    IMergeTreeDataPartsVector& parts_to_drop, bool detach, ContextPtr local_context,
    size_t max_threads)
{
    auto txn = local_context->getCurrentTransaction();

    if (detach)
    {
        auto metadata_snapshot = getInMemoryMetadataPtr();
        if (metadata_snapshot->hasUniqueKey())
            throw Exception("detach partition command is not supported on unique table", ErrorCodes::NOT_IMPLEMENTED);

        /// XXX: Detach parts will break MVCC: queries and tasks which reference those parts will fail.
        // VolumePtr hdfs_volume = getStoragePolicy(IStorage::StorageLocation::MAIN)->local_store_volume();

        // Create detached directory first
        Disks disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
        for (DiskPtr & disk : disks)
        {
            disk->createDirectories(getRelativeDataPath(IStorage::StorageLocation::MAIN) + "/detached");
        }

        UndoResources undo_resources;
        auto write_undo_callback = [&](const DataPartPtr& part) {
            UndoResource ub(txn->getTransactionID(), UndoResourceType::FileSystem, part->getFullRelativePath(),
                part->getRelativePathForDetachedPart(""));
            ub.setDiskName(part->volume->getDisk()->getName());
            undo_resources.push_back(ub);
        };
        for (const auto& data_part : parts_to_drop)
        {
            data_part->enumeratePreviousParts(write_undo_callback);
        }
        local_context->getCnchCatalog()->writeUndoBuffer(UUIDHelpers::UUIDToString(getStorageUUID()),
            txn->getTransactionID(), undo_resources);

        ThreadPool pool(std::min(parts_to_drop.size(), max_threads));
        auto callback = [&pool] (const DataPartPtr& part) {
            pool.scheduleOrThrowOnError([part]() {
                part->renameToDetached("");
            });
        };
        for (const auto& part : parts_to_drop)
        {
            part->enumeratePreviousParts(callback);
        }
        pool.wait();
        /// NOTE: we still need create DROP_RANGE part for detached parts,
    }

    MutableDataPartsVector drop_ranges;

    if (svr_parts_to_drop.size() == 1)
    {
        auto part = svr_parts_to_drop.front();
        auto drop_part_info = part->info();
        drop_part_info.level += 1;
        drop_part_info.mutation = txn->getPrimaryTransactionID().toUInt64();
        drop_part_info.hint_mutation = 0;
        auto disk = getStoragePolicy(IStorage::StorageLocation::AUXILITY)->getAnyDisk();
        auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + drop_part_info.getPartName(), disk);
        String drop_part_name = drop_part_info.getPartName();
        auto drop_part = createPart(
            drop_part_name,
            MergeTreeDataPartType::WIDE,
            drop_part_info,
            single_disk_volume,
            drop_part_name,
            nullptr,
            StorageLocation::AUXILITY);
        drop_part->partition.assign(part->partition());
        drop_part->deleted = true;

        if (txn->isSecondary())
        {
            drop_part->secondary_txn_id = txn->getTransactionID();
        }

        drop_ranges.emplace_back(std::move(drop_part));
    }
    else
    {
        // drop_range parts should belong to the primary transaction
        drop_ranges = createDropRangesFromParts(svr_parts_to_drop, txn);
    }

    CnchDataWriter cnch_writer(*this, local_context, ManipulationType::Drop);
    cnch_writer.dumpAndCommitCnchParts(drop_ranges);
}

StorageCnchMergeTree::MutableDataPartsVector
StorageCnchMergeTree::createDropRangesFromParts(const ServerDataPartsVector & parts_to_drop, const TransactionCnchPtr & txn)
{
    PartitionDropInfos partition_infos;

    for (const auto & part : parts_to_drop)
    {
        auto [iter, inserted] = partition_infos.try_emplace(part->info().partition_id);
        if (inserted)
            iter->second.value.assign(part->partition());

        iter->second.max_block = std::max(iter->second.max_block, part->info().max_block);
        iter->second.rows_count += part->rowsCount();
        iter->second.size += part->part_model().size();
        iter->second.parts_count += 1;
    }

    return createDropRangesFromPartitions(partition_infos, txn);
}

StorageCnchMergeTree::MutableDataPartsVector
StorageCnchMergeTree::createDropRangesFromPartitions(const PartitionDropInfos & partition_infos, const TransactionCnchPtr & txn)
{
    MutableDataPartsVector drop_ranges;
    for (auto && [partition_id, info] : partition_infos)
    {
        MergeTreePartInfo drop_range_info(
            partition_id, 0, info.max_block, MergeTreePartInfo::MAX_LEVEL, txn->getPrimaryTransactionID(), 0 /* must be zero */);
        auto disk = getStoragePolicy(IStorage::StorageLocation::AUXILITY)->getAnyDisk();
        auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + drop_range_info.getPartName(), disk);
        String drop_part_name = drop_range_info.getPartName();
        auto drop_range = createPart(
            drop_part_name,
            MergeTreeDataPartType::WIDE,
            drop_range_info,
            single_disk_volume,
            drop_part_name,
            nullptr,
            StorageLocation::AUXILITY);
        drop_range->partition.assign(info.value);
        drop_range->deleted = true;
        drop_range->covered_parts_rows = info.rows_count;
        drop_range->covered_parts_size = info.size;
        drop_range->covered_parts_count = info.parts_count;
        /// If we don't have this, drop_range parts are not visible to queries in interactive
        /// transaction session
        if (txn->isSecondary())
        {
            drop_range->secondary_txn_id = txn->getTransactionID();
        }
        drop_ranges.push_back(std::move(drop_range));
    }

    return drop_ranges;
}

StoragePolicyPtr StorageCnchMergeTree::getStoragePolicy(StorageLocation location) const
{
    String policy_name = (location == StorageLocation::MAIN ? getSettings()->storage_policy : getContext()->getCnchAuxilityPolicyName());
    return getContext()->getStoragePolicy(policy_name);
}

const String & StorageCnchMergeTree::getRelativeDataPath(StorageLocation location) const
{
    return location == StorageLocation::MAIN ? MergeTreeMetaBase::getRelativeDataPath(location) : relative_auxility_storage_path;
}

Block StorageCnchMergeTree::getBlockWithVirtualPartitionColumns(
    const std::vector<std::shared_ptr<MergeTreePartition>> & partition_list) const
{
    DataTypePtr partition_value_type = getPartitionValueType();
    bool has_partition_value = typeid_cast<const DataTypeTuple *>(partition_value_type.get());
    Block block{
        ColumnWithTypeAndName(ColumnString::create(), std::make_shared<DataTypeString>(), "_partition_id"),
        ColumnWithTypeAndName(partition_value_type->createColumn(), partition_value_type, "_partition_value")};


    MutableColumns columns = block.mutateColumns();

    auto & partition_id_column = columns[0];
    auto & partition_value_column = columns[1];

    for (const auto & partition : partition_list)
    {
        partition_id_column->insert(partition->getID(*this));
        Tuple tuple(partition->value.begin(), partition->value.end());
        if (has_partition_value)
            partition_value_column->insert(std::move(tuple));
    }
    block.setColumns(std::move(columns));
    if (!has_partition_value)
        block.erase(block.getPositionByName("_partition_value"));
    return block;
}

std::set<Int64> StorageCnchMergeTree::getRequiredBucketNumbers(const SelectQueryInfo & query_info, ContextPtr local_context) const
{
    std::set<Int64> bucket_numbers;
    ASTPtr where_expression = query_info.query->as<ASTSelectQuery>()->getWhere();
    const Settings & settings = local_context->getSettingsRef();
    auto metadata_snapshot = getInMemoryMetadataPtr();
    // if number of bucket columns of this table > 1, skip optimisation
    if (settings.optimize_skip_unused_shards && where_expression && isBucketTable()
        && metadata_snapshot->getColumnsForClusterByKey().size() == 1)
    {
        // get constant actions of the expression
        Block sample_block = metadata_snapshot->getSampleBlock();
        NamesAndTypesList source_columns = sample_block.getNamesAndTypesList();

        auto syntax_result = TreeRewriter(local_context).analyze(where_expression, source_columns);
        ExpressionActionsPtr const_actions = ExpressionAnalyzer{where_expression, syntax_result, local_context}.getConstActions();
        Names required_source_columns = syntax_result->requiredSourceColumns();

        // Delete all unneeded columns
        for (const auto & delete_column : sample_block.getNamesAndTypesList())
        {
            if (std::find(required_source_columns.begin(), required_source_columns.end(), delete_column.name)
                == required_source_columns.end())
            {
                sample_block.erase(delete_column.name);
            }
        }

        const_actions->execute(sample_block);

        //replace constant values as literals in AST using visitor
        if (sample_block)
        {
            InDepthNodeVisitor<ReplacingConstantExpressionsMatcher, true> visitor(sample_block);
            visitor.visit(where_expression);
        }

        size_t limit = settings.optimize_skip_unused_shards_limit;
        if (!limit || limit > SSIZE_MAX)
        {
            throw Exception(
                "optimize_skip_unused_shards_limit out of range (0, " + std::to_string(SSIZE_MAX) + "]", ErrorCodes::ARGUMENT_OUT_OF_BOUND);
        }
        // Increment limit so that when limit reaches 0, it means that the limit has been exceeded
        ++limit;

        // NOTE: check for cluster by columns in where clause done in evaluateExpressionOverConstantCondition
        const auto & blocks
            = evaluateExpressionOverConstantCondition(where_expression, metadata_snapshot->getClusterByKey().expression, limit);

        if (!limit)
        {
            LOG_INFO(
                log,
                "Number of values for cluster_by key exceeds optimize_skip_unused_shards_limit = "
                    + std::to_string(settings.optimize_skip_unused_shards_limit)
                    + ", try to increase it, but note that this may increase query processing time.");
        }

        if (blocks)
        {
            for (const auto & block : *blocks)
            {
                // Get bucket number and add to results array
                Block block_copy = block;
                prepareBucketColumn(
                    block_copy,
                    metadata_snapshot->getColumnsForClusterByKey(),
                    metadata_snapshot->getSplitNumberFromClusterByKey(),
                    metadata_snapshot->getWithRangeFromClusterByKey(),
                    metadata_snapshot->getBucketNumberFromClusterByKey(),
                    local_context);
                auto bucket_number
                    = block_copy.getByPosition(block_copy.columns() - 1).column->getInt(0); // this block only contains one row
                bucket_numbers.insert(bucket_number);
            }
        }
    }
    return bucket_numbers;
}
StorageCnchMergeTree * StorageCnchMergeTree::checkStructureAndGetCnchMergeTree(const StoragePtr & source_table) const
{
    StorageCnchMergeTree * src_data = dynamic_cast<StorageCnchMergeTree *>(source_table.get());
    if (!src_data)
        throw Exception(
            "Table " + source_table->getStorageID().getFullTableName() + " is not StorageCnchMergeTree", ErrorCodes::BAD_ARGUMENTS);

    auto metadata = getInMemoryMetadataPtr();
    auto src_metadata = src_data->getInMemoryMetadataPtr();
    Names minmax_column_names = getMinMaxColumnsNames(metadata->getPartitionKey());

    /// Columns order matters if table havs more than one minmax index column.
    if (!metadata->getColumns().getAllPhysical().isCompatableWithKeyColumns(
            src_metadata->getColumns().getAllPhysical(), minmax_column_names))
    {
        throw Exception("Tables have different structure", ErrorCodes::INCOMPATIBLE_COLUMNS);
    }

    auto query_to_string = [](const ASTPtr & ast) {
        if (ast == nullptr)
        {
            return std::string("");
        }

        WriteBufferFromOwnString out;
        formatAST(*ast, out, false, true, true);
        return out.str();
    };

    if (query_to_string(metadata->getSortingKeyAST()) != query_to_string(src_metadata->getSortingKeyAST()))
        throw Exception("Tables have different ordering", ErrorCodes::BAD_ARGUMENTS);

    if (query_to_string(metadata->getSamplingKeyAST()) != query_to_string(src_metadata->getSamplingKeyAST()))
        throw Exception("Tables have different sample by key", ErrorCodes::BAD_ARGUMENTS);

    if (query_to_string(metadata->getPartitionKeyAST()) != query_to_string(src_metadata->getPartitionKeyAST()))
        throw Exception("Tables have different partition key", ErrorCodes::BAD_ARGUMENTS);

    if (format_version != src_data->format_version)
        throw Exception("Tables have different format_version", ErrorCodes::BAD_ARGUMENTS);

    // check root path of source and destination table
    Disks tgt_disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    Disks src_disks = src_data->getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    std::set<String> tgt_path_set;
    for (const DiskPtr & disk : tgt_disks)
    {
        tgt_path_set.insert(disk->getPath());
    }
    for (const DiskPtr & disk : src_disks)
    {
        if (!tgt_path_set.count(disk->getPath()))
            throw Exception("source table and destination table have different hdfs root path", ErrorCodes::BAD_ARGUMENTS);
    }

    // If target table is a bucket table, ensure that source table is a bucket table
    // or if the source table is a bucket table, ensure the table_definition_hash is the same before proceeding to drop parts
    // Can remove this check if rollback has been implemented
    if (isBucketTable() && (!src_data->isBucketTable() || getTableHashForClusterBy() != src_data->getTableHashForClusterBy()))
    {
        LOG_DEBUG(
            log,
            fmt::format(
                "{}.{} table_definition hash [{}] is different from target table's "
                "table_definition hash [{}]",
                src_data->getDatabaseName(),
                src_data->getTableName(),
                src_data->getTableHashForClusterBy(),
                getTableHashForClusterBy()));
        throw Exception(
            "Source table is not a bucket table or has a different CLUSTER BY definition from the target table. ",
            ErrorCodes::BUCKET_TABLE_ENGINE_MISMATCH);
    }

    return src_data;
}

ServerDataPartsVector StorageCnchMergeTree::selectPartsByPartitionCommand(ContextPtr local_context, const PartitionCommand & command)
{
    /// The members of `command` have different meaning depending on the types of partition command:
    /// 1. <COMMAND> PART:
    ///    - command.part = true
    ///    - command.partition is ASTLiteral of part name
    /// 2. <COMMAND> PARTITION <ID>:
    ///    - command.part = false
    ///    - command.partition is partition expression
    /// 3. <COMMAND> PARTHTION WHERE:
    ///    - command.part = false;
    ///    - command.partition is the WHERE predicate (should only includes partition column)

    /// Implementation: reuse selectPartsToRead(). Actually, this is an overkill, because the predicates is usually not
    /// too complicated. Howerver, this is the only way to avoid repeating same code in StorageCnchMergeTree.
    SelectQueryInfo query_info;
    Names column_names_to_return;
    ASTPtr query = std::make_shared<ASTSelectQuery>();
    auto * select = query->as<ASTSelectQuery>();
    select->setExpression(ASTSelectQuery::Expression::SELECT, std::make_shared<ASTExpressionList>());
    select->select()->children.push_back(std::make_shared<ASTLiteral>(1));
    select->replaceDatabaseAndTable(getStorageID());
    /// create a fake query: SELECT 1 FROM TBL WHERE ... as following
    ASTPtr where;
    if (command.part)
    {
        /// Predicate: WHERE _part = value, with value from command.partition
        auto lhs = std::make_shared<ASTIdentifier>("_part");
        auto rhs = command.partition->clone();
        where = makeASTFunction("equals", std::move(lhs), std::move(rhs));
        column_names_to_return.push_back("_part");
    }
    else if (!partitionCommandHasWhere(command))
    {
        fmt::print("Command: {}\n", command.typeToString());
        const auto & partition = command.partition->as<const ASTPartition &>();
        if (!partition.id.empty())
        {
            /// Predicate: WHERE _partition_id = value, with value is partition.id
            auto lhs = std::make_shared<ASTIdentifier>("_partition_id");
            auto rhs = std::make_shared<ASTLiteral>(Field(partition.id));
            where = makeASTFunction("equals", std::move(lhs), std::move(rhs));
            column_names_to_return.push_back("_partition_id");
        }
        else
        {
            /// Predicate: WHERE _partition_value = value, with value is partition.value
            auto lhs = std::make_shared<ASTIdentifier>("_partition_value");
            auto rhs = partition.value->clone();
            if (partition.fields_count == 1)
                rhs = makeASTFunction("tuple", std::move(rhs));
            where = makeASTFunction("equals", std::move(lhs), std::move(rhs));
            column_names_to_return.push_back("_partition_value");
        }
    }
    else
    {
        /// Predicate: WHERE xxx with xxx is command.partition
        where = command.partition->clone();
    }

    select->setExpression(ASTSelectQuery::Expression::WHERE, std::move(where));
    auto metadata_snapshot = getInMemoryMetadataPtr();
    /// So this step will throws if WHERE expression contains columns not in partition key, and it's a good thing
    TreeRewriterResult syntax_analyzer_result(
        metadata_snapshot->partition_key.sample_block.getNamesAndTypesList(), shared_from_this(), metadata_snapshot, true);
    auto analyzed_result = TreeRewriter(local_context).analyzeSelect(query, std::move(syntax_analyzer_result));
    query_info.query = std::move(query);
    query_info.syntax_analyzer_result = std::move(analyzed_result);
    return selectPartsToRead(column_names_to_return, local_context, query_info);
}

String StorageCnchMergeTree::genCreateTableQueryForWorker(const String & suffix)
{
    String worker_table_name = getTableName();

    if (!suffix.empty())
    {
        worker_table_name += '_';
        for (const auto & c : suffix)
        {
            if (c != '-')
                worker_table_name += c;
        }
    }

    return getCreateQueryForCloudTable(getCreateTableSql(), worker_table_name);
}

std::optional<UInt64> StorageCnchMergeTree::totalRows(const ContextPtr & query_context) const
{
    auto parts = getAllParts(query_context);
    if (parts.empty())
        return 0;
    const auto & metadata_snapshot = getInMemoryMetadataPtr();
    if (metadata_snapshot->hasUniqueKey())
        getDeleteBitmapMetaForParts(parts, query_context, query_context->getCurrentTransactionID());
    size_t rows = 0;
    for (const auto & part : parts)
    {
        if (!part->isPartial())
        {
            if (const auto & delete_bitmap = part->getDeleteBitmap(*this, false))
                rows += part->rowsCount() - delete_bitmap->cardinality();
            else
                rows += part->rowsCount();
        }
    }
    return rows;
}

std::optional<UInt64>
StorageCnchMergeTree::totalRowsByPartitionPredicate(const SelectQueryInfo & query_info, ContextPtr local_context) const
{
    /// Similar to selectPartsToRead, but will return {} if the predicate is not a partition predicate or _part
    auto column_names_to_return = query_info.syntax_analyzer_result->requiredSourceColumns();
    auto parts = getAllPartsInPartitions(column_names_to_return, local_context, query_info);
    if (parts.empty())
        return 0;
    const auto & metadata_snapshot = getInMemoryMetadataPtr();
    if (metadata_snapshot->hasUniqueKey())
        getDeleteBitmapMetaForParts(parts, local_context, local_context->getCurrentTransactionID());

    bool partition_column_valid = std::any_of(column_names_to_return.begin(), column_names_to_return.end(), [](const auto & name) {
        return name == "_partition_id" || name == "_partition_value";
    });

    if (partition_column_valid)
    {
        auto partition_list = local_context->getCnchCatalog()->getPartitionList(shared_from_this(), local_context.get());
        Block partition_block = getBlockWithVirtualPartitionColumns(partition_list);
        ASTPtr expression_ast;

        /// Generate valid expressions for filtering
        partition_column_valid = partition_column_valid
            && VirtualColumnUtils::prepareFilterBlockWithQuery(query_info.query, local_context, partition_block, expression_ast);
    }

    PartitionPruner partition_pruner(metadata_snapshot, query_info, local_context, true /* strict */);

    if (!partition_column_valid && partition_pruner.isUseless())
        return {};

    Block virtual_columns_block = getBlockWithPartColumn(parts);
    bool part_column_queried
        = std::any_of(column_names_to_return.begin(), column_names_to_return.end(), [](const auto & name) { return name == "_part"; });
    if (part_column_queried)
        VirtualColumnUtils::filterBlockWithQuery(query_info.query, virtual_columns_block, local_context);
    auto part_values = VirtualColumnUtils::extractSingleValueFromBlock<String>(virtual_columns_block, "_part");
    if (part_values.empty())
        return 0;

    size_t rows = 0;
    for (const auto & part : parts)
        if (!part->isPartial() && (part_values.empty() || part_values.find(part->name()) != part_values.end())
            && !partition_pruner.canBePruned(*part))
        {
            if (const auto & delete_bitmap = part->getDeleteBitmap(*this, false))
                rows += part->rowsCount() - delete_bitmap->cardinality();
            else
                rows += part->rowsCount();
        }
    return rows;
}

void StorageCnchMergeTree::checkMutationIsPossible(const MutationCommands & commands, const Settings & /*settings*/) const
{
    for (const auto & command : commands)
    {
        if (command.type != MutationCommand::MATERIALIZE_INDEX)
            throw Exception(ErrorCodes::NOT_IMPLEMENTED , "StorageCnchMergeTree doesn't support mutation of type {}\n", command.type);
    }
}

void StorageCnchMergeTree::mutate(const MutationCommands & commands, ContextPtr query_context)
{
    if (commands.empty())
        return;

    auto txn = query_context->getCurrentTransaction();
    auto action = txn->createAction<DDLAlterAction>(shared_from_this());
    auto & alter_act = action->as<DDLAlterAction &>();
    alter_act.setMutationCommands(commands);
    txn->appendAction(std::move(action));
    txn->commitV1();

    /// TODO: trigger sync mutation if mutation_sync = 1, this is only an ugly (mainly for CI test)
    auto bg_thread = query_context->tryGetCnchBGThread(CnchBGThreadType::MergeMutate, getStorageID());
    if (bg_thread)
    {
        auto merge_mutate_thread = typeid_cast<CnchMergeMutateThread *>(bg_thread.get());
        auto istorage = shared_from_this();
        merge_mutate_thread->triggerPartMutate(shared_from_this());
    }
}

} // end namespace DB
