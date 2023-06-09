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

#include <Optimizer/Rule/Rules.h>

#include <Optimizer/Rule/Rewrite/DistinctToAggregate.h>
#include <Optimizer/Rule/Rewrite/ImplementSetOperationRules.h>
#include <Optimizer/Rule/Rewrite/InlineProjections.h>
#include <Optimizer/Rule/Rewrite/MergeSetOperationRules.h>
#include <Optimizer/Rule/Rewrite/PullProjectionOnJoinThroughJoin.h>
#include <Optimizer/Rule/Rewrite/PushAggThroughJoinRules.h>
#include <Optimizer/Rule/Rewrite/PushDownLimitRules.h>
#include <Optimizer/Rule/Rewrite/PushIntoTableScanRules.h>
#include <Optimizer/Rule/Rewrite/PushPartialStepThroughExchangeRules.h>
#include <Optimizer/Rule/Rewrite/PushThroughExchangeRules.h>
#include <Optimizer/Rule/Rewrite/RemoveRedundantRules.h>
#include <Optimizer/Rule/Rewrite/SimplifyExpressionRules.h>
#include <Optimizer/Rule/Rewrite/SwapAdjacenRules.h>
#include <Optimizer/Rule/Rewrite/FilterWindowToPartitionTopN.h>

namespace DB
{
std::vector<RulePtr> Rules::mergeSetRules()
{
    return {std::make_shared<MergeUnionRule>(), std::make_shared<MergeIntersectRule>(), std::make_shared<MergeExceptRule>()};
}

std::vector<RulePtr> Rules::implementSetRules()
{
    return {std::make_shared<ImplementIntersectRule>(), std::make_shared<ImplementExceptRule>()};
}

std::vector<RulePtr> Rules::normalizeExpressionRules()
{
    return {std::make_shared<CommonPredicateRewriteRule>(), std::make_shared<SwapPredicateRewriteRule>()};
}

std::vector<RulePtr> Rules::simplifyExpressionRules()
{
    return {
        std::make_shared<SimplifyPredicateRewriteRule>(),
        std::make_shared<UnWarpCastInPredicateRewriteRule>(),
        std::make_shared<SimplifyJoinFilterRewriteRule>(),
        std::make_shared<SimplifyExpressionRewriteRule>()};
}

std::vector<RulePtr> Rules::mergePredicatesRules()
{
    return {
        std::make_shared<MergePredicatesUsingDomainTranslator>()};
}

std::vector<RulePtr> Rules::inlineProjectionRules()
{
    // todo@kaixi: remove InlineProjectionIntoJoin
    return {
        std::make_shared<InlineProjectionIntoJoin>(),
        std::make_shared<InlineProjectionOnJoinIntoJoin>(),
        std::make_shared<InlineProjections>(),
        std::make_shared<PullProjectionOnJoinThroughJoin>()};
}

std::vector<RulePtr> Rules::pushPartialStepRules()
{
    return {
        std::make_shared<PushPartialAggThroughExchange>(),
        std::make_shared<PushPartialSortingThroughExchange>(),
        std::make_shared<PushPartialLimitThroughExchange>(),
        std::make_shared<FilterWindowToPartitionTopN>(),
        std::make_shared<PushDynamicFilterBuilderThroughExchange>()};
}

std::vector<RulePtr> Rules::removeRedundantRules()
{
    return {
        std::make_shared<RemoveRedundantFilter>(),
        std::make_shared<RemoveRedundantProjection>(),
        std::make_shared<RemoveRedundantEnforceSingleRow>(),
        std::make_shared<RemoveRedundantUnion>(),
        std::make_shared<RemoveRedundantCrossJoin>(),
        std::make_shared<RemoveRedundantJoin>(),
        std::make_shared<RemoveRedundantLimit>(),
        // std::make_shared<RemoveRedundantOuterJoin>()
        };
}

std::vector<RulePtr> Rules::pushAggRules()
{
    return {std::make_shared<PushAggThroughOuterJoin>()};
}

std::vector<RulePtr> Rules::pushDownLimitRules()
{
    return {
        std::make_shared<PushLimitIntoDistinct>(),
        std::make_shared<PushLimitThroughProjection>(),
        std::make_shared<PushLimitThroughExtremesStep>(),
        std::make_shared<PushLimitThroughOuterJoin>(),
        std::make_shared<PushLimitThroughUnion>()};
}

std::vector<RulePtr> Rules::distinctToAggregateRules()
{
    return {std::make_shared<DistinctToAggregate>()};
}

std::vector<RulePtr> Rules::pushIntoTableScanRules()
{
    return {std::make_shared<PushLimitIntoTableScan>(), std::make_shared<PushFilterIntoTableScan>()};
}

std::vector<RulePtr> Rules::swapAdjacentRules()
{
    return {std::make_shared<SwapAdjacentWindows>()};
}

}
