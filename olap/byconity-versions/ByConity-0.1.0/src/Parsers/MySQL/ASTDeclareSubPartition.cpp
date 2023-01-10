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

#include <Parsers/MySQL/ASTDeclareSubPartition.h>

#include <Parsers/ASTIdentifier.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/MySQL/ASTDeclareOption.h>
#include <Parsers/ExpressionElementParsers.h>

namespace DB
{

namespace MySQLParser
{

bool ParserDeclareSubPartition::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    if (!ParserKeyword{"SUBPARTITION"}.ignore(pos, expected))
        return false;

    ASTPtr options;
    ASTPtr logical_name;
    ParserIdentifier p_identifier;

    if (!p_identifier.parse(pos, logical_name, expected))
        return false;

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

    auto subpartition_declare = std::make_shared<ASTDeclareSubPartition>();
    subpartition_declare->options = options;
    subpartition_declare->logical_name = logical_name->as<ASTIdentifier>()->name();

    if (options)
    {
        subpartition_declare->options = options;
        subpartition_declare->children.emplace_back(subpartition_declare->options);
    }

    node = subpartition_declare;
    return true;
}

ASTPtr ASTDeclareSubPartition::clone() const
{
    auto res = std::make_shared<ASTDeclareSubPartition>(*this);
    res->children.clear();

    if (options)
    {
        res->options = options->clone();
        res->children.emplace_back(res->options);
    }

    return res;
}
}

}
