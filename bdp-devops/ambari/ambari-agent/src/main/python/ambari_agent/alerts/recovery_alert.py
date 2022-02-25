#!/usr/bin/env python

"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import logging
import datetime
from alerts.base_alert import BaseAlert
logger = logging.getLogger(__name__)

# default recoveries counts
DEFAULT_WARNING_RECOVERIES_COUNT = 2
DEFAULT_CRITICAL_RECOVERIES_COUNT = 4

UNKNOWN_COMPONENT = 'UNKNOWN_COMPONENT'
class RecoveryAlert(BaseAlert):

  def __init__(self, alert_meta, alert_source_meta, config, recovery_manager):
    super(RecoveryAlert, self).__init__(alert_meta, alert_source_meta, config)

    self.recovery_manager = recovery_manager
    self.warning_count = DEFAULT_WARNING_RECOVERIES_COUNT
    self.critical_count = DEFAULT_CRITICAL_RECOVERIES_COUNT

    if 'reporting' in alert_source_meta:
      reporting = alert_source_meta['reporting']
      reporting_state_warning = self.RESULT_WARNING.lower()
      reporting_state_critical = self.RESULT_CRITICAL.lower()

      if reporting_state_warning in reporting and \
          'count' in reporting[reporting_state_warning]:
        self.warning_count = reporting[reporting_state_warning]['count']

      if reporting_state_critical in reporting and \
          'count' in reporting[reporting_state_critical]:
        self.critical_count = reporting[reporting_state_critical]['count']
    if self.critical_count <= self.warning_count:
      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("[Alert][{0}] The CRITICAL value of {1} must be greater than the WARNING value of {2}".format(
          self.get_name(), self.critical_count, self.warning_count))

  def _collect(self):

    component = UNKNOWN_COMPONENT
    if 'componentName' in self.alert_meta:
      component = self.alert_meta['componentName']

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("[Alert][{0}] Checking recovery operations for {1}".format(
        self.get_name(), component))

    recovery_action_info = {}
    recovery_actions = self.recovery_manager.get_actions_copy()
    if component in recovery_actions:
      recovery_action_info = recovery_actions[component]

    warned_threshold_reached = False
    if 'warnedThresholdReached' in recovery_action_info:
      warned_threshold_reached = recovery_action_info['warnedThresholdReached']

    recovered_times = 0
    lastResetText = ""

    # The alert should not go away if warned_threshold_reached (max_lifetime_count reached)
    if not self.recovery_manager.is_action_info_stale(component) or warned_threshold_reached:
      if 'count' in recovery_action_info:
        recovered_times = recovery_action_info['count']
      if 'lastReset' in recovery_action_info:
        lastResetText = " since " + str(datetime.datetime.fromtimestamp(recovery_action_info['lastReset']))

    if recovered_times >= self.critical_count or warned_threshold_reached:
      result = self.RESULT_CRITICAL
    elif recovered_times >= self.warning_count:
      result = self.RESULT_WARNING
    elif recovered_times < self.warning_count and \
        recovered_times < self.critical_count:
      result = self.RESULT_OK
    else:
      result = self.RESULT_UNKNOWN

    return (result, [lastResetText, recovered_times, component])

  def _get_reporting_text(self, state):
    '''
    Gets the default reporting text to use when the alert definition does not
    contain any.
    :param state: the state of the alert in uppercase (such as OK, WARNING, etc)
    :return:  the parametrized text
    '''
    if state == self.RESULT_OK:
      return 'No recovery operations executed for {2}{0}.'
    return '{1} recovery operations executed for {2}{0}.'
