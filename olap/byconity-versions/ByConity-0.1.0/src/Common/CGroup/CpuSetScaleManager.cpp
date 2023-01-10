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

#include <Common/CGroup/CpuSetScaleManager.h>
#include <Common/SystemUtils.h>
#include <Common/CGroup/CGroupManagerFactory.h>
#include <unordered_set>
#include <numa.h>


namespace DB
{

void CpuSetScaleOperator::scale()
{
    /// build cpu numa map
    auto build_numa_map = [](std::vector<int16_t> & cpu_numa_map, size_t numa_node, size_t cpus_num){
        cpu_numa_map.resize(cpus_num);
        for (size_t i=0; i < numa_node+1; ++i)
        {
            bitmask *cpus_bitmask = numa_allocate_cpumask();
            numa_node_to_cpus(i, cpus_bitmask);
            for (size_t j = 0; j < cpus_bitmask->size; ++j)
                if (numa_bitmask_isbitset(cpus_bitmask, j))
                    cpu_numa_map[j] = i;
            numa_free_cpumask(cpus_bitmask);
        }
    };

    /// split cpus to numa cpus
    auto split_cpu_to_numa = [](std::vector<std::vector<size_t>> & cpu_set_cpus, size_t numa_node,
                                const Cpus & cpus, const std::vector<int16_t> & cpu_numa_map){
        cpu_set_cpus.resize(numa_node+1);
        for (size_t i = 0; i < cpus.data.size(); ++i)
        {
            if (cpus.data[i])
            {
                int16_t numa = cpu_numa_map[i];
                cpu_set_cpus[numa].push_back(i);
            }
        }
    };

    /// merge numa cpus to cpus
    auto merge_numa_to_cpus = [](const std::vector<std::vector<size_t>> & cpu_set_cpus, size_t cpus_num) {
        std::vector<bool> data;
        data.resize(cpus_num, false);
        for (const auto & cpu_set_cpu : cpu_set_cpus)
        {
            for (auto cpu : cpu_set_cpu)
                data[cpu] = true;
        }
        Cpus new_cpus(data);
        return new_cpus;
    };

    if (current_cpu_usage <= low_water_level)
    {
        const Cpus & cpus = cpu_set->getCpus();
        size_t numa_node = SystemUtils::getMaxNumaNode();
        size_t cpus_num = SystemUtils::getSystemCpuNum();

        /// build cpu numa map
        std::vector<int16_t> cpu_numa_map;
        build_numa_map(cpu_numa_map, numa_node, cpus_num);

//        std::cout << "numa map: [";
//        for (short ele : cpu_numa_map)
//        {
//            std::cout << ele << ", ";
//        }
//        std::cout << "]" << std::endl;

        /// split cpus to numa cpus
        std::vector<std::vector<size_t>> cpu_set_cpus;
        split_cpu_to_numa(cpu_set_cpus, numa_node, cpus, cpu_numa_map);

//        std::cout << "cpu set cpus: " << std::endl;
//        for (const auto & ele : cpu_set_cpus)
//        {
//            std::cout << "[";
//            for (size_t i : ele)
//                std::cout << i << ", ";
//            std::cout << std::endl;
//        }

        /// choose a cpu scale down
        size_t max_cpu_numa = 0;
        size_t max_cpu_num = 0;
        size_t sum_cpu_num = 0;
        for (size_t i = 0; i < cpu_set_cpus.size(); ++i)
        {
            sum_cpu_num += cpu_set_cpus[i].size();
            if (cpu_set_cpus[i].size() > max_cpu_num)
            {
                max_cpu_num = cpu_set_cpus[i].size();
                max_cpu_numa = i;
            }
        }
        // cpuset has at least 1 cpu
        if (max_cpu_num == 0 || sum_cpu_num == 1)
            return;
        size_t cpu = cpu_set_cpus[max_cpu_numa].back();
        cpu_set_cpus[max_cpu_numa].pop_back();


        LOG_INFO(log, "scale down cpu " + toString(cpu) + " for cpuset: " + cpu_set->getName() + " current usage: " + toString(current_cpu_usage));
        /// merge numa cpus to cpus
        const Cpus & new_cpus = merge_numa_to_cpus(cpu_set_cpus, cpus_num);
        CGroupManagerFactory::instance().scaleCpuSet(*cpu_set, new_cpus);
    }
    else if (current_cpu_usage >= high_water_level)
    {
        const Cpus & cpus = cpu_set->getCpus();
        size_t numa_node = SystemUtils::getMaxNumaNode();
        size_t cpus_num = SystemUtils::getSystemCpuNum();

        /// build cpu numa map
        std::vector<int16_t> cpu_numa_map;
        build_numa_map(cpu_numa_map, numa_node, cpus_num);

        /// split cpus to numa cpus
        // current cpu
        std::vector<std::vector<size_t>> cpu_set_cpus;
        split_cpu_to_numa(cpu_set_cpus, numa_node, cpus, cpu_numa_map);

        // max cpu
        std::vector<std::vector<size_t>> max_cpu_set_cpus;
        split_cpu_to_numa(max_cpu_set_cpus, numa_node, max_cpus, cpu_numa_map);



        /// choose a cpu scale up
        /// choose numa should scale up
        size_t scale_up_cpu_numa = 0;
        size_t max_can_scale_cpu_num = 0;
        for (size_t i = 0; i < cpu_set_cpus.size(); ++i)
        {
            if (max_cpu_set_cpus[i].size() - cpu_set_cpus[i].size() > max_can_scale_cpu_num)
            {
                max_can_scale_cpu_num = max_cpu_set_cpus[i].size() - cpu_set_cpus[i].size();
                scale_up_cpu_numa = i;
            }
        }

        /// choose a cpu bound on the numa to scale up
        size_t cpu = -1;
        for (size_t i = 0; i < cpus_num; ++i)
        {
            if (static_cast<size_t>(cpu_numa_map[i]) == scale_up_cpu_numa
                && !cpus.data[i] && max_cpus.data[i])
            {
                cpu_set_cpus[scale_up_cpu_numa].push_back(i);
                cpu = i;
                break;
            }
        }

        LOG_INFO(log, "scale up " + toString(cpu) + " for cpuset: " + cpu_set->getName() + " current usage: " + toString(current_cpu_usage));

        /// merge numa cpus to cpus
        const Cpus & new_cpus = merge_numa_to_cpus(cpu_set_cpus, cpus_num);
        CGroupManagerFactory::instance().scaleCpuSet(*cpu_set, new_cpus);
    }
}


void CpuSetScaleOperator::sample()
{
    const Cpus & cpus = cpu_set->getCpus();
    std::vector<CpuUsageInfo> cpu_usage_vec;
    std::unordered_set<size_t> cpu_nodes;
    for (size_t i=0; i < cpus.data.size(); ++i)
        if (cpus.data[i])
            cpu_nodes.insert(i);
    SystemUtils::getCpuUsageInfo(cpu_nodes, cpu_usage_vec);
    size_t total1 = 0;
    size_t idle1 = 0;
    for (auto & cpu_usage : cpu_usage_vec)
    {
        total1 += cpu_usage.total();
        idle1 += cpu_usage.idle;
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(100));

    SystemUtils::getCpuUsageInfo(cpu_nodes, cpu_usage_vec);
    size_t total2 = 0;
    size_t idle2 = 0;
    for (auto & cpu_usage : cpu_usage_vec)
    {
        total2 += cpu_usage.total();
        idle2 += cpu_usage.idle;
    }

    size_t total = total2-total1;
    size_t idle = idle2-idle1;
    if (total == 0)
    {
        LOG_WARNING(log, "sample cpu usage failed");
        return;
    }
    float usage = (total-idle)/total * 100;

    current_cpu_usage = (current_cpu_usage * sample_cnt+usage)/(sample_cnt+1);
    sample_cnt += 1;
}

CpuSetScaleOperator::CpuSetScaleOperator(CpuSetScaleOperator && op) noexcept
{
    cpu_set = std::move(op.cpu_set);
    max_cpus = std::move(op.max_cpus);
    high_water_level = op.high_water_level;
    low_water_level = op.low_water_level;
    current_cpu_usage = op.current_cpu_usage;
    sample_cnt = op.sample_cnt;
}

void CpuSetScaleManager::registerCpuSet(CpuSetPtr cpu_set_, float high_water_level_, float low_water_level_, Cpus max_cpus_)
{
    cpu_set_scale_operators.emplace_back(cpu_set_, high_water_level_, low_water_level_, max_cpus_);
}

void CpuSetScaleManager::run()
{
    task->scheduleAfter(interval);
}

void CpuSetScaleManager::thread_func()
{
    for (auto & op : cpu_set_scale_operators)
    {
        op.sample();
        if (op.get_sample_cnt() >= scale_sample_cnt)
        {
            op.scale();
            op.reset();
        }
    }
    task->scheduleAfter(interval);
}
void CpuSetScaleManager::loadCpuSetFromConfig(const Poco::Util::AbstractConfiguration & config)
{
    using Keys = Poco::Util::AbstractConfiguration::Keys;
    Keys keys;
    config.keys("cpu_set_scale", keys);
    for (const String & key : keys)
    {
        Keys cpu_set_scale_keys;
        auto & cgroup_manager = CGroupManagerFactory::instance();
        String cpu_set_name = config.getString("cpu_set_scale."+key+".cpu_set");
        if (!cgroup_manager.isInit() || cgroup_manager.getCpuSet(cpu_set_name) == nullptr)
        {
            LOG_WARNING(log, "not found cpu set for: " + cpu_set_name);
            continue;
        }
        CpuSetPtr cpu_set = cgroup_manager.getCpuSet(cpu_set_name);
        double high_water_level = config.getDouble("cpu_set_scale."+key+".high_water_level");
        double low_water_level = config.getDouble("cpu_set_scale."+key+".low_water_level");
        registerCpuSet(cpu_set, high_water_level, low_water_level, cpu_set->getCpus());
    }
}

}
