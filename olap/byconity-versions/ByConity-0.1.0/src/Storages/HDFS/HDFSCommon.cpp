/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Storages/HDFS/HDFSCommon.h>
#include <Poco/URI.h>
#include <boost/algorithm/string/replace.hpp>

#if USE_HDFS
#include <Common/ShellCommand.h>
#include <Common/Exception.h>
#include <Common/formatIPv6.h>
#include <random>
#include <IO/WriteBufferFromString.h>
#include <IO/Operators.h>
#include <common/logger_useful.h>
#include <ServiceDiscovery/ServiceDiscoveryFactory.h>
#include <ServiceDiscovery/ServiceDiscoveryConsul.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int NETWORK_ERROR;
    extern const int EXCESSIVE_ELEMENT_IN_CONFIG;
    extern const int NO_ELEMENTS_IN_CONFIG;
}

const String HDFSBuilderWrapper::CONFIG_PREFIX = "hdfs";

void HDFSBuilderWrapper::loadFromConfig(const Poco::Util::AbstractConfiguration & config,
    const String & config_path, bool isUser)
{
    Poco::Util::AbstractConfiguration::Keys keys;

    config.keys(config_path, keys);
    for (const auto & key : keys)
    {
        const String key_path = config_path + "." + key;

        String key_name;
        if (key == "hadoop_kerberos_keytab")
        {
            need_kinit = true;
            hadoop_kerberos_keytab = config.getString(key_path);
            continue;
        }
        else if (key == "hadoop_kerberos_principal")
        {
            need_kinit = true;
            hadoop_kerberos_principal = config.getString(key_path);

// #if USE_INTERNAL_HDFS3_LIBRARY
//             hdfsBuilderSetPrincipal(hdfs_builder, hadoop_kerberos_principal.c_str());
// #endif

            continue;
        }
        else if (key == "hadoop_kerberos_kinit_command")
        {
            need_kinit = true;
            hadoop_kerberos_kinit_command = config.getString(key_path);
            continue;
        }
        else if (key == "hadoop_security_kerberos_ticket_cache_path")
        {
            if (isUser)
            {
                throw Exception("hadoop.security.kerberos.ticket.cache.path cannot be set per user",
                    ErrorCodes::EXCESSIVE_ELEMENT_IN_CONFIG);
            }

            hadoop_security_kerberos_ticket_cache_path = config.getString(key_path);
            // standard param - pass further
        }

        key_name = boost::replace_all_copy(key, "_", ".");

        const auto & [k,v] = keep(key_name, config.getString(key_path));
        hdfsBuilderConfSetStr(get(), k.c_str(), v.c_str());
    }
}

String HDFSBuilderWrapper::getKinitCmd()
{

    if (hadoop_kerberos_keytab.empty() || hadoop_kerberos_principal.empty())
    {
        throw Exception("Not enough parameters to run kinit",
            ErrorCodes::NO_ELEMENTS_IN_CONFIG);
    }

    WriteBufferFromOwnString ss;

    String cache_name =  hadoop_security_kerberos_ticket_cache_path.empty() ?
        String() :
        (String(" -c \"") + hadoop_security_kerberos_ticket_cache_path + "\"");

    // command to run looks like
    // kinit -R -t /keytab_dir/clickhouse.keytab -k somebody@TEST.CLICKHOUSE.TECH || ..
    ss << hadoop_kerberos_kinit_command << cache_name <<
        " -R -t \"" << hadoop_kerberos_keytab << "\" -k " << hadoop_kerberos_principal <<
        "|| " << hadoop_kerberos_kinit_command << cache_name << " -t \"" <<
        hadoop_kerberos_keytab << "\" -k " << hadoop_kerberos_principal;
    return ss.str();
}

void HDFSBuilderWrapper::runKinit()
{
    String cmd = getKinitCmd();
    LOG_DEBUG(&Poco::Logger::get("HDFSClient"), "running kinit: {}", cmd);

    std::unique_lock<std::mutex> lck(kinit_mtx);

    auto command = ShellCommand::execute(cmd);
    auto status = command->tryWait();
    if (status)
    {
        throw Exception("kinit failure: " + cmd, ErrorCodes::BAD_ARGUMENTS);
    }
}

