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

#include <Optimizer/ExpressionInliner.h>

namespace DB
{
ASTPtr ExpressionInliner::inlineSymbols(const ConstASTPtr & predicate, const Assignments & mapping)
{
    InlinerVisitor visitor;
    return ASTVisitorUtil::accept(predicate->clone(), visitor, mapping);
}

ASTPtr InlinerVisitor::visitASTIdentifier(ASTPtr & node, const Assignments & context)
{
    auto & identifier = node->as<ASTIdentifier &>();
    if (context.count(identifier.name()))
        return context.at(identifier.name())->clone();
    return node;
}

}
