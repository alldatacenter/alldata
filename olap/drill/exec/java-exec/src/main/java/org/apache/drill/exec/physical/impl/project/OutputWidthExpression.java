/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.physical.impl.project;

import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.exec.expr.AbstractExecExprVisitor;
import org.apache.drill.exec.expr.ValueVectorReadExpression;
import org.apache.drill.exec.expr.fn.output.OutputWidthCalculator;

import java.util.ArrayList;

/**
 * OutputWidthExpressions are used to capture the information required to calculate the width of the output
 * produced by a variable-width expression. This is used by the {@link ProjectMemoryManager} to calculate output-widths of the expressions
 * being projected. Expressions in Drill are represented as a tree of {@link org.apache.drill.common.expression.LogicalExpression}.
 * During the setup phase, the {@link OutputWidthVisitor} walks the tree of LogicalExpressions and reduces it to a tree of
 * OutputWidthExpressions. In the execution phase, the OutputWidthVisitor walks the tree of OutputWidthExpressions and
 * reduces it to a fixed output-width by using the average-sizes of incoming columns obtained from the
 * {@link org.apache.drill.exec.record.RecordBatchSizer}
 *
 */
public abstract class OutputWidthExpression {

    abstract <T, V, E extends Exception> T accept(AbstractExecExprVisitor<T, V, E> visitor, V value) throws E;

    /**
     * IfElseWidthExpr is uded to capture an {@link org.apache.drill.common.expression.IfExpression}. The max of the if-side width and
     * else-side width will be used as the expression width.
     */
    public static class IfElseWidthExpr extends OutputWidthExpression {
        OutputWidthExpression[] expressions;

        public IfElseWidthExpr(OutputWidthExpression ifExpr, OutputWidthExpression elseExpr) {
            this.expressions = new OutputWidthExpression[2];
            this.expressions[0] = ifExpr;
            this.expressions[1] = elseExpr;
        }

        @Override
        public <T, V, E extends Exception> T accept(AbstractExecExprVisitor<T, V, E> visitor, V value) throws E {
            return visitor.visitIfElseWidthExpr(this, value);
        }

    }

    /**
     * FunctionCallExpr captures the details required to calculate the width of the output produced by a function
     * that produces variable-width output. It captures the {@link OutputWidthCalculator} for the function and the
     * arguments.
     */
    public static class FunctionCallExpr extends OutputWidthExpression {
        FunctionHolderExpression holder;
        ArrayList<OutputWidthExpression> args;
        OutputWidthCalculator widthCalculator;

        public FunctionCallExpr(FunctionHolderExpression holder, OutputWidthCalculator widthCalculator,
                                ArrayList<OutputWidthExpression> args) {
            this.holder = holder;
            this.args = args;
            this.widthCalculator = widthCalculator;
        }

        public FunctionHolderExpression getHolder() {
            return holder;
        }

        public ArrayList<OutputWidthExpression> getArgs() {
            return args;
        }

        public OutputWidthCalculator getCalculator() {
            return widthCalculator;
        }

        @Override
        public <T, V, E extends Exception> T accept(AbstractExecExprVisitor<T, V, E> visitor, V value) throws E {
            return visitor.visitFunctionCallExpr(this, value);
        }
    }

    /**
     * VarLenReadExpr captures the inputColumnName and the readExpression used to read a variable length column.
     * The captured inputColumnName will be used to lookup the average entry size for the column in the corresponding.
     * If inputColumnName is null then the readExpression is used to get the name of the column.
     * {@link org.apache.drill.exec.record.RecordBatchSizer}
     */
    public static class VarLenReadExpr extends OutputWidthExpression  {
        ValueVectorReadExpression readExpression;
        String inputColumnName;

        public VarLenReadExpr(ValueVectorReadExpression readExpression) {
            this.readExpression = readExpression;
            this.inputColumnName = null;
        }

        public VarLenReadExpr(String inputColumnName) {
            this.readExpression = null;
            this.inputColumnName = inputColumnName;
        }

        public ValueVectorReadExpression getReadExpression() {
            return readExpression;
        }

        public String getInputColumnName() {
            return inputColumnName;
        }

        @Override
        public <T, V, E extends Exception> T accept(AbstractExecExprVisitor<T, V, E> visitor, V value) throws E {
            return visitor.visitVarLenReadExpr(this, value);
        }
    }

    /**
     * Used to represent fixed-width values used in an expression.
     */

    public static class FixedLenExpr extends OutputWidthExpression {
        /**
         * Only the width of the payload is saved in fixedDataWidth.
         * Metadata width is added when the final output row size is calculated.
         * This is to avoid function {@link OutputWidthCalculator} from using
         * metadata width in the calculations.
         */
        private int fixedDataWidth;

        public FixedLenExpr(int fixedWidth) {
            this.fixedDataWidth = fixedWidth;
        }
        public int getDataWidth() { return fixedDataWidth;}

        @Override
        public <T, V, E extends Exception> T accept(AbstractExecExprVisitor<T, V, E> visitor, V value) throws E {
            return visitor.visitFixedLenExpr(this, value);
        }
    }

}
