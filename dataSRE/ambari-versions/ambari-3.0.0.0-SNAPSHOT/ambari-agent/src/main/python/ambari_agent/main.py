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

def fix_encoding_reimport_bug():
  """
  Fix https://bugs.python.org/issue14847
  """
  b'x'.decode('utf-8')
  b'x'.decode('ascii')

def fix_subprocess_racecondition():
  """
  subprocess in Python has race condition with enabling/disabling gc. Which may lead to turning off python garbage collector.
  This leads to a memory leak.
  This function monkey patches subprocess to fix the issue.

  !!! PLEASE NOTE THIS SHOULD BE CALLED BEFORE ANY OTHER INITIALIZATION was done to avoid already created links to subprocess or subprocess.gc or gc
  """
  # monkey patching subprocess
  import subprocess
  subprocess.gc.isenabled = lambda: True

  # re-importing gc to have correct isenabled for non-subprocess contexts
  import sys
  del sys.modules['gc']
  import gc


"""
# this might cause some unexcepted problems
def fix_subprocess_popen():
  '''
  Workaround for race condition in starting subprocesses concurrently from
  multiple threads via the subprocess and multiprocessing modules.
  See http://bugs.python.org/issue19809 for details and repro script.
  '''
  import os
  import sys

  if os.name == 'posix' and sys.version_info[0] < 3:
    from multiprocessing import forking
    from ambari_commons import subprocess
    import threading

    sp_original_init = subprocess.Popen.__init__
    mp_original_init = forking.Popen.__init__
    lock = threading.RLock() # guards subprocess creation

    def sp_locked_init(self, *a, **kw):
      with lock:
        sp_original_init(self, *a, **kw)

    def mp_locked_init(self, *a, **kw):
      with lock:
        mp_original_init(self, *a, **kw)

    subprocess.Popen.__init__ = sp_locked_init
    forking.Popen.__init__ = mp_locked_init
"""

#fix_subprocess_popen()
fix_subprocess_racecondition()
fix_encoding_reimport_bug()

import logging.handlers
import logging.config
from optparse import OptionParser
import sys
import os
import time
import locale
import platform
import ConfigParser
import signal
import resource
from logging.handlers import SysLogHandler
import AmbariConfig
from NetUtil import NetUtil
from PingPortListener import PingPortListener
import hostname
from DataCleaner import DataCleaner
from ambari_agent.ExitHelper import ExitHelper
import socket
from ambari_commons import OSConst, OSCheck
from ambari_commons.shell import shellRunner
#from ambari_commons.network import reconfigure_urllib2_opener
from HeartbeatHandlers import bind_signal_handlers
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core.logger import Logger
#from resource_management.core.resources.system import File
#from resource_management.core.environment import Environment

from ambari_agent.InitializerModule import InitializerModule

#logging.getLogger('ambari_agent').propagate = False

logger = logging.getLogger()
alerts_logger = logging.getLogger('alerts')
alerts_logger_2 = logging.getLogger('ambari_alerts')
alerts_logger_global = logging.getLogger('ambari_agent.alerts')
apscheduler_logger = logging.getLogger('apscheduler')
apscheduler_logger_global = logging.getLogger('ambari_agent.apscheduler')

formatstr = "%(levelname)s %(asctime)s %(filename)s:%(lineno)d - %(message)s"
agentPid = os.getpid()

# Global variables to be set later.
home_dir = ""

agent_piddir = os.environ['AMBARI_PID_DIR'] if 'AMBARI_PID_DIR' in os.environ else "/var/run/ambari-agent"
agent_pidfile = os.path.join(agent_piddir, "ambari-agent.pid")

config = AmbariConfig.AmbariConfig()

# TODO AMBARI-18733, remove this global variable and calculate it based on home_dir once it is set.
configFile = config.getConfigFile()
two_way_ssl_property = config.TWO_WAY_SSL_PROPERTY

IS_LINUX = platform.system() == "Linux"
SYSLOG_FORMAT_STRING = ' ambari_agent - %(filename)s - [%(process)d] - %(name)s - %(levelname)s - %(message)s'
SYSLOG_FORMATTER = logging.Formatter(SYSLOG_FORMAT_STRING)

_file_logging_handlers ={}

def setup_logging(logger, filename, logging_level):
  logger.propagate = False
  formatter = logging.Formatter(formatstr)

  if filename in _file_logging_handlers:
    rotateLog = _file_logging_handlers[filename]
  else:
    rotateLog = logging.handlers.RotatingFileHandler(filename, "a", 10000000, 25)
    rotateLog.setFormatter(formatter)
    _file_logging_handlers[filename] = rotateLog
  logger.handlers = []
  logger.addHandler(rotateLog)

  logging.basicConfig(format=formatstr, level=logging_level, filename=filename)
  logger.setLevel(logging_level)
  logger.info("loglevel=logging.{0}".format(logging._levelNames[logging_level]))

