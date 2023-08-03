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

package com.netease.arctic.optimizing;

import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;

import java.util.Map;

public class RewriteFilesOutput implements TableOptimizing.OptimizingOutput {
  private final DataFile[] dataFiles;
  private final DeleteFile[] deleteFiles;
  private final Map<String, String> summary;

  public RewriteFilesOutput(DataFile[] dataFiles, DeleteFile[] deleteFiles, Map<String, String> summary) {
    this.dataFiles = dataFiles;
    this.deleteFiles = deleteFiles;
    this.summary = summary;
  }

  public DataFile[] getDataFiles() {
    return dataFiles;
  }

  public DeleteFile[] getDeleteFiles() {
    return deleteFiles;
  }

  @Override
  public Map<String, String> summary() {
    return summary;
  }

  @Override
  public String toString() {
    return "RewriteFilesOutput{" +
        "dataFilesSize=" + (dataFiles == null ? 0 : dataFiles.length) +
        ", deleteFilesSize=" + (deleteFiles == null ? 0 : deleteFiles.length) +
        ", summary=" + summary +
        '}';
  }
}
