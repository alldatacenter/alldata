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

#include <Common/config.h>
#if USE_RDKAFKA

#include <Catalog/Catalog.h>
#include <CloudServices/CnchBGThreadsMap.h>
#include <Columns/ColumnString.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeArray.h>
#include <DataStreams/OneBlockInputStream.h>
#include <Interpreters/Context.h>
#include <Storages/Kafka/CnchKafkaConsumeManager.h>
#include <Storages/Kafka/KafkaCommon.h>
#include <Storages/Kafka/StorageCnchKafka.h>
#include <Storages/System/StorageSystemKafkaTables.h>
#include <Storages/System/CollectWhereClausePredicate.h>
#include <Storages/VirtualColumnUtils.h>
#include <Databases/IDatabase.h>
#include <Processors/Sources/SourceFromInputStream.h>

namespace DB
{

StorageSystemKafkaTables::StorageSystemKafkaTables(const StorageID & table_id_)
    : IStorage(table_id_)
{
    StorageInMemoryMetadata storage_metadata;
    storage_metadata.setColumns(ColumnsDescription({
        { "database",                   std::make_shared<DataTypeString>()  },
        { "name",                       std::make_shared<DataTypeString>()  },
        { "uuid",                       std::make_shared<DataTypeString>()  },
        { "kafka_cluster",              std::make_shared<DataTypeString>()  },
        { "topics",                     std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>()) },
        { "consumer_group",             std::make_shared<DataTypeString>()  },
        { "num_consumers",              std::make_shared<DataTypeUInt32>()  },
        { "consumer_tables",            std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "consumer_hosts",             std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "consumer_partitions",        std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "consumer_offsets",           std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "last_exception",             std::make_shared<DataTypeString>()  },

    }));
    setInMemoryMetadata(storage_metadata);
}

Pipe StorageSystemKafkaTables::read(
    const Names & /* column_names */,
    const StorageMetadataPtr & /*metadata_snapshot*/,
    SelectQueryInfo & query_info,
    ContextPtr context,
    QueryProcessingStage::Enum /*processed_stage*/,
    const size_t /*max_block_size*/,
    const unsigned /*num_streams*/)
{
    Catalog::CatalogPtr cnch_catalog = context->getCnchCatalog();
    if (context->getServerType() != ServerType::cnch_server || !cnch_catalog)
        throw Exception("Table system.kafka_tables is only supported on cnch_server side", ErrorCodes::NOT_IMPLEMENTED);

    auto bg_threads = context->getCnchBGThreadsMap(CnchBGThreadType::Consumer)->getAll();

    MutableColumnPtr col_database_mut = ColumnString::create();
    MutableColumnPtr col_table_mut = ColumnString::create();

    for (const auto & thread : bg_threads)
    {
        const auto & storage_id = thread.second->getStorageID();
        col_database_mut->insert(storage_id.database_name);
        col_table_mut->insert(storage_id.table_name);
    }

    ColumnPtr col_database(std::move(col_database_mut));
    ColumnPtr col_table(std::move(col_table_mut));

    /// Determine what tables are needed by the conditions in the query.
    {
        Block filtered_block
        {
            { col_database, std::make_shared<DataTypeString>(), "database" },
            { col_table, std::make_shared<DataTypeString>(), "name" },
        };

        VirtualColumnUtils::filterBlockWithQuery(query_info.query, filtered_block, context);

        if (!filtered_block.rows())
            return Pipe();

        col_database = filtered_block.getByName("database").column;
        col_table = filtered_block.getByName("name").column;
    }

    MutableColumns res_columns = getInMemoryMetadataPtr()->getSampleBlock().cloneEmptyColumns();

    for (size_t i = 0, size = col_database->size(); i < size; ++i)
    {
        String database = (*col_database)[i].safeGet<String>();
        String table = (*col_table)[i].safeGet<String>();
        auto storage = cnch_catalog->tryGetTable(*context, database, table, TxnTimestamp::maxTS());
        auto * kafka_table = dynamic_cast<StorageCnchKafka*>(storage.get());
        if (!kafka_table)
            continue;

        auto thread = bg_threads.find(kafka_table->getStorageUUID());
        if (thread == bg_threads.end())
            continue;

        auto * manager = dynamic_cast<CnchKafkaConsumeManager*>(thread->second.get());
        if (!manager)
            continue;

        KafkaTableInfo table_info;
        kafka_table->getKafkaTableInfo(table_info);
        const auto consumer_infos = manager->getConsumerInfos();

        size_t col_num = 0;
        res_columns[col_num++]->insert(table_info.database);
        res_columns[col_num++]->insert(table_info.table);
        res_columns[col_num++]->insert(table_info.uuid);
        res_columns[col_num++]->insert(table_info.cluster);

        Array topics;
        for (auto & topic : table_info.topics)
            topics.emplace_back(topic);
        res_columns[col_num++]->insert(topics);

        res_columns[col_num++]->insert(table_info.consumer_group);
        res_columns[col_num++]->insert(consumer_infos.size());

        /// if the table is not consuming, such as dependencies is not ready, just print common info
        if (consumer_infos.empty())
        {
            res_columns[col_num++]->insertDefault();
            res_columns[col_num++]->insertDefault();
            res_columns[col_num++]->insertDefault();
        }
        else
        {
            Array consumer_tables, consumer_hosts, consumer_assignments, offsets;
            for (const auto & consumer : consumer_infos)
            {
                consumer_tables.emplace_back(table_info.table + consumer.table_suffix);
                consumer_hosts.emplace_back(consumer.worker_client_info);
                consumer_assignments.emplace_back(DB::Kafka::toString(consumer.partitions));
            }
            res_columns[col_num++]->insert(consumer_tables);
            res_columns[col_num++]->insert(consumer_hosts);
            res_columns[col_num++]->insert(consumer_assignments);
        }

        /// TODO: Avoid access to Catalog if offsets are not required
        cppkafka::TopicPartitionList tpl;
        std::for_each(consumer_infos.begin(), consumer_infos.end(), [& tpl] (const auto & c)
        {
            tpl.insert(tpl.end(), c.partitions.begin(), c.partitions.end());
        });
        cnch_catalog->getKafkaOffsets(kafka_table->getGroupForBytekv(), tpl);
        res_columns[col_num++]->insert(Array{DB::Kafka::toString(tpl)});

        res_columns[col_num++]->insert(manager->getLastException());
    }

    Block res = getInMemoryMetadataPtr()->getSampleBlock().cloneWithoutColumns();
    if (!res_columns.empty())
    {
        size_t col_num = 0;
        size_t num_columns = res.columns();
        while (col_num < num_columns)
        {
            res.getByPosition(col_num).column = std::move(res_columns[col_num]);
            ++col_num;
        }
    }

    return Pipe(std::make_shared<SourceFromInputStream>(std::make_shared<OneBlockInputStream>(res)));
}

} // end of namespace DB

#endif
