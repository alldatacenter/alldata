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

#include <Functions/JSONPath/ASTs/ASTJSONPathRange.h>
#include <Functions/JSONPath/Parsers/ParserJSONPathQuery.h>
#include <Functions/JSONPath/Parsers/ParserJSONPathRange.h>

#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/CommonParsers.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}
/**
 *
 * @param pos token iterator
 * @param node node of ASTJSONPathQuery
 * @param expected stuff for logging
 * @return was parse successful
 */
bool ParserJSONPathRange::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{

    if (pos->type != TokenType::OpeningSquareBracket)
    {
        return false;
    }
    ++pos;

    auto range = std::make_shared<ASTJSONPathRange>();
    node = range;

    ParserNumber number_p(ParserSettings::CLICKHOUSE);
    ASTPtr number_ptr;
    while (pos->type != TokenType::ClosingSquareBracket)
    {
        if (pos->type != TokenType::Number)
        {
            return false;
        }

        std::pair<UInt32, UInt32> range_indices;
        if (!number_p.parse(pos, number_ptr, expected))
        {
            return false;
        }
        range_indices.first = number_ptr->as<ASTLiteral>()->value.get<UInt32>();

        if (pos->type == TokenType::Comma || pos->type == TokenType::ClosingSquareBracket)
        {
            /// Single index case
            range_indices.second = range_indices.first + 1;
        }
        else if (pos->type == TokenType::BareWord)
        {
            if (!ParserKeyword("TO").ignore(pos, expected))
            {
                return false;
            }
            if (!number_p.parse(pos, number_ptr, expected))
            {
                return false;
            }
            range_indices.second = number_ptr->as<ASTLiteral>()->value.get<UInt32>();
        }
        else
        {
            return false;
        }

        if (range_indices.first >= range_indices.second)
        {
            throw Exception(
                ErrorCodes::BAD_ARGUMENTS,
                "Start of range must be greater than end of range, however {} >= {}",
                range_indices.first,
                range_indices.second);
        }

        range->ranges.push_back(std::move(range_indices));
        if (pos->type != TokenType::ClosingSquareBracket)
        {
            ++pos;
        }
    }
    ++pos;

    /// We can't have both ranges and star present, so parse was successful <=> exactly 1 of these conditions is true
    return !range->ranges.empty() ^ range->is_star;
}

}
