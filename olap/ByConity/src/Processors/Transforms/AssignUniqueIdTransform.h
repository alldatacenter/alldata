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
class AssignUniqueIdTransform : public ISimpleTransform
{
public:
    AssignUniqueIdTransform(const Block & header_, String symbol_);

    String getName() const override { return "AssignUniqueIdTransform"; }
    static Block transformHeader(Block header, String symbol);

protected:
    void transform(Chunk & chunk) override;

private:
    static Int64 ROW_IDS_PER_REQUEST;
    static Int64 MAX_ROW_ID;
    String symbol;
    std::atomic<Int64> row_id_pool{0};
    Int64 unique_value_mask;
    Int64 row_id_counter{0};
    Int64 max_row_id_counter_value{0};
    void requestValues();
};
}
