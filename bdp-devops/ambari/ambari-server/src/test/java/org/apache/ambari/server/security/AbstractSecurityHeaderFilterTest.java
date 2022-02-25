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

import static org.easymock.EasyMock.expectLastCall;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public abstract class AbstractSecurityHeaderFilterTest extends EasyMockSupport {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final Class<? extends AbstractSecurityHeaderFilter> filterClass;
  private final Map<String, String> propertyNameMap;
  private final Map<String, String> defatulPropertyValueMap;

  protected AbstractSecurityHeaderFilterTest(Class<? extends AbstractSecurityHeaderFilter> filterClass, Map<String, String> propertyNameMap, Map<String, String> defatulPropertyValueMap) {
    this.filterClass = filterClass;
    this.propertyNameMap = propertyNameMap;
    this.defatulPropertyValueMap = defatulPropertyValueMap;
  }

  protected abstract void expectHttpServletRequestMock(HttpServletRequest request);

  @Before
  public void setUp() throws Exception {
    temporaryFolder.create();
  }

  @After
  public void tearDown() throws Exception {
    temporaryFolder.delete();
  }

  @Test
  public void testDoFilter_DefaultValuesNoSSL() throws Exception {
    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL.getKey(), "false");
        properties.setProperty(Configuration.HTTP_CHARSET.getKey(), Configuration.HTTP_CHARSET.getDefaultValue());
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER));
    expectLastCall().once();        
    servletResponse.setHeader(AbstractSecurityHeaderFilter.PRAGMA_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.PRAGMA_HEADER));
    expectLastCall().once();
    servletResponse.setCharacterEncoding(Configuration.HTTP_CHARSET.getDefaultValue());
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_DefaultValuesSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL.getKey(), "true");
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME.getKey(), httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(), httpPassFile.getName());
        properties.setProperty(Configuration.HTTP_CHARSET.getKey(), Configuration.HTTP_CHARSET.getDefaultValue());
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.PRAGMA_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.PRAGMA_HEADER));
    expectLastCall().once();
    servletResponse.setCharacterEncoding(Configuration.HTTP_CHARSET.getDefaultValue());
    expectLastCall().once();
    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_CustomValuesNoSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME.getKey(), httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(), httpPassFile.getName());
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "custom1");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "custom2");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "custom3");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER), "custom4");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER), "custom5");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.PRAGMA_HEADER), "custom6");
        properties.setProperty(Configuration.HTTP_CHARSET.getKey(), "custom7");
        properties.setProperty(Configuration.VIEWS_HTTP_CHARSET.getKey(), "custom7");
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, "custom2");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, "custom3");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER, "custom4");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER, "custom5");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.PRAGMA_HEADER, "custom6");
    expectLastCall().once();
    servletResponse.setCharacterEncoding("custom7");
    expectLastCall().once();
    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_CustomValuesSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL.getKey(), "true");
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME.getKey(), httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(), httpPassFile.getName());
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "custom1");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "custom2");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "custom3");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER), "custom4");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER), "custom5");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.PRAGMA_HEADER), "custom6");
        properties.setProperty(Configuration.HTTP_CHARSET.getKey(), "custom7");
        properties.setProperty(Configuration.VIEWS_HTTP_CHARSET.getKey(), "custom7");
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER, "custom1");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, "custom2");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, "custom3");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER, "custom4");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER, "custom5");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.PRAGMA_HEADER, "custom6");
    expectLastCall().once();
    servletResponse.setCharacterEncoding("custom7");
    expectLastCall().once();
    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_EmptyValuesNoSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME.getKey(), httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(), httpPassFile.getName());
        properties.setProperty(Configuration.HTTP_CHARSET.getKey(), "");
        properties.setProperty(Configuration.VIEWS_HTTP_CHARSET.getKey(), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.PRAGMA_HEADER), "");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_EmptyValuesSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL.getKey(), "true");
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME.getKey(), httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(), httpPassFile.getName());
        properties.setProperty(Configuration.HTTP_CHARSET.getKey(), "");
        properties.setProperty(Configuration.VIEWS_HTTP_CHARSET.getKey(), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.PRAGMA_HEADER), "");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

}
