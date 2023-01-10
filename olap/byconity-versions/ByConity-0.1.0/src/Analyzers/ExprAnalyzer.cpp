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

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/parseAggregateFunctionParameters.h>
#include <Analyzers/ExprAnalyzer.h>
#include <Analyzers/QueryAnalyzer.h>
#include <Analyzers/tryEvaluateConstantExpression.h>
#include <Analyzers/function_utils.h>
#include <Core/ColumnsWithTypeAndName.h>
#include <Common/StringUtils/StringUtils.h>
#include <DataTypes/DataTypeFunction.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeSet.h>
#include <DataTypes/FieldToDataType.h>
#include <DataTypes/getLeastSupertype.h>
#include <Functions/FunctionFactory.h>
#include <Interpreters/convertFieldToType.h>
#include <Interpreters/misc.h>
#include <IO/WriteHelpers.h>
#include <Parsers/ASTVisitor.h>
#include <QueryPlan/Void.h>

#include <Poco/String.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int SYNTAX_ERROR;
    extern const int TYPE_MISMATCH;
    extern const int UNKNOWN_FUNCTION;
    extern const int BAD_ARGUMENTS;
    extern const int UNKNOWN_IDENTIFIER;
    extern const int ILLEGAL_AGGREGATION;
    extern const int TOO_FEW_ARGUMENTS_FOR_FUNCTION;
    extern const int TOO_MANY_ARGUMENTS_FOR_FUNCTION;
}

class ExprAnalyzerVisitor : public ASTVisitor<ColumnWithTypeAndName, const Void>
{
public:
    // Call `process` instead of `ASTVisitorUtil::accept` to process a node,
    // as there are some common logics in `process` method.
    ColumnWithTypeAndName process(ASTPtr & node);
    ColumnsWithTypeAndName processNodes(ASTs & nodes);

    ColumnWithTypeAndName visitNode(ASTPtr & node, const Void &) override;
    ColumnWithTypeAndName visitASTFunction(ASTPtr & node, const Void &) override;
    // Note that this method is only used for processing scalar subquery.
    // For generic subquery expression processing, see `handleSubquery`.
    ColumnWithTypeAndName visitASTSubquery(ASTPtr & node, const Void &) override;
    ColumnWithTypeAndName visitASTIdentifier(ASTPtr & node, const Void &) override;
    ColumnWithTypeAndName visitASTFieldReference(ASTPtr & node, const Void &) override;
    ColumnWithTypeAndName visitASTLiteral(ASTPtr & node, const Void &) override;
    ColumnWithTypeAndName visitASTOrderByElement(ASTPtr & node, const Void &) override;
    ColumnWithTypeAndName visitASTQuantifiedComparison(ASTPtr & node, const Void &) override;

    ExprAnalyzerVisitor(ContextMutablePtr context_, Analysis & analysis_, ScopePtr scope_, ExprAnalyzerOptions options_):
        context(std::move(context_)),
        analysis(analysis_),
        options(std::move(options_)),
        use_ansi_semantic(context->getSettingsRef().dialect_type == DialectType::ANSI),
        enable_implicit_type_conversion(context->getSettingsRef().enable_implicit_type_conversion),
        allow_extended_conversion(context->getSettingsRef().allow_extended_type_conversion),
        scopes({scope_})
    {}

    void setInWindow(bool x) { in_window = x; }

private:
    ContextMutablePtr context;
    Analysis & analysis;
    const ExprAnalyzerOptions options;
    const bool use_ansi_semantic;
    const bool enable_implicit_type_conversion;
    const bool allow_extended_conversion;

    std::vector<ScopePtr> scopes;
    // whether we are in an aggregate function
    bool in_aggregate = false;
    // whether we are in a window function
    bool in_window = false;

    ScopePtr currentScope() const
    {
        return scopes.back();
    }

    ScopePtr baseScope() const
    {
        return scopes.front();
    }

    void enterLambda(ScopePtr lambda_scope)
    {
        if (!lambda_scope || lambda_scope->getType() != Scope::ScopeType::LAMBDA)
            throw Exception("Provided scope is not a lambda scope", ErrorCodes::LOGICAL_ERROR);

        scopes.push_back(lambda_scope);
    }

