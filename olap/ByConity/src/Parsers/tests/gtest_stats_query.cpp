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

#include <Parsers/ASTPartition.h>
#include <Parsers/ParserQueryWithOutput.h>
#include <Parsers/ParserStatsQuery.h>
#include <Parsers/parseQuery.h>

#include <gtest/gtest.h>

using namespace DB;

using SampleType = ASTCreateStatsQuery::SampleType;

struct SimpleStatsASTParseResult
{
    String id;
    ASTType type;
    StatsQueryKind kind;
    bool target_all;
    String database;
    String table;
    std::vector<String> columns;
    String cluster;
    std::optional<String> formatted_sql;
};

struct CreateStatsASTParseResult
{
    String id;
    StatsQueryKind kind;
    bool target_all = false;
    std::optional<String> partition_id;
    SampleType sample_type = SampleType::Default;
    std::optional<Int64> sample_rows;
    std::optional<Float64> sample_ratio;
    String database;
    String table;
    std::vector<String> columns;
    String cluster;
    std::optional<String> formatted_sql;
    bool if_not_exists = false;
};

class StatsQueryTest : public ::testing::Test
{
protected:
    void checkCreateStatsQuerySuccessParse(const String & input, const CreateStatsASTParseResult & expect)
    {
        auto beg = input.data();
        auto end = input.data() + input.size();
        ParserQueryWithOutput parser{end, ParserSettings::CLICKHOUSE};
        auto res = parseQuery(parser, beg, end, "", 0, 0);
        auto * ast = res->as<ASTCreateStatsQuery>();
        ASSERT_TRUE(ast);
        EXPECT_EQ(ast->getID(' '), expect.id);
        EXPECT_EQ(ast->getType(), ASTType::ASTCreateStatsQuery);
        EXPECT_EQ(ast->kind, expect.kind);
        EXPECT_EQ(ast->target_all, expect.target_all);
        EXPECT_EQ(ast->database, expect.database);
        EXPECT_EQ(ast->sample_type, expect.sample_type);
        EXPECT_EQ(ast->sample_rows, expect.sample_rows);
        EXPECT_EQ(ast->sample_ratio, expect.sample_ratio) << input;

        EXPECT_EQ(ast->table, expect.table);
        EXPECT_EQ(ast->columns, expect.columns);
        EXPECT_EQ(ast->cluster, expect.cluster);
        EXPECT_EQ(ast->if_not_exists, expect.if_not_exists);

        WriteBufferFromOwnString os;
        IAST::FormatSettings fs{os, true};
        ast->IAST::format(fs);
        auto expected_sql = expect.formatted_sql.value_or(input);
        EXPECT_EQ(expected_sql, os.str());
    }

    template <typename AstStatsQuery>
    void checkSuccessParse(const String & input, const SimpleStatsASTParseResult & expect)
    {
        auto beg = input.data();
        auto end = input.data() + input.size();
        ParserQueryWithOutput parser{end, ParserSettings::CLICKHOUSE};
        auto res = parseQuery(parser, beg, end, "", 0, 0);
        auto * ast = res->as<AstStatsQuery>();
        ASSERT_TRUE(ast);
        EXPECT_EQ(ast->getID(' '), expect.id);
        EXPECT_EQ(ast->getType(), expect.type);
        EXPECT_EQ(ast->kind, expect.kind);
        EXPECT_EQ(ast->target_all, expect.target_all);
        EXPECT_EQ(ast->database, expect.database);
        EXPECT_EQ(ast->table, expect.table);
        EXPECT_EQ(ast->columns, expect.columns);
        EXPECT_EQ(ast->cluster, expect.cluster);


        WriteBufferFromOwnString os;
        IAST::FormatSettings fs{os, true};
        ast->IAST::format(fs);
        auto expected_sql = expect.formatted_sql.value_or(input);
        EXPECT_EQ(expected_sql, os.str());
    }

