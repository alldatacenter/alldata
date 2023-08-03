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

#include <Parsers/ParserDropWarehouseQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ASTDropWarehouseQuery.h>

namespace DB
{

bool ParserDropWarehouseQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_if_exists("IF EXISTS");

    if (!ParserKeyword{"DROP WAREHOUSE"}.ignore(pos, expected))
        return false;

    bool if_exists = false;
    if (s_if_exists.ignore(pos, expected))
        if_exists = true;

    ASTPtr group_name_ast;
    if (!ParserIdentifier{}.parse(pos, group_name_ast, expected))
        return false;
    String group_name = getIdentifierName(group_name_ast);


    auto query = std::make_shared<ASTDropWarehouseQuery>();
    query->name = std::move(group_name);
    query->if_exists = if_exists;
    node = query;
    return true;
}

}
