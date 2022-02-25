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
import logging

logger = logging.getLogger()
installScriptHash = -1


class BackgroundCommandExecutionHandle:
  
  SCHEDULED_STATUS = 'SCHEDULED'
  RUNNING_STATUS = 'RUNNING'
  STOP_REQUEST_STATUS = 'STOP_REQUEST'
  STOPPED_STATUS = 'SCHEDULED'
  
  def __init__(self, command, commandId, on_background_command_started, on_background_command_complete_callback):
    self.command = command
    self.pid = 0
    self.status = None
    self.exitCode = None
    self.commandId = commandId
    self.on_background_command_started = on_background_command_started
    self.on_background_command_complete_callback = on_background_command_complete_callback

  def __str__(self):
    return "[BackgroundHandle: pid='{0}', status='{1}', exitCode='{2}', commandId='{3}']".format(self.pid, self.status, self.exitCode, self.commandId)
