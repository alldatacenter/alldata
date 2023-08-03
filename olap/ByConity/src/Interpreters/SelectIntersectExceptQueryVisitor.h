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

#include <unordered_set>

#include <Parsers/IAST.h>
#include <Interpreters/InDepthNodeVisitor.h>

#include <Parsers/ASTSelectIntersectExceptQuery.h>


namespace DB
{

class ASTFunction;
class ASTSelectWithUnionQuery;

class SelectIntersectExceptQueryMatcher
{
public:
    struct Data
    {
        UnionMode intersect_default_mode;
        UnionMode except_default_mode;

        explicit Data(const Settings & settings) :
            intersect_default_mode(settings.intersect_default_mode),
            except_default_mode(settings.except_default_mode)
        {}
    };

    static bool needChildVisit(const ASTPtr &, const ASTPtr &) { return true; }

    static void visit(ASTPtr & ast, Data &);
    static void visit(ASTSelectWithUnionQuery &, Data &);
};

/// Visit children first.
using SelectIntersectExceptQueryVisitor
    = InDepthNodeVisitor<SelectIntersectExceptQueryMatcher, false>;
}
