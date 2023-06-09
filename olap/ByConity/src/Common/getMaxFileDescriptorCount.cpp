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

#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/ReadBufferFromFile.h>
#include <Common/ShellCommand.h>
#include <Common/getMaxFileDescriptorCount.h>
#include <filesystem>

int getMaxFileDescriptorCount()
{
    namespace fs = std::filesystem;
    int result = -1;
#if defined(__linux__) || defined(__APPLE__)
    using namespace DB;

    if (fs::exists("/proc/sys/fs/file-max"))
    {
        ReadBufferFromFile reader("/proc/sys/fs/file-max");
        readIntText(result, reader);
    }
    else
    {
        auto command = ShellCommand::execute("ulimit -n");
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
