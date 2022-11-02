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
from ambari_commons.os_check import OSCheck

__all__ = ["log_process_information"]

def log_process_information(logger):
  """
  Check if certain configuration sent from the server has been received.
  """

  from ambari_commons.shell import shellRunner
  if OSCheck.is_windows_family():
    cmd_list = ["WMIC path win32_process get Caption,Processid,Commandline", "netstat -an"]
  else:
    cmd_list = ["export COLUMNS=9999 ; ps faux", "netstat -tulpn"]

  shell_runner = shellRunner()

  for cmd in cmd_list:
    ret = shell_runner.run(cmd)
    logger.info("Command '{0}' returned {1}. {2}{3}".format(cmd, ret["exitCode"], ret["error"], ret["output"]))
