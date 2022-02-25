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

__all__ = ["select", "create", "get_hadoop_conf_dir", "get_hadoop_dir", "get_package_dirs"]

# Python Imports
import os
from ambari_commons import subprocess32
import ambari_simplejson as json

# Local Imports
from resource_management.core import shell
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import Link
from resource_management.libraries.functions import component_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import stack_tools
from resource_management.core.exceptions import Fail
from resource_management.core import sudo
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

def _get_cmd(command, package, version):
  conf_selector_path = stack_tools.get_stack_tool_path(stack_tools.CONF_SELECTOR_NAME)
  return ('ambari-python-wrap', conf_selector_path, command, '--package', package, '--stack-version', version, '--conf-version', '0')

def _valid(stack_name, package, ver):
  return (ver and check_stack_feature(StackFeature.CONFIG_VERSIONING, ver))

def get_package_dirs():
  """
  Get package dir mappings
  :return:
  """
  stack_name = default("/clusterLevelParams/stack_name", None)
  if stack_name is None:
    raise Fail("The stack name is not present in the command. Packages for conf-select tool cannot be loaded.")

  stack_packages_config = default("/configurations/cluster-env/stack_packages", None)
  if stack_packages_config is None:
    raise Fail("The stack packages are not defined on the command. Unable to load packages for the conf-select tool")

  data = json.loads(stack_packages_config)

  if stack_name not in data:
    raise Fail(
      "Cannot find conf-select packages for the {0} stack".format(stack_name))

  conf_select_key = "conf-select"
  data = data[stack_name]
  if conf_select_key not in data:
    raise Fail(
      "There are no conf-select packages defined for this command for the {0} stack".format(stack_name))

  package_dirs = data[conf_select_key]

  stack_root = Script.get_stack_root()
  for package_name, directories in package_dirs.iteritems():
    for dir in directories:
      current_dir = dir['current_dir']
      current_dir =  current_dir.format(stack_root)
      dir['current_dir'] = current_dir

  return package_dirs

def create(stack_name, package, version, dry_run = False):
  """
  Creates a config version for the specified package
  :param stack_name: the name of the stack
  :param package: the name of the package, as-used by <conf-selector-tool>
  :param version: the version number to create
  :param dry_run: False to create the versioned config directory, True to only return what would be created
  :return List of directories created
  """
  if not _valid(stack_name, package, version):
    Logger.info("Unable to create versioned configuration directories since the parameters supplied do not support it")
    return []

  # clarify the logging of what we're doing ...
  if dry_run:
    Logger.info(
      "Checking to see which directories will be created for {0} on version {1}".format(package, version))
  else:
    Logger.info("Creating /etc/{0}/{1}/0 if it does not exist".format(package, version))

  command = "dry-run-create" if dry_run else "create-conf-dir"

  code, stdout, stderr = shell.call(_get_cmd(command, package, version), logoutput=False, quiet=False, sudo=True, stderr = subprocess32.PIPE)

  # <conf-selector-tool> can set more than one directory
  # per package, so return that list, especially for dry_run
  # > <conf-selector-tool> dry-run-create --package hive-hcatalog --stack-version 2.4.0.0-169 0
  # /etc/hive-webhcat/2.4.0.0-169/0
  # /etc/hive-hcatalog/2.4.0.0-169/0
  created_directories = []
  if 0 == code and stdout is not None: # just be sure we have a stdout
    for line in stdout.splitlines():
      created_directories.append(line.rstrip('\n'))

  # if directories were created, then do some post-processing
  if not code and stdout and not dry_run:
    # take care of permissions if directories were created
    for directory in created_directories:
      Directory(directory, mode=0755, cd_access='a', create_parents=True)

    # seed the new directories with configurations from the old (current) directories
    _seed_new_configuration_directories(package, created_directories)

  return created_directories


