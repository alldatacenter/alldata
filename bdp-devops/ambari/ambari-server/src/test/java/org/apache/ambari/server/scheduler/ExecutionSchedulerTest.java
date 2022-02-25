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
package org.apache.ambari.server.scheduler;

import static org.easymock.EasyMock.expect;
import static org.mockito.Mockito.spy;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.expectPrivate;

import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class ExecutionSchedulerTest {

  private Configuration configuration;

  @Before
  public void setup() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_THREADS.getKey(), "2");
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_CLUSTERED.getKey(), "false");
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_CONNECTIONS.getKey(), "2");
    properties.setProperty(Configuration.SERVER_JDBC_DRIVER.getKey(), "db.driver");
    properties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), "jdbc:postgresql://localhost/");
    properties.setProperty(Configuration.SERVER_JDBC_USER_NAME.getKey(), "user");
    properties.setProperty(Configuration.SERVER_DB_NAME.getKey(), "derby");

    configuration = new Configuration(properties);

  }

  @After
  public void teardown() throws Exception {
  }

  @Test
  @PrepareForTest({ ExecutionSchedulerImpl.class })
  public void testSchedulerInitialize() throws Exception {

    ExecutionSchedulerImpl executionScheduler =
      spy(new ExecutionSchedulerImpl(configuration));

    Properties actualProperties = executionScheduler
      .getQuartzSchedulerProperties();

    Assert.assertEquals("2", actualProperties.getProperty("org.quartz.threadPool.threadCount"));
    Assert.assertEquals("2", actualProperties.getProperty("org.quartz.dataSource.myDS.maxConnections"));
    Assert.assertEquals("false", actualProperties.getProperty("org.quartz.jobStore.isClustered"));
    Assert.assertEquals("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate",
      actualProperties.getProperty("org.quartz.jobStore.driverDelegateClass"));
    Assert.assertEquals("select 0",
      actualProperties.getProperty("org.quartz.dataSource.myDS.validationQuery"));
    Assert.assertEquals(ExecutionSchedulerImpl.DEFAULT_SCHEDULER_NAME,
      actualProperties.getProperty("org.quartz.scheduler.instanceName"));
    Assert.assertEquals("org.quartz.simpl.SimpleThreadPool",
      actualProperties.getProperty("org.quartz.threadPool.class"));
  }

  @Test
  @PrepareForTest({ ExecutionSchedulerImpl.class })
  public void testSchedulerStartStop() throws Exception {
    StdSchedulerFactory factory = createNiceMock(StdSchedulerFactory.class);
    Scheduler scheduler = createNiceMock(Scheduler.class);

    expect(factory.getScheduler()).andReturn(scheduler);
    expectPrivate(scheduler, "startDelayed", new Integer(180)).once();
    expectNew(StdSchedulerFactory.class).andReturn(factory);
    expectPrivate(scheduler, "shutdown").once();

    PowerMock.replay(factory, StdSchedulerFactory.class, scheduler);

    ExecutionSchedulerImpl executionScheduler = new ExecutionSchedulerImpl(configuration);

    executionScheduler.startScheduler(180);
    executionScheduler.stopScheduler();

    PowerMock.verify(factory, StdSchedulerFactory.class, scheduler);

    Assert.assertTrue(executionScheduler.isInitialized());
  }

  @Test
  public void testGetQuartzDbDelegateClassAndValidationQuery() throws Exception {
    Properties testProperties = new Properties();
    testProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(),
      "jdbc:postgresql://host:port/dbname");
    testProperties.setProperty(Configuration.SERVER_DB_NAME.getKey(), "ambari");
    Configuration configuration1 = new Configuration(testProperties);
    ExecutionSchedulerImpl executionScheduler =
      spy(new ExecutionSchedulerImpl(configuration1));

    String[] subProps = executionScheduler
      .getQuartzDbDelegateClassAndValidationQuery();

    Assert.assertEquals("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate", subProps[0]);
    Assert.assertEquals("select 0", subProps[1]);

    testProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(),
      "jdbc:mysql://host:port/dbname");
    configuration1 = new Configuration(testProperties);
    executionScheduler = spy(new ExecutionSchedulerImpl(configuration1));

    subProps = executionScheduler.getQuartzDbDelegateClassAndValidationQuery();

    Assert.assertEquals("org.quartz.impl.jdbcjobstore.StdJDBCDelegate", subProps[0]);
    Assert.assertEquals("select 0", subProps[1]);

    testProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(),
      "jdbc:oracle:thin://host:port/dbname");
    configuration1 = new Configuration(testProperties);
    executionScheduler = spy(new ExecutionSchedulerImpl(configuration1));

    subProps = executionScheduler.getQuartzDbDelegateClassAndValidationQuery();

    Assert.assertEquals("org.quartz.impl.jdbcjobstore.oracle.OracleDelegate", subProps[0]);
    Assert.assertEquals("select 0 from dual", subProps[1]);
  }

  @Test
  @PrepareForTest({ ExecutionSchedulerImpl.class })
  public void testSchedulerStartDelay() throws Exception {
    StdSchedulerFactory factory = createNiceMock(StdSchedulerFactory.class);
    Scheduler scheduler = createNiceMock(Scheduler.class);

    expect(factory.getScheduler()).andReturn(scheduler).anyTimes();
    expectNew(StdSchedulerFactory.class).andReturn(factory);
    expect(scheduler.isStarted()).andReturn(false).anyTimes();
    expectPrivate(scheduler, "startDelayed", new Integer(180)).once();
    expectPrivate(scheduler, "start").once();

    PowerMock.replay(factory, StdSchedulerFactory.class, scheduler);

    ExecutionSchedulerImpl executionScheduler = new ExecutionSchedulerImpl(configuration);

    executionScheduler.startScheduler(180);
    executionScheduler.startScheduler(null);

    PowerMock.verify(factory, StdSchedulerFactory.class, scheduler);

    Assert.assertTrue(executionScheduler.isInitialized());
  }
}
