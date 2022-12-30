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

package com.bytedance.bitsail.connector.legacy.jdbc.constants;

import java.io.IOException;
import java.io.Serializable;

public interface WriteModeProxy extends Serializable {

  /**
   * Preparation process in task manager.
   */
  default void prepareOnTM() {

  }

  /**
   * Preparation process in flink client.
   */
  default void prepareOnClient() throws IOException {

  }

  /**
   * Process function in flink client when job fails.
   */
  default void onFailureComplete() throws IOException {

  }

  /**
   * Add some extra fields to record to write to JDBC.
   *
   * @param index Index of last field.
   */
  default void afterWriteRecord(int index) {

  }

  enum WriteMode {

    /**
     * Clear old partition and then add records.
     */
    insert,

    /**
     * Upsert records.
     */
    overwrite,
  }
}
