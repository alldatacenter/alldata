/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.query;

import org.apache.atlas.query.antlr4.AtlasDSLParser.AliasExprContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.AtomEContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.CompEContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.ComparisonClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.CountClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.ExprContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.ExprRightContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.FromExpressionContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.FromSrcContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.GroupByExpressionContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.HasClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.HasTermClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.IdentifierContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.IsClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.LimitOffsetContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.MaxClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.MinClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.OrderByExprContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.SelectExprContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.SelectExpressionContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.SingleQrySrcContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.SumClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.ValueArrayContext;
import org.apache.atlas.query.antlr4.AtlasDSLParser.WhereClauseContext;
import org.apache.atlas.query.antlr4.AtlasDSLParserBaseVisitor;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DSLVisitor extends AtlasDSLParserBaseVisitor<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(DSLVisitor.class);

    private static final String AND = "AND";
    private static final String OR  = "OR";

    private final GremlinQueryComposer gremlinQueryComposer;

    public DSLVisitor(GremlinQueryComposer gremlinQueryComposer) {
        this.gremlinQueryComposer = gremlinQueryComposer;
    }

    @Override
    public Void visitLimitOffset(LimitOffsetContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> DSLVisitor.visitLimitOffset({})", ctx);
        }

        gremlinQueryComposer.addLimit(ctx.limitClause().NUMBER().getText(),
                                      (ctx.offsetClause() == null ? "0" : ctx.offsetClause().NUMBER().getText()));

        return super.visitLimitOffset(ctx);
    }

    @Override
    public Void visitSelectExpr(SelectExprContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> DSLVisitor.visitSelectExpr({})", ctx);
        }

        // Select can have only attributes, aliased attributes or aggregate functions

        // Groupby attr also represent select expr, no processing is needed in that case
        // visit groupBy would handle the select expr appropriately
        if (!(ctx.getParent() instanceof GroupByExpressionContext)) {
            String[] items    = new String[ctx.selectExpression().size()];
            String[] labels   = new String[ctx.selectExpression().size()];
            int      countIdx = -1;
            int      sumIdx   = -1;
            int      minIdx   = -1;
            int      maxIdx   = -1;

            for (int i = 0; i < ctx.selectExpression().size(); i++) {
                SelectExpressionContext selectExpression = ctx.selectExpression(i);
                CountClauseContext      countClause      = selectExpression.expr().compE().countClause();
                SumClauseContext        sumClause        = selectExpression.expr().compE().sumClause();
                MinClauseContext        minClause        = selectExpression.expr().compE().minClause();
                MaxClauseContext        maxClause        = selectExpression.expr().compE().maxClause();
                IdentifierContext       identifier       = selectExpression.identifier();

                labels[i] = identifier != null ? identifier.getText() : selectExpression.getText();

                if (Objects.nonNull(countClause)) {
                    items[i] = "count";
                    countIdx = i;
                } else if (Objects.nonNull(sumClause)) {
                    items[i] = sumClause.expr().getText();
                    sumIdx   = i;
                } else if (Objects.nonNull(minClause)) {
                    items[i] = minClause.expr().getText();
                    minIdx   = i;
                } else if (Objects.nonNull(maxClause)) {
                    items[i] = maxClause.expr().getText();
                    maxIdx   = i;
                } else {
                    items[i] = selectExpression.expr().getText();
                }
            }

            SelectClauseComposer selectClauseComposer = new SelectClauseComposer(labels, items, items, countIdx, sumIdx, minIdx, maxIdx);

            gremlinQueryComposer.addSelect(selectClauseComposer);
        }

        return super.visitSelectExpr(ctx);
    }

    @Override
    public Void visitOrderByExpr(OrderByExprContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> DSLVisitor.visitOrderByExpr({})", ctx);
        }

        // Extract the attribute from parentheses
        String text = ctx.expr().getText().replace("(", "").replace(")", "");

        gremlinQueryComposer.addOrderBy(text, (ctx.sortOrder() != null && ctx.sortOrder().getText().equalsIgnoreCase("desc")));

        return super.visitOrderByExpr(ctx);
    }

    @Override
    public Void visitWhereClause(WhereClauseContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> DSLVisitor.visitWhereClause({})", ctx);
        }

        ExprContext expr = ctx.expr();

        processExpr(expr, gremlinQueryComposer);

        return super.visitWhereClause(ctx);
    }

    @Override
    public Void visitFromExpression(final FromExpressionContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> DSLVisitor.visitFromExpression({})", ctx);
        }

        FromSrcContext   fromSrc   = ctx.fromSrc();
        AliasExprContext aliasExpr = fromSrc.aliasExpr();

        if (aliasExpr != null) {
            gremlinQueryComposer.addFromAlias(aliasExpr.identifier(0).getText(), aliasExpr.identifier(1).getText());
        } else {
            if (fromSrc.identifier() != null) {
                gremlinQueryComposer.addFrom(fromSrc.identifier().getText());
            } else {
                gremlinQueryComposer.addFrom(fromSrc.literal().getText());
            }
        }

        return super.visitFromExpression(ctx);
    }

    @Override
    public Void visitSingleQrySrc(SingleQrySrcContext ctx) {
        if (ctx.fromExpression() == null) {
            if (ctx.expr() != null && !gremlinQueryComposer.hasFromClause()) {
                inferFromClause(ctx);
            }

            if (ctx.expr() != null && gremlinQueryComposer.hasFromClause()) {
                processExpr(ctx.expr(), gremlinQueryComposer);
            }
        }

        return super.visitSingleQrySrc(ctx);
    }

    @Override
    public Void visitGroupByExpression(GroupByExpressionContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> DSLVisitor.visitGroupByExpression({})", ctx);
        }

        String s = ctx.selectExpr().getText();

        gremlinQueryComposer.addGroupBy(s);

        return super.visitGroupByExpression(ctx);
    }

    private Void visitIsClause(GremlinQueryComposer gqc, IsClauseContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> DSLVisitor.visitIsClause({})", ctx);
        }

        gqc.addIsA(ctx.arithE().getText(), ctx.identifier().getText());

        return super.visitIsClause(ctx);
    }

    private void visitHasClause(GremlinQueryComposer gqc, HasClauseContext ctx) {
        gqc.addFromProperty(ctx.arithE().getText(), ctx.identifier().getText());

        super.visitHasClause(ctx);
    }

    private void visitHasTermClause(GremlinQueryComposer gqc, HasTermClauseContext ctx) {
        gqc.addHasTerm(ctx.arithE().getText(), ctx.identifier().getText());

        super.visitHasTermClause(ctx);
    }

    private void inferFromClause(SingleQrySrcContext ctx) {
        if (ctx.fromExpression() != null) {
            return;
        }

        if (ctx.expr() != null && gremlinQueryComposer.hasFromClause()) {
            return;
        }

        if (ctx.expr().compE() != null && ctx.expr().compE().isClause() != null && ctx.expr().compE().isClause().arithE() != null) {
            gremlinQueryComposer.addFrom(ctx.expr().compE().isClause().arithE().getText());

            return;
        }

        if (ctx.expr().compE() != null && ctx.expr().compE().hasClause() != null && ctx.expr().compE().hasClause().arithE() != null) {
            gremlinQueryComposer.addFrom(ctx.expr().compE().hasClause().arithE().getText());
        }

        if (ctx.expr().compE() != null && ctx.expr().compE().hasTermClause() != null && ctx.expr().compE().hasTermClause().arithE() != null) {
            gremlinQueryComposer.addFrom(ctx.expr().compE().hasTermClause().arithE().getText());

            return;
        }
    }

    private void processExpr(final ExprContext expr, GremlinQueryComposer gremlinQueryComposer) {
        if (CollectionUtils.isNotEmpty(expr.exprRight())) {
            processExprRight(expr, gremlinQueryComposer);
        } else {
            GremlinQueryComposer original = gremlinQueryComposer.newInstance();
            original.addAll(gremlinQueryComposer.getQueryClauses());

            processExpr(expr.compE(), gremlinQueryComposer);

            if (gremlinQueryComposer.hasAnyTraitAttributeClause()) {
                gremlinQueryComposer.addAll(original.getQueryClauses());
                processExprForTrait(expr, gremlinQueryComposer);
            }

        }
    }

    private void processExprForTrait(final ExprContext expr, GremlinQueryComposer gremlinQueryComposer) {
        //add AND clause
        GremlinQueryComposer nestedProcessor = gremlinQueryComposer.createNestedProcessor();
        processExpr(expr.compE(), nestedProcessor);

        GremlinClauseList clauseList         = nestedProcessor.getQueryClauses();
        if (clauseList.size() > 1) {
            gremlinQueryComposer.addAndClauses(Collections.singletonList(nestedProcessor));
        }
    }

    private void processExprRight(final ExprContext expr, GremlinQueryComposer gremlinQueryComposer) {
        GremlinQueryComposer       nestedProcessor = gremlinQueryComposer.createNestedProcessor();
        List<GremlinQueryComposer> nestedQueries   = new ArrayList<>();
        String                     prev            = null;

        // Process first expression then proceed with the others
        // expr -> compE exprRight*
        processExpr(expr.compE(), nestedProcessor);
        nestedQueries.add(nestedProcessor);

        // Record all processed attributes
        gremlinQueryComposer.addProcessedAttributes(nestedProcessor.getAttributesProcessed());

        for (ExprRightContext exprRight : expr.exprRight()) {
            nestedProcessor = gremlinQueryComposer.createNestedProcessor();

            // AND expression
            if (exprRight.K_AND() != null) {
                if (OR.equalsIgnoreCase(prev)) {
                    // Change of context
                    GremlinQueryComposer orClause = nestedProcessor.createNestedProcessor();

                    orClause.addOrClauses(nestedQueries);
                    nestedQueries.clear();
                    nestedQueries.add(orClause);

                    // Record all processed attributes
                    gremlinQueryComposer.addProcessedAttributes(orClause.getAttributesProcessed());
                }

                prev = AND;
            }

            // OR expression
            if (exprRight.K_OR() != null) {
                if (AND.equalsIgnoreCase(prev)) {
                    // Change of context
                    GremlinQueryComposer andClause = nestedProcessor.createNestedProcessor();

                    andClause.addAndClauses(nestedQueries);
                    nestedQueries.clear();
                    nestedQueries.add(andClause);

                    // Record all processed attributes
                    gremlinQueryComposer.addProcessedAttributes(andClause.getAttributesProcessed());
                }

                prev = OR;
            }

            processExpr(exprRight.compE(), nestedProcessor);
            nestedQueries.add(nestedProcessor);

            // Record all processed attributes
            gremlinQueryComposer.addProcessedAttributes(nestedProcessor.getAttributesProcessed());
        }

        if (AND.equalsIgnoreCase(prev)) {
            gremlinQueryComposer.addAndClauses(nestedQueries);
        } else if (OR.equalsIgnoreCase(prev)) {
            gremlinQueryComposer.addOrClauses(nestedQueries);
        }
    }

    private void processExpr(final CompEContext compE, final GremlinQueryComposer gremlinQueryComposer) {
        if (compE != null) {
            IsClauseContext      isClause      = compE.isClause();
            HasClauseContext     hasClause     = compE.hasClause();
            HasTermClauseContext hasTermClause = compE.hasTermClause();

            if (isClause != null) {
                visitIsClause(gremlinQueryComposer, isClause);
            }

            if (hasClause != null) {
                visitHasClause(gremlinQueryComposer, hasClause);
            }

            if (hasTermClause != null) {
                visitHasTermClause(gremlinQueryComposer, hasTermClause);
            }

            if (isClause == null && hasClause == null && hasTermClause == null) {
                ComparisonClauseContext comparisonClause = compE.comparisonClause();

                // The nested expression might have ANDs/ORs
                if (comparisonClause == null) {
                    ExprContext exprContext = compE.arithE().multiE().atomE().expr();

                    // Only extract comparison clause if there are no nested exprRight clauses
                    if (CollectionUtils.isEmpty(exprContext.exprRight())) {
                        comparisonClause = exprContext.compE().comparisonClause();
                    }
                }

                if (comparisonClause != null) {
                    String       lhs      = comparisonClause.arithE(0).getText();
                    AtomEContext atomECtx = comparisonClause.arithE(1).multiE().atomE();
                    String       op, rhs;

                    if (atomECtx.literal() == null || (atomECtx.literal() != null && atomECtx.literal().valueArray() == null)) {
                        op  = comparisonClause.operator().getText().toUpperCase();
                        rhs = comparisonClause.arithE(1).getText();
                    } else {
                        op  = "in";
                        rhs = getInClause(atomECtx);
                    }

                    gremlinQueryComposer.addWhere(lhs, op, rhs);
                } else {
                    processExpr(compE.arithE().multiE().atomE().expr(), gremlinQueryComposer);
                }
            }
        }
    }

    private String getInClause(AtomEContext atomEContext) {
        StringBuilder     sb                = new StringBuilder();
        ValueArrayContext valueArrayContext = atomEContext.literal().valueArray();
        int               startIdx          = 1;
        int               endIdx            = valueArrayContext.children.size() - 1;

        for (int i = startIdx; i < endIdx; i++) {
            sb.append(valueArrayContext.getChild(i));
        }

        return sb.toString();
    }
}