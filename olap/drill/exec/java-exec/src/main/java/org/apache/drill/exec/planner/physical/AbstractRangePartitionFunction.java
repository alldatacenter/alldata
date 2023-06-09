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
package org.apache.drill.exec.planner.physical;

import java.util.List;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.exec.record.VectorWrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class AbstractRangePartitionFunction  implements PartitionFunction {
  public static final String RANGE_PARTITION_EXPR_NAME = "R_A_N_G_E_E_X_P_R";

  public abstract List<FieldReference> getPartitionRefList();

  public abstract void setup(List<VectorWrapper<?>> partitionKeys);

  public abstract int eval(int index, int numPartitions);

  @JsonIgnore
  @Override
  public FieldReference getPartitionFieldRef() {
    return new FieldReference(RANGE_PARTITION_EXPR_NAME);
  }

}
