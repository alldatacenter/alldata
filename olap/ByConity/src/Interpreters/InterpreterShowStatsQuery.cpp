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
#include <map>
#include <DataStreams/BlocksListBlockInputStream.h>
#include <Interpreters/InterpreterShowStatsQuery.h>
#include <Optimizer/Dump/PlanDump.h>
#include <Parsers/ASTStatsQuery.h>
#include <Statistics/FormattedOutput.h>
#include <Statistics/StatisticsCollector.h>
#include <Statistics/StatsColumnBasic.h>
#include <Statistics/StatsTableBasic.h>
#include <Statistics/TypeUtils.h>
#include <Statistics/serde_extend.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <Poco/Timestamp.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int LOGICAL_ERROR;
    extern const int UNKNOWN_TABLE;
    extern const int FILE_NOT_FOUND;
}
using namespace Statistics;

std::vector<FormattedOutputData> getTableFormattedOutput(
    ContextPtr context, CatalogAdaptorPtr catalog, const StatsTableIdentifier & table_info, const std::vector<String> & column_names)
{
    std::vector<FormattedOutputData> results;
    StatisticsCollector collector_impl(context, catalog, table_info);
    ColumnDescVector cols_desc;
    if (column_names.empty())
    {
        cols_desc = catalog->getCollectableColumns(table_info);
    }
    else
    {
        cols_desc = filterCollectableColumns(catalog->getCollectableColumns(table_info), column_names, true);
    }

    collector_impl.readFromCatalogImpl(cols_desc);

    auto plannode_stats_opt = collector_impl.toPlanNodeStatistics();
    if (!plannode_stats_opt.has_value())
    {
        return {};
    }

    auto plannode_stats = plannode_stats_opt.value();

    auto row_count = plannode_stats->getRowCount();
    {
        FormattedOutputData fod;
        fod.append("identifier", table_info.getTableName() + ".*");
        fod.append("count", row_count);
        results.emplace_back(std::move(fod));
    }

    auto symbols = plannode_stats->getSymbolStatistics();
    for (auto & col : cols_desc)
    {
        auto symbol_iter = symbols.find(col.name);
        if (symbol_iter == symbols.end())
        {
            continue;
        }
        auto & symbol_stats = symbol_iter->second;

        FormattedOutputData fod;

        auto type_name = col.type->getName();
        fod.append("identifier", table_info.getTableName() + "." + col.name);
        fod.append("type", type_name);
        auto ndv = symbol_stats->getNdv();
        fod.append("ndv", ndv);

        auto null_count = symbol_stats->getNullsCount();
        fod.append("count", row_count - null_count);
        fod.append("null_count", null_count);

        fod.append("min", symbol_stats->getMin());
        fod.append("max", symbol_stats->getMax());

        fod.append("has_histogram", !symbol_stats->getHistogram().empty());
        results.emplace_back(std::move(fod));
    }
    return results;
}

void writeDbStats(ContextPtr context, const String & db_name, const String & path)
{
    Protos::DbStats db_stats;
    db_stats.set_db_name(db_name);
    db_stats.set_version(PROTO_VERSION);
    auto catalog = createCatalogAdaptor(context);
    auto tables = catalog->getAllTablesID(db_name);
    for (auto & table : tables)
    {
        StatisticsCollector collector(context, catalog, table);
        collector.readAllFromCatalog();
        auto table_collection = collector.getTableStats().writeToCollection();
        if (table_collection.empty())
        {
            continue;
        }

        auto table_pb = db_stats.add_tables();
        table_pb->set_table_name(table.getTableName());
        for (auto & [k, v] : table_collection)
        {
            table_pb->mutable_blobs()->operator[](static_cast<int64_t>(k)) = v->serialize();
        }
        for (auto & [col_name, col_stats] : collector.getColumnsStats())
        {
            auto column_pb = table_pb->add_columns();
            auto column_collection = col_stats.writeToCollection();
            if (column_collection.empty())
            {
                continue;
            }
            column_pb->set_column_name(col_name);
            for (auto & [k, v] : column_collection)
            {
                column_pb->mutable_blobs()->operator[](static_cast<int64_t>(k)) = v->serialize();
            }
        }
    }
    std::ofstream fout(path, std::ios::binary);
    db_stats.SerializeToOstream(&fout);
}
void writeDbStatsToJson(ContextPtr context, const String & db_name, const String & path)
{
    Protos::DbStats db_stats;
    db_stats.set_db_name(db_name);
    db_stats.set_version(PROTO_VERSION);
    auto catalog = createCatalogAdaptor(context);
    auto tables = catalog->getAllTablesID(db_name);
    Poco::JSON::Object stats_json;
    for (auto & table : tables)
    {
        String table_name = table.getTableName();
        if (table_name.find("_local") == String::npos)
            stats_json.set(db_name + "." + table_name, tableJson(context, db_name, table_name));
    }
    std::ofstream fout(path);
    Poco::Dynamic::Var stats_var(stats_json);
    fout << stats_var.toString();
    fout.close();
}