HDFSBuilderWrapper createHDFSBuilder(const String & uri_str, const Poco::Util::AbstractConfiguration & config)
{

    HDFSConnectionParams hdfs_params = HDFSConnectionParams::parseHdfsFromConfig(config);
    const Poco::URI uri(uri_str);
    // const auto & host = uri.getHost();
    // auto port = uri.getPort();
    // const String path = "//";
    // if (host.empty())
    //     throw Exception("Illegal HDFS URI: " + uri.toString(), ErrorCodes::BAD_ARGUMENTS);

    HDFSBuilderWrapper builder(hdfs_params.createBuilder(uri));
    if (builder.get() == nullptr)
        throw Exception("Unable to create builder to connect to HDFS: " +
            uri.toString() + " " + String(hdfsGetLastError()),
            ErrorCodes::NETWORK_ERROR);

    // hdfsBuilderConfSetStr(builder.get(), "input.read.timeout", "60000"); // 1 min
    // hdfsBuilderConfSetStr(builder.get(), "input.write.timeout", "60000"); // 1 min
    // hdfsBuilderConfSetStr(builder.get(), "input.connect.timeout", "60000"); // 1 min

    String user_info = uri.getUserInfo();
    String user;
    if (!user_info.empty() && user_info.front() != ':')
    {
        size_t delim_pos = user_info.find(':');
        if (delim_pos != String::npos)
            user = user_info.substr(0, delim_pos);
        else
            user = user_info;

        hdfsBuilderSetUserName(builder.get(), user.c_str());
    }

    // The namenode is already set in hdfs_params.createBuilder();
    // hdfsBuilderSetNameNode(builder.get(), host.c_str());
    // if (port != 0)
    // {
    //     hdfsBuilderSetNameNodePort(builder.get(), port);
    // }

    if (config.has(HDFSBuilderWrapper::CONFIG_PREFIX))
    {
        builder.loadFromConfig(config, HDFSBuilderWrapper::CONFIG_PREFIX);
    }

    if (!user.empty())
    {
        String user_config_prefix = HDFSBuilderWrapper::CONFIG_PREFIX + "_" + user;
        if (config.has(user_config_prefix))
        {
#if USE_INTERNAL_HDFS3_LIBRARY
            builder.loadFromConfig(config, user_config_prefix, true);
#else
            throw Exception("Multi user HDFS configuration required internal libhdfs3",
                ErrorCodes::EXCESSIVE_ELEMENT_IN_CONFIG);
#endif
        }
    }

    if (builder.need_kinit)
    {
        builder.runKinit();
    }

    return builder;
}

std::mutex HDFSBuilderWrapper::kinit_mtx;

HDFSFSPtr createHDFSFS(hdfsBuilder * builder)
{
    HDFSFSPtr fs(hdfsBuilderConnect(builder), detail::HDFSFsDeleter());
    if (fs == nullptr)
        throw Exception("Unable to connect to HDFS: " + String(hdfsGetLastError()),
            ErrorCodes::NETWORK_ERROR);

    return fs;
}

std::pair<std::string, size_t> getNameNodeNNProxy(const std::string & nnproxy)
{
    HostWithPortsVec nnproxys;
    auto sd_consul = ServiceDiscoveryFactory::instance().tryGet(ServiceDiscoveryMode::CONSUL);
    if (sd_consul)
    {
        int retry = 0;
        do
        {
            if (retry++ > 2)
                throw Exception("No available nnproxy " + nnproxy, ErrorCodes::NETWORK_ERROR);
            nnproxys = sd_consul->lookup(nnproxy, ComponentType::NNPROXY);
        } while (nnproxys.empty());
    }

    HostWithPortsVec sample;
    int num_retry = 3;
    while (true)
    {
        std::sample(nnproxys.begin(), nnproxys.end(), std::back_inserter(sample), 1, std::mt19937{std::random_device{}()});
        if (!sample.empty() && num_retry-- > 0)
        {
            if (brokenNNs.isBrokenNN(sample[0].getHost())) continue;
        }
        break;
    }
    std::string host{};
    size_t port{};
    for (const auto & service : sample)
    {
        host = service.getHost();
        port = service.tcp_port;
    }

    host = normalizeHost(host);

    return {host, port};
};

HDFSBuilderPtr createHDFSBuilder(const Poco::URI & uri, const std::string hdfs_user, const std::string nnproxy)
{
    auto host = uri.getHost();
    auto port = uri.getPort();

    if (host.empty() || port == 0)
    {
        std::tie(host, port) = getNameNodeNNProxy(nnproxy);
    }
    else
    {
        host = normalizeHost(host);
    }

    HDFSBuilderPtr builder(hdfsNewBuilder());
    if (builder == nullptr)
        throw Exception("Unable to create builder to connect to HDFS: " + uri.toString() + " " + std::string(hdfsGetLastError()),
            ErrorCodes::NETWORK_ERROR);
    hdfsBuilderConfSetStr(builder.get(), "input.read.timeout", "60000"); // 1 min
    hdfsBuilderConfSetStr(builder.get(), "input.write.timeout", "60000"); // 1 min
    hdfsBuilderConfSetStr(builder.get(), "input.connect.timeout", "60000"); // 1 min

    hdfsBuilderSetNameNode(builder.get(), host.c_str());
    hdfsBuilderSetNameNodePort(builder.get(), port);
    hdfsBuilderSetUserName(builder.get(), hdfs_user.c_str());
    return builder;
}


}

#endif
