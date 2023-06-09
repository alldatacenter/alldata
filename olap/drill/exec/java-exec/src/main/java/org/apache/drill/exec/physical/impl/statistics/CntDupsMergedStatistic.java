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
import java.util.List;
import java.util.Map;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;

public class CntDupsMergedStatistic extends AbstractMergedStatistic {
    private Map<String, Long> sumHolder;

    public CntDupsMergedStatistic () {
        this.sumHolder = new HashMap<>();
        state = State.INIT;
    }

    @Override
    public void initialize(String inputName, double samplePercent) {
        super.initialize(Statistic.SUM_DUPS, inputName, samplePercent);
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
            NullableBigIntVector fv = (NullableBigIntVector) vv;
            NullableBigIntVector.Accessor accessor = fv.getAccessor();
            String colName = vv.getField().getName();
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

    public double getStat(String colName) {
        if (state != State.COMPLETE) {
            throw new IllegalStateException(
                    String.format("Statistic `%s` has not completed merging statistics", name));
        }
        return sumHolder.get(colName);
    }

    @Override
    public void setOutput(MapVector output) {
        // Check the input is a Map Vector
        assert (output.getField().getType().getMinorType() == TypeProtos.MinorType.MAP);
        // Dependencies have been configured correctly
        assert (state == State.MERGE);
        for (ValueVector outMapCol : output) {
            String colName = outMapCol.getField().getName();
            NullableBigIntVector vv = (NullableBigIntVector) outMapCol;
            vv.allocateNewSafe();
            // For variable-length columns, we divide by non-null rows since NULL values do not
            // take up space. For fixed-length columns NULL values take up space.
            if (sumHolder.get(colName) != null) {
                vv.getMutator().setSafe(0, sumHolder.get(colName));
            }
        }
        state = State.COMPLETE;
    }

    public void configure(List<MergedStatistic> statisticList) {
        assert (state == State.CONFIG);
        // Now config complete - moving to MERGE state
        state = State.MERGE;
    }
}
