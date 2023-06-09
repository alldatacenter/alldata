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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.realtime.transfer.ruledriven.FunctionUtils;
import com.qlangtech.tis.realtime.transfer.ruledriven.FunctionUtils.Case;
import com.qlangtech.tis.realtime.transfer.ruledriven.FunctionUtils.IDoubleValGetter;
import com.qlangtech.tis.realtime.transfer.ruledriven.FunctionUtils.IIntValGetter;
import com.qlangtech.tis.realtime.transfer.ruledriven.FunctionUtils.IValGetter;
import com.qlangtech.tis.realtime.transfer.ruledriven.GroupValues;
import com.qlangtech.tis.realtime.transfer.ruledriven.TypeCast;
import com.qlangtech.tis.sql.parser.ColName;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.ColRef;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.FunctionDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.IScriptGenerateContext;
import com.qlangtech.tis.sql.parser.utils.NodeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 识别 sql中的方法體
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年5月14日
 */
public class FunctionVisitor extends DefaultTraversalVisitor<Void, Void> {

    public static final String ROW_KEY = "row";

    public static final String SubscriptFunctionName = "getArrayIndexProp";

    public static final String FUNCTION_OP_AND = "op_and";

    private final ColRef colRef;

    protected final Map<ColName, IDataTupleCreator> functionParams;

    protected final IScriptGenerateContext generateContext;

    // 是否在聚合函数调用栈中
    private final Stack<TISUdfMeta> inAggregateFunctionStack = new Stack<>();

    private final Stack<OperatorResultTypeEnum> operatorResultTypeStack = new Stack<>();

    // ======================================================================
    private static final Map<String, TISUdfMeta> /* funcName */
            funcMeta;

    private final boolean processAggregationResult;

    static {
        funcMeta = Maps.newHashMap();
        // funcMeta.put("regexp", new TISUdfMeta(false, FunctionUtils.getMethod("regexp", String.class, String.class)));
        // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF
        funcMeta.put("rlike", new TISUdfMeta(false, FunctionUtils.getMethod("rlike", String.class, String.class)));
        funcMeta.put("min", new TISUdfMeta(true, FunctionUtils.getMethod("min", GroupValues.class, IValGetter.class)));
        funcMeta.put("max", new TISUdfMeta(true, FunctionUtils.getMethod("max", GroupValues.class, IValGetter.class)));
        funcMeta.put("split", new TISUdfMeta(false, FunctionUtils.getMethod("split", String.class, String.class)));
        // concat_ws(String separator, Object... objs)
        funcMeta.put("concat_ws", new TISUdfMeta(false, FunctionUtils.getMethod("concat_ws", String.class, Object[].class)));
        // round(double input, int scale)
        funcMeta.put("round", new TISUdfMeta(false, FunctionUtils.getMethod("round", double.class, int.class)));
        // GroupValues groupValues, IValGetter valGetter
        funcMeta.put("collect_list", new TISUdfMeta(true, FunctionUtils.getMethod("collect_list", GroupValues.class, IValGetter.class)));
        // (GroupValues groupValues, IValGetter valGetter)
        funcMeta.put("collect_set", new TISUdfMeta(true, FunctionUtils.getMethod("collect_set", GroupValues.class, IValGetter.class)));
        // GroupValues groupVales, IIntValGetter getter
        funcMeta.put("count", new TISUdfMeta(true, FunctionUtils.getMethod("count", GroupValues.class, IIntValGetter.class)));
        funcMeta.put(FUNCTION_OP_AND, new TISUdfMeta(false, FunctionUtils.getMethod("op_and", int.class, int.class)));
        // GroupValues datas, IDoubleValGetter valGetter
        funcMeta.put("sum", new TISUdfMeta(true, /* aggregateFunc */
                true, /* numeric */
                FunctionUtils.getMethod("sum", GroupValues.class, IDoubleValGetter.class)));
        funcMeta.put("get_json_object", new TISUdfMeta(false, FunctionUtils.getMethod("get_json_object", String.class, String.class)));
        // caseIfFunc(Object defaultVal, Case... cases)
        funcMeta.put("coalesce", new TISUdfMeta(false, FunctionUtils.getMethod("caseIfFunc", Object.class, Case[].class)));
        // Returns the position of the first occurrence of substr in str. Returns null if either of the arguments are null and returns 0 if substr could not be found in str. Be aware that this is not zero based. The first character in str has index 1.
        funcMeta.put("instr", new TISUdfMeta(false, FunctionUtils.getMethod("instr", String.class, String.class)));
        funcMeta.put("concat", new TISUdfMeta(false, FunctionUtils.getMethod("concat", String.class, String.class)));
    }

