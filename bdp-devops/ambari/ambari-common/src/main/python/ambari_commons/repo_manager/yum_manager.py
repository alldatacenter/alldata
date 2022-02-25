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

import ConfigParser
import glob

from .generic_manager import GenericManagerProperties, GenericManager
from .yum_parser import YumParser
from ambari_commons import shell
from resource_management.core.logger import Logger
from resource_management.core.utils import suppress_stdout
from resource_management.core import sudo

from StringIO import StringIO

import re
import os


class YumManagerProperties(GenericManagerProperties):
  """
  Class to keep all Package-manager depended properties
  """
  locked_output = None
  repo_error = "Failure when receiving data from the peer", "Nothing to do"

  repo_manager_bin = "/usr/bin/yum"
  pkg_manager_bin = "/usr/bin/rpm"
  repo_update_cmd = [repo_manager_bin, "clean", "metadata"]

  available_packages_cmd = [repo_manager_bin, "list", "available", "--showduplicates"]
  installed_packages_cmd = [repo_manager_bin, "list", "installed", "--showduplicates"]
  all_packages_cmd = [repo_manager_bin, "list", "all", "--showduplicates"]

  yum_lib_dir = "/var/lib/yum"
  yum_tr_prefix = "transaction-"

  repo_definition_location = "/etc/yum.repos.d"

  install_cmd = {
    True: [repo_manager_bin, '-y', 'install'],
    False: [repo_manager_bin, '-d', '0', '-e', '0', '-y', 'install']
  }

  remove_cmd = {
    True: [repo_manager_bin, '-y', 'erase'],
    False: [repo_manager_bin, '-d', '0', '-e', '0', '-y', 'erase']
  }

  verify_dependency_cmd = [repo_manager_bin, '-d', '0', '-e', '0', 'check', 'dependencies']
  installed_package_version_command = [pkg_manager_bin, "-q", "--queryformat", "%{version}-%{release}\n"]

  remove_without_dependencies_cmd = ['rpm', '-e', '--nodeps']


