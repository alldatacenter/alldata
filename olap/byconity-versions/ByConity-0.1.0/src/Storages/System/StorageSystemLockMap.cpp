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

#include <Storages/System/StorageSystemLockMap.h>

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesNumber.h>
#include <Protos/data_models.pb.h>
#include <Protos/DataModelHelpers.h>
#include <Transaction/LockManager.h>
#include <Transaction/LockDefines.h>

namespace DB
{
NamesAndTypesList StorageSystemLockMap::getNamesAndTypes()
{
    return {
        {"level", std::make_shared<DataTypeString>()},
        {"uuid", std::make_shared<DataTypeUUID>()},
        {"bucket_id", std::make_shared<DataTypeInt64>()},
        {"partition", std::make_shared<DataTypeString>()},
        {"granted_modes", std::make_shared<DataTypeString>()},
        {"conflicted_modes", std::make_shared<DataTypeString>()},
        {"granted_txns", std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>())},
        {"waiting_txns", std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>())},
    };
}

void StorageSystemLockMap::fillData(MutableColumns & res_columns, ContextPtr, const SelectQueryInfo &) const
{
    LockMaps & lk_maps = LockManager::instance().getLockMaps();
    UInt32 level = 0;
    for (LockMap & lk_map : lk_maps)
    {
        for (LockMapStripe & stripe : lk_map.map_stripes)
        {
            std::lock_guard lg(stripe.mutex);
            for (const auto & [key, lock_context] : stripe.map)
            {
                Protos::DataModelLockField lock_key_model;
                lock_key_model.ParseFromString(key);
                UUID uuid = RPCHelpers::createUUID(lock_key_model.uuid());
                Int64 bucket_id = lock_key_model.has_bucket() ? lock_key_model.bucket() : -1;
                String partition = lock_key_model.has_partition() ? lock_key_model.partition() : "";

                auto [granted_mode, conflicted_mode] = lock_context.getModes();
                Array granted_txns, waiting_txns;
                for (const auto & granted_req : lock_context.getGrantedList())
                    granted_txns.emplace_back(granted_req->getTransactionID().toUInt64());

                for (const auto & waiting_req : lock_context.getWaitingList())
                    waiting_txns.emplace_back(waiting_req->getTransactionID().toUInt64());

                size_t c = 0;
                res_columns[c++]->insert(toString(static_cast<LockLevel>(level)));
                res_columns[c++]->insert(uuid);
                res_columns[c++]->insert(bucket_id);
                res_columns[c++]->insert(partition);
                res_columns[c++]->insert(lockModesToDebugString(granted_mode));
                res_columns[c++]->insert(lockModesToDebugString(conflicted_mode));
                res_columns[c++]->insert(granted_txns);
                res_columns[c++]->insert(waiting_txns);
            }
        }

        ++level;
    }
}

}
