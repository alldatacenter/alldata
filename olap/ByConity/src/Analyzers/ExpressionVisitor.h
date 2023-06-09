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

#include <Parsers/ASTVisitor.h>
#include <Analyzers/function_utils.h>
#include <Analyzers/Analysis.h>
#include <QueryPlan/Void.h>

namespace DB
{

/// Since the ASTFunction is heavily reused, the ExpressionVisitor is used for providing more fine-grained visiting methods.
template <typename C, typename R = void>
class ExpressionVisitor : public ASTVisitor<R, C>
{
private:
    ContextPtr context;

protected:
    virtual R visitExpression(ASTPtr &, IAST &, C &) { throw Exception("not implemented", ErrorCodes::NOT_IMPLEMENTED); }

    virtual R visitLiteral(ASTPtr & node, ASTLiteral & ast, C & visitor_context) { return visitExpression(node, ast, visitor_context); }
    virtual R visitIdentifier(ASTPtr & node, ASTIdentifier & ast, C & visitor_context) { return visitExpression(node, ast, visitor_context); }
    virtual R visitFieldReference(ASTPtr & node, ASTFieldReference & ast, C & visitor_context) { return visitExpression(node, ast, visitor_context); }
    virtual R visitFunction(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitExpression(node, ast, visitor_context); }
    virtual R visitAggregateFunction(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitFunction(node, ast, visitor_context); }
    virtual R visitWindowFunction(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitFunction(node, ast, visitor_context); }
    virtual R visitLambdaExpression(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitFunction(node, ast, visitor_context); }
    virtual R visitGroupingOperation(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitFunction(node, ast, visitor_context); }
    virtual R visitOrdinaryFunction(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitFunction(node, ast, visitor_context); }
    virtual R visitInSubquery(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitFunction(node, ast, visitor_context); }
    virtual R visitExistsSubquery(ASTPtr & node, ASTFunction & ast, C & visitor_context) { return visitFunction(node, ast, visitor_context); }
    virtual R visitScalarSubquery(ASTPtr & node, ASTSubquery & ast, C & visitor_context) { return visitExpression(node, ast, visitor_context); }
    virtual R visitQuantifiedComparisonSubquery(ASTPtr & node, ASTQuantifiedComparison & ast, C & visitor_context) { return visitExpression(node, ast, visitor_context); }

public:
    explicit ExpressionVisitor(ContextPtr context_): context(context_) {}

    virtual R process(ASTPtr & node, C & visitor_context)
    {
        return ASTVisitorUtil::accept(node, *this, visitor_context);
    }

    R visitASTLiteral(ASTPtr & node, C & visitor_context) final
    {
        return visitLiteral(node, node->as<ASTLiteral &>(), visitor_context);
    }

    R visitASTIdentifier(ASTPtr & node, C & visitor_context) final
    {
        return visitIdentifier(node, node->as<ASTIdentifier &>(), visitor_context);
    }

    R visitASTFieldReference(ASTPtr & node, C & visitor_context) final
    {
        return visitFieldReference(node, node->as<ASTFieldReference &>(), visitor_context);
    }

    R visitASTSubquery(ASTPtr & node, C & visitor_context) final
    {
        return visitScalarSubquery(node, node->as<ASTSubquery &>(), visitor_context);
    }

    R visitASTFunction(ASTPtr & node, C & visitor_context) final
    {
        auto & function = node->as<ASTFunction &>();
        auto function_type = getFunctionType(function, context);

        switch (function_type)
        {
            case FunctionType::FUNCTION:
                return visitOrdinaryFunction(node, function, visitor_context);
            case FunctionType::AGGREGATE_FUNCTION:
                return visitAggregateFunction(node, function, visitor_context);
            case FunctionType::WINDOW_FUNCTION:
                return visitWindowFunction(node, function, visitor_context);
            case FunctionType::GROUPING_OPERATION:
                return visitGroupingOperation(node, function, visitor_context);
            case FunctionType::LAMBDA_EXPRESSION:
                return visitLambdaExpression(node, function, visitor_context);
            case FunctionType::EXISTS_SUBQUERY:
                return visitExistsSubquery(node, function, visitor_context);
            case FunctionType::IN_SUBQUERY:
                return visitInSubquery(node, function, visitor_context);
            default:
                throw Exception("unknown function type", ErrorCodes::NOT_IMPLEMENTED);
        }
    }

    R visitASTQuantifiedComparison(ASTPtr & node, C & visitor_context) final
    {
        return visitQuantifiedComparisonSubquery(node, node->as<ASTQuantifiedComparison &>(), visitor_context);
    }
};

/// ExpressionTreeVisitor is used to provide default traversal logic for expression asts. (used for analyzed expr)
template <typename UserContext>
class ExpressionTraversalIncludeSubqueryVisitor: public ExpressionVisitor<const Void>
{
private:
    Analysis & analysis;
    ExpressionVisitor<UserContext> & user_visitor;
    UserContext & user_context;

protected:
    void visitExpression(ASTPtr &, IAST &, const Void &) override {}

    void visitFunction(ASTPtr &, ASTFunction & ast, const Void &) override
    {
        process(ast.arguments);
    }

