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
__all__ = ["RMFTestCase", "Template", "StaticFile", "InlineTemplate", "DownloadSource", "UnknownConfigurationMock", "FunctionMock",
           "CallFunctionMock"]

from unittest import TestCase
import json
import os
import imp
import sys
import pprint
import itertools
from mock.mock import MagicMock, patch
import platform
import re

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  with patch("os.geteuid", return_value=45000):  # required to mock sudo and run tests with right scenario
    from resource_management.core import sudo
    from resource_management.core.environment import Environment
    from resource_management.libraries.script.config_dictionary import ConfigDictionary
    from resource_management.libraries.script.script import Script
    from resource_management.libraries.script.config_dictionary import UnknownConfiguration
    from resource_management.libraries.functions.repository_util import RepositoryUtil

PATH_TO_STACKS = "main/resources/stacks/HDP"
PATH_TO_STACK_TESTS = "test/python/stacks/"

PATH_TO_COMMON_SERVICES = "main/resources/common-services"
PATH_TO_STACK_HOOKS = "main/resources/stack-hooks"

PATH_TO_CUSTOM_ACTIONS = "main/resources/custom_actions"
PATH_TO_CUSTOM_ACTION_TESTS = "test/python/custom_actions"
MAX_SHOWN_DICT_LEN = 10