    public FunctionVisitor(ColRef colRef, Map<ColName, IDataTupleCreator> params, FuncFormat funcFormat, boolean processAggregationResult) {
        this(colRef, params, funcFormat, new MockScriptGenerateContext(), processAggregationResult);
    }

    /**
     * @param colRef
     * @param params
     * @param funcFormat
     * @param generateContext
     * @param processAggregationResult 当前是否在处理聚合结果集的流程
     *                                 <p>
     *                                 for((k:GroupKey, v:GroupValues) <- instancedetails){
     *                                 // 在处理聚合结果的上下文中
     *                                 // processAggregationResult =  true 其他情况都为false
     *                                 }
     *                                 </p>
     */
    public FunctionVisitor(ColRef colRef, Map<ColName, IDataTupleCreator> params, BlockScriptBuffer funcFormat, IScriptGenerateContext generateContext, boolean processAggregationResult) {
        super();
        // this.functionDataTupleCreator = functionDataTupleCreator;
        this.funcFormat = funcFormat;
        this.colRef = colRef;
        this.functionParams = params;
        this.generateContext = generateContext;
        this.processAggregationResult = processAggregationResult;
    }

    @Override
    protected Void visitLogicalBinaryExpression(LogicalBinaryExpression node, Void context) {
        process(node.getLeft(), context);
        if (node.getOperator() == LogicalBinaryExpression.Operator.AND) {
            this.funcFormat.append(" && ");
        } else if (node.getOperator() == LogicalBinaryExpression.Operator.OR) {
            this.funcFormat.append(" || ");
        } else {
            // throw new IllegalStateException();
            NodeUtils.faild("visitLogicalBinaryExpression", node);
        }
        process(node.getRight(), context);
        return null;
    }

    @Override
    protected Void visitSubscriptExpression(SubscriptExpression node, Void context) {
        // example: "split"(i.batch_msg, '[\\w\\W]*\\|')[1]
        this.funcFormat.append(SubscriptFunctionName).append("(");
        this.process(node.getBase(), context);
        this.funcFormat.append(",");
        this.process(node.getIndex(), context);
        this.funcFormat.append(")");
        // return super.visitSubscriptExpression(node, context);
        return null;
    }

    public static void main(String[] args) throws Exception {
        InputStream input = FunctionVisitor.class.getResourceAsStream("/function/func.txt");
        LineIterator lineIt = IOUtils.lineIterator(input, Charset.defaultCharset());
        Set<String> func = Sets.newHashSet();
        while (lineIt.hasNext()) {
            func.add(StringUtils.substringAfter(lineIt.next(), ":"));
        }
        func.forEach((e) -> {
            System.out.println(e);
        });
    }

    @Override
    protected Void visitComparisonExpression(ComparisonExpression node, Void context) {
        this.operatorResultTypeStack.push(OperatorResultTypeEnum.INT);
        try {
            this.funcFormat.append("(");
            process(node.getLeft(), context);
            this.funcFormat.append(operatorLiteria(node.getOperator()));
            if (isEqualOrNotOperator(node.getOperator())) {
                this.funcFormat.append(" String.valueOf(");
            }
            process(node.getRight(), context);
            this.funcFormat.append(")");
            if (isEqualOrNotOperator(node.getOperator())) {
                this.funcFormat.append(")");
            }
        } finally {
            this.operatorResultTypeStack.pop();
        }
        return null;
    }

    private boolean isEqualOrNotOperator(ComparisonExpression.Operator operator) {
        switch (operator) {
            case EQUAL:
            case NOT_EQUAL:
                return true;
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL:
                return false;
            // return IS_DISTINCT_FROM;
            default:
                throw new IllegalArgumentException("Unsupported comparison: " + operator);
        }
    }

    private String operatorLiteria(ComparisonExpression.Operator operator) {
        switch (operator) {
            case EQUAL:
                return "==";
            case NOT_EQUAL:
                return "!=";
            case LESS_THAN:
                return "<";
            case LESS_THAN_OR_EQUAL:
                return "<=";
            case GREATER_THAN:
                return ">";
            case GREATER_THAN_OR_EQUAL:
                return ">=";
            // return IS_DISTINCT_FROM;
            default:
                throw new IllegalArgumentException("Unsupported comparison: " + operator);
        }
    }

