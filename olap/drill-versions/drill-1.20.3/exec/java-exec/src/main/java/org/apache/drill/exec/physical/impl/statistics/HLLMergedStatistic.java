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

// Library implementing HLL algorithm to derive approximate #distinct values(NDV). Please refer:
// 'HyperLogLog: the analysis of a near-optimal cardinality estimation algorithm.' Flajolet et. al.
import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;

public class HLLMergedStatistic extends AbstractMergedStatistic {
  private Map<String, HyperLogLog> hllHolder;
  private long accuracy;

  public HLLMergedStatistic () {
    this.hllHolder = new HashMap<>();
    state = State.INIT;
  }

  @Override
  public void initialize(String inputName, double samplePercent) {
    super.initialize(Statistic.HLL_MERGE, inputName, samplePercent);
    state = State.CONFIG;
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
      HyperLogLog colHLLHolder = null;
      if (hllHolder.get(colName) != null) {
        colHLLHolder = hllHolder.get(colName);
      }
      NullableVarBinaryVector hllVector = (NullableVarBinaryVector) vv;
      NullableVarBinaryVector.Accessor accessor = hllVector.getAccessor();

      try {
        if (!accessor.isNull(0)) {
          ByteArrayInputStream bais = new ByteArrayInputStream(accessor.get(0), 0, vv.getBufferSize());
          HyperLogLog other = HyperLogLog.Builder.build(new DataInputStream(bais));
          if (colHLLHolder != null) {
            colHLLHolder.addAll(other);
            hllHolder.put(colName, colHLLHolder);
          } else {
            hllHolder.put(colName, other);
          }
        }
      } catch (Exception ex) {
        //TODO: Catch IOException/CardinalityMergeException
        //TODO: logger
      }
    }
  }

  public HyperLogLog getStat(String colName) {
    if (state != State.COMPLETE) {
      throw new IllegalStateException(String.format("Statistic `%s` has not completed merging statistics",
          name));
    }
    return hllHolder.get(colName);
  }

  @Override
  public void setOutput(MapVector output) {
    // Check the input is a Map Vector
    assert (output.getField().getType().getMinorType() == TypeProtos.MinorType.MAP);
    // Dependencies have been configured correctly
    assert (state == State.MERGE);
    for (ValueVector outMapCol : output) {
      String colName = outMapCol.getField().getName();
      HyperLogLog colHLLHolder = hllHolder.get(colName);
      NullableVarBinaryVector vv = (NullableVarBinaryVector) outMapCol;
      vv.allocateNewSafe();
      try {
        if (colHLLHolder != null) {
          vv.getMutator().setSafe(0, colHLLHolder.getBytes(),
              0, colHLLHolder.getBytes().length);
        } else {
          vv.getMutator().setNull(0);
        }
      } catch (IOException ex) {
        // TODO: logger
      }
    }
    state = State.COMPLETE;
  }

  public void configure(OptionManager optionsManager) {
    assert (state == State.CONFIG);
    accuracy = optionsManager.getLong(ExecConstants.HLL_ACCURACY);
    // Now config complete - moving to MERGE state
    state = State.MERGE;
  }
}
