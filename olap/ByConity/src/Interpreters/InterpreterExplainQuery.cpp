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

#include <Interpreters/InterpreterExplainQuery.h>

#include <DataStreams/BlockIO.h>
#include <DataStreams/OneBlockInputStream.h>
#include <DataTypes/DataTypeString.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Interpreters/InterpreterSelectWithUnionQuery.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/InterpreterSelectQueryUseOptimizer.h>
#include <Interpreters/DistributedStages/InterpreterDistributedStages.h>
#include <Interpreters/Context.h>
#include <Interpreters/predicateExpressionsUtils.h>
#include <Formats/FormatFactory.h>
#include <Optimizer/QueryUseOptimizerChecker.h>
#include <Optimizer/CardinalityEstimate/CardinalityEstimator.h>
#include <Optimizer/CostModel/CostCalculator.h>
#include <Parsers/DumpASTNode.h>
#include <Parsers/queryToString.h>
#include <Parsers/ASTExplainQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ParserQuery.h>
#include <Storages/StorageDistributed.h>
#include <Storages/StorageView.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/PlanPrinter.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <Processors/printPipeline.h>
#include <Storages/StorageMaterializedView.h>
#include <Common/JSONBuilder.h>
#include <IO/WriteBufferFromString.h>

namespace DB
{

static int fls(int x)
{
    int position;
    int i;
    if(0 != x)
    {
        for (i = (x >> 1), position = 0; i != 0; ++position)
            i >>= 1;
    }
    else
        position = -1;
    return position+1;
}

static unsigned int roundup_pow_of_two(unsigned int x)
{
    if (x <= 1)
        return 2;
    else
        return 1UL << fls(x - 1);
}

namespace ErrorCodes
{
    extern const int INCORRECT_QUERY;
    extern const int INVALID_SETTING_VALUE;
    extern const int UNKNOWN_SETTING;
    extern const int LOGICAL_ERROR;
}

namespace
{
    struct ExplainAnalyzedSyntaxMatcher
    {
        struct Data : public WithContext
        {
            explicit Data(ContextPtr context_) : WithContext(context_) {}
        };

        static bool needChildVisit(ASTPtr & node, ASTPtr &)
        {
            return !node->as<ASTSelectQuery>();
        }

        static void visit(ASTPtr & ast, Data & data)
        {
            if (auto * select = ast->as<ASTSelectQuery>())
                visit(*select, ast, data);
        }

        static void visit(ASTSelectQuery & select, ASTPtr & node, Data & data)
        {
            InterpreterSelectQuery interpreter(
                node, data.getContext(), SelectQueryOptions(QueryProcessingStage::FetchColumns).analyze().modify());

            const SelectQueryInfo & query_info = interpreter.getQueryInfo();
            if (query_info.view_query)
            {
                ASTPtr tmp;
                StorageView::replaceWithSubquery(select, query_info.view_query->clone(), tmp);
            }
        }
    };

    using ExplainAnalyzedSyntaxVisitor = InDepthNodeVisitor<ExplainAnalyzedSyntaxMatcher, true>;

}

BlockIO InterpreterExplainQuery::execute()
{
    BlockIO res;
    res.in = executeImpl();
    return res;
}


Block InterpreterExplainQuery::getSampleBlock()
{
    Block block;
    const auto & ast = query->as<ASTExplainQuery &>();
    if (ast.getKind() == ASTExplainQuery::MaterializedView)
    {
        auto view_table_column = ColumnString::create();
        auto hit_view = ColumnUInt8::create();
        auto match_cost = ColumnInt64::create();
        auto read_cost = ColumnUInt64::create();
        auto view_inner_query = ColumnString::create();
        auto miss_match_reason = ColumnString::create();
        block.insert({std::move(view_table_column), std::make_shared<DataTypeString>(), "view_table"});
        block.insert({std::move(hit_view), std::make_shared<DataTypeUInt8>(), "match_result"});
        block.insert({std::move(match_cost), std::make_shared<DataTypeInt64>(), "match_cost"});
        block.insert({std::move(read_cost), std::make_shared<DataTypeUInt64>(), "read_cost"});
        block.insert({std::move(view_inner_query), std::make_shared<DataTypeString>(), "view_query"});
        block.insert({std::move(miss_match_reason), std::make_shared<DataTypeString>(), "miss_reason"});
    }
    else
    {
        ColumnWithTypeAndName col;
        col.name = "explain";
        col.type = std::make_shared<DataTypeString>();
        col.column = col.type->createColumn();
        block.insert(col);
    }
    return block;
}

/// Split str by line feed and write as separate row to ColumnString.
static void fillColumn(IColumn & column, const std::string & str)
{
    size_t start = 0;
    size_t end = 0;
    size_t size = str.size();

    while (end < size)
    {
        if (str[end] == '\n')
        {
            column.insertData(str.data() + start, end - start);
            start = end + 1;
        }

        ++end;
    }

    if (start < end)
        column.insertData(str.data() + start, end - start);
}

namespace
{

/// Settings. Different for each explain type.

struct QueryPlanSettings
{
    QueryPlan::ExplainPlanOptions query_plan_options;

