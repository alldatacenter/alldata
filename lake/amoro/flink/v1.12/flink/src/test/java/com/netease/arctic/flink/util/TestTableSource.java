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

package com.netease.arctic.flink.util;

import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.sources.DefinedProctimeAttribute;
import org.apache.flink.table.sources.DefinedRowtimeAttributes;
import org.apache.flink.table.sources.RowtimeAttributeDescriptor;
import org.apache.flink.table.sources.TableSource;
import org.apache.flink.table.types.DataType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class TestTableSource
    implements TableSource<Object>, DefinedProctimeAttribute, DefinedRowtimeAttributes {

  private final DataType producedDataType;
  private final List<String> rowtimeAttributes;
  private final String proctimeAttribute;

  private TestTableSource(
      DataType producedDataType,
      List<String> rowtimeAttributes,
      String proctimeAttribute) {
    this.producedDataType = producedDataType;
    this.rowtimeAttributes = rowtimeAttributes;
    this.proctimeAttribute = proctimeAttribute;
  }

  @Nullable
  @Override
  public String getProctimeAttribute() {
    return proctimeAttribute;
  }

  @Override
  public List<RowtimeAttributeDescriptor> getRowtimeAttributeDescriptors() {
    return rowtimeAttributes.stream()
        .map(attr -> new RowtimeAttributeDescriptor(attr, null, null))
        .collect(Collectors.toList());
  }

  @Override
  public DataType getProducedDataType() {
    return producedDataType;
  }

  @Override
  public TableSchema getTableSchema() {
    throw new UnsupportedOperationException("Should not be called");
  }
}