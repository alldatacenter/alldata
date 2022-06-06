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

__all__ = ['get_from_dict', 'convert_to_list']

class KeyNotFound(object):
  # Prohibit instantiation
  def __new__(cls):
    raise AttributeError("Instance creation is not supported.")

def get_from_dict(dict_, keys, default_value=KeyNotFound):
  """
  Tries to get an element from the nested dictionaries (dict_) using the key sequence (keys).
  For example, if keys == ['a', 'b', 'c'], will return dict_[a][b][c] if all keys are present in the map.
  If one of the keys is absent, it returns 'default_value'.
  """

  keys = convert_to_list(keys)

  curr_dict = dict_
  if not curr_dict:
    return default_value
  for x in keys:
    if isinstance(curr_dict, dict) and x in curr_dict:
      curr_dict = curr_dict[x]
    else:
      return default_value

  return curr_dict

def convert_to_list(keys):
  """
  If 'keys' is a list or a tuple, this function returns it unchanged.
  Otherwise returns a list that contains 'keys' as the only element.
  """
  if not isinstance(keys, (list, tuple)):
    return [keys]
  return keys
