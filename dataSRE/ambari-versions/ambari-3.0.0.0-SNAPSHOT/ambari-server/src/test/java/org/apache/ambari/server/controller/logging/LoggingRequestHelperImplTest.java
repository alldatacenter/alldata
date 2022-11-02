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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.commons.codec.binary.Base64;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;


public class LoggingRequestHelperImplTest {

  private static final String TEST_JSON_INPUT_TWO_LIST_ENTRIES =
    "{" +
      "  \"startIndex\" : 0," +
      "  \"pageSize\" : 5," +
      "  \"totalCount\" : 10452," +
      "  \"resultSize\" : 5," +
      "  \"queryTimeMS\" : 1458148754113," +
      "  \"logList\" : [ {" +
      "    \"cluster\" : \"clusterone\"," +
      "    \"method\" : \"chooseUnderReplicatedBlocks\"," +
      "    \"level\" : \"INFO\"," +
      "    \"event_count\" : 1," +
      "    \"ip\" : \"192.168.1.1\"," +
      "    \"type\" : \"hdfs_namenode\"," +
      "    \"seq_num\" : 10584," +
      "    \"path\" : \"/var/log/hadoop/hdfs/hadoop-hdfs-namenode-c6401.ambari.apache.org.log\"," +
      "    \"file\" : \"UnderReplicatedBlocks.java\"," +
      "    \"line_number\" : 394," +
      "    \"host\" : \"c6401.ambari.apache.org\"," +
      "    \"log_message\" : \"chooseUnderReplicatedBlocks selected 2 blocks at priority level 0;  Total=2 Reset bookmarks? false\"," +
      "    \"logger_name\" : \"BlockStateChange\"," +
      "    \"id\" : \"9c5562fb-123f-47c8-aaf5-b5e407326c08\"," +
      "    \"message_md5\" : \"-3892769501348410581\"," +
      "    \"logtime\" : 1458148749036," +
      "    \"event_md5\" : \"1458148749036-2417481968206345035\"," +
      "    \"logfile_line_number\" : 2084," +
      "    \"_ttl_\" : \"+7DAYS\"," +
      "    \"_expire_at_\" : 1458753550322," +
      "    \"_version_\" : 1528979784023932928" +
      "  }, {" +
      "    \"cluster\" : \"clusterone\"," +
      "    \"method\" : \"putMetrics\"," +
      "    \"level\" : \"WARN\"," +
      "    \"event_count\" : 1," +
      "    \"ip\" : \"192.168.1.1\"," +
      "    \"type\" : \"yarn_resourcemanager\"," +
      "    \"seq_num\" : 10583," +
      "    \"path\" : \"/var/log/hadoop-yarn/yarn/yarn-yarn-resourcemanager-c6401.ambari.apache.org.log\"," +
      "    \"file\" : \"HadoopTimelineMetricsSink.java\"," +
      "    \"line_number\" : 262," +
      "    \"host\" : \"c6401.ambari.apache.org\"," +
      "    \"log_message\" : \"Unable to send metrics to collector by address:http://c6401.ambari.apache.org:6188/ws/v1/timeline/metrics\"," +
      "    \"logger_name\" : \"timeline.HadoopTimelineMetricsSink\"," +
      "    \"id\" : \"8361c5a9-5b1c-4f44-bc8f-4c6f07d94228\"," +
      "    \"message_md5\" : \"5942185045779825717\"," +
      "    \"logtime\" : 1458148746937," +
      "    \"event_md5\" : \"14581487469371427138486123628676\"," +
      "    \"logfile_line_number\" : 549," +
      "    \"_ttl_\" : \"+7DAYS\"," +
      "    \"_expire_at_\" : 1458753550322," +
      "    \"_version_\" : 1528979784022884357" +
      "  }" +
      "]" +
      "}";

  private static final String TEST_JSON_INPUT_LOG_FILES_MAP =
    "{" +
    "\"hostLogFiles\":{" +
    "\"hdfs_namenode\": [" +
    "\"/var/log/hadoop/hdfs/hadoop-hdfs-namenode-c6401.ambari.apache.org.log\"" +
    "],\"logsearch_app\": [" +
    "\"/var/log/ambari-logsearch-portal/logsearch.json\"" +
    "]" +
    "}}";

