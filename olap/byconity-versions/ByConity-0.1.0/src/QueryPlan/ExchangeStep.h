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

#include <Interpreters/DistributedStages/ExchangeMode.h>
#include <Optimizer/Property/Property.h>
#include <QueryPlan/IQueryPlanStep.h>

namespace DB
{

class WriteBuffer;
class ReadBuffer;

class ExchangeStep : public IQueryPlanStep
{
public:
    explicit ExchangeStep(DataStreams input_streams_, const ExchangeMode & mode_,  Partitioning schema_, bool keep_order_ = false);

    String getName() const override { return "Exchange"; }

    Type getType() const override { return Type::Exchange; }

    QueryPipelinePtr updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings & context) override;

    void serialize(WriteBuffer & buf) const override;

    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr & context);

    const ExchangeMode & getExchangeMode() const { return exchange_type; }

    const Partitioning & getSchema() const { return schema; }

    bool needKeepOrder() const { return keep_order; }
    const std::unordered_map<String, std::vector<String>> & getOutToInputs() const { return output_to_inputs; }

    Block getHeader() const { return getOutputStream().header; }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    ExchangeMode exchange_type = ExchangeMode::UNKNOWN;
    Partitioning schema;
    bool keep_order = false;
    std::unordered_map<String, std::vector<String>> output_to_inputs;
};


}
