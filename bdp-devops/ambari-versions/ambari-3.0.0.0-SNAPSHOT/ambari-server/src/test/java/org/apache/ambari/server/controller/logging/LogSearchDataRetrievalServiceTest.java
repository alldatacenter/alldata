/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.google.inject.Injector;

/**
 * This test verifies the basic behavior of the
 *   LogSearchDataRetrievalServiceTest, and should
 *   verify the interaction with its dependencies, as
 *   well as the interaction with the LogSearch
 *   server.
 *
 */
public class LogSearchDataRetrievalServiceTest {

  @Test
  public void testGetTailFileWhenHelperIsAvailable() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedResultURI = "http://localhost/test/result";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    LoggingRequestHelper helperMock =
      mockSupport.createMock(LoggingRequestHelper.class);

    Configuration configurationMock =
      mockSupport.createMock(Configuration.class);

    expect(helperFactoryMock.getHelper(null, expectedClusterName)).andReturn(helperMock);
    expect(helperMock.createLogFileTailURI("http://localhost", expectedComponentName, expectedHostName)).andReturn(expectedResultURI);
    expect(configurationMock.getLogSearchMetadataCacheExpireTimeout()).andReturn(1).atLeastOnce();

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setConfiguration(configurationMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();

    String resultTailFileURI =
      retrievalService.getLogFileTailURI("http://localhost", expectedComponentName, expectedHostName, expectedClusterName);

    assertEquals("TailFileURI was not returned as expected", expectedResultURI, resultTailFileURI);

    mockSupport.verifyAll();
  }

  @Test
  public void testGetTailFileWhenRequestHelperIsNull() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    Configuration configurationMock =
      mockSupport.createMock(Configuration.class);

    // return null, to simulate the case where LogSearch Server is
    // not available for some reason
    expect(helperFactoryMock.getHelper(null, expectedClusterName)).andReturn(null);

