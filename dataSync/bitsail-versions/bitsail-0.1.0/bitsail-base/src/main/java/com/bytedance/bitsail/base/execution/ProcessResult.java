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

package com.bytedance.bitsail.base.execution;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Setter
public class ProcessResult<EngineResult> {
  EngineResult jobExecutionResult;
  int taskRetryCount;
  long jobSuccessInputRecordCount;
  long jobSuccessInputRecordBytes;
  long jobFailedInputRecordCount;
  long jobSuccessOutputRecordCount;
  long jobSuccessOutputRecordBytes;
  long jobFailedOutputRecordCount;
  List<String> inputDirtyRecords;
  List<String> outputDirtyRecords;
  String instanceId;

  /**
   * Add dirty records to input dirty records list. Input dirty records list will be initialized if it is null.
   *
   * @param dirtyRecords Dirty records to add.
   */
  public void addInputDirtyRecords(List<String> dirtyRecords) {
    if (inputDirtyRecords == null) {
      inputDirtyRecords = new ArrayList<>();
    }
    if (CollectionUtils.isNotEmpty(dirtyRecords)) {
      inputDirtyRecords.addAll(dirtyRecords);
    }
  }

  /**
   * Add dirty records to output dirty records list. Output dirty records list will be initialized if it is null.
   *
   * @param dirtyRecords Dirty records to add.
   */
  public void addOutputDirtyRecords(List<String> dirtyRecords) {
    if (outputDirtyRecords == null) {
      outputDirtyRecords = new ArrayList<>();
    }
    if (CollectionUtils.isNotEmpty(dirtyRecords)) {
      outputDirtyRecords.addAll(dirtyRecords);
    }
  }
}