  private static final String TEST_JSON_INPUT_LOG_LEVEL_QUERY =
    "{\"pageSize\":\"0\",\"queryTimeMS\":\"1459970731998\",\"resultSize\":\"6\",\"startIndex\":\"0\",\"totalCount\":\"0\"," +
      "\"vNameValues\":[{\"name\":\"FATAL\",\"value\":\"0\"},{\"name\":\"ERROR\",\"value\":\"0\"}," +
      "{\"name\":\"WARN\",\"value\":\"41\"},{\"name\":\"INFO\",\"value\":\"186\"},{\"name\":\"DEBUG\",\"value\":\"0\"}," +
      "{\"name\":\"TRACE\",\"value\":\"0\"}]}";

  private static final String TEST_JSON_INPUT_NULL_LOG_LIST =
    "{\"startIndex\":0,\"pageSize\":0,\"totalCount\":0,\"resultSize\":0,\"sortType\":null,\"sortBy\":null,\"queryTimeMS\":1479850014987,\"logList\":null,\"listSize\":0}";


  private final String EXPECTED_HOST_NAME = "c6401.ambari.apache.org";

  private final String EXPECTED_PORT_NUMBER = "61888";

  private static final String EXPECTED_USER_NAME = "admin-user";

  private static final String EXPECTED_ADMIN_PASSWORD = "admin-pwd";

  private static final String EXPECTED_PROTOCOL = "http";

  private static final String EXPECTED_ENCODED_CREDENTIALS =
    Base64.encodeBase64String((EXPECTED_USER_NAME + ":" + EXPECTED_ADMIN_PASSWORD).getBytes());



  @Test
  public void testLogQueryRequestBasic() throws Exception {
    LoggingCookieStore.INSTANCE.getCookiesMap().clear();
    EasyMockSupport mockSupport =
      new EasyMockSupport();

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperImpl.NetworkConnection networkConnectionMock =
      mockSupport.createMock(LoggingRequestHelperImpl.NetworkConnection.class);

    Config adminPropertiesConfigMock =
      mockSupport.createMock(Config.class);

    Map<String, String> testConfigProperties =
      new HashMap<>();

    testConfigProperties.put("logsearch_admin_username", EXPECTED_USER_NAME);
    testConfigProperties.put("logsearch_admin_password", EXPECTED_ADMIN_PASSWORD);
    testConfigProperties = Collections.unmodifiableMap(testConfigProperties);

    Capture<HttpURLConnection> captureURLConnection = EasyMock.newCapture();
    Capture<HttpURLConnection> captureURLConnectionForAuthentication = EasyMock.newCapture();

    expect(clusterMock.getDesiredConfigByType("logsearch-admin-json")).andReturn(adminPropertiesConfigMock).atLeastOnce();
    expect(clusterMock.getClusterName()).andReturn("clusterone").atLeastOnce();
    expect(adminPropertiesConfigMock.getProperties()).andReturn(testConfigProperties).atLeastOnce();
    expect(networkConnectionMock.readQueryResponseFromServer(capture(captureURLConnection))).andReturn(new StringBuffer(TEST_JSON_INPUT_TWO_LIST_ENTRIES)).atLeastOnce();

    // expect that basic authentication is setup, with the expected encoded credentials
    networkConnectionMock.setupBasicAuthentication(capture(captureURLConnectionForAuthentication), eq(EXPECTED_ENCODED_CREDENTIALS));

    mockSupport.replayAll();

    LoggingRequestHelper helper =
      new LoggingRequestHelperImpl(EXPECTED_HOST_NAME, EXPECTED_PORT_NUMBER, EXPECTED_PROTOCOL, credentialStoreServiceMock, clusterMock, null, networkConnectionMock);

    // invoke query request
    LogQueryResponse result =
      helper.sendQueryRequest(Collections.emptyMap());

    // verify that the HttpURLConnection was created with the proper values
    HttpURLConnection httpURLConnection =
      captureURLConnection.getValue();

    assertEquals("URLConnection did not have the correct hostname information",
      EXPECTED_HOST_NAME, httpURLConnection.getURL().getHost());
    assertEquals("URLConnection did not have the correct port information",
      EXPECTED_PORT_NUMBER, httpURLConnection.getURL().getPort() + "");
    assertEquals("URLConnection did not have the expected http protocol scheme",
      "http", httpURLConnection.getURL().getProtocol());
    assertEquals("URLConnection did not have the expected method set",
      "GET", httpURLConnection.getRequestMethod());
    assertTrue("URLConnection's URL did not have the expected query parameter string",
      httpURLConnection.getURL().getQuery().contains("clusters=clusterone"));

    assertSame("HttpUrlConnection instances passed into NetworkConnection mock should have been the same instance",
      httpURLConnection, captureURLConnectionForAuthentication.getValue());


    assertNotNull("Response object should not be null",
      result);

    // verify that the JSON response returned from the simulated server
    // is parsed properly, and has the expected values
    assertEquals("startIndex not parsed properly",
      "0", result.getStartIndex());
    assertEquals("pageSize not parsed properly",
      "5", result.getPageSize());
    assertEquals("totalCount not parsed properly",
      "10452", result.getTotalCount());
    assertEquals("resultSize not parsed properly",
      "5", result.getResultSize());
    assertEquals("queryTimeMS not parsed properly",
      "1458148754113", result.getQueryTimeMS());

    assertEquals("incorrect number of LogLineResult items parsed",
      2, result.getListOfResults().size());

    List<LogLineResult> listOfLineResults =
      result.getListOfResults();

    verifyFirstLine(listOfLineResults);
    verifySecondLine(listOfLineResults);

    mockSupport.verifyAll();
  }

