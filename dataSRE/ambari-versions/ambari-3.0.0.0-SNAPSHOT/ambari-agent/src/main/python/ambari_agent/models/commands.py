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


class AgentCommand(object):
  status = "STATUS_COMMAND"
  get_version = "GET_VERSION"
  execution = "EXECUTION_COMMAND"
  auto_execution = "AUTO_EXECUTION_COMMAND"
  background_execution = "BACKGROUND_EXECUTION_COMMAND"

  AUTO_EXECUTION_COMMAND_GROUP = [execution, auto_execution, background_execution]
  EXECUTION_COMMAND_GROUP = [execution, background_execution]


class RoleCommand(object):
  install = 'INSTALL'
  start = 'START'
  stop = 'STOP'
  custom_command = 'CUSTOM_COMMAND'


class CustomCommand(object):
  restart = 'RESTART'
  reconfigure = 'RECONFIGURE'
  start = RoleCommand.start


class CommandStatus(object):
  in_progress = 'IN_PROGRESS'
  completed = 'COMPLETED'
  failed = 'FAILED'
