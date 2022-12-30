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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.elasticsearch.doc.parameter.EsDocParameters;
import com.bytedance.bitsail.connector.elasticsearch.doc.tools.EsKeySelector;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.DEFAULT_VERSION;
import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.OPERATION_TYPE_CREATE;
import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.OPERATION_TYPE_DELETE;
import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.OPERATION_TYPE_INDEX;
import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.OPERATION_TYPE_UPDATE;
import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.OPERATION_TYPE_UPSERT;

public class EsRequestConstructor implements Serializable {

  private final String esIndex;
  private final String defaultOperationType;

  private final Integer dynamicFieldIndex;
  private final Integer opTypeIndex;
  private final Integer versionIndex;

  private final EsKeySelector docIdSelector;
  private final EsKeySelector routingSelector;
  private final EsDocConstructor esDocConstructor;

  public EsRequestConstructor(BitSailConfiguration jobConf,
                              EsDocParameters esDocParameters) {
    this.esIndex = jobConf.get(ElasticsearchWriterOptions.ES_INDEX);
    this.defaultOperationType = jobConf.get(ElasticsearchWriterOptions.ES_OPERATION_TYPE);

    this.dynamicFieldIndex = esDocParameters.getDynamicFieldIndex();
    this.opTypeIndex = esDocParameters.getOpTypeIndex();
    this.versionIndex = esDocParameters.getVersionIndex();

    final List<Integer> docIdFieldsIndices = esDocParameters.getIdFieldsIndices();
    final List<Integer> routingFieldsIndices = esDocParameters.getRoutingFieldsIndices();
    this.docIdSelector = new EsKeySelector(docIdFieldsIndices, esDocParameters.getIdDelimiter());
    this.routingSelector = new EsKeySelector(routingFieldsIndices, esDocParameters.getIdDelimiter());
    this.esDocConstructor = new EsDocConstructor(esDocParameters);
  }

  private String getIndex(Row row) {
    return Objects.isNull(dynamicFieldIndex) ? esIndex : String.valueOf(row.getField(dynamicFieldIndex));
  }

  private String getOpType(Row row) {
    return Objects.isNull(opTypeIndex) ? defaultOperationType : String.valueOf(row.getField(opTypeIndex));
  }

  private long getVersion(Row row) {
    return Objects.isNull(versionIndex) ? DEFAULT_VERSION : Long.parseLong(row.getField(versionIndex).toString());
  }

  public ActionRequest createRequest(Row row) {
    String index = getIndex(row);
    String opType = getOpType(row);
    String id = docIdSelector.getKey(row);
    String source = esDocConstructor.form(row);
    String routing = routingSelector.getKey(row);
    long version = getVersion(row);

    return createRequest(index, opType, id, source, routing, version);
  }

  private ActionRequest createRequest(String index, String opType, String id, String source, String routing, long version) {
    ActionRequestValidationException validationException;
    switch (opType.toLowerCase().trim()) {
      case OPERATION_TYPE_DELETE:
        DeleteRequest deleteRequest = new DeleteRequest().index(index).id(id);
        if (version > 0) {
          deleteRequest.version(version).versionType(VersionType.EXTERNAL_GTE);
        }
        if (!Strings.isEmpty(routing)) {
          deleteRequest.routing(routing);
        }

        validationException = deleteRequest.validate();
        if (validationException != null) {
          throw new BitSailException(CommonErrorCode.RUNTIME_ERROR, String.format(
              "oops! construct delete request failed! request is: %s,  reason is: %s",
              deleteRequest, validationException));
        }
        return deleteRequest;
      case OPERATION_TYPE_INDEX:
        IndexRequest indexRequest = new IndexRequest().index(index).id(id);
        if (StringUtils.isEmpty(source)) {
          throw new BitSailException(CommonErrorCode.RUNTIME_ERROR, String.format(
              "oops! construct index request failed, because _doc is empty!, request is : %s", indexRequest));
        }
        indexRequest.source(source, XContentType.JSON);
        if (version > 0) {
          indexRequest.version(version).versionType(VersionType.EXTERNAL_GTE);
        }
        if (!Strings.isEmpty(routing)) {
          indexRequest.routing(routing);
        }
        validationException = indexRequest.validate();
        if (validationException != null) {
          throw new BitSailException(CommonErrorCode.RUNTIME_ERROR, String.format(
              "oops! construct index request failed! request is: %s,  reason is: %s",
              indexRequest, validationException));
        }
        return indexRequest;
      case OPERATION_TYPE_CREATE:
        IndexRequest createRequest = new IndexRequest().index(index).id(id);
        createRequest.create(true);
        if (StringUtils.isEmpty(source)) {
          throw new BitSailException(CommonErrorCode.RUNTIME_ERROR, String.format(
              "oops! construct create request failed, because _doc is empty!, request is : %s", createRequest));
        }
        createRequest.source(source, XContentType.JSON);
        if (version > 0) {
          createRequest.version(version).versionType(VersionType.EXTERNAL_GTE);
        }
        if (!Strings.isEmpty(routing)) {
          createRequest.routing(routing);
        }
        validationException = createRequest.validate();
        if (validationException != null) {
          throw new BitSailException(CommonErrorCode.RUNTIME_ERROR, String.format(
              "oops! construct create request failed! request is: %s,  reason is: %s",
              createRequest, validationException));
        }
        return createRequest;
      case OPERATION_TYPE_UPDATE:
        UpdateRequest updateRequest = new UpdateRequest().index(index).id(id);
        if (source != null) {
          updateRequest.doc(source, XContentType.JSON);
        }
        if (!Strings.isEmpty(routing)) {
          updateRequest.routing(routing);
        }
        validationException = updateRequest.validate();
        if (validationException != null) {
          throw new BitSailException(CommonErrorCode.RUNTIME_ERROR, String.format(
              "oops! construct update request failed! request is: %s,  reason is: %s",
              updateRequest, validationException));
        }
        return updateRequest;
      case OPERATION_TYPE_UPSERT:
        UpdateRequest upsertRequest = new UpdateRequest().index(index).id(id);
        if (source != null) {
          upsertRequest.doc(source, XContentType.JSON);
          upsertRequest.docAsUpsert(true);
        }
        if (!Strings.isEmpty(routing)) {
          upsertRequest.routing(routing);
        }
        validationException = upsertRequest.validate();
        if (upsertRequest.validate() != null) {
          throw new BitSailException(CommonErrorCode.RUNTIME_ERROR, String.format(
              "oops! construct upsert request failed! request is: %s,  reason is: %s",
              upsertRequest, validationException));
        }
        return upsertRequest;
      default:
        throw new RuntimeException("addCustomRequestToIndexer can not reach it");
    }
  }
}
