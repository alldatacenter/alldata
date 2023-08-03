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

#include <Interpreters/DistributedStages/PlanSegment.h>
#include <QueryPlan/QueryPlan.h>
#include <IO/WriteBufferFromString.h>
#include <IO/ReadBufferFromString.h>

#include <gtest/gtest.h>

using namespace DB;

PlanSegmentPtr createPlanSegment()
{
    PlanSegmentPtr plan_segment = std::make_unique<PlanSegment>();

    PlanSegmentInputPtr left = std::make_shared<PlanSegmentInput>(PlanSegmentType::EXCHANGE);
    PlanSegmentInputPtr right = std::make_shared<PlanSegmentInput>(PlanSegmentType::EXCHANGE);
    PlanSegmentOutputPtr output = std::make_shared<PlanSegmentOutput>(PlanSegmentType::OUTPUT);

    plan_segment->appendPlanSegmentInput(left);
    plan_segment->appendPlanSegmentInput(right);
    plan_segment->setPlanSegmentOutput(output);

    QueryPlan query_plan;
    plan_segment->setQueryPlan(std::move(query_plan));

    return plan_segment;
}

TEST(PlanSegmentTest, PlanSegmentSerDer)
{
    PlanSegmentPtr plan_segment = createPlanSegment();

    /**
     * serialize to buffer
     */
    WriteBufferFromOwnString write_buffer;
    plan_segment->serialize(write_buffer);

    /**
     * deserialize from buffer
     */
    ReadBufferFromString read_buffer(write_buffer.str());
    PlanSegmentPtr new_plan_segment = std::make_unique<PlanSegment>();
    new_plan_segment->deserialize(read_buffer);
}
