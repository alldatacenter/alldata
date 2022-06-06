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

import os
import socket
import time
import logging
import traceback
from resource_management.libraries.functions import hive_check
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

OK_MESSAGE = "TCP OK - {0:.3f}s response on port {1}"
CRITICAL_MESSAGE = "Connection failed on host {0}:{1} ({2})"

HIVE_SERVER_INTERACTIVE_THRIFT_PORT_KEY = '{{hive-interactive-site/hive.server2.thrift.port}}'
HIVE_SERVER_INTERACTIVE_THRIFT_HTTP_PORT_KEY = '{{hive-interactive-site/hive.server2.thrift.http.port}}'
HIVE_SERVER_INTERACTIVE_TRANSPORT_MODE_KEY = '{{hive-site/hive.server2.transport.mode}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
HIVE_SERVER2_INTERACTIVE_AUTHENTICATION_KEY = '{{hive-interactive-site/hive.server2.authentication}}'
HIVE_SERVER2_AUTHENTICATION_KEY = '{{hive-site/hive.server2.authentication}}'
HIVE_SERVER_INTERACTIVE_PRINCIPAL_KEY = '{{hive-site/hive.server2.authentication.kerberos.principal}}'
SMOKEUSER_KEYTAB_KEY = '{{cluster-env/smokeuser_keytab}}'
SMOKEUSER_PRINCIPAL_KEY = '{{cluster-env/smokeuser_principal_name}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
HIVE_SSL = '{{hive-site/hive.server2.use.SSL}}'
HIVE_SSL_KEYSTORE_PATH = '{{hive-site/hive.server2.keystore.path}}'
HIVE_SSL_KEYSTORE_PASSWORD = '{{hive-site/hive.server2.keystore.password}}'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'

THRIFT_PORT_DEFAULT = 10500
HIVE_SERVER_INTERACTIVE_TRANSPORT_MODE_DEFAULT = 'binary'
HIVE_SERVER_INTERACTIVE_PRINCIPAL_DEFAULT = 'hive/_HOST@EXAMPLE.COM'
HIVE_SERVER2_INTERACTIVE_AUTHENTICATION_DEFAULT = 'NOSASL'

# default keytab location
SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY = 'default.smoke.keytab'
SMOKEUSER_KEYTAB_DEFAULT = '/etc/security/keytabs/smokeuser.headless.keytab'

# default smoke principal
SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY = 'default.smoke.principal'
SMOKEUSER_PRINCIPAL_DEFAULT = 'ambari-qa@EXAMPLE.COM'

# default smoke user
SMOKEUSER_SCRIPT_PARAM_KEY = 'default.smoke.user'
SMOKEUSER_DEFAULT = 'ambari-qa'

HADOOPUSER_KEY = '{{cluster-env/hadoop.user.name}}'
HADOOPUSER_DEFAULT = 'hadoop'

CHECK_COMMAND_TIMEOUT_KEY = 'check.command.timeout'
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0