    void exitLambda()
    {
        if (scopes.size() <= 1)
            throw Exception("No outer scope.", ErrorCodes::LOGICAL_ERROR);

        if (currentScope()->getType() != Scope::ScopeType::LAMBDA)
            throw Exception("Current scope is not a lambda scope", ErrorCodes::LOGICAL_ERROR);

        scopes.pop_back();
    }

    bool isInLambda() const { return currentScope()->getType() == Scope::ScopeType::LAMBDA; }

    ColumnWithTypeAndName analyzeWindowFunction(ASTFunctionPtr & function);
    ColumnWithTypeAndName analyzeAggregateFunction(ASTFunctionPtr & function);
    ColumnWithTypeAndName analyzeGroupingOperation(ASTFunctionPtr & function);
    ColumnWithTypeAndName analyzeInSubquery(ASTFunctionPtr & function);
    ColumnWithTypeAndName analyzeExistsSubquery(ASTFunctionPtr & function);
    ColumnWithTypeAndName analyzeOrdinaryFunction(ASTFunctionPtr & function);

    std::pair<AggregateFunctionPtr, Array> resolveAggregateFunction(ASTFunction & function);
    DataTypePtr handleSubquery(const ASTPtr & subquery);
    ColumnWithTypeAndName handleResolvedField(ASTPtr & node, const ResolvedField & field);

    void processSubqueryArgsWithCoercion(ASTPtr & lhs_ast, ASTPtr & rhs_ast);
};

DataTypePtr ExprAnalyzer::analyze(ASTPtr expression,
                                  ScopePtr scope,
                                  ContextMutablePtr context,
                                  Analysis & analysis,
                                  ExprAnalyzerOptions options)
{
    ExprAnalyzerVisitor expr_visitor {context, analysis, scope, options};
    return expr_visitor.process(expression).type;
}

ColumnWithTypeAndName ExprAnalyzerVisitor::process(ASTPtr & node)
{
    if (analysis.hasExpressionType(node))
        return {analysis.getExpressionType(node), ""};

    auto result = ASTVisitorUtil::accept(node, *this, {});

    if (!result.type)
        throw Exception("Can not determine expression type: " + serializeAST(*node), ErrorCodes::LOGICAL_ERROR);

    analysis.setExpressionType(node, result.type);
    return result;
}

ColumnsWithTypeAndName ExprAnalyzerVisitor::processNodes(ASTs & nodes)
{
    ColumnsWithTypeAndName processed(nodes.size());
    std::transform(nodes.begin(), nodes.end(), processed.begin(), [&](auto && node) { return process(node);});
    return processed;
}

ColumnWithTypeAndName ExprAnalyzerVisitor::visitNode(ASTPtr & node, const Void &)
{
    throw Exception("Unsupported Node" + node->getID(), ErrorCodes::NOT_IMPLEMENTED);
}

ColumnWithTypeAndName ExprAnalyzerVisitor::visitASTLiteral(ASTPtr & node, const Void &)
{
    auto & literal = node->as<ASTLiteral &>();
    DataTypePtr type = applyVisitor(FieldToDataType(), literal.value);
    // TODO: remove convertFieldToType if we have done in parser/rewriter phase
    ColumnPtr column = type->createColumnConst(1, convertFieldToType(literal.value, *type));
    return {column, type, ""};
}

ColumnWithTypeAndName ExprAnalyzerVisitor::visitASTIdentifier(ASTPtr & node, const Void &)
{
    auto & identifier = node->as<ASTIdentifier &>();
    std::optional<ResolvedField> resolved;

    if (use_ansi_semantic)
        resolved = currentScope()->resolveFieldByAnsi(QualifiedName::extractQualifiedName(identifier));
    else
        resolved = currentScope()->resolveFieldByClickhouse(identifier.name());

    if (!resolved)
        throw Exception("Can not resolve identifier: " + identifier.name(), ErrorCodes::UNKNOWN_IDENTIFIER);

    return handleResolvedField(node, *resolved);
}

ColumnWithTypeAndName ExprAnalyzerVisitor::visitASTFieldReference(ASTPtr & node, const Void &)
{
    // FieldReference is only used to refer to a relation field.
    auto & field_ref = node->as<ASTFieldReference &>();

    if (field_ref.field_index >= baseScope()->size())
        throw Exception("Illegal field reference node", ErrorCodes::LOGICAL_ERROR);

    ResolvedField resolved {baseScope(), field_ref.field_index};

    return handleResolvedField(node, resolved);
}

