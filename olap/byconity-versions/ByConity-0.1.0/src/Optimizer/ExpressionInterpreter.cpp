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

#include <Optimizer/ExpressionInterpreter.h>

#include <Common/FieldVisitorConvertToNumber.h>
#include <DataTypes/DataTypeNothing.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypeSet.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/FunctionsLogical.h>
#include <Functions/InternalFunctionsDynamicFilter.h>
#include <Optimizer/FunctionInvoker.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Utils.h>
#include <Interpreters/convertFieldToType.h>
#include <Interpreters/ActionsVisitor.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/formatAST.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int TYPE_MISMATCH;
}

using InterpretResult = ExpressionInterpreter::InterpretResult;
using InterpretIMResult = ExpressionInterpreter::InterpretIMResult;
using InterpretIMResults = ExpressionInterpreter::InterpretIMResults;

static ASTPtr makeFunction(const String & name, const InterpretIMResults & arguments, const ContextMutablePtr & context)
{
    ASTs argument_asts;
    std::transform(arguments.begin(), arguments.end(), std::back_inserter(argument_asts),
                   [&](const auto & arg) { return arg.convertToAST(context); });
    return makeASTFunction(name, argument_asts);
}

static ColumnsWithTypeAndName convertToFunctionBuilderParams(const InterpretIMResults & arguments)
{
    ColumnsWithTypeAndName columns;
    std::transform(arguments.begin(), arguments.end(), std::back_inserter(columns),
                   [](const auto & arg) { return ColumnWithTypeAndName(arg.value, arg.type, ""); });
    return columns;
}

template <typename T>
static DataTypePtr makeNullableByArgumentTypes(const InterpretIMResults & arguments)
{
    DataTypePtr result = std::make_shared<T>();

    if (std::any_of(arguments.begin(), arguments.end(), [](auto & arg) { return arg.type->isNullable();}))
        result = makeNullable(result);

    return result;
}

static bool isBoolCompatibleType(const DataTypePtr & type)
{
    auto nonnull_type = removeNullable(removeLowCardinality(type));
    return isUInt8(nonnull_type) || isNothing(nonnull_type);
}

