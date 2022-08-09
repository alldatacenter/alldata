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

Ambari Agent

"""

__all__ = ['expect']
from resource_management.libraries.script import Script
from resource_management.libraries.script.config_dictionary import UnknownConfiguration
from resource_management.core.exceptions import Fail

def expect(name, expected_type, default_value=None):
  """
  Expect configuration to be of certain type. If it is not, give a reasonable error message to user.

  Optionally if the configuration is not found default_value for it can be returned.
  """
  subdicts = filter(None, name.split('/'))

  curr_dict = Script.get_config()
  for x in subdicts:
    if x in curr_dict:
      curr_dict = curr_dict[x]
    else:
      if default_value:
        return default_value
      return UnknownConfiguration(curr_dict[-1])
  value = curr_dict

  if expected_type == bool:
    if isinstance(value, bool):
      return value
    elif isinstance(value, basestring):
      if value != None and value.lower() == "true":
        value = True
      elif value != None and value.lower() == "false":
        value = False
      else:
        raise Fail("Configuration {0} expected to be boolean (true or false), but found '{1}'".format(name, value))
    else:
      type_name = type(value).__name__
      raise Fail("Configuration {0} expected to be boolean (true or false), but found instance of unknown type '{1}'".format(name, type_name))
  elif expected_type in [int, long, float]:
    try:
      value = expected_type(value)
    except (ValueError, TypeError):
      raise Fail("Configuration {0} expected to be number, but found '{1}'".format(name, value))
  return value


def expect_v2(name, expected_type, default_value=None):
  """
  Expect configuration to be of certain type. If it is not, give a reasonable error message to user.

  Optionally if the configuration is not found default_value for it can be returned.
  """

  value = Script.get_execution_command().get_value(name, default_value)
  if not value:
    return UnknownConfiguration(name)
  elif value == default_value:
    return value

  if expected_type == bool:
    if isinstance(value, bool):
      return value
    elif isinstance(value, basestring):
      if value != None and value.lower() == "true":
        value = True
      elif value != None and value.lower() == "false":
        value = False
      else:
        raise Fail("Configuration {0} expected to be boolean (true or false), but found '{1}'".format(name, value))
    else:
      type_name = type(value).__name__
      raise Fail(
        "Configuration {0} expected to be boolean (true or false), but found instance of unknown type '{1}'".format(
          name, type_name))
  elif expected_type in [int, long, float]:
    try:
      value = expected_type(value)
    except (ValueError, TypeError):
      raise Fail("Configuration {0} expected to be number, but found '{1}'".format(name, value))
  return value