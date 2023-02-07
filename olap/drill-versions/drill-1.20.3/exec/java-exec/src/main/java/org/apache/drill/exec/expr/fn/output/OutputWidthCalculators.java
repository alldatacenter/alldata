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

package org.apache.drill.exec.expr.fn.output;

import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.FixedLenExpr;

import java.util.List;

/**
 * Return type calculation implementation for functions with return type set as
 * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#CONCAT}.
 */

public class OutputWidthCalculators {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OutputWidthCalculators.class);

    private static int adjustOutputWidth(int outputSize, String prefix) {
        if (outputSize > Types.MAX_VARCHAR_LENGTH || outputSize < 0 /*overflow*/) {
            logger.warn(prefix + " Output size for expressions is too large, setting to MAX_VARCHAR_LENGTH");
            outputSize = Types.MAX_VARCHAR_LENGTH;
        }
        return outputSize;
    }

    public static class ConcatOutputWidthCalculator implements OutputWidthCalculator {

        public static final ConcatOutputWidthCalculator INSTANCE = new ConcatOutputWidthCalculator();

        /**
         * Defines a function's output size estimate as sum of input sizes
         * If calculated size is greater than {@link Types#MAX_VARCHAR_LENGTH},
         * it is replaced with {@link Types#MAX_VARCHAR_LENGTH}.
         *
         * @param args
         * @return return type
         */
        @Override
        public int getOutputWidth(List<FixedLenExpr> args) {
            int outputSize = 0;
            if (args == null || args.size() == 0) {
                throw new IllegalArgumentException();
            }
            for (FixedLenExpr expr : args) {
                outputSize += expr.getDataWidth();
            }
            outputSize = adjustOutputWidth(outputSize, "ConcatOutputWidthCalculator:");
            return outputSize;
        }
    }

    public static class CloneOutputWidthCalculator implements OutputWidthCalculator {

        public static final CloneOutputWidthCalculator INSTANCE = new CloneOutputWidthCalculator();

        /**
         * Defines a function's output size estimate as the same length as the first
         * argument. In other words, treats the function as a CLONE function
         * If calculated size is greater than {@link Types#MAX_VARCHAR_LENGTH},
         * it is replaced with {@link Types#MAX_VARCHAR_LENGTH}.
         *
         * @param args logical expressions
         * @return return type
         */
        @Override
        public int getOutputWidth(List<FixedLenExpr> args) {
            int outputSize = 0;
            if (args == null || args.size() < 1) {
                throw new IllegalArgumentException();
            }
            outputSize = args.get(0).getDataWidth();
            outputSize = adjustOutputWidth(outputSize, "CloneOutputWidthCalculator:");
            return outputSize;
        }
    }

    public static class DefaultOutputWidthCalculator implements OutputWidthCalculator {

        public static final DefaultOutputWidthCalculator INSTANCE = new DefaultOutputWidthCalculator();

        /**
         * Defines a function's output size estimate as some fixed value specified via an option
         * If calculated size is greater than {@link Types#MAX_VARCHAR_LENGTH},
         * it is replaced with {@link Types#MAX_VARCHAR_LENGTH}.
         *
         * @param args logical expressions
         * @return return type
         */
        @Override
        public int getOutputWidth(List<FixedLenExpr> args) {
            //TODO: Read value from options?
            int outputSize = adjustOutputWidth(50, "DefaultOutputWidthCalculator:");
            return outputSize;
        }
    }
}