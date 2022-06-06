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

import re

from resource_management.core.logger import Logger


class GenericManagerProperties(object):
  """
  Class to keep all Package-manager depended properties. Each non-generic implementation should override properties
  declared here
  """
  empty_file = "/dev/null"
  locked_output = None
  repo_error = None

  repo_manager_bin = None
  pkg_manager_bin = None
  repo_update_cmd = None

  available_packages_cmd = None
  installed_packages_cmd = None
  all_packages_cmd = None

  repo_definition_location = None

  install_cmd = {
    True: None,
    False: None
  }

  remove_cmd = {
    True: None,
    False: None
  }

  verify_dependency_cmd = None


class GenericManager(object):
  """
  Interface for all custom implementations.  Provides the required base for any custom manager, to be smoothly integrated
  """

  @property
  def properties(self):
    return GenericManagerProperties

  def install_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext

    :raise ValueError if name is empty
    """
    raise NotImplementedError()

  def remove_package(self, name, context, ignore_dependencies=False):
    """
    Remove package

    :type name str
    :type context ambari_commons.shell.RepoCallContext
    :type ignore_dependencies bool

    :raise ValueError if name is empty
    """
    raise NotImplementedError()

  def upgrade_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext

    :raise ValueError if name is empty
    """
    raise NotImplementedError()

  def check_uncompleted_transactions(self):
    """
    Check package manager against uncompleted transactions.

    :rtype bool
    """
    return False

  def print_uncompleted_transaction_hint(self):
    """
    Print friendly message about they way to fix the issue

    """
    pass

  def get_available_packages_in_repos(self, repositories):
    """
    Gets all (both installed and available) packages that are available at given repositories.
    :type repositories resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """
    raise NotImplementedError()

  def installed_packages(self, pkg_names=None, repo_filter=None):
    raise NotImplementedError()

  def available_packages(self, pkg_names=None, repo_filter=None):
    raise NotImplementedError()

  def all_packages(self, pkg_names=None, repo_filter=None):
    raise NotImplementedError()

  def get_installed_repos(self, hint_packages, all_packages, ignore_repos):
    """
    Gets all installed repos by name based on repos that provide any package
    contained in hintPackages
    Repos starting with value in ignoreRepos will not be returned
    hintPackages must be regexps.
    """
    all_repos = []
    repo_list = []

    for hintPackage in hint_packages:
      for item in all_packages:
        if re.match(hintPackage, item[0]) and not item[2] in all_repos:
          all_repos.append(item[2])

    for repo in all_repos:
      ignore = False
      for ignoredRepo in ignore_repos:
        if self.name_match(ignoredRepo, repo):
          ignore = True
      if not ignore:
        repo_list.append(repo)

    return repo_list

  def get_installed_pkgs_by_repo(self, repos, ignore_packages, installed_packages):
    """
    Get all the installed packages from the repos listed in repos
    """
    packages_from_repo = []
    packages_to_remove = []
    for repo in repos:
      sub_result = []
      for item in installed_packages:
        if repo == item[2]:
          sub_result.append(item[0])
      packages_from_repo = list(set(packages_from_repo + sub_result))

    for package in packages_from_repo:
      keep_package = True
      for ignorePackage in ignore_packages:
        if self.name_match(ignorePackage, package):
          keep_package = False
          break
      if keep_package:
        packages_to_remove.append(package)
    return packages_to_remove

  def get_installed_pkgs_by_names(self, pkg_names, all_packages_list=None):
    """
    Gets all installed packages that start with names in pkgNames
    :type pkg_names list[str]
    :type all_packages_list list[str]
    """
    return self.installed_packages(pkg_names)

  def get_package_details(self, installed_packages, found_packages):
    """
    Gets the name, version, and repoName for the packages
    :type installed_packages list[tuple[str,str,str]]
    :type found_packages list[str]
    """
    package_details = []

    for package in found_packages:
      pkg_detail = {}
      for installed_package in installed_packages:
        if package == installed_package[0]:
          pkg_detail['name'] = installed_package[0]
          pkg_detail['version'] = installed_package[1]
          pkg_detail['repoName'] = installed_package[2]

      package_details.append(pkg_detail)

    return package_details

  def get_repos_to_remove(self, repos, ignore_list):
    repos_to_remove = []
    for repo in repos:
      add_to_remove_list = True
      for ignore_repo in ignore_list:
        if self.name_match(ignore_repo, repo):
          add_to_remove_list = False
          continue
      if add_to_remove_list:
        repos_to_remove.append(repo)
    return repos_to_remove

  def get_installed_package_version(self, package_name):
    raise NotImplementedError()

  def verify_dependencies(self):
    """
    Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
    package installation process or invalid packages state after manual modification of packages list on the host

    :return True if no dependency issues found, False if dependency issue present
    :rtype bool
    """
    raise NotImplementedError()

  def name_match(self, lookup_name, actual_name):
    tokens = actual_name.strip().lower()
    lookup_name = lookup_name.lower()

    return " " not in lookup_name and lookup_name in tokens

  def _executor_error_handler(self, command, error_log, exit_code):
    """
    Error handler for ac_shell.process_executor

    :type command list|str
    :type error_log list
    :type exit_code int
    """
    if isinstance(command, (list, tuple)):
      command = " ".join(command)

    Logger.error("Command execution error: command = \"{0}\", exit code = {1}, stderr = {2}".format(
      command, exit_code, "\n".join(error_log)))
