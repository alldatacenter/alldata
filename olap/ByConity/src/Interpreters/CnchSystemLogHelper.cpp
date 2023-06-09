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

#include <Interpreters/CnchSystemLogHelper.h>
#include <Transaction/Actions/DDLCreateAction.h>
#include <Catalog/Catalog.h>
#include <Common/SettingsChanges.h>
#include <Interpreters/Context_fwd.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/ParserAlterQuery.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/parseQuery.h>
#include <Parsers/queryToString.h>
#include <Parsers/formatAST.h>
#include <Interpreters/InterpreterAlterQuery.h>
#include <Interpreters/InterpreterCreateQuery.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <CloudServices/CnchCreateQueryHelper.h>


namespace DB
{

bool createDatabaseInCatalog(
    const ContextPtr & global_context,
    const String & database_name,
    Poco::Logger * log)
{
    bool ret = true;
    try
    {
        TxnTimestamp start_time = global_context->getTimestamp();
        auto catalog = global_context->getCnchCatalog();
        if (!catalog->isDatabaseExists(database_name, start_time))
        {
            try
            {
                LOG_INFO(log, "Creating database {} in catalog", database_name);
                catalog->createDatabase(database_name, UUIDHelpers::generateV4(), start_time, start_time);
            }
            catch (Exception & e)
            {
                LOG_WARNING(log, "Failed to create database {}, got exception {}", database_name, e.message());
                ret = false;
            }
        }
    }
    catch (Exception & e)
    {
        LOG_WARNING(log, "Unable to get timestamp, got exception: {}", e.message());
        ret = false;
    }

    return ret;
}

String makeAlterColumnQuery(const String& database, const String& table, const Block& expected, const Block& actual)
{
    if (blocksHaveEqualStructure(actual, expected))
        return {};

    Names expected_names = expected.getNames();
    std::sort(expected_names.begin(), expected_names.end());

    Names actual_names = actual.getNames();
    std::sort(actual_names.begin(), actual_names.end());

    Names adding_columns;
    std::set_difference(
        expected_names.begin(),
        expected_names.end(),
        actual_names.begin(),
        actual_names.end(),
        std::inserter(adding_columns, adding_columns.begin())
    );

    Names dropping_columns;
    std::set_difference(
        actual_names.begin(),
        actual_names.end(),
        expected_names.begin(),
        expected_names.end(),
        std::inserter(dropping_columns, dropping_columns.begin())
    );

    Names intersection_columns;
    std::set_intersection(
        actual_names.begin(),
        actual_names.end(),
        expected_names.begin(),
        expected_names.end(),
        std::inserter(intersection_columns, intersection_columns.begin())
    );

    Names type_changing_columns;
    for (const String& s : intersection_columns)
    {
        const ColumnWithTypeAndName& expected_type_and_name = expected.getByName(s);
        const ColumnWithTypeAndName& actual_type_and_name = actual.getByName(s);

        if (expected_type_and_name.type && actual_type_and_name.type)
        {
            if (expected_type_and_name.type->getName() != actual_type_and_name.type->getName())
            {
                type_changing_columns.push_back(s);
            }
        }
    }

    /// find name_after: ADD COLUMN [IF NOT EXISTS] name [type] [default_expr] [codec] [AFTER name_after | FIRST]
    std::vector<std::pair<std::string, std::string>> name_after_map;
    if (!adding_columns.empty())
    {
        Names unsorted_expected_names = expected.getNames();
        std::string name_after_expr = "FIRST";
        for (const std::string & s : unsorted_expected_names)
        {
            auto it = std::find(std::begin(adding_columns), std::end(adding_columns), s);
            if (it != std::end(adding_columns))
                name_after_map.push_back(std::make_pair(*it, name_after_expr));

            name_after_expr = "AFTER "+ s;
        }
    }

    if (!name_after_map.empty() || !dropping_columns.empty() || !type_changing_columns.empty())
    {
        String alter_query = "ALTER TABLE " + database + "." + table;

        String separator = "";
        for (const std::string & s : dropping_columns)
        {
            alter_query += separator + " DROP COLUMN " + backQuoteIfNeed(s);
            separator = ",";
        }

        for (const std::string & s : type_changing_columns)
        {
            const ColumnWithTypeAndName& type_and_name = expected.getByName(s);
            if (type_and_name.type)
            {
                alter_query += separator + " MODIFY COLUMN " + backQuoteIfNeed(s)
                                + " " + type_and_name.type->getName();
                separator = ",";
            }
        }

        for (const auto & p : name_after_map)
        {
            const ColumnWithTypeAndName& type_and_name = expected.getByName(p.first);
            if (type_and_name.type)
            {
                alter_query += separator + " ADD COLUMN " + backQuoteIfNeed(p.first)
                                + " " + type_and_name.type->getName()
                                + " " + p.second;
                separator = ",";
            }
        }

        return alter_query;
    }

    return {};
}

bool createCnchTable(
    ContextPtr global_context,
    const String & database,
    const String & table,
    ASTPtr & create_query_ast,
    Poco::Logger * log)
{
    bool ret = true;
    ParserCreateQuery parser;
    const String table_description = database + "." + table;

    auto query_context = Context::createCopy(global_context);
    query_context->makeSessionContext();
    query_context->makeQueryContext();

    auto & create = create_query_ast->as<ASTCreateQuery &>();
    create.uuid = UUIDHelpers::generateV4();
    String create_table_sql = queryToString(create);

    auto & txn_coordinator = global_context->getCnchTransactionCoordinator();
    TransactionCnchPtr txn = txn_coordinator.createTransaction(CreateTransactionOption().setContext(query_context));

    CreateActionParams params = {database, table, create.uuid, create_table_sql};
    auto create_table = txn->createAction<DDLCreateAction>(std::move(params));
    txn->appendAction(std::move(create_table));

    try
    {
        txn_coordinator.commitV1(txn);
    }
    catch (Exception & e)
    {
        LOG_WARNING(log, "Failed to create CNCH table {}, got exception: {}", table_description, e.message());
        ret = false;
    }

    txn_coordinator.finishTransaction(txn);
    return ret;
}

bool prepareCnchTable(
    ContextPtr global_context,
    const String & database,
    const String & table,
    ASTPtr & create_query_ast,
    Poco::Logger * log)
{
    auto catalog = global_context->getCnchCatalog();

    if (!catalog->isTableExists(database, table, TxnTimestamp::maxTS()))
    {
        LOG_INFO(log, "Creating CNCH System log table: {}.{}", database, table);
        return createCnchTable(global_context, database, table, create_query_ast, log);
    }

    return true;
}

bool syncTableSchema(
    ContextPtr global_context,
    const String & database,
    const String & table,
    const Block & expected_block,
    Poco::Logger * log)
{
    bool ret = true;
    auto catalog = global_context->getCnchCatalog();

    auto query_context = Context::createCopy(global_context);
    query_context->makeSessionContext();
    query_context->makeQueryContext();

    StoragePtr storage = nullptr;
    try
    {
        storage = catalog->getTable(*query_context, database, table, TxnTimestamp::maxTS());
    }
    catch (Exception & e)
    {
        LOG_WARNING(log, "Failed to get CNCH table {}.{}, got exception: {}", database, table, e.message());
        ret = false;
    }

    if (storage)
    {
        const Block actual_block = storage->getInMemoryMetadataPtr()->getSampleBlockNonMaterialized();
        auto execute_alter_query = [&, query_context] (const String & alter_query)
        {
            try
            {
                ParserAlterQuery parser;
                ASTPtr ast = parseQuery(parser, alter_query, 0, DBMS_DEFAULT_MAX_PARSER_DEPTH);

                if (ast)
                {
                    TransactionCnchPtr txn = global_context->getCnchTransactionCoordinator().createTransaction();
                    if (txn)
                    {
                            LOG_INFO(log, "execute alter query: " + alter_query);
                            query_context->setCurrentTransaction(txn);
                            InterpreterAlterQuery intepreter_alter_query{ast, query_context};
                            intepreter_alter_query.execute();
                            query_context->getCnchTransactionCoordinator().finishTransaction(txn);
                    }
                }
            }
            catch (Exception & e)
            {
                LOG_WARNING(log, "Failed to alter table for cnch system log, got exception: " + e.message());
                return false;
            }
            return true;
        };
        if (!blocksHaveEqualStructure(actual_block, expected_block))
        {
            String alter_query = makeAlterColumnQuery(database, table, expected_block, actual_block);


            if (!alter_query.empty())
            {
                ret &= execute_alter_query(alter_query);
            }
        }
    }
    else
        ret = false;

    return ret;
}

bool createView(
    ContextPtr global_context,
    const String & database,
    const String & table,
    Poco::Logger * log)
{
    bool ret = true;

    try
    {
        auto query_context = Context::createCopy(global_context);
        query_context->makeSessionContext();
        query_context->makeQueryContext();

        String create_view_query = "CREATE VIEW IF NOT EXISTS system." + table + " AS SELECT * from " + database + "." + table;
        ParserCreateQuery create_parser;
        ASTPtr create_view_ast = parseQuery(create_parser, create_view_query, 0, DBMS_DEFAULT_MAX_PARSER_DEPTH);
        InterpreterCreateQuery create_interpreter(create_view_ast, query_context);
        create_interpreter.execute();
        LOG_INFO(log, "Create view with query {} successful!", create_view_query);
    }
    catch (Exception & e)
    {
        LOG_DEBUG(log, "Failed to create view for {}.{}, met exception {}", database, table, e.message());
        ret = false;
    }
    return ret;
}

}/// end namespace
