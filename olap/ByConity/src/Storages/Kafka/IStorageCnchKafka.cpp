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

#include <Storages/Kafka/IStorageCnchKafka.h>

#include <Common/Exception.h>
#include <Common/Macros.h>
#include <Common/Configurations.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeString.h>
#include <Interpreters/Context.h>
#include <Storages/Kafka/KafkaCommon.h>

#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/trim.hpp>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int EMPTY_LIST_OF_COLUMNS_PASSED;
}
IStorageCnchKafka::IStorageCnchKafka
    (const StorageID &table_id_,
     ContextMutablePtr context_,
     const ASTPtr setting_changes_,
     const KafkaSettings &settings_,
     const ColumnsDescription & columns_,
     const ConstraintsDescription & constraints_)
    : IStorage(table_id_),
      WithMutableContext(context_->getGlobalContext()),
      settings(settings_)
{
    StorageInMemoryMetadata storage_metadata;
    storage_metadata.setColumns(columns_);
    storage_metadata.setConstraints(constraints_);
    storage_metadata.setSettingsChanges(setting_changes_);
    setInMemoryMetadata(storage_metadata);

    checkAndLoadingSettings(settings);
}

NamesAndTypesList IStorageCnchKafka::getVirtuals() const
{
    return NamesAndTypesList{
            {"_content", std::make_shared<DataTypeString>()},
            {"_info", std::make_shared<DataTypeString>()},
            {"_key", std::make_shared<DataTypeString>()},
            {"_offset", std::make_shared<DataTypeUInt64>()},
            {"_partition", std::make_shared<DataTypeUInt64>()},
            {"_topic", std::make_shared<DataTypeString>()}
    };
}

void IStorageCnchKafka::checkAndLoadingSettings(KafkaSettings &kafka_settings)
{
    /// check cluster and broker list: one and only one is allowed
    if (!kafka_settings.broker_list.changed && !kafka_settings.cluster.changed)
        throw Exception("Required parameter kafka_broker_list or kafka_cluster", ErrorCodes::BAD_ARGUMENTS);
    else if (kafka_settings.broker_list.changed && kafka_settings.cluster.changed)
        throw Exception("Cannot use both kafka_broker_list and kafka_cluster", ErrorCodes::BAD_ARGUMENTS);
    kafka_settings.broker_list.value = getContext()->getMacros()->expand(kafka_settings.broker_list.value);

    /// check topic list: no duplicated topics are allowed
    topics.clear();
    String topic_list = kafka_settings.topic_list.value;
    boost::split(topics, topic_list, [](char c) { return c == ','; });
    for (String & topic : topics)
        boost::trim(topic);
    std::sort(topics.begin(), topics.end());
    if (auto it = std::unique(topics.begin(), topics.end()); it != topics.end())
        throw Exception("Duplicated kafka topics are not allowed" + *it, ErrorCodes::BAD_ARGUMENTS);
    topics = getContext()->getMacros()->expand(topics);

    /// process other parameters
    const auto & global_config = getContext()->getRootConfig();

    if (!kafka_settings.max_partition_fetch_bytes.changed)
        kafka_settings.max_partition_fetch_bytes.value = global_config.kafka_max_partition_fetch_bytes.safeGet();

    auto shard_count_str = getContext()->getMacros()->expand(settings.shard_count);
    shard_count = parse<Int64>(shard_count_str);
    /// XXX: shard_count is not used now, so don't need check validity

    /// multi version check for community kafka
    if (kafka_settings.api_version_request.value != "true" && kafka_settings.api_version_request.value != "false")
        throw Exception("api_version_request must be `true` or `false`", ErrorCodes::BAD_ARGUMENTS);

    static std::set<String> auto_offset_reset_options{"smallest", "earliest", "beginning", "largest", "latest", "end"};
    if (!kafka_settings.auto_offset_reset.value.empty() && !auto_offset_reset_options.count(kafka_settings.auto_offset_reset.value))
        throw Exception("Invalid value for auto_offset_reset", ErrorCodes::BAD_ARGUMENTS);

    /// check the validity of schema path: either remote hdfs path or local loaded path
    if (kafka_settings.format_schema_path.changed)
    {
        auto path = kafka_settings.format_schema_path.value;
        if (!path.empty() && !Kafka::startsWithHDFSOrCFS(path) && !Poco::File(path).exists())
            throw Exception("Invalid format schema path, it should be either hdfs or local loaded path", ErrorCodes::BAD_ARGUMENTS);
    }

    if (!useBytedanceKafka())
    {
        /// TODO: should we throw an Exception later to make a powerful restriction?
        if (kafka_settings.unique_group_prefix.value.empty())
            LOG_WARNING(&Poco::Logger::get("IStorageCnchKafka"), "No unique prefix set for tob kafka, which may cause duplicate keys for offset in bytekv");
    }
}

String IStorageCnchKafka::getGroupForBytekv() const
{
    if (useBytedanceKafka() || settings.unique_group_prefix.value.empty())
        return settings.group_name.value;

    return settings.unique_group_prefix.value + "_" + settings.group_name.value;
}

void IStorageCnchKafka::getKafkaTableInfo(KafkaTableInfo & table_info)
{
    table_info.database = getDatabaseName();
    table_info.table = getTableName();
    table_info.uuid = UUIDHelpers::UUIDToString(getStorageUUID());
    table_info.cluster = settings.cluster.value;
    table_info.topics = topics;
    table_info.consumer_group = settings.group_name.value;
}

}

#endif

