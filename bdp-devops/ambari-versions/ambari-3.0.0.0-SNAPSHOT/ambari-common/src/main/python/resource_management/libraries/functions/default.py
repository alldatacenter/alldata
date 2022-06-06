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

__all__ = ['default', 'default_string']
from resource_management.libraries.script import Script
from resource_management.libraries.script.config_dictionary import UnknownConfiguration
from resource_management.core.logger import Logger

def default(name, default_value):
  subdicts = filter(None, name.split('/'))

  curr_dict = Script.get_config()
  if not curr_dict:
    return default_value
  for x in subdicts:
    if x in curr_dict:
      curr_dict = curr_dict[x]
    else:
      return default_value

  return curr_dict

def default_string(name, default_value, delimiter):
  default_list = default(name, default_value)
  return delimiter.join(default_list)
