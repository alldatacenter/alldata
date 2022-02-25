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

import logging
import socket
import time
from alerts.base_alert import BaseAlert
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from ambari_commons import OSCheck
from ambari_commons.inet_utils import resolve_address, get_host_from_url
logger = logging.getLogger(__name__)

# default timeouts
DEFAULT_WARNING_TIMEOUT = 1.5
DEFAULT_CRITICAL_TIMEOUT = 5.0

class PortAlert(BaseAlert):

  def __init__(self, alert_meta, alert_source_meta, config):
    super(PortAlert, self).__init__(alert_meta, alert_source_meta, config)

    self.uri = None
    self.default_port = None
    self.socket_command = None
    self.socket_command_response = None
    self.warning_timeout = DEFAULT_WARNING_TIMEOUT
    self.critical_timeout = DEFAULT_CRITICAL_TIMEOUT

    if 'uri' in alert_source_meta:
      self.uri = alert_source_meta['uri']

    # always static
    if 'default_port' in alert_source_meta:
      self.default_port = alert_source_meta['default_port']

    if 'reporting' in alert_source_meta:
      reporting = alert_source_meta['reporting']
      reporting_state_warning = self.RESULT_WARNING.lower()
      reporting_state_critical = self.RESULT_CRITICAL.lower()

      if reporting_state_warning in reporting and \
        'value' in reporting[reporting_state_warning]:
        self.warning_timeout = reporting[reporting_state_warning]['value']

      if reporting_state_critical in reporting and \
        'value' in reporting[reporting_state_critical]:
        self.critical_timeout = reporting[reporting_state_critical]['value']

    if 'parameters' in alert_source_meta:
      for parameter in alert_source_meta['parameters']:
        if 'socket.command' == parameter['name']:
          self.socket_command = parameter['value']
        if 'socket.command.response' == parameter['name']:
          self.socket_command_response = parameter['value']

    # check warning threshold for sanity
    if self.warning_timeout >= 30:
      logger.warn("[Alert][{0}] The warning threshold of {1}s is too large, resetting to {2}s".format(
        self.get_name(), str(self.warning_timeout), str(DEFAULT_WARNING_TIMEOUT)))

      self.warning_timeout = DEFAULT_WARNING_TIMEOUT


    # check critical threshold for sanity
    if self.critical_timeout >= 30:
      logger.warn("[Alert][{0}] The critical threshold of {1}s is too large, resetting to {2}s".format(
        self.get_name(), str(self.critical_timeout), str(DEFAULT_CRITICAL_TIMEOUT)))

      self.critical_timeout = DEFAULT_CRITICAL_TIMEOUT


  def _collect(self):
    configurations = self.configuration_builder.get_configuration(self.cluster_id, None, None)
    # can be parameterized or static
    # if not parameterized, this will return the static value
    uri_value = self._get_configuration_value(configurations, self.uri)

    host_not_specified = False
    if uri_value is None:
      host_not_specified = True
      uri_value = self.host_name
      logger.debug("[Alert][{0}] Setting the URI to this host since it wasn't specified".format(
        self.get_name()))

    # in some cases, a single property is a comma-separated list like
    # host1:8080,host2:8081,host3:8083
    uri_value_array = uri_value.split(',')
    if len(uri_value_array) > 1:
      for item in uri_value_array:
        if self.host_name in item:
          uri_value = item
          if logger.isEnabledFor(logging.DEBUG):
            logger.debug("[Alert][{0}] Extracted {1} as the host name while parsing the CSV URI {2}".format(
              self.get_name(), uri_value, str(uri_value_array)))
          break


    host = get_host_from_url(uri_value)
    if host is None or host == "localhost" or host == "0.0.0.0":
      host = self.host_name
      host_not_specified = True

    hosts = [host]
    # If host is not specified in the uri, hence we are using current host name
    # then also add public host name as a fallback.  
    if host_not_specified and host.lower() == self.host_name.lower() \
      and self.host_name.lower() != self.public_host_name.lower():
      hosts.append(self.public_host_name)
    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("[Alert][{0}] List of hosts = {1}".format(self.get_name(), hosts))

    try:
      port = int(get_port_from_url(uri_value))
    except:
      if self.default_port is None:
        label = 'Unable to determine port from URI {0}'.format(uri_value)
        return (self.RESULT_UNKNOWN, [label])

      port = self.default_port

    exceptions = []

    for host in hosts:
      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("[Alert][{0}] Checking {1} on port {2}".format(
          self.get_name(), host, str(port)))

      s = None
      try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(self.critical_timeout)

        if OSCheck.is_windows_family():
          # on windows 0.0.0.0 is invalid address to connect but on linux it resolved to 127.0.0.1
          host = resolve_address(host)

        start_time = time.time()
        s.connect((host, port))
        if self.socket_command is not None:
          s.sendall(self.socket_command)
          data = s.recv(1024)
          if self.socket_command_response is not None and data != self.socket_command_response:
            raise Exception("Expected response {0}, Actual response {1}".format(
              self.socket_command_response, data))
        end_time = time.time()
        milliseconds = end_time - start_time
        seconds = milliseconds / 1000.0

        # not sure why this happens sometimes, but we don't always get a
        # socket exception if the connect() is > than the critical threshold
        if seconds >= self.critical_timeout:
          return (self.RESULT_CRITICAL, ['Socket Timeout', host, port])

        result = self.RESULT_OK
        if seconds >= self.warning_timeout:
          result = self.RESULT_WARNING

        return (result, [seconds, port])
      except Exception as e:
        exceptions.append(e)
      finally:
        if s is not None:
          try:
            s.close()
          except:
            # no need to log a close failure
            pass

    if exceptions:
      return (self.RESULT_CRITICAL, [str(exceptions[0]), hosts[0], port])

  def _get_reporting_text(self, state):
    '''
    Gets the default reporting text to use when the alert definition does not
    contain any.
    :param state: the state of the alert in uppercase (such as OK, WARNING, etc)
    :return:  the parameterized text
    '''
    if state == self.RESULT_OK or state == self.RESULT_WARNING:
      return 'TCP OK - {0:.4f} response on port {1}'

    return 'Connection failed: {0} to {1}:{2}'
