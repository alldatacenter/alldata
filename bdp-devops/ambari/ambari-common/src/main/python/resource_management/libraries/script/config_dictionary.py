#!/usr/bin/env python

'''
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
'''
from resource_management.core.exceptions import Fail
from resource_management.core.encryption import ensure_decrypted

IMMUTABLE_MESSAGE = """Configuration dictionary is immutable!

For adding dynamic properties to xml files please use {{varible_from_params}} substitutions.
Lookup xml files for {{ for examples. 
"""

class ConfigDictionary(dict):
  """
  Immutable config dictionary
  """
  
  def __init__(self, dictionary):
    """
    Recursively turn dict to ConfigDictionary
    """
    for k, v in dictionary.iteritems():
      if isinstance(v, dict):
        dictionary[k] = ConfigDictionary(v)
        
    super(ConfigDictionary, self).__init__(dictionary)

  def __setitem__(self, name, value):
    raise Fail(IMMUTABLE_MESSAGE)

  def __getitem__(self, name):
    """
    - use Python types
    - enable lazy failure for unknown configs. 
    """
    try:
      value = super(ConfigDictionary, self).__getitem__(name)
    except KeyError:
      return UnknownConfiguration(name)

    value = ensure_decrypted(value)

    if value == "true":
      value = True
    elif value == "false":
      value = False
    
    return value


class UnknownConfiguration():
  """
  Lazy failing for unknown configs.
  """
  def __init__(self, name):
    self.name = name
   
  def __getattr__(self, name):
    raise Fail("Configuration parameter '" + self.name + "' was not found in configurations dictionary!")
  
  def __getitem__(self, name):
    """
    Allow [] 
    """
    return self
