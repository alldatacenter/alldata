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

#include <QueryPlan/TableScanStep.h>

#include <Formats/FormatSettings.h>
#include <Functions/FunctionsInBloomFilter.h>
#include <Interpreters/ActionsVisitor.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/TableJoin.h>
#include <Interpreters/convertFieldToType.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Interpreters/misc.h>
#include <MergeTreeCommon/CnchBucketTableCommon.h>
#include <Optimizer/DynamicFilters.h>
#include <Optimizer/PredicateUtils.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Sources/NullSource.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <QueryPlan/QueryPlan.h>
#include <Storages/IStorage_fwd.h>
#include <Storages/MergeTree/MergeTreeWhereOptimizer.h>
#include <Storages/VirtualColumnUtils.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/StorageDistributed.h>
#include <Storages/VirtualColumnUtils.h>
#include <Common/FieldVisitorToString.h>
#include "Interpreters/DatabaseCatalog.h"


namespace DB
{
namespace ErrorCodes
{
    extern const int ILLEGAL_PREWHERE;
    extern const int INVALID_LIMIT_EXPRESSION;
}
void TableScanStep::makeSetsForIndex(const ASTPtr & node, ContextPtr context, PreparedSets & prepared_sets) const
{
    auto settings = context->getSettingsRef();
    SizeLimits size_limits_for_set(settings.max_rows_in_set, settings.max_bytes_in_set, settings.set_overflow_mode);

    if (!node || !storage || !storage->supportsIndexForIn())
        return;

    for (auto & child : node->children)
    {
        /// Don't descend into lambda functions
        const auto * func = child->as<ASTFunction>();
        if (func && func->name == "lambda")
            continue;

        makeSetsForIndex(child, context, prepared_sets);
    }

    const auto * func = node->as<ASTFunction>();
    auto metadata_snapshot = storage->getInMemoryMetadataPtr();
    if (func && functionIsInOrGlobalInOperator(func->name))
    {
        const IAST & args = *func->arguments;
        const ASTPtr & left_in_operand = args.children.at(0);

        if (storage && storage->mayBenefitFromIndexForIn(left_in_operand, context, metadata_snapshot))
        {
            const ASTPtr & arg = args.children.at(1);
            if (arg->as<ASTSubquery>() || arg->as<ASTTableIdentifier>())
            {
                // if (settings.use_index_for_in_with_subqueries)
                //     tryMakeSetForIndexFromSubquery(arg, query_options);
            }
            else
            {
                auto input = storage->getInMemoryMetadataPtr()->getColumns().getAll();
                Names output;
                output.emplace_back(left_in_operand->getColumnName());
                auto temp_actions = createExpressionActions(context, input, output, left_in_operand);
                if (temp_actions->tryFindInIndex(left_in_operand->getColumnName()))
                {
                    makeExplicitSet(func, *temp_actions, true, context, size_limits_for_set, prepared_sets);
                }
            }
        }
    }
}

TableScanStep::TableScanStep(
    ContextPtr  /*context*/,
    StoragePtr storage_,
    const NamesWithAliases & column_alias_,
    const SelectQueryInfo & query_info_,
    QueryProcessingStage::Enum processing_stage_,
    size_t max_block_size_)
    : ISourceStep(DataStream{})
    , storage(storage_)
    , storage_id(storage->getStorageID())
    , column_alias(column_alias_)
    , query_info(query_info_)
    , processing_stage(processing_stage_)
    , max_block_size(max_block_size_)
{
    log = &Poco::Logger::get("TableScanStep");

    for (auto & item : column_alias)
    {
        column_names.emplace_back(item.first);
    }

    // order sensitive
    Names require_column_list = column_names;
    NameSet require_columns{column_names.begin(), column_names.end()};

    //    QueryPlan tmp_query_plan;
    //    storage->read(tmp_query_plan, require_column_list, storage->getInMemoryMetadataPtr(), query_info, context, processing_stage, max_block_size, max_streams, true);

    auto all_columns = storage->getInMemoryMetadataPtr()->getColumns().getAllPhysical();
    auto virtual_col = storage->getVirtuals();
    all_columns.insert(all_columns.end(), virtual_col.begin(), virtual_col.end());
    for (const auto & item : all_columns)
        if (std::find(require_column_list.begin(), require_column_list.end(), item.name) == require_column_list.end()
            && require_columns.contains(item.name))
            require_column_list.push_back(item.name);

    auto header = storage->getInMemoryMetadataPtr()->getSampleBlockForColumns(require_column_list, storage->getVirtuals());


    // init query_info.syntax_analyzer
    auto tree_rewriter_result
        = std::make_shared<TreeRewriterResult>(header.getNamesAndTypesList(), storage, storage->getInMemoryMetadataPtr());
    tree_rewriter_result->required_source_columns = header.getNamesAndTypesList();
    tree_rewriter_result->analyzed_join = std::make_shared<TableJoin>();
    query_info.syntax_analyzer_result = tree_rewriter_result;

    NameToNameMap name_to_name_map;
    for (auto & item : column_alias)
    {
        name_to_name_map[item.first] = item.second;
    }

    const auto select_expression_list = query_info.query->as<ASTSelectQuery>()->select();
    select_expression_list->children.clear();

    for (const auto & item : header)
    {
        select_expression_list->children.emplace_back(std::make_shared<ASTIdentifier>(item.name));
        if (name_to_name_map.contains(item.name))
        {
            output_stream->header.insert(ColumnWithTypeAndName{item.type, name_to_name_map[item.name]});
        }
    }
}

TableScanStep::TableScanStep(
    ContextPtr  context,
    StorageID storage_id_,
    const NamesWithAliases & column_alias_,
    const SelectQueryInfo & query_info_,
    QueryProcessingStage::Enum processing_stage_,
    size_t max_block_size_)
    : TableScanStep(context, DatabaseCatalog::instance().getTable(storage_id_, context), column_alias_, query_info_, processing_stage_, max_block_size_) { }

void TableScanStep::optimizeWhereIntoPrewhre(ContextPtr context)
{
    auto & query = *query_info.query->as<ASTSelectQuery>();
    if (storage && query.where() && !query.prewhere())
    {
        /// PREWHERE optimization: transfer some condition from WHERE to PREWHERE if enabled and viable
        if (const auto & column_sizes = storage->getColumnSizes(); !column_sizes.empty())
        {
            /// Extract column compressed sizes.
            std::unordered_map<std::string, UInt64> column_compressed_sizes;
            for (const auto & [name, sizes] : column_sizes)
                column_compressed_sizes[name] = sizes.data_compressed;

            MergeTreeWhereOptimizer{
                query_info, context, std::move(column_compressed_sizes), storage->getInMemoryMetadataPtr(), column_names, log};
        }
    }
}

SelectQueryInfo TableScanStep::fillQueryInfo(ContextPtr context)
{
    auto * query = query_info.query->as<ASTSelectQuery>();
    SelectQueryInfo copy_query_info = query_info;
    makeSetsForIndex(query->where(), context, copy_query_info.sets);
    makeSetsForIndex(query->prewhere(), context, copy_query_info.sets);
    return copy_query_info;
}

ASTs cloneChildrenReplacement(ASTs ast_children_replacement)
{
    ASTs cloned_replacement;
    cloned_replacement.reserve(ast_children_replacement.size());
    for (const auto & ast_child : ast_children_replacement)
    {
        cloned_replacement.push_back(ast_child->clone());
    }
    return cloned_replacement;
}

void TableScanStep::rewriteInForBucketTable(ContextPtr context) const
{
    const auto * cloud_merge_tree = dynamic_cast<StorageCloudMergeTree *>(storage.get());
    if (!cloud_merge_tree)
        return;

    auto metadata_snapshot = cloud_merge_tree->getInMemoryMetadataPtr();
    const bool isBucketTableAndNeedOptimise = context->getSettingsRef().optimize_skip_unused_shards && cloud_merge_tree->isBucketTable()
        && metadata_snapshot->getColumnsForClusterByKey().size() == 1 && !cloud_merge_tree->getRequiredBucketNumbers().empty();
    if (!isBucketTableAndNeedOptimise)
        return;

    auto query = query_info.query->as<ASTSelectQuery>();

    // NOTE: Have try-catch for every rewrite_in in order to find the WHERE clause that caused the error
    RewriteInQueryVisitor::Data data;
    LOG_TRACE(log, "Before rewriteInForBucketTable:\n: {}", query->dumpTree());
    if (query->where())
    {
        auto ast_children_replacement = cloud_merge_tree->convertBucketNumbersToAstLiterals(query->where(), context);
        if (!ast_children_replacement.empty())
        {
            data.ast_children_replacement = cloneChildrenReplacement(ast_children_replacement);
            ASTPtr & where = query->refWhere();
            RewriteInQueryVisitor(data).visit(where);
        }
    }
    if (query->prewhere())
    {
        auto ast_children_replacement = cloud_merge_tree->convertBucketNumbersToAstLiterals(query->prewhere(), context);
        if (!ast_children_replacement.empty())
        {
            data.ast_children_replacement = cloneChildrenReplacement(ast_children_replacement);
            ASTPtr & pre_where = query->refPrewhere();
            RewriteInQueryVisitor(data).visit(pre_where);
        }
    }
    LOG_TRACE(log, "After rewriteInForBucketTable:\n: {}", query->dumpTree());
}

void TableScanStep::initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & build_context)
{
    storage = DatabaseCatalog::instance().getTable(storage_id, build_context.context);
    rewriteInForBucketTable(build_context.context);
    auto * query = query_info.query->as<ASTSelectQuery>();
    if (auto filter = query->getWhere())
        query->setExpression(ASTSelectQuery::Expression::WHERE, rewriteDynamicFilter(filter, pipeline, build_context));
    if (auto filter = query->getPrewhere())
        query->setExpression(ASTSelectQuery::Expression::PREWHERE, rewriteDynamicFilter(filter, pipeline, build_context));

    // todo support _shard_num rewrite
    // if ()
    // {
    //     VirtualColumnUtils::rewriteEntityInAst(query_info.query, "_shard_num", shard_info.shard_num, "toUInt32");
    // }

    /**
     * reconstuct query level info based on query
     */
    SelectQueryOptions options;
    auto interpreter = std::make_shared<InterpreterSelectQuery>(query_info.query, build_context.context, options.distributedStages());
    interpreter->execute();
    query_info = interpreter->getQueryInfo();
    query_info = fillQueryInfo(build_context.context);

    size_t max_streams = build_context.context->getSettingsRef().max_threads;
    if (max_block_size < build_context.context->getSettingsRef().max_block_size)
        max_streams = 1; // single block single stream.

    if (max_streams > 1 && !storage->isRemote())
        max_streams *= build_context.context->getSettingsRef().max_streams_to_max_threads_ratio;

    auto pipe = storage->read(
        interpreter->getRequiredColumns(),
        storage->getInMemoryMetadataPtr(),
        query_info,
        build_context.context,
        processing_stage,
        max_block_size,
        max_streams);

    QueryPlanStepPtr step;
    if (pipe.empty())
    {
        auto header = storage->getInMemoryMetadataPtr()->getSampleBlockForColumns(column_names, storage->getVirtuals(), storage_id);
        auto null_pipe = InterpreterSelectQuery::generateNullSourcePipe(header, query_info);
        auto read_from_pipe = std::make_shared<ReadFromPreparedSource>(std::move(null_pipe));
        read_from_pipe->setStepDescription("Read from NullSource");
        step = read_from_pipe;
    }
    else
        step = std::make_shared<ReadFromStorageStep>(std::move(pipe), step_description);


    if (auto * source = dynamic_cast<ISourceStep *>(step.get()))
        source->initializePipeline(pipeline, build_context);

    // simple fix, remove this.
    if (!blocksHaveEqualStructure(pipeline.getHeader(), output_stream->header))
    {
        ASTPtr select = std::make_shared<ASTExpressionList>();
        for (const auto & column : column_alias)
        {
            select->children.emplace_back(std::make_shared<ASTIdentifier>(column.first));
        }
        auto actions
            = createExpressionActions(build_context.context, pipeline.getHeader().getNamesAndTypesList(), column_alias, select, true);
        auto expression = std::make_shared<ExpressionActions>(actions, build_context.getActionsSettings());
        pipeline.addSimpleTransform(
            [&](const Block & header) -> ProcessorPtr { return std::make_shared<ExpressionTransform>(header, expression); });
    }

    //
    //    ASTPtr new_prewhere = rewriteDynamicFilter(prewhere, build_context);
    //
    //    if (auto query_prewhere = new_query_info.query->as<ASTSelectQuery>()->getPrewhere())
    //        new_query_info.query->as<ASTSelectQuery>()->refPrewhere() = rewriteDynamicFilter(query_prewhere, build_context);
    //
    //    if (auto query_where = new_query_info.query->as<ASTSelectQuery>()->getWhere())
    //        new_query_info.query->as<ASTSelectQuery>()->refWhere() = rewriteDynamicFilter(query_where, build_context);
    //
    //    std::tie(new_query_info, new_prewhere) = fillPrewhereInfo(build_context.context, new_query_info, new_prewhere);
    //
    //    // order sensitive
    //    Names require_column_list = column_names;
    //    NameSet require_columns{column_names.begin(), column_names.end()};
    //
    //    if (new_query_info.prewhere_info)
    //    {
    //        if (new_query_info.prewhere_info->alias_actions)
    //        {
    //            auto alias_require = new_query_info.prewhere_info->alias_actions->getRequiredColumns();
    //            require_columns.insert(alias_require.begin(), alias_require.end());
    //        }
    //        if (new_query_info.prewhere_info->prewhere_actions)
    //        {
    //            auto prewhere_require = new_query_info.prewhere_info->prewhere_actions->getRequiredColumns();
    //            require_columns.insert(prewhere_require.begin(), prewhere_require.end());
    //        }
    //    }
    //
    //    auto all_columns = storage->getColumns().getAllPhysical();
    //    auto virtual_col = storage->getVirtuals();
    //    all_columns.insert(all_columns.end(), virtual_col.begin(), virtual_col.end());
    //    for (const auto & item : all_columns)
    //        if (std::find(require_column_list.begin(), require_column_list.end(), item.name) == require_column_list.end()
    //            && require_columns.contains(item.name))
    //            require_column_list.push_back(item.name);
    //
    //
    //    NamesAndTypes new_header;
    //    {
    //        auto header = storage->getSampleBlockForColumns(require_column_list);
    //
    //        if (new_query_info.prewhere_info)
    //        {
    //            if (new_query_info.prewhere_info->alias_actions)
    //                new_query_info.prewhere_info->alias_actions->execute(header);
    //
    //            new_query_info.prewhere_info->prewhere_actions->execute(header);
    //            if (new_query_info.prewhere_info->remove_prewhere_column)
    //                header.erase(new_query_info.prewhere_info->prewhere_column_name);
    //
    //            if (new_query_info.prewhere_info->remove_columns_actions)
    //                new_query_info.prewhere_info->remove_columns_actions->execute(header);
    //
    //            auto check_actions = [](const ExpressionActionsPtr & actions) {
    //                if (actions)
    //                    for (const auto & action : actions->getActions())
    //                        if (action.type == ExpressionAction::Type::JOIN || action.type == ExpressionAction::Type::ARRAY_JOIN)
    //                            throw Exception("PREWHERE cannot contain ARRAY JOIN or JOIN action", ErrorCodes::ILLEGAL_PREWHERE);
    //            };
    //
    //            check_actions(new_query_info.prewhere_info->prewhere_actions);
    //            check_actions(new_query_info.prewhere_info->alias_actions);
    //            check_actions(new_query_info.prewhere_info->remove_columns_actions);
    //        }
    //
    //        NameToNameMap name_to_name_map;
    //        for (auto & item : column_alias)
    //        {
    //            name_to_name_map[item.first] = item.second;
    //        }
    //        for (const auto & item : header)
    //        {
    //            if (name_to_name_map.contains(item.name))
    //            {
    //                new_header.emplace_back(name_to_name_map[item.name], item.type);
    //            }
    //            else
    //            {
    //                // additional columns may be generated. e.g. alias columns in PREWHERE clause:
    //                // DDL: CREATE TABLE test.prewhere_alias(`a` Int32, `b` Int32, `c` ALIAS a + b)
    //                // query: select a, (c + toInt32(1)) * 2 from test.prewhere_alias prewhere (c + toInt32(1)) * 2 = 6;
    //                new_header.emplace_back(item.name, item.type);
    //            }
    //        }
    //    }
    //
    //    size_t max_streams = build_context.max_streams;
    //    if (max_block_size < build_context.context.getSettingsRef().max_block_size)
    //        max_streams = 1; // single block single stream.
    //
    //    if (max_streams > 1 && !storage->isRemote())
    //        max_streams *= build_context.context.getSettingsRef().max_streams_to_max_threads_ratio;
    //
    //    if (dynamic_cast<StorageCloudMergeTree *>(storage.get()))
    //        build_context.context.setNameNode();
    //
    //    // for arraySetCheck function, it requires source_columns.
    //    if (new_query_info.syntax_analyzer_result == nullptr)
    //    {
    //        SyntaxAnalyzerResult result;
    //        result.source_columns = all_columns;
    //        new_query_info.syntax_analyzer_result = std::make_shared<const SyntaxAnalyzerResult>(result);
    //    }
    //
    //    auto streams = storage->read(require_column_list, new_query_info, build_context.context, processing_stage, max_block_size, max_streams);
    //    if (streams.empty())
    //    {
    //        Block header;
    //        for (const auto & item : new_header)
    //            header.insert(ColumnWithTypeAndName{item.type->createColumn(), item.type, item.name});
    //        pipeline.streams.emplace_back(std::make_shared<NullBlockInputStream>(header));
    //        return;
    //    }
    //
    //    pipeline.streams.insert(pipeline.streams.end(), streams.begin(), streams.end());
    //
    //
    auto context = build_context.context;
    const auto & settings = context->getSettingsRef();

    StreamLocalLimits limits;
    SizeLimits leaf_limits;
    std::shared_ptr<const EnabledQuota> quota;

    /// Set the limits and quota for reading data, the speed and time of the query.
    if (!options.ignore_limits)
    {
        limits = getLimitsForStorage(settings, options);
        leaf_limits = SizeLimits(settings.max_rows_to_read_leaf, settings.max_bytes_to_read_leaf, settings.read_overflow_mode_leaf);
    }

    if (!options.ignore_quota && (options.to_stage == QueryProcessingStage::Complete))
        quota = context->getQuota();

    auto table_lock = storage->lockForShare(context->getInitialQueryId(), context->getSettingsRef().lock_acquire_timeout);


    /// Table lock is stored inside pipeline here.
    pipeline.setLimits(limits);

    /**
      * Leaf size limits should be applied only for local processing of distributed queries.
      * Such limits allow to control the read stage on leaf nodes and exclude the merging stage.
      * Consider the case when distributed query needs to read from multiple shards. Then leaf
      * limits will be applied on the shards only (including the root node) but will be ignored
      * on the results merging stage.
      */
    if (!storage->isRemote())
        pipeline.setLeafLimits(leaf_limits);

    if (quota)
        pipeline.setQuota(quota);

    /// Order of resources below is important.
    if (context)
        pipeline.addInterpreterContext(std::move(context));

    if (storage)
        pipeline.addStorageHolder(std::move(storage));

    if (table_lock)
        pipeline.addTableLock(std::move(table_lock));
    //
    //    NameToNameMap name_to_name_map;
    //    for (auto & item : column_alias)
    //    {
    //        name_to_name_map[item.second] = item.first;
    //    }
    //    NamesWithAliases outputs;
    //    ASTPtr select = std::make_shared<ASTExpressionList>();
    //    for (const auto & column : new_header)
    //    {
    //        select->children.emplace_back(std::make_shared<ASTIdentifier>(column.name));
    //        if (name_to_name_map.contains(column.name))
    //        {
    //            outputs.emplace_back(name_to_name_map[column.name], column.name);
    //        }
    //    }
    //    auto action
    //        = createExpressionActions(build_context.context, pipeline.firstStream()->getHeader().getNamesAndTypesList(), outputs, select);
    //    pipeline.transform([&](auto & stream) { stream = std::make_shared<ExpressionBlockInputStream>(stream, action); });
}

