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

#include <sstream>
#include <Storages/Kafka/KafkaCommon.h>

#include <IO/Operators.h>
#include <Interpreters/StorageID.h>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <cppkafka/cppkafka.h>
#include <Poco/Util/AbstractConfiguration.h>

#include <cppkafka/topic_partition_list.h>
#include <rapidjson/document.h>
#include <rapidjson/error/en.h>

namespace DB::ErrorCodes {
    extern const int BAD_ARGUMENTS;
}

namespace DB::Kafka
{
/// Configuration prefix
const String CONFIG_PREFIX = "kafka";
const String BYTEDANCE_CONFIG_PREFIX = "bytedance_kafka";

void loadFromConfig(
    cppkafka::Configuration & conf, const Poco::Util::AbstractConfiguration & config, const String & path, bool replace_underline = true)
{
    Poco::Util::AbstractConfiguration::Keys keys;
    config.keys(path, keys);

    for (const auto & key : keys)
    {
        const String key_path = path + "." + key;
        const String key_name = replace_underline ? boost::replace_all_copy(key, "_", ".") : key;
        conf.set(key_name, config.getString(key_path));
    }
}

cppkafka::Configuration createConsumerConfiguration(
     ContextPtr context, const StorageID & storage_id, const Names & topics, const KafkaSettings & settings)
{
    cppkafka::Configuration conf;
    const auto log = &Poco::Logger::get(storage_id.getNameForLogs());

    /// 1) set from global configuration

    /// Update consumer configuration from the configuration
    const auto & config = context->getConfigRef();
    if (config.has(CONFIG_PREFIX))
        loadFromConfig(conf, config, CONFIG_PREFIX);

    /// Update consumer topic-specific configuration
    for (const auto & topic : topics)
    {
        const auto topic_config_key = CONFIG_PREFIX + "_" + topic;
        if (config.has(topic_config_key))
            loadFromConfig(conf, config, topic_config_key);
    }

    /// Update BYTEDANCE_CONFIG
    const auto & cluster = settings.cluster.value;
    if (!cluster.empty())
    {
        if (config.has(BYTEDANCE_CONFIG_PREFIX))
            loadFromConfig(conf, config, BYTEDANCE_CONFIG_PREFIX, false);
        for (const auto & topic : topics)
        {
            const auto bytedance_topic_config_key = BYTEDANCE_CONFIG_PREFIX + "_" + topic;
            if (config.has(bytedance_topic_config_key))
                loadFromConfig(conf, config, bytedance_topic_config_key, false);
        }
    }

    /// 2) load from extra_librdkafka_config in JSON format
    if (const auto & extra_config = settings.extra_librdkafka_config.value; !extra_config.empty())
    {
        using namespace rapidjson;
        Document document;
        ParseResult ok = document.Parse(extra_config.c_str());
        if (!ok)
            throw Exception(
                    String("JSON parse error ") + GetParseError_En(ok.Code()) + " " + DB::toString(ok.Offset()), ErrorCodes::BAD_ARGUMENTS);

        for (auto & member : document.GetObject())
        {
            if (!member.value.IsString())
                continue;
            auto && key = member.name.GetString();
            auto && value = member.value.GetString();
            LOG_TRACE(log, "[extra_config] {}:{}", key, value);
            conf.set(key, value);
        }
    }

    /// 3) apply table-level settings
    const auto & brokers = settings.broker_list.value;
    if (!brokers.empty())
    {
        LOG_TRACE(log, "Setting brokers: {}", brokers);
        conf.set("metadata.broker.list", brokers);
    }
    else
    {
        LOG_TRACE(log, "Setting cluster: {}", cluster);
        conf.set("cluster", cluster);

        std::ostringstream oss;
        std::copy(topics.begin(), topics.end(), std::ostream_iterator<std::string>(oss, ","));
        auto topics_text = oss.str();
        LOG_TRACE(log, "Setting topics: {}", topics_text);
        conf.set("topics", topics_text);
    }

    const auto & group = settings.group_name.value;
    auto client_id = storage_id.getDatabaseName() + '.' + storage_id.getTableName();
    LOG_TRACE(log, "Setting Group ID: {} Client ID: {}", group, client_id);
    conf.set("group.id", group);
    conf.set("client.id", client_id);
    conf.set("max.partition.fetch.bytes", std::to_string(settings.max_partition_fetch_bytes));

    conf.set("enable.auto.commit", "false");

    conf.set("enable.partition.eof", "false");

    conf.set("api.version.request", settings.api_version_request);
    if (!settings.broker_version_fallback.value.empty())
        conf.set("broker.version.fallback", settings.broker_version_fallback);

    if (!settings.auto_offset_reset.value.empty())
        conf.set("auto.offset.reset", settings.auto_offset_reset);

    conf.set_log_callback([log] (cppkafka::KafkaHandleBase & handle, int level,
                                  const std::string & facility, const std::string & message)
                          {
                            LOG_TRACE(log, "RDKAFKA|{}|{}|{}|{}", level, facility, handle.get_handle() ? handle.get_name() : "", message);
                          });

    return conf;
}

String toString(const cppkafka::TopicPartitionList & tpl)
{
    WriteBufferFromOwnString oss;
    oss << "[ ";
    for (auto iter = tpl.begin(); iter != tpl.end(); ++iter)
    {
        if (iter != tpl.begin())
            oss << ", ";

        oss << iter->get_topic() << "["
            << iter->get_partition() << ":"
            << (iter->get_offset() == RD_KAFKA_OFFSET_INVALID ? "#" : std::to_string(iter->get_offset()))
            << "]";
    }
    oss << " ]";

    return oss.str();
}

}
