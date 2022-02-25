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
package org.apache.ambari.server.orm;

import java.util.Properties;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.state.Clusters;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcPropertyTest {
  Properties properties;
  private Injector injector;

  @Before
  public void configure() {
    properties = new Properties();
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), "src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), "src/test/resources/version");
    properties.setProperty(Configuration.OS_VERSION.getKey(), "centos5");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), "src/test/resources/");
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testNormal() throws Exception {
    injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties), new LdapModule());
    injector.getInstance(GuiceJpaInitializer.class);

    injector.getInstance(Clusters.class);
  }

  @Test
  public void testJdbcProperty() throws Exception {
    properties.setProperty(Configuration.SERVER_JDBC_PROPERTIES_PREFIX + "shutdown", "true");
    injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties), new LdapModule());
    injector.getInstance(GuiceJpaInitializer.class);
    try {
      injector.getInstance(Clusters.class);
      Assert.fail("Expected in-memory to fail because property 'shutdown' specified.");
    } catch (Throwable t) {
      // expect failure
    }
  }


}
