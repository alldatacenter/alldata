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
from resource_management.core.exceptions import Fail
import re

def get_port_from_url(address):
  """
  Return port from URL. If the address is numeric, the address is assumed to be a port and is returned.
  If address is UnknownConfiguration, UnknownConfiguration will be returned. 
  If no port was found, Fail will be raised.
  """
  
  if is_empty(address):
    return address

  if isinstance(address, (int, long)):
    return address

  if address is None or address.strip() == "":
    return ""
  
  port = re.findall(":([\d]{1,5})(?=/|$)", address)
  if port:
    return port[0]
  elif address.isdigit():
    return address

  raise Fail("No port in URL:{0}".format(address))
