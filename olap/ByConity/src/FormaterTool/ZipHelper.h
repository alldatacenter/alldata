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
#include <Poco/Zip/ZipLocalFileHeader.h>
#include <Poco/Delegate.h>
#include <string>


namespace DB
{
#define TEMP_SUFIX ".tmp"

using DelegatePair = std::pair<const Poco::Zip::ZipLocalFileHeader, const std::string>;

/***
 * Helper class for compressing/uncompression part files when interact with HDFS. Please note that
 * we choose a algorithm with low compress rate to speed up processing. Do not realy on this for data compression.
 */

class ZipHelper
{
public:
    using PocoDelegate = Poco::Delegate<ZipHelper, DelegatePair>;

    static void zipFile(const std::string & source, const std::string & target);
    void unzipFile(const std::string & source, const std::string & target);

private:
    void setSource(const std::string & source);

    [[noreturn]]
    void onDecompressError(const void* , DelegatePair & info);

    std::string source_file;
};

}
