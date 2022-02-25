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

package org.apache.ambari.server.controller;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.session.SessionHandler;
import org.junit.Test;

public class AmbariSessionManagerTest {

  @Test
  public void testGetCurrentSessionId() throws Exception {

    HttpSession session = createNiceMock(HttpSession.class);
    AmbariSessionManager sessionManager =
        createMockBuilder(AmbariSessionManager.class).addMockedMethod("getHttpSession").createMock();

    expect(sessionManager.getHttpSession()).andReturn(session);
    expect(sessionManager.getHttpSession()).andReturn(null);
    expect(session.getId()).andReturn("SESSION_ID").anyTimes();

    replay(session, sessionManager);

    assertEquals("SESSION_ID", sessionManager.getCurrentSessionId());
    assertNull(sessionManager.getCurrentSessionId());

    verify(session, sessionManager);
  }

  @Test
  public void testGetSessionCookie() throws Exception {
    SessionHandler sessionHandler = createNiceMock(SessionHandler.class);
    SessionCookieConfig sessionCookieConfig = createNiceMock(SessionCookieConfig.class);

    AmbariSessionManager ambariSessionManager = new AmbariSessionManager();

    ambariSessionManager.sessionHandler = sessionHandler;

    expect(sessionCookieConfig.getName()).andReturn("SESSION_COOKIE").anyTimes();
    expect(sessionHandler.getSessionCookieConfig()).andReturn(sessionCookieConfig).anyTimes();

    replay(sessionHandler, sessionCookieConfig);

    assertEquals("SESSION_COOKIE", ambariSessionManager.getSessionCookie());

    verify(sessionHandler, sessionCookieConfig);
  }

  @Test
  public void testSetAttribute() throws Exception {
    HttpSession session = createNiceMock(HttpSession.class);
    AmbariSessionManager sessionManager =
        createMockBuilder(AmbariSessionManager.class).addMockedMethod("getHttpSession").createMock();

    expect(sessionManager.getHttpSession()).andReturn(session);
    session.setAttribute("foo", "bar");

    replay(session, sessionManager);

    sessionManager.setAttribute("foo", "bar");

    verify(session, sessionManager);
  }

  @Test
  public void testGetAttribute() throws Exception {
    HttpSession session = createNiceMock(HttpSession.class);
    AmbariSessionManager sessionManager =
        createMockBuilder(AmbariSessionManager.class).addMockedMethod("getHttpSession").createMock();

    expect(sessionManager.getHttpSession()).andReturn(session);
    expect(session.getAttribute("foo")).andReturn("bar");

    replay(session, sessionManager);

    assertEquals("bar", sessionManager.getAttribute("foo"));

    verify(session, sessionManager);
  }

  @Test
  public void testRemoveAttribute() throws Exception {
    HttpSession session = createNiceMock(HttpSession.class);
    AmbariSessionManager sessionManager =
        createMockBuilder(AmbariSessionManager.class).addMockedMethod("getHttpSession").createMock();

    expect(sessionManager.getHttpSession()).andReturn(session);
    session.removeAttribute("foo");

    replay(session, sessionManager);

    sessionManager.removeAttribute("foo");

    verify(session, sessionManager);
  }
}
