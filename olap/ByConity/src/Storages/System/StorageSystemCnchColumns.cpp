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

#include <Storages/System/StorageSystemCnchColumns.h>
#include <Catalog/Catalog.h>
#include <Catalog/CatalogFactory.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>
#include <Common/Status.h>
#include <Parsers/queryToString.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Poco/Logger.h>

namespace DB
{
NamesAndTypesList StorageSystemCnchColumns::getNamesAndTypes()
{
    return {
        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"name", std::make_shared<DataTypeString>()},
        {"table_uuid", std::make_shared<DataTypeUUID>()},
        {"type", std::make_shared<DataTypeString>()},
        {"flags", std::make_shared<DataTypeString>()},
        {"default_kind", std::make_shared<DataTypeString>()},
        {"default_expression", std::make_shared<DataTypeString>()},
        {"data_compressed_bytes", std::make_shared<DataTypeUInt64>()},
        {"data_uncompressed_bytes", std::make_shared<DataTypeUInt64>()},
        {"marks_bytes", std::make_shared<DataTypeUInt64>()},
        {"comment", std::make_shared<DataTypeString>()},
        {"is_in_partition_key", std::make_shared<DataTypeUInt8>()},
        {"is_in_sorting_key", std::make_shared<DataTypeUInt8>()},
        {"is_in_primary_key", std::make_shared<DataTypeUInt8>()},
        {"is_in_sampling_key", std::make_shared<DataTypeUInt8>()},
        {"compression_codec", std::make_shared<DataTypeString>()}};
}

void StorageSystemCnchColumns::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo &) const
{
    Catalog::CatalogPtr cnch_catalog = context->getCnchCatalog();

    if (context->getServerType() == ServerType::cnch_server && cnch_catalog)
    {
        Stopwatch stop_watch;
        stop_watch.start();
        Catalog::Catalog::DataModelTables res = cnch_catalog->getAllTables();
        UInt64 time_pass_ms = stop_watch.elapsedMilliseconds();
        if (time_pass_ms > 2000)
            LOG_INFO(&Poco::Logger::get("StorageSystemCnchColumns"),
                "cnch_catalog->getAllTables() took {} ms", time_pass_ms);

        ContextMutablePtr mutable_context = Context::createCopy(context);
        for (size_t i = 0, size = res.size(); i != size; ++i)
        {
            if (!Status::isDeleted(res[i].status()))
            {
                String db = res[i].database();
                String table_name = res[i].name();
                auto table_uuid = RPCHelpers::createUUID(res[i].uuid());
                String create_query = res[i].definition();

                ColumnsDescription columns;
                Names cols_required_for_partition_key;
                Names cols_required_for_sorting_key;
                Names cols_required_for_primary_key;
                Names cols_required_for_sampling;
                MergeTreeData::ColumnSizeByName column_sizes;

                {
                    StoragePtr storage
                        = Catalog::CatalogFactory::getTableByDefinition(mutable_context, db, table_name, create_query);

                    StorageMetadataPtr metadata_snapshot = storage->getInMemoryMetadataPtr();

                    columns = metadata_snapshot->getColumns();

                    cols_required_for_partition_key = metadata_snapshot->getColumnsRequiredForPartitionKey();
                    cols_required_for_sorting_key = metadata_snapshot->getColumnsRequiredForSortingKey();
                    cols_required_for_primary_key = metadata_snapshot->getColumnsRequiredForPrimaryKey();
                    cols_required_for_sampling = metadata_snapshot->getColumnsRequiredForSampling();
                    column_sizes = storage->getColumnSizes();
                }

                for (const auto & column : columns)
                {
                    size_t res_index = 0;
                    res_columns[res_index++]->insert(db);
                    res_columns[res_index++]->insert(table_name);
                    res_columns[res_index++]->insert(column.name);
                    res_columns[res_index++]->insert(table_uuid);
                    res_columns[res_index++]->insert(column.type->getName());
                    res_columns[res_index++]->insert(String("")); /// what do bloom set mean here , taken from clickhouse system.columns

                    if (column.default_desc.expression)
                    {
                        res_columns[res_index++]->insert(toString(column.default_desc.kind));
                        res_columns[res_index++]->insert(queryToString(column.default_desc.expression));
                    }
                    else
                    {
                        res_columns[res_index++]->insertDefault();
                        res_columns[res_index++]->insertDefault();
                    }

                    {
                        const auto it = column_sizes.find(column.name);
                        if (it == std::end(column_sizes))
                        {
                            res_columns[res_index++]->insertDefault();
                            res_columns[res_index++]->insertDefault();
                            res_columns[res_index++]->insertDefault();
                        }
                        else
                        {
                            res_columns[res_index++]->insert(it->second.data_compressed);
                            res_columns[res_index++]->insert(it->second.data_uncompressed);
                            res_columns[res_index++]->insert(it->second.marks);
                        }
                    }

                    res_columns[res_index++]->insert(column.comment);

                    {
                        auto find_in_vector = [&key = column.name](const Names & names) {
                            return std::find(names.cbegin(), names.cend(), key) != names.end();
                        };

                        res_columns[res_index++]->insert(find_in_vector(cols_required_for_partition_key));
                        res_columns[res_index++]->insert(find_in_vector(cols_required_for_sorting_key));
                        res_columns[res_index++]->insert(find_in_vector(cols_required_for_primary_key));
                        res_columns[res_index++]->insert(find_in_vector(cols_required_for_sampling));
                    }
                    if (column.codec)
                        res_columns[res_index++]->insert(queryToString(column.codec));
                    else
                        res_columns[res_index++]->insertDefault();
                }
            }
        }
    }
}
}
