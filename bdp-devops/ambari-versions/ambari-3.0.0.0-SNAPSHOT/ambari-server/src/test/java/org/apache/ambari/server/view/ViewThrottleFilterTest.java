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
package org.apache.ambari.server.view;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests the {@link ViewThrottleFilter}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ViewThrottleFilter.class, Semaphore.class })
public class ViewThrottleFilterTest extends EasyMockSupport {

  private Injector m_injector;
  private Semaphore m_mockSemaphore = createStrictMock(Semaphore.class);

  /**
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new MockModule());
    PowerMockito.whenNew(Semaphore.class).withAnyArguments().thenReturn(m_mockSemaphore);
  }

  /**
   * Tests that acquiring the {@link Semaphore} ensures that the
   * {@link FilterChain} is invoked and that the {@link Semaphore} is eventually
   * released.
   *
   * @throws Exception
   */
  @Test
  public void testAcquireInvokesFilterChain() throws Exception {
    Configuration configuration = m_injector.getInstance(Configuration.class);
    EasyMock.expect(configuration.getViewRequestThreadPoolMaxSize()).andReturn(1).atLeastOnce();
    EasyMock.expect(configuration.getViewRequestThreadPoolTimeout()).andReturn(2000).atLeastOnce();
    EasyMock.expect(configuration.getClientThreadPoolSize()).andReturn(25).atLeastOnce();

    // servlet mocks
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    HttpServletResponse response = createStrictMock(HttpServletResponse.class);
    FilterChain filterChain = createStrictMock(FilterChain.class);

    // semaphore
    EasyMock.expect(m_mockSemaphore.tryAcquire(2000, TimeUnit.MILLISECONDS)).andReturn(true);

    // filter chain
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall().once();

    // semaphore release
    m_mockSemaphore.release();
    EasyMock.expectLastCall().once();

    replayAll();

    ViewThrottleFilter filter = new ViewThrottleFilter();
    m_injector.injectMembers(filter);
    filter.init(null);
    filter.doFilter(request, response, filterChain);

    verifyAll();
  }

  /**
   * Tests that the failure to acquire the {@link Semaphore} stops the request
   * and returns a {@link HttpServletResponse#SC_SERVICE_UNAVAILABLE}.
   *
   * @throws Exception
   */
  @Test
  public void testSemaphorePreventsFilterChain() throws Exception {
    Configuration configuration = m_injector.getInstance(Configuration.class);
    EasyMock.expect(configuration.getViewRequestThreadPoolMaxSize()).andReturn(1).atLeastOnce();
    EasyMock.expect(configuration.getViewRequestThreadPoolTimeout()).andReturn(2000).atLeastOnce();
    EasyMock.expect(configuration.getClientThreadPoolSize()).andReturn(25).atLeastOnce();

    // servlet mocks
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    HttpServletResponse response = createStrictMock(HttpServletResponse.class);
    FilterChain filterChain = createStrictMock(FilterChain.class);

    // semaphore
    EasyMock.expect(m_mockSemaphore.tryAcquire(2000, TimeUnit.MILLISECONDS)).andReturn(false);

    // response
    response.sendError(EasyMock.eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE), EasyMock.anyString());
    EasyMock.expectLastCall().once();

    replayAll();

    ViewThrottleFilter filter = new ViewThrottleFilter();
    m_injector.injectMembers(filter);
    filter.init(null);
    filter.doFilter(request, response, filterChain);

    verifyAll();
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      binder.bind(Configuration.class).toInstance(createNiceMock(Configuration.class));
      binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
    }
  }
}