void TableScanStep::serialize(WriteBuffer & buffer) const
{
    writeBinary(step_description, buffer);
    serializeBlock(output_stream->header, buffer);

    storage_id.serialize(buffer);
    query_info.serialize(buffer);
    serializeStrings(column_names, buffer);
    writeBinary(column_alias, buffer);
    writeBinary(static_cast<UInt8>(processing_stage), buffer);
    writeBinary(max_block_size, buffer);
}

QueryPlanStepPtr TableScanStep::deserialize(ReadBuffer & buffer, ContextPtr context)
{
    String step_description;
    SelectQueryInfo query_info;

    readBinary(step_description, buffer);
    auto header = deserializeBlock(buffer);
    StorageID storage_id = StorageID::deserialize(buffer, context);
    query_info.deserialize(buffer);

    UInt8 binary_stage;
    size_t max_block_size;

    Names column_names = deserializeStrings(buffer);
    NamesWithAliases columns;
    readBinary(columns, buffer);
    readBinary(binary_stage, buffer);
    auto processed_stage = static_cast<QueryProcessingStage::Enum>(binary_stage);
    readBinary(max_block_size, buffer);

    return std::make_unique<TableScanStep>(context, storage_id, columns, query_info, processed_stage, max_block_size);
}

std::shared_ptr<IQueryPlanStep> TableScanStep::copy(ContextPtr /*context*/) const
{
    SelectQueryInfo copy_query_info = query_info; // fixme@kaixi: deep copy here
    copy_query_info.query = query_info.query->clone();
    auto new_prewhere = copy_query_info.query->as<ASTSelectQuery &>().prewhere();

    return std::make_unique<TableScanStep>(
        output_stream.value(),
        storage,
        storage_id,
        original_table,
        column_names,
        column_alias,
        copy_query_info,
        processing_stage,
        max_block_size);
}

