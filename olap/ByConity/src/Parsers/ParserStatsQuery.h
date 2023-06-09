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

#pragma once

#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTStatsQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/IParserBase.h>
#include <Parsers/parseDatabaseAndTableName.h>
#include "parseDatabaseAndTableName.h"

namespace DB
{

bool parseStatsQueryKind(IParser::Pos & pos, Expected & expected, StatsQueryKind & kind);

/** Query like this:
  * (SHOW | DROP) (STATS | TABLE_STATS | COLUMN_STATS) (ALL | [db_name.]table_name) [AT COLUMN column_name] [ON CLUSTER cluster]
  *
  * or:
  * CREATE (STATS | TABLE_STATS | COLUMN_STATS) (ALL | [db_name.]table_name) [AT COLUMN column_name]
  *                                                                          [ON CLUSTER cluster]
  *                                                                          [PARTITION partition | PARTITION ID 'partition_id']
  *                                                                          [WITH NUM BUCKETS|TOPN|SAMPLES]
  */
template <typename ParserName, typename QueryAstClass, typename QueryInfo>
class ParserStatsQueryBase : public IParserBase
{
public:
    [[nodiscard]] const char * getName() const override { return ParserName::Name; }
    using SampleType = ASTCreateStatsQuery::SampleType;

protected:
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        ParserKeyword s_query_prefix(QueryInfo::QueryPrefix);
        ParserKeyword s_all("ALL");
        ParserKeyword s_on("ON");
        ParserToken open(TokenType::OpeningRoundBracket);
        ParserToken close(TokenType::ClosingRoundBracket);
        ParserIdentifier p_column_name;

        auto query = std::make_shared<QueryAstClass>();

        if (!s_query_prefix.ignore(pos, expected))
            return false;

        if (!parseStatsQueryKind(pos, expected, query->kind))
            return false;

        if constexpr (std::is_same_v<QueryInfo, CreateStatsQueryInfo>)
        {
            // IF NOT EXISTS is valid only for create
            ParserKeyword s_if_not_exists("IF NOT EXISTS");
            if (s_if_not_exists.ignore(pos, expected))
                query->if_not_exists = true;
        }

        query->target_all = s_all.ignore(pos, expected);

        if (!query->target_all)
        {
            bool any_database = false;
            bool any_table = false;

            if (!parseDatabaseAndTableNameOrAsterisks(pos, expected, query->database, any_database, query->table, any_table))
                return false;
            
            // collect on any database is not implemented
            if (any_database)
                return false;

            if (any_table)
            {
                query->target_all = true;
            }
            else if (open.ignore(pos, expected))
            {
                // parse columns when given table
                auto parse_id = [&query, &pos, &expected] {
                    ASTPtr identifier;
                    if (!ParserIdentifier(true).parse(pos, identifier, expected))
                        return false;

                    query->columns.emplace_back(getIdentifierName(identifier));
                    return true;
                };

                if (!ParserList::parseUtil(pos, expected, parse_id, false))
                    return false;

                if (!close.ignore(pos, expected))
                    return false;
            }
        }

        if (s_on.ignore(pos, expected))
        {
            if (!ASTQueryWithOnCluster::parse(pos, query->cluster, expected))
                return false;
        }

        if (!parseSuffix(pos, expected, *query))
            return false;

        node = query;
        return true;
    }

    virtual bool parseSuffix(Pos &, Expected &, IAST &) { return true; }
};

struct CreateStatsParserName
{
    static constexpr auto Name = "Create stats query";
};

struct ShowStatsParserName
{
    static constexpr auto Name = "Show stats query";
};

struct DropStatsParserName
{
    static constexpr auto Name = "Drop stats query";
};

using ParserShowStatsQuery = ParserStatsQueryBase<ShowStatsParserName, ASTShowStatsQuery, ShowStatsQueryInfo>;
using ParserDropStatsQuery = ParserStatsQueryBase<DropStatsParserName, ASTDropStatsQuery, DropStatsQueryInfo>;

class ParserCreateStatsQuery : public ParserStatsQueryBase<CreateStatsParserName, ASTCreateStatsQuery, CreateStatsQueryInfo>
{
protected:
    bool parseSuffix(Pos &, Expected &, IAST &) override;
};

}
