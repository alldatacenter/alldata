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

#include <iomanip>
#include <memory>
#include <brpc/channel.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <Common/Brpc/BaseConfigHolder.h>
#include <Common/Brpc/BrpcChannelPoolConfigHolder.h>
#include <Common/Brpc/BrpcChannelPoolOptions.h>

namespace DB
{
std::ostream & operator<<(std::ostream & os, const BrpcChannelPoolConfigHolder::PoolOptionsMap & pool_options_map)
{
    for (const auto & pair : pool_options_map)
    {
        os << pair.second << "\n";
    }
    return os;
}

void BrpcChannelPoolConfigHolder::afterInit(const PoolOptionsMap * conf_ptr)
{
    std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Info> "
              << "BrpcChannelPoolConfigHolder::afterinit Init brpc client channel pool with config: " << *conf_ptr << std::endl;
}

std::unique_ptr<BrpcChannelPoolConfigHolder::PoolOptionsMap>
BrpcChannelPoolConfigHolder::createTypedConfig(RawConfAutoPtr conf_ptr) noexcept
{
    auto option_unique_ptr = std::make_unique<PoolOptionsMap>();
    Poco::Util::AbstractConfiguration::Keys pool_name_keys;
    conf_ptr->keys(name, pool_name_keys);

    const BrpcChannelPoolOptions::PoolOptions * rpc_default_pool_options = BrpcChannelPoolOptions::getInstance().getDefaultPoolOptions("rpc_default");

    for (auto & pool_name : pool_name_keys)
    {
        auto pair = option_unique_ptr->find(pool_name);
        if (likely(pair == option_unique_ptr->end()))
        {
            const BrpcChannelPoolOptions::PoolOptions * default_options = BrpcChannelPoolOptions::getInstance().getDefaultPoolOptions(pool_name);
            if (!default_options)
            {
                default_options = rpc_default_pool_options;
            }
            RawConfAutoPtr pool_options_conf_ptr = conf_ptr->createView(pool_name);
            auto & options = (*option_unique_ptr)[pool_name];
            options.pool_name = pool_name;
            auto tag_prefix = "channel_pool." + pool_name + ".";
            fillWithConfig(options, *default_options, pool_options_conf_ptr, tag_prefix);
        }
        else
        {
            std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Warning> "
                      << "BrpcChannelPoolConfigHolder::createTypedConfig "
                      << "Duplicate properties in config <brpc><pool_options></pool_options></brpc>. "
                      << "Configs except the first one are omitted.";
        }
    }
    // make sure that a rpc_default config is in pool_options_map
    BrpcChannelPoolOptions::PoolOptions rpc_default_options = *rpc_default_pool_options; // copy
    rpc_default_options.pool_name = BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY;
    option_unique_ptr->emplace(BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY, std::move(rpc_default_options));

    // make sure that a stream_default config is in pool_options_map
    BrpcChannelPoolOptions::PoolOptions stream_default_options = *BrpcChannelPoolOptions::getInstance().getDefaultPoolOptions("stream_default");
    stream_default_options.pool_name = BrpcChannelPoolOptions::STREAM_DEFAULT_CONFIG_KEY;
    option_unique_ptr->emplace(BrpcChannelPoolOptions::STREAM_DEFAULT_CONFIG_KEY, std::move(stream_default_options));
    return option_unique_ptr;
}

void BrpcChannelPoolConfigHolder::fillWithConfig(
    BrpcChannelPoolOptions::PoolOptions & options,
    const BrpcChannelPoolOptions::PoolOptions & default_options,
    RawConfAutoPtr & pool_options_conf_ptr,
    const std::string & tag_prefix)
{
    // Poco::Util::ConfigurationView
    // If a property is not found in the view, it is searched in the original configuration.
    options.max_connections = pool_options_conf_ptr->getUInt(tag_prefix + "max_connections", default_options.max_connections);
    options.load_balancer = pool_options_conf_ptr->getString(tag_prefix + "load_balancer", default_options.load_balancer);
    options.channel_options.connect_timeout_ms = pool_options_conf_ptr->getInt(
        tag_prefix + "channel_options.connect_timeout_ms", default_options.channel_options.connect_timeout_ms);
    options.channel_options.timeout_ms = pool_options_conf_ptr->getInt(
        tag_prefix + "channel_options.timeout_ms", default_options.channel_options.timeout_ms);
    options.channel_options.backup_request_ms = pool_options_conf_ptr->getInt(
        tag_prefix + "channel_options.backup_request_ms", default_options.channel_options.backup_request_ms);
    options.channel_options.max_retry = pool_options_conf_ptr->getInt(
        tag_prefix + "channel_options.max_retry", default_options.channel_options.max_retry);
    options.channel_options.enable_circuit_breaker = pool_options_conf_ptr->getBool(
        tag_prefix + "channel_options.enable_circuit_breaker", default_options.channel_options.enable_circuit_breaker);
    options.channel_options.connection_type = pool_options_conf_ptr->getString(
        tag_prefix + "channel_options.connection_type", "single");
    options.channel_options.log_succeed_without_server = pool_options_conf_ptr->getBool(
        tag_prefix + "channel_options.log_succeed_without_server", default_options.channel_options.log_succeed_without_server);
    options.channel_options.succeed_without_server = pool_options_conf_ptr->getBool(
        tag_prefix + "channel_options.succeed_without_server", default_options.channel_options.succeed_without_server);
}

void BrpcChannelPoolConfigHolder::onChange(const PoolOptionsMap * old_conf_ptr, const PoolOptionsMap * new_conf_ptr)
{
    std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Info> "
              << "BrpcChannelPoolConfigHolder::onChange Client Conection Pool config changed from\n"
              << *old_conf_ptr << "to\n"
              << *new_conf_ptr;
}

bool BrpcChannelPoolConfigHolder::hasChanged(const PoolOptionsMap * old_conf_ptr, const PoolOptionsMap * new_conf_ptr)
{
    if (!old_conf_ptr || old_conf_ptr->size() != new_conf_ptr->size())
    {
        return true;
    }
    else
    {
        for (const auto & pair : *old_conf_ptr)
        {
            auto got = new_conf_ptr->find(pair.first);
            if (got == new_conf_ptr->end() || got->second != pair.second)
            {
                return true;
            }
        }
    }
    return false;
}

}
