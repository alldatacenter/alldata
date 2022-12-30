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

package com.bytedance.bitsail.base.extension;

import com.bytedance.bitsail.base.execution.ProcessResult;

import java.io.Serializable;

/**
 * Created 2022/4/24
 */
public interface GlobalCommittable extends Serializable {

  /**
   * commit(processResult) will be called when a job successes
   * e.g. Hive writer will move temp files into formal hive path and then add partition
   *
   * @param processResult job execution result
   */
  void commit(ProcessResult<?> processResult) throws Exception;

  /**
   * abort() will be called for handling failure when a job fails
   * e.g. JDBC writer will delete inserted data when the job fails
   */
  void abort() throws Exception;

  /**
   * onDestroy() will be called when a job finished (in both success and fail scenario)
   * e.g. Hive writer will delete temp files when the job finishes
   */
  default void onDestroy() throws Exception {

  }
}
