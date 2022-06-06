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

from resource_management import *


def mysql_service(daemon_name=None, action='start'): 
  status_cmd = format("pgrep -l '^{process_name}$'")
  cmd = ('service', daemon_name, action)

  if action == 'status':
    try:
      Execute(status_cmd)
    except Fail:
      raise ComponentIsNotRunning()
  elif action == 'stop':
    import params
    Execute(cmd,
            logoutput = True,
            only_if = status_cmd,
            sudo = True,
    )
  elif action == 'start':
    import params   
    Execute(cmd,
      logoutput = True,
      not_if = status_cmd,
      sudo = True,
    )



