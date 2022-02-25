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

import os
import tempfile
import re
from .generic_manager import GenericManager, GenericManagerProperties
from .apt_parser import AptParser

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons import shell
from resource_management.core.logger import Logger


def replace_underscores(function_to_decorate):
  def wrapper(*args, **kwargs):
    self = args[0]
    name = args[1].replace("_", "-")
    return function_to_decorate(self, name, *args[2:], **kwargs)
  return wrapper


class AptManagerProperties(GenericManagerProperties):
  """
  Class to keep all Package-manager depended properties
  """
  locked_output = "Unable to lock the administration directory"
  repo_error = "Failure when receiving data from the peer"

  repo_manager_bin = "/usr/bin/apt-get"
  repo_cache_bin = "/usr/bin/apt-cache"
  pkg_manager_bin = "/usr/bin/dpkg"
  repo_update_cmd = [repo_manager_bin, 'update', '-qq']

  available_packages_cmd = [repo_cache_bin, "dump"]
  installed_packages_cmd = ['COLUMNS=999', pkg_manager_bin, "-l"]

  repo_definition_location = "/etc/apt/sources.list.d"

  install_cmd = {
    True: [repo_manager_bin, '-o', "Dpkg::Options::=--force-confdef", '--allow-unauthenticated', '--assume-yes', 'install'],
    False: [repo_manager_bin, '-q', '-o', "Dpkg::Options::=--force-confdef", '--allow-unauthenticated', '--assume-yes', 'install']
  }

  remove_cmd = {
    True: [repo_manager_bin, '-y', 'remove'],
    False: [repo_manager_bin, '-y', '-q', 'remove']
  }

  verify_dependency_cmd = [repo_manager_bin, '-qq', 'check']

  install_cmd_env = {'DEBIAN_FRONTEND': 'noninteractive'}

  repo_url_exclude = "ubuntu.com"
  configuration_dump_cmd = [AMBARI_SUDO_BINARY, "apt-config", "dump"]