GRACEFUL_STOP_TRIES = 300
GRACEFUL_STOP_TRIES_SLEEP = 0.1


def add_syslog_handler(logger):

  syslog_enabled = config.has_option("logging","syslog_enabled") and (int(config.get("logging","syslog_enabled")) == 1)

  #add syslog handler if we are on linux and syslog is enabled in ambari config
  if syslog_enabled and IS_LINUX:
    logger.info("Adding syslog handler to ambari agent logger")
    syslog_handler = SysLogHandler(address="/dev/log",
                                   facility=SysLogHandler.LOG_LOCAL1)

    syslog_handler.setFormatter(SYSLOG_FORMATTER)
    logger.addHandler(syslog_handler)

def update_log_level(config):
  # Setting loglevel based on config file
  global logger
  global home_dir
  log_cfg_file = os.path.join(os.path.dirname(AmbariConfig.AmbariConfig.getConfigFile(home_dir)), "logging.conf")
  if os.path.exists(log_cfg_file):
    logging.config.fileConfig(log_cfg_file)
    # create logger
    logger = logging.getLogger(__name__)
    logger.info("Logging configured by " + log_cfg_file)
  else:
    try:
      loglevel = config.get('agent', 'loglevel')
      if loglevel is not None:
        if loglevel == 'DEBUG':
          logging.basicConfig(format=formatstr, level=logging.DEBUG, filename=AmbariConfig.AmbariConfig.getLogFile())
          logger.setLevel(logging.DEBUG)
          logger.info("Newloglevel=logging.DEBUG")
        else:
          logging.basicConfig(format=formatstr, level=logging.INFO, filename=AmbariConfig.AmbariConfig.getLogFile())
          logger.setLevel(logging.INFO)
          logger.debug("Newloglevel=logging.INFO")
    except Exception, err:
      logger.info("Default loglevel=DEBUG")


# TODO AMBARI-18733, move inside AmbariConfig
def resolve_ambari_config():
  """
  Load the configurations.
  In production, home_dir will be "". When running multiple Agents per host, each agent will have a unique path.
  """
  global config
  global home_dir
  configPath = os.path.abspath(AmbariConfig.AmbariConfig.getConfigFile(home_dir))
  try:
    if os.path.exists(configPath):
      config.read(configPath)
    else:
      raise Exception("No config found at {0}, use default".format(configPath))

  except Exception, err:
    logger.warn(err)

def check_sudo():
  # don't need to check sudo for root.
  if os.geteuid() == 0:
    return

  runner = shellRunner()
  test_command = [AMBARI_SUDO_BINARY, '/usr/bin/test', '/']
  test_command_str = ' '.join(test_command)

  start_time = time.time()
  res = runner.run(test_command)
  end_time = time.time()
  run_time = end_time - start_time

  if res['exitCode'] != 0:
    raise Exception("Please check your sudo configurations.\n" + test_command_str + " failed with " + res['error'] + res['output']) # bad sudo configurations

  if run_time > 2:
    logger.warn(("Sudo commands on this host are running slowly ('{0}' took {1} seconds).\n" +
                "This will create a significant slow down for ambari-agent service tasks.").format(test_command_str, run_time))

# Updates the hard limit for open file handles
def update_open_files_ulimit(config):
  global logger
  # get the current soft and hard limits
  # if the specified value is greater than or equal to the soft limit
  # we can update the hard limit
  (soft_limit, hard_limit) = resource.getrlimit(resource.RLIMIT_NOFILE)
  open_files_ulimit = config.get_ulimit_open_files()
  if open_files_ulimit >= soft_limit:
    try:
      resource.setrlimit(resource.RLIMIT_NOFILE, (soft_limit, open_files_ulimit))
      logger.info('open files ulimit = {0}'.format(open_files_ulimit))
    except ValueError, err:
      logger.error('Unable to set open files ulimit to {0}: {1}'.format(open_files_ulimit, str(err)))
      logger.info('open files ulimit = {0}'.format(hard_limit))

