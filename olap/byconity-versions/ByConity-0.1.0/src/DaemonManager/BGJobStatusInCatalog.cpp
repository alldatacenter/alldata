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

#include <DaemonManager/BGJobStatusInCatalog.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}


namespace BGJobStatusInCatalog
{

CnchBGThreadStatus deserializeFromChar(char c)
{
    if (c == '0')
        return CnchBGThreadStatus::Running;
    else if (c == '1')
        return CnchBGThreadStatus::Stopped;
    else if (c == '2')
        return CnchBGThreadStatus::Removed;

    throw Exception(String("Invalid argument to function toJobStatus: ") + c, ErrorCodes::LOGICAL_ERROR);
}

CnchBGThreadStatus deserializeFromString(const std::string & str)
{
    if (str.size() != 1)
        throw Exception("background thread status size is not correct", ErrorCodes::LOGICAL_ERROR);

    return BGJobStatusInCatalog::deserializeFromChar(str[0]);
}

char serializeToChar(CnchBGThreadStatus status)
{
    if (status == CnchBGThreadStatus::Running)
        return '0';
    if (status == CnchBGThreadStatus::Stopped)
        return '1';
    if (status == CnchBGThreadStatus::Removed)
        return '2';

    throw Exception("Invalid argument to function toJobStatus: " + toString(static_cast<std::underlying_type<CnchBGThreadStatus>::type>(status)), ErrorCodes::LOGICAL_ERROR);
}

CatalogBGJobStatusPersistentStoreProxy::CatalogBGJobStatusPersistentStoreProxy(
    std::shared_ptr<Catalog::Catalog> catalog_,
    CnchBGThreadType type_)
    : catalog{std::move(catalog_)}, statuses_cache{}, type{type_}
{}

std::optional<CnchBGThreadStatus> CatalogBGJobStatusPersistentStoreProxy::createStatusIfNotExist(const StorageID & storage_id, CnchBGThreadStatus init_status) const
{
    std::optional<CnchBGThreadStatus> status = catalog->getBGJobStatus(storage_id.uuid, type);
    if (!status)
    {
        setStatus(storage_id.uuid, init_status);
        return {};
    }
    else
    {
        return status.value();
    }
}

void CatalogBGJobStatusPersistentStoreProxy::setStatus(const UUID & table_uuid, CnchBGThreadStatus status) const
{
    catalog->setBGJobStatus(table_uuid, type, status);
}

CnchBGThreadStatus CatalogBGJobStatusPersistentStoreProxy::getStatus(const UUID & table_uuid, bool use_cache) const
{
    if (use_cache)
    {
        if (statuses_cache.empty())
            throw Exception("statuses cache is empty, it need to be fetch ahead, this is developer mistake", ErrorCodes::LOGICAL_ERROR);
        else
        {
            auto it = statuses_cache.find(table_uuid);
            if (it == statuses_cache.end())
                throw Exception("can't find uuid in statues cache, this is program logic issue", ErrorCodes::LOGICAL_ERROR);
            return it->second;
        }
    }

    throw Exception("getStatus without using cache, this is developer mistake", ErrorCodes::LOGICAL_ERROR);
}

IBGJobStatusPersistentStoreProxy::CacheClearer CatalogBGJobStatusPersistentStoreProxy::fetchStatusesIntoCache()
{
    if (!statuses_cache.empty())
        throw Exception("Status cache is not empty, this is program logic error", ErrorCodes::LOGICAL_ERROR);

    if (catalog) // catalog is nullptr in unittest
        statuses_cache = catalog->getBGJobStatuses(type);
    return CacheClearer{this};
}

IBGJobStatusPersistentStoreProxy::CacheClearer::CacheClearer(IBGJobStatusPersistentStoreProxy * proxy_)
    :proxy{proxy_}
{}

IBGJobStatusPersistentStoreProxy::CacheClearer::CacheClearer(CacheClearer && other)
    :proxy{other.proxy}
{
    other.proxy = nullptr;
}

IBGJobStatusPersistentStoreProxy::CacheClearer & IBGJobStatusPersistentStoreProxy::CacheClearer::operator=(CacheClearer && other)
{
    std::swap(proxy, other.proxy);
    /// calling clear up in local dtor
    CacheClearer local{std::move(other)};
    return *this;
}

IBGJobStatusPersistentStoreProxy::CacheClearer::~CacheClearer()
{
    if (proxy)
        proxy->clearCache();
}

void CatalogBGJobStatusPersistentStoreProxy::clearCache()
{
    statuses_cache.clear();
}

} /// end namespace DB::BGJobStatusInCatalog

} // end namespace DB
