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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor;

import java.io.Serializable;

/**
 * Created 2020/9/10.
 * custom extractor need to implement the interface
 */
public interface EventTimeExtractor extends Serializable {
  /**
   * parse byte[] to message object
   *
   * @param record record bytes
   * @return raw record object
   */
  Object parse(byte[] record) throws Exception;

  /**
   * extract event time from a message object
   *
   * @param record record to extract event timestamp
   * @return event timestamp
   */
  long extract(Object record);

  /**
   * get value of dynamic partition key
   *
   * @param record       raw record
   * @param fieldName,   not support nested field yet
   * @param defaultValue default value for the field
   * @return dynamic partition value
   */
  String getField(Object record, String fieldName, String defaultValue) throws Exception;
}
