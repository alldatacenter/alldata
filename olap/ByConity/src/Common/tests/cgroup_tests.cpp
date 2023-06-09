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

#include <iostream>

#include <gtest/gtest.h>

#include <Common/CGroup/CGroupManager.h>
#include <Common/CGroup/CGroupManagerFactory.h>
#include <Common/ThreadPool.h>

using namespace DB;

void test_cpu(std::vector<ThreadFromGlobalPool> & threads)
{
    CGroupManager & cgroup_manager = CGroupManagerFactory::instance();
    CpuControllerPtr cpu = cgroup_manager.createCpu("test", 1025);
    for (auto & thread : threads)
    {
        cpu->addTask(thread.gettid());
    }

    ASSERT_EQ(cpu->getTasks().size(), threads.size());
    ASSERT_EQ(cpu->getShare(), 1025);
}

TEST(CGroup, Flow)
{
    std::vector<ThreadFromGlobalPool> threads;
    threads.reserve(20);
    for (int i = 0; i < 20; ++i)
    {
        threads.emplace_back([](){
            std::this_thread::sleep_for(std::chrono::milliseconds(5000));
        });
    }
    test_cpu(threads);
    for (auto & thread : threads)
    {
        thread.join();
    }
}
