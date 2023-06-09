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
package org.apache.drill.exec.store.http.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.drill.exec.ExecConstants;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

/**
 * HTTP proxy settings. Provides a builder to create settings
 * from the Drill config or from code. Allows combining the two.
 * The Drill config allows integrating with Linux env. vars. Allows
 * combinations: take values from config, but selectively replace bits,
 * such as the user name/password.
 * <p>
 * This class provides values passed to the HTTP client, whatever it
 * might be.
 *
 * @see <a href="https://www.shellhacks.com/linux-proxy-server-settings-set-proxy-command-line/">
 * Proxy Server Settings</a>
 *
 */
public class HttpProxyConfig {
  private static final Logger logger = LoggerFactory.getLogger(HttpProxyConfig.class);

  public enum ProxyType {
    NONE, HTTP, SOCKS
  }

  public static class ProxyBuilder {
    private String url;
    private String typeStr;
    private ProxyType type = ProxyType.NONE;
    private String host;
    private int port = 80;
    private String username;
    private String password;

    public ProxyBuilder fromHttpConfig(Config config) {
      url(config.getString(ExecConstants.HTTP_PROXY_URL));
      type(config.getString(ExecConstants.HTTP_PROXY_TYPE));
      host(config.getString(ExecConstants.HTTP_PROXY_HOST));
      port(config.getInt(ExecConstants.HTTP_PROXY_PORT));
      username(config.getString(ExecConstants.HTTP_PROXY_USER_NAME));
      password(config.getString(ExecConstants.HTTP_PROXY_PASSWORD));
      return this;
    }

    public ProxyBuilder fromHttpsConfig(Config config) {
      url(config.getString(ExecConstants.HTTPS_PROXY_URL));
      type(config.getString(ExecConstants.HTTPS_PROXY_TYPE));
      host(config.getString(ExecConstants.HTTPS_PROXY_HOST));
      port(config.getInt(ExecConstants.HTTPS_PROXY_PORT));
      username(config.getString(ExecConstants.HTTPS_PROXY_USER_NAME));
      password(config.getString(ExecConstants.HTTPS_PROXY_PASSWORD));
      return this;
    }

    public ProxyBuilder fromConfigForURL(Config config, String url) {
      try {
        URL parsed = new URL(url);
        if (parsed.getProtocol().equals("https")) {
          return fromHttpsConfig(config);
        }
      } catch (Exception e) {
        // This is not the place to warn about a bad URL.
        // Just assume HTTP, something later will fail.
      }
      return fromHttpConfig(config);
    }

    public ProxyBuilder url(String url) {
      this.url = url;
      return this;
    }

    public ProxyBuilder type(ProxyType type) {
      this.type = type;
      this.typeStr = null;
      return this;
    }

    public ProxyBuilder type(String type) {
      this.typeStr = type;
      this.type = null;
      return this;
    }

    public ProxyBuilder host(String host) {
      this.host = host;
      return this;
    }

    public ProxyBuilder port(int port) {
      this.port = port;
      return this;
    }

    public ProxyBuilder username(String username) {
      this.username = username;
      return this;
    }

    public ProxyBuilder password(String password) {
      this.password = password;
      return this;
    }

    public HttpProxyConfig build() {
      buildFromUrl();
      buildType();

      // Info can come from the config file. Ignore extra spaces.
      host = host == null ? null : host.trim();
      username = username == null ? null : username.trim();
      password = password == null ? null : password.trim();
      return new HttpProxyConfig(this);
    }

    private void buildFromUrl() {
      url = url == null ? null : url.trim();
      if (Strings.isNullOrEmpty(url)) {
        return;
      }
      typeStr = null;
      URL parsed;
      try {
        parsed = new URL(url);
      } catch (MalformedURLException e) {
        logger.warn("Invalid proxy url: {}, assuming NONE", typeStr);
        type = ProxyType.NONE;
        return;
      }
      type = ProxyType.HTTP;
      host = parsed.getHost();
      port = parsed.getPort();
      String userInfo = parsed.getUserInfo();
      if (userInfo != null) {
        String[] parts = userInfo.split(":");
        username = parts[0];
        password = parts.length > 1 ? parts[1] : null;
      }
    }

    private void buildType() {

      // If type string, validate to a type
      if (typeStr != null) {
        typeStr = typeStr.trim().toUpperCase();
        if (!Strings.isNullOrEmpty(typeStr)) {
          try {
            type = ProxyType.valueOf(typeStr);
          } catch (IllegalArgumentException e) {
            logger.warn("Invalid proxy type: {}, assuming NONE", typeStr);
            this.type = ProxyType.NONE;
          }
        }
      }

      // If not type, assume NONE
      if (type == null) {
        type = ProxyType.NONE;
      }

      // Validate host based on type
      if (type != ProxyType.NONE) {
        host = host == null ? null : host.trim();
        if (Strings.isNullOrEmpty(host)) {
          logger.warn("{} proxy type specified, but host is null. Reverting to NONE",
              type.name());
          type = ProxyType.NONE;
        }
      }

      // If no proxy, ignore other settings
      if (type == ProxyType.NONE) {
        host = null;
        username = null;
        password = null;
      }
    }
  }

  public final ProxyType type;
  public final String host;
  public final int port;
  public final String username;
  public final String password;

  private HttpProxyConfig(ProxyBuilder builder) {
    this.type = builder.type;
    this.host = builder.host;
    this.port = builder.port;
    this.username = builder.username;
    this.password = builder.password;
  }

  public static ProxyBuilder builder() { return new ProxyBuilder(); }
}
