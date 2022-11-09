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

package com.bytedance.bitsail.connector.doris.sink.streamload;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.doris.config.DorisExecutionOptions;
import com.bytedance.bitsail.connector.doris.config.DorisOptions;
import com.bytedance.bitsail.connector.doris.error.DorisErrorCode;
import com.bytedance.bitsail.connector.doris.http.RespContent;
import com.bytedance.bitsail.connector.doris.partition.DorisPartition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DorisStreamLoad {
  private static final Logger LOG = LoggerFactory.getLogger(DorisStreamLoad.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String STREAM_LOAD_URL_FORMAT = "http://%s/api/%s/%s/_stream_load?";
  private static final int SUCCESS_STATUS_CODE = 200;

  protected DorisExecutionOptions executionOptions;
  protected DorisOptions dorisOptions;
  protected String authEncoding;
  protected CloseableHttpClient httpClient;
  protected final HttpClientBuilder httpClientBuilder = HttpClients
      .custom()
      .setRedirectStrategy(new DefaultRedirectStrategy() {
        @Override
        protected boolean isRedirectable(String method) {
          return true;
        }
      });
  public DorisStreamLoad(DorisExecutionOptions executionOptions, DorisOptions dorisOptions) {
    this.executionOptions = executionOptions;
    this.dorisOptions = dorisOptions;
    this.authEncoding = basicAuthHeader(dorisOptions.getUsername(), dorisOptions.getPassword());
    this.httpClient = httpClientBuilder.build();
  }

  public void load(String value, DorisOptions options, boolean isTemp) throws BitSailException {
    LoadResponse loadResponse = loadBatch(value, options, isTemp);
    LOG.info("StreamLoad Response:{}", loadResponse);
    if (loadResponse.status != SUCCESS_STATUS_CODE) {
      throw new BitSailException(DorisErrorCode.LOAD_FAILED, "stream load error: " + loadResponse.respContent);
    } else {
      try {
        RespContent respContent = OBJECT_MAPPER.readValue(loadResponse.respContent, RespContent.class);
        if (!LoadStatus.SUCCESS.equals(respContent.getStatus())) {
          String errMsg = String.format("stream load error: %s, see more in %s, load value string: %s",
              respContent.getMessage(), respContent.getErrorURL(), value);
          throw new BitSailException(DorisErrorCode.LOAD_FAILED, errMsg);
        }
      } catch (IOException e) {
        throw new BitSailException(DorisErrorCode.LOAD_FAILED, e.getMessage());
      }
    }
  }

  private LoadResponse loadBatch(String value, DorisOptions options, boolean isTemp) {
    String label = executionOptions.getStreamLoadProp().getProperty("label");
    if (StringUtils.isBlank(label)) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
      String formatDate = sdf.format(new Date());
      label = String.format("bitsail_doris_connector_%s_%s", formatDate,
          UUID.randomUUID().toString().replaceAll("-", ""));
    }

    try {
      HttpPut put = new HttpPut(getRandomUrl(isTemp));
      if (options.getLoadDataFormat().equals(DorisOptions.LOAD_CONTENT_TYPE.JSON)) {
        put.setHeader("format", "json");
        put.setHeader("strip_outer_array", "true");
      } else if (options.getLoadDataFormat().equals(DorisOptions.LOAD_CONTENT_TYPE.CSV)) {
        put.setHeader("format", "csv");
        put.setHeader("column_separator", options.getFieldDelimiter());
      }

      if (isTemp && dorisOptions.isTableHasPartitions()) {
        String tempPartitions = dorisOptions.getPartitions().stream().map(DorisPartition::getTempName).collect(Collectors.joining(","));
        put.setHeader("temporary_partitions", tempPartitions);
      }

      put.setHeader(HttpHeaders.EXPECT, "100-continue");
      //set column meta info
      List<String> columnNames = new ArrayList<>();
      for (ColumnInfo columnInfo : options.getColumnInfos()) {
        columnNames.add(columnInfo.getName());
      }
      put.setHeader("columns", String.join(",", columnNames));
      put.setHeader(HttpHeaders.AUTHORIZATION, this.authEncoding);
      put.setHeader("label", label);
      for (Map.Entry<Object, Object> entry : executionOptions.getStreamLoadProp().entrySet()) {
        put.setHeader(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }
      StringEntity entity = new StringEntity(value, "UTF-8");
      put.setEntity(entity);

      try (CloseableHttpResponse response = httpClient.execute(put)) {
        final int statusCode = response.getStatusLine().getStatusCode();
        final String reasonPhrase = response.getStatusLine().getReasonPhrase();
        String loadResult = "";
        if (response.getEntity() != null) {
          loadResult = EntityUtils.toString(response.getEntity());
        }
        return new LoadResponse(statusCode, reasonPhrase, loadResult);
      }
    } catch (Exception e) {
      String err = "failed to stream load data with label: " + label;
      LOG.warn(err, e);
      return new LoadResponse(-1, e.getMessage(), err);
    }
  }

  private String getRandomUrl(boolean isTemp) {
    if (StringUtils.isEmpty(dorisOptions.getFeNodes())) {
      throw new IllegalArgumentException("Empty Fe nodes found!");
    }
    List<String> nodes = Arrays.stream(dorisOptions.getFeNodes().split(",")).map(String::trim).collect(Collectors.toList());
    Collections.shuffle(nodes);
    String tableName = dorisOptions.getTableName();
    if (isTemp && !dorisOptions.isTableHasPartitions()) {
      tableName = dorisOptions.getTmpTableName();
    }
    return String.format(STREAM_LOAD_URL_FORMAT, nodes.get(0), dorisOptions.getDatabaseName(), tableName);
  }

  protected String basicAuthHeader(String username, String password) {
    final String tobeEncode = username + ":" + password;
    byte[] encoded = Base64.encodeBase64(tobeEncode.getBytes(StandardCharsets.UTF_8));
    return "Basic " + new String(encoded);
  }

  @VisibleForTesting
  public DorisStreamLoad() {}

  public static class LoadResponse {
    public int status;
    public String respMsg;
    public String respContent;

    public LoadResponse(int status, String respMsg, String respContent) {
      this.status = status;
      this.respMsg = respMsg;
      this.respContent = respContent;
    }

    @Override
    public String toString() {
      try {
        return OBJECT_MAPPER.writeValueAsString(this);
      } catch (JsonProcessingException e) {
        return "";
      }
    }
  }
}

