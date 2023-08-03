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

package com.netease.arctic.spark.test;

import com.google.common.collect.Iterators;
import com.netease.arctic.hive.HMSMockServer;
import com.netease.arctic.spark.reader.SparkParquetReaders;
import com.netease.arctic.spark.test.helper.ResourceInputFile;
import org.apache.iceberg.Schema;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.mapping.MappedField;
import org.apache.iceberg.mapping.NameMapping;
import org.apache.iceberg.parquet.AdaptHiveParquet;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.catalyst.InternalRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class TestImpalaParquet {
  InputFile targetInputParquet;

  @BeforeEach
  public void setup() {
    targetInputParquet = ResourceInputFile.newFile(HMSMockServer.class.getClassLoader(),
        "string_is_bytes.parquet");
  }


  @Test
  public void sparkRead() {
    NameMapping mapping = NameMapping.of(MappedField.of(1, "str"));
    Schema schema = new Schema(Types.NestedField.of(1, true, "str", Types.StringType.get()));

    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(targetInputParquet)
        .project(schema)
        .withNameMapping(mapping)
        .createReaderFunc(fileSchema -> SparkParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);
    CloseableIterator<Object> iterator = builder.build().iterator();
    while (iterator.hasNext()) {
      InternalRow next = (InternalRow) iterator.next();
      Assertions.assertEquals("hello parquet", next.getString(0));
    }
  }

  @Test
  public void genericFilter() {
    NameMapping mapping = NameMapping.of(MappedField.of(1, "str"));
    Schema schema = new Schema(Types.NestedField.of(1, true, "str", Types.StringType.get()));
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(targetInputParquet)
        .project(schema)
        .withNameMapping(mapping)
        .filter(Expressions.in("str", "aa"))
        .createReaderFunc(fileSchema -> SparkParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);
    CloseableIterator<Object> iterator = builder.build().iterator();
    Assertions.assertEquals(0, Iterators.size(iterator));
  }
}
