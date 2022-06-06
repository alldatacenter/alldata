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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.view.ImpersonatorSetting;
import org.apache.ambari.view.ViewContext;
import org.junit.Assert;
import org.junit.Test;


public class HttpImpersonatorImplTest {

  @Test
  public void testRequestURL() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    String responseBody = "Response body...";
    InputStream inputStream = new ByteArrayInputStream(responseBody.getBytes(Charset.forName("UTF-8")));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), eq((String) null), eq((Map<String, List<String>>)null))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);
    expect(viewContext.getUsername()).andReturn("joe").anyTimes();

    replay(streamProvider, urlConnection, viewContext);

    HttpImpersonatorImpl impersonator = new HttpImpersonatorImpl(viewContext, streamProvider);
    ImpersonatorSetting setting = new ImpersonatorSettingImpl(viewContext);

    Assert.assertEquals(responseBody, impersonator.requestURL("spec", "requestMethod", setting));

    verify(streamProvider, urlConnection, viewContext);
  }

  @Test
  public void testRequestURLWithCustom() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    String responseBody = "Response body...";
    InputStream inputStream = new ByteArrayInputStream(responseBody.getBytes(Charset.forName("UTF-8")));

    expect(streamProvider.processURL(eq("spec?impersonate=hive"), eq("requestMethod"), eq((String) null), eq((Map<String, List<String>>)null))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection);

    HttpImpersonatorImpl impersonator = new HttpImpersonatorImpl(viewContext, streamProvider);
    ImpersonatorSetting setting = new ImpersonatorSettingImpl("hive", "impersonate");

    Assert.assertEquals(responseBody, impersonator.requestURL("spec", "requestMethod", setting));

    verify(streamProvider, urlConnection);
  }
}
