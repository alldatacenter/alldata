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

package com.netease.arctic.iceberg;

import org.apache.iceberg.StructLike;
import org.apache.iceberg.types.Comparators;
import org.apache.iceberg.types.JavaHash;
import org.apache.iceberg.types.Types;

import java.util.Comparator;

/**
 * Factory to create {@link StructLikeWrapper}.
 */
public class StructLikeWrapperFactory {

  private final Comparator<StructLike> comparator;
  private final JavaHash<StructLike> structHash;

  public StructLikeWrapperFactory(Types.StructType type) {
    this.comparator = Comparators.forType(type);
    this.structHash = JavaHash.forType(type);
  }

  public StructLikeWrapper create() {
    return new StructLikeWrapper(comparator, structHash);
  }
}
