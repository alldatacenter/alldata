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

import platform

from resource_management.libraries.functions.default import *
from resource_management.libraries.functions.format import *
from resource_management.libraries.functions.find_path import *
from resource_management.libraries.functions.find_executable import *
from resource_management.libraries.functions.get_kinit_path import *
from resource_management.libraries.functions.get_kdestroy_path import *
from resource_management.libraries.functions.get_klist_path import *
from resource_management.libraries.functions.get_unique_id_and_date import *
from resource_management.libraries.functions.check_process_status import *
from resource_management.libraries.functions.is_empty import *
from resource_management.libraries.functions.substitute_vars import *
from resource_management.libraries.functions.get_port_from_url import *
from resource_management.libraries.functions.hive_check import *
from resource_management.libraries.functions.version import *
from resource_management.libraries.functions.format_jvm_option import *
from resource_management.libraries.functions.constants import *
from resource_management.libraries.functions.get_stack_version import *
from resource_management.libraries.functions.lzo_utils import *
from resource_management.libraries.functions.setup_ranger_plugin import *
from resource_management.libraries.functions.curl_krb_request import *
from resource_management.libraries.functions.get_bare_principal import *
from resource_management.libraries.functions.get_path_from_url import *
from resource_management.libraries.functions.show_logs import *
from resource_management.libraries.functions.log_process_information import *

IS_WINDOWS = platform.system() == "Windows"

if IS_WINDOWS:
  from resource_management.libraries.functions.windows_service_utils import *
  from resource_management.libraries.functions.install_stack_msi import *
  from resource_management.libraries.functions.install_jdbc_driver import *
  from resource_management.libraries.functions.reload_windows_env import *
