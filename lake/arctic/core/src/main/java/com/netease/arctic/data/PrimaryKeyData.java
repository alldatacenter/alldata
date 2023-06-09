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

package com.netease.arctic.data;

import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Accessor;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * A struct of primary key values.
 * <p>
 * Instances of this class can produce primary key values from a data row passed to {@link #primaryKey(StructLike)}
 * and calculate th tree node of data by passing to {@link #treeNode(long)}
 */
public class PrimaryKeyData implements StructLike, Serializable {

  private final PrimaryKeySpec primaryKeySpec;
  private final int size;
  private final Object[] primaryTuple;
  private final Accessor<StructLike>[] accessors;

  @SuppressWarnings("unchecked")
  public PrimaryKeyData(PrimaryKeySpec primaryKeySpec, Schema inputSchema) {
    this.primaryKeySpec = primaryKeySpec;

    List<PrimaryKeySpec.PrimaryKeyField> fields = primaryKeySpec.fields();
    this.size = fields.size();
    this.primaryTuple = new Object[size];
    this.accessors = (Accessor<StructLike>[]) Array.newInstance(Accessor.class, size);

    Schema schema = primaryKeySpec.getSchema();
    for (int i = 0; i < size; i += 1) {
      PrimaryKeySpec.PrimaryKeyField field = fields.get(i);
      Accessor<StructLike> accessor = inputSchema.accessorForField(inputSchema.findField(field.fieldName()).fieldId());
      Preconditions.checkArgument(accessor != null,
          "Cannot build accessor for field: " + schema.findField(field.fieldName()));
      this.accessors[i] = accessor;
    }
  }

  private PrimaryKeyData(PrimaryKeyData toCopy) {
    this.primaryKeySpec = toCopy.primaryKeySpec;
    this.size = toCopy.size;
    this.primaryTuple = new Object[toCopy.primaryTuple.length];
    this.accessors = toCopy.accessors;

    System.arraycopy(toCopy.primaryTuple, 0, this.primaryTuple, 0, primaryTuple.length);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < primaryTuple.length; i += 1) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(primaryTuple[i]);
    }
    sb.append("]");
    return sb.toString();
  }

  public PrimaryKeyData copy() {
    return new PrimaryKeyData(this);
  }

  @SuppressWarnings("unchecked")
  public void primaryKey(StructLike row) {
    for (int i = 0; i < primaryTuple.length; i += 1) {
      primaryTuple[i] = accessors[i].get(row);
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public <T> T get(int pos, Class<T> javaClass) {
    return javaClass.cast(primaryTuple[pos]);
  }

  @Override
  public <T> void set(int pos, T value) {
    primaryTuple[pos] = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof PrimaryKeyData)) {
      return false;
    }

    PrimaryKeyData that = (PrimaryKeyData) o;
    return Arrays.equals(primaryTuple, that.primaryTuple);
  }

  @Override
  public int hashCode() {
    int hashcode = Math.abs(Arrays.hashCode(primaryTuple));
    return hashcode == Integer.MIN_VALUE ? Integer.MAX_VALUE : hashcode;
  }

  public DataTreeNode treeNode(long mask) {
    return DataTreeNode.of(mask, Math.abs(hashCode()) & mask);
  }
}