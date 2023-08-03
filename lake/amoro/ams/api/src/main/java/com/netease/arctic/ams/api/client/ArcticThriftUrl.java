/*
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

package com.netease.arctic.ams.api.client;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.properties.AmsHAProperties;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArcticThriftUrl {
  public static final String PARAM_SOCKET_TIMEOUT = "socketTimeout";
  public static final int DEFAULT_SOCKET_TIMEOUT = 5000;
  public static final String ZOOKEEPER_FLAG = "zookeeper";
  public static final String THRIFT_FLAG = "thrift";
  public static final String THRIFT_URL_FORMAT = "thrift://%s:%d/%s%s";
  public static final int maxRetries = 3;
  private static final Logger logger = LoggerFactory.getLogger(ArcticThriftUrl.class);
  private static final Pattern PATTERN = Pattern.compile("zookeeper://(\\S+)/([\\w-]+)");
  private final String schema;
  private final String host;
  private final int port;
  private final String catalogName;
  private final int socketTimeout;

  // origin url before parse
  private final String url;

  private ArcticThriftUrl(
      String schema, String host, int port, String catalogName, int socketTimeout,
      String url) {
    this.schema = schema;
    this.host = host;
    this.port = port;
    this.catalogName = catalogName;
    this.socketTimeout = socketTimeout;
    this.url = url;
  }

  /**
   * parse thrift url, now support thrift://host:port/{catalogName} and zookeeper://host:port/{cluster}/{catalogName}
   * . parse to ArcticThriftUrl that contains properties schema, host, port, catalogName, socketTimeout, url.
   *
   * @param url - thrift url
   * @return -
   */
  public static ArcticThriftUrl parse(String url, String serviceName) {
    if (url == null) {
      throw new IllegalArgumentException("thrift url is null");
    }
    if (url.startsWith(ZOOKEEPER_FLAG)) {
      return parserZookeeperUrl(url, serviceName);
    } else {
      return parserThriftUrl(url);
    }
  }

  private static ArcticThriftUrl parserThriftUrl(String url) {
    int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    try {
      URI uri = new URI(url.toLowerCase(Locale.ROOT));
      String schema = uri.getScheme();
      String host = uri.getHost();
      int port = uri.getPort();
      String path = uri.getPath();
      if (path != null && path.startsWith("/")) {
        path = path.substring(1);
      }
      if (uri.getQuery() != null) {
        for (String paramExpression : uri.getQuery().split("&")) {
          String[] paramSplit = paramExpression.split("=");
          if (paramSplit.length == 2) {
            if (paramSplit[0].equalsIgnoreCase(PARAM_SOCKET_TIMEOUT)) {
              socketTimeout = Integer.parseInt(paramSplit[1]);
            }
          }
        }
      }
      String catalogName = path;
      return new ArcticThriftUrl(schema, host, port, catalogName, socketTimeout, url);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("parse metastore url failed", e);
    }
  }

  private static ArcticThriftUrl parserZookeeperUrl(String url, String serviceName) {
    String thriftUrl = url;
    String query = "";
    if (url.contains("?")) {
      query = url.substring(url.indexOf("?"));
      thriftUrl = url.substring(0, url.indexOf("?"));
    }
    Matcher m = PATTERN.matcher(thriftUrl);
    if (m.matches()) {
      String zkServerAddress;
      String cluster;
      String catalog = "";
      if (m.group(1).contains("/")) {
        zkServerAddress = m.group(1).substring(0, m.group(1).indexOf("/"));
        cluster = m.group(1).substring(m.group(1).indexOf("/") + 1);
        catalog = m.group(2);
      } else {
        zkServerAddress = m.group(1);
        cluster = m.group(2);
      }
      int retryCount = 0;
      while (retryCount < maxRetries) {
        try {
          AmsServerInfo serverInfo = findAmsServerInfo(serviceName, zkServerAddress, cluster);
          url = String.format(THRIFT_URL_FORMAT, serverInfo.getHost(),
              serverInfo.getThriftBindPort(), catalog, query);
          int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
          for (String paramExpression : query.replace("?", "").split("&")) {
            String[] paramSplit = paramExpression.split("=");
            if (paramSplit.length == 2) {
              if (paramSplit[0].equalsIgnoreCase(PARAM_SOCKET_TIMEOUT)) {
                socketTimeout = Integer.parseInt(paramSplit[1]);
              }
            }
          }
          return new ArcticThriftUrl("thrift",
              serverInfo.getHost(), serverInfo.getThriftBindPort(), catalog.toLowerCase(), socketTimeout, url);
        } catch (KeeperException.AuthFailedException authFailedException) {
          // If kerberos authentication is not enabled on the zk,
          // an error occurs when the thread carrying kerberos authentication information accesses the zk.
          // Therefore, clear the authentication information and try again
          retryCount++;
          logger.error(
              String.format("Caught exception, retrying... (retry count: %s)", retryCount),
              authFailedException);
          try {
            Subject subject = Subject.getSubject(java.security.AccessController.getContext());

            if (subject != null) {
              LoginContext loginContext = new LoginContext("", subject);
              loginContext.logout();
            }
          } catch (LoginException e) {
            logger.error("Failed to logout", e);
          }
        } catch (Exception e) {
          retryCount++;
          logger.error(String.format("Caught exception, retrying... (retry count: %s)", retryCount), e);
          throw new RuntimeException(String.format("invalid ams url %s", url));
        }
      }
    } else {
      throw new RuntimeException(String.format("invalid ams url %s", url));
    }
    return null;
  }

  private static AmsServerInfo findAmsServerInfo(String serviceName, String zkServerAddress, String cluster)
      throws Exception {
    switch (serviceName) {
      case Constants.THRIFT_TABLE_SERVICE_NAME:
        return JSONObject.parseObject(
            ZookeeperService.getInstance(zkServerAddress).getData(AmsHAProperties.getTableServiceMasterPath(cluster)),
            AmsServerInfo.class);
      case Constants.THRIFT_OPTIMIZING_SERVICE_NAME:
        return JSONObject.parseObject(
            ZookeeperService.getInstance(zkServerAddress)
                .getData(AmsHAProperties.getOptimizingServiceMasterPath(cluster)),
            AmsServerInfo.class);
      default:
        throw new RuntimeException(String.format("invalid service name %s", serviceName));
    }
  }

  public String schema() {
    return schema;
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  public String catalogName() {
    return catalogName;
  }

  public int socketTimeout() {
    return socketTimeout;
  }

  public String url() {
    return url;
  }

  public String serverUrl() {
    return String.format(THRIFT_URL_FORMAT, host, port, "", "");
  }

  @Override
  public String toString() {
    return "ArcticThriftUrl{" +
        "schema='" + schema + '\'' +
        ", host='" + host + '\'' +
        ", port=" + port +
        ", catalogName='" + catalogName + '\'' +
        ", socketTimeout=" + socketTimeout +
        ", url='" + url + '\'' +
        '}';
  }
}