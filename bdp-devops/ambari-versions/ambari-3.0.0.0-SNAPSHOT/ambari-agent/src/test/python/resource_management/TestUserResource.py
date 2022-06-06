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
from mock.mock import patch, MagicMock, PropertyMock

from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from resource_management.core import Environment, Fail
from resource_management.core.system import System
from resource_management.core.resources import User
from resource_management.core.shell import preexec_fn
from ambari_commons import subprocess32
import os
import select

if get_platform() != PLATFORM_WINDOWS:
  import pwd
  import pty


subproc_stdout = MagicMock()

@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
@patch.object(os, "read", new=MagicMock(return_value=None))
@patch.object(select, "select", new=MagicMock(return_value=([subproc_stdout], None, None)))
@patch.object(os, "environ", new = {'PATH':'/bin'})
@patch("pty.openpty", new = MagicMock(return_value=(1,5)))
@patch.object(os, "close", new=MagicMock())
class TestUserResource(TestCase):

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_action_create_nonexistent(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = None
    with Environment('/') as env:
      user = User("mapred", action = "create", shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E useradd -m -s /bin/bash mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)
    
  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_action_create_existent(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E usermod -s /bin/bash mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_action_delete(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = 1

    with Environment('/') as env:
      user = User("mapred", action = "remove", shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', 'ambari-sudo.sh  PATH=/bin -H -E userdel mapred'], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_attribute_comment(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", comment = "testComment", 
          shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E usermod -c testComment -s /bin/bash mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_attribute_home(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", home = "/test/home", 
          shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E usermod -s /bin/bash -d /test/home mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_attribute_password(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", password = "secure", 
          shell = "/bin/bash")    

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E usermod -s /bin/bash -p secure mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_attribute_shell(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", shell = "/bin/sh")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E usermod -s /bin/sh mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_attribute_uid(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", uid = 1, shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E usermod -s /bin/bash -u 1 mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_attribute_gid(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", gid = "1", shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E usermod -s /bin/bash -g 1 mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch('resource_management.core.providers.accounts.UserProvider.user_groups', new_callable=PropertyMock)
  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_attribute_groups(self, getpwnam_mock, popen_mock, user_groups_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    user_groups_mock.return_value = ['hadoop']
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = _get_user_entity()

    with Environment('/') as env:
      user = User("mapred", action = "create", groups = ['1','2','3'], 
          shell = "/bin/bash")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', 'ambari-sudo.sh  PATH=/bin -H -E usermod -s /bin/bash -G 1,2,3,hadoop mapred'], shell=False, preexec_fn=preexec_fn, env={'PATH': '/bin'}, close_fds=True, stdout=-1, stderr=-2, cwd=None)
    self.assertEqual(popen_mock.call_count, 1)

  @patch.object(subprocess32, "Popen")
  @patch("pwd.getpwnam")
  def test_missing_shell_argument(self, getpwnam_mock, popen_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    subproc_mock.stdout = subproc_stdout
    popen_mock.return_value = subproc_mock
    getpwnam_mock.return_value = None
    with Environment('/') as env:
      user = User("mapred", action = "create")

    popen_mock.assert_called_with(['/bin/bash', '--login', '--noprofile', '-c', "ambari-sudo.sh  PATH=/bin -H -E useradd -m mapred"], shell=False, preexec_fn=preexec_fn, stderr=-2, stdout=-1, env={'PATH': '/bin'}, cwd=None, close_fds=True)
    self.assertEqual(popen_mock.call_count, 1)

  @patch('__builtin__.open')
  @patch("pwd.getpwnam")
  def test_parsing_local_users(self, pwd_mock, open_mock):
    """
    Tests that parsing users out of /etc/groups can tolerate some bad lines
    """
    class MagicFile(object):
      def read(self):
        return  """
          group1:x:1:
          group2:x:2:user1,user2
          group3:x:3
          invalid
        """

      def __exit__(self, exc_type, exc_val, exc_tb):
        pass

      def __enter__(self):
        return self

    pwd_mock.return_value = "user1"
    open_mock.return_value = MagicFile()

    from resource_management.core.providers.accounts import UserProvider

    user = MagicMock()
    provider = UserProvider(user)
    provider.resource.username = "user1"
    provider.resource.fetch_nonlocal_groups = False
    groups = provider.user_groups

    self.assertEquals(1, len(groups))
    self.assertTrue("group2" in groups)


def _get_user_entity():
  user = MagicMock()
  user.pw_name='mapred'
  user.pw_passwd='x'
  user.pw_uid=0
  user.pw_gid=0
  user.pw_gecos='root'
  user.pw_dir='/root'
  user.pw_shell='/bin/false'
  
  return user
