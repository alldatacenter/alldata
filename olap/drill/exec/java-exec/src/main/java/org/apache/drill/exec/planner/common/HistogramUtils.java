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
package org.apache.drill.exec.planner.common;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.metastore.statistics.Histogram;

public class HistogramUtils {

  /**
   * Build a histogram using the t-digest byte array.
   * The type of histogram is dependent on the data type of the column.
   */
  public static Histogram buildHistogramFromTDigest(byte[] tdigest_bytearray,
                                                    TypeProtos.MajorType type,
                                                    int numBuckets,
                                                    long nonNullCount) {
    Histogram histogram = null;
    if (type != null && type.hasMinorType()) {
      switch (type.getMinorType()) {
        case INT:
        case BIGINT:
        case FLOAT4:
        case FLOAT8:
        case DATE:
        case TIME:
        case TIMESTAMP:
        case BIT:
          histogram = NumericEquiDepthHistogram.buildFromTDigest(tdigest_bytearray, numBuckets, nonNullCount);
          break;
        default:
          // TODO: support other data types
          break;
      }
    }
    return histogram;
  }

}
