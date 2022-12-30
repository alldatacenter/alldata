/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.base.constants;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public enum ClientMetricName {
  METRIC_JOB_START("start"),

  METRIC_JOB_SUCCESS("success"),

  METRIC_JOB_TERMINAL("terminal"),

  METRIC_JOB_FAILED("fail"),

  METRIC_JOB_DURATION("duration"),

  METRIC_JOB_TASK_RETRY_COUNT("taskRetryCount"),

  METRIC_JOB_RUNNING_TIME("jobRunningTime"),

  METRIC_JOB_SUCCESS_INPUT_RECORD_COUNT("successInputRecordCount"),

  METRIC_JOB_SUCCESS_INPUT_RECORD_BYTES("successInputRecordBytes"),

  METRIC_JOB_FAILED_INPUT_RECORD_COUNT("failedInputRecordCount"),

  METRIC_JOB_SUCCESS_OUTPUT_RECORD_COUNT("successOutputRecordCount"),

  METRIC_JOB_SUCCESS_OUTPUT_RECORD_BYTES("successOutputRecordBytes"),

  METRIC_JOB_FAILED_OUTPUT_RECORD_COUNT("failedOutputRecordCount"),
  ;

  @Getter
  String name;

  ClientMetricName(String name) {
    this.name = name;
  }

  String getFullName(String prefix, String tag) {
    StringBuilder builder = new StringBuilder();
    if (StringUtils.isNotEmpty(prefix)) {
      builder.append(prefix)
          .append(".");
    }
    builder.append(this.name);
    if (StringUtils.isNotEmpty(tag)) {
      builder.append(tag);
    }
    return builder.toString();
  }
}
