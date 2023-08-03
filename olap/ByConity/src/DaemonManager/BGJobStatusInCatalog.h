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

#include <Core/Types.h>
#include <CloudServices/CnchBGThreadCommon.h>
#include <Catalog/Catalog.h>

namespace DB
{
namespace BGJobStatusInCatalog
{
    CnchBGThreadStatus deserializeFromChar(char c);
    CnchBGThreadStatus deserializeFromString(const std::string & str);
    char serializeToChar(CnchBGThreadStatus status);

    /// This class is used to inject dependency of catalog into BackgroundJob,
    /// so that write unit testing are easier
    class IBGJobStatusPersistentStoreProxy
    {
    public:
        /// clear the statuses_cache in dtor
        class CacheClearer
        {
            public:
                CacheClearer() = default;
                CacheClearer(IBGJobStatusPersistentStoreProxy *);
                CacheClearer(const CacheClearer &) = delete;
                CacheClearer(CacheClearer &&);
                CacheClearer & operator=(CacheClearer &&);
                CacheClearer & operator=(const CacheClearer &) = delete;
                ~CacheClearer();
            private:
                IBGJobStatusPersistentStoreProxy * proxy = nullptr;
        };

        /// return non-empty result if there is status already exist
        virtual std::optional<CnchBGThreadStatus> createStatusIfNotExist(const StorageID & storage_id, CnchBGThreadStatus init_status) const = 0;
        virtual void setStatus(const UUID & table_uuid, CnchBGThreadStatus status) const = 0;
        virtual CnchBGThreadStatus getStatus(const UUID & table_uuid,  bool use_cache) const = 0;
        virtual CacheClearer fetchStatusesIntoCache() = 0;
        virtual ~IBGJobStatusPersistentStoreProxy() = default;
    protected:
        virtual void clearCache() {}
    };

    /// this class support get/setStatus operator for each table by calling coresponding methods in Catalog,
    /// it also support batch getStatus by fetch statuses of all tables
    /// into cache , after that the getStatus will use cache data instead of fetching data from Catalog
    class CatalogBGJobStatusPersistentStoreProxy : public IBGJobStatusPersistentStoreProxy
    {
    public:
        CatalogBGJobStatusPersistentStoreProxy(std::shared_ptr<Catalog::Catalog>, CnchBGThreadType);

        std::optional<CnchBGThreadStatus> createStatusIfNotExist(const StorageID & storage_id, CnchBGThreadStatus init_status) const override;
        void setStatus(const UUID & table_uuid, CnchBGThreadStatus status) const override;
        CnchBGThreadStatus getStatus(const UUID & table_uuid, bool use_cache) const override;
        CacheClearer fetchStatusesIntoCache() override;
    protected:
        void clearCache() override;
        /// keep member variables protected for testing
        std::shared_ptr<Catalog::Catalog> catalog;
        std::unordered_map<UUID, CnchBGThreadStatus> statuses_cache;
        CnchBGThreadType type;
    };
}

}
