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

package org.apache.ambari.server.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.proxy.ProxyService;
import org.apache.ambari.view.URLConnectionProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around an internal URL stream provider.
 */
public class ViewURLStreamProvider implements org.apache.ambari.view.URLStreamProvider, URLConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ViewContextImpl.class);

  /**
   * The key for the "doAs" header.
   */
  private static final String DO_AS_PARAM = "doAs";

  /**
   * The view context.
   */
  private final ViewContext viewContext;

  /**
   * Internal stream provider.
   */
  private final URLStreamProvider streamProvider;

  /**
   * Apply host port restrictions if any
   */
  private HostPortRestrictionHandler hostPortRestrictionHandler;

  // ----- Constructor -----------------------------------------------------

  /**
   * Construct a view URL stream provider.
   *
   * @param viewContext    the associated view context
   * @param streamProvider the underlying stream provider
   */
  protected ViewURLStreamProvider(ViewContext viewContext, URLStreamProvider streamProvider) {
    this.viewContext = viewContext;
    this.streamProvider = streamProvider;
  }

  /**
   * Get an instance of HostPortRestrictionHandler
   *
   * @return
   */
  private HostPortRestrictionHandler getHostPortRestrictionHandler() {
    if (hostPortRestrictionHandler == null) {
      HostPortRestrictionHandler hostPortRestrictionHandlerTmp =
          new HostPortRestrictionHandler(viewContext.getAmbariProperty(Configuration.PROXY_ALLOWED_HOST_PORTS.getKey()));
      hostPortRestrictionHandler = hostPortRestrictionHandlerTmp;
    }
    return hostPortRestrictionHandler;
  }

  // ----- URLStreamProvider -----------------------------------------------

  @Override
  public InputStream readFrom(String spec, String requestMethod, String body, Map<String, String> headers)
      throws IOException {
    return getInputStream(spec, requestMethod, headers, body == null ? null : body.getBytes());
  }

  @Override
  public InputStream readFrom(String spec, String requestMethod, InputStream body, Map<String, String> headers)
      throws IOException {
    return getInputStream(spec, requestMethod, headers, body == null ? null : IOUtils.toByteArray(body));
  }


  @Override
  public InputStream readAs(String spec, String requestMethod, String body, Map<String, String> headers,
                            String userName)
      throws IOException {
    return readFrom(addDoAs(spec, userName), requestMethod, body, headers);
  }

  @Override
  public InputStream readAs(String spec, String requestMethod, InputStream body, Map<String, String> headers,
                            String userName) throws IOException {
    return readFrom(addDoAs(spec, userName), requestMethod, body, headers);
  }


  @Override
  public InputStream readAsCurrent(String spec, String requestMethod, String body, Map<String, String> headers)
      throws IOException {

    return readAs(spec, requestMethod, body, headers, viewContext.getUsername());
  }

  @Override
  public InputStream readAsCurrent(String spec, String requestMethod, InputStream body, Map<String, String> headers)
      throws IOException {

    return readAs(spec, requestMethod, body, headers, viewContext.getUsername());
  }

  @Override
  public HttpURLConnection getConnection(String spec,
                                         String requestMethod,
                                         String body,
                                         Map<String, String> headers) throws IOException {
    return getHttpURLConnection(spec, requestMethod, headers, body == null ? null : body.getBytes());
  }

  @Override
  public HttpURLConnection getConnection(String spec,
                                         String requestMethod,
                                         InputStream body,
                                         Map<String, String> headers) throws IOException {
    return getHttpURLConnection(spec, requestMethod, headers, body == null ? null : IOUtils.toByteArray(body));
  }

  @Override
  public HttpURLConnection getConnectionAs(String spec,
                                           String requestMethod,
                                           String body,
                                           Map<String, String> headers,
                                           String userName) throws IOException {
    return getConnection(addDoAs(spec, userName), requestMethod, body, headers);
  }

  @Override
  public HttpURLConnection getConnectionAs(String spec,
                                           String requestMethod,
                                           InputStream body,
                                           Map<String, String> headers,
                                           String userName) throws IOException {
    return getConnection(addDoAs(spec, userName), requestMethod, body, headers);
  }

  @Override
  public HttpURLConnection getConnectionAsCurrent(String spec,
                                                  String requestMethod,
                                                  String body,
                                                  Map<String, String> headers) throws IOException {
    return getConnectionAs(spec, requestMethod, body, headers, viewContext.getUsername());
  }

  @Override
  public HttpURLConnection getConnectionAsCurrent(String spec,
                                                  String requestMethod,
                                                  InputStream body,
                                                  Map<String, String> headers) throws IOException {
    return getConnectionAs(spec, requestMethod, body, headers, viewContext.getUsername());
  }

  // ----- helper methods ----------------------------------------------------

  // add the "do as" query parameter
  private String addDoAs(String spec, String userName) throws IOException {
    if (spec.toLowerCase().contains(DO_AS_PARAM)) {
      throw new IllegalArgumentException("URL cannot contain \"" + DO_AS_PARAM + "\" parameter.");
    }

    try {
      URIBuilder builder = new URIBuilder(spec);
      builder.addParameter(DO_AS_PARAM, userName);
      return builder.build().toString();
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  // get the input stream response from the underlying stream provider
  private InputStream getInputStream(String spec, String requestMethod, Map<String, String> headers, byte[] info)
      throws IOException {
    if (!isProxyCallAllowed(spec)) {
      LOG.warn("Call to " + spec + " is not allowed. See ambari.properties proxy.allowed.hostports.");
      throw new IOException("Call to " + spec + " is not allowed. See ambari.properties proxy.allowed.hostports.");
    }

    HttpURLConnection connection = getHttpURLConnection(spec, requestMethod, headers, info);

    int responseCode = connection.getResponseCode();

    return responseCode >= ProxyService.HTTP_ERROR_RANGE_START ?
        connection.getErrorStream() : connection.getInputStream();
  }

  // get the input stream response from the underlying stream provider
  private HttpURLConnection getHttpURLConnection(String spec, String requestMethod,
                                                 Map<String, String> headers, byte[] info)
      throws IOException {
    if (!isProxyCallAllowed(spec)) {
      LOG.warn("Call to " + spec + " is not allowed. See ambari.properties proxy.allowed.hostports.");
      throw new IOException("Call to " + spec + " is not allowed. See ambari.properties proxy.allowed.hostports.");
    }

    // adapt the headers to the internal URLStreamProvider processURL signature

    Map<String, List<String>> headerMap = new HashMap<>();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      headerMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
    }
    return streamProvider.processURL(spec, requestMethod, info, headerMap);
  }

  /**
   * Is it allowed to make calls to the supplied url
   * @param spec the URL
   * @return
   */
  protected boolean isProxyCallAllowed(String spec) {
    if (StringUtils.isNotBlank(spec) && getHostPortRestrictionHandler().proxyCallRestricted()) {
      try {
        URL url = new URL(spec);
        return getHostPortRestrictionHandler().allowProxy(url.getHost(),
                                                               Integer.toString(url.getPort() == -1
                                                                                    ? url.getDefaultPort()
                                                                                    : url.getPort()));
      } catch (MalformedURLException ignored) {
        // actual connection attempt will throw
      }
    }

    return true;
  }

  /**
   * Creates a list of allowed hosts and ports
   */
  class HostPortRestrictionHandler {
    private final String allowedHostPortsValue;
    private Map<String, HashSet<String>> allowedHostPorts = null;
    private Boolean isProxyCallRestricted = Boolean.FALSE;

    public HostPortRestrictionHandler(String allowedHostPortsValue) {
      this.allowedHostPortsValue = allowedHostPortsValue;
      LOG.debug("Proxy restriction will be derived from {}", allowedHostPortsValue);
    }

    /**
     * Checks supplied host and port against the restriction list
     *
     * @param host
     * @param port
     *
     * @return if the host and port combination is allowed
     */
    public boolean allowProxy(String host, String port) {
      LOG.debug("Checking host {} port {} against allowed list.", host, port);
      if (StringUtils.isNotBlank(host)) {
        String hostToCompare = host.trim().toLowerCase();
        if (allowedHostPorts == null) {
          initializeAllowedHostPorts();
        }

        if (isProxyCallRestricted) {
          if (allowedHostPorts.containsKey(hostToCompare)) {
            if (allowedHostPorts.get(hostToCompare).contains("*")) {
              return true;
            }
            String portToCompare = "";
            if (StringUtils.isNotBlank(port)) {
              portToCompare = port.trim();
            }
            if (allowedHostPorts.get(hostToCompare).contains(portToCompare)) {
              return true;  // requested host port allowed
            }
            return false;
          }
          return false; // restricted, at least hostname need to match
        } // no restriction
      }
      return true;
    }

    /**
     * Initialize the allowed list of hosts and ports
     */
    private void initializeAllowedHostPorts() {
      boolean proxyCallRestricted = false;
      Map<String, HashSet<String>> allowed = new HashMap<>();
      if (StringUtils.isNotBlank(allowedHostPortsValue)) {
        String allowedStr = allowedHostPortsValue.toLowerCase();
        if (!allowedStr.equals(Configuration.PROXY_ALLOWED_HOST_PORTS.getDefaultValue())) {
          proxyCallRestricted = true;
          String[] hostPorts = allowedStr.trim().split(",");
          for (String hostPortStr : hostPorts) {
            String[] hostAndPort = hostPortStr.trim().split(":");
            if (hostAndPort.length == 1) {
              if (!allowed.containsKey(hostAndPort[0])) {
                allowed.put(hostAndPort[0], new HashSet<>());
              }
              allowed.get(hostAndPort[0]).add("*");
              LOG.debug("Allow proxy to host {} and all ports.", hostAndPort[0]);
            } else {
              if (!allowed.containsKey(hostAndPort[0])) {
                allowed.put(hostAndPort[0], new HashSet<>());
              }
              allowed.get(hostAndPort[0]).add(hostAndPort[1]);
              LOG.debug("Allow proxy to host {} and port {}", hostAndPort[0], hostAndPort[1]);
            }
          }
        }
      }
      allowedHostPorts = allowed;
      isProxyCallRestricted = proxyCallRestricted;
    }

    /**
     * Is there a restriction of which host and port the call can be made to
     * @return
     */
    public Boolean proxyCallRestricted() {
      if (allowedHostPorts == null) {
        initializeAllowedHostPorts();
      }
      return isProxyCallRestricted;
    }
  }
}