  @Test
  public void testLogLevelRequestBasic() throws Exception {
    LoggingCookieStore.INSTANCE.getCookiesMap().clear();
    EasyMockSupport mockSupport =
      new EasyMockSupport();

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperImpl.NetworkConnection networkConnectionMock =
      mockSupport.createMock(LoggingRequestHelperImpl.NetworkConnection.class);

    Config adminPropertiesConfigMock =
      mockSupport.createMock(Config.class);

    Map<String, String> testConfigProperties =
      new HashMap<>();
    testConfigProperties.put("logsearch_admin_username", "admin-user");
    testConfigProperties.put("logsearch_admin_password", "admin-pwd");
    testConfigProperties = Collections.unmodifiableMap(testConfigProperties);

    Capture<HttpURLConnection> captureURLConnection = EasyMock.newCapture();
    Capture<HttpURLConnection> captureURLConnectionForAuthentication = EasyMock.newCapture();

    expect(clusterMock.getDesiredConfigByType("logsearch-admin-json")).andReturn(adminPropertiesConfigMock).atLeastOnce();
    expect(adminPropertiesConfigMock.getProperties()).andReturn(testConfigProperties).atLeastOnce();
    expect(networkConnectionMock.readQueryResponseFromServer(capture(captureURLConnection))).andReturn(new StringBuffer(TEST_JSON_INPUT_LOG_LEVEL_QUERY)).atLeastOnce();

    // expect that basic authentication is setup, with the expected encoded credentials
    networkConnectionMock.setupBasicAuthentication(capture(captureURLConnectionForAuthentication), eq(EXPECTED_ENCODED_CREDENTIALS));


    mockSupport.replayAll();


    LoggingRequestHelper helper =
      new LoggingRequestHelperImpl(EXPECTED_HOST_NAME, EXPECTED_PORT_NUMBER, EXPECTED_PROTOCOL, credentialStoreServiceMock, clusterMock, null, networkConnectionMock);

    // invoke query request
    LogLevelQueryResponse result =
      helper.sendLogLevelQueryRequest("hdfs_datanode", EXPECTED_HOST_NAME);

    // verify that the HttpURLConnection was created with the proper values
    HttpURLConnection httpURLConnection =
      captureURLConnection.getValue();

    assertEquals("URLConnection did not have the correct hostname information",
      EXPECTED_HOST_NAME, httpURLConnection.getURL().getHost());
    assertEquals("URLConnection did not have the correct port information",
      EXPECTED_PORT_NUMBER, httpURLConnection.getURL().getPort() + "");
    assertEquals("URLConnection did not have the expected http protocol scheme",
      "http", httpURLConnection.getURL().getProtocol());
    assertEquals("URLConnection did not have the expected method set",
      "GET", httpURLConnection.getRequestMethod());

    assertSame("HttpUrlConnection instances passed into NetworkConnection mock should have been the same instance",
      httpURLConnection, captureURLConnectionForAuthentication.getValue());

    assertNotNull("Response object should not be null",
      result);

    // expected values taken from JSON input string declared above
    assertEquals("startIndex not parsed properly",
      "0", result.getStartIndex());
    assertEquals("pageSize not parsed properly",
      "0", result.getPageSize());
    assertEquals("totalCount not parsed properly",
      "0", result.getTotalCount());
    assertEquals("resultSize not parsed properly",
      "6", result.getResultSize());
    assertEquals("queryTimeMS not parsed properly",
      "1459970731998", result.getQueryTimeMS());

    assertEquals("Incorrect number of log level count items parsed",
      6, result.getNameValueList().size());

    List<NameValuePair> resultList =
      result.getNameValueList();
    assertNameValuePair("FATAL", "0", resultList.get(0));
    assertNameValuePair("ERROR", "0", resultList.get(1));
    assertNameValuePair("WARN", "41", resultList.get(2));
    assertNameValuePair("INFO", "186", resultList.get(3));
    assertNameValuePair("DEBUG", "0", resultList.get(4));
    assertNameValuePair("TRACE", "0", resultList.get(5));

    mockSupport.verifyAll();
  }

