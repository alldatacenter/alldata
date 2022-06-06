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
package org.apache.ambari.server.actionmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.events.publishers.JPAEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

import junit.framework.Assert;

/**
 * Tests {@link ActionScheduler}, focusing on multi-threaded concerns.
 */
public class TestActionSchedulerThreading {

  private static Injector injector;

  private Clusters clusters;
  private OrmTestHelper ormTestHelper;

  /**
   * Setup test methods.
   *
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    ormTestHelper = injector.getInstance(OrmTestHelper.class);
  }

  /**
   * Cleanup test methods.
   */
  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * Tests that applying configurations for a given stack correctly sets
   * {@link DesiredConfig}s. This takes into account EclipseLink caching issues
   * by specifically checking the actionscheduler headless thread.
   */
  @Test
  public void testDesiredConfigurationsAfterApplyingLatestForStackInOtherThreads()
      throws Exception {
    long clusterId = ormTestHelper.createCluster(UUID.randomUUID().toString());
    Cluster cluster = clusters.getCluster(clusterId);
    ormTestHelper.addHost(clusters, cluster, "h1");

    StackId stackId = cluster.getCurrentStackVersion();
    StackId newStackId = new StackId("HDP-2.2.0");
    RepositoryVersionEntity repoVersion220 = ormTestHelper.getOrCreateRepositoryVersion(newStackId, "2.2.0-1234");

    // make sure the stacks are different
    Assert.assertFalse(stackId.equals(newStackId));

    // add a service
    String serviceName = "ZOOKEEPER";
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(cluster);
    Service service = cluster.addService(serviceName, repositoryVersion);
    String configType = "zoo.cfg";

    Map<String, String> properties = new HashMap<>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);

    // zoo-cfg for v1 on current stack
    properties.put("foo-property-1", "foo-value-1");
    Config c1 = configFactory.createNew(stackId, cluster, configType, "version-1", properties, propertiesAttributes);

    // make v1 "current"
    cluster.addDesiredConfig("admin", Sets.newHashSet(c1), "note-1");

    // bump the stack
    service.setDesiredRepositoryVersion(repoVersion220);

    // save v2
    // zoo-cfg for v2 on new stack
    properties.put("foo-property-2", "foo-value-2");
    Config c2 = configFactory.createNew(newStackId, cluster, configType, "version-2", properties, propertiesAttributes);

    // make v2 "current"
    cluster.addDesiredConfig("admin", Sets.newHashSet(c2), "note-2");

    // check desired config
    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    desiredConfig = desiredConfigs.get(configType);
    assertNotNull(desiredConfig);
    assertEquals(Long.valueOf(2), desiredConfig.getVersion());
    assertEquals("version-2", desiredConfig.getTag());

    final String hostName = cluster.getHosts().iterator().next().getHostName();

    // move the stack back to the old stack
    service.setDesiredRepositoryVersion(repositoryVersion);

    // create the semaphores, taking 1 from each to make them blocking from the
    // start
    Semaphore applyLatestConfigsSemaphore = new Semaphore(1, true);
    Semaphore threadInitialCachingSemaphore = new Semaphore(1, true);
    threadInitialCachingSemaphore.acquire();
    applyLatestConfigsSemaphore.acquire();

    final InstrumentedActionScheduler runnable = new InstrumentedActionScheduler(clusterId, hostName,
        threadInitialCachingSemaphore, applyLatestConfigsSemaphore);

    injector.injectMembers(runnable);

    final Thread thread = new Thread(runnable);

    // start the thread to populate the data in it, waiting to ensure that it
    // finishes the calls it needs to make
    thread.start();
    threadInitialCachingSemaphore.acquire();

    // apply the configs for the old stack
    cluster.applyLatestConfigurations(stackId, serviceName);

    // wake the thread up and have it verify that it can see the updated configs
    applyLatestConfigsSemaphore.release();

    // wait for the thread to finish
    thread.join();

    // if the thread failed, then fail this test
    runnable.validateAssertions();
  }

  /**
   * Checks the desired configurations of a cluster in a separate thread in
   * order to test whether the {@link EntityManager} has evicted stale entries.
   */
  private final static class InstrumentedActionScheduler extends ActionScheduler {
    private final long clusterId;
    private final Semaphore threadInitialCachingSemaphore;
    private final Semaphore applyLatestConfigsSemaphore;
    private final String hostName;
    private Throwable throwable = null;

    @Inject
    private ConfigHelper configHelper;

    @Inject
    private Clusters clusters;

    @Inject
    private UnitOfWork unitOfWork;

    /**
     * Constructor.
     *
     * @param clusterId
     * @param hostName
     * @param threadInitialCachingSemaphore
     * @param applyLatestConfigsSemaphore
     */
    private InstrumentedActionScheduler(long clusterId, String hostName,
        Semaphore threadInitialCachingSemaphore, Semaphore applyLatestConfigsSemaphore) {

      super(1000, 1000, injector.getInstance(ActionDBAccessor.class),
          injector.getInstance(JPAEventPublisher.class));

      this.clusterId = clusterId;
      this.threadInitialCachingSemaphore = threadInitialCachingSemaphore;
      this.applyLatestConfigsSemaphore = applyLatestConfigsSemaphore;
      this.hostName = hostName;
    }

    /**
     *
     */
    @Override
    public void run() {
      unitOfWork.begin();
      try {
        // this is an important line - it replicates what the real scheduler
        // does during its work routine by grabbing the current thread's
        // EntityManager so the event can property evict entries
        threadEntityManager = entityManagerProvider.get();

        // first get the configs in order to cache the entities in this thread's
        // L1 cache
        Cluster cluster = clusters.getCluster(clusterId);

        // {zoo.cfg={tag=version-2}}
        Map<String, Map<String, String>> effectiveDesiredTags = configHelper.getEffectiveDesiredTags(
            cluster, hostName);

        assertEquals("version-2", effectiveDesiredTags.get("zoo.cfg").get("tag"));

        // signal the caller that we're done making our initial call to populate
        // the EntityManager
        threadInitialCachingSemaphore.release();

        // wait for the method to switch configs
        applyLatestConfigsSemaphore.acquire();

        // {zoo.cfg={tag=version-1}}
        effectiveDesiredTags = configHelper.getEffectiveDesiredTags(cluster, hostName);
        assertEquals("version-1", effectiveDesiredTags.get("zoo.cfg").get("tag"));
      } catch (Throwable throwable) {
        this.throwable = throwable;
      } finally {
        applyLatestConfigsSemaphore.release();
        unitOfWork.end();
      }
    }

    /**
     * Raise an assertion if the thread failed.
     */
    private void validateAssertions() {
      if (null != throwable) {
        throw new AssertionError(throwable);
      }
    }
  }

}
