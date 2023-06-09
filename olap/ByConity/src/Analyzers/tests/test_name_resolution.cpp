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

#include <Analyzers/Scope.h>

#include <gtest/gtest.h>
#include <Poco/StringTokenizer.h>

using namespace DB;

FieldDescription strToField(const String & str)
{
    Poco::StringTokenizer tokenizer {str, "."};
    std::vector<String> name_prefix;
    String name;

    auto count = tokenizer.count();

    if (count >= 3)
        name_prefix.push_back(tokenizer[count - 3]);

    if (count >= 2)
        name_prefix.push_back(tokenizer[count - 2]);

    if (count >= 1)
        name = tokenizer[count - 1];

    return FieldDescription {name, nullptr, QualifiedName(name_prefix)};
}

QualifiedName strToName(const String & str)
{
    Poco::StringTokenizer tokenizer {str, "."};
    std::vector<String> name;

    for (auto & token: tokenizer)
        name.push_back(token);

    if (name.empty())
        name.push_back("");

    return QualifiedName(name);
}

TEST(NameResolutionTest, ResolveByAnsiSqlSemantic)
{
    EXPECT_FALSE(strToField("").matchName(strToName("")));
    EXPECT_FALSE(strToField("").matchName(strToName("col1")));

    EXPECT_FALSE(strToField("col1").matchName(strToName("")));
    EXPECT_TRUE(strToField("col1").matchName(strToName("col1")));
    EXPECT_FALSE(strToField("col1").matchName(strToName("col2")));
    EXPECT_FALSE(strToField("col1").matchName(strToName("table1.col1")));
    EXPECT_FALSE(strToField("col1").matchName(strToName("db1.table1.col1")));

    EXPECT_FALSE(strToField("table1.col1").matchName(strToName("")));
    EXPECT_TRUE(strToField("table1.col1").matchName(strToName("col1")));
    EXPECT_FALSE(strToField("table1.col1").matchName(strToName("col2")));
    EXPECT_TRUE(strToField("table1.col1").matchName(strToName("table1.col1")));
    EXPECT_FALSE(strToField("table1.col1").matchName(strToName("table2.col1")));
    EXPECT_FALSE(strToField("table1.col1").matchName(strToName("table1.col2")));
    EXPECT_FALSE(strToField("table1.col1").matchName(strToName("table2.col2")));
    EXPECT_FALSE(strToField("table1.col1").matchName(strToName("db1.table1.col1")));

    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("")));
    EXPECT_TRUE(strToField("db1.table1.col1").matchName(strToName("col1")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("col2")));
    EXPECT_TRUE(strToField("db1.table1.col1").matchName(strToName("table1.col1")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("table2.col1")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("table1.col2")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("table2.col2")));
    EXPECT_TRUE(strToField("db1.table1.col1").matchName(strToName("db1.table1.col1")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("db2.table1.col1")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("db1.table2.col1")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("db1.table1.col2")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("db1.table2.col2")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("db2.table1.col2")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("db2.table2.col1")));
    EXPECT_FALSE(strToField("db1.table1.col1").matchName(strToName("db2.table2.col2")));

}
