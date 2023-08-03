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

#include <memory>
#include <string>

namespace Poco::Net { class TCPServer; }

namespace DB
{
class GRPCServer;

/// Provides an unified interface to access a protocol implementing server
/// no matter what type it has (HTTPServer, TCPServer, MySQLServer, GRPCServer, ...).
class ProtocolServerAdapter
{
    friend class ProtocolServers;
public:
    ProtocolServerAdapter(ProtocolServerAdapter && src) = default;
    ProtocolServerAdapter & operator =(ProtocolServerAdapter && src) = default;
    ProtocolServerAdapter(const char * port_name_, const std::string & description_, std::unique_ptr<Poco::Net::TCPServer> tcp_server_);

#if USE_GRPC
    ProtocolServerAdapter(const char * port_name_, const std::string & description_, std::unique_ptr<GRPCServer> grpc_server_);
#endif

    /// Starts the server. A new thread will be created that waits for and accepts incoming connections.
    void start() { impl->start(); }

    /// Stops the server. No new connections will be accepted.
    void stop() { impl->stop(); }

    /// Returns the number of currently handled connections.
    size_t currentConnections() const { return impl->currentConnections(); }

    /// Returns the number of current threads.
    size_t currentThreads() const { return impl->currentThreads(); }

    const std::string & getPortName() const { return port_name; }

    const std::string & getDescription() const { return description; }

private:
    class Impl
    {
    public:
        virtual ~Impl() {}
        virtual void start() = 0;
        virtual void stop() = 0;
        virtual size_t currentConnections() const = 0;
        virtual size_t currentThreads() const = 0;
    };
    class TCPServerAdapterImpl;
    class GRPCServerAdapterImpl;

    std::string port_name;
    std::string description;
    std::unique_ptr<Impl> impl;
};

}
