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

#include <Interpreters/Context_fwd.h>
#include <Databases/IDatabase.h>
// #include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Dictionaries/IDictionary.h>
#include <Protos/data_models.pb.h>
#include <common/singleton.h>

namespace DB::Catalog
{

class CatalogFactory : public ext::singleton<CatalogFactory>
{

public:
    using DatabasePtr = std::shared_ptr<DB::IDatabase>;
    // using MutableDataPartPtr = std::shared_ptr<MergeTreeDataPartCNCH>;
    // using DataPartPtr = std::shared_ptr<const MergeTreeDataPartCNCH>;

    static DatabasePtr getDatabaseByDataModel(const DB::Protos::DataModelDB & db_model, const ContextPtr & context);

    static StoragePtr getTableByDataModel(ContextMutablePtr context, const DB::Protos::DataModelTable * table_model);

    static StoragePtr getTableByDefinition(ContextMutablePtr context, const String & db, const String & table, const String & create);

    static ASTPtr getCreateDictionaryByDataModel(const DB::Protos::DataModelDictionary & dict_model);
};

}
