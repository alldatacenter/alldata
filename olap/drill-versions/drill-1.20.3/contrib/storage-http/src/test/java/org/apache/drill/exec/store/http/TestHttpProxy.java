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
package org.apache.drill.exec.store.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.http.util.HttpProxyConfig;
import org.apache.drill.exec.store.http.util.HttpProxyConfig.ProxyType;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.ConfigBuilder;
import org.junit.Ignore;
import org.junit.Test;

import com.typesafe.config.Config;

public class TestHttpProxy extends BaseTest {

  @Test
  public void testBasics() {
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .type("socks")
        .host(" foo.com ")
        .port(1234)
        .username(" bob ")
        .password(" secret ")
        .build();
    assertEquals(ProxyType.SOCKS, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);
  }

  @Test
  public void testURL() {
    // See https://www.shellhacks.com/linux-proxy-server-settings-set-proxy-command-line/
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .url("http://bob:secret@foo.com:1234")
        .build();
    assertEquals(ProxyType.HTTP, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);
  }

  @Test
  public void testURLAndConfig() {
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .url("http://foo.com:1234")
        .username("bob")
        .password("secret")
        .build();
    assertEquals(ProxyType.HTTP, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);
  }

  @Test
  public void testNone() {
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .type("")
        .host("foo.com")
        .port(1234)
        .username("bob")
        .password("secret")
        .build();
    assertEquals(ProxyType.NONE, proxy.type);
    assertNull(proxy.host);
    assertNull(proxy.username);
    assertNull(proxy.password);
  }

  @Test
  public void testBlankType() {
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .type("  ")
        .host("foo.com")
        .port(1234)
        .username("bob")
        .password("secret")
        .build();
    assertEquals(ProxyType.NONE, proxy.type);
    assertNull(proxy.host);
    assertNull(proxy.username);
    assertNull(proxy.password);
  }

  @Test
  public void testBadType() {
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .type("bogus")
        .host("foo.com")
        .port(1234)
        .username("bob")
        .password("secret")
        .build();
    assertEquals(ProxyType.NONE, proxy.type);
    assertNull(proxy.host);
    assertNull(proxy.username);
    assertNull(proxy.password);
  }

  @Test
  public void testHttpConfig() {
    Config config = new ConfigBuilder()
        .put(ExecConstants.HTTP_PROXY_URL, "http://bob:secret@foo.com:1234")
        .build();
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .fromHttpConfig(config)
        .build();
    assertEquals(ProxyType.HTTP, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);
  }

  @Test
  public void testHttpUrlConfig() {
    Config config = new ConfigBuilder()
        .put(ExecConstants.HTTP_PROXY_URL, "")
        .put(ExecConstants.HTTP_PROXY_TYPE, "socks")
        .put(ExecConstants.HTTP_PROXY_HOST, "foo.com")
        .put(ExecConstants.HTTP_PROXY_PORT, 1234)
        .put(ExecConstants.HTTP_PROXY_USER_NAME, "bob")
        .put(ExecConstants.HTTP_PROXY_PASSWORD, "secret")
        .build();
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .fromHttpConfig(config)
        .build();
    assertEquals(ProxyType.SOCKS, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);
  }

  @Test
  public void testHttpsUrlConfig() {
    Config config = new ConfigBuilder()
        .put(ExecConstants.HTTPS_PROXY_URL, "https://bob:secret@foo.com:1234")
        .build();
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .fromHttpsConfig(config)
        .build();
    assertEquals(ProxyType.HTTP, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);
  }

  @Test
  public void testHttpsConfig() {
    Config config = new ConfigBuilder()
        .put(ExecConstants.HTTPS_PROXY_URL, "")
        .put(ExecConstants.HTTPS_PROXY_TYPE, "socks")
        .put(ExecConstants.HTTPS_PROXY_HOST, "foo.com")
        .put(ExecConstants.HTTPS_PROXY_PORT, 1234)
        .put(ExecConstants.HTTPS_PROXY_USER_NAME, "bob")
        .put(ExecConstants.HTTPS_PROXY_PASSWORD, "secret")
        .build();
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .fromHttpsConfig(config)
        .build();
    assertEquals(ProxyType.SOCKS, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);
  }

  @Test
  public void testConfigForUrl() {
    Config config = new ConfigBuilder()
        .put(ExecConstants.HTTP_PROXY_URL, "http://bob:secret@foo.com:1234")
        .put(ExecConstants.HTTPS_PROXY_URL, "http://alice:s3cr3t@bar.com:2345")
        .build();
    doTestConfigForUrl(config);
  }

  private void doTestConfigForUrl(Config config) {
    HttpProxyConfig proxy = HttpProxyConfig.builder()
        .fromConfigForURL(config, "http://google.com")
        .build();
    assertEquals(ProxyType.HTTP, proxy.type);
    assertEquals("foo.com", proxy.host);
    assertEquals(1234, proxy.port);
    assertEquals("bob", proxy.username);
    assertEquals("secret", proxy.password);

    proxy = HttpProxyConfig.builder()
        .fromConfigForURL(config, "https://google.com")
        .build();
    assertEquals(ProxyType.HTTP, proxy.type);
    assertEquals("bar.com", proxy.host);
    assertEquals(2345, proxy.port);
    assertEquals("alice", proxy.username);
    assertEquals("s3cr3t", proxy.password);
  }

  // To run this test, set two env vars in your run/debug
  // configuration, then comment out the @Ignore:
  // http_proxy=http://bob:secret@foo.com:1234
  // https_proxy=http://alice:s3cr3t@bar.com:2345
  @Test
  @Ignore("Requires manual setup")
  public void testEnvVar() {
    Config config = new ConfigBuilder()
        .build();
    doTestConfigForUrl(config);
  }
}
