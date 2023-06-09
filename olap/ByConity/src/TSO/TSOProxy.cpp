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

#include <TSO/TSOProxy.h>
//#include <TSO/TSOMetaByteKVImpl.h>
#include <TSO/TSOMetaFDBImpl.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int TSO_INTERNAL_ERROR;
}

namespace TSO
{

TSOProxy::TSOProxy(const TSOConfig & config)
{
    if (config.type == StoreType::FDB)
    {
        metastore_ptr = std::make_shared<TSOMetaFDBImpl>(config.fdb_conf.cluster_conf_path, config.key_name);
    }
    else
        throw Exception("TSO metastore type should be set. Only support foundationdb and bytekv.", ErrorCodes::TSO_INTERNAL_ERROR);
}

void TSOProxy::setTimestamp(UInt64 timestamp)
{
    metastore_ptr->put(std::to_string(timestamp));
}

UInt64 TSOProxy::getTimestamp()
{
    String timestamp_str;
    metastore_ptr->get(timestamp_str);
    if (timestamp_str.empty())
    {
        return 0;
    }
    else
    {
        return std::stoull(timestamp_str);
    }
}

void TSOProxy::clean()
{
    metastore_ptr->clean();
}

}

}