class RMFTestCase(TestCase):
  # provides more verbose output when comparing assertion failures
  maxDiff = None

  # (default) build all paths to test stack scripts
  TARGET_STACKS = 'TARGET_STACKS'

  # (default) build all paths to test custom action scripts
  TARGET_CUSTOM_ACTIONS = 'TARGET_CUSTOM_ACTIONS'

  # build all paths to test common services scripts
  TARGET_COMMON_SERVICES = 'TARGET_COMMON_SERVICES'

  # build all paths to test common services scripts
  TARGET_STACK_HOOKS = 'TARGET_STACK_HOOKS'

  def executeScript(self, path, classname=None, command=None, config_file=None,
                    config_dict=None,
                    # common mocks for all the scripts
                    config_overrides = None,
                    stack_version = None,
                    checked_call_mocks = itertools.cycle([(0, "OK.")]),
                    call_mocks = itertools.cycle([(0, "OK.")]),
                    os_type=('Suse','11','Final'),
                    kinit_path_local="/usr/bin/kinit",
                    os_env={'PATH':'/bin'},
                    target=TARGET_STACKS,
                    mocks_dict={},
                    try_install=False,
                    command_args=[],
                    log_out_files=False,
                    available_packages_in_repos = []):

    norm_path = os.path.normpath(path)

    if target == self.TARGET_STACKS:
      stack_version = norm_path.split(os.sep)[0]

    base_path, configs_path = self._get_test_paths(target, stack_version)
    script_path = os.path.join(base_path, norm_path)

    if config_file is not None and config_dict is None:
      self.config_dict = self.get_config_file(configs_path, config_file)
    elif config_dict is not None and config_file is None:
      self.config_dict = config_dict
    else:
      raise RuntimeError("Please specify either config_file or config_dict parameter")

    # add the stack tools & features from the stack if the test case's JSON file didn't have them
    if "stack_tools" not in self.config_dict["configurations"]["cluster-env"]:
      self.config_dict["configurations"]["cluster-env"]["stack_tools"] = RMFTestCase.get_stack_tools()

    if "stack_features" not in self.config_dict["configurations"]["cluster-env"]:
      self.config_dict["configurations"]["cluster-env"]["stack_features"] = RMFTestCase.get_stack_features()

    if "stack_packages" not in self.config_dict["configurations"]["cluster-env"]:
      self.config_dict["configurations"]["cluster-env"]["stack_packages"] = RMFTestCase.get_stack_packages()

    if config_overrides:
      for key, value in config_overrides.iteritems():
        self.config_dict[key] = value

    self.config_dict = ConfigDictionary(self.config_dict)

    # append basedir to PYTHONPATH
    scriptsdir = os.path.dirname(script_path)
    basedir = os.path.dirname(scriptsdir)
    sys.path.append(scriptsdir)
    
    # get method to execute
    try:
      with patch.object(platform, 'linux_distribution', return_value=os_type):
        script_module = imp.load_source(classname, script_path)
        Script.instance = None
        script_class_inst = RMFTestCase._get_attr(script_module, classname)()
        script_class_inst.log_out_files = log_out_files
        script_class_inst.available_packages_in_repos = available_packages_in_repos
        Script.repository_util = RepositoryUtil(self.config_dict)
        method = RMFTestCase._get_attr(script_class_inst, command)
    except IOError, err:
      raise RuntimeError("Cannot load class %s from %s: %s" % (classname, norm_path, err.message))
    
    # Reload params import, otherwise it won't change properties during next import
    if 'params' in sys.modules:  
      del(sys.modules["params"])

    if 'params_windows' in sys.modules:
      del(sys.modules["params_windows"])

    if 'params_linux' in sys.modules:
      del(sys.modules["params_linux"])

    # Reload status_params import, otherwise it won't change properties during next import
    if 'status_params' in sys.modules:
      del(sys.modules["status_params"])

    with Environment(basedir, test_mode=True) as RMFTestCase.env:
      with patch('resource_management.core.shell.checked_call', side_effect=checked_call_mocks) as mocks_dict['checked_call']:
        with patch('resource_management.core.shell.call', side_effect=call_mocks) as mocks_dict['call']:
          with patch.object(Script, 'get_config', return_value=self.config_dict) as mocks_dict['get_config']:
            with patch.object(Script, 'get_tmp_dir', return_value="/tmp") as mocks_dict['get_tmp_dir']:
              with patch.object(Script, 'post_start') as mocks_dict['post_start']:
                with patch('resource_management.libraries.functions.get_kinit_path', return_value=kinit_path_local) as mocks_dict['get_kinit_path']:
                  with patch.object(platform, 'linux_distribution', return_value=os_type) as mocks_dict['linux_distribution']:
                    with patch('resource_management.libraries.functions.stack_select.is_package_supported', return_value=True):
                      with patch('resource_management.libraries.functions.stack_select.get_supported_packages', return_value=MagicMock()):
                        with patch.object(os, "environ", new=os_env) as mocks_dict['environ']:
                          with patch('resource_management.libraries.functions.stack_select.unsafe_get_stack_versions', return_value = (("",0,[]))):
                            if not try_install:
                              with patch.object(Script, 'install_packages') as install_mock_value:
                                method(RMFTestCase.env, *command_args)
                            else:
                              method(RMFTestCase.env, *command_args)

    sys.path.remove(scriptsdir)

  def get_config_file(self, configs_path, config_file):
    """
    Loads the specified JSON config file
    :param configs_path:
    :param config_file:
    :return:
    """
    config_file_path = os.path.join(configs_path, config_file)

    try:
      with open(config_file_path, "r") as f:
        return json.load(f)
    except IOError:
      raise RuntimeError("Can not read config file: " + config_file_path)


  def _get_test_paths(self, target, stack_version):
    """
    Gets the base and configs path variables.
    :param target:
    :param stack_version:
    :return:
    """
    src_dir = RMFTestCase.get_src_folder()

    if target == self.TARGET_STACKS:
      base_path = os.path.join(src_dir, PATH_TO_STACKS)

      configs_path = os.path.join(src_dir, PATH_TO_STACK_TESTS, "configs") if stack_version is None \
        else os.path.join(src_dir, PATH_TO_STACK_TESTS, stack_version, "configs")

      return base_path, configs_path
    elif target == self.TARGET_CUSTOM_ACTIONS:
      base_path = os.path.join(src_dir, PATH_TO_CUSTOM_ACTIONS)
      configs_path = os.path.join(src_dir, PATH_TO_CUSTOM_ACTION_TESTS, "configs")
      return base_path, configs_path
    elif target == self.TARGET_COMMON_SERVICES:
      base_path = os.path.join(src_dir, PATH_TO_COMMON_SERVICES)
      configs_path = os.path.join(src_dir, PATH_TO_STACK_TESTS, stack_version, "configs")
      return base_path, configs_path
    elif target == self.TARGET_STACK_HOOKS:
      base_path = os.path.join(src_dir, PATH_TO_STACK_HOOKS)

      configs_path = os.path.join(src_dir, PATH_TO_STACK_TESTS, "configs") if stack_version is None \
        else os.path.join(src_dir, PATH_TO_STACK_TESTS, stack_version, "configs")

      return base_path, configs_path
    else:
      raise RuntimeError("Wrong target value %s", target)


  def getConfig(self):
    return self.config_dict
          
  @staticmethod
  def get_src_folder():
    return os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
  
  @staticmethod
  def _getCommonServicesFolder():
    return os.path.join(RMFTestCase.get_src_folder(), PATH_TO_COMMON_SERVICES)

  @staticmethod
  def get_stack_tools():
    """
    Read stack_tools config property from resources/stacks/configs/stack_tools.json
    """
    stack_tools_file = os.path.join(RMFTestCase.get_src_folder(), PATH_TO_STACK_TESTS, "configs", "stack_tools.json")
    with open(stack_tools_file, "r") as f:
      return f.read()

  @staticmethod
  def get_stack_features():
    """
    Read stack_features config property from resources/stacks/configs/stack_features.json
    """
    stack_features_file = os.path.join(RMFTestCase.get_src_folder(), PATH_TO_STACK_TESTS, "configs", "stack_features.json")
    with open(stack_features_file, "r") as f:
      return f.read()

  @staticmethod
  def get_stack_packages():
    """
    Read stack_packages config property from resources/stacks/configs/stack_packages.json
    """
    stack_packages_file = os.path.join(RMFTestCase.get_src_folder(), PATH_TO_STACK_TESTS, "configs", "stack_packages.json")
    with open(stack_packages_file, "r") as f:
      return f.read()

  @staticmethod
  def _getStackTestsFolder():
    return os.path.join(RMFTestCase.get_src_folder(), PATH_TO_STACK_TESTS)

  @staticmethod
  def _get_attr(module, attr):
    module_methods = dir(module)
    if not attr in module_methods:
      raise RuntimeError("'{0}' has no attribute '{1}'".format(module, attr))
    method = getattr(module, attr)
    return method
  
  def _ppformat(self, val):
    if isinstance(val, dict) and len(val) > MAX_SHOWN_DICT_LEN:
      return "self.getConfig()['configurations']['?']"
    
    val = pprint.pformat(val)
    
    if val.startswith("u'") or val.startswith('u"'):
      return val[1:]
    
    return val

  def reindent(self, s, numSpaces):
    return "\n".join((numSpaces * " ") + i for i in s.splitlines())

  def printResources(self, intendation=4):
    print
    for resource in RMFTestCase.env.resource_list:
      s = "'{0}', {1},".format(
        resource.__class__.__name__, self._ppformat(resource.name))
      has_arguments = False
      for k,v in resource.arguments.iteritems():
        has_arguments = True
        # correctly output octal mode numbers
        if k == 'mode' and isinstance( v, int ):
          val = oct(v)
        elif  isinstance( v, UnknownConfiguration):
          val = "UnknownConfigurationMock()"
        elif hasattr(v, '__call__') and hasattr(v, '__name__'):
          val = "FunctionMock('{0}')".format(v.__name__)
        else:
          val = self._ppformat(v)
        # If value is multiline, format it
        if "\n" in val:
          lines = val.splitlines()
          firstLine = lines[0]
          nextlines = "\n".join(lines [1:])
          nextlines = self.reindent(nextlines, 2)
          val = "\n".join([firstLine, nextlines])
        param_str="{0} = {1},".format(k, val)
        s+="\n" + self.reindent(param_str, intendation)
      # Decide whether we want bracket to be at next line
      if has_arguments:
        before_bracket = "\n"
      else:
        before_bracket = ""
      # Add assertion
      s = "self.assertResourceCalled({0}{1})".format(s, before_bracket)
      # Intendation
      s = self.reindent(s, intendation)
      print s
    print(self.reindent("self.assertNoMoreResources()", intendation))

  def assertResourceCalledIgnoreEarlier(self, resource_type, name, **kwargs):
    """
    Fast fowards past earlier resources called, popping them off the list until the specified
    resource is hit. If it's not found, then an assertion is thrown that there are no more
    resources.
    """
    with patch.object(UnknownConfiguration, '__getattr__', return_value=lambda: "UnknownConfiguration()"):
      while len(RMFTestCase.env.resource_list) >= 0:
        # no more items means exit the loop
        self.assertNotEqual(len(RMFTestCase.env.resource_list), 0, "The specified resource was not found in the call stack.")

        # take the next resource and try it out
        resource = RMFTestCase.env.resource_list.pop(0)
        try:
          self.assertEquals(resource_type, resource.__class__.__name__)
          self.assertEquals(name, resource.name)
          self.assertEquals(kwargs, resource.arguments)
          break
        except AssertionError:
          pass

  def assertResourceCalled(self, resource_type, name, **kwargs):
    with patch.object(UnknownConfiguration, '__getattr__', return_value=lambda: "UnknownConfiguration()"):
      self.assertNotEqual(len(RMFTestCase.env.resource_list), 0, "There were no more resources executed!")
      resource = RMFTestCase.env.resource_list.pop(0)

      self.assertEquals(resource_type, resource.__class__.__name__)
      self.assertEquals(name, resource.name)
      self.assertEquals(kwargs, resource.arguments)

  def assertResourceCalledRegexp(self, resource_type, name, **kwargs):
    with patch.object(UnknownConfiguration, '__getattr__', return_value=lambda: "UnknownConfiguration()"):
      self.assertNotEqual(len(RMFTestCase.env.resource_list), 0, "There were no more resources executed!")
      resource = RMFTestCase.env.resource_list.pop(0)
      
      self.assertRegexpMatches(resource.__class__.__name__, resource_type)
      self.assertRegexpMatches(resource.name, name)
      for key in set(resource.arguments.keys()) | set(kwargs.keys()):
        resource_value = resource.arguments.get(key, '')
        actual_value = kwargs.get(key, '')
        if self.isstring(resource_value):
          self.assertRegexpMatches(resource_value, actual_value,
                                   msg="Key '%s': '%s' does not match with '%s'" % (key, resource_value, actual_value))
        else: # check only the type of a custom object
          self.assertEquals(resource_value.__class__.__name__, actual_value.__class__.__name__)

  def assertRegexpMatches(self, value, pattern, msg=None):
    if not re.match(pattern, value):
      raise AssertionError, msg or 'pattern %s does not match %s' % (pattern, value)

  def isstring(self, s):
    if (sys.version_info[0] == 3):
      return isinstance(s, str)
    return isinstance(s, basestring)

  def assertNoMoreResources(self):
    self.assertEquals(len(RMFTestCase.env.resource_list), 0, "There were other resources executed!")
    
  def assertResourceCalledByIndex(self, index, resource_type, name, **kwargs):
    resource = RMFTestCase.env.resource_list[index]
    self.assertEquals(resource_type, resource.__class__.__name__)
    self.assertEquals(name, resource.name)
    self.assertEquals(kwargs, resource.arguments)