def perform_prestart_checks(expected_hostname):
  # Check if current hostname is equal to expected one (got from the server
  # during bootstrap.
  global config

  if expected_hostname is not None:
    current_hostname = hostname.hostname(config)
    if current_hostname != expected_hostname:
      print("Determined hostname does not match expected. Please check agent "
            "log for details")
      msg = "Ambari agent machine hostname ({0}) does not match expected ambari " \
            "server hostname ({1}). Aborting registration. Please check hostname, " \
            "hostname -f and /etc/hosts file to confirm your " \
            "hostname is setup correctly".format(current_hostname, expected_hostname)
      logger.error(msg)
      sys.exit(1)
  # Check if there is another instance running
  if os.path.isfile(agent_pidfile) and not OSCheck.get_os_family() == OSConst.WINSRV_FAMILY:
    print("%s already exists, exiting" % agent_pidfile)
    sys.exit(1)
  # check if ambari prefix exists
  elif config.has_option('agent', 'prefix') and not os.path.isdir(os.path.abspath(config.get('agent', 'prefix'))):
    msg = "Ambari prefix dir %s does not exists, can't continue" \
          % config.get("agent", "prefix")
    logger.error(msg)
    print(msg)
    sys.exit(1)
  elif not config.has_option('agent', 'prefix'):
    msg = "Ambari prefix dir %s not configured, can't continue"
    logger.error(msg)
    print(msg)
    sys.exit(1)

  check_sudo()


def daemonize():
  pid = str(os.getpid())
  file(agent_pidfile, 'w').write(pid)

def stop_agent():
# stop existing Ambari agent
  pid = -1
  runner = shellRunner()
  try:
    with open(agent_pidfile, 'r') as f:
      pid = f.read()
    pid = int(pid)

    runner.run([AMBARI_SUDO_BINARY, 'kill', '-15', str(pid)])
    for i in range(GRACEFUL_STOP_TRIES):
      result = runner.run([AMBARI_SUDO_BINARY, 'kill', '-0', str(pid)])
      if result['exitCode'] != 0:
        logger.info("Agent died gracefully, exiting.")
        sys.exit(0)
      time.sleep(GRACEFUL_STOP_TRIES_SLEEP)
    logger.info("Agent not going to die gracefully, going to execute kill -9")
    raise Exception("Agent is running")
  except Exception, err:
    #raise
    if pid == -1:
      print ("Agent process is not running")
    else:
      res = runner.run([AMBARI_SUDO_BINARY, 'kill', '-9', str(pid)])
      if res['exitCode'] != 0:
        raise Exception("Error while performing agent stop. " + res['error'] + res['output'])
      else:
        logger.info("Agent stopped successfully by kill -9, exiting.")
    sys.exit(0)

def reset_agent(options):
  global home_dir
  try:
    # update agent config file
    agent_config = ConfigParser.ConfigParser()
    # TODO AMBARI-18733, calculate configFile based on home_dir
    agent_config.read(configFile)
    server_host = agent_config.get('server', 'hostname')
    new_host = options[2]
    if new_host is not None and server_host != new_host:
      print "Updating server host from " + server_host + " to " + new_host
      agent_config.set('server', 'hostname', new_host)
      with (open(configFile, "wb")) as new_agent_config:
        agent_config.write(new_agent_config)

    # clear agent certs
    agent_keysdir = agent_config.get('security', 'keysdir')
    print "Removing Agent certificates..."
    for root, dirs, files in os.walk(agent_keysdir, topdown=False):
      for name in files:
        os.remove(os.path.join(root, name))
      for name in dirs:
        os.rmdir(os.path.join(root, name))
  except Exception, err:
    print("A problem occurred while trying to reset the agent: " + str(err))
    sys.exit(1)

  sys.exit(0)

MAX_RETRIES = 10

def run_threads(initializer_module):
  initializer_module.alert_scheduler_handler.start()
  initializer_module.heartbeat_thread.start()
  initializer_module.component_status_executor.start()
  initializer_module.command_status_reporter.start()
  initializer_module.host_status_reporter.start()
  initializer_module.alert_status_reporter.start()
  initializer_module.action_queue.start()

  while not initializer_module.stop_event.is_set():
    signal.pause()

  initializer_module.action_queue.interrupt()

  initializer_module.command_status_reporter.join()
  initializer_module.component_status_executor.join()
  initializer_module.host_status_reporter.join()
  initializer_module.alert_status_reporter.join()
  initializer_module.heartbeat_thread.join()
  initializer_module.action_queue.join()

