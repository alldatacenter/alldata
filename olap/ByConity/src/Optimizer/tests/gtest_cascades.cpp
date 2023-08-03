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

#include <Optimizer/tests/gtest_optimizer_test_utils.h>

#include <DataTypes/DataTypesNumber.h>
#include <Databases/DatabaseMemory.h>
#include <Databases/DatabaseOrdinary.h>
#include <IO/WriteBufferFromFileDescriptor.h>
#include <Interpreters/Context.h>
#include <Interpreters/InterpreterCreateQuery.h>
#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/PredicateUtils.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/ParserSelectQuery.h>
#include <Parsers/formatAST.h>
#include <Parsers/parseQuery.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <Storages/System/StorageSystemNumbers.h>
#include <Storages/System/StorageSystemOne.h>
#include <Common/tests/gtest_global_context.h>
#include <Common/tests/gtest_global_register.h>

#include <gtest/gtest.h>

#include <iomanip>
#include <iostream>

using namespace DB;

PlanNodePtr projection(Names columns)
{
    Assignments assignments;
    NameToType name_to_type;
    for (auto col : columns)
    {
        assignments.emplace_back(col, std::make_shared<ASTIdentifier>(col));
        name_to_type[col] = std::make_shared<DataTypeString>();
    }
    auto step = std::make_shared<ProjectionStep>(DataStream{}, assignments, name_to_type);
    return PlanNodeBase::createPlanNode(1, std::move(step));
}

PlanNodePtr join(const PlanNodePtr & left, const PlanNodePtr & right, const Names & left_keys, const Names & right_keys)
{
    DataStream output;
    for (const auto & col : left->getStep()->getOutputStream().header)
        output.header.insert(ColumnWithTypeAndName{col.type, col.name});
    for (const auto & col : right->getStep()->getOutputStream().header)
        output.header.insert(ColumnWithTypeAndName{col.type, col.name});

    auto join_step = std::make_shared<JoinStep>(
        DataStreams{left->getStep()->getOutputStream(), right->getStep()->getOutputStream()},
        output,
        ASTTableJoin::Kind::Inner,
        ASTTableJoin::Strictness::All,
        left_keys,
        right_keys,
        PredicateConst::TRUE_VALUE);
    return PlanNodeBase::createPlanNode(1, std::move(join_step), {left, right});
}

PlanNodePtr star(int table_size)
{
    PlanNodes tables;

    // table 0
    Names columns;
    for (int index = 1; index < table_size; ++index)
        columns.emplace_back("c_0." + std::to_string(index));
    tables.emplace_back(projection(columns));

    // table 1 ~ table_size
    for (int index = 1; index < table_size; ++index)
        tables.emplace_back(projection({"c_" + std::to_string(index)}));

    PlanNodePtr left = tables[0];
    for (int index = 1; index < table_size; ++index)
        left = join(left, tables[index], Names{"c_0." + std::to_string(index)}, Names{"c_" + std::to_string(index)});
    return left;
}

PlanNodePtr cyclic(int table_size)
{
    PlanNodes tables;
    for (int index = 0; index < table_size; ++index)
        tables.emplace_back(projection({"c_" + std::to_string(index)}));

    PlanNodePtr left = tables[0];
    for (int index = 1; index < table_size; ++index)
        left = join(left, tables[index], Names{"c_" + std::to_string(index - 1)}, Names{"c_" + std::to_string(index)});
    return left;
}

PlanNodePtr chain(int table_size)
{
    PlanNodes tables;
    for (int index = 0; index < table_size; ++index)
        tables.emplace_back(projection({"c_1." + std::to_string(index), "c_2." + std::to_string(index)}));

    PlanNodePtr left = tables[0];
    for (int index = 1; index < table_size; ++index)
        left = join(left, tables[index], Names{"c_1." + std::to_string(index - 1)}, Names{"c_2." + std::to_string(index)});
    return left;
}

TEST(OptimizerCascadesTest, DISABLED_Timeout)
{
    auto context = Context::createCopy(getContext().context);

    CascadesOptimizer optimizer;

    for (int table_size = 2; table_size < 11; ++table_size)
    {
        auto start = std::chrono::high_resolution_clock::now();
        std::cout << "Size " << table_size << '\n';
        QueryPlan plan = createQueryPlan(cyclic(table_size));
        optimizer.rewrite(plan, context);
        auto end = std::chrono::high_resolution_clock::now();
        auto ms_int = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        std::cout << "Used " << ms_int.count() << " ms\n\n";
    }
}
