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

package org.apache.ambari.server.controller.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Convenience class to handle the connection details of a LogSearch query request.
 *
 */
public class LoggingRequestHelperImpl implements LoggingRequestHelper {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingRequestHelperImpl.class);

  private static final String LOGSEARCH_ADMIN_JSON_CONFIG_TYPE_NAME = "logsearch-admin-json";

  private static final String LOGSEARCH_ADMIN_USERNAME_PROPERTY_NAME = "logsearch_admin_username";

  private static final String LOGSEARCH_ADMIN_PASSWORD_PROPERTY_NAME = "logsearch_admin_password";

  private static final String LOGSEARCH_QUERY_PATH = "/api/v1/service/logs";

  private static final String LOGSEARCH_GET_LOG_LEVELS_PATH = "/api/v1/service/logs/levels/counts";

  private static final String LOGSEARCH_GET_LOG_FILES_PATH = "/api/v1/service/logs/files";

  private static final String LOGSEARCH_ADMIN_CREDENTIAL_NAME = "logsearch.admin.credential";

  private static final String COMPONENT_QUERY_PARAMETER_NAME = "component_name";

  private static final String HOST_QUERY_PARAMETER_NAME = "host_name";

  private static final String DEFAULT_PAGE_SIZE = "50";

  private static final String PAGE_SIZE_QUERY_PARAMETER_NAME = "pageSize";

  private static final String COOKIE_HEADER = "Cookie";

  private static final String SET_COOKIES_HEADER = "Set-Cookie";

  private static final int DEFAULT_LOGSEARCH_CONNECT_TIMEOUT_IN_MILLISECONDS = 5000;

  private static final int DEFAULT_LOGSEARCH_READ_TIMEOUT_IN_MILLISECONDS = 5000;

  private static final String LOGSEARCH_CLUSTERS_QUERY_PARAMETER_NAME = "clusters";

  private static AtomicInteger errorLogCounterForLogSearchConnectionExceptions = new AtomicInteger(0);

  private final String hostName;

  private final String portNumber;

  private final String protocol;

  private final CredentialStoreService credentialStoreService;

  private final Cluster cluster;

  private final String externalAddress;

  private final NetworkConnection networkConnection;

  private SSLSocketFactory sslSocketFactory;

  private int logSearchConnectTimeoutInMilliseconds = DEFAULT_LOGSEARCH_CONNECT_TIMEOUT_IN_MILLISECONDS;

  private int logSearchReadTimeoutInMilliseconds = DEFAULT_LOGSEARCH_READ_TIMEOUT_IN_MILLISECONDS;


  public LoggingRequestHelperImpl(String hostName, String portNumber, String protocol, CredentialStoreService credentialStoreService, Cluster cluster, String externalAddress) {
    this(hostName, portNumber, protocol, credentialStoreService, cluster, externalAddress, new DefaultNetworkConnection());
  }

  protected LoggingRequestHelperImpl(String hostName, String portNumber, String protocol, CredentialStoreService credentialStoreService, Cluster cluster, String externalAddress, NetworkConnection networkConnection) {
    this.hostName = hostName;
    this.portNumber = portNumber;
    this.protocol = protocol;
    this.credentialStoreService = credentialStoreService;
    this.cluster = cluster;
    this.externalAddress = externalAddress;
    this.networkConnection = networkConnection;
  }

  public int getLogSearchConnectTimeoutInMilliseconds() {
    return this.logSearchConnectTimeoutInMilliseconds;
  }

  public void setLogSearchConnectTimeoutInMilliseconds(int logSearchConnectTimeoutInMilliseconds) {
    this.logSearchConnectTimeoutInMilliseconds = logSearchConnectTimeoutInMilliseconds;
  }

  public int getLogSearchReadTimeoutInMilliseconds() {
    return this.logSearchReadTimeoutInMilliseconds;
  }

  public void setLogSearchReadTimeoutInMilliseconds(int logSearchReadTimeoutInMilliseconds) {
    this.logSearchReadTimeoutInMilliseconds = logSearchReadTimeoutInMilliseconds;
  }

  @Override
  public LogQueryResponse sendQueryRequest(Map<String, String> queryParameters) {
    try {
      // use the Apache builder to create the correct URI
      URI logSearchURI = createLogSearchQueryURI(protocol, queryParameters);
      LOG.debug("Attempting to connect to LogSearch server at {}", logSearchURI);
      HttpURLConnection httpURLConnection  = (HttpURLConnection) logSearchURI.toURL().openConnection();
      secure(httpURLConnection, protocol);
      httpURLConnection.setRequestMethod("GET");
      httpURLConnection.setConnectTimeout(logSearchConnectTimeoutInMilliseconds);
      httpURLConnection.setReadTimeout(logSearchReadTimeoutInMilliseconds);

      addCookiesFromCookieStore(httpURLConnection);
      LOG.debug("Attempting request to LogSearch Portal Server, with connect timeout = {} milliseconds and read timeout = {} milliseconds",
        logSearchConnectTimeoutInMilliseconds, logSearchReadTimeoutInMilliseconds);

      setupCredentials(httpURLConnection);

      StringBuffer buffer = networkConnection.readQueryResponseFromServer(httpURLConnection);
      addCookiesToCookieStoreFromResponse(httpURLConnection);

      // setup a reader for the JSON response
      StringReader stringReader = new StringReader(buffer.toString());

      ObjectReader logQueryResponseReader =
        createObjectReader(LogQueryResponse.class);

      return logQueryResponseReader.readValue(stringReader);

    } catch (Exception e) {
      Utils.logErrorMessageWithThrowableWithCounter(LOG, errorLogCounterForLogSearchConnectionExceptions,
        "Error occurred while trying to connect to the LogSearch service...", e);
    }

    return null;
  }

  private void secure(HttpURLConnection connection, String protocol) {
    if ("https".equals(protocol)) {
      HttpsURLConnection secureConnection = (HttpsURLConnection) connection;
      loadTrustStore();
      secureConnection.setSSLSocketFactory(this.sslSocketFactory);
    }
  }

  private void loadTrustStore() {
    if (this.sslSocketFactory == null) {
      ComponentSSLConfiguration sslConfig = ComponentSSLConfiguration.instance();
      String trustStorePath = sslConfig.getTruststorePath();
      String trustStoreType = sslConfig.getTruststoreType();
      String trustStorePassword = sslConfig.getTruststorePassword();

      if (trustStorePath == null || trustStorePassword == null) {
        String trustStoreErrorMsg = "Can\'t load TrustStore. Truststore path or password is not set.";
        LOG.error(trustStoreErrorMsg);
        throw new IllegalStateException(trustStoreErrorMsg);
      }

      try (FileInputStream in = new FileInputStream(new File(trustStorePath))) {
        KeyStore e = KeyStore.getInstance(trustStoreType == null ? KeyStore.getDefaultType() : trustStoreType);
        e.load(in, trustStorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(e);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init((KeyManager[]) null, tmf.getTrustManagers(), (SecureRandom) null);
        this.sslSocketFactory = context.getSocketFactory();
      } catch (Exception ex) {
        LOG.error("Unable to load TrustStore", ex);
      }
    }
  }

  private void addCookiesFromCookieStore(HttpURLConnection httpURLConnection) {
    if (LoggingCookieStore.INSTANCE.getCookiesMap().size() > 0) {
      List<String> cookiesStrList = new ArrayList<>();
      for (Map.Entry<String, String> entry : LoggingCookieStore.INSTANCE.getCookiesMap().entrySet()) {
        cookiesStrList.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
      }
      httpURLConnection.setRequestProperty(COOKIE_HEADER, StringUtils.join(cookiesStrList, "; "));
    }
  }

  private void addCookiesToCookieStoreFromResponse(HttpURLConnection httpURLConnection) {
    Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
    List<String> cookiesHeader = headerFields.get(SET_COOKIES_HEADER);
    if (cookiesHeader != null) {
      for (String cookie : cookiesHeader) {
        HttpCookie cookie1 = HttpCookie.parse(cookie).get(0);
        LoggingCookieStore.INSTANCE.addCookie(cookie1.getName(), cookie1.getValue());
      }
    }
  }


  private void setupCredentials(HttpURLConnection httpURLConnection) {
    final String logSearchAdminUser =
      getLogSearchAdminUser();
    final String logSearchAdminPassword =
      getLogSearchAdminPassword();

    // first attempt to use the LogSearch admin configuration to
    // obtain the LogSearch server credential
    if ((logSearchAdminUser != null) && (logSearchAdminPassword != null) && StringUtils.isEmpty(externalAddress)) {
      LOG.debug("Credential found in config, will be used to connect to LogSearch");
      networkConnection.setupBasicAuthentication(httpURLConnection, createEncodedCredentials(logSearchAdminUser, logSearchAdminPassword));
    } else {
      // if no credential found in config, attempt to locate the credential using
      // the Ambari CredentialStoreService
      PrincipalKeyCredential principalKeyCredential =
        getLogSearchCredentials();

      // determine the credential to use for connecting to LogSearch
      if (principalKeyCredential != null) {
        // setup credential stored in credential service
        LOG.debug("Credential found in CredentialStore, will be used to connect to LogSearch");
        networkConnection.setupBasicAuthentication(httpURLConnection, createEncodedCredentials(principalKeyCredential));
      } else {
        LOG.debug("No LogSearch credential could be found, this is probably an error in configuration");
        if (StringUtils.isEmpty(externalAddress)) {
          LOG.error("No LogSearch credential could be found, this is required for external LogSearch (credential: {})", LOGSEARCH_ADMIN_CREDENTIAL_NAME);
        }
      }
    }
  }

  private String getLogSearchAdminUser() {
    Config logSearchAdminConfig =
      cluster.getDesiredConfigByType(LOGSEARCH_ADMIN_JSON_CONFIG_TYPE_NAME);

    if (logSearchAdminConfig != null) {
      return logSearchAdminConfig.getProperties().get(LOGSEARCH_ADMIN_USERNAME_PROPERTY_NAME);
    }

    return null;
  }

  private String getLogSearchAdminPassword() {
    Config logSearchAdminConfig =
      cluster.getDesiredConfigByType(LOGSEARCH_ADMIN_JSON_CONFIG_TYPE_NAME);

    if (logSearchAdminConfig != null) {
      return logSearchAdminConfig.getProperties().get(LOGSEARCH_ADMIN_PASSWORD_PROPERTY_NAME);
    }

    return null;
  }

  @Override
  public HostLogFilesResponse sendGetLogFileNamesRequest(String hostName) {
    try {
      // use the Apache builder to create the correct URI
      URIBuilder uriBuilder = createBasicURI(protocol);
      appendUriPath(uriBuilder, LOGSEARCH_GET_LOG_FILES_PATH);
      uriBuilder.addParameter(HOST_QUERY_PARAMETER_NAME, hostName);
      uriBuilder.addParameter(LOGSEARCH_CLUSTERS_QUERY_PARAMETER_NAME, cluster.getClusterName());

      // add any query strings specified
      URI logFileNamesURI = uriBuilder.build();
      LOG.debug("Attempting to connect to LogSearch server at {}", logFileNamesURI);

      HttpURLConnection httpURLConnection  = (HttpURLConnection) logFileNamesURI.toURL().openConnection();
      secure(httpURLConnection, protocol);
      httpURLConnection.setRequestMethod("GET");

      addCookiesFromCookieStore(httpURLConnection);

      setupCredentials(httpURLConnection);

      StringBuffer buffer = networkConnection.readQueryResponseFromServer(httpURLConnection);

      addCookiesToCookieStoreFromResponse(httpURLConnection);

      // setup a reader for the JSON response
      StringReader stringReader =
        new StringReader(buffer.toString());

      ObjectReader hostFilesQueryResponseReader = createObjectReader(HostLogFilesResponse.class);

      HostLogFilesResponse response = hostFilesQueryResponseReader.readValue(stringReader);

      if (LOG.isDebugEnabled() && response != null && MapUtils.isNotEmpty(response.getHostLogFiles())) {
        for (Map.Entry<String, List<String>> componentEntry : response.getHostLogFiles().entrySet()) {
          LOG.debug("Log files for component '{}' : {}", componentEntry.getKey(), StringUtils.join(componentEntry.getValue(), ","));
        }
      }
      return response;
    } catch (Exception e) {
      Utils.logErrorMessageWithThrowableWithCounter(LOG, errorLogCounterForLogSearchConnectionExceptions,
        "Error occurred while trying to connect to the LogSearch service...", e);
    }
    return null;
  }

  @Override
  public LogLevelQueryResponse sendLogLevelQueryRequest(String componentName, String hostName) {
    try {
      // use the Apache builder to create the correct URI
      URI logLevelQueryURI = createLogLevelQueryURI(protocol, componentName, hostName);
      LOG.debug("Attempting to connect to LogSearch server at {}", logLevelQueryURI);

      HttpURLConnection httpURLConnection  = (HttpURLConnection) logLevelQueryURI.toURL().openConnection();
      secure(httpURLConnection, protocol);
      httpURLConnection.setRequestMethod("GET");

      addCookiesFromCookieStore(httpURLConnection);

      setupCredentials(httpURLConnection);

      StringBuffer buffer = networkConnection.readQueryResponseFromServer(httpURLConnection);

      addCookiesToCookieStoreFromResponse(httpURLConnection);

      // setup a reader for the JSON response
      StringReader stringReader =
        new StringReader(buffer.toString());

      ObjectReader logQueryResponseReader = createObjectReader(LogLevelQueryResponse.class);

      return logQueryResponseReader.readValue(stringReader);

    } catch (Exception e) {
      Utils.logErrorMessageWithThrowableWithCounter(LOG, errorLogCounterForLogSearchConnectionExceptions,
        "Error occurred while trying to connect to the LogSearch service...", e);
    }

    return null;
  }

  /**
   * Generates the log file tail URI, using the LogSearch server's
   * query parameters.
   *
   * @param baseURI the base URI for this request, typically the URI to the
   *                Ambari Integration searchEngine component
   *
   * @param componentName the component name
   * @param hostName the host name
   *
   * @return
   */
  @Override
  public String createLogFileTailURI(String baseURI, String componentName, String hostName) {
    return baseURI + "?" + COMPONENT_QUERY_PARAMETER_NAME + "=" + componentName + "&" + HOST_QUERY_PARAMETER_NAME + "=" + hostName
      + "&" + PAGE_SIZE_QUERY_PARAMETER_NAME + "=" + DEFAULT_PAGE_SIZE;
  }

  private static ObjectReader createObjectReader(Class type) {
    // setup the Jackson mapper/reader to read in the data structure
    ObjectMapper mapper = createJSONObjectMapper();

    return mapper.reader(type);
  }

  private URI createLogSearchQueryURI(String scheme, Map<String, String> queryParameters) throws URISyntaxException {
    URIBuilder uriBuilder = createBasicURI(scheme);
    appendUriPath(uriBuilder, LOGSEARCH_QUERY_PATH);

    // set the current cluster name, in case this LogSearch service supports data
    // for multiple clusters
    uriBuilder.addParameter(LOGSEARCH_CLUSTERS_QUERY_PARAMETER_NAME, cluster.getClusterName());

    // add any query strings specified
    for (String key : queryParameters.keySet()) {
      uriBuilder.addParameter(key, queryParameters.get(key));
    }

    return uriBuilder.build();
  }

  private URIBuilder createBasicURI(String scheme) throws URISyntaxException {
    URIBuilder uriBuilder = new URIBuilder();
    if (StringUtils.isNotBlank(externalAddress)) {
      final URI uri = new URI(externalAddress);
      uriBuilder.setScheme(uri.getScheme());
      uriBuilder.setHost(uri.getHost());
      if (uri.getPort() != -1) {
        uriBuilder.setPort(uri.getPort());
      }
      // add path as well to make it work with proxies like: https://sample.org/logsearch
      if (StringUtils.isNotBlank(uri.getPath())) {
        uriBuilder.setPath(uri.getPath());
      }
    } else {
      uriBuilder.setScheme(scheme);
      uriBuilder.setHost(hostName);
      uriBuilder.setPort(Integer.parseInt(portNumber));
    }
    return uriBuilder;
  }

  private void appendUriPath(URIBuilder uriBuilder, String path) {
    if (StringUtils.isNotEmpty(uriBuilder.getPath())) {
      uriBuilder.setPath(uriBuilder.getPath() + path);
    } else {
      uriBuilder.setPath(path);
    }
  }

  private URI createLogLevelQueryURI(String scheme, String componentName, String hostName) throws URISyntaxException {
    URIBuilder uriBuilder = createBasicURI(scheme);
    appendUriPath(uriBuilder, LOGSEARCH_GET_LOG_LEVELS_PATH);

    Map<String, String> queryParameters = new HashMap<>();
    // set the query parameters to limit this level count
    // request to the specific component on the specified host
    queryParameters.put(HOST_QUERY_PARAMETER_NAME, hostName);
    queryParameters.put(COMPONENT_QUERY_PARAMETER_NAME,componentName);

    // add any query strings specified
    for (String key : queryParameters.keySet()) {
      uriBuilder.addParameter(key, queryParameters.get(key));
    }

    return uriBuilder.build();
  }



  protected static ObjectMapper createJSONObjectMapper() {
    ObjectMapper mapper =
      new ObjectMapper();
    AnnotationIntrospector introspector =
      new JacksonAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    return mapper;
  }

  private PrincipalKeyCredential getLogSearchCredentials() {
    try {
      Credential credential =
        credentialStoreService.getCredential(cluster.getClusterName(), LOGSEARCH_ADMIN_CREDENTIAL_NAME);
      if ((credential != null)  && (credential instanceof PrincipalKeyCredential)) {
        return (PrincipalKeyCredential)credential;
      }

      if (credential == null) {
        LOG.debug("LogSearch credentials could not be obtained from store.");
      } else {
        LOG.debug("LogSearch credentials were not of the correct type, this is likely an error in configuration, credential type is = {}",
          credential.getClass().getName());
      }
    } catch (AmbariException ambariException) {
      LOG.debug("Error encountered while trying to obtain LogSearch admin credentials.", ambariException);
    }

    return null;
  }

  private static String createEncodedCredentials(PrincipalKeyCredential principalKeyCredential) {
    return createEncodedCredentials(principalKeyCredential.getPrincipal(), new String(principalKeyCredential.getKey()));
  }

  private static String createEncodedCredentials(String userName, String password) {
    return Base64.encodeBase64String((userName + ":" + password).getBytes());
  }

  /**
   * Interface used to abstract out the network access needed to
   * connect to the LogSearch Server.
   *
   * This abstraction is useful for unit testing this class, and simulating
   * different output and error conditions.
   */
  interface NetworkConnection {
    StringBuffer readQueryResponseFromServer(HttpURLConnection httpURLConnection) throws IOException;

    void setupBasicAuthentication(HttpURLConnection httpURLConnection, String encodedCredentials);
  }

  /**
   * The default implementation of NetworkConnection, that reads
   * the InputStream associated with the HttpURL connection passed in.
   */
  private static class DefaultNetworkConnection implements NetworkConnection {
    @Override
    public StringBuffer readQueryResponseFromServer(HttpURLConnection httpURLConnection) throws IOException {
      InputStream resultStream = null;
      try {
        // read in the response from LogSearch
        resultStream = httpURLConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(resultStream));
        LOG.debug("Response code from LogSearch Service is = {}", httpURLConnection.getResponseCode());


        String line = reader.readLine();
        StringBuffer buffer = new StringBuffer();
        while (line != null) {
          buffer.append(line);
          line = reader.readLine();
        }

        LOG.debug("Sucessfully retrieved response from server, response = {}", buffer);

        return buffer;
      } finally {
        // make sure to close the stream after request is completed
        if (resultStream != null) {
          resultStream.close();
        }
      }
    }

    @Override
    public void setupBasicAuthentication(HttpURLConnection httpURLConnection, String encodedCredentials) {
      // default implementation for this method should just set the Authorization header
      // required for Basic Authentication to the LogSearch Server
      httpURLConnection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
    }
  }


}
