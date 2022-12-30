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

package com.bytedance.bitsail.connector.doris.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Properties;

/**
 * JDBC sink batch options.
 */
@AllArgsConstructor
@Builder
@Data
public class DorisExecutionOptions implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final int DEFAULT_CHECK_INTERVAL = 10000;
  public static final int DEFAULT_MAX_RETRY_TIMES = 1;
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
  private static final int DEFAULT_BUFFER_COUNT = 3;
  private final int flushIntervalMs;
  private final int maxRetries;
  private final int bufferSize;
  private final int bufferCount;
  private final String labelPrefix;
  private final boolean isBatch;

  /**
   * Properties for the StreamLoad.
   */
  private final Properties streamLoadProp;

  private final Boolean enableDelete;

  private final WRITE_MODE writerMode;

  public enum WRITE_MODE {
    STREAMING_TWO_PC,
    STREAMING_UPSERT,
    BATCH_REPLACE,
    BATCH_UPSERT
  }
}

