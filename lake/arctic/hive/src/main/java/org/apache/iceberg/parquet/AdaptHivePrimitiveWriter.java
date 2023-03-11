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

package org.apache.iceberg.parquet;

import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.ColumnWriteStore;

import java.util.List;

public class AdaptHivePrimitiveWriter<T> implements ParquetValueWriter<T> {

  protected final AdaptHiveColumnWriter<T> column;

  private final List<TripleWriter<?>> children;

  protected AdaptHivePrimitiveWriter(ColumnDescriptor desc) {
    //Change For Arctic
    this.column = AdaptHiveColumnWriter.newWriter(desc);
    //Change For Arctic
    this.children = ImmutableList.of(column);
  }

  @Override
  public void write(int repetitionLevel, T value) {
    column.write(repetitionLevel, value);
  }

  @Override
  public List<TripleWriter<?>> columns() {
    return children;
  }

  @Override
  public void setColumnStore(ColumnWriteStore columnStore) {
    this.column.setColumnStore(columnStore);
  }
}