# event - event, that will be passed to Controller and NetUtil to make able to interrupt loops form outside process
# we need this for windows os, where no sigterm available
def main(initializer_module, heartbeat_stop_callback=None):
  global config
  global home_dir

  parser = OptionParser()
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", help="verbose log output", default=False)
  parser.add_option("-e", "--expected-hostname", dest="expected_hostname", action="store",
                    help="expected hostname of current host. If hostname differs, agent will fail", default=None)
  parser.add_option("--home", dest="home_dir", action="store", help="Home directory", default="")
  (options, args) = parser.parse_args()

  expected_hostname = options.expected_hostname
  home_dir = options.home_dir

  logging_level = logging.DEBUG if options.verbose else logging.INFO

  setup_logging(logger, AmbariConfig.AmbariConfig.getLogFile(), logging_level)
  global is_logger_setup
  is_logger_setup = True
  setup_logging(alerts_logger, AmbariConfig.AmbariConfig.getAlertsLogFile(), logging_level)
  setup_logging(alerts_logger_2, AmbariConfig.AmbariConfig.getAlertsLogFile(), logging_level)
  setup_logging(alerts_logger_global, AmbariConfig.AmbariConfig.getAlertsLogFile(), logging_level)
  setup_logging(apscheduler_logger, AmbariConfig.AmbariConfig.getAlertsLogFile(), logging_level)
  setup_logging(apscheduler_logger_global, AmbariConfig.AmbariConfig.getAlertsLogFile(), logging_level)
  Logger.initialize_logger('resource_management', logging_level=logging_level)
  #with Environment() as env:
  #  File("/abc")

  # init data, once loggers are setup to see exceptions/errors of initialization.
  initializer_module.init()

  if home_dir != "":
    # When running multiple Ambari Agents on this host for simulation, each one will use a unique home directory.
    Logger.info("Agent is using Home Dir: %s" % str(home_dir))

  # use the host's locale for numeric formatting
  try:
    locale.setlocale(locale.LC_ALL, '')
  except locale.Error as ex:
    logger.warning("Cannot set locale for ambari-agent. Please check your systemwide locale settings. Failed due to: {0}.".format(str(ex)))

  default_cfg = {'agent': {'prefix': '/home/ambari'}}
  config.load(default_cfg)

  if (len(sys.argv) > 1) and sys.argv[1] == 'stop':
    stop_agent()

  if (len(sys.argv) > 2) and sys.argv[1] == 'reset':
    reset_agent(sys.argv)

  # Check for ambari configuration file.
  resolve_ambari_config()

  # Add syslog hanlder based on ambari config file
  add_syslog_handler(logger)

  # Starting data cleanup daemon
  data_cleaner = None
  if config.has_option('agent', 'data_cleanup_interval') and int(config.get('agent','data_cleanup_interval')) > 0:
    data_cleaner = DataCleaner(config)
    data_cleaner.start()

  perform_prestart_checks(expected_hostname)

  # Starting ping port listener
  try:
    #This acts as a single process machine-wide lock (albeit incomplete, since
    # we still need an extra file to track the Agent PID)
    ping_port_listener = PingPortListener(config)
  except Exception as ex:
    err_message = "Failed to start ping port listener of: " + str(ex)
    logger.error(err_message)
    sys.stderr.write(err_message)
    sys.exit(1)
  ping_port_listener.start()

  update_log_level(config)

  update_open_files_ulimit(config)

  if not config.use_system_proxy_setting():
    logger.info('Agent is configured to ignore system proxy settings')
    #reconfigure_urllib2_opener(ignore_system_proxy=True)

  if not OSCheck.get_os_family() == OSConst.WINSRV_FAMILY:
    daemonize()

  #
  # Iterate through the list of server hostnames and connect to the first active server
  #

  active_server = None
  server_hostnames = hostname.server_hostnames(config)

  connected = False
  stopped = False

  # Keep trying to connect to a server or bail out if ambari-agent was stopped
  while not connected and not stopped and not initializer_module.stop_event.is_set():
    for server_hostname in server_hostnames:
      server_url = config.get_api_url(server_hostname)
      try:
        server_ip = socket.gethostbyname(server_hostname)
        logger.info('Connecting to Ambari server at %s (%s)', server_url, server_ip)
      except socket.error:
        logger.warn("Unable to determine the IP address of the Ambari server '%s'", server_hostname)

      # Wait until MAX_RETRIES to see if server is reachable
      netutil = NetUtil(config, initializer_module.stop_event)
      (retries, connected, stopped) = netutil.try_to_connect(server_url, MAX_RETRIES, logger)

      # if connected, launch controller
      if connected:
        logger.info('Connected to Ambari server %s', server_hostname)
        # Set the active server
        active_server = server_hostname
        # Launch Controller communication
        run_threads(initializer_module)

      #
      # If Ambari Agent connected to the server or
      # Ambari Agent was stopped using stop event
      # Clean up if not Windows OS
      #
      if connected or stopped:
        ExitHelper().exit()
        logger.info("finished")
        break
    pass # for server_hostname in server_hostnames
  pass # while not (connected or stopped)

  return active_server

if __name__ == "__main__":
  is_logger_setup = False
  try:
    initializer_module = InitializerModule()
    heartbeat_stop_callback = bind_signal_handlers(agentPid, initializer_module.stop_event)

    main(initializer_module, heartbeat_stop_callback)
  except SystemExit:
    raise
  except BaseException:
    if is_logger_setup:
      logger.exception("Exiting with exception:")
    raise