    void checkFailParse(String input)
    {
        auto beg = input.data();
        auto end = input.data() + input.size();
        ParserQueryWithOutput parser{end, ParserSettings::CLICKHOUSE};
        bool throws = false;

        try
        {
            parseQuery(parser, beg, end, "", 0, 0);
        }
        catch (Exception & e)
        {
            if (e.code() == ErrorCodes::SYNTAX_ERROR)
                throws = true;
        }

        ASSERT_TRUE(throws);
    }
};


TEST_F(StatsQueryTest, SimpleStatsQueryParsers)
{
    checkSuccessParse<ASTShowStatsQuery>(
        "SHOW TABLE_STATS t1",
        {
            .id = "SHOW TABLE_STATS t1",
            .type = ASTType::ASTShowStatsQuery,
            .kind = StatsQueryKind::TABLE_STATS,
            .target_all = false,
            .table = "t1",
        });

    checkSuccessParse<ASTDropStatsQuery>(
        "DROP COLUMN_STATS t1 (xx)",
        {
            .id = "DROP COLUMN_STATS t1 xx",
            .type = ASTType::ASTDropStatsQuery,
            .kind = StatsQueryKind::COLUMN_STATS,
            .target_all = false,
            .table = "t1",
            .columns = {"xx"},
            .formatted_sql = "DROP COLUMN_STATS t1 (xx)",
        });

    checkSuccessParse<ASTDropStatsQuery>(
        "DROP TABLE_STATS db1.t1",
        {
            .id = "DROP TABLE_STATS db1 t1",
            .type = ASTType::ASTDropStatsQuery,
            .kind = StatsQueryKind::TABLE_STATS,
            .target_all = false,
            .database = "db1",
            .table = "t1",
        });

    checkSuccessParse<ASTShowStatsQuery>(
        "SHOW STATS db1.t1 (gg, kk)",
        {
            .id = "SHOW STATS db1 t1 gg kk",
            .type = ASTType::ASTShowStatsQuery,
            .kind = StatsQueryKind::ALL_STATS,
            .target_all = false,
            .database = "db1",
            .table = "t1",
            .columns = {"gg", "kk"},
            .formatted_sql = "SHOW STATS db1.t1 (gg, kk)",
        });

    checkSuccessParse<ASTShowStatsQuery>(
        "SHOW COLUMN_STATS ALL",
        {
            .id = "SHOW COLUMN_STATS",
            .type = ASTType::ASTShowStatsQuery,
            .kind = StatsQueryKind::COLUMN_STATS,
            .target_all = true,
        });

    checkSuccessParse<ASTDropStatsQuery>(
        "DROP COLUMN_STATS t1 ON CLUSTER c1",
        {
            .id = "DROP COLUMN_STATS t1",
            .type = ASTType::ASTDropStatsQuery,
            .kind = StatsQueryKind::COLUMN_STATS,
            .table = "t1",
            .cluster = "c1",
        });

    checkSuccessParse<ASTShowStatsQuery>(
        "SHOW COLUMN_STATS ALL ON CLUSTER c2",
        {
            .id = "SHOW COLUMN_STATS",
            .type = ASTType::ASTShowStatsQuery,
            .kind = StatsQueryKind::COLUMN_STATS,
            .target_all = true,
            .cluster = "c2",
        });

    checkFailParse("SHOW TABLE_STATS tt.");
    checkFailParse("SHOW TABLE_STATS tt.123");
    checkFailParse("SHOW TABLE_STATS IF NOT EXISTS tt");
    checkFailParse("SHOW TABLE_STATS tt ON");
    checkFailParse("SHOW COLUMN_STATS a, b, c");
    checkFailParse("SHOW COLUMN_STATS (a, b, c)");
    checkFailParse("DROP TABLE_STATS ALL (oo)");
    checkFailParse("DROP TABLE_STATS ALL (oo, ww)");
    checkFailParse("DROP TABLE_STATS ALL (oo, ww)");
}