class YumManager(GenericManager):

  @property
  def properties(self):
    return YumManagerProperties

  def get_available_packages_in_repos(self, repos):
    """
    Gets all (both installed and available) packages that are available at given repositories.

    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """
    available_packages = []
    installed_packages = []
    repo_ids = [repo.repo_id for repo in repos.items]

    if repos.feat.scoped:
      Logger.info("Looking for matching packages in the following repositories: {0}".format(", ".join(repo_ids)))
    else:
      Logger.info("Packages will be queried using all available repositories on the system.")

    for repo in repo_ids:
      repo = repo if repos.feat.scoped else None
      available_packages.extend(self.available_packages(repo_filter=repo))
      installed_packages.extend(self.installed_packages(repo_filter=repo))

    # fallback logic

    if repos.feat.scoped:
      fallback_repo_ids = set(repo_ids) ^ self._build_repos_ids(repos)  # no reason to scan the same repos again
      if fallback_repo_ids:
        Logger.info("Adding fallback repositories: {0}".format(", ".join(fallback_repo_ids)))

        for repo in fallback_repo_ids:
          available_packages.extend(self.available_packages(repo_filter=repo))
          installed_packages.extend(self.installed_packages(repo_filter=repo))

    return [package[0] for package in available_packages + installed_packages]

  def available_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of available packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    :rtype list[list,]
    """

    packages = []
    cmd = list(self.properties.available_packages_cmd)
    if repo_filter:
      cmd.extend(["--disablerepo=*", "--enablerepo=" + repo_filter])

    with shell.process_executor(cmd, error_callback=self._executor_error_handler) as output:
      for pkg in YumParser.packages_reader(output):
        if pkg_names and not pkg[0] in pkg_names:
          continue

        packages.append(pkg)

    return packages

  def installed_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of the installed packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    :rtype list[list,]
    """

    packages = []
    cmd = self.properties.installed_packages_cmd

    with shell.process_executor(cmd, error_callback=self._executor_error_handler) as output:
      for pkg in YumParser.packages_reader(output):
        if pkg_names and not pkg[0] in pkg_names:
          continue

        if repo_filter and repo_filter.lower() != pkg[2].lower():
          continue
        packages.append(pkg)

    return packages

  def all_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of the installed packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    :rtype list[list,]
    """

    packages = []
    cmd = self.properties.all_packages_cmd

    with shell.process_executor(cmd, error_callback=self._executor_error_handler) as output:
      for pkg in YumParser.packages_reader(output):
        if pkg_names and not pkg[0] in pkg_names:
          continue

        if repo_filter and repo_filter.lower() != pkg[2].lower():
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
    ret = shell.subprocess_executor(self.properties.verify_dependency_cmd)
    pattern = re.compile("has missing requires|Error:")

    if ret.code or (ret.out and pattern.search(ret.out)):
      err_msg = Logger.filter_text("Failed to verify package dependencies. Execution of '{0}' returned {1}. {2}".format(
        self.properties.verify_dependency_cmd, ret.code, ret.out))
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
        enable_repo_option = '--enablerepo=' + ",".join(sorted(context.use_repos.keys()))
        disable_repo_option = '--disablerepo=' + "*" if not context.skip_repos or len(context.skip_repos) == 0 else ','.join(context.skip_repos)
        cmd = cmd + [disable_repo_option, enable_repo_option]
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
      raise ValueError("Remove command were executed with no package name passed")
    if self._check_existence(name):
      if ignore_dependencies:
        cmd = self.properties.remove_without_dependencies_cmd + [name]
      else:
        cmd = self.properties.remove_cmd[context.log_output] + [name]
      Logger.info("Removing package {0} ('{1}')".format(name, shell.string_cmd_from_args_list(cmd)))
      shell.repository_manager_executor(cmd, self.properties, context)
    else:
      Logger.info("Skipping removal of non-existing package {0}".format(name))

  def _check_existence(self, name):
    """
    For regexp names:
    If only part of packages were installed during early canceling.
    Let's say:
    1. install hbase_2_3_*
    2. Only hbase_2_3_1234 is installed, but is not hbase_2_3_1234_regionserver yet.
    3. We cancel the yum

    In that case this is bug of packages we require.
    And hbase_2_3_*_regionserver should be added to metainfo.xml.

    Checking existence should never fail in such a case for hbase_2_3_*, otherwise it
    gonna break things like removing packages and some others.

    Note: this method SHOULD NOT use yum directly (yum.rpmdb doesn't use it). Because a lot of issues we have, when customer have
    yum in inconsistant state (locked, used, having invalid repo). Once packages are installed
    we should not rely on that.
    """
    if not name:
      raise ValueError("Package name can't be empty")

    if os.geteuid() == 0:
      return self.yum_check_package_available(name)
    else:
      return self.rpm_check_package_available(name)

  def yum_check_package_available(self, name):
    """
    Does the same as rpm_check_package_avaiable, but faster.
    However need root permissions.
    """
    import yum  # Python Yum API is much faster then other check methods. (even then "import rpm")
    yb = yum.YumBase()
    name_regex = re.escape(name).replace("\\?", ".").replace("\\*", ".*") + '$'
    regex = re.compile(name_regex)

    with suppress_stdout():
      package_list = yb.rpmdb.simplePkgList()

    for package in package_list:
      if regex.match(package[0]):
        return True

    return False

  @staticmethod
  def _build_repos_ids(repos):
    """
    Gets a set of repository identifiers based on the supplied repository JSON structure as
    well as any matching repos defined in /etc/yum.repos.d.
    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return:  the list of repo IDs from both the command and any matches found on the system
    with the same URLs.
    """

    repo_ids = []
    base_urls = []
    mirrors = []

    for repo in repos.items:
      repo_ids.append(repo.repo_id)

      if repo.base_url:
        base_urls.append(repo.base_url)

      if repo.mirrors_list:
        mirrors.append(repo.mirrors_list)

    # for every repo file, find any which match the base URLs we're trying to write out
    # if there are any matches, it means the repo already exists and we should use it to search
    # for packages to install
    for repo_file in glob.glob(os.path.join(YumManagerProperties.repo_definition_location, "*.repo")):
      config_parser = ConfigParser.ConfigParser()
      config_parser.read(repo_file)
      sections = config_parser.sections()
      for section in sections:
        if config_parser.has_option(section, "baseurl"):
          base_url = config_parser.get(section, "baseurl")
          if base_url in base_urls:
            repo_ids.append(section)

        if config_parser.has_option(section, "mirrorlist"):
          mirror = config_parser.get(section, "mirrorlist")
          if mirror in mirrors:
            repo_ids.append(section)

    return set(repo_ids)

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

  def __extract_transaction_id(self, filename):
    """
    :type filename str
    """
    return filename.split(".", 1)[1]

  def __transaction_file_parser(self, f):
    """
    :type f file|BinaryIO|StringIO
    :rtype collections.Iterable(str)
    """
    for line in f:
      yield line.split(":", 1)[1].strip()

  def uncomplete_transactions(self):
    """
    Transactions reader

    :rtype collections.Iterable(YumTransactionItem)
    """
    transactions = {}

    prefix_len = len(self.properties.yum_tr_prefix)
    for item in sudo.listdir(self.properties.yum_lib_dir):
      if self.properties.yum_tr_prefix == item[:prefix_len]:
        tr_id = self.__extract_transaction_id(item)

        f = StringIO(sudo.read_file(os.path.join(self.properties.yum_lib_dir, item)))
        pkgs_in_transaction = list(self.__transaction_file_parser(f))

        if tr_id not in transactions:
          transactions[tr_id] = YumTransactionItem(tr_id)

        if RPMTransactions.all in item:
          transactions[tr_id].pkgs_all = pkgs_in_transaction
        elif RPMTransactions.done in item:
          transactions[tr_id].pkgs_done = pkgs_in_transaction

    for tr in transactions.values():
      if len(tr.pkgs_all) == 0:
        continue

      if isinstance(tr, YumTransactionItem):
        yield tr

  def check_uncompleted_transactions(self):
    """
    Check package manager against uncompleted transactions.

    :rtype bool
    """

    transactions = list(self.uncomplete_transactions())

    if len(transactions) > 0:
      Logger.info("Yum non-completed transactions check failed, found {0} non-completed transaction(s):".format(len(transactions)))
      for tr in transactions:
        Logger.info("[{0}] Packages broken: {1}; Packages not-installed {2}".format(
          tr.transaction_id,
          ", ".join(tr.pkgs_done),
          ", ".join(tr.pkgs_aborted)
        ))

      return True

    Logger.info("Yum non-completed transactions check passed")
    return False

  def print_uncompleted_transaction_hint(self):
    """
    Print friendly message about they way to fix the issue

    """
    help_msg = """*** Incomplete Yum Transactions ***
    
Ambari has detected that there are incomplete Yum transactions on this host. This will interfere with the installation process and must be resolved before continuing.

- Identify the pending transactions with the command 'yum history list <packages failed>'
- Revert each pending transaction with the command 'yum history undo'
- Flush the transaction log with 'yum-complete-transaction --cleanup-only'

If the issue persists, old transaction files may be the cause.
Please delete them from /var/lib/yum/transaction*
"""

    for line in help_msg.split("\n"):
      Logger.error(line)


class YumTransactionItem(object):
  def __init__(self, transaction_id, pkgs_done=None, pkgs_all=None):
    self.transaction_id = transaction_id
    self.pkgs_done = pkgs_done if pkgs_done else []
    self.pkgs_all = pkgs_all if pkgs_all else []

  @property
  def pkgs_aborted(self):
    return set(self.pkgs_all) ^ set(self.pkgs_done)


class RPMTransactions(object):
  all = "all"
  done = "done"
  aborted = "aborted"  # custom one
