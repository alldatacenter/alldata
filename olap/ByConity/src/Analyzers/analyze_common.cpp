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

#include <Analyzers/analyze_common.h>
#include <Analyzers/ExpressionVisitor.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Optimizer/PredicateUtils.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

ASTPtr joinCondition(const ASTTableJoin & join)
{
    if (join.using_expression_list)
        return join.using_expression_list;
    if (join.on_expression)
        return join.on_expression;
    return nullptr;
}

bool isNormalInnerJoin(const ASTTableJoin & join)
{
    return join.kind == ASTTableJoin::Kind::Inner && (
               join.strictness == ASTTableJoin::Strictness::All ||
               join.strictness == ASTTableJoin::Strictness::Any ||
               join.strictness == ASTTableJoin::Strictness::Unspecified
           );
}

bool isCrossJoin(const ASTTableJoin & join)
{
    return isCross(join.kind) || isComma(join.kind);
}

bool isSemiOrAntiJoin(const ASTTableJoin & join)
{
    return isSemi(join.strictness) || isAnti(join.strictness);
}

bool isAsofJoin(const ASTTableJoin & join)
{
    return isAsof(join.strictness);
}

String getFunctionForInequality(ASOF::Inequality inequality)
{
    if (inequality == ASOF::Inequality::Less)
        return "less";
    else if (inequality == ASOF::Inequality::Greater)
        return "greater";
    else if (inequality == ASOF::Inequality::LessOrEquals)
        return "lessOrEquals";
    else if (inequality == ASOF::Inequality::GreaterOrEquals)
        return "greaterOrEquals";
    throw Exception("Unknown inequality", ErrorCodes::LOGICAL_ERROR);
}

static void expressionToCnf(const ASTPtr & node, std::vector<ASTPtr> & members)
{
    if (const auto * func = node->as<ASTFunction>(); func && func->name == "and")
    {
        for (const auto & child : func->arguments->children)
            expressionToCnf(child, members);
        return;
    }
    members.push_back(node);
}

std::vector<ASTPtr> expressionToCnf(const ASTPtr & node)
{
    std::vector<ASTPtr> members;
    expressionToCnf(node, members);
    return members;
}

ASTPtr cnfToExpression(const std::vector<ASTPtr> & cnf)
{
    if (cnf.empty())
        return PredicateConst::TRUE_VALUE;

    if (cnf.size() == 1)
        return cnf[0];

    return makeASTFunction("and", cnf);
}

namespace
{
    struct ExtractExpressionContext
    {
        const std::function<bool(const ASTPtr &)> & expr_filter;
        std::vector<ASTPtr> result;

        explicit ExtractExpressionContext(const std::function<bool(const ASTPtr &)> & expr_filter_): expr_filter(expr_filter_) {}
    };

    class ExtractExpressionVisitor: public ExpressionVisitor<ExtractExpressionContext>
    {
    protected:
        void visitExpression(ASTPtr & node, IAST &, ExtractExpressionContext & ctx) override
        {
            if (ctx.expr_filter(node))
                ctx.result.push_back(node);
        }
    public:
        using ExpressionVisitor<ExtractExpressionContext>::ExpressionVisitor;
    };
}

std::vector<ASTPtr> extractExpressions(ContextPtr context, Analysis & analysis, ASTPtr root, bool include_subquery,
                                       const std::function<bool(const ASTPtr &)> & filter)
{
    ExtractExpressionContext extractor_context {filter};
    ExtractExpressionVisitor extractor_visitor {context};

    if (include_subquery)
        traverseExpressionTree<ExtractExpressionContext, ExpressionTraversalIncludeSubqueryVisitor>(root, extractor_visitor, extractor_context, analysis, context);
    else
        traverseExpressionTree(root, extractor_visitor, extractor_context, analysis, context);

    return extractor_context.result;
}

std::vector<ASTPtr> extractReferencesToScope(ContextPtr context, Analysis & analysis, ASTPtr root, ScopePtr scope, bool include_subquery)
{
    auto filter = [&](const ASTPtr & node) -> bool {
        if (auto col_ref = analysis.tryGetColumnReference(node); col_ref && col_ref->scope == scope)
            return true;

        return false;
    };

    return extractExpressions(context, analysis, root, include_subquery, filter);
}

}
