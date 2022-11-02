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

import java.util.concurrent.TimeUnit;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;

/**
 * The {@link CachedAlertFlushService} is used to periodically flush cached
 * alert data to the database. This service is controlled by
 * {@link Configuration#isAlertCacheEnabled()} and
 * {@link Configuration#getAlertCacheFlushInterval()}.
 */
@AmbariService
@Experimental(feature = ExperimentalFeature.ALERT_CACHING)
public class CachedAlertFlushService extends AbstractScheduledService {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(CachedAlertFlushService.class);

  /**
   * Configuration.
   */
  @Inject
  private Configuration m_configuration;

  /**
   * Used for flushing cached entities to the database.
   */
  @Inject
  private AlertsDAO m_alertsDAO;

  /**
   * {@inheritDoc}
   */
  @Override
  protected Scheduler scheduler() {
    int flushIntervalInMinutes = m_configuration.getAlertCacheFlushInterval();
    return Scheduler.newFixedDelaySchedule(flushIntervalInMinutes, flushIntervalInMinutes,
        TimeUnit.MINUTES);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Invokes {@link #stop()} if not enabled.
   */
  @Override
  protected void startUp() throws Exception {
    boolean enabled = m_configuration.isAlertCacheEnabled();
    if (!enabled) {
      stopAsync();
    }
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Flushes cached alerts to the database.
   */
  @Override
  protected void runOneIteration() throws Exception {
    try {
      LOG.info("Flushing cached alerts to the database");
      m_alertsDAO.flushCachedEntitiesToJPA();
    } catch (Exception exception) {
      LOG.error("Unable to flush cached alerts to the database", exception);
    }
  }
}
