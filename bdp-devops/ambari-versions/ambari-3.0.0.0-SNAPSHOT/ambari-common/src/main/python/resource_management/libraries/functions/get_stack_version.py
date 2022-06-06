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

Ambari Agent

"""
__all__ = ["get_stack_version"]

import os
import re

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core import shell
from resource_management.libraries.functions import stack_tools


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def get_stack_version(package_name):
  """
  @param package_name, name of the package, from which, function will try to get stack version
  """
  try:
    component_home_dir = os.environ[package_name.upper() + "_HOME"]
  except KeyError:
    Logger.info('Skipping get_stack_version since the component {0} is not yet available'.format(package_name))
    return None # lazy fail

  #As a rule, component_home_dir is of the form <stack_root_dir>\[\]<component_versioned_subdir>[\]
  home_dir_split = os.path.split(component_home_dir)
  iSubdir = len(home_dir_split) - 1
  while not home_dir_split[iSubdir]:
    iSubdir -= 1

  #The component subdir is expected to be of the form <package_name>-<package_version>.<stack_version>
  # with package_version = #.#.# and stack_version=#.#.#.#-<build_number>
  match = re.findall('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', home_dir_split[iSubdir])
  if not match:
    Logger.info('Failed to get extracted version for component {0}. Home dir not in expected format.'.format(package_name))
    return None # lazy fail

  return match[0]

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def get_stack_version(package_name):
  """
  @param package_name, name of the package, from which, function will try to get stack version
  """

  stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)

  if not os.path.exists(stack_selector_path):
    Logger.info('Skipping get_stack_version since ' + stack_selector_path + ' is not yet available')
    return None # lazy fail
  
  try:
    command = 'ambari-python-wrap {stack_selector_path} status {package_name}'.format(
            stack_selector_path=stack_selector_path, package_name=package_name)
    return_code, stack_output = shell.call(command, timeout=20)
  except Exception, e:
    Logger.error(str(e))
    raise Fail('Unable to execute ' + stack_selector_path + ' command to retrieve the version.')

  if return_code != 0:
    raise Fail(
      'Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

  stack_version = re.sub(package_name + ' - ', '', stack_output)
  stack_version = stack_version.rstrip()
  match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+(-[0-9]+)?', stack_version)

  if match is None:
    Logger.info('Failed to get extracted version with ' + stack_selector_path)
    return None # lazy fail

  return stack_version