ColumnWithTypeAndName ExprAnalyzerVisitor::visitASTOrderByElement(ASTPtr & node, const Void &)
{
    auto & order_by = node->as<ASTOrderByElement &>();
    // TODO: handle collation/fill_xxx fields
    if (order_by.with_fill)
        throw Exception("ORDER BY WITH FILL not implemented", ErrorCodes::NOT_IMPLEMENTED);
    return process(order_by.children.front());
}

ColumnWithTypeAndName ExprAnalyzerVisitor::visitASTFunction(ASTPtr & node, const Void &)
{
    ASTFunctionPtr function_ptr = std::dynamic_pointer_cast<ASTFunction>(node);
    auto function_type = getFunctionType(*function_ptr, context);

    if (function_type == FunctionType::WINDOW_FUNCTION)
        return analyzeWindowFunction(function_ptr);
    if (function_type == FunctionType::AGGREGATE_FUNCTION)
        return analyzeAggregateFunction(function_ptr);
    if (function_type == FunctionType::GROUPING_OPERATION)
        return analyzeGroupingOperation(function_ptr);
    if (function_type == FunctionType::IN_SUBQUERY)
        return analyzeInSubquery(function_ptr);
    if (function_type == FunctionType::EXISTS_SUBQUERY)
        return analyzeExistsSubquery(function_ptr);
    if (function_type == FunctionType::FUNCTION)
        return analyzeOrdinaryFunction(function_ptr);

    Exception ex("Unknown function " + function_ptr->name, ErrorCodes::UNKNOWN_FUNCTION);

    auto hints = FunctionFactory::instance().getHints(function_ptr->name);
    if (!hints.empty())
        ex.addMessage(" Maybe you meant: " + toString(hints) + ".");

    hints = AggregateFunctionFactory::instance().getHints(function_ptr->name);
    if (!hints.empty())
        ex.addMessage("Or unknown aggregate function " + function_ptr->name + ". Maybe you meant: " + toString(hints));

    throw Exception(ex);
}

ColumnWithTypeAndName ExprAnalyzerVisitor::visitASTQuantifiedComparison(ASTPtr & node, const Void &)
{
    auto  quantified_comparison = std::dynamic_pointer_cast<ASTQuantifiedComparison>(node);
    auto & lhs_ast = quantified_comparison->children[0];
    auto & rhs_ast = quantified_comparison->children[1];
    processSubqueryArgsWithCoercion(lhs_ast, rhs_ast);
    analysis.quantified_comparison_subqueries[options.select_query].push_back(quantified_comparison);
    return {nullptr, std::make_shared<DataTypeUInt8>(), ""};
}


ColumnWithTypeAndName ExprAnalyzerVisitor::visitASTSubquery(ASTPtr & node, const Void &)
{
    auto type = handleSubquery(node);

    // when a scalar subquery has 0 rows, it returns NULL, hence we change its type to Nullable type
    // note that this feature is not compatible with subquery with multiple output returning Tuple type
    // see test 00420_null_in_scalar_subqueries
    if (!type->isNullable() && type->canBeInsideNullable())
    {
        type = makeNullable(type);
        analysis.setTypeCoercion(node, type);
    }

    analysis.scalar_subqueries[options.select_query].push_back(node);
    return {nullptr, type, ""};
}

