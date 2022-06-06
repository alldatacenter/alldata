#!/usr/bin/env python

'''
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
'''

import os
import shutil
import json
import ast
import logging

from ambari_server.serverClassPath import ServerClassPath

from ambari_commons.exceptions import FatalException
from ambari_commons.inet_utils import download_file_anyway
from ambari_commons.logging_utils import print_info_msg, print_error_msg, print_warning_msg
from ambari_commons.os_utils import copy_file, run_os_command, change_owner, set_file_permissions
from ambari_server.serverConfiguration import get_ambari_properties, get_ambari_version, get_stack_location, \
  get_common_services_location, get_mpacks_staging_location, get_server_temp_location, get_extension_location, \
  get_java_exe_path, read_ambari_user, parse_properties_file, JDBC_DATABASE_PROPERTY, get_dashboard_location
from ambari_server.setupSecurity import ensure_can_start_under_current_user, generate_env
from ambari_server.setupActions import INSTALL_MPACK_ACTION, UNINSTALL_MPACK_ACTION, UPGRADE_MPACK_ACTION
from ambari_server.userInput import get_YN_input
from ambari_server.dbConfiguration import ensure_jdbc_driver_is_installed, LINUX_DBMS_KEYS_LIST

from resource_management.core import sudo
from resource_management.core.environment import Environment
from resource_management.libraries.functions.tar_archive import untar_archive, get_archive_root_dir
from resource_management.libraries.functions.version import compare_versions


MPACK_INSTALL_CHECKER_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.checks.MpackInstallChecker --mpack-stacks {2}"

logger = logging.getLogger(__name__)

MPACKS_REPLAY_LOG_FILENAME = "mpacks_replay.log"
MPACKS_CACHE_DIRNAME = "cache"
SERVICES_DIRNAME = "services"
DASHBOARDS_DIRNAME = "dashboards"
GRAFANA_DASHBOARDS_DIRNAME = "grafana-dashboards"
SERVICE_METRICS_DIRNAME = "service-metrics"
METRICS_EXTENSION = ".txt"

STACK_DEFINITIONS_RESOURCE_NAME = "stack-definitions"
EXTENSION_DEFINITIONS_RESOURCE_NAME = "extension-definitions"
SERVICE_DEFINITIONS_RESOURCE_NAME = "service-definitions"
MPACKS_RESOURCE_NAME = "mpacks"

BEFORE_INSTALL_HOOK_NAME = "before-install"
BEFORE_UPGRADE_HOOK_NAME = "before-upgrade"
AFTER_INSTALL_HOOK_NAME = "after-install"
AFTER_UPGRADE_HOOK_NAME = "after-upgrade"

PYTHON_HOOK_TYPE = "python"
SHELL_HOOK_TYPE = "shell"

STACK_DEFINITIONS_ARTIFACT_NAME = "stack-definitions"
SERVICE_DEFINITIONS_ARTIFACT_NAME = "service-definitions"
EXTENSION_DEFINITIONS_ARTIFACT_NAME = "extension-definitions"
STACK_ADDON_SERVICE_DEFINITIONS_ARTIFACT_NAME = "stack-addon-service-definitions"

RESOURCE_FRIENDLY_NAMES = {
  STACK_DEFINITIONS_RESOURCE_NAME : "stack definitions",
  SERVICE_DEFINITIONS_RESOURCE_NAME: "service definitions",
  EXTENSION_DEFINITIONS_RESOURCE_NAME: "extension definitions",
  MPACKS_RESOURCE_NAME: "management packs"
}

class _named_dict(dict):
  """
  Allow to get dict items using attribute notation, eg dict.attr == dict['attr']
  """
  def __init__(self, _dict):

    def repl_list(_list):
      for i, e in enumerate(_list):
        if isinstance(e, list):
          _list[i] = repl_list(e)
        if isinstance(e, dict):
          _list[i] = _named_dict(e)
      return _list

    dict.__init__(self, _dict)
    for key, value in self.iteritems():
      if isinstance(value, dict):
        self[key] = _named_dict(value)
      if isinstance(value, list):
        self[key] = repl_list(value)

  def __getattr__(self, item):
    if item in self:
      return self[item]
    else:
      dict.__getattr__(self, item)

def _get_temp_dir():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  tmpdir = get_server_temp_location(properties)
  if not os.path.exists(tmpdir):
    sudo.makedirs(tmpdir, 0755)
  return tmpdir

def download_mpack(mpack_path):
  """
  Download management pack
  :param mpack_path: Path to management pack
  :return: Path where the management pack was downloaded
  """
  # Download management pack to a temp location
  tmpdir = _get_temp_dir()
  archive_filename = os.path.basename(mpack_path)
  tmp_archive_path = os.path.join(tmpdir, archive_filename)

  print_info_msg("Download management pack to temp location {0}".format(tmp_archive_path))
  if os.path.exists(tmp_archive_path):
    os.remove(tmp_archive_path)
  if os.path.exists(mpack_path):
    # local path
    copy_file(mpack_path, tmp_archive_path)
  else:
    # remote path
    download_file_anyway(mpack_path, tmp_archive_path)
  return tmp_archive_path

