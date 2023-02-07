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
package org.apache.drill.exec.ops;

/**
 * Interface for updating a statistic. Provides just the methods
 * to add to or update a statistic, hiding implementation. Allows
 * a test-time implementation that differs from the run-time
 * version.
 */

public interface OperatorStatReceiver {

  /**
   * Add a long value to the existing value. Creates the stat
   * (with an initial value of zero) if the stat does not yet
   * exist.
   *
   * @param metric the metric to update
   * @param value the value to add to the existing value
   */

  void addLongStat(MetricDef metric, long value);

  /**
   * Add a double value to the existing value. Creates the stat
   * (with an initial value of zero) if the stat does not yet
   * exist.
   *
   * @param metric the metric to update
   * @param value the value to add to the existing value
   */

  void addDoubleStat(MetricDef metric, double value);

  /**
   * Set a stat to the specified long value. Creates the stat
   * if the stat does not yet exist.
   *
   * @param metric the metric to update
   * @param value the value to set
   */

  void setLongStat(MetricDef metric, long value);

  /**
   * Set a stat to the specified double value. Creates the stat
   * if the stat does not yet exist.
   *
   * @param metric the metric to update
   * @param value the value to set
   */

  void setDoubleStat(MetricDef metric, double value);

  void startWait();

  void stopWait();
}
