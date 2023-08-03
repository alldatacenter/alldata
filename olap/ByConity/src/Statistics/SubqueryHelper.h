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
#include <memory>
#include <Interpreters/Context.h>

namespace DB::Statistics
{
class SubqueryHelper
{
public:
    static SubqueryHelper create(ContextPtr context, const String & sql);
    SubqueryHelper(SubqueryHelper && other) = default;
    Block getNextBlock();
    ~SubqueryHelper();

    struct DataImpl;

private:
    explicit SubqueryHelper(std::unique_ptr<DataImpl> impl_);

    std::unique_ptr<DataImpl> impl;
    // use unique_ptr to ensure that sub_context will be destroyed after block_io
};

// if don't care about result, use this
void executeSubQuery(ContextPtr context, const String & sql);

inline Block getOnlyRowFrom(SubqueryHelper & helper)
{
    Block block;
    do
    {
        block = helper.getNextBlock();
    } while (block && block.rows() == 0);

    if (!block)
    {
        throw Exception("Not a Valid Block", ErrorCodes::LOGICAL_ERROR);
    }

    if (block.rows() > 1)
    {
        throw Exception("Too much rows", ErrorCodes::LOGICAL_ERROR);
    }
    return block;
}

}
