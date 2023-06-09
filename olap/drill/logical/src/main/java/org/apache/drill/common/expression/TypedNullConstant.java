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
package org.apache.drill.common.expression;

import java.util.Collections;
import java.util.Iterator;

import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;

public class TypedNullConstant extends LogicalExpressionBase {

    private final MajorType type;

    public TypedNullConstant(MajorType type) {
      super(null);
      this.type = TypeProtos.MajorType.newBuilder().mergeFrom(type).setMode(DataMode.OPTIONAL).build();
    }

    @Override
    public MajorType getMajorType() {
      return this.type;
    }


    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitNullConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }

}
