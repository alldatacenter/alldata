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
from ambari_commons import subprocess32
import sys
import logging
import time

from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import get_debug_mode, print_warning_msg, print_info_msg, set_debug_mode_from_options
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.os_utils import is_root
from ambari_server.ambariPath import AmbariPath
from ambari_server.dbConfiguration import ensure_dbms_is_running, ensure_jdbc_driver_is_installed
from ambari_server.serverConfiguration import configDefaults, find_jdk, get_ambari_properties, \
  get_java_exe_path, read_ambari_user, \
  get_is_active_instance, update_properties, get_ambari_server_ui_port, PID_NAME, \
  check_database_name_property, parse_properties_file, get_missing_properties

from ambari_server.serverConfiguration import get_web_server_startup_timeout
from ambari_server.serverUtils import refresh_stack_hash
from ambari_server.setupHttps import get_fqdn
from ambari_server.setupSecurity import generate_env, ensure_can_start_under_current_user
from ambari_server.utils import check_reverse_lookup, save_pid, locate_file, locate_all_file_paths, looking_for_pid, \
  save_main_pid_ex, check_exitcode, get_live_pids_count, wait_for_ui_start
from ambari_server.serverClassPath import ServerClassPath

logger = logging.getLogger(__name__)

# debug settings
SERVER_START_DEBUG = False
SUSPEND_START_MODE = False

# server commands
ambari_provider_module_option = ""
ambari_provider_module = os.environ.get('AMBARI_PROVIDER_MODULE')
if ambari_provider_module is not None:
  ambari_provider_module_option = "-Dprovider.module.class=" + \
                                  ambari_provider_module + " "

jvm_args = os.getenv('AMBARI_JVM_ARGS', '-Xms512m -Xmx2048m -XX:MaxPermSize=128m')

ENV_FOREGROUND_KEY = "AMBARI_SERVER_RUN_IN_FOREGROUND"
CHECK_DATABASE_HELPER_CMD = "{0} -cp {1} org.apache.ambari.server.checks.DatabaseConsistencyChecker"
IS_FOREGROUND = ENV_FOREGROUND_KEY in os.environ and os.environ[ENV_FOREGROUND_KEY].lower() == "true"

SERVER_START_CMD = "{0} " \
    "-server -XX:NewRatio=3 " \
    "-XX:+UseConcMarkSweepGC " + \
    "-XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60 " \
    "-XX:+CMSClassUnloadingEnabled " \
    "-Dsun.zip.disableMemoryMapping=true " + \
    "{1} {2} " \
    "-cp {3} "\
    "org.apache.ambari.server.controller.AmbariServer " \
    "> {4} 2>&1 || echo $? > {5}"
SERVER_START_CMD_DEBUG = "{0} " \
    "-server -XX:NewRatio=2 " \
    "-XX:+UseConcMarkSweepGC " + \
    "{1} {2} " \
    " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005," \
    "server=y,suspend={6} " \
    "-cp {3} " + \
    "org.apache.ambari.server.controller.AmbariServer " \
    "> {4} 2>&1 || echo $? > {5}"
    
if not IS_FOREGROUND:
  SERVER_START_CMD += " &"
  SERVER_START_CMD_DEBUG += " &"

SERVER_START_CMD_WINDOWS = "{0} " \
    "-server -XX:NewRatio=3 " \
    "-XX:+UseConcMarkSweepGC " + \
    "-XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60 " \
    "-XX:+CMSClassUnloadingEnabled " \
    "{1} {2} " \
    "-cp {3} " \
    "org.apache.ambari.server.controller.AmbariServer"
SERVER_START_CMD_DEBUG_WINDOWS = "{0} " \
    "-server -XX:NewRatio=2 " \
    "-XX:+UseConcMarkSweepGC " \
    "{1} {2} " \
    "-Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend={4} " \
    "-cp {3} " \
    "org.apache.ambari.server.controller.AmbariServer"

SERVER_START_TIMEOUT = 5  #seconds
SERVER_START_RETRIES = 4

SERVER_PING_TIMEOUT_WINDOWS = 5
SERVER_PING_ATTEMPTS_WINDOWS = 4

SERVER_SEARCH_PATTERN = "org.apache.ambari.server.controller.AmbariServer"

