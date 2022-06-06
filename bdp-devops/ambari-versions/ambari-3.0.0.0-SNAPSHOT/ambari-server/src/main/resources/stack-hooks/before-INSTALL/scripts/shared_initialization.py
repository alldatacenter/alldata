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

from resource_management.libraries.functions import stack_tools
from resource_management.libraries.functions.version import compare_versions
from resource_management.core.resources.packaging import Package

def install_packages():
  import params
  if params.host_sys_prepped:
    return

  packages = ['unzip', 'curl']
  if params.stack_version_formatted != "" and compare_versions(params.stack_version_formatted, '2.2') >= 0:
    stack_selector_package = stack_tools.get_stack_tool_package(stack_tools.STACK_SELECTOR_NAME)
    packages.append(stack_selector_package)
  Package(packages,
          retry_on_repo_unavailability=params.agent_stack_retry_on_unavailability,
          retry_count=params.agent_stack_retry_count)
