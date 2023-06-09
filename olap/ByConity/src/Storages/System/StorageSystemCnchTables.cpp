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

#include <Catalog/Catalog.h>
#include <Parsers/queryToString.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/parseQuery.h>
#include <Parsers/ASTClusterByElement.h>
#include <Parsers/ASTCreateQuery.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeUUID.h>
#include <Interpreters/Context.h>
#include <Storages/System/StorageSystemCnchTables.h>
#include <Storages/VirtualColumnUtils.h>
#include <Common/Status.h>
#include <common/logger_useful.h>
// #include <Parsers/ASTClusterByElement.h>
#include <Processors/Sources/NullSource.h>
#include <Processors/Sources/SourceFromSingleChunk.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}


StorageSystemCnchTables::StorageSystemCnchTables(const StorageID & table_id_)
    : IStorage(table_id_)
{
    StorageInMemoryMetadata storage_metadata;
    storage_metadata.setColumns(ColumnsDescription(
        {
            {"database", std::make_shared<DataTypeString>()},
            {"name", std::make_shared<DataTypeString>()},
            {"uuid", std::make_shared<DataTypeUUID>()},
            {"vw_name", std::make_shared<DataTypeString>()},
            {"definition", std::make_shared<DataTypeString>()},
            {"txn_id", std::make_shared<DataTypeUInt64>()},
            {"previous_version", std::make_shared<DataTypeUInt64>()},
            {"current_version", std::make_shared<DataTypeUInt64>()},
            {"modification_time", std::make_shared<DataTypeDateTime>()},
            {"is_preallocated", std::make_shared<DataTypeUInt8>()},
            {"is_detached", std::make_shared<DataTypeUInt8>()},
            {"partition_key", std::make_shared<DataTypeString>()},
            {"sorting_key", std::make_shared<DataTypeString>()},
            {"primary_key", std::make_shared<DataTypeString>()},
            {"sampling_key", std::make_shared<DataTypeString>()},
            {"cluster_key", std::make_shared<DataTypeString>()},
            {"split_number", std::make_shared<DataTypeInt64>()},
            {"with_range", std::make_shared<DataTypeUInt8>()},
        }));
    setInMemoryMetadata(storage_metadata);
}

static std::unordered_set<String> key_columns =
{
    "partition_key",
    "sorting_key",
    "primary_key",
    "sampling_key",
    "cluster_key",
    "split_number",
    "with_range"
};