  @Test
  public void testLogFileNameRequestBasic() throws Exception {
    LoggingCookieStore.INSTANCE.getCookiesMap().clear();
    final String expectedComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperImpl.NetworkConnection networkConnectionMock =
      mockSupport.createMock(LoggingRequestHelperImpl.NetworkConnection.class);

    Config adminPropertiesConfigMock =
      mockSupport.createMock(Config.class);

    Map<String, String> testConfigProperties =
      new HashMap<>();
    testConfigProperties.put("logsearch_admin_username", "admin-user");
    testConfigProperties.put("logsearch_admin_password", "admin-pwd");
    testConfigProperties = Collections.unmodifiableMap(testConfigProperties);

    Capture<HttpURLConnection> captureURLConnection = EasyMock.newCapture();
    Capture<HttpURLConnection> captureURLConnectionForAuthentication = EasyMock.newCapture();

    expect(clusterMock.getDesiredConfigByType("logsearch-admin-json")).andReturn(adminPropertiesConfigMock).atLeastOnce();
    expect(clusterMock.getClusterName()).andReturn("clusterone").atLeastOnce();
    expect(adminPropertiesConfigMock.getProperties()).andReturn(testConfigProperties).atLeastOnce();
    expect(networkConnectionMock.readQueryResponseFromServer(capture(captureURLConnection))).andReturn(new StringBuffer(TEST_JSON_INPUT_LOG_FILES_MAP)).atLeastOnce();

    // expect that basic authentication is setup, with the expected encoded credentials
    networkConnectionMock.setupBasicAuthentication(capture(captureURLConnectionForAuthentication), eq(EXPECTED_ENCODED_CREDENTIALS));

    mockSupport.replayAll();

    LoggingRequestHelper helper =
      new LoggingRequestHelperImpl(EXPECTED_HOST_NAME, EXPECTED_PORT_NUMBER, EXPECTED_PROTOCOL, credentialStoreServiceMock, clusterMock, null, networkConnectionMock);

    // invoke query request
    HostLogFilesResponse result =
      helper.sendGetLogFileNamesRequest(EXPECTED_HOST_NAME);

    // verify that the HttpURLConnection was created with the propert values
    HttpURLConnection httpURLConnection =
      captureURLConnection.getValue();

    assertEquals("URLConnection did not have the correct hostname information",
      EXPECTED_HOST_NAME, httpURLConnection.getURL().getHost());
    assertEquals("URLConnection did not have the correct port information",
      EXPECTED_PORT_NUMBER, httpURLConnection.getURL().getPort() + "");
    assertEquals("URLConnection did not have the expected http protocol scheme",
      "http", httpURLConnection.getURL().getProtocol());
    assertEquals("URLConnection did not have the expected method set",
      "GET", httpURLConnection.getRequestMethod());
    assertTrue("URLConnection's URL did not have the expected query parameter string",
      httpURLConnection.getURL().getQuery().contains("clusters=clusterone"));

    assertSame("HttpUrlConnection instances passed into NetworkConnection mock should have been the same instance",
      httpURLConnection, captureURLConnectionForAuthentication.getValue());

    final String resultQuery =
      httpURLConnection.getURL().getQuery();

    // verify that the query contains the three required parameters
    assertTrue("host_name parameter was not included in query",
      resultQuery.contains("host_name=c6401.ambari.apache.org"));

    assertNotNull("Response object should not be null",
      result);
    assertEquals("Response Set was not of the expected size",
      2, result.getHostLogFiles().size());
    assertEquals("Response did not include the expected file name",
      "/var/log/hadoop/hdfs/hadoop-hdfs-namenode-c6401.ambari.apache.org.log",
      result.getHostLogFiles().get(expectedComponentName).get(0));

    mockSupport.verifyAll();
  }

