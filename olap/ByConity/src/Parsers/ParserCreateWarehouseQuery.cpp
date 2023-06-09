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

#include <Parsers/ParserCreateWarehouseQuery.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTCreateWarehouseQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/ParserSetQuery.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_MANAGER_INCOMPATIBLE_SETTINGS;
}
bool ParserCreateWarehouseQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_create_warehouse("CREATE WAREHOUSE ");
    ParserKeyword s_if_not_exists("IF NOT EXISTS");
    ParserKeyword s_settings("SETTINGS");
    ParserLiteral value_p;
    ParserSetQuery settings_p(/* parse_only_internals_ = */ true);

    ASTPtr settings;
    bool if_not_exists = false;

    auto query = std::make_shared<ASTCreateWarehouseQuery>();

    if (!ParserKeyword{"CREATE WAREHOUSE"}.ignore(pos, expected))
        return false;

    if (s_if_not_exists.ignore(pos, expected))
        if_not_exists = true;

    ASTPtr warehouse_name_ast;
    if (!ParserIdentifier{}.parse(pos, warehouse_name_ast, expected))
        return false;
    String warehouse_name = getIdentifierName(warehouse_name_ast);

    if (s_settings.ignore(pos, expected))
    {
        if (!settings_p.parse(pos, settings, expected))
            return false;
    }

    query->name = std::move(warehouse_name);
    if (settings)
        query->set(query->settings, settings);

    query->if_not_exists = if_not_exists;
    node = query;
    return true;
}

}
