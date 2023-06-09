/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.utils.map;

import com.netease.arctic.iceberg.optimize.StructLikeWrapper;
import com.netease.arctic.utils.ObjectSizeCalculator;
import org.apache.iceberg.StructLike;

/**
 * Size Estimator for StructLikeWrapper record payload.
 */
public class StructLikeWrapperSizeEstimator implements SizeEstimator<StructLikeWrapper> {
  @Override
  public long sizeEstimate(StructLikeWrapper structLikeWrapper) {
    if (structLikeWrapper == null) {
      return 0;
    }
    StructLike structLike = structLikeWrapper.get();
    return ObjectSizeCalculator.getObjectSize(structLikeObjects(structLike));
  }

  private Object[] structLikeObjects(StructLike structLike) {
    if (structLike == null) {
      return null;
    }

    Object[] values = new Object[structLike.size()];
    for (int i = 0; i < values.length; i += 1) {
      Object value = structLike.get(i, Object.class);

      if (value instanceof StructLike) {
        values[i] = structLikeObjects((StructLike) value);
      } else {
        values[i] = value;
      }
    }

    return values;
  }
}
