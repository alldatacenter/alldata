#!/usr/bin/env ambari-python-wrap

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

__all__ = ["Hook"]

from resource_management.libraries.script import Script
from ambari_commons import subprocess32
import sys


class Hook(Script):
  """
  Executes a hook for a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  """

  HOOK_METHOD_NAME = "hook"  # This method is always executed at hooks

  def choose_method_to_execute(self, command_name):
    """
    Changes logics of resolving method name
    """
    return super(Hook, self).choose_method_to_execute(self.HOOK_METHOD_NAME)

  def run_custom_hook(self, command):
    """
    Runs custom hook
    """
    args = sys.argv
    
    # Hook script to run
    args[0] = args[0].replace('before-'+args[1], command)
    args[0] = args[0].replace('after-'+args[1], command)
    
    # Hook script base directory
    args[3] = args[3].replace('before-'+args[1], command)
    args[3] = args[3].replace('after-'+args[1], command)
    
    args[1] = command.split("-")[1]

    cmd = [sys.executable]
    cmd.extend(args)

    if subprocess32.call(cmd) != 0:
      self.fail_with_error("Error: Unable to run the custom hook script " + cmd.__str__())

