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

import time
from ambari_agent import hostname
from ambari_agent.Hardware import Hardware
from ambari_agent.HostInfo import HostInfo
from ambari_agent.Utils import Utils

class Register:
  """ Registering with the server. Get the hardware profile and
  declare success for now """
  def __init__(self, config):
    self.config = config
    self.hardware = Hardware(self.config)
    self.init_time_ms = int(1000*time.time())

  def build(self, response_id='-1'):
    timestamp = int(time.time()*1000)

    hostInfo = HostInfo(self.config)
    agentEnv = { }
    hostInfo.register(agentEnv, runExpensiveChecks=True)

    current_ping_port = self.config.get('agent','ping_port')

    register = { 'id'                : int(response_id),
                 'timestamp'         : timestamp,
                 'hostname'          : hostname.hostname(self.config),
                 'currentPingPort'   : int(current_ping_port),
                 'publicHostname'    : hostname.public_hostname(self.config),
                 'hardwareProfile'   : self.hardware.get(),
                 'agentEnv'          : agentEnv,
                 'agentVersion'      : Utils.read_agent_version(self.config),
                 'prefix'            : self.config.get('agent', 'prefix'),
                 'agentStartTime'    : self.init_time_ms
               }
    return register