class AptManager(GenericManager):

  def get_installed_package_version(self, package_name):
    r = shell.subprocess_executor("dpkg -s {0} | grep Version | awk '{{print $2}}'".format(package_name))
    return r.out.strip(os.linesep)

  @property
  def properties(self):
    return AptManagerProperties

  def installed_packages(self, pkg_names=None, repo_filter=None):
    """
    Return all installed packages in the system except packages in REPO_URL_EXCLUDE

    :type pkg_names list|set
    :type repo_filter str|None

    :return formatted list of packages
    """
    packages = []
    available_packages = self._available_packages_dict(pkg_names, repo_filter)

    with shell.process_executor(self.properties.installed_packages_cmd, error_callback=self._executor_error_handler,
                                strategy=shell.ReaderStrategy.BufferedChunks) as output:
      for package, version in AptParser.packages_installed_reader(output):
        if package in available_packages:
          packages.append(available_packages[package])

        if package not in available_packages:
          packages.append([package, version, "installed"])  # case, when some package not belongs to any known repo

    return packages

  def _available_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of the installed packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    """

    with shell.process_executor(self.properties.available_packages_cmd, error_callback=self._executor_error_handler,
                                strategy=shell.ReaderStrategy.BufferedChunks) as output:

      for pkg_item in AptParser.packages_reader(output):
        if repo_filter and repo_filter not in pkg_item[2]:
          continue
        if self.properties.repo_url_exclude in pkg_item[2]:
          continue
        if pkg_names and pkg_item[0] not in pkg_names:
          continue

        yield pkg_item

  def _available_packages_dict(self, pkg_names=None, repo_filter=None):
    """
    Same as available packages, but result returns as dict and package name as key

    :type pkg_names list|set
    :type repo_filter str|None
    """
    result = {}

    for item in self._available_packages(pkg_names, repo_filter):
      result[item[0]] = item

    return result

  def available_packages(self, pkg_names=None, repo_filter=None):
    """
    Returning list of the installed packages with possibility to filter them by name

    :type pkg_names list|set
    :type repo_filter str|None
    """
    return [item for item in self._available_packages(pkg_names, repo_filter)]

  def all_packages(self, pkg_names=None, repo_filter=None):
    return self.available_packages(pkg_names, repo_filter)

  def transform_baseurl_to_repoid(self, base_url):
    """
    Transforms the URL looking like proto://localhost/some/long/path to localhost_some_long_path

    :type base_url str
    :rtype str
    """
    url_proto_mask = "://"
    url_proto_pos = base_url.find(url_proto_mask)
    if url_proto_pos > 0:
      base_url = base_url[url_proto_pos+len(url_proto_mask):]

    return base_url.replace("/", "_").replace(" ", "_")

  def get_available_packages_in_repos(self, repos):
    """
    Gets all (both installed and available) packages that are available at given repositories.
    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """

    filtered_packages = []
    packages = self.available_packages()
    repo_ids = []

    for repo in repos.items:
      repo_ids.append(self.transform_baseurl_to_repoid(repo.base_url))

    if repos.feat.scoped:
      Logger.info("Looking for matching packages in the following repositories: {0}".format(", ".join(repo_ids)))
      for repo_id in repo_ids:
        for package in packages:
          if repo_id in package[2]:
            filtered_packages.append(package[0])

      return filtered_packages
    else:
      Logger.info("Packages will be queried using all available repositories on the system.")

      # this is the case where the hosts are marked as sysprepped, but
      # search the repos on-system anyway.  the url specified in ambari must match the one
      # in the list file for this to work
      for repo_id in repo_ids:
        for package in packages:
          if repo_id in package[2]:
            filtered_packages.append(package[0])

      if len(filtered_packages) > 0:
        Logger.info("Found packages for repo {}".format(str(filtered_packages)))
        return filtered_packages
      else:
        return [package[0] for package in packages]

  def package_manager_configuration(self):
    """
    Reading apt configuration

    :return dict with apt properties
    """
    with shell.process_executor(self.properties.configuration_dump_cmd, error_callback=self._executor_error_handler) as output:
      configuration = list(AptParser.config_reader(output))

    return dict(configuration)

  def verify_dependencies(self):
    """
    Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
    package installation process or invalid packages state after manual modification of packages list on the host

    :return True if no dependency issues found, False if dependency issue present
    :rtype bool
    """
    r = shell.subprocess_executor(self.properties.verify_dependency_cmd)
    pattern = re.compile("has missing dependency|E:")

    if r.code or (r.out and pattern.search(r.out)):
      err_msg = Logger.filter_text("Failed to verify package dependencies. Execution of '%s' returned %s. %s" % (self.properties.verify_dependency_cmd, r.code, r.out))
      Logger.error(err_msg)
      return False

    return True

  @replace_underscores
  def install_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext

    :raise ValueError if name is empty
    """
    from resource_management.core import sudo

    apt_sources_list_tmp_dir = None

    if not name:
      raise ValueError("Installation command was executed with no package name")
    elif not self._check_existence(name) or context.action_force:
      cmd = self.properties.install_cmd[context.log_output]
      copied_sources_files = []
      is_tmp_dir_created = False
      if context.use_repos:
        if 'base' in context.use_repos:
          use_repos = set([v for k, v in context.use_repos.items() if k != 'base'])
        else:
          cmd = cmd + ['-o', 'Dir::Etc::SourceList={0}'.format(self.properties.empty_file)]
          use_repos = set(context.use_repos.values())

        if use_repos:
          is_tmp_dir_created = True
          apt_sources_list_tmp_dir = tempfile.mkdtemp(suffix="-ambari-apt-sources-d")
          Logger.info("Temporary sources directory was created: {}".format(apt_sources_list_tmp_dir))

          for repo in use_repos:
            new_sources_file = os.path.join(apt_sources_list_tmp_dir, repo + '.list')
            Logger.info("Temporary sources file will be copied: {0}".format(new_sources_file))
            sudo.copy(os.path.join(self.properties.repo_definition_location, repo + '.list'), new_sources_file)
            copied_sources_files.append(new_sources_file)
          cmd = cmd + ['-o', 'Dir::Etc::SourceParts='.format(apt_sources_list_tmp_dir)]

      cmd = cmd + [name]
      Logger.info("Installing package {0} ('{1}')".format(name, shell.string_cmd_from_args_list(cmd)))
      shell.repository_manager_executor(cmd, self.properties, context, env=self.properties.install_cmd_env)

      if is_tmp_dir_created:
        for temporary_sources_file in copied_sources_files:
          Logger.info("Removing temporary sources file: {0}".format(temporary_sources_file))
          os.remove(temporary_sources_file)
        if apt_sources_list_tmp_dir:
          Logger.info("Removing temporary sources directory: {0}".format(apt_sources_list_tmp_dir))
          os.rmdir(apt_sources_list_tmp_dir)
    else:
      Logger.info("Skipping installation of existing package {0}".format(name))

  @replace_underscores
  def upgrade_package(self, name, context):
    """
    Install package

    :type name str
    :type context ambari_commons.shell.RepoCallContext

    :raise ValueError if name is empty
    """
    context.is_upgrade = True
    return self.install_package(name, context)

  @replace_underscores
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

  @replace_underscores
  def _check_existence(self, name):
    """
    For regexp names:
    If only part of packages were installed during early canceling.
    Let's say:
    1. install hbase-2-3-.*
    2. Only hbase-2-3-1234 is installed, but is not hbase-2-3-1234-regionserver yet.
    3. We cancel the apt-get

    In that case this is bug of packages we require.
    And hbase-2-3-*-regionserver should be added to metainfo.xml.

    Checking existence should never fail in such a case for hbase-2-3-.*, otherwise it
    gonna break things like removing packages and some other things.

    Note: this method SHOULD NOT use apt-get (apt.cache is using dpkg not apt). Because a lot of issues we have, when customer have
    apt-get in inconsistant state (locked, used, having invalid repo). Once packages are installed
    we should not rely on that.
    """
    # this method is more optimised than #installed_packages, as here we do not call available packages(as we do not
    # interested in repository, from where package come)
    cmd = self.properties.installed_packages_cmd + [name]

    with shell.process_executor(cmd, strategy=shell.ReaderStrategy.BufferedChunks, silent=True) as output:
      for package, version in AptParser.packages_installed_reader(output):
        return package == name

    return False
