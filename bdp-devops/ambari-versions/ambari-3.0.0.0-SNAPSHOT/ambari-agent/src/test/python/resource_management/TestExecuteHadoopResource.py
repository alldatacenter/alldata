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

from unittest import TestCase
from mock.mock import patch, MagicMock
from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from resource_management import *
from resource_management.libraries.resources.execute_hadoop\
  import ExecuteHadoop

@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestExecuteHadoopResource(TestCase):
  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_default_args(self, execute_mock):
    '''
    Test if default arguments are passed to Execute
    '''
    with Environment() as env:
      ExecuteHadoop("command",
                    conf_dir="conf_dir",
                    user="user",
                    logoutput=True,
      )
      self.assertEqual(execute_mock.call_count, 1)
      self.assertEqual(execute_mock.call_args[0][0].command,'hadoop --config conf_dir command')
      self.assertEqual(execute_mock.call_args[0][0].arguments,
                       {'logoutput': True,
                        'tries': 1,
                        'user': 'user',
                        'try_sleep': 0,
                        'path': [],
                        'environment': {}})

  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_command_list(self, execute_mock):
    '''
    Test for "command" passed as List
    '''
    with Environment("/") as env:
      ExecuteHadoop(["command1","command2"],
                    action="run",
                    conf_dir="conf_dir",
                    user="user",
      )
      self.assertEqual(execute_mock.call_count, 2)
      self.assertEqual(execute_mock.call_args_list[0][0][0].command,
                       'hadoop --config conf_dir command1')
      self.assertEqual(execute_mock.call_args_list[1][0][0].command,
                       'hadoop --config conf_dir command2')
      self.assertEqual(execute_mock.call_args_list[0][0][0].arguments,
                       {'logoutput': None,
                        'tries': 1,
                        'user': 'user',
                        'environment': {},
                        'try_sleep': 0,
                        'path': []})
      self.assertEqual(execute_mock.call_args_list[1][0][0].arguments,
                       {'logoutput': None,
                        'tries': 1,
                        'user': 'user',
                        'try_sleep': 0,
                        'path': [],
                        'environment': {}})


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_command_tuple(self, execute_mock):
    '''
    Test for "command" passed as Tuple
    '''
    with Environment("/") as env:
      ExecuteHadoop(("command1","command2","command3"),
                    action="run",
                    conf_dir="conf_dir",
                    user="user",
      )
      self.assertEqual(execute_mock.call_count, 1)
      self.assertEqual(execute_mock.call_args[0][0].command,
                       'hadoop --config conf_dir command1 command2 command3')