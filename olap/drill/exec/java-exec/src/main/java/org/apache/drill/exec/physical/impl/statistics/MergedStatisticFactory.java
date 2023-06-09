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

import org.apache.drill.metastore.statistics.Statistic;

import java.util.HashMap;

public class MergedStatisticFactory {
  private HashMap<String,Class<? extends MergedStatistic>> statsClasses = new HashMap<>( );
  /*
   * Creates the appropriate statistics object given the name of the statistics and the input statistic
   */
  private static MergedStatisticFactory instance = new MergedStatisticFactory();
  //Can not instantiate
  private MergedStatisticFactory() {
    statsClasses.put(Statistic.COLNAME, ColumnMergedStatistic.class);
    statsClasses.put(Statistic.COLTYPE, ColTypeMergedStatistic.class);
    statsClasses.put(Statistic.ROWCOUNT, RowCountMergedStatistic.class);
    statsClasses.put(Statistic.NNROWCOUNT, NNRowCountMergedStatistic.class);
    statsClasses.put(Statistic.AVG_WIDTH, AvgWidthMergedStatistic.class);
    statsClasses.put(Statistic.HLL_MERGE, HLLMergedStatistic.class);
    statsClasses.put(Statistic.NDV, NDVMergedStatistic.class);
    statsClasses.put(Statistic.SUM_DUPS, CntDupsMergedStatistic.class);
    statsClasses.put(Statistic.TDIGEST_MERGE, TDigestMergedStatistic.class);
  }

  private MergedStatistic newMergedStatistic(String outputStatName)
      throws InstantiationException, IllegalAccessException {
    MergedStatistic stat = statsClasses.get(outputStatName).newInstance();
    return stat;
  }

  public static MergedStatistic getMergedStatistic(String outputStatName, String inputStatName, double samplePercent) {
    try {
      MergedStatistic statistic = instance.newMergedStatistic(outputStatName);
      if (statistic == null) {
        throw new IllegalArgumentException("No implementation found for " + outputStatName);
      } else {
        statistic.initialize(inputStatName, samplePercent);
        return statistic;
      }
    } catch (InstantiationException ex) {
      throw new IllegalArgumentException("Cannot instantiate class for " + outputStatName);
    } catch (IllegalAccessException ex) {
      throw new IllegalArgumentException("Cannot access class for " + outputStatName);
    }
  }
}

