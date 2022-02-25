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
import tempfile

__all__ = ["Script"]

import re
import os
import sys
import ssl
import logging
import platform
import inspect
import tarfile
import traceback
import time
from optparse import OptionParser
import resource_management
from ambari_commons import OSCheck, OSConst
from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from ambari_commons.constants import UPGRADE_TYPE_HOST_ORDERED
from ambari_commons.network import reconfigure_urllib2_opener
from ambari_commons.inet_utils import resolve_address, ensure_ssl_using_protocol
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.resources import XmlConfig
from resource_management.libraries.resources import PropertiesFile
from resource_management.core import sudo
from resource_management.core.resources import File, Directory
from resource_management.core.source import InlineTemplate
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail, ClientComponentHasNoStatus, ComponentIsNotRunning
from resource_management.core.resources.packaging import Package
from resource_management.libraries.functions import version_select_util
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions import stack_tools
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.script.config_dictionary import ConfigDictionary, UnknownConfiguration
from resource_management.libraries.functions.repository_util import CommandRepository, RepositoryUtil
from resource_management.core.resources.system import Execute
from contextlib import closing
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.execution_command.execution_command import ExecutionCommand
from resource_management.libraries.functions.fcntl_based_process_lock import FcntlBasedProcessLock

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

if OSCheck.is_windows_family():
  from resource_management.libraries.functions.install_windows_msi import install_windows_msi
  from resource_management.libraries.functions.reload_windows_env import reload_windows_env
  from resource_management.libraries.functions.zip_archive import archive_dir
  from resource_management.libraries.resources import Msi
else:
  from resource_management.libraries.functions.tar_archive import archive_dir

USAGE = """Usage: {0} <COMMAND> <JSON_CONFIG> <BASEDIR> <STROUTPUT> <LOGGING_LEVEL> <TMP_DIR> [PROTOCOL]

<COMMAND> command type (INSTALL/CONFIGURE/START/STOP/SERVICE_CHECK...)
<JSON_CONFIG> path to command json file. Ex: /var/lib/ambari-agent/data/command-2.json
<BASEDIR> path to service metadata dir. Ex: /var/lib/ambari-agent/cache/common-services/HDFS/2.1.0.2.0/package
<STROUTPUT> path to file with structured command output (file will be created). Ex:/tmp/my.txt
<LOGGING_LEVEL> log level for stdout. Ex:DEBUG,INFO
<TMP_DIR> temporary directory for executable scripts. Ex: /var/lib/ambari-agent/tmp
[PROTOCOL] optional protocol to use during https connections. Ex: see python ssl.PROTOCOL_<PROTO> variables, default PROTOCOL_TLSv1_2
"""

_PASSWORD_MAP = {"/configurations/cluster-env/hadoop.user.name":"/configurations/cluster-env/hadoop.user.password"}
STACK_VERSION_PLACEHOLDER = "${stack_version}"
COUNT_OF_LAST_LINES_OF_OUT_FILES_LOGGED = 100
OUT_FILES_MASK = "*.out"
AGENT_TASKS_LOG_FILE = "/var/log/ambari-agent/agent_tasks.log"

def get_path_from_configuration(name, configuration):
  subdicts = filter(None, name.split('/'))

  for x in subdicts:
    if x in configuration:
      configuration = configuration[x]
    else:
      return None

  return configuration

def get_config_lock_file():
  return os.path.join(Script.get_tmp_dir(), "link_configs_lock_file")