logger = logging.getLogger('ambari_alerts')

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (HIVE_SERVER_INTERACTIVE_THRIFT_PORT_KEY, SECURITY_ENABLED_KEY, SMOKEUSER_KEY,
          HIVE_SERVER2_INTERACTIVE_AUTHENTICATION_KEY, HIVE_SERVER2_AUTHENTICATION_KEY,
          HIVE_SERVER_INTERACTIVE_PRINCIPAL_KEY, SMOKEUSER_KEYTAB_KEY, SMOKEUSER_PRINCIPAL_KEY,
          HIVE_SERVER_INTERACTIVE_THRIFT_HTTP_PORT_KEY, HIVE_SERVER_INTERACTIVE_TRANSPORT_MODE_KEY,
          KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, HIVE_SSL, HIVE_SSL_KEYSTORE_PATH, HIVE_SSL_KEYSTORE_PASSWORD)


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def get_tokens():
  pass

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return ('UNKNOWN', ['There were no configurations supplied to the script.'])

  transport_mode = HIVE_SERVER_INTERACTIVE_TRANSPORT_MODE_DEFAULT
  if HIVE_SERVER_INTERACTIVE_TRANSPORT_MODE_KEY in configurations:
    transport_mode = configurations[HIVE_SERVER_INTERACTIVE_TRANSPORT_MODE_KEY]

  port = THRIFT_PORT_DEFAULT
  if transport_mode.lower() == 'binary' and HIVE_SERVER_INTERACTIVE_THRIFT_PORT_KEY in configurations:
    port = int(configurations[HIVE_SERVER_INTERACTIVE_THRIFT_PORT_KEY])
  elif transport_mode.lower() == 'http' and HIVE_SERVER_INTERACTIVE_THRIFT_HTTP_PORT_KEY in configurations:
    port = int(configurations[HIVE_SERVER_INTERACTIVE_THRIFT_HTTP_PORT_KEY])

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  check_command_timeout = CHECK_COMMAND_TIMEOUT_DEFAULT
  if CHECK_COMMAND_TIMEOUT_KEY in parameters:
    check_command_timeout = float(parameters[CHECK_COMMAND_TIMEOUT_KEY])

  hive_server2_authentication = HIVE_SERVER2_INTERACTIVE_AUTHENTICATION_DEFAULT
  if HIVE_SERVER2_INTERACTIVE_AUTHENTICATION_KEY in configurations:
    hive_server2_authentication = configurations[HIVE_SERVER2_INTERACTIVE_AUTHENTICATION_KEY]
  elif HIVE_SERVER2_AUTHENTICATION_KEY in configurations:
    hive_server2_authentication = configurations[HIVE_SERVER2_AUTHENTICATION_KEY]

  hive_ssl = False
  if HIVE_SSL in configurations:
    hive_ssl = configurations[HIVE_SSL]

  hive_ssl_keystore_path = None
  if HIVE_SSL_KEYSTORE_PATH in configurations:
    hive_ssl_keystore_path = configurations[HIVE_SSL_KEYSTORE_PATH]

  hive_ssl_keystore_password = None
  if HIVE_SSL_KEYSTORE_PASSWORD in configurations:
    hive_ssl_keystore_password = configurations[HIVE_SSL_KEYSTORE_PASSWORD]

  # defaults
  smokeuser_keytab = SMOKEUSER_KEYTAB_DEFAULT
  smokeuser_principal = SMOKEUSER_PRINCIPAL_DEFAULT
  smokeuser = SMOKEUSER_DEFAULT

  # check script params
  if SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY in parameters:
    smokeuser_principal = parameters[SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY]

  if SMOKEUSER_SCRIPT_PARAM_KEY in parameters:
    smokeuser = parameters[SMOKEUSER_SCRIPT_PARAM_KEY]

  if SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY in parameters:
    smokeuser_keytab = parameters[SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY]


  # check configurations last as they should always take precedence
  if SMOKEUSER_PRINCIPAL_KEY in configurations:
    smokeuser_principal = configurations[SMOKEUSER_PRINCIPAL_KEY]

  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  result_code = None

  if security_enabled:
    hive_server_principal = HIVE_SERVER_INTERACTIVE_PRINCIPAL_DEFAULT
    if HIVE_SERVER_INTERACTIVE_PRINCIPAL_KEY in configurations:
      hive_server_principal = configurations[HIVE_SERVER_INTERACTIVE_PRINCIPAL_KEY]

    if SMOKEUSER_KEYTAB_KEY in configurations:
      smokeuser_keytab = configurations[SMOKEUSER_KEYTAB_KEY]

    # Get the configured Kerberos executable search paths, if any
    if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
      kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
    else:
      kerberos_executable_search_paths = None

    kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
    kinitcmd=format("{kinit_path_local} -kt {smokeuser_keytab} {smokeuser_principal}; ")
  else:
    hive_server_principal = None
    kinitcmd=None

  try:
    if host_name is None:
      host_name = socket.getfqdn()

    start_time = time.time()

    try:
      hive_check.check_thrift_port_sasl(host_name, port, hive_server2_authentication, hive_server_principal,
                                        kinitcmd, smokeuser, transport_mode=transport_mode, ssl=hive_ssl,
                                        ssl_keystore=hive_ssl_keystore_path, ssl_password=hive_ssl_keystore_password,
                                        check_command_timeout=int(check_command_timeout))
      result_code = 'OK'
      total_time = time.time() - start_time
      label = OK_MESSAGE.format(total_time, port)
    except:
      result_code = 'CRITICAL'
      label = CRITICAL_MESSAGE.format(host_name, port, traceback.format_exc())

  except:
    label = traceback.format_exc()
    result_code = 'UNKNOWN'

  return (result_code, [label])


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def execute(configurations={}, parameters={}, host_name=None):
  pass