namespace function_simplify_rules_
{
template <bool value>
bool isBoolValue(const InterpretIMResult & result)
{
    if (isBoolCompatibleType(result.type) && result.isValue())
    {
        auto field = result.getField();

        if constexpr (value)
            return !field.isNull() && applyVisitor(FieldVisitorConvertToNumber<bool>(), field);
        else
            return field.isNull() || !applyVisitor(FieldVisitorConvertToNumber<bool>(), field);
    }

    return false;
}

inline bool isFunction(const InterpretIMResult & result, std::string_view name)
{
    return result.isAST() && result.ast->as<ASTFunction>() && result.ast->as<ASTFunction>()->name == name;
}

inline bool isIdentifier(const InterpretIMResult & result, String & extract_name)
{
    return result.isAST() && tryGetIdentifierNameInto(result.ast, extract_name);
}

using namespace FunctionsLogicalDetail;

template <typename FunctionName, typename FunctionImpl>
struct LogicalFunctionRewriter
{
    static bool apply(const ASTFunction & function, InterpretIMResults argument_results, InterpretIMResult & rewrite_result,
                      const ContextMutablePtr & context)
    {
        if (function.name != FunctionName::name)
            return false;

        Ternary::ResultType const_value = 0;
        bool has_const = false;

        for (int i = static_cast<int>(argument_results.size()) - 1; i >= 0; --i)
        {
            if (argument_results[i].isValue())
            {
                auto field = argument_results[i].getField();
                Ternary::ResultType value = field.isNull() ? Ternary::Null : Ternary::makeValue(applyVisitor(FieldVisitorConvertToNumber<bool>(), field));

                if (has_const)
                {
                    const_value = FunctionImpl::apply(const_value, value);
                }
                else
                {
                    const_value = value;
                    has_const = true;
                }

                argument_results.erase(argument_results.begin() + i);
            }
        }

        auto ternary_to_field = [](Ternary::ResultType value) -> Field
        {
            if (value == Ternary::False)
                return 0U;
            else if (value == Ternary::True)
                return 1U;
            else if (value == Ternary::Null)
                return Null{};
            else
                throw Exception("Unknown ternary value: " + std::to_string(value), ErrorCodes::LOGICAL_ERROR);
        };

        if (has_const)
        {
            if (FunctionImpl::isSaturatedValueTernary(const_value))
            {
                // `x AND 0` returns `0`
                rewrite_result = {std::make_shared<DataTypeUInt8>(), ternary_to_field(const_value)};
                return true;
            }
            else if (FunctionImpl::isNeutralValueTernary(const_value))
            {
                // `x AND 1` return `x`
            }
            else
            {
                // `x AND NULL` return `x AND NULL`
                argument_results.emplace_back(makeNullable(std::make_shared<DataTypeNothing>()), Null());
            }
        }

        assert(!argument_results.empty());

        DataTypePtr result_type = makeNullableByArgumentTypes<DataTypeUInt8>(argument_results);

        if (argument_results.size() == 1)
        {
            auto & single_arg = argument_results[0];
            assert(single_arg.isAST());

            if (isBoolCompatibleType(single_arg.type))
                rewrite_result = {result_type, single_arg.ast};
            else
            {
                auto cast_func = makeASTFunction("cast", single_arg.ast, std::make_shared<ASTLiteral>(result_type->getName()));
                rewrite_result = {result_type, cast_func};
            }
        }
        else
        {
            rewrite_result = {result_type, makeFunction(function.name, argument_results, context)};
        }

        return true;
    }
};

using RewriteAnd = LogicalFunctionRewriter<NameAnd, AndImpl>;
using RewriteOr = LogicalFunctionRewriter<NameOr, OrImpl>;

// suppose x is a non-nullable column, infer
//   x IS NULL      ==> FALSE
//   x IS NOT NULL  ==> TRUE
bool simplifyNullPrediction(const ASTFunction & function, const InterpretIMResults & argument_results, InterpretIMResult & simplify_result)
{
    bool is_null = function.name == "isNull";
    bool is_not_null = function.name == "isNotNull";

    if (!(is_null || is_not_null) || argument_results.front().type->isNullable())
        return false;

    simplify_result = {std::make_shared<DataTypeUInt8>(), is_null ? 0U : 1U};
    return true;
}

// `a = a` ==> TRUE
bool simplifyTrivialEquals(const ASTFunction & function, const InterpretIMResults & argument_results, InterpretIMResult & simplify_result)
{
    String left, right;

    if (function.name == "equals" && isIdentifier(argument_results[0], left) &&
        isIdentifier(argument_results[1], right) && left == right)
    {
        simplify_result = {std::make_shared<DataTypeUInt8>(), 1U};
        return true;
    }

    return false;
}

bool simplifyIf(const ASTFunction & function, const InterpretIMResults & argument_results, InterpretIMResult & simplify_result,
                bool & reevaluate)
{
    if (function.name != "if")
        return false;

    // if(expr, null, 0) return false.
    // this simplifies filter produced by PredicatePushdown like
    // if(isNull(`ws_order_number`), NULL, cast(multiIf(`build_side_non_null_symbol` = 1, 1, NULL, 0, 0), 'UInt8'))
    if (isBoolValue<false>(argument_results[1]) && isBoolValue<false>(argument_results[2]))
    {
        simplify_result = {std::make_shared<DataTypeUInt8>(), 0U};
        return true;
    }

    // if(isNull(expr), NULL, 1) return isNotNull(expr)
    if (isFunction(argument_results[0], "isNull") && isBoolValue<false>(argument_results[1]) && isBoolValue<true>(argument_results[2]))
    {
        const auto & is_null_func = argument_results[0].ast->as<ASTFunction &>();
        simplify_result = {std::make_shared<DataTypeUInt8>(), makeASTFunction("isNotNull", is_null_func.arguments->children)};
        reevaluate = true;
        return true;
    }

    return false;
}
}

