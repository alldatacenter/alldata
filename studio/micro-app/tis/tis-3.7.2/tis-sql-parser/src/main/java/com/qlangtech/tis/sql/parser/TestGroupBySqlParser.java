/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.sql.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta.SqlDataFlowTopology;
// import com.facebook.presto.sql.SqlFormatter;
import com.facebook.presto.sql.tree.AliasedRelation;
import com.facebook.presto.sql.tree.ArithmeticBinaryExpression;
import com.facebook.presto.sql.tree.ArithmeticBinaryExpression.Operator;
import com.facebook.presto.sql.tree.Cast;
import com.facebook.presto.sql.tree.CoalesceExpression;
import com.facebook.presto.sql.tree.ComparisonExpression;
import com.facebook.presto.sql.tree.DefaultTraversalVisitor;
import com.facebook.presto.sql.tree.DereferenceExpression;
import com.facebook.presto.sql.tree.Expression;
import com.facebook.presto.sql.tree.FunctionCall;
import com.facebook.presto.sql.tree.GroupBy;
import com.facebook.presto.sql.tree.GroupingElement;
import com.facebook.presto.sql.tree.Identifier;
import com.facebook.presto.sql.tree.Join;
import com.facebook.presto.sql.tree.Join.Type;
import com.facebook.presto.sql.tree.JoinCriteria;
import com.facebook.presto.sql.tree.JoinOn;
import com.facebook.presto.sql.tree.LogicalBinaryExpression;
import com.facebook.presto.sql.tree.LongLiteral;
import com.facebook.presto.sql.tree.Node;
import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.sql.tree.Query;
import com.facebook.presto.sql.tree.QuerySpecification;
import com.facebook.presto.sql.tree.Relation;
import com.facebook.presto.sql.tree.SearchedCaseExpression;
import com.facebook.presto.sql.tree.Select;
import com.facebook.presto.sql.tree.SelectItem;
import com.facebook.presto.sql.tree.SingleColumn;
import com.facebook.presto.sql.tree.StringLiteral;
import com.facebook.presto.sql.tree.SubscriptExpression;
import com.facebook.presto.sql.tree.Table;
import com.facebook.presto.sql.tree.TableSubquery;
import com.facebook.presto.sql.tree.WhenClause;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.common.utils.Assert;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestGroupBySqlParser {

    public static void main(String[] args) throws Exception {
        // SqlParser parser = new SqlParser();
        // TestGroupBySqlParser sqlParser = new TestGroupBySqlParser();
        List<SqlTaskNode> taskNodes = SqlTaskNodeMeta.getSqlDataFlowTopology("totalpay").parseTaskNodes();
        // 找到终端节点
        // 有依赖，但是没有被依赖
        // 查找 终止节点
        // findTerminatorTaskNode(taskNodes);
        SqlTaskNode terminatorNode = null;
        // StringBuffer content = new StringBuffer();
        // Map<ColName, ValueOperator> columnTracer = Maps.newHashMap();
        // Rewriter rewriter = Rewriter.create(columnTracer);
        // Query query =
        terminatorNode.parse(true);
    // System.out.println(SqlFormatter.formatSql(query, Optional.empty()));
    //
    // System.out.println("terminator node:" + terminatorNode.getExportName());
    // for (SqlTaskNode taskNode : taskNodes) {
    //
    // Query query = (Query) parser.createStatement(taskNode.getContent(), new
    // ParsingOptions());
    // StringBuffer content = new StringBuffer();
    // Map<String, Object> columnTracer = Maps.newHashMap();
    // Rewriter rewriter = new Rewriter(content, 0, new Stack<Object>(),
    // columnTracer);
    // sqlParser.process(query, rewriter);
    //
    // System.out.println("===========================================");
    // System.out.println(content);
    // }
    }

    // private static class Visitor extends TISStackableAstVisitor<Void, Integer> {
    //
    // @Override
    // public Void process(Node node, StackableAstVisitorContext<Integer> context) {
    // return super.process(node, context);
    // }
    //
    // protected Void visitNode(Node node, StackableAstVisitorContext<Integer>
    // context) {
    // throw new UnsupportedOperationException(String.valueOf(node));
    // }
    //
    // protected Void visitAliasedRelation(AliasedRelation node,
    // StackableAstVisitorContext<Integer> context) {
    //
    // this.process(node.getRelation(), context);
    //
    // if (context.getPreviousNode().isPresent()) {
    // System.out.println("getPreviousNode:" + context.getPreviousNode().get());
    // }
    //
    // System.out.println(node.getAlias().getValue());
    //
    // return null;
    // }
    //
    // @Override
    // protected Void visitComparisonExpression(ComparisonExpression node,
    // StackableAstVisitorContext<Integer> context) {
    //
    // node.getLeft();
    // // System.out.println(node.getOperator() + "," +
    // node.getOperator().getValue());
    // node.getRight();
    //
    // return null;
    // // return super.visitComparisonExpression(node, context);
    // }
    //
    // @Override
    // protected Void visitLogicalBinaryExpression(LogicalBinaryExpression node,
    // StackableAstVisitorContext<Integer> context) {
    //
    // System.out.println("operator:" + node.getOperator());
    //
    // this.process(node.getLeft(), context);
    //
    // this.process(node.getRight(), context);
    //
    // return null;
    //
    // // return super.visitLogicalBinaryExpression(node, context);
    // }
    //
    // @Override
    // protected Void visitTable(Table node, StackableAstVisitorContext<Integer>
    // context) {
    //
    // // System.out.println(node.getName() + "," +
    // context.getPreviousNode().get());
    //
    // return null;
    // }
    //
    // protected Void visitQuery(Query node, StackableAstVisitorContext<Integer>
    // context) {
    // if (node.getWith().isPresent()) {
    //
    // }
    //
    // processRelation(node.getQueryBody(), context);
    //
    // if (node.getOrderBy().isPresent()) {
    // process(node.getOrderBy().get(), context);
    // }
    //
    // if (node.getLimit().isPresent()) {
    // // append(indent, "LIMIT " + node.getLimit().get())
    // // .append('\n');
    // }
    //
    // return null;
    // }
    //
    // @Override
    // protected Void visitSelect(Select node, StackableAstVisitorContext<Integer>
    // context) {
    //
    // for (SelectItem item : node.getSelectItems()) {
    // visitSelectItem(item, context);
    // }
    // return null;
    // // return super.visitSelect(node, context);
    // }
    //
    // @Override
    // protected Void visitSelectItem(SelectItem node,
    // StackableAstVisitorContext<Integer> context) {
    //
    // // System.out.println(node);
    //
    // return null;
    // // return super.visitSelectItem(node, context);
    // }
    //
    // protected Void visitQuerySpecification(QuerySpecification body,
    // StackableAstVisitorContext<Integer> context) {
    // // return visitQueryBody(node, context);
    //
    // // Select select = ;
    // this.visitSelect(body.getSelect(), context);
    // // System.out.println("Columns = " + select.getSelectItems());
    //
    // // System.out.println("FromClass = " + body.getFrom().get().getClass());
    //
    // // System.out.println("From = " + body.getFrom().get());
    //
    // // Optional<Expression> where = body.getWhere();
    // // System.out.println("Where = " + where.get());
    // // System.out.println("Group by = " + body.getGroupBy());
    // // System.out.println("Order by = " + body.getOrderBy());
    //
    // if (body.getLimit().isPresent()) {
    // // System.out.println("Limit = " + body.getLimit().get());
    // }
    //
    // // final List<Expression> groupIds = Lists.newArrayList();
    // Optional<GroupBy> gby = body.getGroupBy();
    // if (gby.isPresent()) {
    // this.visitGroupBy(gby.get(), context);
    // // GroupBy group = gby.get();
    // // for (GroupingElement ge : group.getGroupingElements()) {
    // // ge.enumerateGroupingSets().forEach((e) -> e.stream().forEach((r) -> {
    // // // rewriter.append(r.getClass()).append(",");
    // // groupIds.add(r);
    // // }));
    // // }
    // }
    // Optional<Relation> from = body.getFrom();
    // ///////////////////////////////////////////////////////////////////////////
    // //
    // System.out.println("======================================================");
    // // List<SelectItem>
    // SingleColumn single = null;
    //
    // // int itemSize = select.getSelectItems().size();
    // // int i = 0;
    // // for (SelectItem col : select.getSelectItems()) {
    // //
    // // if (col instanceof SingleColumn) {
    // // single = (SingleColumn) col;
    // // processExpression(single.getExpression(), rewriter);
    // // Optional<Identifier> alias = single.getAlias();
    // //
    // // if (alias.isPresent()) {
    // // // 这个可能是function的列
    // // rewriter.addColumnReference(alias.get().getValue(),
    // single.getExpression(),
    // // from);
    // // System.out.println(
    // // single.getExpression().getClass() + ":" + single.getExpression() + ":" +
    // // alias.get());
    // //
    // // rewriter.append(" AS ").append(alias.get());
    // // } else {
    // // System.out.println(
    // // "==================" + single.getExpression() + "<<" +
    // // single.getExpression().getClass());
    // // // rewriter.append(single.getExpression());
    // // // processExpression(single.getExpression(), rewriter);
    // // DereferenceExpression deref = null;
    // // if (single.getExpression() instanceof DereferenceExpression) {
    // // deref = (DereferenceExpression) single.getExpression();
    // //
    // // rewriter.addColumnReference(deref.getField().getValue(), deref, from);
    // // } else if (single.getExpression() instanceof Identifier) {
    // // if (!from.isPresent()) {
    // // throw new IllegalStateException("have not set from \n" +
    // query.toString());
    // // }
    // //
    // // rewriter.addColumnReference( //
    // // new ColName(((Identifier) single.getExpression()).getValue()) //
    // // , null //
    // // , from);
    // //
    // // } else {
    // // throw new IllegalStateException(
    // // "illegal type " + single.getExpression() + "," +
    // // single.getExpression().getClass());
    // // }
    // //
    // // }
    // //
    // // } else {
    // //
    // // }
    // //
    // // if (i++ < (itemSize - 1)) {
    // // rewriter.append(",");
    // // }
    // // }
    //
    // Relation rel = null;
    // // Table table = null;
    // if (from.isPresent()) {
    // rel = from.get();
    // // rewriter.newline().append("FROM ");
    // // processRelation(rel, rewriter);
    //
    // this.processRelation(rel, context);
    //
    // } else {
    // throw new IllegalStateException("have not set from \n" + body);
    // }
    //
    // Optional<Expression> w = body.getWhere();
    // Expression where = null;
    // if (w.isPresent()) {
    // where = w.get();
    // process(where, context);
    // // rewriter.newline().append("WHERE ");
    // // rewriter.append(table);
    //
    // // processExpression(where, rewriter);
    //
    // } else {
    // // if (table != null) {
    // // rewriter.newline().append("WHERE ");
    // // }
    // }
    //
    // // for (int ii = 0; ii < groupIds.size(); ii++) {
    // // processExpression(groupIds.get(ii), rewriter);
    // // if (ii < groupIds.size() - 1) {
    // // rewriter.append(",");
    // // }
    // // }
    //
    // return null;
    // }
    //
    // @Override
    // protected Void visitJoin(Join node, StackableAstVisitorContext<Integer>
    // context) {
    //
    // JoinCriteria criteria = node.getCriteria().orElse(null);
    // String type = node.getType().toString();
    // if (criteria instanceof NaturalJoin) {
    // type = "NATURAL " + type;
    // }
    //
    // if (node.getType() != Join.Type.IMPLICIT) {
    // // builder.append('(');
    // }
    // process(node.getLeft(), context);
    //
    // // builder.append('\n');
    // if (node.getType() == Join.Type.IMPLICIT) {
    // // append(indent, ", ");
    // } else {
    // // append(indent, type).append(" JOIN ");
    // }
    //
    // process(node.getRight(), context);
    //
    // if (node.getType() != Join.Type.CROSS && node.getType() !=
    // Join.Type.IMPLICIT) {
    // if (criteria instanceof JoinUsing) {
    // JoinUsing using = (JoinUsing) criteria;
    // // builder.append(" USING (").append(Joiner.on(",
    // // ").join(using.getColumns())).append(")");
    // } else if (criteria instanceof JoinOn) {
    // JoinOn on = (JoinOn) criteria;
    // // builder.append(" ON ").append(formatExpression(on.getExpression(),
    // // parameters));
    // } else if (!(criteria instanceof NaturalJoin)) {
    // throw new UnsupportedOperationException("unknown join criteria: " +
    // criteria);
    // }
    // }
    //
    // if (node.getType() != Join.Type.IMPLICIT) {
    // // builder.append(")");
    // }
    //
    // return null;
    //
    // }
    //
    // private void processRelation(Relation relation,
    // StackableAstVisitorContext<Integer> indent) {
    // // TODO: handle this properly
    // if (relation instanceof Table) {
    // // builder.append("TABLE ").append(((Table)
    // relation).getName()).append('\n');
    // } else {
    // process(relation, indent);
    // }
    // }
    //
    // }
    public void process(Query query, Rewriter rewriter) {
        QuerySpecification body = (QuerySpecification) query.getQueryBody();
        Select select = body.getSelect();
        if (body.getLimit().isPresent()) {
        // System.out.println("Limit = " + body.getLimit().get());
        }
        final List<Expression> groupIds = Lists.newArrayList();
        Optional<GroupBy> gby = body.getGroupBy();
        if (gby.isPresent()) {
            GroupBy group = gby.get();
            for (GroupingElement ge : group.getGroupingElements()) {
                ge.enumerateGroupingSets().forEach((e) -> e.stream().forEach((r) -> {
                    // rewriter.append(r.getClass()).append(",");
                    groupIds.add(r);
                }));
            }
        }
        Optional<Relation> from = body.getFrom();
        // /////////////////////////////////////////////////////////////////////////
        // System.out.println("======================================================");
        // List<SelectItem>
        SingleColumn single = null;
        rewriter.append("SELECT ");
        int itemSize = select.getSelectItems().size();
        int i = 0;
        for (SelectItem col : select.getSelectItems()) {
            if (col instanceof SingleColumn) {
                single = (SingleColumn) col;
                processExpression(single.getExpression(), rewriter);
                Optional<Identifier> alias = single.getAlias();
                if (alias.isPresent()) {
                    // 这个可能是function的列
                    rewriter.addColumnReference(alias.get().getValue(), single.getExpression(), from);
                    // System.out.println(
                    // single.getExpression().getClass() + ":" + single.getExpression() + ":" +
                    // alias.get());
                    rewriter.append(" AS ").append(alias.get());
                } else {
                    // System.out.println(
                    // "==================" + single.getExpression() + "<<" +
                    // single.getExpression().getClass());
                    // rewriter.append(single.getExpression());
                    // processExpression(single.getExpression(), rewriter);
                    DereferenceExpression deref = null;
                    if (single.getExpression() instanceof DereferenceExpression) {
                        deref = (DereferenceExpression) single.getExpression();
                        rewriter.addColumnReference(deref.getField().getValue(), deref, from);
                    } else if (single.getExpression() instanceof Identifier) {
                        if (!from.isPresent()) {
                            throw new IllegalStateException("have not set from \n" + query.toString());
                        }
                        //
                        //
                        rewriter.addColumnReference(//
                        new ColName(((Identifier) single.getExpression()).getValue()), null, from);
                    } else {
                        throw new IllegalStateException("illegal type " + single.getExpression() + "," + single.getExpression().getClass());
                    }
                }
            } else {
            }
            if (i++ < (itemSize - 1)) {
                rewriter.append(",");
            }
        }
        Relation rel = null;
        // Table table = null;
        if (from.isPresent()) {
            rel = from.get();
            rewriter.newline().append("FROM ");
            processRelation(rel, rewriter);
        } else {
            throw new IllegalStateException("have not set from \n" + query.toString());
        }
        Optional<Expression> w = body.getWhere();
        Expression where = null;
        if (w.isPresent()) {
            where = w.get();
            rewriter.newline().append("WHERE ");
            // rewriter.append(table);
            processExpression(where, rewriter);
        } else {
        // if (table != null) {
        // rewriter.newline().append("WHERE ");
        // }
        }
        for (int ii = 0; ii < groupIds.size(); ii++) {
            processExpression(groupIds.get(ii), rewriter);
            if (ii < groupIds.size() - 1) {
                rewriter.append(",");
            }
        }
    }

    // private void processExpression(Expression expression, Rewriter rewriter) {
    // processExpression(expression, rewriter, false /* fromSelectItem */);
    // }
    private void processExpression(Expression expression, Rewriter rewriter) {
        if (expression instanceof ArithmeticBinaryExpression) {
            // ((COALESCE(pay.coupon_fee, 0) - COALESCE(pay.coupon_cost, 0)) *
            // COALESCE(pay.coupon_num, 0))
            ArithmeticBinaryExpression arithmeticBinaryExpression = (ArithmeticBinaryExpression) expression;
            rewriter.append("(");
            processExpression(arithmeticBinaryExpression.getLeft(), rewriter);
            Operator operator = arithmeticBinaryExpression.getOperator();
            rewriter.append(operator.getValue());
            processExpression(arithmeticBinaryExpression.getRight(), rewriter);
            rewriter.append(")");
        } else if (expression instanceof Cast) {
            Cast cast = (Cast) expression;
            rewriter.append("CAST(");
            processExpression(cast.getExpression(), rewriter);
            rewriter.append(",").append(cast.getType()).append(")");
        } else if (expression instanceof LogicalBinaryExpression) {
            process((LogicalBinaryExpression) expression, rewriter);
        } else if (expression instanceof ComparisonExpression) {
            process((ComparisonExpression) expression, rewriter);
        } else if (expression instanceof CoalesceExpression) {
            // COALESCE(a2.has_fetch, 0)
            process((CoalesceExpression) expression, rewriter);
        } else if (expression instanceof FunctionCall) {
            // "concat_ws"(',', "collect_set"("split"(i.batch_msg, '[\\w\\W]*\\|')[1]))
            process((FunctionCall) expression, rewriter);
        } else if (expression instanceof SearchedCaseExpression) {
            // (CASE WHEN ("sum"("op_and"(i.draw_status, 8)) > 0) THEN 1 ELSE 0 END)
            SearchedCaseExpression e = (SearchedCaseExpression) expression;
            process(e, rewriter);
        } else if (expression instanceof StringLiteral) {
            rewriter.append(expression);
        } else if (expression instanceof SubscriptExpression) {
            SubscriptExpression subscript = (SubscriptExpression) expression;
            processExpression(subscript.getBase(), rewriter);
            rewriter.append("[");
            processExpression(subscript.getIndex(), rewriter);
            rewriter.append("]");
        // rewriter.append("base:").append(subscript.getBase()).append(",index:").append(subscript.getIndex());
        } else if (expression instanceof DereferenceExpression) {
            DereferenceExpression dereference = (DereferenceExpression) expression;
            processExpression(dereference.getBase(), rewriter);
            rewriter.append(".").append(dereference.getField());
        } else if (expression instanceof Identifier) {
            rewriter.append(expression);
        } else if (expression instanceof LongLiteral) {
            LongLiteral longLiteral = (LongLiteral) expression;
            rewriter.append(longLiteral.getValue());
        } else {
            throw new IllegalStateException("illegal:" + expression.getClass() + ",expression:" + expression + ",loc:colnum:" + expression.getLocation().get().getColumnNumber() + ",line:" + expression.getLocation().get().getLineNumber());
        }
    }

    private void process(ComparisonExpression where, Rewriter rewriter) {
        Expression left = where.getLeft();
        if (left instanceof FunctionCall) {
            process((FunctionCall) left, rewriter);
        } else {
            rewriter.append(left);
        }
        rewriter.append(where.getOperator().getValue()).append(where.getRight());
        // rewriter.append("op:").append(where.getOperator()).append("left:").append(where.getLeft()).append("right:")
        // .append(where.getRight());
        popAiasedTableRelation(rewriter);
    }

    private void process(LogicalBinaryExpression where, Rewriter rewriter) {
        rewriter.append(where);
        popAiasedTableRelation(rewriter);
    }

    private void process(CoalesceExpression expression, Rewriter rewriter) {
        // COALESCE(a2.has_fetch, 0)
        rewriter.append(" COALESCE(");
        int operandSize = expression.getOperands().size();
        int index = 0;
        for (Expression operand : expression.getOperands()) {
            // rewriter.append(operand);
            processExpression(operand, rewriter);
            if (index++ < operandSize - 1) {
                rewriter.append(",");
            }
        }
        rewriter.append(")");
    }

    private void process(FunctionCall expression, Rewriter rewriter) {
        // "concat_ws"(',', "collect_set"("split"(i.batch_msg, '[\\w\\W]*\\|')[1]))
        QualifiedName name = expression.getName();
        rewriter.append(name.toString()).append("(");
        List<Expression> exps = expression.getArguments();
        int argsize = exps.size();
        int index = 0;
        for (Expression arg : exps) {
            processExpression(arg, rewriter);
            // }
            if (index++ < argsize - 1) {
                rewriter.append(",");
            }
        }
        rewriter.append(")");
    }

    private void process(SearchedCaseExpression expression, Rewriter rewriter) {
        // (CASE WHEN ("sum"("op_and"(i.draw_status, 8)) > 0) THEN 1 ELSE 0 END)
        rewriter.append("CASE ");
        for (WhenClause when : expression.getWhenClauses()) {
            rewriter.append("WHEN ");
            processExpression(when.getOperand(), rewriter);
            // rewriter.append(when.getOperand().getClass());
            rewriter.append(" THEN ").append(when.getResult());
        }
        Optional<Expression> dft = expression.getDefaultValue();
        if (dft.isPresent()) {
            rewriter.append(" ELSE ").append(dft.get());
        }
        rewriter.append(" END");
    }

    private static void popAiasedTableRelation(Rewriter rewriter) {
        AliasedRelation aliasRelation = null;
        while (!rewriter.stack.isEmpty()) {
            aliasRelation = (AliasedRelation) rewriter.stack.pop();
            rewriter.append(" AND ").append(aliasRelation.getAlias().getValue()).append(".pt='").append("201901222222").append("'");
        }
    }

    public static class Rewriter {

        private final StringBuffer rewriter;

        final int stackDeep;

        private final Stack<Object> stack;

        private final Map<ColName, ValueOperator> columnTracer;

        public Rewriter(StringBuffer rewriter, int stackDeep, Stack<Object> stack, Map<ColName, ValueOperator> columnTracer) {
            super();
            this.rewriter = rewriter;
            this.stackDeep = stackDeep;
            this.stack = stack;
            this.columnTracer = columnTracer;
        }

        public Rewriter newline() {
            rewriter.append("\n");
            for (int i = 0; i < stackDeep; i++) {
                rewriter.append(" ");
            }
            return this;
        }

        public Rewriter append(Object content) {
            rewriter.append(content);
            return this;
        }

        public Rewriter deeper() {
            return new Rewriter(rewriter, stackDeep + 2, this.stack, this.columnTracer);
        }

        public Rewriter newRewriter() {
            // new Rewriter(content, 0, new Stack<Object>(), columnTracer);
            return create(this.columnTracer);
        }

        public static Rewriter create(Map<ColName, ValueOperator> columnTracer) {
            return new Rewriter(new StringBuffer(), 0, new Stack<Object>(), columnTracer);
        }

        public void addColumnReference(String colName, Expression expression, Optional<Relation> from) {
            DereferenceExpression deref = null;
            if (expression instanceof DereferenceExpression) {
                deref = (DereferenceExpression) expression;
                addColumnReference(new ColName(deref.getField().getValue(), colName), deref.getBase(), from);
                return;
            }
            SearchedCaseExpression caseExp = null;
            if (expression instanceof SearchedCaseExpression) {
                // CASE WHEN (cc.is_enterprise_card > 0) OR (p.is_enterprise_card_pay >0) THEN 1
                // ELSE 0 END AS is_enterprise_card
                caseExp = (SearchedCaseExpression) expression;
                ColName c = new ColName(colName);
                DereferenceVisitor dereferenceVisitor = new DereferenceVisitor(getAliasTable(from.get()));
                caseExp.accept(dereferenceVisitor, null);
                List<ValueOperator> params = Lists.newArrayList();
                // this.columnTracer.put(c, valueOperator);
                return;
            }
            throw new IllegalStateException("colName:" + colName + "," + expression.toString() + "\n" + expression.getClass());
        // System.out.println("<<<<<<<<<<<" + colName + ":" + expression + ",type:" +
        // expression.getClass());
        }

        public void addColumnReference(ColName colName, Expression expression, Optional<Relation> from) {
            if (expression instanceof Identifier) {
                // expression, from.get()));
                return;
            }
            throw new IllegalStateException(expression.toString() + "\n" + expression.getClass());
        // System.out.println("<<<<<<<<<<<" + colName + ":" + expression + ",type:" +
        // expression.getClass());
        }

        // public void addColumnReference(String colName, Optional<Relation> from) {
        // // System.out.println("<<<<<<<<<<<" + colName + ":" + refTab);
        //
        // this.columnTracer.put(new ColName(colName), getFromTableName(null,
        // from.get()));
        // }
        /**
         * @param outputName
         * @param id
         *            table 别名
         * @param rel
         *            from 的表
         * @return
         */
        private ValueOperator getFromTableName(ColName outputName, Identifier id, Relation rel) {
            return null;
        // if (rel instanceof AliasedRelation) {
        // AliasedRelation aliasRelation = (AliasedRelation) rel;
        // if (id != null && aliasRelation.getAlias().equals(id)) {
        // return getFromTableName(outputName, id, aliasRelation.getRelation());
        // }
        //
        // if (id == null) {
        // return getFromTableName(outputName, null, aliasRelation.getRelation());
        // }
        // }
        //
        // if (rel instanceof Table) {
        // Table t = (Table) rel;
        //
        // return ValueOperator.createTableValueOperator(outputName, t);
        //
        // // return String.valueOf(t.getName());
        // }
        // Join join = null;
        // if (rel instanceof Join) {
        // join = (Join) rel;
        //
        // Map<String /* alias */, Table/* tablename */> aliasMap = getAliasTable(join);
        //
        // Table t = aliasMap.get(id.getValue());
        // if (t == null) {
        // throw new IllegalStateException("alias:" + id.getValue() + ","
        // + Joiner.on(",").join(aliasMap.keySet()) + "\n can not find in " + rel);
        // }
        // return ValueOperator.createTableValueOperator(outputName, t);
        // }
        //
        // throw new IllegalStateException("rel is not valid:\n" + String.valueOf(rel));
        }

        private Map<String, /* alias */
        Table> getAliasTable(Relation rel) {
            if (rel instanceof AliasedRelation) {
                AliasedRelation aliasRelation = (AliasedRelation) rel;
                return getAliasTable(aliasRelation.getRelation());
            }
            if (rel instanceof Table) {
                Table t = (Table) rel;
            // return
            // return String.valueOf(t.getName());
            }
            if (rel instanceof TableSubquery) {
                TableSubquery t = (TableSubquery) rel;
            // return
            // return String.valueOf(t.getName());
            }
            Join join = null;
            if (rel instanceof Join) {
                join = (Join) rel;
                JoinVisitor visitor = new JoinVisitor();
                join.accept(visitor, null);
                return visitor.aliasMap;
            }
            // throw new IllegalStateException("rel is not valid:\n" + String.valueOf(rel));
            return Collections.emptyMap();
        }
    }

    private static class DereferenceVisitor extends DefaultTraversalVisitor<Node, Void> {

        private final Map<String, Table> /* tablename */
        fieldBaseRef = Maps.newHashMap();

        private Map<String, Table> /* tablename */
        aliasTableMap;

        public DereferenceVisitor(Map<String, /* alias */
        Table> aliasTableMap) {
            this.aliasTableMap = aliasTableMap;
        }

        @Override
        protected Node visitDereferenceExpression(DereferenceExpression node, Void context) {
            if (node.getBase() instanceof Identifier) {
                Identifier id = (Identifier) node.getBase();
                Table t = aliasTableMap.get(id.getValue());
                if (t == null) {
                    throw new IllegalStateException("id:" + id.getValue() + " can not find relevant table");
                }
                fieldBaseRef.put(node.getField().getValue(), t);
            }
            return super.visitDereferenceExpression(node, context);
        }
    }

    private static class JoinVisitor extends DefaultTraversalVisitor<Node, Void> {

        private Map<String, Table> /* tablename */
        aliasMap = Maps.newHashMap();

        @Override
        protected Node visitAliasedRelation(AliasedRelation node, Void context) {
            // node.getAlias().getValue());
            if (node.getRelation() instanceof Table) {
                aliasMap.put(node.getAlias().getValue(), (Table) node.getRelation());
                return null;
            }
            if (node.getRelation() instanceof Table) {
                aliasMap.put(node.getAlias().getValue(), (Table) node.getRelation());
                return null;
            }
            // return null;
            return super.visitAliasedRelation(node, context);
        }
    }

    /**
     * @param rel
     * @param rewriter
     * @param stack
     *            调用栈之间需要传递一些参数
     */
    private void processRelation(Relation rel, Rewriter rewriter) {
        Table tab = null;
        Join join = null;
        JoinCriteria jc = null;
        if (rel instanceof Join) {
            join = ((Join) rel);
            processRelation(join.getLeft(), rewriter.deeper());
            rewriter.newline();
            final Type t = join.getType();
            switch(t) {
                case LEFT:
                    rewriter.append(" LEFT JOIN ");
                    break;
                case RIGHT:
                    rewriter.append(" RIGHT JOIN ");
                    break;
                case INNER:
                    rewriter.append(" INNER JOIN ");
                    break;
                default:
                    throw new IllegalStateException("join type " + t + " is not allow " + rel.getLocation().get());
            }
            // System.out.println(createSpace(layer) + join.getType());
            processRelation(join.getRight(), rewriter.deeper());
            if (join.getCriteria().isPresent()) {
                jc = join.getCriteria().get();
                if (jc instanceof JoinOn) {
                    rewriter.append(" ON (");
                    Assert.assertEquals(1, jc.getNodes().size());
                    for (Node n : jc.getNodes()) {
                        rewriter.append(n);
                    }
                    popAiasedTableRelation(rewriter);
                    rewriter.append(")");
                }
            } else {
                Assert.fail("invalid type:" + jc);
            }
            return;
        }
        AliasedRelation aliasRelation = null;
        if (rel instanceof AliasedRelation) {
            aliasRelation = (AliasedRelation) rel;
            if (aliasRelation.getRelation() instanceof Table) {
                tab = (Table) aliasRelation.getRelation();
                rewriter.append(tab.getName());
                rewriter.stack.push(aliasRelation);
            } else {
                processRelation(aliasRelation.getRelation(), rewriter.deeper());
            }
            rewriter.append(" AS ").append(aliasRelation.getAlias().getValue());
            return;
        }
        TableSubquery subQuery = null;
        if (rel instanceof TableSubquery) {
            subQuery = (TableSubquery) rel;
            rewriter.newline().append("(");
            process(subQuery.getQuery(), rewriter);
            rewriter.newline().append(")");
            return;
        }
        if (rel instanceof Table) {
            Table t = (Table) rel;
            rewriter.append(t.getName());
            return;
        }
        throw new IllegalStateException("illegal:" + rel.getClass() + ",rel:" + rel + ",loc:colnum:" + rel.getLocation().get().getColumnNumber() + ",line:" + rel.getLocation().get().getLineNumber());
    }
    // private static String createSpace(int layer) {
    // StringBuffer buffer = new StringBuffer();
    // for (int i = 0; i < layer; i++) {
    // buffer.append(" ");
    // }
    // return buffer.toString();
    // }
}
