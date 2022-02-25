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

# Python Imports
import os
import sys
import re
import ambari_simplejson as json

# Local Imports
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import component_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_tools
from resource_management.core import shell
from resource_management.core import sudo
from resource_management.core.shell import call
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.version_select_util import get_versions_from_stack_root
from resource_management.libraries.functions import stack_features
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import upgrade_summary

STACK_SELECT_PREFIX = 'ambari-python-wrap'

# mapping of service check to <stack-selector-tool> component
SERVICE_CHECK_DIRECTORY_MAP = {
  "HDFS_SERVICE_CHECK" : "hadoop-client",
  "TEZ_SERVICE_CHECK" : "hadoop-client",
  "PIG_SERVICE_CHECK" : "hadoop-client",
  "HIVE_SERVICE_CHECK" : "hadoop-client",
  "OOZIE_SERVICE_CHECK" : "hadoop-client",
  "MAHOUT_SERVICE_CHECK" : "mahout-client",
  "MAPREDUCE2_SERVICE_CHECK" : "hadoop-client",
  "YARN_SERVICE_CHECK" : "hadoop-client"
}

# <stack-root>/current/hadoop-client/[bin|sbin|libexec|lib]
# <stack-root>/2.3.0.0-1234/hadoop/[bin|sbin|libexec|lib]
HADOOP_DIR_TEMPLATE = "{0}/{1}/{2}/{3}"

# <stack-root>/current/hadoop-client
# <stack-root>/2.3.0.0-1234/hadoop
HADOOP_HOME_DIR_TEMPLATE = "{0}/{1}/{2}"

HADOOP_DIR_DEFAULTS = {
  "home": "/usr/lib/hadoop",
  "libexec": "/usr/lib/hadoop/libexec",
  "sbin": "/usr/lib/hadoop/sbin",
  "bin": "/usr/bin",
  "lib": "/usr/lib/hadoop/lib",
  "conf": "/etc/hadoop/conf"
}

PACKAGE_SCOPE_INSTALL = "INSTALL"
PACKAGE_SCOPE_STANDARD = "STANDARD"
PACKAGE_SCOPE_PATCH = "PATCH"
PACKAGE_SCOPE_STACK_SELECT = "STACK-SELECT-PACKAGE"

# the legacy key is used when a package has changed from one version of the stack select tool to another.
_PACKAGE_SCOPE_LEGACY = "LEGACY"

# the valid scopes which can be requested
_PACKAGE_SCOPES = (PACKAGE_SCOPE_INSTALL, PACKAGE_SCOPE_STANDARD, PACKAGE_SCOPE_PATCH, PACKAGE_SCOPE_STACK_SELECT)

# the orchestration types which equal to a partial (non-STANDARD) upgrade
_PARTIAL_ORCHESTRATION_SCOPES = ("PATCH", "MAINT")

def get_package_name(default_package = None):
  """
  Gets the stack-select package name for the service name and
  component from the current command. Not all services/components are used with the
  stack-select tools, so those will return no packages.

  :return:  the stack-select package name for the command's component or None
  """
  config = Script.get_config()

  if 'role' not in config or 'serviceName' not in config:
    raise Fail("Both the role and the service name must be included in the command in order to determine which packages to use with the stack-select tool")

  service_name = config['serviceName']
  component_name = config['role']

  # should return a single item
  try:
    package = get_packages(PACKAGE_SCOPE_STACK_SELECT, service_name, component_name)
    if package is None:
      package = default_package

    return package
  except:
    if default_package is not None:
      return default_package
    else:
      raise


def is_package_supported(package, supported_packages = None):
  """
  Gets whether the specified package is supported by the <stack_select> tool.
  :param package: the package to check
  :param supported_packages: the list of supported packages pre-fetched
  :return: True if the package is support, False otherwise
  """
  if supported_packages is None:
    supported_packages = get_supported_packages()

  if package in supported_packages:
    return True

  return False


def get_supported_packages():
  """
  Parses the output from <stack-select> packages and returns an array of the various packages.
  :return: and array of packages support by <stack-select>
  """
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  command = (STACK_SELECT_PREFIX, stack_selector_path, "packages")
  code, stdout = shell.call(command, sudo = True,  quiet = True)

  if code != 0 or stdout is None:
    raise Fail("Unable to query for supported packages using {0}".format(stack_selector_path))

  # turn the output into lines, stripping each line
  return [line.strip() for line in stdout.splitlines()]


