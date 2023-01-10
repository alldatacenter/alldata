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

#include <AggregateFunctions/registerAggregateFunctions.h>
#include <Catalog/Catalog.h>
#include <Catalog/CatalogConfig.h>
#include <Catalog/CatalogFactory.h>
#include <Core/Defines.h>
#include <Core/Types.h>
#include <Common/getMultipleKeysFromConfig.h>
#include <DaemonManager/DaemonFactory.h>
#include <DaemonManager/DaemonManager.h>
#include <DaemonManager/registerDaemons.h>
#include <DaemonManager/DaemonJobGlobalGC.h>
#include <Dictionaries/registerDictionaries.h>
#include <Disks/registerDisks.h>
#include <Functions/registerFunctions.h>
#include <CloudServices/CnchServerClient.h>
#include <ServiceDiscovery/registerServiceDiscovery.h>
#include <Storages/registerStorages.h>
#include <TableFunctions/registerTableFunctions.h>
#include <brpc/server.h>
#include <gflags/gflags.h>
#include <Poco/Util/HelpFormatter.h>
#include <Poco/Environment.h>
#include <Common/StringUtils/StringUtils.h>
#include <DaemonManager/DMDefines.h>
#include <DaemonManager/DaemonHelper.h>
#include <DaemonManager/DaemonManagerServiceImpl.h>
#include <DaemonManager/FixCatalogMetaDataTask.h>
#include <chrono>

using namespace std::chrono_literals;

#define DAEMON_MANAGER_VERSION "1.0.0"

using Poco::Logger;

namespace brpc
{
namespace policy
{
    DECLARE_string(consul_agent_addr);
}
}

namespace DB::ErrorCodes
{
    extern const int INVALID_CONFIG_PARAMETER;
    extern const int NETWORK_ERROR;
    extern const int BAD_ARGUMENTS;
}

