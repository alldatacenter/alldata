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

#include <Optimizer/Equivalences.h>
#include <Optimizer/JoinGraph.h>
#include <Optimizer/MaterializedView/MaterializedViewStructure.h>
#include <Optimizer/Rewriter/Rewriter.h>
#include <Optimizer/SymbolTransformMap.h>
#include <Optimizer/Utils.h>
#include <Parsers/ASTTableColumnReference.h>
#include <QueryPlan/Assignment.h>

namespace DB
{
/**
  * MaterializedViewRewriter is based on "Optimizing Queries Using Materialized Views:
  * A Practical, Scalable Solution" by Goldstein and Larson.
  */
class MaterializedViewRewriter : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "MaterializedViewRewriter"; }

private:
    static std::map<String, std::vector<MaterializedViewStructurePtr>>
    getRelatedMaterializedViews(QueryPlan & plan, ContextMutablePtr context);
};
}