def get_packages(scope, service_name = None, component_name = None):
  """
  Gets the packages which should be used with the stack's stack-select tool for the
  specified service/component. Not all services/components are used with the stack-select tools,
  so those will return no packages.

  :param scope: the scope of the command
  :param service_name:  the service name, such as ZOOKEEPER
  :param component_name: the component name, such as ZOOKEEPER_SERVER
  :return:  the packages to use with stack-select or None
  """
  from resource_management.libraries.functions.default import default

  if scope not in _PACKAGE_SCOPES:
    raise Fail("The specified scope of {0} is not valid".format(scope))

  config = Script.get_config()

  if service_name is None or component_name is None:
    if 'role' not in config or 'serviceName' not in config:
      raise Fail("Both the role and the service name must be included in the command in order to determine which packages to use with the stack-select tool")

    service_name = config['serviceName']
    component_name = config['role']


  stack_name = default("/clusterLevelParams/stack_name", None)
  if stack_name is None:
    raise Fail("The stack name is not present in the command. Packages for stack-select tool cannot be loaded.")

  stack_packages_config = default("/configurations/cluster-env/stack_packages", None)
  if stack_packages_config is None:
    raise Fail("The stack packages are not defined on the command. Unable to load packages for the stack-select tool")

  data = json.loads(stack_packages_config)

  if stack_name not in data:
    raise Fail(
      "Cannot find stack-select packages for the {0} stack".format(stack_name))

  stack_select_key = "stack-select"
  data = data[stack_name]
  if stack_select_key not in data:
    raise Fail(
      "There are no stack-select packages defined for this command for the {0} stack".format(stack_name))

  # this should now be the dictionary of role name to package name
  data = data[stack_select_key]
  service_name = service_name.upper()
  component_name = component_name.upper()

  if service_name not in data:
    Logger.info("Skipping stack-select on {0} because it does not exist in the stack-select package structure.".format(service_name))
    return None

  data = data[service_name]

  if component_name not in data:
    Logger.info("Skipping stack-select on {0} because it does not exist in the stack-select package structure.".format(component_name))
    return None

  # this one scope is not an array, so transform it into one for now so we can
  # use the same code below
  packages = data[component_name][scope]
  if scope == PACKAGE_SCOPE_STACK_SELECT:
    packages = [packages]

  # grab the package name from the JSON and validate it against the packages
  # that the stack-select tool supports - if it doesn't support it, then try to find the legacy
  # package name if it exists
  supported_packages = get_supported_packages()
  for index, package in enumerate(packages):
    if not is_package_supported(package, supported_packages=supported_packages):
      if _PACKAGE_SCOPE_LEGACY in data[component_name]:
        legacy_package = data[component_name][_PACKAGE_SCOPE_LEGACY]
        Logger.info(
          "The package {0} is not supported by this version of the stack-select tool, defaulting to the legacy package of {1}".format(package, legacy_package))

        # use the legacy package
        packages[index] = legacy_package
      else:
        raise Fail("The package {0} is not supported by this version of the stack-select tool.".format(package))

  # transform the array bcak to a single element
  if scope == PACKAGE_SCOPE_STACK_SELECT:
    packages = packages[0]

  return packages


def select_all(version_to_select):
  """
  Executes <stack-selector-tool> on every component for the specified version. If the value passed in is a
  stack version such as "2.3", then this will find the latest installed version which
  could be "2.3.0.0-9999". If a version is specified instead, such as 2.3.0.0-1234, it will use
  that exact version.
  :param version_to_select: the version to <stack-selector-tool> on, such as "2.3" or "2.3.0.0-1234"
  """
  stack_root = Script.get_stack_root()
  (stack_selector_name, stack_selector_path, stack_selector_package) = stack_tools.get_stack_tool(stack_tools.STACK_SELECTOR_NAME)
  # it's an error, but it shouldn't really stop anything from working
  if version_to_select is None:
    Logger.error(format("Unable to execute {stack_selector_name} after installing because there was no version specified"))
    return

  Logger.info("Executing {0} set all on {1}".format(stack_selector_name, version_to_select))

  command = format('{sudo} {stack_selector_path} set all `ambari-python-wrap {stack_selector_path} versions | grep ^{version_to_select} | tail -1`')
  only_if_command = format('ls -d {stack_root}/{version_to_select}*')
  Execute(command, only_if = only_if_command)


