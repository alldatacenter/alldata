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

package com.bytedance.bitsail.common.ddl.sink;

import com.bytedance.bitsail.common.ddl.ExternalEngineConnector;
import com.bytedance.bitsail.common.model.ColumnInfo;

import java.util.List;

public interface SinkEngineConnector extends ExternalEngineConnector {

  void addColumns(List<ColumnInfo> columnsToAdd) throws Exception;

  void deleteColumns(List<ColumnInfo> columnsToDelete) throws Exception;

  void updateColumns(List<ColumnInfo> columnsToUpdate) throws Exception;

  boolean isTypeCompatible(String newer, String older);

  /**
   * get columns which in the excluded list.
   */
  List<String> getExcludedColumnInfos() throws Exception;
}
