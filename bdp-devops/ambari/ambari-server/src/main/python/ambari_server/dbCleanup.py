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

from ambari_commons.logging_utils import print_info_msg, print_error_msg
from ambari_commons.os_utils import run_os_command
from ambari_server.dbConfiguration import ensure_jdbc_driver_is_installed
from ambari_server.serverConfiguration import configDefaults, \
    get_ambari_properties, get_java_exe_path, read_ambari_user, get_db_type
from ambari_server.setupSecurity import generate_env, ensure_can_start_under_current_user
from ambari_server.userInput import get_YN_input
from ambari_server.serverClassPath import ServerClassPath
from ambari_server.serverUtils import is_server_runing
import datetime
import logging

logger = logging.getLogger(__name__)

DB_CLEANUP_CMD = "{0} -cp {1} org.apache.ambari.server.cleanup.CleanupDriver --cluster-name {2} --from-date {3}> " + configDefaults.SERVER_OUT_FILE + " 2>&1"

#
# Run the db cleanup process
#
def run_db_purge(options):

    if validate_args(options):
        return 1

    status, stateDesc = is_server_runing()

    if not options.silent:
      db_title = get_db_type(get_ambari_properties()).title

      confirmBackup = get_YN_input("Ambari Server configured for {0}. Confirm you have made a backup of the Ambari Server database [y/n]".format(
              db_title), True)
      if not confirmBackup:
          print_info_msg("Ambari Server Database purge aborted")
          return 0

      if status:
          print_error_msg("The database purge historical data cannot proceed while Ambari Server is running. Please shut down Ambari first.")
          return 1

      confirm = get_YN_input(
          "Ambari server is using db type {0}. Cleanable database entries older than {1} will be purged. Proceed [y/n]".format(
              db_title, options.purge_from_date), True)
      if not confirm:
          print_info_msg("Ambari Server Database purge aborted")
          return 0



    jdk_path = get_java_exe_path()
    if jdk_path is None:
        print_error_msg("No JDK found, please run the \"setup\" command to install a JDK automatically or install any "
                        "JDK manually to {0}".format(configDefaults.JDK_INSTALL_DIR));
        return 1

    ensure_jdbc_driver_is_installed(options, get_ambari_properties())

    serverClassPath = ServerClassPath(get_ambari_properties(), options)
    class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell()

    ambari_user = read_ambari_user()
    current_user = ensure_can_start_under_current_user(ambari_user)
    environ = generate_env(options, ambari_user, current_user)

    print "Purging historical data from the database ..."
    command = DB_CLEANUP_CMD.format(jdk_path, class_path, options.cluster_name, options.purge_from_date)
    (retcode, stdout, stderr) = run_os_command(command, env=environ)

    print_info_msg("Return code from database cleanup command, retcode = " + str(retcode))

    if stdout:
        print "Console output from database purge-history command:"
        print stdout
        print
    if stderr:
        print "Error output from database purge-history command:"
        print stderr
        print
    if retcode > 0:
        print_error_msg("Error encountered while purging the Ambari Server Database. Check the ambari-server.log for details.")
    else:
        print "Purging historical data completed. Check the ambari-server.log for details."
    return retcode

#
# Database purge
#
def db_purge(options):
    return run_db_purge(options)


def validate_args(options):
    if not options.cluster_name:
        print_error_msg("Please provide the --cluster-name argument.")
        return 1

    if not options.purge_from_date:
        print_error_msg("Please provide the --from-date argument.")
        return 1

    try:
        datetime.datetime.strptime(options.purge_from_date, "%Y-%m-%d")
    except ValueError as e:
        print_error_msg("The --from-date argument has an invalid format. {0}".format(e.args[0]))
        return 1;
