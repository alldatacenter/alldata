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
import signal
import os
import re

import ambari_simplejson as json

from ambari_commons.os_check import OSCheck
from ambari_commons.shell import RepoCallContext
from ambari_commons.str_utils import cbool, cint
from ambari_commons.repo_manager import ManagerFactory
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_tools
from resource_management.libraries.functions.stack_select import get_stack_versions
from resource_management.libraries.functions.repo_version_history \
    import read_actual_version_from_history_file, write_actual_version_to_history_file, REPO_VERSION_HISTORY_FILE
from resource_management.core.resources.system import Link
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.repository_util import CommandRepository
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.script.script import Script
from resource_management.core import sudo


class InstallPackages(Script):
  """
  This script is a part of Rolling Upgrade workflow and is described at
  appropriate design doc.
  It installs repositories to the node and then installs packages.
  For now, repositories are installed into individual files.
  """

  UBUNTU_REPO_COMPONENTS_POSTFIX = ["main"]

  def __init__(self):
    super(InstallPackages, self).__init__()

    self.repo_mgr = ManagerFactory.get()
    self.repo_files = {}

  def actionexecute(self, env):
    num_errors = 0

    # Parse parameters
    config = Script.get_config()

    try:
      command_repository = CommandRepository(config['repositoryFile'])
    except KeyError:
      raise Fail("The command repository indicated by 'repositoryFile' was not found")

    # Handle a SIGTERM and SIGINT gracefully
    signal.signal(signal.SIGTERM, self.abort_handler)
    signal.signal(signal.SIGINT, self.abort_handler)

    self.repository_version = command_repository.version_string

    # Select dict that contains parameters
    try:
      package_list = json.loads(config['roleParams']['package_list'])
      stack_id = config['roleParams']['stack_id']
    except KeyError:
      pass

    self.stack_name = Script.get_stack_name()
    if self.stack_name is None:
      raise Fail("Cannot determine the stack name")

    self.stack_root_folder = Script.get_stack_root()
    if self.stack_root_folder is None:
      raise Fail("Cannot determine the stack's root directory")

    if self.repository_version is None:
      raise Fail("Cannot determine the repository version to install")

    self.repository_version = self.repository_version.strip()

    try:
      if not command_repository.items:
        Logger.warning(
          "Repository list is empty. Ambari may not be managing the repositories for {0}.".format(
            self.repository_version))
      else:
        Logger.info(
          "Will install packages for repository version {0}".format(self.repository_version))
        new_repo_files = Script.repository_util.create_repo_files()
        self.repo_files.update(new_repo_files)
    except Exception as err:
      Logger.logger.exception("Cannot install repository files. Error: {0}".format(str(err)))
      num_errors += 1

    # Build structured output with initial values
    self.structured_output = {
      'package_installation_result': 'FAIL',
      'repository_version_id': command_repository.version_id
    }

    self.put_structured_out(self.structured_output)

    try:
      # check package manager non-completed transactions
      if self.repo_mgr.check_uncompleted_transactions():
        self.repo_mgr.print_uncompleted_transaction_hint()
        num_errors += 1
    except Exception as e:  # we need to ignore any exception
      Logger.warning("Failed to check for uncompleted package manager transactions: " + str(e))

    if num_errors > 0:
      raise Fail("Failed to distribute repositories/install packages")

    # Initial list of versions, used to compute the new version installed
    self.old_versions = get_stack_versions(self.stack_root_folder)

    try:
      is_package_install_successful = False
      ret_code = self.install_packages(package_list)
      if ret_code == 0:
        self.structured_output['package_installation_result'] = 'SUCCESS'
        self.put_structured_out(self.structured_output)
        is_package_install_successful = True
      else:
        num_errors += 1
    except Exception as err:
      num_errors += 1
      Logger.logger.exception("Could not install packages. Error: {0}".format(str(err)))

    # Provide correct exit code
    if num_errors > 0:
      raise Fail("Failed to distribute repositories/install packages")

    self._fix_default_links_for_current()
    # if installing a version of HDP that needs some symlink love, then create them
    if is_package_install_successful and 'actual_version' in self.structured_output:
      self._relink_configurations_with_conf_select(stack_id, self.structured_output['actual_version'])

  def _fix_default_links_for_current(self):
    """
    If a prior version of Ambari did not correctly reverse the conf symlinks, then they would
    be put into a bad state when distributing a new stack. For example:

    /etc/component/conf (directory)
    <stack-root>/v1/component/conf -> /etc/component/conf

    When distributing v2, we'd detect the /etc/component/conf problems and would try to adjust it:
    /etc/component/conf -> <stack-root>/current/component/conf
    <stack-root>/v2/component/conf -> /etc/component/v2/0

    The problem is that v1 never gets changed (since the stack being distributed is v2), and
    we end up with a circular link:
    /etc/component/conf -> <stack-root>/current/component/conf
    <stack-root>/v1/component/conf -> /etc/component/conf

    :return: None
    """
    Logger.info("Attempting to fix any configuration symlinks which are not in the correct state")
    from resource_management.libraries.functions import stack_select
    restricted_packages = conf_select.get_restricted_packages()

    if 0 == len(restricted_packages):
      Logger.info("There are no restricted conf-select packages for this installation")
    else:
      Logger.info("Restricting conf-select packages to {0}".format(restricted_packages))

    for package_name, directories in conf_select.get_package_dirs().iteritems():
      Logger.info("Attempting to fix the default conf links for {0}".format(package_name))
      Logger.info("The following directories will be fixed for {0}: {1}".format(package_name, str(directories)))

      component_name = None
      for directory_struct in directories:
        if "component" in directory_struct:
          component_name = directory_struct["component"]

      if component_name:
        stack_version = stack_select.get_stack_version_before_install(component_name)
      else:
        Logger.warning("Unable to fix {0} since stack using outdated stack_packages.json".format(package_name))
        return

      if 0 == len(restricted_packages) or package_name in restricted_packages:
        if stack_version:
          conf_select.convert_conf_directories_to_symlinks(package_name, stack_version, directories)
        else:
          Logger.warning(
            "Unable to fix {0} since there is no known installed version for this component".format(package_name))

  def _relink_configurations_with_conf_select(self, stack_id, stack_version):
    """
    Sets up the required structure for /etc/<component>/conf symlinks and <stack-root>/current
    configuration symlinks IFF the current stack is < HDP 2.3+ and the new stack is >= HDP 2.3

    stack_id:  stack id, ie HDP-2.3
    stack_version:  version to set, ie 2.3.0.0-1234
    """
    if stack_id is None:
      Logger.info("Cannot create config links when stack_id is not defined")
      return

    args = stack_id.upper().split('-')
    if len(args) != 2:
      Logger.info("Unrecognized stack id {0}, cannot create config links".format(stack_id))
      return

    target_stack_version = args[1]
    if not (target_stack_version and check_stack_feature(StackFeature.CONFIG_VERSIONING, target_stack_version)):
      Logger.info("Configuration symlinks are not needed for {0}".format(stack_version))
      return

    # After upgrading hdf-select package from HDF-2.X to HDF-3.Y, we need to create this symlink
    if self.stack_name.upper() == "HDF" \
            and not sudo.path_exists("/usr/bin/conf-select") and sudo.path_exists("/usr/bin/hdfconf-select"):
      Link("/usr/bin/conf-select", to="/usr/bin/hdfconf-select")

    restricted_packages = conf_select.get_restricted_packages()

    if 0 == len(restricted_packages):
      Logger.info("There are no restricted conf-select packages for this installation")
    else:
      Logger.info("Restricting conf-select packages to {0}".format(restricted_packages))

    for package_name, directories in conf_select.get_package_dirs().iteritems():
      if 0 == len(restricted_packages) or package_name in restricted_packages:
        conf_select.convert_conf_directories_to_symlinks(package_name, stack_version, directories)


  def compute_actual_version(self):
    """
    After packages are installed, determine what the new actual version is.
    """

    # If the repo contains a build number, optimistically assume it to be the actual_version. It will get changed
    # to correct value if it is not
    self.actual_version = None
    self.repo_version_with_build_number = None
    if self.repository_version:
      m = re.search("[\d\.]+-\d+", self.repository_version)
      if m:
        # Contains a build number
        self.repo_version_with_build_number = self.repository_version
        self.structured_output['actual_version'] = self.repo_version_with_build_number  # This is the best value known so far.
        self.put_structured_out(self.structured_output)

    Logger.info("Attempting to determine actual version with build number.")
    Logger.info("Old versions: {0}".format(self.old_versions))

    new_versions = get_stack_versions(self.stack_root_folder)
    Logger.info("New versions: {0}".format(new_versions))

    deltas = set(new_versions) - set(self.old_versions)
    Logger.info("Deltas: {0}".format(deltas))

    # Get version without build number
    normalized_repo_version = self.repository_version.split('-')[0]

    if 1 == len(deltas):
      self.actual_version = next(iter(deltas)).strip()
      self.structured_output['actual_version'] = self.actual_version
      self.put_structured_out(self.structured_output)
      write_actual_version_to_history_file(normalized_repo_version, self.actual_version)
      Logger.info(
        "Found actual version {0} by checking the delta between versions before and after installing packages".format(
          self.actual_version))
    else:
      # If the first install attempt does a partial install and is unable to report this to the server,
      # then a subsequent attempt will report an empty delta. For this reason, we search for a best fit version for the repo version
      Logger.info("Cannot determine actual version installed by checking the delta between versions "
                  "before and after installing package")
      Logger.info("Will try to find for the actual version by searching for best possible match in the list of versions installed")
      self.actual_version = self.find_best_fit_version(new_versions, self.repository_version)
      if self.actual_version is not None:
        self.actual_version = self.actual_version.strip()
        self.structured_output['actual_version'] = self.actual_version
        self.put_structured_out(self.structured_output)
        Logger.info("Found actual version {0} by searching for best possible match".format(self.actual_version))
      else:
        msg = "Could not determine actual version installed. Try reinstalling packages again."
        raise Fail(msg)

  def check_partial_install(self):
    """
    If an installation did not complete successfully, check if installation was partially complete and
    log the partially completed version to REPO_VERSION_HISTORY_FILE.
    :return:
    """
    Logger.info("Installation of packages failed. Checking if installation was partially complete")
    Logger.info("Old versions: {0}".format(self.old_versions))

    new_versions = get_stack_versions(self.stack_root_folder)
    Logger.info("New versions: {0}".format(new_versions))

    deltas = set(new_versions) - set(self.old_versions)
    Logger.info("Deltas: {0}".format(deltas))

    # Get version without build number
    normalized_repo_version = self.repository_version.split('-')[0]

    if 1 == len(deltas):
      # Some packages were installed successfully. Log this version to REPO_VERSION_HISTORY_FILE
      partial_install_version = next(iter(deltas)).strip()
      write_actual_version_to_history_file(normalized_repo_version, partial_install_version)
      Logger.info("Version {0} was partially installed. ".format(partial_install_version))

  def find_best_fit_version(self, versions, repo_version):
    """
    Given a list of installed versions and a repo version, search for a version that best fits the repo version
    If the repo version is found in the list of installed versions, return the repo version itself.
    If the repo version is not found in the list of installed versions
    normalize the repo version and use the REPO_VERSION_HISTORY_FILE file to search the list.

    :param versions: List of versions installed
    :param repo_version: Repo version to search
    :return: Matching version, None if no match was found.
    """
    if versions is None or repo_version is None:
      return None

    build_num_match = re.search("[\d\.]+-\d+", repo_version)
    if build_num_match and repo_version in versions:
      # If repo version has build number and is found in the list of versions, return it as the matching version
      Logger.info("Best Fit Version: Resolved from repo version with valid build number: {0}".format(repo_version))
      return repo_version

    # Get version without build number
    normalized_repo_version = repo_version.split('-')[0]

    # Find all versions that match the normalized repo version
    match_versions = filter(lambda x: x.startswith(normalized_repo_version), versions)
    if match_versions:

      if len(match_versions) == 1:
        # Resolved without conflicts
        Logger.info("Best Fit Version: Resolved from normalized repo version without conflicts: {0}".format(match_versions[0]))
        return match_versions[0]

      # Resolve conflicts using REPO_VERSION_HISTORY_FILE
      history_version = read_actual_version_from_history_file(normalized_repo_version)

      # Validate history version retrieved is valid
      if history_version in match_versions:
        Logger.info("Best Fit Version: Resolved from normalized repo version using {0}: {1}".format(REPO_VERSION_HISTORY_FILE, history_version))
        return history_version

    # No matching version
    return None


  def install_packages(self, package_list):
    """
    Actually install the packages using the package manager.
    :param package_list: List of package names to install
    :return: Returns 0 if no errors were found, and 1 otherwise.
    """
    ret_code = 0
    
    config = self.get_config()
    agent_stack_retry_on_unavailability = cbool(config['ambariLevelParams']['agent_stack_retry_on_unavailability'])
    agent_stack_retry_count = cint(config['ambariLevelParams']['agent_stack_retry_count'])

    # Install packages
    packages_were_checked = False
    packages_installed_before = []
    stack_selector_package = stack_tools.get_stack_tool_package(stack_tools.STACK_SELECTOR_NAME)

    try:
      # install the stack-selector; we need to supply the action as "upgrade" here since the normal
      # install command will skip if the package is already installed in the system.
      # This is required for non-versioned components, like stack-select, since each version of
      # the stack comes with one. Also, scope the install by repository since we need to pick a
      # specific repo that the stack-select tools are coming out of in case there are multiple
      # patches installed
      repositories = config['repositoryFile']['repositories']
      command_repos = CommandRepository(config['repositoryFile'])
      repository_ids = [repository['repoId'] for repository in repositories]
      repos_to_use = {}

      if not command_repos.items:
        raise ValueError("No repositories passed with the command")

      for repo_id in repository_ids:
        if repo_id in self.repo_files:
          repos_to_use[repo_id] = self.repo_files[repo_id]

      self.repo_mgr.upgrade_package(stack_selector_package, RepoCallContext(
        ignore_errors=False,
        use_repos=repos_to_use,
        retry_on_repo_unavailability=agent_stack_retry_on_unavailability,
        retry_count=agent_stack_retry_count))

      packages_installed_before = self.repo_mgr.installed_packages()
      packages_installed_before = [package[0] for package in packages_installed_before]
      packages_were_checked = True
      filtered_package_list = self.filter_package_list(package_list)
      try:
        available_packages_in_repos = self.repo_mgr.get_available_packages_in_repos(command_repos)
      except Exception:
        available_packages_in_repos = []

      installation_context = RepoCallContext(
        ignore_errors=False,
        retry_on_repo_unavailability=agent_stack_retry_on_unavailability,
        retry_count=agent_stack_retry_count
      )

      for package in filtered_package_list:
        name = self.get_package_from_available(package['name'], available_packages_in_repos)

        # This enables upgrading non-versioned packages, despite the fact they exist.
        # Needed by 'mahout' which is non-version but have to be updated
        self.repo_mgr.upgrade_package(name, installation_context)
    except Exception as err:
      ret_code = 1
      Logger.logger.error("Package Manager failed to install packages: {0}".format(str(err)))

      # Remove already installed packages in case of fail
      if packages_were_checked and packages_installed_before:
        packages_installed_after = self.repo_mgr.installed_packages()
        packages_installed_after = [package[0] for package in packages_installed_after]
        packages_installed_before = set(packages_installed_before)
        new_packages_installed = [package for package in packages_installed_after if package not in packages_installed_before]

        if OSCheck.is_ubuntu_family():
          package_version_string = self.repository_version.replace('.', '-')
        else:
          package_version_string = self.repository_version.replace('-', '_')
          package_version_string = package_version_string.replace('.', '_')

        for package in new_packages_installed:
          if package_version_string and (package_version_string in package):
            self.repo_mgr.remove_package(package, RepoCallContext())

    if not self.repo_mgr.verify_dependencies():
      ret_code = 1
      Logger.logger.error("Failure while verifying dependencies")
      Logger.logger.error("*******************************************************************************")
      Logger.logger.error("Manually verify and fix package dependencies and then re-run install_packages")
      Logger.logger.error("*******************************************************************************")

    # Compute the actual version in order to save it in structured out
    try:
      if ret_code == 0:
         self.compute_actual_version()
      else:
        self.check_partial_install()
    except Fail as err:
      ret_code = 1
      Logger.logger.exception("Failure while computing actual version. Error: {0}".format(str(err)))
    return ret_code

  def abort_handler(self, signum, frame):
    Logger.error("Caught signal {0}, will handle it gracefully. Compute the actual version if possible before exiting.".format(signum))
    self.check_partial_install()
    
  def filter_package_list(self, package_list):
    """
    Note: that we have skipUpgrade option in metainfo.xml to filter packages,
    as well as condition option to filter them conditionally,
    so use this method only if, for some reason the metainfo option cannot be used.
  
    :param package_list: original list
    :return: filtered package_list
    """
    filtered_package_list = []
    for package in package_list:
      if self.check_package_condition(package):
        filtered_package_list.append(package)
    return filtered_package_list


if __name__ == "__main__":
  InstallPackages().execute()
