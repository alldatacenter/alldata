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
package org.apache.drill.exec.planner.types.decimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM;

public abstract class DrillBaseComputeScalePrecision {
  private static final Logger logger = LoggerFactory.getLogger(DrillBaseComputeScalePrecision.class);

  protected final static int MAX_NUMERIC_PRECISION = DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision();

  protected int outputScale = 0;
  protected int outputPrecision = 0;

  public DrillBaseComputeScalePrecision(int leftPrecision, int leftScale, int rightPrecision, int rightScale) {
    computeScalePrecision(leftPrecision, leftScale, rightPrecision, rightScale);
  }

  public abstract void computeScalePrecision(int leftPrecision, int leftScale, int rightPrecision, int rightScale);

  public int getOutputScale() {
    return outputScale;
  }

  public int getOutputPrecision() {
    return outputPrecision;
  }

  /**
   * Cuts down the fractional part if the current precision
   * exceeds the maximum precision range.
   */
  protected void adjustScaleAndPrecision() {
    if (outputPrecision > MAX_NUMERIC_PRECISION) {
      outputScale = outputScale - (outputPrecision - MAX_NUMERIC_PRECISION);
      outputPrecision = MAX_NUMERIC_PRECISION;
    }
    if (outputScale < 0) {
      logger.warn("Resulting precision: {} may overflow max allowed precision: {}.\n" +
          "Forced setting max allowed precision and 0 scale.",
          MAX_NUMERIC_PRECISION - outputScale, MAX_NUMERIC_PRECISION);
      outputScale = 0;
    }
  }
}