    protected Void visitNode(Node node, Void context) {
        // throw new UnsupportedOperationException();
        // NodeUtils.faild("not supported", node);
        System.out.println("notsupported:" + node.getClass());
        return null;
    }

    @Override
    protected Void visitArithmeticBinary(ArithmeticBinaryExpression node, Void context) {
        this.funcFormat.append("(");
        process(node.getLeft(), context);
        this.funcFormat.append(node.getOperator().getValue());
        process(node.getRight(), context);
        this.funcFormat.append(")");
        return null;
    }

    @Override
    protected Void visitDereferenceExpression(DereferenceExpression node, Void context) {
        ColName col = new ColName(node.getField().getValue());
        IDataTupleCreator tupleCreator = StreamTransformVisitor.createTableTupleCreator(node, this.colRef);
        this.functionParams.put(col, tupleCreator);
        boolean isAggregateNumerica = this.isAggregateNumerica();
        boolean isAggregateFunc = this.isAggregateFunc();
        // =============================================
        if (isAggregateNumerica) {
            // this.funcFormat.append("Double.parseDouble(");
        }
        if (generateContext.isJoinPoint()) {
            this.processJoinPointPram(col);
            if (!this.operatorResultTypeStack.isEmpty()) {
                OperatorResultTypeEnum opResultTypeEnum = this.operatorResultTypeStack.peek();
                // TODO: 暂时先注释掉
                // this.funcFormat.append("(").append(opResultTypeEnum.getLiteria()).append(")");
            }
            this.funcFormat.append("getMediaResult(\"" + col.getName() + "\",row)");
        } else {
            if (generateContext.isNextGroupByFunction() && !generateContext.isGroupByFunction()) {
                this.funcFormat.startLine("v.getMediaProp(\"" + col.getName() + "\")");
            } else if (this.processAggregationResult && !isAggregateFunc) {
                this.funcFormat.append("k.getKeyVal(\"").append(col.getName()).append("\")");
            } else {
                this.funcFormat.append(isAggregateFunc ? "r" : ROW_KEY);
                this.funcFormat.append(".getColumn(\"").append(col.getName()).append("\")");
            }
        }
        if (isAggregateNumerica) {
            // this.funcFormat.append(")");
        }
        // =============================================
        return null;
    }

    protected void processJoinPointPram(ColName param) {
    }

    private boolean isAggregateNumerica() {
        if (this.inAggregateFunctionStack.isEmpty()) {
            return false;
        }
        TISUdfMeta udfMeta = this.inAggregateFunctionStack.peek();
        boolean isAggregateNumerica = udfMeta != null && udfMeta.isAggregateFunc() && udfMeta.isNumeric();
        return isAggregateNumerica;
    }

    /**
     * 查看调用栈中是否有聚合函數
     *
     * @return
     */
    private boolean isAggregateFunc() {
        return this.inAggregateFunctionStack.stream().filter((r) -> {
            return r.isAggregateFunc();
        }).findFirst().isPresent();
    }

    @Override
    @SuppressWarnings("all")
    protected Void visitCast(Cast node, Void context) {
        String type = StringUtils.lowerCase(node.getType());
        TypeCast cast = TypeCast.getTypeCast(type);
        this.funcFormat.append("typeCast(\"").append(type).append("\",");
        this.process(node.getExpression(), context);
        this.funcFormat.append(")");
        return null;
        // return super.visitCast(node, context);
    }

    public String getFuncFormat() {
        return this.funcFormat.toString();
    }

    @Override
    protected Void visitStringLiteral(StringLiteral node, Void context) {
        this.funcFormat.append("\"").append(node.getValue()).append("\"");
        return null;
        // return super.visitStringLiteral(node, context);
    }

    @Override
    protected Void visitLongLiteral(LongLiteral node, Void context) {
        this.funcFormat.append(node.getValue());
        // return super.visitLongLiteral(node, context);
        return null;
    }