TEST_F(StatsQueryTest, SimpleStatsASTClone)
{
    std::shared_ptr<ASTShowStatsQuery> ptr1 = std::make_shared<ASTShowStatsQuery>();
    ptr1->database = "db";
    ptr1->table = "table";
    ptr1->kind = StatsQueryKind::COLUMN_STATS;
    ptr1->target_all = true;
    ptr1->cluster = "";

    ASTPtr cloned = ptr1->clone();
    std::shared_ptr<ASTShowStatsQuery> ptr2 = std::dynamic_pointer_cast<ASTShowStatsQuery>(cloned);
    ASSERT_TRUE(ptr2);
    EXPECT_TRUE(ptr1 != ptr2);
    EXPECT_TRUE(ptr1->database == ptr2->database);
    EXPECT_TRUE(ptr1->table == ptr2->table);
    EXPECT_TRUE(ptr1->kind == ptr2->kind);
    EXPECT_TRUE(ptr1->target_all == ptr2->target_all);
    EXPECT_TRUE(ptr1->cluster == ptr2->cluster);
}

TEST_F(StatsQueryTest, SimpleStatsASTRewrittenWithoutOnCluster)
{
    std::shared_ptr<ASTShowStatsQuery> ptr1 = std::make_shared<ASTShowStatsQuery>();
    ptr1->database = "";
    ptr1->table = "table";
    ptr1->kind = StatsQueryKind::TABLE_STATS;
    ptr1->target_all = false;
    ptr1->cluster = "CC2";

    ASTPtr rewritten = ptr1->getRewrittenASTWithoutOnCluster("new_db");
    std::shared_ptr<ASTShowStatsQuery> ptr2 = std::dynamic_pointer_cast<ASTShowStatsQuery>(rewritten);
    ASSERT_TRUE(ptr2);
    EXPECT_TRUE(ptr1 != ptr2);
    EXPECT_TRUE(ptr2->database == "new_db");
    EXPECT_TRUE(ptr1->table == ptr2->table);
    EXPECT_TRUE(ptr1->kind == ptr2->kind);
    EXPECT_TRUE(ptr1->target_all == ptr2->target_all);
    EXPECT_TRUE(ptr2->cluster.empty());
}

