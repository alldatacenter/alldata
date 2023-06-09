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

#include <iostream>
#include <vector>

#include <hdfs/hdfs.h>
#include <common/logger_useful.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Storages/HDFS/HDFSFileSystem.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/HDFS/WriteBufferFromHDFS.h>
#include <IO/ReadBufferFromFile.h>
#include <IO/copyData.h>
#include <IO/WriteBufferFromFile.h>
#include <Common/Stopwatch.h>
#include <Poco/AutoPtr.h>
#include <Poco/ConsoleChannel.h>
#include <Poco/FormattingChannel.h>
#include <Poco/Logger.h>
#include <Poco/Path.h>
#include <Poco/PatternFormatter.h>
#include <boost/program_options.hpp>
#include <IO/copyData.h>
#include <IO/WriteBufferFromFile.h>

using String = std::string;

int main(int argc, char ** argv) {
    using namespace std;
    namespace po = boost::program_options;
    po::options_description desc("Allowed options");
    desc.add_options()
        ("help,h", "print help message")
        ("nnproxy,n", po::value<string>(), "nnproxy name")
        ("mkdir,m", po::value<string>(), "create a directory")
        ("rmdir,e", po::value<string>(), "remove a directory")
        ("rmr,r", po::value<string>(), "remove a directory recursively")
        ("ls,l", po::value<string>(), "list a directory")
        ("read_entire_file", po::value<string>(), "")
        ("put", po::value<std::vector<string>>()->multitoken(),"upload a file to hdfs")
        ("get", po::value<std::vector<string>>()->multitoken(),"download a file from hdfs")
        ("enable_logging", "Enable logging output")
        ("logging_level", po::value<string>()->default_value("debug"), "logging level")
        ;

    po::variables_map options;
    po::store(po::parse_command_line(argc, argv, desc), options);

    if (options.count("help"))
    {
        std::cout << "Usage: " << argv[0] << " [options]" << std::endl;
        std::cout << desc << std::endl;
        return 0;
    }

    if (options.count("enable_logging"))
    {
        Poco::AutoPtr<Poco::PatternFormatter> formatter(new Poco::PatternFormatter("%Y.%m.%d %H:%M:%S.%F <%p> %s: %t"));
        Poco::AutoPtr<Poco::ConsoleChannel> console_chanel(new Poco::ConsoleChannel);
        Poco::AutoPtr<Poco::FormattingChannel> channel(new Poco::FormattingChannel(formatter, console_chanel));
        Poco::Logger::root().setLevel(options["logging_level"].as<string>());
        Poco::Logger::root().setChannel(channel);
    }

    string nnproxy = "pxd-nnproxy.service.lf";
    if (options.count("nnproxy"))
        nnproxy = options["nnproxy"].as<string>();
    DB::HDFSConnectionParams hdfs_params(DB::HDFSConnectionParams::CONN_NNPROXY, "clickhouse", nnproxy) ;
    DB::registerDefaultHdfsFileSystem(hdfs_params, 100000, 100, 10);

    auto& hdfs_fs = DB::getDefaultHdfsFileSystem();

    if (options.count("mkdir"))
    {
        auto directory = options["mkdir"].as<string>();

        hdfs_fs->createDirectory(directory);
        std::cout << "mkdir " << directory << " success" << std::endl;
    }
    else if (options.count("rmdir"))
    {
        auto directory = options["rmdir"].as<string>();
        hdfs_fs->remove(directory, false);
        std::cout << "rmdir " << directory << " success" << std::endl;
    }
    else if (options.count("rmr"))
    {
        auto directory = options["rmr"].as<string>();
        hdfs_fs->remove(directory, true);
        std::cout << "rmr " << directory << " success" << std::endl;
    }
    else if (options.count("ls"))
    {
        auto directory = options["ls"].as<string>();
        if (!hdfs_fs->exists(directory))
        {
            std::cout << directory << " No such file or directory" << std::endl;
            return 0;
        }
        std::vector<String> files;
        hdfs_fs->list(directory, files);
        for (auto & f : files)
        {
            std::cout << directory << "/" << f << std::endl;
        }
    }
    else if (options.count("read_entire_file"))
    {
        Stopwatch watch;

        auto path = options["read_entire_file"].as<string>();
//        DB::HDFSConnectionParams hdfs_params(DB::HDFSConnectionParams::CONN_NNPROXY, "clickhouse", nnproxy);
        DB::ReadBufferFromByteHDFS reader(path, true, hdfs_params);

        while (!reader.eof())
        {
            size_t count = reader.buffer().end() - reader.position();
            reader.position() += count;
        }

        std::cout << "Read " << path << " with " << reader.count() << " takes "
            << watch.elapsedMilliseconds() << " ms" << std::endl;
    }
    else if(options.count("put"))
    {
        auto& params = options["put"].as<std::vector<std::string>>();
        if(params.size()!=2) {
            std::cout << "missing params, ./hdfs_command put src_path dst_path";
            return -1;
        }
        std::string src_path = params[0];
        std::string dst_path = params[1];
        Stopwatch watch;
        DB::ReadBufferFromFile  reader(src_path);
        DB::WriteBufferFromHDFS wb(dst_path, hdfs_params);
        DB::copyData(reader, wb);
        wb.next();
        std::cout <<"Upload " << src_path << "to " << dst_path << " takes" << watch.elapsedMilliseconds() << " ms" << std::endl;
    }
    else if(options.count("get"))
    {
        auto& params = options["get"].as<std::vector<std::string>>();
        if(params.size()!=2) {
            std::cout << "missing params, ./hdfs_command get src_path dst_path";
            return -1;
        }
        std::string src_path = params[0];
        std::string dst_path = params[1];
        Stopwatch watch;
        DB::ReadBufferFromByteHDFS reader(src_path, true, hdfs_params );
        DB::WriteBufferFromFile wb(dst_path);
        DB::copyData(reader, wb);
        wb.next();
        std::cout <<"download " << src_path << " to " << dst_path << " takes" << watch.elapsedMilliseconds() << " ms" << std::endl;
    }

    return 0;
}