ColumnWithTypeAndName ExprAnalyzerVisitor::analyzeOrdinaryFunction(ASTFunctionPtr & function)
{
    auto overload_resolver = FunctionFactory::instance().get(function->name, context);
    ASTs & arguments = function->arguments->children;
    ColumnsWithTypeAndName processed_arguments(arguments.size());
    DataTypes arguments_types(arguments.size());
    std::vector<size_t> lambda_arg_index;

    for (size_t i = 0; i < arguments.size(); ++i)
    {
        auto * lambda = arguments[i]->as<ASTFunction>();
        // for high-ordered functions, we analyze lambda expression args in next cycle
        if (lambda && lambda->name == "lambda")
        {
            auto lambda_arg_size = getLambdaExpressionArguments(*lambda).size();
            arguments_types[i] = std::make_shared<DataTypeFunction>(DataTypes(lambda_arg_size));
            lambda_arg_index.emplace_back(i);
        }
        else
        {
            processed_arguments[i] = process(arguments[i]);

            // fix 01457_int256_hashing.sql
            if (checkFunctionIsInOrGlobalInOperator(*function) && i == 1)
            {
                processed_arguments[i].type = std::make_shared<DataTypeSet>();
            }

            arguments_types[i] = processed_arguments[i].type;
        }
    }

    if (!lambda_arg_index.empty())
    {
        // resolve lambda expressions' argument types
        overload_resolver->getLambdaArgumentTypes(arguments_types);
        for (const auto & index : lambda_arg_index)
        {
            auto & lambda = arguments[index]->as<ASTFunction &>();
            const auto * lambda_type = typeid_cast<const DataTypeFunction *>(arguments_types[index].get());
            auto lambda_args = getLambdaExpressionArguments(lambda);
            auto lambda_body = getLambdaExpressionBody(lambda);

            FieldDescriptions lambda_fields;

            for (size_t j = 0; j < lambda_args.size(); ++j)
            {
                auto lambda_arg_name = tryGetIdentifierName(lambda_args[j]);
                if (!lambda_arg_name)
                    throw Exception("lambda argument declarations must be identifiers", ErrorCodes::TYPE_MISMATCH);

                lambda_fields.emplace_back(*lambda_arg_name, lambda_type->getArgumentTypes()[j]);
            }

            const auto *lambda_scope = analysis.scope_factory.createLambdaScope(currentScope(), lambda_fields);

            analysis.setScope(lambda, lambda_scope);

            enterLambda(lambda_scope);
            auto lambda_ret_type = process(lambda_body).type;
            exitLambda();

            auto resolved_lambda_type = std::make_shared<DataTypeFunction>(lambda_type->getArgumentTypes(), lambda_ret_type);
            // since we don't call `process` for lambda argument, register its type manually
            analysis.setExpressionType(arguments[index], resolved_lambda_type);
            processed_arguments[index] = {nullptr, resolved_lambda_type,""};
        }
    }

    auto function_base = overload_resolver->build(processed_arguments);

    // post analysis for sub column optimization
    String func_name_lowercase = Poco::toLower(function->name);

    if ((startsWith(func_name_lowercase, "mapelement") || startsWith(func_name_lowercase, "arrayelement"))
        && function->arguments->children.size() == 2)
    {
        if (auto column_reference = analysis.tryGetColumnReference(function->arguments->children[0]))
        {
            const auto & resolved_field = column_reference->getFieldDescription();
            if (resolved_field.origin_table &&
                resolved_field.origin_table->supportsMapImplicitColumn() &&
                !resolved_field.type->isMapKVStore())
            {
                if (auto * key_lit = function->arguments->children[1]->as<ASTLiteral>())
                {
                    auto key_name = key_lit->getColumnName();
                    auto column_id = SubColumnID::mapElement(key_name);
                    analysis.setSubColumnReference(function, SubColumnReference {*column_reference, column_id});
                    analysis.addUsedSubColumn(resolved_field.origin_table_ast, resolved_field.index_of_origin_scope, column_id);
                }
            }
        }
    }
    if (startsWith(func_name_lowercase, "mapkeys") && function->arguments->children.size() == 1)
    {
        if (auto column_reference = analysis.tryGetColumnReference(function->arguments->children[0]))
        {
            const auto & resolved_field = column_reference->getFieldDescription();
            if (resolved_field.origin_table &&
                resolved_field.origin_table->supportsMapImplicitColumn() &&
                resolved_field.type->isMap() &&
                resolved_field.type->isMapKVStore())
            {
                auto column_id = SubColumnID::mapKeys();
                analysis.setSubColumnReference(function, SubColumnReference {*column_reference, column_id});
                analysis.addUsedSubColumn(resolved_field.origin_table_ast, resolved_field.index_of_origin_scope, column_id);
            }
        }
    }

    if (!function_base->isDeterministicInScopeOfQuery() || !function_base->isDeterministicInScopeOfQuery()
        || !function_base->isSuitableForConstantFolding())
    {
        analysis.addNonDeterministicFunctions(*function);
        context->setFunctionDeterministic(function->name, false);
    }
    return {nullptr, function_base->getResultType(), ""};
}

