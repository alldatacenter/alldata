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
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Parsers/ASTVisitor.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
class ProjectionEstimator
{
public:
    static PlanNodeStatisticsPtr estimate(PlanNodeStatisticsPtr & child_stats, const ProjectionStep & step);
};

// calculate expression stats
class ScalarStatsCalculator : public ConstASTVisitor<SymbolStatisticsPtr, std::unordered_map<String, SymbolStatisticsPtr>>
{
public:
    static SymbolStatisticsPtr
    estimate(ConstASTPtr expression, const DataTypePtr & type_, UInt64 total_rows_, std::unordered_map<String, SymbolStatisticsPtr> SymbolStatisticsPtr);
    SymbolStatisticsPtr visitNode(const ConstASTPtr & node, std::unordered_map<String, SymbolStatisticsPtr> & context) override;
    SymbolStatisticsPtr visitASTIdentifier(const ConstASTPtr & node, std::unordered_map<String, SymbolStatisticsPtr> & context) override;
    SymbolStatisticsPtr visitASTFunction(const ConstASTPtr & node, std::unordered_map<String, SymbolStatisticsPtr> & context) override;
    SymbolStatisticsPtr visitASTLiteral(const ConstASTPtr & node, std::unordered_map<String, SymbolStatisticsPtr> & context) override;

private:
    ScalarStatsCalculator(const DataTypePtr & type_, UInt64 total_rows_) : type(type_), total_rows(total_rows_) { }
    const DataTypePtr & type;
    UInt64 total_rows;
};

}
