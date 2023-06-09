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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import okhttp3.HttpUrl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.CredentialProviderUtils;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = HttpApiConfig.HttpApiConfigBuilder.class)
public class HttpApiConfig {
  private static final Logger logger = LoggerFactory.getLogger(HttpApiConfig.class);

  protected static final String DEFAULT_INPUT_FORMAT = "json";
  protected static final String CSV_INPUT_FORMAT = "csv";
  protected static final String XML_INPUT_FORMAT = "xml";

  @JsonProperty
  private final String url;
  /**
   * Whether this API configuration represents a schema (with the
   * table providing additional parts of the URL), or if this
   * API represents a table (the URL is complete except for
   * parameters specified in the WHERE clause.)
   */
  @JsonInclude
  @JsonProperty
  private final boolean requireTail;

  @JsonProperty
  private final String method;

  @JsonProperty
  private final String postBody;

  @JsonProperty
  private final Map<String, String> headers;

  /**
   * List of query parameters which can be used in the SQL WHERE clause
   * to push filters to the REST request as HTTP query parameters.
   */
  @JsonProperty
  private final List<String> params;

  /**
   * Path within the message to the JSON object, or array of JSON
   * objects, which contain the actual data. Allows a request to
   * skip over "overhead" such as status codes. Must be a slash-delimited
   * set of JSON field names.
   */
  @JsonProperty
  private final String dataPath;

  @JsonProperty
  private final String authType;
  @JsonProperty
  private final String inputType;
  @JsonProperty
  private final int xmlDataLevel;
  @JsonProperty
  private final String limitQueryParam;
  @JsonProperty
  private final boolean errorOn400;
  @JsonProperty
  private final boolean caseSensitiveFilters;

  // Enables the user to configure JSON options at the connection level rather than globally.
  @JsonProperty
  private final HttpJsonOptions jsonOptions;

  @JsonInclude
  @JsonProperty
  private final boolean verifySSLCert;
  private final CredentialsProvider credentialsProvider;
  @JsonProperty
  private final HttpPaginatorConfig paginator;

  protected boolean directCredentials;

  public static HttpApiConfigBuilder builder() {
    return new HttpApiConfigBuilder();
  }

  public String url() {
    return this.url;
  }

  public boolean requireTail() {
    return this.requireTail;
  }

  public String method() {
    return this.method;
  }

  public String postBody() {
    return this.postBody;
  }

  public Map<String, String> headers() {
    return this.headers;
  }

  public List<String> params() {
    return this.params;
  }

  public String dataPath() {
    return this.dataPath;
  }

  public String authType() {
    return this.authType;
  }

  public String inputType() {
    return this.inputType;
  }

  public int xmlDataLevel() {
    return this.xmlDataLevel;
  }

  public String limitQueryParam() {
    return this.limitQueryParam;
  }

  public boolean errorOn400() {
    return this.errorOn400;
  }

  public HttpJsonOptions jsonOptions() {
    return this.jsonOptions;
  }

  public boolean verifySSLCert() {
    return this.verifySSLCert;
  }

