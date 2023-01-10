/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Interpreters/InterpreterInsertQuery.h>

#include <Access/AccessFlags.h>
#include <DataStreams/AddingDefaultBlockOutputStream.h>
#include <DataStreams/CheckConstraintsBlockOutputStream.h>
#include <DataStreams/CountingBlockOutputStream.h>
#include <Processors/Transforms/getSourceFromFromASTInsertQuery.h>
#include <DataStreams/PushingToViewsBlockOutputStream.h>
#include <DataStreams/SquashingBlockOutputStream.h>
#include <DataStreams/SquashingBlockInputStream.h>
#include <DataStreams/UnionBlockInputStream.h>
#include <DataStreams/OwningBlockInputStream.h>
#include <DataStreams/NullAndDoCopyBlockInputStream.h>
#include <DataStreams/copyData.h>
#include <IO/ConnectionTimeoutsContext.h>
#include <IO/SnappyReadBuffer.h>
#include <Interpreters/InterpreterSelectWithUnionQuery.h>
#include <Interpreters/InterpreterWatchQuery.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Processors/Sources/SinkToOutputStream.h>
#include <Processors/Sources/SourceFromInputStream.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <Storages/StorageDistributed.h>
#include <Storages/StorageMaterializedView.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <TableFunctions/TableFunctionFactory.h>
#include <Common/checkStackSize.h>
#include <Interpreters/Context.h>
#include <Interpreters/QueryLog.h>
#include <Interpreters/TranslateQualifiedNamesVisitor.h>
#include <Interpreters/getTableExpressions.h>
#include <Interpreters/processColumnTransformers.h>
#include <Interpreters/trySetVirtualWarehouse.h>
#include <CloudServices/CnchServerResource.h>
#include <DataTypes/DataTypeNullable.h>
#include <Columns/ColumnNullable.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int NO_SUCH_COLUMN_IN_TABLE;
    extern const int ILLEGAL_COLUMN;
    extern const int DUPLICATE_COLUMN;
}

InterpreterInsertQuery::InterpreterInsertQuery(
    const ASTPtr & query_ptr_, ContextPtr context_, bool allow_materialized_, bool no_squash_, bool no_destination_)
    : WithContext(context_)
    , query_ptr(query_ptr_)
    , allow_materialized(allow_materialized_)
    , no_squash(no_squash_)
    , no_destination(no_destination_)
{
    checkStackSize();
}


StoragePtr InterpreterInsertQuery::getTable(ASTInsertQuery & query)
{
    if (query.table_function)
    {
        const auto & factory = TableFunctionFactory::instance();
        TableFunctionPtr table_function_ptr = factory.get(query.table_function, getContext());
        return table_function_ptr->execute(query.table_function, getContext(), table_function_ptr->getName());
    }

    query.table_id = getContext()->resolveStorageID(query.table_id);
    return DatabaseCatalog::instance().getTable(query.table_id, getContext());
}

Block InterpreterInsertQuery::getSampleBlock(
    const ASTInsertQuery & query,
    const StoragePtr & table,
    const StorageMetadataPtr & metadata_snapshot) const
{
    /// If the query does not include information about columns
    if (!query.columns)
    {
        if (no_destination)
            return metadata_snapshot->getSampleBlockWithVirtuals(table->getVirtuals());
        else
            return metadata_snapshot->getSampleBlockNonMaterialized();
    }

    Block table_sample_non_materialized;
    if (no_destination)
        table_sample_non_materialized = metadata_snapshot->getSampleBlockWithVirtuals(table->getVirtuals());
    else
        table_sample_non_materialized = metadata_snapshot->getSampleBlockNonMaterialized(/*include_func_columns*/ true);

    Block table_sample = metadata_snapshot->getSampleBlock(/*include_func_columns*/ true);

    const auto columns_ast = processColumnTransformers(getContext()->getCurrentDatabase(), table, metadata_snapshot, query.columns);

    /// Form the block based on the column names from the query
    Block res;
    for (const auto & identifier : columns_ast->children)
    {
        std::string current_name = identifier->getColumnName();

        /// The table does not have a column with that name
        if (!table_sample.has(current_name))
            throw Exception("No such column " + current_name + " in table " + table->getStorageID().getNameForLogs(),
                ErrorCodes::NO_SUCH_COLUMN_IN_TABLE);

        if (!allow_materialized && !table_sample_non_materialized.has(current_name))
            throw Exception("Cannot insert column " + current_name + ", because it is MATERIALIZED column.", ErrorCodes::ILLEGAL_COLUMN);
        if (res.has(current_name))
            throw Exception("Column " + current_name + " specified more than once", ErrorCodes::DUPLICATE_COLUMN);

        res.insert(ColumnWithTypeAndName(table_sample.getByName(current_name).type, current_name));
    }
    return res;
}


