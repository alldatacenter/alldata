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

#include <Optimizer/Rule/Rule.h>

namespace DB
{
/**
 * Magic Set create special partial structure to filter data early.
 * While these options may result in more computation inside the view,
 * they could be cheaper overall (because the Partial Result or
 * the Filter table is cheaper to compute).
 *
 * see more
 * - Cost-Based Optimization for Magic: Algebra and Implementation
 * - Sideways Information Passing for Push-Style Query Processing
 *
 * todo
 * - magic set work with dynamic filter.
 * - magic set work with CTE.
 * - implement more push down rules if needed.
 */
class MagicSetRule : public Rule
{
public:
    RuleType getType() const override = 0;
    PatternPtr getPattern() const override = 0;

    const std::vector<RuleType> & blockRules() const override;
    bool isEnabled(ContextPtr context) override { return context->getSettingsRef().enable_magic_set; }

    /*
     * Build Magic Set as special join: Y filter join X.
     * Y is small filter source, X is big target.
     * <pre>
     * - Inner Join (magic set filter join)
     *     - X
     *         - Aggregating (add distinct if enforce_distinct)
     *             - Projection (prune column or reallocate symbol)
     *                  - Y
     * </pre>
     */
    static PlanNodePtr buildMagicSetAsFilterJoin(
        const PlanNodePtr & source, const PlanNodePtr & filter_source,
        const Names& source_names, const Names& filter_names,
        ContextMutablePtr & context, bool enforce_distinct = true);
};

/**
 * Transforms:
 * <pre>
 * - Join or Right Join
 *     - Aggregating
 *         - X (large source as target )
 *     - Y (small source as filter)
 * </pre>
 * Into:
 * <pre>
 * - Join or Right Join
 *     - Aggregating
 *         - Inner Join (magic set filter join)
 *             - X
 *             - Aggregating
 *                 - Projection
 *                     - Y
 *     - Y
 * </pre>
 */
class MagicSetForAggregation : public MagicSetRule
{
public:
    RuleType getType() const override { return RuleType::MAGIC_SET_FOR_AGGREGATION; }
    String getName() const override { return "MAGIC_SET_FOR_AGGREGATION"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

/**
 * Similar with MagicSetForAggregation.
 *
 * Transforms:
 * <pre>
 * - Join or Right Join
 *     - Projection
 *         - Aggregating
 *             - X (large source as target )
 *     - Y (small source as filter)
 * </pre>
 * Into:
 * <pre>
 * - Join or Right Join
 *     - Projection
 *         - Aggregating
 *             - Inner Join (magic set filter join)
 *                 - X
 *                 - Aggregating
 *                     - Projection
 *                         - Y
 *     - Y
 * </pre>
 */
class MagicSetForProjectionAggregation : public MagicSetRule
{
public:
    RuleType getType() const override { return RuleType::MAGIC_SET_FOR_PROJECTION_AGGREGATION; }
    String getName() const override { return "MAGIC_SET_FOR_PROJECTION_AGGREGATION"; }

    PatternPtr getPattern() const override;

protected:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
};

}