namespace DB::DaemonManager
{

static std::string getCanonicalPath(std::string && path)
{
    Poco::trimInPlace(path);
    if (path.empty())
        throw Exception("path configuration parameter is empty", ErrorCodes::INVALID_CONFIG_PARAMETER);
    if (path.back() != '/')
        path += '/';
    return std::move(path);
}

void DaemonManager::uninitialize()
{
    logger().information("shutting down");
    BaseDaemon::uninitialize();
}

int DaemonManager::run()
{
    if (config().hasOption("help"))
    {
        Poco::Util::HelpFormatter help_formatter(DaemonManager::options());
        auto header_str = fmt::format("{} [OPTION] [-- [ARG]...]\n"
                                      "positional arguments can be used to rewrite config.xml properties, for example, --http_port=8010",
                                      commandName());
        help_formatter.setHeader(header_str);
        help_formatter.format(std::cout);
        return 0;
    }

    if (config().hasOption("version"))
    {
        std::cout << "CNCH daemon manager version " << DAEMON_MANAGER_VERSION << "." << std::endl;
        return 0;
    }

    return Application::run();
}

void DaemonManager::initialize(Poco::Util::Application & self)
{
    BaseDaemon::initialize(self);
    logger().information("starting up");

    LOG_INFO(&logger(), "OS name: {}, version: {}, architecture: {}",
        Poco::Environment::osName(),
        Poco::Environment::osVersion(),
        Poco::Environment::osArchitecture());
}

std::string DaemonManager::getDefaultCorePath() const
{
    return getCanonicalPath(config().getString("path", DBMS_DEFAULT_PATH)) + "cores";
}

void DaemonManager::defineOptions(Poco::Util::OptionSet & options)
{
    options.addOption(
        Poco::Util::Option("help", "h", "show help and exit")
            .required(false)
            .repeatable(false)
            .binding("help"));
    options.addOption(
        Poco::Util::Option("version", "V", "show version and exit")
            .required(false)
            .repeatable(false)
            .binding("version"));
    BaseDaemon::defineOptions(options);
}

std::vector<DaemonJobPtr> createLocalDaemonJobs(
    const Poco::Util::AbstractConfiguration & app_config,
    ContextMutablePtr global_context,
    Logger * log)
{
    std::map<std::string, unsigned int> default_config = {
        { "GLOBAL_GC", 5000},
        { "TXN_GC", 600000}
    };

    std::map<std::string, unsigned int> config = updateConfig(std::move(default_config), app_config);
    LOG_INFO(log, "Local Daemon Job config:");
    printConfig(config, log);

    std::vector<DaemonJobPtr> res;

    std::for_each(config.begin(), config.end(),
        [& res, & global_context] (const std::pair<std::string, unsigned int> & config_element)
        {
            DaemonJobPtr daemon_job = DaemonFactory::instance().createLocalDaemonJob(config_element.first, global_context);
            daemon_job->setInterval(config_element.second);
            res.push_back(std::move(daemon_job));
        }
    );

    return res;

}

std::unordered_map<CnchBGThreadType, DaemonJobServerBGThreadPtr> createDaemonJobsForBGThread(
    const Poco::Util::AbstractConfiguration & app_config,
    ContextMutablePtr global_context,
    Logger * log)
{
    std::unordered_map<CnchBGThreadType, DaemonJobServerBGThreadPtr> res;
    std::map<std::string, unsigned int> default_config = {
        { "PART_GC", 10000},
        { "PART_MERGE", 10000},
        { "CONSUMER", 10000},
        { "DEDUP_WORKER", 10000},
        //{ "PART_CLUSTERING", 10000}
    };

    std::map<std::string, unsigned int> config = updateConfig(std::move(default_config), app_config);
    LOG_INFO(log, "Daemon Job for server config:");
    printConfig(config, log);

    std::for_each(config.begin(), config.end(),
        [& res, & global_context] (const std::pair<std::string, unsigned int> & config_element)
        {
            DaemonJobServerBGThreadPtr daemon = DaemonFactory::instance().createDaemonJobForBGThreadInServer(config_element.first, global_context);
            daemon->setInterval(config_element.second);
            if (!res.try_emplace(daemon->getType(), daemon).second)
                throw Exception("Find duplicate daemon jobs in the config", ErrorCodes::INVALID_CONFIG_PARAMETER);

        }
    );

    return res;
}

int DaemonManager::main(const std::vector<std::string> &)
{
    DB::registerFunctions();
    DB::registerAggregateFunctions();
    DB::registerTableFunctions();
    DB::registerStorages();
    DB::registerDictionaries();
    DB::registerDisks();
    DB::registerServiceDiscovery();
    registerDaemonJobs();

    const char * consul_http_host = getenv("CONSUL_HTTP_HOST");
    const char * consul_http_port = getenv("CONSUL_HTTP_PORT");
    if (consul_http_host != nullptr && consul_http_port != nullptr)
        brpc::policy::FLAGS_consul_agent_addr = "http://" + createHostPortString(consul_http_host, consul_http_port);

    Logger * log = &logger();
    LOG_INFO(log, "Daemon Manager start up...");

    Catalog::CatalogConfig catalog_conf(config());

    /** Context contains all that query execution is dependent:
      *  settings, available functions, data types, aggregate functions, databases, ...
      */
    auto shared_context = Context::createShared();
    global_context = Context::createGlobal(shared_context.get());

    global_context->makeGlobalContext();
    global_context->setServerType("daemon_manager");
    global_context->setSetting("background_schedule_pool_size", config().getUInt64("background_schedule_pool_size", 12));
    GlobalThreadPool::initialize(config().getUInt("max_thread_pool_size", 100));

    global_context->initCatalog(catalog_conf, config().getString("catalog.name_space", "default"));
    global_context->initServiceDiscoveryClient();
    global_context->initCnchServerClientPool(config().getString("service_discovery.server.psm", "data.cnch.server"));
    global_context->initTSOClientPool(config().getString("service_discovery.tso.psm", "data.cnch.tso"));

    global_context->setCnchTopologyMaster();
    global_context->setSetting("cnch_data_retention_time_in_sec", config().getUInt64("cnch_data_retention_time_in_sec", 3*24*60*60));

    std::string path = getCanonicalPath(config().getString("path", DBMS_DEFAULT_PATH));
    global_context->setPath(path);

    HDFSConnectionParams hdfs_params = HDFSConnectionParams::parseHdfsFromConfig(config());
    global_context->setHdfsConnectionParams(hdfs_params);

    /// Temporary solution to solve the problem with Disk initialization
    fs::create_directories(path + "disks/");

    LOG_INFO(log, "Global context initialized.");

    SCOPE_EXIT({
        global_context->shutdown();

        /** Explicitly destroy Context. It is more convenient than in destructor of Server, because logger is still available.
          * At this moment, no one could own shared part of Context.
          */
        global_context.reset();
        shared_context.reset();
        LOG_INFO(log, "Destroyed global context.");
    });

    std::unordered_map<CnchBGThreadType, DaemonJobServerBGThreadPtr> daemon_jobs_for_bg_thread_in_server =
        createDaemonJobsForBGThread(config(), global_context, log);
    std::vector<DaemonJobPtr> local_daemon_jobs = createLocalDaemonJobs(config(), global_context, log);

    std::for_each(local_daemon_jobs.begin(), local_daemon_jobs.end(),
        [] (const DaemonJobPtr & daemon_job) {
            daemon_job->init();
        }
    );

    auto storage_cache_size = config().getUInt("daemon_manager.storage_cache_size", 10000);
    StorageCache cache(storage_cache_size); /* Cache size = storage_cache_size, invalidate an entry every 180s if unused */

    const size_t liveness_check_interval = config().getUInt("daemon_manager.liveness_check_interval", LIVENESS_CHECK_INTERVAL);
    std::for_each(
        daemon_jobs_for_bg_thread_in_server.begin(),
        daemon_jobs_for_bg_thread_in_server.end(),
        [liveness_check_interval, & cache] (auto & p)
        {
            auto & daemon = p.second;
            daemon->init();
            daemon->setLivenessCheckInterval(liveness_check_interval);
            daemon->setStorageCache(&cache);
        }
    );

    brpc::Server server;

    // launch brpc service
    int port = config().getInt("daemon_manager.port", 8090);
    std::unique_ptr<DaemonManagerServiceImpl> daemon_manager_service =
        std::make_unique<DaemonManagerServiceImpl>(daemon_jobs_for_bg_thread_in_server);

    if (server.AddService(daemon_manager_service.get(), brpc::SERVER_DOESNT_OWN_SERVICE) != 0)
    {
        LOG_ERROR(log, "Fail to add daemon manager service.");
        exit(-1);
    }

    brpc::ServerOptions options;
    options.idle_timeout_sec = -1;
    std::string host_port = createHostPortString("::", port);
    if (server.Start(host_port.c_str(), &options) != 0)
    {
        LOG_ERROR(log, "Fail to start Daemon Manager RPC server.");
        exit(-1);
    }

    LOG_INFO(log, "Daemon manager service starts on address {}", host_port);

    std::for_each(
        daemon_jobs_for_bg_thread_in_server.begin(),
        daemon_jobs_for_bg_thread_in_server.end(),
        [] (auto & p)
        {
            p.second->start();
        }
    );

    std::for_each(local_daemon_jobs.begin(), local_daemon_jobs.end(),
        [] (const DaemonJobPtr & daemon_job) {
            daemon_job->start();
        }
    );

    auto fix_metadata_task = global_context->getSchedulePool().createTask("Fix catalog metadata", [& log, this] () { fixCatalogMetaData(global_context, log); });
    fix_metadata_task->activateAndSchedule();
    waitForTerminationRequest();

    LOG_INFO(log, "Shutting down!");
    LOG_INFO(log, "BRPC server stop accepting new connections and requests from existing connections");
    if (0 == server.Stop(5000))
        LOG_INFO(log, "BRPC server stop succesfully");
    else
        LOG_INFO(log, "BRPC server doesn't stop succesfully with in 5 second");

    LOG_INFO(log, "Wait until brpc requests in progress are done");
    if (0 == server.Join())
        LOG_INFO(log, "brpc joins succesfully");
    else
        LOG_INFO(log, "brpc doesn't join succesfully");

    LOG_INFO(log, "Wait for daemons for bg thread to finish.");
    std::for_each(
        daemon_jobs_for_bg_thread_in_server.begin(),
        daemon_jobs_for_bg_thread_in_server.end(),
        [] (auto & p)
        {
            p.second->stop();
        }
    );

    LOG_INFO(log, "Wait for daemons for local job to finish.");
    std::for_each(local_daemon_jobs.begin(), local_daemon_jobs.end(),
        [] (const DaemonJobPtr & daemon_job) {
            daemon_job->stop();
        }
    );

    LOG_INFO(log, "daemons for local job finish succesfully.");

    return Application::EXIT_OK;
}

}/// end namespace DaemonManager

int mainEntryClickHouseDaemonManager(int argc, char ** argv)
{
    DB::DaemonManager::DaemonManager app;
    try
    {
        return app.run(argc, argv);
    }
    catch (...)
    {
        std::cerr << DB::getCurrentExceptionMessage(true) << "\n";
        auto code = DB::getCurrentExceptionCode();
        return code ? code : 1;
    }
}
