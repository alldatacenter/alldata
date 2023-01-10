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

#include <Optimizer/Rewriter/Rewriter.h>
#include <QueryPlan/CTEInfo.h>
#include <QueryPlan/CTERefStep.h>

namespace DB
{
/**
 * 1. Always Inlining Single-use CTEs
 * 2. Remove unused CTE from cte_info
 */
class RemoveUnusedCTE : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "RemoveUnusedCTE"; }

private:
    class Rewriter;
};
}
