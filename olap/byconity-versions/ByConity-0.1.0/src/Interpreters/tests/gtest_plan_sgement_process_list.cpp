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

#include <string>
#include <thread>
#include <Interpreters/Context.h>
#include <Interpreters/DistributedStages/AddressInfo.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Columns/IColumn.h>
#include <DataTypes/DataTypeFactory.h>
#include <QueryPlan/ReadNothingStep.h>
#include <gtest/gtest.h>
#include <Poco/ConsoleChannel.h>
#include <common/scope_guard.h>
#include <Common/tests/gtest_global_context.h>
#include <Common/tests/gtest_utils.h>



using namespace DB;

namespace UnitTest
{

Block createBlock()
{
    ColumnWithTypeAndName column;
    column.name = "RES";

    DataTypePtr type = DataTypeFactory::instance().get("UInt8");
    column.column = type->createColumnConst(1, Field(1));
    column.type = type;

    ColumnsWithTypeAndName columns;
    columns.push_back(column);

    return Block(columns);
}

QueryPlan generateEmptyPlan()
{
    QueryPlan plan;

    Block block = createBlock();
    auto step = std::make_unique<ReadNothingStep>(block);
    plan.addStep(std::move(step));

    return plan;
}

TEST(PlanSegmentProcessList, InsertTest)
{
    initLogger();
    const auto & context = getContext().context;
    context->setProcessListEntry(nullptr);
    auto & client_info = context->getClientInfo();
    auto & plan_segment_processlist = context->getPlanSegmentProcessList();
    PlanSegment plan_segment = PlanSegment();
    plan_segment.setQueryId("PlanSegmentProcessList_test");
    plan_segment.setPlanSegmentId(0);
    plan_segment.setQueryPlan(generateEmptyPlan());

    client_info.current_query_id = plan_segment.getQueryId() + std::to_string(plan_segment.getPlanSegmentId());
    client_info.current_user = "test";
    client_info.initial_query_id = plan_segment.getQueryId();
    AddressInfo coordinator_address("localhost", 8888, "test", "123456", 9999, 6666);
    plan_segment.setCoordinatorAddress(coordinator_address);
    plan_segment_processlist.insert(plan_segment, context);
}

TEST(PlanSegmentProcessList, InsertReplaceSuccessTest)
{
    initLogger();
    const auto & context = getContext().context;
    context->setProcessListEntry(nullptr);
    auto & client_info = context->getClientInfo();
    auto & plan_segment_processlist = context->getPlanSegmentProcessList();
    PlanSegment plan_segment = PlanSegment();
    plan_segment.setQueryId("PlanSegmentProcessList_test");
    plan_segment.setPlanSegmentId(0);
    plan_segment.setQueryPlan(generateEmptyPlan());

    client_info.current_query_id = plan_segment.getQueryId() + std::to_string(plan_segment.getPlanSegmentId());
    client_info.current_user = "test";
    client_info.initial_query_id = plan_segment.getQueryId();
    AddressInfo coordinator_address("localhost", 8888, "test", "123456", 9999, 6666);
    plan_segment.setCoordinatorAddress(coordinator_address);
    auto plan_segment_process_entry = plan_segment_processlist.insert(plan_segment, context);
    auto async_func = [to_release_entry = std::move(plan_segment_process_entry)]() {
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
        to_release_entry.get();
    };
    std::thread thread(std::move(async_func));
    SCOPE_EXIT({
        if (thread.joinable())
            thread.join();
    });
    plan_segment.setCoordinatorAddress(AddressInfo("localhost", 8888, "test", "123456", 6666, 9999));
    plan_segment_processlist.insert(plan_segment, context, true);
}

TEST(PlanSegmentProcessList, InsertReplaceTimeoutTest)
{
    const auto & context = getContext().context;
    context->setProcessListEntry(nullptr);
    auto & client_info = context->getClientInfo();
    auto & plan_segment_processlist = context->getPlanSegmentProcessList();
    PlanSegment plan_segment = PlanSegment();
    plan_segment.setQueryId("PlanSegmentProcessList_test");
    plan_segment.setPlanSegmentId(0);
    plan_segment.setQueryPlan(generateEmptyPlan());

    client_info.current_query_id = plan_segment.getQueryId() + std::to_string(plan_segment.getPlanSegmentId());
    client_info.current_user = "test";
    client_info.initial_query_id = plan_segment.getQueryId();
    AddressInfo coordinator_address("localhost", 8888, "test", "123456", 9999, 6666);
    plan_segment.setCoordinatorAddress(coordinator_address);
    auto plan_segment_process_entry = plan_segment_processlist.insert(plan_segment, context);

    auto async_func = [&, to_release_entry = std::move(plan_segment_process_entry)]() {
        std::this_thread::sleep_for(
            std::chrono::milliseconds(context->getSettingsRef().replace_running_query_max_wait_ms.totalMilliseconds() + 500));
        to_release_entry.get();
    };
    std::thread thread(std::move(async_func));
    SCOPE_EXIT({
        if (thread.joinable())
            thread.join();
    });

    plan_segment.setCoordinatorAddress(AddressInfo("localhost", 8888, "test", "123456", 6666, 9999));
    ASSERT_THROW(plan_segment_processlist.insert(plan_segment, context, true), DB::Exception);
}

}
