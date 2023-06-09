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

#include <Parsers/ASTDumpInfoQuery.h>
#include <Parsers/ParserDumpInfoQuery.h>
#include <Parsers/ParserSelectWithUnionQuery.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/parseQuery.h>
#include <Parsers/formatAST.h>
namespace DB
{
bool ParserDumpInfoQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_dump("DUMP");
    if (!s_dump.ignore(pos, expected))
    {
        return false;
    }
    auto query = std::make_shared<ASTDumpInfoQuery>();
    ParserSelectWithUnionQuery select_p(dt);
    ASTPtr sub_query;
    if(select_p.parse(pos, sub_query, expected))
    {
        WriteBufferFromOwnString buf;
        formatAST(*sub_query, buf, false, false);
        query->dump_string = buf.str();
        query->children.emplace_back(sub_query);
        query->dump_query = std::move(sub_query);
    }
    else
        return false;
    node = std::move(query);
    return true;
}

}