def expand_mpack(archive_path):
  """
  Expand management pack
  :param archive_path: Local path to management pack
  :return: Path where the management pack was expanded
  """
  tmpdir = _get_temp_dir()
  archive_root_dir = get_archive_root_dir(archive_path)
  if not archive_root_dir:
    print_error_msg("Malformed management pack. Root directory missing!")
    raise FatalException(-1, 'Malformed management pack. Root directory missing!')

  # Expand management pack in temp directory
  tmp_root_dir = os.path.join(tmpdir, archive_root_dir)
  print_info_msg("Expand management pack at temp location {0}".format(tmp_root_dir))
  if os.path.exists(tmp_root_dir):
    sudo.rmtree(tmp_root_dir)

  with Environment():
    untar_archive(archive_path, tmpdir)

  if not os.path.exists(tmp_root_dir):
    print_error_msg("Malformed management pack. Failed to expand management pack!")
    raise FatalException(-1, 'Malformed management pack. Failed to expand management pack!')
  return tmp_root_dir

def read_mpack_metadata(mpack_dir):
  """
  Read management pack metadata
  :param mpack_dir: Path where the expanded management pack is location
  :return: Management pack metadata
  """
  # Read mpack metadata
  mpack_metafile = os.path.join(mpack_dir, "mpack.json")
  if not os.path.exists(mpack_metafile):
    print_error_msg("Malformed management pack. Metadata file missing!")
    return None

  mpack_metadata = _named_dict(json.load(open(mpack_metafile, "r")))
  return mpack_metadata

def get_mpack_properties():
  """
  Read ambari properties required for management packs
  :return: (stack_location, service_definitions_location, mpacks_staging_location)
  """
  # Get ambari config properties
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  stack_location = get_stack_location(properties)
  extension_location = get_extension_location(properties)
  service_definitions_location = get_common_services_location(properties)
  mpacks_staging_location = get_mpacks_staging_location(properties)
  dashboard_location = get_dashboard_location(properties)
  ambari_version = get_ambari_version(properties)
  return stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location

def _get_mpack_name_version(mpack_path):
  """
  Get mpack name and version
  :param mpack_path: Path to mpack
  """
  if not mpack_path:
    print_error_msg("Management pack not specified!")
    raise FatalException(-1, 'Management pack not specified!')

  # Download management pack to a temp location
  tmp_archive_path = download_mpack(mpack_path)
  if not (tmp_archive_path and os.path.exists(tmp_archive_path)):
    print_error_msg("Management pack could not be downloaded!")
    raise FatalException(-1, 'Management pack could not be downloaded!')

  # Expand management pack in temp directory
  tmp_root_dir = expand_mpack(tmp_archive_path)

  # Read mpack metadata
  mpack_metadata = read_mpack_metadata(tmp_root_dir)
  if not mpack_metadata:
    raise FatalException(-1, 'Malformed management pack {0}. Metadata file missing!'.format(mpack_path))

  return (mpack_metadata.name, mpack_metadata.version)

def create_symlink(src_dir, dest_dir, file_name, force=False):
  """
  Helper function to create symbolic link (dest_dir/file_name -> src_dir/file_name)
  :param src_dir: Source directory
  :param dest_dir: Destination directory
  :param file_name: File name
  :param force: Remove existing symlink
  """
  src_path = os.path.join(src_dir, file_name)
  dest_link = os.path.join(dest_dir, file_name)
  create_symlink_using_path(src_path, dest_link, force)

def create_symlink_using_path(src_path, dest_link, force=False):
  """
  Helper function to create symbolic link (dest_link -> src_path)
  :param src_path: Source path
  :param dest_link: Destination link
  :param force: Remove existing symlink
  """
  if force and os.path.islink(dest_link):
    sudo.unlink(dest_link)

  sudo.symlink(src_path, dest_link)
  print_info_msg("Symlink: " + dest_link)

def remove_symlinks(stack_location, extension_location, service_definitions_location, dashboard_location, staged_mpack_dir):
  """
  Helper function to remove all symbolic links pointed to a management pack
  :param stack_location: Path to stacks folder
                         (/var/lib/ambari-server/resources/stacks)
  :param extension_location: Path to extensions folder
                             (/var/lib/ambari-server/resources/extension)
  :param service_definitions_location: Path to service_definitions folder
                                      (/var/lib/ambari-server/resources/common-services)
  :param dashboard_location: Path to the custom service dashboard folder
                             (/var/lib/ambari-server/resources/dashboards)
  :param staged_mpack_dir: Path to management pack staging location
                           (/var/lib/ambari-server/resources/mpacks/mpack_name-mpack_version)
  """
  for location in [stack_location, extension_location, service_definitions_location, dashboard_location]:
    for root, dirs, files in os.walk(location):
      for name in files:
        file = os.path.join(root, name)
        if os.path.islink(file) and staged_mpack_dir in os.path.realpath(file):
          print_info_msg("Removing symlink {0}".format(file))
          sudo.unlink(file)
      for name in dirs:
        dir = os.path.join(root, name)
        if os.path.islink(dir) and staged_mpack_dir in os.path.realpath(dir):
          print_info_msg("Removing symlink {0}".format(dir))
          sudo.unlink(dir)

