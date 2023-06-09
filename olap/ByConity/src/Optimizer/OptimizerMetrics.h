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

#include <Interpreters/StorageID.h>
#include <Core/Types.h>
#include <memory>

namespace DB
{
using DatabaseAndTableName = std::pair<String, String>;

/**
 * Metrics used in optimizer per query.
 */
class OptimizerMetrics
{
public:
    void addMaterializedView(const StorageID & view) { used_materialized_views.emplace_back(view); }
    const std::vector<StorageID> & getUsedMaterializedViews() const { return used_materialized_views; }

private:
    std::vector<StorageID> used_materialized_views;
};

using OptimizerMetricsPtr = std::shared_ptr<OptimizerMetrics>;
}
