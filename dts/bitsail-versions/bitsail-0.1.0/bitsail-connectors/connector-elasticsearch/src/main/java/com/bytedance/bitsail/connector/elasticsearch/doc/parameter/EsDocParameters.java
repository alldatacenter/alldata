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

package com.bytedance.bitsail.connector.elasticsearch.doc.parameter;

import com.bytedance.bitsail.common.model.ColumnInfo;

import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class EsDocParameters implements Serializable {
  /**
   * The indices of record fields that are used to producing document '_id'.
   */
  private List<Integer> idFieldsIndices;

  /**
   * The indices of record fields that will not be inserted into Elasticsearch document.
   */
  private List<Integer> excludedFieldsIndices;

  /**
   * The indices of record fields that are used for routing.
   */
  private List<Integer> routingFieldsIndices;

  /**
   * Delimiter used for construct composite document '_id' and routing id.
   */
  private String idDelimiter;

  /**
   * The index of dynamic field in records.
   * We use dynamic field to determine what Elasticsearch index to insert for a record.
   * Notice that, dynamic field will not be included in document.
   */
  private Integer dynamicFieldIndex;

  /**
   * The index of operation type.
   */
  private Integer opTypeIndex;

  /**
   * The index of version.
   */
  private Integer versionIndex;

  /**
   * Option for determining if empty fields in record should be included in document.
   */
  private boolean ignoreBlankValue;

  /**
   * Option for determining if map entries should be expanded in document.
   */
  private boolean flattenMap;

  /**
   * User defined json features for creating json format document.
   */
  private List<SerializerFeature> jsonFeatures;

  /**
   * Used to format json document.
   */
  private List<ColumnInfo> columns;
}
