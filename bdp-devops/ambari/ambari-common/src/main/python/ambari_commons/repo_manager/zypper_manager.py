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
from ambari_commons import shell
from .generic_manager import GenericManagerProperties, GenericManager
from .zypper_parser import ZypperParser
from resource_management.core.logger import Logger

import re


class ZypperManagerProperties(GenericManagerProperties):
  """
  Class to keep all Package-manager depended properties
  """
  locked_output = "System management is locked by the application"
  repo_error = "Failure when receiving data from the peer"

  repo_manager_bin = "/usr/bin/zypper"
  pkg_manager_bin = "/bin/rpm"
  repo_update_cmd = [repo_manager_bin, "clean"]

  available_packages_cmd = [repo_manager_bin, "--no-gpg-checks", "search", "--uninstalled-only", "--details"]
  installed_packages_cmd = [repo_manager_bin, "--no-gpg-checks", "search", "--installed-only", "--details"]
  all_packages_cmd = [repo_manager_bin, "--no-gpg-checks", "search", "--details"]

  repo_definition_location = "/etc/zypp/repos.d"

  install_cmd = {
    True: [repo_manager_bin, "install", "--auto-agree-with-licenses", "--no-confirm"],
    False: [repo_manager_bin, "--quiet", "install", "--auto-agree-with-licenses", "--no-confirm"]
  }

  remove_cmd = {
    True: [repo_manager_bin, "remove", "--no-confirm"],
    False: [repo_manager_bin, "--quiet", "remove", "--no-confirm"]
  }

  verify_dependency_cmd = [repo_manager_bin, "--quiet", "--non-interactive", "verify", "--dry-run"]
  list_active_repos_cmd = ['/usr/bin/zypper', 'repos']
  installed_package_version_command = [pkg_manager_bin, "-q", "--queryformat", "%{version}-%{release}\n"]


