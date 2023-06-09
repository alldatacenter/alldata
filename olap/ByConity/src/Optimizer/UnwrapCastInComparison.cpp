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

#include <Optimizer/UnwrapCastInComparison.h>

#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/convertFieldToType.h>
#include <Optimizer/FunctionInvoker.h>
#include <Optimizer/LiteralEncoder.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Utils.h>

#include <cmath>

namespace DB
{

const static auto type_string = std::make_shared<DataTypeString>(); // NOLINT(cert-err58-cpp)
const static auto type_int32 = std::make_shared<DataTypeInt32>(); // NOLINT(cert-err58-cpp)
const static auto type_int64 = std::make_shared<DataTypeInt64>(); // NOLINT(cert-err58-cpp)

ASTPtr unwrapCastInComparison(const ConstASTPtr & expression, ContextMutablePtr context, const NameToType & column_types)
{
    UnwrapCastInComparisonVisitor unwrap_cast_visitor;
    auto type_analyzer = TypeAnalyzer::create(context, column_types);
    UnwrapCastInComparisonContext unwrap_cast_context{
        .context = context,
        .column_types = column_types,
        .type_analyzer = type_analyzer,
    };
    return ASTVisitorUtil::accept(expression->clone(), unwrap_cast_visitor, unwrap_cast_context);
}

ASTPtr UnwrapCastInComparisonVisitor::visitASTFunction(ASTPtr & node, UnwrapCastInComparisonContext & context)
{
    auto & function = node->as<ASTFunction &>();

    if (!isComparisonFunction(function))
        return rewriteArgs(function, context);

    auto & left = function.arguments->as<ASTExpressionList &>().children[0];
    auto & right = function.arguments->as<ASTExpressionList &>().children[1];

    auto * cast = left->as<ASTFunction>();
    if (!cast || !isConversionFunction(*cast))
        return rewriteArgs(function, context);

    auto evaluate_result = ExpressionInterpreter::evaluateConstantExpression(right, context.column_types, context.context);

    // rhs is not a literal
    if (!evaluate_result.has_value())
        return rewriteArgs(function, context);

    auto & literal = evaluate_result->second;
    DataTypePtr & literal_type = evaluate_result->first;

    if (literal.isNull() || isNaNOrInf(literal, literal_type))
        return rewriteArgs(function, context, true);

    auto & cast_source = cast->arguments->as<ASTExpressionList &>().children[0];
    auto source_type = context.type_analyzer.getType(cast_source);
    auto target_type = context.type_analyzer.getType(left);

    if (!source_type || !target_type)
        return node;

    if (!isComparableTypes(target_type, literal_type, context.context))
        return rewriteArgs(function, context, true);

    if (!isCastMonotonicAndInjective(source_type, target_type, literal, literal_type, context.context))
        return rewriteArgs(function, context, true);

    auto source_range = source_type->getRange();

    if (!source_range)
        return rewriteArgs(function, context, true);

    // unwrap cast if the literal is out of the range of source_type -> target_type
    Field max_in_target_type = coerce(source_range->max, source_type, target_type, context.context);
    int compare_res = compare(max_in_target_type, target_type, literal, literal_type, context.context);
    if (compare_res == -1)
    {
        if (function.name == "equals" || function.name == "greater" || function.name == "greaterOrEquals")
        {
            return inferExpressionToFalse(cast_source, source_type);
        }
        else if (function.name == "notEquals" || function.name == "less" || function.name == "lessOrEquals")
        {
            return inferExpressionToTrue(cast_source, source_type);
        }
    }
    else if (compare_res == 0)
    {
        if (function.name == "equals" || function.name == "greaterOrEquals")
            return rewriteComparisonAndRevisit("equals", cast_source, std::make_shared<ASTLiteral>(source_range->max), context);
        else if (function.name == "notEquals" || function.name == "less")
            return rewriteComparisonAndRevisit("notEquals", cast_source, std::make_shared<ASTLiteral>(source_range->max), context);
        else if (function.name == "lessOrEquals")
            return inferExpressionToTrue(cast_source, source_type);
        else if (function.name == "greater")
            return inferExpressionToFalse(cast_source, source_type);
    }

    Field min_in_target_type = coerce(source_range->min, source_type, target_type, context.context);
    compare_res = compare(min_in_target_type, target_type, literal, literal_type, context.context);
    if (compare_res == 1)
    {
        if (function.name == "equals" || function.name == "less" || function.name == "lessOrEquals")
        {
            return inferExpressionToFalse(cast_source, source_type);
        }
        else if (function.name == "notEquals" || function.name == "greater" || function.name == "greaterOrEquals")
        {
            return inferExpressionToTrue(cast_source, source_type);
        }
    }
    else if (compare_res == 0)
    {
        if (function.name == "equals" || function.name == "lessOrEquals")
            return rewriteComparisonAndRevisit("equals", cast_source, std::make_shared<ASTLiteral>(source_range->min), context);
        else if (function.name == "notEquals" || function.name == "greater")
            return rewriteComparisonAndRevisit("notEquals", cast_source, std::make_shared<ASTLiteral>(source_range->min), context);
        else if (function.name == "greaterOrEquals")
            return inferExpressionToTrue(cast_source, source_type);
        else if (function.name == "less")
            return inferExpressionToFalse(cast_source, source_type);
    }

    // Apply cast on the literal side, since we have check the literal is not out of the range,
    // the revert type cast(literal type -> source type) should be safe.
    // Note that the logic may be broken in some cases if neither source type & target type don't
    // have an accurate representation for literal.
    // e.g. CAST(int8 AS Float32) = toDecimal64('1.000000000001', 12)
    // but this won't happen as (float, decimal) are not comparable.
    Field literal_in_source_type;
    Field round_trip_literal;
    try
    {
        literal_in_source_type = coerce(literal, literal_type, source_type, context.context);
        round_trip_literal = coerce(literal_in_source_type, source_type, literal_type, context.context);
    }
    catch (Exception &)
    {
        return rewriteArgs(function, context, true);
    }

    auto casted_literal_type = literal_type->isNullable() ? makeNullable(source_type) : removeNullable(source_type);
    auto ast_casted_literal = LiteralEncoder::encode(literal_in_source_type, casted_literal_type, context.context);
    compare_res = compare(literal, literal_type, round_trip_literal, literal_type, context.context);

    if (compare_res == 0)
    {
        return rewriteComparisonAndRevisit(function.name, cast_source, ast_casted_literal, context);
    }
    else if (compare_res == -1)
    {
        if (function.name == "equals")
            return inferExpressionToFalse(cast_source, source_type);
        else if (function.name == "notEquals")
            return inferExpressionToTrue(cast_source, source_type);
        else if (function.name == "less" || function.name == "lessOrEquals")
            return rewriteComparisonAndRevisit("less", cast_source, ast_casted_literal, context);
        else if (function.name == "greater" || function.name == "greaterOrEquals")
            return rewriteComparisonAndRevisit("greaterOrEquals", cast_source, ast_casted_literal, context);
    }
    else
    {
        if (function.name == "equals")
            return inferExpressionToFalse(cast_source, source_type);
        else if (function.name == "notEquals")
            return inferExpressionToTrue(cast_source, source_type);
        else if (function.name == "less" || function.name == "lessOrEquals")
            return rewriteComparisonAndRevisit("lessOrEquals", cast_source, ast_casted_literal, context);
        else if (function.name == "greater" || function.name == "greaterOrEquals")
            return rewriteComparisonAndRevisit("greater", cast_source, ast_casted_literal, context);
    }

    __builtin_unreachable();
}

ASTPtr UnwrapCastInComparisonVisitor::rewriteArgs(ASTFunction & function, UnwrapCastInComparisonContext & context, bool first_only)
{
    auto new_func = std::make_shared<ASTFunction>(function);

    if (function.arguments && !function.arguments->children.empty())
    {
        ASTs new_args(function.arguments->children.size());

        for (size_t i = 0; i < function.arguments->children.size(); ++i)
        {
            auto & arg = function.arguments->children[i];
            new_args[i] = (first_only && i > 0) ? arg : ASTVisitorUtil::accept(arg, *this, context);
        }

        new_func->children.clear();
        new_func->arguments = std::make_shared<ASTExpressionList>();
        new_func->arguments->children = std::move(new_args);
        new_func->children.push_back(new_func->arguments);
    }

    return new_func;
}

bool UnwrapCastInComparisonVisitor::isComparisonFunction(const ASTFunction & function)
{
    return function.name == "equals" || function.name == "notEquals" || function.name == "less" || function.name == "lessOrEquals"
        || function.name == "greater" || function.name == "greaterOrEquals";
}

static const char * toXXXFunctionPrefixes[]
    = {"toUInt8",
       "toUInt16",
       "toUInt32",
       "toUInt64",
       "toInt8",
       "toInt16",
       "toInt32",
       "toInt64",
       "toFloat32",
       "toFloat64",
       "toUUID",
       "toFixedString",
       "toDate",
       // "toDate32", // TODO: date32
       "toDateTime",
       "toDateTime32", // alias to toDateTime
       // "toDateTime64",  // TODO: not implemented since it requires decimal scale
       "toDecimal32",
       "toDecimal64",
       "toDecimal128",
       nullptr};

bool UnwrapCastInComparisonVisitor::isConversionFunction(const ASTFunction & function)
{
    using Utils::checkFunctionName;

    // cast2 is not qualified, since cast2(nullable_int8, 'Int32') will convert NULL to zero, which make type casting not injective
    if (checkFunctionName(function, "cast"))
        return true;

    for (const char ** it = toXXXFunctionPrefixes; *it; ++it)
    {
        String func_prefix(*it);
        if (function.name == func_prefix || function.name == func_prefix + "OrZero" || function.name == func_prefix + "OrNull")
            return true;
    }

    return function.name == "toString";
}

bool UnwrapCastInComparisonVisitor::isNaNOrInf(Field & field, const DataTypePtr & type)
{
    if (auto type_id = type->getTypeId(); type_id == TypeIndex::Float32 || type_id == TypeIndex::Float64)
    {
        auto val = field.safeGet<Float64>();
        return std::isnan(val) || std::isinf(val);
    }

    return false;
}

bool UnwrapCastInComparisonVisitor::isComparableTypes(const DataTypePtr & left, const DataTypePtr & right, ContextPtr context)
{
    auto nonnull_left = removeNullable(left);
    auto nonnull_right = removeNullable(right);

    try
    {
        compare(nonnull_left->getDefault(), nonnull_left, nonnull_right->getDefault(), nonnull_right, context);
    }
    catch (Exception & )
    {
        return false;
    }

    return true;
}

// NOLINTBEGIN(bugprone-branch-clone, hicpp-multiway-paths-covered)
bool UnwrapCastInComparisonVisitor::isCastMonotonicAndInjective(
    const DataTypePtr & from_type, const DataTypePtr & to_type, const Field & literal, const DataTypePtr & literal_type, ContextPtr context)
{
    auto from_id = removeNullable(from_type)->getTypeId();
    auto to_id = removeNullable(to_type)->getTypeId();

    // int->float32 is injective only within (-2^24, 2^24)
    auto is_lossless_cast_to_float32 = [](const Field & literal_, const DataTypePtr & literal_type_, ContextPtr & context_) {
        return compare(literal_, literal_type_, -16777216L, type_int32, context_) > 0
            && compare(literal_, literal_type_, 16777216L, type_int32, context_) < 0;
    };

    // int->float64 is injective only within (-2^53, 2^53)
    auto is_lossless_cast_to_float64 = [](const Field & literal_, const DataTypePtr & literal_type_, ContextPtr & context_) {
        return compare(literal_, literal_type_, -9007199254740992L, type_int64, context_) > 0
            && compare(literal_, literal_type_, 9007199254740992L, type_int64, context_) < 0;
    };

    switch (from_id)
    {
        case TypeIndex::UInt8:
            switch (to_id)
            {
                case TypeIndex::UInt8:
                    [[fallthrough]];
                case TypeIndex::UInt16:
                    [[fallthrough]];
                case TypeIndex::UInt32:
                    [[fallthrough]];
                case TypeIndex::UInt64:
                    [[fallthrough]];
                case TypeIndex::Int16:
                    [[fallthrough]];
                case TypeIndex::Int32:
                    [[fallthrough]];
                case TypeIndex::Int64:
                    [[fallthrough]];
                case TypeIndex::Float32:
                    [[fallthrough]];
                case TypeIndex::Float64:
                    return true;
                default:
                    return false;
            }
        case TypeIndex::UInt16:
            switch (to_id)
            {
                case TypeIndex::UInt16:
                    [[fallthrough]];
                case TypeIndex::UInt32:
                    [[fallthrough]];
                case TypeIndex::UInt64:
                    [[fallthrough]];
                case TypeIndex::Int32:
                    [[fallthrough]];
                case TypeIndex::Int64:
                    [[fallthrough]];
                case TypeIndex::Float32:
                    [[fallthrough]];
                case TypeIndex::Float64:
                    return true;
                default:
                    return false;
            }
        case TypeIndex::UInt32:
            switch (to_id)
            {
                case TypeIndex::UInt32:
                    [[fallthrough]];
                case TypeIndex::UInt64:
                    [[fallthrough]];
                case TypeIndex::Int64:
                    [[fallthrough]];
                case TypeIndex::Float64:
                    return true;
                case TypeIndex::Float32:
                    return is_lossless_cast_to_float32(literal, literal_type, context);
                default:
                    return false;
            }
        case TypeIndex::UInt64:
            switch (to_id)
            {
                case TypeIndex::UInt64:
                    return true;
                case TypeIndex::Float32:
                    return is_lossless_cast_to_float32(literal, literal_type, context);
                case TypeIndex::Float64:
                    return is_lossless_cast_to_float64(literal, literal_type, context);
                default:
                    return false;
            }
        case TypeIndex::Int8:
            switch (to_id)
            {
                case TypeIndex::Int8:
                    [[fallthrough]];
                case TypeIndex::Int16:
                    [[fallthrough]];
                case TypeIndex::Int32:
                    [[fallthrough]];
                case TypeIndex::Int64:
                    [[fallthrough]];
                case TypeIndex::Float32:
                    [[fallthrough]];
                case TypeIndex::Float64:
                    return true;
                default:
                    return false;
            }
        case TypeIndex::Int16:
            switch (to_id)
            {
                case TypeIndex::Int16:
                    [[fallthrough]];
                case TypeIndex::Int32:
                    [[fallthrough]];
                case TypeIndex::Int64:
                    [[fallthrough]];
                case TypeIndex::Float32:
                    [[fallthrough]];
                case TypeIndex::Float64:
                    return true;
                default:
                    return false;
            }
        case TypeIndex::Int32:
            switch (to_id)
            {
                case TypeIndex::Int32:
                    [[fallthrough]];
                case TypeIndex::Int64:
                    [[fallthrough]];
                case TypeIndex::Float64:
                    return true;
                case TypeIndex::Float32:
                    return is_lossless_cast_to_float32(literal, literal_type, context);
                default:
                    return false;
            }
        case TypeIndex::Int64:
            switch (to_id)
            {
                case TypeIndex::Int64:
                    return true;
                case TypeIndex::Float32:
                    return is_lossless_cast_to_float32(literal, literal_type, context);
                case TypeIndex::Float64:
                    return is_lossless_cast_to_float64(literal, literal_type, context);
                default:
                    return false;
            }
        case TypeIndex::Float32:
            switch (to_id)
            {
                case TypeIndex::Float32:
                    [[fallthrough]];
                case TypeIndex::Float64:
                    return true;
                default:
                    return false;
            }
        case TypeIndex::Float64:
            switch (to_id)
            {
                case TypeIndex::Float64:
                    return true;
                default:
                    return false;
            }
        default:
            break;
    }

    return false;
}
// NOLINTEND(bugprone-branch-clone, hicpp-multiway-paths-covered)

ASTPtr UnwrapCastInComparisonVisitor::inferExpressionToTrue(const ASTPtr & expression, const DataTypePtr & type)
{
    if (!type->isNullable())
        return PredicateConst::TRUE_VALUE;
    else
        return makeASTFunction("trueIfNotNull", expression);
}

ASTPtr UnwrapCastInComparisonVisitor::inferExpressionToFalse(const ASTPtr & expression, const DataTypePtr & type)
{
    if (!type->isNullable())
        return PredicateConst::FALSE_VALUE;
    else
        return makeASTFunction("falseIfNotNull", expression);
}

ASTPtr UnwrapCastInComparisonVisitor::rewriteComparisonAndRevisit(
    const String & comparison_op, const ASTPtr & left, const ASTPtr & right, UnwrapCastInComparisonContext & context)
{
    ASTPtr rewritten = makeASTFunction(comparison_op, left, right);
    return ASTVisitorUtil::accept(rewritten, *this, context);
}

Field UnwrapCastInComparisonVisitor::coerce(
    const Field & source, const DataTypePtr & source_type, const DataTypePtr & target_type, ContextPtr context)
{
    auto target_type_name = target_type->getName();
    FieldsWithType cast_args{{source_type, source}, {type_string, target_type_name}};
    return FunctionInvoker::execute("cast", cast_args, context).value;
}

int UnwrapCastInComparisonVisitor::compare(
    const Field & left, const DataTypePtr & left_type, const Field & right, const DataTypePtr & right_type, ContextPtr context)
{
    FieldsWithType args{{left_type, left}, {right_type, right}};

    if (FunctionInvoker::execute("equals", args, context).value.safeGet<UInt64>())
        return 0;
    else if (FunctionInvoker::execute("less", args, context).value.safeGet<UInt64>())
        return -1;
    else
        return 1;
}

}
