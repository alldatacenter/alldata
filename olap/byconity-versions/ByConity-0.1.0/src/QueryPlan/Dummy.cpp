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

#include <QueryPlan/ValuesStep.h>
#include <QueryPlan/Dummy.h>
#include <Core/NamesAndTypes.h>

namespace DB
{

std::pair<String, PlanNodePtr> createDummyPlanNode(ContextMutablePtr context)
{
    auto symbol = context->getSymbolAllocator()->newSymbol("dummy");
    NamesAndTypes header;
    Fields data;

    header.emplace_back(symbol, std::make_shared<DataTypeUInt8>());
    data.emplace_back(0U);

    auto values_step = std::make_shared<ValuesStep>(header, data);
    auto node = PlanNodeBase::createPlanNode(context->nextNodeId(), values_step);
    return {std::move(symbol), std::move(node)};
}
}
