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

#include <filesystem>
#include <fstream>
#include <Analyzers/QueryAnalyzer.h>
#include <Analyzers/QueryRewriter.h>
#include <DataStreams/OneBlockInputStream.h>
#include <Interpreters/InterpreterCreateQuery.h>
#include <Interpreters/InterpreterDropStatsQuery.h>
#include <Interpreters/InterpreterReproduceQueryUseOptimizer.h>
#include <Optimizer/CardinalityEstimate/CardinalityEstimator.h>
#include <Optimizer/Dump/PlanDump.h>
#include <Optimizer/PlanOptimizer.h>
#include <Parsers/ASTReproduceQuery.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/parseQuery.h>
#include <QueryPlan/QueryPlanner.h>
#include <Statistics/CatalogAdaptor.h>
#include <Statistics/SubqueryHelper.h>

namespace DB
{
BlockIO InterpreterReproduceQueryUseOptimizer::execute()
{
    auto query_body_ptr = query_ptr->clone();
    String zip_path_string = query_body_ptr->as<ASTReproduceQuery>()->reproduce_path;
    std::filesystem::path zip_path = std::filesystem::path(zip_path_string);
    if (!std::filesystem::exists(zip_path) || zip_path.extension() != ".zip")
    {
        throw Exception("zip format is not found: " + zip_path_string, ErrorCodes::LOGICAL_ERROR);
    }
    auto pos = zip_path_string.find(".zip");
    String dir_path_string = zip_path_string.substr(0, pos);
    unzipDirectory(dir_path_string, zip_path_string);

    String path_query = dir_path_string + "/query.sql";
    String path_settings = dir_path_string + "/settings_changed.json";
    String path_stats = dir_path_string + "/stats.json";
    String path_ddl = dir_path_string + "/ddl.json";
    String path_others = dir_path_string + "/others.json";

    std::ifstream fin(path_query);
    std::stringstream buffer;
    buffer << fin.rdbuf();
    String query(buffer.str());

    //setting recreation
    loadSettings(context, path_settings);
    createClusterInfo(path_others);


    //create tables use ddl recreation
    createTablesFromJson(path_ddl);

    //load stats
    loadStats(context, path_stats);

    ASTPtr ast = parse(query);
    String explain;
    if (ast->getType() == DB::ASTType::ASTSelectQuery || ast->getType() == DB::ASTType::ASTSelectWithUnionQuery)
    {
        auto query_plan = plan(ast);
        CardinalityEstimator::estimate(*query_plan, context);
        std::unordered_map<PlanNodeId, double> costs = CostCalculator::calculate(*query_plan, *context);
        explain = DB::PlanPrinter::textLogicalPlan(*query_plan, context, true, true, costs);
    }
    auto explain_column = ColumnString::create();
    std::istringstream ss(explain);
    String line;
    while (std::getline(ss, line))
    {
        explain_column->insert(std::move(line));
    }
    Block block;
    BlockIO res;
    block.insert({std::move(explain_column), std::make_shared<DataTypeString>(), "explain"});
    res.in = std::make_shared<OneBlockInputStream>(block);


    return res;
}
ASTPtr InterpreterReproduceQueryUseOptimizer::parse(const String & query)
{
    const char * begin = query.data();
    const char * end = begin + query.size();

    ParserQuery parser(
        end, (context->getSettingsRef().dialect_type == DialectType::ANSI ? ParserSettings::ANSI : ParserSettings::CLICKHOUSE));
    std::string out_error_message;
    auto ast = tryParseQuery(
        parser, begin, end, out_error_message, false, "", false, context->getSettingsRef().max_query_size, DBMS_DEFAULT_MAX_PARSER_DEPTH);

    return ast;
}
QueryPlanPtr InterpreterReproduceQueryUseOptimizer::plan(ASTPtr ast)
{
    if (!ast)
        throw Exception("ast is null in reproduce", ErrorCodes::LOGICAL_ERROR);
    context->createPlanNodeIdAllocator();
    context->createSymbolAllocator();
    context->createOptimizerMetrics();

    ast = QueryRewriter::rewrite(ast, context);
    AnalysisPtr ast_result = QueryAnalyzer::analyze(ast, context);
    QueryPlanPtr query_plan = QueryPlanner::plan(ast, *ast_result, context);
    PlanOptimizer::optimize(*query_plan, context);
    return query_plan;
}
void InterpreterReproduceQueryUseOptimizer::createTablesFromJson(const String & path)
{
    std::filesystem::path path_{path};
    if (!std::filesystem::exists(path_))
    {
        throw Exception("the path of ddl.json is null. ", ErrorCodes::LOGICAL_ERROR);
    }
    std::ifstream fin(path);
    std::stringstream buffer;
    buffer << fin.rdbuf();
    String ddl(buffer.str());
    fin.close();
    Pparser parser;
    PVar ddl_var = parser.parse(ddl);
    PObject ddl_object = *ddl_var.extract<PObject::Ptr>();
    for (auto & [k, v] : ddl_object)
    {
        String query = v.toString();
        ASTPtr ast = parse(query);
        if (!ast)
        {
            continue;
        }
        auto * create = ast->as<ASTCreateQuery>();
        String create_table = create->table;
        String create_database = create->database.empty() ? context->getCurrentDatabase() : create->database;
        DatabasePtr database_ptr = DatabaseCatalog::instance().getDatabase(create_database);
        if (database_ptr->isTableExist(create_table, context))
        {
            continue;
        }

        create->uuid = UUIDHelpers::Nil;
        InterpreterCreateQuery create_interpreter(ast, context);
        create_interpreter.execute();
        //        resetTransaction();
        String db_table = create_database + "." + create_table;
        String warnings_info
            = "Reproduce::execute the create table--: " + db_table + " ; You can drop it by:  drop table " + db_table + ";";
        LOG_WARNING(log, warnings_info);
    }
}
void InterpreterReproduceQueryUseOptimizer::createClusterInfo(const String & path)
{
    std::filesystem::path path_{path};
    if (!std::filesystem::exists(path_))
    {
        throw Exception("the path of others.json is null. ", ErrorCodes::LOGICAL_ERROR);
    }
    std::ifstream fin(path);
    std::stringstream buffer;
    buffer << fin.rdbuf();
    String others(buffer.str());
    fin.close();
    Pparser parser;
    PVar others_var = parser.parse(others);
    PObject others_object = *others_var.extract<PObject::Ptr>();
    context->setSetting("memory_catalog_worker_size", others_object.get("memory_catalog_worker_size").toString());
    context->setSetting("enable_memory_catalog", true);
    //    context->getSettings().enable_memory_catalog = true;

    String database_name = others_object.get("CurrentDatabase").toString();
    if (DatabaseCatalog::instance().isDatabaseExist(database_name))
    {
        context->setCurrentDatabase(database_name);
        return;
    }

    String sql = "create database " + database_name;
    Statistics::executeSubQuery(context, sql);

    context->setCurrentDatabase(database_name);
    context->setQueryContext(context);
    String warnings_info = "Reproduce::execute the subquery--: " + sql + " ; You can drop it by:  drop database " + database_name + ";";
    LOG_WARNING(log, warnings_info);
}

}
