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

#include <Server/IServer.h>
#include <daemon/BaseDaemon.h>

namespace Poco
{
    namespace Net
    {
        class ServerSocket;
    }
}

namespace DB
{

/// standalone clickhouse-keeper server (replacement for ZooKeeper). Uses the same
/// config as clickhouse-server. Serves requests on TCP ports with or without
/// SSL using ZooKeeper protocol.
class Keeper : public BaseDaemon, public IServer
{
public:
    using ServerApplication::run;

    Poco::Util::LayeredConfiguration & config() const override
    {
        return BaseDaemon::config();
    }

    Poco::Logger & logger() const override
    {
        return BaseDaemon::logger();
    }

    ContextMutablePtr context() const override
    {
        return global_context;
    }

    bool isCancelled() const override
    {
        return BaseDaemon::isCancelled();
    }

    void defineOptions(Poco::Util::OptionSet & _options) override;

protected:
    void logRevision() const override;

    void handleCustomArguments(const std::string & arg, const std::string & value);

    int run() override;

    void initialize(Application & self) override;

    void uninitialize() override;

    int main(const std::vector<std::string> & args) override;

    std::string getDefaultConfigFileName() const override;

private:
    ContextMutablePtr global_context;

    Poco::Net::SocketAddress socketBindListen(Poco::Net::ServerSocket & socket, const std::string & host, UInt16 port, [[maybe_unused]] bool secure = false) const;

    using CreateServerFunc = std::function<void(UInt16)>;
    void createServer(const std::string & listen_host, const char * port_name, bool listen_try, CreateServerFunc && func) const;
};

}
