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

#include <Common/Config/ConfigProcessor.h>
#include <Common/ZooKeeper/ZooKeeper.h>
#include <Common/Exception.h>
#include <Common/Stopwatch.h>
#include <Storages/MergeTree/EphemeralLockInZooKeeper.h>

#include <common/scope_guard.h>

#include <iostream>


using namespace DB;

/// This test is useful for assessing the performance of acquiring block numbers in all partitions (and there
/// can be ~1000 of them). This is needed when creating a mutation entry for a ReplicatedMergeTree table.
int main(int argc, char ** argv)
try
{
    if (argc != 3)
    {
        std::cerr << "usage: " << argv[0] << " <zookeeper_config> <path_to_table>" << std::endl;
        return 3;
    }

    ConfigProcessor processor(argv[1], false, true);
    auto config = processor.loadConfig().configuration;
    String root_path = argv[2];

    zkutil::ZooKeeper zk(*config, "zookeeper", nullptr);

    String temp_path = root_path + "/temp";
    String blocks_path = root_path + "/block_numbers";

    Stopwatch total_timer;
    Stopwatch timer;

    EphemeralLocksInAllPartitions locks(blocks_path, "test_lock-", temp_path, zk);

    std::cerr << "Locked, elapsed: " << timer.elapsedSeconds() << std::endl;
    for (const auto & lock : locks.getLocks())
        std::cout << lock.partition_id << " " << lock.number << std::endl;
    timer.restart();

    locks.unlock();
    std::cerr << "Abandoned, elapsed: " << timer.elapsedSeconds() << std::endl;

    std::cerr << "Total elapsed: " << total_timer.elapsedSeconds() << std::endl;

    return 0;
}
catch (const Exception & e)
{
    std::cerr << e.what() << ", " << e.displayText() << ": " << std::endl
              << e.getStackTraceString() << std::endl;
    throw;
}
catch (Poco::Exception & e)
{
    std::cerr << "Exception: " << e.displayText() << std::endl;
    throw;
}
catch (std::exception & e)
{
    std::cerr << "std::exception: " << e.what() << std::endl;
    throw;
}
catch (...)
{
    std::cerr << "Some exception" << std::endl;
    throw;
}
