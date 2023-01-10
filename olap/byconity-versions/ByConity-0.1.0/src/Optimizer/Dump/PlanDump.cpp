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
#include <Databases/DatabaseMemory.h>
#include <Optimizer/Dump/PlanDump.h>
#include <Parsers/formatAST.h>
#include <Statistics/CatalogAdaptor.h>
#include <Statistics/StatisticsBase.h>
#include <Statistics/StatisticsCollector.h>
#include <Storages/StorageDistributed.h>
#include <Poco/JSON/Object.h>
#include <Common/SettingsChanges.h>

namespace DB
{
StatisticsTag StatisticsTagFromString(const String & tag_string)
{
    const google::protobuf::EnumDescriptor * descriptor = Protos::StatisticsType_descriptor();
    return static_cast<StatisticsTag>(descriptor->FindValueByName(tag_string)->number());
}
String StatisticsTagToString(StatisticsTag tag)
{
    const google::protobuf::EnumDescriptor * descriptor = Protos::StatisticsType_descriptor();
    return descriptor->FindValueByNumber(static_cast<int>(tag))->name();
}
PVar tableJson(ContextPtr context, const String & db_name, const String & table_name)
{
    Poco::JSON::Object::Ptr json = new Poco::JSON::Object(true);
    auto catalog = createCatalogAdaptor(context);
    auto Tabledentifier = catalog->getTableIdByName(db_name, table_name);
    StatisticsCollector collector(context, catalog, Tabledentifier.value());
    collector.readAllFromCatalog();
    auto table_collection = collector.getTableStats().writeToCollection();
    if (table_collection.empty())
    {
        return json;
    }
    for (auto & [k, v] : table_collection)
    {
        Poco::JSON::Parser parser;
        String tag_string = StatisticsTagToString(k);
        if (tag_string != "Invalid")
            json->set(tag_string, parser.parse(v->serializeToJson()));
    }
    PObject::Ptr columns = new Poco::JSON::Object(true);
    for (auto & [col_name, col_stats] : collector.getColumnsStats())
    {
        auto column_collection = col_stats.writeToCollection();
        if (column_collection.empty())
        {
            continue;
        }
        Poco::JSON::Object::Ptr json_column = new Poco::JSON::Object(true);
        for (auto & [k, v] : column_collection)
        {
            Poco::JSON::Parser parser;
            String tag_string = StatisticsTagToString(k);
            if (tag_string != "Invalid")
                json_column->set(tag_string, parser.parse(v->serializeToJson()));
        }
        columns->set(col_name, Poco::Dynamic::Var(*json_column));
    }
    json->set("Columns", *columns);

    return Poco::Dynamic::Var(*json);
}


Void NodeDumper::visitPlanNode(PlanNodeBase & node, Void & void_context)
{
    PlanNodes & children = node.getChildren();
    for (auto & iter : children)
    {
        // printEdge(node, *iter);
        VisitorUtil::accept(*iter, *this, void_context);
    }
    return Void{};
}

Void NodeDumper::visitTableScanNode(TableScanNode & node, Void & void_context)
{
    //    Poco::JSON::Object Storage_DDL;
    auto & step_ptr = node.getStep();
    auto & step = dynamic_cast<const TableScanStep &>(*step_ptr);
    String database = step.getDatabase();
    String table = step.getTable();
    String database_table = database + "." + table;
    if (!visited_tables.count(database_table))
    {
        visited_tables.insert(database_table);
        ASTPtr create_query = DatabaseCatalog::instance().getDatabase(database)->getCreateTableQuery(table, context);
        WriteBufferFromOwnString buf;
        formatAST(*create_query, buf, false, false);
        String res = buf.str();

        query_ddl.set(database_table, res);
        query_stats.set(database_table, tableJson(context, database, table));
    }
    StoragePtr storage = step.getStorage();
    if (const auto * storage_distributed = dynamic_cast<const StorageDistributed *>(storage.get()))
    {
        String local_database = storage_distributed->getRemoteDatabaseName();
        String local_table = storage_distributed->getRemoteTableName();
        String local_database_table = local_database + "." + local_table;
        if (!visited_tables.count(local_database_table))
        {
            visited_tables.insert(local_database_table);
            ASTPtr create_query = DatabaseCatalog::instance().getDatabase(local_database)->getCreateTableQuery(local_table, context);
            WriteBufferFromOwnString buf;
            formatAST(*create_query, buf, false, false);
            String res = buf.str();

            query_ddl.set(local_database_table, res);
        }
    }

    visitPlanNode(node, void_context);
    return Void{};
}
Void NodeDumper::visitCTERefNode(CTERefNode & node, Void & void_context)
{
    auto & step = dynamic_cast<const CTERefStep &>(*node.getStep().get());
    if (cte_helper)
    {
        cte_helper->accept(step.getId(), *this, void_context);
    }
    return Void{};
}
void cleanPlanDumpFiles(String dump_path, String query_id)
{
    std::filesystem::path plan_dump_path(dump_path + query_id + "/");

    try
    {
        if (!std::filesystem::exists(plan_dump_path))
        {
            std::filesystem::create_directories(plan_dump_path);
            return;
        }
    }
    catch (...)
    {
    }
}
void dumpQuery(const String & sql, ContextPtr context)
{
    String dump_path = context->getSettingsRef().graphviz_path.toString();
    String query_id = context->getCurrentQueryId();
    cleanPlanDumpFiles(dump_path, query_id);

    std::stringstream path;
    path << dump_path + query_id + "/";
    path << "query"
         << ".sql";
    std::ofstream out(path.str());
    out << sql;
    out.close();
}
void dumpDdlStats(QueryPlan & plan, ContextMutablePtr context)
{
    Poco::JSON::Object query_ddl_object;
    Poco::JSON::Object query_stats_object;

    NodeDumper node_dumper{query_ddl_object, query_stats_object, context, &plan.getCTEInfo()};
    Void void_context{};
    VisitorUtil::accept(plan.getPlanNode(), node_dumper, void_context);
    Poco::Dynamic::Var query_ddl(query_ddl_object);
    Poco::Dynamic::Var query_stats(query_stats_object);

    String dump_path = context->getSettingsRef().graphviz_path.toString();
    String query_id = context->getCurrentQueryId();
    cleanPlanDumpFiles(dump_path, query_id);

    //        save the ddl data
    std::stringstream path;
    path << dump_path + query_id + "/";
    path << "ddl"
         << ".json";
    std::ofstream out(path.str());
    out << query_ddl.toString();
    out.close();

    //        save the statistic data
    std::stringstream path_stats;
    path_stats << dump_path + query_id + "/";
    path_stats << "stats"
               << ".json";
    std::ofstream outS(path_stats.str());
    outS << query_stats.toString();
    outS.close();

    dumpSetting(context);
}
void dumpSetting(ContextPtr context)
{
    Poco::JSON::Object settings_change;
    context->getSettingsRef().dumpToJSON(settings_change);
    Poco::Dynamic::Var Settings_Change(settings_change);

    String dump_path = context->getSettingsRef().graphviz_path.toString();
    String query_id = context->getCurrentQueryId();

    std::stringstream path;
    path << dump_path + query_id + "/";
    path << "settings_changed"
         << ".json";
    std::ofstream out(path.str());
    out << Settings_Change.toString();
    out.close();
}
void dumpClusterInfo(ContextPtr context, size_t parallel)
{
    Poco::JSON::Object other_json;
    String path = context->getSettingsRef().graphviz_path.toString();
    String other_path = path + context->getCurrentQueryId() + "/others.json";
    other_json.set("memory_catalog_worker_size", toString(parallel));
    other_json.set("CurrentDatabase", context->getCurrentDatabase());
    std::ofstream out(other_path);
    out << PVar(other_json).toString();
    out.close();
}

void loadStats(ContextPtr context, const String & path)
{
    std::filesystem::path path_{path};
    if (!std::filesystem::exists(path_))
    {
        throw Exception("the path of stats.json is null. ", ErrorCodes::LOGICAL_ERROR);
    }
    //get the json object from file
    std::ifstream fin(path);
    std::stringstream buffer;
    buffer << fin.rdbuf();
    std::string json_str(buffer.str());
    fin.close();
    Pparser parser;
    PVar var = parser.parse(json_str);
    PObject object = *var.extract<PObject::Ptr>();

    auto catalog = createCatalogAdaptor(context);
    auto logger = &Poco::Logger::get("load stats");

    for (auto & it : object)
    {
        //traverse tables ,the json format is database_table->{TableBasic,Columns},
        // Columns ->{Column_name,Column_name2,...},Column_name->{ColumnBasic,NdvBucketsResult};
        String database_table = it.first; //table_name
        size_t pos = database_table.find('.');
        if (pos == String::npos)
        {
            throw Exception("Table not found in the database_table string", ErrorCodes::LOGICAL_ERROR);
        }
        String db_name = database_table.substr(0, pos);
        String table_name = database_table.substr(pos + 1);
        auto table_id_opt = catalog->getTableIdByName(db_name, table_name);
        if (!table_id_opt)
        {
            String info_warning = "table ";
            info_warning.append(table_name);
            info_warning.append(" not exist in database ");
            info_warning.append(db_name);
            auto msg = info_warning;
            logger->warning(msg);
            continue;
        }
        PVar table_var = it.second;
        PObject table_object = *table_var.extract<PObject::Ptr>();

        //traverse TableBasic;
        StatisticsCollector collector(context, catalog, table_id_opt.value());

        {
            StatsCollection collection;
            //            auto tag = static_cast<StatisticsTag>(1);
            auto tag = StatisticsTagFromString("TableBasic");
            auto obj = createStatisticsBaseFromJson(tag, table_object.get("TableBasic").toString());
            collection[tag] = std::move(obj);
            StatisticsCollector::TableStats table_stats;
            table_stats.readFromCollection(collection);
            collector.setTableStats(std::move(table_stats));
        }
        //traverse Column_Name
        {
            PVar columns_var = table_object.get("Columns");
            PObject columns_object = *columns_var.extract<PObject::Ptr>();
            for (auto & col_name : columns_object)
            {
                PVar col_json = col_name.second;
                PObject col_object = *col_json.extract<PObject::Ptr>();
                StatsCollection collection;
                for (auto & [k, v] : col_object)
                {
                    String Statistics_Tag = k;
                    auto tag = StatisticsTagFromString(Statistics_Tag);
                    auto obj = createStatisticsBaseFromJson(tag, v.toString());
                    collection[tag] = std::move(obj);
                }
                StatisticsCollector::ColumnStats column_stats;
                column_stats.readFromCollection(collection);
                collector.setColumnStats(col_name.first, std::move(column_stats));
            }
            collector.writeToCatalog();
        }
    }
}

void loadSettings(ContextMutablePtr context, const String & path)
{
    std::filesystem::path path_{path};
    if (!std::filesystem::exists(path_))
    {
        throw Exception("the path of setting.json is null. ", ErrorCodes::LOGICAL_ERROR);
    }
    std::ifstream fin(path);
    std::stringstream buffer;
    buffer << fin.rdbuf();
    String settings(buffer.str());
    fin.close();
    Pparser parser;
    PVar settings_var = parser.parse(settings);
    PObject settings_object = *settings_var.extract<PObject::Ptr>();
    SettingsChanges setting_changes;
    for (auto & [k, v] : settings_object)
    {
        setting_changes.emplace_back(k, v.toString());
    }
    context->applySettingsChanges(setting_changes);
}

/**
 * Compress a directory to a zip file
 * desfile: Target file, such as: /directory/test.zip
 * srcdir: source directory (path to the folder which is to be compressed)
 */
void zipDirectory(const String & des_file, const String & src_dir)
{
    Poco::Path src_dir_path(src_dir);
    src_dir_path.makeDirectory();

    std::ofstream out_stream(des_file, std::ios::binary);
    Poco::Zip::Compress compress(out_stream, true);
    compress.addRecursive(src_dir_path, Poco::Zip::ZipCommon::CL_NORMAL);

    compress.close();
    out_stream.close();
}

/**
 * Extract the ZIP file to a directory
 * desdir: Target directory, such as: /directory
 * srcdir: source compressed file, such as: /directory/test.zip
 */
void unzipDirectory(const String & des_dir, const String & src_file)
{
    std::ifstream in_stream(src_file, std::ios::binary);
    Poco::Zip::Decompress decompress(in_stream, des_dir);
    decompress.decompressAllFiles();
    in_stream.close();
}

}