std::shared_ptr<IStorage> TableScanStep::getStorage() const
{
    return storage;
}

void TableScanStep::allocate(ContextPtr context)
{
    original_table = storage_id.table_name;
    auto * cnch = dynamic_cast<StorageCnchMergeTree *>(storage.get());

    if (!cnch)
        return;

    storage_id.database_name = cnch->getDatabaseName();
    auto prepare_res = cnch->prepareReadContext(column_names, cnch->getInMemoryMetadataPtr(),query_info, context);
    storage_id.table_name = prepare_res.local_table_name;
    storage_id.uuid = UUIDHelpers::Nil;
    if (query_info.query)
    {
        query_info = fillQueryInfo(context);
        /// trigger preallocate tables
        // if (!cnch->isOnDemandMode())
        // cnch->read(column_names, query_info, context, processed_stage, max_block_size, 1);
        // cnch->genPlanSegmentQueryAndAllocate(column_names, query_info, context);
        auto db_table = getDatabaseAndTable(query_info.query->as<ASTSelectQuery &>(), 0);
        if (!db_table->table.empty())
        {
            if (db_table->table != storage_id.table_name)
            {
                if (query_info.query)
                {
                    ASTSelectQuery & select_query = query_info.query->as<ASTSelectQuery &>();
                    select_query.replaceDatabaseAndTable(storage_id.database_name, storage_id.table_name);
                }
            }
        }
    }

}