    // CASE WHEN SUM( OP_AND(i.draw_status,8)) > 0 THEN 1 ELSE 0
    @Override
    protected Void visitFunctionCall(FunctionCall node, Void context) {
        // "concat_ws"(',', "collect_set"("split"(i.batch_msg,
        // '[\\w\\W]*\\|')[1]))
        String functionName = StringUtils.lowerCase(String.valueOf(node.getName()));
        TISUdfMeta fmeta = funcMeta.get(functionName);
        if (fmeta == null) {
            throw new IllegalStateException("func:" + functionName + " have not been define in funcMeta");
        }
        funcFormat.append(functionName).append("(");
        inAggregateFunctionStack.push(fmeta);
        try {
            if (fmeta.isAggregateFunc()) {
                // 如果是聚合函数
                funcFormat.append("v, (r) => { \n");
                if (fmeta.isNumeric()) {
                    // funcFormat.append("(Double)");
                }
                writeParams(node, context);
                funcFormat.append("}");
            } else {
                // 如果是普通非聚合函数
                writeParams(node, context);
            }
        } finally {
            inAggregateFunctionStack.pop();
        }
        funcFormat.append(")");
        return null;
    }

    private void writeParams(FunctionCall node, Void context) {
        List<Expression> arguments = node.getArguments();
        boolean first = true;
        for (Expression arg : arguments) {
            if (!first) {
                funcFormat.append(",");
            }
            first = false;
            this.process(arg, context);
        }
    }

    @Override
    protected Void visitCoalesceExpression(CoalesceExpression node, Void context) {
        // COALESCE(a2.has_fetch, 0)
        // 既是聚合又是數字類型的統計
        final boolean isAggregateNumerica = this.isAggregateNumerica();
        funcFormat.append(isAggregateNumerica ? "defaultDoubleVal" : "defaultVal").append("(");
        this.inAggregateFunctionStack.push(new TISUdfMeta(false, /* aggregateFunc */
                null));
        try {
            boolean first = true;
            for (Expression oper : node.getOperands()) {
                if (!first) {
                    funcFormat.append(",");
                }
                this.process(oper, context);
                first = false;
            }
        } finally {
            this.inAggregateFunctionStack.pop();
        }
        funcFormat.append(")");
        return null;
    }

    @Override
    protected Void visitSearchedCaseExpression(SearchedCaseExpression node, Void context) {
        // (CASE WHEN ("sum"("op_and"(i.draw_status, 8)) > 0) THEN 1 ELSE 0 END)
        // StringBuffer rewriter = new StringBuffer();
        // (CASE WHEN ("sum"("op_and"(i.draw_status, 8)) > 0) THEN 1 ELSE 0 END)
        // rewriter.append("CASE ");
        this.funcFormat.startLine("caseIfFunc(");
        Optional<Expression> dft = node.getDefaultValue();
        if (!dft.isPresent()) {
            NodeUtils.faild("default value shall exist", node);
        }
        // if (dft.isPresent()) {
        // rewriter.append(" ELSE ").append(dft.get());
        this.process(dft.get(), context);
        // boolean first = true;
        for (WhenClause when : node.getWhenClauses()) {
            this.funcFormat.append(",");
            this.funcFormat.startLine("new Case(");
            // this.funcFormat.methodBody(false, "new Callable<Boolean>() ",
            // (rr) -> {
            // rr.methodBody("public Boolean call() throws Exception", (kk) -> {
            // kk.startLine("return ");
            // this.process(when.getOperand(), context);
            // kk.append(";");
            // });
            // });
            // this.funcFormat.methodBody(false, "new Callable<Boolean>() ",
            // (rr) -> {
            // rr.methodBody("public Boolean call() throws Exception", (kk) -> {
            // kk.startLine("return ");
            this.process(when.getOperand(), context);
            // kk.append(";");
            // });
            // });
            this.funcFormat.append(",");
            this.process(when.getResult(), context);
            this.funcFormat.append(")");
        }
        this.funcFormat.append(")");
        return null;
    }

    private final BlockScriptBuffer funcFormat;

    private static class MockScriptGenerateContext implements IScriptGenerateContext {
        @Override
        public IDataTupleCreator getTupleCreator() {
            return null;
        }

        @Override
        public boolean isLastFunctInChain() {
            return false;
        }

        @Override
        public boolean isGroupByFunction() {
            return false;
        }

        @Override
        public boolean isNextGroupByFunction() {
            return false;
        }

        @Override
        public boolean isNotDeriveFrom(EntityName entityName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isJoinPoint() {
            return false;
        }

        @Override
        public EntityName getEntityName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ColName getOutputColName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FunctionDataTupleCreator getFunctionDataTuple() {
            throw new UnsupportedOperationException();
        }
    }
}