class ZypperManager(GenericManager):

  @property
  def properties(self):
    return ZypperManagerProperties

  def get_available_packages_in_repos(self, repos):
    """
    Gets all (both installed and available) packages that are available at given repositories.
    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """

    available_packages = []
    repo_ids = [repository.repo_id for repository in repos.items]

    # zypper cant tell from which repository were installed package, as repo r matching by pkg_name
    # as result repository would be matched if it contains package with same meta info

    if repos.feat.scoped:
      Logger.info("Looking for matching packages in the following repositories: {0}".format(", ".join(repo_ids)))
      for repo in repo_ids:
        available_packages.extend(self.all_packages(repo_filter=repo))
    else:
      Logger.info("Packages will be queried using all available repositories on the system.")
      available_packages.extend(self.all_packages())

    return [package[0] for package in available_packages]

  def installed_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of the installed packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    :rtype list[list,]
    """
    packages = []
    cmd = list(self.properties.installed_packages_cmd)

    if repo_filter:
      cmd.extend(["--repo=" + repo_filter])

    with shell.process_executor(cmd, error_callback=self._executor_error_handler) as output:
      for pkg in ZypperParser.packages_reader(output):
        if pkg_names and not pkg[0] in pkg_names:
          continue

        packages.append(pkg)

    return packages

  def available_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of the available packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    :rtype list[list,]
    """
    packages = []
    cmd = list(self.properties.available_packages_cmd)

    if repo_filter:
      cmd.extend(["--repo=" + repo_filter])

    with shell.process_executor(cmd, error_callback=self._executor_error_handler) as output:
      for pkg in ZypperParser.packages_reader(output):
        if pkg_names and not pkg[0] in pkg_names:
          continue

        packages.append(pkg)

    return packages

  def all_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of the all packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    :rtype list[list,]
    """
    packages = []
    cmd = list(self.properties.all_packages_cmd)

    if repo_filter:
      cmd.extend(["--repo=" + repo_filter])

    with shell.process_executor(cmd, error_callback=self._executor_error_handler) as output:
      for pkg in ZypperParser.packages_reader(output):
        if pkg_names and not pkg[0] in pkg_names:
          continue

        packages.append(pkg)

    return packages

  def verify_dependencies(self):
    """
    Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
    package installation process or invalid packages state after manual modification of packages list on the host

    :return True if no dependency issues found, False if dependency issue present
    :rtype bool
    """
    r = shell.subprocess_executor(self.properties.verify_dependency_cmd)
    pattern = re.compile("\d+ new package(s)? to install")

    if r.code or (r.out and pattern.search(r.out)):
      err_msg = Logger.filter_text("Failed to verify package dependencies. Execution of '{0}' returned {1}. {2}".format(
        self.properties.verify_dependency_cmd, r.code, r.out))
      Logger.error(err_msg)
      return False

    return True

  def install_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext

    :raise ValueError if name is empty
    """
    if not name:
      raise ValueError("Installation command was executed with no package name")
    elif not self._check_existence(name) or context.action_force:
      cmd = self.properties.install_cmd[context.log_output]

      if context.use_repos:
        active_base_repos = self.get_active_base_repos()
        if 'base' in context.use_repos:
          # Remove 'base' from use_repos list
          use_repos = filter(lambda x: x != 'base', context.use_repos)
          use_repos.extend(active_base_repos)
        use_repos_options = []
        for repo in sorted(context.use_repos):
          use_repos_options = use_repos_options + ['--repo', repo]
        cmd = cmd + use_repos_options

      cmd = cmd + [name]
      Logger.info("Installing package {0} ('{1}')".format(name, shell.string_cmd_from_args_list(cmd)))

      shell.repository_manager_executor(cmd, self.properties, context)
    else:
      Logger.info("Skipping installation of existing package {0}".format(name))

  def upgrade_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext

    :raise ValueError if name is empty
    """
    context.is_upgrade = True
    return self.install_package(name, context)

  def remove_package(self, name, context, ignore_dependencies=False):
    """
    Remove package

    :type name str
    :type context ambari_commons.shell.RepoCallContext
    :type ignore_dependencies bool

    :raise ValueError if name is empty
    """
    if not name:
      raise ValueError("Installation command were executed with no package name passed")
    elif self._check_existence(name):
      cmd = self.properties.remove_cmd[context.log_output] + [name]
      Logger.info("Removing package {0} ('{1}')".format(name, shell.string_cmd_from_args_list(cmd)))
      shell.repository_manager_executor(cmd, self.properties, context)
    else:
      Logger.info("Skipping removal of non-existing package {0}".format(name))

  def get_active_base_repos(self):
    enabled_repos = []

    with shell.process_executor(self.properties.list_active_repos_cmd, error_callback=self._executor_error_handler) as output:
      for _, repo_name, repo_enabled, _ in ZypperParser.repo_list_reader(output):
        if repo_enabled and repo_name.startswith("SUSE-"):
          enabled_repos.append(repo_name)

        if repo_enabled and ("OSS" in repo_name) or ("OpenSuse" in repo_name):
          enabled_repos.append(repo_name)

    return enabled_repos

  def rpm_check_package_available(self, name):
    import rpm # this is faster then calling 'rpm'-binary externally.
    ts = rpm.TransactionSet()
    packages = ts.dbMatch()

    name_regex = re.escape(name).replace("\\?", ".").replace("\\*", ".*") + '$'
    regex = re.compile(name_regex)

    for package in packages:
      if regex.match(package['name']):
        return True
    return False

  def get_installed_package_version(self, package_name):
    version = None
    cmd = list(self.properties.installed_package_version_command) + [package_name]
    result = shell.subprocess_executor(cmd)
    try:
      if result.code == 0:
        version = result.out.strip().partition(".el")[0]
    except IndexError:
      pass

    return version

  def _check_existence(self, name):
    """
    For regexp names:
    If only part of packages were installed during early canceling.
    Let's say:
    1. install hbase_2_3_*
    2. Only hbase_2_3_1234 is installed, but is not hbase_2_3_1234_regionserver yet.
    3. We cancel the zypper

    In that case this is bug of packages we require.
    And hbase_2_3_*_regionserver should be added to metainfo.xml.

    Checking existence should never fail in such a case for hbase_2_3_*, otherwise it
    gonna break things like removing packages and some other things.

    Note: this method SHOULD NOT use zypper. Because a lot of issues we have, when customer have
    zypper in inconsistant state (locked, used, having invalid repo). Once packages are installed
    we should not rely on that.
    """
    if not name:
      raise ValueError("Package name can't be empty")

    return self.rpm_check_package_available(name)
