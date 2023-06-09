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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.MajorTypeSerDe;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;

public class AvgWidthMergedStatistic extends AbstractMergedStatistic {
  private Map<String, Double> sumHolder;
  ColTypeMergedStatistic types;
  NNRowCountMergedStatistic nonNullStatCounts;
  RowCountMergedStatistic statCounts;

  public AvgWidthMergedStatistic () {
    this.sumHolder = new HashMap<>();
    types = null;
    nonNullStatCounts = null;
    statCounts = null;
    state = State.INIT;
  }

  @Override
  public void initialize(String inputName, double samplePercent) {
    super.initialize(Statistic.AVG_WIDTH, inputName, samplePercent);
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
      NullableFloat8Vector fv = (NullableFloat8Vector) vv;
      NullableFloat8Vector.Accessor accessor = fv.getAccessor();
      String colName = vv.getField().getName();
      double sum = 0;
      if (sumHolder.get(colName) != null) {
        sum = sumHolder.get(colName);
      }
      if (!accessor.isNull(0)) {
        sum += accessor.get(0);
        sumHolder.put(colName, sum);
      }
    }
  }

  public double getStat(String colName) {
    if (state != State.COMPLETE) {
      throw new IllegalStateException(
          String.format("Statistic `%s` has not completed merging statistics", name));
    }
    return sumHolder.get(colName)/((samplePercent/100.0) *getRowCount(colName));
  }

  @Override
  public void setOutput(MapVector output) {
    // Check the input is a Map Vector
    assert (output.getField().getType().getMinorType() == TypeProtos.MinorType.MAP);
    // Dependencies have been configured correctly
    assert (state == State.MERGE);
    for (ValueVector outMapCol : output) {
      String colName = outMapCol.getField().getName();
      NullableFloat8Vector vv = (NullableFloat8Vector) outMapCol;
      vv.allocateNewSafe();
      // For variable-length columns, we divide by non-null rows since NULL values do not
      // take up space. For fixed-length columns NULL values take up space.
      if (sumHolder.get(colName) != null
          && getRowCount(colName) > 0) {
        vv.getMutator().setSafe(0, sumHolder.get(colName)/((samplePercent/100.0) *getRowCount(colName)));
      } else {
        vv.getMutator().setNull(0);
      }
    }
    state = State.COMPLETE;
  }

  public void configure(List<MergedStatistic> statisticList) {
    assert (state == State.CONFIG);
    for (MergedStatistic statistic : statisticList) {
      if (statistic.getName().equals(Statistic.COLTYPE)) {
        types = (ColTypeMergedStatistic) statistic;
      } else if (statistic.getName().equals(Statistic.ROWCOUNT)) {
        statCounts = (RowCountMergedStatistic) statistic;
      } else if (statistic.getName().equals(Statistic.NNROWCOUNT)) {
        nonNullStatCounts = (NNRowCountMergedStatistic) statistic;
      }
    }
    assert (types != null && statCounts != null && nonNullStatCounts != null);
    // Now config complete - moving to MERGE state
    state = State.MERGE;
  }

  private long getRowCount(String colName) {
    byte[] typeAsBytes = types.getStat(colName);
    int type  = -1;
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule deModule = new SimpleModule("StatisticsSerDeModeule") //
            .addDeserializer(TypeProtos.MajorType.class, new MajorTypeSerDe.De());
    mapper.registerModule(deModule);
    try {
      type = mapper.readValue(typeAsBytes, TypeProtos.MajorType.class).getMinorType().getNumber();
    } catch (IOException ex) {
      //Ignore exception
    }
    // If variable length type - then use the nonNullCount. Otherwise, use the Count,
    // since even NULL values take up the same space.
    if (type == TypeProtos.MinorType.VAR16CHAR.getNumber()
        || type == TypeProtos.MinorType.VARCHAR.getNumber()
        || type == TypeProtos.MinorType.VARBINARY.getNumber()) {
      return nonNullStatCounts.getStat(colName);
    } else {
      return statCounts.getStat(colName);
    }
  }
}
