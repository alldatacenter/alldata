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

#pragma once

#if !defined(ARCADIA_BUILD)
#include <Common/config.h>
#endif


// #if USE_HDFS
#include <memory>
#include <type_traits>
#include <vector>

#include <hdfs/hdfs.h> // Y_IGNORE
#include <common/types.h>
#include <mutex>
#include <unordered_map>
// #include <Interpreters/Context.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <Poco/URI.h>
#include <random>
#include <Common/HostWithPorts.h>
#include <Storages/HDFS/HDFSAuth.h>
namespace DB
{
inline bool isHdfsScheme(const std::string & scheme)
{
    return strcasecmp(scheme.c_str(), "hdfs") == 0;
}

inline bool isCfsScheme(const std::string & scheme)
{
    return strcasecmp(scheme.c_str(), "cfs") == 0;
}

inline bool isConsulScheme(const std::string & scheme)
{
    return scheme.empty() || strcasecmp(scheme.c_str(), "consul") == 0;
}

inline bool isHdfsOrCfsScheme(const std::string & scheme)
{
    return isHdfsScheme(scheme) || isCfsScheme(scheme);
}

inline void constructHDFSUri(const std::string & host, const std::string & defaultScheme, unsigned short port, Poco::URI * out)
{
    auto pos = host.find("://");
    if (pos != std::string::npos)
    {
        out->setScheme(host.substr(0, pos));
        out->setHost(host.substr(pos + 3));
    }
    else
    {
        out->setHost(host);
        out->setScheme(defaultScheme);
    }
    out->setPort(port);
}
namespace detail
{

    struct HDFSBuilderDeleter
    {
        void operator()(hdfsBuilder * builder_ptr)
        {
            hdfsFreeBuilder(builder_ptr);
        }
    };

    struct HDFSFsDeleter
    {
        void operator()(hdfsFS fs_ptr)
        {
            hdfsDisconnect(fs_ptr);
        }
    };
}


struct HDFSFileInfo
{
    hdfsFileInfo * file_info;
    int length;

    HDFSFileInfo() : file_info(nullptr) , length(0) {}

    HDFSFileInfo(const HDFSFileInfo & other) = delete;
    HDFSFileInfo(HDFSFileInfo && other) = default;
    HDFSFileInfo & operator=(const HDFSFileInfo & other) = delete;
    HDFSFileInfo & operator=(HDFSFileInfo && other) = default;

    ~HDFSFileInfo()
    {
        hdfsFreeFileInfo(file_info, length);
    }
};


using HDFSBuilderPtr = std::unique_ptr<hdfsBuilder, detail::HDFSBuilderDeleter>;
using HDFSFSPtr = std::shared_ptr<std::remove_pointer_t<hdfsFS>>;

class HDFSConnectionParams
{
public:
    enum HDFSConnectionType
    {
        CONN_DUMMY,
        CONN_HDFS,
        CONN_CFS,
        CONN_HA,
        CONN_NNPROXY
    };
    using IpWithPort = std::pair<String, int>;
    HDFSConnectionParams() ;
    HDFSConnectionParams(HDFSConnectionType t, const String & hdfs_user_, const String & hdfs_service_);
    HDFSConnectionParams(HDFSConnectionType t, const String & hdfs_user_, const std::vector<IpWithPort>& addrs_);


    HDFSConnectionType conn_type;
    String hdfs_user;
    String hdfs_service;
    std::vector<IpWithPort> addrs;
    HDFSKrb5Params krb5_params;

    bool use_nnproxy_ha = false; // for whether to use ha config for nnproxies.
    bool inited = false;
    size_t nnproxy_index = 0 ;

    HDFSBuilderPtr createBuilder(const Poco::URI & uri  ) const ;

    void setNNProxyHa(bool val ) {
        use_nnproxy_ha = val ;
    }

    void lookupOnNeed();
    void setNNProxyBroken();

    Poco::URI formatPath([[maybe_unused]] const String & path) const ;


    static HDFSConnectionParams parseHdfsFromConfig([[maybe_unused]]const  Poco::Util::AbstractConfiguration & config,
        const String& config_prefix = "");

    static HDFSConnectionParams parseFromMisusedNNProxyStr(String hdfs_nnproxy, String hdfs_user="clickhouse");

    static const HDFSConnectionParams & defaultNNProxy()
    {
        static HDFSConnectionParams params(CONN_NNPROXY, "clickhouse", "nnproxy");
        return params;
    }

    static const HDFSConnectionParams & emptyHost()
    {
        static HDFSConnectionParams params(CONN_HDFS, "clickhouse", {{"", 0}});
        return params;
    }

