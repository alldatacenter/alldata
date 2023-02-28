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
package com.qlangtech.tis.sql.parser.visitor;

import com.facebook.presto.sql.tree.*;
import com.facebook.presto.sql.tree.ArithmeticBinaryExpression.Operator;
import com.google.common.collect.ImmutableSet;
import com.qlangtech.tis.sql.parser.ColName;
import com.qlangtech.tis.sql.parser.IDumpNodeMapContext;
import com.qlangtech.tis.sql.parser.NodeProcessResult;
import com.qlangtech.tis.sql.parser.TisGroupBy;
import com.qlangtech.tis.sql.parser.TisGroupBy.TisGroup;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.ColRef;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.FunctionDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import com.qlangtech.tis.sql.parser.utils.NodeUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class StreamTransformVisitor extends TISStackableAstVisitor<NodeProcessResult<?>, Integer> {

    private ColRef colsRef;

    private final IDumpNodeMapContext dumpNodesContext;

    public StreamTransformVisitor(IDumpNodeMapContext dumpNodesContext) {
        this.dumpNodesContext = dumpNodesContext;
    }

    // private Optional<TisGroupBy> groupBy = Optional.empty();
    public ColRef getColsRef() {
        if (this.colsRef == null) {
            throw new NullPointerException("colsRef can not be null");
        }
        return this.colsRef;
    }

    @Override
    public NodeProcessResult<?> process(Node node, StackableAstVisitorContext<Integer> context) {
        return super.process(node, context);
    }

    // public Optional<TisGroupBy> getGroupBy() {
    // return this.groupBy;
    // }
    protected NodeProcessResult<?> visitQuery(Query node, StackableAstVisitorContext<Integer> context) {
        if (node.getWith().isPresent()) {
            throw new IllegalStateException("with is not support," + node.getWith().get());
        }
        processRelation(node.getQueryBody(), context);
        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }
        if (node.getLimit().isPresent()) {
        // append(indent, "LIMIT " + node.getLimit().get())
        // .append('\n');
        }
        return null;
    }

    protected NodeProcessResult<?> visitAliasedRelation(AliasedRelation node, StackableAstVisitorContext<Integer> context) {
        this.process(node.getRelation(), context);
        if (context.getPreviousNode().isPresent()) {
        // System.out.println("getPreviousNode:" + context.getPreviousNode().get());
        }
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitGroupBy(GroupBy node, StackableAstVisitorContext<Integer> context) {
        // System.out.println(
        // "GROUP BY " + (node.isDistinct() ? " DISTINCT " : "") +
        // formatGroupBy(node.getGroupingElements()));
        TisGroupBy groupBy = new TisGroupBy();
        for (GroupingElement groupingElement : node.getGroupingElements()) {
            if (groupingElement instanceof SimpleGroupBy) {
                Set<Expression> columns = ImmutableSet.copyOf(((SimpleGroupBy) groupingElement).getColumnExpressions());
                for (Expression g : columns) {
                    if (g instanceof DereferenceExpression) {
                        DereferenceExpression group = (DereferenceExpression) g;
                        if (!(group.getBase() instanceof Identifier)) {
                            faild(group.getBase());
                        }
                        groupBy.add(new TisGroup(((Identifier) group.getBase()).getValue(), group.getField().getValue()));
                    } else {
                        faild("groupby cols shall col must has tab reference", g);
                    }
                }
            // this.groupBy = Optional.of(groupBy);
            } else {
                faild(node);
            }
        // else if (groupingElement instanceof GroupingSets) {
        // result = format("GROUPING SETS (%s)", Joiner.on(", ").join(
        // ((GroupingSets) groupingElement).getSets().stream()
        // .map(ExpressionFormatter::formatGroupingSet)
        // .iterator()));
        // }
        // else if (groupingElement instanceof Cube) {
        // result = format("CUBE %s", formatGroupingSet(((Cube)
        // groupingElement).getColumns()));
        // }
        // else if (groupingElement instanceof Rollup) {
        // result = format("ROLLUP %s", formatGroupingSet(((Rollup)
        // groupingElement).getColumns()));
        // }
        // resultStrings.add(result);
        }
        if (groupBy.getGroups().size() > 0) {
            this.colsRef.getColRefMap().entrySet().forEach((r) -> {
                if (r.getValue() instanceof FunctionDataTupleCreator) {
                    ((FunctionDataTupleCreator) r.getValue()).setGroupBy(groupBy);
                }
            // System.out.println(r.getKey() + ":" + r.getValue().hashCode());
            });
        }
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitFunctionCall(FunctionCall expression, StackableAstVisitorContext<Integer> context) {
        // "concat_ws"(',', "collect_set"("split"(i.batch_msg, '[\\w\\W]*\\|')[1]))
        QualifiedName name = expression.getName();
        // rewriter.append(name.toString()).append("(");
        List<Expression> exps = expression.getArguments();
        int argsize = exps.size();
        int index = 0;
        for (Expression arg : exps) {
            this.process(arg, context);
        // processExpression(arg, rewriter);
        // if (arg instanceof FunctionCall) {
        // process((FunctionCall) arg, rewriter);
        // } else {
        // rewriter.append(arg);
        // }
        // if (index++ < argsize - 1) {
        // rewriter.append(",");
        // }
        }
        return null;
    // return super.visitFunctionCall(node, context);
    }

    @Override
    protected NodeProcessResult<?> visitTableSubquery(TableSubquery node, StackableAstVisitorContext<Integer> context) {
        this.process(node.getQuery(), context);
        // return super.visitTableSubquery(node, context);
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitLongLiteral(LongLiteral node, StackableAstVisitorContext<Integer> context) {
        return null;
    // return super.visitLongLiteral(node, context);
    }

    @Override
    protected NodeProcessResult<?> visitCast(Cast node, StackableAstVisitorContext<Integer> context) {
        // System.out.println("type:" + node.getType());
        this.process(node.getExpression(), context);
        // return super.visitCast(node, context);
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitStringLiteral(StringLiteral node, StackableAstVisitorContext<Integer> context) {
        // System.out.println("Slice:" + node.getSlice() + ",val:" + node.getValue());
        return null;
    // return super.visitStringLiteral(node, context);
    }

    @Override
    protected NodeProcessResult<?> visitIdentifier(Identifier node, StackableAstVisitorContext<Integer> context) {
        return null;
    // return super.visitIdentifier(node, context);
    }

    @Override
    protected NodeProcessResult<?> visitDereferenceExpression(DereferenceExpression node, StackableAstVisitorContext<Integer> context) {
        this.process(node.getBase(), context);
        // System.out.println(node.getField().getValue());
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitComparisonExpression(ComparisonExpression node, StackableAstVisitorContext<Integer> context) {
        this.process(node.getLeft(), context);
        // System.out.println(node.getOperator() + "," + node.getOperator().getValue());
        this.process(node.getRight(), context);
        return null;
    // return super.visitComparisonExpression(node, context);
    }

    @Override
    protected NodeProcessResult<?> visitLogicalBinaryExpression(LogicalBinaryExpression node, StackableAstVisitorContext<Integer> context) {
        // System.out.println("operator:" + node.getOperator());
        this.process(node.getLeft(), context);
        this.process(node.getRight(), context);
        return null;
    // return super.visitLogicalBinaryExpression(node, context);
    }

    @Override
    protected NodeProcessResult<?> visitTable(Table node, StackableAstVisitorContext<Integer> context) {
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitSearchedCaseExpression(SearchedCaseExpression node, StackableAstVisitorContext<Integer> context) {
        processSearchedCaseExpression(node, context);
        return null;
    }

    private void processSearchedCaseExpression(SearchedCaseExpression expression, StackableAstVisitorContext<Integer> context) {
        for (WhenClause when : expression.getWhenClauses()) {
            // rewriter.append("WHEN ");
            // processExpression(when.getOperand(), rewriter);
            this.process(when.getOperand(), context);
            // rewriter.append(when.getOperand().getClass());
            // rewriter.append(" THEN ").append(when.getResult());
            this.process(when.getResult(), context);
        }
        Optional<Expression> dft = expression.getDefaultValue();
        if (dft.isPresent()) {
            // rewriter.append(" ELSE ").append(dft.get());
            this.process(dft.get(), context);
        }
    // rewriter.append(" END");
    }

    @Override
    protected NodeProcessResult<?> visitCoalesceExpression(CoalesceExpression expression, StackableAstVisitorContext<Integer> context) {
        // int index = 0;
        for (Expression operand : expression.getOperands()) {
            // rewriter.append(operand);
            // processExpression(operand, rewriter);
            this.process(operand, context);
        // if (index++ < operandSize - 1) {
        // rewriter.append(",");
        // }
        }
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitArithmeticBinary(ArithmeticBinaryExpression arithmeticBinaryExpression, StackableAstVisitorContext<Integer> context) {
        // ArithmeticBinaryExpression arithmeticBinaryExpression =
        // (ArithmeticBinaryExpression) expression;
        // rewriter.append("(");
        process(arithmeticBinaryExpression.getLeft(), context);
        Operator operator = arithmeticBinaryExpression.getOperator();
        // System.out.println("operator:" + operator.getValue());
        // rewriter.append(operator.getValue());
        process(arithmeticBinaryExpression.getRight(), context);
        return null;
    // return super.visitArithmeticBinary(node, context);
    }

    @Override
    protected NodeProcessResult<?> visitSubscriptExpression(SubscriptExpression subscript, StackableAstVisitorContext<Integer> context) {
        // SubscriptExpression subscript = (SubscriptExpression) expression;
        process(subscript.getBase(), context);
        // rewriter.append("[");
        process(subscript.getIndex(), context);
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitSelect(Select node, StackableAstVisitorContext<Integer> context) {
        context.processSelect = true;
        try {
            SingleColumn single = null;
            Expression express = null;
            DereferenceExpression dref = null;
            ColName colName = null;
            ColRef colRef = new ColRef();
            // Map<ColName /* colName */, IDataTupleCreator> colRefMap = Maps.newHashMap();
            // Map<String/* ref */, IDataTupleCreator> baseRefMap = Maps.newHashMap();
            NodeProcessResult<ColRef> result = new NodeProcessResult<ColRef>(colRef);
            IDataTupleCreator tupleCreator = null;
            for (SelectItem item : node.getSelectItems()) {
                if (item instanceof SingleColumn) {
                    single = (SingleColumn) item;
                    express = single.getExpression();
                    if (express instanceof DereferenceExpression) {
                        dref = ((DereferenceExpression) express);
                        if (dref.getBase() instanceof Identifier) {
                            if (single.getAlias().isPresent()) {
                                colName = new ColName(dref.getField().getValue(), single.getAlias().get().getValue());
                            } else {
                                colName = new ColName(dref.getField().getValue());
                            }
                            tupleCreator = createTableTupleCreator(dref, colRef);
                            colRef.getColRefMap().put(colName, tupleCreator);
                        } else {
                            // this.process(dref.getBase(), context);
                            faild(dref.getBase());
                        }
                    } else {
                        if (single.getAlias().isPresent()) {
                            String name = single.getAlias().get().getValue();
                            colName = new ColName(name);
                            if (express instanceof SearchedCaseExpression) {
                                colRef.getColRefMap().put(colName, new FunctionDataTupleCreator(express, colRef));
                                continue;
                            } else if (express instanceof CoalesceExpression) {
                                // COALESCE(a2.has_fetch, 0)
                                colRef.getColRefMap().put(colName, new FunctionDataTupleCreator(express, colRef));
                                continue;
                            } else if (express instanceof FunctionCall) {
                                // "concat_ws"(',', "collect_set"("split"(i.batch_msg, '[\\w\\W]*\\|')[1]))
                                colRef.getColRefMap().put(colName, new FunctionDataTupleCreator(express, colRef));
                                continue;
                            } else if (express instanceof SubscriptExpression) {
                                // "split"(i.batch_msg, '[\\w\\W]*\\|')[1]
                                colRef.getColRefMap().put(colName, new FunctionDataTupleCreator(express, colRef));
                                continue;
                            } else if (express instanceof Identifier) {
                                processIdentifier((Identifier) express, colRef);
                                continue;
                            } else if (express instanceof StringLiteral) {
                                /**
                                 *                                 select 中存在以下使用常量作为列字段
                                 *                                 SELECT
                                 *                                 '' as num_unit_id,
                                 *                                 '' as num_unit_name
                                 *                                 FROM goods
                                 */
                                colRef.getColRefMap().put(colName, new FunctionDataTupleCreator(express, colRef));
                                continue;
                            } else if (express instanceof IfExpression) {
                                /**
                                 * IF((COALESCE("instr"(sl.logo_url, 'http'), 0) > 0), sl.logo_url, "concat"('http://', sl.logo_url))
                                 */
                                colRef.getColRefMap().put(colName, new FunctionDataTupleCreator(express, colRef));
                                continue;
                            }
                            faild(express);
                        } else if (express instanceof Identifier) {
                            // 没有设置表应用，select里面的表应该只有一个
                            // 这种情况下from中只有一个原表
                            processIdentifier((Identifier) express, colRef);
                            continue;
                        }
                        faild(express);
                    // process(express, context);
                    // IDataTupleCreator tupleCreator = new IDataTupleCreator() {
                    //
                    // @Override
                    // public Object getVal(String name) {
                    // return null;
                    // }
                    // };
                    //
                    // ValueOperator.createGatherValueOperator(new
                    // ColName(single.getAlias().get().getValue()),
                    // express, params);
                    }
                // single.getAlias();
                } else {
                    throw new IllegalStateException("item type:" + item.getClass() + "" + item);
                }
            // visitSelectItem(item, context);
            }
            // System.out.println(MoreObjects.toStringHelper(colRefMap).add);
            return result;
        // return new NodeProcessResult<Map<ColName /* colName */, String /* base */
        // >>(colRefMap);
        } finally {
            context.processSelect = false;
        }
    // return super.visitSelect(node, context);
    }

    private void processIdentifier(Identifier express, ColRef colRef) {
        ColName colName;
        colName = new ColName(express.getValue());
        colRef.getColRefMap().put(colName, createTableTupleCreator(null, colRef));
    }

    public static final String DEFAULT_SINGLE_TABLE = "default_single_table";

    public static IDataTupleCreator createTableTupleCreator(DereferenceExpression dref, ColRef colRef) {
        // IDataTupleCreator tupleCreator;
        String baseRef = null;
        if (dref != null) {
            baseRef = ((Identifier) dref.getBase()).getValue();
        } else {
            baseRef = DEFAULT_SINGLE_TABLE;
        }
        // }
        return colRef.createBaseRefIfNull(baseRef);
    // return tupleCreator;
    }

    public static void faild(Node node) {
        // NodeLocation loc = node.getLocation().get();
        //
        // throw new IllegalStateException(node + ",type:" + node.getClass() + ",[line:"
        // + loc.getLineNumber() + ",col:"
        // + loc.getColumnNumber() + "]");
        NodeUtils.faild(null, node);
    }

    public static void faild(String msg, Node node) {
        // NodeLocation loc = node.getLocation().get();
        //
        // throw new IllegalStateException(node + ",type:" + node.getClass() + ",[line:"
        // + loc.getLineNumber() + ",col:"
        // + loc.getColumnNumber() + "]");
        NodeUtils.faild(msg, node);
    }

    // @Override
    // protected NodeProcessResult<?> visitSelectItem(SelectItem node,
    // StackableAstVisitorContext<Integer> context) {
    //
    // System.out.println(node);
    //
    // return null;
    // // return super.visitSelectItem(node, context);
    // }
    @SuppressWarnings("all")
    protected NodeProcessResult<?> visitQuerySpecification(QuerySpecification body, StackableAstVisitorContext<Integer> context) {
        // return visitQueryBody(node, context);
        // Select select = ;
        // this.visitSelect(, context);
        //
        NodeProcessResult<ColRef> selectresult = (NodeProcessResult<ColRef>) this.process(body.getSelect(), context);
        if (selectresult == null || selectresult.getResult() == null || selectresult.getResult().getColRefMap().size() < 1) {
            throw new IllegalStateException("selectresult can not be null");
        }
        this.colsRef = selectresult.getResult();
        if (body.getLimit().isPresent()) {
            System.out.println("Limit = " + body.getLimit().get());
        }
        // final List<Expression> groupIds = Lists.newArrayList();
        Optional<GroupBy> gby = body.getGroupBy();
        GroupBy group = null;
        if (gby.isPresent()) {
            group = gby.get();
            this.visitGroupBy(group, context);
        }
        Optional<Relation> from = body.getFrom();
        // /////////////////////////////////////////////////////////////////////////
        // System.out.println("======================================================");
        // List<SelectItem>
        // SingleColumn single = null;
        Relation rel = null;
        // Table table = null;
        if (from.isPresent()) {
            rel = from.get();
            // rewriter.newline().append("FROM ");
            // processRelation(rel, rewriter);
            processFromRemoveAlias(colsRef, rel, context);
        // this.processRelation(rel, context);
        } else {
            throw new IllegalStateException("have not set from \n" + body);
        }
        // colsRef.colRefMap.entrySet().stream()
        // .forEach((r) -> System.out.println("[" + r.getKey() + "]," +
        // r.getValue().toString()));
        Optional<Expression> w = body.getWhere();
        Expression where = null;
        if (w.isPresent()) {
            where = w.get();
            process(where, context);
        // rewriter.newline().append("WHERE ");
        // rewriter.append(table);
        // processExpression(where, rewriter);
        } else {
        // if (table != null) {
        // rewriter.newline().append("WHERE ");
        // }
        }
        return null;
    }

    private void processFromRemoveAlias(ColRef colRef, Relation from, StackableAstVisitorContext<Integer> context) {
        TableReferenceVisitor tabRefVisitor = new TableReferenceVisitor(colRef, this.dumpNodesContext);
        tabRefVisitor.process(from, context);
    }

    @Override
    protected NodeProcessResult<?> visitJoin(Join node, StackableAstVisitorContext<Integer> context) {
        JoinCriteria criteria = node.getCriteria().orElse(null);
        String type = node.getType().toString();
        if (criteria instanceof NaturalJoin) {
            type = "NATURAL " + type;
        }
        if (node.getType() != Join.Type.IMPLICIT) {
        // builder.append('(');
        }
        process(node.getLeft(), context);
        // builder.append('\n');
        if (node.getType() == Join.Type.IMPLICIT) {
        // append(indent, ", ");
        } else {
        // append(indent, type).append(" JOIN ");
        }
        process(node.getRight(), context);
        if (node.getType() != Join.Type.CROSS && node.getType() != Join.Type.IMPLICIT) {
            if (criteria instanceof JoinUsing) {
                JoinUsing using = (JoinUsing) criteria;
            // builder.append(" USING (").append(Joiner.on(",
            // ").join(using.getColumns())).append(")");
            } else if (criteria instanceof JoinOn) {
                JoinOn on = (JoinOn) criteria;
            // builder.append(" ON ").append(formatExpression(on.getExpression(),
            // parameters));
            } else if (!(criteria instanceof NaturalJoin)) {
                throw new UnsupportedOperationException("unknown join criteria: " + criteria);
            }
        }
        if (node.getType() != Join.Type.IMPLICIT) {
        // builder.append(")");
        }
        return null;
    }

    private void processRelation(Relation relation, StackableAstVisitorContext<Integer> indent) {
        // TODO: handle this properly
        if (relation instanceof Table) {
        // builder.append("TABLE ").append(((Table) relation).getName()).append('\n');
        } else {
            process(relation, indent);
        }
    }

    @Override
    protected NodeProcessResult<?> visitIsNotNullPredicate(IsNotNullPredicate node, StackableAstVisitorContext<Integer> context) {
        // 当在WHERE条件中出现 Where的 约束条件时候：Where g.entity_id IS NOT NULL
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitIsNullPredicate(IsNullPredicate node, StackableAstVisitorContext<Integer> context) {
        // 当在WHERE条件中出现 Where的 约束条件时候：Where g.entity_id IS  NULL
        return null;
    }

    protected NodeProcessResult<?> visitNode(Node node, StackableAstVisitorContext<Integer> context) {
        throw new UnsupportedOperationException(String.valueOf(node));
    }
}
