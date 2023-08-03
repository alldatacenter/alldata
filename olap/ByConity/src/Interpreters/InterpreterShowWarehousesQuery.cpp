/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#include <Interpreters/executeQuery.h>
#include <Interpreters/InterpreterShowWarehousesQuery.h>
#include <Parsers/ASTShowWarehousesQuery.h>


namespace DB
{

InterpreterShowWarehousesQuery::InterpreterShowWarehousesQuery(const ASTPtr & query_ptr_, ContextMutablePtr context_)
    : WithMutableContext(context_), query_ptr(query_ptr_) {}

String InterpreterShowWarehousesQuery::getRewrittenQuery()
{
    auto & show = query_ptr->as<ASTShowWarehousesQuery &>();
    std::stringstream rewritten_query;

    rewritten_query << "SELECT * from system.virtual_warehouses";

    if (!show.like.empty())
    {
       rewritten_query << " WHERE name LIKE " << std::quoted(show.like, '\'');
    }

    rewritten_query << " ORDER BY name ";
    return rewritten_query.str();

}

BlockIO InterpreterShowWarehousesQuery::execute()
{
    return executeQuery(getRewrittenQuery(), getContext(), true);
}

}
