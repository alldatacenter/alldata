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

#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTFunction.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeUUID.h>
#include <Storages/System/StorageSystemMetastore.h>
#include <Storages/MergeTree/MergeTreeMeta.h>
#include <Storages/MergeTree/MergeTreeData.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INCORRECT_QUERY;
    extern const int LOGICAL_ERROR;
}

/// Note: used to parse database and table from where condition. Only support to parse `database='xxx' and table='xxx'`. Consider to inhence functionality later.
void evaluateWhereCondition(const ASTPtr & ast, std::map<String,String> & conditions)
{
    if (!ast)
        return;

    if (ASTFunction * func = ast->as<ASTFunction>())
    {
        if (func->name == "and" || func->name == "or")
        {
            for (auto & arg : func->arguments->children)
                evaluateWhereCondition(arg, conditions);
        }
        else if (func->name == "equals")
        {
            String key, value;
            for (auto & arg : func->arguments->children)
            {
                if (ASTIdentifier * ident = arg->as<ASTIdentifier>())
                    key = ident->name();
                else if (ASTLiteral * literal = arg->as<ASTLiteral>())
                {
                    if (literal->value.getType() == Field::Types::String)
                        value = literal->value.get<String>();
                }
            }
            if (!key.empty() && !value.empty())
                conditions.emplace(key, value);
        }
    }
    else
        return;
}

NamesAndTypesList StorageSystemMetastore::getNamesAndTypes()
{
    return {
        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"uuid",  std::make_shared<DataTypeUUID>()},
        {"meta_key", std::make_shared<DataTypeString>()},
    };
}

void StorageSystemMetastore::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo & query_info) const
{

    std::map<String, String> conditions;
    evaluateWhereCondition(query_info.query->as<ASTSelectQuery>()->where(), conditions);

    String database = conditions["database"];
    String table = conditions["table"];

    if (database.empty() || table.empty())
        throw Exception("Missing databse or table in where clause.", ErrorCodes::INCORRECT_QUERY);


    StoragePtr storage = DatabaseCatalog::instance().getTable(StorageID{database, table}, context);

    if (auto * data = dynamic_cast<MergeTreeData *>(storage.get()))
    {
        auto metastore = data->getMetastore();
        auto it = metastore->getMetaInfo();
        auto uuid_prefix_len = toString(storage->getStorageID().uuid).length() + 1; /// length of 'uuid_'
        while(it->hasNext())
        {
            res_columns[0]->insert(database);
            res_columns[1]->insert(table);
            res_columns[2]->insert(storage->getStorageID().uuid);
            res_columns[3]->insert(it->key().substr(uuid_prefix_len));
            it->next();
        }
    }
    else
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Metastore is not initialized in table {}.{}", database, table);
}

}
