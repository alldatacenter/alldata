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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.LogDefinition;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This test case verifies the basic behavior of the
 * LoggingSearchPropertyProvider.
 *
 * Specifically, it verifies that this PropertyProvider
 * implementation uses the output from LogSearch queries
 * to attach the correct logging-related output to the
 * HostComponent resources in Ambari.
 *
 */
public class LoggingSearchPropertyProviderTest {

  @Before
  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testBasicCallAsAdministrator() throws Exception {
    testBasicCall(TestAuthenticationFactory.createAdministrator(), true);
  }

  @Test
  public void testBasicCallAsClusterAdministrator() throws Exception {
    testBasicCall(TestAuthenticationFactory.createClusterAdministrator(), true);
  }

  @Test
  public void testBasicCallAsClusterOperator() throws Exception {
    testBasicCall(TestAuthenticationFactory.createClusterOperator(), true);
  }

  @Test
  public void testBasicCallAsServiceAdministrator() throws Exception {
    testBasicCall(TestAuthenticationFactory.createServiceAdministrator(), true);
  }

  @Test
  public void testBasicCallAsServiceOperator() throws Exception {
    testBasicCall(TestAuthenticationFactory.createServiceOperator(), false);
  }

  @Test
  public void testBasicCallAsClusterUser() throws Exception {
    testBasicCall(TestAuthenticationFactory.createClusterUser(), false);
  }

