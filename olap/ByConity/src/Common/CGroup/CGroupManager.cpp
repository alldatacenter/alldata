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


#include <filesystem>
#include <fstream>
#include <Common/CGroup/CGroupManager.h>
#include <Common/SystemUtils.h>
#include <Common/Exception.h>
#include <regex>
#include <Poco/String.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int CPU_ALLOC_INVALIDATE;
    extern const int CREATE_CGROUP_DIRECTORY_FAILED;
}

const String CGroupManager::CGROUP_ROOT_PATH = "/sys/fs/cgroup";
const String CGroupManager::CGROUP_CPU_SET_PATH = CGROUP_ROOT_PATH + "/cpuset/tiger";
const String CGroupManager::CGROUP_CPU_PATH = CGROUP_ROOT_PATH + "/cpu/tiger";
const String CGroupManager::SYSTEM = "system";

static String getDefaultMems()
{
    size_t numa_node = SystemUtils::getMaxNumaNode();
    std::stringstream mems_ss;
    mems_ss << "0";
    if (numa_node > 0)
        mems_ss << "-" << numa_node;
    mems_ss << "\n";
    return mems_ss.str();
}

CpuSetPtr CGroupManager::getCpuSet(const String & cpu_set_name)
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    if (cpu_set_map.count(cpu_set_name))
        return cpu_set_map.find(cpu_set_name)->second;
    return nullptr;
}

CGroupManager::CGroupManager(PassKey ) {}

CpuSetPtr CGroupManager::createCpuSet(const String & cpu_set_name, const Cpus & cpus)
{
    if (!enableForCpu())
        return nullptr;
    if (cpu_set_map.count(cpu_set_name))
        return cpu_set_map.find(cpu_set_name)->second;

    std::lock_guard<std::recursive_mutex> lock(mutex);
    if (cpu_set_map.count(cpu_set_name))
        return cpu_set_map.find(cpu_set_name)->second;

    std::filesystem::path cpu_set_root_path(getClickhouseCpuSetPath());
    String cpu_set_path = (cpu_set_root_path/cpu_set_name).string();
    if (cpu_set_name != SYSTEM)
        alloc(cpus);
    cpu_set_map[cpu_set_name] = std::make_shared<CpuSet>(CpuSet::PassKey(), cpu_set_name, cpu_set_path, cpus, true);
    return cpu_set_map.find(cpu_set_name)->second;
}

CpuSetPtr CGroupManager::createCpuSet(const String & cpu_set_name, const String & cpus)
{
    Cpus c(cpus);
    return createCpuSet(cpu_set_name, c);
}

void CGroupManager::destroyCpuSet(const String & cpu_set_name)
{
    if (!enable())
        return;
    if (!cpu_set_map.count(cpu_set_name))
        return;
    std::lock_guard<std::recursive_mutex> lock(mutex);
    if (!cpu_set_map.count(cpu_set_name))
        return;
    auto it = cpu_set_map.find(cpu_set_name);
    auto & cpu_set = it->second;
    if (cpu_set_name != SYSTEM)
    {
        CpuSetPtr system_cpu_set = getCpuSet(SYSTEM);
        // TODO: how make move tasks thread safe(maybe file lock?)
        const auto & tasks = cpu_set->getTasks();
        system_cpu_set->addTasks(tasks);
        free(cpu_set->getCpus());
    }
    cpu_set_map.erase(it);
}

void CGroupManager::destroyCpuSet(const CpuSet & cpu_set)
{
    destroyCpuSet(cpu_set.name);
}

void CGroupManager::free(const Cpus & cpus)
{

    CpuSetPtr system_cpu_set = getCpuSet(SYSTEM);
    auto & system_cpus = system_cpu_set->cpus;
    if (system_cpus.data.size() != SystemUtils::getSystemCpuNum()
        && system_cpus.data.size() != cpus.data.size())
        throw Exception("free cpu failed, host cpu num: " + std::to_string(SystemUtils::getSystemCpuNum())
                        + " system cpu size: " + std::to_string(system_cpus.data.size())
                        + " alloc cpu size: " + std::to_string(cpus.data.size()), ErrorCodes::CPU_ALLOC_INVALIDATE);
    for (size_t i = 0; i < system_cpus.data.size(); ++i)
        system_cpus.data[i] = system_cpus.data[i]|cpus.data[i];
    system_cpu_set->resetCpuSet();
}

