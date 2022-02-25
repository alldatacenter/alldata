/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.api.resources.HostComponentResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.LocalUriInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.RequestFactory;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.request.eventcreator.DefaultEventCreator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import junit.framework.Assert;

public class DefaultEventCreatorTest {

  private DefaultEventCreator defaultEventCreator;
  private RequestFactory requestFactory = new RequestFactory();

  @BeforeClass
  public static void beforeClass() {
    SecurityContextHolder.setContext(new SecurityContext() {
      @Override
      public Authentication getAuthentication() {
        return new Authentication() {
          @Override
          public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
          }

          @Override
          public Object getCredentials() {
            return null;
          }

          @Override
          public Object getDetails() {
            return null;
          }

          @Override
          public Object getPrincipal() {
            return new User("testuser", "password", Collections.emptyList());
          }

          @Override
          public boolean isAuthenticated() {
            return true;
          }

          @Override
          public void setAuthenticated(boolean b) throws IllegalArgumentException {

          }

          @Override
          public String getName() {
            return ((User) getPrincipal()).getUsername();
          }
        };
      }

      @Override
      public void setAuthentication(Authentication authentication) {

      }
    });

    setHttpRequest();
  }

  private static void setHttpRequest() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new HttpServletRequest() {
      @Override
      public String getAuthType() {
        return null;
      }

      @Override
      public javax.servlet.http.Cookie[] getCookies() {
        return new javax.servlet.http.Cookie[0];
      }

      @Override
      public long getDateHeader(String s) {
        return 0;
      }

      @Override
      public String getHeader(String s) {
        return null;
      }

      @Override
      public Enumeration<String> getHeaders(String s) {
        return null;
      }

      @Override
      public Enumeration<String> getHeaderNames() {
        return null;
      }

      @Override
      public int getIntHeader(String s) {
        return 0;
      }

      @Override
      public String getMethod() {
        return null;
      }

      @Override
      public String getPathInfo() {
        return null;
      }

      @Override
      public String getPathTranslated() {
        return null;
      }

      @Override
      public String getContextPath() {
        return null;
      }

      @Override
      public String getQueryString() {
        return null;
      }

      @Override
      public String getRemoteUser() {
        return null;
      }

      @Override
      public boolean isUserInRole(String s) {
        return false;
      }

      @Override
      public Principal getUserPrincipal() {
        return null;
      }

      @Override
      public String getRequestedSessionId() {
        return null;
      }

      @Override
      public String getRequestURI() {
        return null;
      }

      @Override
      public StringBuffer getRequestURL() {
        return null;
      }

      @Override
      public String getServletPath() {
        return null;
      }

      @Override
      public HttpSession getSession(boolean b) {
        return null;
      }

      @Override
      public HttpSession getSession() {
        return null;
      }

      public String changeSessionId() {
        return null;
      }

      @Override
      public boolean isRequestedSessionIdValid() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdFromCookie() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdFromURL() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdFromUrl() {
        return false;
      }

      @Override
      public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
      }

      @Override
      public void login(String s, String s1) throws ServletException {

      }

      @Override
      public void logout() throws ServletException {

      }

      @Override
      public Collection<Part> getParts() throws IOException, ServletException {
        return null;
      }

      @Override
      public Part getPart(String s) throws IOException, ServletException {
        return null;
      }

      public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
      }

      @Override
      public Object getAttribute(String s) {
        return null;
      }

      @Override
      public Enumeration<String> getAttributeNames() {
        return null;
      }

      @Override
      public String getCharacterEncoding() {
        return null;
      }

      @Override
      public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

      }

      @Override
      public int getContentLength() {
        return 0;
      }

      public long getContentLengthLong() {
        return 0;
      }

      @Override
      public String getContentType() {
        return null;
      }

      @Override
      public ServletInputStream getInputStream() throws IOException {
        return null;
      }

      @Override
      public String getParameter(String s) {
        return null;
      }

      @Override
      public Enumeration<String> getParameterNames() {
        return null;
      }

      @Override
      public String[] getParameterValues(String s) {
        return new String[0];
      }

      @Override
      public Map<String, String[]> getParameterMap() {
        return null;
      }

      @Override
      public String getProtocol() {
        return null;
      }

      @Override
      public String getScheme() {
        return null;
      }

      @Override
      public String getServerName() {
        return null;
      }

      @Override
      public int getServerPort() {
        return 0;
      }

      @Override
      public BufferedReader getReader() throws IOException {
        return null;
      }

      @Override
      public String getRemoteAddr() {
        return "1.2.3.4";
      }

      @Override
      public String getRemoteHost() {
        return null;
      }

      @Override
      public void setAttribute(String s, Object o) {

      }

      @Override
      public void removeAttribute(String s) {

      }

      @Override
      public Locale getLocale() {
        return null;
      }

      @Override
      public Enumeration<Locale> getLocales() {
        return null;
      }

      @Override
      public boolean isSecure() {
        return false;
      }

      @Override
      public RequestDispatcher getRequestDispatcher(String s) {
        return null;
      }

      @Override
      public String getRealPath(String s) {
        return null;
      }

      @Override
      public int getRemotePort() {
        return 0;
      }

      @Override
      public String getLocalName() {
        return null;
      }

      @Override
      public String getLocalAddr() {
        return null;
      }

      @Override
      public int getLocalPort() {
        return 0;
      }

      @Override
      public ServletContext getServletContext() {
        return null;
      }

      @Override
      public AsyncContext startAsync() throws IllegalStateException {
        return null;
      }

      @Override
      public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
      }

      @Override
      public boolean isAsyncStarted() {
        return false;
      }

      @Override
      public boolean isAsyncSupported() {
        return false;
      }

      @Override
      public AsyncContext getAsyncContext() {
        return null;
      }

      @Override
      public DispatcherType getDispatcherType() {
        return null;
      }
    }));
  }

  @Before
  public void before() {
    defaultEventCreator = new DefaultEventCreator();
  }

  @Test
  public void defaultEventCreatorTest__okWithMessage() {
    ResourceInstance resource = new QueryImpl(new HashMap<>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(null, new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK, "message"));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "User(testuser), RemoteIp(1.2.3.4), RequestType(POST), url(http://apache.org), ResultStatus(200 OK)";
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void defaultEventCreatorTest__errorWithMessage() {
    ResourceInstance resource = new QueryImpl(new HashMap<>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(null, new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, "message"));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "User(testuser), RemoteIp(1.2.3.4), RequestType(POST), url(http://apache.org), ResultStatus(400 Bad Request), Reason(message)";
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void defaultEventCreatorTest__okWithoutMessage() {
    ResourceInstance resource = new QueryImpl(new HashMap<>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(null, new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "User(testuser), RemoteIp(1.2.3.4), RequestType(POST), url(http://apache.org), ResultStatus(200 OK)";
    Assert.assertEquals(expected, actual);
  }

}
