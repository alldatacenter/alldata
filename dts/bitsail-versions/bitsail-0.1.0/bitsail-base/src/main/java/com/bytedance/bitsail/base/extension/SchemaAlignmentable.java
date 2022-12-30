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

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.ExternalEngineConnector;

import java.io.Serializable;

/**
 * Created 2022/5/23
 */
public interface SchemaAlignmentable extends Serializable {

  /**
   * create a connector for operating schema of the reader/writer
   *
   * @param executionEnviron  execution environment
   * @param selfConfiguration configuration for the reader/writer
   */
  ExternalEngineConnector createExternalEngineConnector(ExecutionEnviron executionEnviron,
                                                        BitSailConfiguration selfConfiguration);

  /**
   * if the reader/writer support column mapping check when finishing schema alignment
   */
  default boolean isSchemaComparable() {
    return false;
  }
}
