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

#include <QueryPlan/PlanCopier.h>

#include <Parsers/ASTIdentifier.h>

namespace DB
{

std::shared_ptr<ProjectionStep> PlanCopier::reallocateWithProjection(
    const DataStream & data_stream, SymbolAllocator & symbolAllocator,
    std::unordered_map<std::string, std::string> & reallocated_names)
{
    Assignments assignments;
    NameToType name_to_type;
    for (const auto & name_and_type : data_stream.header)
    {
        const auto & name = name_and_type.name;
        auto reallocated_name = symbolAllocator.newSymbol(name);
        reallocated_names.emplace(name, reallocated_name);
        assignments.emplace_back(reallocated_name, std::make_shared<ASTIdentifier>(name));
        name_to_type.emplace(reallocated_name, name_and_type.type);
    }
    return std::make_shared<ProjectionStep>(data_stream, assignments, name_to_type);
}

PlanNodePtr PlanCopier::copy(const PlanNodePtr & plan, ContextMutablePtr & context) // NOLINT(misc-no-recursion)
{
    PlanNodes children;
    for (auto & child : plan->getChildren())
        children.emplace_back(copy(child, context));

    auto new_node = plan->copy(context->nextNodeId(), context);
    new_node->replaceChildren(children);
    new_node->setStatistics(plan->getStatistics());
    return new_node;
}

bool PlanCopier::isOverlapping(const DataStream & lho, const DataStream & rho)
{
    NameSet name_set;
    std::transform(
        lho.header.begin(),
        lho.header.end(),
        std::inserter(name_set, name_set.end()),
        [] (const auto & nameAndType) { return nameAndType.name; });

    return std::any_of(rho.header.begin(), rho.header.end(),
                       [&] (const auto & nameAndType) { return name_set.contains(nameAndType.name); } );
}

}
