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
#include <Common/ConfigurationCommon.h>
#include <Coordination/Defines.h>

namespace DB
{
#define RM_CONFIG_FIELDS_LIST(M) \
    M(UInt64, port, "", 9000, ConfigFlag::Default, "desc: rpc port") \
    M(String, election_path, "", RESOURCE_MANAGER_ELECTION_DEFAULT_PATH, ConfigFlag::Default, "") \
    M(UInt64, init_client_tries, "", 3, ConfigFlag::Default, "") \
    M(UInt64, init_client_retry_interval_ms, "", 3000, ConfigFlag::Default, "") \
    M(UInt64, max_retry_times, "", 3, ConfigFlag::Default, "") \
    M(UInt64, check_leader_info_interval_ms, "", 1000, ConfigFlag::Default, "") \
    M(UInt64, wait_before_become_leader_ms, "", 3000, ConfigFlag::Default, "") \
    M(Bool, enable_auto_resource_sharing, "", false, ConfigFlag::Default, "") \
    M(UInt64, auto_resource_sharing_task_interval_ms, "", 5000, ConfigFlag::Default, "") \
    M(UInt64, worker_register_visible_granularity_sec, "", 5, ConfigFlag::Default, "change workers' state from Registering to Running every N seconds to avoid changing worker topology frequently.") \
    M(UInt64, lost_worker_timeout_seconds, "", 10, ConfigFlag::Default, "") \

DECLARE_CONFIG_DATA(RMConfigurationData, RM_CONFIG_FIELDS_LIST)
struct RMConfiguration final : public RMConfigurationData
{
};

#define SD_CONFIG_FIELDS_LIST(M) \
    M(String, mode, "", "local", ConfigFlag::Recommended, "") \
    M(String, server_psm, "server.psm", "data.cnch.server", ConfigFlag::Recommended, "") \
    M(String, vw_psm, "vw.psm", "data.cnch.vw", ConfigFlag::Recommended, "") \
    M(String, tso_psm, "tso.psm", "data.cnch.tso", ConfigFlag::Recommended, "") \
    M(String, daemon_manager_psm, "daemon_manager.psm", "data.cnch.daemon_manager", ConfigFlag::Recommended, "") \
    M(String, resource_manager_psm, "resource_manager.psm", "data.cnch.resource_manager", ConfigFlag::Default, "") \

DECLARE_CONFIG_DATA(SDConfigurationData, SD_CONFIG_FIELDS_LIST)

struct SDConfiguration final : public SDConfigurationData
{
};


#define ROOT_CONFIG_FIELDS_LIST(M) \
    M(UInt64, tcp_port, "", 9000, ConfigFlag::Recommended, "") \
    M(UInt64, http_port, "", 8123, ConfigFlag::Recommended, "") \
    M(UInt64, rpc_port, "", 9100, ConfigFlag::Recommended, "") \
    M(UInt64, exchange_port, "", 0, ConfigFlag::Default, "") \
    M(UInt64, exchange_status_port, "", 0, ConfigFlag::Default, "") \
    M(Int64, keep_alive_timeout, "", 10, ConfigFlag::Default, "") \
    M(UInt64, max_connections, "", 1024 * 16, ConfigFlag::Default, "") \
    M(Bool, listen_try, "", false, ConfigFlag::Default, "") \
    M(Bool, listen_reuse_port, "", false, ConfigFlag::Default, "") \
    M(UInt64, listen_backlog, "", 64, ConfigFlag::Default, "") \
    M(UInt64, asynchronous_metrics_update_period_s, "", 60, ConfigFlag::Default, "") \
    M(String, cnch_type, "", "server", ConfigFlag::Recommended, "") \
    M(UInt64, max_concurrent_queries, "", 0, ConfigFlag::Default, "") \
    M(UInt64, max_concurrent_insert_queries, "", 0, ConfigFlag::Default, "") \
    M(UInt64, max_concurrent_system_queries, "", 0, ConfigFlag::Default, "") \
    M(Float32, cache_size_to_ram_max_ratio, "", 0.5, ConfigFlag::Default, "") \
    M(UInt64, uncompressed_cache_size, "", 0, ConfigFlag::Default, "") \
    M(UInt64, mark_cache_size, "", 5368709120, ConfigFlag::Default, "") \
    M(UInt64, cnch_checksums_cache_size, "", 5368709120, ConfigFlag::Default, "") \
    M(UInt64, shutdown_wait_unfinished, "", 5, ConfigFlag::Default, "") \
    M(UInt64, cnch_transaction_ts_expire_time, "", 2 * 60 * 60 * 1000, ConfigFlag::Default, "") \
    M(UInt64, cnch_task_heartbeat_interval, "", 5, ConfigFlag::Default, "") \
    M(UInt64, cnch_task_heartbeat_max_retries, "", 5, ConfigFlag::Default, "") \
    /**
     * Mutable */ \
    M(MutableUInt64, max_server_memory_usage, "", 0, ConfigFlag::Default, "") \
    M(MutableFloat32, max_server_memory_usage_to_ram_ratio, "", 0.8, ConfigFlag::Default, "") \
    M(MutableUInt64, kafka_max_partition_fetch_bytes, "", 1048576, ConfigFlag::Default, "") \
    M(MutableUInt64, stream_poll_timeout_ms, "", 500, ConfigFlag::Default, "") \
    M(MutableUInt64, debug_disable_merge_mutate_thread, "", false, ConfigFlag::Default, "") \
    M(MutableBool, debug_disable_merge_commit, "", false, ConfigFlag::Default, "") \
    /**
     * Might be removed */ \
    M(MutableUInt64, max_table_size_to_drop, "", 50000000000lu, ConfigFlag::Default, "") \
    M(MutableUInt64, max_partition_size_to_drop, "", 50000000000lu, ConfigFlag::Default, "") \
    M(MutableUInt64, databases_load_pool_size, "", 3, ConfigFlag::Default, "") \
    M(MutableUInt64, tables_load_pool_size, "", 8, ConfigFlag::Default, "") \
    M(MutableUInt64, parts_load_pool_size, "", 48, ConfigFlag::Default, "")

DECLARE_CONFIG_DATA(RootConfigurationData, ROOT_CONFIG_FIELDS_LIST)


struct RootConfiguration final : public RootConfigurationData
{
    RMConfiguration resource_manager;
    SDConfiguration service_discovery;

    RootConfiguration()
    {
        sub_configs.push_back(&resource_manager);
        sub_configs.push_back(&service_discovery);
    }

    void loadFromPocoConfigImpl(const PocoAbstractConfig & config, const String & current_prefix) override;
};

}
