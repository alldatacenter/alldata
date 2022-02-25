/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.cleanup;


import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.jpa.AmbariJpaPersistModule;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

import junit.framework.Assert;

/**
 * Functional test for the Cleanup process.
 */
@Ignore("Ignored in order not to run with the unit tests as it's time consuming. Should be part of a functional test suit.")
public class CleanupServiceFunctionalTest {

  private static Injector injector;

  @BeforeClass
  public static void beforeClass() throws Exception {

    injector = Guice.createInjector(
        new CleanupModule(),
        new ControllerModule(getTestProperties())
    );

    // start the persistService
    injector.getInstance(AmbariJpaPersistService.class).start();

  }

  private static Module getTestPersistModule() {
    AmbariJpaPersistModule persistModule = new AmbariJpaPersistModule("ambari-server");
    Configuration testConfiguration = new Configuration(getTestProperties());
    Properties persistenceProperties = ControllerModule.getPersistenceProperties(testConfiguration);

    // overriding JPA properties with test specific values
    persistenceProperties.setProperty(PersistenceUnitProperties.SCHEMA_GENERATION_DATABASE_ACTION, PersistenceUnitProperties.NONE);

    // this doesn't work for something similar to what's described here: http://stackoverflow.com/questions/3606825/problems-with-generating-sql-via-eclipselink-missing-separator
    // line breaks are not supported; SQL statements need to be in one line! - appears to be fixed in eclipselink 2.6
    //persistenceProperties.setProperty(PersistenceUnitProperties.SCHEMA_GENERATION_CREATE_SCRIPT_SOURCE, "Ambari-DDL-Postgres-CREATE.sql");

    // Let the initialization be performed by the jPA provider!
    //persistenceProperties.setProperty(PersistenceUnitProperties.SCHEMA_GENERATION_SQL_LOAD_SCRIPT_SOURCE, getTestDataDDL());

    //persistenceProperties.setProperty(PersistenceUnitProperties.SCHEMA_GENERATION_DROP_SOURCE, PersistenceUnitProperties.SCHEMA_GENERATION_METADATA_SOURCE);
    persistenceProperties.setProperty(PersistenceUnitProperties.THROW_EXCEPTIONS, "true");

    // todo remove these when switching to derby!
    persistenceProperties.setProperty(PersistenceUnitProperties.JDBC_USER, "ambari");
    persistenceProperties.setProperty(PersistenceUnitProperties.JDBC_PASSWORD, "bigdata");

    return persistModule.properties(persistenceProperties);
  }

  private static String getTestDataDDL() {
    return "ddl-func-test/ddl-cleanup-test-data.sql";
  }

  @AfterClass
  public static void afterClass() {
    injector.getInstance(AmbariJpaPersistService.class).stop();
  }

  /**
   * Override JPA config values read from the ambari.properties
   *
   * @return
   */
  private static Properties getTestProperties() {
    Properties properties = new Properties();
    properties.put("server.jdbc.connection-pool", "internal");
    properties.put("server.persistence.type", "remote");

    properties.put("server.jdbc.driver", "org.postgresql.Driver");
    properties.put("server.jdbc.user.name", "ambari");
    //properties.put("server.jdbc.user.passwd", "bigdata");
    properties.put("server.jdbc.url", "jdbc:postgresql://192.168.59.103:5432/ambari");
    properties.put(Configuration.SHARED_RESOURCES_DIR.getKey(), "/Users/lpuskas/prj/ambari/ambari-server/src/test/resources");

    return properties;
  }

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testIOCContext() throws Exception {

    // WHEN
    CleanupServiceImpl cleanupService = injector.getInstance(CleanupServiceImpl.class);

    // THEN
    Assert.assertNotNull("The cleanupService instance should be present in the IoC context", cleanupService);
    //Assert.assertFalse("The cleanup registry shouldn't be empty", cleanupService.showCleanupRegistry().isEmpty());
  }

  @Test
  public void testRunCleanup() throws Exception {
    // GIVEN
    CleanupService<TimeBasedCleanupPolicy> cleanupService = injector.getInstance(CleanupServiceImpl.class);
    TimeBasedCleanupPolicy cleanupPolicy = new TimeBasedCleanupPolicy("cluster-1", 1455891250758L);

    // WHEN
    cleanupService.cleanup(cleanupPolicy);

    // THEN
    // todo assert eg.:on the amount of deleted rows

  }

  @Test
  public void testServicesShouldBeInSingletonScope() throws Exception {
    // GIVEN
    // the cleanup guice context is build

    // WHEN
    CleanupService cleanupService1 = injector.getInstance(CleanupServiceImpl.class);
    CleanupService cleanupService2 = injector.getInstance(CleanupServiceImpl.class);

    // THEN
    Assert.assertEquals("The ChainedCleanupService is not in Singleton scope!", cleanupService1, cleanupService2);
    //Assert.assertEquals("Registered services are not is not in Singleton scope!", cleanupService1.showCleanupRegistry(), cleanupService2.showCleanupRegistry());

  }

}