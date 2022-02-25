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

__all__ = ["Logger"]
import sys
import logging
from resource_management.libraries.script.config_dictionary import UnknownConfiguration
from resource_management.core.utils import PasswordString

MESSAGE_MAX_LEN = 512
DICTIONARY_MAX_LEN = 5

class Logger:
  logger = None
  # unprotected_strings : protected_strings map
  sensitive_strings = {}
  
  @staticmethod
  def initialize_logger(name='resource_management', logging_level=logging.INFO, format='%(asctime)s - %(message)s'):
    if Logger.logger:
      return
    # set up logging (two separate loggers for stderr and stdout with different loglevels)
    logger = logging.getLogger(name)
    logger.setLevel(logging_level)
    formatter = logging.Formatter(format)
    chout = logging.StreamHandler(sys.stdout)
    chout.setLevel(logging_level)
    chout.setFormatter(formatter)
    cherr = logging.StreamHandler(sys.stderr)
    cherr.setLevel(logging.ERROR)
    cherr.setFormatter(formatter)
    logger.handlers = []
    logger.addHandler(cherr)
    logger.addHandler(chout)

    Logger.logger = logger

  @staticmethod
  def isEnabledFor(level):
    return Logger.logger is not None and Logger.logger.isEnabledFor(level)

  @staticmethod
  def exception(text):
    Logger.logger.exception(Logger.filter_text(text))

  @staticmethod
  def error(text):
    Logger.logger.error(Logger.filter_text(text))

  @staticmethod
  def warning(text):
    Logger.logger.warning(Logger.filter_text(text))

  @staticmethod
  def info(text):
    Logger.logger.info(Logger.filter_text(text))

  @staticmethod
  def debug(text):
    Logger.logger.debug(Logger.filter_text(text))

  @staticmethod
  def error_resource(resource):
    Logger.error(Logger.filter_text(Logger._get_resource_repr(resource)))

  @staticmethod
  def warning_resource(resource):
    Logger.warning(Logger.filter_text(Logger._get_resource_repr(resource)))

  @staticmethod
  def info_resource(resource):
    Logger.info(Logger.filter_text(Logger._get_resource_repr(resource)))

  @staticmethod
  def debug_resource(resource):
    Logger.debug(Logger.filter_text(Logger._get_resource_repr(resource)))
    
  @staticmethod    
  def filter_text(text):
    """
    Replace passwords with [PROTECTED] and remove shell.py placeholders
    """
    from resource_management.core.shell import PLACEHOLDERS_TO_STR
    
    for unprotected_string, protected_string in Logger.sensitive_strings.iteritems():
      text = text.replace(unprotected_string, protected_string)

    for placeholder in PLACEHOLDERS_TO_STR.keys():
      text = text.replace(placeholder, '')

    return text
  
  @staticmethod
  def _get_resource_repr(resource):
    return Logger.get_function_repr(repr(resource), resource.arguments, resource)
  
  @staticmethod
  def _get_resource_name_repr(name):
    if isinstance(name, basestring) and not isinstance(name, PasswordString):
      name = "'" + name + "'" # print string cutely not with repr
    else:
      name = repr(name)
      
    return name
  
  @staticmethod
  def format_command_for_output(command):
    """
    Format command to be output by replacing the PasswordStrings.
    """
    if isinstance(command, (list, tuple)):
      result = []
      for x in command:
        if isinstance(x, PasswordString):
          result.append(repr(x).strip("'")) # string ''
        else:
          result.append(x)
    else:
      if isinstance(command, PasswordString):
        result = repr(command).strip("'") # string ''
      else:
        result = command
    
    return result
  
  @staticmethod
  def get_function_repr(name, arguments, resource=None):
    logger_level = logging._levelNames[Logger.logger.level]

    arguments_str = ""
    for x,y in arguments.iteritems():
      # for arguments which want to override the output
      if resource and 'log_str' in dir(resource._arguments[x]):
        val = resource._arguments[x].log_str(x, y)
      # don't show long arguments
      elif isinstance(y, basestring) and len(y) > MESSAGE_MAX_LEN:
        val = '...'
      # strip unicode 'u' sign
      elif isinstance(y, unicode):
        val = repr(y).lstrip('u')
      # don't show dicts of configurations
      # usually too long
      elif isinstance(y, dict) and len(y) > DICTIONARY_MAX_LEN:
        val = "..."
      # for configs which didn't come
      elif isinstance(y, UnknownConfiguration):
        val = "[EMPTY]"
      # correctly output 'mode' (as they are octal values like 0755)
      elif y and x == 'mode':
        try:
          val = oct(y)
        except:
          val = repr(y)
      # for functions show only function name
      elif hasattr(y, '__call__') and hasattr(y, '__name__'):
        val = y.__name__
      else:
        val = repr(y)

      arguments_str += "'{0}': {1}, ".format(x, val)

    if arguments_str:
      arguments_str = arguments_str[:-2]
        
    return unicode("{0} {{{1}}}", 'UTF-8').format(name, arguments_str)
