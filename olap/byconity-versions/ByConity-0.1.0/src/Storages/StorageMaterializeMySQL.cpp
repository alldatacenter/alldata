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

#if !defined(ARCADIA_BUILD)
#    include "config_core.h"
#endif

#if USE_MYSQL

#include <Storages/StorageMaterializeMySQL.h>

#include <Core/Settings.h>
#include <Interpreters/Context.h>
#include <Interpreters/ExpressionAnalyzer.h>
#include <Interpreters/TreeRewriter.h>

#include <Parsers/ASTFunction.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>

#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTIdentifier.h>

#include <Processors/Pipe.h>
#include <Processors/Transforms/FilterTransform.h>

#include <Databases/MySQL/DatabaseMaterializeMySQL.h>
#include <Storages/ReadFinalForExternalReplicaStorage.h>
#include <Storages/SelectQueryInfo.h>

namespace DB
{

StorageMaterializeMySQL::StorageMaterializeMySQL(const StoragePtr & nested_storage_, const IDatabase * database_)
    : StorageProxy(nested_storage_->getStorageID()), nested_storage(nested_storage_), database(database_)
{
    StorageInMemoryMetadata in_memory_metadata;
    in_memory_metadata = nested_storage->getInMemoryMetadata();
    setInMemoryMetadata(in_memory_metadata);
}

bool StorageMaterializeMySQL::needRewriteQueryWithFinal(const Names & column_names) const
{
    return needRewriteQueryWithFinalForStorage(column_names, nested_storage);
}

Pipe StorageMaterializeMySQL::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr context,
    QueryProcessingStage::Enum processed_stage,
    size_t max_block_size,
    unsigned int num_streams)
{
    /// If the background synchronization thread has exception.
    rethrowSyncExceptionIfNeed(database);
    return nested_storage->read(column_names, metadata_snapshot, query_info, context, processed_stage, max_block_size, num_streams);
}

NamesAndTypesList StorageMaterializeMySQL::getVirtuals() const
{
    /// If the background synchronization thread has exception.
    rethrowSyncExceptionIfNeed(database);
    return nested_storage->getVirtuals();
}

}

#endif
