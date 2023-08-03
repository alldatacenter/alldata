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

#include <Parsers/ParserCreateWorkerGroupQuery.h>

#include <Parsers/ASTCreateWorkerGroupQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ParserSetQuery.h>

namespace DB
{
bool ParserCreateWorkerGroupQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_create("CREATE");
    ParserKeyword s_worker_group("WORKER GROUP");
    ParserKeyword s_if_not_exists("IF NOT EXISTS");
    ParserKeyword s_in("IN");
    ParserKeyword s_settings("SETTINGS");
    ParserSetQuery settings_p(/* parse_only_internals_ = */ true);

    if (!s_create.ignore(pos, expected))
        return false;

    if (!s_worker_group.ignore(pos, expected))
        return false;

    bool if_not_exists = false;
    if (s_if_not_exists.ignore(pos, expected))
        if_not_exists = true;

    ASTPtr worker_group_id_ast;
    if (!ParserIdentifier{}.parse(pos, worker_group_id_ast, expected))
        return false;

    ASTPtr vw_name_ast;
    if (s_in.ignore(pos, expected))
    {
        if (!ParserIdentifier{}.parse(pos, vw_name_ast, expected))
            return false;
    }

    ASTPtr settings;
    if (s_settings.ignore(pos, expected))
    {
        if (!settings_p.parse(pos, settings, expected))
            return false;
    }

    /// construct ast
    auto query = std::make_shared<ASTCreateWorkerGroupQuery>();
    query->if_not_exists = if_not_exists;
    query->worker_group_id = getIdentifierName(worker_group_id_ast);
    if (vw_name_ast)
        query->vw_name = getIdentifierName(vw_name_ast);
    query->set(query->settings, settings);

    node = query;
    return true;
}

}
