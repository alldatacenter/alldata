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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Copy from iceberg {@link org.apache.iceberg.util.StructLikeWrapper},
 * Add new constructor to create StructLikeWrapper more cheap
 */
public class StructLikeWrapper implements Serializable {

  public static StructLikeWrapper forType(Types.StructType struct) {
    return new StructLikeWrapper(struct);
  }

  private final transient Comparator<StructLike> comparator;
  private final transient JavaHash<StructLike> structHash;
  private transient Integer hashCode;
  private StructLike struct;

  private StructLikeWrapper(Types.StructType type) {
    this.comparator = Comparators.forType(type);
    this.structHash = JavaHash.forType(type);
    this.hashCode = null;
  }

  public StructLikeWrapper(
      Comparator<StructLike> comparator,
      JavaHash<StructLike> structHash) {
    this.comparator = comparator;
    this.structHash = structHash;
  }

  public StructLikeWrapper set(StructLike newStruct) {
    this.struct = newStruct;
    this.hashCode = null;
    return this;
  }

  public StructLike get() {
    return struct;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof StructLikeWrapper)) {
      return false;
    }

    StructLikeWrapper that = (StructLikeWrapper) other;

    if (this.struct == that.struct) {
      return true;
    }

    if (this.struct == null ^ that.struct == null) {
      return false;
    }

    return comparator.compare(this.struct, that.struct) == 0;
  }

  @Override
  public int hashCode() {
    if (hashCode == null) {
      this.hashCode = structHash.hash(struct);
    }

    return hashCode;
  }
}
