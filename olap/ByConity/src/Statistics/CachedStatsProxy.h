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
#include <Statistics/CatalogAdaptor.h>
#include <Statistics/StatisticsBase.h>
#include <Statistics/StatsTableIdentifier.h>


namespace DB::Statistics
{
class CachedStatsProxy
{
public:
    virtual StatsData get(const StatsTableIdentifier & table_id) = 0;
    virtual StatsData get(const StatsTableIdentifier & table_id, bool table_info, const ColumnDescVector & columns) = 0;
    // usually need to std::move
    virtual void put(const StatsTableIdentifier & table_id, StatsData && data) = 0;
    virtual void drop(const StatsTableIdentifier & table_id) = 0;
    virtual void dropColumns(const StatsTableIdentifier & table_id, const ColumnDescVector & cols_desc) = 0;
    virtual ~CachedStatsProxy() = default;
};

using CachedStatsProxyPtr = std::unique_ptr<CachedStatsProxy>;
CachedStatsProxyPtr createCachedStatsProxy(const CatalogAdaptorPtr & catalog);

} // namespace DB;