EXITCODE_NAME = "ambari-server.exitcode"

CHECK_DATABASE_SKIPPED_PROPERTY = "check_database_skipped"

AMBARI_SERVER_DIE_MSG = "Ambari Server java process died with exitcode {0}. Check {1} for more information."
AMBARI_SERVER_NOT_STARTED_MSG = "Ambari Server java process hasn't been started or can't be determined."
AMBARI_SERVER_STOPPED = "Ambari Server java process has stopped. Please check the logs for more information."
AMBARI_SERVER_UI_TIMEOUT = "Server not yet listening on http port {0} after {1} seconds. Exiting."
AMBARI_SERVER_STARTED_SUCCESS_MSG = "Ambari Server has started successfully"

# linux open-file limit
ULIMIT_OPEN_FILES_KEY = 'ulimit.open.files'
ULIMIT_OPEN_FILES_DEFAULT = 65536

AMBARI_ENV_FILE = AmbariPath.get("/var/lib/ambari-server/ambari-env.sh")

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def ensure_server_security_is_configured():
  pass

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def ensure_server_security_is_configured():
  if not is_root():
    print "Unable to check firewall status when starting without root privileges."
    print "Please do not forget to disable or adjust firewall if needed"


def get_ulimit_open_files(properties):
  open_files_val = properties[ULIMIT_OPEN_FILES_KEY]
  open_files = int(open_files_val) if (open_files_val and int(open_files_val) > 0) else ULIMIT_OPEN_FILES_DEFAULT
  return open_files

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def generate_child_process_param_list(ambari_user, java_exe, class_path, debug_start, suspend_mode):
  conf_dir = class_path
  if class_path.find(' ') != -1:
    conf_dir = '"' + class_path + '"'
  command_base = SERVER_START_CMD_DEBUG_WINDOWS if debug_start else SERVER_START_CMD_WINDOWS
  command = command_base.format(
      java_exe,
      ambari_provider_module_option,
      jvm_args,
      conf_dir,
      suspend_mode)
  return command

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def generate_child_process_param_list(ambari_user, java_exe, class_path,
                                      debug_start, suspend_mode):
  from ambari_commons.os_linux import ULIMIT_CMD

  properties = get_ambari_properties()

  command_base = SERVER_START_CMD_DEBUG if debug_start else SERVER_START_CMD

  ulimit_cmd = "%s %s" % (ULIMIT_CMD, str(get_ulimit_open_files(properties)))
  command = command_base.format(java_exe,
          ambari_provider_module_option,
          jvm_args,
          class_path,
          configDefaults.SERVER_OUT_FILE,
          os.path.join(configDefaults.PID_DIR, EXITCODE_NAME),
          suspend_mode)

  # required to start properly server instance
  os.chdir(configDefaults.ROOT_FS_PATH)

  #For properly daemonization server should be started using shell as parent
  param_list = [locate_file('sh', '/bin'), "-c"]
  if is_root() and ambari_user != "root":
    # To inherit exported environment variables (especially AMBARI_PASSPHRASE),
    # from subprocess32, we have to skip --login option of su command. That's why
    # we change dir to / (otherwise subprocess32 can face with 'permission denied'
    # errors while trying to list current directory
    cmd = "{ulimit_cmd} ; {su} {ambari_user} -s {sh_shell} -c '. {ambari_env_file} && {command}'".format(ulimit_cmd=ulimit_cmd,
                                                                                su=locate_file('su', '/bin'), ambari_user=ambari_user,
                                                                                sh_shell=locate_file('sh', '/bin'), command=command,
                                                                                ambari_env_file=AMBARI_ENV_FILE)
  else:
    cmd = "{ulimit_cmd} ; {command}".format(ulimit_cmd=ulimit_cmd, command=command)
    
  param_list.append(cmd)
  return param_list

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def wait_for_server_start(pidFile, scmStatus):
  # Wait for the HTTP port to be open
  iter_start = 0
  while iter_start < SERVER_PING_ATTEMPTS_WINDOWS and not get_fqdn(SERVER_PING_TIMEOUT_WINDOWS):
    if scmStatus is not None:
      scmStatus.reportStartPending()
    iter_start += 1

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def wait_for_server_start(pidFile, scmStatus):
  properties = get_ambari_properties()
  if properties == -1:
    err ="Error getting ambari properties"
    raise FatalException(-1, err)

  #wait for server process for SERVER_START_TIMEOUT seconds
  sys.stdout.write('Waiting for server start...')
  sys.stdout.flush()
  pids = []
  pid = None
  # looking_for_pid() might return partrial pid list on slow hardware
  for i in range(1, SERVER_START_RETRIES):
    pids = looking_for_pid(SERVER_SEARCH_PATTERN, SERVER_START_TIMEOUT)
    pid = save_main_pid_ex(pids, pidFile, locate_all_file_paths('sh', '/bin') +
                           locate_all_file_paths('bash', '/bin') +
                           locate_all_file_paths('dash', '/bin'), IS_FOREGROUND)
    if pid:
      break
    else:
      sys.stdout.write("Unable to determine server PID. Retrying...\n")
      sys.stdout.flush()

  exception = None
  if pid:
    ambari_server_ui_port = get_ambari_server_ui_port(properties)
    web_server_startup_timeout = get_web_server_startup_timeout(properties)
    waitStart = time.time()
    if not wait_for_ui_start(int(ambari_server_ui_port), pid, web_server_startup_timeout):
      waitTime = int(time.time()-waitStart)
      # Java process stopped, due to a DB check or other startup issue
      if waitTime < web_server_startup_timeout:
        exception = FatalException(-1, AMBARI_SERVER_STOPPED)
      # UI didn't come up on time
      else:
        exception = FatalException(1, AMBARI_SERVER_UI_TIMEOUT.format(ambari_server_ui_port, web_server_startup_timeout))
  elif get_live_pids_count(pids) <= 0:
    exitcode = check_exitcode(os.path.join(configDefaults.PID_DIR, EXITCODE_NAME))
    exception = FatalException(-1, AMBARI_SERVER_DIE_MSG.format(exitcode, configDefaults.SERVER_OUT_FILE))
  else:
    exception = FatalException(-1, AMBARI_SERVER_NOT_STARTED_MSG)

  if os.path.isfile(configDefaults.SERVER_OUT_FILE):
    if 'DB_CHECK_ERROR' in open(configDefaults.SERVER_OUT_FILE).read():
      print "\nDB configs consistency check failed. Run \"ambari-server start --skip-database-check\" to skip. " \
        "You may try --auto-fix-database flag to attempt to fix issues automatically. " \
        "If you use this \"--skip-database-check\" option, do not make any changes to your cluster topology " \
        "or perform a cluster upgrade until you correct the database consistency issues. See " + \
        configDefaults.DB_CHECK_LOG + " for more details on the consistency issues."
    elif 'DB_CHECK_WARNING' in open(configDefaults.SERVER_OUT_FILE).read():
      print "\nDB configs consistency check found warnings. See " + configDefaults.DB_CHECK_LOG + " for more details."
    # Only presume that DB check was successful if it explicitly appears in the log. An unexpected error may prevent
    # the consistency check from running at all, so missing error/warning message in the log cannot imply the check was
    # successful
    elif 'DB_CHECK_SUCCESS' in open(configDefaults.SERVER_OUT_FILE).read():
      print "\nDB configs consistency check: no errors and warnings were found."
  else:
    sys.stdout.write(configDefaults.SERVER_OUT_FILE + " does not exist")

  if exception:
    raise exception


