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

#include <Parsers/IAST.h>
#include <Parsers/ASTVisitor.h>
#include <Optimizer/ExpressionInterpreter.h>

namespace DB
{

/**
 * UnwrapCastInComparison is used to optimize expressions of the below structure:
 *   CAST(expr, type) comparison_op const_expr
 *
 * It can infer the expression into a constant, e.g. given x::UInt8,
 * `CAST(x, 'Int32') > 1000` can be inferred to `False`. Or, it can move cast
 * to the constant side, e.g. given x::UInt8, `CAST(x, 'Int32') > 10` can be
 * rewritten to `x > CAST(10, 'UInt8')`.
 */
ASTPtr unwrapCastInComparison(const ConstASTPtr & expression, ContextMutablePtr context, const NameToType & column_types);

struct UnwrapCastInComparisonContext
{
    ContextMutablePtr context;
    NameToType column_types;
    const TypeAnalyzer & type_analyzer;
};

class UnwrapCastInComparisonVisitor: public ASTVisitor<ASTPtr, UnwrapCastInComparisonContext>
{
    ASTPtr visitASTFunction(ASTPtr & node, UnwrapCastInComparisonContext & context) override;
    ASTPtr visitASTLiteral(ASTPtr & node, UnwrapCastInComparisonContext &) override { return node; }
    ASTPtr visitASTIdentifier(ASTPtr & node, UnwrapCastInComparisonContext &) override { return node; }
    ASTPtr visitASTSubquery(ASTPtr & node, UnwrapCastInComparisonContext &) override { return node; }

    ASTPtr rewriteArgs(ASTFunction & function, UnwrapCastInComparisonContext & context, bool first_only = false);
    static bool isComparisonFunction(const ASTFunction & function);
    static bool isConversionFunction(const ASTFunction & function);
    static bool isNaNOrInf(Field & field, const DataTypePtr & type);
    static bool isComparableTypes(const DataTypePtr & left, const DataTypePtr & right, ContextPtr context);
    static ASTPtr inferExpressionToTrue(const ASTPtr & expression, const DataTypePtr & type);
    static ASTPtr inferExpressionToFalse(const ASTPtr & expression, const DataTypePtr & type);
    ASTPtr rewriteComparisonAndRevisit(const String & comparison_op, const ASTPtr & left, const ASTPtr & right,
                                       UnwrapCastInComparisonContext & context);

// visible for test
public:
    // Return true if type cast source_type->target_type is non-decreasing and injective.
    // Partial injective is allowed, i.e. the type cast is at least injective at output `target_value`.
    static bool isCastMonotonicAndInjective(const DataTypePtr & from_type, const DataTypePtr & to_type, const Field & literal,
                                            const DataTypePtr & literal_type, ContextPtr context);

    static Field coerce(const Field & source, const DataTypePtr & source_type, const DataTypePtr & target_type, ContextPtr context);

    // compare left with right, neither can be null.
    // return: 0 if left = right, -1 if left < right, 1 if left > right.
    static int compare(const Field & left, const DataTypePtr & left_type, const Field & right, const DataTypePtr & right_type,
                       ContextPtr context);
};

}