class Script(object):
  instance = None

  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  Script instances share configuration as a class parameter and therefore
  different Script instances can not be used from different threads at
  the same time within a single python process

  Accepted command line arguments mapping:
  1 command type (START/STOP/...)
  2 path to command json file
  3 path to service metadata dir (Directory "package" inside service directory)
  4 path to file with structured command output (file will be created)
  """
  config = None
  execution_command = None
  module_configs = None
  cluster_settings = None
  stack_settings = None
  stack_version_from_distro_select = None
  structuredOut = {}
  command_data_file = ""
  basedir = ""
  stroutfile = ""
  logging_level = ""

  # Class variable
  tmp_dir = ""
  force_https_protocol = "PROTOCOL_TLSv1_2" if hasattr(ssl, "PROTOCOL_TLSv1_2") else "PROTOCOL_TLSv1"
  ca_cert_file_path = None

  def load_structured_out(self):
    Script.structuredOut = {}
    if os.path.exists(self.stroutfile):
      if os.path.getsize(self.stroutfile) > 0:
        with open(self.stroutfile, 'r') as fp:
          try:
            Script.structuredOut = json.load(fp)
          except Exception:
            errMsg = 'Unable to read structured output from ' + self.stroutfile
            Logger.logger.exception(errMsg)
            pass

    # version is only set in a specific way and should not be carried
    if "version" in Script.structuredOut:
      del Script.structuredOut["version"]
    # reset security issues and errors found on previous runs
    if "securityIssuesFound" in Script.structuredOut:
      del Script.structuredOut["securityIssuesFound"]
    if "securityStateErrorInfo" in Script.structuredOut:
      del Script.structuredOut["securityStateErrorInfo"]

  def put_structured_out(self, sout):
    Script.structuredOut.update(sout)
    try:
      with open(self.stroutfile, 'w') as fp:
        json.dump(Script.structuredOut, fp)
    except IOError, err:
      Script.structuredOut.update({"errMsg" : "Unable to write to " + self.stroutfile})


  def get_config_dir_during_stack_upgrade(self, env, base_dir, conf_select_name):
    """
    Because this gets called during a Rolling Upgrade, the new configs have already been saved, so we must be
    careful to only call configure() on the directory with the new version.

    If valid, returns the config directory to save configs to, otherwise, return None
    """
    import params
    env.set_params(params)

    required_attributes = ["stack_name", "stack_root", "version"]
    for attribute in required_attributes:
      if not hasattr(params, attribute):
        raise Fail("Failed in function 'stack_upgrade_save_new_config' because params was missing variable %s." % attribute)

    Logger.info("stack_upgrade_save_new_config(): Checking if can write new client configs to new config version folder.")

    if check_stack_feature(StackFeature.CONFIG_VERSIONING, params.version):
      # Even though hdp-select has not yet been called, write new configs to the new config directory.
      config_path = os.path.join(params.stack_root, params.version, conf_select_name, "conf")
      return os.path.realpath(config_path)
    return None

  def save_component_version_to_structured_out(self, command_name):
    """
    Saves the version of the component for this command to the structured out file. If the
    command is an install command and the repository is trusted, then it will use the version of
    the repository. Otherwise, it will consult the stack-select tool to read the symlink version.

    Under rare circumstances, a component may have a bug which prevents it from reporting a
    version back after being installed. This is most likely due to the stack-select tool not being
    invoked by the package's installer. In these rare cases, we try to see if the component
    should have reported a version and we try to fallback to the "<stack-select> versions" command.

    :param command_name: command name
    :return: None
    """
    from resource_management.libraries.functions.default import default
    from resource_management.libraries.functions import stack_select

    repository_resolved = default("repositoryFile/resolved", False)
    repository_version = default("repositoryFile/repoVersion", None)
    is_install_command = command_name is not None and command_name.lower() == "install"

    # start out with no version
    component_version = None

    # install command + trusted repo means use the repo version and don't consult stack-select
    # this is needed in cases where an existing symlink is on the system and stack-select can't
    # change it on installation (because it's scared to in order to support parallel installs)
    if is_install_command and repository_resolved and repository_version is not None:
      Logger.info("The repository with version {0} for this command has been marked as resolved."\
        " It will be used to report the version of the component which was installed".format(repository_version))

      component_version = repository_version

    stack_name = Script.get_stack_name()
    stack_select_package_name = stack_select.get_package_name()

    if stack_select_package_name and stack_name:
      # only query for the component version from stack-select if we can't trust the repository yet
      if component_version is None:
        component_version = version_select_util.get_component_version_from_symlink(stack_name, stack_select_package_name)

      # last ditch effort - should cover the edge case where the package failed to setup its
      # link and we have to try to see if <stack-select> can help
      if component_version is None:
        output, code, versions = stack_select.unsafe_get_stack_versions()
        if len(versions) == 1:
          component_version = versions[0]
          Logger.error("The '{0}' component did not advertise a version. This may indicate a problem with the component packaging. " \
                         "However, the stack-select tool was able to report a single version installed ({1}). " \
                         "This is the version that will be reported.".format(stack_select_package_name, component_version))

      if component_version:
        self.put_structured_out({"version": component_version})

        # if repository_version_id is passed, pass it back with the version
        from resource_management.libraries.functions.default import default
        repo_version_id = default("/repositoryFile/repoVersionId", None)
        if repo_version_id:
          self.put_structured_out({"repository_version_id": repo_version_id})
      else:
        if not self.is_hook():
          Logger.error("The '{0}' component did not advertise a version. This may indicate a problem with the component packaging.".format(stack_select_package_name))


  def should_expose_component_version(self, command_name):
    """
    Analyzes config and given command to determine if stack version should be written
    to structured out. Currently only HDP stack versions >= 2.2 are supported.
    :param command_name: command name
    :return: True or False
    """
    from resource_management.libraries.functions.default import default
    stack_version_unformatted = str(default("/clusterLevelParams/stack_version", ""))
    stack_version_formatted = format_stack_version(stack_version_unformatted)
    if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
      if command_name.lower() == "get_version":
        return True
      else:
        # Populate version only on base commands
        return command_name.lower() == "start" or command_name.lower() == "install" or command_name.lower() == "restart"
    return False

  def execute(self):
    """
    Sets up logging;
    Parses command parameters and executes method relevant to command type
    """
    parser = OptionParser()
    parser.add_option("-o", "--out-files-logging", dest="log_out_files", action="store_true",
                      help="use this option to enable outputting *.out files of the service pre-start")
    (self.options, args) = parser.parse_args()

    self.log_out_files = self.options.log_out_files

    # parse arguments
    if len(args) < 6:
     print "Script expects at least 6 arguments"
     print USAGE.format(os.path.basename(sys.argv[0])) # print to stdout
     sys.exit(1)

    self.command_name = str.lower(sys.argv[1])
    self.command_data_file = sys.argv[2]
    self.basedir = sys.argv[3]
    self.stroutfile = sys.argv[4]
    self.load_structured_out()
    self.logging_level = sys.argv[5]
    Script.tmp_dir = sys.argv[6]
    # optional script arguments for forcing https protocol and ca_certs file
    if len(sys.argv) >= 8:
      Script.force_https_protocol = sys.argv[7]
    if len(sys.argv) >= 9:
      Script.ca_cert_file_path = sys.argv[8]

    logging_level_str = logging._levelNames[self.logging_level]
    Logger.initialize_logger(__name__, logging_level=logging_level_str)

    # on windows we need to reload some of env variables manually because there is no default paths for configs(like
    # /etc/something/conf on linux. When this env vars created by one of the Script execution, they can not be updated
    # in agent, so other Script executions will not be able to access to new env variables
    if OSCheck.is_windows_family():
      reload_windows_env()

    # !!! status commands re-use structured output files; if the status command doesn't update the
    # the file (because it doesn't have to) then we must ensure that the file is reset to prevent
    # old, stale structured output from a prior status command from being used
    if self.command_name == "status":
      Script.structuredOut = {}
      self.put_structured_out({})

    # make sure that script has forced https protocol and ca_certs file passed from agent
    ensure_ssl_using_protocol(Script.get_force_https_protocol_name(), Script.get_ca_cert_file_path())

    try:
      with open(self.command_data_file) as f:
        pass
        Script.config = ConfigDictionary(json.load(f))
        Script.execution_command = ExecutionCommand(Script.config)
        Script.module_configs = Script.execution_command.get_module_configs()
        Script.cluster_settings = Script.execution_command.get_cluster_settings()
        Script.stack_settings = Script.execution_command.get_stack_settings()
        # load passwords here(used on windows to impersonate different users)
        Script.passwords = {}
        for k, v in _PASSWORD_MAP.iteritems():
          if get_path_from_configuration(k, Script.config) and get_path_from_configuration(v, Script.config):
            Script.passwords[get_path_from_configuration(k, Script.config)] = get_path_from_configuration(v, Script.config)

    except IOError:
      Logger.logger.exception("Can not read json file with command parameters: ")
      sys.exit(1)

    Script.repository_util = RepositoryUtil(Script.config)

    # Run class method depending on a command type
    try:
      method = self.choose_method_to_execute(self.command_name)
      with Environment(self.basedir, tmp_dir=Script.tmp_dir) as env:
        env.config.download_path = Script.tmp_dir

        if not self.is_hook():
          self.execute_prefix_function(self.command_name, 'pre', env)

        method(env)

        if not self.is_hook():
          self.execute_prefix_function(self.command_name, 'post', env)

    except Fail as ex:
      ex.pre_raise()
      raise
    finally:
      try:
        if self.should_expose_component_version(self.command_name):
          self.save_component_version_to_structured_out(self.command_name)
      except:
        Logger.exception("Reporting component version failed")

  def get_version(self, env):
    pass

  def execute_prefix_function(self, command_name, afix, env):
    """
    Execute action afix (prefix or suffix) based on command_name and afix type
    example: command_name=start, afix=pre will result in execution of self.pre_start(env) if exists
    """
    self_methods = dir(self)
    method_name = "{0}_{1}".format(afix, command_name)
    if not method_name in self_methods:
      Logger.logger.debug("Action afix '{0}' not present".format(method_name))
      return
    Logger.logger.debug("Execute action afix: {0}".format(method_name))
    method = getattr(self, method_name)
    method(env)

  def is_hook(self):
    from resource_management.libraries.script.hook import Hook
    return (Hook in self.__class__.__bases__)

  def get_log_folder(self):
    return ""

  def get_user(self):
    return ""

  def get_pid_files(self):
    return []

  def pre_start(self, env=None):
    """
    Executed before any start method. Posts contents of relevant *.out files to command execution log.
    """
    if self.log_out_files:
      log_folder = self.get_log_folder()
      user = self.get_user()

      if log_folder == "":
        Logger.logger.warn("Log folder for current script is not defined")
        return

      if user == "":
        Logger.logger.warn("User for current script is not defined")
        return

      show_logs(log_folder, user, lines_count=COUNT_OF_LAST_LINES_OF_OUT_FILES_LOGGED, mask=OUT_FILES_MASK)

  def post_start(self, env=None):
    pid_files = self.get_pid_files()
    if pid_files == []:
      Logger.logger.warning("Pid files for current script are not defined")
      return

    pids = []
    for pid_file in pid_files:
      if not sudo.path_exists(pid_file):
        raise Fail("Pid file {0} doesn't exist after starting of the component.".format(pid_file))

      pids.append(sudo.read_file(pid_file).strip())

    Logger.info("Component has started with pid(s): {0}".format(', '.join(pids)))

  def post_stop(self, env):
    """
    Executed after completion of every stop method. Waits until component is actually stopped (check is performed using
     components status() method.
    """
    self_methods = dir(self)

    if not 'status' in self_methods:
      pass
    status_method = getattr(self, 'status')
    component_is_stopped = False
    counter = 0
    while not component_is_stopped:
      try:
        if counter % 100 == 0:
          Logger.logger.info("Waiting for actual component stop")
        status_method(env)
        time.sleep(0.1)
        counter += 1
      except ComponentIsNotRunning, e:
        Logger.logger.debug("'status' reports ComponentIsNotRunning")
        component_is_stopped = True
      except ClientComponentHasNoStatus, e:
        Logger.logger.debug("Client component has no status")
        component_is_stopped = True

  def choose_method_to_execute(self, command_name):
    """
    Returns a callable object that should be executed for a given command.
    """
    self_methods = dir(self)
    if not command_name in self_methods:
      raise Fail("Script '{0}' has no method '{1}'".format(sys.argv[0], command_name))
    method = getattr(self, command_name)
    return method

  def get_stack_version_before_packages_installed(self):
    """
    This works in a lazy way (calculates the version first time and stores it). 
    If you need to recalculate the version explicitly set:
    
    Script.stack_version_from_distro_select = None
    
    before the call. However takes a bit of time, so better to avoid.

    :return: stack version including the build number. e.g.: 2.3.4.0-1234.
    """
    from resource_management.libraries.functions import stack_select
    from ambari_commons.repo_manager import ManagerFactory

    # preferred way is to get the actual selected version of current component
    stack_select_package_name = stack_select.get_package_name()
    if not Script.stack_version_from_distro_select and stack_select_package_name:
      Script.stack_version_from_distro_select = stack_select.get_stack_version_before_install(stack_select_package_name)

    # If <stack-selector-tool> has not yet been done (situations like first install),
    # we can use <stack-selector-tool> version itself.
    # Wildcards cause a lot of troubles with installing packages, if the version contains wildcards we should try to specify it.
    if not Script.stack_version_from_distro_select or '*' in Script.stack_version_from_distro_select:
      # FIXME: this method is not reliable to get stack-selector-version
      # as if there are multiple versions installed with different <stack-selector-tool>, we won't detect the older one (if needed).
      pkg_provider = ManagerFactory.get()

      Script.stack_version_from_distro_select = pkg_provider.get_installed_package_version(
              stack_tools.get_stack_tool_package(stack_tools.STACK_SELECTOR_NAME))


    return Script.stack_version_from_distro_select

  def get_package_from_available(self, name, available_packages_in_repos=None):
    """
    This function matches package names with ${stack_version} placeholder to actual package names from
    Ambari-managed repository.
    Package names without ${stack_version} placeholder are returned as is.
    """

    if STACK_VERSION_PLACEHOLDER not in name:
      return name

    if not available_packages_in_repos:
      available_packages_in_repos = self.load_available_packages()

    from resource_management.libraries.functions.default import default

    package_delimiter = '-' if OSCheck.is_ubuntu_family() else '_'
    package_regex = name.replace(STACK_VERSION_PLACEHOLDER, '(\d|{0})+'.format(package_delimiter)) + "$"
    repo = default('/repositoryFile', None)
    name_with_version = None

    if repo:
      command_repo = CommandRepository(repo)
      version_str = command_repo.version_string.replace('.', package_delimiter).replace("-", package_delimiter)
      name_with_version = name.replace(STACK_VERSION_PLACEHOLDER, version_str)

    for package in available_packages_in_repos:
      if re.match(package_regex, package):
        return package

    if name_with_version:
      raise Fail("No package found for {0}(expected name: {1})".format(name, name_with_version))
    else:
      raise Fail("Cannot match package for regexp name {0}. Available packages: {1}".format(name, self.available_packages_in_repos))

  def format_package_name(self, name):
    from resource_management.libraries.functions.default import default
    """
    This function replaces ${stack_version} placeholder with actual version.  If the package
    version is passed from the server, use that as an absolute truth.

    :param name name of the package
    :param repo_version actual version of the repo currently installing
    """
    if not STACK_VERSION_PLACEHOLDER in name:
      return name

    stack_version_package_formatted = ""

    package_delimiter = '-' if OSCheck.is_ubuntu_family() else '_'

    # repositoryFile is the truth
    # package_version should be made to the form W_X_Y_Z_nnnn
    package_version = default("repositoryFile/repoVersion", None)

    # TODO remove legacy checks
    if package_version is None:
      package_version = default("roleParams/package_version", None)

    # TODO remove legacy checks
    if package_version is None:
      package_version = default("hostLevelParams/package_version", None)

    if (package_version is None or '-' not in package_version) and default('/repositoryFile', None):
      return self.get_package_from_available(name)

    if package_version is not None:
      package_version = package_version.replace('.', package_delimiter).replace('-', package_delimiter)

    # The cluster effective version comes down when the version is known after the initial
    # install.  In that case we should not be guessing which version when invoking INSTALL, but
    # use the supplied version to build the package_version
    effective_version = default("commandParams/version", None)
    role_command = default("roleCommand", None)

    if (package_version is None or '*' in package_version) \
        and effective_version is not None and 'INSTALL' == role_command:
      package_version = effective_version.replace('.', package_delimiter).replace('-', package_delimiter)
      Logger.info("Version {0} was provided as effective cluster version.  Using package version {1}".format(effective_version, package_version))

    if package_version:
      stack_version_package_formatted = package_version
      if OSCheck.is_ubuntu_family():
        stack_version_package_formatted = package_version.replace('_', package_delimiter)

    # Wildcards cause a lot of troubles with installing packages, if the version contains wildcards we try to specify it.
    if not package_version or '*' in package_version:
      repo_version = self.get_stack_version_before_packages_installed()
      stack_version_package_formatted = repo_version.replace('.', package_delimiter).replace('-', package_delimiter) if STACK_VERSION_PLACEHOLDER in name else name

    package_name = name.replace(STACK_VERSION_PLACEHOLDER, stack_version_package_formatted)

    return package_name

  @staticmethod
  def get_config():
    """
    HACK. Uses static field to store configuration. This is a workaround for
    "circular dependency" issue when importing params.py file and passing to
     it a configuration instance.
    """
    return Script.config

  @staticmethod
  def get_execution_command():
    """
    The dot access dict object holds command.json
    :return:
    """
    return Script.execution_command

  @staticmethod
  def get_module_configs():
    """
    The dict object holds configurations block in command.json which maps service configurations
    :return: module_configs object
    """
    if not Script.module_configs:
      Script.module_configs = Script.execution_command.get_module_configs()
    return Script.module_configs

  @staticmethod
  def get_cluster_settings():
    """
    The dict object holds cluster_settings block in command.json which maps cluster configurations
    :return: cluster_settings object
    """
    if not Script.cluster_settings and Script.execution_command:
      Script.cluster_settings = Script.execution_command.get_cluster_settings()
    return Script.cluster_settings

  @staticmethod
  def get_stack_settings():
    """
    The dict object holds stack_settings block in command.json which maps stack configurations
    :return: stack_settings object
    """
    if not Script.stack_settings and Script.execution_command:
      Script.stack_settings = Script.execution_command.get_stack_settings()
    return Script.stack_settings

  @staticmethod
  def get_password(user):
    return Script.passwords[user]

  @staticmethod
  def get_tmp_dir():
    """
    HACK. Uses static field to avoid "circular dependency" issue when
    importing params.py.
    """
    return Script.tmp_dir

  @staticmethod
  def get_force_https_protocol_name():
    """
    Get forced https protocol name.

    :return: protocol name, PROTOCOL_TLSv1_2 by default
    """
    return Script.force_https_protocol

  @staticmethod
  def get_force_https_protocol_value():
    """
    Get forced https protocol value that correspondents to ssl module variable.

    :return: protocol value
    """
    return getattr(ssl, Script.get_force_https_protocol_name())

  @staticmethod
  def get_ca_cert_file_path():
    """
    Get path to file with trusted certificates.

    :return: trusted certificates file path
    """
    return Script.ca_cert_file_path

  @staticmethod
  def get_component_from_role(role_directory_map, default_role):
    """
    Gets the <stack-root>/current/<component> component given an Ambari role,
    such as DATANODE or HBASE_MASTER.
    :return:  the component name, such as hbase-master
    """
    from resource_management.libraries.functions.default import default

    command_role = default("/role", default_role)
    if command_role in role_directory_map:
      return role_directory_map[command_role]
    else:
      return role_directory_map[default_role]

  @staticmethod
  def get_stack_name():
    """
    Gets the name of the stack from clusterLevelParams/stack_name.
    :return: a stack name or None
    """
    from resource_management.libraries.functions.default import default
    stack_name = default("/clusterLevelParams/stack_name", None)
    if stack_name is None:
      stack_name = default("/configurations/cluster-env/stack_name", "HDP")

    return stack_name

  @staticmethod
  def get_stack_root():
    """
    Get the stack-specific install root directory
    :return: stack_root
    """
    from resource_management.libraries.functions.default import default
    stack_name = Script.get_stack_name()
    stack_root_json = default("/configurations/cluster-env/stack_root", None)

    if stack_root_json is None:
      return "/usr/{0}".format(stack_name.lower())

    stack_root = json.loads(stack_root_json)

    if stack_name not in stack_root:
      Logger.warning("Cannot determine stack root for stack named {0}".format(stack_name))
      return "/usr/{0}".format(stack_name.lower())

    return stack_root[stack_name]

  @staticmethod
  def get_stack_version():
    """
    Gets the normalized version of the stack in the form #.#.#.# if it is
    present on the configurations sent.
    :return: a normalized stack version or None
    """
    config = Script.get_config()
    if 'clusterLevelParams' not in config or 'stack_version' not in config['clusterLevelParams']:
      return None

    stack_version_unformatted = str(config['clusterLevelParams']['stack_version'])

    if stack_version_unformatted is None or stack_version_unformatted == '':
      return None

    return format_stack_version(stack_version_unformatted)

  @staticmethod
  def in_stack_upgrade():
    from resource_management.libraries.functions.default import default

    upgrade_direction = default("/commandParams/upgrade_direction", None)
    return upgrade_direction is not None and upgrade_direction in [Direction.UPGRADE, Direction.DOWNGRADE]


  @staticmethod
  def is_stack_greater(stack_version_formatted, compare_to_version):
    """
    Gets whether the provided stack_version_formatted (normalized)
    is greater than the specified stack version
    :param stack_version_formatted: the version of stack to compare
    :param compare_to_version: the version of stack to compare to
    :return: True if the command's stack is greater than the specified version
    """
    if stack_version_formatted is None or stack_version_formatted == "":
      return False

    return compare_versions(stack_version_formatted, compare_to_version) > 0

  @staticmethod
  def is_stack_greater_or_equal(compare_to_version):
    """
    Gets whether the hostLevelParams/stack_version, after being normalized,
    is greater than or equal to the specified stack version
    :param compare_to_version: the version to compare to
    :return: True if the command's stack is greater than or equal the specified version
    """
    return Script.is_stack_greater_or_equal_to(Script.get_stack_version(), compare_to_version)

  @staticmethod
  def is_stack_greater_or_equal_to(stack_version_formatted, compare_to_version):
    """
    Gets whether the provided stack_version_formatted (normalized)
    is greater than or equal to the specified stack version
    :param stack_version_formatted: the version of stack to compare
    :param compare_to_version: the version of stack to compare to
    :return: True if the command's stack is greater than or equal to the specified version
    """
    if stack_version_formatted is None or stack_version_formatted == "":
      return False

    return compare_versions(stack_version_formatted, compare_to_version) >= 0

  @staticmethod
  def is_stack_less_than(compare_to_version):
    """
    Gets whether the hostLevelParams/stack_version, after being normalized,
    is less than the specified stack version
    :param compare_to_version: the version to compare to
    :return: True if the command's stack is less than the specified version
    """
    stack_version_formatted = Script.get_stack_version()

    if stack_version_formatted is None:
      return False

    return compare_versions(stack_version_formatted, compare_to_version) < 0

  def install(self, env):
    """
    Default implementation of install command is to install all packages
    from a list, received from the server.
    Feel free to override install() method with your implementation. It
    usually makes sense to call install_packages() manually in this case
    """
    self.install_packages(env)

  def load_available_packages(self):
    from ambari_commons.repo_manager import ManagerFactory

    if self.available_packages_in_repos:
      return self.available_packages_in_repos

    config = self.get_config()

    service_name = config['serviceName'] if 'serviceName' in config else None
    repos = CommandRepository(config['repositoryFile'])

    repo_ids = [repo.repo_id for repo in repos.items]
    Logger.info("Command repositories: {0}".format(", ".join(repo_ids)))
    repos.items = [x for x in repos.items if (not x.applicable_services or service_name in x.applicable_services) ]
    applicable_repo_ids = [repo.repo_id for repo in repos.items]
    Logger.info("Applicable repositories: {0}".format(", ".join(applicable_repo_ids)))


    pkg_provider = ManagerFactory.get()
    try:
      self.available_packages_in_repos = pkg_provider.get_available_packages_in_repos(repos)
    except Exception as err:
      Logger.exception("Unable to load available packages")
      self.available_packages_in_repos = []

    return self.available_packages_in_repos


  def install_packages(self, env):
    """
    List of packages that are required< by service is received from the server
    as a command parameter. The method installs all packages
    from this list
    
    exclude_packages - list of regexes (possibly raw strings as well), the
    packages which match the regex won't be installed.
    NOTE: regexes don't have Python syntax, but simple package regexes which support only * and .* and ?
    """
    config = self.get_config()

    if 'host_sys_prepped' in config['ambariLevelParams']:
      # do not install anything on sys-prepped host
      if config['ambariLevelParams']['host_sys_prepped'] is True:
        Logger.info("Node has all packages pre-installed. Skipping.")
        return
      pass
    try:
      package_list_str = config['commandParams']['package_list']
      agent_stack_retry_on_unavailability = bool(config['ambariLevelParams']['agent_stack_retry_on_unavailability'])
      agent_stack_retry_count = int(config['ambariLevelParams']['agent_stack_retry_count'])
      if isinstance(package_list_str, basestring) and len(package_list_str) > 0:
        package_list = json.loads(package_list_str)
        for package in package_list:
          if self.check_package_condition(package):
            name = self.format_package_name(package['name'])
            # HACK: On Windows, only install ambari-metrics packages using Choco Package Installer
            # TODO: Update this once choco packages for hadoop are created. This is because, service metainfo.xml support
            # <osFamily>any<osFamily> which would cause installation failure on Windows.
            if OSCheck.is_windows_family():
              if "ambari-metrics" in name:
                Package(name)
            else:
              Package(name,
                      retry_on_repo_unavailability=agent_stack_retry_on_unavailability,
                      retry_count=agent_stack_retry_count)
    except KeyError:
      traceback.print_exc()

    if OSCheck.is_windows_family():
      #TODO hacky install of windows msi, remove it or move to old(2.1) stack definition when component based install will be implemented
      hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
      install_windows_msi(config['ambariLevelParams']['jdk_location'],
                          config["agentLevelParams"]["agentCacheDir"], ["hdp-2.3.0.0.winpkg.msi", "hdp-2.3.0.0.cab", "hdp-2.3.0.0-01.cab"],
                          hadoop_user, self.get_password(hadoop_user),
                          str(config['clusterLevelParams']['stack_version']))
      reload_windows_env()

  def check_package_condition(self, package):
    condition = package['condition']

    if not condition:
      return True

    return self.should_install_package(package)

  def should_install_package(self, package):
    from resource_management.libraries.functions import package_conditions
    condition = package['condition']
    try:
      chooser_method = getattr(package_conditions, condition)
    except AttributeError:
      name = package['name']
      raise Fail("Condition with name '{0}', when installing package {1}. Please check package_conditions.py.".format(condition, name))

    return chooser_method()

  @staticmethod
  def matches_any_regexp(string, regexp_list):
    for regex in regexp_list:
      # we cannot use here Python regex, since * will create some troubles matching plaintext names. 
      package_regex = '^' + re.escape(regex).replace('\\.\\*','.*').replace("\\?", ".").replace("\\*", ".*") + '$'
      if re.match(package_regex, string):
        return True
    return False

  @staticmethod
  def fail_with_error(message):
    """
    Prints error message and exits with non-zero exit code
    """
    print("Error: " + message)
    sys.stderr.write("Error: " + message)
    sys.exit(1)


  def start(self, env, upgrade_type=None):
    """
    To be overridden by subclasses
    """
    self.fail_with_error("start method isn't implemented")

  def stop(self, env, upgrade_type=None):
    """
    To be overridden by subclasses
    """
    self.fail_with_error("stop method isn't implemented")

  # TODO, remove after all services have switched to pre_upgrade_restart
  def pre_rolling_restart(self, env):
    """
    To be overridden by subclasses
    """
    pass

  def disable_security(self, env):
    """
    To be overridden by subclasses if a custom action is required upon dekerberization (e.g. removing zk ACLs)
    """
    pass

  def restart(self, env):
    """
    Default implementation of restart command is to call stop and start methods
    Feel free to override restart() method with your implementation.
    For client components we call install
    """
    config = self.get_config()
    componentCategory = None
    try:
      componentCategory = config['roleParams']['component_category']
    except KeyError:
      pass

    upgrade_type_command_param = ""
    direction = None
    is_rolling_restart = None
    if config is not None:
      command_params = config["commandParams"] if "commandParams" in config else None
      if command_params is not None:
        upgrade_type_command_param = command_params["upgrade_type"] if "upgrade_type" in command_params else ""
        direction = command_params["upgrade_direction"] if "upgrade_direction" in command_params else None
        is_rolling_restart = command_params["rolling_restart"] if "rolling_restart" in command_params else None

    upgrade_type = Script.get_upgrade_type(upgrade_type_command_param)
    is_stack_upgrade = upgrade_type is not None

    # need this before actually executing so that failures still report upgrade info
    if is_stack_upgrade:
      upgrade_info = {"upgrade_type": upgrade_type_command_param}
      if direction is not None:
        upgrade_info["direction"] = direction.upper()

      Script.structuredOut.update(upgrade_info)

    if componentCategory and componentCategory.strip().lower() == 'CLIENT'.lower():
      if is_stack_upgrade:
        # Remain backward compatible with the rest of the services that haven't switched to using
        # the pre_upgrade_restart method. Once done. remove the else-block.
        if "pre_upgrade_restart" in dir(self):
          self.pre_upgrade_restart(env, upgrade_type=upgrade_type)
        else:
          self.pre_rolling_restart(env)

      self.install(env)
    else:
      # To remain backward compatible with older stacks, only pass upgrade_type if available.
      # TODO, remove checking the argspec for "upgrade_type" once all of the services support that optional param.
      if "upgrade_type" in inspect.getargspec(self.stop).args:
        self.stop(env, upgrade_type=upgrade_type)
      else:
        if is_stack_upgrade:
          self.stop(env, rolling_restart=(upgrade_type == UPGRADE_TYPE_ROLLING))
        else:
          self.stop(env)

      if is_stack_upgrade:
        # Remain backward compatible with the rest of the services that haven't switched to using
        # the pre_upgrade_restart method. Once done. remove the else-block.
        if "pre_upgrade_restart" in dir(self):
          self.pre_upgrade_restart(env, upgrade_type=upgrade_type)
        else:
          self.pre_rolling_restart(env)

      service_name = config['serviceName'] if config is not None and 'serviceName' in config else None
      try:
        #TODO Once the logic for pid is available from Ranger and Ranger KMS code, will remove the below if block.
        services_to_skip = ['RANGER', 'RANGER_KMS']
        if service_name in services_to_skip:
          Logger.info('Temporarily skipping status check for {0} service only.'.format(service_name))
        elif is_stack_upgrade:
          Logger.info('Skipping status check for {0} service during upgrade'.format(service_name))
        else:
          self.status(env)
          raise Fail("Stop command finished but process keep running.")
      except ComponentIsNotRunning as e:
        pass  # expected
      except ClientComponentHasNoStatus as e:
        pass  # expected

      # To remain backward compatible with older stacks, only pass upgrade_type if available.
      # TODO, remove checking the argspec for "upgrade_type" once all of the services support that optional param.
      self.pre_start(env)
      if "upgrade_type" in inspect.getargspec(self.start).args:
        self.start(env, upgrade_type=upgrade_type)
      else:
        if is_stack_upgrade:
          self.start(env, rolling_restart=(upgrade_type == UPGRADE_TYPE_ROLLING))
        else:
          self.start(env)
      self.post_start(env)

      if is_rolling_restart:
        self.post_rolling_restart(env)

      if is_stack_upgrade:
        self.post_upgrade_restart(env, upgrade_type=upgrade_type)


    if self.should_expose_component_version("restart"):
      self.save_component_version_to_structured_out("restart")

  def post_upgrade_restart(self, env, upgrade_type=None):
    """
    To be overridden by subclasses
    """
    pass

  # TODO, remove after all services have switched to post_upgrade_restart
  def post_rolling_restart(self, env):
    """
    To be overridden by subclasses
    """
    # Mostly Actions are the same for both of these cases. If they are different this method should be overriden.
    self.post_upgrade_restart(env, UPGRADE_TYPE_ROLLING)

  def configure(self, env, upgrade_type=None, config_dir=None):
    """
    To be overridden by subclasses (may invoke save_configs)
    :param upgrade_type: only valid during RU/EU, otherwise will be None
    :param config_dir: for some clients during RU, the location to save configs to, otherwise None
    """
    self.fail_with_error('configure method isn\'t implemented')

  def save_configs(self, env):
    """
    To be overridden by subclasses
    Creates / updates configuration files
    """
    self.fail_with_error('save_configs method isn\'t implemented')

  def reconfigure(self, env):
    """
    Default implementation of RECONFIGURE action which may be overridden by subclasses
    """
    Logger.info("Refresh config files ...")
    self.save_configs(env)

    config = self.get_config()
    if "reconfigureAction" in config["commandParams"] and config["commandParams"]["reconfigureAction"] is not None:
      reconfigure_action = config["commandParams"]["reconfigureAction"]
      Logger.info("Call %s" % reconfigure_action)
      method = self.choose_method_to_execute(reconfigure_action)
      method(env)

  def generate_configs_get_template_file_content(self, filename, dicts):
    config = self.get_config()
    content = ''
    for dict in dicts.split(','):
      if dict.strip() in config['configurations']:
        try:
          content += config['configurations'][dict.strip()]['content']
        except Fail:
          # 'content' section not available in the component client configuration
          pass

    return content

  def generate_configs_get_xml_file_content(self, filename, dict):
    config = self.get_config()
    return {'configurations':config['configurations'][dict],
            'configuration_attributes':config['configurationAttributes'][dict]}

  def generate_configs_get_xml_file_dict(self, filename, dict):
    config = self.get_config()
    return config['configurations'][dict]

  def generate_configs(self, env):
    """
    Generates config files and stores them as an archive in tmp_dir
    based on xml_configs_list and env_configs_list from commandParams
    """
    import params
    env.set_params(params)

    config = self.get_config()

    xml_configs_list = config['commandParams']['xml_configs_list']
    env_configs_list = config['commandParams']['env_configs_list']
    properties_configs_list = config['commandParams']['properties_configs_list']

    Directory(self.get_tmp_dir(), create_parents = True)

    conf_tmp_dir = tempfile.mkdtemp(dir=self.get_tmp_dir())
    os.chmod(conf_tmp_dir, 0700)
    output_filename = os.path.join(self.get_tmp_dir(), config['commandParams']['output_file'])

    try:
      for file_dict in xml_configs_list:
        for filename, dict in file_dict.iteritems():
          XmlConfig(filename,
                    conf_dir=conf_tmp_dir,
                    mode=0644,
                    **self.generate_configs_get_xml_file_content(filename, dict)
          )
      for file_dict in env_configs_list:
        for filename,dicts in file_dict.iteritems():
          File(os.path.join(conf_tmp_dir, filename),
               mode=0644,
               content=InlineTemplate(self.generate_configs_get_template_file_content(filename, dicts)))

      for file_dict in properties_configs_list:
        for filename, dict in file_dict.iteritems():
          PropertiesFile(os.path.join(conf_tmp_dir, filename),
                         mode=0644,
                         properties=self.generate_configs_get_xml_file_dict(filename, dict)
          )
      with closing(tarfile.open(output_filename, "w:gz")) as tar:
        os.chmod(output_filename, 0600)
        try:
          tar.add(conf_tmp_dir, arcname=os.path.basename("."))
        finally:
          tar.close()

    finally:
      Directory(conf_tmp_dir, action="delete")

  @staticmethod
  def get_instance():
    if Script.instance is None:

      from resource_management.libraries.functions.default import default
      use_proxy = default("/agentLevelParams/agentConfigParams/agent/use_system_proxy_settings", True)
      if not use_proxy:
        reconfigure_urllib2_opener(ignore_system_proxy=True)

      Script.instance = Script()
    return Script.instance

  @staticmethod
  def get_upgrade_type(upgrade_type_command_param):
    upgrade_type = None
    if upgrade_type_command_param.lower() == "rolling_upgrade":
      upgrade_type = UPGRADE_TYPE_ROLLING
    elif upgrade_type_command_param.lower() == "nonrolling_upgrade":
      upgrade_type = UPGRADE_TYPE_NON_ROLLING
    elif upgrade_type_command_param.lower() == "host_ordered_upgrade":
      upgrade_type = UPGRADE_TYPE_HOST_ORDERED

    return upgrade_type


  def __init__(self):
    self.available_packages_in_repos = []
    if Script.instance is not None:
      raise Fail("An instantiation already exists! Use, get_instance() method.")
