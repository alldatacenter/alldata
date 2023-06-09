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
package org.apache.drill.exec.expr;

import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.FunctionCallExpr;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.FixedLenExpr;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.VarLenReadExpr;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.IfElseWidthExpr;


public abstract class AbstractExecExprVisitor<T, VAL, EXCEP extends Exception> extends AbstractExprVisitor<T, VAL, EXCEP> {

    public T visitValueVectorWriteExpression(ValueVectorWriteExpression writeExpr, VAL value) throws EXCEP {
        return visitUnknown(writeExpr, value);
    }

    public T visitValueVectorReadExpression(ValueVectorReadExpression readExpr, VAL value) throws EXCEP {
        return visitUnknown(readExpr, value);
    }

    public T visitFunctionCallExpr(FunctionCallExpr functionCallExpr, VAL value) throws EXCEP {
        return visitUnknown(functionCallExpr, value);
    }

    public T visitFixedLenExpr(FixedLenExpr fixedLenExpr, VAL value) throws EXCEP {
        return visitUnknown(fixedLenExpr, value);
    }

    public T visitVarLenReadExpr(VarLenReadExpr varLenReadExpr, VAL value) throws EXCEP {
        return visitUnknown(varLenReadExpr, value);
    }

    public T visitIfElseWidthExpr(IfElseWidthExpr ifElseWidthExpr, VAL value) throws EXCEP {
        return visitUnknown(ifElseWidthExpr, value);
    }

    public T visitUnknown(OutputWidthExpression e, VAL value) throws EXCEP {
        throw new UnsupportedOperationException(String.format("Expression of type %s not handled by visitor type %s.",
                e.getClass().getCanonicalName(), this.getClass().getCanonicalName()));
    }
}
