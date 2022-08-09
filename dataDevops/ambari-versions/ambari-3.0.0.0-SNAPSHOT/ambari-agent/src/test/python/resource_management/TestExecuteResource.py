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
from ambari_agent import main
main.MEMORY_LEAK_DEBUG_FILEPATH = "/tmp/memory_leak_debug.out"
from unittest import TestCase
from mock.mock import patch, MagicMock, call
from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from resource_management.core.system import System
from resource_management.core.resources.system import Execute
from resource_management.core.environment import Environment
from resource_management.core.shell import quote_bash_args

from ambari_commons import subprocess32
import logging
import os
from resource_management import Fail

if get_platform() != PLATFORM_WINDOWS:
  import grp
  import pwd

import select


@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestExecuteResource(TestCase):
  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch.object(logging.Logger, "info")
  @patch.object(subprocess32, "Popen")
  def test_attribute_logoutput(self, popen_mock, info_mock, select_mock, os_read_mock):
    subproc_mock = MagicMock()
    subproc_mock.wait.return_value = MagicMock()
    subproc_mock.stdout = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    select_mock.return_value = ([subproc_mock.stdout], None, None)
    os_read_mock.return_value = None

    with Environment("/") as env:
      Execute('echo "1"',
              logoutput=True)
      Execute('echo "2"',
              logoutput=False)

    info_mock.assert_called('1')
    self.assertTrue("call('2')" not in str(info_mock.mock_calls))
    
  @patch('subprocess32.Popen.communicate')
  @patch.object(subprocess32, "Popen")
  def test_attribute_wait(self, popen_mock, proc_communicate_mock):
    with Environment("/") as env:
      Execute('echo "1"',
              wait_for_finish=False)
      Execute('echo "2"',
              wait_for_finish=False)
    
    self.assertTrue(popen_mock.called, 'subprocess32.Popen should have been called!')
    self.assertFalse(proc_communicate_mock.called, 'proc.communicate should not have been called!')

  @patch("resource_management.core.sudo.path_exists")
  @patch.object(subprocess32, "Popen")
  def test_attribute_creates(self, popen_mock, exists_mock):
    exists_mock.return_value = True

    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout.readline = MagicMock(side_effect = ['OK'])
    subproc_mock.communicate.side_effect = [["1"]]
    popen_mock.return_value = subproc_mock

    with Environment("/") as env:
      Execute('echo "1"',
              creates="/must/be/created")

    exists_mock.assert_called_with("/must/be/created")
    self.assertEqual(subproc_mock.call_count, 0)

  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch.object(subprocess32, "Popen")
  def test_attribute_path(self, popen_mock, select_mock, os_read_mock):
    subproc_mock = MagicMock()
    subproc_mock.wait.return_value = MagicMock()
    subproc_mock.stdout = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    select_mock.return_value = ([subproc_mock.stdout], None, None)
    os_read_mock.return_value = None
    
    with Environment("/") as env:
      execute_resource = Execute('echo "1"',
                                 path=["/test/one", "test/two"]
      )
    expected_command = ['/bin/bash', '--login', '--noprofile', '-c', 'echo "1"']
    self.assertEqual(popen_mock.call_args_list[0][0][0], expected_command)

  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch('time.sleep')
  @patch.object(subprocess32, "Popen")
  def test_attribute_try_sleep_tries(self, popen_mock, time_mock, select_mock, os_read_mock):
    expected_call = "call('Retrying after %d seconds. Reason: %s', 1, 'Fail')"
    
    subproc_mock_one = MagicMock()
    subproc_mock_one.returncode = 1
    subproc_mock_one.stdout = MagicMock()
    subproc_mock_zero = MagicMock()
    subproc_mock_zero.stdout = MagicMock()
    subproc_mock_zero.returncode = 0
    #subproc_mock.stdout.readline = MagicMock(side_effect = [Fail("Fail"), "OK"])
    popen_mock.side_effect = [subproc_mock_one, subproc_mock_zero]
    select_mock.side_effect = [([subproc_mock_one.stdout], None, None),([subproc_mock_zero.stdout], None, None)]
    os_read_mock.return_value = None

    with Environment("/") as env:
      Execute('echo "1"',
              tries=2,
              try_sleep=10
      )
    pass

    self.assertTrue(call(10) in time_mock.call_args_list)

  @patch("pwd.getpwnam")
  def test_attribute_group(self, getpwnam_mock):
    def error(argument):
      self.assertEqual(argument, "test_user")
      raise KeyError("fail")

    getpwnam_mock.side_effect = error
    try:
      with Environment("/") as env:
        Execute('echo "1"',
                user="test_user",
        )
    except Fail as e:
      pass

  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch.object(subprocess32, "Popen")
  def test_attribute_environment(self, popen_mock, select_mock, os_read_mock):
    expected_dict = {"JAVA_HOME": "/test/java/home"}

    subproc_mock = MagicMock()
    subproc_mock.wait.return_value = MagicMock()
    subproc_mock.stdout = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    select_mock.return_value = ([subproc_mock.stdout], None, None)
    os_read_mock.return_value = None

    with Environment("/") as env:
      Execute('echo "1"',
              environment=expected_dict
      )

    self.assertEqual(popen_mock.call_args_list[0][1]["env"], expected_dict)
    pass

  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch.object(subprocess32, "Popen")
  def test_attribute_environment_non_root(self, popen_mock, select_mock, os_read_mock):
    expected_user = 'test_user'

    subproc_mock = MagicMock()
    subproc_mock.wait.return_value = MagicMock()
    subproc_mock.stdout = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    select_mock.return_value = ([subproc_mock.stdout], None, None)
    os_read_mock.return_value = None

    with Environment("/") as env:
      execute_resource = Execute('echo "1"',
                                 user=expected_user,
                                 environment={'JAVA_HOME': '/test/java/home',
                                              'PATH': "/bin"}
      )
      

    expected_command = ['/bin/bash', '--login', '--noprofile', '-c', 'ambari-sudo.sh su test_user -l -s /bin/bash -c ' + quote_bash_args('export  PATH=' + quote_bash_args(os.environ['PATH'] + ':/bin') + ' JAVA_HOME=/test/java/home ; echo "1"')]
    self.assertEqual(popen_mock.call_args_list[0][0][0], expected_command)


  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch.object(subprocess32, "Popen")
  def test_attribute_cwd(self, popen_mock, select_mock, os_read_mock):
    expected_cwd = "/test/work/directory"

    subproc_mock = MagicMock()
    subproc_mock.wait.return_value = MagicMock()
    subproc_mock.stdout = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    select_mock.return_value = ([subproc_mock.stdout], None, None)
    os_read_mock.return_value = None

    with Environment("/") as env:
      Execute('echo "1"',
              cwd=expected_cwd
      )

    self.assertEqual(popen_mock.call_args_list[0][1]["cwd"], expected_cwd)

  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch.object(subprocess32, "Popen")
  def test_attribute_command_escaping(self, popen_mock, select_mock, os_read_mock):
    expected_command0 = "arg1 arg2 'quoted arg'"
    expected_command1 = "arg1 arg2 'command \"arg\"'"
    expected_command2 = 'arg1 arg2 \'command \'"\'"\'arg\'"\'"\'\''
    expected_command3 = "arg1 arg2 'echo `ls /root`'"
    expected_command4 = "arg1 arg2 '$ROOT'"
    expected_command5 = "arg1 arg2 '`ls /root`'"

    subproc_mock = MagicMock()
    subproc_mock.wait.return_value = MagicMock()
    subproc_mock.stdout = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    select_mock.return_value = ([subproc_mock.stdout], None, None)
    os_read_mock.return_value = None

    with Environment("/") as env:
      Execute(('arg1', 'arg2', 'quoted arg'),
      )
      Execute(('arg1', 'arg2', 'command "arg"'),
      )
      Execute(('arg1', 'arg2', "command 'arg'"),
      )
      Execute(('arg1', 'arg2', "echo `ls /root`"),
      )
      Execute(('arg1', 'arg2', "$ROOT"),
      )
      Execute(('arg1', 'arg2', "`ls /root`"),
      )

    self.assertEqual(popen_mock.call_args_list[0][0][0][4], expected_command0)
    self.assertEqual(popen_mock.call_args_list[1][0][0][4], expected_command1)
    self.assertEqual(popen_mock.call_args_list[2][0][0][4], expected_command2)
    self.assertEqual(popen_mock.call_args_list[3][0][0][4], expected_command3)
    self.assertEqual(popen_mock.call_args_list[4][0][0][4], expected_command4)
    self.assertEqual(popen_mock.call_args_list[5][0][0][4], expected_command5)

  @patch.object(os, "read")
  @patch.object(select, "select")
  @patch.object(subprocess32, "Popen")
  def test_attribute_command_one_line(self, popen_mock, select_mock, os_read_mock):
    expected_command = "rm -rf /somedir"

    subproc_mock = MagicMock()
    subproc_mock.wait.return_value = MagicMock()
    subproc_mock.stdout = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    select_mock.return_value = ([subproc_mock.stdout], None, None)
    os_read_mock.return_value = None

    with Environment("/") as env:
      Execute(expected_command)

    self.assertEqual(popen_mock.call_args_list[0][0][0][4], expected_command)
