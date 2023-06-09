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
#include <Common/Exception.h>
#include <IO/ReadBufferFromFile.h>
#include <IO/WriteBufferFromFile.h>
#include <IO/HashingWriteBuffer.h>
#include <IO/ReadHelpers.h>
#include <dirent.h>
#include <unistd.h>
#include <sys/stat.h>
#include <cstring>
#include <cstdio>
#include <fcntl.h>
#include <unistd.h>
#include <iostream>
#include <fstream>
#include <unordered_set>
#include <sys/syscall.h>
#include <common/logger_useful.h>
#include <boost/algorithm/string.hpp>

#if defined(__linux__)
    #ifdef __clang__
        #pragma clang diagnostic push
        #pragma clang diagnostic ignored "-Wreserved-id-macro"
        #pragma clang diagnostic ignored "-Wold-style-cast"
        #pragma clang diagnostic ignored "-Wcast-qual"
    #endif

    #include <numa.h>

    #ifdef __clang__
        #pragma clang diagnostic pop
    #endif
#endif


namespace DB
{
namespace ErrorCodes
{
    extern const int CANNOT_CLOSE_FILE;
    extern const int CANNOT_OPEN_FILE;
    extern const int CANNOT_WRITE_TO_FILE_DESCRIPTOR;
    extern const int CANNOT_READ_ALL_DATA;
}

struct CpuUsageInfo
{
    size_t cpu_node;
    size_t user;
    size_t nice;
    size_t system;
    size_t idle;
    size_t iowait;
    size_t irq;
    size_t softirq;

    size_t total()
    {
        return user + nice + system + idle + iowait + irq + softirq;
    }
};

class SystemUtils
{
public:
    static size_t getSystemCpuNum()
    {
        return sysconf(_SC_NPROCESSORS_ONLN);
    }

    /**
     * for remove cgroup dir
     * copy from: https://stackoverflow.com/questions/2256945/removing-a-non-empty-directory-programmatically-in-c-or-c
     * @param path path
     * @return 0 for success
     */
    static int rmdirAll(const char* path)
    {
        DIR *d = opendir(path);
        size_t path_len = strlen(path);
        int r = -1;
        if (d)
        {
            struct dirent *p;
            r = 0;
            while (!r && (p = readdir(d)))
            {
                int r2 = -1;
                char *buf;
                size_t len;

                /* Skip the names "." and ".." as we don't want to recurse on them. */
                if (!strcmp(p->d_name, ".") || !strcmp(p->d_name, ".."))
                    continue;

                len = path_len + strlen(p->d_name) + 2;
                buf = static_cast<char *>(malloc(len));
                if (buf)
                {
                    struct stat statbuf;
                    snprintf(buf, len, "%s/%s", path, p->d_name);
                    if (!stat(buf, &statbuf))
                    {
                        if (S_ISDIR(statbuf.st_mode))
                            r2 = rmdirAll(buf);
                        else
                            r2 = 0;
                    }
                    free(buf);
                }
                r = r2;
            }
            closedir(d);
        }
        if (!r)
            r = rmdir(path);
        return r;
    }

    static void writeStringToFile(const String & filename, const String & content, bool trunc = false)
    {
        if (trunc)
        {
            if (::open(filename.c_str(), O_TRUNC) < 0)
                throwFromErrno("open file error, file: " + filename, ErrorCodes::CANNOT_OPEN_FILE);
        }

        WriteBufferFromFile file_writer(filename);
        HashingWriteBuffer hashing_writer(file_writer);
        hashing_writer.write(content.c_str(), content.size());
        hashing_writer.getHash();
        file_writer.close();
    }

    static size_t gettid()
    {
        #if defined(__linux__)
        return static_cast<size_t>(syscall(SYS_gettid));
        #endif
        return 0;
    }

    static size_t getMaxNumaNode()
    {
        #if defined(__linux__)
        return numa_max_node();
        #endif
        return 0;
    }

    static void getCpuUsageInfo(const std::unordered_set<size_t> & cpu_nodes, std::vector<CpuUsageInfo> & cpu_usage_info_vec)
    {
#if defined(__linux__)
        cpu_usage_info_vec.resize(cpu_nodes.size());
        ReadBufferFromFile file_reader("/proc/stat");
        /// skip first head
        String line;
        readString(line, file_reader);
        skipWhitespaceIfAny(file_reader);

        size_t cpu_num = getSystemCpuNum();
        size_t idx=0;
        for (size_t i = 0; i < cpu_num; ++i)
        {
            readString(line, file_reader);
            skipWhitespaceIfAny(file_reader);
            if (cpu_nodes.count(i))
            {
                std::vector<String> split_strings;
                boost::split(split_strings, line, boost::is_any_of(" "), boost::token_compress_on);
                cpu_usage_info_vec[idx].cpu_node = idx;
                cpu_usage_info_vec[idx].user = std::stoul(split_strings[1]);
                cpu_usage_info_vec[idx].nice = std::stoul(split_strings[2]);
                cpu_usage_info_vec[idx].system = std::stoul(split_strings[3]);
                cpu_usage_info_vec[idx].idle = std::stoul(split_strings[4]);
                cpu_usage_info_vec[idx].iowait = std::stoul(split_strings[5]);
                cpu_usage_info_vec[idx].irq = std::stoul(split_strings[6]);
                cpu_usage_info_vec[idx].softirq = std::stoul(split_strings[7]);
                idx++;
            }
        }
#endif
    }
};
}
