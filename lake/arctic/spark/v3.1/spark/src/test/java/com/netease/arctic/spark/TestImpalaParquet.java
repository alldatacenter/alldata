/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.netease.arctic.spark.reader.SparkParquetReaders;
import java.util.HashSet;
import java.util.Set;
import org.apache.iceberg.Files;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.AdaptHiveGenericParquetReaders;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.mapping.MappedField;
import org.apache.iceberg.mapping.NameMapping;
import org.apache.iceberg.parquet.AdaptHiveParquet;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.InternalRow;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class TestImpalaParquet {

  @Test
  public void sparkRead() {
    NameMapping mapping = NameMapping.of(MappedField.of(1, "str"));
    Schema schema = new Schema(Types.NestedField.of(1, true, "str", Types.StringType.get()));
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(

            Files.localInput(this.getClass().getClassLoader().getResource("string_is_bytes.parquet").getFile()))
        .project(schema)
        .withNameMapping(mapping)
        .createReaderFunc(fileSchema -> SparkParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);
    CloseableIterator<Object> iterator = builder.build().iterator();
    while (iterator.hasNext()) {
      InternalRow next = (InternalRow)iterator.next();
      Assert.assertTrue(next.getString(0).equals("hello parquet"));
    }
  }

  @Test
  public void genericFilter() {
    NameMapping mapping = NameMapping.of(MappedField.of(1, "str"));
    Schema schema = new Schema(Types.NestedField.of(1, true, "str", Types.StringType.get()));
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(

            Files.localInput(this.getClass().getClassLoader().getResource("string_is_bytes.parquet").getFile()))
        .project(schema)
        .withNameMapping(mapping)
        .filter(Expressions.in("str", "aa"))
        .createReaderFunc(fileSchema -> SparkParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);
    CloseableIterator<Object> iterator = builder.build().iterator();
    Assert.assertTrue(Iterators.size(iterator) == 0);
  }
}