/** A query that just reads all data without any complex computations or filetering.
  * If we just pipe the result to INSERT, we don't have to use too many threads for read.
  */
static bool isTrivialSelect(const ASTPtr & select)
{
    if (auto * select_query = select->as<ASTSelectQuery>())
    {
        const auto & tables = select_query->tables();

        if (!tables)
            return false;

        const auto & tables_in_select_query = tables->as<ASTTablesInSelectQuery &>();

        if (tables_in_select_query.children.size() != 1)
            return false;

        const auto & child = tables_in_select_query.children.front();
        const auto & table_element = child->as<ASTTablesInSelectQueryElement &>();
        const auto & table_expr = table_element.table_expression->as<ASTTableExpression &>();

        if (table_expr.subquery)
            return false;

        /// Note: how to write it in more generic way?
        return (!select_query->distinct
            && !select_query->limit_with_ties
            && !select_query->prewhere()
            && !select_query->where()
            && !select_query->groupBy()
            && !select_query->having()
            && !select_query->orderBy()
            && !select_query->limitBy());
    }
    /// This query is ASTSelectWithUnionQuery subquery
    return false;
};


BlockIO InterpreterInsertQuery::execute()
{
    const Settings & settings = getContext()->getSettingsRef();
    auto & query = query_ptr->as<ASTInsertQuery &>();

    BlockIO res;

    StoragePtr table = getTable(query);
    auto table_lock = table->lockForShare(getContext()->getInitialQueryId(), settings.lock_acquire_timeout);
    auto metadata_snapshot = table->getInMemoryMetadataPtr();

    auto query_sample_block = getSampleBlock(query, table, metadata_snapshot);
    if (!query.table_function)
        getContext()->checkAccess(AccessType::INSERT, query.table_id, query_sample_block.getNames());

    bool is_distributed_insert_select = false;

    if (query.select && table->isRemote() &&
        (settings.parallel_distributed_insert_select || settings.distributed_perfect_shard))
    {
        // Distributed INSERT SELECT
        if (auto maybe_pipeline = table->distributedWrite(query, getContext()))
        {
            res.pipeline = std::move(*maybe_pipeline);
            is_distributed_insert_select = true;
        }
    }

    /// Directly forward the query to cnch worker if select or infile
    if (getContext()->getServerType() == ServerType::cnch_server && (query.select || query.in_file) && table->isRemote())
    {
        /// Handle the insert commit for insert select/infile case in cnch server.
        table->write(query_ptr, metadata_snapshot, getContext());

        bool enable_staging_area_for_write = settings.enable_staging_area_for_write;
        if (const auto * cnch_table = dynamic_cast<const StorageCnchMergeTree *>(table.get());
            cnch_table && metadata_snapshot->hasUniqueKey() && !enable_staging_area_for_write)
        {
            /// for unique table, insert select|infile is committed from worker side
            return {};
        }
        else
        {
            TransactionCnchPtr txn = getContext()->getCurrentTransaction();
            txn->setMainTableUUID(table->getStorageUUID());
            txn->commitV2();
            LOG_TRACE(&Logger::get("InterpreterInsertQuery::execute"), "Finish insert infile/select commit in cnch server.");
            return {};
        }
    }

    BlockOutputStreams out_streams;
    if (!is_distributed_insert_select || query.watch)
    {
        size_t out_streams_size = 1;
        if (query.select)
        {
            auto insert_select_context = Context::createCopy(getContext());
            /// Cannot use trySetVirtualWarehouseAndWorkerGroup, because it only works in server node
            if (auto * cloud_table = dynamic_cast<StorageCloudMergeTree *>(table.get()))
            {
                auto worker_group = getWorkerGroupForTable(*cloud_table, insert_select_context);
                insert_select_context->setCurrentWorkerGroup(worker_group);
                /// set worker group for select query
                insert_select_context->initCnchServerResource(insert_select_context->getCurrentTransactionID());
                LOG_DEBUG(
                    &Logger::get("VirtualWarehouse"),
                    "Set worker group {} for table {}", worker_group->getQualifiedName(), cloud_table->getStorageID().getNameForLogs());
            }

            bool is_trivial_insert_select = false;

            if (settings.optimize_trivial_insert_select)
            {
                const auto & select_query = query.select->as<ASTSelectWithUnionQuery &>();
                const auto & selects = select_query.list_of_selects->children;
                const auto & union_modes = select_query.list_of_modes;

                /// ASTSelectWithUnionQuery is not normalized now, so it may pass some queries which can be Trivial select queries
                const auto mode_is_all = [](const auto & mode) { return mode == ASTSelectWithUnionQuery::Mode::ALL; };

                is_trivial_insert_select =
                    std::all_of(union_modes.begin(), union_modes.end(), std::move(mode_is_all))
                    && std::all_of(selects.begin(), selects.end(), isTrivialSelect);
            }

            if (insert_select_context->getServerType() == ServerType::cnch_worker)
            {
                Settings new_settings = insert_select_context->getSettings();
                new_settings.prefer_cnch_catalog = true;
                insert_select_context->setSettings(std::move(new_settings));
            }

            if (is_trivial_insert_select)
            {
                /** When doing trivial INSERT INTO ... SELECT ... FROM table,
                  * don't need to process SELECT with more than max_insert_threads
                  * and it's reasonable to set block size for SELECT to the desired block size for INSERT
                  * to avoid unnecessary squashing.
                  */

                Settings new_settings = insert_select_context->getSettings();

                new_settings.max_threads = std::max<UInt64>(1, settings.max_insert_threads);

                if (table->prefersLargeBlocks())
                {
                    if (settings.min_insert_block_size_rows)
                        new_settings.max_block_size = settings.min_insert_block_size_rows;
                    if (settings.min_insert_block_size_bytes)
                        new_settings.preferred_block_size_bytes = settings.min_insert_block_size_bytes;
                }

                insert_select_context->setSettings(new_settings);

                InterpreterSelectWithUnionQuery interpreter_select{
                    query.select, insert_select_context, SelectQueryOptions(QueryProcessingStage::Complete, 1)};
                res = interpreter_select.execute();
            }
            else
            {
                /// Passing 1 as subquery_depth will disable limiting size of intermediate result.
                InterpreterSelectWithUnionQuery interpreter_select{
                    query.select, insert_select_context, SelectQueryOptions(QueryProcessingStage::Complete, 1)};
                res = interpreter_select.execute();
            }

            res.pipeline.dropTotalsAndExtremes();

            if (table->supportsParallelInsert() && settings.max_insert_threads > 1)
                out_streams_size = std::min(size_t(settings.max_insert_threads), res.pipeline.getNumStreams());

            res.pipeline.resize(out_streams_size);

            /// Allow to insert Nullable into non-Nullable columns, NULL values will be added as defaults values.
            if (getContext()->getSettingsRef().insert_null_as_default)
            {
                const auto & input_columns = res.pipeline.getHeader().getColumnsWithTypeAndName();
                const auto & query_columns = query_sample_block.getColumnsWithTypeAndName();
                const auto & output_columns = metadata_snapshot->getColumns();

                if (input_columns.size() == query_columns.size())
                {
                    for (size_t col_idx = 0; col_idx < query_columns.size(); ++col_idx)
                    {
                        /// Change query sample block columns to Nullable to allow inserting nullable columns, where NULL values will be substituted with
                        /// default column values (in AddingDefaultBlockOutputStream), so all values will be cast correctly.
                        if (input_columns[col_idx].type->isNullable() && !query_columns[col_idx].type->isNullable() && output_columns.hasDefault(query_columns[col_idx].name))
                            query_sample_block.setColumn(col_idx, ColumnWithTypeAndName(makeNullable(query_columns[col_idx].column), makeNullable(query_columns[col_idx].type), query_columns[col_idx].name));
                    }
                }
            }
        }
        else if (query.watch)
        {
            InterpreterWatchQuery interpreter_watch{ query.watch, getContext() };
            res = interpreter_watch.execute();
            res.pipeline.init(Pipe(std::make_shared<SourceFromInputStream>(std::move(res.in))));
        }

        for (size_t i = 0; i < out_streams_size; i++)
        {
            /// We create a pipeline of several streams, into which we will write data.
            BlockOutputStreamPtr out;

            /// NOTE: we explicitly ignore bound materialized views when inserting into Kafka Storage.
            ///       Otherwise we'll get duplicates when MV reads same rows again from Kafka.
            if (table->noPushingToViews() && !no_destination)
                out = table->write(query_ptr, metadata_snapshot, getContext());
            else
                out = std::make_shared<PushingToViewsBlockOutputStream>(table, metadata_snapshot, getContext(), query_ptr, no_destination);

            /// Note that we wrap transforms one on top of another, so we write them in reverse of data processing order.

            /// Checking constraints. It must be done after calculation of all defaults, so we can check them on calculated columns.
            if (const auto & constraints = metadata_snapshot->getConstraints(); !constraints.empty())
                out = std::make_shared<CheckConstraintsBlockOutputStream>(
                    query.table_id, out, out->getHeader(), metadata_snapshot->getConstraints(), getContext());

            bool null_as_default = query.select && getContext()->getSettingsRef().insert_null_as_default;

            /// Actually we don't know structure of input blocks from query/table,
            /// because some clients break insertion protocol (columns != header)
            out = std::make_shared<AddingDefaultBlockOutputStream>(
                out, query_sample_block, metadata_snapshot->getColumns(), getContext(), null_as_default);

            /// It's important to squash blocks as early as possible (before other transforms),
            ///  because other transforms may work inefficient if block size is small.

            /// Do not squash blocks if it is a sync INSERT into Distributed, since it lead to double bufferization on client and server side.
            /// Client-side bufferization might cause excessive timeouts (especially in case of big blocks).
            if (!(settings.insert_distributed_sync && table->isRemote()) && !no_squash && !query.watch)
            {
                bool table_prefers_large_blocks = table->prefersLargeBlocks();

                out = std::make_shared<SquashingBlockOutputStream>(
                    out,
                    out->getHeader(),
                    table_prefers_large_blocks ? settings.min_insert_block_size_rows : settings.max_block_size,
                    table_prefers_large_blocks ? settings.min_insert_block_size_bytes : 0);
            }

            auto out_wrapper = std::make_shared<CountingBlockOutputStream>(out);
            out_wrapper->setProcessListElement(getContext()->getProcessListElement());
            out_streams.emplace_back(std::move(out_wrapper));
        }
    }

    /// What type of query: INSERT or INSERT SELECT or INSERT WATCH?
    if (is_distributed_insert_select)
    {
        /// Pipeline was already built.
    }
    else if (query.in_file)
    {
        // read data stream from in_file, and copy it to out
        // Special handling in_file based on url type:
        String uristr = typeid_cast<const ASTLiteral &>(*query.in_file).value.safeGet<String>();
        // create Datastream based on Format:
        String format = query.format;
        if (format.empty())
            format = getContext()->getDefaultFormat();

        auto input_stream = buildInputStreamFromSource(getContext(), out_streams.at(0)->getHeader(), settings, uristr, format);

        res.in = std::make_shared<NullAndDoCopyBlockInputStream>(input_stream, out_streams.at(0));
        res.out = nullptr;
    }
    else if (query.select || query.watch)
    {
        const auto & header = out_streams.at(0)->getHeader();
        auto actions_dag = ActionsDAG::makeConvertingActions(
                res.pipeline.getHeader().getColumnsWithTypeAndName(),
                header.getColumnsWithTypeAndName(),
                ActionsDAG::MatchColumnsMode::Position);
        auto actions = std::make_shared<ExpressionActions>(actions_dag, ExpressionActionsSettings::fromContext(getContext(), CompileExpressions::yes));

        res.pipeline.addSimpleTransform([&](const Block & in_header) -> ProcessorPtr
        {
            return std::make_shared<ExpressionTransform>(in_header, actions);
        });

        res.pipeline.setSinks([&](const Block &, QueryPipeline::StreamType type) -> ProcessorPtr
        {
            if (type != QueryPipeline::StreamType::Main)
                return nullptr;

            auto stream = std::move(out_streams.back());
            out_streams.pop_back();

            return std::make_shared<SinkToOutputStream>(std::move(stream));
        });

        if (!allow_materialized)
        {
            for (const auto & column : metadata_snapshot->getColumns())
                if (column.default_desc.kind == ColumnDefaultKind::Materialized && header.has(column.name))
                    throw Exception("Cannot insert column " + column.name + ", because it is MATERIALIZED column.", ErrorCodes::ILLEGAL_COLUMN);
        }
    }
    else if (query.data && !query.has_tail) /// can execute without additional data
    {
        auto pipe = getSourceFromFromASTInsertQuery(query_ptr, nullptr, query_sample_block, getContext(), nullptr);
        res.pipeline.init(std::move(pipe));
        res.pipeline.resize(1);
        res.pipeline.setSinks([&](const Block &, Pipe::StreamType)
        {
            return std::make_shared<SinkToOutputStream>(out_streams.at(0));
        });
    }
    else
        res.out = std::move(out_streams.at(0));

    res.pipeline.addStorageHolder(table);
    if (const auto * mv = dynamic_cast<const StorageMaterializedView *>(table.get()))
    {
        if (auto inner_table = mv->tryGetTargetTable())
            res.pipeline.addStorageHolder(inner_table);
    }

    table->setUpdateTimeNow();
    return res;
}


