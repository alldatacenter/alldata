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

#include <Optimizer/tests/gtest_base_plan_test.h>
#include <Optimizer/tests/test_config.h>

namespace DB
{

/**
 * change resource path in test.config.h.in.
 */
class BaseDumpPlanTest : public AbstractPlanTestSuite
{
public:
    explicit BaseDumpPlanTest(
        bool enable_statistics_ = true,
        const std::unordered_map<String, Field> & settings = {})
        :
        AbstractPlanTestSuite("test_dump", settings),enable_statistics(enable_statistics_)
    {
        createTables();
        dropTableStatistics();
        if (enable_statistics)
            loadTableStatistics();
    }

    std::vector<std::filesystem::path> getTableDDLFiles() override { return {std::filesystem::path(PLAN_DUMP_PATH) / "create_table.sql"}; }
    std::filesystem::path getStatisticsFile() override { return std::filesystem::path(PLAN_DUMP_PATH) / "test_dump.bin"; }
    std::filesystem::path getQueriesDir() override { return std::filesystem::path(PLAN_DUMP_PATH) / "queries"; }
    std::filesystem::path getExpectedExplainDir() override { return std::filesystem::path(PLAN_DUMP_PATH) / ""; }

    bool enable_statistics;
};

}
