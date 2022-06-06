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

import logging
import ConfigParser
import StringIO
import hostname
import ambari_simplejson as json
import os
import ssl

from ambari_agent.FileCache import FileCache
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

logger = logging.getLogger(__name__)

"""
The below config is necessary only for unit tests.
"""
content = """

[server]
hostname=localhost
url_port=8440
secured_url_port=8441

[agent]
prefix={ps}tmp{ps}ambari-agent
tmp_dir={ps}tmp{ps}ambari-agent{ps}tmp
data_cleanup_interval=86400
data_cleanup_max_age=2592000
data_cleanup_max_size_MB = 100
ping_port=8670
cache_dir={ps}tmp
parallel_execution=0
system_resource_overrides={ps}etc{ps}resource_overrides
tolerate_download_failures=false

[services]

[python]
custom_actions_dir = {ps}var{ps}lib{ps}ambari-agent{ps}resources{ps}custom_actions


[network]
use_system_proxy_settings=true

[security]
keysdir={ps}tmp{ps}ambari-agent
server_crt=ca.crt
passphrase_env_var_name=AMBARI_PASSPHRASE

[heartbeat]
state_interval = 1
dirs={ps}etc{ps}hadoop,{ps}etc{ps}hadoop{ps}conf,{ps}var{ps}run{ps}hadoop,{ps}var{ps}log{ps}hadoop
log_max_symbols_size=900000
iddle_interval_min=1
iddle_interval_max=10


[logging]
log_command_executes = 0

""".format(ps=os.sep)