bool TableScanStep::setFilter(const std::vector<ConstASTPtr> & filters) const
{
    auto filter = PredicateUtils::combineConjuncts(filters);
    if (PredicateUtils::isTruePredicate(filter))
        return false;

    auto * query = query_info.query->as<ASTSelectQuery>();
    auto query_filter = query->getWhere();
    if (!query_filter)
    {
        query->setExpression(ASTSelectQuery::Expression::WHERE, std::move(filter));
        return true;
    }

    auto query_filters = PredicateUtils::extractConjuncts(query_filter);
    size_t original_filters_counts = query_filters.size();
    query_filters.insert(query_filters.end(), filters.begin(), filters.end());
    auto combine_filter = PredicateUtils::combineConjuncts(query_filters);

    if (PredicateUtils::extractConjuncts(combine_filter).size() == original_filters_counts)
        return false;

    query->setExpression(ASTSelectQuery::Expression::WHERE, std::move(combine_filter));
    return true;
}

bool TableScanStep::hasFilter() const
{
    auto * query = query_info.query->as<ASTSelectQuery>();
    return query->where().get();
}

bool TableScanStep::hasLimit() const
{
    auto * query = query_info.query->as<ASTSelectQuery>();
    return query->limitLength().get();
}

static UInt64 getUIntValue(const ASTPtr & node, const ContextPtr & context)
{
    const auto & [field, type] = evaluateConstantExpression(node, context);

    if (!isNativeNumber(type))
        throw Exception(
            "Illegal type " + type->getName() + " of LIMIT expression, must be numeric type", ErrorCodes::INVALID_LIMIT_EXPRESSION);

    Field converted = convertFieldToType(field, DataTypeUInt64());
    if (converted.isNull())
        throw Exception(
            "The value " + applyVisitor(FieldVisitorToString(), field) + " of LIMIT expression is not representable as UInt64",
            ErrorCodes::INVALID_LIMIT_EXPRESSION);

    return converted.safeGet<UInt64>();
}

