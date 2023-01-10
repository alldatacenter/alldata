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

#include <Core/Defines.h>
#include <Core/Types.h>
#include <Poco/Util/AbstractConfiguration.h>
// #include <Core/SettingsCommon.h>
#include <Core/BaseSettings.h>
#include <Common/SettingsChanges.h>

namespace DB
{
class ASTStorage;
struct Settings;

/** Settings for the CnchHive family of engines.
  * Could be loaded from config or from a CREATE TABLE query (SETTINGS clause).
  */

#define APPLY_FOR_CNCHHIVE_SETTINGS(M) \
    /** How many rows correspond to one primary key value. */ \
    M(UInt64, index_granularity, 8192, "", 0) \
    M(String, cnch_vw_default, "vw_default", "", 0) \
    M(String, cnch_vw_read, "vw_read", "", 0) \
    M(String, cnch_vw_write, "vw_write", "", 0) \
    M(String, cnch_vw_task, "vw_task", "", 0) \
    /** Parquet skip useless row group */ \
    M(Bool, enable_skip_row_group, false, "", 0) \
    /** DBMS_DEFAULT_CONNECT_TIMEOUT_SEC 10 **/ \
    M(UInt64, hive_metastore_client_conn_timeout, DBMS_DEFAULT_CONNECT_TIMEOUT_SEC * 1000, "", 0) \
    /** DBMS_DEFAULT_RECEIVE_TIMEOUT_SEC 300 **/ \
    M(UInt64, hive_metastore_client_recv_timeout, DBMS_DEFAULT_RECEIVE_TIMEOUT_SEC * 100, "", 0) \
    M(UInt64, hive_metastore_client_send_timeout, DBMS_DEFAULT_SEND_TIMEOUT_SEC * 100, "", 0) \
    M(UInt64, max_hive_metastore_client_retry, 3, "", 0) \
    M(UInt64, get_hive_metastore_client_timeout, DBMS_DEFAULT_CONNECT_TIMEOUT_SEC * 1000, "", 0) \
    M(UInt64, max_hive_metastore_client_connections, 64, "", 0) \
    /** allocate part policy**/ \
    M(Bool, use_simple_hash, true, "", 0) \
    /** parallel read parquet max threads **/ \
    M(UInt64, max_read_row_group_threads, 32, "", 0) \
    M(Bool, cnch_temporary_table, 0, "", 0)


/// Settings that should not change after the creation of a table.
#define APPLY_FOR_IMMUTABLE_CNCH_HIVE_SETTINGS(M) M(index_granularity)

DECLARE_SETTINGS_TRAITS(CnchHiveSettingsTraits, APPLY_FOR_CNCHHIVE_SETTINGS)


struct CnchHiveSettings : public BaseSettings<CnchHiveSettingsTraits>
{
public:
    void loadFromConfig(const String & config_elem, const Poco::Util::AbstractConfiguration & config);

    /// NOTE: will rewrite the AST to add immutable settings.
    void loadFromQuery(ASTStorage & storage_def);

    // void applyChange(const SettingChange & change);
    // void applyChanges(const SettingsChanges & changes);

    // void set(const String & key, const Field & value);

    // bool hasKey(const String & key);
};

}