class AmbariConfig:
  TWO_WAY_SSL_PROPERTY = "security.server.two_way_ssl"
  COMMAND_FILE_RETENTION_POLICY_PROPERTY = 'command_file_retention_policy'
  AMBARI_PROPERTIES_CATEGORY = 'agentConfig'
  SERVER_CONNECTION_INFO = "{0}/connection_info"
  CONNECTION_PROTOCOL = "https"

  # linux open-file limit
  ULIMIT_OPEN_FILES_KEY = 'ulimit.open.files'

  # #### Command JSON file retention policies #####
  # Keep all command-*.json files
  COMMAND_FILE_RETENTION_POLICY_KEEP = 'keep'
  # Remove command-*.json files if the operation was successful
  COMMAND_FILE_RETENTION_POLICY_REMOVE_ON_SUCCESS = 'remove_on_success'
  # Remove all command-*.json files when no longer needed
  COMMAND_FILE_RETENTION_POLICY_REMOVE = 'remove'
  # #### Command JSON file retention policies (end) #####

  config = None
  net = None

  def __init__(self):
    global content
    self.config = ConfigParser.RawConfigParser()
    self.config.readfp(StringIO.StringIO(content))

  def get(self, section, value, default=None):
    try:
      return str(self.config.get(section, value)).strip()
    except ConfigParser.Error as err:
      if default is not None:
        return default
      raise err

  def set(self, section, option, value):
    self.config.set(section, option, value)

  def add_section(self, section):
    self.config.add_section(section)

  def has_section(self, section):
    return self.config.has_section(section)

  def setConfig(self, customConfig):
    self.config = customConfig

  def getConfig(self):
    return self.config

  @classmethod
  def get_resolved_config(cls, home_dir=""):
    if hasattr(cls, "_conf_cache"):
      return getattr(cls, "_conf_cache")
    config = cls()
    configPath = os.path.abspath(cls.getConfigFile(home_dir))
    try:
      if os.path.exists(configPath):
        config.read(configPath)
      else:
        raise Exception("No config found at {0}, use default".format(configPath))

    except Exception, err:
      logger.warn(err)
    setattr(cls, "_conf_cache", config)
    return config

  @staticmethod
  @OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
  def getConfigFile(home_dir=""):
    """
    Get the configuration file path.
    :param home_dir: In production, will be "". When running multiple Agents per host, each agent will have a unique path.
    :return: Configuration file path.
    """
    if 'AMBARI_AGENT_CONF_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_CONF_DIR'], "ambari-agent.ini")
    else:
      # home_dir may be an empty string
      return os.path.join(os.sep, home_dir, "etc", "ambari-agent", "conf", "ambari-agent.ini")

  @property
  def server_hostname(self):
    return self.get('server', 'hostname')

  @property
  def secured_url_port(self):
    return self.get('server', 'secured_url_port')

  @property
  def command_reports_interval(self):
    return int(self.get('agent', 'command_reports_interval', default='5'))

  @property
  def alert_reports_interval(self):
    return int(self.get('agent', 'alert_reports_interval', default='5'))

  @property
  def status_commands_run_interval(self):
    return int(self.get('agent', 'status_commands_run_interval', default='20'))

  @property
  def command_update_output(self):
    return bool(int(self.get('agent', 'command_update_output', default='1')))

  @property
  def host_status_report_interval(self):
    return int(self.get('heartbeat', 'state_interval_seconds', '60'))

  @property
  def log_max_symbols_size(self):
    return int(self.get('heartbeat', 'log_max_symbols_size', '900000'))

  @property
  def cache_dir(self):
    return self.get('agent', 'cache_dir', default='/var/lib/ambari-agent/cache')

  @property
  def cluster_cache_dir(self):
    return os.path.join(self.cache_dir, FileCache.CLUSTER_CACHE_DIRECTORY)

  @property
  def alerts_cachedir(self):
    return os.path.join(self.cache_dir, FileCache.ALERTS_CACHE_DIRECTORY)

  @property
  def stacks_dir(self):
    return os.path.join(self.cache_dir, FileCache.STACKS_CACHE_DIRECTORY)

  @property
  def common_services_dir(self):
    return os.path.join(self.cache_dir, FileCache.COMMON_SERVICES_DIRECTORY)

  @property
  def extensions_dir(self):
    return os.path.join(self.cache_dir, FileCache.EXTENSIONS_CACHE_DIRECTORY)

  @property
  def host_scripts_dir(self):
    return os.path.join(self.cache_dir, FileCache.HOST_SCRIPTS_CACHE_DIRECTORY)

  @property
  def command_file_retention_policy(self):
    """
    Returns the Agent's command file retention policy.  This policy indicates what to do with the
    command-*.json and status_command.json files after they are done being used to execute commands
    from the Ambari server.

    Possible policy values are:

    * keep - Keep all command-*.json files
    * remove - Remove all command-*.json files when no longer needed
    * remove_on_success - Remove command-*.json files if the operation was successful

    The policy value is expected to be set in the Ambari agent's ambari-agent.ini file, under the
    [agent] section.

    For example:
        command_file_retention_policy=remove

    However, if the value is not set, or set to an unexpected value, "keep" will be returned, since
    this has been the (only) policy for past versions.

    :rtype: string
    :return: the command file retention policy, either "keep", "remove", or "remove_on_success"
    """
    policy = self.get('agent', self.COMMAND_FILE_RETENTION_POLICY_PROPERTY, default=self.COMMAND_FILE_RETENTION_POLICY_KEEP)
    policies = [self.COMMAND_FILE_RETENTION_POLICY_KEEP,
                self.COMMAND_FILE_RETENTION_POLICY_REMOVE,
                self.COMMAND_FILE_RETENTION_POLICY_REMOVE_ON_SUCCESS]

    if policy.lower() in policies:
      return policy.lower()
    else:
      logger.warning('The configured command_file_retention_policy is invalid, returning "%s" instead: %s',
                     self.COMMAND_FILE_RETENTION_POLICY_KEEP,
                     policy)
      return self.COMMAND_FILE_RETENTION_POLICY_KEEP


  # TODO AMBARI-18733, change usages of this function to provide the home_dir.
  @staticmethod
  def getLogFile(home_dir=""):
    """
    Get the log file path.
    :param home_dir: In production, will be "". When running multiple Agents per host, each agent will have a unique path.
    :return: Log file path.
    """
    if 'AMBARI_AGENT_LOG_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_LOG_DIR'], "ambari-agent.log")
    else:
      return os.path.join(os.sep, home_dir, "var", "log", "ambari-agent", "ambari-agent.log")

  # TODO AMBARI-18733, change usages of this function to provide the home_dir.
  @staticmethod
  def getAlertsLogFile(home_dir=""):
    """
    Get the alerts log file path.
    :param home_dir: In production, will be "". When running multiple Agents per host, each agent will have a unique path.
    :return: Alerts log file path.
    """
    if 'AMBARI_AGENT_LOG_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_LOG_DIR'], "ambari-alerts.log")
    else:
      return os.path.join(os.sep, home_dir, "var", "log", "ambari-agent", "ambari-alerts.log")

  # TODO AMBARI-18733, change usages of this function to provide the home_dir.
  @staticmethod
  def getOutFile(home_dir=""):
    """
    Get the out file path.
    :param home_dir: In production, will be "". When running multiple Agents per host, each agent will have a unique path.
    :return: Out file path.
    """
    if 'AMBARI_AGENT_LOG_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_LOG_DIR'], "ambari-agent.out")
    else:
      return os.path.join(os.sep, home_dir, "var", "log", "ambari-agent", "ambari-agent.out")

  def has_option(self, section, option):
    return self.config.has_option(section, option)

  def remove_option(self, section, option):
    return self.config.remove_option(section, option)

  def load(self, data):
    self.config = ConfigParser.RawConfigParser(data)

  def read(self, filename):
    self.config.read(filename)

  def getServerOption(self, url, name, default=None):
    from ambari_agent.NetUtil import NetUtil
    status, response = NetUtil(self).checkURL(url)
    if status is True:
      try:
        data = json.loads(response)
        if name in data:
          return data[name]
      except:
        pass
    return default

  def get_api_url(self, server_hostname):
    return "%s://%s:%s" % (self.CONNECTION_PROTOCOL,
                           server_hostname,
                           self.get('server', 'url_port'))

  def isTwoWaySSLConnection(self, server_hostname):
    req_url = self.get_api_url(server_hostname)
    response = self.getServerOption(self.SERVER_CONNECTION_INFO.format(req_url), self.TWO_WAY_SSL_PROPERTY, 'false')
    if response is None:
      return False
    elif response.lower() == "true":
      return True
    else:
      return False

  def get_parallel_exec_option(self):
    return int(self.get('agent', 'parallel_execution', 0))

  def get_ulimit_open_files(self):
    open_files_config_val =  int(self.get('agent', self.ULIMIT_OPEN_FILES_KEY, 0))
    open_files_ulimit = int(open_files_config_val) if (open_files_config_val and int(open_files_config_val) > 0) else 0
    return open_files_ulimit

  def set_ulimit_open_files(self, value):
    self.set('agent', self.ULIMIT_OPEN_FILES_KEY, value)


  def use_system_proxy_setting(self):
    """
    Return `True` if Agent need to honor system proxy setting and `False` if not

    :rtype bool
    """
    return "true" == self.get("network", "use_system_proxy_settings", "true").lower()

  def get_multiprocess_status_commands_executor_enabled(self):
    return bool(int(self.get('agent', 'multiprocess_status_commands_executor_enabled', 1)))

  def update_configuration_from_metadata(self, reg_resp):
    if reg_resp and AmbariConfig.AMBARI_PROPERTIES_CATEGORY in reg_resp:
      if not self.has_section(AmbariConfig.AMBARI_PROPERTIES_CATEGORY):
        self.add_section(AmbariConfig.AMBARI_PROPERTIES_CATEGORY)
      for k, v in reg_resp[AmbariConfig.AMBARI_PROPERTIES_CATEGORY].items():
        self.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, k, v)
        logger.info("Updating config property (%s) with value (%s)", k, v)
    pass

  def get_force_https_protocol_name(self):
    """
    Get forced https protocol name.

    :return: protocol name, PROTOCOL_TLSv1_2 by default
    """
    default = "PROTOCOL_TLSv1_2" if hasattr(ssl, "PROTOCOL_TLSv1_2") else "PROTOCOL_TLSv1"
    return self.get('security', 'force_https_protocol', default=default)

  def get_force_https_protocol_value(self):
    """
    Get forced https protocol value that correspondents to ssl module variable.

    :return: protocol value
    """
    return getattr(ssl, self.get_force_https_protocol_name())

  def get_ca_cert_file_path(self):
    """
    Get path to file with trusted certificates.

    :return: trusted certificates file path
    """
    return self.get('security', 'ca_cert_path', default="")

  @property
  def send_alert_changes_only(self):
    return bool(self.get('agent', 'send_alert_changes_only', '0'))


def isSameHostList(hostlist1, hostlist2):
  is_same = True

  if (hostlist1 is not None and hostlist2 is not None):
    if (len(hostlist1) != len(hostlist2)):
      is_same = False
    else:
      host_lookup = {}
      for item1 in hostlist1:
        host_lookup[item1.lower()] = True
      for item2 in hostlist2:
        if item2.lower() in host_lookup:
          del host_lookup[item2.lower()]
        else:
          is_same = False
          break
    pass
  elif (hostlist1 is not None or hostlist2 is not None):
    is_same = False
  return is_same

def updateConfigServerHostname(configFile, new_hosts):
  # update agent config file
  agent_config = ConfigParser.ConfigParser()
  agent_config.read(configFile)
  server_hosts = agent_config.get('server', 'hostname')
  if new_hosts is not None:
      new_host_names = hostname.arrayFromCsvString(new_hosts)
      if not isSameHostList(server_hosts, new_host_names):
        print "Updating server hostname from " + server_hosts + " to " + new_hosts
        agent_config.set('server', 'hostname', new_hosts)
        with (open(configFile, "wb")) as new_agent_config:
          agent_config.write(new_agent_config)

def main():
  print AmbariConfig().config

if __name__ == "__main__":
  main()