ExpressionInterpreter::ExpressionInterpreter(InterpretSetting setting_, ContextMutablePtr context_)
    : context(std::move(context_)), setting(std::move(setting_)), type_analyzer(TypeAnalyzer::create(context, setting.identifier_types))
{}

ExpressionInterpreter ExpressionInterpreter::basicInterpreter(ExpressionInterpreter::IdentifierTypes types, ContextMutablePtr context)
{
    ExpressionInterpreter::InterpretSetting setting
        {
            .identifier_types = std::move(types)
        };
    return {std::move(setting), std::move(context)};
}

ExpressionInterpreter ExpressionInterpreter::optimizedInterpreter(ExpressionInterpreter::IdentifierTypes types, ExpressionInterpreter::IdentifierValues values, ContextMutablePtr context)
{
    ExpressionInterpreter::InterpretSetting setting
        {
            .identifier_types = std::move(types),
            .identifier_values = std::move(values),
            .enable_null_simplify = true,
            .enable_function_simplify = true
        };

    return {std::move(setting), std::move(context)};
}

std::pair<DataTypePtr, ASTPtr> ExpressionInterpreter::optimizeExpression(const ConstASTPtr & expression) const
{
    auto result = evaluate(expression);
    return {result.type, result.convertToAST(context)};
}

ASTPtr ExpressionInterpreter::optimizePredicate(const ConstASTPtr & expression) const
{
    auto result = evaluate(expression);
    Utils::checkState(isBoolCompatibleType(result.type));

    if (result.isAST())
        return result.ast;

    const auto & field = result.value;
    UInt8 x = !field.isNull() && applyVisitor(FieldVisitorConvertToNumber<bool>(), field);
    return std::make_shared<ASTLiteral>(x);
}

std::optional<std::pair<DataTypePtr, Field>> ExpressionInterpreter::evaluateConstantExpression(const ConstASTPtr & expression) const
{
    auto result = evaluate(expression);
    return result.isAST() ? std::nullopt : std::make_optional(std::make_pair(result.type, result.value));
}

InterpretResult ExpressionInterpreter::evaluate(const ConstASTPtr & expression) const
{
    auto im_result = visit(expression);

    if (im_result.isAST())
        return {im_result.type, im_result.ast};
    else
        return {im_result.type, im_result.getField()};
}

InterpretIMResult::InterpretIMResult(DataTypePtr type_, const Field & field)
{
    type = std::move(type_);
    // TODO: we don't have to call convertFieldToType if we have done in Parser/Analyzer
    value = type->createColumnConst(1, convertFieldToType(field, *type));
}

InterpretIMResult ExpressionInterpreter::visit(const ConstASTPtr & node) const
{
    if (const auto * ast_literal = node->as<ASTLiteral>())
        return visitASTLiteral(*ast_literal, node);
    if (const auto * ast_identifier = node->as<ASTIdentifier>())
        return visitASTIdentifier(*ast_identifier, node);
    if (const auto * ast_func = node->as<ASTFunction>())
    {
        const auto & func_name = ast_func->name;
        const static NameSet functions_not_evaluate
            {
                "arrayJoin",

                "arraySetCheck",
                "arraySetGet",
                "arraySetGetAny",

                InternalFunctionDynamicFilter::name,

                "str_to_map",
                "getMapKeys",

                // TODO: support nullIn
                "nullIn",
                "notNullIn",
                "globalNullIn",
                "globalNotNullIn"
            };

        if (functions_not_evaluate.count(func_name))
            return originalNode(node);
        if (func_name == "in" || func_name == "globalIn" || func_name == "notIn" || func_name == "globalNotIn")
            return visitInFunction(*ast_func, node);
        return visitOrdinaryFunction(*ast_func, node);
    }

    throw Exception(ErrorCodes::LOGICAL_ERROR, "Unable to evaluate AST");
}

InterpretIMResult ExpressionInterpreter::visitASTLiteral(const ASTLiteral & literal, const ConstASTPtr & node) const
{
    return {getType(node), literal.value};
}

