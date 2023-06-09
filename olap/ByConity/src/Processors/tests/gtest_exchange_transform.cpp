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

#include <memory>
#include <vector>
#include <gtest/gtest.h>

#include <Columns/ColumnNullable.h>
#include <Columns/ColumnsNumber.h>
#include <Core/Block.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypesNumber.h>
#include <Functions/FunctionFactory.h>
#include <Interpreters/Context.h>
#include <Processors/Chunk.h>
#include <Processors/Exchange/BroadcastExchangeSink.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/IBroadcastReceiver.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/ExchangeDataKey.h>
#include <Processors/Exchange/LoadBalancedExchangeSink.h>
#include <Processors/Exchange/RepartitionTransform.h>
#include <Processors/Executors/PipelineExecutor.h>
#include <Processors/QueryPipeline.h>
#include <Processors/tests/gtest_processers_utils.h>
#include <Common/tests/gtest_global_context.h>

using namespace DB;
namespace UnitTest
{
TEST(RepartitionTransform, doRepartitionTest)
{
    const size_t partition_num = 6;
    const size_t rows = 100;
    Block block = createUInt64Block(rows, 10, 88);
    Block header = block.cloneEmpty();
    Chunk chunk(block.mutateColumns(), rows);
    ColumnsWithTypeAndName arguments;
    arguments.push_back(header.getByPosition(1));
    arguments.push_back(header.getByPosition(2));
    auto func = createRepartitionFunction(getContext().context, arguments);
    auto res_pair = RepartitionTransform::doRepartition(
        partition_num, chunk, header, ColumnNumbers{1, 2}, func, RepartitionTransform::REPARTITION_FUNC_RESULT_TYPE);
    auto & selector = res_pair.first;
    auto & startpoints = res_pair.second;
    ASSERT_TRUE(selector.size() == rows);
    ASSERT_TRUE(startpoints.size() == partition_num + 1);
    for (size_t i = 0; i <= partition_num; i++)
    {
        if (startpoints[i] > 0)
        {
            ASSERT_TRUE(startpoints[i] - startpoints[i-1] == rows);
            break;
        }
    }
}

TEST(RepartitionTransform, doRepartitionNullableTest)
{
    const size_t partition_num = 6;
    const size_t rows = 100;
    ColumnsWithTypeAndName cols;
    for (int i = 0; i < 3; i++)
    {
        auto nest_col = ColumnUInt64::create(rows, 88);
        auto res_null_map = ColumnUInt8::create(rows,1);
        auto column = ColumnNullable::create(std::move(nest_col), std::move(res_null_map));
        cols.emplace_back(std::move(column), std::make_shared<DataTypeNullable>(std::make_shared<DataTypeUInt64>()), "column" + std::to_string(i));
    }

    Block block {cols};

    Block header = block.cloneEmpty();
    Chunk chunk(block.mutateColumns(), rows);
    ColumnsWithTypeAndName arguments;
    arguments.push_back(header.getByPosition(1));
    arguments.push_back(header.getByPosition(2));
    auto func = createRepartitionFunction(getContext().context, arguments);
    auto res_pair = RepartitionTransform::doRepartition(
        partition_num, chunk, header, ColumnNumbers{1, 2}, func, RepartitionTransform::REPARTITION_FUNC_RESULT_TYPE);
    auto & selector = res_pair.first;
    auto & startpoints = res_pair.second;
    ASSERT_TRUE(selector.size() == rows);
    ASSERT_TRUE(startpoints.size() == partition_num + 1);
    for (size_t i = 0; i <= partition_num; i++)
    {
        if (startpoints[i] > 0)
        {
            ASSERT_TRUE(startpoints[i] - startpoints[i-1] == rows);
            break;
        }
    }
}

}
