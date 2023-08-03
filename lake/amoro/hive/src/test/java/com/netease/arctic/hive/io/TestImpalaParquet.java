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

package com.netease.arctic.hive.io;

import com.google.common.collect.Iterators;
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
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;

/**
 * Impala may write string type column with binary value in parquet file,
 * which is okay for Hive readers, Arctic need to support it too for mixed-hive format tables.
 */
public class TestImpalaParquet {

  private static final String PARQUET_FILE_NAME = "string_is_bytes.parquet";

  @Test
  public void testReadParquetFileProducedByImpala() {
    NameMapping mapping = NameMapping.of(MappedField.of(1, "str"));
    Schema schema = new Schema(Types.NestedField.of(1, true, "str", Types.StringType.get()));
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(
        Files.localInput(loadParquetFilePath()))
        .project(schema)
        .withNameMapping(mapping)
        .createReaderFunc(fileSchema -> AdaptHiveGenericParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);
    for (Object o : builder.build()) {
      Record next = (Record) o;
      Assert.assertEquals("hello parquet", next.get(0));
    }
  }

  @Test
  public void testReadParquetFileProducedByImpalaWithFilter() {
    NameMapping mapping = NameMapping.of(MappedField.of(1, "str"));
    Schema schema = new Schema(Types.NestedField.of(1, true, "str", Types.StringType.get()));
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(
        Files.localInput(loadParquetFilePath()))
        .project(schema)
        .withNameMapping(mapping)
        .filter(Expressions.in("str", "aa"))
        .createReaderFunc(fileSchema -> AdaptHiveGenericParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);
    CloseableIterator<Object> iterator = builder.build().iterator();
    Assert.assertEquals(0, Iterators.size(iterator));
  }

  private String loadParquetFilePath() {
    URL fileUrl = this.getClass().getClassLoader().getResource(PARQUET_FILE_NAME);
    if (fileUrl != null) {
      return fileUrl.getFile();
    } else {
      throw new IllegalStateException("Cannot load parquet file produced by impala");
    }
  }
}
