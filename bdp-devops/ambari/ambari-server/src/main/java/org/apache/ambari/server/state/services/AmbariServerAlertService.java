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
package org.apache.ambari.server.state.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.alerts.AlertRunnable;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.ServerSource;
import org.apache.ambari.server.state.alert.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * The {@link AmbariServerAlertService} is used to manage the dynamically loaded
 * {@link AlertRunnable}s which perform server-side alert checks. The
 * {@link AlertRunnable}s are scheduled using a {@link ScheduledExecutorService}
 * with a fixed thread pool size.
 */
@AmbariService
public class AmbariServerAlertService extends AbstractScheduledService {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AmbariServerAlertService.class);

  /**
   * Used to inject the constructed {@link Runnable}s.
   */
  @Inject
  private Injector m_injector;

  /**
   * Used for looking up alert definitions.
   */
  @Inject
  private AlertDefinitionDAO m_dao;

  /**
   * Used to get alert definitions to use when generating alert instances.
   */
  @Inject
  private Provider<Clusters> m_clustersProvider;

  /**
   * Used to coerce {@link AlertDefinitionEntity} into {@link AlertDefinition}.
   */
  @Inject
  private AlertDefinitionFactory m_alertDefinitionFactory;

  /**
   * The executor to use to run all {@link Runnable} alert classes.
   */
  private ScheduledExecutorService m_scheduledExecutorService;

  /**
   * A map of all of the definition names to {@link ScheduledFuture}s.
   */
  private final Map<String, ScheduledAlert> m_futureMap = new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   */
  public AmbariServerAlertService() {
  }

  @Inject
  public void initExecutor(@Named("alertServiceCorePoolSize") int alertServiceCorePoolSize) {
    this.m_scheduledExecutorService = Executors.newScheduledThreadPool(alertServiceCorePoolSize);
  }
  /**
   * {@inheritDoc}
   */
  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Loads all of the definitions with SERVER source type and schedules
   * the ones that are enabled.
   */
  @Override
  protected void startUp() throws Exception {
    Map<String, Cluster> clusterMap = m_clustersProvider.get().getClusters();
    for (Cluster cluster : clusterMap.values()) {
      for (AlertDefinitionEntity entity : m_dao.findBySourceType(cluster.getClusterId(), SourceType.SERVER)) {
        // don't schedule disabled alert definitions
        if (!entity.getEnabled()) {
          continue;
        }
        // schedule the Runnable for the definition
        scheduleRunnable(entity);
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Compares all known {@link RootComponent#AMBARI_SERVER} alerts with those that
   * are scheduled. If any are not scheduled or have their intervals changed,
   * then reschedule those.
   */
  @Override
  protected void runOneIteration() throws Exception {
    Map<String, Cluster> clusterMap = m_clustersProvider.get().getClusters();
    for (Cluster cluster : clusterMap.values()) {
      // get all of the cluster alerts with SERVER source type
      List<AlertDefinitionEntity> entities = m_dao.findBySourceType(cluster.getClusterId(), SourceType.SERVER);

      // for each alert, check to see if it's scheduled correctly
      for (AlertDefinitionEntity entity : entities) {
        String definitionName = entity.getDefinitionName();
        ScheduledAlert scheduledAlert = m_futureMap.get(definitionName);

        // disabled or new alerts may not have anything mapped yet
        ScheduledFuture<?> scheduledFuture = null;
        if (null != scheduledAlert) {
          scheduledFuture = scheduledAlert.getScheduledFuture();
        }

        // if the definition is not enabled, ensure it's not scheduled and
        // then continue to the next one
        if (!entity.getEnabled()) {
          if (null != scheduledFuture) {
            unschedule(definitionName, scheduledFuture);
          }

          continue;
        }

        // if the definition hasn't been scheduled, then schedule it
        if (null == scheduledAlert || null == scheduledFuture) {
          scheduleRunnable(entity);
          continue;
        }

        // compare the delay of the future to the definition; if they don't
        // match then reschedule this definition
        int scheduledInterval = scheduledAlert.getInterval();
        if (scheduledInterval != entity.getScheduleInterval()) {
          // unschedule
          unschedule(definitionName, scheduledFuture);

          // reschedule
          scheduleRunnable(entity);
        }
      }
    }
  }

  /**
   * Invokes {@link ScheduledFuture#cancel(boolean)} and removes the mapping
   * from {@link #m_futureMap}. Does nothing if the future is not scheduled.
   *
   * @param scheduledFuture
   */
  private void unschedule(String definitionName, ScheduledFuture<?> scheduledFuture) {

    m_futureMap.remove(definitionName);

    if (null != scheduledFuture) {
      scheduledFuture.cancel(true);
      LOG.info("Unscheduled server alert {}", definitionName);
    }
  }

  /**
   * Schedules the {@link Runnable} referenced by the
   * {@link AlertDefinitionEntity} to run at a fixed interval.
   *
   * @param entity
   *          the entity to schedule the runnable for (not {@code null}).
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  private void scheduleRunnable(AlertDefinitionEntity entity)
      throws ClassNotFoundException,
      IllegalAccessException, InstantiationException {

    // if the definition is disabled, do nothing
    if (!entity.getEnabled()) {
      return;
    }

    AlertDefinition definition = m_alertDefinitionFactory.coerce(entity);
    ServerSource serverSource = (ServerSource) definition.getSource();
    String sourceClass = serverSource.getSourceClass();
    int interval = definition.getInterval();

    try {
      Class<?> clazz = Class.forName(sourceClass);
      if (!AlertRunnable.class.isAssignableFrom(clazz)) {
        LOG.warn(
            "Unable to schedule a server side alert for {} because it is not an {}", sourceClass,
            AlertRunnable.class);

        return;
      }

      // instantiate and inject
      Constructor<? extends AlertRunnable> constructor = clazz.asSubclass(
          AlertRunnable.class).getConstructor(String.class);

      AlertRunnable alertRunnable = constructor.newInstance(entity.getDefinitionName());
      m_injector.injectMembers(alertRunnable);

      // schedule the runnable alert
      ScheduledFuture<?> scheduledFuture = m_scheduledExecutorService.scheduleWithFixedDelay(
          alertRunnable, interval, interval, TimeUnit.MINUTES);

      String definitionName = entity.getDefinitionName();
      ScheduledAlert scheduledAlert = new ScheduledAlert(scheduledFuture, interval);
      m_futureMap.put(definitionName, scheduledAlert);

      LOG.info("Scheduled server alert {} to run every {} minutes",
          definitionName, interval);

    } catch (ClassNotFoundException cnfe) {
      LOG.error(
          "Unable to schedule a server side alert for {} because it could not be found in the classpath",
          sourceClass);
    } catch (NoSuchMethodException nsme) {
      LOG.error(
          "Unable to schedule a server side alert for {} because it does not have a constructor which takes the proper arguments.",
          sourceClass);
    } catch (InvocationTargetException ite) {
      LOG.error(
          "Unable to schedule a server side alert for {} because an exception occurred while constructing the instance.",
          sourceClass, ite);
    }
  }

  /**
   * The {@link ScheduledAlert} class is used as a way to encapsulate a
   * {@link ScheduledFuture} with the interval it was scheduled with.
   */
  private static final class ScheduledAlert {
    private final ScheduledFuture<?> m_scheduledFuture;
    private final int m_interval;


    /**
     * Constructor.
     *
     * @param scheduledFuture
     * @param interval
     */
    private ScheduledAlert(ScheduledFuture<?> scheduledFuture, int interval) {

      m_scheduledFuture = scheduledFuture;
      m_interval = interval;
    }

    /**
     * @return the scheduledFuture
     */
    private ScheduledFuture<?> getScheduledFuture() {
      return m_scheduledFuture;
    }

    /**
     * @return the interval
     */
    private int getInterval() {
      return m_interval;
    }
  }
}
