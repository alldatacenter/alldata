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

package org.apache.ambari.server.security.authentication.kerberos;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.startsWith;

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationEventHandler;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationException;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationFilter;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

public class AmbariKerberosAuthenticationFilterTest extends EasyMockSupport {
  private Configuration configuration;

  private AuthenticationEntryPoint entryPoint;

  private AuthenticationManager authenticationManager;

  private AmbariAuthenticationEventHandler eventHandler;

  @Before
  public void setUp() {
    SecurityContextHolder.getContext().setAuthentication(null);

    entryPoint = createMock(AmbariEntryPoint.class);
    configuration = createMock(Configuration.class);
    authenticationManager = createMock(AuthenticationManager.class);
    eventHandler = createMock(AmbariAuthenticationEventHandler.class);
  }

  @Test (expected = IllegalArgumentException.class)
  public void ensureNonNullEventHandler() {
    new AmbariKerberosAuthenticationFilter(authenticationManager, entryPoint, configuration, null);
  }

  @Test
  public void shouldApplyTrue() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    expect(httpServletRequest.getHeader("Authorization")).andReturn("Negotiate .....").once();

    expect(configuration.getKerberosAuthenticationProperties()).andReturn(createProperties(true)).once();

    replayAll();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        eventHandler
    );

    Assert.assertTrue(filter.shouldApply(httpServletRequest));

    verifyAll();
  }

  @Test
  public void shouldApplyFalseMissingHeader() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    expect(httpServletRequest.getHeader("Authorization")).andReturn(null).once();

    expect(configuration.getKerberosAuthenticationProperties()).andReturn(createProperties(true)).once();

    replayAll();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        eventHandler
    );

    Assert.assertFalse(filter.shouldApply(httpServletRequest));

    verifyAll();
  }

  @Test
  public void shouldApplyNotFalseEnabled() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);

    expect(configuration.getKerberosAuthenticationProperties()).andReturn(createProperties(false)).once();

    replayAll();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        eventHandler
    );

    Assert.assertFalse(filter.shouldApply(httpServletRequest));

    verifyAll();
  }

  @Test
  public void testDoFilterSuccessful() throws IOException, ServletException {
    Capture<? extends AmbariAuthenticationFilter> captureFilter = newCapture(CaptureType.ALL);

    // GIVEN
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    HttpSession session = createMock(HttpSession.class);
    FilterChain filterChain = createMock(FilterChain.class);

    expect(request.getHeader("Authorization")).andReturn("Negotiate ").once();
    expect(request.getHeader(startsWith("X-Forwarded-"))).andReturn(null).times(6);
    expect(request.getRemoteAddr()).andReturn("1.2.3.4").once();
    expect(request.getSession(false)).andReturn(session).once();
    expect(request.getQueryString()).andReturn(null).once();
    expect(request.getParameter(anyString())).andReturn(null).anyTimes();
    expect(session.getId()).andReturn("sessionID").once();

    expect(authenticationManager.authenticate(anyObject(Authentication.class)))
        .andAnswer(new IAnswer<Authentication>() {
          @Override
          public Authentication answer() throws Throwable {
            return (Authentication) getCurrentArguments()[0];
          }
        })
        .anyTimes();

    expect(configuration.getKerberosAuthenticationProperties()).andReturn(createProperties(true)).once();

    eventHandler.beforeAttemptAuthentication(capture(captureFilter), eq(request), eq(response));
    expectLastCall().once();
    eventHandler.onSuccessfulAuthentication(capture(captureFilter), eq(request), eq(response), anyObject(Authentication.class));
    expectLastCall().once();

    filterChain.doFilter(request, response);
    expectLastCall().once();

    replayAll();
    // WHEN
    AmbariAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(authenticationManager, entryPoint, configuration, eventHandler);
    filter.doFilter(request, response, filterChain);
    // THEN
    verifyAll();

    List<? extends AmbariAuthenticationFilter> capturedFilters = captureFilter.getValues();
    for (AmbariAuthenticationFilter capturedFiltered : capturedFilters) {
      Assert.assertSame(filter, capturedFiltered);
    }
  }

  @Test
  public void testDoFilterUnsuccessful() throws IOException, ServletException {
    Capture<? extends AmbariAuthenticationFilter> captureFilter = newCapture(CaptureType.ALL);

    // GIVEN
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    HttpSession session = createMock(HttpSession.class);
    FilterChain filterChain = createMock(FilterChain.class);

    expect(request.getHeader("Authorization")).andReturn("Negotiate ").once();
    expect(request.getHeader(startsWith("X-Forwarded-"))).andReturn(null).times(6);
    expect(request.getRemoteAddr()).andReturn("1.2.3.4").once();
    expect(request.getSession(false)).andReturn(session).once();
    expect(request.getQueryString()).andReturn(null).once();
    expect(request.getParameter(anyString())).andReturn(null).anyTimes();
    expect(session.getId()).andReturn("sessionID").once();

    expect(authenticationManager.authenticate(anyObject(Authentication.class))).andThrow(new InvalidUsernamePasswordCombinationException("user")).once();

    expect(configuration.getKerberosAuthenticationProperties()).andReturn(createProperties(true)).once();

    eventHandler.beforeAttemptAuthentication(capture(captureFilter), eq(request), eq(response));
    expectLastCall().once();
    eventHandler.onUnsuccessfulAuthentication(capture(captureFilter), eq(request), eq(response), anyObject(AmbariAuthenticationException.class));
    expectLastCall().once();

    entryPoint.commence(eq(request), eq(response), anyObject(AmbariAuthenticationException.class));
    expectLastCall().once();

    replayAll();
    // WHEN
    AmbariAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(authenticationManager, entryPoint, configuration, eventHandler);
    filter.doFilter(request, response, filterChain);
    // THEN
    verifyAll();

    List<? extends AmbariAuthenticationFilter> capturedFilters = captureFilter.getValues();
    for (AmbariAuthenticationFilter capturedFiltered : capturedFilters) {
      Assert.assertSame(filter, capturedFiltered);
    }
  }

  private AmbariKerberosAuthenticationProperties createProperties(Boolean enabled) {
    AmbariKerberosAuthenticationProperties properties = createMock(AmbariKerberosAuthenticationProperties.class);
    expect(properties.isKerberosAuthenticationEnabled()).andReturn(enabled).once();
    return properties;
  }
}