void CGroupManager::alloc(const Cpus & cpus)
{
    CpuSetPtr system_cpu_set = getCpuSet(SYSTEM);
    auto & system_cpus = system_cpu_set->cpus;
    if (system_cpus.data.size() != SystemUtils::getSystemCpuNum()
        && system_cpus.data.size() != cpus.data.size())
        throw Exception("alloc cpu failed, host cpu num: " + std::to_string(SystemUtils::getSystemCpuNum())
                            + " system cpu size: " + std::to_string(system_cpus.data.size())
                            + " alloc cpu size: " + std::to_string(cpus.data.size()), ErrorCodes::CPU_ALLOC_INVALIDATE);

    std::vector<size_t> indexes;
    for (size_t i = 0; i < system_cpus.data.size(); ++i)
    {
        if (cpus.data[i] && !system_cpus.data[i])
            throw Exception("", ErrorCodes::CPU_ALLOC_INVALIDATE);
        else if (cpus.data[i])
            indexes.push_back(i);
    }

    for (auto & index : indexes)
        system_cpus.data[index] = false;
    system_cpu_set->resetCpuSet();
}
void CGroupManager::initCpuSetRoot()
{
    std::vector<bool> data(SystemUtils::getSystemCpuNum(), true);
    Cpus cpus(data);
    SystemUtils::writeStringToFile(getCGroupCpuSetPath() + "/" + CpuSet::CPUS, cpus.toString() + "\n", true);
    SystemUtils::writeStringToFile(getCGroupCpuSetPath() + "/" + CpuSet::MEMS, getDefaultMems(), true);
    SystemUtils::writeStringToFile(getCGroupCpuSetPath() + "/" + CpuSet::CPU_EXCLUSIVE, std::to_string(true) + "\n", true);
}
void CGroupManager::initClickhouseCpuSet()
{
    if (std::filesystem::exists(getClickhouseCpuSetPath()))
    {
        CpuSet root_cpu_set(getCGroupCpuSetPath(), getCGroupCpuSetPath());
        std::filesystem::recursive_directory_iterator dir_it(getClickhouseCpuSetPath());
        for (const auto & p : dir_it)
        {
            if (std::filesystem::is_directory(p))
            {
                CpuSet cpu_set(p.path(), p.path());
                const auto & tasks = cpu_set.getTasks();
                root_cpu_set.addTasks(tasks);
            }
        }

        SystemUtils::rmdirAll(getClickhouseCpuSetPath().c_str());
    }
    std::error_code error_code;
    bool res = std::filesystem::create_directory(getClickhouseCpuSetPath(), error_code);
    if (!res)
        throw Exception("create cgroup directory " + getClickhouseCpuSetPath() + " failed, filesystem error code: " + std::to_string(error_code.value()), ErrorCodes::CREATE_CGROUP_DIRECTORY_FAILED);
    std::vector<bool> data(SystemUtils::getSystemCpuNum(), true);
    Cpus cpus(data);
    SystemUtils::writeStringToFile(getClickhouseCpuSetPath() + "/" + CpuSet::CPUS, cpus.toString() + "\n", true);
    SystemUtils::writeStringToFile(getClickhouseCpuSetPath() + "/" + CpuSet::MEMS,  getDefaultMems(), true);
    SystemUtils::writeStringToFile(getClickhouseCpuSetPath() + "/" + CpuSet::CPU_EXCLUSIVE, std::to_string(true) + "\n", true);
}

void CGroupManager::moveClickhouseProc()
{
    auto pid = getpid();
    std::stringstream ss;
    ss << pid;
    CpuSetPtr system_cpu_set = getCpuSet(SYSTEM);
    SystemUtils::writeStringToFile(system_cpu_set->dir_path + "/" + CpuSet::PROC, ss.str());
}

void CGroupManager::scaleCpuSet(CpuSet & cpu_set, const Cpus & cpus)
{
    Cpus & current_cpus = cpu_set.cpus;
    std::vector<bool> scale_up;
    std::vector<bool> scale_down;
    for (size_t i = 0; i < current_cpus.data.size(); ++i)
    {
        if (cpus.data[i] && !current_cpus.data[i])
        {
            if (scale_up.empty())
                scale_up.resize(current_cpus.data.size(), false);
            scale_up[i] = true;
        }
        else if (!cpus.data[i] && current_cpus.data[i])
        {
            if (scale_down.empty())
                scale_down.resize(current_cpus.data.size(), false);
            scale_down[i] = true;
        }
    }

    /// 1. alloc cpu from system
    if (!scale_up.empty())
    {
        Cpus scale_up_cpus(scale_up);
        alloc(scale_up_cpus);
    }

    /// 2. set current cpu
    cpu_set.cpus = cpus;
    cpu_set.resetCpuSet();

    /// 3. free cpu to system
    if (!scale_down.empty())
    {
        Cpus scale_down_cpus(scale_down);
        free(scale_down_cpus);
    }

}
void CGroupManager::init()
{
    if (enable())
    {
        initCpuSetRoot();
        initClickhouseCpuSet();
        std::vector<bool> data(SystemUtils::getSystemCpuNum(), true);
        Cpus cpus(data);
        createCpuSet(SYSTEM, cpus);
        moveClickhouseProc();
        init_flag = true;
    }
}
CpuControllerPtr CGroupManager::createCpu(const String & cpu_name, const UInt64 share)
{
    if (!enable())
        return nullptr;
    if (cpu_map.count(cpu_name))
        return cpu_map.find(cpu_name)->second;

    std::lock_guard<std::recursive_mutex> lock(mutex);
    if (cpu_map.count(cpu_name))
        return cpu_map.find(cpu_name)->second;

    std::filesystem::path cpu_root_path(getClickhouseCpuPath());
    String cpu_path = (cpu_root_path/cpu_name).string();
    cpu_map[cpu_name] = std::make_shared<CpuController>(CpuController::PassKey(), cpu_name, cpu_path, share);
    return cpu_map.find(cpu_name)->second;
}
CpuControllerPtr CGroupManager::getCpu(const String & cpu_name)
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    if (cpu_map.count(cpu_name))
        return cpu_map.find(cpu_name)->second;
    return nullptr;
}

}

