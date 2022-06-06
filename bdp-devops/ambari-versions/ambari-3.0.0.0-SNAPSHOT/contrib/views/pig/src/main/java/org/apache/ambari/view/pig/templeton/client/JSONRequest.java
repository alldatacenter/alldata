/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.pig.templeton.client;

import com.google.gson.Gson;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.ambari.view.AmbariHttpException;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.ambari.AmbariApiException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Request handler, supports GET, POST, PUT, DELETE methods
 * @param <RESPONSE> data type to deserialize response from JSON
 */
public class JSONRequest<RESPONSE> {
  protected final Class<RESPONSE> responseClass;
  protected final ViewContext context;
  protected final WebResource resource;
  private String username;
  private String doAs;

  protected final Gson gson = new Gson();

  protected final static Logger LOG =
      LoggerFactory.getLogger(JSONRequest.class);

  /**
   * Constructor
   * @param resource object that represents resource
   * @param responseClass model class
   * @param username will be used to auth as proxyuser
   * @param doAs run query from this user
   * @param context View Context instance
   */
  public JSONRequest(WebResource resource, Class<RESPONSE> responseClass, String username, String doAs, ViewContext context) {
    this.resource = resource;
    this.responseClass = responseClass;
    this.username = username;
    this.context = context;
    this.doAs = doAs;
  }

  /**
   * Main implementation of GET request
   * @param resource resource
   * @return unmarshalled response data
   */
  public RESPONSE get(WebResource resource) throws IOException {
    LOG.info("GET {}", resource);

    InputStream inputStream = readFrom(resource, "GET", null, new HashMap<String, String>());

    recordLastCurlCommand(String.format("curl \"" + resource.toString() + "\""));
    String responseJson = IOUtils.toString(inputStream);
    LOG.debug("RESPONSE {}", responseJson);
    return gson.fromJson(responseJson, responseClass);
  }

  /**
   * Make GET request
   * @see #get(WebResource)
   */
  public RESPONSE get() throws IOException {
    return get(this.resource);
  }

  /**
   * Make GET request
   * @see #get(WebResource)
   */
  public RESPONSE get(MultivaluedMapImpl params) throws IOException {
    return get(this.resource.queryParams(params));
  }

  /**
   * Main implementation of POST request
   * @param resource resource
   * @param data post body
   * @return unmarshalled response data
   */
  public RESPONSE post(WebResource resource, MultivaluedMapImpl data) throws IOException {
    LOG.info("POST: {}", resource);
    LOG.debug("data: {}", data);

    StringBuilder curlBuilder = new StringBuilder();

    UriBuilder builder = getUriBuilder(data, curlBuilder);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    recordLastCurlCommand(String.format("curl " + curlBuilder.toString() + " \"" + resource.toString() + "\""));
    InputStream inputStream = readFrom(resource, "POST", builder.build().getRawQuery(), headers);
    String responseJson = IOUtils.toString(inputStream);

    LOG.debug("RESPONSE => {}", responseJson);
    return gson.fromJson(responseJson, responseClass);
  }

  /**
   * @see #post(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE post(MultivaluedMapImpl data) throws IOException {
    return post(resource, data);
  }

  /**
   * @see #post(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE post() throws IOException {
    return post(resource, new MultivaluedMapImpl());
  }

  /**
   * @see #post(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE post(MultivaluedMapImpl params, MultivaluedMapImpl data) throws IOException {
    return post(resource.queryParams(params), data);
  }

  /**
   * Main implementation of PUT request
   * @param resource resource
   * @param data put body
   * @return unmarshalled response data
   */
  public RESPONSE put(WebResource resource, MultivaluedMapImpl data) throws IOException {
    LOG.info("PUT {}", resource);

    StringBuilder curlBuilder = new StringBuilder();

    UriBuilder builder = getUriBuilder(data, curlBuilder);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    recordLastCurlCommand(String.format("curl -X PUT " + curlBuilder.toString() + " \"" + resource.toString() + "\""));

    InputStream inputStream = readFrom(resource, "PUT", builder.build().getRawQuery(), headers);
    String responseJson = IOUtils.toString(inputStream);

    LOG.debug("RESPONSE => {}", responseJson);
    return gson.fromJson(responseJson, responseClass);
  }

