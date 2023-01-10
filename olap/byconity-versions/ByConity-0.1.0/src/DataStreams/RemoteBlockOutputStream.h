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

#include <Core/Block.h>
#include <DataStreams/IBlockOutputStream.h>
#include <Common/Throttler.h>
#include <IO/ConnectionTimeouts.h>
#include <Interpreters/ClientInfo.h>
#include <Interpreters/Context.h>

namespace DB
{

class Connection;
class ReadBuffer;
struct Settings;


/** Allow to execute INSERT query on remote server and send data for it.
  */
class RemoteBlockOutputStream : public IBlockOutputStream
{
public:
    RemoteBlockOutputStream(Connection & connection_,
                            const ConnectionTimeouts & timeouts,
                            const String & query_,
                            const Settings & settings_,
                            const ClientInfo & client_info_,
                            ContextPtr context_);

    Block getHeader() const override { return header; }

    void write(const Block & block) override;
    void writeSuffix() override;

    /// Send pre-serialized and possibly pre-compressed block of data, that will be read from 'input'.
    void writePrepared(ReadBuffer & input, size_t size = 0);

    ~RemoteBlockOutputStream() override;

    void parseQueryWorkerMetrics(const QueryWorkerMetricElements & elements);

private:
    Connection & connection;
    String query;
    Block header;
    bool finished = false;
    ContextPtr context;
};

}
