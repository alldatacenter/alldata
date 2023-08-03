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

#include <Parsers/MySQL/ASTDeclarePartitionOptions.h>

#include <Parsers/ASTLiteral.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/MySQL/ASTDeclarePartition.h>

namespace DB
{

namespace MySQLParser
{

ASTPtr ASTDeclarePartitionOptions::clone() const
{
    auto res = std::make_shared<ASTDeclarePartitionOptions>(*this);
    res->children.clear();

    if (partition_numbers)
    {
        res->partition_numbers = partition_numbers->clone();
        res->children.emplace_back(res->partition_numbers);
    }

    if (partition_expression)
    {
        res->partition_expression = partition_expression->clone();
        res->children.emplace_back(res->partition_expression);
    }

    if (subpartition_numbers)
    {
        res->subpartition_numbers = subpartition_numbers->clone();
        res->children.emplace_back(res->subpartition_numbers);
    }

    if (subpartition_expression)
    {
        res->subpartition_expression = subpartition_expression->clone();
        res->children.emplace_back(res->subpartition_expression);
    }

    return res;
}

static inline bool parsePartitionExpression(IParser::Pos & pos, std::string & type, ASTPtr & node, Expected & expected, bool subpartition = false)
{
    ASTPtr expression;
    ParserExpression p_expression(ParserSettings::CLICKHOUSE);
    if (!subpartition && ParserKeyword("LIST").ignore(pos, expected))
    {
        type = "list";
        ParserKeyword("COLUMNS").ignore(pos, expected);
        if (!p_expression.parse(pos, expression, expected))
            return false;
    }
    else if (!subpartition && ParserKeyword("RANGE").ignore(pos, expected))
    {
        type = "range";
        ParserKeyword("COLUMNS").ignore(pos, expected);
        if (!p_expression.parse(pos, expression, expected))
            return false;
    }
    else
    {
        if (ParserKeyword("LINEAR").ignore(pos, expected))
            type = "linear_";

        if (ParserKeyword("KEY").ignore(pos, expected))
        {
            type += "key";

            if (ParserKeyword("ALGORITHM").ignore(pos, expected))
            {
                if (!ParserToken(TokenType::Equals).ignore(pos, expected))
                    return false;

                ASTPtr algorithm;
                ParserLiteral p_literal(ParserSettings::CLICKHOUSE);
                if (!p_literal.parse(pos, algorithm, expected) || !algorithm->as<ASTLiteral>())
                    return false;

                UInt64 algorithm_type = algorithm->as<ASTLiteral>()->value.safeGet<UInt64>();

                if (algorithm_type != 1 && algorithm_type != 2)
                    return false;

                type += "_" + toString(algorithm_type);
            }

            if (!p_expression.parse(pos, expression, expected))
                return false;
        }
        else if (ParserKeyword("HASH").ignore(pos, expected))
        {
            type += "hash";
            if (!p_expression.parse(pos, expression, expected))
                return false;
        }
        else
            return false;
    }

    node = expression;
    return true;
}

bool ParserDeclarePartitionOptions::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    String partition_type;
    ASTPtr partition_numbers;
    ASTPtr partition_expression;
    String subpartition_type;
    ASTPtr subpartition_numbers;
    ASTPtr subpartition_expression;
    ASTPtr declare_partitions;

    if (!ParserKeyword("PARTITION BY").ignore(pos, expected))
        return false;

    if (!parsePartitionExpression(pos, partition_type, partition_expression, expected))
        return false;

    if (ParserKeyword("PARTITIONS").ignore(pos, expected))
    {
        ParserLiteral p_literal(ParserSettings::CLICKHOUSE);
        if (!p_literal.parse(pos, partition_numbers, expected))
            return false;
    }

    if (ParserKeyword("SUBPARTITION BY").ignore(pos, expected))
    {
        if (!parsePartitionExpression(pos, subpartition_type, subpartition_expression, expected, true))
            return false;

        if (ParserKeyword("SUBPARTITIONS").ignore(pos, expected))
        {
            ParserLiteral p_literal(ParserSettings::CLICKHOUSE);
            if (!p_literal.parse(pos, subpartition_numbers, expected))
                return false;
        }
    }

    if (ParserToken(TokenType::OpeningRoundBracket).ignore(pos, expected))
    {
        if (!ParserList(std::make_unique<ParserDeclarePartition>(), std::make_unique<ParserToken>(TokenType::Comma))
                 .parse(pos, declare_partitions, expected))
            return false;

        if (!ParserToken(TokenType::ClosingRoundBracket).ignore(pos, expected))
            return false;
    }

    auto declare_partition_options = std::make_shared<ASTDeclarePartitionOptions>();
    declare_partition_options->partition_type = partition_type;
    declare_partition_options->partition_numbers = partition_numbers;
    declare_partition_options->partition_expression = partition_expression;
    declare_partition_options->subpartition_type = subpartition_type;
    declare_partition_options->subpartition_numbers = subpartition_numbers;
    declare_partition_options->subpartition_expression = subpartition_expression;
    declare_partition_options->declare_partitions = declare_partitions;

    if (declare_partition_options->partition_numbers)
        declare_partition_options->children.emplace_back(declare_partition_options->partition_numbers);

    if (declare_partition_options->partition_expression)
        declare_partition_options->children.emplace_back(declare_partition_options->partition_expression);

    if (declare_partition_options->subpartition_numbers)
        declare_partition_options->children.emplace_back(declare_partition_options->subpartition_numbers);

    if (declare_partition_options->subpartition_expression)
        declare_partition_options->children.emplace_back(declare_partition_options->subpartition_expression);

    if (declare_partition_options->declare_partitions)
        declare_partition_options->children.emplace_back(declare_partition_options->declare_partitions);

    node = declare_partition_options;
    return true;
}
}

}
