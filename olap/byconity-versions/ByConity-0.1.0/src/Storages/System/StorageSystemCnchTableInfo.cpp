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

#include <Storages/System/StorageSystemCnchTableInfo.h>
#include <Storages/VirtualColumnUtils.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeDateTime.h>
#include <Interpreters/Context.h>
#include <Processors/Sources/SourceFromInputStream.h>
#include <Common/RpcClientPool.h>
#include <Common/HostWithPorts.h>
#include <CloudServices/CnchServerClient.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <Catalog/Catalog.h>
#include <TSO/TSOClient.h>
#include <DataStreams/NullBlockInputStream.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

class CnchTableInfoBlockInputStream : public IBlockInputStream
{
public:
    CnchTableInfoBlockInputStream(
        const std::vector<UInt8> & columns_mask_,
        const Block & header_,
        UInt64 max_block_size_,
        std::vector<DB::Protos::DataModelTableInfo> all_table_info_,
        UInt64 current_ts_)
        : columns_mask(columns_mask_), header(header_), max_block_size(max_block_size_)
        , all_table_info(all_table_info_), current_ts(current_ts_)
    {
    }

    String getName() const override { return "CnchTableInfo"; }
    Block getHeader() const override { return header; }

protected:
    Block readImpl() override
    {
        if (table_num >= all_table_info.size())
            return {};

        Block res = header;
        MutableColumns res_columns = header.cloneEmptyColumns();
        size_t rows_count = 0;

        while (rows_count < max_block_size && table_num < all_table_info.size())
        {
            auto & current_table_info = all_table_info[table_num++];

            size_t src_index = 0;
            size_t res_index = 0;

            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(current_table_info.database());
            if (columns_mask[src_index++])
                res_columns[res_index++]->insert(current_table_info.table());
            if (columns_mask[src_index++])
            {
                UInt64 last_modification_time = current_table_info.last_modification_time();
                if (last_modification_time == 0)
                    last_modification_time = current_ts;
                res_columns[res_index++]->insert((last_modification_time>>18)/1000);
            }
            if (columns_mask[src_index++])
            {
                res_columns[res_index++]->insert(current_table_info.cluster_status());
            }

            rows_count++;
        }

        res.setColumns(std::move(res_columns));
        return res;
    }

private:
    std::vector<UInt8> columns_mask;
    Block header;
    UInt64 max_block_size;
    std::vector<DB::Protos::DataModelTableInfo> all_table_info;
    UInt64 current_ts;
    size_t table_num = 0;
};

StorageSystemCnchTableInfo::StorageSystemCnchTableInfo(const StorageID & table_id_)
    : IStorage(table_id_)
{
    StorageInMemoryMetadata storage_metadata;
    storage_metadata.setColumns(ColumnsDescription(
        {
            {"database", std::make_shared<DataTypeString>()},
            {"table", std::make_shared<DataTypeString>()},
            {"last_modification_time", std::make_shared<DataTypeDateTime>()},
            {"cluster_status", std::make_shared<DataTypeUInt8>()}
        }));
    setInMemoryMetadata(storage_metadata);
}

Pipe StorageSystemCnchTableInfo::read(
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
        throw Exception("Table system.cnch_table_info only support cnch_server", ErrorCodes::LOGICAL_ERROR);

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

    auto table_ids = cnch_catalog->getAllTablesID();

    /// Add `database` column.
    MutableColumnPtr database_column_mut = ColumnString::create();
    /// Add `table` column.
    MutableColumnPtr table_column_mut = ColumnString::create();
    /// ADD 'index' column
    MutableColumnPtr index_column_mut = ColumnUInt64::create();

    for (size_t i=0; i<table_ids.size(); i++)
    {
        if (table_ids[i]->database() == "cnch_system" || table_ids[i]->database() == "system")
            continue;

        database_column_mut->insert(table_ids[i]->database());
        table_column_mut->insert(table_ids[i]->name());
        index_column_mut->insert(i);
    }

    block_to_filter.insert(ColumnWithTypeAndName(std::move(database_column_mut), std::make_shared<DataTypeString>(), "database"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(table_column_mut), std::make_shared<DataTypeString>(), "table"));
    block_to_filter.insert(ColumnWithTypeAndName(std::move(index_column_mut), std::make_shared<DataTypeUInt64>(), "index"));

    Pipe pipe;

    /// Filter block with `database` and `table` column.
    VirtualColumnUtils::filterBlockWithQuery(query_info.query, block_to_filter, context);

    if (!block_to_filter.rows())
        return pipe;

    ColumnPtr filtered_index_column = block_to_filter.getByName("index").column;

    std::unordered_map<HostWithPorts, std::vector<std::shared_ptr<Protos::TableIdentifier>>, std::hash<HostWithPorts>, HostWithPorts::IsExactlySame> distribution;

    UInt64 ts = context->getTimestamp();
    for (size_t i=0; i<filtered_index_column->size(); i++)
    {
        auto current_table = table_ids[(*filtered_index_column)[i].get<UInt64>()];
        auto host_ports = context->getCnchTopologyMaster()->getTargetServer(current_table->uuid(), ts, true);
        /// TODO: If cannot determine target server for the table, should we return some info to user?
        if (host_ports.empty())
            continue;
        auto found = distribution.find(host_ports);
        if (found != distribution.end())
        {
            found->second.emplace_back(current_table);
        }
        else
        {
            std::vector<std::shared_ptr<Protos::TableIdentifier>> table_models{current_table};
            distribution.emplace(host_ports, table_models);
        }
    }

    std::vector<DB::Protos::DataModelTableInfo> all_table_info;

    for (auto it = distribution.begin(); it != distribution.end(); it++)
    {
        auto rpc_client = context->getCnchServerClientPool().get(it->first);
        auto table_infos = rpc_client->getTableInfo(it->second);

        for (auto & table_info : table_infos)
            all_table_info.emplace_back(table_info);
    }

    UInt64 current_timestamp = context->getTimestamp();


    BlockInputStreamPtr input = std::make_shared<CnchTableInfoBlockInputStream>(std::move(columns_mask), std::move(res_block), max_block_size,
        all_table_info, current_timestamp);
    ProcessorPtr source = std::make_shared<SourceFromInputStream>(input);
    pipe.addSource(source);

    return pipe;
}

}
