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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.junit.Assert;
import org.junit.Test;

public class ViewURLStreamProviderTest {

  @Test
  public void testReadFrom() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadFromNullBody() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq((byte[]) null), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", (String) null, headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAs() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAs("spec", "requestMethod", "params", headers, "joe"));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAsCurrent() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);
    expect(viewContext.getUsername()).andReturn("joe").anyTimes();

    replay(streamProvider, urlConnection, inputStream, viewContext);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAsCurrent("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection, inputStream, viewContext);
  }

  @Test
  public void testReadFromInputStream() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    InputStream body = new ByteArrayInputStream("params".getBytes());

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", body, headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadFromNullInputStreamBody() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq((byte[]) null), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", (InputStream) null, headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAsInputStream() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    InputStream body = new ByteArrayInputStream("params".getBytes());

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAs("spec", "requestMethod", body, headers, "joe"));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAsCurrentInputStream() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    InputStream body = new ByteArrayInputStream("params".getBytes());

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);
    expect(viewContext.getUsername()).andReturn("joe").anyTimes();

    replay(streamProvider, urlConnection, inputStream, viewContext);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAsCurrent("spec", "requestMethod", body, headers));

    verify(streamProvider, urlConnection, inputStream, viewContext);
  }

  @Test
  public void testGetConnection() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"),
                                     eq("requestMethod"),
                                     aryEq("params".getBytes()),
                                     eq(headerMap))).andReturn(urlConnection);

    replay(streamProvider, urlConnection);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(urlConnection, viewURLStreamProvider.getConnection("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection);
  }

  @Test
  public void testGetConnectionAs() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);

    replay(streamProvider, urlConnection);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(urlConnection, viewURLStreamProvider.getConnectionAs("spec",
                                                                             "requestMethod",
                                                                             "params",
                                                                             headers,
                                                                             "joe"));

    verify(streamProvider, urlConnection);
  }

  @Test
  public void testGetConnectionCurrent() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(viewContext.getUsername()).andReturn("joe").anyTimes();

    replay(streamProvider, urlConnection, viewContext);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(urlConnection, viewURLStreamProvider.getConnectionAsCurrent("spec",
                                                                                    "requestMethod",
                                                                                    "params",
                                                                                    headers));

    verify(streamProvider, urlConnection, viewContext);
  }

  @Test
  public void testProxyRestriction() throws Exception {
    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(null, null);
    Properties ambariProperties = new Properties();
    Configuration configuration = new Configuration(ambariProperties);
    Assert.assertEquals(
        Configuration.PROXY_ALLOWED_HOST_PORTS.getDefaultValue(),
        configuration.getProxyHostAndPorts());
    ViewURLStreamProvider.HostPortRestrictionHandler hprh =
        viewURLStreamProvider.new HostPortRestrictionHandler(
            configuration.getProxyHostAndPorts());
    Assert.assertFalse(hprh.proxyCallRestricted());
    Assert.assertTrue(hprh.allowProxy("host1.com", null));
    Assert.assertTrue(hprh.allowProxy(null, null));
    Assert.assertTrue(hprh.allowProxy("host1.com", " "));
    Assert.assertTrue(hprh.allowProxy("host1.com ", " "));
    Assert.assertTrue(hprh.allowProxy(" host1.com ", "8080"));

    ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.PROXY_ALLOWED_HOST_PORTS.getKey(), "");
    configuration = new Configuration(ambariProperties);
    hprh =
        viewURLStreamProvider.new HostPortRestrictionHandler(
            configuration.getProxyHostAndPorts());
    Assert.assertFalse(hprh.proxyCallRestricted());
    Assert.assertTrue(hprh.allowProxy("host1.com", null));
    Assert.assertTrue(hprh.allowProxy(null, null));
    Assert.assertTrue(hprh.allowProxy("host1.com", " "));
    Assert.assertTrue(hprh.allowProxy("host1.com ", " "));
    Assert.assertTrue(hprh.allowProxy(" host1.com ", "8080"));

    ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.PROXY_ALLOWED_HOST_PORTS.getKey(), "host1.com:*");
    configuration = new Configuration(ambariProperties);
    hprh =
        viewURLStreamProvider.new HostPortRestrictionHandler(
            configuration.getProxyHostAndPorts());
    Assert.assertTrue(hprh.proxyCallRestricted());
    Assert.assertTrue(hprh.allowProxy("host1.com", null));
    Assert.assertTrue(hprh.allowProxy(null, null));
    Assert.assertTrue(hprh.allowProxy("host1.com", "20"));
    Assert.assertFalse(hprh.allowProxy("host2.com ", " "));
    Assert.assertFalse(hprh.allowProxy(" host2.com ", "8080"));

    ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.PROXY_ALLOWED_HOST_PORTS.getKey(), " host1.com:80 ,host2.org:443, host2.org:22");
    configuration = new Configuration(ambariProperties);
    hprh =
        viewURLStreamProvider.new HostPortRestrictionHandler(
            configuration.getProxyHostAndPorts());
    Assert.assertTrue(hprh.proxyCallRestricted());
    Assert.assertTrue(hprh.allowProxy("host1.com", "80"));
    Assert.assertFalse(hprh.allowProxy("host1.com", "20"));
    Assert.assertFalse(hprh.allowProxy("host2.org", "404"));
    Assert.assertFalse(hprh.allowProxy("host2.com", "22"));

    ViewContext viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getAmbariProperty(anyObject(String.class))).andReturn(
        " host1.com:80 ,host2.org:443, host2.org:22");
    replay(viewContext);
    viewURLStreamProvider = new ViewURLStreamProvider(viewContext, null);


    Assert.assertTrue(viewURLStreamProvider.isProxyCallAllowed("http://host1.com/tt"));
    Assert.assertTrue(viewURLStreamProvider.isProxyCallAllowed("https://host2.org/tt"));
    Assert.assertFalse(viewURLStreamProvider.isProxyCallAllowed("https://host2.org:444/tt"));

    viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getAmbariProperty(anyObject(String.class))).andReturn("c6401.ambari.apache.org:8088");
    replay(viewContext);
    viewURLStreamProvider = new ViewURLStreamProvider(viewContext, null);
    Assert.assertTrue(viewURLStreamProvider.isProxyCallAllowed(
        "http://c6401.ambari.apache.org:8088/ws/v1/cluster/get-node-labels"));

    viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getAmbariProperty(anyObject(String.class))).andReturn("*:8088");
    replay(viewContext);
    viewURLStreamProvider = new ViewURLStreamProvider(viewContext, null);
    Assert.assertFalse(viewURLStreamProvider.isProxyCallAllowed(
        "http://c6401.ambari.apache.org:8088/ws/v1/cluster/get-node-labels"));

    viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getAmbariProperty(anyObject(String.class))).andReturn("c6401.ambari.apache.org:*");
    replay(viewContext);
    viewURLStreamProvider = new ViewURLStreamProvider(viewContext, null);
    Assert.assertTrue(viewURLStreamProvider.isProxyCallAllowed(
        "http://c6401.ambari.apache.org:8088/ws/v1/cluster/get-node-labels"));

    viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getAmbariProperty(anyObject(String.class))).andReturn(
        "c6401.ambari.apache.org:80,c6401.ambari.apache.org:443");
    replay(viewContext);
    viewURLStreamProvider = new ViewURLStreamProvider(viewContext, null);
    Assert.assertTrue(viewURLStreamProvider.isProxyCallAllowed(
        "http://c6401.ambari.apache.org/ws/v1/cluster/get-node-labels"));
    Assert.assertTrue(viewURLStreamProvider.isProxyCallAllowed(
        "https://c6401.ambari.apache.org/ws/v1/cluster/get-node-labels"));
  }
}
