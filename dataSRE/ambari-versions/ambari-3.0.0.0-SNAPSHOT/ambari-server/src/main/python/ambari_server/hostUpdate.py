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
import sys
import logging

from ambari_commons.exceptions import FatalException
from ambari_server import serverConfiguration
from ambari_server import dbConfiguration
from ambari_server import setupSecurity
from ambari_commons import os_utils
from ambari_server import userInput
from ambari_server import serverUtils
from ambari_server.serverConfiguration import configDefaults, get_java_exe_path, get_ambari_properties, read_ambari_user, \
                                              parse_properties_file, JDBC_DATABASE_PROPERTY
from ambari_commons.logging_utils import print_info_msg, print_warning_msg, print_error_msg
from ambari_server.dbConfiguration import ensure_jdbc_driver_is_installed, LINUX_DBMS_KEYS_LIST
from ambari_server.serverClassPath import ServerClassPath
from ambari_server.setupSecurity import ensure_can_start_under_current_user, generate_env
from ambari_commons.os_utils import run_os_command
from ambari_server.serverUtils import is_server_runing
from ambari_server.userInput import get_YN_input

logger = logging.getLogger(__name__)

HOST_UPDATE_HELPER_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.update.HostUpdateHelper {2}" + \
                            " > " + configDefaults.SERVER_OUT_FILE + " 2>&1"

def update_host_names(args, options):
  logger.info("Update host names.")
  services_stopped = userInput.get_YN_input("Please, confirm Ambari services are stopped [y/n] (n)? ", False)
  if not services_stopped:
    print 'Exiting...'
    sys.exit(1)

  pending_commands = userInput.get_YN_input("Please, confirm there are no pending commands on cluster [y/n] (n)? ", False)
  if not pending_commands:
    print 'Exiting...'
    sys.exit(1)

  db_backup_done = userInput.get_YN_input("Please, confirm you have made backup of the Ambari db [y/n] (n)? ", False)
  if not db_backup_done:
    print 'Exiting...'
    sys.exit(1)

  status, pid = serverUtils.is_server_runing()
  if status:
    raise FatalException(1, "Ambari Server should be stopped")

  try:
    host_mapping_file_path = args[1]
  except IndexError:
    #host_mapping file is mandatory
    raise FatalException(1, "Invalid number of host update arguments. Probably, you forgot to add json file with "
                            "host changes.")

  if not os.path.isfile(host_mapping_file_path):
    raise FatalException(1, "Invalid file path or file doesn't exist")

  if not os.access(host_mapping_file_path, os.R_OK):
    raise FatalException(1, "File is not readable")

  jdk_path = serverConfiguration.get_java_exe_path()

  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    sys.exit(1)

  properties = serverConfiguration.get_ambari_properties()
  serverConfiguration.parse_properties_file(options)
  options.database_index = LINUX_DBMS_KEYS_LIST.index(properties[JDBC_DATABASE_PROPERTY])

  dbConfiguration.ensure_jdbc_driver_is_installed(options, serverConfiguration.get_ambari_properties())

  serverClassPath = ServerClassPath(serverConfiguration.get_ambari_properties(), options)
  class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell()

  command = HOST_UPDATE_HELPER_CMD.format(jdk_path, class_path, host_mapping_file_path)

  ambari_user = serverConfiguration.read_ambari_user()
  current_user = setupSecurity.ensure_can_start_under_current_user(ambari_user)
  environ = setupSecurity.generate_env(options, ambari_user, current_user)

  (retcode, stdout, stderr) = os_utils.run_os_command(command, env=environ)
  print_info_msg("Return code from update host names command, retcode = " + str(retcode))

  if retcode > 0:
    print_error_msg("Error executing update host names, please check the server logs.")
    raise FatalException(1, 'Host names update failed.')
  else:
    print_info_msg('Host names update completed successfully')