def run_mpack_install_checker(options, mpack_stacks):
  """
  Run MpackInstallChecker to validate that there is no cluster deployed with a stack that is not included in the management pack
  :param options: Options passed
  :param mpack_stacks: List of stacks included in the management pack
  :return: Output of MpackInstallChecker
  """
  properties = get_ambari_properties()
  database_type = properties[JDBC_DATABASE_PROPERTY]
  jdk_path = get_java_exe_path()

  if not jdk_path or not database_type:
    # Ambari Server has not been setup, so no cluster would be present
    return (0, "", "")

  parse_properties_file(options)
  options.database_index = LINUX_DBMS_KEYS_LIST.index(properties[JDBC_DATABASE_PROPERTY])
  ensure_jdbc_driver_is_installed(options, properties)

  serverClassPath = ServerClassPath(properties, options)
  class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell()

  command = MPACK_INSTALL_CHECKER_CMD.format(jdk_path, class_path, ",".join(mpack_stacks))

  ambari_user = read_ambari_user()
  current_user = ensure_can_start_under_current_user(ambari_user)
  environ = generate_env(options, ambari_user, current_user)

  return run_os_command(command, env=environ)


def validate_purge(options, purge_list, mpack_dir, mpack_metadata, replay_mode=False):
  """
  Validate purge options
  :param purge_list: List of resources to purge
  :param mpack_metadata: Management pack metadata
  :param replay_mode: Flag to indicate if purging in replay mode
  """
  # Get ambari mpacks config properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()

  if not purge_list:
    return

  if STACK_DEFINITIONS_RESOURCE_NAME in purge_list:
    mpack_stacks = []
    for artifact in mpack_metadata.artifacts:
      if artifact.type == STACK_DEFINITIONS_ARTIFACT_NAME:
        artifact_source_dir = os.path.join(mpack_dir, artifact.source_dir)
        for file in sorted(os.listdir(artifact_source_dir)):
          if os.path.isdir(os.path.join(artifact_source_dir, file)):
            stack_name = file
            mpack_stacks.append(stack_name)
    if not mpack_stacks:
      # Don't purge stacks accidentally when installing add-on mpacks
      err = "The management pack you are attempting to install does not contain {0}. Since this management pack " \
            "does not contain a stack, the --purge option with --purge-list={1} would cause your existing Ambari " \
            "installation to be unusable. Due to that we cannot install this management pack.".format(
          RESOURCE_FRIENDLY_NAMES[STACK_DEFINITIONS_RESOURCE_NAME], purge_list)
      print_error_msg(err)
      raise FatalException(1, err)
    else:
      # Valid that there are no clusters deployed with a stack that is not included in the management pack
      (retcode, stdout, stderr) = run_mpack_install_checker(options, mpack_stacks)
      if retcode > 0:
        print_error_msg(stderr)
        raise FatalException(1, stderr)

  if not replay_mode:
    purge_resources = set((v) for k, v in RESOURCE_FRIENDLY_NAMES.iteritems() if k in purge_list)
    answer = 'yes' if options.silent else 'no'
    warn_msg = "CAUTION: You have specified the --purge option with --purge-list={0}. " \
               "This will replace all existing {1} currently installed.\n" \
               "Are you absolutely sure you want to perform the purge [yes/no]? ({2})".format(
        purge_list, ", ".join(purge_resources), answer)
    okToPurge = get_YN_input(warn_msg, options.silent, answer)
    if not okToPurge:
      err = "Management pack installation cancelled by user"
      raise FatalException(1, err)

def purge_stacks_and_mpacks(purge_list, replay_mode=False):
  """
  Purge all stacks and management packs
  :param replay_mode: Flag to indicate if purging in replay mode
  """
  # Get ambari mpacks config properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()

  print_info_msg("Purging existing stack definitions and management packs")

  if not purge_list:
    print_info_msg("Nothing to purge")
    return

  # Don't delete default stack_advisor.py (stacks/stack_advisor.py)
  if STACK_DEFINITIONS_RESOURCE_NAME in purge_list and os.path.exists(stack_location):
    print_info_msg("Purging stack location: " + stack_location)
    for file in sorted(os.listdir(stack_location)):
      path = os.path.join(stack_location, file)
      if(os.path.isdir(path)):
        sudo.rmtree(path)

  if EXTENSION_DEFINITIONS_RESOURCE_NAME in purge_list and os.path.exists(extension_location):
    print_info_msg("Purging extension location: " + extension_location)
    sudo.rmtree(extension_location)

  if SERVICE_DEFINITIONS_RESOURCE_NAME in purge_list and os.path.exists(service_definitions_location):
    print_info_msg("Purging service definitions location: " + service_definitions_location)
    sudo.rmtree(service_definitions_location)

  # Don't purge mpacks staging directory in replay mode
  if MPACKS_RESOURCE_NAME in purge_list and not replay_mode and os.path.exists(mpacks_staging_location):
    print_info_msg("Purging mpacks staging location: " + mpacks_staging_location)
    sudo.rmtree(mpacks_staging_location)
    sudo.makedir(mpacks_staging_location, 0755)