    void visitWindowFunction(ASTPtr &, ASTFunction & ast, const Void &) override
    {
        process(ast.arguments);

        // window partition by/order by may have been rewritten by Analyzer
        auto window_analysis = analysis.getWindowAnalysis(ast.ptr());
        auto resolved_window = window_analysis->resolved_window;

        if (resolved_window->partition_by)
            process(resolved_window->partition_by);

        if (resolved_window->order_by)
            process(resolved_window->order_by);
    }

    void visitLambdaExpression(ASTPtr &, ASTFunction & ast, const Void &) override
    {
        auto lambda_body = getLambdaExpressionBody(ast);
        process(lambda_body);
    }

    void visitInSubquery(ASTPtr &, ASTFunction & ast, const Void &) override
    {
        process(ast.arguments->children[0]);
        auto subquery = ast.arguments->children[1];
        process(subquery->children[0]);
    }

    void visitExistsSubquery(ASTPtr &, ASTFunction & ast, const Void &) override
    {
        auto subquery = ast.arguments->children[0];
        process(subquery->children[0]);
    }

    void visitScalarSubquery(ASTPtr &, ASTSubquery & ast, const Void &) override
    {
        process(ast.children[0]);
    }

    void visitQuantifiedComparisonSubquery(ASTPtr &, ASTQuantifiedComparison & ast, const Void &) override
    {
        process(ast.children[0]);
        auto subquery = ast.children[1];
        process(subquery->children[0]);
    }

public:
    ExpressionTraversalIncludeSubqueryVisitor(ExpressionVisitor<UserContext> & user_visitor_, UserContext & user_context_, Analysis & analysis_, ContextPtr context_):
        ExpressionVisitor(context_), analysis(analysis_), user_visitor(user_visitor_), user_context(user_context_) {}

    void process(ASTPtr & node, const Void & traversal_context) override
    {
        // node is expression AST
        if (node->as<ASTIdentifier>() || node->as<ASTFunction>() || node->as<ASTLiteral>() || node->as<ASTSubquery>() || node->as<ASTFieldReference>() || node->as<ASTQuantifiedComparison>())
            user_visitor.process(node, user_context);

        return ASTVisitorUtil::accept(node, *this, traversal_context);
    }

    void process(ASTPtr & node)
    {
        process(node, {});
    }

    void process(ASTs & nodes)
    {
        for (auto & node: nodes)
            process(node, {});
    }

    void visitASTSelectQuery(ASTPtr & node, const Void &) override
    {
        auto & select_query = node->as<ASTSelectQuery &>();

        if (auto tables = select_query.tables())
            process(tables);
        if (auto where = select_query.where())
            process(where);
        if (auto groupBy = select_query.groupBy())
            process(groupBy);
        if (auto having = select_query.having())
            process(having);
        if (auto select = select_query.select())
            process(analysis.getSelectExpressions(select_query));
        if (auto orderBy = select_query.orderBy())
            process(orderBy);
        if (auto limitBy = select_query.limitBy())
            process(limitBy);
    }

    void visitASTSelectWithUnionQuery(ASTPtr & node, const Void &) override
    {
        process(node->as<ASTSelectWithUnionQuery &>().list_of_selects);
    }

    void visitASTExpressionList(ASTPtr & node, const Void &) override
    {
        process(node->children);
    }

    void visitASTOrderByElement(ASTPtr & node, const Void &) override
    {
        process(node->children[0]);
    }

    void visitASTTablesInSelectQuery(ASTPtr & node, const Void &) override
    {
        process(node->children);
    }

    void visitASTTablesInSelectQueryElement(ASTPtr & node, const Void &) override
    {
        auto & elem = node->as<ASTTablesInSelectQueryElement &>();
        if (elem.table_expression)
            process(elem.table_expression);
        if (elem.table_join)
            process(elem.table_join);
    }

    void visitASTTableJoin(ASTPtr & node, const Void &) override
    {
        auto & table_join = node->as<ASTTableJoin &>();
        if (table_join.on_expression)
            process(table_join.on_expression);
        // process JOIN USING list?
    }

    void visitASTTableExpression(ASTPtr & node, const Void &) override
    {
        auto & table_expression = node->as<ASTTableExpression &>();
        if (table_expression.subquery)
            process(table_expression.subquery->children[0]);
    }
};

template <typename UserContext>
class ExpressionTraversalVisitor: public ExpressionTraversalIncludeSubqueryVisitor<UserContext>
{
public:
    using ExpressionTraversalIncludeSubqueryVisitor<UserContext>::ExpressionTraversalIncludeSubqueryVisitor;
    using ExpressionTraversalIncludeSubqueryVisitor<UserContext>::process;

    void visitASTSelectQuery(ASTPtr &, const Void &) override {}

    void visitASTSelectWithUnionQuery(ASTPtr &, const Void &) override {}
};

template<typename UserContext, template<typename> typename TraversalVisitor = ExpressionTraversalVisitor>
void traverseExpressionTree(ASTPtr & node, ExpressionVisitor<UserContext> & user_visitor, UserContext & user_context, Analysis & analysis, ContextPtr context)
{
    TraversalVisitor<UserContext> visitor {user_visitor, user_context, analysis, context};
    visitor.process(node);
}

}
