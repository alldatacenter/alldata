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

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell
__all__ = ["check_process_status"]

import os

def check_process_status(pid_file):
  """
  Function checks whether process is running.
  Process is considered running, if pid file exists, and process with
  a pid, mentioned in pid file is running
  If process is not running, will throw ComponentIsNotRunning exception

  @param pid_file: path to service pid file
  """
  from resource_management.core import sudo

  if not pid_file or not os.path.isfile(pid_file):
    Logger.info("Pid file {0} is empty or does not exist".format(str(pid_file)))
    raise ComponentIsNotRunning()
  
  try:
    pid = int(sudo.read_file(pid_file))
  except:
    Logger.info("Pid file {0} does not exist or does not contain a process id number".format(pid_file))
    raise ComponentIsNotRunning()

  try:
    # Kill will not actually kill the process
    # From the doc:
    # If sig is 0, then no signal is sent, but error checking is still
    # performed; this can be used to check for the existence of a
    # process ID or process group ID.
    sudo.kill(pid, 0)
  except OSError:
    Logger.info("Process with pid {0} is not running. Stale pid file"
              " at {1}".format(pid, pid_file))
    raise ComponentIsNotRunning()


def wait_process_stopped(pid_file):
  """
    Waits until component is actually stopped (check is performed using
    check_process_status() method.
    """
  import time
  component_is_stopped = False
  counter = 0
  while not component_is_stopped:
    try:
      if counter % 10 == 0:
        Logger.logger.info("Waiting for actual component stop")
      check_process_status(pid_file)
      time.sleep(1)
      counter += 1
    except ComponentIsNotRunning, e:
      Logger.logger.debug(" reports ComponentIsNotRunning")
      component_is_stopped = True
