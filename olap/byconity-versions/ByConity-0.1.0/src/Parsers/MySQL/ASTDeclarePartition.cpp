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

#include <Parsers/MySQL/ASTDeclarePartition.h>

#include <Parsers/ASTIdentifier.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/MySQL/ASTDeclareOption.h>
#include <Parsers/MySQL/ASTDeclareSubPartition.h>

namespace DB
{

namespace MySQLParser
{

ASTPtr ASTDeclarePartition::clone() const
{
    auto res = std::make_shared<ASTDeclarePartition>(*this);
    res->children.clear();

    if (options)
    {
        res->options = options->clone();
        res->children.emplace_back(res->options);
    }

    if (less_than)
    {
        res->less_than = less_than->clone();
        res->children.emplace_back(res->less_than);
    }

    if (in_expression)
    {
        res->in_expression = in_expression->clone();
        res->children.emplace_back(res->in_expression);
    }

    if (subpartitions)
    {
        res->subpartitions = subpartitions->clone();
        res->children.emplace_back(res->subpartitions);
    }

    return res;
}

bool ParserDeclarePartition::parseImpl(IParser::Pos & pos, ASTPtr & node, Expected & expected)
{
    if (!ParserKeyword{"PARTITION"}.ignore(pos, expected))
        return false;

    ASTPtr options;
    ASTPtr less_than;
    ASTPtr in_expression;
    ASTPtr partition_name;

    ParserExpression p_expression(ParserSettings::CLICKHOUSE);
    ParserIdentifier p_identifier;

    if (!p_identifier.parse(pos, partition_name, expected))
        return false;

    ParserKeyword p_values("VALUES");
    if (p_values.ignore(pos, expected))
    {
        if (ParserKeyword{"IN"}.ignore(pos, expected))
        {
            if (!p_expression.parse(pos, in_expression, expected))
                return false;
        }
        else if (ParserKeyword{"LESS THAN"}.ignore(pos, expected))
        {
            if (!p_expression.parse(pos, less_than, expected))
                return false;
        }
    }

    ParserDeclareOptions options_p{
        {
            OptionDescribe("ENGINE", "engine", std::make_shared<ParserIdentifier>()),
            OptionDescribe("STORAGE ENGINE", "engine", std::make_shared<ParserIdentifier>()),
            OptionDescribe("COMMENT", "comment", std::make_shared<ParserStringLiteral>()),
            OptionDescribe("DATA DIRECTORY", "data_directory", std::make_shared<ParserStringLiteral>()),
            OptionDescribe("INDEX DIRECTORY", "index_directory", std::make_shared<ParserStringLiteral>()),
            OptionDescribe("MAX_ROWS", "max_rows", std::make_shared<ParserLiteral>(ParserSettings::CLICKHOUSE)),
            OptionDescribe("MIN_ROWS", "min_rows", std::make_shared<ParserLiteral>(ParserSettings::CLICKHOUSE)),
            OptionDescribe("TABLESPACE", "tablespace", std::make_shared<ParserIdentifier>()),
        }
    };

    /// Optional options
    options_p.parse(pos, options, expected);

    ASTPtr subpartitions;
    if (ParserToken(TokenType::OpeningRoundBracket).ignore(pos, expected))
    {
        if (!DB::ParserList(std::make_unique<ParserDeclareSubPartition>(), std::make_unique<ParserToken>(TokenType::Comma))
                 .parse(pos, subpartitions, expected))
            return false;

        if (!ParserToken(TokenType::ClosingRoundBracket).ignore(pos, expected))
            return false;
    }

    auto partition_declare = std::make_shared<ASTDeclarePartition>();
    partition_declare->options = options;
    partition_declare->less_than = less_than;
    partition_declare->in_expression = in_expression;
    partition_declare->subpartitions = subpartitions;
    partition_declare->partition_name = partition_name->as<ASTIdentifier>()->name();

    if (options)
    {
        partition_declare->options = options;
        partition_declare->children.emplace_back(partition_declare->options);
    }

    if (partition_declare->less_than)
        partition_declare->children.emplace_back(partition_declare->less_than);

    if (partition_declare->in_expression)
        partition_declare->children.emplace_back(partition_declare->in_expression);

    if (partition_declare->subpartitions)
        partition_declare->children.emplace_back(partition_declare->subpartitions);

    node = partition_declare;
    return true;
}
}

}