def process_stack_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process stack-definitions artifacts
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  for file in sorted(os.listdir(artifact_source_dir)):
    if os.path.isfile(os.path.join(artifact_source_dir, file)):
      # Example: /var/lib/ambari-server/resources/stacks/stack_advisor.py
      create_symlink(artifact_source_dir, stack_location, file, options.force)
    else:
      src_stack_dir = os.path.join(artifact_source_dir, file)
      dest_stack_dir = os.path.join(stack_location, file)
      if not os.path.exists(dest_stack_dir):
        sudo.makedir(dest_stack_dir, 0755)
      for file in sorted(os.listdir(src_stack_dir)):
        if os.path.isfile(os.path.join(src_stack_dir, file)):
          create_symlink(src_stack_dir, dest_stack_dir, file, options.force)
        else:
          src_stack_version_dir = os.path.join(src_stack_dir, file)
          dest_stack_version_dir = os.path.join(dest_stack_dir, file)
          if not os.path.exists(dest_stack_version_dir):
            sudo.makedir(dest_stack_version_dir, 0755)
          for file in sorted(os.listdir(src_stack_version_dir)):
            if file == SERVICES_DIRNAME:
              src_stack_services_dir = os.path.join(src_stack_version_dir, file)
              dest_stack_services_dir = os.path.join(dest_stack_version_dir, file)
              if not os.path.exists(dest_stack_services_dir):
                sudo.makedir(dest_stack_services_dir, 0755)
              for file in sorted(os.listdir(src_stack_services_dir)):
                create_symlink(src_stack_services_dir, dest_stack_services_dir, file, options.force)
                src_service_dir = os.path.join(src_stack_services_dir, file)
                create_dashboard_symlinks(src_service_dir, file, dashboard_location, options)
            else:
              create_symlink(src_stack_version_dir, dest_stack_version_dir, file, options.force)

def create_dashboard_symlinks(src_service_dir, service_name, dashboard_location, options):
  """
  If there is a dashboards directory under the src_service_dir,
  create symlinks under the dashboard_location for the grafana_dashboards
  and the service-metrics
  :param src_service_dir: Location of the service directory in the management pack
  :param service_name: Name of the service directory
  :param dashboard_location: Location of the dashboards directory in ambari-server/resources
  :param options: Command line options
  """
  src_dashboards_dir = os.path.join(src_service_dir, DASHBOARDS_DIRNAME)
  if not os.path.exists(src_dashboards_dir):
    return

  src_grafana_dashboards_dir = os.path.join(src_dashboards_dir, GRAFANA_DASHBOARDS_DIRNAME)
  src_metrics_dir = os.path.join(src_dashboards_dir, SERVICE_METRICS_DIRNAME)
  if os.path.exists(src_grafana_dashboards_dir):
    dest_grafana_dashboards_dir = os.path.join(dashboard_location, GRAFANA_DASHBOARDS_DIRNAME)
    dest_service_dashboards_link = os.path.join(dest_grafana_dashboards_dir, service_name)
    if os.path.exists(dest_service_dashboards_link):
      message = "Grafana dashboards already exist for service {0}.".format(service_name)
      print_warning_msg(message)
    else:
      create_symlink_using_path(src_grafana_dashboards_dir, dest_service_dashboards_link, options.force)

  service_metrics_filename = service_name + METRICS_EXTENSION
  src_metrics_file = os.path.join(src_metrics_dir, service_metrics_filename)
  if os.path.exists(src_metrics_file):
    dest_metrics_dir = os.path.join(dashboard_location, SERVICE_METRICS_DIRNAME)
    if os.path.exists(os.path.join(dest_metrics_dir, service_metrics_filename)):
      message = "Service metrics already exist for service {0}.".format(service_name)
      print_warning_msg(message)
    else:
      create_symlink(src_metrics_dir, dest_metrics_dir, service_metrics_filename, options.force)

def process_extension_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process extension-definitions artifacts
  Creates references for everything under the artifact_source_dir in the extension_location directory.
  This links all files, creates the extension name and extension version directories.
  Under the extension version directory, it creates links for all files and directories other than the
  services directory.  It creates links from the services directory to each service in the extension version.
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  if not os.path.exists(extension_location):
    sudo.makedir(extension_location, 0755)
  for file in sorted(os.listdir(artifact_source_dir)):
    if os.path.isfile(os.path.join(artifact_source_dir, file)):
      # Example: /var/lib/ambari-server/resources/extensions/README.txt
      create_symlink(artifact_source_dir, extension_location, file, options.force)
    else:
      src_extension_dir = os.path.join(artifact_source_dir, file)
      dest_extension_dir = os.path.join(extension_location, file)
      if not os.path.exists(dest_extension_dir):
        sudo.makedir(dest_extension_dir, 0755)
      for file in sorted(os.listdir(src_extension_dir)):
        create_symlink(src_extension_dir, dest_extension_dir, file, options.force)
        src_extension_version_dir = os.path.join(src_extension_dir, file)
        src_services_dir = os.path.join(src_extension_version_dir, SERVICES_DIRNAME)
        if os.path.exists(src_services_dir):
          for file in sorted(os.listdir(src_services_dir)):
            src_service_dir = os.path.join(src_services_dir, file)
            create_dashboard_symlinks(src_service_dir, file, dashboard_location, options)

def process_service_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process service-definitions artifact
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  for file in sorted(os.listdir(artifact_source_dir)):
    service_name = file
    src_service_definitions_dir = os.path.join(artifact_source_dir, file)
    dest_service_definitions_dir = os.path.join(service_definitions_location, file)
    if not os.path.exists(dest_service_definitions_dir):
      sudo.makedir(dest_service_definitions_dir, 0755)
    for file in sorted(os.listdir(src_service_definitions_dir)):
      create_symlink(src_service_definitions_dir, dest_service_definitions_dir, file, options.force)
      src_service_version_dir = os.path.join(src_service_definitions_dir, file)
      create_dashboard_symlinks(src_service_version_dir, service_name, dashboard_location, options)

