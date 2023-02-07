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
package org.apache.drill.exec.server.rest.ssl;

import java.util.Arrays;

import org.apache.drill.categories.OptionsTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(OptionsTest.class)
public class SslContextFactoryConfiguratorTest extends ClusterTest {

  private static SslContextFactoryConfigurator sslContextFactoryConfigurator;

  @BeforeClass
  public static void setUpClass() throws Exception {
    ClusterFixtureBuilder fixtureBuilder = ClusterFixture.builder(dirTestWatcher)
        // imitate proper ssl config for embedded web
        .configProperty(ExecConstants.SSL_PROTOCOL, "TLSv1.2")
        .configProperty(ExecConstants.HTTP_ENABLE_SSL, true)
        .configProperty(ExecConstants.HTTP_TRUSTSTORE_PATH, "/tmp/ssl/cacerts.jks")
        .configProperty(ExecConstants.HTTP_TRUSTSTORE_PASSWORD, "passphrase")
        .configProperty(ExecConstants.HTTP_KEYSTORE_PATH, "/tmp/ssl/keystore.jks")
        .configProperty(ExecConstants.HTTP_KEYSTORE_PASSWORD, "passphrase")
        .configProperty(ExecConstants.SSL_KEY_PASSWORD, "passphrase")
        .configProperty(ExecConstants.SSL_USE_HADOOP_CONF, false)

        // few specific opts for Jetty sslContextFactory
        .configProperty(ExecConstants.HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_SSL_SESSION_TIMEOUT, 30)
        .configProperty(ExecConstants.HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_WANT_CLIENT_AUTH, true);
    fixtureBuilder.configBuilder().put(ExecConstants.HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_EXCLUDE_PROTOCOLS,
        Arrays.asList("TLSv1.0", "TLSv1.1"));
    startCluster(fixtureBuilder);
    sslContextFactoryConfigurator = new SslContextFactoryConfigurator(cluster.config(), cluster.drillbit().getContext().getEndpoint().getAddress());
  }

  @Test
  public void configureNewSslContextFactory() throws Exception {
    SslContextFactory sslContextFactory = sslContextFactoryConfigurator.configureNewSslContextFactory();

    assertEquals(30, sslContextFactory.getSslSessionTimeout());
    assertTrue(sslContextFactory.getWantClientAuth());
    assertArrayEquals(new String[]{"TLSv1.0", "TLSv1.1"}, sslContextFactory.getExcludeProtocols());
  }
}
