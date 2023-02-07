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

// Library implementing TDigest algorithm to derive approximate quantiles. Please refer to:
// 'Computing Extremely Accurate Quantiles using t-Digests' by Ted Dunning and Otmar Ertl

import com.tdunning.math.stats.MergingDigest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;

import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;

public class TDigestMergedStatistic extends AbstractMergedStatistic {
  private Map<String, MergingDigest> tdigestHolder;
  private int compression;

  public TDigestMergedStatistic() {
    this.tdigestHolder = new HashMap<>();
    state = State.INIT;
  }

  @Override
  public void initialize(String inputName, double samplePercent) {
    super.initialize(Statistic.TDIGEST_MERGE, inputName, samplePercent);
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
      MergingDigest colTdigestHolder = null;
      if (tdigestHolder.get(colName) != null) {
        colTdigestHolder = tdigestHolder.get(colName);
      }
      NullableVarBinaryVector tdigestVector = (NullableVarBinaryVector) vv;
      NullableVarBinaryVector.Accessor accessor = tdigestVector.getAccessor();

      if (!accessor.isNull(0)) {
        MergingDigest other = MergingDigest.fromBytes(ByteBuffer.wrap(accessor.get(0)));
        if (colTdigestHolder != null) {
          colTdigestHolder.add(other);
          tdigestHolder.put(colName, colTdigestHolder);
        } else {
          tdigestHolder.put(colName, other);
        }
      }
    }
  }

  public MergingDigest getStat(String colName) {
    if (state != State.COMPLETE) {
      throw new IllegalStateException(String.format("Statistic `%s` has not completed merging statistics",
          name));
    }
    return tdigestHolder.get(colName);
  }

  @Override
  public void setOutput(MapVector output) {
    // Check the input is a Map Vector
    assert (output.getField().getType().getMinorType() == TypeProtos.MinorType.MAP);
    // Dependencies have been configured correctly
    assert (state == State.MERGE);
    for (ValueVector outMapCol : output) {
      String colName = outMapCol.getField().getName();
      MergingDigest colTdigestHolder = tdigestHolder.get(colName);
      NullableVarBinaryVector vv = (NullableVarBinaryVector) outMapCol;
      vv.allocateNewSafe();
      try {
        if (colTdigestHolder != null) {
          int size = colTdigestHolder.smallByteSize();
          java.nio.ByteBuffer byteBuf = java.nio.ByteBuffer.allocate(size);
          colTdigestHolder.asSmallBytes(byteBuf);
          // NOTE: in setting the VV below, we are using the byte[] instead of the ByteBuffer because the
          // latter was producing incorrect output (after re-reading the data from the VV).  It is
          // unclear whether the setSafe() api for ByteBuffer has a bug, so to be safe we are using the
          // byte[] directly which works.
          vv.getMutator().setSafe(0, byteBuf.array(), 0, byteBuf.array().length);
        } else {
          vv.getMutator().setNull(0);
        }
      } catch (Exception ex) {
        // TODO: logger
      }
    }
    state = State.COMPLETE;
  }

  public void configure(OptionManager optionsManager) {
    assert (state == State.CONFIG);
    compression = (int) optionsManager.getLong(ExecConstants.TDIGEST_COMPRESSION);
    // Now config complete - moving to MERGE state
    state = State.MERGE;
  }
}
