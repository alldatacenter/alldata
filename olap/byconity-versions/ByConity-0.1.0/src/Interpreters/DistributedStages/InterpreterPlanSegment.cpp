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

#include<Interpreters/DistributedStages/InterpreterPlanSegment.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NOT_IMPLEMENTED;
    extern const int PARAMETER_OUT_OF_BOUND;
    extern const int ARGUMENT_OUT_OF_BOUND;
}

InterpreterPlanSegment::InterpreterPlanSegment
(
    PlanSegment * plan_segment_,
    ContextPtr context_
)
: plan_segment(plan_segment_)
, context(context_)
{

}

BlockIO InterpreterPlanSegment::execute()
{
    BlockIO res;
    if (plan_segment)
    {
        res.pipeline = std::move(*(plan_segment->getQueryPlan().buildQueryPipeline(
            QueryPlanOptimizationSettings::fromContext(context), BuildQueryPipelineSettings::fromContext(context)))
            );
    }
    return res;
}



}
