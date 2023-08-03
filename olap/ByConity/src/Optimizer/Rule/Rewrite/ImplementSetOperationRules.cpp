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

#include <Interpreters/predicateExpressionsUtils.h>
#include <Optimizer/ImplementSetOperation.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/Rule/Rewrite/ImplementSetOperationRules.h>
#include <Parsers/ASTFunction.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/IntersectStep.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

PatternPtr ImplementExceptRule::getPattern() const
{
    return Patterns::except();
}

TransformResult ImplementExceptRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = *rule_context.context;
    SetOperationNodeTranslator translator{context};

    const auto * step = dynamic_cast<const ExceptStep *>(node->getStep().get());

    /**
    * Converts EXCEPT DISTINCT queries into UNION ALL..GROUP BY...WHERE
    * E.g.:
    *     SELECT a FROM foo
    *     EXCEPT DISTINCT
    *     SELECT x FROM bar
    * =>
    *     SELECT a
    *     FROM
    *     (
    *         SELECT a,
    *         sum(foo_marker) AS foo_count,
    *         sum(bar_marker) AS bar_count
    *         FROM
    *         (
    *             SELECT a, 1 as foo_marker, 0 as bar_marker
    *             FROM foo
    *             UNION ALL
    *             SELECT x, 0 as foo_marker, 1 as bar_marker
    *             FROM bar
    *         ) T1
    *     GROUP BY a
    *     ) T2
    *     WHERE foo_count >= 1 AND bar_count = 0;
    */
    if (step->isDistinct())
    {
        auto translator_result = translator.makeSetContainmentPlanForDistinct(*node);

        // intersect predicate: the row must be present in every source
        ASTs greaters;
        greaters.emplace_back(makeASTFunction(
            "greaterOrEquals", ASTs{std::make_shared<ASTIdentifier>(translator_result.count_symbols[0]), std::make_shared<ASTLiteral>(1)}));
        for (size_t index = 1; index < translator_result.count_symbols.size(); ++index)
        {
            greaters.emplace_back(makeASTFunction(
                "equals", ASTs{std::make_shared<ASTIdentifier>(translator_result.count_symbols[index]), std::make_shared<ASTLiteral>(0)}));
        }

        auto predicate = composeAnd(greaters);

        auto filter_step = std::make_shared<FilterStep>(translator_result.plan_node->getStep()->getOutputStream(), predicate);
        PlanNodes children{translator_result.plan_node};
        PlanNodePtr filter_node = std::make_shared<FilterNode>(context.nextNodeId(), std::move(filter_step), children);
        return filter_node;
    }
    throw Exception("except distinct not impl", ErrorCodes::NOT_IMPLEMENTED);
}

PatternPtr ImplementIntersectRule::getPattern() const
{
    return Patterns::intersect();
}

TransformResult ImplementIntersectRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = *rule_context.context;
    SetOperationNodeTranslator translator{context};

    const auto * step = dynamic_cast<const IntersectStep *>(node->getStep().get());

    /**
     * Converts INTERSECT DISTINCT queries into UNION ALL..GROUP BY...WHERE
     * E.g.:
     *     SELECT a FROM foo
     *     INTERSECT DISTINCT
     *     SELECT x FROM bar
     * =>
     *     SELECT a
     *     FROM
     *     (
     *         SELECT a,
     *         sum(foo_marker) AS foo_count,
     *         sum(bar_marker) AS bar_count
     *         FROM
     *         (
     *             SELECT a, 1 as foo_marker, 0 as bar_marker
     *             FROM foo
     *             UNION ALL
     *             SELECT x, 0 as foo_marker, 1 as bar_marker
     *             FROM bar
     *         ) T1
     *     GROUP BY a
     *     ) T2
     *     WHERE foo_count >= 1 AND bar_count >= 1;
     */
    if (step->isDistinct())
    {
        auto translator_result = translator.makeSetContainmentPlanForDistinct(*node);

        // intersect predicate: the row must be present in every source
        ASTs greaters;
        for (const auto & item : translator_result.count_symbols)
            greaters.emplace_back(
                makeASTFunction("greaterOrEquals", ASTs{std::make_shared<ASTIdentifier>(item), std::make_shared<ASTLiteral>(1)}));

        auto predicate = composeAnd(greaters);

        auto filter_step = std::make_shared<FilterStep>(translator_result.plan_node->getStep()->getOutputStream(), predicate);
        PlanNodes children{translator_result.plan_node};
        PlanNodePtr filter_node = std::make_shared<FilterNode>(context.nextNodeId(), std::move(filter_step), children);
        return filter_node;
    }
    throw Exception("intersect distinct not impl", ErrorCodes::NOT_IMPLEMENTED);
}

}
