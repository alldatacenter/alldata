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

#include <Interpreters/Context.h>
#include <Optimizer/MaterializedView/MaterializedViewStructure.h>

namespace DB
{
class MaterializedViewMemoryCache
{
public:
    static MaterializedViewMemoryCache & instance();

    std::optional<MaterializedViewStructurePtr> getMaterializedViewStructure(
        const StorageID & database_and_table_name, ContextMutablePtr context);

private:
    MaterializedViewMemoryCache() = default;

    // todo: implement lru cache.
    // std::unordered_map<String, MaterializedViewStructurePtr> materialized_view_structures;
    // mutable std::shared_mutex mutex;
};
}
