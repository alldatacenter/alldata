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

#include <Common/getCurrentProcessFDCount.h>
#include <Common/ShellCommand.h>
#include <IO/WriteBufferFromString.h>
#include <unistd.h>
#include <fmt/format.h>
#include <IO/ReadHelpers.h>
#include <filesystem>


int getCurrentProcessFDCount()
{
    namespace fs = std::filesystem;
    int result = -1;
#if defined(__linux__)  || defined(__APPLE__)
    using namespace DB;

    Int32 pid = getpid();

    auto proc_fs_path = fmt::format("/proc/{}/fd", pid);
    if (fs::exists(proc_fs_path))
    {
        result = std::distance(fs::directory_iterator(proc_fs_path), fs::directory_iterator{});
    }
    else if (fs::exists("/dev/fd"))
    {
        result = std::distance(fs::directory_iterator("/dev/fd"), fs::directory_iterator{});
    }
    else
    {
        /// Then try lsof command
        String by_lsof = fmt::format("lsof -p {} | wc -l", pid);
        auto command = ShellCommand::execute(by_lsof);

        try
        {
            readIntText(result, command->out);
            command->wait();
        }
        catch (...)
        {
        }
    }

#endif
    return result;
}