  @Test
  public void testLogFileNameRequestWithNullLogList() throws Exception {
    final String expectedComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperImpl.NetworkConnection networkConnectionMock =
      mockSupport.createMock(LoggingRequestHelperImpl.NetworkConnection.class);

    Config adminPropertiesConfigMock =
      mockSupport.createMock(Config.class);

    Map<String, String> testConfigProperties =
      new HashMap<>();
    testConfigProperties.put("logsearch_admin_username", "admin-user");
    testConfigProperties.put("logsearch_admin_password", "admin-pwd");
    testConfigProperties = Collections.unmodifiableMap(testConfigProperties);

    Capture<HttpURLConnection> captureURLConnection = new Capture<>();
    Capture<HttpURLConnection> captureURLConnectionForAuthentication = new Capture<>();

    expect(clusterMock.getDesiredConfigByType("logsearch-admin-json")).andReturn(adminPropertiesConfigMock).atLeastOnce();
    expect(clusterMock.getClusterName()).andReturn("clusterone").atLeastOnce();
    expect(adminPropertiesConfigMock.getProperties()).andReturn(testConfigProperties).atLeastOnce();
    expect(networkConnectionMock.readQueryResponseFromServer(capture(captureURLConnection))).andReturn(new StringBuffer(TEST_JSON_INPUT_NULL_LOG_LIST)).atLeastOnce();

    // expect that basic authentication is setup, with the expected encoded credentials
    networkConnectionMock.setupBasicAuthentication(capture(captureURLConnectionForAuthentication), eq(EXPECTED_ENCODED_CREDENTIALS));

    mockSupport.replayAll();

    LoggingRequestHelper helper =
      new LoggingRequestHelperImpl(EXPECTED_HOST_NAME, EXPECTED_PORT_NUMBER, EXPECTED_PROTOCOL, credentialStoreServiceMock, clusterMock, null, networkConnectionMock);

    // invoke query request
    HostLogFilesResponse result =
      helper.sendGetLogFileNamesRequest(EXPECTED_HOST_NAME);

    // verify that the HttpURLConnection was created with the propert values
    HttpURLConnection httpURLConnection =
      captureURLConnection.getValue();

    assertEquals("URLConnection did not have the correct hostname information",
      EXPECTED_HOST_NAME, httpURLConnection.getURL().getHost());
    assertEquals("URLConnection did not have the correct port information",
      EXPECTED_PORT_NUMBER, httpURLConnection.getURL().getPort() + "");
    assertEquals("URLConnection did not have the expected http protocol scheme",
      "http", httpURLConnection.getURL().getProtocol());
    assertEquals("URLConnection did not have the expected method set",
      "GET", httpURLConnection.getRequestMethod());
    assertTrue("URLConnection's URL did not have the expected query parameter string",
      httpURLConnection.getURL().getQuery().contains("clusters=clusterone"));

    assertSame("HttpUrlConnection instances passed into NetworkConnection mock should have been the same instance",
      httpURLConnection, captureURLConnectionForAuthentication.getValue());

    final String resultQuery =
      httpURLConnection.getURL().getQuery();

    // verify that the query contains the three required parameters
    assertTrue("host_name parameter was not included in query",
      resultQuery.contains("host_name=c6401.ambari.apache.org"));

    assertNotNull("Response object should not be null",
      result);
    assertNull("Response Map should be null", result.getHostLogFiles());

    mockSupport.verifyAll();
  }

