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
package org.apache.ambari.server.api.services;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactory;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sun.jersey.core.util.MultivaluedMapImpl;

public class LoggingServiceTest {

  @Before
  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testGetSearchEngineWhenLogSearchNotRunningAsAdministrator() throws Exception {
    testGetSearchEngineWhenLogSearchNotRunning(TestAuthenticationFactory.createAdministrator(), true);
  }
  
  @Test
  public void testGetSearchEngineWhenLogSearchNotRunningAsClusterAdministrator() throws Exception {
    testGetSearchEngineWhenLogSearchNotRunning(TestAuthenticationFactory.createClusterAdministrator(), true);
  }
  
  @Test
  public void testGetSearchEngineWhenLogSearchNotRunningAsClusterOperator() throws Exception {
    testGetSearchEngineWhenLogSearchNotRunning(TestAuthenticationFactory.createClusterOperator(), true);
  }
  
  @Test
  public void testGetSearchEngineWhenLogSearchNotRunningAsServiceAdministrator() throws Exception {
    testGetSearchEngineWhenLogSearchNotRunning(TestAuthenticationFactory.createServiceAdministrator(), true);
  }
  
  @Test
  public void testGetSearchEngineWhenLogSearchNotRunningAsServiceOperator() throws Exception {
    testGetSearchEngineWhenLogSearchNotRunning(TestAuthenticationFactory.createServiceOperator(), false);
  }

  @Test
  public void testGetSearchEngineWhenLogSearchNotRunningAsClusterUser() throws Exception {
    testGetSearchEngineWhenLogSearchNotRunning(TestAuthenticationFactory.createClusterUser(), false);
  }


  private void testGetSearchEngineWhenLogSearchNotRunning(Authentication authentication, boolean shouldBeAuthorized) throws Exception {
    final String expectedClusterName = "clusterone";
    final String expectedErrorMessage =
      "LogSearch is not currently available.  If LogSearch is deployed in this cluster, please verify that the LogSearch services are running.";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    LoggingService.ControllerFactory controllerFactoryMock =
      mockSupport.createMock(LoggingService.ControllerFactory.class);

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperFactory helperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    UriInfo uriInfoMock =
      mockSupport.createMock(UriInfo.class);

    if(shouldBeAuthorized) {
      expect(uriInfoMock.getQueryParameters()).andReturn(new MultivaluedMapImpl()).atLeastOnce();

      // return null from this factory, to simulate the case where LogSearch is
      // not running, or is not deployed in the current cluster
      expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(null).atLeastOnce();
    }

    expect(controllerFactoryMock.getController()).andReturn(controllerMock).atLeastOnce();
    expect(controllerMock.getClusters()).andReturn(clustersMock).once();
    expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).once();
    expect(clusterMock.getResourceId()).andReturn(4L).once();

    mockSupport.replayAll();

    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    LoggingService loggingService =
      new LoggingService(expectedClusterName, controllerFactoryMock);
    loggingService.setLoggingRequestHelperFactory(helperFactoryMock);

    Response resource = loggingService.getSearchEngine("", null, uriInfoMock);

    assertNotNull("The response returned by the LoggingService should not have been null",
                  resource);

    if(shouldBeAuthorized) {
      assertEquals("An OK status should have been returned",
          HttpURLConnection.HTTP_NOT_FOUND, resource.getStatus());
      assertNotNull("A non-null Entity should have been returned",
          resource.getEntity());
      assertEquals("Expected error message was not included in the response",
          expectedErrorMessage, resource.getEntity());
    }
    else {
      assertEquals("A FORBIDDEN status should have been returned",
          HttpURLConnection.HTTP_FORBIDDEN, resource.getStatus());
    }

    mockSupport.verifyAll();
  }

}
