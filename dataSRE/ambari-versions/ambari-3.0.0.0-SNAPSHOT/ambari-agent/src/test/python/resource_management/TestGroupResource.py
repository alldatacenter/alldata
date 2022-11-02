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

from resource_management.core import Environment, Fail
from resource_management.core.resources import Group
from resource_management.core.system import System
from resource_management.core.shell import preexec_fn

import os
import select
from ambari_commons import subprocess32

if get_platform() != PLATFORM_WINDOWS:
  import grp
  import pty


subproc_stdout = MagicMock()

@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
@patch.object(os, "read", new=MagicMock(return_value=None))
@patch.object(select, "select", new=MagicMock(return_value=([subproc_stdout], None, None)))
@patch.object(os, "environ", new = {'PATH':'/bin'})
@patch("pty.openpty", new = MagicMock(return_value=(1,5)))
@patch.object(os, "close", new=MagicMock())
class TestGroupResource(TestCase):

  @patch("grp.getgrnam")
  @patch.object(subprocess32, "Popen")
  def test_action_create_nonexistent(self, popen_mock, getgrnam_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getgrnam_mock.side_effect = KeyError()

    with Environment('/') as env:
      Group('hadoop',
            action='create',
            password='secure'
      )
    

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E groupadd -p secure hadoop"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    getgrnam_mock.assert_called_with('hadoop')


  @patch("grp.getgrnam")
  @patch.object(subprocess32, "Popen")
  def test_action_create_existent(self, popen_mock, getgrnam_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = _get_group()

    with Environment('/') as env:
      Group('mapred',
            action='create',
            gid=2,
            password='secure'
      )
    

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E groupmod -p secure -g 2 mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    getgrnam_mock.assert_called_with('mapred')


  @patch("grp.getgrnam")
  @patch.object(subprocess32, "Popen")
  def test_action_create_fail(self, popen_mock, getgrnam_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 1
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = _get_group()

    try:
      with Environment('/') as env:
        Group('mapred',
              action='create',
              gid=2,
              password='secure'
        )
      
      self.fail("Action 'create' should fail when checked_call fails")
    except Fail:
      pass
    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E groupmod -p secure -g 2 mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    getgrnam_mock.assert_called_with('mapred')


  @patch("grp.getgrnam")
  @patch.object(subprocess32, "Popen")
  def test_action_remove(self, popen_mock, getgrnam_mock):

    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = _get_group()

    with Environment('/') as env:
      Group('mapred',
            action='remove'
      )
    

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', 'ambari-sudo.sh  PATH=/bin -H -E groupdel mapred'], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    getgrnam_mock.assert_called_with('mapred')


  @patch("grp.getgrnam")
  @patch.object(subprocess32, "Popen")
  def test_action_remove_fail(self, popen_mock, getgrnam_mock):

    subproc_mock = MagicMock()
    subproc_mock.returncode = 1
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = _get_group()

    try:
      with Environment('/') as env:
        Group('mapred',
              action='remove'
        )
      
      self.fail("Action 'delete' should fail when checked_call fails")
    except Fail:
      pass

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', 'ambari-sudo.sh  PATH=/bin -H -E groupdel mapred'], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    getgrnam_mock.assert_called_with('mapred')
    
def _get_group():
  group = MagicMock()
  group.gr_name='mapred'
  group.gr_passwd='x'
  group.gr_gid=0
  group.gr_mem=[]
  
  return group
  
