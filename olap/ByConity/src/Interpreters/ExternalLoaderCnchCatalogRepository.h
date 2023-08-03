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

#include <Interpreters/IExternalLoaderConfigRepository.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/StorageID.h>
#include <Protos/data_models.pb.h>
#include <unordered_map>

namespace DB
{

namespace Catalog
{
class Catalog;
}

class CnchCatalogDictionaryCache
{
public:
    CnchCatalogDictionaryCache(ContextPtr context);
    void loadFromCatalog();
    std::set<std::string> getAllUUIDString() const;
    bool exists(const String & uuid_str) const;
    Poco::Timestamp getUpdateTime(const String & uuid_str) const;
    LoadablesConfigurationPtr load(const String & uuid_str) const;
    std::optional<UUID> findUUID(const StorageID & storage_id) const;
private:
    ContextPtr context;
    std::shared_ptr<Catalog::Catalog> catalog;
    std::unordered_map<String, DB::Protos::DataModelDictionary> data;
    mutable std::mutex data_mutex;
};

/// Cnch Catalog repository used by ExternalLoader
class ExternalLoaderCnchCatalogRepository : public IExternalLoaderConfigRepository
{
public:
    explicit ExternalLoaderCnchCatalogRepository(ContextPtr context);

    std::string getName() const override;

    std::set<std::string> getAllLoadablesDefinitionNames() override;

    bool exists(const std::string & loadable_definition_name) override;

    Poco::Timestamp getUpdateTime(const std::string & loadable_definition_name) override;

    LoadablesConfigurationPtr load(const std::string & loadable_definition_name) override;

    static StorageID parseStorageID(const std::string & loadable_definition_name);
    static std::optional<UUID> resolveDictionaryName(const std::string & name, const std::string & current_database_name, ContextPtr context);
private:
    /// cache data from catalog
    CnchCatalogDictionaryCache & cache;
    std::shared_ptr<Catalog::Catalog> catalog;
};

}