    /// Apply query plan optimizations.
    bool optimize = true;
    bool json = false;

    constexpr static char name[] = "PLAN";

    std::unordered_map<std::string, std::reference_wrapper<bool>> boolean_settings =
    {
            {"header", query_plan_options.header},
            {"description", query_plan_options.description},
            {"actions", query_plan_options.actions},
            {"indexes", query_plan_options.indexes},
            {"optimize", optimize},
            {"json", json}
    };
};

struct QueryPipelineSettings
{
    QueryPlan::ExplainPipelineOptions query_pipeline_options;
    bool graph = false;
    bool compact = true;

    constexpr static char name[] = "PIPELINE";

    std::unordered_map<std::string, std::reference_wrapper<bool>> boolean_settings =
    {
            {"header", query_pipeline_options.header},
            {"graph", graph},
            {"compact", compact},
    };
};

template <typename Settings>
struct ExplainSettings : public Settings
{
    using Settings::boolean_settings;

    bool has(const std::string & name_) const
    {
        return boolean_settings.count(name_) > 0;
    }

    void setBooleanSetting(const std::string & name_, bool value)
    {
        auto it = boolean_settings.find(name_);
        if (it == boolean_settings.end())
            throw Exception("Unknown setting for ExplainSettings: " + name_, ErrorCodes::LOGICAL_ERROR);

        it->second.get() = value;
    }