# HACK: This is used to check Templates, StaticFile, InlineTemplate in testcases    
def Template(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import Template
    return Template(name, **kwargs)
  
def StaticFile(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import StaticFile
    from resource_management.core import sudo
    sudo.path_isfile = lambda path: True
    sudo.read_file = lambda path: 'dummy_output'
    return StaticFile(name, **kwargs)
  
def InlineTemplate(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import InlineTemplate
    return InlineTemplate(name, **kwargs)
  
class DownloadSource():
  def __init__(self, name, **kwargs):
    self.name = name
  
  def __eq__(self, other):
    from resource_management.core.source import DownloadSource
    return isinstance(other, DownloadSource) and self.name == other.name

class UnknownConfigurationMock():
  def __eq__(self, other):
    return isinstance(other, UnknownConfiguration)

  def __ne__(self, other):
    return not self.__eq__(other)
  
  def __repr__(self):
    return "UnknownConfigurationMock()"
  
class FunctionMock():
  def __init__(self, name):
    self.name = name
    
  def __ne__(self, other):
    return not self.__eq__(other)
    
  def __eq__(self, other):
    return hasattr(other, '__call__') and hasattr(other, '__name__') and self.name == other.__name__

class CallFunctionMock():
  """
  Used to mock callable with specified arguments and expected results.
  Callable will be called with arguments and result will be compared.
  """
  def __init__(self, call_result=None, *args, **kwargs):
    self.call_result = call_result
    self.args = args
    self.kwargs = kwargs

  def __ne__(self, other):
    return not self.__eq__(other)

  def __eq__(self, other):
    if hasattr(other, '__call__'):
      result = other(*self.args, **self.kwargs)
      return self.call_result == result
    return False

def experimental_mock(*args, **kwargs):
  """
  Used to disable experimental mocks...
  :return: 
  """
  def decorator(function):
    def wrapper(*args, **kwargs):
      return function(*args, **kwargs)
    return wrapper
  return decorator

