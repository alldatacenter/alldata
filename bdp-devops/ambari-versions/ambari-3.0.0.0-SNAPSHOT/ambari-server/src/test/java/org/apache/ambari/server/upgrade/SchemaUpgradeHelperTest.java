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
package org.apache.ambari.server.upgrade;

import java.lang.reflect.Method;
import java.sql.SQLException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

/**
 * Base Test Upgrade Catalog class
 */
abstract class TestUpgradeCatalog extends AbstractUpgradeCatalog{

  @Inject
  public TestUpgradeCatalog(Injector injector) {
    super(injector);
  }

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // no op
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    // no op
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    // no op
  }
}

/**
 * Sample Upgrade Catalog version 1.0
 */
class TestUpgradeCatalog10 extends TestUpgradeCatalog {

  @Inject
  public TestUpgradeCatalog10(Injector injector) {
    super(injector);
  }

  @Override
  public String getTargetVersion() {
    return "0.1.0";
  }
}

/**
 * Sample Upgrade Catalog version 2.0
 */
class TestUpgradeCatalog20 extends TestUpgradeCatalog {

  @Inject
  public TestUpgradeCatalog20(Injector injector) {
    super(injector);
  }

  @Override
  public String getTargetVersion() {
    return "0.2.0";
  }
}

/**
 * Sample Upgrade Catalog version 3.0
 */
class TestUpgradeCatalog30 extends TestUpgradeCatalog {

  @Inject
  public TestUpgradeCatalog30(Injector injector) {
    super(injector);
  }

  @Override
  public String getTargetVersion() {
    return "0.3.0";
  }
}


class UpgradeHelperTestModule extends InMemoryDefaultTestModule {

  UpgradeHelperTestModule() {
  }

  @Override
  protected void configure() {
    super.configure();

    Multibinder<UpgradeCatalog> catalogBinder =
      Multibinder.newSetBinder(binder(), UpgradeCatalog.class);
    catalogBinder.addBinding().to(TestUpgradeCatalog10.class);
    catalogBinder.addBinding().to(TestUpgradeCatalog20.class);
    catalogBinder.addBinding().to(TestUpgradeCatalog30.class);

    EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
  }
}

/**
 * Test class for {@link SchemaUpgradeHelper}
 */
public class SchemaUpgradeHelperTest {

  private SchemaUpgradeHelper schemaUpgradeHelper;

  @Before
  public void init(){
    Injector injector = Guice.createInjector(new UpgradeHelperTestModule());
    injector.getInstance(AmbariJpaPersistService.class).start();
    schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);
  }

  @Test
  public void testGetMinimalUpgradeCatalogVersion() throws Exception{
    Method getMinimalUpgradeCatalogVersion = schemaUpgradeHelper.getClass().getDeclaredMethod("getMinimalUpgradeCatalogVersion");
    getMinimalUpgradeCatalogVersion.setAccessible(true);
    String s = (String)getMinimalUpgradeCatalogVersion.invoke(schemaUpgradeHelper);

    Assert.assertEquals("0.1.0", s);
  }

  @Test
  public void testVerifyUpgradePath() throws Exception{
    Method verifyUpgradePath = schemaUpgradeHelper.getClass().getDeclaredMethod("verifyUpgradePath", String.class, String.class);
    verifyUpgradePath.setAccessible(true);

    boolean failToVerify = (boolean)verifyUpgradePath.invoke(schemaUpgradeHelper, "0.3.0", "0.2.0");
    boolean verifyPassed = (boolean)verifyUpgradePath.invoke(schemaUpgradeHelper, "0.1.0", "0.2.0");

    Assert.assertTrue(verifyPassed);
    Assert.assertFalse(failToVerify);
  }

}
