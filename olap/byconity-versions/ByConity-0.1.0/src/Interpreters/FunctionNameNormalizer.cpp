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

#include <Interpreters/FunctionNameNormalizer.h>

#include <Parsers/ASTColumnDeclaration.h>
#include <Parsers/ASTCreateQuery.h>

namespace DB
{

const String & getFunctionCanonicalNameIfAny(const String & name);
const String & getAggregateFunctionCanonicalNameIfAny(const String & name);

void FunctionNameNormalizer::visit(IAST * ast)
{
    if (!ast)
        return;

    // Normalize only selected children. Avoid normalizing engine clause because some engine might
    // have the same name as function, e.g. Log.
    if (auto * node_storage = ast->as<ASTStorage>())
    {
        visit(node_storage->partition_by);
        visit(node_storage->primary_key);
        visit(node_storage->order_by);
        visit(node_storage->unique_key);
        visit(node_storage->sample_by);
        visit(node_storage->ttl_table);
        return;
    }

    // Normalize only selected children. Avoid normalizing type clause because some type might
    // have the same name as function, e.g. Date.
    if (auto * node_decl = ast->as<ASTColumnDeclaration>())
    {
        visit(node_decl->default_expression.get());
        visit(node_decl->ttl.get());
        return;
    }

    if (auto * node_func = ast->as<ASTFunction>())
        node_func->name = getAggregateFunctionCanonicalNameIfAny(getFunctionCanonicalNameIfAny(node_func->name));

    for (auto & child : ast->children)
        visit(child.get());
}

}
