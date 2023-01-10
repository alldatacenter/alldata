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

#include <Optimizer/SymbolTransformMap.h>

#include <Optimizer/SimpleExpressionRewriter.h>
#include <Optimizer/Utils.h>
#include <Parsers/ASTTableColumnReference.h>
#include <QueryPlan/PlanVisitor.h>

namespace DB
{
class SymbolTransformMap::Visitor : public PlanNodeVisitor<Void, Void>
{
public:
    Void visitAggregatingNode(AggregatingNode & node, Void & context) override
    {
        const auto * agg_step = dynamic_cast<const AggregatingStep *>(node.getStep().get());
        for (const auto & aggregate_description : agg_step->getAggregates())
        {
            auto function = Utils::extractAggregateToFunction(aggregate_description);
            addSymbolExpressionMapping(aggregate_description.column_name, function);
        }
        return visitChildren(node, context);
    }

    Void visitFilterNode(FilterNode & node, Void & context) override
    {
        return visitChildren(node, context);
    }

    Void visitProjectionNode(ProjectionNode & node, Void & context) override
    {
        const auto * project_step = dynamic_cast<const ProjectionStep *>(node.getStep().get());
        for (const auto & assignment : project_step->getAssignments())
        {
            if (Utils::isIdentity(assignment))
                continue;
            addSymbolExpressionMapping(assignment.first, assignment.second);
            // if (const auto * function = dynamic_cast<const ASTFunction *>(assignment.second.get()))
            // {
            //     if (function->name == "cast" && TypeCoercion::compatible)
            //     {
            //         symbol_to_cast_lossless_expressions.emplace(assignment.first, function->children[0]);
            //     }
            // }
        }
        return visitChildren(node, context);
    }

    Void visitJoinNode(JoinNode & node, Void & context) override { return visitChildren(node, context); }

    Void visitTableScanNode(TableScanNode & node, Void &) override
    {
        auto table_step = dynamic_cast<const TableScanStep *>(node.getStep().get());
        for (const auto & item : table_step->getColumnAlias())
        {
            auto column_reference = std::make_shared<ASTTableColumnReference>(table_step->getStorage(), item.first);
            addSymbolExpressionMapping(item.second, column_reference);
        }
        return Void{};
    }

    Void visitChildren(PlanNodeBase & node, Void & context)
    {
        for (auto & child : node.getChildren())
            VisitorUtil::accept(*child, *this, context);
        return Void{};
    }

public:
    std::unordered_map<String, ConstASTPtr> symbol_to_expressions;
    std::unordered_map<String, ConstASTPtr> symbol_to_cast_lossless_expressions;
    bool valid = true;

    void addSymbolExpressionMapping(const String & symbol, ConstASTPtr expr)
    {
        // violation may happen when matching the root node, which may contain duplicate
        // symbol names with other plan nodes. e.g. select sum(amount) as amount
        if (!symbol_to_expressions.emplace(symbol, std::move(expr)).second)
            valid = false;
    }
};

class SymbolTransformMap::Rewriter : public SimpleExpressionRewriter<Void>
{
public:
    Rewriter(
        const std::unordered_map<String, ConstASTPtr> & symbol_to_expressions_,
        std::unordered_map<String, ConstASTPtr> & expression_lineage_)
        : symbol_to_expressions(symbol_to_expressions_)
        , expression_lineage(expression_lineage_)
    {
    }

    ASTPtr visitASTIdentifier(ASTPtr & expr, Void & context) override
    {
        auto & name = expr->as<ASTIdentifier &>().name();

        if (expression_lineage.count(name))
            return expression_lineage.at(name)->clone();

        if (!symbol_to_expressions.count(name))
            throw Exception("Unknown column " + name + " in SymbolTransformMap", ErrorCodes::LOGICAL_ERROR);
        ASTPtr rewrite = ASTVisitorUtil::accept(symbol_to_expressions.at(name)->clone(), *this, context);
        expression_lineage[name] = rewrite;
        return rewrite;
    }

private:
    const std::unordered_map<String, ConstASTPtr> & symbol_to_expressions;
    std::unordered_map<String, ConstASTPtr> & expression_lineage;
};

std::optional<SymbolTransformMap> SymbolTransformMap::buildFrom(PlanNodeBase & plan)
{
    Visitor visitor;
    Void context;
    VisitorUtil::accept(plan, visitor, context);
    std::optional<SymbolTransformMap> ret;
    if (visitor.valid)
        ret = SymbolTransformMap{visitor.symbol_to_expressions, visitor.symbol_to_cast_lossless_expressions};
    return ret;
}

ASTPtr SymbolTransformMap::inlineReferences(const ConstASTPtr & expression) const
{
    Rewriter rewriter{symbol_to_expressions, expression_lineage};
    Void context;
    return ASTVisitorUtil::accept(expression->clone(), rewriter, context);
}
}
