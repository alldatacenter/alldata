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

package com.bytedance.bitsail.common.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * a thread safe http util tool and avoid memory leaking,
 * since content_type is indicated in each method, we do not need to pass content_type to the header
 */
@Slf4j
public class HttpManager {

  /**
   *
   */
  private static final int HTTP_TIMEOUT_MS = 60 * 15 * 1000;

  private static CloseableHttpClient closeableHttpClient;

  static {
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

    RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(HTTP_TIMEOUT_MS)
        .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
        .setSocketTimeout(HTTP_TIMEOUT_MS).build();

    closeableHttpClient = httpClientBuilder.setDefaultRequestConfig(config).build();
  }

  public static CloseableHttpClient getCloseableHttpClient() {
    return closeableHttpClient;
  }

  public static WrappedResponse sendGet(String url, Map<String, String> paramMap, Map<String, String> headers) throws IOException {
    URIBuilder uriBuilder;
    HttpGet httpGet;
    try {
      uriBuilder = new URIBuilder(url);
      if (paramMap != null) {
        paramMap.forEach(uriBuilder::setParameter);
      }
      httpGet = new HttpGet(uriBuilder.build());
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }

    if (headers != null) {
      headers.forEach(httpGet::addHeader);
    }

    CloseableHttpResponse response = closeableHttpClient
        .execute(httpGet);
    return wrapAndClose(response);
  }

  public static CloseableHttpResponse sendPost(String url, String jsonStr, Map<String, String> headers) throws IOException {
    HttpPost httpPost = new HttpPost(url);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      httpPost.addHeader(entry.getKey(), entry.getValue());
    }
    StringEntity entity = new StringEntity(jsonStr, "UTF-8");
    httpPost.setEntity(entity);
    CloseableHttpResponse response = closeableHttpClient
        .execute(httpPost);
    return response;
  }

  public static WrappedResponse sendPost(String url, Map<String, String> headers, Map<String, Object> body, ContentType contentType) throws IOException {

    HttpPost httpPost = new HttpPost(url);

    Header header = new BasicHeader("Content-Type", contentType.getMimeType());
    httpPost.addHeader(header);

    if (headers != null) {
      headers.forEach(httpPost::addHeader);
    }

    if (body != null) {
      StringEntity entity = getEntityByContentType(body, contentType);
      httpPost.setEntity(entity);
    }

    CloseableHttpResponse postResponse = closeableHttpClient
        .execute(httpPost);
    return wrapAndClose(postResponse);
  }

  public static WrappedResponse sendPatch(String url, Map<String, String> headers, Map<String, Object> body, ContentType contentType) throws IOException {

    HttpPatch httpPatch = new HttpPatch(url);

    Header header = new BasicHeader("Content-Type", contentType.getMimeType());
    httpPatch.addHeader(header);

    if (headers != null) {
      headers.forEach(httpPatch::addHeader);
    }

    if (body != null) {
      StringEntity entity = getEntityByContentType(body, contentType);
      httpPatch.setEntity(entity);
    }

    CloseableHttpResponse patchResponse = closeableHttpClient
        .execute(httpPatch);
    return wrapAndClose(patchResponse);
  }

  public static WrappedResponse sendPut(String url, Map<String, String> headers, Map<String, Object> body, ContentType contentType) throws IOException {

    HttpPut httpPut = new HttpPut(url);

    Header header = new BasicHeader("Content-Type", contentType.getMimeType());
    httpPut.addHeader(header);

    if (headers != null) {
      headers.forEach(httpPut::setHeader);
    }

    if (body != null) {
      StringEntity entity = getEntityByContentType(body, contentType);
      httpPut.setEntity(entity);
    }

    CloseableHttpResponse putResponse = closeableHttpClient
        .execute(httpPut);
    return wrapAndClose(putResponse);
  }

  public static WrappedResponse sendDelete(String url, Map<String, String> headers, Map<String, Object> body, ContentType contentType) throws IOException {
    HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);

    Header header = new BasicHeader("Content-Type", contentType.getMimeType());
    httpDelete.addHeader(header);

    if (headers != null) {
      headers.forEach(httpDelete::setHeader);
    }

    if (body != null) {
      StringEntity entity = getEntityByContentType(body, contentType);
      httpDelete.setEntity(entity);
    }

    CloseableHttpResponse response = closeableHttpClient
        .execute(httpDelete);

    return wrapAndClose(response);
  }

  /**
   * generate http request body by contentType
   *
   * @param body
   * @param contentType
   * @return
   */
  public static StringEntity getEntityByContentType(Map<String, Object> body, ContentType contentType) {
    StringEntity entity;
    switch (contentType.getMimeType()) {
      case "application/json":
        entity = new StringEntity(JsonSerializer.serialize(body), "UTF-8");
        break;
      case "application/x-www-form-urlencoded":
        String encodedBody = Arrays.stream(body.entrySet().toArray(new Map.Entry[0]))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
        entity = new StringEntity(encodedBody, "UTF-8");
        break;
      default:
        entity = new StringEntity(JsonSerializer.serialize(body), "UTF-8");
    }
    return entity;
  }

  /**
   * encapsulate the apache httpclient response, and close the response after wrapping
   *
   * @param response
   * @return
   * @throws IOException
   */
  public static WrappedResponse wrapAndClose(CloseableHttpResponse response) throws IOException {
    try {
      int statusCode = response.getStatusLine().getStatusCode();
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      EntityUtils.consume(response.getEntity());
      WrappedResponse wrappedResponse = new WrappedResponse(statusCode, responseString);
      return wrappedResponse;
    } finally {
      response.close();
    }
  }

  /**
   * a useful response to get status code and the http response result
   */
  @Setter
  @Getter
  public static class WrappedResponse {
    private int statusCode;
    private String result;

    public WrappedResponse(int statusCode, String result) {
      this.statusCode = statusCode;
      this.result = result;
    }
  }

  private static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "DELETE";

    public HttpDeleteWithBody(final String uri) {
      super();
      setURI(URI.create(uri));
    }

    public HttpDeleteWithBody(final URI uri) {
      super();
      setURI(uri);
    }

    public HttpDeleteWithBody() {
      super();
    }

    @Override
    public String getMethod() {
      return METHOD_NAME;
    }
  }

}
