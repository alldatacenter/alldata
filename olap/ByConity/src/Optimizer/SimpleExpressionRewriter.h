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

#include <Parsers/IAST_fwd.h>
#include <Parsers/ASTVisitor.h>

namespace DB
{
/**
 * Rewrite AST in place, reduce the overhead of clone.
 */
template <typename C>
class SimpleExpressionRewriter : public ASTVisitor<ASTPtr, C>
{
public:
    ASTPtr visitNode(ASTPtr & node, C & context) override
    {
        for (auto & child : node->children)
        {
            auto result = ASTVisitorUtil::accept(child, *this, context);
            if (result != child)
                child = std::move(result);
        }
        return node;
    }
};
}
