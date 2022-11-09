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

package com.bytedance.bitsail.component.format.api;

import com.bytedance.bitsail.common.BitSailException;

import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.io.Serializable;

/**
 *
 */
public interface RowBuilder extends Serializable {

  /**
   * build a bitsail row, the row would be reused in the next iteration
   *
   * @throws BitSailException
   */
  void build(Object value, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo) throws BitSailException;

  default void build(Object value, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo, int[] fieldIndexes) throws BitSailException {
    build(value, reuse, mandatoryEncoding, rowTypeInfo);
  }

  /**
   * @param value       Raw data to transform to 'bitsail rows'.
   * @param reuse       The transformed `bitsail row`.
   * @param rowTypeInfo Determine the format (field name, data types) of the transformed row.
   */
  default void build(Object value, Row reuse, RowTypeInfo rowTypeInfo) throws BitSailException {
    build(value, reuse, "UTF-8", rowTypeInfo);
  }
}
