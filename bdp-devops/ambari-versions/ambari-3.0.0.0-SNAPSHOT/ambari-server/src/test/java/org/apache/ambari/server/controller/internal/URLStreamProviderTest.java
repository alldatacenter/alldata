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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * URLStreamProvider tests.
 */
public class URLStreamProviderTest {

  @Test
  public void testProcessURL() throws Exception {
    HttpURLConnection connection = createNiceMock(HttpURLConnection.class);
    AppCookieManager appCookieManager = createNiceMock(AppCookieManager.class);

    URLStreamProvider urlStreamProvider = createMockBuilder(URLStreamProvider.class).
        withConstructor(Integer.TYPE, Integer.TYPE, String.class, String.class, String.class).
        withArgs(1000, 1000, "path", "password", "type").
        addMockedMethod("getAppCookieManager").
        addMockedMethod("getConnection", String.class).
        createMock();

    expect(urlStreamProvider.getAppCookieManager()).andReturn(appCookieManager).anyTimes();
    expect(urlStreamProvider.getConnection("spec")).andReturn(connection);

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("Header1", Collections.singletonList("value"));
    headerMap.put("Cookie", Collections.singletonList("FOO=bar"));

    expect(appCookieManager.getCachedAppCookie("spec")).andReturn("APPCOOKIE=abcdef");

    connection.setConnectTimeout(1000);
    connection.setReadTimeout(1000);
    connection.setRequestMethod("GET");

    connection.setRequestProperty("Header1", "value");
    connection.setRequestProperty("Cookie", "FOO=bar; APPCOOKIE=abcdef");

    replay(urlStreamProvider, connection, appCookieManager);

    Assert.assertEquals(connection, urlStreamProvider.processURL("spec", "GET", (String) null, headerMap));

    verify(urlStreamProvider, connection, appCookieManager);
  }

  @Test
  public void testProcessURL_securityNotSetup() throws Exception {

    URLStreamProvider urlStreamProvider = createMockBuilder(URLStreamProvider.class).
        withConstructor(Integer.TYPE, Integer.TYPE, String.class, String.class, String.class).
        withArgs(1000, 1000, null, null, null).
        addMockedMethod("getAppCookieManager").
        addMockedMethod("getConnection", String.class).
        createMock();

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("Header1", Collections.singletonList("value"));
    headerMap.put("Cookie", Collections.singletonList("FOO=bar"));

    replay(urlStreamProvider);

    try {
      urlStreamProvider.processURL("https://spec", "GET", (String) null, headerMap);
      Assert.fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // expected
    }

    verify(urlStreamProvider);
  }

  @Test
  public void testAppendCookie() throws Exception {
    Assert.assertEquals("newCookie", URLStreamProvider.appendCookie(null, "newCookie"));
    Assert.assertEquals("newCookie", URLStreamProvider.appendCookie("", "newCookie"));
    Assert.assertEquals("oldCookie; newCookie", URLStreamProvider.appendCookie("oldCookie", "newCookie"));
    Assert.assertEquals("oldCookie1; oldCookie2; newCookie", URLStreamProvider.appendCookie("oldCookie1; oldCookie2", "newCookie"));
  }
}
