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

#include <Storages/StorageCnchMergeTree.h>
#include <Poco/Logger.h>

namespace DB::DaemonManager
{
    inline bool isCnchTable(const StoragePtr & storage)
    {
        auto * cnch_table = dynamic_cast<StorageCnchMergeTree *>(storage.get());
        return cnch_table != nullptr;
    }

    void printConfig(std::map<std::string, unsigned int> & config, Poco::Logger * log);

    std::map<std::string, unsigned int> updateConfig(
        std::map<std::string, unsigned int> && default_config,
        const Poco::Util::AbstractConfiguration & app_config);
}