  /**
   * Verifies that if the LogSearch admin configuration for user/password credentials
   * is not available, the integration layer will attempt to locate the LogSearch credential
   * in the CredentialStoreService as a fallback mechanism.
   *
   * @throws Exception
   */
  @Test
  public void testLogQueryRequestBasicCredentialsNotInConfig() throws Exception {
    LoggingCookieStore.INSTANCE.getCookiesMap().clear();
    final String expectedClusterName = "my-test-cluster";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperImpl.NetworkConnection networkConnectionMock =
      mockSupport.createMock(LoggingRequestHelperImpl.NetworkConnection.class);

    Config adminPropertiesConfigMock =
      mockSupport.createMock(Config.class);

    Capture<HttpURLConnection> captureURLConnection = EasyMock.newCapture();
    Capture<HttpURLConnection> captureURLConnectionForAuthentication = EasyMock.newCapture();

    expect(clusterMock.getDesiredConfigByType("logsearch-admin-json")).andReturn(adminPropertiesConfigMock).atLeastOnce();
    expect(clusterMock.getClusterName()).andReturn(expectedClusterName).atLeastOnce();
    expect(adminPropertiesConfigMock.getProperties()).andReturn(Collections.emptyMap()).atLeastOnce();
    expect(networkConnectionMock.readQueryResponseFromServer(capture(captureURLConnection))).andReturn(new StringBuffer(TEST_JSON_INPUT_TWO_LIST_ENTRIES)).atLeastOnce();
    // the credential store service should be consulted in this case, in order
    // to attempt to obtain the LogSearch credential from the store
    expect(credentialStoreServiceMock.getCredential(expectedClusterName, "logsearch.admin.credential")).andReturn(new PrincipalKeyCredential(EXPECTED_USER_NAME, EXPECTED_ADMIN_PASSWORD)).atLeastOnce();

    // expect that basic authentication is setup, with the expected encoded credentials
    networkConnectionMock.setupBasicAuthentication(capture(captureURLConnectionForAuthentication), eq(EXPECTED_ENCODED_CREDENTIALS));

    mockSupport.replayAll();

    LoggingRequestHelper helper =
      new LoggingRequestHelperImpl(EXPECTED_HOST_NAME, EXPECTED_PORT_NUMBER, EXPECTED_PROTOCOL, credentialStoreServiceMock, clusterMock, null, networkConnectionMock);

    // invoke query request
    LogQueryResponse result =
      helper.sendQueryRequest(Collections.emptyMap());

    // verify that the HttpURLConnection was created with the proper values
    HttpURLConnection httpURLConnection =
      captureURLConnection.getValue();

    assertEquals("URLConnection did not have the correct hostname information",
      EXPECTED_HOST_NAME, httpURLConnection.getURL().getHost());
    assertEquals("URLConnection did not have the correct port information",
      EXPECTED_PORT_NUMBER, httpURLConnection.getURL().getPort() + "");
    assertEquals("URLConnection did not have the expected http protocol scheme",
      "http", httpURLConnection.getURL().getProtocol());
    assertEquals("URLConnection did not have the expected method set",
      "GET", httpURLConnection.getRequestMethod());

    assertSame("HttpUrlConnection instances passed into NetworkConnection mock should have been the same instance",
      httpURLConnection, captureURLConnectionForAuthentication.getValue());


    assertNotNull("Response object should not be null",
      result);

    // verify that the JSON response returned from the simulated server
    // is parsed properly, and has the expected values
    assertEquals("startIndex not parsed properly",
      "0", result.getStartIndex());
    assertEquals("pageSize not parsed properly",
      "5", result.getPageSize());
    assertEquals("totalCount not parsed properly",
      "10452", result.getTotalCount());
    assertEquals("resultSize not parsed properly",
      "5", result.getResultSize());
    assertEquals("queryTimeMS not parsed properly",
      "1458148754113", result.getQueryTimeMS());

    assertEquals("incorrect number of LogLineResult items parsed",
      2, result.getListOfResults().size());

    List<LogLineResult> listOfLineResults =
      result.getListOfResults();

    verifyFirstLine(listOfLineResults);
    verifySecondLine(listOfLineResults);

    mockSupport.verifyAll();
  }

