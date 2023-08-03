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

#include <common/types.h>

namespace DB
{
using RuntimeFilterId = size_t;

class RuntimeFilterHolder
{
public:
    RuntimeFilterHolder(const String & query_id_, size_t segment_id_, RuntimeFilterId filter_id_)
        : query_id(query_id_), segment_id(segment_id_), filter_id(filter_id_)
    {
    }

    RuntimeFilterHolder(const RuntimeFilterHolder & other) = delete;
    RuntimeFilterHolder(RuntimeFilterHolder && other) = default;
    RuntimeFilterHolder & operator=(const RuntimeFilterHolder & other) = delete;
    RuntimeFilterHolder & operator=(RuntimeFilterHolder && other) = default;

    ~RuntimeFilterHolder();

private:
    String query_id;
    size_t segment_id;
    RuntimeFilterId filter_id;
};
}

