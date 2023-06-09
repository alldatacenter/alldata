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

#include <Optimizer/Rewriter/UnifyJoinOutputs.h>

#include <Optimizer/SymbolsExtractor.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/SymbolMapper.h>
#include <QueryPlan/CTERefStep.h>

namespace DB
{

void UnifyJoinOutputs::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    auto union_find_map = UnionFindExtractor::extract(plan);
    UnifyJoinOutputs::Rewriter rewriter{context, plan.getCTEInfo(), union_find_map};
    std::set<String> require;
    auto result = VisitorUtil::accept(plan.getPlanNode(), rewriter, require);
    plan.update(result);
}

std::unordered_map<PlanNodeId, UnionFind<String>> UnifyJoinOutputs::UnionFindExtractor::extract(QueryPlan & plan)
{
    UnionFindExtractor extractor {plan.getCTEInfo()};
    std::unordered_map<PlanNodeId, UnionFind<String>> union_find_map;
    VisitorUtil::accept(plan.getPlanNode(), extractor, union_find_map);
    return union_find_map;
}

Void UnifyJoinOutputs::UnionFindExtractor::visitJoinNode(JoinNode & node, std::unordered_map<PlanNodeId, UnionFind<String>> & union_find_map)
{
    auto step = dynamic_cast<const JoinStep *>(node.getStep().get());
    if (!step->supportReorder(true))
        return visitPlanNode(node, union_find_map);

    VisitorUtil::accept(node.getChildren()[0], *this, union_find_map);
    VisitorUtil::accept(node.getChildren()[1], *this, union_find_map);

    UnionFind<String> union_find{union_find_map[node.getChildren()[0]->getId()], union_find_map[node.getChildren()[1]->getId()]};
    for (size_t i = 0; i < step->getLeftKeys().size(); i++)
        union_find.add(step->getLeftKeys()[i], step->getRightKeys()[i]);

    union_find_map.emplace(node.getId(), std::move(union_find));
    return Void{};
}

Void UnifyJoinOutputs::UnionFindExtractor::visitCTERefNode(CTERefNode & node, std::unordered_map<PlanNodeId, UnionFind<String>> & context)
{
    const auto * step = dynamic_cast<const CTERefStep *>(node.getStep().get());
    cte_helper.accept(step->getId(), *this, context);
    return Void{};
}

PlanNodePtr UnifyJoinOutputs::Rewriter::visitPlanNode(PlanNodeBase & node, std::set<String> &)
{
    if (node.getChildren().empty())
        return node.shared_from_this();

    PlanNodes children;
    DataStreams inputs;
    for (const auto & child : node.getChildren())
    {
        std::set<String> require;
        for (const auto & item : child->getStep()->getOutputStream().header)
            require.insert(item.name);

        auto result = VisitorUtil::accept(child, *this, require);
        children.emplace_back(result);
        inputs.push_back(result->getStep()->getOutputStream());
    }

    auto new_step = node.getStep()->copy(context);
    new_step->setInputStreams(inputs);
    node.setStep(new_step);

    node.replaceChildren(children);
    return node.shared_from_this();
}

PlanNodePtr UnifyJoinOutputs::Rewriter::visitJoinNode(JoinNode & node, std::set<String> & require)
{
    auto step = dynamic_cast<const JoinStep *>(node.getStep().get());
    if (!step->supportReorder(true))
        return visitPlanNode(node, require);

    if (step->getFilter())
    {
        auto filter_symbols = SymbolsExtractor::extract(step->getFilter());
        require.insert(filter_symbols.begin(), filter_symbols.end());
    }

    auto & union_find = union_find_map[node.getId()];
    auto & left_union_find = union_find_map[node.getChildren()[0]->getId()];
    auto & right_union_find = union_find_map[node.getChildren()[1]->getId()];

    std::vector<std::pair<String, String>> criteria;
    auto left_sets = left_union_find.getSets();
    auto right_sets = right_union_find.getSets();
    NameSet represent_set;
    for (size_t i = 0; i < step->getLeftKeys().size(); i++)
    {
        String left_key = step->getLeftKeys()[i];
        for (const auto & set : left_sets)
            if (set.count(left_key))
                left_key = *std::min_element(set.begin(), set.end());

        String right_key = step->getRightKeys()[i];
        for (const auto & set : right_sets)
            if (set.count(right_key))
                right_key = *std::min_element(set.begin(), set.end());

        if (!represent_set.count(union_find.find(left_key)))
        {
            criteria.emplace_back(left_key, right_key);
            represent_set.insert(union_find.find(left_key));
        }
    }

    std::sort(criteria.begin(), criteria.end(), [](auto & a, auto & b) { return a.first < b.first; });
    Names left_keys;
    Names right_keys;
    std::set<String> left_require = require;
    std::set<String> right_require = require;
    for (const auto & item : criteria)
    {
        left_keys.emplace_back(item.first);
        left_require.insert(item.first);
        right_keys.emplace_back(item.second);
        right_require.insert(item.second);
    }

    auto left = VisitorUtil::accept(node.getChildren()[0], *this, left_require);
    auto right = VisitorUtil::accept(node.getChildren()[1], *this, right_require);

    std::unordered_map<String, DataTypePtr> name_to_types;
    for (auto & item : left->getStep()->getOutputStream().header)
        name_to_types.emplace(item.name, item.type);
    for (auto & item : right->getStep()->getOutputStream().header)
        name_to_types.emplace(item.name, item.type);

    NamesAndTypes new_outputs;
    for (const auto & name : require)
        if (name_to_types.count(name))
            new_outputs.emplace_back(name, name_to_types[name]);

    auto new_step = std::make_shared<JoinStep>(
        DataStreams{left->getStep()->getOutputStream(), right->getStep()->getOutputStream()},
        DataStream{new_outputs},
        step->getKind(),
        step->getStrictness(),
        std::move(left_keys),
        std::move(right_keys),
        step->getFilter(),
        step->isHasUsing(),
        step->getRequireRightKeys(),
        step->getAsofInequality(),
        step->getDistributionType(),
        step->isMagic());
    return PlanNodeBase::createPlanNode(node.getId(), new_step, PlanNodes{left, right});
}

PlanNodePtr UnifyJoinOutputs::Rewriter::visitCTERefNode(CTERefNode & node, std::set<String> & require)
{
    auto step = dynamic_cast<const CTERefStep *>(node.getStep().get());
    std::set<String> mapped;
    for (auto & item : require)
        if (step->getOutputColumns().contains(item))
            mapped.emplace(step->getOutputColumns().at(item));

    auto cte_plan = cte_helper.acceptAndUpdate(step->getId(), *this, mapped);
    auto new_step = std::dynamic_pointer_cast<CTERefStep>(node.getStep()->copy(context));
    DataStreams input_streams;
    input_streams.emplace_back(cte_plan->getStep()->getOutputStream());
    new_step->setInputStreams(input_streams);
    node.setStep(new_step);
    return node.shared_from_this();
}

}
