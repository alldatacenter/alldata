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

package org.apache.ambari.server.controller.utilities;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/*Ignore this test because DatabaseChecker is not used anymore and it will be removed soon*/

public class DatabaseCheckerTest {
  private static Injector injector;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @BeforeClass
  public static void setupClass() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @Before
  public void setup() throws Exception {
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Ignore
  @Test
  public void testCheckDBVersion_Valid() throws Exception {
    MetainfoDAO metainfoDAO =  createMock(MetainfoDAO.class);
    MetainfoEntity metainfoEntity = new MetainfoEntity();
    String serverVersion = ambariMetaInfo.getServerVersion();
    metainfoEntity.setMetainfoName(Configuration.SERVER_VERSION_KEY);
    metainfoEntity.setMetainfoValue(serverVersion);
    expect(metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY)).
      andReturn(metainfoEntity);
    replay(metainfoDAO);
    DatabaseChecker.metainfoDAO = metainfoDAO;
    DatabaseChecker.ambariMetaInfo = ambariMetaInfo;
    try {
      DatabaseChecker.checkDBVersion();
    } catch (AmbariException ae) {
      fail("DB versions check failed.");
    }
  }

  @Ignore
  @Test(expected = AmbariException.class)
  public void testCheckDBVersionInvalid() throws Exception {
    MetainfoDAO metainfoDAO =  createMock(MetainfoDAO.class);
    MetainfoEntity metainfoEntity = new MetainfoEntity();
    metainfoEntity.setMetainfoName(Configuration.SERVER_VERSION_KEY);
    metainfoEntity.setMetainfoValue("0.0.0"); // Incompatible version
    expect(metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY)).
      andReturn(metainfoEntity);
    replay(metainfoDAO);
    DatabaseChecker.metainfoDAO = metainfoDAO;
    DatabaseChecker.ambariMetaInfo = ambariMetaInfo;

    DatabaseChecker.checkDBVersion();
  }
}
