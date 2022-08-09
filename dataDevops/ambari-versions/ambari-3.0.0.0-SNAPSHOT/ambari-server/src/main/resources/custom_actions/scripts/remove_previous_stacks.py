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
import re

import os
from resource_management import Script, format, Package, Execute, Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions import stack_tools
from resource_management.libraries.functions.stack_select import get_stack_versions
from ambari_commons.repo_manager import ManagerFactory

CURRENT_ = "/current/"
stack_root = Script.get_stack_root()
stack_root_current = stack_root + CURRENT_


class RemovePreviousStacks(Script):


  def actionexecute(self, env):
    config = Script.get_config()
    structured_output = {}
    version = config['commandParams']['version']
    self.stack_tool_package = stack_tools.get_stack_tool_package(stack_tools.STACK_SELECTOR_NAME)

    versions_to_remove = self.get_lower_versions(version)
    self.pkg_provider = ManagerFactory.get()

    for low_version in versions_to_remove:
      self.remove_stack_version(structured_output, low_version)

  def remove_stack_version(self, structured_output, version):
    # check simlinks not refer to version for remove
    self.check_no_symlink_to_version(structured_output, version)
    packages_to_remove = self.get_packages_to_remove(version)
    for package in packages_to_remove:
      Package(package, action="remove")
    self.remove_stack_folder(structured_output, version)
    structured_output["remove_previous_stacks"] = {"exit_code": 0,
                                       "message": format("Stack version {0} successfully removed!".format(version))}
    self.put_structured_out(structured_output)

  def remove_stack_folder(self, structured_output, version):
    if version and version != '' and stack_root and stack_root != '':

      Logger.info("Removing {0}/{1}".format(stack_root, version))
      try:
        Execute(('rm', '-f', stack_root + version),
                sudo=True)
      finally:
        structured_output["remove_previous_stacks"] = {"exit_code": -1,
                                           "message": "Failed to remove version {0}{1}".format(stack_root, version)}
        self.put_structured_out(structured_output)

  def get_packages_to_remove(self, version):
    packages = []
    formated_version = version.replace('.', '_').replace('-', '_')
    all_installed_packages = self.pkg_provider.all_installed_packages()

    all_installed_packages = [package[0] for package in all_installed_packages]
    for package in all_installed_packages:
      if formated_version in package and self.stack_tool_package not in package:
        packages.append(package)
        Logger.info("%s added to remove" % (package))
    return packages

  def check_no_symlink_to_version(self, structured_output, version):
    files = os.listdir(stack_root_current)
    for file in files:
      if version in os.path.realpath(stack_root_current + file):
        structured_output["remove_previous_stacks"] = {"exit_code": -1,
                                           "message": "{0} contains symlink to version for remove! {1}".format(
                                             stack_root_current, version)}
        self.put_structured_out(structured_output)
        raise Fail("{0} contains symlink to version for remove! {1}".format(stack_root_current, version))

  def get_lower_versions(self, current_version):
    versions = get_stack_versions(stack_root)
    Logger.info("available versions: {0}".format(str(versions)))

    lover_versions = []
    for version in versions:
      if self.compare(version, current_version) < 0 :
        lover_versions.append(version)
        Logger.info("version %s added to remove" % (version))
    return lover_versions

  def compare(self, version1, version2):
    """
    Compare version1 and version2
    :param version1:
    :param version2:
    :return: Return negative if version1<version2, zero if version1==version2, positive if version1>version2
    """
    vesion1_sections = re.findall(r"[\w']+", version1)
    vesion2_sections = re.findall(r"[\w']+", version2)
    return cmp(vesion1_sections, vesion2_sections)

if __name__ == "__main__":
  RemovePreviousStacks().execute()