ColumnWithTypeAndName ExprAnalyzerVisitor::analyzeAggregateFunction(ASTFunctionPtr & function)
{
    if (options.aggregate_support == ExprAnalyzerOptions::AggregateSupport::DISALLOWED)
        throw Exception("Aggregate function is not supported in " + options.statement_name,
                        ErrorCodes::SYNTAX_ERROR);

    if (!options.select_query)
        throw Exception("Provide query node if aggregate function is allowed", ErrorCodes::LOGICAL_ERROR);

    if (in_aggregate)
        throw Exception("Nested aggregate function is not supported", ErrorCodes::ILLEGAL_AGGREGATION);

    in_aggregate = true;
    AggregateFunctionPtr aggregator;
    Array parameters;
    std::tie(aggregator, parameters) = resolveAggregateFunction(*function);

    AggregateAnalysis aggregate_analysis;
    aggregate_analysis.expression = function;
    aggregate_analysis.function = aggregator;
    aggregate_analysis.parameters = parameters;
    analysis.aggregate_results[options.select_query].push_back(aggregate_analysis);
    in_aggregate = false;
    return {nullptr, aggregate_analysis.function->getReturnType(), ""};
}

ColumnWithTypeAndName ExprAnalyzerVisitor::analyzeWindowFunction(ASTFunctionPtr & function)
{
    if (options.window_support == ExprAnalyzerOptions::WindowSupport::DISALLOWED)
        throw Exception("Window function is not supported in " + options.statement_name, ErrorCodes::SYNTAX_ERROR);

    if (!options.select_query)
        throw Exception("Provide query node if window function is allowed", ErrorCodes::LOGICAL_ERROR);

    if (in_window)
        throw Exception("Nested window function is not supported", ErrorCodes::SYNTAX_ERROR);

    if (in_aggregate)
        throw Exception("Window function under an aggregate function is not supported", ErrorCodes::SYNTAX_ERROR);

    in_window = true;

    String window_name;
    ResolvedWindowPtr resolved_window;
    AggregateFunctionPtr aggregator;
    Array parameters;

    if (!function->window_name.empty())
    {
        window_name = function->window_name;
        resolved_window = analysis.getRegisteredWindow(*options.select_query, function->window_name);
    }
    else
    {
        window_name = function->window_definition->as<ASTWindowDefinition &>().getDefaultWindowName();
        resolved_window = resolveWindow(function->window_definition, analysis.getRegisteredWindows(*options.select_query), context);
    }

    if (resolved_window->partition_by)
        processNodes(resolved_window->partition_by->children);

    if (resolved_window->order_by)
        processNodes(resolved_window->order_by->children);

    std::tie(aggregator, parameters) = resolveAggregateFunction(*function);

    auto window_analysis = std::make_shared<WindowAnalysis>();
    window_analysis->expression = function;
    window_analysis->window_name = window_name;
    window_analysis->resolved_window = resolved_window;
    window_analysis->aggregator = aggregator;
    window_analysis->parameters = parameters;
    analysis.addWindowAnalysis(*options.select_query, std::move(window_analysis));
    in_window = false;
    return {nullptr, aggregator->getReturnType(), ""};
}

ColumnWithTypeAndName ExprAnalyzerVisitor::analyzeGroupingOperation(ASTFunctionPtr & function)
{
    if (options.aggregate_support == ExprAnalyzerOptions::AggregateSupport::DISALLOWED)
        throw Exception("Grouping operation is not supported in " + options.statement_name,
                        ErrorCodes::SYNTAX_ERROR);

    if (!options.select_query)
        throw Exception("Provide query node if grouping operation is allowed", ErrorCodes::LOGICAL_ERROR);

    if (in_aggregate)
        throw Exception("Nested aggregate function is not supported", ErrorCodes::SYNTAX_ERROR);

    in_aggregate = true;

    if (!function->arguments || function->arguments->children.empty())
        throw Exception(ErrorCodes::TOO_FEW_ARGUMENTS_FOR_FUNCTION, "Function GROUPING expects at least one argument");

    if (function->arguments->children.size() > 64)
        throw Exception(ErrorCodes::TOO_MANY_ARGUMENTS_FOR_FUNCTION, "Function GROUPING can have up to 64 arguments, but {} provided", function->arguments->children.size());

    processNodes(function->arguments->children);

    analysis.grouping_operations[options.select_query].push_back(function);
    in_aggregate = false;
    return {nullptr, std::make_shared<DataTypeUInt64>(), ""};
}

