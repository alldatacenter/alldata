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
package org.apache.drill.exec.physical.resultSet.model;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.BaseRepeatedValueVector;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;

public class ContainerVisitor<R, A> {

  public R apply(VectorContainer container, A arg) {
    return visitContainer(container, arg);
  }

  private R visitContainer(VectorContainer container, A arg) {
    return visitChildren(container, arg);
  }

  public R visitChildren(VectorContainer container, A arg) {
    for (int i = 0; i < container.getNumberOfColumns(); i++) {
      final ValueVector vector = container.getValueVector(i).getValueVector();
      apply(vector, arg);
    }
    return null;
  }

  protected R apply(ValueVector vector, A arg) {
    final MaterializedField schema = vector.getField();
    final MajorType majorType = schema.getType();
    final MinorType type = majorType.getMinorType();
    final DataMode mode = majorType.getMode();
    switch (type) {
    case MAP:
      if (mode == DataMode.REPEATED) {
        return visitRepeatedMap((RepeatedMapVector) vector, arg);
      } else {
        return visitMap((AbstractMapVector) vector, arg);
      }
    case LIST:
      if (mode == DataMode.REPEATED) {
        return visitRepeatedList((RepeatedListVector) vector, arg);
      } else {
        return visitList((ListVector) vector, arg);
      }
    default:
      if (mode == DataMode.REPEATED) {
        return visitRepeatedPrimitive((BaseRepeatedValueVector) vector, arg);
      } else {
        return visitPrimitive(vector, arg);
      }
    }
  }

  protected R visitRepeatedMap(RepeatedMapVector vector, A arg) {
    visitChildren(vector, arg);
    return visitVector(vector, arg);
  }

  protected R visitMap(AbstractMapVector vector, A arg) {
    visitChildren(vector, arg);
    return visitVector(vector, arg);
  }

  private R visitChildren(AbstractMapVector vector, A arg) {
    for (int i = 0; i < vector.size(); i++) {
      apply(vector.getChildByOrdinal(i), arg);
    }
    return null;
  }

  protected R visitRepeatedList(RepeatedListVector vector, A arg) {
    return visitVector(vector, arg);
  }

  protected R visitList(ListVector vector, A arg) {
    return visitVector(vector, arg);
  }

  protected R visitRepeatedPrimitive(BaseRepeatedValueVector vector, A arg) {
    return visitVector(vector, arg);
  }

  protected R visitPrimitive(ValueVector vector, A arg) {
    return visitVector(vector, arg);
  }

  protected R visitVector(ValueVector vector, A arg) {
    return null;
  }

}
