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
#include <string>
#include <Common/StringUtils/StringUtils.h>
#include <Common/config.h>
#include <common/logger_useful.h>
#include <loggers/OwnFormattingChannel.h>
#include <Parsers/parseQuery.h>
#include <Parsers/ParserPartToolkitQuery.h>
#include <Parsers/ASTPartToolKit.h>
#include <Formats/registerFormats.h>
#include <Functions/registerFunctions.h>
#include <Storages/registerStorages.h>
#include <Dictionaries/registerDictionaries.h>
#include <Disks/registerDisks.h>
#include <FormaterTool/PartMerger.h>
#include <FormaterTool/PartConverter.h>
#include <FormaterTool/PartWriter.h>
#include <Poco/Path.h>
#include <Poco/FileChannel.h>
#include <Poco/ConsoleChannel.h>
#include <Poco/SplitterChannel.h>
#include <Poco/FormattingChannel.h>
#include <Poco/PatternFormatter.h>
#include <Poco/Util/XMLConfiguration.h>
#include <Poco/Logger.h>

int mainHelp(int , char **)
{
    /// TODO: make help more clear
    std::cout << "Usage : \n";
    std::cout << "clickhouse [part-writer|part-converter] query" << std::endl;
    return 0;
}

void run(const std::string & query, Poco::Logger * log)
{
    LOG_DEBUG(log, "Executing query : {}", query);
    DB::ThreadStatus status;

    DB::registerFunctions();
    DB::registerDictionaries();
    DB::registerDisks();
    DB::registerStorages();
    DB::registerFormats();

    DB::ConfigurationPtr configuration(new Poco::Util::XMLConfiguration());

    auto shared_context = DB::Context::createShared();
    auto mutable_context_ptr = DB::Context::createGlobal(shared_context.get());
    mutable_context_ptr->makeGlobalContext();
    mutable_context_ptr->setConfig(configuration);
    mutable_context_ptr->setMarkCache(1000000);

    const char * begin = query.data();
    const char * end =  query.data() + query.size();

    DB::ParserPartToolkitQuery parser(end);
    auto ast = DB::parseQuery(parser, begin, end, "", 10000, 100);
    const DB::ASTPartToolKit & query_ast = ast->as<DB::ASTPartToolKit &>();

    std::shared_ptr<DB::PartToolkitBase> executor = nullptr;

    if (query_ast.type == DB::PartToolType::WRITER)
        executor = std::make_shared<DB::PartWriter>(ast, mutable_context_ptr);
    else if (query_ast.type == DB::PartToolType::MERGER)
    {
        LOG_ERROR(log, "Part merger is not implmented in cnch. ");
        return;
    }
    else if (query_ast.type == DB::PartToolType::CONVERTER)
        executor = std::make_shared<DB::PartConverter>(ast, mutable_context_ptr);

    executor->execute();
}

int mainEntryClickhousePartToolkit(int argc, char ** argv)
{
    /// init poco log config.
    Poco::AutoPtr<Poco::PatternFormatter> pf = new Poco::PatternFormatter("[%Y-%m-%d %H:%M:%S.%i] <%p> %s: %t");
    pf->setProperty("times", "local");

    Poco::AutoPtr<Poco::SplitterChannel> split_channel = new Poco::SplitterChannel;

    Poco::AutoPtr<Poco::ConsoleChannel> cout_channel = new Poco::ConsoleChannel(std::cout);
    Poco::AutoPtr<Poco::FormattingChannel> fcout_channel = new Poco::FormattingChannel(pf, cout_channel);

    split_channel->addChannel(fcout_channel);

    Poco::AutoPtr<Poco::ConsoleChannel> cerr_channel = new Poco::ConsoleChannel(std::cerr);
    Poco::AutoPtr<Poco::FormattingChannel> fcerr_channel = new Poco::FormattingChannel(pf, cerr_channel);
    Poco::AutoPtr<DB::OwnFormattingChannel> of_channel = new DB::OwnFormattingChannel();
    of_channel->setChannel(fcerr_channel);
    of_channel->setLevel(Poco::Message::PRIO_ERROR);
    split_channel->addChannel(of_channel);

    Poco::AutoPtr<Poco::FileChannel> f_channel = new Poco::FileChannel;
    f_channel->setProperty(Poco::FileChannel::PROP_PATH, Poco::Path::current() + "task.log");
    f_channel->setProperty(Poco::FileChannel::PROP_ROTATEONOPEN, "true");
    Poco::AutoPtr<Poco::FormattingChannel> ff_channel = new Poco::FormattingChannel(pf, f_channel);
    split_channel->addChannel(ff_channel);

    Poco::Logger::root().setChannel(split_channel);
    Poco::Logger::root().setLevel("debug");

    Poco::Logger * log = &Poco::Logger::get("part-toolkit");

    if (argc < 2)
    {
        mainHelp(argc, argv);
        return 1;
    }

    try
    {
        std::string sql = argv[1];
        run(sql, log);
    }
    catch (const Poco::Exception & e)
    {
        LOG_ERROR(log, "Interupted by Poco::exception: {}", e.what());
        return -1;
    }
    catch (const std::exception & e)
    {
        LOG_ERROR(log, "Interupted by std::exception: {}", e.what());
        return -1;
    }
    catch (...)
    {
        LOG_ERROR(log, "Unknown exception occurs.");
        return -1;
    }

    return 0;
}
