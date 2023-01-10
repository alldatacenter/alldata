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

#include <Parsers/ASTSelectQuery.h>
#include <Parsers/IAST_fwd.h>
#include <Storages/IStorage.h>

#include <common/shared_ptr_helper.h>


namespace DB
{

class StorageView final : public shared_ptr_helper<StorageView>, public IStorage
{
    friend struct shared_ptr_helper<StorageView>;
public:
    std::string getName() const override { return "View"; }
    bool isView() const override { return true; }

    /// It is passed inside the query and solved at its level.
    bool supportsSampling() const override { return true; }
    bool supportsFinal() const override { return true; }
    bool supportsMapImplicitColumn() const override { return true; }
    const ASTPtr & getInnerQuery() const
    {
        return getInMemoryMetadataPtr()->getSelectQuery().inner_query ;
    }
    Pipe read(
        const Names & column_names,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned num_streams) override;

    void read(
        QueryPlan & query_plan,
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned num_streams) override;

    static void replaceWithSubquery(ASTSelectQuery & select_query, ASTPtr & view_name, const StorageMetadataPtr & metadata_snapshot)
    {
        replaceWithSubquery(select_query, metadata_snapshot->getSelectQuery().inner_query->clone(), view_name);
    }

    static void replaceWithSubquery(ASTSelectQuery & outer_query, ASTPtr view_query, ASTPtr & view_name);
    static ASTPtr restoreViewName(ASTSelectQuery & select_query, const ASTPtr & view_name);

protected:
    StorageView(
        const StorageID & table_id_,
        const ASTCreateQuery & query,
        const ColumnsDescription & columns_,
        const String & comment);
};

}