def process_stack_addon_service_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process stack addon service definitions artifact
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  service_versions_map = None
  if "service_versions_map" in artifact:
    service_versions_map = artifact.service_versions_map
  if not service_versions_map:
    msg = "Must provide service versions map for " + STACK_ADDON_SERVICE_DEFINITIONS_ARTIFACT_NAME  +" artifact!"
    print_error_msg(msg)
    raise FatalException(-1, msg)
  for service_name in sorted(os.listdir(artifact_source_dir)):
    source_service_path = os.path.join(artifact_source_dir, service_name)
    for service_version in sorted(os.listdir(source_service_path)):
      source_service_version_path = os.path.join(source_service_path, service_version)
      for service_version_entry in service_versions_map:
        if service_name == service_version_entry.service_name and service_version == service_version_entry.service_version:
          applicable_stacks = service_version_entry.applicable_stacks
          for applicable_stack in applicable_stacks:
            stack_name = applicable_stack.stack_name
            stack_version = applicable_stack.stack_version
            dest_stack_path = os.path.join(stack_location, stack_name)
            dest_stack_version_path = os.path.join(dest_stack_path, stack_version)
            dest_stack_services_path = os.path.join(dest_stack_version_path, SERVICES_DIRNAME)
            dest_link = os.path.join(dest_stack_services_path, service_name)
            if os.path.exists(dest_stack_path) and os.path.exists(dest_stack_version_path):
              if not os.path.exists(dest_stack_services_path):
                sudo.makedir(dest_stack_services_path, 0755)
              if options.force and os.path.islink(dest_link):
                sudo.unlink(dest_link)
              sudo.symlink(source_service_version_path, dest_link)
              create_dashboard_symlinks(source_service_version_path, service_name, dashboard_location, options)

