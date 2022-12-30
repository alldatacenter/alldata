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

package com.bytedance.bitsail.connector.elasticsearch.rest;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.connector.elasticsearch.doc.EsRequestConstructor;
import com.bytedance.bitsail.connector.elasticsearch.doc.parameter.EsDocParameters;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EsRequestEmitter {

  private final EsRequestConstructor constructor;

  public EsRequestEmitter(BitSailConfiguration jobConf) {
    EsDocParameters parameters = initEsDocParams(jobConf);
    this.constructor = new EsRequestConstructor(jobConf, parameters);
  }

  @VisibleForTesting
  public static EsDocParameters initEsDocParams(BitSailConfiguration jobConf) {
    String esIdFields = jobConf.get(ElasticsearchWriterOptions.ES_ID_FIELDS);
    String idDelimiter = jobConf.get(ElasticsearchWriterOptions.DOC_ID_DELIMITER);
    String docExcludeFields = jobConf.get(ElasticsearchWriterOptions.DOC_EXCLUDE_FIELDS);
    String routingFields = jobConf.get(ElasticsearchWriterOptions.SHARD_ROUTING_FIELDS);

    boolean ignoreBlankValue = jobConf.get(ElasticsearchWriterOptions.IGNORE_BLANK_VALUE);
    boolean flattenMap = jobConf.get(ElasticsearchWriterOptions.FLATTEN_MAP);
    List<SerializerFeature> jsonFeatures = FastJsonUtil.parseSerializerFeaturesFromConfig(
        jobConf.get(ElasticsearchWriterOptions.JSON_SERIALIZER_FEATURES));

    List<ColumnInfo> columns = jobConf.getNecessaryOption(ElasticsearchWriterOptions.COLUMNS, CommonErrorCode.LACK_NECESSARY_FIELDS);

    List<Integer> idFieldsIndices = new ArrayList<>();
    List<Integer> excludeFieldsIndices = new ArrayList<>();
    List<Integer> routingFieldsIndices = new ArrayList<>();
    if (StringUtils.isNotEmpty(esIdFields)) {
      List<String> fieldsNames = Arrays.asList(esIdFields.split(",\\s*"));
      idFieldsIndices = getFieldsIndices(columns, fieldsNames);
    }

    if (StringUtils.isNotEmpty(docExcludeFields)) {
      List<String> fieldsNames = Arrays.asList(docExcludeFields.split(",\\s*"));
      excludeFieldsIndices = getFieldsIndices(columns, fieldsNames);
    }

    if (StringUtils.isNotEmpty(routingFields)) {
      List<String> fieldsNames = Arrays.asList(routingFields.split(",\\s*"));
      routingFieldsIndices = getFieldsIndices(columns, fieldsNames);
    }

    Integer dynamicFieldIndex = jobConf.fieldExists(ElasticsearchWriterOptions.ES_DYNAMIC_INDEX_FIELD) ?
        getFieldsIndices(columns,
        ImmutableList.of(jobConf.get(ElasticsearchWriterOptions.ES_DYNAMIC_INDEX_FIELD))).get(0)
        : null;

    Integer versionIndex = jobConf.fieldExists(ElasticsearchWriterOptions.ES_VERSION_FIELD) ?
        getFieldsIndices(columns, ImmutableList.of(jobConf.get(ElasticsearchWriterOptions.ES_VERSION_FIELD))).get(0)
        : null;

    Integer opTypeIndex = jobConf.fieldExists(ElasticsearchWriterOptions.ES_OPERATION_TYPE_FIELD) ?
        getFieldsIndices(columns, ImmutableList.of(jobConf.get(ElasticsearchWriterOptions.ES_OPERATION_TYPE_FIELD))).get(0)
        : null;

    return EsDocParameters.builder()
        .idFieldsIndices(idFieldsIndices)
        .excludedFieldsIndices(excludeFieldsIndices)
        .routingFieldsIndices(routingFieldsIndices)
        .dynamicFieldIndex(dynamicFieldIndex)
        .opTypeIndex(opTypeIndex)
        .versionIndex(versionIndex)
        .ignoreBlankValue(ignoreBlankValue)
        .flattenMap(flattenMap)
        .columns(columns)
        .jsonFeatures(jsonFeatures)
        .idDelimiter(idDelimiter)
        .build();
  }

  private static List<Integer> getFieldsIndices(List<ColumnInfo> columns, List<String> fieldNames) {
    Map<String, Integer> columnIndexMap = new HashMap<>();
    for (int i = 0; i < columns.size(); i++) {
      String name = columns.get(i).getName();
      columnIndexMap.put(name, i);
    }

    return fieldNames.stream()
        .map(fieldName -> {
          if (columnIndexMap.containsKey(fieldName)) {
            return columnIndexMap.get(fieldName);
          } else {
            throw new IllegalArgumentException("fieldName: " + fieldName + " is not in columns");
          }
        }).collect(Collectors.toList());
  }

  public void emit(Row row, BulkProcessor bulkProcessor) {
    ActionRequest request = constructor.createRequest(row);
    emit(request, bulkProcessor);
  }

  public void emit(ActionRequest request, BulkProcessor bulkProcessor) {
    if (request instanceof IndexRequest) {
      bulkProcessor.add((IndexRequest) request);
    } else if (request instanceof DeleteRequest) {
      bulkProcessor.add((DeleteRequest) request);
    } else if (request instanceof UpdateRequest) {
      bulkProcessor.add((UpdateRequest) request);
    } else {
      throw new RuntimeException("Unsupported request type: " + request.getClass());
    }
  }
}
