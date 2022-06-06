/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.util.regex.Matcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.easymock.Capture;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)               // Allow mocking static methods
@PrepareForTest(AuthorizationHelper.class)    // This class has a static method that will be mocked
public class UserNameOverrideFilterTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private HttpServletRequest userRelatedRequest;

  @Mock(type = MockType.NICE)
  private ServletResponse response;

  @Mock(type = MockType.NICE)
  private FilterChain filterChain;

  private UserNameOverrideFilter filter = new UserNameOverrideFilter();


  @Test
  public void testGetUserNameMatcherNoUserNameInUri() throws Exception {
    // Given
    String uri = "/aaa/bbb";

    // When
    Matcher m = filter.getUserNameMatcher(uri);
    boolean isMatch = m.matches();

    // Then
    assertFalse(isMatch);
  }

  @Test
  public void testGetUserNameMatcherNoPostInUri() throws Exception {
    // Given
    String uri = "/aaa/users/user1@domain";

    // When
    Matcher m = filter.getUserNameMatcher(uri);
    boolean isMatch = m.find();

    String pre = isMatch ? m.group("pre") : null;
    String userName = isMatch ? m.group("username") : null;
    String post = isMatch ? m.group("post") : null;


    // Then
    assertTrue(isMatch);

    assertEquals("/aaa/users/", pre);
    assertEquals("user1@domain", userName);
    assertEquals("", post);
  }



  @Test
  public void testGetUserNameMatcherPostInUri() throws Exception {
    // Given
    String uri = "/aaa/users/user1@domain/privileges";

    // When
    Matcher m = filter.getUserNameMatcher(uri);
    boolean isMatch = m.find();

    String pre = isMatch ? m.group("pre") : null;
    String userName = isMatch ? m.group("username") : null;
    String post = isMatch ? m.group("post") : null;


    // Then
    assertTrue(isMatch);

    assertEquals("/aaa/users/", pre);
    assertEquals("user1@domain", userName);
    assertEquals("/privileges", post);
  }

  @Test
  public void testDoFilterNoUserNameInUri() throws Exception {
    // Given
    expect(userRelatedRequest.getRequestURI()).andReturn("/test/test1").anyTimes();
    filterChain.doFilter(same(userRelatedRequest), same(response));
    expectLastCall();

    replayAll();

    // When
    filter.doFilter(userRelatedRequest, response, filterChain);

    // Then

    verifyAll();
  }

  @Test
  public void testDoFilterWithUserNameInUri() throws Exception {
    // Given
    expect(userRelatedRequest.getRequestURI()).andReturn("/test/users/testUserName/test1").anyTimes();

    // filterChain should be invoked with the same req and resp as the OverrideUserName filter doesn't change these
    filterChain.doFilter(same(userRelatedRequest), same(response));
    expectLastCall();

    replayAll();

    // When
    filter.doFilter(userRelatedRequest, response, filterChain);

    // Then

    verifyAll();
  }

  @Test
  public void testDoFilterWithLoginAliasInUri() throws Exception {
    // Given
    expect(userRelatedRequest.getRequestURI()).andReturn(String.format("/test/users/%s/test1", URLEncoder.encode("testLoginAlias@testdomain.com", "UTF-8"))).anyTimes();

    Capture<ServletRequest> requestCapture = Capture.newInstance();
    filterChain.doFilter(capture(requestCapture), same(response));
    expectLastCall();

    PowerMock.mockStatic(AuthorizationHelper.class);
    expect(AuthorizationHelper.resolveLoginAliasToUserName(eq("testLoginAlias@testdomain.com"))).andReturn("testuser1");

    PowerMock.replay(AuthorizationHelper.class);
    replayAll();

    // When
    filter.doFilter(userRelatedRequest, response, filterChain);

    // Then
    HttpServletRequest updatedRequest = (HttpServletRequest)requestCapture.getValue();
    assertEquals("testLoginAlias@testdomain.com login alias in the request Uri should be resolved to testuser1 user name !", "/test/users/testuser1/test1", updatedRequest.getRequestURI());

    PowerMock.verify(AuthorizationHelper.class);
    verifyAll();
  }

  @After
  public void tearDown() throws Exception {
    resetAll();
  }
}
