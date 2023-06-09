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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.drill.common.PlanStringBuilder;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = HttpOAuthConfig.HttpOAuthConfigBuilder.class)
public class HttpOAuthConfig {

  private final String callbackURL;
  private final String authorizationURL;
  private final Map<String, String> authorizationParams;
  private final String tokenType;
  private final boolean generateCSRFToken;
  private final String scope;
  private final boolean accessTokenInHeader;

  @JsonCreator
  public HttpOAuthConfig(@JsonProperty("callbackURL") String callbackURL,
                         @JsonProperty("authorizationURL") String authorizationURL,
                         @JsonProperty("authorizationParams") Map<String, String> authorizationParams,
                         @JsonProperty("tokenType") String tokenType,
                         @JsonProperty("generateCSRFToken") boolean generateCSRFToken,
                         @JsonProperty("scope") String scope,
                         @JsonProperty("accessTokenInHeader") boolean accessTokenInHeader) {
    this.callbackURL = callbackURL;
    this.authorizationURL = authorizationURL;
    this.authorizationParams = authorizationParams;
    this.tokenType = tokenType;
    this.generateCSRFToken = generateCSRFToken;
    this.accessTokenInHeader = accessTokenInHeader;
    this.scope = scope;
  }

  public HttpOAuthConfig(HttpOAuthConfig.HttpOAuthConfigBuilder builder) {
    this.callbackURL = builder.callbackURL;
    this.authorizationURL = builder.authorizationURL;
    this.authorizationParams = builder.authorizationParams;
    this.generateCSRFToken = builder.generateCSRFToken;
    this.tokenType = builder.tokenType;
    this.accessTokenInHeader = builder.accessTokenInHeader;
    this.scope = builder.scope;
  }

  public static HttpOAuthConfigBuilder builder() {
    return new HttpOAuthConfigBuilder();
  }

  public String getCallbackURL() {
    return this.callbackURL;
  }

  public String getAuthorizationURL() {
    return this.authorizationURL;
  }

  public Map<String, String> getAuthorizationParams() {
    return this.authorizationParams;
  }

  public String getTokenType() {
    return this.tokenType;
  }

  public boolean isGenerateCSRFToken() {
    return this.generateCSRFToken;
  }

  public String getScope() {
    return this.scope;
  }

  public boolean isAccessTokenInHeader() {
    return this.accessTokenInHeader;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("callbackURL", callbackURL)
      .field("authorizationURL", authorizationURL)
      .field("authorizationParams", authorizationParams)
      .field("tokenType", tokenType)
      .field("generateCSRFToken", generateCSRFToken)
      .field("scope", scope)
      .field("accessTokenInHeader", accessTokenInHeader)
      .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(callbackURL, authorizationURL, authorizationParams,
      tokenType, generateCSRFToken, scope, accessTokenInHeader);
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    } else if (that == null || getClass() != that.getClass()) {
      return false;
    }
    HttpOAuthConfig thatConfig = (HttpOAuthConfig) that;
    return Objects.equals(callbackURL, thatConfig.callbackURL) &&
      Objects.equals(authorizationURL, thatConfig.authorizationURL) &&
      Objects.equals(authorizationParams, thatConfig.authorizationParams) &&
      Objects.equals(tokenType, thatConfig.tokenType) &&
      Objects.equals(generateCSRFToken, thatConfig.generateCSRFToken) &&
      Objects.equals(scope, thatConfig.scope) &&
      Objects.equals(accessTokenInHeader, thatConfig.accessTokenInHeader);
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class HttpOAuthConfigBuilder {
    private String callbackURL;

    private String authorizationURL;

    private Map<String, String> authorizationParams;

    private String tokenType;

    private boolean generateCSRFToken;

    private String scope;

    private boolean accessTokenInHeader;

    private Map<String, String> tokens;

    HttpOAuthConfigBuilder() {
    }

    public HttpOAuthConfig build() {
      return new HttpOAuthConfig(this);
    }

    public HttpOAuthConfigBuilder callbackURL(String callbackURL) {
      this.callbackURL = callbackURL;
      return this;
    }

    public HttpOAuthConfigBuilder authorizationURL(String authorizationURL) {
      this.authorizationURL = authorizationURL;
      return this;
    }

    public HttpOAuthConfigBuilder authorizationParams(Map<String, String> authorizationParams) {
      this.authorizationParams = authorizationParams;
      return this;
    }

    public HttpOAuthConfigBuilder tokenType(String tokenType) {
      this.tokenType = tokenType;
      return this;
    }

    public HttpOAuthConfigBuilder generateCSRFToken(boolean generateCSRFToken) {
      this.generateCSRFToken = generateCSRFToken;
      return this;
    }

    public HttpOAuthConfigBuilder scope(String scope) {
      this.scope = scope;
      return this;
    }

    public HttpOAuthConfigBuilder accessTokenInHeader(boolean accessTokenInHeader) {
      this.accessTokenInHeader = accessTokenInHeader;
      return this;
    }
  }
}
