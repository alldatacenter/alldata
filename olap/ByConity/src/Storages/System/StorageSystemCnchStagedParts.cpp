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

#include <Storages/System/StorageSystemCnchStagedParts.h>

#include <Catalog/Catalog.h>
#include <CloudServices/CnchPartsHelper.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/System/CollectWhereClausePredicate.h>
namespace DB
{

namespace ErrorCodes
{
    extern const int INCORRECT_QUERY;
    extern const int UNSUPPORTED_PARAMETER;
}
NamesAndTypesList StorageSystemCnchStagedParts::getNamesAndTypes()
{
    return {
        {"partition", std::make_shared<DataTypeString>()},
        {"name", std::make_shared<DataTypeString>()},
        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"to_publish", std::make_shared<DataTypeUInt8>()},
        {"to_gc", std::make_shared<DataTypeUInt8>()},
        {"bytes_on_disk", std::make_shared<DataTypeUInt64>()},
        {"rows_count", std::make_shared<DataTypeUInt64>()},
        {"columns", std::make_shared<DataTypeString>()},
        {"marks_count", std::make_shared<DataTypeUInt64>()},
        {"ttl", std::make_shared<DataTypeString>()},
        {"commit_time", std::make_shared<DataTypeDateTime>()},
        {"columns_commit_time", std::make_shared<DataTypeDateTime>()},
        {"previous_version", std::make_shared<DataTypeUInt64>()},
        {"partition_id", std::make_shared<DataTypeString>()},
        {"bucket_number", std::make_shared<DataTypeInt64>()},
        {"hdfs_path", std::make_shared<DataTypeString>()},
    };
}

ColumnsDescription StorageSystemCnchStagedParts::getColumnsAndAlias()
{
    auto columns = ColumnsDescription(getNamesAndTypes());

    auto add_alias = [&](const String & alias_name, const String & column_name) {
        ColumnDescription column(alias_name, columns.get(column_name).type);
        column.default_desc.kind = ColumnDefaultKind::Alias;
        column.default_desc.expression = std::make_shared<ASTIdentifier>(column_name);
        columns.add(column);
    };

    /// Add aliases for column names for align with table system.parts.
    add_alias("bytes", "bytes_on_disk");
    add_alias("rows", "rows_count");

    return columns;
}

void StorageSystemCnchStagedParts::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo & query_info) const
{
    auto cnch_catalog = context->getCnchCatalog();
    if (context->getServerType() != ServerType::cnch_server || !cnch_catalog)
        throw Exception("Table system.cnch_staged_parts only support cnch_server", ErrorCodes::NOT_IMPLEMENTED);

    // check for required structure of WHERE clause for cnch_staged_parts
    ASTPtr where_expression = query_info.query->as<ASTSelectQuery>()->where();
    const std::vector<std::map<String,String>> value_by_column_names = collectWhereORClausePredicate(where_expression, context);

    String only_selected_db;
    String only_selected_table;
    bool enable_filter_by_table = false;

    if (value_by_column_names.size() == 1)
    {
        const auto value_by_column_name = value_by_column_names.at(0);
        auto db_it = value_by_column_name.find("database");
        auto table_it = value_by_column_name.find("table");
        if ((db_it != value_by_column_name.end()) &&
            (table_it != value_by_column_name.end()))
        {
            only_selected_db = db_it->second;
            only_selected_table = table_it->second;
            enable_filter_by_table = true;
            LOG_TRACE(&Poco::Logger::get("StorageSystemCnchStagedParts"),
                    "filtering from catalog by table with db name {} and table name {}",
                    only_selected_db, only_selected_table);
        }
    }

    if (!enable_filter_by_table)
        throw Exception(
            "Please check if where condition follow this form "
            "system.cnch_staged_parts WHERE database = 'some_name' AND table = 'another_name'",
            ErrorCodes::INCORRECT_QUERY);
    TransactionCnchPtr cnch_txn = context->getCurrentTransaction();
    TxnTimestamp start_time = cnch_txn ? cnch_txn->getStartTime() : TxnTimestamp{context->getTimestamp()};

    DB::StoragePtr table = cnch_catalog->tryGetTable(*context, only_selected_db, only_selected_table, start_time);
    if (!table)
        return;

    auto * data = dynamic_cast<StorageCnchMergeTree *>(table.get());

    if (!data || !data->getInMemoryMetadataPtr()->hasUniqueKey())
        throw Exception("Table system.cnch_staged_parts only support CnchMergeTree unique engine", ErrorCodes::UNSUPPORTED_PARAMETER);

    auto all_parts = CnchPartsHelper::toIMergeTreeDataPartsVector(cnch_catalog->getStagedParts(table, start_time));
    auto visible_parts = CnchPartsHelper::calcVisibleParts(all_parts, false);
    auto current_visible = visible_parts.cbegin();

    //get CNCH staged parts
    for (size_t i = 0, size = all_parts.size(); i != size; ++i)
    {
        if (all_parts[i]->deleted)
            continue;
        size_t col_num = 0;
        {
            WriteBufferFromOwnString out;
            all_parts[i]->partition.serializeText(*data, out, format_settings);
            res_columns[col_num++]->insert(out.str());
        }
        res_columns[col_num++]->insert(all_parts[i]->name);
        res_columns[col_num++]->insert(only_selected_db);
        res_columns[col_num++]->insert(only_selected_table);

        /// all_parts and visible_parts are both ordered by info.
        /// For each part within all_parts, we find the first part greater than or equal to it in visible_parts,
        /// which means the part is visible if they are equal to each other
        while (current_visible != visible_parts.cend() && (*current_visible)->info < all_parts[i]->info)
            ++current_visible;
        bool to_publish = current_visible != visible_parts.cend() && all_parts[i]->info == (*current_visible)->info;
        res_columns[col_num++]->insert(to_publish);
        res_columns[col_num++]->insert(!to_publish);

        res_columns[col_num++]->insert(all_parts[i]->getBytesOnDisk());
        res_columns[col_num++]->insert(all_parts[i]->rows_count);
        res_columns[col_num++]->insert(all_parts[i]->getColumnsPtr()->toString());
        res_columns[col_num++]->insert(all_parts[i]->getMarksCount());

        String ttl = std::to_string(all_parts[i]->ttl_infos.table_ttl.min) + "," + std::to_string(all_parts[i]->ttl_infos.table_ttl.max);
        res_columns[col_num++]->insert(ttl);

        // first 48 bits represent times
        res_columns[col_num++]->insert((all_parts[i]->commit_time.toUInt64() >> 18) / 1000);
        res_columns[col_num++]->insert((all_parts[i]->columns_commit_time.toUInt64() >> 18) / 1000);

        res_columns[col_num++]->insert(all_parts[i]->info.hint_mutation);
        res_columns[col_num++]->insert(all_parts[i]->info.partition_id);
        res_columns[col_num++]->insert(all_parts[i]->bucket_number);
        res_columns[col_num++]->insert(all_parts[i]->getFullPath());
    }
}
}
