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

#include <Storages/System/StorageSystemPersistentBGJobStatus.h>
#include <Core/NamesAndTypes.h>
#include <Columns/IColumn.h>
#include <Interpreters/Context.h>
#include <Storages/SelectQueryInfo.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <CloudServices/CnchBGThreadCommon.h>
#include <Catalog/Catalog.h>

namespace DB
{
    NamesAndTypesList StorageSystemPersistentBGJobStatus::getNamesAndTypes()
    {
        return
        {
            {"type", std::make_shared<DataTypeString>()},
            {"uuid", std::make_shared<DataTypeUUID>()},
            {"status", std::make_shared<DataTypeString>()},
        };
    }

    void StorageSystemPersistentBGJobStatus::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo &) const
    {
        /// TODO: can optimize further to reduce number of request by checking where condition

        const std::vector<CnchBGThreadType> types {
            CnchBGThreadType::MergeMutate,
            CnchBGThreadType::Clustering,
            CnchBGThreadType::PartGC,
            CnchBGThreadType::Consumer,
            CnchBGThreadType::DedupWorker
        };

        std::shared_ptr<Catalog::Catalog> catalog = context->getCnchCatalog();
        for (CnchBGThreadType type : types)
        {
            std::unordered_map<UUID, CnchBGThreadStatus> statuses = catalog->getBGJobStatuses(type);
            std::for_each(statuses.begin(), statuses.end(),
                [type, & res_columns] (const auto & p)
                {
                    size_t column_num = 0;
                    res_columns[column_num++]->insert(toString(type));
                    res_columns[column_num++]->insert(p.first);
                    res_columns[column_num++]->insert(toString(p.second));
                }
            );
        }
    }
} // end namespace
