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

#include <Coordination/KeeperStateManager.h>

#include <Coordination/Defines.h>
#include <Common/Exception.h>
#include <Common/isLocalAddress.h>
#include <ServiceDiscovery/ServiceDiscoveryConsul.h>
#include <ServiceDiscovery/ServiceDiscoveryLocal.h>
#include <ServiceDiscovery/ServiceDiscoveryFactory.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadBufferFromFile.h>
#include <libnuraft/srv_config.hxx>

#include <filesystem>
#include <string>

namespace DB
{

namespace ErrorCodes
{
    extern const int RAFT_ERROR;
    extern const int CORRUPTED_DATA;
}
namespace
{

struct KeeperEndpoint
{
    String host;
    Int32 port;
    Int32 id;
    Int32 priority{1};
    bool can_become_leader{true};
    bool start_as_follower{false};
};

std::vector<KeeperEndpoint> parseKeeperEndpointsFromServiceDiscovery(const Poco::Util::AbstractConfiguration & config)
{
    std::vector<KeeperEndpoint> result;

    auto service_discovery = ServiceDiscoveryFactory::instance().get(config);
    /// TODO: fallback to service_discovery.tso.psm
    auto psm = config.getString("service_discovery.keeper.psm");
    auto endpoints = service_discovery->lookupEndpoints(psm);

    for (auto & endpoint: endpoints)
    {
        if (!endpoint.tags.count("PORT1"))
            throw Exception(ErrorCodes::BAD_ARGUMENTS, "Can't find `PORT1`(Keeper interserver port) from service_discovery");
        if (!endpoint.tags.count("id"))
            throw Exception(ErrorCodes::BAD_ARGUMENTS, "Can't find `id`(Raft instance id) from service_discovery");

        result.emplace_back(KeeperEndpoint{endpoint.host, std::stoi(endpoint.tags.at("PORT1")), std::stoi(endpoint.tags.at("id"))});
        if (auto it = endpoint.tags.find("priority"); it != endpoint.tags.end())
            result.back().priority = std::stoi(it->second);
        if (auto it = endpoint.tags.find("can_become_leader"); it != endpoint.tags.end())
            result.back().priority = std::stoi(it->second);
        if (auto it = endpoint.tags.find("start_as_follower"); it != endpoint.tags.end())
            result.back().start_as_follower = std::stoi(it->second);
    }

    return result;
}

std::vector<KeeperEndpoint> parseKeeperEndpointsFromConfiguration(const Poco::Util::AbstractConfiguration & config, const String & config_prefix)
{
    std::vector<KeeperEndpoint> result;
    Poco::Util::AbstractConfiguration::Keys keys;
    config.keys(config_prefix + ".raft_configuration", keys);

    for (const auto & server_key : keys)
    {
        if (!startsWith(server_key, "server"))
            continue;

        std::string full_prefix = config_prefix + ".raft_configuration." + server_key;
        std::string hostname = config.getString(full_prefix + ".hostname");
        int port = config.getInt(full_prefix + ".port");
        int new_server_id = config.getInt(full_prefix + ".id");
        int32_t priority = config.getInt(full_prefix + ".priority", 1);
        bool can_become_leader = config.getBool(full_prefix + ".can_become_leader", true);
        bool start_as_follower = config.getBool(full_prefix + ".start_as_follower", false);

        result.emplace_back(KeeperEndpoint{hostname, port, new_server_id, priority, can_become_leader, start_as_follower});
    }
    return result;
}

}

KeeperStateManager::KeeperConfigurationWrapper
KeeperStateManager::parseServersConfiguration(const Poco::Util::AbstractConfiguration & config, bool allow_without_us) const
{
    std::vector<KeeperEndpoint> endpoints;

    if (config.has(config_prefix + ".raft_configuration"))
        endpoints = parseKeeperEndpointsFromConfiguration(config, config_prefix);
    else
        endpoints = parseKeeperEndpointsFromServiceDiscovery(config);

    KeeperConfigurationWrapper result;
    result.cluster_config = std::make_shared<nuraft::cluster_config>();

    for (auto & endpoint: endpoints)
    {
        auto endpoint_name = endpoint.host + ":" + std::to_string(endpoint.port);
        auto peer_config = nuraft::cs_new<nuraft::srv_config>(endpoint.id, 0, endpoint_name, "", !endpoint.can_become_leader, endpoint.priority);

        if (endpoint.id == my_server_id)
        {
            result.config = peer_config;
            result.port = endpoint.port;
        }

        result.cluster_config->get_servers().push_back(peer_config);

        if (endpoint.start_as_follower)
            result.servers_start_as_followers.insert(endpoint.id);
    }

    if (!result.config && !allow_without_us)
        throw Exception(ErrorCodes::RAFT_ERROR, "Our server id {} not found in raft_configuration section", my_server_id);

    if (result.servers_start_as_followers.size() == endpoints.size())
        throw Exception(ErrorCodes::RAFT_ERROR, "At least one of servers should be able to start as leader (without <start_as_follower>)");

    return result;
}

KeeperStateManager::KeeperStateManager(
    int server_id_, const std::string & host, int port, const std::string & logs_path, const std::string & state_file_path)
    : my_server_id(server_id_)
    , secure(false)
    , log_store(nuraft::cs_new<KeeperLogStore>(logs_path, 5000, false, false))
    , server_state_path(state_file_path)
    , logger(&Poco::Logger::get("KeeperStateManager"))
{
    auto peer_config = nuraft::cs_new<nuraft::srv_config>(my_server_id, host + ":" + std::to_string(port));
    configuration_wrapper.cluster_config = nuraft::cs_new<nuraft::cluster_config>();
    configuration_wrapper.port = port;
    configuration_wrapper.config = peer_config;
    configuration_wrapper.cluster_config->get_servers().push_back(peer_config);
}

KeeperStateManager::KeeperStateManager(
    int server_id_,
    const std::string & config_prefix_,
    const std::string & log_storage_path,
    const std::string & state_file_path,
    const Poco::Util::AbstractConfiguration & config,
    const CoordinationSettingsPtr & coordination_settings)
    : my_server_id(server_id_)
    , secure(config.getBool(config_prefix_ + ".raft_configuration.secure", false))
    , config_prefix(config_prefix_)
    , configuration_wrapper(parseServersConfiguration(config, false))
    , log_store(nuraft::cs_new<KeeperLogStore>(
          log_storage_path,
          coordination_settings->rotate_log_storage_interval,
          coordination_settings->force_sync,
          coordination_settings->compress_logs))
    , server_state_path(state_file_path)
    , logger(&Poco::Logger::get("KeeperStateManager"))
{
}

void KeeperStateManager::loadLogStore(uint64_t last_commited_index, uint64_t logs_to_keep)
{
    log_store->init(last_commited_index, logs_to_keep);
}

ClusterConfigPtr KeeperStateManager::getLatestConfigFromLogStore() const
{
    auto entry_with_change = log_store->getLatestConfigChange();
    if (entry_with_change)
        return ClusterConfig::deserialize(entry_with_change->get_buf());
    return nullptr;
}

void KeeperStateManager::flushLogStore()
{
    log_store->flush();
}

void KeeperStateManager::save_config(const nuraft::cluster_config & config)
{
    std::lock_guard lock(configuration_wrapper_mutex);
    nuraft::ptr<nuraft::buffer> buf = config.serialize();
    configuration_wrapper.cluster_config = nuraft::cluster_config::deserialize(*buf);
}

const std::filesystem::path & KeeperStateManager::getOldServerStatePath()
{
    static auto old_path = [this]
    {
        return server_state_path.parent_path() / (server_state_path.filename().generic_string() + "-OLD");
    }();

    return old_path;
}

namespace
{
enum ServerStateVersion : uint8_t
{
    V1 = 0
};

constexpr auto current_server_state_version = ServerStateVersion::V1;

}

void KeeperStateManager::save_state(const nuraft::srv_state & state)
{
    const auto & old_path = getOldServerStatePath();

    if (std::filesystem::exists(server_state_path))
        std::filesystem::rename(server_state_path, old_path);

    WriteBufferFromFile server_state_file(server_state_path, DBMS_DEFAULT_BUFFER_SIZE, O_TRUNC | O_CREAT | O_WRONLY);
    auto buf = state.serialize();

    // calculate checksum
    SipHash hash;
    hash.update(current_server_state_version);
    hash.update(reinterpret_cast<const char *>(buf->data_begin()), buf->size());
    writeIntBinary(hash.get64(), server_state_file);

    writeIntBinary(static_cast<uint8_t>(current_server_state_version), server_state_file);

    server_state_file.write(reinterpret_cast<const char *>(buf->data_begin()), buf->size());
    server_state_file.sync();
    server_state_file.close();

    std::filesystem::remove(old_path);
}

nuraft::ptr<nuraft::srv_state> KeeperStateManager::read_state()
{
    const auto & old_path = getOldServerStatePath();

    const auto try_read_file = [this](const auto & path) -> nuraft::ptr<nuraft::srv_state>
    {
        try
        {
            ReadBufferFromFile read_buf(path);
            auto content_size = read_buf.size(); /// TODO: getFileSize() (see https://github.com/ClickHouse/ClickHouse/pull/38227/files)

            if (content_size == 0)
                return nullptr;

            uint64_t read_checksum{0};
            readIntBinary(read_checksum, read_buf);

            uint8_t version;
            readIntBinary(version, read_buf);

            auto buffer_size = content_size - sizeof read_checksum - sizeof version;

            auto state_buf = nuraft::buffer::alloc(buffer_size);
            read_buf.read(reinterpret_cast<char *>(state_buf->data_begin()), buffer_size);

            SipHash hash;
            hash.update(version);
            hash.update(reinterpret_cast<const char *>(state_buf->data_begin()), state_buf->size());

            if (read_checksum != hash.get64())
            {
                const auto error_string = fmt::format(
                    "Invalid checksum while reading state from {}. Got {}, expected {}",
                    path.generic_string(),
                    hash.get64(),
                    read_checksum);
#ifdef NDEBUG
                LOG_ERROR(logger, error_string);
                return nullptr;
#else
                throw Exception(ErrorCodes::CORRUPTED_DATA, error_string);
#endif
            }

            auto state = nuraft::srv_state::deserialize(*state_buf);
            LOG_INFO(logger, "Read state from {}", path.generic_string());
            return state;
        }
        catch (const std::exception & e)
        {
            if (const auto * exception = dynamic_cast<const Exception *>(&e);
                exception != nullptr && exception->code() == ErrorCodes::CORRUPTED_DATA)
            {
                throw;
            }

            LOG_ERROR(logger, "Failed to deserialize state from {}", path.generic_string());
            return nullptr;
        }
    };

    if (std::filesystem::exists(server_state_path))
    {
        auto state = try_read_file(server_state_path);

        if (state)
        {
            if (std::filesystem::exists(old_path))
                std::filesystem::remove(old_path);

            return state;
        }

        std::filesystem::remove(server_state_path);
    }

    if (std::filesystem::exists(old_path))
    {
        auto state = try_read_file(old_path);

        if (state)
        {
            std::filesystem::rename(old_path, server_state_path);
            return state;
        }

        std::filesystem::remove(old_path);
    }

    LOG_WARNING(logger, "No state was read");
    return nullptr;
}

ConfigUpdateActions KeeperStateManager::getConfigurationDiff(const Poco::Util::AbstractConfiguration & config) const
{
    auto new_configuration_wrapper = parseServersConfiguration(config, true);

    std::unordered_map<int, KeeperServerConfigPtr> new_ids, old_ids;
    for (const auto & new_server : new_configuration_wrapper.cluster_config->get_servers())
        new_ids[new_server->get_id()] = new_server;

    {
        std::lock_guard lock(configuration_wrapper_mutex);
        for (const auto & old_server : configuration_wrapper.cluster_config->get_servers())
            old_ids[old_server->get_id()] = old_server;
    }

    ConfigUpdateActions result;

    /// First of all add new servers
    for (auto [new_id, server_config] : new_ids)
    {
        if (!old_ids.count(new_id))
            result.emplace_back(ConfigUpdateAction{ConfigUpdateActionType::AddServer, server_config});
    }

    /// After that remove old ones
    for (auto [old_id, server_config] : old_ids)
    {
        if (!new_ids.count(old_id))
            result.emplace_back(ConfigUpdateAction{ConfigUpdateActionType::RemoveServer, server_config});
    }

    {
        std::lock_guard lock(configuration_wrapper_mutex);
        /// And update priority if required
        for (const auto & old_server : configuration_wrapper.cluster_config->get_servers())
        {
            for (const auto & new_server : new_configuration_wrapper.cluster_config->get_servers())
            {
                if (old_server->get_id() == new_server->get_id())
                {
                    if (old_server->get_priority() != new_server->get_priority())
                    {
                        result.emplace_back(ConfigUpdateAction{ConfigUpdateActionType::UpdatePriority, new_server});
                    }
                    break;
                }
            }
        }
    }

    return result;
}

}
