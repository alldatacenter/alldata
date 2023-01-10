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

#include <Client/ConnectionPool.h>
#include <Interpreters/Cluster.h>
#include <Parsers/IAST.h>

namespace DB
{

struct Settings;
class Cluster;
class Throttler;
struct SelectQueryInfo;

class Pipe;
using Pipes = std::vector<Pipe>;

class QueryPlan;
using QueryPlanPtr = std::unique_ptr<QueryPlan>;

namespace ClusterProxy
{

/// Base class for the implementation of the details of distributed query
/// execution that are specific to the query type.
class IStreamFactory
{
public:
    virtual ~IStreamFactory() = default;

    virtual void createForShard(
            const Cluster::ShardInfo & shard_info,
            const ASTPtr & query_ast,
            ContextPtr context, const ThrottlerPtr & throttler,
            std::vector<QueryPlanPtr> & res,
            Pipes & remote_pipes,
            Pipes & delayed_pipes,
            Poco::Logger * log) = 0;
};

}

}