    expect(configurationMock.getLogSearchMetadataCacheExpireTimeout()).andReturn(1).atLeastOnce();

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setConfiguration(configurationMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();

    String resultTailFileURI =
      retrievalService.getLogFileTailURI("http://localhost", expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("TailFileURI should be null in this case", resultTailFileURI);

    mockSupport.verifyAll();
  }

  @Test
  public void testGetLogFileNamesDefault() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    Executor executorMock = mockSupport.createMock(Executor.class);

    Injector injectorMock =
      mockSupport.createMock(Injector.class);

    Configuration configurationMock =
      mockSupport.createMock(Configuration.class);

    // expect the executor to be called to execute the LogSearch request
    executorMock.execute(isA(LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable.class));
    // executor should only be called once
    expectLastCall().once();

    expect(injectorMock.getInstance(LoggingRequestHelperFactory.class)).andReturn(helperFactoryMock);

    expect(configurationMock.getLogSearchMetadataCacheExpireTimeout()).andReturn(1).atLeastOnce();

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setInjector(injectorMock);
    retrievalService.setConfiguration(configurationMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();
    retrievalService.setExecutor(executorMock);


    assertEquals("Default request set should be empty", 0, retrievalService.getCurrentRequests().size());

    Set<String> resultSet = retrievalService.getLogFileNames(expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);
    assertEquals("Incorrect number of entries in the current request set", 1, retrievalService.getCurrentRequests().size());

    assertTrue("Incorrect HostComponent set on request set",
                retrievalService.getCurrentRequests().contains(expectedComponentName + "+" + expectedHostName));
    assertEquals("Incorrect size for failure counts for components, should be 0",
                 0, retrievalService.getComponentRequestFailureCounts().size());

    mockSupport.verifyAll();
  }

  @Test
  public void testGetLogFileNamesExistingFailuresLessThanThreshold() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    Executor executorMock = mockSupport.createMock(Executor.class);

    Injector injectorMock =
      mockSupport.createMock(Injector.class);

    Configuration configurationMock =
      mockSupport.createMock(Configuration.class);

    // expect the executor to be called to execute the LogSearch request
    executorMock.execute(isA(LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable.class));
    // executor should only be called once
    expectLastCall().once();

    expect(injectorMock.getInstance(LoggingRequestHelperFactory.class)).andReturn(helperFactoryMock);

    expect(configurationMock.getLogSearchMetadataCacheExpireTimeout()).andReturn(1).atLeastOnce();

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setInjector(injectorMock);
    retrievalService.setConfiguration(configurationMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();
    retrievalService.setExecutor(executorMock);
    // initialize the comopnent-based failure count to have a count > 0, but less than threshold (10)
    retrievalService.getComponentRequestFailureCounts().put(expectedComponentName, new AtomicInteger(5));


    assertEquals("Default request set should be empty", 0, retrievalService.getCurrentRequests().size());

    Set<String> resultSet = retrievalService.getLogFileNames(expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);
    assertEquals("Incorrect number of entries in the current request set", 1, retrievalService.getCurrentRequests().size());

    assertTrue("Incorrect HostComponent set on request set",
      retrievalService.getCurrentRequests().contains(expectedComponentName + "+" + expectedHostName));
    assertEquals("Incorrect size for failure counts for components, should be 0",
      1, retrievalService.getComponentRequestFailureCounts().size());
    assertEquals("Incorrect failure count for component",
      5, retrievalService.getComponentRequestFailureCounts().get(expectedComponentName).get());

    mockSupport.verifyAll();
  }

  @Test
  public void testGetLogFileNamesExistingFailuresAtThreshold() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    Executor executorMock = mockSupport.createMock(Executor.class);

    Injector injectorMock =
      mockSupport.createMock(Injector.class);

    Configuration configurationMock =
      mockSupport.createMock(Configuration.class);

    expect(configurationMock.getLogSearchMetadataCacheExpireTimeout()).andReturn(1).atLeastOnce();

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setInjector(injectorMock);
    retrievalService.setConfiguration(configurationMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();
    retrievalService.setExecutor(executorMock);
    // initialize the comopnent-based failure count to have a count at the threshold
    retrievalService.getComponentRequestFailureCounts().put(expectedComponentName, new AtomicInteger(10));

    assertEquals("Default request set should be empty", 0, retrievalService.getCurrentRequests().size());

    Set<String> resultSet =
      retrievalService.getLogFileNames(expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);
    assertEquals("Incorrect number of entries in the current request set", 0, retrievalService.getCurrentRequests().size());

    assertEquals("Incorrect size for failure counts for components, should be 0",
      1, retrievalService.getComponentRequestFailureCounts().size());
    assertEquals("Incorrect failure count for component",
      10, retrievalService.getComponentRequestFailureCounts().get(expectedComponentName).get());

    mockSupport.verifyAll();
  }

  @Test
  public void testGetLogFileNamesExistingFailuresOverThreshold() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    Executor executorMock = mockSupport.createMock(Executor.class);

    Injector injectorMock =
      mockSupport.createMock(Injector.class);

    Configuration configurationMock =
      mockSupport.createMock(Configuration.class);

    expect(configurationMock.getLogSearchMetadataCacheExpireTimeout()).andReturn(1).atLeastOnce();

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setInjector(injectorMock);
    retrievalService.setConfiguration(configurationMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();
    retrievalService.setExecutor(executorMock);
    // initialize the comopnent-based failure count to have a count over the threshold
    retrievalService.getComponentRequestFailureCounts().put(expectedComponentName, new AtomicInteger(20));

    assertEquals("Default request set should be empty", 0, retrievalService.getCurrentRequests().size());

    Set<String> resultSet =
      retrievalService.getLogFileNames(expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);
    assertEquals("Incorrect number of entries in the current request set", 0, retrievalService.getCurrentRequests().size());

    assertEquals("Incorrect size for failure counts for components, should be 0",
      1, retrievalService.getComponentRequestFailureCounts().size());
    assertEquals("Incorrect failure count for component",
      20, retrievalService.getComponentRequestFailureCounts().get(expectedComponentName).get());

    mockSupport.verifyAll();
  }

  @Test
  public void testGetLogFileNamesIgnoreMultipleRequestsForSameHostComponent() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    Executor executorMock = mockSupport.createMock(Executor.class);

    Configuration configurationMock =
      mockSupport.createMock(Configuration.class);

    expect(configurationMock.getLogSearchMetadataCacheExpireTimeout()).andReturn(1).atLeastOnce();

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setConfiguration(configurationMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();
    // there should be no expectations set on this mock
    retrievalService.setExecutor(executorMock);

    // set the current requests to include this expected HostComponent
    // this simulates the case where a request is ongoing for this HostComponent,
    // but is not yet completed.
    retrievalService.getCurrentRequests().add(expectedComponentName + "+" + expectedHostName);

    Set<String> resultSet = retrievalService.getLogFileNames(expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);

    mockSupport.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithSuccessfulCall() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;

    final HostLogFilesResponse resp = new HostLogFilesResponse();
    Map<String, List<String>> componentMap = new HashMap<>();
    List<String> expectedList = new ArrayList<>();
    expectedList.add("/this/is/just/a/test/directory");
    componentMap.put(expectedComponentName, expectedList);
    resp.setHostLogFiles(componentMap);

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);
    LoggingRequestHelper helperMock = mockSupport.createMock(LoggingRequestHelper.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);
    Map<String, AtomicInteger> componentFailureCounts = mockSupport.createMock(Map.class);

    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(helperMock);
    expect(helperMock.sendGetLogFileNamesRequest(expectedHostName)).andReturn(resp);
    // expect that the results will be placed in the cache
    cacheMock.put(expectedComponentAndHostName, Collections.singleton("/this/is/just/a/test/directory"));
    // expect that the completed request is removed from the current request set
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();

    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, componentFailureCounts, controllerMock);
    loggingRunnable.run();