ColumnWithTypeAndName ExprAnalyzerVisitor::analyzeInSubquery(ASTFunctionPtr & function)
{
    auto & lhs_ast = function->arguments->children[0];
    auto & rhs_ast = function->arguments->children[1];
    processSubqueryArgsWithCoercion(lhs_ast, rhs_ast);
    analysis.in_subqueries[options.select_query].push_back(function);
    return {nullptr, std::make_shared<DataTypeUInt8>(), ""};
}

ColumnWithTypeAndName ExprAnalyzerVisitor::analyzeExistsSubquery(ASTFunctionPtr & function)
{
    if (function->children.size() != 1)
        throw Exception("Invalid exists subquery expression: " + serializeAST(*function), ErrorCodes::SYNTAX_ERROR);

    handleSubquery(function->arguments->children[0]);
    analysis.exists_subqueries[options.select_query].push_back(function);
    return {nullptr, std::make_shared<DataTypeUInt8>(), ""};
}

void ExprAnalyzerVisitor::processSubqueryArgsWithCoercion(ASTPtr & lhs_ast, ASTPtr & rhs_ast)
{
    auto lhs_type = process(lhs_ast).type;
    auto rhs_type = handleSubquery(rhs_ast);

    if (!lhs_type->equals(*rhs_type))
    {
        DataTypePtr super_type = nullptr;
        if (enable_implicit_type_conversion)
            super_type = getLeastSupertype({lhs_type, rhs_type}, allow_extended_conversion);
        if (!super_type)
            throw Exception("Incompatible types for IN prediacte", ErrorCodes::TYPE_MISMATCH);
        if (!lhs_type->equals(*super_type))
            analysis.setTypeCoercion(lhs_ast, super_type);
        if (!rhs_type->equals(*super_type))
            analysis.setTypeCoercion(rhs_ast, super_type);
    }
}

std::pair<AggregateFunctionPtr, Array> ExprAnalyzerVisitor::resolveAggregateFunction(ASTFunction & function)
{
    auto processed_arguments = processNodes(function.arguments->children);
    Array parameters = (function.parameters) ? getAggregateFunctionParametersArray(function.parameters, "", context) : Array();
    DataTypes argument_types(processed_arguments.size());
    std::transform(processed_arguments.begin(), processed_arguments.end(), argument_types.begin(), [](auto && arg) { return arg.type; });

    for (auto & item : argument_types)
        item = recursiveRemoveLowCardinality(item);

    AggregateFunctionProperties properties;
    auto aggregate = AggregateFunctionFactory::instance().get(function.name, argument_types, parameters, properties);

    return {aggregate, parameters};
}

DataTypePtr ExprAnalyzerVisitor::handleSubquery(const ASTPtr & subquery)
{
    if (auto * s = subquery->as<ASTSubquery>(); !s)
        throw Exception("Invalid subquery expression", ErrorCodes::LOGICAL_ERROR);

    if (options.subquery_support == ExprAnalyzerOptions::SubquerySupport::DISALLOWED)
        throw Exception("Subquery is not supported in " + options.statement_name, ErrorCodes::SYNTAX_ERROR);

    if (!options.select_query)
        throw Exception("Provide query node if subquery is allowed", ErrorCodes::LOGICAL_ERROR);

    if (isInLambda())
        throw Exception("Subquery is not support in lambda", ErrorCodes::SYNTAX_ERROR);

    QueryAnalyzer::analyze(subquery->children[0], currentScope(), context, analysis);
    auto & output_columns = analysis.getOutputDescription(*subquery);

    DataTypePtr type;

    if (output_columns.size() == 1)
    {
        type = output_columns[0].type;
    }
    else
    {
        // if subquery has multiple output column, its return type should be the tuple of columns
        DataTypes column_types(output_columns.size());
        std::transform(output_columns.begin(), output_columns.end(), column_types.begin(), std::mem_fn(&FieldDescription::type));
        type = std::make_shared<DataTypeTuple>(column_types);
    }

    analysis.setExpressionType(subquery, type);
    return type;
}

ColumnWithTypeAndName ExprAnalyzerVisitor::handleResolvedField(ASTPtr & node, const ResolvedField & field)
{
    if (field.scope->getType() == Scope::ScopeType::RELATION)
        analysis.setColumnReference(node, field);
    else if (field.scope->getType() == Scope::ScopeType::LAMBDA)
        analysis.setLambdaArgumentReference(node, field);

    return {nullptr, field.getFieldDescription().type, ""};
}

}
