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

#include <IO/ReadBuffer.h>
#include <IO/ReadBufferFromString.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBuffer.h>
#include <IO/WriteBufferFromString.h>
#include <IO/WriteHelpers.h>
#include <Common/ActionBlocker.h>
#include <common/types.h>

#include <atomic>
#include <map>
#include <shared_mutex>
#include <utility>

namespace zkutil
{
    class ZooKeeper;
    using ZooKeeperPtr = std::shared_ptr<ZooKeeper>;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int DUPLICATE_INTERSERVER_IO_ENDPOINT;
    extern const int NO_SUCH_INTERSERVER_IO_ENDPOINT;
}

class HTMLForm;
class HTTPServerResponse;

/** Query processor from other servers.
  */
class InterserverIOEndpoint
{
public:
    virtual std::string getId(const std::string & path) const = 0;
    virtual void processQuery(const HTMLForm & params, ReadBuffer & body, WriteBuffer & out, HTTPServerResponse & response) = 0;
    virtual ~InterserverIOEndpoint() = default;

    /// You need to stop the data transfer if blocker is activated.
    ActionBlocker blocker;
    std::shared_mutex rwlock;
};

using InterserverIOEndpointPtr = std::shared_ptr<InterserverIOEndpoint>;


/** Here you can register a service that processes requests from other servers.
  * Used to transfer chunks in ReplicatedMergeTree.
  */
class InterserverIOHandler
{
public:
    void addEndpoint(const String & name, InterserverIOEndpointPtr endpoint)
    {
        std::lock_guard lock(mutex);
        bool inserted = endpoint_map.try_emplace(name, std::move(endpoint)).second;
        if (!inserted)
            throw Exception("Duplicate interserver IO endpoint: " + name, ErrorCodes::DUPLICATE_INTERSERVER_IO_ENDPOINT);
    }

    bool removeEndpointIfExists(const String & name)
    {
        std::lock_guard lock(mutex);
        return endpoint_map.erase(name);
    }

    InterserverIOEndpointPtr getEndpoint(const String & name)
    try
    {
        std::lock_guard lock(mutex);
        return endpoint_map.at(name);
    }
    catch (...)
    {
        throw Exception("No interserver IO endpoint named " + name, ErrorCodes::NO_SUCH_INTERSERVER_IO_ENDPOINT);
    }

private:
    using EndpointMap = std::map<String, InterserverIOEndpointPtr>;

    EndpointMap endpoint_map;
    std::mutex mutex;
};

}
