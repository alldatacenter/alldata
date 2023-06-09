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
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeUUID.h>
#include <Interpreters/Context.h>
#include <Storages/System/StorageSystemCnchTablesHistory.h>
#include <Storages/VirtualColumnUtils.h>
#include <DataStreams/NullBlockInputStream.h>
#include <Processors/Sources/SourceFromInputStream.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

class CnchTablesHistoryInputStream : public IBlockInputStream
{
public:
    CnchTablesHistoryInputStream(
        const std::vector<UInt8> & columns_mask_,
        const Block & header_,
        UInt64 max_block_size_,
        std::vector<DB::Protos::DataModelTable> tables_)
        :columns_mask(columns_mask_), header(header_), max_block_size(max_block_size_),
         tables(tables_)
    {
    }

    String getName() const override { return "CnchTablesHistory"; }
    Block getHeader() const override { return header; }

protected:
    Block readImpl() override
    {
        if (table_num >= tables.size())
            return {};

        Block res = header;
        MutableColumns res_columns = header.cloneEmptyColumns();
        size_t rows_count = 0;

        while (rows_count < max_block_size && table_num < tables.size())
        {
            auto & current_table = tables[table_num++];

            size_t src_index = 0;
            size_t res_index = 0;

            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(current_table.database());
            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(current_table.name());
            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(RPCHelpers::createUUID(current_table.uuid()));
            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(current_table.definition());
            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(current_table.txnid());
            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(current_table.previous_version());
            if (columns_mask[src_index++])
                res_columns[res_index++]->insert((current_table.commit_time() >> 18) /1000);

            rows_count++;
        }

        res.setColumns(std::move(res_columns));
        return res;
    }

private:
    std::vector<UInt8> columns_mask;
    Block header;
    UInt64 max_block_size;
    std::vector<DB::Protos::DataModelTable> tables;
    size_t table_num = 0;
};

StorageSystemCnchTablesHistory::StorageSystemCnchTablesHistory(const StorageID & table_id_)
    : IStorage(table_id_)
{
    StorageInMemoryMetadata storage_metadata;
    storage_metadata.setColumns(ColumnsDescription(
        {
            {"database", std::make_shared<DataTypeString>()},
            {"name", std::make_shared<DataTypeString>()},
            {"uuid", std::make_shared<DataTypeUUID>()},
            {"definition", std::make_shared<DataTypeString>()},
            {"current_version", std::make_shared<DataTypeUInt64>()},
            {"previous_version", std::make_shared<DataTypeUInt64>()},
            {"delete_time", std::make_shared<DataTypeDateTime>()},
        }));
    setInMemoryMetadata(storage_metadata);
}

Pipe StorageSystemCnchTablesHistory::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr context,
    QueryProcessingStage::Enum /*processed_stage*/,
    const size_t max_block_size,
    const unsigned /*num_streams*/)
{
    Catalog::CatalogPtr cnch_catalog = context->getCnchCatalog();

    if (context->getServerType() != ServerType::cnch_server || !cnch_catalog)
        throw Exception("Table system.cnch_tables_history only support cnch_server", ErrorCodes::LOGICAL_ERROR);

    NameSet names_set(column_names.begin(), column_names.end());

    Block sample_block = metadata_snapshot->getSampleBlock();
    Block res_block;

    std::vector<UInt8> columns_mask(sample_block.columns());
    for (size_t i = 0, size = columns_mask.size(); i < size; ++i)
    {
        if (names_set.count(sample_block.getByPosition(i).name))
        {
            columns_mask[i] = 1;
            res_block.insert(sample_block.getByPosition(i));
        }
    }

    Block block_to_filter;

    auto trashed_tables_id = cnch_catalog->getTrashTableID();

    /// Add `database` column.
    MutableColumnPtr database_column_mut = ColumnString::create();
    /// Add `table` column.
    MutableColumnPtr table_column_mut = ColumnString::create();
    /// Add `uuid` column
    MutableColumnPtr uuid_column_mut = ColumnUInt128::create();
    /// ADD 'index' column
    MutableColumnPtr index_column_mut = ColumnUInt64::create();

    for (size_t i=0; i<trashed_tables_id.size(); i++)
    {
        database_column_mut->insert(trashed_tables_id[i]->database());
        table_column_mut->insert(trashed_tables_id[i]->name());
        uuid_column_mut->insert(stringToUUID(trashed_tables_id[i]->uuid()));
        index_column_mut->insert(i);
    }

    block_to_filter.insert(ColumnWithTypeAndName(std::move(database_column_mut), std::make_shared<DataTypeString>(), "database"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(table_column_mut), std::make_shared<DataTypeString>(), "table"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(uuid_column_mut), std::make_shared<DataTypeUUID>(), "uuid"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(index_column_mut), std::make_shared<DataTypeUInt64>(), "index"));

    VirtualColumnUtils::filterBlockWithQuery(query_info.query, block_to_filter, context);

    Pipe pipe;

    if (!block_to_filter.rows())
       return pipe;

    ColumnPtr filtered_index_column = block_to_filter.getByName("index").column;

    std::vector<std::shared_ptr<Protos::TableIdentifier>> requested_table_id;
    requested_table_id.reserve(filtered_index_column->size());
    for (size_t i=0; i<filtered_index_column->size(); i++)
        requested_table_id.push_back(trashed_tables_id[(*filtered_index_column)[i].get<UInt64>()]);

    auto tables = cnch_catalog->getTablesByID(requested_table_id);

    BlockInputStreamPtr input = std::make_shared<CnchTablesHistoryInputStream>(std::move(columns_mask),
        std::move(res_block), max_block_size, tables);
    ProcessorPtr source = std::make_shared<SourceFromInputStream>(input);
    pipe.addSource(source);
    return pipe;
}

}
