/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.ddl.external;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.sink.SinkEngineConnectorBase;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;

/**
 * Created 2022/8/23
 */
@Getter
public class FakeSinkEngineConnector extends SinkEngineConnectorBase {
  private List<ColumnInfo> columnInfos;

  private List<ColumnInfo> updatedColumns;
  private List<ColumnInfo> deletedColumns;
  private List<ColumnInfo> addedColumns;

  public FakeSinkEngineConnector(List<ColumnInfo> columnInfos,
                                 BitSailConfiguration commonConfiguration,
                                 BitSailConfiguration writerConfiguration) {
    super(commonConfiguration, writerConfiguration);
    this.columnInfos = columnInfos;
    updatedColumns = Lists.newArrayList();
    deletedColumns = Lists.newArrayList();
    addedColumns = Lists.newArrayList();
  }

  @Override
  public String getExternalEngineName() {
    return "fake";
  }

  @Override
  public List<ColumnInfo> getExternalColumnInfos() throws Exception {
    return columnInfos;
  }

  @Override
  public TypeInfoConverter createTypeInfoConverter() {
    return new FileMappingTypeInfoConverter(getExternalEngineName());
  }

  @Override
  public void addColumns(List<ColumnInfo> columnsToAdd) throws Exception {
    addedColumns = columnsToAdd;
  }

  @Override
  public void deleteColumns(List<ColumnInfo> columnsToDelete) throws Exception {
    deletedColumns = columnsToDelete;
  }

  @Override
  public void updateColumns(List<ColumnInfo> columnsToUpdate) throws Exception {
    updatedColumns = columnsToUpdate;
  }

  @Override
  public boolean isTypeCompatible(String newer, String older) {
    return false;
  }

  @Override
  public List<String> getExcludedColumnInfos() throws Exception {
    return Lists.newArrayList();
  }
}
