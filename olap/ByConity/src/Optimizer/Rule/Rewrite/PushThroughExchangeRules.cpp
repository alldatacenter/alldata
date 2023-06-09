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

#include <Optimizer/Rule/Rewrite/PushThroughExchangeRules.h>

#include <QueryPlan/PlanNode.h>
#include <Optimizer/Rule/Pattern.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/SymbolUtils.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/ExchangeStep.h>

namespace DB
{
PatternPtr PushDynamicFilterBuilderThroughExchange::getPattern() const
{
    return Patterns::project()
        ->matchingStep<ProjectionStep>([](const auto & project) { return !project.getDynamicFilters().empty(); })
        ->withSingle(Patterns::exchange());
}

TransformResult PushDynamicFilterBuilderThroughExchange::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    const auto * project_step = dynamic_cast<const ProjectionStep *>(node->getStep().get());
    const auto & exchange = node->getChildren()[0];

    std::unordered_map<String, DynamicFilterBuildInfo> pushdown_dynamic_filter;
    std::unordered_map<String, DynamicFilterBuildInfo> remaining_dynamic_filter;
    for (const auto & dynamic_filter : project_step->getDynamicFilters())
        if (Utils::isIdentity(dynamic_filter.first, project_step->getAssignments().at(dynamic_filter.first)))
            pushdown_dynamic_filter.emplace(dynamic_filter);
        else
            remaining_dynamic_filter.emplace(dynamic_filter);

    Assignments assignments;
    NameToType name_to_type;
    for (const auto & name_and_type : project_step->getInputStreams()[0].header)
    {
        assignments.emplace_back(name_and_type.name, std::make_shared<ASTIdentifier>(name_and_type.name));
        name_to_type.emplace(name_and_type.name, name_and_type.type);
    }

    auto plan = PlanNodeBase::createPlanNode(
        exchange->getId(),
        exchange->getStep(),
        {PlanNodeBase::createPlanNode(
            context.context->nextNodeId(),
            std::make_shared<ProjectionStep>(
                project_step->getInputStreams()[0], assignments, name_to_type, false, pushdown_dynamic_filter),
            exchange->getChildren())});

    if (project_step->isFinalProject() || !Utils::isIdentity(project_step->getAssignments())) {
        return PlanNodeBase::createPlanNode(
            node->getId(),
            std::make_shared<ProjectionStep>(
                project_step->getInputStreams()[0],
                project_step->getAssignments(),
                project_step->getNameToType(),
                project_step->isFinalProject(),
                remaining_dynamic_filter),
            {plan});
    }
    return plan;
}
}