    std::string getSettingsList() const
    {
        std::string res;
        for (const auto & setting : boolean_settings)
        {
            if (!res.empty())
                res += ", ";

            res += setting.first;
        }

        return res;
    }
};

template <typename Settings>
ExplainSettings<Settings> checkAndGetSettings(const ASTPtr & ast_settings)
{
    if (!ast_settings)
        return {};

    ExplainSettings<Settings> settings;
    const auto & set_query = ast_settings->as<ASTSetQuery &>();

    for (const auto & change : set_query.changes)
    {
        if (!settings.has(change.name))
            throw Exception("Unknown setting \"" + change.name + "\" for EXPLAIN " + Settings::name + " query. "
                            "Supported settings: " + settings.getSettingsList(), ErrorCodes::UNKNOWN_SETTING);

        if (change.value.getType() != Field::Types::UInt64)
            throw Exception("Invalid type " + std::string(change.value.getTypeName()) + " for setting \"" + change.name +
                            "\" only boolean settings are supported", ErrorCodes::INVALID_SETTING_VALUE);

        auto value = change.value.get<UInt64>();
        if (value > 1)
            throw Exception("Invalid value " + std::to_string(value) + " for setting \"" + change.name +
                            "\". Only boolean settings are supported", ErrorCodes::INVALID_SETTING_VALUE);

        settings.setBooleanSetting(change.name, value);
    }

    return settings;
}

}

void InterpreterExplainQuery::rewriteDistributedToLocal(ASTPtr & ast)
{
    if (!ast)
        return;

    if (ASTSelectWithUnionQuery * select_with_union = ast->as<ASTSelectWithUnionQuery>())
    {
        for (auto & child : select_with_union->list_of_selects->children)
            rewriteDistributedToLocal(child);
    }
    else if (ASTTableExpression * table = ast->as<ASTTableExpression>())
    {
        if (table->subquery)
            rewriteDistributedToLocal(table->subquery);
        else if (table->database_and_table_name)
        {
            auto target_table_id = getContext()->resolveStorageID(table->database_and_table_name);
            auto storage = DatabaseCatalog::instance().getTable(target_table_id, getContext());
            auto * distributed = dynamic_cast<StorageDistributed *>(storage.get());

            if (!distributed)
                return;

            String table_alias = table->database_and_table_name->tryGetAlias();
            auto old_table_ast = std::find(table->children.begin(), table->children.end(), table->database_and_table_name);
            table->database_and_table_name = std::make_shared<ASTTableIdentifier>(distributed->getRemoteDatabaseName(), distributed->getRemoteTableName());
            *old_table_ast = table->database_and_table_name;

            if (!table_alias.empty())
                table->database_and_table_name->setAlias(table_alias);
        }
    }
    else
    {
        for (auto & child : ast->children)
            rewriteDistributedToLocal(child);
    }
}

BlockInputStreamPtr InterpreterExplainQuery::executeImpl()
{
    const auto & ast = query->as<ASTExplainQuery &>();

    Block sample_block = getSampleBlock();
    MutableColumns res_columns = sample_block.cloneEmptyColumns();

    WriteBufferFromOwnString buf;
    bool single_line = false;

    // if settings.enable_optimizer = true && query is supported by optimizer, print plan with optimizer.
    // if query is not supported by optimizer, settings `settings.enable_optimizer` in context will be disabled.
    if (QueryUseOptimizerChecker::check(query, getContext()))
    {
        explainUsingOptimizer(query, buf, single_line);
    }
    else if (ast.getKind() == ASTExplainQuery::ParsedAST)
    {
        if (ast.getSettings())
            throw Exception("Settings are not supported for EXPLAIN AST query.", ErrorCodes::UNKNOWN_SETTING);

        dumpAST(*ast.getExplainedQuery(), buf);
    }
    else if (ast.getKind() == ASTExplainQuery::AnalyzedSyntax)
    {
        if (ast.getSettings())
            throw Exception("Settings are not supported for EXPLAIN SYNTAX query.", ErrorCodes::UNKNOWN_SETTING);

        ExplainAnalyzedSyntaxVisitor::Data data(getContext());
        ExplainAnalyzedSyntaxVisitor(data).visit(query);

        ast.getExplainedQuery()->format(IAST::FormatSettings(buf, false));
    }
    else if (ast.getKind() == ASTExplainQuery::QueryPlan)
    {
        if (!dynamic_cast<const ASTSelectWithUnionQuery *>(ast.getExplainedQuery().get()))
            throw Exception("Only SELECT is supported for EXPLAIN query", ErrorCodes::INCORRECT_QUERY);

        auto settings = checkAndGetSettings<QueryPlanSettings>(ast.getSettings());
        QueryPlan plan;

        InterpreterSelectWithUnionQuery interpreter(ast.getExplainedQuery(), getContext(), SelectQueryOptions());
        interpreter.buildQueryPlan(plan);

        if (settings.optimize)
            plan.optimize(QueryPlanOptimizationSettings::fromContext(getContext()));

        if (settings.json)
        {
            /// Add extra layers to make plan look more like from postgres.
            auto plan_map = std::make_unique<JSONBuilder::JSONMap>();
            plan_map->add("Plan", plan.explainPlan(settings.query_plan_options));
            auto plan_array = std::make_unique<JSONBuilder::JSONArray>();
            plan_array->add(std::move(plan_map));

            auto format_settings = getFormatSettings(getContext());
            format_settings.json.quote_64bit_integers = false;

            JSONBuilder::FormatSettings json_format_settings{.settings = format_settings};
            JSONBuilder::FormatContext format_context{.out = buf};

            plan_array->format(json_format_settings, format_context);

            single_line = true;
        }
        else
            plan.explainPlan(buf, settings.query_plan_options);
    }
    else if (ast.getKind() == ASTExplainQuery::QueryPipeline)
    {
        if (!dynamic_cast<const ASTSelectWithUnionQuery *>(ast.getExplainedQuery().get()))
            throw Exception("Only SELECT is supported for EXPLAIN query", ErrorCodes::INCORRECT_QUERY);

        auto settings = checkAndGetSettings<QueryPipelineSettings>(ast.getSettings());
        QueryPlan plan;

        InterpreterSelectWithUnionQuery interpreter(ast.getExplainedQuery(), getContext(), SelectQueryOptions());
        interpreter.buildQueryPlan(plan);
        auto pipeline = plan.buildQueryPipeline(
            QueryPlanOptimizationSettings::fromContext(getContext()),
            BuildQueryPipelineSettings::fromContext(getContext()));

        if (settings.graph)
        {
            /// Pipe holds QueryPlan, should not go out-of-scope
            auto pipe = QueryPipeline::getPipe(std::move(*pipeline));
            const auto & processors = pipe.getProcessors();

            if (settings.compact)
                printPipelineCompact(processors, buf, settings.query_pipeline_options.header);
            else
                printPipeline(processors, buf);
        }
        else
        {
            plan.explainPipeline(buf, settings.query_pipeline_options);
        }
    }
    else if (ast.getKind() == ASTExplainQuery::MaterializedView)
    {
        /// rewrite distributed table to local table
        ASTPtr explain_query = ast.getExplainedQuery()->clone();
        rewriteDistributedToLocal(explain_query);
        ASTSelectWithUnionQuery * select_union_query = explain_query->as<ASTSelectWithUnionQuery>();
        if (select_union_query && !select_union_query->list_of_selects->children.empty())
        {
            Block block;
            auto view_table_column = ColumnString::create();
            auto hit_view = ColumnUInt8::create();
            auto match_cost = ColumnInt64::create();
            auto read_cost = ColumnUInt64::create();
            auto view_inner_query = ColumnString::create();
            auto miss_match_reason = ColumnString::create();

            /// Enable enable_view_based_query_rewrite setting to get view match result
            ContextMutablePtr context = Context::createCopy(getContext());
            context->setSetting("enable_view_based_query_rewrite", 1);
            for (const auto & element : select_union_query->list_of_selects->children)
            {
                InterpreterSelectQuery interpreter{element, context, SelectQueryOptions(QueryProcessingStage::FetchColumns)};
                std::shared_ptr<const MaterializedViewOptimizerResult> mv_optimizer_result = interpreter.getMaterializeViewMatchResult();
                int MAX = std::numeric_limits<int>::max();
                if (!mv_optimizer_result->validate_info.empty())
                {
                    view_table_column->insertDefault();
                    hit_view->insert(0);
                    match_cost->insertDefault();
                    read_cost->insert(0);
                    view_inner_query->insertDefault();
                    miss_match_reason->insert(mv_optimizer_result->validate_info);
                }
                else
                {
                    for (auto iter = mv_optimizer_result->views_match_info.begin(); iter != mv_optimizer_result->views_match_info.end();
                         iter++)
                    {
                        MatchResult::ResultPtr result = *iter;
                        if (!result->view_table)
                            continue;

                        /// insert view name
                        view_table_column->insert(result->view_table->getStorageID().getFullNameNotQuoted());

                        /// insert view match result
                        if (result->cost != MAX && iter == mv_optimizer_result->views_match_info.begin())
                            hit_view->insert(1);
                        else
                            hit_view->insert(0);

                        /// insert cost value
                        match_cost->insert(result->cost);

                        /// insert read cost
                        read_cost->insert(result->read_cost);

                        /// insert view inner query
                        auto * materialized_view = dynamic_cast<StorageMaterializedView *>(result->view_table.get());
                        if (materialized_view)
                        {
                            ASTPtr view_query = materialized_view->getInnerQuery();
                            view_inner_query->insert(queryToString(view_query));
                        }
                        else
                            view_inner_query->insertDefault();

                        miss_match_reason->insert(result->view_match_info);
                    }
                }
            }
            block.insert({std::move(view_table_column), std::make_shared<DataTypeString>(), "view_table"});
            block.insert({std::move(hit_view), std::make_shared<DataTypeUInt8>(), "match_result"});
            block.insert({std::move(match_cost), std::make_shared<DataTypeInt64>(), "match_cost"});
            block.insert({std::move(read_cost), std::make_shared<DataTypeUInt64>(), "read_cost"});
            block.insert({std::move(view_inner_query), std::make_shared<DataTypeString>(), "view_query"});
            block.insert({std::move(miss_match_reason), std::make_shared<DataTypeString>(), "miss_reason"});
            return std::make_shared<OneBlockInputStream>(block);
        }
    }
    else if (ast.getKind() == ASTExplainQuery::ExplainKind::QueryElement)
    {
        ASTPtr explain_query = ast.getExplainedQuery()->clone();
        rewriteDistributedToLocal(explain_query);
        InterpreterSelectWithUnionQuery interpreter(explain_query, getContext(),
                                                    SelectQueryOptions(QueryProcessingStage::FetchColumns).analyze().modify());
        const auto & ast_ptr = interpreter.getQuery()->children.at(0)->as<ASTExpressionList &>();
        if (ast_ptr.children.size() != 1)
            throw Exception("Element query not support multiple select query", ErrorCodes::LOGICAL_ERROR);
        const auto & select_query = ast_ptr.children.at(0)->as<ASTSelectQuery &>();
        ASTs where_expressions = {select_query.prewhere(), select_query.where()};
        where_expressions.erase(std::remove_if(where_expressions.begin(), where_expressions.end(), [](const auto & q) { return !q;} ), where_expressions.end());
        ASTPtr where = composeAnd(where_expressions);

        buf << "{";
        elementDatabaseAndTable(select_query, where, buf);
        elementDimensions(select_query.select(), buf);
        elementMetrics(select_query.select(), buf);
        elementWhere(where, buf);
        elementGroupBy(select_query.groupBy(), buf);
        buf << "}";
    }
    else if (ast.getKind() == ASTExplainQuery::PlanSegment)
    {
         if (!dynamic_cast<const ASTSelectWithUnionQuery *>(ast.getExplainedQuery().get()))
            throw Exception("Only SELECT is supported for EXPLAIN query", ErrorCodes::INCORRECT_QUERY);

        auto interpreter = std::make_unique<InterpreterDistributedStages>(ast.getExplainedQuery(), Context::createCopy(getContext()));
        auto * plan_segment_tree = interpreter->getPlanSegmentTree();
        if (plan_segment_tree)
             buf << plan_segment_tree->toString();
    }

    if (single_line)
        res_columns[0]->insertData(buf.str().data(), buf.str().size());
    else
        fillColumn(*res_columns[0], buf.str());

    return std::make_shared<OneBlockInputStream>(sample_block.cloneWithColumns(std::move(res_columns)));
}


void InterpreterExplainQuery::elementDatabaseAndTable(const ASTSelectQuery & select_query, const ASTPtr & where, WriteBuffer & buffer)
{
    if (!select_query.tables())
        throw Exception("Can not get database and table in element query", ErrorCodes::LOGICAL_ERROR);

    if(select_query.tables()->children.size() != 1)
        throw Exception("Element query not support multiple table query, such as join, etc", ErrorCodes::LOGICAL_ERROR);

    auto database_and_table = getDatabaseAndTable(select_query, 0);
    if (database_and_table)
    {
        StoragePtr storage = DatabaseCatalog::instance().getTable({database_and_table->database, database_and_table->table}, getContext());
        buffer << "\"database\": \"" << storage->getStorageID().getDatabaseName() << "\", ";
        buffer << "\"table\": \"" << storage->getStorageID().getTableName() << "\", ";
        auto dependencies = DatabaseCatalog::instance().getDependencies(storage->getStorageID());
        buffer << "\"dependencies\": [";
        for (size_t i = 0, size = dependencies.size(); i < size; ++i)
            buffer << "\"" << dependencies[i].getDatabaseName() << "." << dependencies[i].getTableName() << "\"" << (i + 1 != size ? ", " : "");
        buffer << "], ";

        listPartitionKeys(storage, buffer);
        listRowsOfOnePartition(storage, select_query.groupBy(), where, buffer);
    }
    else
        throw Exception("Can not get database and table in element query", ErrorCodes::LOGICAL_ERROR);
}

void InterpreterExplainQuery::listPartitionKeys(StoragePtr & storage, WriteBuffer & buffer)
{
    buffer << "\"partition_keys\": [";
    if (auto partition_key = storage->getInMemoryMetadataPtr()->getPartitionKey().expression_list_ast)
    {
        const auto & partition_expr_list = partition_key->as<ASTExpressionList &>();
        if (!partition_expr_list.children.empty())
        {
            for (size_t i = 0, size = partition_expr_list.children.size(); i < size; ++i)
            {
                buffer << "\"" << partition_expr_list.children[i]->getColumnName() << "\"" << (i + 1 != size ? ", " : "");
            }
        }
    }
    buffer << "], ";
}

/**
 * Select one partition to estimate view table definition is suitable with ratio of view rows and base table rows.
 */
void InterpreterExplainQuery::listRowsOfOnePartition(StoragePtr & storage, const ASTPtr & group_by, const ASTPtr & where, WriteBuffer & buffer)
{
    auto partition_condition = getActivePartCondition(storage);
    if (partition_condition)
    {
        UInt64 base_rows = 0;
        {
            WriteBufferFromOwnString query_buffer;
            query_buffer << "select count() as count from " << backQuoteIfNeed(storage->getStorageID().getDatabaseName()) << "."
                     << backQuoteIfNeed(storage->getStorageID().getTableName()) << " where " << *partition_condition;

            String query_str = query_buffer.str();
            const char * begin = query_str.data();
            const char * end = query_str.data() + query_str.size();

            ParserQuery parser(end, ParserSettings::valueOf(getContext()->getSettingsRef().dialect_type));
            auto query_ast = parseQuery(parser, begin, end, "", 0, 0);

            InterpreterSelectWithUnionQuery select(query_ast, getContext(), SelectQueryOptions());
            BlockInputStreamPtr in = select.execute().getInputStream();

            in->readPrefix();
            Block block = in->read();
            in->readSuffix();

            auto & column = block.getByName("count").column;
            if (column->size() == 1)
                base_rows = column->getUInt(0);
        }

        UInt64 view_rows = 0;
        {
            WriteBufferFromOwnString query_ss;
            query_ss << "select";
            if(group_by)
            {
                query_ss << " uniq(";
                auto & expression_list = group_by->as<ASTExpressionList &>();
                for (size_t i = 0, size = expression_list.children.size(); i < size; ++i)
                {
                    expression_list.children[i]->format(IAST::FormatSettings(query_ss, true, true));
                    query_ss << (i + 1 != size ? ", " : ")");
                }
                query_ss << " as count";
            }
            else
            {
                query_ss << " count() as count";
            }

            query_ss << " from " << backQuoteIfNeed(storage->getStorageID().getDatabaseName()) << "."
                     << backQuoteIfNeed(storage->getStorageID().getTableName()) << " where " << *partition_condition;

            if (where)
            {
                query_ss << " and (";
                where->format(IAST::FormatSettings(query_ss, true));
                query_ss << ")";
            }

            String query_str = query_ss.str();

            LOG_DEBUG(log, "element view rows query-{}",  query_str);
            const char * begin = query_str.data();
            const char * end = query_str.data() + query_str.size();

            ParserQuery parser(end, ParserSettings::valueOf(getContext()->getSettingsRef().dialect_type));
            auto query_ast = parseQuery(parser, begin, end, "", 0, 0);

            InterpreterSelectWithUnionQuery select(query_ast, getContext(), SelectQueryOptions());
            BlockInputStreamPtr in = select.execute().getInputStream();

            in->readPrefix();
            Block block = in->read();
            in->readSuffix();

            auto & column = block.getByName("count").column;
            if (column->size() == 1)
            {
                if (isColumnNullable(*column))
                {
                    const auto * nullable_column = static_cast<const ColumnNullable *>(column.get());
                    view_rows = nullable_column->isNullAt(0) ? 0 : nullable_column->getNestedColumnPtr()->getUInt(0);
                }
                else
                {
                    view_rows = column->getUInt(0);
                }
                LOG_DEBUG(log, "view query count column-{}, result-{}", column->dumpStructure(), view_rows);
            }
        }

        buffer << "\"base_rows\": " << base_rows << ", " << "\"view_rows\": " << view_rows << ", ";
        auto * merge_tree_data = dynamic_cast<MergeTreeData *>(storage.get());
        size_t mv_index_granularity = merge_tree_data->getSettings()->index_granularity;
        if (view_rows != 0 && base_rows != 0 && merge_tree_data->getSettings()->index_granularity!= 0)
            mv_index_granularity = roundup_pow_of_two(static_cast<unsigned int>(merge_tree_data->getSettings()->index_granularity / (base_rows / view_rows)));
        buffer << "\"base_rows\": " << base_rows << ", " << "\"view_rows\": " << view_rows << ", " << "\"recommend_mv_index_granularity\": " << mv_index_granularity << ", ";
    }
}


std::optional<String> InterpreterExplainQuery::getActivePartCondition(StoragePtr & storage)
{
    auto * merge_tree = dynamic_cast<MergeTreeData *>(storage.get());
    if (!merge_tree)
        throw Exception("Unknown engine: " + storage->getName(), ErrorCodes::LOGICAL_ERROR);

    auto parts = merge_tree->getDataPartsVector();
    if (!parts.empty())
    {
        return "_partition_id = '" + parts[0]->partition.getID(*merge_tree) + "'";
    }

    return {};
}

void InterpreterExplainQuery::elementWhere(const ASTPtr & where, WriteBuffer & buffer)
{
    buffer << "\"where\": " << "\"";
    if (where)
        where->format(IAST::FormatSettings(buffer, true));
    buffer << "\", ";
}

void InterpreterExplainQuery::elementMetrics(const ASTPtr & select, WriteBuffer & buffer)
{
    buffer << "\"metrics\": [";
    std::vector<String> metric_aliases;
    if(select)
    {
        auto & expression_list = select->as<ASTExpressionList &>();
        bool first_metric = true;
        for (auto & child : expression_list.children)
        {
            auto * func = child->as<ASTFunction>();
            if (func && AggregateFunctionFactory::instance().isAggregateFunctionName(func->name))
            {
                if(!first_metric)
                    buffer << ", ";
                buffer << "\"";
                child->format(IAST::FormatSettings(buffer, true, true));
                buffer << "\"";
                metric_aliases.push_back(child->tryGetAlias());
                first_metric = false;
            }
        }
    }

    buffer << "], ";
    buffer << "\"metric_aliases\": [";
    for(size_t i = 0, size = metric_aliases.size(); i < size; ++i)
        buffer << "\"" << metric_aliases[i] << "\"" << (i + 1 != size ? ", " : "");
    buffer << "], ";
}

void InterpreterExplainQuery::elementDimensions(const ASTPtr & select, WriteBuffer & buffer)
{
    buffer << "\"dimensions\": [";
    std::vector<String> dimension_aliases;
    if(select)
    {
        auto & expression_list = select->as<ASTExpressionList &>();
        bool first_dimension = true;
        for (auto & child : expression_list.children)
        {
            auto * func = child->as<ASTFunction>();
            auto * identifier = child->as<ASTIdentifier>();
            if (identifier || (func && !AggregateFunctionFactory::instance().isAggregateFunctionName(func->name)))
            {
                if (!first_dimension)
                    buffer << ", ";
                buffer << "\"";
                child->format(IAST::FormatSettings(buffer, true, true));
                buffer << "\"";
                dimension_aliases.push_back(child->tryGetAlias());
                first_dimension = false;
            }
        }
    }

    buffer << "], ";
    buffer << "\"dimension_aliases\": [";
    for(size_t i = 0, size = dimension_aliases.size(); i < size; ++i)
        buffer << "\"" << dimension_aliases[i] << "\"" << (i + 1 != size ? ", " : "");
    buffer << "], ";
}

void InterpreterExplainQuery::elementGroupBy(const ASTPtr & group_by, WriteBuffer & buffer)
{
    buffer << "\"group_by\": [";
    if(group_by)
    {
        auto & expression_list = group_by->as<ASTExpressionList &>();
        bool first_group_expression = true;
        for (auto & child : expression_list.children)
        {
            if(child->as<ASTFunction>() || child->as<ASTIdentifier>())
            {
                if (!first_group_expression)
                    buffer << ", ";
                buffer << "\"";
                child->format(IAST::FormatSettings(buffer, true, true));
                buffer << "\"";
                first_group_expression = false;
            }
        }
    }
    buffer << "]";
}

void InterpreterExplainQuery::explainUsingOptimizer(const ASTPtr & ast, WriteBuffer & buffer, bool & single_line)
{
    const auto & explain = ast->as<ASTExplainQuery &>();
    auto settings = checkAndGetSettings<QueryPlanSettings>(explain.getSettings());

    auto context = Context::createCopy(getContext());
    InterpreterSelectQueryUseOptimizer interpreter(explain.getExplainedQuery(), context, SelectQueryOptions());
    auto query_plan = interpreter.buildQueryPlan();

    CardinalityEstimator::estimate(*query_plan, context);
    PlanCostMap costs = CostCalculator::calculate(*query_plan, *context);
    if (settings.json)
    {
        buffer << PlanPrinter::jsonLogicalPlan(*query_plan, true, true);
        single_line = true;
    }
    else
        buffer << PlanPrinter::textLogicalPlan(*query_plan, context, true, true, costs);
}

}
