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

package org.apache.ambari.server.serveraction.users;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Test cases for the persiter service implementation.
 * For fully testing the behavior it builds up  GUICE context.
 */
public class CsvFilePersisterServiceFunctionalTest {

  private static final String TEST_CSV = "/tmp/users.csv";

  private static Injector injector;
  private static CollectionPersisterServiceFactory serviceFactory;
  private CsvFilePersisterService csvFileCsvFilePersisterService;
  private Path testCsvPath;

  /**
   * Guice module for testing service / factory behavior
   */
  private static class TestPersistServiceModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new FactoryModuleBuilder().implement(CollectionPersisterService.class, CsvFilePersisterService.class).build(CollectionPersisterServiceFactory.class));
    }
  }

  @BeforeClass
  public static void beforeClass() {
    injector = Guice.createInjector(new TestPersistServiceModule());
    serviceFactory = (CollectionPersisterServiceFactory) injector.getInstance(CollectionPersisterServiceFactory.class);
  }

  @Before
  public void before() {
    csvFileCsvFilePersisterService = serviceFactory.createCsvFilePersisterService(TEST_CSV);
    testCsvPath = Paths.get(TEST_CSV);
  }

  @Test
  public void shouldCreateCsvFileWithExpectedPermissions() throws IOException {

    Assert.assertNotNull(csvFileCsvFilePersisterService);

    Assert.assertTrue("The generated file couldn't be found", Files.exists(testCsvPath));

    Assert.assertTrue("The generated files doesn't have all the expected permissions", Files.getPosixFilePermissions(testCsvPath).containsAll(csvFileCsvFilePersisterService.getCsvPermissions()));

    Assert.assertFalse("The generated file has more than the required permissions", Files.getPosixFilePermissions(testCsvPath).contains(PosixFilePermission.GROUP_EXECUTE));

  }

  @After
  public void after() throws IOException {
    Files.deleteIfExists(Paths.get(TEST_CSV));
  }

}