def select(stack_name, package, version, ignore_errors=False):
  """
  Selects a config version for the specified package.

  :param stack_name: the name of the stack
  :param package: the name of the package, as-used by <conf-selector-tool>
  :param version: the version number to create
  :param ignore_errors: optional argument to ignore any error and simply log a warning
  """
  try:
    # do nothing if the stack does not support versioned configurations
    if not _valid(stack_name, package, version):
      return

    create(stack_name, package, version)
    shell.checked_call(_get_cmd("set-conf-dir", package, version), logoutput=False, quiet=False, sudo=True)
  except Exception, exception:
    if ignore_errors is True:
      Logger.warning("Could not select the directory for package {0}. Error: {1}".format(package,
        str(exception)))
    else:
      raise



def get_hadoop_conf_dir():
  """
  Return the hadoop shared conf directory which should be used for the command's component. The
  directory including the component's version is tried first, but if that doesn't exist,
  this will fallback to using "current".
  """
  stack_root = Script.get_stack_root()
  stack_version = Script.get_stack_version()

  hadoop_conf_dir = os.path.join(os.path.sep, "etc", "hadoop", "conf")
  if check_stack_feature(StackFeature.CONFIG_VERSIONING, stack_version):
    # read the desired version from the component map and use that for building the hadoop home
    version = component_version.get_component_repository_version()
    if version is None:
      version = default("/commandParams/version", None)

    hadoop_conf_dir = os.path.join(stack_root, str(version), "hadoop", "conf")
    if version is None or sudo.path_isdir(hadoop_conf_dir) is False:
      hadoop_conf_dir = os.path.join(stack_root, "current", "hadoop-client", "conf")

    Logger.info("Using hadoop conf dir: {0}".format(hadoop_conf_dir))

  return hadoop_conf_dir


