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

#pragma once

#include <common/types.h>
#include <Common/Exception.h>

#include <filesystem>
#include <memory>
#include <string>
#include <sys/statvfs.h>
#include <Poco/TemporaryFile.h>


namespace DB
{

using TemporaryFile = Poco::TemporaryFile;

bool enoughSpaceInDirectory(const std::string & path, size_t data_size);
std::unique_ptr<TemporaryFile> createTemporaryFile(const std::string & path);

/// Returns mount point of filesystem where absolute_path (must exist) is located
std::filesystem::path getMountPoint(std::filesystem::path absolute_path);

/// Returns name of filesystem mounted to mount_point
#if !defined(__linux__)
[[noreturn]]
#endif
String getFilesystemName([[maybe_unused]] const String & mount_point);

struct statvfs getStatVFS(const String & path);

/// Returns true if path starts with prefix path
bool pathStartsWith(const std::filesystem::path & path, const std::filesystem::path & prefix_path);

/// Returns true if path starts with prefix path
bool pathStartsWith(const String & path, const String & prefix_path);

bool symlinkStartsWith(const String & path, const String & prefix_path);

String joinPaths(const std::vector<String>& components, bool add_post_slash = false);
}

namespace FS
{
bool createFile(const std::string & path);

bool canRead(const std::string & path);
bool canWrite(const std::string & path);

time_t getModificationTime(const std::string & path);
Poco::Timestamp getModificationTimestamp(const std::string & path);
void setModificationTime(const std::string & path, time_t time);
}