InterpretIMResult ExpressionInterpreter::visitASTIdentifier(const ASTIdentifier & identifier, const ConstASTPtr & node) const
{
    if (auto it = setting.identifier_values.find(identifier.name());
        it != setting.identifier_values.end())
        return {getType(node), it->second};

    return originalNode(node);
}

InterpretIMResult ExpressionInterpreter::visitOrdinaryFunction(const ASTFunction & function, const ConstASTPtr & node) const
{
    InterpretIMResults argument_results;
    bool all_const = true;
    bool has_null_argument = false;
    bool has_lambda_argument = false;

    if (function.arguments)
    {
        auto is_lambda_argument = [](const auto & ast)
        {
            if (const auto * ast_func = ast->template as<ASTFunction>())
                return ast_func->name == "lambda";

            return false;
        };

        has_lambda_argument = std::any_of(function.arguments->children.begin(), function.arguments->children.end(), is_lambda_argument);
        ExpressionTypes arguments_type_provider;

        if (has_lambda_argument)
            arguments_type_provider = type_analyzer.getExpressionTypes(node);

        for (const auto & child: function.arguments->children)
        {
            InterpretIMResult argument_result;

            if (!is_lambda_argument(child))
            {
                argument_result = visit(child);

                if (argument_result.isAST())
                    all_const = false;
                else if (argument_result.getField().isNull())
                    has_null_argument = true;
            }
            else
                argument_result = {arguments_type_provider.at(child), child};

            argument_results.push_back(std::move(argument_result));
        }
    }

    auto function_builder = FunctionFactory::instance().get(function.name, context);
    auto function_builder_params = convertToFunctionBuilderParams(argument_results);
    FunctionBasePtr function_base = function_builder->build(function_builder_params);
    // arguments' type may be changed by some simplify rules, so refresh current node's type
    auto function_ret_type = function_base->getResultType();
    Utils::checkState(function_ret_type != nullptr, "Function return type is null: " + serializeAST(*node));

    // === Constant folding ===
    // Note that the prerequisite of constant folding in ce slightly diff with that in cnch, this is intentional. i.e.,
    //   In ce, constant folding requires `function_base->isSuitableForConstantFolding() == true` and `isColumnConst(*res_col)`
    //   In cnch, constant folding requires `function_base->isDeterministic() == true` and `function_base->isSuitableForConstantFolding() == true`
    // This is because some functions do not satisfy `isColumnConst(*res_col)` in cnch, which cause constant folding not work and
    // furthermore block other optimizations(e.g. outer join to inner join)
    if (function_base->isSuitableForConstantFolding() && !has_lambda_argument)
    {
        ColumnPtr res_col;

        if (all_const)
            res_col = function_base->execute(function_builder_params, function_ret_type, 1, false);
        else
            res_col = function_base->getConstantResultForNonConstArguments(function_builder_params, function_ret_type);

        if (res_col && isColumnConst(*res_col))
        {
            if (res_col->empty())
                res_col = res_col->cloneResized(1);

            if (res_col->size() == 1)
            {
                try
                {
                    (*res_col)[0];
                    return {function_ret_type, res_col};
                }
                catch (Exception & ) {}
            }
        }
    }

    // === Null simplify ===
    if (has_null_argument && function_builder->useDefaultImplementationForNulls() && setting.enable_null_simplify)
        return {makeNullable(std::make_shared<DataTypeNothing>()), Null()};

    // === Function simplify ===
    using namespace function_simplify_rules_;

    bool simplified = false;
    InterpretIMResult simplify_result;
    bool reevaluate = false;

    if (setting.enable_function_simplify)
    {
        // TODO: simplify CASE expr
        simplified =
            RewriteAnd::apply(function, argument_results, simplify_result, context) ||
            RewriteOr::apply(function, argument_results, simplify_result, context) ||
            simplifyNullPrediction(function, argument_results, simplify_result) ||
            simplifyTrivialEquals(function, argument_results, simplify_result) ||
            simplifyIf(function, argument_results, simplify_result, reevaluate);
    }

    if (!simplified)
        return {function_ret_type, makeFunction(function.name, argument_results, context)};
    else if (!reevaluate)
        return simplify_result;
    else
    {
        assert(simplify_result.isAST());
        return visit(simplify_result.ast);
    }
}

