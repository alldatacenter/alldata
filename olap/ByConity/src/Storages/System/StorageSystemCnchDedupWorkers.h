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

#include <CloudServices/CnchBGThreadsMap.h>
#include <Storages/System/IStorageSystemOneBlock.h>
#include <common/shared_ptr_helper.h>

namespace DB
{
class StorageSystemCnchDedupWorkers : public shared_ptr_helper<StorageSystemCnchDedupWorkers>,
                                  public IStorageSystemOneBlock<StorageSystemCnchDedupWorkers>
{
public:
    explicit StorageSystemCnchDedupWorkers(const StorageID & table_id_) : IStorageSystemOneBlock(table_id_) { }

    std::string getName() const override { return "SystemDedupWorkers"; }

    static NamesAndTypesList getNamesAndTypes();

    ColumnsDescription getColumnsAndAlias();

protected:
    const FormatSettings format_settings;

    void fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo & query_info) const override;

    void fillDataOnServer(
        MutableColumns & res_columns,
        const ColumnPtr & needed_uuids,
        const std::map<UUID, StoragePtr> & tables,
        ContextPtr context,
        const UUIDToBGThreads & uuid_to_threads) const;

    void fillDataOnWorker(
        MutableColumns & res_columns, const ColumnPtr & needed_uuids, const std::map<UUID, StoragePtr> & tables, ContextPtr context) const;
};
}