  public UriBuilder getUriBuilder(MultivaluedMapImpl data, StringBuilder curlBuilder) {
    MultivaluedMapImpl effectiveData;
    if (data == null)
      effectiveData = new MultivaluedMapImpl();
    else
      effectiveData = new MultivaluedMapImpl(data);

    effectiveData.putSingle("user.name", username);
    effectiveData.putSingle("doAs", doAs);

    UriBuilder builder = UriBuilder.fromPath("host/");
    for(String key : effectiveData.keySet()) {
      for(String value : effectiveData.get(key)) {
        builder.queryParam(key, value);
        curlBuilder.append(String.format("-d %s=\"%s\" ", key, value.replace("\"", "\\\"")));
      }
    }

    if (data != null)
      LOG.debug("data: {}", builder.build().getRawQuery());
    return builder;
  }

  /**
   * @see #put(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE put(MultivaluedMapImpl data) throws IOException {
    return put(resource, data);
  }

  /**
   * @see #put(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE put() throws IOException {
    return put(resource, new MultivaluedMapImpl());
  }

  /**
   * @see #put(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE put(MultivaluedMapImpl params, MultivaluedMapImpl data) throws IOException {
    return put(resource.queryParams(params), data);
  }

  /**
   * Main implementation of DELETE request
   * @param resource resource
   * @param data delete body
   * @return unmarshalled response data
   */
  public RESPONSE delete(WebResource resource, MultivaluedMapImpl data) throws IOException {
    LOG.info("DELETE {}", resource.toString());

    StringBuilder curlBuilder = new StringBuilder();

    UriBuilder builder = getUriBuilder(data, curlBuilder);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    recordLastCurlCommand(String.format("curl -X DELETE " + curlBuilder.toString() + " \"" + resource.toString() + "\""));

    InputStream inputStream = readFrom(resource, "DELETE", builder.build().getRawQuery(), headers);
    String responseJson = IOUtils.toString(inputStream);

    LOG.debug("RESPONSE => {}", responseJson);
    return gson.fromJson(responseJson, responseClass);
  }

  /**
   * @see #delete(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE delete(MultivaluedMapImpl data) throws IOException {
    return delete(resource, data);
  }

  /**
   * @see #delete(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE delete() throws IOException {
    return delete(resource, new MultivaluedMapImpl());
  }

  /**
   * @see #delete(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE delete(MultivaluedMapImpl params, MultivaluedMapImpl data) throws IOException {
    return delete(resource.queryParams(params), data);
  }

  private static void recordLastCurlCommand(String curl) {
    LOG.info(curl);
  }

  public InputStream readFrom(WebResource resource, String method, String body, Map<String, String> headers) throws IOException {
    HttpURLConnection connection;
    resource = resource.queryParam("user.name", username)
        .queryParam("doAs", doAs);

    String path = resource.toString();
    if (doAs == null) {
      connection = context.getURLConnectionProvider().getConnection(path,
          method, body, headers);
    } else {
      connection = context.getURLConnectionProvider().getConnectionAs(path,
          method, body, headers, doAs);
    }
    return getInputStream(connection);
  }

  private InputStream getInputStream(HttpURLConnection connection) throws IOException {
    int responseCode = connection.getResponseCode();
    if (responseCode >= Response.Status.BAD_REQUEST.getStatusCode()) {
      String message = connection.getResponseMessage();
      if (connection.getErrorStream() != null) {
        message = IOUtils.toString(connection.getErrorStream());
      }
      LOG.error("Got error response for url {}. Response code:{}. {}", connection.getURL(), responseCode, message);
      throw new AmbariApiException(message);
    }
    return connection.getInputStream();
  }

}
