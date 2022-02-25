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

import StringIO
import sys
from unittest import TestCase
from mock.mock import patch, MagicMock
from only_for_platform import not_for_platform, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck, OSConst

utils = __import__('ambari_server.utils').utils


@not_for_platform(PLATFORM_WINDOWS)
class TestUtils(TestCase):

  @patch.object(OSCheck, "get_os_family")
  @patch('os.listdir')
  @patch('os.path.isdir')
  def test_get_ubuntu_pg_version(self, path_isdir_mock, os_listdir_mock, get_os_family_mock):
    get_os_family_mock.return_value = OSConst.UBUNTU_FAMILY
    path_isdir_mock.return_value = True
    os_listdir_mock.return_value = ['8.4', '9.1']

    self.assertEqual('9.1', utils.get_ubuntu_pg_version())

  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch('ambari_server.utils.get_ubuntu_pg_version')
  def test_get_postgre_running_status(self, get_ubuntu_pg_version_mock, is_redhat_family, is_ubuntu_family, is_suse_family):
    is_redhat_family.return_value = False
    is_ubuntu_family.return_value = True
    is_suse_family.return_value = False
    utils.PG_STATUS_RUNNING_DEFAULT = "red_running"
    get_ubuntu_pg_version_mock.return_value = '9.1'

    self.assertEqual('9.1/main', utils.get_postgre_running_status())
    is_redhat_family.return_value = True
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = False
    self.assertEqual('red_running', utils.get_postgre_running_status())

  @patch('os.path.isfile')
  def test_locate_file(self, isfile_mock):
    utils.ENV_PATH = ['/test']
    # File was found in the path
    isfile_mock.return_value = True
    self.assertEquals('/test/myfile', utils.locate_file('myfile'))
    # File not found in the path
    isfile_mock.return_value = False
    self.assertEquals('myfile', utils.locate_file('myfile'))
    # Testing default vaule
    isfile_mock.return_value = False
    self.assertEquals('/tmp/myfile', utils.locate_file('myfile', '/tmp'))

  @patch('os.path.exists')
  @patch('os.path.join')
  def test_pid_exists(self, path_join_mock, path_exists_mock):
    path_join_mock.return_value = '/test'
    path_exists_mock.return_value = True
    self.assertTrue(utils.pid_exists('1'))

  @patch('time.time')
  @patch('__builtin__.open')
  @patch('time.sleep')
  @patch('os.listdir')
  @patch('os.path.join')
  @patch.object(utils, 'get_symlink_path')
  def test_looking_for_pid(self, get_symlink_path_mock, path_join_mock,
                      listdir_mock, sleep_mock, open_mock, time_mock):
    def test_read():
      return "test args"

    def test_obj():
      pass

    test_obj.read = test_read
    path_join_mock.return_value = '/'
    open_mock.return_value = test_obj
    listdir_mock.return_value = ['1000']
    get_symlink_path_mock.return_value = "/symlinkpath"
    time_mock.side_effect = [0, 0, 0, 0, 0, 0, 6]

    out = StringIO.StringIO()
    sys.stdout = out
    r = utils.looking_for_pid("test args", 5)
    self.assertEqual(".....", out.getvalue())
    sys.stdout = sys.__stdout__

    self.assertEquals(len(r), 1)
    self.assertEquals(r[0], {
       "pid": "1000",
       "exe": "/symlinkpath",
       "cmd": "test args"
      })

  @patch('os.path.normpath')
  @patch('os.path.join')
  @patch('os.path.dirname')
  @patch('os.readlink')
  def test_get_symlink_path(self, readlink_mock, dirname_mock, join_mock,
                            normpath_mock):
    normpath_mock.return_value = "test value"
    self.assertEquals(utils.get_symlink_path("/"), "test value")

  @patch.object(utils, 'pid_exists')
  @patch('__builtin__.open')
  @patch('os.kill')
  def test_save_main_pid_ex(self, kill_mock, open_mock, pid_exists_mock):
    def test_write(data):
      self.assertEquals(data, "222\n")

    def test_close():
      pass

    def test_obj():
      pass

    test_obj.write = test_write
    test_obj.close = test_close
    open_mock.return_value = test_obj
    pid_exists_mock.return_value = True

    utils.save_main_pid_ex([{"pid": "111",
                             "exe": "/exe1",
                             "cmd": ""
                             },
                            {"pid": "222",
                             "exe": "/exe2",
                             "cmd": ""
                             },
                            ], "/pidfile", ["/exe1"])
    self.assertEquals(open_mock.call_count, 1)
    self.assertEquals(pid_exists_mock.call_count, 4)
    self.assertEquals(kill_mock.call_count, 1)

  @patch('os.path.isfile')
  @patch('__builtin__.open')
  @patch('os.remove')
  def test_check_exitcode(self, remove_mock, open_mock, isfile_mock):
    def test_read():
      return "777"

    def test_close():
      pass

    def test_obj():
      pass

    test_obj.read = test_read
    test_obj.close = test_close
    open_mock.return_value = test_obj
    isfile_mock.return_value = True

    self.assertEquals(utils.check_exitcode("/tmp/nofile"), 777)


  def test_format_with_reload(self):
    from resource_management.libraries.functions import format
    from resource_management.libraries.functions.format import ConfigurationFormatter
    from resource_management.core.environment import Environment

    env = Environment()
    with env:
      # declare some environment variables
      env_params = {}
      env_params["envfoo"] = "env-foo1"
      env_params["envbar"] = "env-bar1"
      env.config.params = env_params

      # declare some local variables
      foo = "foo1"
      bar = "bar1"

      # make sure local variables and env variables work
      message = "{foo} {bar} {envfoo} {envbar}"
      formatted_message = format(message)
      self.assertEquals("foo1 bar1 env-foo1 env-bar1", formatted_message)

      # try the same thing with an instance; we pass in keyword args to be
      # combined with the env params
      formatter = ConfigurationFormatter()
      formatted_message = formatter.format(message, foo="foo2", bar="bar2")
      self.assertEquals("foo2 bar2 env-foo1 env-bar1", formatted_message)

      # now supply keyword args to override env params
      formatted_message = formatter.format(message, envfoo="foobar", envbar="foobarbaz", foo="foo3", bar="bar3")
      self.assertEquals("foo3 bar3 foobar foobarbaz", formatted_message)

  def test_compare_versions(self):
    self.assertEquals(utils.compare_versions("1.7.0", "2.0.0"), -1)
    self.assertEquals(utils.compare_versions("2.0.0", "2.0.0"), 0)
    self.assertEquals(utils.compare_versions("2.1.0", "2.0.0"), 1)

    self.assertEquals(utils.compare_versions("1.7.0_abc", "2.0.0-abc"), -1)
    self.assertEquals(utils.compare_versions("2.0.0.abc", "2.0.0_abc"), 0)
    self.assertEquals(utils.compare_versions("2.1.0-abc", "2.0.0.abc"), 1)

    self.assertEquals(utils.compare_versions("2.1.0-1","2.0.0-2"),1)
    self.assertEquals(utils.compare_versions("2.0.0_1","2.0.0-2"),0)
    self.assertEquals(utils.compare_versions("2.0.0-1","2.0.0-2"),0)
    self.assertEquals(utils.compare_versions("2.0.0_1","2.0.0_2"),0)
    self.assertEquals(utils.compare_versions("2.0.0-abc","2.0.0_abc"),0)

class FakeProperties(object):
  def __init__(self, prop_map):
    self.prop_map = prop_map

  def get_property(self, prop_name):
    return self.prop_map[prop_name]
