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
package org.apache.drill.exec.physical.impl.statistics;

import java.util.HashMap;
import java.util.Map;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;

public class NNRowCountMergedStatistic extends AbstractMergedStatistic {

  private Map<String, Long> sumHolder;

  public NNRowCountMergedStatistic() {
    this.sumHolder = new HashMap<>();
    state = State.INIT;
  }

  @Override
  public void initialize(String inputName, double samplePercent) {
    super.initialize(Statistic.NNROWCOUNT, inputName, samplePercent);
    state = State.MERGE;
  }

  @Override
  public String getName() {
      return name;
  }

  @Override
  public String getInput() {
    return inputName;
  }

  @Override
  public void merge(MapVector input) {
    // Check the input is a Map Vector
    assert (input.getField().getType().getMinorType() == TypeProtos.MinorType.MAP);
    for (ValueVector vv : input) {
      String colName = vv.getField().getName();
      NullableBigIntVector biv = (NullableBigIntVector) vv;
      NullableBigIntVector.Accessor accessor = biv.getAccessor();
      long sum = 0;
      if (sumHolder.get(colName) != null) {
        sum = sumHolder.get(colName);
      }
      if (!accessor.isNull(0)) {
        sum += accessor.get(0);
        sumHolder.put(colName, sum);
      }
    }
  }

  public long getStat(String colName) {
    if (state != State.COMPLETE) {
      throw new IllegalStateException(String.format("Statistic `%s` has not completed merging statistics",
          name));
    }
    return (long)(100.0*sumHolder.get(colName)/ samplePercent);
  }

  @Override
  public void setOutput(MapVector output) {
    // Check the input is a Map Vector
    assert (output.getField().getType().getMinorType() == TypeProtos.MinorType.MAP);
    for (ValueVector outMapCol : output) {
      String colName = outMapCol.getField().getName();
      NullableBigIntVector vv = (NullableBigIntVector) outMapCol;
      vv.allocateNewSafe();
      if (sumHolder.get(colName) != null) {
        vv.getMutator().setSafe(0, (long)(100.0*sumHolder.get(colName)/ samplePercent));
      } else {
        vv.getMutator().setNull(0);
      }
    }
    state = State.COMPLETE;
  }
}
