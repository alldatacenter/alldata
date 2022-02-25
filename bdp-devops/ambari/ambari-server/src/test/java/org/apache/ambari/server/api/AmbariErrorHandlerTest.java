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

package org.apache.ambari.server.api;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.security.authentication.jwt.JwtAuthenticationPropertiesProvider;
import org.easymock.EasyMockSupport;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class AmbariErrorHandlerTest extends EasyMockSupport {
  Gson gson = new Gson();


  @Test
  public void testHandle() throws Exception {

  }

  @Test
  public void testErrorWithJetty() throws Exception {
    Server server = new Server(0);
    JwtAuthenticationPropertiesProvider propertiesProvider = createNiceMock(JwtAuthenticationPropertiesProvider.class);
    expect(propertiesProvider.get()).andReturn(null).anyTimes();

    replayAll();

    ServletContextHandler root = new ServletContextHandler(server, "/",
      ServletContextHandler.SECURITY | ServletContextHandler.SESSIONS);

    root.addServlet(HelloServlet.class, "/hello");
    root.addServlet(DefaultServlet.class, "/");
    root.setErrorHandler(new AmbariErrorHandler(gson, propertiesProvider));

    server.start();

    int localPort = ((ServerConnector)server.getConnectors()[0]).getLocalPort();

    Client client = new Client();
    WebResource resource = client.resource("http://localhost:" + localPort + "/");


    ClientResponse successResponse = resource.path("hello").get(ClientResponse.class);
    assertEquals(HttpServletResponse.SC_OK, successResponse.getStatus());

    ClientResponse failResponse = resource.path("fail").get(ClientResponse.class);

    assertEquals(HttpServletResponse.SC_NOT_FOUND, failResponse.getStatus());

    try {
      String response = failResponse.getEntity(String.class);
      System.out.println(response);
      Map map;
      map = gson.fromJson(response, Map.class);
      System.out.println(map);
      assertNotNull("Incorrect response status", map.get("status"));
      assertNotNull("Incorrect response message", map.get("message"));
    } catch (JsonSyntaxException e1) {
      fail("Incorrect response");
    }

     server.stop();

    verifyAll();
  }


  @SuppressWarnings("serial")
  public static class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().println("hello");
    }

  }
}