    mockSupport.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithFailedCallNullHelper() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);
    Map<String, AtomicInteger> componentFailureCounts = mockSupport.createMock(Map.class);

    // return null to simulate an error during helper instance creation
    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(null);
    // expect that the completed request is removed from the current request set,
    // even in the event of a failure to obtain the LogSearch data
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();

    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, componentFailureCounts, controllerMock);
    loggingRunnable.run();

    mockSupport.verifyAll();

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithFailedCallNullResult() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;
    final AtomicInteger testInteger = new AtomicInteger(0);

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);
    LoggingRequestHelper helperMock = mockSupport.createMock(LoggingRequestHelper.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);
    Map<String, AtomicInteger> componentFailureCounts = mockSupport.createMock(Map.class);

    Capture<AtomicInteger> captureFailureCount = EasyMock.newCapture();

    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(helperMock);
    // return null to simulate an error occurring during the LogSearch data request
    expect(helperMock.sendGetLogFileNamesRequest(expectedHostName)).andReturn(null);
    // expect that the completed request is removed from the current request set,
    // even in the event of a failure to obtain the LogSearch data
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();
    // expect that the component failure map is initially empty
    expect(componentFailureCounts.containsKey(expectedComponentName)).andReturn(false);
    // expect that the component map is updated with a new count
    expect(componentFailureCounts.put(eq(expectedComponentName), capture(captureFailureCount))).andReturn(new AtomicInteger(0));
    // expect that the runnable will obtain an increment the failure count
    expect(componentFailureCounts.get(expectedComponentName)).andReturn(testInteger);


    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, componentFailureCounts, controllerMock);
    loggingRunnable.run();

    assertEquals("Initial count set by Runnable should be 0",
                 0, captureFailureCount.getValue().get());
    assertEquals("Failure count should have been incremented",
                 1, testInteger.get());

    mockSupport.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithFailedCallNullResultExistingFailureCount() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;
    final AtomicInteger testFailureCount = new AtomicInteger(2);

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);
    LoggingRequestHelper helperMock = mockSupport.createMock(LoggingRequestHelper.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);
    Map<String, AtomicInteger> componentFailureCounts = mockSupport.createMock(Map.class);

    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(helperMock);
    // return null to simulate an error occurring during the LogSearch data request
    expect(helperMock.sendGetLogFileNamesRequest(expectedHostName)).andReturn(null);
    // expect that the completed request is removed from the current request set,
    // even in the event of a failure to obtain the LogSearch data
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();
    // expect that the component failure map is initially empty
    expect(componentFailureCounts.containsKey(expectedComponentName)).andReturn(true);
    // expect that the runnable will obtain an increment the existing failure count
    expect(componentFailureCounts.get(expectedComponentName)).andReturn(testFailureCount);

    mockSupport.replayAll();

    assertEquals("Initial count should be 2",
                 2, testFailureCount.get());

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
        cacheMock, currentRequestsMock, helperFactoryMock, componentFailureCounts, controllerMock);
    loggingRunnable.run();

    assertEquals("Failure count should have been incremented",
                 3, testFailureCount.get());

    mockSupport.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithFailedCallEmptyResult() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;
    final AtomicInteger testInteger = new AtomicInteger(0);

    final HostLogFilesResponse resp = new HostLogFilesResponse();
    resp.setHostLogFiles(new HashMap<>());
    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);
    LoggingRequestHelper helperMock = mockSupport.createMock(LoggingRequestHelper.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);
    Map<String, AtomicInteger> componentFailureCounts = mockSupport.createMock(Map.class);

    Capture<AtomicInteger> captureFailureCount = EasyMock.newCapture();

    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(helperMock);
    // return null to simulate an error occurring during the LogSearch data request
    expect(helperMock.sendGetLogFileNamesRequest(expectedHostName)).andReturn(resp);
    // expect that the completed request is removed from the current request set,
    // even in the event of a failure to obtain the LogSearch data
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();
    // expect that the component failure map is initially empty
    expect(componentFailureCounts.containsKey(expectedComponentName)).andReturn(false);
    // expect that the component map is updated with a new count
    expect(componentFailureCounts.put(eq(expectedComponentName), capture(captureFailureCount))).andReturn(new AtomicInteger(0));
    // expect that the runnable will obtain an increment the failure count
    expect(componentFailureCounts.get(expectedComponentName)).andReturn(testInteger);

    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, componentFailureCounts, controllerMock);
    loggingRunnable.run();

    assertEquals("Initial count set by Runnable should be 0",
      0, captureFailureCount.getValue().get());
    assertEquals("Failure count should have been incremented",
      1, testInteger.get());

    mockSupport.verifyAll();
  }

}
