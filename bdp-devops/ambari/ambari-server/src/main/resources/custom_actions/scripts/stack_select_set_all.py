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

import os
import socket
from ambari_commons.os_check import OSCheck
from resource_management.libraries.script import Script
from resource_management.libraries.functions import stack_tools
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.constants import Direction
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.libraries.functions.decorator import experimental

class UpgradeSetAll(Script):
  """
  This script is a part of stack upgrade workflow and is used to set the
  all of the component versions as a final step in the upgrade process
  """
  @experimental(feature="PATCH_UPGRADES", disable = False, comment = "The stack-select tool will only be invoked if this is a standard upgrade which cannot be downgraded.")
  def actionexecute(self, env):
    summary = upgrade_summary.get_upgrade_summary()
    if summary is None:
      Logger.warning("There is no upgrade in progress")
      return

    if summary.associated_version is None:
      Logger.warning("There is no version associated with the upgrade in progress")
      return

    if summary.orchestration != "STANDARD":
      Logger.warning("The 'stack-select set all' command can only be invoked during STANDARD upgrades")
      return

    if summary.direction.lower() != Direction.UPGRADE or summary.is_downgrade_allowed or summary.is_revert:
      Logger.warning("The 'stack-select set all' command can only be invoked during an UPGRADE which cannot be downgraded")
      return

    # other os?
    if OSCheck.is_redhat_family():
      cmd = ('/usr/bin/yum', 'clean', 'all')
      code, out = shell.call(cmd, sudo=True)

    stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)

    # this script runs on all hosts; if this host doesn't have stack components,
    # then don't invoke the stack tool
    # (no need to log that it's skipped - the function will do that)
    if is_host_skippable(stack_selector_path, summary.associated_version):
      return

    # invoke "set all"
    cmd = ('ambari-python-wrap', stack_selector_path, 'set', 'all', summary.associated_version)
    code, out = shell.call(cmd, sudo=True)
    if code != 0:
      raise Exception("Command '{0}' exit code is nonzero".format(cmd))


def is_host_skippable(stack_selector_path, associated_version):
  """
  Gets whether this host should not have the stack select tool called.
  :param stack_selector_path  the path to the stack selector tool.
  :param associated_version: the version to use with the stack selector tool.
  :return: True if this host should be skipped, False otherwise.
  """
  if not os.path.exists(stack_selector_path):
    Logger.info("{0} does not have any stack components installed and will not invoke {1}".format(
      socket.gethostname(), stack_selector_path))

    return True

  # invoke the tool, checking its output
  cmd = ('ambari-python-wrap', stack_selector_path, "versions")
  code, out = shell.call(cmd, sudo=True)

  if code != 0:
    Logger.info("{0} is unable to determine which stack versions are available using {1}".format(
        socket.gethostname(), stack_selector_path))

    return True

  # check to see if the output is empty, indicating no versions installed
  if not out.strip():
    Logger.info("{0} has no stack versions installed".format(socket.gethostname()))
    return True

  # some pre-prepped systems may have a version, so there may be a version, so
  # add the extra check if it is available
  if not associated_version in out:
    Logger.info("{0} is not found in the list of versions {1}".format(associated_version, out))
    return True

  return False


if __name__ == "__main__":
  UpgradeSetAll().execute()
