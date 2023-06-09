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

#include <gtest/gtest.h>

#include <Interpreters/QueryNormalizer.h>
#include <Parsers/IAST.h>
#include <Parsers/queryToString.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/parseQuery.h>
#include <Core/Settings.h>

using namespace DB;


TEST(QueryNormalizer, SimpleLoopAlias)
{
    String query = "a as a";
    ParserExpressionList parser(false, ParserSettings::CLICKHOUSE);
    ASTPtr ast = parseQuery(parser, query, 0, 0);

    Aliases aliases;
    aliases["a"] = parseQuery(parser, "a as a", 0, 0)->children[0];

    Settings settings;
    QueryNormalizer::Data normalizer_data(aliases, {}, false, settings, false);
    EXPECT_THROW(QueryNormalizer(normalizer_data).visit(ast), Exception);
}

TEST(QueryNormalizer, SimpleCycleAlias)
{
    String query = "a as b, b as a";
    ParserExpressionList parser(false, ParserSettings::CLICKHOUSE);
    ASTPtr ast = parseQuery(parser, query, 0, 0);

    Aliases aliases;
    aliases["a"] = parseQuery(parser, "b as a", 0, 0)->children[0];
    aliases["b"] = parseQuery(parser, "a as b", 0, 0)->children[0];

    Settings settings;
    QueryNormalizer::Data normalizer_data(aliases, {}, false, settings, true);
    EXPECT_THROW(QueryNormalizer(normalizer_data).visit(ast), Exception);
}