bool TableScanStep::setLimit(size_t limit, const ContextMutablePtr & context)
{
    auto & query = *query_info.query->as<ASTSelectQuery>();
    auto limit_length = query.getLimitLength();
    if (limit_length)
    {
        if (getUIntValue(limit_length, context) <= limit)
            return false;
    }
    query.setExpression(ASTSelectQuery::Expression::LIMIT_LENGTH, std::make_shared<ASTLiteral>(limit));

    if (!query.distinct && !query.prewhere() && !query.where() && !query.groupBy() && !query.having() && !query.orderBy()
        && !query.limitBy() && query.limitLength() && limit < max_block_size)
    {
        max_block_size = std::max(static_cast<size_t>(1), limit);
    }

    return true;
}

QueryProcessingStage::Enum TableScanStep::getProcessedStage() const
{
    return processing_stage;
}

size_t TableScanStep::getMaxBlockSize() const
{
    return max_block_size;
}

ASTPtr
TableScanStep::rewriteDynamicFilter(const ASTPtr & filter, QueryPipeline & pipeline, const BuildQueryPipelineSettings & build_context)
{
    if (!filter)
        return nullptr;

    auto filters = DynamicFilters::extractDynamicFilters(filter);
    if (filters.first.empty())
        return filter;

    std::vector<ConstASTPtr> predicates = std::move(filters.second);
    for (auto & dynamic_filter : filters.first)
    {
        auto description = DynamicFilters::extractDescription(dynamic_filter).value();
        pipeline.addRuntimeFilterHolder(RuntimeFilterHolder{
            build_context.distributed_settings.query_id, build_context.distributed_settings.plan_segment_id, description.id});

        if (description.type == DynamicFilterType::Range)
        {
            auto dynamic_filters = DynamicFilters::createDynamicFilterRuntime(
                description,
                build_context.context->getInitialQueryId(),
                build_context.distributed_settings.plan_segment_id,
                build_context.context->getSettingsRef().wait_runtime_filter_timeout,
                RuntimeFilterManager::getInstance(),
                "TableScan");
            predicates.insert(predicates.end(), dynamic_filters.begin(), dynamic_filters.end());
        }
    }

    return PredicateUtils::combineConjuncts(predicates);
}


}
