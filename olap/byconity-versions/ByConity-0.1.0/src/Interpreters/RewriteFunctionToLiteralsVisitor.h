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

#include <Parsers/ASTFunction.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Storages/StorageInMemoryMetadata.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/evaluateConstantExpression.h>

namespace DB
{

/// Rewrites functions to literal if possible, e.g. tuple(1,3) --> (1,3), array(1,2,3) --> [1,2,3], 1 > 0 -> 0
class RewriteFunctionToLiteralsMatcher
{
public:
    struct Data : public WithContext
    {
        explicit Data(ContextPtr context_) : WithContext(context_) {}
    };

    static void visit(ASTPtr & ast, Data & data);
    static bool needChildVisit(const ASTPtr &, const ASTPtr &) { return true; }
};

using RewriteFunctionToLiteralsVisitor = InDepthNodeVisitor<RewriteFunctionToLiteralsMatcher, false>;

}
