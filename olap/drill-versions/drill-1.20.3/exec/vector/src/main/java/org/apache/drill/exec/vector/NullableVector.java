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
package org.apache.drill.exec.vector;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.MaterializedField;

public interface NullableVector extends ValueVector {

  public interface Mutator extends ValueVector.Mutator {

    /**
     * Used by the vector accessors to force the last set value.
     * @param n the value of the last set field used to
     * fill empties
     */

    void setSetCount(int n);
  }

  MaterializedField bitsField = MaterializedField.create(BITS_VECTOR_NAME, Types.required(MinorType.UINT1));

  ValueVector getBitsVector();
  ValueVector getValuesVector();
}
