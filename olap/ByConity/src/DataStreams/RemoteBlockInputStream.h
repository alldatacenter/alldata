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

#include <optional>

#include <common/logger_useful.h>

#include <DataStreams/IBlockInputStream.h>
#include <Common/Throttler.h>
#include <Client/ConnectionPool.h>
#include <Client/MultiplexedConnections.h>
#include <Interpreters/Cluster.h>

#include <DataStreams/RemoteQueryExecutor.h>

namespace DB
{

class Context;

/** This class allows one to launch queries on remote replicas of one shard and get results
  */
class RemoteBlockInputStream : public IBlockInputStream
{
public:
    /// Takes already set connection.
    RemoteBlockInputStream(
            Connection & connection,
            const String & query_, const Block & header_, ContextPtr context_,
            const ThrottlerPtr & throttler = nullptr, const Scalars & scalars_ = Scalars(), const Tables & external_tables_ = Tables(),
            QueryProcessingStage::Enum stage_ = QueryProcessingStage::Complete);

    /// Accepts several connections already taken from pool.
    RemoteBlockInputStream(
            std::vector<IConnectionPool::Entry> && connections,
            const String & query_, const Block & header_, ContextPtr context_,
            const ThrottlerPtr & throttler = nullptr, const Scalars & scalars_ = Scalars(), const Tables & external_tables_ = Tables(),
            QueryProcessingStage::Enum stage_ = QueryProcessingStage::Complete);

    /// Takes a pool and gets one or several connections from it.
    RemoteBlockInputStream(
            const ConnectionPoolWithFailoverPtr & pool,
            const String & query_, const Block & header_, ContextPtr context_,
            const ThrottlerPtr & throttler = nullptr, const Scalars & scalars_ = Scalars(), const Tables & external_tables_ = Tables(),
            QueryProcessingStage::Enum stage_ = QueryProcessingStage::Complete);

    /// Set the query_id. For now, used by performance test to later find the query
    /// in the server query_log. Must be called before sending the query to the server.
    void setQueryId(const std::string & query_id) { query_executor.setQueryId(query_id); }

    /// Specify how we allocate connections on a shard.
    void setPoolMode(PoolMode pool_mode) { query_executor.setPoolMode(pool_mode); }

    void setMainTable(StorageID main_table_) { query_executor.setMainTable(std::move(main_table_)); }

    /// Sends query (initiates calculation) before read()
    void readPrefix() override;

    /// Prevent default progress notification because progress' callback is called by its own.
    void progress(const Progress & /*value*/) override {}

    void cancel(bool kill) override;

    String getName() const override { return "Remote"; }

    Block getHeader() const override { return query_executor.getHeader(); }
    Block getTotals() override { return query_executor.getTotals(); }
    Block getExtremes() override { return query_executor.getExtremes(); }

    const ExtendedProfileInfo & getExtendedProfileInfo() const { return query_executor.getExtendedProfileInfo(); }

protected:
    Block readImpl() override;
    void readSuffixImpl() override;

private:
    RemoteQueryExecutor query_executor;
    Poco::Logger * log = &Poco::Logger::get("RemoteBlockInputStream");

    void init();
};

}
