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
package org.apache.ambari.server.state.alert;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.agent.StaleAlert;
import org.apache.ambari.server.alerts.StaleAlertRunnable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class AlertHelper {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AlertHelper.class);

  /**
   * The multiplier for the interval of the definition which is being checked
   * for staleness. If this value is {@code 2}, then alerts are considered stale
   * if they haven't run in more than 2x their interval value.
   */
  private static final int INTERVAL_WAIT_FACTOR_DEFAULT = 2;

  /**
   * A parameter which exposes the interval multipler to use for calculating
   * staleness. If this does not exist, then
   * {@link #INTERVAL_WAIT_FACTOR_DEFAULT} will be used.
   */
  private static final String STALE_INTERVAL_MULTIPLIER_PARAM_KEY = "stale.interval.multiplier";

  /**
   * Contains stale alerts with stale timestamp by host id.
   */
  private ConcurrentMap<Long, ConcurrentMap<Long, Long>> staleAlerts = new ConcurrentHashMap<>();

  /**
   * Gets the wait factor multiplier off of the definition, returning
   * {@link #INTERVAL_WAIT_FACTOR_DEFAULT} if not specified. This will look for
   * {@link #STALE_INTERVAL_MULTIPLIER_PARAM_KEY} in the definition parameters.
   * The value returned from this method will be guaranteed to be in the range
   * of 2 to 10.
   *
   * @param definition
   *          the definition to read
   * @return the wait factor interval multiplier
   */
  public int getWaitFactorMultiplier(AlertDefinition definition) {
    // start with the default
    int waitFactor = INTERVAL_WAIT_FACTOR_DEFAULT;

    // coerce the entity into a business object so that the list of parameters
    // can be extracted and used for threshold calculation
    try {
      ServerSource serverSource = (ServerSource) definition.getSource();
      List<ParameterizedSource.AlertParameter> parameters = serverSource.getParameters();
      for (ParameterizedSource.AlertParameter parameter : parameters) {
        Object value = parameter.getValue();

        if (StringUtils.equals(parameter.getName(), STALE_INTERVAL_MULTIPLIER_PARAM_KEY)) {
          waitFactor = getThresholdValue(value, INTERVAL_WAIT_FACTOR_DEFAULT);
        }
      }

      if (waitFactor < 2 || waitFactor > 10) {
        LOG.warn(
            "The interval multipler of {} is outside the valid range for {} and will be set to 2",
            waitFactor, definition.getLabel());

        waitFactor = 2;
      }
    } catch (Exception exception) {
      LOG.error("Unable to read the {} parameter for {}", STALE_INTERVAL_MULTIPLIER_PARAM_KEY,
          StaleAlertRunnable.class.getSimpleName(), exception);
    }

    return waitFactor;
  }

  /**
   * Converts the given value to an integer safely.
   *
   * @param value
   * @param defaultValue
   * @return
   */
  public int getThresholdValue(Object value, int defaultValue) {
    if (null == value) {
      return defaultValue;
    }

    if (value instanceof Number) {
      return ((Number) value).intValue();
    }

    if (!(value instanceof String)) {
      value = value.toString();
    }

    if (!NumberUtils.isNumber((String) value)) {
      return defaultValue;
    }

    Number number = NumberUtils.createNumber((String) value);
    return number.intValue();
  }

  /**
   * Saves stale alerts for specified host.
   * @param hostId host id
   * @param staleAlertsDefinitionId list of stale alerts
   */
  public void addStaleAlerts(Long hostId, List<StaleAlert> staleAlertsDefinitionId) {
    staleAlerts.put(hostId, new ConcurrentHashMap<>());
    ConcurrentMap<Long, Long> hostStaleAlerts = staleAlerts.get(hostId);
    staleAlertsDefinitionId.forEach(s -> hostStaleAlerts.put(s.getId(), s.getTimestamp()));
  }

  /**
   * Retrieves stale alerts for specified host.
   * @param hostId host id
   * @return
   */
  public Map<Long, Long> getStaleAlerts(Long hostId) {
    return staleAlerts.containsKey(hostId) ? new HashMap<>(staleAlerts.get(hostId)) : Collections.emptyMap();
  }

  /**
   * Removes all stale alerts for specified host.
   * @param hostId host id
   */
  public void clearStaleAlerts(Long hostId) {
    staleAlerts.remove(hostId);
  }

  /**
   * Removes specified stale alert from specified host.
   * @param hostId host id.
   * @param definitionId alert definition id.
   */
  public void clearStaleAlert(Long hostId, Long definitionId) {
    if (staleAlerts.containsKey(hostId)) {
      staleAlerts.get(hostId).remove(definitionId);
    }
  }

  /**
   * Removes specified stale alert from specified host.
   * @param definitionId alert definition id.
   */
  public void clearStaleAlert(Long definitionId) {
    staleAlerts.forEach((k, v) -> v.remove(definitionId));
  }

  /**
   * Retrieves all host ids containing specified definition id.
   * @param definitionId alert definition id.
   * @return host ids list.
   */
  public List<Long> getHostIdsByDefinitionId(Long definitionId) {
    return staleAlerts.entrySet().stream()
        .filter(e -> e.getValue().containsKey(definitionId))
        .map(e -> e.getKey()).collect(Collectors.toList());
  }
}
