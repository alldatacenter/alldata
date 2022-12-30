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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMeta;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * Created 2020/12/10.
 */
@Getter
@EqualsAndHashCode(of = {"columns", "columnTypes"})
@ToString(of = {"columns", "columnTypes"})
public class HiveMeta extends FileSystemMeta {
  String columns;
  String columnTypes;
  String inputFormat;
  String outputFormat;
  String serializationLib;
  Map<String, String> serdeParameters;

  public HiveMeta(String columns,
                  String columnTypes,
                  String inputFormat,
                  String outputFormat,
                  String serializationLib,
                  Map<String, String> serdeParameters) {
    super(System.currentTimeMillis());
    this.columns = columns;
    this.columnTypes = columnTypes;
    this.inputFormat = inputFormat;
    this.outputFormat = outputFormat;
    this.serializationLib = serializationLib;
    this.serdeParameters = serdeParameters;
  }
}
