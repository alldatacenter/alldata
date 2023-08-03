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

#include <ResourceManagement/ResourceManager.h>

#include <Catalog/Catalog.h>
#include <Common/Exception.h>
#include <Common/getMultipleKeysFromConfig.h>
#include <Core/UUID.h>
// TODO(zuochuang.zema): MERGE http handler
// #include <HTTPHandler/HTTPHandlerFactory.h>
// #include <HTTPHandler/PrometheusRequestHandler.h>
#include <Interpreters/Context.h>
#include <Poco/Net/HTTPServer.h>
#include <Poco/Net/NetException.h>
#include <ResourceManagement/ResourceManagerController.h>
#include <ResourceManagement/ResourceManagerServiceImpl.h>
#include <ServiceDiscovery/registerServiceDiscovery.h>

#include <brpc/server.h>
#include <Poco/Util/HelpFormatter.h>

#include <ResourceManagement/CommonData.h>

namespace brpc
{
namespace policy
{
    DECLARE_string(consul_agent_addr);
}
}

namespace DB
{

void ResourceManager::defineOptions(Poco::Util::OptionSet & _options)
{
    Application::defineOptions(_options);

    _options.addOption(Poco::Util::Option("help", "h", "show help and exit").required(false).repeatable(false).binding("help"));

    _options.addOption(Poco::Util::Option("config-file", "", "set config file path")
                           .required(false)
                           .repeatable(false)
                           .argument("config-file", true)
                           .binding("config-file"));
}

void ResourceManager::initialize(Poco::Util::Application & self)
{
    BaseDaemon::initialize(self);
}

int ResourceManager::run()
{
    if (config().hasOption("help"))
    {
        Poco::Util::HelpFormatter helpFormatter(ResourceManager::options());
        std::stringstream header;
        header << "e.g. : " << commandName() << " --config-file /etc/usr/config.xml";
        helpFormatter.setHeader(header.str());
        helpFormatter.format(std::cout);
        return 0;
    }

    if (config().hasOption("version"))
    {
        std::cout << "CNCH resource manager version " << ResourceManagerVersion << "." << std::endl;
        return 0;
    }
    try
    {
        return ServerApplication::run();
    }
    catch (...)
    {
        std::cerr << DB::getCurrentExceptionMessage(true) << "\n";
        auto code = DB::getCurrentExceptionCode();
        return code ? code : 1;
    }
}

int ResourceManager::main(const std::vector<std::string> &)
{
    namespace RM = DB::ResourceManagement;


    registerServiceDiscovery();
    const char * consul_http_host = getenv("CONSUL_HTTP_HOST");
    const char * consul_http_port = getenv("CONSUL_HTTP_PORT");
    if (consul_http_host != nullptr && consul_http_port != nullptr)
        brpc::policy::FLAGS_consul_agent_addr = "http://" + createHostPortString(consul_http_host, consul_http_port);

    Poco::Logger * log = &logger();
    LOG_INFO(log, "Resource Manager is starting up...");

    auto shared_context = Context::createShared();
    global_context = Context::createGlobal(shared_context.get());
    global_context->makeGlobalContext();
    global_context->setServerType(config().getString("cnch_type", "resource_manager"));
    global_context->setApplicationType(Context::ApplicationType::SERVER);
    global_context->initCnchConfig(config());
    global_context->initRootConfig(config());

    global_context->initServiceDiscoveryClient();

    /// Initialize catalog
    Catalog::CatalogConfig catalog_conf(global_context->getCnchConfigRef());
    auto name_space = global_context->getCnchConfigRef().getString("catalog.name_space", "default");
    global_context->initCatalog(catalog_conf, name_space);

    auto rm_controller = std::make_shared<RM::ResourceManagerController>(global_context);

    SCOPE_EXIT({
        rm_controller.reset();

        global_context->shutdown();
        /** Explicitly destroy Context. It is more convenient than in destructor of Server, because logger is still available.
          * At this moment, no one could own shared part of Context.
          */
        global_context.reset();
        LOG_DEBUG(log, "Destroyed global context.");
    });

    // --------- START OF METRICS WRITER INIT ---------
//     std::vector<std::string> listen_hosts = DB::getMultipleValuesFromConfig(config(), "", "listen_host");

//     bool listen_try = config().getBool("listen_try", false);
//     if (listen_hosts.empty())
//     {
//         listen_hosts.emplace_back("::1");
//         listen_hosts.emplace_back("127.0.0.1");
//         listen_try = true;
//     }

//     auto make_socket_address = [&](const std::string & host, UInt16 port)
//     {
//         Poco::Net::SocketAddress socket_address;
//         try
//         {
//             socket_address = Poco::Net::SocketAddress(host, port);
//         }
//         catch (const Poco::Net::DNSException & e)
//         {
//             const auto code = e.code();
//             if (code == EAI_FAMILY
// #if defined(EAI_ADDRFAMILY)
//                 || code == EAI_ADDRFAMILY
// #endif
//                 )
//             {
//                 LOG_ERROR(log,
//                     "Cannot resolve listen_host (" << host << "), error " << e.code() << ": " << e.message() << ". "
//                     "If it is an IPv6 address and your host has disabled IPv6, then consider to "
//                     "specify IPv4 address to listen in <listen_host> element of configuration "
//                     "file. Example: <listen_host>0.0.0.0</listen_host>");
//             }

//             throw;
//         }
//         return socket_address;
//     };

//     auto socket_bind_listen = [&](auto & socket, const std::string & host, UInt16 port, bool secure = 0)
//     {
//             auto address = make_socket_address(host, port);
// #if !defined(POCO_CLICKHOUSE_PATCH) || POCO_VERSION <= 0x02000000 // TODO: fill correct version
//             if (secure)
//                 /// Bug in old poco, listen() after bind() with reusePort param will fail because have no implementation in SecureServerSocketImpl
//                 /// https://github.com/pocoproject/poco/pull/2257
//                 socket.bind(address, /* reuseAddress = */ true);
//             else
// #endif
// #if POCO_VERSION < 0x01080000
//                 socket.bind(address, /* reuseAddress = */ true);
// #else
//                 socket.bind(address, /* reuseAddress = */ true, /* reusePort = */ config().getBool("listen_reuse_port", false));
// #endif

//             socket.listen(/* backlog = */ config().getUInt("listen_backlog", 64));

//             return address;
//     };
//     std::unique_ptr<Poco::Net::HTTPServer> http_server;
//     Poco::ThreadPool server_pool(3, config().getUInt("resource_manager.http.max_connections", 1024));


//     Poco::Net::HTTPServerParams::Ptr http_params = new Poco::Net::HTTPServerParams;
//     Poco::Timespan keep_alive_timeout(config().getUInt("resource_manager.http.keep_alive_timeout", 10), 0);
//     http_params->setKeepAliveTimeout(keep_alive_timeout);

//     for (const auto & listen_host : listen_hosts)
//     {

//         auto create_server = [&](const char * port_name, auto && func)
//         {
//             /// For testing purposes, user may omit tcp_port or http_port or https_port in configuration file.
//             if (!config().has(port_name))
//                 return;

//             auto port = config().getInt(port_name);
//             try
//             {
//                 func(port);
//             }
//             catch (const Poco::Exception &)
//             {
//                 std::string message = "Listen [" + listen_host + "]:" + std::to_string(port) + " failed: " + getCurrentExceptionMessage(false);

//                 if (listen_try)
//                 {
//                     LOG_ERROR(log, message
//                         << ". If it is an IPv6 or IPv4 address and your host has disabled IPv6 or IPv4, then consider to "
//                         "specify not disabled IPv4 or IPv6 address to listen in <listen_host> element of configuration "
//                         "file. Example for disabled IPv6: <listen_host>0.0.0.0</listen_host> ."
//                         " Example for disabled IPv4: <listen_host>::</listen_host>");
//                 }
//                 else
//                 {
//                     LOG_ERROR(log, message);
//                     throw;
//                 }
//             }
//         };

//         /// Prometheus (if defined and not setup yet with http_port)
//         create_server("resource_manager.http.port", [&](UInt16 port)
//         {

//             Poco::Net::ServerSocket socket;

//             auto address = socket_bind_listen(socket, listen_host, port);
//             socket.setReceiveTimeout(config().getInt("resource_manager.http.receive_timeout", DEFAULT_HTTP_READ_BUFFER_TIMEOUT));
//             socket.setSendTimeout(config().getInt("resource_manager.http.send_timeout", DEFAULT_HTTP_READ_BUFFER_TIMEOUT));
//             auto handler_factory = new HTTPRequestHandlerFactoryMain(config(), "PrometheusHandler-factory");
//             handler_factory->addHandler<HTTPRootRequestHandlerFactory>();
//             handler_factory->addHandler<ResourceManagerPrometheusHandlerFactory>(*global_context, "resource_manager.http.prometheus");
//             http_server = std::make_unique<Poco::Net::HTTPServer>(
//                 handler_factory,
//                 server_pool,
//                 socket,
//                 http_params);


//             LOG_INFO(log, "Listening http://" + address.toString());
//         });
//     }
//     if (http_server)
//         http_server->start();

    // --------- END OF METRICS WRITER INIT ---------

    {
        // Launch brpc service
        brpc::Server brpc_server;

        int port = config().getInt("resource_manager.port");
        auto resource_manager_service = std::make_unique<RM::ResourceManagerServiceImpl>(*rm_controller);

        if (brpc_server.AddService(resource_manager_service.get(), brpc::SERVER_DOESNT_OWN_SERVICE) != 0)
        {
            LOG_ERROR(log, "Fail to add ResourceManager service.");
            return Application::EXIT_UNAVAILABLE;
        }

        brpc::ServerOptions options;
        options.idle_timeout_sec = -1;
        std::string brpc_listen_interface = createHostPortString("::", port);
        if (brpc_server.Start(brpc_listen_interface.c_str(), &options) != 0)
        {
            LOG_ERROR(log, "Fail to start Resource Manager RPC server on address {}", brpc_listen_interface);
            return Application::EXIT_UNAVAILABLE;
        }

        LOG_INFO(log, "Resource Manager service starts on address {}", brpc_listen_interface);

        waitForTerminationRequest();

        brpc_server.Stop(0);
        brpc_server.Join();
    }

    LOG_INFO(log, "Wait for ResourceManager to finish.");

    return Application::EXIT_OK;
}

}

int mainEntryClickHouseResourceManager(int argc, char ** argv)
{
    DB::ResourceManager app;
    try
    {
        return app.run(argc, argv);
    }
    catch (...)
    {
        std::cerr << DB::getCurrentExceptionMessage(true) << "\n";
        auto code = DB::getCurrentExceptionCode();

        return code ? code : -1;
    }
}
