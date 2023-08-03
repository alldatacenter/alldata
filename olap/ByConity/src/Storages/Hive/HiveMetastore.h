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

#pragma once

#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>
#include <Poco/Logger.h>
#include <Common/HostWithPorts.h>
#include <Common/PoolBase.h>
#include <Common/config.h>

#include <Storages/ColumnsDescription.h>
#include <Storages/Hive/HiveDataPart.h>
#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Storages/Hive/HivePartition.h>
#include <Storages/MergeTree/CnchHiveSettings.h>
#include <hivemetastore/ThriftHiveMetastore.h>
#include <hivemetastore/hive_metastore_types.h>

namespace DB
{
using HivePartitionPtr = std::shared_ptr<HivePartition>;
using HivePartitionVector = std::vector<HivePartitionPtr>;
using HivePartitionInfoPtr = std::shared_ptr<HivePartitionInfo>;
using HivePartInfoPtr = std::shared_ptr<HivePartInfo>;
using Names = std::vector<String>;
using HiveNameToType = std::unordered_map<String, String>;

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace Apache::Hadoop::Hive;

using ThriftHiveMetastoreClientBuilder = std::function<std::shared_ptr<Apache::Hadoop::Hive::ThriftHiveMetastoreClient>()>;

///
class ThriftHiveMetastoreClientPool : public PoolBase<Apache::Hadoop::Hive::ThriftHiveMetastoreClient>
{
public:
    using Object = Apache::Hadoop::Hive::ThriftHiveMetastoreClient;
    using ObjectPtr = std::shared_ptr<Object>;
    using Entry = PoolBase<Apache::Hadoop::Hive::ThriftHiveMetastoreClient>::Entry;
    explicit ThriftHiveMetastoreClientPool(ThriftHiveMetastoreClientBuilder builder_, int max_hive_metastore_client_connections);

protected:
    ObjectPtr allocObject() override { return builder(); }

    ThriftHiveMetastoreClientBuilder builder;
};

class HiveMetastoreClient
{
public:
    HiveMetastoreClient(ThriftHiveMetastoreClientBuilder builder_, const CnchHiveSettings & settings_)
        : client_pool(builder_, settings_.max_hive_metastore_client_connections), settings(settings_)
    {
    }
    ~HiveMetastoreClient() = default;

    String normalizeHdfsSchema(const String & path);
    String escapeHiveTablePrefix(const String & fullpath, const String & path);

    Strings getBucketColNames(const String & db_name, const String & table_name);
    Strings getSortColNames(const String & db_name, const String & table_name);
    void getTable(Table & table, const String & db_name, const String & table_name);

    //partition related insterface
    Strings getPartitionKeyList(const String & db_name, const String & table_name);
    Strings getPartitionValues(
        const StoragePtr & storage, const String & db_name, const String & table_name, const String & table_path, const String & name);
    HivePartitionVector getPartitionList(
        const StoragePtr & storage, const String & db_name, const String & table_name, const String & table_path, int16_t max_parts = -1);
    HivePartitionVector getPartitionsByFilter(
        const StoragePtr & storage,
        const String & db_name,
        const String & table_name,
        const String & table_path,
        const String & filter,
        int16_t max_parts = -1);

    //data parts related interface
    // MutableHiveDataPartsCNCHVector getDataPartsInPartition(
    //     const StoragePtr & storage,
    //     HivePartitionPtr & partition,
    //     const HDFSConnectionParams & hdfs_paras,
    //     const NamesAndTypesList & index_names_and_types,
    //     const std::set<Int64> & required_bucket_numbers = {});

    HiveDataPartsCNCHVector getDataPartsInPartition(
        const StoragePtr & storage,
        HivePartitionPtr & partition,
        const HDFSConnectionParams & hdfs_paras,
        const std::set<Int64> & required_bucket_numbers = {});

    //schema check
    void check(const ColumnsDescription & columns, const String & db_name, const String & table_name);

    //Stroage Format check
    void checkStorageFormat(const String & db_name, const String & table_name);

private:
    /// get part index
    bool getDataPartIndex(const String & part_name, Int64 & index);

    void tryCallHiveClient(std::function<void(ThriftHiveMetastoreClientPool::Entry &)> func);

    //Hive type convert
    Strings convertMapTypeToCnch(const String & hive_type);
    String convertArrayTypeToCnch(const String & hive_type);
    String convertStructTypeToCnch(const String & hive_type);
    size_t getVcharLength(const String & hive_type);
    size_t getCharLength(const String & hive_type);
    std::vector<UInt32> getDecimalPrecisionScale(const String & hive_type);

    HivePartitionVector getPartitionsFromMetastore(
        const StoragePtr & storage, const String & db_name, const String & table_name, const String & table_path, int16_t max_parts = -1);
    Strings getPartitionIDsFromMetastore(
        [[maybe_unused]] const StoragePtr & storage,
        const String & db_name,
        const String & table_name,
        const String & table_path,
        int16_t max_parts = -1);
    Strings getPartitionIDs(const StoragePtr & storage, const String & db_name, const String & table_name, const String & table_path);

    void getColumns(std::vector<FieldSchema> & result, const String & db_name, const String & table_name);
    bool checkColumnType(DataTypePtr & base, DataTypePtr & internal);
    DataTypePtr
    parseColumnType(const std::unordered_map<String, std::shared_ptr<IDataType>> & hive_type_to_internal_type, const String & hive_type);

    ThriftHiveMetastoreClientPool client_pool;
    const CnchHiveSettings settings;
    mutable std::mutex mutex;
    // Poco::Logger * log{};
};

using HiveMetastoreClientPtr = std::shared_ptr<HiveMetastoreClient>;
class HiveMetastoreClientFactory final : private boost::noncopyable
{
public:
    static HiveMetastoreClientFactory & instance();

    HiveMetastoreClientPtr getOrCreate(const String & name, const CnchHiveSettings & settings);

private:
    static std::shared_ptr<Apache::Hadoop::Hive::ThriftHiveMetastoreClient>
    createThriftHiveMetastoreClient(const String & name, const CnchHiveSettings & settings);

    /// mutex to protect clients structure
    std::mutex mutex;
    std::map<String, HiveMetastoreClientPtr> clients;
};

}
