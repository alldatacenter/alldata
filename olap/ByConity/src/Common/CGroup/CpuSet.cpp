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

#include <Common/CGroup/CpuSet.h>
#include <filesystem>
#include <Common/Exception.h>
#include <sstream>
#include <Common/SystemUtils.h>
#include <regex>
#include <Poco/String.h>


namespace DB
{

namespace ErrorCodes
{
extern const int CREATE_CGROUP_DIRECTORY_FAILED;
}

const String CpuSet::TASK_FILE = "tasks";
const String CpuSet::CPU_EXCLUSIVE = "cpuset.cpu_exclusive";
const String CpuSet::CPUS = "cpuset.cpus";
const String CpuSet::MEMS = "cpuset.mems";
const String CpuSet::PROC = "cgroup.procs";

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

CpuSet::CpuSet(PassKey, String name_, String path_, Cpus cpus_, bool enable_exclusive_)
    :name(name_), dir_path(path_), enable_exclusive(enable_exclusive_), cpus(cpus_)
{
    if (std::filesystem::exists(dir_path))
        SystemUtils::rmdirAll(dir_path.c_str());
    createCpuSetController();
    resetCpuSet();
}
CpuSet::CpuSet(String name_, String path_):name(name_), dir_path(path_)
{
    std::filesystem::path cpus_path = dir_path + "/" + CPUS;
    std::filesystem::path enable_exclusive_path = dir_path + "/" + CPUS;
    ReadBufferFromFile cpus_input(cpus_path);
    ReadBufferFromFile enable_exclusive_input(enable_exclusive_path);

    String cpus_str;
    String enable_exclusive_str;

    readString(cpus_str, cpus_input);
    readString(enable_exclusive_str, enable_exclusive_input);

    enable_exclusive = (enable_exclusive_str == "1");
    cpus = Cpus(cpus_str);
}

void CpuSet::createCpuSetController()
{
    std::error_code error_code;
    bool res = std::filesystem::create_directory(dir_path, error_code);
    if (!res)
        throw Exception("create cgroup directory " + dir_path +" failed, filesystem error code: " + std::to_string(error_code.value()), ErrorCodes::CREATE_CGROUP_DIRECTORY_FAILED);
}

void CpuSet::resetCpuSet()
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    std::filesystem::path exclusive_path = dir_path + "/" + CPU_EXCLUSIVE;
    std::filesystem::path cpus_path = dir_path + "/" + CPUS;
    std::filesystem::path mems_path = dir_path + "/" + MEMS;

    SystemUtils::writeStringToFile(cpus_path, cpus.toString() + "\n", true);
    SystemUtils::writeStringToFile(exclusive_path, std::to_string(enable_exclusive) + "\n", true);
    SystemUtils::writeStringToFile(mems_path, getDefaultMems(), true);
}


void CpuSet::addTask(size_t tid)
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    std::filesystem::path task_path = dir_path + "/" + TASK_FILE;
    std::stringstream ss;
    ss << tid << std::endl;
    SystemUtils::writeStringToFile(task_path, ss.str());
}

void CpuSet::addTasks(const std::vector<size_t> & tids)
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    for (const auto & tid : tids)
        addTask(tid);
}

std::vector<size_t> CpuSet::getTasks()
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    std::vector<size_t> tids;
    std::filesystem::path task_path = dir_path + "/" + TASK_FILE;
    ReadBufferFromFile input(task_path);

    String line;
    while (!input.eof())
    {
        readString(line, input);
        skipWhitespaceIfAny(input);
        tids.emplace_back(std::stoul(line));
    }
    return tids;
}

Cpus CpuSet::getCpus()
{
    std::filesystem::path cpus_path = dir_path + "/" + CPUS;
    ReadBufferFromFile input(cpus_path);
    String line;
    readString(line, input);
    cpus = Cpus(line);
    return cpus;
}

String Cpus::toString()
{
    std::stringstream ss;
    size_t range = 0;
    size_t last_begin = -1;

    auto end_func = [](std::stringstream & ss_, size_t idx_, size_t range_)
    {
        if (range_ > 1)
            ss_ << '-' << idx_;
        ss_ << ',';
    };

    for (size_t idx = 0; idx < data.size(); ++idx)
    {
        if (!data[idx] && range > 0)
        {
            end_func(ss, idx-1, range);
            range=0;
        }
        else if (data[idx])
        {
            if (range == 0)
            {
                last_begin = idx;
                ss << last_begin;
            }
            ++range;
            if (idx == data.size()-1)
            {
                end_func(ss, idx, range);
            }
        }
    }
    ss.seekp(-1, std::ios_base::end);
    ss << '\0';
    return ss.str();
}
Cpus::Cpus(String cpus)
{
    auto data_ = parse(cpus);
    data = std::move(data_);
}

std::vector<bool> Cpus::parse(const String & cpus_str)
{
    size_t cpu_num = SystemUtils::getSystemCpuNum();
    std::vector<bool> data_(cpu_num, false);
    std::regex re(",");

    // cpus str example: 0,1-3,5
    std::vector<std::string> strs(std::sregex_token_iterator(cpus_str.begin(), cpus_str.end(), re, -1),
                                  std::sregex_token_iterator());
    for (const auto & s : strs)
    {
        if (s.find('-') != std::basic_string<char>::npos)
        {
            const char * cs = s.c_str();
            String begin_s;
            String end_s;
            std::stringstream ss;
            for (size_t i = 0; i < s.size(); ++i)
            {
                if (Poco::Ascii::isSpace(cs[i]))
                    continue;
                if (cs[i] == '-')
                {
                    begin_s = ss.str();
                    ss.clear();
                    ss.str("");

                    continue;
                }
                else
                    ss << cs[i];
            }
            end_s = ss.str();
            size_t begin = std::stoul(begin_s);
            size_t end = std::stoul(end_s);
            for (size_t i = begin; i <= std::min(end, cpu_num-1); ++i)
            {
                data_[i] = true;
            }
        }
        else
        {
            const String real_s = Poco::trim(s);
            if (real_s.empty())
                continue;
            size_t pos = std::stoul(real_s);
            if (pos >= cpu_num)
                continue;
            data_[pos] = true;
        }
    }
    return data_;
}

}

