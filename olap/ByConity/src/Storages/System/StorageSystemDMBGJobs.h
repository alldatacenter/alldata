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

#include <Storages/System/IStorageSystemOneBlock.h>
#include <common/shared_ptr_helper.h>

namespace DB
{
class Context;

/* System table "dm_bg_jobs" for query info of bg job that managed in DaemonManager
*/
class StorageSystemDMBGJobs : public shared_ptr_helper<StorageSystemDMBGJobs>,
				   public DB::IStorageSystemOneBlock<StorageSystemDMBGJobs>
{
    friend struct shared_ptr_helper<StorageSystemDMBGJobs>;

protected:
    void fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo & query_info) const override;

    using IStorageSystemOneBlock::IStorageSystemOneBlock;

public:
    std::string getName() const override { return "SystemBackgroundJobs"; }
    static NamesAndTypesList getNamesAndTypes();

};

}