void readDbStats(ContextPtr context, const String & original_db_name, const String & path)
{
    std::ifstream fin(path, std::ios::binary);
    Protos::DbStats db_stats;
    db_stats.ParseFromIstream(&fin);

    auto version = db_stats.has_version() ? db_stats.version() : Protos::DbStats_Version_V1;
    if (version == Protos::DbStats_Version_V1)
    {
        version = Protos::DbStats_Version_V2;
    }
    if (version != PROTO_VERSION)
    {
        throw Exception("stats version is incorrect", ErrorCodes::LOGICAL_ERROR);
    }

    auto db_name = original_db_name;
    auto catalog = createCatalogAdaptor(context);
    auto logger = &Poco::Logger::get("load stats");

    for (auto & table_pb : db_stats.tables())
    {
        auto table_name = table_pb.table_name();
        auto table_id_opt = catalog->getTableIdByName(db_name, table_name);
        if (!table_id_opt)
        {
            auto msg = "table " + table_name + " not exist in database " + db_name;
            logger->warning(msg);
            continue;
        }

        StatisticsCollector collector(context, catalog, table_id_opt.value());

        {
            StatsCollection collection;
            for (auto & [k, v] : table_pb.blobs())
            {
                auto tag = static_cast<StatisticsTag>(k);
                auto obj = createStatisticsBase(tag, v);
                collection[tag] = std::move(obj);
            }
            StatisticsCollector::TableStats table_stats;
            table_stats.readFromCollection(collection);
            collector.setTableStats(std::move(table_stats));
        }

        for (auto & column_pb : table_pb.columns())
        {
            auto column_name = column_pb.column_name();
            StatsCollection collection;
            for (auto & [k, v] : column_pb.blobs())
            {
                auto tag = static_cast<StatisticsTag>(k);
                auto obj = createStatisticsBase(tag, v);
                collection[tag] = std::move(obj);
            }
            StatisticsCollector::ColumnStats column_stats;
            column_stats.readFromCollection(collection);
            collector.setColumnStats(column_name, std::move(column_stats));
        }
        collector.writeToCatalog();
    }
}

static std::vector<StatsTableIdentifier> getTables(ContextPtr context, const ASTShowStatsQuery * query)
{
    std::vector<StatsTableIdentifier> tables;
    auto catalog = createCatalogAdaptor(context);
    auto db = context->resolveDatabase(query->database);
    if (query->target_all)
    {
        tables = catalog->getAllTablesID(db);
    }
    else
    {
        auto table_info_opt = catalog->getTableIdByName(db, query->table);
        if (!table_info_opt)
        {
            auto msg = "Unknown Table (" + query->table + ") in database (" + db + ")";
            throw Exception(msg, ErrorCodes::UNKNOWN_TABLE);
        }
        tables.emplace_back(table_info_opt.value());
    }
    return tables;
}