TEST_F(StatsQueryTest, CreateStatsQueryParsers)
{
    checkCreateStatsQuerySuccessParse(
        "CREATE STATS t1",
        CreateStatsASTParseResult{
            .id = "CREATE STATS t1",
            .kind = StatsQueryKind::ALL_STATS,
            .table = "t1",
        });

    checkCreateStatsQuerySuccessParse(
        "CREATE STATS IF NOT EXISTS ALL",
        CreateStatsASTParseResult{
            .id = "CREATE STATS ifNotExists",
            .kind = StatsQueryKind::ALL_STATS,
            .target_all = true,
            .formatted_sql = "CREATE STATS IF NOT EXISTS ALL",
            .if_not_exists = true});

    checkCreateStatsQuerySuccessParse(
        "CREATE STATS IF NOT EXISTS ss.kk",
        CreateStatsASTParseResult{
            .id = "CREATE STATS ifNotExists ss kk",
            .kind = StatsQueryKind::ALL_STATS,
            .database = "ss",
            .table = "kk",
            .formatted_sql = "CREATE STATS IF NOT EXISTS ss.kk",
            .if_not_exists = true});

    checkCreateStatsQuerySuccessParse(
        "CREATE STATS ALL",
        CreateStatsASTParseResult{
            .id = "CREATE STATS",
            .kind = StatsQueryKind::ALL_STATS,
            .target_all = true,
            .formatted_sql = "CREATE STATS ALL"});

    checkCreateStatsQuerySuccessParse(
        "CREATE STATS `ALL`",
        CreateStatsASTParseResult{
            .id = "CREATE STATS ALL",
            .kind = StatsQueryKind::ALL_STATS,
            .table = "ALL",
            .formatted_sql = "CREATE STATS ALL"});

    checkCreateStatsQuerySuccessParse(
        "CREATE column_stats t1 On cluster c1 Partition Id 'xxx'",
        CreateStatsASTParseResult{
            .id = "CREATE COLUMN_STATS t1",
            .kind = StatsQueryKind::COLUMN_STATS,
            .partition_id = "xxx",
            .table = "t1",
            .cluster = "c1",
            .formatted_sql = "CREATE COLUMN_STATS t1 ON CLUSTER c1 PARTITION ID 'xxx'"});

    checkCreateStatsQuerySuccessParse(
        "CREATE column_stats if not exists t1 On cluster c1 Partition Id 'xxx'",
        CreateStatsASTParseResult{
            .id = "CREATE COLUMN_STATS ifNotExists t1",
            .kind = StatsQueryKind::COLUMN_STATS,
            .partition_id = "xxx",
            .table = "t1",
            .cluster = "c1",
            .formatted_sql = "CREATE COLUMN_STATS IF NOT EXISTS t1 ON CLUSTER c1 PARTITION ID 'xxx'",
            .if_not_exists = true});

    checkCreateStatsQuerySuccessParse(
        "CREATE TABLE_STATS ALL WITH SAMPLE 10 ROWS",
        CreateStatsASTParseResult{
            .id = "CREATE TABLE_STATS sample rows=10",
            .kind = StatsQueryKind::TABLE_STATS,
            .target_all = true,
            .sample_type = SampleType::Sample,
            .sample_rows = 10,
            .formatted_sql = "CREATE TABLE_STATS ALL WITH SAMPLE 10 ROWS"});

    checkCreateStatsQuerySuccessParse(
        "CREATE TABLE_STATS ALL WITH FULLSCAN",
        CreateStatsASTParseResult{
            .id = "CREATE TABLE_STATS fullScan",
            .kind = StatsQueryKind::TABLE_STATS,
            .target_all = true,
            .sample_type = SampleType::FullScan,
            .formatted_sql = "CREATE TABLE_STATS ALL WITH FULLSCAN"});

    checkCreateStatsQuerySuccessParse(
        "CREATE STATS db1.t1 PARTITION ID 'yyy' with sample 0.25 ratio 5 rows",
        CreateStatsASTParseResult{
            .id = "CREATE STATS db1 t1 sample rows=5 ratio=0.25",
            .kind = StatsQueryKind::ALL_STATS,
            .partition_id = "yyy",
            .sample_type = SampleType::Sample,
            .sample_rows = 5,
            .sample_ratio = 0.25,
            .database = "db1",
            .table = "t1",
            .formatted_sql = "CREATE STATS db1.t1 PARTITION ID 'yyy' WITH SAMPLE 5 ROWS 0.25 RATIO",
        });

    checkCreateStatsQuerySuccessParse(
        "CREATE COLUMN_STATS db1.t1 (xx) PARTITION ID 'yyy' with sample 5 rows",
        CreateStatsASTParseResult{
            .id = "CREATE COLUMN_STATS db1 t1 xx sample rows=5",
            .kind = StatsQueryKind::COLUMN_STATS,
            .partition_id = "yyy",
            .sample_type = SampleType::Sample,
            .sample_rows = 5,
            .database = "db1",
            .table = "t1",
            .columns = {"xx"},
            .formatted_sql = "CREATE COLUMN_STATS db1.t1 (xx) PARTITION ID 'yyy' WITH SAMPLE 5 ROWS",
        });

    checkCreateStatsQuerySuccessParse(
        "CREATE COLUMN_STATS ALL ON CLUSTER c1",
        CreateStatsASTParseResult{
            .id = "CREATE COLUMN_STATS",
            .kind = StatsQueryKind::COLUMN_STATS,
            .target_all = true,
            .cluster = "c1",
            .formatted_sql = "CREATE COLUMN_STATS ALL ON CLUSTER c1",
        });

    checkCreateStatsQuerySuccessParse(
        "CREATE STATS t1 ON CLUSTER c1 WITH SAMPLE 7 ROWS 0.25 RATIO",
        CreateStatsASTParseResult{
            .id = "CREATE STATS t1 sample rows=7 ratio=0.25",
            .kind = StatsQueryKind::ALL_STATS,
            .sample_type = SampleType::Sample,
            .sample_rows = 7,
            .sample_ratio = 0.25,
            .table = "t1",
            .cluster = "c1",
        });

    checkFailParse("CREATE STATS");
    checkFailParse("CREATE STATS IF NOT EXISTS");
    checkFailParse("CREATE STATS db1.");
    checkFailParse("CREATE STATS db1.123");
    checkFailParse("CREATE STATS ON CLUSTER c1");
    checkFailParse("CREATE STATS db1. ON CLUSTER c1");
    checkFailParse("CREATE STATS t1 WITH");
    checkFailParse("CREATE STATS t1 WITH FULLSCAN SAMPLE");
    checkFailParse("CREATE STATS t1 WITH 0.01 ROWS 100 RATIO");
    checkFailParse("CREATE STATS t1 WITH RATIO 1000");
}