  /**
   * Verifies the following:
   *
   *   1. That this PropertyProvider implementation uses
   *      the expected interfaces to make queries to the LogSearch
   *      service.
   *   2. That the PropertyProvider queries the current HostComponent
   *      resource in order to obtain the correct information to send to
   *      LogSearch.
   *   3. That the output of the LogSearch query is properly set on the
   *      HostComponent resource in the expected structure.
   *
   * @param authentication the authenticated user
   * @param authorizedForLogSearch true of the user is expected to be authorized; otherwise false
   * @throws Exception
   */
  private void testBasicCall(Authentication authentication, boolean authorizedForLogSearch) throws Exception {
    final String expectedLogFilePath =
      "/var/log/hdfs/hdfs_namenode.log";

    final String expectedSearchEnginePath = "/api/v1/clusters/clusterone/logging/searchEngine";

    final String expectedAmbariURL = "http://c6401.ambari.apache.org:8080";

    final String expectedStackName = "HDP";
    final String expectedStackVersion = "2.4";
    final String expectedComponentName = "NAMENODE";
    final String expectedServiceName = "HDFS";
    final String expectedLogSearchComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport = new EasyMockSupport();

    Resource resourceMock =
      mockSupport.createMock(Resource.class);
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "component_name"))).andReturn(expectedComponentName).atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "host_name"))).andReturn("c6401.ambari.apache.org").atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "cluster_name"))).andReturn("clusterone").atLeastOnce();

    Capture<HostComponentLoggingInfo> captureLogInfo = Capture.newInstance();

    if(authorizedForLogSearch) {
      // expect set method to be called
      resourceMock.setProperty(eq("logging"), capture(captureLogInfo));
    }

    LogLevelQueryResponse levelQueryResponse =
      new LogLevelQueryResponse();

    levelQueryResponse.setTotalCount("3");
    // setup test data for log levels
    List<NameValuePair> testListOfLogLevels =
      new LinkedList<>();
    testListOfLogLevels.add(new NameValuePair("ERROR", "150"));
    testListOfLogLevels.add(new NameValuePair("WARN", "500"));
    testListOfLogLevels.add(new NameValuePair("INFO", "2200"));

    levelQueryResponse.setNameValueList(testListOfLogLevels);

    Request requestMock =
      mockSupport.createMock(Request.class);

    Predicate predicateMock =
      mockSupport.createMock(Predicate.class);

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperFactory loggingRequestHelperFactoryMock =
        mockSupport.createMock(LoggingRequestHelperFactory.class);

    LogSearchDataRetrievalService dataRetrievalServiceMock =
        mockSupport.createMock(LogSearchDataRetrievalService.class);

    if(authorizedForLogSearch) {
      LoggingRequestHelper loggingRequestHelperMock =
          mockSupport.createMock(LoggingRequestHelper.class);

      AmbariMetaInfo metaInfoMock =
          mockSupport.createMock(AmbariMetaInfo.class);

      StackId stackIdMock =
          mockSupport.createMock(StackId.class);

      ComponentInfo componentInfoMock =
          mockSupport.createMock(ComponentInfo.class);

      LogDefinition logDefinitionMock =
          mockSupport.createMock(LogDefinition.class);

      Service serviceMock = mockSupport.createNiceMock(Service.class);
      expect(controllerMock.findServiceName(clusterMock, expectedComponentName)).andReturn(expectedServiceName).atLeastOnce();
      expect(clusterMock.getService(expectedServiceName)).andReturn(serviceMock).anyTimes();
      expect(serviceMock.getDesiredStackId()).andReturn(stackIdMock).anyTimes();

      expect(controllerMock.getAmbariServerURI(expectedSearchEnginePath)).
          andReturn(expectedAmbariURL + expectedSearchEnginePath).atLeastOnce();
      expect(controllerMock.getAmbariMetaInfo()).andReturn(metaInfoMock).atLeastOnce();
      expect(metaInfoMock.getComponent(expectedStackName, expectedStackVersion, expectedServiceName, expectedComponentName)).andReturn(componentInfoMock).atLeastOnce();
      expect(stackIdMock.getStackName()).andReturn(expectedStackName).atLeastOnce();
      expect(stackIdMock.getStackVersion()).andReturn(expectedStackVersion).atLeastOnce();

      expect(componentInfoMock.getLogs()).andReturn(Collections.singletonList(logDefinitionMock)).atLeastOnce();
      expect(logDefinitionMock.getLogId()).andReturn(expectedLogSearchComponentName).atLeastOnce();

      expect(dataRetrievalServiceMock.getLogFileNames(expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(Collections.singleton(expectedLogFilePath)).atLeastOnce();
      expect(dataRetrievalServiceMock.getLogFileTailURI(expectedAmbariURL + expectedSearchEnginePath, expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn("").atLeastOnce();

      expect(loggingRequestHelperFactoryMock.getHelper(anyObject(AmbariManagementController.class), anyObject(String.class)))
          .andReturn(loggingRequestHelperMock).atLeastOnce();
    }

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster("clusterone")).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getResourceId()).andReturn(4L).atLeastOnce();

    mockSupport.replayAll();

    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    LoggingSearchPropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider();

    propertyProvider.setAmbariManagementController(controllerMock);
    propertyProvider.setLogSearchDataRetrievalService(dataRetrievalServiceMock);
    propertyProvider.setLoggingRequestHelperFactory(loggingRequestHelperFactoryMock);

    Set<Resource> returnedResources =
      propertyProvider.populateResources(Collections.singleton(resourceMock), requestMock, predicateMock);

    // verify that the property provider attached
    // the expected logging structure to the associated resource

    assertEquals("Returned resource set was of an incorrect size",
      1, returnedResources.size());

    if(authorizedForLogSearch) {
      HostComponentLoggingInfo returnedLogInfo =
          captureLogInfo.getValue();

      assertNotNull("Returned log info should not be null",
          returnedLogInfo);

      assertEquals("Returned component was not the correct name",
          "hdfs_namenode", returnedLogInfo.getComponentName());

      assertEquals("Returned list of log file names for this component was incorrect",
          1, returnedLogInfo.getListOfLogFileDefinitions().size());

      LogFileDefinitionInfo definitionInfo =
          returnedLogInfo.getListOfLogFileDefinitions().get(0);

      assertEquals("Incorrect log file type was found",
          LogFileType.SERVICE, definitionInfo.getLogFileType());
      assertEquals("Incorrect log file path found",
          expectedLogFilePath, definitionInfo.getLogFileName());
      assertEquals("Incorrect URL path to searchEngine",
          expectedAmbariURL + expectedSearchEnginePath, definitionInfo.getSearchEngineURL());

      // verify that the log level count information
      // was not added to the HostComponent resource
      assertNull(returnedLogInfo.getListOfLogLevels());
    }
    else {
      assertFalse("Unauthorized user should not be able to retrieve log info", captureLogInfo.hasCaptured());
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testBasicCallWithNullTailLogURIReturnedAsAdministrator() throws Exception {
    testBasicCallWithNullTailLogURIReturned(TestAuthenticationFactory.createAdministrator(), true);
  }

  @Test
  public void testBasicCallWithNullTailLogURIReturnedAsClusterAdministrator() throws Exception {
    testBasicCallWithNullTailLogURIReturned(TestAuthenticationFactory.createClusterAdministrator(), true);
  }

  @Test
  public void testBasicCallWithNullTailLogURIReturnedAsClusterOperator() throws Exception {
    testBasicCallWithNullTailLogURIReturned(TestAuthenticationFactory.createClusterOperator(), true);
  }

  @Test
  public void testBasicCallWithNullTailLogURIReturnedAsServiceAdministrator() throws Exception {
    testBasicCallWithNullTailLogURIReturned(TestAuthenticationFactory.createServiceAdministrator(), true);
  }

  @Test
  public void testBasicCallWithNullTailLogURIReturnedAsServiceOperator() throws Exception {
    testBasicCallWithNullTailLogURIReturned(TestAuthenticationFactory.createServiceOperator(), false);
  }

  @Test
  public void testBasicCallWithNullTailLogURIReturnedAsClusterUser() throws Exception {
    testBasicCallWithNullTailLogURIReturned(TestAuthenticationFactory.createClusterUser(), false);
  }

  /**
   * Verifies the following:
   *
   *   1. That this PropertyProvider implementation uses
   *      the expected interfaces to make queries to the LogSearch
   *      service.
   *   2. That the PropertyProvider queries the current HostComponent
   *      resource in order to obtain the correct information to send to
   *      LogSearch.
   *   3. That the output of the LogSearch query is properly set on the
   *      HostComponent resource in the expected structure.
   *   4. That the proper error-handling is in place in the event that a null
   *      tail log URI is returned by the retrieval service.
   *
   * @param authentication the authenticated user
   * @param authorizedForLogSearch true of the user is expected to be authorized; otherwise false
   * @throws Exception
   */
  private void testBasicCallWithNullTailLogURIReturned(Authentication authentication, boolean authorizedForLogSearch) throws Exception {
    final String expectedLogFilePath =
      "/var/log/hdfs/hdfs_namenode.log";

    final String expectedSearchEnginePath = "/api/v1/clusters/clusterone/logging/searchEngine";

    final String expectedAmbariURL = "http://c6401.ambari.apache.org:8080";

    final String expectedStackName = "HDP";
    final String expectedStackVersion = "2.4";
    final String expectedComponentName = "NAMENODE";
    final String expectedServiceName = "HDFS";
    final String expectedLogSearchComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport = new EasyMockSupport();

    Resource resourceMock =
      mockSupport.createMock(Resource.class);
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "component_name"))).andReturn(expectedComponentName).atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "host_name"))).andReturn("c6401.ambari.apache.org").atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "cluster_name"))).andReturn("clusterone").atLeastOnce();

    Capture<HostComponentLoggingInfo> captureLogInfo = Capture.newInstance();

    if(authorizedForLogSearch) {
      // expect set method to be called
      resourceMock.setProperty(eq("logging"), capture(captureLogInfo));
    }

    LogLevelQueryResponse levelQueryResponse =
      new LogLevelQueryResponse();

    levelQueryResponse.setTotalCount("3");
    // setup test data for log levels
    List<NameValuePair> testListOfLogLevels =
      new LinkedList<>();
    testListOfLogLevels.add(new NameValuePair("ERROR", "150"));
    testListOfLogLevels.add(new NameValuePair("WARN", "500"));
    testListOfLogLevels.add(new NameValuePair("INFO", "2200"));

    levelQueryResponse.setNameValueList(testListOfLogLevels);

    Request requestMock =
      mockSupport.createMock(Request.class);

    Predicate predicateMock =
      mockSupport.createMock(Predicate.class);

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LogDefinition logDefinitionMock =
      mockSupport.createMock(LogDefinition.class);

    LogSearchDataRetrievalService dataRetrievalServiceMock =
      mockSupport.createMock(LogSearchDataRetrievalService.class);

    LoggingRequestHelperFactory loggingRequestHelperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    if(authorizedForLogSearch) {
      AmbariMetaInfo metaInfoMock =
          mockSupport.createMock(AmbariMetaInfo.class);

      StackId stackIdMock =
          mockSupport.createMock(StackId.class);

      ComponentInfo componentInfoMock =
          mockSupport.createMock(ComponentInfo.class);

      LoggingRequestHelper loggingRequestHelperMock =
          mockSupport.createMock(LoggingRequestHelper.class);

      Service serviceMock = mockSupport.createNiceMock(Service.class);
      expect(controllerMock.findServiceName(clusterMock, expectedComponentName)).andReturn(expectedServiceName).atLeastOnce();
      expect(clusterMock.getService(expectedServiceName)).andReturn(serviceMock).anyTimes();
      expect(serviceMock.getDesiredStackId()).andReturn(stackIdMock).anyTimes();

      expect(dataRetrievalServiceMock.getLogFileNames(expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(Collections.singleton(expectedLogFilePath)).atLeastOnce();
      // return null, to simulate the case when the LogSearch service goes down, and the helper object
      // is not available to continue servicing the request.
      expect(dataRetrievalServiceMock.getLogFileTailURI(expectedAmbariURL + expectedSearchEnginePath, expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(null).atLeastOnce();

      expect(loggingRequestHelperFactoryMock.getHelper(anyObject(AmbariManagementController.class), anyObject(String.class)))
          .andReturn(loggingRequestHelperMock).atLeastOnce();

      expect(controllerMock.getAmbariServerURI(expectedSearchEnginePath)).
          andReturn(expectedAmbariURL + expectedSearchEnginePath).atLeastOnce();
      expect(controllerMock.getAmbariMetaInfo()).andReturn(metaInfoMock).atLeastOnce();

      expect(metaInfoMock.getComponent(expectedStackName, expectedStackVersion, expectedServiceName, expectedComponentName)).andReturn(componentInfoMock).atLeastOnce();

      expect(componentInfoMock.getLogs()).andReturn(Collections.singletonList(logDefinitionMock)).atLeastOnce();
      expect(logDefinitionMock.getLogId()).andReturn(expectedLogSearchComponentName).atLeastOnce();

      expect(stackIdMock.getStackName()).andReturn(expectedStackName).atLeastOnce();
      expect(stackIdMock.getStackVersion()).andReturn(expectedStackVersion).atLeastOnce();
    }

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster("clusterone")).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getResourceId()).andReturn(4L).atLeastOnce();

    mockSupport.replayAll();

    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    LoggingSearchPropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider();

    propertyProvider.setAmbariManagementController(controllerMock);
    propertyProvider.setLogSearchDataRetrievalService(dataRetrievalServiceMock);
    propertyProvider.setLoggingRequestHelperFactory(loggingRequestHelperFactoryMock);

    Set<Resource> returnedResources =
      propertyProvider.populateResources(Collections.singleton(resourceMock), requestMock, predicateMock);

    // verify that the property provider attached
    // the expected logging structure to the associated resource

    assertEquals("Returned resource set was of an incorrect size",
      1, returnedResources.size());

    if(authorizedForLogSearch) {
      HostComponentLoggingInfo returnedLogInfo =
          captureLogInfo.getValue();

      assertNotNull("Returned log info should not be null",
          returnedLogInfo);

      assertEquals("Returned component was not the correct name",
          "hdfs_namenode", returnedLogInfo.getComponentName());

      assertEquals("Returned list of log file names for this component was incorrect",
          0, returnedLogInfo.getListOfLogFileDefinitions().size());

      // verify that the log level count information
      // was not added to the HostComponent resource
      assertNull(returnedLogInfo.getListOfLogLevels());
    }
    else {
      assertFalse("Unauthorized user should not be able to retrieve log info", captureLogInfo.hasCaptured());
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testCheckWhenLogSearchNotAvailableAsAdministrator() throws Exception {
    testCheckWhenLogSearchNotAvailable(TestAuthenticationFactory.createAdministrator(), true);
  }

  @Test
  public void testCheckWhenLogSearchNotAvailableAsClusterAdministrator() throws Exception {
    testCheckWhenLogSearchNotAvailable(TestAuthenticationFactory.createClusterAdministrator(), true);
  }

  @Test
  public void testCheckWhenLogSearchNotAvailableAsClusterOperator() throws Exception {
    testCheckWhenLogSearchNotAvailable(TestAuthenticationFactory.createClusterOperator(), true);
  }

  @Test
  public void testCheckWhenLogSearchNotAvailableAsServiceAdministrator() throws Exception {
    testCheckWhenLogSearchNotAvailable(TestAuthenticationFactory.createServiceAdministrator(), true);
  }

  @Test
  public void testCheckWhenLogSearchNotAvailableAsServiceOperator() throws Exception {
    testCheckWhenLogSearchNotAvailable(TestAuthenticationFactory.createServiceOperator(), false);
  }

  @Test
  public void testCheckWhenLogSearchNotAvailableAsClusterUser() throws Exception {
    testCheckWhenLogSearchNotAvailable(TestAuthenticationFactory.createClusterUser(), false);
  }

  /**
   * Verifies that this property provider implementation will
   * properly handle the case of LogSearch not being deployed in
   * the cluster or available.
   *
   * @param authentication the authenticated user
   * @param authorizedForLogSearch true of the user is expected to be authorized; otherwise false
   * @throws Exception
   */
  private void testCheckWhenLogSearchNotAvailable(Authentication authentication, boolean authorizedForLogSearch) throws Exception {

    final String expectedStackName = "HDP";
    final String expectedStackVersion = "2.4";
    final String expectedComponentName = "NAMENODE";
    final String expectedServiceName = "HDFS";
    final String expectedLogSearchComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport = new EasyMockSupport();

    Resource resourceMock =
      mockSupport.createMock(Resource.class);
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "component_name"))).andReturn(expectedComponentName).atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "host_name"))).andReturn("c6401.ambari.apache.org").atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "cluster_name"))).andReturn("clusterone").atLeastOnce();

    Request requestMock =
      mockSupport.createMock(Request.class);

    Predicate predicateMock =
      mockSupport.createMock(Predicate.class);

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LogSearchDataRetrievalService dataRetrievalServiceMock =
      mockSupport.createMock(LogSearchDataRetrievalService.class);

    LoggingRequestHelperFactory loggingRequestHelperFactoryMock =
        mockSupport.createMock(LoggingRequestHelperFactory.class);

    if(authorizedForLogSearch) {
      AmbariMetaInfo metaInfoMock =
          mockSupport.createMock(AmbariMetaInfo.class);

      StackId stackIdMock =
          mockSupport.createMock(StackId.class);

      LogDefinition logDefinitionMock =
          mockSupport.createMock(LogDefinition.class);

      ComponentInfo componentInfoMock =
          mockSupport.createMock(ComponentInfo.class);

      LoggingRequestHelper loggingRequestHelperMock =
          mockSupport.createMock(LoggingRequestHelper.class);

      Service serviceMock = mockSupport.createNiceMock(Service.class);
      expect(controllerMock.findServiceName(clusterMock, expectedComponentName)).andReturn(expectedServiceName).atLeastOnce();
      expect(clusterMock.getService(expectedServiceName)).andReturn(serviceMock).anyTimes();
      expect(serviceMock.getDesiredStackId()).andReturn(stackIdMock).anyTimes();


      expect(controllerMock.getAmbariMetaInfo()).andReturn(metaInfoMock).atLeastOnce();
      expect(stackIdMock.getStackName()).andReturn(expectedStackName).atLeastOnce();
      expect(stackIdMock.getStackVersion()).andReturn(expectedStackVersion).atLeastOnce();

      expect(metaInfoMock.getComponent(expectedStackName, expectedStackVersion, expectedServiceName, expectedComponentName)).andReturn(componentInfoMock).atLeastOnce();

      // simulate the case when LogSearch is not deployed, or is not available for some reason
      expect(dataRetrievalServiceMock.getLogFileNames(expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(null).atLeastOnce();

      expect(componentInfoMock.getLogs()).andReturn(Collections.singletonList(logDefinitionMock)).atLeastOnce();
      expect(logDefinitionMock.getLogId()).andReturn(expectedLogSearchComponentName).atLeastOnce();

      expect(loggingRequestHelperFactoryMock.getHelper(anyObject(AmbariManagementController.class), anyObject(String.class)))
          .andReturn(loggingRequestHelperMock).atLeastOnce();
    }

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster("clusterone")).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getResourceId()).andReturn(4L).atLeastOnce();

    mockSupport.replayAll();

    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    LoggingSearchPropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider();

    propertyProvider.setAmbariManagementController(controllerMock);
    propertyProvider.setLogSearchDataRetrievalService(dataRetrievalServiceMock);
    propertyProvider.setLoggingRequestHelperFactory(loggingRequestHelperFactoryMock);


    // execute the populate resources method, verify that no exceptions occur, due to
    // the LogSearch helper not being available
    Set<Resource> returnedResources =
      propertyProvider.populateResources(Collections.singleton(resourceMock), requestMock, predicateMock);

    // verify that the set of resources has not changed in size
    assertEquals("Returned resource set was of an incorrect size",
      1, returnedResources.size());

    // verify that the single resource passed in was returned
    assertSame("Returned resource was not the expected instance.",
      resourceMock, returnedResources.iterator().next());

    mockSupport.verifyAll();
  }

}
