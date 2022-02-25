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

from resource_management.libraries.functions.is_empty import is_empty
import re

def get_path_from_url(address):
  """
  Return port from URL. If the address is numeric, the address is assumed to be a port and is returned.
  If address is UnknownConfiguration, UnknownConfiguration will be returned.
  """
  if is_empty(address):
    return address

  result = re.findall("^((.+)://)?(([a-zA-Z0-9]|\.|-)*)(:([\d]{2,}))?/(.*)$", address)
  if result:
    return result[0][6]
  return None