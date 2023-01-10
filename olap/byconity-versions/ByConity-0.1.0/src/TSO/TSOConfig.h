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
#include <Poco/Util/AbstractConfiguration.h>
#include <Common/Exception.h>
#include <Core/Types.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int TSO_OPERATION_ERROR;
}

namespace TSO
{

enum class StoreType
{
    UNINIT,
    BYTEKV,
    FDB
};

struct TSOConfig
{
    TSOConfig() {}

    TSOConfig(const Poco::Util::AbstractConfiguration & poco_config)
    {
        if (poco_config.has("tso_service"))
        {
            if (poco_config.has("tso_service.type"))
            {
                std::string metastore_type = poco_config.getString("tso_service.type");
                if (metastore_type == "fdb")
                    type = StoreType::FDB;
                else if (metastore_type == "bytekv")
                    type = StoreType::BYTEKV;
                else
                    throw Exception("Unsupportted metastore type " + metastore_type, ErrorCodes::TSO_OPERATION_ERROR);
            }

            if (type == StoreType::FDB)
            {
                fdb_conf.cluster_conf_path = poco_config.getString("tso_service.fdb.cluster_file");
            }
            else if (type == StoreType::BYTEKV)
            {
                bytekv_conf.service_name = poco_config.getString("tso_service.bytekv.service_name");
                bytekv_conf.cluster_name = poco_config.getString("tso_service.bytekv.cluster_name");
                bytekv_conf.name_space = poco_config.getString("tso_service.bytekv.name_space");
                bytekv_conf.table_name = poco_config.getString("tso_service.bytekv.table_name");
            }

            key_name = poco_config.getString("tso_service.key_name", "tso");
        }
    }

    struct FDBConf
    {
        String cluster_conf_path;
    };

    struct ByteKVConf
    {
        String service_name;
        String cluster_name;
        String name_space;
        String table_name;
    };

    StoreType type = StoreType::UNINIT;
    FDBConf fdb_conf;
    ByteKVConf bytekv_conf;

    String key_name;
};

}

}
