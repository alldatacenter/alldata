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

__all__ = ["format"]
import sys
from string import Formatter
from resource_management.core.exceptions import Fail
from resource_management.core.utils import checked_unite
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from resource_management.core.shell import quote_bash_args
from resource_management.core import utils


class ConfigurationFormatter(Formatter):
  """
  Flags:
  !e - escape bash properties flag
  !h - hide sensitive information from the logs
  !p - password flag, !p=!s+!e. Has both !e, !h effect
  """
  def format(self, format_string, *args, **kwargs):
    variables = kwargs
    
    if Environment.has_instance():
      env = Environment.get_instance()
      params = env.config.params
  
      # don't use checked_unite for this as it would interfere with reload(module)
      # for things like params and status_params; instead, start out copying
      # the environment parameters and add in any locally declared variables to
      # override existing env parameters
      all_params = params.copy()
    else:
      all_params = {}
      
    all_params.update(variables)

    self.convert_field = self.convert_field_protected
    result_protected = self.vformat(format_string, args, all_params)
    
    self.convert_field = self.convert_field_unprotected
    result_unprotected = self.vformat(format_string, args, all_params)
    
    if result_protected != result_unprotected:
      Logger.sensitive_strings[result_unprotected] = result_protected
      
    return result_unprotected
  
  def convert_field_unprotected(self, value, conversion):
    return self._convert_field(value, conversion, False)
  
  def convert_field_protected(self, value, conversion):
    """
    Enable masking sensitive information like
    passwords from logs via !p (password) format flag.
    """
    return self._convert_field(value, conversion, True)
  
  def _convert_field(self, value, conversion, is_protected):
    if conversion == 'e':
      return quote_bash_args(unicode(value))
    elif conversion == 'h':
      return utils.PASSWORDS_HIDE_STRING if is_protected else value
    elif conversion == 'p':
      return utils.PASSWORDS_HIDE_STRING if is_protected else self._convert_field(value, 'e', is_protected)
      
    return super(ConfigurationFormatter, self).convert_field(value, conversion)


def format(format_string, *args, **kwargs):
  variables = sys._getframe(1).f_locals
  
  result = checked_unite(kwargs, variables)
  result.pop("self", None) # self kwarg would result in an error
  return ConfigurationFormatter().format(format_string, args, **result)
