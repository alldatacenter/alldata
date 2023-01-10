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
#include <QueryPlan/ITransformingStep.h>

namespace DB
{

class ActionsDAG;
using ActionsDAGPtr = std::shared_ptr<ActionsDAG>;

enum class TotalsMode;

/// Execute HAVING and calculate totals. See TotalsHavingTransform.
class TotalsHavingStep : public ITransformingStep
{
public:
    TotalsHavingStep(
            const DataStream & input_stream_,
            bool overflow_row_,
            const ActionsDAGPtr & actions_dag_,
            const std::string & filter_column_,
            TotalsMode totals_mode_,
            double auto_include_threshold_,
            bool final_);

    String getName() const override { return "TotalsHaving"; }

    Type getType() const override { return Type::TotalsHaving; }

    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings) override;

    void describeActions(JSONBuilder::JSONMap & map) const override;
    void describeActions(FormatSettings & settings) const override;

    const ActionsDAGPtr & getActions() const { return actions_dag; }

    void serialize(WriteBuffer & buf) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    bool overflow_row;
    ActionsDAGPtr actions_dag;
    String filter_column_name;
    TotalsMode totals_mode;
    double auto_include_threshold;
    bool final;
};

}

