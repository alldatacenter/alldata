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

package com.bytedance.bitsail.connector.elasticsearch.doc;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.elasticsearch.doc.parameter.EsDocParameters;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EsDocConstructor {

  private final EsDocParameters esDocParameters;

  public EsDocConstructor(EsDocParameters esDocParameters) {
    this.esDocParameters = esDocParameters;
  }

  public String form(Row row) {
    List<ColumnInfo> columns = esDocParameters.getColumns();
    boolean ignoreBlankValue = esDocParameters.isIgnoreBlankValue();
    List<Integer> excludeFieldsIndices = esDocParameters.getExcludedFieldsIndices();
    Integer dynamicFieldIndex = esDocParameters.getDynamicFieldIndex();
    boolean flattenMap = esDocParameters.isFlattenMap();
    List<SerializerFeature> jsonFeatures = esDocParameters.getJsonFeatures();

    JSONObject jsonObject = new JSONObject();
    String columnName;
    String columnType;
    Object columnValue;

    Object[] fields = row.getFields();
    for (int i = 0; i < fields.length; i++) {
      columnName = columns.get(i).getName();
      columnType = columns.get(i).getType();
      columnValue = fields[i];

      if (ignoreBlankValue && isBlankValue(columnValue)) {
        continue;
      }

      if (!excludeFieldsIndices.isEmpty() && excludeFieldsIndices.contains(i)) {
        continue;
      }

      if (dynamicFieldIndex != null && dynamicFieldIndex == i) {
        continue;
      }

      if (flattenMap && Objects.nonNull(columnValue) && columnType.trim().toUpperCase().startsWith("MAP")) {
        ((Map<?, ?>) columnValue).forEach((k, v) -> {
          if (ignoreBlankValue && isBlankValue(v)) {
            return;
          }
          jsonObject.put(k.toString(), v);
        });
        continue;
      }

      jsonObject.put(columnName, columnValue);
    }

    String doc = JSONObject.toJSONString(jsonObject, jsonFeatures.toArray(new SerializerFeature[0]));

    return StringUtils.isEmpty(doc) ? null : doc;
  }

  private boolean isBlankValue(Object columnValue) {
    if (Objects.nonNull(columnValue) && columnValue instanceof Number) {
      return ((Number) columnValue).intValue() == 0;
    }
    return false;
  }
}
