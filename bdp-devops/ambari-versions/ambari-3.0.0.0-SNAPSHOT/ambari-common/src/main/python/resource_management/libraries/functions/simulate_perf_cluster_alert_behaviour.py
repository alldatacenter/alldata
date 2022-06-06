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
__all__ = ["simulate_perf_cluster_alert_behaviour"]

import logging
import random
import time

from datetime import datetime
from resource_management.core.exceptions import Fail

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

OK_MESSAGE = 'Ok'
FAIL_MESSAGE = 'Expected Fail'
UNKNOWN_MESSAGE = 'Expected Unknown'

logger = logging.getLogger('ambari_alerts')

return_values_map = {"true":[RESULT_CODE_OK, OK_MESSAGE], "false":[RESULT_CODE_CRITICAL, FAIL_MESSAGE],
                     "none":[RESULT_CODE_UNKNOWN, UNKNOWN_MESSAGE]}

def simulate_perf_cluster_alert_behaviour(alert_behaviour_properties, configurations):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  alert_behaviour_type=None
  alert_behaviour_type_key=alert_behaviour_properties["alert_behaviour_type"]
  if alert_behaviour_type_key in configurations:
    alert_behaviour_type = configurations[alert_behaviour_type_key].lower()

  if alert_behaviour_type == "percentage":
    alert_success_percentage=None
    alert_success_percentage_key=alert_behaviour_properties["alert_success_percentage"]

    if alert_success_percentage_key in configurations:
      alert_success_percentage = configurations[alert_success_percentage_key]

    if alert_success_percentage:
      random_number = random.uniform(0, 100)
      if random_number <= int(alert_success_percentage):
        return (RESULT_CODE_OK, [OK_MESSAGE])
      else:
        return (RESULT_CODE_CRITICAL, [FAIL_MESSAGE])
    else:
      raise Fail("Percentage behaviour was set but alert.success.percentage was not set!")
  elif alert_behaviour_type == "timeout":
    alert_timeout_return_value=None
    alert_timeout_secs=None
    alert_timeout_return_value_key=alert_behaviour_properties["alert_timeout_return_value"]
    alert_timeout_secs_key=alert_behaviour_properties["alert_timeout_secs"]

    if alert_timeout_return_value_key in configurations:
      alert_timeout_return_value = configurations[alert_timeout_return_value_key].lower()

    if alert_timeout_secs_key in configurations:
      alert_timeout_secs = configurations[alert_timeout_secs_key]

    if alert_timeout_return_value and alert_timeout_secs:
      logger.info("Sleeping for {0} seconds".format(alert_timeout_secs))
      print "Sleeping for {0} seconds".format(alert_timeout_secs)
      time.sleep(int(alert_timeout_secs))
      return (return_values_map[alert_timeout_return_value][0], [return_values_map[alert_timeout_return_value][1]])
    else:
      raise Fail("Timeout behaviour was set but alert.timeout.return.value/alert.timeout.secs were not set!")
  elif alert_behaviour_type == "flip":
    alert_flip_interval_mins=None
    alert_flip_interval_mins_key=alert_behaviour_properties["alert_flip_interval_mins"]

    if alert_flip_interval_mins_key in configurations:
      alert_flip_interval_mins = configurations[alert_flip_interval_mins_key]

    if alert_flip_interval_mins:
      curr_time = datetime.utcnow()
      return_value = ((curr_time.minute / int(alert_flip_interval_mins)) % 2) == 0
      return (return_values_map[str(return_value).lower()][0], [return_values_map[str(return_value).lower()][1]])
    else:
      raise Fail("Flip behaviour was set but alert.flip.interval.mins was not set!")



  result_code = RESULT_CODE_OK
  label = OK_MESSAGE
  return (result_code, [label])