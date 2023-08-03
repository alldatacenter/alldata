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

#include <gtest/gtest.h>
#include <TSO/TSOImpl.h>


using namespace DB::TSO;
using TSOServicePtr = std::shared_ptr<TSOImpl>;

TEST(TSOImpl, testSetGetClock)
{
    size_t physical_time_expected = 13;
    size_t logical_time_expected = 0;
    TSOServicePtr tso_service = std::make_shared<TSOImpl>();

    tso_service->setPhysicalTime(physical_time_expected);
    TSOClock cur_ts_actual = tso_service->getClock();

    EXPECT_EQ(cur_ts_actual.physical , physical_time_expected);
    EXPECT_EQ(cur_ts_actual.logical, logical_time_expected);
}
