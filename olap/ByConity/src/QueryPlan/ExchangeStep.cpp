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

#include <QueryPlan/ExchangeStep.h>


namespace DB
{


ExchangeStep::ExchangeStep(DataStreams input_streams_, const ExchangeMode & mode_, Partitioning schema_, bool keep_order_)
    : exchange_type(mode_)
    , schema(std::move(schema_))
    , keep_order(keep_order_)
{
    setInputStreams(input_streams_);
}

void ExchangeStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream = DataStream{.header = input_streams[0].header};
    for (size_t i = 0; i < output_stream->header.columns(); ++i)
    {
        String output_symbol = output_stream->header.getByPosition(i).name;
        std::vector<String> inputs;
        for (auto & input_stream : input_streams)
        {
            String input_symbol = input_stream.header.getByPosition(i).name;
            inputs.emplace_back(input_symbol);
        }
        output_to_inputs[output_symbol] = inputs;
    }
}

QueryPipelinePtr ExchangeStep::updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings &)
{
    return std::move(pipelines[0]);
}

void ExchangeStep::serialize(WriteBuffer &) const
{
    throw Exception("ExchangeStep should be rewritten into RemoteExchangeSourceStep", ErrorCodes::NOT_IMPLEMENTED);
}

QueryPlanStepPtr ExchangeStep::deserialize(ReadBuffer &, ContextPtr &)
{
    throw Exception("ExchangeStep should be rewritten into RemoteExchangeSourceStep", ErrorCodes::NOT_IMPLEMENTED);
}

std::shared_ptr<IQueryPlanStep> ExchangeStep::copy(ContextPtr) const
{
    return std::make_shared<ExchangeStep>(input_streams, exchange_type, schema, keep_order);
}

}
