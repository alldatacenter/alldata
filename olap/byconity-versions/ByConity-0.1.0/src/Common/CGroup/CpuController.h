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

#pragma once
#include <Core/Types.h>
#include <mutex>
#include <memory>

namespace DB
{

class CpuController
{
private:
    friend class CGroupManager;
    static const String SHARE;
    static const String TASK_FILE;
    String dir_path;
    String name;
    struct PassKey{};
    void init(UInt64 share);
    std::recursive_mutex mutex;
public:
    CpuController(PassKey pass_key, String name, String dir_path, UInt64 share);
    void addTask(size_t tid);
    void addTasks(const std::vector<size_t> & tids);
    std::vector<size_t> getTasks();
    UInt64 getShare();
    void setShare(UInt64 share);
};

using CpuControllerPtr = std::shared_ptr<CpuController>;

}

