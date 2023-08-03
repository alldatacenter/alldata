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

#include <Optimizer/SymbolEquivalencesDeriver.h>
#include <Optimizer/Utils.h>

namespace DB
{
SymbolEquivalencesPtr
SymbolEquivalencesDeriver::deriveEquivalences(ConstQueryPlanStepPtr step, std::vector<SymbolEquivalencesPtr> children_equivalences)
{
    static SymbolEquivalencesDeriverVisitor derive;
    return VisitorUtil::accept(step, derive, children_equivalences);
}

SymbolEquivalencesPtr SymbolEquivalencesDeriverVisitor::visitStep(const IQueryPlanStep &, std::vector<SymbolEquivalencesPtr> &)
{
    return std::make_shared<SymbolEquivalences>();
}

SymbolEquivalencesPtr SymbolEquivalencesDeriverVisitor::visitJoinStep(const JoinStep & step, std::vector<SymbolEquivalencesPtr> & context)
{
    auto result = std::make_shared<SymbolEquivalences>(*context[0], *context[1]);

    if (step.getKind() == ASTTableJoin::Kind::Inner)
    {
        for (size_t index = 0; index < step.getLeftKeys().size(); index++)
        {
            result->add(step.getLeftKeys().at(index), step.getRightKeys().at(index));
        }
    }
    return result;
}

SymbolEquivalencesPtr SymbolEquivalencesDeriverVisitor::visitFilterStep(const FilterStep &, std::vector<SymbolEquivalencesPtr> & context)
{
    return context[0];
}

SymbolEquivalencesPtr
SymbolEquivalencesDeriverVisitor::visitProjectionStep(const ProjectionStep & step, std::vector<SymbolEquivalencesPtr> & context)
{
    auto assignments = step.getAssignments();
    std::unordered_map<String, String> identities = Utils::computeIdentityTranslations(assignments);
    std::unordered_map<String, String> revert_identifies;

    for (auto & item : identities)
    {
        revert_identifies[item.second] = item.first;
    }

    return context[0]->translate(identities);
}

SymbolEquivalencesPtr
SymbolEquivalencesDeriverVisitor::visitAggregatingStep(const AggregatingStep & step, std::vector<SymbolEquivalencesPtr> & context)
{
    NameSet set{step.getKeys().begin(), step.getKeys().end()};
    return context[0]->translate(set);
}
SymbolEquivalencesPtr
SymbolEquivalencesDeriverVisitor::visitExchangeStep(const ExchangeStep &, std::vector<SymbolEquivalencesPtr> & context)
{
    return context[0];
}

}