BlockIO InterpreterShowStatsQuery::executeTable()
{
    auto query = query_ptr->as<const ASTShowStatsQuery>();
    // Block sample_block = getSampleBlock();
    // MutableColumns res_columns = sample_block.cloneEmptyColumns();
    auto context = getContext();
    auto tables = getTables(context, query);
    auto catalog = createCatalogAdaptor(context);

    BlocksList blocks;

    for (auto & table_info : tables)
    {
        auto fods = getTableFormattedOutput(context, catalog, table_info, query->columns);
        // adjust here to change the order
        auto block = outputFormattedBlock(fods, {"identifier", "type", "count", "null_count", "ndv", "min", "max", "has_histogram"});
        blocks.emplace_back(std::move(block));
    }

    BlockIO res;
    // res.in = std::make_shared<Block>(sample_block.cloneWithColumns(std::move(res_columns)));
    res.in = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));
    return res;
}

std::vector<FormattedOutputData> getColumnFormattedOutput(const String & full_column_name, const SymbolStatistics & symbol_stats)
{
    std::vector<FormattedOutputData> fods;

    // table.col  | bucket_id | range | count | ndv | cumulative_count | cumulative_ndv |

    double cumulative_count = 0;
    double cumulative_ndv = 0;
    auto bucket_id = 0;
    auto & histogram = symbol_stats.getHistogram();
    for (auto & bucket : histogram.getBuckets())
    {
        auto count = bucket.getCount();
        if (count == 0)
        {
            continue;
        }
        FormattedOutputData fod;
        fod.append("identifier", full_column_name);
        fod.append("bucket_id", bucket_id);
        auto low_inc = bucket.isLowerClosed();
        auto high_inc = bucket.isUpperClosed();
        auto low = bucket.getLowerBound();
        auto high = bucket.getUpperBound();
        auto ndv = bucket.getNumDistinct();
        String range
            = (low_inc ? "[" : "(") + boost::lexical_cast<String>(low) + ", " + boost::lexical_cast<String>(high) + (high_inc ? "]" : ")");
        fod.append("range", range);
        fod.append("count", count);
        fod.append("ndv", ndv);
        cumulative_count += count;
        cumulative_ndv += ndv;
        fod.append("cumulative_count", cumulative_count);
        fod.append("cumulative_ndv", cumulative_ndv);
        fods.emplace_back(std::move(fod));
        ++bucket_id;
    }
    return fods;
}

BlocksList getColumnsFormattedOutput(
    ContextPtr context, CatalogAdaptorPtr catalog, const StatsTableIdentifier & table_info, const std::vector<String> & target_columns)
{
    StatisticsCollector collector_impl(context, catalog, table_info);

    if (!target_columns.empty())
    {
        auto cols_desc = filterCollectableColumns(catalog->getCollectableColumns(table_info), target_columns, true);
        collector_impl.readFromCatalog(target_columns);
    }
    else
    {
        collector_impl.readAllFromCatalog();
    }

    auto plannode_stats_opt = collector_impl.toPlanNodeStatistics();
    if (!plannode_stats_opt.has_value())
    {
        return {};
    }
    auto plannode_stats = plannode_stats_opt.value();

    BlocksList blocks;
    const auto & plan_stats = plannode_stats->getSymbolStatistics();
    if (plan_stats.empty())
    {
        return {};
    }

    auto cols_desc = catalog->getCollectableColumns(table_info);
    for (auto & col_desc : cols_desc)
    {
        auto col_name = col_desc.name;
        if (plan_stats.count(col_name) == 0)
        {
            continue;
        }

        const auto & symbol_stats = plan_stats.at(col_name);

        if (symbol_stats->getHistogram().empty())
        {
            continue;
        }
        auto full_col_name = table_info.getTableName() + "." + col_name;
        auto fods = getColumnFormattedOutput(full_col_name, *symbol_stats);
        auto block = outputFormattedBlock(fods, {"identifier", "bucket_id", "range", "count", "ndv", "cumulative_count", "cumulative_ndv"});
        blocks.emplace_back(std::move(block));
    }
    return blocks;
}