InterpretIMResult ExpressionInterpreter::visitInFunction(const ASTFunction & function, const ConstASTPtr &) const
{
    const auto & left_arg = function.arguments->children[0];
    const auto & right_arg = function.arguments->children[1];
    auto left_arg_result = visit(left_arg);
    auto rewritten_left_arg = left_arg_result.convertToAST(context);

    if (left_arg_result.isAST() && !setting.enable_function_simplify)
        return {std::make_shared<DataTypeUInt8>(), makeASTFunction(function.name, rewritten_left_arg, right_arg)};

    // build set for IN statement(see also ActionsVisitor)
    SetPtr set;
    {
        auto & left_arg_type = left_arg_result.type;
        DataTypes set_element_types = {left_arg_type};
        const auto * left_tuple_type = typeid_cast<const DataTypeTuple *>(left_arg_type.get());
        if (left_tuple_type && left_tuple_type->getElements().size() != 1)
            set_element_types = left_tuple_type->getElements();

        for (auto & element_type : set_element_types)
            if (const auto * low_cardinality_type = typeid_cast<const DataTypeLowCardinality *>(element_type.get()))
                element_type = low_cardinality_type->getDictionaryType();

        Block block;
        auto right_arg_function = std::dynamic_pointer_cast<ASTFunction>(right_arg);
        if (right_arg_function && (right_arg_function->name == "tuple" || right_arg_function->name == "array"))
            block = createBlockForSet(left_arg_type, right_arg_function, set_element_types, context);
        else
            block = createBlockForSet(left_arg_type, right_arg, set_element_types, context);

        const auto & settings = context->getSettingsRef();
        SizeLimits size_limits{settings.max_rows_in_set, settings.max_bytes_in_set, settings.set_overflow_mode};
        set = std::make_shared<Set>(size_limits, true, settings.transform_null_in);
        set->setHeader(block.cloneEmpty());
        set->insertFromBlock(block);
        set->finishInsert();
    }

    // constant folding
    if (left_arg_result.isValue())
    {
        auto column_set = ColumnSet::create(1, set);
        ColumnPtr const_column_set = ColumnConst::create(std::move(column_set), 1);
        ColumnsWithTypeAndName columns_with_types;
        columns_with_types.emplace_back(left_arg_result.value, left_arg_result.type, "");
        columns_with_types.emplace_back(const_column_set, std::make_shared<DataTypeSet>(), "");
        auto result = FunctionInvoker::execute(function.name, columns_with_types, context);
        return {result.type, result.value};
    }

    // convert Set to AST
    auto set_columns = set->getSetElements();
    // TODO: support (x, y) IN ((1, 2), (3, 4))
    if (set_columns.size() != 1)
        return {std::make_shared<DataTypeUInt8>(), makeASTFunction(function.name, rewritten_left_arg, right_arg)};

    auto & set_column = set_columns[0];
    ASTs set_values;

    for (size_t i = 0; i < set_column->size(); ++i)
        set_values.push_back(LiteralEncoder::encodeForComparisonExpr((*set_column)[i], left_arg_result.type, context));

    // rewrite `x IN 1` to `x = 1`
    if (set_values.size() == 1)
    {
        // TODO: x IN NULL?
        auto result_type = makeNullableByArgumentTypes<DataTypeUInt8>({left_arg_result});
        String comparison_op = (function.name == "in" || function.name == "globalIn") ? "equals" : "notEquals";
        auto comparison_func = makeASTFunction(comparison_op, rewritten_left_arg, set_values.front());
        return {result_type, comparison_func};
    }

    auto tuple_func = makeASTFunction("tuple", set_values);
    return {std::make_shared<DataTypeUInt8>(), makeASTFunction(function.name, rewritten_left_arg, tuple_func)};
}

}
