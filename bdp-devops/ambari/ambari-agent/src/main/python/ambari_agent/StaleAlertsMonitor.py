#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import time
import logging

logger = logging.getLogger(__name__)

class StaleAlertsMonitor():
  """
  For tracks which alerts were not ran or collected for a long time, to report the data to server.
  Which is shown as a separate alert on server.
  """
  def __init__(self, initializer_module):
    self.alert_definitions_cache = initializer_module.alert_definitions_cache
    #self.stale_interval_multiplier = 2
    self.alerts_run_time = {}
    self.alert_created_time = {}

  def save_executed_alerts(self, alerts):
    """
    Saves the last ran time for all the alerts passed
    """
    for alert in alerts:
      timestamp = alert['timestamp'] / 1000
      alert_id = alert['definitionId']
      self.alerts_run_time[alert_id] = timestamp

  def get_stale_alerts(self):
    """
    Gets alerts which were not ran/collected for stale_interval_multiplier*alert_interval time
    """
    stale_alert_ids = []
    curr_time = time.time()

    if not self.alert_definitions_cache:
      return []

    for cluster_id, command in self.alert_definitions_cache.iteritems():
      # the cluster has not yet initialized staleAlertsAlert
      if not 'staleIntervalMultiplier' in command:
        continue

      stale_interval_multiplier = command['staleIntervalMultiplier']
      for definition in command['alertDefinitions']:
        definition_id = definition['definitionId']
        if not definition_id in self.alerts_run_time:
          self.alerts_run_time[definition_id] = curr_time

        interval_seconds = definition['interval']*60

        if curr_time > self.alerts_run_time[definition_id]+(interval_seconds*stale_interval_multiplier):
          logger.info("Alert %s got stale. Reporting to the server.", definition['name'])
          last_run_time_ms = int(self.alerts_run_time[definition_id]*1000)
          stale_alert_ids.append({"id": definition_id, "timestamp": last_run_time_ms})

    return stale_alert_ids