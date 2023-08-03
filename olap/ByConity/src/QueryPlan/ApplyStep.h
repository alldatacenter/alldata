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
#include <QueryPlan/Assignment.h>
#include <QueryPlan/ITransformingStep.h>

namespace DB
{
class ApplyStep : public IQueryPlanStep
{
public:
    enum class ApplyType
    {
        CROSS = 0,
        LEFT,
        SEMI,
        ANTI
    };

    enum class SubqueryType
    {
        SCALAR = 0,
        IN,
        EXISTS,
        QUANTIFIED_COMPARISON
    };

    ApplyStep(DataStreams input_streams_, Names correlation_, ApplyType apply_type_, SubqueryType subquery_type_, Assignment assignment_);

    String getName() const override { return "Apply"; }
    Type getType() const override { return Type::Apply; }

    QueryPipelinePtr updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &) override;
    void serialize(WriteBuffer & buffer) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buffer, ContextPtr context);

    const Names & getCorrelation() const { return correlation; }
    ApplyType getApplyType() const { return apply_type; }
    SubqueryType getSubqueryType() const { return subquery_type; }
    const Assignment & getAssignment() const { return assignment; }
    DataTypePtr getAssignmentDataType() const;
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    /**
     * Correlation symbols, returned from input (outer plan) used in subquery (inner plan)
     */
    Names correlation;
    ApplyType apply_type;
    SubqueryType subquery_type;

    /**
     * Expressions that use subquery symbols.
     * <p>
     * Subquery expressions are different than other expressions
     * in a sense that they might use an entire subquery result
     * as an input (e.g: "x IN (subquery)", "x < ALL (subquery)").
     * Such expressions are invalid in linear operator context
     * (e.g: ProjectNode) in logical plan, but are correct in
     * ApplyNode context.
     * <p>
     * Example 1:
     * - expression: input_symbol_X IN (subquery_symbol_Y)
     * - meaning: if set consisting of all values for subquery_symbol_Y contains value represented by input_symbol_X
     * <p>
     * Example 2:
     * - expression: input_symbol_X < ALL (subquery_symbol_Y)
     * - meaning: if input_symbol_X is smaller than all subquery values represented by subquery_symbol_Y
     * <p>
     */
    Assignment assignment;
};

}
