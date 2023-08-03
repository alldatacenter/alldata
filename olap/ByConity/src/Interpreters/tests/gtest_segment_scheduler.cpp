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

#include <Core/Types.h>
#include <Core/Block.h>
#include <Interpreters/Context.h>
#include <Interpreters/Cluster.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/DistributedStages/AddressInfo.h>
#include <Interpreters/CancellationCode.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Parsers/IAST_fwd.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <Common/Stopwatch.h>
#include <Interpreters/SegmentScheduler.h>
#include <Poco/Util/XMLConfiguration.h>
#include <Common/ZooKeeper/ZooKeeperNodeCache.h>
#include <Common/ZooKeeper/KeeperException.h>
#include <Common/Config/ConfigProcessor.h>
#include <gtest/gtest.h>

namespace DB
{
class MockSegmentScheduler : public SegmentScheduler
{
public:
    MockSegmentScheduler() = default;
    virtual ~MockSegmentScheduler() override {}

    virtual AddressInfos sendPlanSegment(PlanSegment *, bool, ContextPtr, std::shared_ptr<DAGGraph>, std::vector<size_t>) override
    {
        AddressInfos res;
        AddressInfo address("127.0.0.1", 9000, "test", "test", 9001, 9002);
        res.emplace_back(std::move(address));
        std::cerr << "call sendPlanSegment!" << std::endl;
        return res;
    }
};
}
