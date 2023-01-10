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
#include <Core/Types.h>

namespace DB
{
    /**
     * Converts an predicate expression to disjunctive normal form (DNF).
     * DNF: It is a form of logical formula which is disjunction of conjunctive clauses.
     *
     * Example: `(a OR b) AND c` will be converted to DNF `(a AND c) OR (b AND c)`
     */
    ASTPtr toDNF(const ASTPtr & predicate);

    /**
     * Decomposes an AND predicate expression.
     *
     * Example: `a AND b AND (c OR d)` will be decomposed to `a`, `b`, `c or d`
     */
    ASTs decomposeAnd(const ASTPtr & predicate);

    /**
     * Decomposes an OR predicate expression.
     *
     * Example: `a OR b OR (c AND d)` will be decomposed to `a`, `b`, `c AND d`
     */
    ASTs decomposeOr(const ASTPtr & predicate);

    /**
     * Decomposes an XOR predicate expression.
     *
     * Example: `a XOR b XOR (c AND d)` will be decomposed to `a`, `b`, `c AND d`
     */
    ASTs decomposeXor(const ASTPtr & predicate);

    /**
     * Composes a list of predicates into an AND.
     */
    ASTPtr composeAnd(const ASTs & predicates);

    /**
     * Composes a list of predicates into a OR.
     */
    ASTPtr composeOr(const ASTs & predicates);

    /**
    * Composes a list of predicates into an XOR.
    */
    ASTPtr composeXor(const ASTs & predicates);

    /**
     * Negates a predicate expression by adding NOT
     */
    ASTPtr negate(const ASTPtr & predicate);

    /**
     * Return whether the name is a comparison function name
     */
    inline bool isComparisonFunctionName(const String & name)
    {
        return name == "equals" || name == "notEquals" || name == "less" || name == "greater" ||
               name == "lessOrEquals" || name == "greaterOrEquals";
    }

    inline bool isInFunctionName(const String & name)
    {
        return name == "in" || name == "notIn";
    }

    inline bool isLikeFunctionName(const String & name)
    {
        return name == "like" || name == "notLike";
    }
}
