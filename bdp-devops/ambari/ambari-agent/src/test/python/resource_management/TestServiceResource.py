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

from unittest import TestCase
from mock.mock import patch, MagicMock
from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from resource_management.core.environment import Environment
from resource_management.core.exceptions import Fail
from resource_management.core.resources.service import Service


@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestServiceResource(TestCase):

  @patch("resource_management.core.shell.call")
  def test_action_start(self,shell_mock):
    return_values = [(-1, None),(0,None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="start")
    self.assertEqual(shell_mock.call_count,2)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])
    self.assertEqual(shell_mock.call_args_list[1][0][0],['/etc/init.d/some_service','start'])

  @patch("resource_management.core.shell.call")
  def test_action_stop(self,shell_mock):
    return_values = [(0, None),(0,None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="stop")
    self.assertEqual(shell_mock.call_count,2)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])
    self.assertEqual(shell_mock.call_args_list[1][0][0],['/etc/init.d/some_service','stop'])

  @patch("resource_management.core.shell.call")
  def test_action_restart(self,shell_mock):
    return_values = [(0, None),(0,None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="restart")
    self.assertEqual(shell_mock.call_count,2)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])
    self.assertEqual(shell_mock.call_args_list[1][0][0],['/etc/init.d/some_service','restart'])

  @patch("resource_management.core.shell.call")
  def test_action_start_running(self,shell_mock):
    return_values = [(0, None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="start")
    self.assertEqual(shell_mock.call_count,1)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])

  @patch("resource_management.core.shell.call")
  def test_action_stop_stopped(self,shell_mock):
    return_values = [(1, None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="stop")
    self.assertEqual(shell_mock.call_count,1)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])

  @patch("resource_management.core.shell.call")
  def test_action_restart_stopped(self,shell_mock):
    return_values = [(1, None),(0,None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="restart")
    self.assertEqual(shell_mock.call_count,2)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])
    self.assertEqual(shell_mock.call_args_list[1][0][0],['/etc/init.d/some_service','start'])

  @patch("resource_management.core.shell.call")
  def test_action_reload(self,shell_mock):
    return_values = [(0, None),(0,None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="reload")
    self.assertEqual(shell_mock.call_count,2)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])
    self.assertEqual(shell_mock.call_args_list[1][0][0],['/etc/init.d/some_service','reload'])

  @patch("resource_management.core.shell.call")
  def test_action_reload_stopped(self,shell_mock):
    return_values = [(1, None),(0,None)]
    shell_mock.side_effect = return_values
    with Environment('/') as env:
      Service('some_service', action="reload")
    self.assertEqual(shell_mock.call_count,2)
    self.assertEqual(shell_mock.call_args_list[0][0][0],['/etc/init.d/some_service','status'])
    self.assertEqual(shell_mock.call_args_list[1][0][0],['/etc/init.d/some_service','start'])

  @patch("resource_management.core.shell.call")
  def test_action_nothing(self,shell_mock):
    shell_mock.return_value = (0,0)
    with Environment('/') as env:
      Service('some_service', action="nothing")
    self.assertEqual(shell_mock.call_count,0)

  def test_action_not_existing(self):
    try:
      with Environment('/') as env:
        Service('some_service', action="not_existing_action")
      self.fail("Service should fail with nonexistent action")
    except Fail as e:
      self.assertEqual("ServiceProvider[Service['some_service']] does not implement action not_existing_action",str(e))
