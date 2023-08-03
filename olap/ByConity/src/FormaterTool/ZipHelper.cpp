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

#include <FormaterTool/ZipHelper.h>
#include <Common/Exception.h>
#include <Poco/Zip/Compress.h>
#include <Poco/Zip/Decompress.h>
#include <Poco/Zip/ZipCommon.h>
#include <Poco/Path.h>
#include <Poco/File.h>
#include <iostream>
#include <fstream>

namespace DB
{

namespace ErrorCodes
{
    extern const int POCO_EXCEPTION;
}

void ZipHelper::unzipFile(const std::string & source, const std::string & target)
{
    setSource(source);
    std::ifstream in(source, std::ios::binary);
    String temp_path = target + TEMP_SUFIX;
    try {
        Poco::Zip::Decompress decompress(in, Poco::Path(temp_path), true);

        decompress.EError += PocoDelegate(this, &ZipHelper::onDecompressError);
        decompress.decompressAllFiles();
        decompress.EError -= PocoDelegate(this, &ZipHelper::onDecompressError);

        Poco::File tmp_part(temp_path);
        tmp_part.renameTo(target);
    } catch (Exception & e) {
        Poco::File tmp_path(temp_path);
        if (tmp_path.exists())
            tmp_path.remove(true);
        throw e;
    }
}

void ZipHelper::zipFile(const std::string & source, const std::string & target)
{
    Poco::Path path(source);
    Poco::File target_file(target);
    if (target_file.exists())
        target_file.remove(false);
    std::ofstream out(target, std::ios::binary);
    Poco::Zip::Compress compress(out, true, true);
    if (path.isFile())
    {
        compress.addFile(path, path.getFileName());
    }
    else if (path.isDirectory())
    {
        /// Coarse grained compression to speed up packaging process.
        compress.addRecursive(path, Poco::Zip::ZipCommon::CompressionMethod::CM_STORE, Poco::Zip::ZipCommon::CL_SUPERFAST, false);
    }
    compress.close();
}

void ZipHelper::onDecompressError(const void* , DelegatePair & info)
{
    throw Exception("Failed to unzip file : " + source_file + ". Error message: " + info.second +
        ". This may because zip file is broken.", ErrorCodes::POCO_EXCEPTION);
}

void ZipHelper::setSource(const std::string & source)
{
    source_file = source;
}

}
