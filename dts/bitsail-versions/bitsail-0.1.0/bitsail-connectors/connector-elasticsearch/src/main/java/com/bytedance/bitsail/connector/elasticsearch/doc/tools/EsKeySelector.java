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

package com.bytedance.bitsail.connector.elasticsearch.doc.tools;

import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.elasticsearch.base.EsConstants;

import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
public class EsKeySelector implements Serializable {

  private final List<Integer> fieldsIndices;
  private final String delimiter;

  public String getKey(Row row) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < fieldsIndices.size(); i++) {
      if (i > 0) {
        builder.append(delimiter);
      }
      final Object value = row.getField(fieldsIndices.get(i));
      if (value == null) {
        builder.append(EsConstants.KEY_NULL_LITERAL);
      } else {
        builder.append(value);
      }
    }
    return builder.toString();
  }
}