BlockIO InterpreterShowStatsQuery::executeColumn()
{
    auto query = query_ptr->as<const ASTShowStatsQuery>();
    auto context = getContext();
    // Block sample_block = getSampleBlock();
    // MutableColumns res_columns = sample_block.cloneEmptyColumns();
    auto tables = getTables(context, query);
    auto catalog = Statistics::createCatalogAdaptor(context);

    BlocksList blocks;
    if (!query->columns.empty())
    {
        if (tables.size() != 1)
        {
            throw Exception("columns specifier is supported only for single table", ErrorCodes::BAD_ARGUMENTS);
        }

        auto table_info = tables[0];
        auto new_blocks = getColumnsFormattedOutput(context, catalog, table_info, query->columns);
        blocks.splice(blocks.end(), std::move(new_blocks));
    }
    else
    {
        for (auto & table_info : tables)
        {
            auto new_blocks = getColumnsFormattedOutput(context, catalog, table_info, {});
            blocks.splice(blocks.end(), std::move(new_blocks));
        }
    }

    BlockIO res;
    res.in = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));
    return res;
}

static bool isSpecialFunction(const String & name)
{
    static std::set<String> specials({"__save", "__load", "__jsonsave", "__jsonload"});
    return specials.count(name);
}

void InterpreterShowStatsQuery::executeSpecial()
{
    auto query = query_ptr->as<const ASTShowStatsQuery>();
    auto context = getContext();
    auto catalog = Statistics::createCatalogAdaptor(context);

    // refactor this into an explicit command
    if (query->table == "__save")
    {
        catalog->checkHealth(/*is_write=*/false);
        auto db_name = context->resolveDatabase(query->database);
        auto path = context->getSettingsRef().graphviz_path.toString() + "/" + db_name + ".bin";

        writeDbStats(context, db_name, path);
    }
    else if (query->table == "__load")
    {
        catalog->checkHealth(/*is_write=*/true);
        auto db_name = query->database;
        if (db_name.empty())
            db_name = context->getCurrentDatabase();
        auto path = context->getSettingsRef().graphviz_path.toString() + "/" + db_name + ".bin";
        if (!std::filesystem::exists(path))
        {
            throw Exception("file " + path + " not exists", ErrorCodes::FILE_NOT_FOUND);
        }

        readDbStats(context, db_name, path);
    }
    else if (query->table == "__jsonsave")
    {
        catalog->checkHealth(/*is_write=*/false);
        auto db_name = query->database;
        if (db_name.empty())
            db_name = context->getCurrentDatabase();
        auto path = context->getSettingsRef().graphviz_path.toString() + "/" + db_name + ".json";
        writeDbStatsToJson(context, db_name, path);
    }
    else if (query->table == "__jsonload")
    {
        catalog->checkHealth(/*is_write=*/true);
        auto db_name = query->database;
        if (db_name.empty())
            db_name = context->getCurrentDatabase();
        auto path = context->getSettingsRef().graphviz_path.toString() + "/" + db_name + ".json";
        if (!std::filesystem::exists(path))
        {
            throw Exception("json_file " + path + " not exists", ErrorCodes::FILE_NOT_FOUND);
        }

        loadStats(context, path);
    }
    else
    {
        throw Exception("unknown special action: " + query->table, ErrorCodes::NOT_IMPLEMENTED);
    }
}

BlockIO InterpreterShowStatsQuery::execute()
{
    auto query = query_ptr->as<const ASTShowStatsQuery>();
    auto context = getContext();
    auto catalog = Statistics::createCatalogAdaptor(context);

    if (isSpecialFunction(query->table))
    {
        executeSpecial();
        return {};
    }
    else if (query->kind == StatsQueryKind::COLUMN_STATS)
    {
        catalog->checkHealth(/*is_write=*/false);
        // throw Exception("unimplemented", ErrorCodes::LOGICAL_ERROR);
        return executeColumn();
    }
    else
    {
        catalog->checkHealth(/*is_write=*/false);
        return executeTable();
    }
}

}