  private static void verifyFirstLine(List<LogLineResult> listOfLineResults) {
    LogLineResult resultOne = listOfLineResults.get(0);
    // verify that all fields in this class are parsed as expected
    assertEquals("Cluster name not parsed properly",
      "clusterone", resultOne.getClusterName());
    assertEquals("Method Name not parsed properly",
      "chooseUnderReplicatedBlocks", resultOne.getLogMethod());
    assertEquals("Log Level not parsed properly",
      "INFO", resultOne.getLogLevel());
    assertEquals("event_count not parsed properly",
      "1", resultOne.getEventCount());
    assertEquals("ip address not parsed properly",
      "192.168.1.1", resultOne.getIpAddress());
    assertEquals("component type not parsed properly",
      "hdfs_namenode", resultOne.getComponentType());
    assertEquals("sequence number not parsed properly",
      "10584", resultOne.getSequenceNumber());
    assertEquals("log file path not parsed properly",
      "/var/log/hadoop/hdfs/hadoop-hdfs-namenode-c6401.ambari.apache.org.log", resultOne.getLogFilePath());
    assertEquals("log src file name not parsed properly",
      "UnderReplicatedBlocks.java", resultOne.getSourceFile());
    assertEquals("log src line number not parsed properly",
      "394", resultOne.getSourceFileLineNumber());
    assertEquals("host name not parsed properly",
      "c6401.ambari.apache.org", resultOne.getHostName());
    assertEquals("log message not parsed properly",
      "chooseUnderReplicatedBlocks selected 2 blocks at priority level 0;  Total=2 Reset bookmarks? false", resultOne.getLogMessage());
    assertEquals("logger name not parsed properly",
      "BlockStateChange", resultOne.getLoggerName());
    assertEquals("id not parsed properly",
      "9c5562fb-123f-47c8-aaf5-b5e407326c08", resultOne.getId());
    assertEquals("message MD5 not parsed properly",
      "-3892769501348410581", resultOne.getMessageMD5());
    assertEquals("log time not parsed properly",
      "1458148749036", resultOne.getLogTime());
    assertEquals("event MD5 not parsed properly",
      "1458148749036-2417481968206345035", resultOne.getEventMD5());
    assertEquals("logfile line number not parsed properly",
      "2084", resultOne.getLogFileLineNumber());
    assertEquals("ttl not parsed properly",
      "+7DAYS", resultOne.getTtl());
    assertEquals("expire at not parsed properly",
      "1458753550322", resultOne.getExpirationTime());
    assertEquals("version not parsed properly",
      "1528979784023932928", resultOne.getVersion());
  }

