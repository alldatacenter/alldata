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
#include <Interpreters/Context_fwd.h>
#include <Parsers/IAST_fwd.h>
#include <Storages/SelectQueryInfo.h>

#include <unordered_set>


namespace DB
{

class NamesAndTypesList;


namespace VirtualColumnUtils
{

/// Adds to the select query section `WITH value AS column_name`, and uses func
/// to wrap the value (if any)
///
/// For example:
/// - `WITH 9000 as _port`.
/// - `WITH toUInt16(9000) as _port`.
void rewriteEntityInAst(ASTPtr ast, const String & column_name, const Field & value, const String & func = "");

/// Prepare `expression_ast` to filter block. Returns true if `expression_ast` is not trimmed, that is,
/// `block` provides all needed columns for `expression_ast`, else return false.
bool prepareFilterBlockWithQuery(const ASTPtr & query, ContextPtr context, Block block, ASTPtr & expression_ast);

/// Leave in the block only the rows that fit under the WHERE clause and the PREWHERE clause of the query.
/// Only elements of the outer conjunction are considered, depending only on the columns present in the block.
/// If `expression_ast` is passed, use it to filter block.
void filterBlockWithQuery(const ASTPtr & query, Block & block, ContextPtr context, ASTPtr expression_ast = {});

/// Extract from the input stream a set of `name` column values
template <typename T>
auto extractSingleValueFromBlock(const Block & block, const String & name)
{
    std::unordered_set<T> res;
    const ColumnWithTypeAndName & data = block.getByName(name);
    size_t rows = block.rows();
    for (size_t i = 0; i < rows; ++i)
        res.insert((*data.column)[i].get<T>());
    return res;
}

/// Utility method for cascading drop/detach partition
void cascadingDrop(const StorageID & table_id, const ASTPtr & partition_or_predicate,
                   bool drop_where, bool detach, bool cascading, ContextPtr context);

}

}
