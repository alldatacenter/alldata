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

from ambari_commons.repo_manager.generic_manager import GenericManagerProperties, GenericManager
from ambari_commons.shell import shellRunner

from resource_management.core.logger import Logger
#from ambari_commons.shell import shellRunner


INSTALL_CMD = {
  True: ['cmd', '/c', 'choco', 'install', '--pre', '-y', '-v'],
  False: ['cmd', '/c', 'choco', 'install', '--pre', '-y'],
}

UPGRADE_CMD = {
  True: ['cmd', '/c', 'choco', 'upgrade', '--pre', '-y', '-f', '-v'],
  False: ['cmd', '/c', 'choco', 'upgrade', '--pre', '-y', '-f'],
}

REMOVE_CMD = {
  True: ['cmd', '/c', 'choco', 'uninstall', '-y', '-v'],
  False: ['cmd', '/c', 'choco', 'uninstall', '-y'],
}

CHECK_CMD = {
  True: ['cmd', '/c', 'choco', 'list', '--pre', '--local-only', '-v'],
  False: ['cmd', '/c', 'choco', 'list', '--pre', '--local-only'],
}


class ChocoManagerProperties(GenericManagerProperties):
  pass


class ChocoManager(GenericManager):
  def install_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext
    """
    if not self._check_existence(name) or context.use_repos:
      cmd = INSTALL_CMD[context.log_output]
      if context.use_repos:
        enable_repo_option = '-s' + ",".join(sorted(context.use_repos.keys()))
        cmd = cmd + [enable_repo_option]
      cmd = cmd + [name]
      cmdString = " ".join(cmd)
      Logger.info("Installing package %s ('%s')" % (name, cmdString))
      runner = shellRunner()
      res = runner.run(cmd)
      if res['exitCode'] != 0:
        raise Exception("Error while installing choco package " + name + ". " + res['error'] + res['output'])
    else:
      Logger.info("Skipping installation of existing package %s" % (name))

  def upgrade_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext
    """
    cmd = UPGRADE_CMD[context.log_output]
    if context.use_repos:
      enable_repo_option = '-s' + ",".join(sorted(context.use_repos.keys()))
      cmd = cmd + [enable_repo_option]
    cmd = cmd + [name]
    cmdString = " ".join(cmd)
    Logger.info("Upgrading package %s ('%s')" % (name, cmdString))
    runner = shellRunner()
    res = runner.run(cmd)
    if res['exitCode'] != 0:
      raise Exception("Error while upgrading choco package " + name + ". " + res['error'] + res['output'])

  def remove_package(self, name, context, ignore_dependencies=False):
    """
    Remove package

    :type name str
    :type context ambari_commons.shell.RepoCallContext
    :type ignore_dependencies bool
    """
    if self._check_existence(name, context):
      cmd = REMOVE_CMD[context.log_output] + [name]
      cmdString = " ".join(cmd)
      Logger.info("Removing package %s ('%s')" % (name, " ".join(cmd)))
      runner = shellRunner()
      res = runner.run(cmd)
      if res['exitCode'] != 0:
        raise Exception("Error while upgrading choco package " + name + ". " + res['error'] + res['output'])
    else:
      Logger.info("Skipping removal of non-existing package %s" % (name))

  def _check_existence(self, name, context):
    cmd = CHECK_CMD[context.log_output] + [name]
    runner = shellRunner()
    res = runner.run(cmd)
    if name in res['output']:
      return True
    return False
