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
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Common/CGroup/CpuController.h>
#include <Common/Exception.h>
#include <Common/SystemUtils.h>


namespace DB
{

namespace ErrorCodes
{
extern const int CREATE_CGROUP_DIRECTORY_FAILED;
}

const String CpuController::SHARE = "cpu.shares";
const String CpuController::TASK_FILE = "tasks";

void CpuController::init(UInt64 share)
{
    std::error_code error_code;
    bool res = std::filesystem::create_directory(dir_path, error_code);
    if (!res)
        throw Exception("create cgroup cpu directory " + dir_path +" failed, filesystem error code: " + std::to_string(error_code.value()), ErrorCodes::CREATE_CGROUP_DIRECTORY_FAILED);
    std::filesystem::path share_path = dir_path + "/" + SHARE;
    SystemUtils::writeStringToFile(share_path, toString(share), true);
}

CpuController::CpuController(CpuController::PassKey , String name_, String dir_path_, UInt64 share_)
    :dir_path(dir_path_), name(name_)
{
    if (std::filesystem::exists(dir_path))
        SystemUtils::rmdirAll(dir_path.c_str());
    init(share_);
}

void CpuController::addTask(size_t tid)
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    std::filesystem::path task_path = dir_path + "/" + TASK_FILE;
    std::stringstream ss;
    ss << tid << std::endl;
    SystemUtils::writeStringToFile(task_path, ss.str());
}

void CpuController::addTasks(const std::vector<size_t> & tids)
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    for (const auto & tid : tids)
        addTask(tid);
}

std::vector<size_t> CpuController::getTasks()
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

UInt64 CpuController::getShare()
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    std::filesystem::path share_path = dir_path + "/" + SHARE;
    ReadBufferFromFile input(share_path);
    String s;
    readString(s, input);
    return std::stoull(s);
}

void CpuController::setShare(UInt64 share)
{
    std::lock_guard<std::recursive_mutex> lock(mutex);
    std::filesystem::path share_path = dir_path + "/" + SHARE;
    SystemUtils::writeStringToFile(share_path, toString(share), true);
}

}

