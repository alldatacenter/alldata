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
import re

# [a,b,c]
REGEX_LIST = '^\w*\[.+\]\w*$'

# {a: v, b: v2, c: v3}
REGEX_DICTIONARY = '^\w*\{.+\}\w*$'

"""
storm-cluster:
  hosts:
    [c6401.ambari.apache.org, c6402.ambari.apache.org, c6403-master.ambari.apache.org]
  groups:
    [hadoop, hadoop-secure]

^\s* - allow any whitespace or newlines to start
\S+ - at least 1 word character (including dashes)
[ ]*:[ ]* - followed by a colon (allowing spaces around the colon)
[\r\n\f]+ - at least 1 newline

\s*\S+[ ]*:[ ]*[\r\n\f] - follow with the same basically to ensure a map of maps
"""
REGEX_NESTED_MAPS = "^\s*\S+[ ]*:[ ]*[\r\n\f]+\s*\S+[ ]*:[ ]*[\r\n\f]"


def escape_yaml_property(value):
  unquouted_values = ["null", "Null", "NULL", "true", "True", "TRUE", "false",
    "False", "FALSE", "YES", "Yes", "yes", "NO", "No", "no", "ON", "On", "on",
    "OFF", "Off", "off"]

  # known list of boolean/null types
  if value in unquouted_values:
    return value

  # quick pythonic check for integer
  try:
    int(value)
    return value
  except ValueError:
    pass

  # quick pythonic check for float
  try:
    float(value)
    return value
  except ValueError:
    pass

  # if is list [a,b,c] or dictionary {a: v, b: v2, c: v3}
  if re.match(REGEX_LIST, value) or re.match(REGEX_DICTIONARY, value):
    return value

  # check for a nested map
  if re.match(REGEX_NESTED_MAPS, value):
    # nested maps must begin on a newline and not have whitespace on the first line
    value = value.lstrip()
    return "\n" + value

  # no more checks, so assume it's a string a quote it
  value = value.replace("'", "''")
  value = "'" + value + "'"
  return value


def get_values_from_yaml_array(yaml_array):
  """
  Converts a YAML array into a normal array of values. For example, this
  will turn ['c6401','c6402']
  into an array with two strings, "c6401" and "c6402".
  :param yaml_array: the YAML array to convert
  :return:  a python array or None if the value could not be converted correctly.
  """
  if yaml_array is None:
    return None

  matches = re.findall(r'[\'|\"](.+?)[\'|\"]', yaml_array)
  if matches is None or len(matches) == 0:
    return None

  return matches
