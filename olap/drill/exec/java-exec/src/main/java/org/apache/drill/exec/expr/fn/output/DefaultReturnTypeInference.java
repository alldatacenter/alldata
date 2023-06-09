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

import java.util.List;
import java.util.Set;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.fn.FunctionAttributes;
import org.apache.drill.exec.expr.fn.FunctionUtils;
import org.apache.drill.exec.expr.fn.ValueReference;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * Return type calculation implementation for functions with return type set as
 * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DEFAULT}.
 */
public class DefaultReturnTypeInference implements ReturnTypeInference {

  public static final DefaultReturnTypeInference INSTANCE = new DefaultReturnTypeInference();

  /**
   * Calculates return type and its nullability. Precision and scale is not included.
   *
   * @param logicalExpressions logical expressions
   * @param attributes function attributes
   * @return return type
   */
  @Override
  public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
    if (attributes.getReturnValue().getType().getMinorType() == TypeProtos.MinorType.UNION) {
      Set<TypeProtos.MinorType> subTypes = Sets.newHashSet();
      for (ValueReference ref : attributes.getParameters()) {
        subTypes.add(ref.getType().getMinorType());
      }

      TypeProtos.MajorType.Builder builder = TypeProtos.MajorType.newBuilder()
          .setMinorType(TypeProtos.MinorType.UNION)
          .setMode(TypeProtos.DataMode.OPTIONAL);

      for (TypeProtos.MinorType subType : subTypes) {
        // LATE is not a valid concrete type; used only as a method
        // annotation.
        if (subType != TypeProtos.MinorType.LATE) {
          builder.addSubType(subType);
        }
      }
      return builder.build();
    }
    return attributes.getReturnValue().getType().toBuilder()
        .setMode(FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes))
        .build();
  }
}
