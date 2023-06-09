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

#include <fmt/core.h>
#include <Poco/DateTime.h>
#include <Poco/DateTimeFormatter.h>
#include <Common/Brpc/BrpcApplication.h>
#include <Common/Brpc/BrpcChannelPoolConfigHolder.h>
#include <Common/Brpc/BrpcGflagsConfigHolder.h>
#include <Common/Brpc/BrpcPocoLogSink.h>

namespace DB
{
BrpcApplication::BrpcApplication()
{
    logger = &Poco::Logger::get("BrpcApplication");

    //Init Brpc log
    initBrpcLog();

    //Init ConfigHolders
    initBuildinConfigHolders();
}

BrpcApplication::~BrpcApplication()
{
    delete ::logging::SetLogSink(old_sink);
    std::lock_guard<std::mutex> guard(holder_map_mutex);
    config_holder_map.clear();
}

BrpcApplication & BrpcApplication::getInstance()
{
    static BrpcApplication singleton;
    return singleton;
}

void BrpcApplication::initBrpcLog()
{
    int bprc_log_priority = poco2BrpcLogPriority(logger->getLevel());
    ::logging::SetMinLogLevel(bprc_log_priority);
    old_sink = ::logging::SetLogSink(new BrpcPocoLogSink());
}


void BrpcApplication::initialize(const RawConfig & app_conf)
{
    RawConfAutoPtr brpc_config = const_cast<RawConfig *>(app_conf.createView(BrpcApplication::prefix));
    auto holder_map = snapshotConfigHolderMap();

    for (const auto & item : holder_map)
    {
        item.second->init(brpc_config);
    }
    std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i")
              << " <Info> BrpcApplication::initialize Brpc is initialized with {} config holders" << holder_map.size() << " config holders"
              << std::endl;
}

BrpcApplication::ConfigHolderMap BrpcApplication::snapshotConfigHolderMap()
{
    ConfigHolderMap holder_map;
    std::lock_guard<std::mutex> guard(holder_map_mutex);
    for (const auto & item : config_holder_map)
    {
        holder_map[item.first] = item.second;
    }
    return holder_map;
}


void BrpcApplication::reloadConfig(const RawConfig & app_conf)
{
    RawConfAutoPtr brpc_config = const_cast<RawConfig *>(app_conf.createView(BrpcApplication::prefix));

    auto holders_snapshot = snapshotConfigHolderMap();


    for (const auto & item : holders_snapshot)
    {
        item.second->reload(brpc_config);
    }
    std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i")
              << fmt::format(" <Info> Reload brpc config with {} config holders", holders_snapshot.size()) << std::endl;
}

void BrpcApplication::initBuildinConfigHolders()
{
    registerNamedConfigHolder(std::make_shared<BrpcGflagsConfigHolder>());
    registerNamedConfigHolder(std::make_shared<BrpcChannelPoolConfigHolder>());
}


}
