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

#include <Catalog/CatalogFactory.h>
#include <Dictionaries/DictionaryFactory.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <CloudServices/CnchCreateQueryHelper.h>
#include <Parsers/parseQuery.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/ASTCreateQuery.h>
#include <Common/Status.h>
#include "Core/UUID.h"
#include <Interpreters/Context.h>
#include <Interpreters/InterpreterCreateQuery.h>

#include <Databases/DatabaseCnch.h>
#include <Protos/RPCHelpers.h>
#include <Transaction/TxnTimestamp.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int CATALOG_SERVICE_INTERNAL_ERROR;
}

namespace Catalog
{

CatalogFactory::DatabasePtr CatalogFactory::getDatabaseByDataModel(const DB::Protos::DataModelDB & db_model, const ContextPtr & context)
{
    auto uuid = db_model.has_uuid() ? RPCHelpers::createUUID(db_model.uuid()) : UUIDHelpers::Nil;
    return std::make_shared<DatabaseCnch>(db_model.name(), uuid, context);
}

StoragePtr CatalogFactory::getTableByDataModel(
    ContextMutablePtr context,
    const DB::Protos::DataModelTable * table_model)
{
    const auto & db = table_model->database();
    const auto & table = table_model->name();
    const auto & create_query = table_model->definition();
    auto storage_ptr = getTableByDefinition(context, db, table, create_query);
    storage_ptr->commit_time = TxnTimestamp{table_model->commit_time()};
    if (auto * merge_tree = dynamic_cast<MergeTreeMetaBase *>(storage_ptr.get()))
    {
        merge_tree->part_columns = std::make_shared<NamesAndTypesList>(merge_tree->getInMemoryMetadataPtr()->getColumns().getAllPhysical());
        for (const auto & version : table_model->definitions())
        {
            auto s = getTableByDefinition(context, db, table, version.definition());
            merge_tree->previous_versions_part_columns[version.commit_time()] = std::make_shared<NamesAndTypesList>(s->getInMemoryMetadataPtr()->getColumns().getAllPhysical());
        }

    }
    storage_ptr->is_dropped = DB::Status::isDeleted(table_model->status());
    storage_ptr->is_detached = DB::Status::isDetached(table_model->status());
    return storage_ptr;
}

StoragePtr CatalogFactory::getTableByDefinition(
    ContextMutablePtr context,
    [[maybe_unused]] const String & db,
    [[maybe_unused]] const String & table,
    const String & create)
{
    auto res = createStorageFromQuery(create, context);
    res->setCreateTableSql(create);
    return res;
}

ASTPtr CatalogFactory::getCreateDictionaryByDataModel(const DB::Protos::DataModelDictionary & dict_model)
{
    const auto & create_query = dict_model.definition();
    const char *begin = create_query.data();
    const char *end = begin + create_query.size();
    ParserQuery parser(end);
    ASTPtr ast = parseQuery(parser, begin, end, "", 0, 0);
    ASTCreateQuery *create_ast = ast->as<ASTCreateQuery>();
    if (!create_ast)
        throw Exception("Wrong dictionary definition.", ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

    return ast;
}

}

}