TEST_F(StatsQueryTest, CreateStatsASTClone)
{
    std::shared_ptr<ASTCreateStatsQuery> ptr1 = std::make_shared<ASTCreateStatsQuery>();
    ptr1->database = "db";
    ptr1->table = "table";
    ptr1->kind = StatsQueryKind::COLUMN_STATS;
    ptr1->target_all = true;
    ptr1->cluster = "";
    ptr1->sample_type = SampleType::Sample;
    ptr1->sample_rows = 4;
    ptr1->sample_ratio = 0.01;
    ptr1->if_not_exists = true;
    ptr1->partition = std::make_shared<ASTPartition>();
    ptr1->partition->as<ASTPartition>()->id = "xxx";

    ASTPtr cloned = ptr1->clone();
    std::shared_ptr<ASTCreateStatsQuery> ptr2 = std::dynamic_pointer_cast<ASTCreateStatsQuery>(cloned);
    ASSERT_TRUE(ptr2);
    ASSERT_TRUE(ptr1 != ptr2);
    ASSERT_TRUE(ptr1->database == ptr2->database);
    ASSERT_TRUE(ptr1->table == ptr2->table);
    ASSERT_TRUE(ptr1->kind == ptr2->kind);
    ASSERT_TRUE(ptr1->target_all == ptr2->target_all);
    ASSERT_TRUE(ptr1->if_not_exists == ptr2->if_not_exists);
    ASSERT_TRUE(ptr1->cluster == ptr2->cluster);
    ASSERT_TRUE(ptr2->sample_type == SampleType::Sample);
    ASSERT_TRUE(ptr2->sample_rows == 4);
    ASSERT_TRUE(ptr2->sample_ratio == 0.01);
    ASSERT_TRUE(ptr2->partition != ptr1->partition);
    ASSERT_TRUE(ptr2->partition->as<ASTPartition>()->id == ptr1->partition->as<ASTPartition>()->id);
    ASSERT_TRUE(ptr2->children.size() == 1);
    ASSERT_TRUE(ptr2->children[0] == ptr2->partition);
}

TEST_F(StatsQueryTest, CreateStatsASTRewrittenWithoutOnCluster)
{
    std::shared_ptr<ASTCreateStatsQuery> ptr1 = std::make_shared<ASTCreateStatsQuery>();
    ptr1->database = "";
    ptr1->table = "table";
    ptr1->kind = StatsQueryKind::TABLE_STATS;
    ptr1->target_all = false;
    ptr1->cluster = "CC2";

    ASTPtr rewritten = ptr1->getRewrittenASTWithoutOnCluster("new_db");
    std::shared_ptr<ASTCreateStatsQuery> ptr2 = std::dynamic_pointer_cast<ASTCreateStatsQuery>(rewritten);
    ASSERT_TRUE(ptr2);
    ASSERT_TRUE(ptr1 != ptr2);
    ASSERT_TRUE(ptr2->database == "new_db");
    ASSERT_TRUE(ptr1->table == ptr2->table);
    ASSERT_TRUE(ptr1->kind == ptr2->kind);
    ASSERT_TRUE(ptr1->target_all == ptr2->target_all);
    ASSERT_TRUE(ptr2->cluster.empty());
}
