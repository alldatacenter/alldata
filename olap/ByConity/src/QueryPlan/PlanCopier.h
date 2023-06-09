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

#include <Core/Names.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/SymbolAllocator.h>
#include <QueryPlan/ProjectionStep.h>

#include <memory>

namespace DB
{

// todo@kaixi: implement for all plan nodes
class PlanCopier
{
public:
    static std::shared_ptr<ProjectionStep> reallocateWithProjection(
        const DataStream & data_stream, SymbolAllocator & symbolAllocator,
        std::unordered_map<std::string, std::string> & reallocated_names);

    static PlanNodePtr copy(const PlanNodePtr & plan, ContextMutablePtr & context);

    static bool isOverlapping(const DataStream & data_stream, const DataStream & data_stream2);

    PlanCopier() = delete;
};

}

