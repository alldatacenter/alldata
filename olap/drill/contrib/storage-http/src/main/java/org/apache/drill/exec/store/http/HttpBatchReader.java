/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.http;

import com.typesafe.config.Config;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.store.easy.json.loader.JsonLoader;
import org.apache.drill.exec.store.easy.json.loader.JsonLoaderImpl.JsonLoaderBuilder;
import org.apache.drill.exec.store.easy.json.loader.JsonLoaderOptions;
import org.apache.drill.exec.store.http.paginator.Paginator;
import org.apache.drill.exec.store.http.util.HttpProxyConfig;
import org.apache.drill.exec.store.http.util.HttpProxyConfig.ProxyBuilder;
import org.apache.drill.exec.store.http.util.SimpleHttp;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;
import org.apache.drill.exec.store.ImplicitColumnUtils.ImplicitColumns;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class HttpBatchReader implements ManagedReader<SchemaNegotiator> {

  private static final String[] STRING_METADATA_FIELDS = {"_response_message", "_response_protocol", "_response_url"};
  private static final String RESPONSE_CODE_FIELD = "_response_code";
  private static final Logger logger = LoggerFactory.getLogger(HttpBatchReader.class);

  private final HttpSubScan subScan;
  private final int maxRecords;
  protected final Paginator paginator;
  protected String baseUrl;
  private JsonLoader jsonLoader;
  private ResultSetLoader resultSetLoader;

  protected ImplicitColumns implicitColumns;


  public HttpBatchReader(HttpSubScan subScan) {
    this.subScan = subScan;
    this.maxRecords = subScan.maxRecords();
    this.baseUrl = subScan.tableSpec().connectionConfig().url();
    this.paginator = null;
  }

  public HttpBatchReader(HttpSubScan subScan, Paginator paginator) {
    this.subScan = subScan;
    this.maxRecords = subScan.maxRecords();
    this.paginator = paginator;
    this.baseUrl = paginator.next();
    logger.debug("Batch reader with URL: {}", this.baseUrl);
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {

    // Result set loader setup
    String tempDirPath = negotiator
        .drillConfig()
        .getString(ExecConstants.DRILL_TMP_DIR);

    HttpUrl url = buildUrl();
    logger.debug("Final URL: {}", url);

    CustomErrorContext errorContext = new ChildErrorContext(negotiator.parentErrorContext()) {
      @Override
      public void addContext(UserException.Builder builder) {
        super.addContext(builder);
        builder.addContext("URL", url.toString());
      }
    };
    negotiator.setErrorContext(errorContext);

    // Http client setup
    SimpleHttp http = SimpleHttp.builder()
      .scanDefn(subScan)
      .url(url)
      .tempDir(new File(tempDirPath))
      .proxyConfig(proxySettings(negotiator.drillConfig(), url))
      .errorContext(errorContext)
      .build();

    // JSON loader setup

    resultSetLoader = negotiator.build();
    if (implicitColumnsAreProjected()) {
      implicitColumns = new ImplicitColumns(resultSetLoader.writer());
      buildImplicitColumns();
    }

    // inStream is expected to be closed by the JsonLoader.
    InputStream inStream = http.getInputStream();
    populateImplicitFieldMap(http);

    try {
      JsonLoaderBuilder jsonBuilder = new JsonLoaderBuilder()
          .implicitFields(implicitColumns)
          .resultSetLoader(resultSetLoader)
          .standardOptions(negotiator.queryOptions())
          .maxRows(maxRecords)
          .dataPath(subScan.tableSpec().connectionConfig().dataPath())
          .errorContext(errorContext)
          .fromStream(inStream);

      if (subScan.tableSpec().connectionConfig().jsonOptions() != null) {
        JsonLoaderOptions jsonOptions = subScan
          .tableSpec()
          .connectionConfig()
          .jsonOptions()
          .getJsonOptions(negotiator.queryOptions());
        jsonBuilder.options(jsonOptions);
      } else {
        jsonBuilder.standardOptions(negotiator.queryOptions());
      }
      jsonLoader = jsonBuilder.build();
    } catch (Throwable t) {

      // Paranoia: ensure stream is closed if anything goes wrong.
      // After this, the JSON loader will close the stream.
      AutoCloseables.closeSilently(inStream);
      AutoCloseables.closeSilently(http);
      throw t;
    }

    // Close the http client
    http.close();
    return true;
  }

  protected void buildImplicitColumns() {
    // Add String fields
    for (String fieldName : STRING_METADATA_FIELDS) {
      implicitColumns.addImplicitColumn(fieldName, MinorType.VARCHAR);
    }
    implicitColumns.addImplicitColumn(RESPONSE_CODE_FIELD, MinorType.INT);
  }

  protected void populateImplicitFieldMap(SimpleHttp http) {
    // If the implicit fields are not projected, skip this step
    if (! implicitColumnsAreProjected()) {
      return;
    }

    implicitColumns.getColumn(STRING_METADATA_FIELDS[0]).setValue(http.getResponseMessage());
    implicitColumns.getColumn(STRING_METADATA_FIELDS[1]).setValue(http.getResponseProtocol());
    implicitColumns.getColumn(STRING_METADATA_FIELDS[2]).setValue(http.getResponseURL());
    implicitColumns.getColumn(RESPONSE_CODE_FIELD).setValue(http.getResponseCode());
  }

  protected boolean implicitColumnsAreProjected() {
    List<SchemaPath> columns = subScan.columns();
    for (SchemaPath path : columns) {
      if (path.nameEquals(STRING_METADATA_FIELDS[0]) ||
        path.nameEquals(STRING_METADATA_FIELDS[1]) ||
        path.nameEquals(STRING_METADATA_FIELDS[2]) ||
        path.nameEquals(RESPONSE_CODE_FIELD)) {
        return true;
      }
    }
    return false;
  }

  protected HttpUrl buildUrl() {
    logger.debug("Building URL from {}", baseUrl);
    HttpApiConfig apiConfig = subScan.tableSpec().connectionConfig();

    // Append table name, if present. When pagination is used, the paginator adds this.
    if (subScan.tableSpec().tableName() != null && paginator == null) {
      baseUrl += subScan.tableSpec().tableName();
    }

    // Add URL Parameters to baseURL
    HttpUrl parsedURL = HttpUrl.parse(baseUrl);
    baseUrl = SimpleHttp.mapURLParameters(parsedURL, subScan.filters());

    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
    if (apiConfig.params() != null && !apiConfig.params().isEmpty() &&
        subScan.filters() != null) {
      addFilters(urlBuilder, apiConfig.params(), subScan.filters());
    }

    // Add limit parameter if defined and limit set
    if (! Strings.isNullOrEmpty(apiConfig.limitQueryParam()) && maxRecords > 0) {
      urlBuilder.addQueryParameter(apiConfig.limitQueryParam(), String.valueOf(maxRecords));
    }

    return urlBuilder.build();
  }

  /**
   * Convert equality filter conditions into HTTP query parameters
   * Parameters must appear in the order defined in the config.
   */
  protected void addFilters(Builder urlBuilder, List<String> params,
      Map<String, String> filters) {

    for (String param : params) {
      String value = filters.get(param);
      if (value != null) {
        urlBuilder.addQueryParameter(param, value);
      }
    }
  }

  protected HttpProxyConfig proxySettings(Config drillConfig, HttpUrl url) {
    final HttpStoragePluginConfig config = subScan.tableSpec().config();
    final ProxyBuilder builder = HttpProxyConfig.builder()
        .fromConfigForURL(drillConfig, url.toString());
    final String proxyType = config.proxyType();
    if (proxyType != null && !"direct".equals(proxyType)) {
      UsernamePasswordCredentials credentials = config.getUsernamePasswordCredentials();
      builder
        .type(config.proxyType())
        .host(config.proxyHost())
        .port(config.proxyPort())
        .username(credentials.getUsername())
        .password(credentials.getPassword());
    }
    return builder.build();
  }

  @Override
  public boolean next() {
    boolean result = jsonLoader.readBatch();

    // Allows limitless pagination.
    if (paginator != null &&
      maxRecords < 0 && (resultSetLoader.totalRowCount()) < paginator.getPageSize()) {
      logger.debug("Partially filled page received, ending pagination");
      paginator.notifyPartialPage();
    }
    return result;
  }

  @Override
  public void close() {
    if (jsonLoader != null) {
      jsonLoader.close();
      jsonLoader = null;
    }
  }
}
