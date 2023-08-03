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

#include <Parsers/ParserRefreshQuery.h>
#include <Parsers/ASTRefreshQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/parseDatabaseAndTableName.h>
#include <Parsers/ParserPartition.h>


namespace DB
{
    bool ParserRefreshQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
    {
        auto query = std::make_shared<ASTRefreshQuery>();
        node = query;

        ParserKeyword s_refresh_view("REFRESH MATERIALIZED VIEW");
        ParserKeyword s_partition("PARTITION");
        ParserKeyword s_sync("SYNC");
        ParserKeyword s_async("ASYNC");
        ParserPartition parser_partition(dt);

        if (!s_refresh_view.ignore(pos, expected))
            return false;

        if (!parseDatabaseAndTableName(pos, expected, query->database, query->table))
            return false;

        if (s_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, query->partition, expected))
                return false;
        }

        if (s_sync.ignore(pos, expected))
            query->async = false;
        else if (s_async.ignore(pos, expected))
            query->async = true;

        return true;
    }
}