  private static void verifySecondLine(List<LogLineResult> listOfLineResults) {
    LogLineResult resultTwo = listOfLineResults.get(1);
    // verify second log line record's data is parsed correctly
    assertEquals("Cluster name not parsed properly",
      "clusterone", resultTwo.getClusterName());
    assertEquals("Method Name not parsed properly",
      "putMetrics", resultTwo.getLogMethod());
    assertEquals("Log Level not parsed properly",
      "WARN", resultTwo.getLogLevel());
    assertEquals("event_count not parsed properly",
      "1", resultTwo.getEventCount());
    assertEquals("ip address not parsed properly",
      "192.168.1.1", resultTwo.getIpAddress());
    assertEquals("component type not parsed properly",
      "yarn_resourcemanager", resultTwo.getComponentType());
    assertEquals("sequence number not parsed properly",
      "10583", resultTwo.getSequenceNumber());
    assertEquals("log file path not parsed properly",
      "/var/log/hadoop-yarn/yarn/yarn-yarn-resourcemanager-c6401.ambari.apache.org.log", resultTwo.getLogFilePath());
    assertEquals("log src file name not parsed properly",
      "HadoopTimelineMetricsSink.java", resultTwo.getSourceFile());
    assertEquals("log src line number not parsed properly",
      "262", resultTwo.getSourceFileLineNumber());
    assertEquals("host name not parsed properly",
      "c6401.ambari.apache.org", resultTwo.getHostName());
    assertEquals("log message not parsed properly",
      "Unable to send metrics to collector by address:http://c6401.ambari.apache.org:6188/ws/v1/timeline/metrics", resultTwo.getLogMessage());
    assertEquals("logger name not parsed properly",
      "timeline.HadoopTimelineMetricsSink", resultTwo.getLoggerName());
    assertEquals("id not parsed properly",
      "8361c5a9-5b1c-4f44-bc8f-4c6f07d94228", resultTwo.getId());
    assertEquals("message MD5 not parsed properly",
      "5942185045779825717", resultTwo.getMessageMD5());
    assertEquals("log time not parsed properly",
      "1458148746937", resultTwo.getLogTime());
    assertEquals("event MD5 not parsed properly",
      "14581487469371427138486123628676", resultTwo.getEventMD5());
    assertEquals("logfile line number not parsed properly",
      "549", resultTwo.getLogFileLineNumber());
    assertEquals("ttl not parsed properly",
      "+7DAYS", resultTwo.getTtl());
    assertEquals("expire at not parsed properly",
      "1458753550322", resultTwo.getExpirationTime());
    assertEquals("version not parsed properly",
      "1528979784022884357", resultTwo.getVersion());
  }

  @Test
  public void testCreateLogFileTailURI() throws Exception {
    LoggingCookieStore.INSTANCE.getCookiesMap().clear();
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedPort = "61888";
    final String expectedComponentName = "hdfs_namenode";

    final String expectedBaseURI =
      "http://" + expectedHostName + ":" + expectedPort + "/api/v1/clusters/clusterone/logging/searchEngine";

    final String expectedTailFileURI = expectedBaseURI + "?component_name=" + expectedComponentName
      + "&host_name=" + expectedHostName + "&pageSize=50";


    EasyMockSupport mockSupport =
      new EasyMockSupport();

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);
    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);
    LoggingRequestHelperImpl.NetworkConnection networkConnectionMock =
      mockSupport.createMock(LoggingRequestHelperImpl.NetworkConnection.class);

    mockSupport.replayAll();

    LoggingRequestHelper helper =
      new LoggingRequestHelperImpl("c6401.ambari.apache.org", "61888", "http", credentialStoreServiceMock, clusterMock, null, networkConnectionMock);

    String result = helper.createLogFileTailURI(expectedBaseURI, expectedComponentName, expectedHostName);

    // verify that the URI contains the expected LogSearch query parameters,
    // including the correct default page size
    assertEquals("LogFile Tail URI was not generated as expected",
      expectedTailFileURI,
      result);


    mockSupport.verifyAll();
  }


  /**
   * Convenience method for asserting on the values of NameValuePair instances
   *
   * @param expectedName the expected name
   * @param expectedValue the expected value
   * @param nameValuePair the NameValuePair instance to test
   */
  static void assertNameValuePair(String expectedName, String expectedValue, NameValuePair nameValuePair) {
    assertEquals("Unexpected name found in this pair",
      expectedName, nameValuePair.getName());
    assertEquals("Unexpected value found in this pair",
      expectedValue, nameValuePair.getValue());
  }

}
