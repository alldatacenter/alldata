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

package com.netease.arctic.spark;

import org.apache.iceberg.StructLike;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.types.BinaryType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DecimalType;
import org.apache.spark.sql.types.StringType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * this class copied from iceberg  org.apache.iceberg.spark.source.InternalRowWrapper
 * for InternalRowWrapper is not public class
 */
public class SparkInternalRowWrapper implements StructLike {
  private final DataType[] types;
  private final BiFunction<InternalRow, Integer, ?>[] getters;
  private InternalRow row = null;

  public SparkInternalRowWrapper(StructType rowType) {
    this.types = Stream.of(rowType.fields())
        .map(StructField::dataType)
        .toArray(DataType[]::new);
    this.getters = Stream.of(types)
        .map(SparkInternalRowWrapper::getter)
        .toArray(BiFunction[]::new);
  }

  @Override
  public int size() {
    return types.length;
  }

  @Override
  public <T> T get(int pos, Class<T> javaClass) {
    if (row.isNullAt(pos)) {
      return null;
    } else if (getters[pos] != null) {
      return javaClass.cast(getters[pos].apply(row, pos));
    }

    return javaClass.cast(row.get(pos, types[pos]));
  }

  @Override
  public <T> void set(int pos, T value) {
    row.update(pos, value);
  }


  public SparkInternalRowWrapper wrap(InternalRow internalRow) {
    this.row = internalRow;
    return this;
  }

  private static BiFunction<InternalRow, Integer, ?> getter(DataType type) {
    if (type instanceof StringType) {
      return (row, pos) -> row.getUTF8String(pos).toString();
    } else if (type instanceof DecimalType) {
      DecimalType decimal = (DecimalType) type;
      return (row, pos) ->
          row.getDecimal(pos, decimal.precision(), decimal.scale()).toJavaBigDecimal();
    } else if (type instanceof BinaryType) {
      return (row, pos) -> ByteBuffer.wrap(row.getBinary(pos));
    } else if (type instanceof StructType) {
      StructType structType = (StructType) type;
      SparkInternalRowWrapper nestedWrapper = new SparkInternalRowWrapper(structType);
      return (row, pos) -> nestedWrapper.wrap(row.getStruct(pos, structType.size()));
    }

    return null;
  }
}

