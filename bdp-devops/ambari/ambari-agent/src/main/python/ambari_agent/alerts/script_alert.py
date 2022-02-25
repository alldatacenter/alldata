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

import imp
import logging
import os
import re
from alerts.base_alert import BaseAlert
from resource_management.core.environment import Environment
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.curl_krb_request import KERBEROS_KINIT_TIMER_PARAMETER
from ambari_commons.constants import AGENT_TMP_DIR

logger = logging.getLogger(__name__)

class ScriptAlert(BaseAlert):
  PATH_TO_SCRIPT_REGEXP = re.compile(r'((.*)services(.*)package)')

  def __init__(self, alert_meta, alert_source_meta, config):

    """ ScriptAlert reporting structure is output from the script itself """

    alert_source_meta['reporting'] = {
      'ok': { 'text': '{0}' },
      'warning': { 'text': '{0}' },
      'critical': { 'text': '{0}' },
      'unknown': { 'text': '{0}' }
    }

    super(ScriptAlert, self).__init__(alert_meta, alert_source_meta, config)

    self.path = None
    self.stacks_dir = None
    self.common_services_dir = None
    self.host_scripts_dir = None
    self.extensions_dir = None
    self.path_to_script = None
    self.parameters = {}

    # will force a kinit even if klist says there are valid tickets (4 hour default)
    self.kinit_timeout = long(config.get('agent', 'alert_kinit_timeout', BaseAlert._DEFAULT_KINIT_TIMEOUT))

    if 'path' in alert_source_meta:
      self.path = alert_source_meta['path']

    if 'common_services_directory' in alert_source_meta:
      self.common_services_dir = alert_source_meta['common_services_directory']

    if 'stacks_directory' in alert_source_meta:
      self.stacks_dir = alert_source_meta['stacks_directory']

    if 'host_scripts_directory' in alert_source_meta:
      self.host_scripts_dir = alert_source_meta['host_scripts_directory']

    if 'extensions_directory' in alert_source_meta:
      self.extensions_dir = alert_source_meta['extensions_directory']

    # convert a list of script parameters, like timeouts, into a dictionary
    # so the the scripts can easily lookup the data
    if 'parameters' in alert_source_meta:
      parameters = alert_source_meta['parameters']
      for parameter in parameters:
        if 'name' not in parameter or 'value' not in parameter:
          continue

        # create the dictionary value
        parameter_name = parameter['name']
        parameter_value = parameter['value']
        self.parameters[parameter_name] = parameter_value

    # pass in some basic parameters to the scripts
    self.parameters[KERBEROS_KINIT_TIMER_PARAMETER] = self.kinit_timeout

  def _collect(self):
    cmd_module = self._load_source()

    full_configurations = self.configuration_builder.get_configuration(self.cluster_id, None, None)
    if cmd_module is not None:
      configurations = {}

      try:
        tokens = cmd_module.get_tokens()
        if tokens is not None:
          # for each token, if there is a value, store in; otherwise don't store
          # a key with a value of None
          for token in tokens:
            value = self._get_configuration_value(full_configurations, token)
            if value is not None:
              configurations[token] = value
      except AttributeError:
        # it's OK if the module doesn't have get_tokens() ; no tokens will
        # be passed in so hopefully the script doesn't need any
        logger.debug("The script {0} does not have a get_tokens() function".format(str(cmd_module)))

      Script.config = full_configurations

      # try to get basedir for scripts
      # it's needed for server side scripts to properly use resource management
      matchObj = ScriptAlert.PATH_TO_SCRIPT_REGEXP.match(self.path_to_script)
      if matchObj:
        basedir = matchObj.group(1)
        with Environment(basedir, tmp_dir=AGENT_TMP_DIR, logger=logging.getLogger('alerts')) as env:
          result = cmd_module.execute(configurations, self.parameters, self.host_name)
      else:
        result = cmd_module.execute(configurations, self.parameters, self.host_name)

      loggerMsg = "[Alert][{0}] Failed with result {2}: {3}".format(
        self.get_name(), self.path_to_script, result[0], result[1])

      if result[0] == self.RESULT_CRITICAL:
        logger.error(loggerMsg)
      elif result[0] == self.RESULT_WARNING or result[0] == self.RESULT_UNKNOWN:
        logger.debug(loggerMsg)

      return result
    else:
      return (self.RESULT_UNKNOWN, ["Unable to execute script {0}".format(self.path)])


  def _load_source(self):
    if self.path is None and self.stack_path is None and self.host_scripts_dir is None:
      raise Exception("The attribute 'path' must be specified")

    paths = self.path.split('/')
    self.path_to_script = self.path

    # if the path doesn't exist and stacks dir is defined, try that
    if not os.path.exists(self.path_to_script) and self.stacks_dir is not None:
      self.path_to_script = os.path.join(self.stacks_dir, *paths)

    # if the path doesn't exist and common services dir is defined, try that
    if not os.path.exists(self.path_to_script) and self.common_services_dir is not None:
      self.path_to_script = os.path.join(self.common_services_dir, *paths)

    # if the path doesn't exist and the host script dir is defined, try that
    if not os.path.exists(self.path_to_script) and self.host_scripts_dir is not None:
      self.path_to_script = os.path.join(self.host_scripts_dir, *paths)

    # if the path doesn't exist and the extensions dir is defined, try that
    if not os.path.exists(self.path_to_script) and self.extensions_dir is not None:
      self.path_to_script = os.path.join(self.extensions_dir, *paths)

    # if the path can't be evaluated, throw exception
    if not os.path.exists(self.path_to_script) or not os.path.isfile(self.path_to_script):
      raise Exception(
        "Unable to find '{0}' as an absolute path or part of {1} or {2}".format(self.path,
          self.stacks_dir, self.host_scripts_dir))

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("[Alert][{0}] Executing script check {1}".format(
        self.get_name(), self.path_to_script))


    if (not self.path_to_script.endswith('.py')):
      logger.error("[Alert][{0}] Unable to execute script {1}".format(
        self.get_name(), self.path_to_script))

      return None

    return imp.load_source(self._get_alert_meta_value_safely('name'), self.path_to_script)


  def _get_reporting_text(self, state):
    '''
    Always returns {0} since the result of the script alert is a rendered string.
    This will ensure that the base class takes the result string and just uses
    it directly.

    :param state: the state of the alert in uppercase (such as OK, WARNING, etc)
    :return:  the parameterized text
    '''
    return '{0}'
