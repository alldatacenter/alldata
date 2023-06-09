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
#include <QueryPlan/ISourceStep.h>

namespace DB
{
using Fields = std::vector<Field>;

/**
 * select single row filled with single constant
 *
 * SELECT 1 -- single value
 * SELECT ((1, 2, 3), (4, 5, 6), (7, 8, 9)) -- single tuple
 *
 * select single row filled with multiple constants
 *
 * SELECT 1, 2, 3
 *
 * select multiple rows filled with multiple constants
 *
 * SELECT 1, 2, 3
 *   UNION ALL
 * SELECT 4, 5, 6
 *   UNION ALL
 * SELECT 7, 8, 9
 *
 * Join with value step.
 *
 * SELECT * FROM nation, (SELECT 1 as c1, 2 as c2 , 3 as c3 UNION ALL SELECT 4, 5, 6 UNION ALL SELECT 7, 8, 9);
 */
class ValuesStep : public ISourceStep
{
public:
    ValuesStep(Block header, Fields fields_, size_t rows_ = 1);

    String getName() const override { return "Values"; }
    Type getType() const override { return Type::Values; }
    const Fields & getFields() const { return fields; }
    size_t getRows() const { return rows; }

    void initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & build_context) override;
    void serialize(WriteBuffer & buffer) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buffer, ContextPtr context);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr context) const override;
    void setUniqueId(Int32 unique_id_) { unique_id = unique_id_; }

private:
    Fields fields;
    size_t rows;
    Int32 unique_id;
};

}