Pipe StorageSystemCnchTables::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr context,
    QueryProcessingStage::Enum /*processed_stage*/,
    const size_t /*max_block_size*/,
    const unsigned /*num_streams*/)
{
    Catalog::CatalogPtr cnch_catalog = context->getCnchCatalog();

    if (context->getServerType() != ServerType::cnch_server || !cnch_catalog)
        throw Exception("Table system.cnch_tables_history only support cnch_server", ErrorCodes::LOGICAL_ERROR);

    bool require_key_columns = false;
    for (auto & name : column_names)
    {
        if (key_columns.count(name))
        {
            require_key_columns = true;
            break;
        }
    }

    NameSet names_set(column_names.begin(), column_names.end());

    Block sample_block = metadata_snapshot->getSampleBlock();
    Block header;

    std::vector<UInt8> columns_mask(sample_block.columns());
    for (size_t i = 0, size = columns_mask.size(); i < size; ++i)
    {
        if (names_set.count(sample_block.getByPosition(i).name))
        {
            columns_mask[i] = 1;
            header.insert(sample_block.getByPosition(i));
        }
    }

    Catalog::Catalog::DataModelTables table_models = cnch_catalog->getAllTables();

    Block block_to_filter;

    /// Add `database` column.
    MutableColumnPtr database_column_mut = ColumnString::create();
    /// Add `name` column.
    MutableColumnPtr name_column_mut = ColumnString::create();
    /// Add `uuid` column
    MutableColumnPtr uuid_column_mut = ColumnUUID::create();
    /// ADD 'index' column
    MutableColumnPtr index_column_mut = ColumnUInt64::create();

    for (size_t i=0; i<table_models.size(); i++)
    {
        database_column_mut->insert(table_models[i].database());
        name_column_mut->insert(table_models[i].name());
        uuid_column_mut->insert(RPCHelpers::createUUID(table_models[i].uuid()));
        index_column_mut->insert(i);
    }

    block_to_filter.insert(ColumnWithTypeAndName(std::move(database_column_mut), std::make_shared<DataTypeString>(), "database"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(name_column_mut), std::make_shared<DataTypeString>(), "name"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(uuid_column_mut), std::make_shared<DataTypeUUID>(), "uuid"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(index_column_mut), std::make_shared<DataTypeUInt64>(), "index"));

    VirtualColumnUtils::filterBlockWithQuery(query_info.query, block_to_filter, context);

    if (!block_to_filter.rows())
        return Pipe(std::make_shared<NullSource>(std::move(header)));

    ColumnPtr filtered_index_column = block_to_filter.getByName("index").column;

    MutableColumns res_columns = header.cloneEmptyColumns();

    for (size_t i = 0; i<filtered_index_column->size(); i++)
    {
        auto table_model = table_models[(*filtered_index_column)[i].get<UInt64>()];
        if (Status::isDeleted(table_model.status()))
            continue;

        size_t col_num = 0;
        size_t src_index = 0;
        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.database());

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.name());

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(RPCHelpers::createUUID(table_model.uuid()));

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.vw_name());

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.definition());

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.txnid());

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.previous_version());

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.commit_time());

        if (columns_mask[src_index++])
        {
            auto modification_time = (table_model.commit_time() >> 18) ;  // first 48 bits represent times
            res_columns[col_num++]->insert(modification_time/1000) ; // convert to seconds
        }

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(table_model.vw_name().size() != 0); // is_preallocated should be 1 when vw_name exists

        if (columns_mask[src_index++])
            res_columns[col_num++]->insert(Status::isDetached(table_model.status())) ;

        if (require_key_columns)
        {
            const char * begin = table_model.definition().data();
            const char * end = begin + table_model.definition().size();
            ParserQuery parser(end);
            IAST * ast_partition_by = nullptr;
            IAST * ast_primary_key = nullptr;
            IAST * ast_order_by = nullptr;
            IAST * ast_sample_by = nullptr;
            IAST * ast_cluster_by = nullptr;
            /// create AST from CREATE query to extract storage info
            ASTPtr ast;

            try
            {
                ast = parseQuery(parser, begin, end, "", 0, 0);
            }
            catch (...)
            {
                tryLogCurrentException(&Poco::Logger::get("StorageSystemCnchTables"));
            }

            if (ast)
            {
                const auto & ast_create_query = ast->as<ASTCreateQuery &>();
                const ASTStorage * ast_storage = ast_create_query.storage;
                if (ast_storage) {
                    ast_partition_by = ast_storage->partition_by;
                    ast_order_by = ast_storage->order_by;
                    ast_primary_key = ast_storage->primary_key;
                    ast_sample_by = ast_storage->sample_by;
                    ast_cluster_by = ast_storage->cluster_by;
                }
            }


            if (columns_mask[src_index++])
                ast_partition_by ? (res_columns[col_num++]->insert(queryToString(*ast_partition_by))) : (res_columns[col_num++]->insertDefault());


            if (columns_mask[src_index++])
                ast_order_by ? (res_columns[col_num++]->insert(queryToString(*ast_order_by))) : (res_columns[col_num++]->insertDefault());

            if (columns_mask[src_index++])
                ast_primary_key ? (res_columns[col_num++]->insert(queryToString(*ast_primary_key))) : (res_columns[col_num++]->insertDefault());

            if (columns_mask[src_index++])
                ast_sample_by ? (res_columns[col_num++]->insert(queryToString(*ast_sample_by))) : (res_columns[col_num++]->insertDefault());

            if(ast_cluster_by)
            {
                auto cluster_by = ast_cluster_by->as<ASTClusterByElement>();
                if (columns_mask[src_index++])
                    res_columns[col_num++]->insert(queryToString(*ast_cluster_by));
                if (columns_mask[src_index++])
                    res_columns[col_num++]->insert(cluster_by->split_number);
                if (columns_mask[src_index++])
                    res_columns[col_num++]->insert(cluster_by->is_with_range);
            }
            else
            {
                if (columns_mask[src_index++])
                    res_columns[col_num++]->insertDefault();

                if (columns_mask[src_index++])
                    res_columns[col_num++]->insert(-1); // shard ratio is not available

                if (columns_mask[src_index++])
                    res_columns[col_num++]->insert(0); // with range is not available
            }
        }
    }

    UInt64 num_rows = res_columns.at(0)->size();
    Chunk chunk(std::move(res_columns), num_rows);

    return Pipe(std::make_shared<SourceFromSingleChunk>(std::move(header), std::move(chunk)));
}

}