  public HttpPaginatorConfig paginator() {
    return this.paginator;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HttpApiConfig that = (HttpApiConfig) o;
    return requireTail == that.requireTail
      && xmlDataLevel == that.xmlDataLevel
      && errorOn400 == that.errorOn400
      && verifySSLCert == that.verifySSLCert
      && directCredentials == that.directCredentials
      && caseSensitiveFilters == that.caseSensitiveFilters
      && Objects.equals(url, that.url)
      && Objects.equals(method, that.method)
      && Objects.equals(postBody, that.postBody)
      && Objects.equals(headers, that.headers)
      && Objects.equals(params, that.params)
      && Objects.equals(dataPath, that.dataPath)
      && Objects.equals(authType, that.authType)
      && Objects.equals(inputType, that.inputType)
      && Objects.equals(limitQueryParam, that.limitQueryParam)
      && Objects.equals(jsonOptions, that.jsonOptions)
      && Objects.equals(credentialsProvider, that.credentialsProvider)
      && Objects.equals(paginator, that.paginator);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, requireTail, method, postBody, headers, params, dataPath,
      authType, inputType, xmlDataLevel, limitQueryParam, errorOn400, jsonOptions, verifySSLCert,
      credentialsProvider, paginator, directCredentials, caseSensitiveFilters);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("url", url)
      .field("requireTail", requireTail)
      .field("method", method)
      .field("postBody", postBody)
      .field("headers", headers)
      .field("params", params)
      .field("dataPath", dataPath)
      .field("caseSensitiveFilters", caseSensitiveFilters)
      .field("authType", authType)
      .field("inputType", inputType)
      .field("xmlDataLevel", xmlDataLevel)
      .field("limitQueryParam", limitQueryParam)
      .field("errorOn400", errorOn400)
      .field("jsonOptions", jsonOptions)
      .field("verifySSLCert", verifySSLCert)
      .field("credentialsProvider", credentialsProvider)
      .field("paginator", paginator)
      .field("directCredentials", directCredentials)
      .toString();
  }

  public enum HttpMethod {
    /**
     * Value for HTTP GET method
     */
    GET,
    /**
     * Value for HTTP POST method
     */
    POST
  }

  private HttpApiConfig(HttpApiConfig.HttpApiConfigBuilder builder) {
    this.headers = builder.headers;
    this.method = StringUtils.isEmpty(builder.method)
        ? HttpMethod.GET.toString() : builder.method.trim().toUpperCase();
    this.url = builder.url;
    this.jsonOptions = builder.jsonOptions;

    HttpMethod httpMethod = HttpMethod.valueOf(this.method);
    // Get the request method.  Only accept GET and POST requests.  Anything else will default to GET.
    switch (httpMethod) {
      case GET:
      case POST:
        break;
      default:
        throw UserException
          .validationError()
          .message("Invalid HTTP method: %s.  Drill supports 'GET' and , 'POST'.", method)
          .build(logger);
    }
    if (StringUtils.isEmpty(url)) {
      throw UserException
        .validationError()
        .message("URL is required for the HTTP storage plugin.")
        .build(logger);
    }

    // Get the authentication method. Future functionality will include OAUTH2 authentication but for now
    // Accept either basic or none.  The default is none.
    this.authType = StringUtils.defaultIfEmpty(builder.authType, "none");
    this.postBody = builder.postBody;
    this.params = CollectionUtils.isEmpty(builder.params) ? null :
      ImmutableList.copyOf(builder.params);
    this.dataPath = StringUtils.defaultIfEmpty(builder.dataPath, null);

    // Default to true for backward compatibility with first PR.
    this.requireTail = builder.requireTail;

    // Default to true for backward compatibility, and better security practices
    this.verifySSLCert = builder.verifySSLCert;

    this.inputType = builder.inputType.trim().toLowerCase();

    this.xmlDataLevel = Math.max(1, builder.xmlDataLevel);
    this.errorOn400 = builder.errorOn400;
    this.caseSensitiveFilters = builder.caseSensitiveFilters;
    this.credentialsProvider = CredentialProviderUtils.getCredentialsProvider(builder.userName, builder.password, builder.credentialsProvider);
    this.directCredentials = builder.credentialsProvider == null;

    this.limitQueryParam = builder.limitQueryParam;
    this.paginator = builder.paginator;
  }

  @JsonProperty
  public String userName() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getUsername();
    }
    return null;
  }

  @JsonProperty
  public String password() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getPassword();
    }
    return null;
  }

  @JsonIgnore
  public HttpUrl getHttpUrl() {
    return HttpUrl.parse(this.url);
  }

  @JsonIgnore
  public HttpMethod getMethodType() {
    return HttpMethod.valueOf(this.method);
  }

  @JsonIgnore
  public UsernamePasswordCredentials getUsernamePasswordCredentials() {
    return new UsernamePasswordCredentials(credentialsProvider);
  }

  @JsonProperty
  public CredentialsProvider credentialsProvider() {
    if (directCredentials) {
      return null;
    }
    return credentialsProvider;
  }

  @JsonProperty
  public boolean caseSensitiveFilters() {
    return this.caseSensitiveFilters;
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class HttpApiConfigBuilder {
    private String userName;

    private String password;

    private boolean requireTail = true;

    private boolean verifySSLCert = true;

    private String inputType = DEFAULT_INPUT_FORMAT;

    private String url;

    private String method;

    private String postBody;

    private Map<String, String> headers;

    private List<String> params;

    private String dataPath;

    private String authType;

    private int xmlDataLevel;

    private boolean caseSensitiveFilters;

    private String limitQueryParam;

    private boolean errorOn400;

    private HttpJsonOptions jsonOptions;

    private CredentialsProvider credentialsProvider;

    private HttpPaginatorConfig paginator;

    private boolean directCredentials;

    public HttpApiConfig build() {
      return new HttpApiConfig(this);
    }

    public String userName() {
      return this.userName;
    }

    public String password() {
      return this.password;
    }

    public boolean requireTail() {
      return this.requireTail;
    }

    public boolean verifySSLCert() {
      return this.verifySSLCert;
    }

    public String inputType() {
      return this.inputType;
    }

    public HttpApiConfigBuilder userName(String userName) {
      this.userName = userName;
      return this;
    }

    public HttpApiConfigBuilder password(String password) {
      this.password = password;
      return this;
    }

    public HttpApiConfigBuilder requireTail(boolean requireTail) {
      this.requireTail = requireTail;
      return this;
    }

    public HttpApiConfigBuilder verifySSLCert(boolean verifySSLCert) {
      this.verifySSLCert = verifySSLCert;
      return this;
    }

    public HttpApiConfigBuilder inputType(String inputType) {
      this.inputType = inputType;
      return this;
    }

    public HttpApiConfigBuilder url(String url) {
      this.url = url;
      return this;
    }

    public HttpApiConfigBuilder caseSensitiveFilters(boolean caseSensitiveFilters) {
      this.caseSensitiveFilters = caseSensitiveFilters;
      return this;
    }

    public HttpApiConfigBuilder method(String method) {
      this.method = method;
      return this;
    }

    public HttpApiConfigBuilder postBody(String postBody) {
      this.postBody = postBody;
      return this;
    }

    public HttpApiConfigBuilder headers(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public HttpApiConfigBuilder params(List<String> params) {
      this.params = params;
      return this;
    }

    public HttpApiConfigBuilder dataPath(String dataPath) {
      this.dataPath = dataPath;
      return this;
    }

    public HttpApiConfigBuilder authType(String authType) {
      this.authType = authType;
      return this;
    }

    public HttpApiConfigBuilder xmlDataLevel(int xmlDataLevel) {
      this.xmlDataLevel = xmlDataLevel;
      return this;
    }

    public HttpApiConfigBuilder limitQueryParam(String limitQueryParam) {
      this.limitQueryParam = limitQueryParam;
      return this;
    }

    public HttpApiConfigBuilder errorOn400(boolean errorOn400) {
      this.errorOn400 = errorOn400;
      return this;
    }

    public HttpApiConfigBuilder jsonOptions(HttpJsonOptions jsonOptions) {
      this.jsonOptions = jsonOptions;
      return this;
    }

    public HttpApiConfigBuilder credentialsProvider(CredentialsProvider credentialsProvider) {
      this.credentialsProvider = credentialsProvider;
      return this;
    }

    public HttpApiConfigBuilder paginator(HttpPaginatorConfig paginator) {
      this.paginator = paginator;
      return this;
    }

    public HttpApiConfigBuilder directCredentials(boolean directCredentials) {
      this.directCredentials = directCredentials;
      return this;
    }
  }
}