def server_process_main(options, scmStatus=None):
  properties = get_ambari_properties()
  if properties == -1:
    err ="Error getting ambari properties"
    raise FatalException(-1, err)

  properties_for_print = []
  logger.info("Ambari server properties config:")
  for key, value in properties.getPropertyDict().items():
     if "passwd" not in key and "password" not in key:
       properties_for_print.append(key + "=" + value)

  logger.info(properties_for_print)

  # debug mode, including stop Java process at startup
  try:
    set_debug_mode_from_options(options)
  except AttributeError:
    pass

  if not check_reverse_lookup():
    print_warning_msg("The hostname was not found in the reverse DNS lookup. "
                      "This may result in incorrect behavior. "
                      "Please check the DNS setup and fix the issue.")

  check_database_name_property()
  parse_properties_file(options)

  is_active_instance = get_is_active_instance()
  if not is_active_instance:
      print_warning_msg("This instance of ambari server is not designated as active. Cannot start ambari server.")
      err = "This is not an active instance. Shutting down..."
      raise FatalException(1, err)

  ambari_user = read_ambari_user()
  current_user = ensure_can_start_under_current_user(ambari_user)

  print_info_msg("Ambari Server is not running...")

  jdk_path = find_jdk()
  if jdk_path is None:
    err = "No JDK found, please run the \"ambari-server setup\" " \
          "command to install a JDK automatically or install any " \
          "JDK manually to " + configDefaults.JDK_INSTALL_DIR
    raise FatalException(1, err)

  if not options.skip_properties_validation:
    missing_properties = get_missing_properties(properties)
    if missing_properties:
      err = "Required properties are not found: " + str(missing_properties) + ". To skip properties validation " \
            "use \"--skip-properties-validation\""
      raise FatalException(1, err)

  # Preparations
  if is_root():
    print configDefaults.MESSAGE_SERVER_RUNNING_AS_ROOT

  ensure_jdbc_driver_is_installed(options, properties)

  ensure_dbms_is_running(options, properties, scmStatus)

  if scmStatus is not None:
    scmStatus.reportStartPending()

  refresh_stack_hash(properties)

  if scmStatus is not None:
    scmStatus.reportStartPending()

  ensure_server_security_is_configured()

  if scmStatus is not None:
    scmStatus.reportStartPending()

  java_exe = get_java_exe_path()

  serverClassPath = ServerClassPath(properties, options)

  debug_mode = get_debug_mode()
  debug_start = (debug_mode & 1) or SERVER_START_DEBUG
  suspend_start = (debug_mode & 2) or SUSPEND_START_MODE
  suspend_mode = 'y' if suspend_start else 'n'

  environ = generate_env(options, ambari_user, current_user)
  class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell(validate_classpath=True)

  if options.skip_database_check:
    global jvm_args
    jvm_args += " -DskipDatabaseConsistencyCheck"
    print "Ambari Server is starting with the database consistency check skipped. Do not make any changes to your cluster " \
          "topology or perform a cluster upgrade until you correct the database consistency issues. See \"" \
          + configDefaults.DB_CHECK_LOG + "\" for more details on the consistency issues."
    properties.process_pair(CHECK_DATABASE_SKIPPED_PROPERTY, "true")
  else:
    print "Ambari database consistency check started..."
    if options.fix_database_consistency:
      jvm_args += " -DfixDatabaseConsistency"
    properties.process_pair(CHECK_DATABASE_SKIPPED_PROPERTY, "false")

  update_properties(properties)
  param_list = generate_child_process_param_list(ambari_user, java_exe, class_path, debug_start, suspend_mode)

  # The launched shell process and sub-processes should have a group id that
  # is different from the parent.
  def make_process_independent():
    if IS_FOREGROUND: # upstart script is not able to track process from different pgid.
      return
    
    processId = os.getpid()
    if processId > 0:
      try:
        os.setpgid(processId, processId)
      except OSError, e:
        print_warning_msg('setpgid({0}, {0}) failed - {1}'.format(pidJava, str(e)))
        pass

  print_info_msg("Running server: " + str(param_list))
  procJava = subprocess32.Popen(param_list, env=environ, preexec_fn=make_process_independent)

  pidJava = procJava.pid
  if pidJava <= 0:
    procJava.terminate()
    exitcode = procJava.returncode
    exitfile = os.path.join(configDefaults.PID_DIR, EXITCODE_NAME)
    save_pid(exitcode, exitfile)

    if scmStatus is not None:
      scmStatus.reportStopPending()

    raise FatalException(-1, AMBARI_SERVER_DIE_MSG.format(exitcode, configDefaults.SERVER_OUT_FILE))
  else:
    pidfile = os.path.join(configDefaults.PID_DIR, PID_NAME)

    print "Server PID at: "+pidfile
    print "Server out at: "+configDefaults.SERVER_OUT_FILE
    print "Server log at: "+configDefaults.SERVER_LOG_FILE

    wait_for_server_start(pidfile, scmStatus)

  if scmStatus is not None:
    scmStatus.reportStarted()
    
  if IS_FOREGROUND:
    procJava.communicate()

  return procJava