def convert_conf_directories_to_symlinks(package, version, dirs):
  """
  Reverses the symlinks created by the package installer and invokes the conf-select tool to
  create versioned configuration directories for the given package. If the package does not exist,
  then no work is performed.

  - Creates /etc/<component>/<version>/0 via <conf-selector-tool>
  - Creates a /etc/<component>/conf.backup directory, if needed
  - Copies all configs from /etc/<component>/conf to conf.backup, if needed
  - Removes /etc/<component>/conf, if needed
  - <stack-root>/current/<component>-client/conf -> /etc/<component>/<version>/0 via <conf-selector-tool>
  - Links /etc/<component>/conf -> <stack-root>/current/[component]-client/conf

  :param package: the package to create symlinks for (zookeeper, falcon, etc)
  :param version: the version number to use with <conf-selector-tool> (2.3.0.0-1234)
  :param dirs: the directories associated with the package (from get_package_dirs())
  """
  # if the conf_dir doesn't exist, then that indicates that the package's service is not installed
  # on this host and nothing should be done with conf symlinks
  stack_name = Script.get_stack_name()
  for directory_struct in dirs:
    if not os.path.exists(directory_struct['conf_dir']):
      Logger.info("Skipping the conf-select tool on {0} since {1} does not exist.".format(
        package, directory_struct['conf_dir']))

      return

  # determine which directories would be created, if any are needed
  dry_run_directory = create(stack_name, package, version, dry_run = True)

  # if the dry run reported an error, then we must assume that the package does not exist in
  # the conf-select tool
  if len(dry_run_directory) == 0:
    Logger.info("The conf-select tool reported an error for the package {0}. The configuration linking will be skipped.".format(package))
    return


  need_dirs = []
  for d in dry_run_directory:
    if not os.path.exists(d):
      need_dirs.append(d)

  # log that we'll actually be creating some directories soon
  if len(need_dirs) > 0:
    Logger.info("Package {0} will have the following new configuration directories created: {1}".format(
      package, ", ".join(dry_run_directory)))

  # Create the versioned /etc/[component]/[version]/0 folder (using create-conf-dir) and then
  # set it for the installed component:
  # - Creates /etc/<component>/<version>/0
  # - Links <stack-root>/<version>/<component>/conf -> /etc/<component>/<version>/0
  select(stack_name, package, version, ignore_errors = True)

  # check every existing link to see if it's a link and if it's pointed to the right spot
  for directory_struct in dirs:
    try:
      # check if conf is a link already
      old_conf = directory_struct['conf_dir']
      current_dir = directory_struct['current_dir']
      if os.path.islink(old_conf):
        # it's already a link; make sure it's a link to where we want it
        if os.readlink(old_conf) != current_dir:
          # the link isn't to the right spot; re-link it
          Logger.info("Re-linking symlink {0} to {1}".format(old_conf, current_dir))
          Link(old_conf, action = "delete")
          Link(old_conf, to = current_dir)
        else:
          Logger.info("{0} is already linked to {1}".format(old_conf, current_dir))
      elif os.path.isdir(old_conf):
        # the /etc/<component>/conf directory is not a link, so turn it into one
        Logger.info("{0} is a directory - it must be converted into a symlink".format(old_conf))

        backup_dir = _get_backup_conf_directory(old_conf)
        Logger.info("Backing up {0} to {1} if destination doesn't exist already.".format(old_conf, backup_dir))
        Execute(("cp", "-R", "-p", old_conf, backup_dir),
          not_if = format("test -e {backup_dir}"), sudo = True)

        # delete the old /etc/<component>/conf directory now that it's been backed up
        Directory(old_conf, action = "delete")

        # link /etc/[component]/conf -> <stack-root>/current/[component]-client/conf
        Link(old_conf, to = current_dir)
      else:
        # missing entirely
        # /etc/<component>/conf -> <stack-root>/current/<component>/conf
        if package in ["atlas", ]:
          # HACK for Atlas
          '''
          In the case of Atlas, the Hive RPM installs /usr/$stack/$version/atlas with some partial packages that
          contain Hive hooks, while the Atlas RPM is responsible for installing the full content.

          If the user does not have Atlas currently installed on their stack, then /usr/$stack/current/atlas-client
          will be a broken symlink, and we should not create the
          symlink /etc/atlas/conf -> /usr/$stack/current/atlas-client/conf .
          If we mistakenly create this symlink, then when the user performs an EU/RU and then adds Atlas service
          then the Atlas RPM will not be able to copy its artifacts into /etc/atlas/conf directory and therefore
          prevent Ambari from by copying those unmanaged contents into /etc/atlas/$version/0
          '''
          component_list = default("/localComponents", [])
          if "ATLAS_SERVER" in component_list or "ATLAS_CLIENT" in component_list:
            Logger.info("Atlas is installed on this host.")
            parent_dir = os.path.dirname(current_dir)
            if os.path.exists(parent_dir):
              Link(old_conf, to = current_dir)
            else:
              Logger.info(
                "Will not create symlink from {0} to {1} because the destination's parent dir does not exist.".format(
                  old_conf, current_dir))
          else:
            Logger.info(
            "Will not create symlink from {0} to {1} because Atlas is not installed on this host.".format(
              old_conf, current_dir))
        else:
          # Normal path for other packages
          Link(old_conf, to = current_dir)

    except Exception, e:
      Logger.warning("Could not change symlink for package {0} to point to current directory. Error: {1}".format(package, e))


def get_restricted_packages():
  """
  Gets the list of conf-select 'package' names that need to be invoked on the command.
  When the server passes down the list of packages to install, check the service names
  and use the information in stack_packages json to determine the list of packages that should
  be executed.  That is valid only for PATCH or MAINT upgrades.  STANDARD upgrades should be
  conf-select'ing everything it can find.
  """
  package_names = []

  # shortcut the common case if we are not patching
  cluster_version_summary = default("/roleParameters/cluster_version_summary/services", None)

  if cluster_version_summary is None:
    Logger.info("Cluster Summary is not available, there are no restrictions for conf-select")
    return package_names

  service_names = []

  # pick out the services that are targeted
  for servicename, servicedetail in cluster_version_summary.iteritems():
    if servicedetail['upgrade']:
      service_names.append(servicename)

  if 0 == len(service_names):
    Logger.info("No services found, there are no restrictions for conf-select")
    return package_names

  stack_name = default("/clusterLevelParams/stack_name", None)
  if stack_name is None:
    Logger.info("The stack name is not present in the command. Restricted names skipped.")
    return package_names

  stack_packages_config = default("/configurations/cluster-env/stack_packages", None)
  if stack_packages_config is None:
    Logger.info("The stack packages are not defined on the command. Restricted names skipped.")
    return package_names

  data = json.loads(stack_packages_config)

  if stack_name not in data:
    Logger.info("Cannot find conf-select packages for the {0} stack".format(stack_name))
    return package_names

  conf_select_key = "conf-select-patching"
  if conf_select_key not in data[stack_name]:
    Logger.info("There are no conf-select-patching elements defined for this command for the {0} stack".format(stack_name))
    return package_names

  service_dict = data[stack_name][conf_select_key]

  for servicename in service_names:
    if servicename in service_dict and 'packages' in service_dict[servicename]:
      package_names.extend(service_dict[servicename]['packages'])

  return package_names


