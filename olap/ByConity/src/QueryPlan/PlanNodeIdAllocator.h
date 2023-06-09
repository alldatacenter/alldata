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

#include <Core/Types.h>

#include <memory>

namespace DB
{
using PlanNodeId = UInt32;

class PlanNodeIdAllocator;
using PlanNodeIdAllocatorPtr = std::shared_ptr<PlanNodeIdAllocator>;

class PlanNodeIdAllocator
{
public:
    PlanNodeIdAllocator() : next_id(0) { }

    explicit PlanNodeIdAllocator(PlanNodeId next_id_) : next_id(next_id_) { }

    PlanNodeId nextId() { return next_id++; }

private:
    PlanNodeId next_id;
};
}