def select_packages(version):
  """
  Uses the command's service and role to determine the stack-select packages which need to be invoked.
  If in an upgrade, then the upgrade summary's orchestration is used to determine which packages
  to install.
  :param version: the version to select
  :return: None
  """
  package_scope = PACKAGE_SCOPE_STANDARD
  orchestration = package_scope
  summary = upgrade_summary.get_upgrade_summary()

  if summary is not None:
    orchestration = summary.orchestration
    if orchestration is None:
      raise Fail("The upgrade summary for does not contain an orchestration type")

    # if the orchestration is patch or maint, use the "patch" key from the package JSON
    if orchestration.upper() in _PARTIAL_ORCHESTRATION_SCOPES:
      package_scope = PACKAGE_SCOPE_PATCH

  stack_select_packages = get_packages(package_scope)
  if stack_select_packages is None:
    return

  Logger.info("The following packages will be stack-selected to version {0} using a {1} orchestration and {2} scope: {3}".format(
    version, orchestration.upper(), package_scope, ", ".join(stack_select_packages)))

  for stack_select_package_name in stack_select_packages:
    select(stack_select_package_name, version)


def select(component, version):
  """
  Executes <stack-selector-tool> on the specific component and version. Some global
  variables that are imported via params/status_params/params_linux will need
  to be recalcuated after the <stack-selector-tool>. However, python does not re-import
  existing modules. The only way to ensure that the configuration variables are
  recalculated is to call reload(...) on each module that has global parameters.
  After invoking <stack-selector-tool>, this function will also reload params, status_params,
  and params_linux.
  :param component: the <stack-selector-tool> component, such as oozie-server. If "all", then all components
  will be updated.
  :param version: the version to set the component to, such as 2.2.0.0-1234
  """
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  command = (STACK_SELECT_PREFIX, stack_selector_path, "set", component, version)
  Execute(command, sudo=True)

  # don't trust the ordering of modules:
  # 1) status_params
  # 2) params_linux
  # 3) params
  modules = sys.modules
  param_modules = "status_params", "params_linux", "params"
  for moduleName in param_modules:
    if moduleName in modules:
      module = modules.get(moduleName)
      reload(module)
      Logger.info("After {0}, reloaded module {1}".format(command, moduleName))


def get_role_component_current_stack_version():
  """
  Gets the current HDP version of the component that this role command is for.
  :return:  the current HDP version of the specified component or None
  """
  role = default("/role", "")
  role_command =  default("/roleCommand", "")

  stack_selector_name = stack_tools.get_stack_tool_name(stack_tools.STACK_SELECTOR_NAME)
  Logger.info("Checking version for {0} via {1}".format(role, stack_selector_name))
  if role_command == "SERVICE_CHECK" and role in SERVICE_CHECK_DIRECTORY_MAP:
    stack_select_component = SERVICE_CHECK_DIRECTORY_MAP[role]
  else:
    stack_select_component = get_package_name()

  if stack_select_component is None:
    if not role:
      Logger.error("No role information available.")
    elif not role.lower().endswith("client"):
      Logger.error("Mapping unavailable for role {0}. Skip checking its version.".format(role))
    return None

  current_stack_version = get_stack_version(stack_select_component)

  if current_stack_version is None:
    Logger.warning("Unable to determine {0} version for {1}".format(
      stack_selector_name, stack_select_component))
  else:
    Logger.info("{0} is currently at version {1}".format(
      stack_select_component, current_stack_version))

  return current_stack_version


