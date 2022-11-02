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

package org.apache.ambari.server.security;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.core.AuthenticationException;

public class AmbariEntryPointTest extends EasyMockSupport {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testCommenceDefault() throws Exception {
    testCommence(null);
  }

  @Test
  public void testCommenceKerberosAuthenticationEnabled() throws Exception {
    testCommence(Boolean.TRUE);
  }

  @Test
  public void testCommenceKerberosAuthenticationNotEnabled() throws Exception {
    testCommence(Boolean.FALSE);
  }

  private void testCommence(Boolean kerberosAuthenticationEnabled) throws IOException, ServletException {
    HttpServletRequest request = createStrictMock(HttpServletRequest.class);
    HttpServletResponse response = createStrictMock(HttpServletResponse.class);
    AuthenticationException exception = createStrictMock(AuthenticationException.class);

    if (Boolean.TRUE == kerberosAuthenticationEnabled) {
      response.setHeader("WWW-Authenticate", "Negotiate");
      expectLastCall().once();
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication requested");
      expectLastCall().once();
    } else {
      expect(exception.getMessage()).andReturn("message").once();
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "message");
      expectLastCall().once();
    }

    replayAll();


    Properties properties = new Properties();
    if (kerberosAuthenticationEnabled != null) {
      properties.setProperty(Configuration.KERBEROS_AUTH_ENABLED.getKey(), kerberosAuthenticationEnabled.toString());
      properties.setProperty(Configuration.KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(), temporaryFolder.newFile().getAbsolutePath());
    }
    AmbariEntryPoint entryPoint = new AmbariEntryPoint(new Configuration(properties));
    entryPoint.commence(request, response, exception);

    verifyAll();

  }

}