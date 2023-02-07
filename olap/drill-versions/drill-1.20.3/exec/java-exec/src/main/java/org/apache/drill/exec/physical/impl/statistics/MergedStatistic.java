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

import org.apache.drill.exec.vector.complex.MapVector;

/*
 * Interface for implementing a merged statistic. A merged statistic can merge
 * the input statistics to get the overall value. e.g. `rowcount` merged statistic
 * should merge all `rowcount` input statistic and return the overall `rowcount`.
 * Given `rowcount`s 10 and 20, the `rowcount` merged statistic will return 30.
 */
public interface MergedStatistic {

  /** Initialize the merged statistic
   *
   *  @param inputName - the input {@link StatisticsAggBatch} statistic for this merged statistic
   *  @param samplePercent - the sample percentage used for extrapolation post merge phase
   */
  void initialize(String inputName, double samplePercent);

  /** Gets the name of the merged statistic
   *
   * @return - name of this merged statistic
   */
  String getName();

  /**
   * Gets the name of the input statistic
   *
   *  @return - name of the input {@link StatisticsAggBatch} statistic for this merged statistic
   */
  String getInput();

  /** Merges the input statistic (incoming value vector) into the existing
   * merged statistic
   *
   * @param input - the input value vector to merge
   */
  void merge(MapVector input);

  /** Sets the merged statistic value in the output (outgoing value vector)
   *
   * @param output - the output vector where to populate the statistic value
   */
  void setOutput(MapVector output);
}