def get_hadoop_dir(target):
  """
  Return the hadoop shared directory which should be used for the command's component. The
  directory including the component's version is tried first, but if that doesn't exist,
  this will fallback to using "current".

  :target: the target directory
  """
  stack_root = Script.get_stack_root()
  stack_version = Script.get_stack_version()

  if not target in HADOOP_DIR_DEFAULTS:
    raise Fail("Target {0} not defined".format(target))

  hadoop_dir = HADOOP_DIR_DEFAULTS[target]

  formatted_stack_version = format_stack_version(stack_version)

  if stack_features.check_stack_feature(StackFeature.ROLLING_UPGRADE, formatted_stack_version):
    # read the desired version from the component map and use that for building the hadoop home
    version = component_version.get_component_repository_version()
    if version is None:
      version = default("/commandParams/version", None)

    # home uses a different template
    if target == "home":
      hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, version, "hadoop")
      if version is None or sudo.path_isdir(hadoop_dir) is False:
        hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, "current", "hadoop-client")
    else:
      hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_root, version, "hadoop", target)
      if version is None or sudo.path_isdir(hadoop_dir) is False:
        hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_root, "current", "hadoop-client", target)

  return hadoop_dir


def get_hadoop_dir_for_stack_version(target, stack_version):
  """
  Return the hadoop shared directory for the provided stack version. This is necessary
  when folder paths of downgrade-source stack-version are needed after <stack-selector-tool>.
  :target: the target directory
  :stack_version: stack version to get hadoop dir for
  """

  stack_root = Script.get_stack_root()
  if not target in HADOOP_DIR_DEFAULTS:
    raise Fail("Target {0} not defined".format(target))

  # home uses a different template
  if target == "home":
    hadoop_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, stack_version, "hadoop")
  else:
    hadoop_dir = HADOOP_DIR_TEMPLATE.format(stack_root, stack_version, "hadoop", target)

  return hadoop_dir


def _get_upgrade_stack():
  """
  Gets the stack name and stack version if an upgrade is currently in progress.
  :return:  the stack name and stack version as a tuple, or None if an
  upgrade is not in progress.
  """
  from resource_management.libraries.functions.default import default
  direction = default("/commandParams/upgrade_direction", None)
  stack_name = default("/clusterLevelParams/stack_name", None)
  stack_version = default("/commandParams/version", None)

  if direction and stack_name and stack_version:
    return (stack_name, stack_version)

  return None

def unsafe_get_stack_versions():
  """
  Gets list of stack versions installed on the host.
  By default a call to <stack-selector-tool> versions is made to get the list of installed stack versions.
  DO NOT use a fall-back since this function is called by alerts in order to find potential errors.
  :return: Returns a tuple of (exit code, output, list of installed stack versions).
  """
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  code, out = call((STACK_SELECT_PREFIX, stack_selector_path, 'versions'))
  versions = []
  if 0 == code:
    for line in out.splitlines():
      versions.append(line.rstrip('\n'))
  return (code, out, versions)

def get_stack_versions(stack_root):
  """
  Gets list of stack versions installed on the host.
  By default a call to <stack-selector-tool> versions is made to get the list of installed stack versions.
  As a fallback list of installed versions is collected from stack version directories in stack install root.
  :param stack_root: Stack install root
  :return: Returns list of installed stack versions.
  """
  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)
  code, out = call((STACK_SELECT_PREFIX, stack_selector_path, 'versions'))
  versions = []
  if 0 == code:
    for line in out.splitlines():
      versions.append(line.rstrip('\n'))
  if not versions:
    versions = get_versions_from_stack_root(stack_root)
  return versions

def get_stack_version_before_install(component_name):
  """
  Works in the similar way to '<stack-selector-tool> status component',
  but also works for not yet installed packages.
  
  Note: won't work if doing initial install.
  """
  stack_root = Script.get_stack_root()
  component_dir = HADOOP_HOME_DIR_TEMPLATE.format(stack_root, "current", component_name)
  stack_selector_name = stack_tools.get_stack_tool_name(stack_tools.STACK_SELECTOR_NAME)
  if os.path.islink(component_dir):
    stack_version = os.path.basename(os.path.dirname(os.readlink(component_dir)))
    match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', stack_version)
    if match is None:
      Logger.info('Failed to get extracted version with {0} in method get_stack_version_before_install'.format(stack_selector_name))
      return None # lazy fail
    return stack_version
  else:
    return None
