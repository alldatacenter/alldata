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
#include <Processors/ISimpleTransform.h>

namespace DB
{
class EnforceSingleRowTransform : public IProcessor
{
public:
    explicit EnforceSingleRowTransform(const Block & header_);

    String getName() const override { return "EnforceSingleRowTransform"; }

    Status prepare() override;

private:
    InputPort & input;
    OutputPort & output;
    Port::Data single_row;

    bool has_input = false;
    bool output_finished = false;

    /**
     * create a row with all columns set null, if input don't have any results.
     */
    Port::Data createNullSingleRow() const;
};
}
