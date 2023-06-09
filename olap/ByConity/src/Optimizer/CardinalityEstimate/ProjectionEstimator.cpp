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

#include <Optimizer/CardinalityEstimate/ProjectionEstimator.h>

#include <Optimizer/SymbolsExtractor.h>
#include <Parsers/ASTVisitor.h>
#include <Common/FieldVisitorConvertToNumber.h>
#include <Common/FieldVisitors.h>

namespace DB
{
PlanNodeStatisticsPtr ProjectionEstimator::estimate(PlanNodeStatisticsPtr & child_stats, const ProjectionStep & step)
{
    if (!child_stats)
    {
        return nullptr;
    }

    PlanNodeStatisticsPtr project_stats = child_stats->copy();
    const auto & name_to_type = step.getNameToType();

    std::unordered_map<String, SymbolStatisticsPtr> & symbol_statistics = project_stats->getSymbolStatistics();
    std::unordered_map<String, SymbolStatisticsPtr> calculated_symbol_statistics;
    for (const auto & assignment : step.getAssignments())
    {
        auto result = ScalarStatsCalculator::estimate(
            assignment.second, name_to_type.at(assignment.first), project_stats->getRowCount(), symbol_statistics);
        if (result && !result->isUnknown())
        {
            calculated_symbol_statistics[assignment.first] = result;
            if (result->getType() && result->getType()->getName() != step.getNameToType().at(assignment.first)->getName())
            {
                calculated_symbol_statistics[assignment.first]->getHistogram().clear();
            }
        }
    }

    return std::make_shared<PlanNodeStatistics>(project_stats->getRowCount(), std::move(calculated_symbol_statistics));
}

SymbolStatisticsPtr ScalarStatsCalculator::estimate(
    ConstASTPtr expression, const DataTypePtr & type_, UInt64 total_rows_, std::unordered_map<String, SymbolStatisticsPtr> symbolStatistics)
{
    ScalarStatsCalculator calculator{type_, total_rows_};
    return ASTVisitorUtil::accept(expression, calculator, symbolStatistics);
}

SymbolStatisticsPtr ScalarStatsCalculator::visitNode(const ConstASTPtr &, std::unordered_map<String, SymbolStatisticsPtr> &)
{
    return SymbolStatistics::UNKNOWN;
}

SymbolStatisticsPtr
ScalarStatsCalculator::visitASTIdentifier(const ConstASTPtr & node, std::unordered_map<String, SymbolStatisticsPtr> & context)
{
    const auto & identifier = node->as<ASTIdentifier &>();
    return context[identifier.name()];
}

SymbolStatisticsPtr
ScalarStatsCalculator::visitASTFunction(const ConstASTPtr & node, std::unordered_map<String, SymbolStatisticsPtr> & context)
{
    // TODO literal interpreter
    auto symbols = SymbolsExtractor::extract(node);

    auto result = SymbolStatistics::UNKNOWN;
    for (const auto & symbol : symbols)
    {
        if (context.contains(symbol))
        {
            auto child = context.at(symbol);
            if (child->getNdv() > result->getNdv())
            {
                result = child->copy();
            }
        }
    }

    result->getHistogram().clear();
    return result;
}

SymbolStatisticsPtr
ScalarStatsCalculator::visitASTLiteral(const ConstASTPtr & node, std::unordered_map<String, SymbolStatisticsPtr> & context)
{
    const auto * literal = dynamic_cast<const ASTLiteral *>(node.get());
    if (literal->value.isNull())
        return std::make_shared<SymbolStatistics>(1, 0, 0, 1);
    DataTypePtr tmp_type = type;
    if (tmp_type->isNullable())
        tmp_type = dynamic_cast<const DataTypeNullable *>(type.get())->getNestedType();
    if (tmp_type->isValueRepresentedByNumber())
    {
        double value = applyVisitor(FieldVisitorConvertToNumber<Float64>(), literal->value);
        return std::make_shared<SymbolStatistics>(
            1, value, value, 0, 8, Histogram{Buckets{Bucket(value, value, 1, total_rows, true, true)}}, type, "unknown", false);
    }

    if (tmp_type->getTypeId() == TypeIndex::String || type->getTypeId() == TypeIndex::FixedString)
    {
        String str = literal->value.safeGet<String>();
        double value = CityHash_v1_0_2::CityHash64(str.data(), str.size());
        return std::make_shared<SymbolStatistics>(
            1, value, value, 0, 8, Histogram{Buckets{Bucket(value, value, 1, total_rows, true, true)}}, type, "unknown", false);
    }
    return visitNode(node, context);
}

}
