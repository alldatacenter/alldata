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

#include <Core/QueryProcessingStage.h>
#include <Interpreters/ClusterProxy/IStreamFactory.h>
#include <Interpreters/StorageID.h>
#include <Storages/IStorage_fwd.h>

namespace DB
{

namespace ClusterProxy
{

class SelectStreamFactory final : public IStreamFactory
{
public:
    /// Database in a query.
    SelectStreamFactory(
        const Block & header_,
        QueryProcessingStage::Enum processed_stage_,
        StorageID main_table_,
        const Scalars & scalars_,
        bool has_virtual_shard_num_column_,
        const Tables & external_tables);

    /// TableFunction in a query.
    SelectStreamFactory(
        const Block & header_,
        QueryProcessingStage::Enum processed_stage_,
        ASTPtr table_func_ptr_,
        const Scalars & scalars_,
        bool has_virtual_shard_num_column_,
        const Tables & external_tables_);

    void createForShard(
        const Cluster::ShardInfo & shard_info,
        const ASTPtr & query_ast,
        ContextPtr context, const ThrottlerPtr & throttler,
        std::vector<QueryPlanPtr> & plans,
        Pipes & remote_pipes,
        Pipes & delayed_pipes,
        Poco::Logger * log) override;

private:
    const Block header;
    QueryProcessingStage::Enum processed_stage;
    StorageID main_table = StorageID::createEmpty();
    ASTPtr table_func_ptr;
    Scalars scalars;
    bool has_virtual_shard_num_column = false;
    Tables external_tables;
};

}

}