def search_mpacks(mpack_name, max_mpack_version=None):
  """
  Search for all "mpack_name" management packs installed.
  If "max_mpack_version" is specified return only management packs < max_mpack_version
  :param mpack_name: Management pack name
  :param max_mpack_version: Max management pack version number
  :return: List of management pack
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  results = []
  if os.path.exists(mpacks_staging_location) and os.path.isdir(mpacks_staging_location):
    staged_mpack_dirs = sorted(os.listdir(mpacks_staging_location))
    for dir in staged_mpack_dirs:
      if dir == MPACKS_CACHE_DIRNAME:
        continue
      staged_mpack_dir = os.path.join(mpacks_staging_location, dir)
      if os.path.isdir(staged_mpack_dir):
        staged_mpack_metadata = read_mpack_metadata(staged_mpack_dir)
        if not staged_mpack_metadata:
          print_error_msg("Skipping malformed management pack in directory {0}.".format(staged_mpack_dir))
          continue
        staged_mpack_name = staged_mpack_metadata.name
        staged_mpack_version = staged_mpack_metadata.version
        if mpack_name == staged_mpack_name and \
             (not max_mpack_version or compare_versions(staged_mpack_version, max_mpack_version, format=True) < 0):
          results.append((staged_mpack_name, staged_mpack_version))
  return results

def _uninstall_mpacks(mpack_name, max_mpack_version=None):
  """
  Uninstall all "mpack_name" management packs.
  If "max_mpack_version" is specified uninstall only management packs < max_mpack_version
  :param mpack_name: Management pack name
  :param max_mpack_version: Max management pack version number
  """
  results = search_mpacks(mpack_name, max_mpack_version)
  for result in results:
    _uninstall_mpack(result[0], result[1])

def _uninstall_mpack(mpack_name, mpack_version):
  """
  Uninstall specific management pack
  :param mpack_name: Management pack name
  :param mpack_version: Management pack version
  """
  print_info_msg("Uninstalling management pack {0}-{1}".format(mpack_name, mpack_version))
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  found = False
  if os.path.exists(mpacks_staging_location) and os.path.isdir(mpacks_staging_location):
    staged_mpack_dirs = sorted(os.listdir(mpacks_staging_location))
    for dir in staged_mpack_dirs:
      if dir == MPACKS_CACHE_DIRNAME:
        continue
      staged_mpack_dir = os.path.join(mpacks_staging_location, dir)
      if os.path.isdir(staged_mpack_dir):
        staged_mpack_metadata = read_mpack_metadata(staged_mpack_dir)
        if not staged_mpack_metadata:
          print_error_msg("Skipping malformed management pack {0}-{1}. Metadata file missing!".format(
                  staged_mpack_name, staged_mpack_version))
          continue
        staged_mpack_name = staged_mpack_metadata.name
        staged_mpack_version = staged_mpack_metadata.version
        if mpack_name == staged_mpack_name and compare_versions(staged_mpack_version, mpack_version, format=True) == 0:
          print_info_msg("Removing management pack staging location {0}".format(staged_mpack_dir))
          sudo.rmtree(staged_mpack_dir)
          remove_symlinks(stack_location, extension_location, service_definitions_location, dashboard_location, staged_mpack_dir)
          found = True
          break
  if not found:
    print_error_msg("Management pack {0}-{1} is not installed!".format(mpack_name, mpack_version))
  else:
    print_info_msg("Management pack {0}-{1} successfully uninstalled!".format(mpack_name, mpack_version))

def validate_mpack_prerequisites(mpack_metadata):
  """
  Validate management pack prerequisites
  :param mpack_metadata: Management pack metadata
  """
  # Get ambari config properties
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  stack_location = get_stack_location(properties)
  current_ambari_version = get_ambari_version(properties)
  fail = False

  mpack_prerequisites = mpack_metadata.prerequisites
  if "min_ambari_version" in mpack_prerequisites:
    min_ambari_version = mpack_prerequisites.min_ambari_version
    if(compare_versions(min_ambari_version, current_ambari_version, format=True) > 0):
      print_error_msg("Prerequisite failure! Current Ambari Version = {0}, "
                      "Min Ambari Version = {1}".format(current_ambari_version, min_ambari_version))
      fail = True
  if "max_ambari_version" in mpack_prerequisites:
    max_ambari_version = mpack_prerequisites.max_ambari_version
    if(compare_versions(max_ambari_version, current_ambari_version, format=True) < 0):
      print_error_msg("Prerequisite failure! Current Ambari Version = {0}, "
                      "Max Ambari Version = {1}".format(current_ambari_version, max_ambari_version))
  if "min_stack_versions" in mpack_prerequisites:
    min_stack_versions = mpack_prerequisites.min_stack_versions
    stack_found = False
    for min_stack_version in min_stack_versions:
      stack_name = min_stack_version.stack_name
      stack_version = min_stack_version.stack_version
      stack_dir = os.path.join(stack_location, stack_name, stack_version)
      if os.path.exists(stack_dir) and os.path.isdir(stack_dir):
        stack_found = True
        break
    if not stack_found:
      print_error_msg("Prerequisite failure! Min applicable stack not found")
      fail = True

  if fail:
    raise FatalException(-1, "Prerequisites for management pack {0}-{1} failed!".format(
            mpack_metadata.name, mpack_metadata.version))

def _install_mpack(options, replay_mode=False, is_upgrade=False):
  """
  Install management pack
  :param options: Command line options
  :param replay_mode: Flag to indicate if executing command in replay mode
  """

  mpack_path = options.mpack_path
  if not mpack_path:
    print_error_msg("Management pack not specified!")
    raise FatalException(-1, 'Management pack not specified!')

  print_info_msg("Installing management pack {0}".format(mpack_path))

  # Download management pack to a temp location
  tmp_archive_path = download_mpack(mpack_path)
  if not (tmp_archive_path and os.path.exists(tmp_archive_path)):
    print_error_msg("Management pack could not be downloaded!")
    raise FatalException(-1, 'Management pack could not be downloaded!')

  # Expand management pack in temp directory
  tmp_root_dir = expand_mpack(tmp_archive_path)

  # Read mpack metadata
  mpack_metadata = read_mpack_metadata(tmp_root_dir)
  if not mpack_metadata:
    raise FatalException(-1, 'Malformed management pack {0}. Metadata file missing!'.format(mpack_path))

  # Validate management pack prerequisites
  # Skip validation in replay mode
  if not replay_mode:
    validate_mpack_prerequisites(mpack_metadata)

  if is_upgrade:
    # Execute pre upgrade hook
    _execute_hook(mpack_metadata, BEFORE_UPGRADE_HOOK_NAME, tmp_root_dir)
  else:
    # Execute pre install hook
    _execute_hook(mpack_metadata, BEFORE_INSTALL_HOOK_NAME, tmp_root_dir)

  # Purge previously installed stacks and management packs
  if not is_upgrade and options.purge and options.purge_list:
    purge_resources = options.purge_list.split(",")
    validate_purge(options, purge_resources, tmp_root_dir, mpack_metadata, replay_mode)
    purge_stacks_and_mpacks(purge_resources, replay_mode)

  adjust_ownership_list = []
  change_ownership_list = []

  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  mpacks_cache_location = os.path.join(mpacks_staging_location, MPACKS_CACHE_DIRNAME)
  # Create directories
  if not os.path.exists(stack_location):
    sudo.makedir(stack_location, 0755)
  adjust_ownership_list.append((stack_location, "0755", "{0}", True))
  change_ownership_list.append((stack_location,"{0}",True))
  if not os.path.exists(extension_location):
    sudo.makedir(extension_location, 0755)
  adjust_ownership_list.append((extension_location, "0755", "{0}", True))
  change_ownership_list.append((extension_location,"{0}",True))
  if not os.path.exists(service_definitions_location):
    sudo.makedir(service_definitions_location, 0755)
  adjust_ownership_list.append((service_definitions_location, "0755", "{0}", True))
  change_ownership_list.append((service_definitions_location,"{0}",True))
  if not os.path.exists(mpacks_staging_location):
    sudo.makedir(mpacks_staging_location, 0755)
  adjust_ownership_list.append((mpacks_staging_location, "0755", "{0}", True))
  change_ownership_list.append((mpacks_staging_location,"{0}",True))
  if not os.path.exists(mpacks_cache_location):
    sudo.makedir(mpacks_cache_location, 0755)
  adjust_ownership_list.append((mpacks_cache_location, "0755", "{0}", True))
  change_ownership_list.append((mpacks_cache_location,"{0}",True))
  if not os.path.exists(dashboard_location):
    sudo.makedir(dashboard_location, 0755)
    sudo.makedir(os.path.join(dashboard_location, GRAFANA_DASHBOARDS_DIRNAME), 0755)
    sudo.makedir(os.path.join(dashboard_location, SERVICE_METRICS_DIRNAME), 0755)
  adjust_ownership_list.append((dashboard_location, "0755", "{0}", True))
  change_ownership_list.append((dashboard_location,"{0}",True))

  # Stage management pack (Stage at /var/lib/ambari-server/resources/mpacks/mpack_name-mpack_version)
  mpack_name = mpack_metadata.name
  mpack_version = mpack_metadata.version
  mpack_dirname = mpack_name + "-" + mpack_version
  mpack_staging_dir = os.path.join(mpacks_staging_location, mpack_dirname)
  mpack_archive_path = os.path.join(mpacks_cache_location, os.path.basename(tmp_archive_path))

  print_info_msg("Stage management pack {0}-{1} to staging location {2}".format(
          mpack_name, mpack_version, mpack_staging_dir))
  if os.path.exists(mpack_staging_dir):
    if options.force:
      print_info_msg("Force removing previously installed management pack from {0}".format(mpack_staging_dir))
      sudo.rmtree(mpack_staging_dir)
    else:
      error_msg = "Management pack {0}-{1} already installed!".format(mpack_name, mpack_version)
      print_error_msg(error_msg)
      raise FatalException(-1, error_msg)
  shutil.move(tmp_root_dir, mpack_staging_dir)
  shutil.move(tmp_archive_path, mpack_archive_path)

  # Process setup steps for all artifacts (stack-definitions, extension-definitions,
  # service-definitions, stack-addon-service-definitions) in the management pack
  for artifact in mpack_metadata.artifacts:
    # Artifact name (Friendly name)
    artifact_name = artifact.name
    # Artifact type (stack-definitions, extension-definitions, service-definitions, etc)
    artifact_type = artifact.type
    # Artifact directory with contents of the artifact
    artifact_source_dir = os.path.join(mpack_staging_dir, artifact.source_dir)

    print_info_msg("Processing artifact {0} of type {1} in {2}".format(
            artifact_name, artifact_type, artifact_source_dir))
    if artifact.type == STACK_DEFINITIONS_ARTIFACT_NAME:
      process_stack_definitions_artifact(artifact, artifact_source_dir, options)
    elif artifact.type == EXTENSION_DEFINITIONS_ARTIFACT_NAME:
      process_extension_definitions_artifact(artifact, artifact_source_dir, options)
    elif artifact.type == SERVICE_DEFINITIONS_ARTIFACT_NAME:
      process_service_definitions_artifact(artifact, artifact_source_dir, options)
    elif artifact.type == STACK_ADDON_SERVICE_DEFINITIONS_ARTIFACT_NAME:
      process_stack_addon_service_definitions_artifact(artifact, artifact_source_dir, options)
    else:
      print_info_msg("Unknown artifact {0} of type {1}".format(artifact_name, artifact_type))

  ambari_user = read_ambari_user()

  if ambari_user:
     # This is required when a non-admin user is configured to setup ambari-server
    print_info_msg("Adjusting file permissions and ownerships")
    for pack in adjust_ownership_list:
      file = pack[0]
      mod = pack[1]
      user = pack[2].format(ambari_user)
      recursive = pack[3]
      logger.info("Setting file permissions: {0} {1} {2} {3}".format(file, mod, user, recursive))
      set_file_permissions(file, mod, user, recursive)

    for pack in change_ownership_list:
      path = pack[0]
      user = pack[1].format(ambari_user)
      recursive = pack[2]
      logger.info("Changing ownership: {0} {1} {2}".format(path, user, recursive))
      change_owner(path, user, recursive)

  print_info_msg("Management pack {0}-{1} successfully installed! Please restart ambari-server.".format(mpack_name, mpack_version))
  return mpack_metadata, mpack_name, mpack_version, mpack_staging_dir, mpack_archive_path

# TODO
def _execute_hook(mpack_metadata, hook_name, base_dir):
  if "hooks" in mpack_metadata:
    hooks = mpack_metadata["hooks"]
    for hook in hooks:
      if hook_name == hook.name:
        hook_script = os.path.join(base_dir, hook.script)
        if os.path.exists(hook_script):
          print_info_msg("Executing {0} hook script : {1}".format(hook_name, hook_script))
          if hook.type == PYTHON_HOOK_TYPE:
            command = ["/usr/bin/ambari-python-wrap", hook_script]
          elif hook.type == SHELL_HOOK_TYPE:
            command = ["/bin/bash", hook_script]
          else:
            raise FatalException(-1, "Malformed management pack. Unknown hook type for {0} hook script"
                                 .format(hook_name))
          (returncode, stdoutdata, stderrdata) = run_os_command(command)
          if returncode != 0:
            msg = "Failed to execute {0} hook. Failed with error code {0}".format(hook_name, returncode)
            print_error_msg(msg)
            print_error_msg(stderrdata)
            raise FatalException(-1, msg)
          else:
            print_info_msg(stdoutdata)
        else:
          raise FatalException(-1, "Malformed management pack. Missing {0} hook script {1}"
                               .format(hook_name, hook_script))

def get_replay_log_file():
  """
  Helper function to get mpack replay log file path
  :return: mpack replay log file path
  """
  stack_location, extension_location, service_definitions_location, mpacks_staging_location, dashboard_location = get_mpack_properties()
  replay_log_file = os.path.join(mpacks_staging_location, MPACKS_REPLAY_LOG_FILENAME)
  return replay_log_file

def add_replay_log(mpack_command, mpack_archive_path, purge, purge_list, force, verbose):
  """
  Helper function to add mpack replay log entry
  :param mpack_command: mpack command
  :param mpack_archive_path: mpack archive path (/var/lib/ambari-server/resources/mpacks/mpack.tar.gz)
  :param purge: purge command line option
  :param purge: purge list command line option
  :param force: force command line option
  :param verbose: verbose command line option
  """
  replay_log_file = get_replay_log_file()
  log = { 'mpack_command' : mpack_command, 'mpack_path' : mpack_archive_path, 'purge' : purge, 'purge_list': purge_list, 'force' : force, 'verbose' : verbose }
  with open(replay_log_file, "a") as replay_log:
    replay_log.write("{0}\n".format(log))

def remove_replay_logs(mpack_name):
  replay_log_file = get_replay_log_file()
  logs = []
  if os.path.exists(replay_log_file):
    with open(replay_log_file, "r") as f:
      for log in f:
        log = log.strip()
        options = _named_dict(ast.literal_eval(log))
        (name, version) = _get_mpack_name_version(options.mpack_path)
        if mpack_name != name:
          logs.append(log)
    with open(replay_log_file, "w") as replay_log:
      for log in logs:
        replay_log.write("{0}\n".format(log))

def install_mpack(options, replay_mode=False):
  """
  Install management pack
  :param options: Command line options
  :param replay_mode: Flag to indicate if executing command in replay mode
  """
  logger.info("Install mpack.")
  # Force install when replaying logs
  if replay_mode:
    options.force = True
  (mpack_metadata, mpack_name, mpack_version, mpack_staging_dir, mpack_archive_path) = _install_mpack(options, replay_mode)

  # Execute post install hook
  _execute_hook(mpack_metadata, AFTER_INSTALL_HOOK_NAME, mpack_staging_dir)

  if not replay_mode:
    add_replay_log(INSTALL_MPACK_ACTION, mpack_archive_path, options.purge, options.purge_list, options.force, options.verbose)

def uninstall_mpack(options, replay_mode=False):
  """
  Uninstall management pack
  :param options: Command line options
  :param replay_mode: Flag to indicate if executing command in replay mode
  """
  logger.info("Uninstall mpack.")
  mpack_name = options.mpack_name

  if not mpack_name:
    print_error_msg("Management pack name not specified!")
    raise FatalException(-1, 'Management pack name not specified!')

  results = search_mpacks(mpack_name)
  if not results:
    print_error_msg("No management packs found that can be uninstalled!")
    raise FatalException(-1, 'No management packs found that can be uninstalled!')

  _uninstall_mpacks(mpack_name)

  print_info_msg("Management pack {0} successfully uninstalled!".format(mpack_name))
  if not replay_mode:
    remove_replay_logs(mpack_name)

def upgrade_mpack(options, replay_mode=False):
  """
  Upgrade management pack
  :param options: command line options
  :param replay_mode: Flag to indicate if executing command in replay mode
  """
  logger.info("Upgrade mpack.")
  mpack_path = options.mpack_path

  if not mpack_path:
    print_error_msg("Management pack not specified!")
    raise FatalException(-1, 'Management pack not specified!')

  (mpack_name, mpack_version) = _get_mpack_name_version(mpack_path)
  results = search_mpacks(mpack_name, mpack_version)
  if not results:
    print_error_msg("No management packs found that can be upgraded!")
    raise FatalException(-1, 'No management packs found that can be upgraded!')

  print_info_msg("Upgrading management pack {0}".format(mpack_path))

  # Force install new management pack version
  options.force = True
  (mpack_metadata, mpack_name, mpack_version, mpack_staging_dir, mpack_archive_path) = _install_mpack(options, replay_mode, is_upgrade=True)

  # Uninstall old management packs
  _uninstall_mpacks(mpack_name, mpack_version)

  # Execute post upgrade hook
  _execute_hook(mpack_metadata, AFTER_UPGRADE_HOOK_NAME, mpack_staging_dir)

  print_info_msg("Management pack {0}-{1} successfully upgraded!".format(mpack_name, mpack_version))
  if not replay_mode:
    add_replay_log(UPGRADE_MPACK_ACTION, mpack_archive_path, False, [], options.force, options.verbose)

def replay_mpack_logs():
  """
  Replay mpack logs during ambari-server upgrade
  """
  replay_log_file = get_replay_log_file()
  if os.path.exists(replay_log_file):
    with open(replay_log_file, "r") as f:
      for replay_log in f:
        replay_log = replay_log.strip()
        print_info_msg("===========================================================================================")
        print_info_msg("Executing Mpack Replay Log :")
        print_info_msg(replay_log)
        print_info_msg("===========================================================================================")
        replay_options = _named_dict(ast.literal_eval(replay_log))
        if 'purge_list' not in replay_options:
          replay_options.purge_list = ",".join([STACK_DEFINITIONS_RESOURCE_NAME, MPACKS_RESOURCE_NAME])
        if replay_options.mpack_command == INSTALL_MPACK_ACTION:
          install_mpack(replay_options, replay_mode=True)
        elif replay_options.mpack_command == UPGRADE_MPACK_ACTION:
          upgrade_mpack(replay_options, replay_mode=True)
        else:
          error_msg = "Invalid mpack command {0} in mpack replay log {1}!".format(replay_options.mpack_command, replay_log_file)
          print_error_msg(error_msg)
          raise FatalException(-1, error_msg)
  else:
    print_info_msg("No mpack replay logs found. Skipping replaying mpack commands")



