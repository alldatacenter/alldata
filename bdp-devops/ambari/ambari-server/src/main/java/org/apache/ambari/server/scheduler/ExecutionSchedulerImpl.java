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

import java.util.List;
import java.util.Properties;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.state.scheduler.GuiceJobFactory;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class ExecutionSchedulerImpl implements ExecutionScheduler {
  @Inject
  private Configuration configuration;
  @Inject
  GuiceJobFactory guiceJobFactory;

  private static final Logger LOG = LoggerFactory.getLogger(ExecutionSchedulerImpl.class);
  protected static final String DEFAULT_SCHEDULER_NAME = "ExecutionScheduler";
  protected Scheduler scheduler;
  protected static volatile boolean isInitialized = false;

  @Inject
  public ExecutionSchedulerImpl(Injector injector) {
    injector.injectMembers(this);
  }

  /**
   * Constructor used for unit testing
   * @param configuration
   */
  protected ExecutionSchedulerImpl(Configuration configuration) {
    this.configuration = configuration;
  }

  protected synchronized void initializeScheduler() {
    StdSchedulerFactory sf = new StdSchedulerFactory();
    Properties properties = getQuartzSchedulerProperties();
    try {
      sf.initialize(properties);
    } catch (SchedulerException e) {
      LOG.warn("Failed to initialize Request Execution Scheduler properties !");
      LOG.debug("Scheduler properties: \n{}", properties);
      e.printStackTrace();
      return;
    }
    try {
      scheduler = sf.getScheduler();
      scheduler.setJobFactory(guiceJobFactory);
      isInitialized = true;
    } catch (SchedulerException e) {
      LOG.warn("Failed to create Request Execution scheduler !");
      e.printStackTrace();
    }
  }

  protected Properties getQuartzSchedulerProperties() {
    Properties properties = new Properties();
    properties.setProperty("org.quartz.scheduler.instanceName", DEFAULT_SCHEDULER_NAME);
    properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
    properties.setProperty("org.quartz.threadPool.class",
      "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount",
      configuration.getExecutionSchedulerThreads());

    // Job Store Configuration
    properties.setProperty("org.quartz.jobStore.class",
      "org.quartz.impl.jdbcjobstore.JobStoreTX");
    properties.setProperty("org.quartz.jobStore.isClustered",
      configuration.isExecutionSchedulerClusterd());

    String[] subProps = getQuartzDbDelegateClassAndValidationQuery();

    properties.setProperty("org.quartz.jobStore.driverDelegateClass",
      subProps[0]);
    // Allow only strings in the jobDataMap which is serialized
    properties.setProperty("org.quartz.jobStore.useProperties", "false");

    // Data store configuration
    properties.setProperty("org.quartz.jobStore.dataSource", "myDS");
    properties.setProperty("org.quartz.dataSource.myDS.driver",
      configuration.getDatabaseDriver());
    properties.setProperty("org.quartz.dataSource.myDS.URL",
      configuration.getDatabaseUrl());
    properties.setProperty("org.quartz.dataSource.myDS.user",
      configuration.getDatabaseUser());
    properties.setProperty("org.quartz.dataSource.myDS.password",
      configuration.getDatabasePassword());
    properties.setProperty("org.quartz.dataSource.myDS.maxConnections",
      configuration.getExecutionSchedulerConnections());
    properties.setProperty("org.quartz.dataSource.myDS.maxCachedStatementsPerConnection",
      configuration.getExecutionSchedulerMaxStatementsPerConnection());
    properties.setProperty("org.quartz.dataSource.myDS.validationQuery",
      subProps[1]);

    // Skip update check
    properties.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");

    LOG.debug("Using quartz properties: {}", properties);
    return properties;
  }

  protected synchronized boolean isInitialized() {
    return isInitialized;
  }


  protected String[] getQuartzDbDelegateClassAndValidationQuery() {
    String dbDelegate = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
    String dbValidate = "select 0";

    DatabaseType databaseType = configuration.getDatabaseType();
    if (databaseType == DatabaseType.POSTGRES) {
      dbDelegate = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
    } else if (databaseType == DatabaseType.ORACLE) {
      dbDelegate = "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate";
      dbValidate = "select 0 from dual";
    }

    return new String[] { dbDelegate, dbValidate };
  }

  @Override
  public synchronized void startScheduler(Integer delay) throws AmbariException {
    try {
      if (!isInitialized) {
        initializeScheduler();
        isInitialized = true;
      }
    } catch (Exception e) {
      String msg = "Unable to initialize Request Execution scheduler !";
      LOG.warn(msg);
      e.printStackTrace();
      throw new AmbariException(msg);
    }
    try {
      if (!scheduler.isStarted()) {
        // To avoid issue created due to change in server clock in between the
        // scheduler initialization and scheduler start,
        // start immediately if no delay provided.
        if (delay != null) {
          scheduler.startDelayed(delay);
        } else {
          scheduler.start();
        }

      } else {
        LOG.info("Scheduler " + scheduler.getSchedulerInstanceId() +
          " already started. Skipping start.");
      }
    } catch (SchedulerException e) {
      LOG.error("Failed to start scheduler", e);
      throw new AmbariException(e.getMessage());
    }
  }

  @Override
  public synchronized void stopScheduler() throws AmbariException {
    if (scheduler == null) {
      throw new AmbariException("Scheduler not instantiated !");
    }
    try {
      scheduler.shutdown();
    } catch (SchedulerException e) {
      LOG.error("Failed to stop scheduler", e);
      throw new AmbariException(e.getMessage());
    }
  }

  @Override
  public void scheduleJob(Trigger trigger) throws SchedulerException {
    scheduler.scheduleJob(trigger);
  }

  @Override
  public void addJob(JobDetail jobDetail) throws SchedulerException {
    scheduler.addJob(jobDetail, true);
  }

  @Override
  public void deleteJob(JobKey jobKey) throws SchedulerException {
    scheduler.deleteJob(jobKey);
  }

  @Override
  public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
    return scheduler.getJobDetail(jobKey);
  }

  @Override
  public List<? extends Trigger> getTriggersForJob(JobKey jobKey) throws SchedulerException {
    return scheduler.getTriggersOfJob(jobKey);
  }

  @Override
  public boolean isSchedulerStarted() throws SchedulerException {
    return scheduler.isStarted();
  }

}
