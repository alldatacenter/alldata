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

package org.apache.ambari.server.api.services;

import java.io.IOException;
import java.util.Map;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.RandomPortJerseyTest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.utils.StageUtils;
import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.test.framework.WebAppDescriptor;

import junit.framework.Assert;

public class PersistServiceTest extends RandomPortJerseyTest {
  static String PACKAGE_NAME = "org.apache.ambari.server.api.services";
  private static final Logger LOG = LoggerFactory.getLogger(PersistServiceTest.class);
  Injector injector;
  protected Client client;

  public  PersistServiceTest() {
    super(new WebAppDescriptor.Builder(PACKAGE_NAME).servletClass(ServletContainer.class)
        .initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
        .build());
  }

  public class MockModule extends AbstractModule {


    @Override
    protected void configure() {
      requestStaticInjection(PersistKeyValueService.class);
    }
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new InMemoryDefaultTestModule(), new MockModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testPersist() throws UniformInterfaceException, JSONException,
    IOException {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(clientConfig);
    WebResource webResource = client.resource(String.format("http://localhost:%d/persist", getTestPort()));
    
    webResource.post("{\"xyx\" : \"t\"}");
    LOG.info("Done posting to the server");
    String output = webResource.get(String.class);
    LOG.info("All key values " + output);
    Map<String, String> jsonOutput = StageUtils.fromJson(output, Map.class);
    String value = jsonOutput.get("xyx");
    Assert.assertEquals("t", value);
    webResource = client.resource(String.format("http://localhost:%d/persist/xyx", getTestPort()));
    output = webResource.get(String.class);
    Assert.assertEquals("t", output);
    LOG.info("Value for xyx " + output);
  }
}