StorageID InterpreterInsertQuery::getDatabaseTable() const
{
    return query_ptr->as<ASTInsertQuery &>().table_id;
}


void InterpreterInsertQuery::extendQueryLogElemImpl(QueryLogElement & elem, const ASTPtr &, ContextPtr context_) const
{
    elem.query_kind = "Insert";
    const auto & insert_table = context_->getInsertionTable();
    if (!insert_table.empty())
    {
        elem.query_databases.insert(insert_table.getDatabaseName());
        elem.query_tables.insert(insert_table.getFullNameNotQuoted());
    }
}

BlockInputStreamPtr InterpreterInsertQuery::buildInputStreamFromSource(const ContextPtr context_ptr, const Block & sample, const Settings & settings, const String & source_uri, const String & format)
{
    // Assume no query and fragment in uri, todo, add sanity check
    String fuzzyFileNames;
    String uriPrefix = source_uri.substr(0, source_uri.find_last_of('/'));
    if (uriPrefix.length() == source_uri.length())
    {
        fuzzyFileNames = source_uri;
        uriPrefix.clear();
    }
    else
    {
        uriPrefix += "/";
        fuzzyFileNames = source_uri.substr(uriPrefix.length());
    }

    Poco::URI uri(uriPrefix);
    String scheme = uri.getScheme();

    BlockInputStreams inputs;
    // For HDFS inputs with fuzzname, let ReadBufferFromByteHDFS to handle multiple files
#if USE_HDFS
    if (DB::isHdfsOrCfsScheme(scheme))
    {
        std::unique_ptr<ReadBuffer> read_buf = std::make_unique<ReadBufferFromByteHDFS>(source_uri, false, context_ptr->getHdfsConnectionParams(), DBMS_DEFAULT_BUFFER_SIZE, nullptr,  0,  false, context_ptr->getProcessList().getHDFSDownloadThrottler());
        // snappy compression suport
        if (endsWith(source_uri, "snappy"))
        {
            if (settings.snappy_format_blocked)
            {
                read_buf = std::make_unique<SnappyReadBuffer<true> >(std::move(read_buf));
            }
            else
            {
                read_buf = std::make_unique<SnappyReadBuffer<false> >(std::move(read_buf));
            }
        }
        inputs.emplace_back(
            std::make_shared<OwningBlockInputStream<ReadBuffer>>(
                context_ptr->getInputFormat(format, *read_buf,
                                        sample, // sample_block
                                        settings.max_insert_block_size),
                std::move(read_buf)));
    }
    else
#endif
    {
        std::vector<String> fuzzyNameList = parseDescription(fuzzyFileNames, 0, fuzzyFileNames.length(), ',' , 100/* hard coded max files */);
        std::vector<std::vector<String> > fileNames;
        for(auto fuzzyName : fuzzyNameList)
            fileNames.push_back(parseDescription(fuzzyName, 0, fuzzyName.length(), '|', 100));

        for (auto & vecNames : fileNames)
        {
            for (auto & name: vecNames)
            {
                std::unique_ptr<ReadBuffer> read_buf = nullptr;

                if (scheme.empty() || scheme == "file")
                {
                    read_buf = std::make_unique<ReadBufferFromFile>(Poco::URI(uriPrefix + name).getPath());
                }
#if USE_HDFS
                else if (DB::isHdfsOrCfsScheme(scheme))
                {
                    read_buf = std::make_unique<ReadBufferFromByteHDFS>(uriPrefix + name, false, context_ptr->getHdfsConnectionParams(), DBMS_DEFAULT_BUFFER_SIZE, nullptr,  0,  false, context_ptr->getProcessList().getHDFSDownloadThrottler());
                }
#endif
                else
                {
                    throw Exception("URI scheme " + scheme + " is not supported with insert statement yet", ErrorCodes::NOT_IMPLEMENTED);
                }

                // snappy compression suport
                if (endsWith(name, "snappy"))
                {
                    if (settings.snappy_format_blocked)
                    {
                        read_buf = std::make_unique<SnappyReadBuffer<true> >(std::move(read_buf));
                    }
                    else
                    {
                        read_buf = std::make_unique<SnappyReadBuffer<false> >(std::move(read_buf));
                    }
                }
                inputs.emplace_back(
                        std::make_shared<OwningBlockInputStream<ReadBuffer>>(
                            context_ptr->getInputFormat(format, *read_buf,
                                sample, // sample_block
                                settings.max_insert_block_size),
                            std::move(read_buf)));
            }
        }
    }

    if (inputs.size() == 0)
        throw Exception("Inputs interpreter error", ErrorCodes::LOGICAL_ERROR);

    auto stream = inputs[0];
    if (inputs.size() > 1)
    {
        // Squash is used to generate larger(less part to merge later)
        stream = std::make_shared<SquashingBlockInputStream>(
            std::make_shared<UnionBlockInputStream>(inputs, nullptr, settings.max_distributed_connections),
            settings.min_insert_block_size_rows,
            settings.min_insert_block_size_bytes);
    }

    return stream;
}

}
