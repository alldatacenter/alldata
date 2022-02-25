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

import resource

WARNING_KEY = "ulimit.warning.threshold"
CRITICAL_KEY = "ulimit.critical.threshold"

DEFAULT_WARNING_KEY = 200000
DEFAULT_CRITICAL_KEY = 800000

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return None

def execute(configurations={}, parameters={}, host_name=None):
  """
  Performs advanced ulimit checks under Linux.

  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running

  """

  # try:
  soft_ulimit, hard_ulimiit = resource.getrlimit(resource.RLIMIT_NOFILE)
  return_code, label = _get_warnings_for_partition(parameters, soft_ulimit)
  # except Exception as e:
  #   return 'CRITICAL', ["Unable to determine ulimit for open files (-n)"]

  return return_code, [label]

def _get_warnings_for_partition(parameters, soft_ulimit):

  # start with hard coded defaults
  warning_count = DEFAULT_WARNING_KEY
  critical_count = DEFAULT_CRITICAL_KEY

  if WARNING_KEY in parameters:
    warning_count = int(parameters[WARNING_KEY])

  if CRITICAL_KEY in parameters:
    critical_count = int(parameters[CRITICAL_KEY])

  if soft_ulimit is None or soft_ulimit == "":
    return 'CRITICAL', ['Unable to determine ulimit for open files (-n)']

  return_code = "OK"
  label = "Ulimit for open files (-n) is {0}".format(soft_ulimit)

  if soft_ulimit >= critical_count:
    label = "Ulimit for open files (-n) is {0} which is higher or equal than critical value of {1}".format(soft_ulimit, critical_count)
    return_code = 'CRITICAL'
  elif soft_ulimit >= warning_count:
    label = "Ulimit for open files (-n) is {0} which is higher or equal than warning value of {1}".format(soft_ulimit, warning_count)
    return_code = 'WARNING'

  return return_code, label

