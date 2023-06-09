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

#include <DaemonManager/DaemonJob.h>
#include <Catalog/Catalog.h>

namespace DB::Protos
{
class DataModelTable;
}

namespace DB::DaemonManager
{

class DaemonJobGlobalGC : public DaemonJob
{
public:
    DaemonJobGlobalGC(ContextMutablePtr global_context_)
        : DaemonJob{global_context_, CnchBGThreadType::GlobalGC}
    {}
protected:
    bool executeImpl() override;
private:
    Catalog::IMetaStore::IteratorPtr trash_table_it;
    std::vector<DB::Protos::DataModelTable> tables_need_gc;
};

namespace GlobalGCHelpers
{

using ToServerForGCSender = std::function<bool(
    CnchServerClient & client,
    const std::vector<DB::Protos::DataModelTable> & tables_need_gc
)>;

bool sendToServerForGC(
    const std::vector<DB::Protos::DataModelTable> & tables_need_gc,
    std::vector<std::pair<String, long>> & num_of_table_can_send_sorted,
    const std::vector<CnchServerClientPtr> & server_clients,
    ToServerForGCSender sender,
    Poco::Logger * log);

std::vector<std::pair<String, long>> sortByValue(
    std::vector<std::pair<String, long>> && num_of_table_can_send);

}
}
