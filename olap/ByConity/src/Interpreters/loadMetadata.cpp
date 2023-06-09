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

#include <Common/ThreadPool.h>
#include <Common/config.h>

#include <Poco/DirectoryIterator.h>

#include <Parsers/ParserCreateQuery.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/parseQuery.h>

#include <Interpreters/InterpreterCreateQuery.h>
#include <Interpreters/Context.h>
#include <Interpreters/loadMetadata.h>

#include <Databases/DatabaseOrdinary.h>

#include <IO/ReadBufferFromFile.h>
#include <IO/ReadHelpers.h>
#include <Common/escapeForFileName.h>

#include <Common/typeid_cast.h>
#include <Common/StringUtils/StringUtils.h>
#include <filesystem>

#if USE_HDFS
#include <hdfs/hdfs.h>
#include <IO/copyData.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <IO/WriteBufferFromFile.h>
#include <Storages/HDFS/HDFSCommon.h>
#endif
namespace fs = std::filesystem;

namespace DB
{

static void executeCreateQuery(
    const String & query,
    ContextMutablePtr context,
    const String & database,
    const String & file_name,
    bool has_force_restore_data_flag)
{
    ParserCreateQuery parser(ParserSettings::valueOf(context->getSettingsRef().dialect_type));
    ASTPtr ast = parseQuery(
        parser, query.data(), query.data() + query.size(), "in file " + file_name, 0, context->getSettingsRef().max_parser_depth);

    auto & ast_create_query = ast->as<ASTCreateQuery &>();
    ast_create_query.database = database;

    InterpreterCreateQuery interpreter(ast, context);
    interpreter.setInternal(true);
    interpreter.setForceAttach(true);
    interpreter.setForceRestoreData(has_force_restore_data_flag);
    interpreter.execute();
}


static void loadDatabase(
    ContextMutablePtr context,
    const String & database,
    const String & database_path,
    bool force_restore_data)
{
    String database_attach_query;
    String database_metadata_file = database_path + ".sql";

    if (fs::exists(fs::path(database_metadata_file)))
    {
        /// There is .sql file with database creation statement.
        ReadBufferFromFile in(database_metadata_file, 1024);
        readStringUntilEOF(database_attach_query, in);
    }
    else if (fs::exists(fs::path(database_path)))
    {
        /// Database exists, but .sql file is absent. It's old-style Ordinary database (e.g. system or default)
        database_attach_query = "ATTACH DATABASE " + backQuoteIfNeed(database) + " ENGINE = Ordinary";
    }
    else
    {
        /// It's first server run and we need create default and system databases.
        /// .sql file with database engine will be written for CREATE query.
        database_attach_query = "CREATE DATABASE " + backQuoteIfNeed(database) + " ENGINE = Atomic";
    }

    try
    {
        executeCreateQuery(database_attach_query, context, database, database_metadata_file, force_restore_data);
    }
    catch (Exception & e)
    {
        e.addMessage(fmt::format("while loading database {} from path {}", backQuote(database), database_path));
        throw;
    }
}


void loadMetadata(ContextMutablePtr context, const String & default_database_name)
{
    Poco::Logger * log = &Poco::Logger::get("loadMetadata");

    String path = context->getPath() + "metadata";

    /** There may exist 'force_restore_data' file, that means,
      *  skip safety threshold on difference of data parts while initializing tables.
      * This file is deleted after successful loading of tables.
      * (flag is "one-shot")
      */
    auto force_restore_data_flag_file = fs::path(context->getFlagsPath()) / "force_restore_data";
    bool has_force_restore_data_flag = fs::exists(force_restore_data_flag_file);

    /// Loop over databases.
    std::map<String, String> databases;
    fs::directory_iterator dir_end;
    for (fs::directory_iterator it(path); it != dir_end; ++it)
    {
        if (it->is_symlink())
            continue;

        const auto current_file = it->path().filename().string();
        if (!it->is_directory())
        {
            /// TODO: DETACH DATABASE PERMANENTLY ?
            if (fs::path(current_file).extension() == ".sql")
            {
                String db_name = fs::path(current_file).stem();
                if (db_name != DatabaseCatalog::SYSTEM_DATABASE)
                    databases.emplace(unescapeForFileName(db_name), fs::path(path) / db_name);
            }

            /// Temporary fails may be left from previous server runs.
            if (fs::path(current_file).extension() == ".tmp")
            {
                LOG_WARNING(log, "Removing temporary file {}", it->path().string());
                try
                {
                    fs::remove(it->path());
                }
                catch (...)
                {
                    /// It does not prevent server to startup.
                    tryLogCurrentException(log);
                }
            }

            continue;
        }

        /// For '.svn', '.gitignore' directory and similar.
        if (current_file.at(0) == '.')
            continue;

        if (current_file == DatabaseCatalog::SYSTEM_DATABASE)
            continue;

        databases.emplace(unescapeForFileName(current_file), it->path().string());
    }

    /// clickhouse-local creates DatabaseMemory as default database by itself
    /// For clickhouse-server we need create default database
    bool create_default_db_if_not_exists = !default_database_name.empty();
    bool metadata_dir_for_default_db_already_exists = databases.count(default_database_name);
    if (create_default_db_if_not_exists && !metadata_dir_for_default_db_already_exists)
        databases.emplace(default_database_name, path + "/" + escapeForFileName(default_database_name));

    for (const auto & [name, db_path] : databases)
        loadDatabase(context, name, db_path, has_force_restore_data_flag);

    if (has_force_restore_data_flag)
    {
        try
        {
            fs::remove(force_restore_data_flag_file);
        }
        catch (...)
        {
            tryLogCurrentException("Load metadata", "Can't remove force restore file to enable data sanity checks");
        }
    }
}


void loadMetadataSystem(ContextMutablePtr context)
{
    String path = context->getPath() + "metadata/" + DatabaseCatalog::SYSTEM_DATABASE;
    String metadata_file = path + ".sql";
    if (fs::exists(fs::path(path)) || fs::exists(fs::path(metadata_file)))
    {
        /// 'has_force_restore_data_flag' is true, to not fail on loading query_log table, if it is corrupted.
        loadDatabase(context, DatabaseCatalog::SYSTEM_DATABASE, path, true);
    }
    else
    {
        /// Initialize system database manually
        String database_create_query = "CREATE DATABASE ";
        database_create_query += DatabaseCatalog::SYSTEM_DATABASE;
        database_create_query += " ENGINE=Atomic";
        executeCreateQuery(database_create_query, context, DatabaseCatalog::SYSTEM_DATABASE, "<no file>", true);
    }

}

/* Load schema files from hdfs*/
void reloadFormatSchema(String remote_format_schema_path, String format_schema_path, Poco::Logger * log)
{
#if USE_HDFS
    if (!remote_format_schema_path.empty())
    {
        remote_format_schema_path += "/"; // add it by default
        // try download files from remote_format_schema_path to format_schema_path
        Poco::URI remote_uri(remote_format_schema_path);
        if (remote_uri.getScheme() == "hdfs")
        {
            HDFSBuilderPtr builder = createHDFSBuilder(remote_uri);
            HDFSFSPtr fs = createHDFSFS(builder.get());
            int num = 0;
            hdfsFileInfo* files = hdfsListDirectory(fs.get(), remote_uri.getPath().c_str(), &num);
            for (int i = 0; i < num; i++)
            {
                String fileName(files[i].mName);
                    Poco::Path path(fileName);
                String shortFileName = path.getFileName();
                String suffix = path.getExtension();
                if (files[i].mKind == kObjectKindDirectory || (suffix != "proto" && suffix != "capnp")) continue; // skip directory
                Poco::File target_file(format_schema_path+ "/" + shortFileName);
                // avoid download same file multiple times, checking size for now, it is not solid but should work online
                if (target_file.exists() && (target_file.getSize() == UInt64(files[i].mSize)))
                {
                    if(log)
                    {

                        LOG_TRACE(log, "skip get same size remote_format_schema " + shortFileName);
                    }
                    continue;
                }

                Poco::File file(format_schema_path+"/chtmp_" + shortFileName);
                if (file.exists()) file.remove(); // remove last residual file

                ReadBufferFromByteHDFS reader(fileName);
                WriteBufferFromFile writer(file.path());
                copyData(reader, writer, nullptr);
                if (target_file.exists()) target_file.remove();

                file.renameTo(format_schema_path+ "/" + shortFileName);
                if(log)
                {
                    LOG_INFO(log, "get remote_format_schema " + shortFileName);
                }
            }
            hdfsFreeFileInfo(files, num);
        }
        else
        {
            if(log) {LOG_ERROR(log, "remote_format_schema_path only support hdfs");}
        }
    }
#endif
    return;
}

}
