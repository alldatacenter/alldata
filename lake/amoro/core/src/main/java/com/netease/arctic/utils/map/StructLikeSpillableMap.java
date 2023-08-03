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

import com.netease.arctic.iceberg.StructLikeWrapper;
import com.netease.arctic.utils.SerializationUtil;
import org.apache.iceberg.types.Types;

import javax.annotation.Nullable;

/**
 * Copy form iceberg {@link org.apache.iceberg.util.StructLikeMap}. Make using StructLikeWrapper more cheap
 */
public class StructLikeSpillableMap<T> extends StructLikeBaseMap<T> {

  public static <T> StructLikeSpillableMap<T> create(Types.StructType type,
                                                     Long maxInMemorySizeInBytes,
                                                     @Nullable String backendBaseDir) {
    return new StructLikeSpillableMap<>(type, maxInMemorySizeInBytes, backendBaseDir);
  }

  private final SimpleMap<StructLikeWrapper, T> wrapperMap;

  private StructLikeSpillableMap(Types.StructType type, Long maxInMemorySizeInBytes, @Nullable String backendBaseDir) {
    super(type);
    this.wrapperMap = new SimpleSpillableMap<>(maxInMemorySizeInBytes, backendBaseDir,
        SerializationUtil.createStructLikeWrapperSerializer(structLikeWrapperFactory),
        SerializationUtil.createJavaSimpleSerializer(),
        new StructLikeWrapperSizeEstimator(), new DefaultSizeEstimator<>());
  }

  @Override
  protected SimpleMap<StructLikeWrapper, T> getInternalMap() {
    return wrapperMap;
  }
}