def _seed_new_configuration_directories(package, created_directories):
  """
  Copies any files from the "current" configuration directory to the directories which were
  newly created with <conf-selector-tool>. This function helps ensure that files which are not tracked
  by Ambari will be available after performing a stack upgrade. Although old configurations
  will be copied as well, they will be overwritten when the components are writing out their
  configs after upgrade during their restart.

  This function will catch all errors, logging them, but not raising an exception. This is to
  prevent problems here from stopping and otherwise healthy upgrade.

  :param package: the <conf-selector-tool> package name
  :param created_directories: a list of directories that <conf-selector-tool> said it created
  :return: None
  """
  package_dirs = get_package_dirs()
  if package not in package_dirs:
    Logger.warning("Unable to seed newly created configuration directories for {0} because it is an unknown component".format(package))
    return

  # seed the directories with any existing configurations
  # this allows files which are not tracked by Ambari to be available after an upgrade
  Logger.info("Seeding versioned configuration directories for {0}".format(package))
  expected_directories = package_dirs[package]

  try:
    # if the expected directories don't match those created, we can't seed them
    if len(created_directories) != len(expected_directories):
      Logger.warning("The known configuration directories for {0} do not match those created by conf-select: {1}".format(
        package, str(created_directories)))

      return

    # short circuit for a simple 1:1 mapping
    if len(expected_directories) == 1:
      # <stack-root>/current/component/conf
      # the current directory is the source of the seeded configurations;
      source_seed_directory = expected_directories[0]["current_dir"]
      target_seed_directory = created_directories[0]
      _copy_configurations(source_seed_directory, target_seed_directory)
    else:
      for created_directory in created_directories:
        for expected_directory_structure in expected_directories:
          prefix = expected_directory_structure.get("prefix", None)
          if prefix is not None and created_directory.startswith(prefix):
            source_seed_directory = expected_directory_structure["current_dir"]
            target_seed_directory = created_directory
            _copy_configurations(source_seed_directory, target_seed_directory)

  except Exception, e:
    Logger.warning("Unable to seed new configuration directories for {0}. {1}".format(package, str(e)))


def _copy_configurations(source_directory, target_directory):
  """
  Copies from the source directory to the target directory. If the source directory is a symlink
  then it will be followed (deferenced) but any other symlinks found to copy will not be. This
  will ensure that if the configuration directory itself is a symlink, then it's contents will be
  copied, preserving and children found which are also symlinks.

  :param source_directory:  the source directory to copy from
  :param target_directory:  the target directory to copy to
  :return: None
  """
  # append trailing slash so the cp command works correctly WRT recursion and symlinks
  source_directory = os.path.join(source_directory, "*")
  Execute(as_sudo(["cp", "-R", "-p", "-v", source_directory, target_directory], auto_escape = False),
    logoutput = True)

def _get_backup_conf_directory(old_conf):
  """
  Calculates the conf.backup absolute directory given the /etc/<component>/conf location.
  :param old_conf:  the old conf directory (ie /etc/<component>/conf)
  :return:  the conf.backup absolute directory
  """
  old_parent = os.path.abspath(os.path.join(old_conf, os.pardir))
  backup_dir = os.path.join(old_parent, "conf.backup")
  return backup_dir