    String toString() const;

};

class HDFSBuilderWrapper
{

friend HDFSBuilderWrapper createHDFSBuilder(const String & uri_str, const Poco::Util::AbstractConfiguration &);

static const String CONFIG_PREFIX;

public:
    HDFSBuilderWrapper() : hdfs_builder(hdfsNewBuilder()) {}

    // need_kinit is always false in this constructor.
    HDFSBuilderWrapper(HDFSBuilderPtr builderPtr) : hdfs_builder(std::move(builderPtr)){}

    ~HDFSBuilderWrapper() {}

    HDFSBuilderWrapper(const HDFSBuilderWrapper &) = delete;
    HDFSBuilderWrapper(HDFSBuilderWrapper &&) = default;
    hdfsBuilder * get() { return hdfs_builder.get(); }

private:
    void loadFromConfig(const Poco::Util::AbstractConfiguration & config, const String & config_path, bool isUser = false);

    String getKinitCmd();

    void runKinit();

    // hdfs builder relies on an external config data storage
    std::pair<String, String>& keep(const String & k, const String & v)
    {
        return config_stor.emplace_back(std::make_pair(k, v));
    }

    // HDFSConnectionParams hdfs_params = HDFSConnectionParams::defaultNNProxy();
    HDFSBuilderPtr  hdfs_builder;
    String hadoop_kerberos_keytab;
    String hadoop_kerberos_principal;
    String hadoop_kerberos_kinit_command = "kinit";
    String hadoop_security_kerberos_ticket_cache_path;

    static std::mutex kinit_mtx;
    std::vector<std::pair<String, String>> config_stor;
    bool need_kinit{false};
};


template<typename T>
using fs_ptr = std::shared_ptr<T>;

/**
 * Bytedance hdfs related
 */
std::pair<std::string, size_t> getNameNodeNNProxy(const std::string & nnproxy);
HDFSBuilderPtr createHDFSBuilder(const Poco::URI & hdfs_uri, const std::string hdfs_user = "clickhouse", const std::string = "nnproxy");

/**
 * Set of know broken HDFS name node with ttl info
 */
class TTLBrokenNameNodes
{
public:
    using TTLNameNode = std::pair<std::string, time_t>;
    // default ttl is two hours
    TTLBrokenNameNodes(size_t ttl_ = 7200):ttl(ttl_), generator(rd()){}

    std::mutex nnMutex;

    void insert(const std::string& namenode)
    {
        std::unique_lock lock(nnMutex);
        time_t current_time = time(nullptr);
        // pop out node while ttl expired
        auto it = nns.find(namenode);
        if (it == nns.end())
        {
            nns.emplace(namenode, current_time);
        }
        else
        {
            it->second = current_time;
        }
    }

    bool isBrokenNN(const std::string& namenode)
    {
        std::unique_lock lock(nnMutex);
        time_t current_time = time(nullptr);
        return isBrokenNNInternal(namenode, current_time);
    }

    size_t findOneGoodNN(const HostWithPortsVec& hosts) {
        std::uniform_int_distribution<size_t> dist(0, hosts.size()-1);
        size_t start_point = dist(generator);
        size_t current_point = start_point;
        time_t current_time = time(nullptr);
        std::unique_lock lock(nnMutex);
        while(current_point != start_point) {
            if(isBrokenNNInternal(hosts[current_point].getHost(),current_time)) {
                current_point = (current_point + 1 ) % hosts.size();
            } else {
                break;
            }
        }
        return current_point;
    }
private:
    bool isBrokenNNInternal(const std::string& namenode, const time_t & current_time) {
        auto it = nns.find(namenode);
        if (it == nns.end()) return false;
        if (it->second + time_t(ttl) > current_time)
        {
            nns.erase(it);
            return false;
        }
        return true;
    }

    size_t ttl;
    std::unordered_map<std::string, time_t>  nns;
    std::random_device rd;
    std::mt19937 generator;
};

static TTLBrokenNameNodes brokenNNs; // NameNode blacklist

// set read/connect timeout, default value in libhdfs3 is about 1 hour, and too large
/// TODO Allow to tune from query Settings.
HDFSBuilderWrapper createHDFSBuilder(const String & uri_str, const Poco::Util::AbstractConfiguration &);
HDFSFSPtr createHDFSFS(hdfsBuilder * builder);

String getNameNodeUrl(const String & hdfs_url);
String getNameNodeCluster(const String & hdfs_url);

/// use default params or build from url
/// build params from custom url
HDFSConnectionParams hdfsParamsFromUrl(const Poco::URI & uri);

}
// #endif
